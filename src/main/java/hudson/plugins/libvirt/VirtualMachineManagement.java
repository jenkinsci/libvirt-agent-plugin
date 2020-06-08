package hudson.plugins.libvirt;


import com.google.common.collect.Collections2;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.ManagementLink;
import hudson.model.Saveable;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerProxy;

import java.io.IOException;
import java.util.Collection;

import static hudson.plugins.libvirt.util.Consts.PLUGIN_IMAGES_URL;


/**
 * Manage the libvirt hypervisors.
 */
@Extension
public class VirtualMachineManagement extends ManagementLink implements StaplerProxy, Describable<VirtualMachineManagement>, Saveable {

    @Override
    public String getIconFileName() {
        return PLUGIN_IMAGES_URL + "/64x64/libvirt.png";
    }

    @Override
    public String getUrlName() {
        return "libvirt-slave";
    }

    @Override
    public String getDisplayName() {
        return Messages.DisplayName();
    }

    @Override
    public String getDescription() {
        return Messages.PluginDescription();
    }

    public static VirtualMachineManagement get() {
        return ManagementLink.all().get(VirtualMachineManagement.class);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return Jenkins.get().getDescriptorByType(DescriptorImpl.class);
    }

    /**
     * Descriptor is only used for UI form bindings.
     */
    @Extension
    public static final class DescriptorImpl extends Descriptor<VirtualMachineManagement> {

        @Override
        public String getDisplayName() {
            return null; // unused
        }
    }

    public VirtualMachineManagementServer getServer(String serverName) {
        return new VirtualMachineManagementServer(serverName);
    }

    @Override
    public Object getTarget() {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        return this;
    }

    @Override
    public void save() throws IOException {

    }


    public Collection<String> getServerNames() {
        return Collections2.transform(PluginImpl.getInstance().getServers(), Hypervisor::getHypervisorHost);
    }
}
