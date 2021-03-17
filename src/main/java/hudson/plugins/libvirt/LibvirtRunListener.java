package hudson.plugins.libvirt;

import hudson.model.Node;
import hudson.model.Run;
import hudson.model.listeners.RunListener;

import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Executor;

@Extension
public final class LibvirtRunListener extends RunListener<Run<?, ?>> {

    private static final Logger LOGGER = Logger.getLogger(LibvirtRunListener.class.getName());

    public LibvirtRunListener() {
    }

    @Override
    public void onFinalized(final Run<?, ?> r) {
        super.onFinalized(r);
        Executor executor = r.getExecutor();
        if (executor == null) {
            return;
        }
        Computer computer = executor.getOwner();
        Node node = computer.getNode();


        if (node instanceof VirtualMachineSlave) {
            VirtualMachineSlave slave = (VirtualMachineSlave) node;

            if (slave.getRebootAfterRun()) {

                LOGGER.log(Level.INFO, "Virtual machine {0} is to be shut down.", slave.getVirtualMachineName());
                ComputerUtils.disconnect(slave.getVirtualMachineName(), computer);

                VirtualMachineLauncher launcher = (VirtualMachineLauncher) slave.getLauncher();
                VirtualMachine virtualMachine = launcher.getVirtualMachine();

                ComputerUtils.stop(virtualMachine, slave.getShutdownMethod());
                ComputerUtils.revertToSnapshot(virtualMachine, slave.getSnapshotName());
                ComputerUtils.start(virtualMachine);
            }
        }
    }
}
