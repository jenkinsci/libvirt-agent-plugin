/**
 *  Copyright (C) 2010, Byte-Code srl <http://www.byte-code.com>
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
 */

package hudson.plugins.libvirt;

import hudson.model.TaskListener;
import hudson.model.Slave;
import hudson.slaves.OfflineCause;
import hudson.slaves.SlaveComputer;
import hudson.util.StreamTaskListener;
import hudson.util.io.ReopenableRotatingFileOutputStream;

import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.libvirt.Domain;
import org.libvirt.DomainInfo.DomainState;

public class VirtualMachineSlaveComputer extends SlaveComputer {

	private static final Logger logger = Logger.getLogger(VirtualMachineSlaveComputer.class.getName());
			
	private final TaskListener taskListener;
	
    public VirtualMachineSlaveComputer(Slave slave) {
        super(slave);    
        this.taskListener = new StreamTaskListener(new ReopenableRotatingFileOutputStream(getLogFile(),10));
    }

	@Override
	public Future<?> disconnect(OfflineCause cause) {
		VirtualMachineSlave slave = (VirtualMachineSlave) getNode();
		String virtualMachineName = slave.getVirtualMachineName();
		VirtualMachineLauncher vmL = (VirtualMachineLauncher) getLauncher();
		Hypervisor hypervisor = vmL.findOurHypervisorInstance();
		logger.log(Level.INFO, "Virtual machine \"" + virtualMachineName + "\" (slave \"" + getDisplayName() + "\") is to be shut down. reason: "+cause+" ("+cause.getClass().getName()+")");
		taskListener.getLogger().println("Virtual machine \"" + virtualMachineName + "\" (slave \"" + getDisplayName() + "\") is to be shut down.");
		try {			
            Map<String, Domain> computers = hypervisor.getDomains();
            Domain domain = computers.get(virtualMachineName);
            if (domain != null) {
            	if (domain.getInfo().state.equals(DomainState.VIR_DOMAIN_RUNNING) || domain.getInfo().state.equals(DomainState.VIR_DOMAIN_BLOCKED)) {
            		String snapshotName = slave.getSnapshotName();
                    if (snapshotName != null && snapshotName.length() > 0) {
                    	taskListener.getLogger().println("Reverting to " + snapshotName + " and shutting down.");
                    	domain.revertToSnapshot(domain.snapshotLookupByName(snapshotName));
                    } else {
                    	taskListener.getLogger().println("Shutting down.");
                    	domain.shutdown();
                    }
                } else {
                    taskListener.getLogger().println("Already suspended, no shutdown required.");
                }
                Hypervisor vmC = vmL.findOurHypervisorInstance();
                vmC.markVMOffline(getDisplayName(), vmL.getVirtualMachineName());
            } else {
            	// log to slave 
            	taskListener.getLogger().println("\"" + virtualMachineName + "\" not found on Hypervisor, can not shut down!");
            	
            	// log to jenkins
            	LogRecord rec = new LogRecord(Level.WARNING, "Can not shut down {0} on Hypervisor {1}, domain not found!");
                rec.setParameters(new Object[]{virtualMachineName, hypervisor.getHypervisorURI()});
                logger.log(rec);
            }
        } catch (Throwable t) {
        	taskListener.fatalError(t.getMessage(), t);
        	
            LogRecord rec = new LogRecord(Level.SEVERE, "Error while shutting down {0} on Hypervisor {1}.");
            rec.setParameters(new Object[]{slave.getVirtualMachineName(), hypervisor.getHypervisorURI()});
            rec.setThrown(t);
            logger.log(rec);
        }
		return super.disconnect(cause);
	}
    	
}
