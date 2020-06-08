package hudson.plugins.libvirt;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.plugins.libvirt.lib.IDomain;
import hudson.plugins.libvirt.lib.IDomainSnapshot;
import hudson.plugins.libvirt.lib.VirtException;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.OfflineCause;

import java.io.IOException;
import java.util.Map;


@Extension
public class LibvirtSnapshotRevertRunListener extends RunListener<Run<?, ?>> {

    @Override
    public void onStarted(Run<?, ?> r, TaskListener listener) {
        Executor executor = r.getExecutor();

        if (executor == null) {
            return;
        }

        Node node = executor.getOwner().getNode();

        if (node instanceof VirtualMachineSlave) {
            VirtualMachineSlave slave = (VirtualMachineSlave) node;

            String snapshotName = null;

            BeforeJobSnapshotJobProperty prop = r.getParent().getProperty(BeforeJobSnapshotJobProperty.class);
            String jobBeforeJobSnapshotName = null;
            if (prop != null) {
                jobBeforeJobSnapshotName = prop.getSnapshotName();
            }

            String slaveBeforeJobSnapshotName = slave.getBeforeJobSnapshotName();

            if (jobBeforeJobSnapshotName != null && jobBeforeJobSnapshotName.length() > 0) {
                listener.getLogger().println("Got snapshot " + jobBeforeJobSnapshotName + " from job configuration");
                snapshotName = jobBeforeJobSnapshotName;
            }

            if (slaveBeforeJobSnapshotName != null && slaveBeforeJobSnapshotName.length() > 0) {
                if (snapshotName == null) {
                    listener.getLogger().println("Got snapshot " + slaveBeforeJobSnapshotName + " from slave/node configuration");
                    snapshotName = slaveBeforeJobSnapshotName;
                } else {
                    listener.getLogger().println("Favouring snapshot from previously identified source over "
                                                 + slaveBeforeJobSnapshotName + " from slave/node configuration");
                }
            }

            if (snapshotName != null) {
                revertVMSnapshot(slave, snapshotName, listener);
            }
        }
    }

    private static void revertVMSnapshot(VirtualMachineSlave slave, String snapshotName, TaskListener listener) {
        ComputerLauncher launcher = slave.getLauncher();
        if (launcher instanceof VirtualMachineLauncher) {

            VirtualMachineLauncher slaveLauncher = (VirtualMachineLauncher) launcher;
            String vmName = slaveLauncher.getVirtualMachineName();

            listener.getLogger().println("Preparing to revert " + vmName + " to snapshot " + snapshotName + ".");

            Hypervisor hypervisor = null;
            try {
                hypervisor = slaveLauncher.findOurHypervisorInstance();
            } catch (VirtException e) {
                listener.fatalError("reverting " + vmName + " to " + snapshotName + " failed: " + e.getMessage());
                return;
            }

            try {
                Map<String, IDomain> domains = hypervisor.getDomains();
                IDomain domain = domains.get(vmName);

                if (domain != null) {
                    try {
                        IDomainSnapshot snapshot = domain.snapshotLookupByName(snapshotName);
                        try {
                            Computer computer = slave.getComputer();
                            try {
                                computer.getChannel().syncLocalIO();
                                try {
                                    computer.getChannel().close();
                                    computer.disconnect(new OfflineCause.ByCLI("Stopping " + vmName + " to revert to snapshot " + snapshotName + "."));
                                    try {
                                        computer.waitUntilOffline();

                                        listener.getLogger().println("Reverting " + vmName + " to snapshot " + snapshotName + ".");
                                        domain.revertToSnapshot(snapshot);

                                        listener.getLogger().println("Relaunching " + vmName + ".");
                                        try {
                                            launcher.launch(slave.getComputer(), listener);
                                        } catch (IOException e) {
                                            listener.fatalError("Could not relaunch VM: " + e);
                                        } catch (InterruptedException e) {
                                            listener.fatalError("Could not relaunch VM: " + e);
                                        } catch (NullPointerException e) {
                                            listener.fatalError("Could not determine node.");
                                        }
                                    } catch (InterruptedException e) {
                                        listener.fatalError("Interrupted while waiting for computer to be offline: " + e);
                                    }
                                } catch (IOException e) {
                                    listener.fatalError("Error closing channel: " + e);
                                }
                            } catch (InterruptedException e) {
                                listener.fatalError("Interrupted while syncing IO: " + e);
                            } catch (NullPointerException e) {
                                listener.fatalError("Could not determine channel.");
                            }
                        } catch (VirtException e) {
                            listener.fatalError("No snapshot named " + snapshotName + " for VM: " + e);
                        }
                    } catch (VirtException e) {
                        listener.fatalError("No snapshot named " + snapshotName + " for VM: " + e);
                    }
                } else {
                    listener.fatalError("No VM named " + vmName);
                }
            } catch (VirtException e) {
                listener.fatalError("Can't get VM domains: " + e);
            }
        }
    }
}
