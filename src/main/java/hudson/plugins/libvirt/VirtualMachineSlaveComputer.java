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

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import hudson.model.Executor;
import hudson.model.Job;
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

    /**
     * On disconnect, stop the virtual machine and revert to the "Revert" snapshot if set.
     *
     * If the cause of the disconnection is "OfflineClause.RevertSnapshot" do not revert
     * to the "Revert" snapshot as we are already disconnecting to revert to another
     * specific snapshot.
     *
     * @param cause Object that identifies the reason the node was disconnected.
     * @return Future to track the asynchronous disconnect operation.
     */
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

        VirtualMachineLauncher vmL = (VirtualMachineLauncher) getLauncher();
        Hypervisor hypervisor;
        try {
            hypervisor = vmL.findOurHypervisorInstance();
        } catch (VirtException e) {
            getListener().getLogger().println("cannot find hypervisor instance on disconnect: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "cannot find hypervisor instance on disconnect: {0}", e.getMessage());
            return super.disconnect(cause);
        }

        LOGGER.log(Level.INFO, "Preparing to shut down VM \""  + slave.getVirtualMachineName() + "\" (agent \"" + getDisplayName() + "\"): " + reason);
        getListener().getLogger().println("Preparing to shut down VM \"" + slave.getVirtualMachineName() + "\" (agent \"" + getDisplayName() + "\"): " + reason);
        try {
            ComputerUtils.stop(vmL.getVirtualMachine(), slave.getShutdownMethod(), getListener());

            // Revert the node to the "Revert" snapshot unless we are disconnecting to revert another snapshot
            if (!(cause instanceof OfflineClause.RevertSnapshot) && !slave.getSnapshotName().isEmpty()) {
                LOGGER.info("Preparing to revert VM \"" + slave.getVirtualMachineName() + "\" to Revert snapshot \"" + slave.getSnapshotName() + "\"");
                getListener().getLogger().println("Preparing to revert VM \"" + slave.getVirtualMachineName() + "\" to Revert snapshot \"" + slave.getSnapshotName() + "\"");
                ComputerUtils.revertToSnapshot(vmL.getVirtualMachine(), slave.getSnapshotName(), getListener());
            }

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

    /**
     * On Task completion, reboot the node if it's configured to do so.
     *
     * @param executor The executor.
     */
    private void afterTaskCompleted(Executor executor, Queue.Task task) {
        VirtualMachineSlave slave = (VirtualMachineSlave) this.getNode();
        if (slave != null && slave.getRebootAfterRun()) {
            VirtualMachineLauncher launcher = (VirtualMachineLauncher) slave.getLauncher();
            VirtualMachine virtualMachine = launcher.getVirtualMachine();

            LOGGER.log(Level.INFO, "Preparing to reboot VM \"" + slave.getVirtualMachineName() + "\" (agent \"" + getDisplayName() + "\") after task \"" + task.getDisplayName() + "\"");
            getListener().getLogger().println("Preparing to reboot VM \"" + slave.getVirtualMachineName() + "\" (agent \"" + getDisplayName() + "\") after task \"" + task.getDisplayName() + "\"");

            // Disconnect will handle the 'Revert' snapshot if it's configured on the node
            ComputerUtils.disconnect(slave.getVirtualMachineName(), executor.getOwner(), new OfflineClause.Reboot("Reboot after run"));
            ComputerUtils.start(virtualMachine);
        }
    }

    @Override
    public void taskCompleted(Executor executor, Queue.Task task, long durationMS) {
        super.taskCompleted(executor, task, durationMS);
        afterTaskCompleted(executor, task);
    }

    @Override
    public void taskCompletedWithProblems(Executor executor, Queue.Task task, long durationMS, Throwable problems) {
        super.taskCompletedWithProblems(executor, task, durationMS, problems);
        afterTaskCompleted(executor, task);
    }

    /**
     * On task start, revert the node to the BeforeJobSnapshot if it's set on the Job or the Node.
     *
     * @param executor The executor.
     * @param task The task.
     */
    @Override
    public synchronized void taskAccepted(Executor executor, Queue.Task task) {
        super.taskAccepted(executor, task);

        VirtualMachineSlave slave = (VirtualMachineSlave) this.getNode();
        if (slave != null) {
            String snapshotName = null;

            Job<?, ?> job;
            if (task.getOwnerTask() instanceof Job) {
                job = (Job<?, ?>) task.getOwnerTask();
            } else {
                LOGGER.warning("Unable to find job for task \"" + task.getName() + "\"");
                return;
            }

            // Get the Job BeforeJobSnapshot if it's set
            BeforeJobSnapshotJobProperty prop = job.getProperty(BeforeJobSnapshotJobProperty.class);
            String jobBeforeJobSnapshotName;
            if (prop != null) {
                jobBeforeJobSnapshotName = prop.getSnapshotName();

                if (jobBeforeJobSnapshotName != null && !jobBeforeJobSnapshotName.isEmpty()) {
                    LOGGER.info("Got Before Job snapshot \"" + jobBeforeJobSnapshotName + "\" from job \"" + job.getName() + "\"");
                    getListener().getLogger().println("Got Before Job snapshot \"" + jobBeforeJobSnapshotName + "\" from job \"" + job.getName() + "\"");
                    snapshotName = jobBeforeJobSnapshotName;
                }
            }

            // If we didn't get a Job level BeforeJobSnapshot, get it from the Node if it's set
            if (snapshotName == null) {
                String slaveBeforeJobSnapshotName = slave.getBeforeJobSnapshotName();
                if (slaveBeforeJobSnapshotName != null && !slaveBeforeJobSnapshotName.isEmpty()) {
                    LOGGER.info("Got Before Job snapshot \"" + slaveBeforeJobSnapshotName + "\" from node \"" + getDisplayName() + "\"");
                    getListener().getLogger().println("Got Before Job snapshot \"" + slaveBeforeJobSnapshotName + "\" from node \"" + getDisplayName() + "\"");
                    snapshotName = slaveBeforeJobSnapshotName;
                }
            }

            // If we got a BeforeJobSnapshot from the Job or the Node, revert the Virtual Machine to it
            if (snapshotName != null) {
                VirtualMachineLauncher launcher = (VirtualMachineLauncher) slave.getLauncher();
                VirtualMachine virtualMachine = launcher.getVirtualMachine();

                LOGGER.info("Will revert VM \"" + virtualMachine.getName() + "\" to Before Job snapshot \"" + snapshotName + "\" for task \"" + task.getDisplayName() + "\"");
                getListener().getLogger().println("Will revert VM \"" + virtualMachine.getName() + "\" to Before Job snapshot \"" + snapshotName + "\" for task \"" + task.getDisplayName() + "\"");

                SlaveComputer slaveComputer = slave.getComputer();
                if (slaveComputer == null) {
                    getListener().fatalError("Could not determine node.");
                    return;
                }

                ComputerUtils.disconnect(virtualMachine.getName(), slaveComputer, getListener(), new OfflineClause.RevertSnapshot("Reverting to snapshot \"" + snapshotName + "\""));
                ComputerUtils.revertToSnapshot(virtualMachine, snapshotName);
                ComputerUtils.start(virtualMachine);

                LOGGER.info("Relaunching agent \"" + getDisplayName() + "\"");
                getListener().getLogger().println("Relaunching agent \"" + getDisplayName() + "\"");
                try {
                    this.getLauncher().launch(slaveComputer, getListener());
                } catch (IOException | InterruptedException e) {
                    getListener().fatalError("Could not relaunch VM: " + e);
                }
            }
        }
    }
}
