package hudson.plugins.libvirt.lib;

/**
 * @author Nigel Magnay
 * @version 04/02/2014
 */
public interface IDomain {
    String getName() throws VirtException;

    String[] snapshotListNames() throws VirtException;

    int snapshotNum() throws VirtException;

    IDomainSnapshot snapshotLookupByName(String snapshotName) throws VirtException;

    void revertToSnapshot(IDomainSnapshot aVoid) throws VirtException;

    void shutdown() throws VirtException;

    boolean isRunningOrBlocked() throws VirtException;

    boolean isNotBlockedAndNotRunning() throws VirtException;

    void create() throws VirtException;

    void destroy() throws VirtException;

    void suspend() throws VirtException;
}
