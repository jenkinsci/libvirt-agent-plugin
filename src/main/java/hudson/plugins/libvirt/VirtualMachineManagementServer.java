package hudson.plugins.libvirt;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.plugins.libvirt.lib.IDomain;
import hudson.plugins.libvirt.lib.VirtException;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.verb.POST;

import jakarta.servlet.ServletException;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Nigel Magnay
 * @version 22/02/2014
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
    public void doControlSubmit(@QueryParameter("stopId") String stopId, StaplerRequest2 req, StaplerResponse2 rsp) throws ServletException,
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

        @NonNull
        @Override
        public String getDisplayName() {
            return "server ";
        }


    }
}
