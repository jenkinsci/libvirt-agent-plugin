package hudson.plugins.libvirt.lib.libvirt;

import hudson.plugins.libvirt.lib.IDomainSnapshot;
import org.libvirt.DomainSnapshot;

/**
 * Created by magnayn on 04/02/2014.
 */
public class LibVirtDomainSnapshotImpl implements IDomainSnapshot {

    private final DomainSnapshot domainSnapshot;

    public LibVirtDomainSnapshotImpl(DomainSnapshot domainSnapshot) {
        this.domainSnapshot = domainSnapshot;
    }

    public DomainSnapshot getSnapshot() {
        return domainSnapshot;
    }
}
