/**
 * Copyright (C) 2010, Byte-Code srl <http://www.byte-code.com>
 * Copyright (C) 2012, Philipp Bartsch <tastybug@tastybug.com>
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package hudson.plugins.libvirt;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;

import hudson.Plugin;
import hudson.slaves.Cloud;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.verb.POST;

/**
 * @author Marco Mornati
 * @author Philipp Bartsch
 */
public class PluginImpl extends Plugin {

    private static final Logger LOGGER = Logger.getLogger(PluginImpl.class.getName());

    private static PluginImpl INSTANCE;

    /**
     * Constructor.
     */
    public PluginImpl() {
        setInstance(this);
    }

    /**
     * Set the singleton instance, used in constructor.
     *
     * @param plugin the singleton
     */
    private static void setInstance(PluginImpl plugin) {
        INSTANCE = plugin;
    }

    /**
     * Returns this singleton instance.
     *
     * @return the singleton.
     */
    public static PluginImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public void start() throws Exception {
        LOGGER.log(Level.FINE, "Starting libvirt-slave plugin");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() throws Exception {
        LOGGER.log(Level.FINE, "Stopping libvirt-slave plugin.");
    }

    public synchronized Collection<Hypervisor> getServers() {
        Jenkins jenkins = Jenkins.get();
        Collection<? extends Cloud> clouds = jenkins.clouds;
        Collection<Hypervisor> libvirtClouds = (Collection<Hypervisor>) Collections2.filter(
                clouds, (@Nullable final Cloud input) -> input instanceof Hypervisor
        );

        return libvirtClouds;
    }

    public Hypervisor getServer(final String host) {
        return Iterables.find(getServers(),
                (@Nullable final Hypervisor input) -> null != input && host.equals(input.getHypervisorHost())
        );
    }

    @POST
    public FormValidation doCheckStartupWaitingPeriodSeconds(@QueryParameter String secsValue)
            throws IOException, ServletException {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        try {
            int v = Integer.parseInt(secsValue);
            if (v < 0) {
                return FormValidation.error("Negative value..");
            } else if (v == 0) {
                return FormValidation.warning("You declared this virtual machine to be ready right away. "
                        + "It probably needs a couple of seconds before it is ready to process jobs!");
            } else {
                return FormValidation.ok();
            }
        } catch (NumberFormatException e) {
            return FormValidation.error("Not a number..");
        }
    }

    @POST
    public FormValidation doCheckStartupTimesToRetryOnFailure(@QueryParameter String retriesValue)
            throws IOException, ServletException {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        try {
            int v = Integer.parseInt(retriesValue);
            if (v < 0) {
                return FormValidation.error("Negative value.");
            } else {
                return FormValidation.ok();
            }
        } catch (NumberFormatException e) {
            return FormValidation.error("Not a number.");
        }
    }

    public void doComputerNameValues(StaplerRequest2 req, StaplerResponse2 rsp,
                                     @QueryParameter("value") String value)
            throws IOException, ServletException {
        ListBoxModel m = new ListBoxModel();
        List<VirtualMachine> virtualMachines = null;

        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            rsp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        for (Cloud cloud : Jenkins.get().clouds) {
            if (cloud instanceof Hypervisor) {
                Hypervisor hypervisor = (Hypervisor) cloud;
                if (value != null && value.equals(hypervisor.getHypervisorDescription())) {
                    virtualMachines = hypervisor.getVirtualMachines();
                    break;
                }
            }
        }
        if (virtualMachines != null) {
            for (VirtualMachine vm : virtualMachines) {
                m.add(new ListBoxModel.Option(vm.getName(), vm.getName()));
            }
        }
        m.writeTo(req, rsp);
    }

    public void doSnapshotNameValues(StaplerRequest2 req, StaplerResponse2 rsp,
                                     @QueryParameter("vm") String vm,
                                     @QueryParameter("hypervisor") String hypervisor)
            throws IOException, ServletException {
        ListBoxModel m = new ListBoxModel();

        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            rsp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        m.add(new ListBoxModel.Option("", ""));
        for (Cloud cloud : Jenkins.get().clouds) {
            if (cloud instanceof Hypervisor) {
                Hypervisor hypHandle = (Hypervisor) cloud;
                if (hypervisor != null && hypervisor.equals(hypHandle.getHypervisorURI())) {
                    String[] ss = hypHandle.getSnapshots(vm);
                    for (String sshot : ss) {
                        m.add(new ListBoxModel.Option(sshot, sshot));
                    }
                }
            }
        }
        m.writeTo(req, rsp);
    }
}
