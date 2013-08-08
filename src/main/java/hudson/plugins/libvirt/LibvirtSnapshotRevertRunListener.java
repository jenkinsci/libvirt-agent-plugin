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

// Supertype doesn't declare generic types, can't do anything to fix that and still be able to override.
@SuppressWarnings("rawtypes")
@Extension
public class LibvirtSnapshotRevertRunListener extends RunListener<Run> {

    @Override
    public void onStarted(Run r, TaskListener listener) {
        Node node = r.getExecutor().getOwner().getNode();

        if (node instanceof VirtualMachineSlave) {
            VirtualMachineSlave slave = (VirtualMachineSlave) node;

            String beforeJobSnapshotName = slave.getBeforeJobSnapshotName();
            if (beforeJobSnapshotName != null && beforeJobSnapshotName.length() > 0) {

                ComputerLauncher launcher = slave.getLauncher();
                if (launcher instanceof ComputerLauncher) {

                    VirtualMachineLauncher slaveLauncher = (VirtualMachineLauncher) launcher;
                    Hypervisor hypervisor = slaveLauncher.findOurHypervisorInstance();

                    try {
                        Map<String, Domain> domains = hypervisor.getDomains();

                        String vmName = slaveLauncher.getVirtualMachineName();
                        Domain domain = domains.get(vmName);
                        if (domain != null) {
                            listener.getLogger().println("Preparing to revert " + vmName + " to snapshot " + beforeJobSnapshotName + ".");

                            try {
                                DomainSnapshot snapshot = domain.snapshotLookupByName(beforeJobSnapshotName);
                                try {
                                    Computer computer = slave.getComputer();
                                    try {
                                        computer.getChannel().syncLocalIO();
                                        try {
                                            computer.getChannel().close();
                                            computer.disconnect(null);
                                            try {
                                                computer.waitUntilOffline();

                                                listener.getLogger().println("Reverting " + vmName + " to snapshot " + beforeJobSnapshotName + ".");
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
                                    listener.fatalError("No snapshot named " + beforeJobSnapshotName + " for VM: " + e);
                                }
                            } catch (LibvirtException e) {
                                listener.fatalError("No snapshot named " + beforeJobSnapshotName + " for VM: " + e);
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
    }
}
