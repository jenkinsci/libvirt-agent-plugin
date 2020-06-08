/**
 *  Copyright (C) 2010, Byte-Code srl <http://www.byte-code.com>
 *  Copyright (C) 2012  Philipp Bartsch <tastybug@tastybug.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Date: Mar 04, 2010
 * @author Marco Mornati<mmornati@byte-code.com>
 * @author Philipp Bartsch <tastybug@tastybug.com>
 */
package hudson.plugins.libvirt;

import hudson.model.TaskListener;

import hudson.model.Descriptor;
import hudson.plugins.libvirt.lib.IDomain;
import hudson.plugins.libvirt.lib.VirtException;
import hudson.slaves.Cloud;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;

import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;


public class VirtualMachineLauncher extends ComputerLauncher {

    private static final Logger LOGGER = Logger.getLogger(VirtualMachineLauncher.class.getName());
    private static final int MSEC_PER_SEC = 1000;

    private final ComputerLauncher delegate;
    private transient VirtualMachine virtualMachine;
    private final String hypervisorDescription;
    private final String virtualMachineName;
    private final String snapshotName;
    private final int waitTimeMs;
    private final int timesToRetryOnFailure;

    @DataBoundConstructor
    public VirtualMachineLauncher(ComputerLauncher delegate, String hypervisorDescription, String virtualMachineName, String snapshotName,
            int waitingTimeSecs, int timesToRetryOnFailure) {
        super();
        this.delegate = delegate;
        this.virtualMachineName = virtualMachineName;
        this.snapshotName = snapshotName;
        this.hypervisorDescription = hypervisorDescription;
        this.waitTimeMs = waitingTimeSecs * MSEC_PER_SEC;
        this.timesToRetryOnFailure = timesToRetryOnFailure;
        lookupVirtualMachineHandle();
    }

    private void lookupVirtualMachineHandle() {
        if (hypervisorDescription != null && virtualMachineName != null) {
            LOGGER.log(Level.FINE, "Grabbing hypervisor...");
            Hypervisor hypervisor;
            try {
                hypervisor = findOurHypervisorInstance();
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

    public Hypervisor findOurHypervisorInstance() throws VirtException {
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

    @Override
    public void launch(SlaveComputer slaveComputer, TaskListener taskListener) throws IOException, InterruptedException {

        taskListener.getLogger().println("Virtual machine \"" + virtualMachineName + "\" (slave title \"" + slaveComputer.getDisplayName() + "\") is to be started.");
        try {
            if (virtualMachine == null) {
                taskListener.getLogger().println("No connection ready to the Hypervisor, connecting...");
                lookupVirtualMachineHandle();
                if (virtualMachine == null) { // still null? no such vm!
                    throw new Exception("Virtual machine \"" + virtualMachineName + "\" (slave title \"" + slaveComputer.getDisplayName() + "\") not found on the specified hypervisor!");
                }
            }

            Map<String, IDomain> computers = virtualMachine.getHypervisor().getDomains();
            IDomain domain = computers.get(virtualMachine.getName());
            if (domain != null) {
                if (domain.isNotBlockedAndNotRunning()) {
                    taskListener.getLogger().println("Starting, waiting for " + waitTimeMs + "ms to let it fully boot up...");
                    domain.create();
                    Thread.sleep(waitTimeMs);

                    int attempts = 0;
                    while (true) {
                        attempts++;

                        taskListener.getLogger().println("Connecting slave client.");

                        // This call doesn't seem to actually throw anything, but we'll catch IOException just in case
                        try {
                            delegate.launch(slaveComputer, taskListener);
                        } catch (IOException e) {
                            taskListener.getLogger().println("unexpectedly caught exception when delegating launch of slave: " + e.getMessage());
                        }

                        if (slaveComputer.isOnline()) {
                            break;
                        } else if (attempts >= timesToRetryOnFailure) {
                            taskListener.getLogger().println("Maximum retries reached. Failed to start slave client.");
                            break;
                        }

                        taskListener.getLogger().println("Not up yet, waiting for " + waitTimeMs + "ms more ("
                                                         + attempts + "/" + timesToRetryOnFailure + " retries)...");
                        Thread.sleep(waitTimeMs);
                    }
                } else {
                    taskListener.getLogger().println("Already running, no startup required.");

                taskListener.getLogger().println("Connecting slave client.");
                delegate.launch(slaveComputer, taskListener);
                }
            } else {
                throw new IOException("VM \"" + virtualMachine.getName() + "\" (slave title \"" + slaveComputer.getDisplayName() + "\") not found!");
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
}
