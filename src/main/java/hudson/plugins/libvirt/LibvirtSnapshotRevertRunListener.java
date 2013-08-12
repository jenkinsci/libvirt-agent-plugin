package hudson.plugins.libvirt;

import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.listeners.RunListener;
import hudson.slaves.ComputerLauncher;

import java.io.IOException;
import java.util.Map;

import org.libvirt.Domain;
import org.libvirt.DomainSnapshot;
import org.libvirt.LibvirtException;

@Extension
public class LibvirtSnapshotRevertRunListener extends RunListener<Run<?, ?>> {

    @Override
    public void onStarted(Run<?, ?> r, TaskListener listener) {
        Node node = r.getExecutor().getOwner().getNode();

        if (node instanceof VirtualMachineSlave) {
            VirtualMachineSlave slave = (VirtualMachineSlave) node;

            String snapshotName = null;

            BeforeJobSnapshotJobProperty prop = r.getParent().getProperty(BeforeJobSnapshotJobProperty.class);
            String jobBeforeJobSnapshotName = null;
            if (prop != null)
                jobBeforeJobSnapshotName = prop.getSnapshotName();

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
                    listener.getLogger().println("Favouring snapshot from previously identified source over " +
                                                 slaveBeforeJobSnapshotName + " from slave/node configuration");
                }
            }

            if (snapshotName != null)
                revertVMSnapshot(slave, snapshotName, listener);
        }
    }

    private static void revertVMSnapshot(VirtualMachineSlave slave, String snapshotName, TaskListener listener) {
        ComputerLauncher launcher = slave.getLauncher();
        if (launcher instanceof ComputerLauncher) {

            VirtualMachineLauncher slaveLauncher = (VirtualMachineLauncher) launcher;
            Hypervisor hypervisor = slaveLauncher.findOurHypervisorInstance();

            try {
                Map<String, Domain> domains = hypervisor.getDomains();

                String vmName = slaveLauncher.getVirtualMachineName();
                Domain domain = domains.get(vmName);
                if (domain != null) {
                    listener.getLogger().println("Preparing to revert " + vmName + " to snapshot " + snapshotName + ".");

                    try {
                        DomainSnapshot snapshot = domain.snapshotLookupByName(snapshotName);
                        try {
                            Computer computer = slave.getComputer();
                            try {
                                computer.getChannel().syncLocalIO();
                                try {
                                    computer.getChannel().close();
                                    computer.disconnect(null);
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
                                        }
                                    } catch (InterruptedException e) {
                                        listener.fatalError("Interrupted while waiting for computer to be offline: " + e);
                                    }
                                } catch (IOException e) {
                                    listener.fatalError("Error closing channel: " + e);
                                }
                            } catch (InterruptedException e) {
                                listener.fatalError("Interrupted while syncing IO: " + e);
                            }
                        } catch (LibvirtException e) {
                            listener.fatalError("No snapshot named " + snapshotName + " for VM: " + e);
                        }
                    } catch (LibvirtException e) {
                        listener.fatalError("No snapshot named " + snapshotName + " for VM: " + e);
                    }
                } else {
                    listener.fatalError("No VM named " + vmName);
                }
            } catch (LibvirtException e) {
                listener.fatalError("Can't get VM domains: " + e);
            }
        }
    }
}
