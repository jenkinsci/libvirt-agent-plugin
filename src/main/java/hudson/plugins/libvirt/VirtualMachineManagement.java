package hudson.plugins.libvirt;


import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import hudson.Extension;
import hudson.model.*;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerProxy;

import javax.annotation.Nullable;
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


    public DescriptorImpl getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(DescriptorImpl.class);
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


    public Object getTarget() {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        return this;
    }


    public void save() throws IOException {

    }


    public Collection<String> getServerNames() {
        return Collections2.transform(PluginImpl.getInstance().getServers(), new Function<Hypervisor, String>() {
            public String apply(@Nullable Hypervisor input) {
                return input.getHypervisorHost();
            }
        } );
    }


}
