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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.Extension;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.plugins.libvirt.lib.VirtException;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Slave;
import hudson.slaves.Cloud;
import hudson.slaves.ComputerListener;
import hudson.slaves.NodeProperty;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.SlaveComputer;
import hudson.util.ListBoxModel;

import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Marco Mornati
 * @author Philipp Bartsch
 */
public class VirtualMachineSlave extends Slave {

    static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(VirtualMachineSlave.class.getName());

    private final String      hypervisorDescription;
    private final String      snapshotName;
    private final String      virtualMachineName;
    private final int         startupWaitingPeriodSeconds;
    private final String      shutdownMethod;
    private final boolean     rebootAfterRun;
    private final int         startupTimesToRetryOnFailure;
    private final String      beforeJobSnapshotName;

    @DataBoundConstructor
    public VirtualMachineSlave(String name, String nodeDescription, String remoteFS,
                               String numExecutors, Mode mode, String labelString,
                               VirtualMachineLauncher launcher,
                               ComputerLauncher delegateLauncher,
                               RetentionStrategy<? extends Computer> retentionStrategy,
                               List<? extends NodeProperty<?>> nodeProperties,
                               String hypervisorDescription, String virtualMachineName,
                               String snapshotName, int startupWaitingPeriodSeconds,
                               String shutdownMethod, boolean rebootAfterRun,
                               int startupTimesToRetryOnFailure, String beforeJobSnapshotName)
            throws
            Descriptor.FormException, IOException {
        super(name, remoteFS,
              launcher == null ? new VirtualMachineLauncher(delegateLauncher,
                                                            hypervisorDescription,
                                                            virtualMachineName,
                                                            startupWaitingPeriodSeconds,
                                                            startupTimesToRetryOnFailure) : launcher);

        Number executors = Util.tryParseNumber(numExecutors, 1);
        if (executors == null) {
            this.setNumExecutors(1);
        } else {
            this.setNumExecutors(executors.intValue());
        }
        this.setNodeDescription(nodeDescription);
        this.setMode(mode);
        this.setLabelString(labelString);
        this.setRetentionStrategy(retentionStrategy);
        this.setNodeProperties(nodeProperties);
        this.hypervisorDescription = hypervisorDescription;
        this.virtualMachineName = virtualMachineName;
        this.snapshotName = snapshotName;
        this.startupWaitingPeriodSeconds = startupWaitingPeriodSeconds;
        this.shutdownMethod = shutdownMethod;
        this.rebootAfterRun = rebootAfterRun;
        this.startupTimesToRetryOnFailure = startupTimesToRetryOnFailure;
        this.beforeJobSnapshotName = beforeJobSnapshotName;
    }

    public String getHypervisorDescription() {
        return hypervisorDescription;
    }

    public String getVirtualMachineName() {
        return virtualMachineName;
    }

    public String getSnapshotName() {
        return snapshotName;
    }

    public int getStartupWaitingPeriodSeconds() {
        return startupWaitingPeriodSeconds;
    }

    public String getShutdownMethod() {
        return shutdownMethod;
    }

    public boolean getRebootAfterRun() {
        return rebootAfterRun;
    }

    public int getStartupTimesToRetryOnFailure() {
        return startupTimesToRetryOnFailure;
    }

    public String getBeforeJobSnapshotName() {
        return beforeJobSnapshotName;
    }

    /**
     * For UI.
     *
     * @return original launcher
     */
    public ComputerLauncher getDelegateLauncher() {
        return ((VirtualMachineLauncher) getLauncher()).getLauncher();
    }

    @Override
    public Computer createComputer() {
        return new VirtualMachineSlaveComputer(this);
    }

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @Extension
    public static class VirtualMachineComputerListener extends ComputerListener {

        @Override
        public void preLaunch(Computer c, TaskListener taskListener)
                throws IOException, InterruptedException {
            /* We may be called on any agent type so check that we should
             * be in here. */
            if (!(c.getNode() instanceof VirtualMachineSlave)) {
                return;
            }

            VirtualMachineLauncher vmL = (VirtualMachineLauncher) ((SlaveComputer) c).getLauncher();
            try {
                Hypervisor vmC = vmL.findOurHypervisorInstance();
                if (!vmC.markVMOnline(c.getDisplayName(), vmL.getVirtualMachineName())) {
                    throw new AbortException("Capacity threshold  (" + vmC.getMaxOnlineSlaves()
                                             + ") reached at hypervisor \""
                                             + vmC.getHypervisorDescription()
                                             + "\", agent commissioning delayed.");
                }
            } catch (VirtException e) {
                LOGGER.log(Level.WARNING, "aborting agent launch due to:", e);
                throw new AbortException(e.getMessage());
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {

        private static final String DESCRIPTION = " A hypervisor description";
        private static final String VMNAME = "A VM name";
        private static final String SNAPSHOT = "A snapshot";

        public DescriptorImpl() {
            load();
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Agent virtual computer running on a virtualization platform (via libvirt)";
        }

        @Override
        public boolean isInstantiable() {
            return true;
        }

        public List<VirtualMachine> getDefinedVirtualMachines(String description) {
            List<VirtualMachine> virtualMachinesList = new ArrayList<>();
            Hypervisor hypervisor = getHypervisorByDescription(description);
            if (hypervisor != null) {
                virtualMachinesList.addAll(hypervisor.getVirtualMachines());
            }
            Collections.sort(virtualMachinesList);
            return virtualMachinesList;
        }

        public String[] getDefinedSnapshots(String description, String vmName) {
            Hypervisor hypervisor = getHypervisorByDescription(description);
            if (hypervisor != null) {
                return hypervisor.getSnapshots(vmName);
            }
            return new String[0];
        }

        public ListBoxModel doFillHypervisorDescriptionItems() {
            ListBoxModel items = new ListBoxModel();
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            for (Cloud cloud : Jenkins.get().clouds) {
                if (cloud instanceof Hypervisor) {
                    items.add(((Hypervisor) cloud).getHypervisorURI(),
                              ((Hypervisor) cloud).getHypervisorDescription());
                }
            }
            return items;
        }

        public String getHypervisorDescription() {
            return DESCRIPTION;
        }

        public String getVirtualMachineName() {
            return VMNAME;
        }

        public String getSnapshotName() {
            return SNAPSHOT;
        }

        private Hypervisor getHypervisorByDescription(String description) {
            if (description != null && !description.isEmpty()) {
                for (Cloud cloud : Jenkins.get().clouds) {
                    if (cloud instanceof Hypervisor
                        && ((Hypervisor) cloud).getHypervisorDescription().equals(description)) {
                        return (Hypervisor) cloud;
                    }
                }
            }
            return null;
        }
    }
}
