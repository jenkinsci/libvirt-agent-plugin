package hudson.plugins.libvirt.lib.libvirt;

import hudson.plugins.libvirt.lib.IDomain;
import hudson.plugins.libvirt.lib.IDomainSnapshot;
import hudson.plugins.libvirt.lib.VirtException;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.DomainSnapshot;
import org.libvirt.LibvirtException;

/**
 * Created by magnayn on 04/02/2014.
 */
public class LibVirtDomainImpl implements IDomain {
    private final Domain domain;
    public LibVirtDomainImpl(Domain domain) {
        this.domain = domain;
    }

    public String getName() throws VirtException {
        try {
            return domain.getName();
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    public String[] snapshotListNames() throws VirtException {
        try {
            return domain.snapshotListNames();
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    public int snapshotNum() throws VirtException {
        try {
            return domain.snapshotNum();
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    public IDomainSnapshot snapshotLookupByName(String snapshotName) throws VirtException {
        try {
            return new LibVirtDomainSnapshotImpl(domain.snapshotLookupByName(snapshotName));
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    public void revertToSnapshot(IDomainSnapshot ds) throws VirtException {
        try {
            DomainSnapshot snapshot = ((LibVirtDomainSnapshotImpl) ds).getSnapshot();
            domain.revertToSnapshot(snapshot);
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    public void shutdown() throws VirtException {
        try {
            domain.shutdown();
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    public boolean isRunningOrBlocked() throws VirtException {
        try {
            return (domain.getInfo().state.equals(DomainInfo.DomainState.VIR_DOMAIN_RUNNING) || domain.getInfo().state.equals(DomainInfo.DomainState.VIR_DOMAIN_BLOCKED));
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    public boolean isNotBlockedAndNotRunning() throws VirtException {
        try {
            return (domain.getInfo().state != DomainInfo.DomainState.VIR_DOMAIN_BLOCKED && domain.getInfo().state != DomainInfo.DomainState.VIR_DOMAIN_RUNNING);
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    public void create() throws VirtException {
        try {
            domain.create();
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    public void destroy() throws VirtException {
        try {
            domain.destroy();
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    public void suspend() throws VirtException {
        try {
            domain.suspend();
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }
}
