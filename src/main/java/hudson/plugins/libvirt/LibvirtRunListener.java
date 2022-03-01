package hudson.plugins.libvirt;

import hudson.model.Node;
import hudson.model.Run;
import hudson.model.listeners.RunListener;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.ExecutionException;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.slaves.OfflineCause;
import hudson.slaves.SlaveComputer;

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
                try {
                    computer.disconnect(new OfflineCause.ByCLI("Stopping " + slave.getVirtualMachineName()
                                                               + " as a part of onFinalized().")).get();
                    SlaveComputer slaveComputer = slave.getComputer();
                    if (slaveComputer != null) {
                        slaveComputer.tryReconnect();
                    }
                } catch (final InterruptedException e) {
                    LOGGER.log(Level.INFO, "Interrupted while disconnecting from virtual machine {0}.", slave.getVirtualMachineName());
                } catch (final ExecutionException e) {
                    LOGGER.log(Level.WARNING, "Execution exception catched while disconnecting from virtual machine {0}.", slave.getVirtualMachineName());
                }
            }
        }
    }
}
