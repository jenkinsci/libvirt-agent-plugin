package hudson.plugins.libvirt;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;

import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.plugins.libvirt.lib.IDomain;
import hudson.plugins.libvirt.lib.IDomainSnapshot;
import hudson.plugins.libvirt.lib.VirtException;
import hudson.slaves.OfflineCause;

public final class ComputerUtils {

    private static final Logger LOGGER = Logger.getLogger(ComputerUtils.class.getName());

    private static final int RETRY_MAX = 5;

    private static final int RETRY_WAIT_MS = 500;

    private ComputerUtils() {
    }

    public static void disconnect(final String name, final Computer computer) {
        disconnect(name, computer, null);
    }

    public static void disconnect(final String name, final Computer computer,
            @CheckForNull final TaskListener listener) {
        try {
            computer.getChannel().syncLocalIO();
            try {
                computer.getChannel().close();
                computer.disconnect(new OfflineCause.ByCLI("Stopping " + name + "."));
                try {
                    computer.waitUntilOffline();
                } catch (final InterruptedException e) {
                    error(listener, "Interrupted while waiting for computer to be offline: " + e);
                }
            } catch (final IOException e) {
                error(listener, "Error closing channel: " + e);
            }
        } catch (final InterruptedException e) {
            error(listener, "Interrupted while syncing IO: " + e);
        } catch (final NullPointerException e) {
            error(listener, "Could not determine channel.");
        }
    }

    public static void start(final VirtualMachine virtualMachine) {
        start(virtualMachine, null);
    }

    public static void start(final VirtualMachine virtualMachine, @CheckForNull final TaskListener listener) {
        final IDomain domain = getDomain(virtualMachine, listener);
        if (domain != null) {
            try {
                if (domain.isRunningOrBlocked()) {
                    log(listener, "Machine " + virtualMachine.getName()
                            + " is already running, no startup required.");
                    return;
                }
            } catch (final VirtException e) {
                error(listener, "Error checking if " + virtualMachine.getName()
                        + " is already running, will consider it as stopped.");
            }

            for (int i = 0; i < RETRY_MAX; i++) {
                try {
                    log(listener, "Starting " + virtualMachine.getName() + "...");
                    domain.create();
                } catch (final VirtException e) {
                    try {
                        Thread.sleep(RETRY_WAIT_MS);
                    } catch (final Exception e2) {
                    }
                    continue;
                }
                break;
            }
        }
    }

    public static void stop(final VirtualMachine virtualMachine, final String shutdownMethod) {
        stop(virtualMachine, shutdownMethod, null);
    }

    public static void stop(final VirtualMachine virtualMachine, final String shutdownMethod,
            @CheckForNull final TaskListener listener) {
        final IDomain domain = getDomain(virtualMachine, listener);
        if (domain != null) {
            try {
                if (domain.isNotBlockedAndNotRunning()) {
                    log(listener,
                            "Machine " + virtualMachine.getName() + " is not running, no shutdown required.");
                    return;
                }
            } catch (final VirtException e) {
                error(listener, "Error checking if " + virtualMachine.getName()
                        + " is stopped, will consider it as running.");
            }

            for (int i = 0; i < RETRY_MAX; i++) {
                try {
                    log(listener, "Stopping " + virtualMachine.getName() + " (using method " + shutdownMethod
                            + ")...");
                    if ("suspend".equals(shutdownMethod)) {
                        domain.suspend();
                    } else if ("destroy".equals(shutdownMethod)) {
                        domain.destroy();
                    } else {
                        domain.shutdown();
                    }
                } catch (final VirtException e) {
                    try {
                        Thread.sleep(RETRY_WAIT_MS);
                    } catch (final Exception e2) {
                    }
                    continue;
                }
                break;
            }
        }
    }

    public static void revertToSnapshot(final VirtualMachine virtualMachine, final String snapshotName) {
        revertToSnapshot(virtualMachine, snapshotName, null);
    }

    public static void revertToSnapshot(final VirtualMachine virtualMachine, final String snapshotName,
            @CheckForNull final TaskListener listener) {
        if (snapshotName != null && snapshotName.length() > 0) {
            final IDomain domain = getDomain(virtualMachine, listener);
            if (domain != null) {
                try {
                    final IDomainSnapshot snapshot = domain.snapshotLookupByName(snapshotName);
                    try {
                        log(listener, "Reverting " + virtualMachine.getName() + " to snapshot " + snapshotName
                                + ".");
                        domain.revertToSnapshot(snapshot);
                    } catch (final VirtException e) {
                        error(listener, "Error reverting to snapshot named " + snapshotName + " for VM "
                                + virtualMachine.getName() + ": " + e);
                    }
                } catch (final VirtException e) {
                    error(listener, "No snapshot named " + snapshotName + " for VM "
                            + virtualMachine.getName() + ": " + e);
                }

            }
        }
    }

    private static @CheckForNull IDomain getDomain(final VirtualMachine virtualMachine,
            @CheckForNull final TaskListener listener) {
        IDomain domain = null;
        try {
            final Map<String, IDomain> domains = virtualMachine.getHypervisor().getDomains();
            domain = domains.get(virtualMachine.getName());
            if (domain == null) {
                error(listener, "No VM named " + virtualMachine.getName());
            }
        } catch (final VirtException e) {
            error(listener, "Can't get VM domains: " + e);
        }
        return domain;
    }

    private static void log(@CheckForNull final TaskListener listener, final String message) {
        if (listener != null) {
            listener.getLogger().println(message);
        }
        LOGGER.log(Level.INFO, message);
    }

    private static void error(@CheckForNull final TaskListener listener, final String message) {
        if (listener != null) {
            listener.fatalError(message);
        }
        LOGGER.log(Level.WARNING, message);
    }

}
