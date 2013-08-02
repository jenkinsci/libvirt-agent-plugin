/**
 *  Copyright (C) 2010, Byte-Code srl <http://www.byte-code.com>
 *  Copyright (C) 2012  Philipp Bartsch <tastybug@tastybug.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Date: Mar 04, 2010
 * @author Marco Mornati<mmornati@byte-code.com>
 * @author Philipp Bartsch <tastybug@tastybug.com>
 */
package hudson.plugins.libvirt;

import hudson.Plugin;
import hudson.model.Hudson;
import hudson.slaves.Cloud;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;


import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;


public class PluginImpl extends Plugin {

    private static final java.util.logging.Logger LOGGER = Logger.getLogger(PluginImpl.class.getName());

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

	public FormValidation doCheckStartupWaitingPeriodSeconds (@QueryParameter String secsValue) throws IOException, ServletException {
		try {
			int v = Integer.parseInt(secsValue);
		    if (v < 0) {
		    	return FormValidation.error("Negative value..");
		    } else if (v == 0) {
		    	return FormValidation.warning("You declared this virtual machine to be ready right away. It probably needs a couple of seconds before it is ready to process jobs!");
		    } else {
		    	return FormValidation.ok();
		    }
		} catch (NumberFormatException e) {
		    return FormValidation.error("Not a number..");
		}
	}

    public FormValidation doCheckStartupTimesToRetryOnFailure(@QueryParameter String retriesValue) throws IOException, ServletException {
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

    public void doComputerNameValues(StaplerRequest req, StaplerResponse rsp, @QueryParameter("value") String value) throws IOException, ServletException {
        ListBoxModel m = new ListBoxModel();
        List<VirtualMachine> virtualMachines = null;
        for (Cloud cloud : Hudson.getInstance().clouds) {
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
            m.get(0).selected = true;
        }
        m.writeTo(req, rsp);
    }

    public void doSnapshotNameValues(StaplerRequest req, StaplerResponse rsp, @QueryParameter("vm") String vm, @QueryParameter("hypervisor") String hypervisor) throws IOException, ServletException {
        ListBoxModel m = new ListBoxModel();
        m.add(new ListBoxModel.Option("", ""));
        for (Cloud cloud : Hudson.getInstance().clouds) {
            if (cloud instanceof Hypervisor) {
                Hypervisor hypHandle = (Hypervisor) cloud;
                if (hypervisor != null && hypervisor.equals(hypHandle.getHypervisorURI())) {
                	String[] ss  = hypHandle.getSnapshots(vm);
                	for (String sshot : ss) {
                		m.add(new ListBoxModel.Option(sshot, sshot));
                	}
                }
            }
        }
        
        m.writeTo(req, rsp);
    }
}

