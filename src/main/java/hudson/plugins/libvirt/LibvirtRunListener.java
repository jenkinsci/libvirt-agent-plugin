package hudson.plugins.libvirt;

import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Executor;
import java.util.Map;

import hudson.plugins.libvirt.lib.IDomain;
import hudson.plugins.libvirt.lib.VirtException;
import java.io.IOException;

@Extension
public final class LibvirtRunListener extends RunListener<Run<?, ?>> {
    public LibvirtRunListener() {
    }

    @Override
    public void onStarted(final Run<?, ?> r, final TaskListener listener) {
        super.onStarted(r, listener);
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

                try {
                    System.err.println("NukeSlaveListener about to disconnect. the next error bitching about a slave disconnecting is normal");
                    computer.getChannel().syncLocalIO();
                    computer.getChannel().close();
                    computer.disconnect(null);
                    computer.waitUntilOffline();
                } catch (IOException | InterruptedException e) {
                }

                VirtualMachineLauncher launcher = (VirtualMachineLauncher) slave.getLauncher();
                VirtualMachine virtualMachine = launcher.getVirtualMachine();

                for (int i = 0; i < 5; i++) {
                    try {
                        Map<String, IDomain> computers = virtualMachine.getHypervisor().getDomains();
                        IDomain domain = computers.get(virtualMachine.getName());
                        domain.create();
                    } catch (VirtException e) {
                        try {
                            Thread.sleep(500);
                        } catch (Exception e2) {
                        }
                        continue;
                    }
                    break;
                }
            }
        }
    }
}
