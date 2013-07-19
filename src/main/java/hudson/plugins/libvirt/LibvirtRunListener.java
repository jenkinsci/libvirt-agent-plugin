package hudson.plugins.libvirt;

import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.Executor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

@Extension
public final class LibvirtRunListener extends RunListener<Run> {
    public LibvirtRunListener () {
    }

    @Override
    public void onStarted(Run r, TaskListener listener) {
        super.onStarted(r, listener);
    }

    @Override
    public void onFinalized(Run r) {
        super.onFinalized(r);
        Computer computer = r.getExecutor().getOwner();
        Node node = computer.getNode();


        if (node instanceof VirtualMachineSlave) {
            VirtualMachineSlave slave = (VirtualMachineSlave)node;

            if (slave.getRebootAfterRun()) {

                try {
                    System.err.println("NukeSlaveListener about to disconnect. the next error bitching about a slave disconnecting is normal");
                    computer.getChannel().syncLocalIO();
                    computer.getChannel().close();
                    computer.disconnect(null);
                    computer.waitUntilOffline();
                } catch (Exception e) {
                }

                VirtualMachineLauncher launcher = (VirtualMachineLauncher)slave.getLauncher();
                VirtualMachine virtualMachine = launcher.getVirtualMachine();

                for (int i = 0; i < 5; i++) {
                    try {
                        Map<String, Domain> computers = virtualMachine.getHypervisor().getDomains();
                        Domain domain = computers.get(virtualMachine.getName());
                        domain.create();
                    } catch(LibvirtException e) {
                        try {Thread.sleep(500); } catch (Exception e2) {}
                        continue;
                    }
                    break;
                }

            }
        }
    }
}


