package hudson.plugins.libvirt.lib.jlibvirt;

import com.nirima.libvirt.DomainSnapshot;
import hudson.plugins.libvirt.lib.IDomainSnapshot;


/**
 * @author Nigel Magnay
 */
public class JLibVirtDomainSnapshotImpl implements IDomainSnapshot {

    private final DomainSnapshot domainSnapshot;

    public JLibVirtDomainSnapshotImpl(DomainSnapshot domainSnapshot) {
        this.domainSnapshot = domainSnapshot;
    }

    public DomainSnapshot getSnapshot() {
        return domainSnapshot;
    }
}
