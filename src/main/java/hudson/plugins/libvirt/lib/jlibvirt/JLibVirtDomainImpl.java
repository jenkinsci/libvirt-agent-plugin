package hudson.plugins.libvirt.lib.jlibvirt;

import com.nirima.libvirt.Domain;
import com.nirima.libvirt.DomainSnapshot;
import hudson.plugins.libvirt.lib.IDomain;
import hudson.plugins.libvirt.lib.IDomainSnapshot;
import hudson.plugins.libvirt.lib.VirtException;


/**
 * Created by magnayn on 04/02/2014.
 */
public class JLibVirtDomainImpl implements IDomain {
    private final Domain domain;
    public JLibVirtDomainImpl(Domain domain) {
        this.domain = domain;
    }

    public String getName() throws VirtException {
        try {
            return domain.getName();
        } catch (Exception e) {
            throw new VirtException(e);
        }
    }

    public String[] snapshotListNames() throws VirtException {
        try {
            return domain.snapshotListNames();
        } catch (Exception e) {
            throw new VirtException(e);
        }
    }

    public int snapshotNum() throws VirtException {
        try {
            return domain.snapshotNum();
        } catch (Exception e) {
            throw new VirtException(e);
        }
    }

    public IDomainSnapshot snapshotLookupByName(String snapshotName) throws VirtException {
        try {
            return new JLibVirtDomainSnapshotImpl(domain.snapshotLookupByName(snapshotName));
        } catch (Exception e) {
            throw new VirtException(e);
        }
    }

    public void revertToSnapshot(IDomainSnapshot ds) throws VirtException {
        try {
            DomainSnapshot snapshot = ((JLibVirtDomainSnapshotImpl) ds).getSnapshot();
            domain.revertToSnapshot(snapshot);
        } catch (Exception e) {
            throw new VirtException(e);
        }
    }

    public void shutdown() throws VirtException {
        try {
            domain.shutdown();
        } catch (Exception e) {
            throw new VirtException(e);
        }
    }

    public boolean isRunningOrBlocked() throws VirtException {
        try {
            Domain.DomainState domainState = domain.getState();
            return (domainState.equals(Domain.DomainState.RUNNING) || domainState.equals(Domain.DomainState.BLOCKED));
        } catch (Exception e) {
            throw new VirtException(e);
        }
    }

    public boolean isNotBlockedAndNotRunning() throws VirtException {
        try {
            Domain.DomainState domainState = domain.getState();
            return (domainState != Domain.DomainState.BLOCKED && domainState != Domain.DomainState.RUNNING);
        } catch (Exception e) {
            throw new VirtException(e);
        }
    }

    public void create() throws VirtException {
        try {
            domain.create();
        } catch (Exception e) {
            throw new VirtException(e);
        }
    }

    public void destroy() throws VirtException {
        try {
            domain.destroy();
        } catch (Exception e) {
            throw new VirtException(e);
        }
    }

    public void suspend() throws VirtException {
        try {
            domain.suspend();
        } catch (Exception e) {
            throw new VirtException(e);
        }
    }
}
