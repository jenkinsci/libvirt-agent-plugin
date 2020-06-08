package hudson.plugins.libvirt.lib.libvirt;


import hudson.plugins.libvirt.lib.IConnect;
import hudson.plugins.libvirt.lib.IDomain;
import hudson.plugins.libvirt.lib.VirtException;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;

/**
 * Created by magnayn on 04/02/2014.
 */
public class LibVirtConnectImpl implements IConnect {
    private final Connect connect;

    public LibVirtConnectImpl(Connect connect) {
        this.connect = connect;
    }

    public LibVirtConnectImpl(String hypervisorUri, boolean b) throws VirtException {
        try {
            this.connect = new Connect(hypervisorUri, b);
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    public long getVersion() throws VirtException {
        try {
            return connect.getVersion();
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    public int[] listDomains() throws VirtException {
        try {
            return connect.listDomains();
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    public String[] listDefinedDomains() throws VirtException {
        try {
            return connect.listDefinedDomains();
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    public IDomain domainLookupByName(String c) throws VirtException {
        try {
            return new LibVirtDomainImpl(connect.domainLookupByName(c));
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    public IDomain domainLookupByID(int c) throws VirtException {
        try {
            return new LibVirtDomainImpl(connect.domainLookupByID(c));
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    public void close() throws VirtException {
        try {
            connect.close();
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    public boolean isConnected() throws VirtException {
        try {
            return connect.isConnected();
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }
}
