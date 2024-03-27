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
import hudson.slaves.DelegatingComputerLauncher;
import hudson.slaves.SlaveComputer;

import jenkins.model.Jenkins;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Marco Mornati
 * @author Philipp Bartsch
 */
public class VirtualMachineLauncher extends DelegatingComputerLauncher {

    private static final Logger LOGGER = Logger.getLogger(VirtualMachineLauncher.class.getName());

    @Deprecated
    private final ComputerLauncher delegate = null;
    private transient VirtualMachine virtualMachine;
    private final String hypervisorDescription;
    private final String virtualMachineName;
    private final int waitingTimeSecs;
    private final int timesToRetryOnFailure;

    @DataBoundConstructor
    public VirtualMachineLauncher(ComputerLauncher launcher, String hypervisorDescription, String virtualMachineName,
            int waitingTimeSecs, int timesToRetryOnFailure) {
        super(launcher);
        this.virtualMachineName = virtualMachineName;
        this.hypervisorDescription = hypervisorDescription;
        this.waitingTimeSecs = waitingTimeSecs;
        this.timesToRetryOnFailure = timesToRetryOnFailure;
        lookupVirtualMachineHandle();
    }

    /**
     * Private constructor for readResolve().
     */
    private VirtualMachineLauncher(ComputerLauncher launcher, VirtualMachine virtualMachine, String hypervisorDescription,
                                   String virtualMachineName, int waitingTimeSecs, int timesToRetryOnFailure) {
        super(launcher);
        this.virtualMachine = virtualMachine;
        this.hypervisorDescription = hypervisorDescription;
        this.virtualMachineName = virtualMachineName;
        this.waitingTimeSecs = waitingTimeSecs;
        this.timesToRetryOnFailure = timesToRetryOnFailure;
    }

    /**
     * Migrates instances from the old parent class to the new parent class.
     * @return the deserialized instance.
     * @throws ObjectStreamException if something went wrong.
     */
    private Object readResolve() throws ObjectStreamException {
        if (delegate != null) {
            return new VirtualMachineLauncher(delegate, virtualMachine, hypervisorDescription, virtualMachineName,
                    waitingTimeSecs, timesToRetryOnFailure);
        }
        return this;
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

    /**
     * @deprecated use {@link #getLauncher()}
     */
    @Deprecated
    public ComputerLauncher getDelegate() {
        return launcher;
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
        return launcher.isLaunchSupported();
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
                    launcher.launch(slaveComputer, taskListener);
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
    public Descriptor<ComputerLauncher> getDescriptor() {
        // Don't allow creation of launcher from UI
        throw new UnsupportedOperationException();
    }
}
