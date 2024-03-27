/**
 * Copyright (C) 2010, Byte-Code srl <http://www.byte-code.com>
 * Copyright (C) 2012, Philipp Bartsch <tastybug@tastybug.com>
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package hudson.plugins.libvirt;

import hudson.model.TaskListener;

import hudson.model.Descriptor;
import hudson.plugins.libvirt.lib.VirtException;
import hudson.slaves.Cloud;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;

import jenkins.model.Jenkins;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Marco Mornati
 * @author Philipp Bartsch
 */
public class VirtualMachineLauncher extends ComputerLauncher {

    private static final Logger LOGGER = Logger.getLogger(VirtualMachineLauncher.class.getName());

    private final ComputerLauncher delegate;
    private transient VirtualMachine virtualMachine;
    private final String hypervisorDescription;
    private final String virtualMachineName;
    private final int waitingTimeSecs;
    private final int timesToRetryOnFailure;

    @DataBoundConstructor
    public VirtualMachineLauncher(ComputerLauncher delegate, String hypervisorDescription, String virtualMachineName,
            int waitingTimeSecs, int timesToRetryOnFailure) {
        super();
        this.delegate = delegate;
        this.virtualMachineName = virtualMachineName;
        this.hypervisorDescription = hypervisorDescription;
        this.waitingTimeSecs = waitingTimeSecs;
        this.timesToRetryOnFailure = timesToRetryOnFailure;
        lookupVirtualMachineHandle();
    }

    private void lookupVirtualMachineHandle() {
        if (hypervisorDescription != null && virtualMachineName != null) {
            LOGGER.log(Level.FINE, "Grabbing hypervisor...");
            Hypervisor hypervisor;
            try {
                hypervisor = lookupHypervisorInstance();
                LOGGER.log(Level.FINE, "Hypervisor found, searching for a matching virtual machine for \"{0}\"...", virtualMachineName);

                for (VirtualMachine vm : hypervisor.getVirtualMachines()) {
                    if (vm.getName().equals(virtualMachineName)) {
                        virtualMachine = vm;
                        break;
                    }
                }
            } catch (VirtException e) {
                LOGGER.log(Level.SEVERE, "no Hypervisor found, searching for a matching virtual machine for \"{0}\" {1}", new Object[]{virtualMachineName, e.getMessage()});
            }
        }
    }

    public ComputerLauncher getDelegate() {
        return delegate;
    }

    public VirtualMachine getVirtualMachine() {
        lookupVirtualMachineHandle();
        return virtualMachine;
    }

    public String getVirtualMachineName() {
        return virtualMachineName;
    }

    @Override
    public boolean isLaunchSupported() {
        return true;
    }

    private Hypervisor lookupHypervisorInstance() throws VirtException {
        if (hypervisorDescription != null && virtualMachineName != null) {
            for (Cloud cloud : Jenkins.get().clouds) {
                if (cloud instanceof Hypervisor && ((Hypervisor) cloud).getHypervisorDescription().equals(hypervisorDescription)) {
                    return (Hypervisor) cloud;
                }
            }
        }
        LOGGER.log(Level.SEVERE, "Could not find our libvirt cloud instance!");
        throw new VirtException("Could not find our libvirt cloud instance!");
    }

    public Hypervisor findOurHypervisorInstance() throws VirtException {
        return lookupHypervisorInstance();
    }

    @Override
    public void launch(SlaveComputer slaveComputer, TaskListener taskListener) throws IOException, InterruptedException {

        taskListener.getLogger().println("Virtual machine \"" + virtualMachineName + "\" (agent title \"" + slaveComputer.getDisplayName() + "\") is to be started.");
        try {
            if (virtualMachine == null) {
                taskListener.getLogger().println("No connection ready to the Hypervisor, connecting...");
                lookupVirtualMachineHandle();
                if (virtualMachine == null) { // still null? no such vm!
                    throw new Exception(
                        "Virtual machine \"" + virtualMachineName + "\" (agent title \"" + slaveComputer.getDisplayName() + "\") not found on the specified hypervisor!"
                    );
                }
            }

            ComputerUtils.start(virtualMachine, taskListener);
            taskListener.getLogger().println("Waiting for " + waitingTimeSecs + "s to let it fully boot up...");
            Thread.sleep(TimeUnit.SECONDS.toMillis(waitingTimeSecs));

            int retries = -1;
            while (true) {
                retries++;

                taskListener.getLogger().println("Connecting agent client.");

                // This call doesn't seem to actually throw anything, but we'll catch IOException just in case
                try {
                    delegate.launch(slaveComputer, taskListener);
                } catch (IOException e) {
                    taskListener.getLogger().println("unexpectedly caught exception when delegating launch of agent: " + e.getMessage());
                }

                if (slaveComputer.isOnline()) {
                    break;
                } else if (retries >= timesToRetryOnFailure) {
                    taskListener.getLogger().println("Maximum retries reached. Failed to start agent client.");
                    break;
                }

                taskListener.getLogger().println("Not up yet, waiting for " + waitingTimeSecs + "s more ("
                                                 + retries + "/" + timesToRetryOnFailure + " retries)");
                Thread.sleep(TimeUnit.SECONDS.toMillis(waitingTimeSecs));
            }
        } catch (IOException e) {
            taskListener.fatalError(e.getMessage(), e);

            LogRecord rec = new LogRecord(Level.SEVERE, "Error while launching {0} on Hypervisor {1}.");
            rec.setParameters(new Object[]{virtualMachine.getName(), virtualMachine.getHypervisor().getHypervisorURI()});
            rec.setThrown(e);
            LOGGER.log(rec);
            throw e;
        } catch (Exception t) {
            taskListener.fatalError(t.getMessage(), t);

            LogRecord rec = new LogRecord(Level.SEVERE, "Error while launching {0} on Hypervisor {1}.");
            rec.setParameters(new Object[]{virtualMachine.getName(), virtualMachine.getHypervisor().getHypervisorURI()});
            rec.setThrown(t);
            LOGGER.log(rec);
        }
    }

    @Override
    public synchronized void afterDisconnect(SlaveComputer slaveComputer, TaskListener taskListener) {
        delegate.afterDisconnect(slaveComputer, taskListener);
    }

    @Override
    public void beforeDisconnect(SlaveComputer slaveComputer, TaskListener taskListener) {
        delegate.beforeDisconnect(slaveComputer, taskListener);
    }

    @Override
    public Descriptor<ComputerLauncher> getDescriptor() {
        throw new UnsupportedOperationException();
    }

    public boolean getTcpSupported() {
        return Jenkins.get().getTcpSlaveAgentListener() != null;
    }

    public boolean getInstanceIdentityInstalled() {
        return Jenkins.get().getPluginManager().getPlugin("instance-identity") != null;
    }

    public boolean getWebSocketSupported() {
        // HACK!! Work around @Restricted(Beta.class). Normally, we would write:
        // return WebSockets.isSupported();
        try {
            Class<?> cl = Class.forName("jenkins.websocket.WebSockets");
            Method m =  cl.getMethod("isSupported");
            return (boolean) m.invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException x) {
            return false;
        }
    }
}
