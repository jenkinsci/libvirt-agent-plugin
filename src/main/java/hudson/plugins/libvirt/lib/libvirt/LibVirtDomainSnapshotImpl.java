package hudson.plugins.libvirt.lib.libvirt;

import hudson.plugins.libvirt.lib.IDomainSnapshot;
import org.libvirt.DomainSnapshot;

public class LibVirtDomainSnapshotImpl implements IDomainSnapshot {

    private final DomainSnapshot domainSnapshot;

    public LibVirtDomainSnapshotImpl(DomainSnapshot domainSnapshot) {
        this.domainSnapshot = domainSnapshot;
    }

    public DomainSnapshot getSnapshot() {
        return domainSnapshot;
    }
}
