/**
 * Copyright (C) 2010, Byte-Code srl <http://www.byte-code.com>
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

import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.concurrent.ExecutionException;

import hudson.model.Executor;
import hudson.model.Queue;
import hudson.model.Slave;
import hudson.plugins.libvirt.lib.VirtException;
import hudson.slaves.OfflineCause;
import hudson.slaves.SlaveComputer;

/**
 * @author Marco Mornati
 */
public class VirtualMachineSlaveComputer extends SlaveComputer {

    private static final Logger LOGGER = Logger.getLogger(VirtualMachineSlaveComputer.class.getName());

    public VirtualMachineSlaveComputer(Slave slave) {
        super(slave);
    }

    @Override
    public Future<?> disconnect(OfflineCause cause) {
        String reason = "unknown";
        if (cause != null) {
            reason =  "reason: " + cause + " (" + cause.getClass().getName() + ")";
        }

        VirtualMachineSlave slave = (VirtualMachineSlave) getNode();
        if (null == slave) {
            getListener().getLogger().println("disconnect from null agent reason: " + reason);
            LOGGER.log(Level.SEVERE, "disconnect from null agent reason: {0}", reason);
            return super.disconnect(cause);
        }
        String virtualMachineName = slave.getVirtualMachineName();
        VirtualMachineLauncher vmL = (VirtualMachineLauncher) getLauncher();
        Hypervisor hypervisor;
        try {
            hypervisor = vmL.findOurHypervisorInstance();
        } catch (VirtException e) {
            getListener().getLogger().println("cannot find hypervisor instance on disconnect: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "cannot find hypervisor instance on disconnect: {0}", e.getMessage());
            return super.disconnect(cause);
        }

        LOGGER.log(Level.INFO, "Virtual machine \""  + virtualMachineName + "\" (agent \"" + getDisplayName() + "\") is to be shut down." + reason);
        getListener().getLogger().println("Virtual machine \"" + virtualMachineName + "\" (agent \"" + getDisplayName() + "\") is to be shut down.");

        try {
            ComputerUtils.disconnect(virtualMachineName, this, getListener());
            ComputerUtils.stop(vmL.getVirtualMachine(), slave.getShutdownMethod(), getListener());
            ComputerUtils.revertToSnapshot(vmL.getVirtualMachine(), slave.getSnapshotName(), getListener());
            hypervisor.markVMOffline(getDisplayName(), vmL.getVirtualMachineName());
        } catch (VirtException t) {
            getListener().fatalError(t.getMessage(), t);

            LogRecord rec = new LogRecord(Level.SEVERE, "Error while shutting down {0} on Hypervisor {1}.");
            rec.setParameters(new Object[]{slave.getVirtualMachineName(), hypervisor.getHypervisorURI()});
            rec.setThrown(t);
            LOGGER.log(rec);
        }
        return super.disconnect(cause);
    }

    private void afterTaskCompleted(Executor executor) {
        VirtualMachineSlave slave = (VirtualMachineSlave) this.getNode();
        if (slave != null && slave.getRebootAfterRun()) {
            LOGGER.log(Level.INFO, "Virtual machine \""  + slave.getVirtualMachineName() + "\" (agent \"" + getDisplayName() + "\") is to be shut down.");
            getListener().getLogger().println("Virtual machine \"" + slave.getVirtualMachineName() + "\" (agent \"" + getDisplayName() + "\") is to be shut down.");
            try {
                disconnect(new OfflineCause.ByCLI("Stopping " + slave.getVirtualMachineName() + " as a part of afterTaskCompleted().")).get();
                tryReconnect();
            } catch (final InterruptedException e) {
                LOGGER.log(Level.INFO, "Interrupted while disconnecting from virtual machine {0}.", slave.getVirtualMachineName());
            } catch (final ExecutionException e) {
                LOGGER.log(Level.WARNING, "Execution exception catched while disconnecting from virtual machine {0}. ", slave.getVirtualMachineName());
            }
        }
    }

    @Override
    public void taskCompleted(Executor executor, Queue.Task task, long durationMS) {
        super.taskCompleted(executor, task, durationMS);
        afterTaskCompleted(executor);
    }

    @Override
    public void taskCompletedWithProblems(Executor executor, Queue.Task task, long durationMS, Throwable problems) {
        super.taskCompletedWithProblems(executor, task, durationMS, problems);
        afterTaskCompleted(executor);
    }
}
