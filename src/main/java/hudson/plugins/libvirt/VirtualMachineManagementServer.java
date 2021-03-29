package hudson.plugins.libvirt;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.plugins.libvirt.lib.IDomain;
import hudson.plugins.libvirt.lib.VirtException;
import hudson.plugins.libvirt.util.Consts;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.verb.POST;

import javax.servlet.ServletException;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by magnayn on 22/02/2014.
 */
public class VirtualMachineManagementServer implements Describable<VirtualMachineManagementServer> {
    private final String host;
    private final Hypervisor theCloud;

    public Descriptor<VirtualMachineManagementServer> getDescriptor() {
        return Jenkins.get().getDescriptorByType(DescriptorImpl.class);
    }

    public String getUrl() {
        return VirtualMachineManagement.get().getUrlName() + "/server/" + host;
    }

    public VirtualMachineManagementServer(String host) {
        this.host = host;
        this.theCloud = PluginImpl.getInstance().getServer(host);
    }

    public Collection<IDomain> getDomains() throws VirtException {
        return this.theCloud.getDomains().values();
    }


    public String asTime(Long time) {
        if (time == null) {
            return "";
        }

        long when = System.currentTimeMillis() - time;

        Date dt = new Date(when);
        return dt.toString();
    }

    @POST
    public void doControlSubmit(@QueryParameter("stopId") String stopId, StaplerRequest req, StaplerResponse rsp) throws ServletException,
            IOException,
            InterruptedException, VirtException {

        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            rsp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        theCloud.getDomains().get(stopId).shutdown();

        rsp.sendRedirect(".");
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<VirtualMachineManagementServer> {

        @Override
        public String getDisplayName() {
            return "server ";
        }


    }
}
