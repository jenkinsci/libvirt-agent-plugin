package hudson.plugins.libvirt;

import hudson.Extension;
import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.slaves.ComputerLauncher;

import java.io.IOException;


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
            VirtualMachine virtualMachine = slaveLauncher.getVirtualMachine();

            listener.getLogger().println("Preparing to revert " + virtualMachine.getName() + " to snapshot " + snapshotName + ".");

            ComputerUtils.disconnect(virtualMachine.getName(), slave.getComputer(), listener);
            ComputerUtils.stop(virtualMachine, slave.getShutdownMethod());
            ComputerUtils.revertToSnapshot(virtualMachine, snapshotName);
            ComputerUtils.start(virtualMachine);

            listener.getLogger().println("Relaunching " + virtualMachine.getName() + ".");
            try {
                launcher.launch(slave.getComputer(), listener);
            } catch (IOException | InterruptedException e) {
                listener.fatalError("Could not relaunch VM: " + e);
            } catch (NullPointerException e) {
                listener.fatalError("Could not determine node.");
            }
        }
    }
}
