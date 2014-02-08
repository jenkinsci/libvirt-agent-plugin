package hudson.plugins.libvirt.lib;

/**
 * Created by magnayn on 04/02/2014.
 */
public interface IDomain {
    String getName() throws VirtException;

    String[] snapshotListNames() throws VirtException;

    int snapshotNum() throws VirtException;

    IDomainSnapshot snapshotLookupByName(String snapshotName) throws VirtException;

    int revertToSnapshot(IDomainSnapshot aVoid) throws VirtException;

    void shutdown() throws VirtException;

    boolean isRunningOrBlocked() throws VirtException;

    boolean isNotBlockedAndNotRunning() throws VirtException;

    int create() throws VirtException;
}