package hudson.plugins.libvirt.lib.libvirt;


import hudson.plugins.libvirt.lib.IConnect;
import hudson.plugins.libvirt.lib.IDomain;
import hudson.plugins.libvirt.lib.VirtException;
import org.libvirt.Connect;
import org.libvirt.Connect.OpenFlags;
import org.libvirt.ConnectAuth;
import org.libvirt.LibvirtException;

/**
 * Created by magnayn on 04/02/2014.
 */
public class LibVirtConnectImpl implements IConnect {
    private final Connect connect;

    public LibVirtConnectImpl(Connect connect) {
        this.connect = connect;
    }

    public LibVirtConnectImpl(String hypervisorUri, ConnectAuth auth, boolean readOnly) throws VirtException {
        int flags = 0;
        if (readOnly) {
            flags |= OpenFlags.READONLY.getBit();
        }
        try {
            this.connect = new Connect(hypervisorUri, auth, flags);
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    @Override
    public long getVersion() throws VirtException {
        try {
            return connect.getVersion();
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    @Override
    public int[] listDomains() throws VirtException {
        try {
            return connect.listDomains();
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    @Override
    public String[] listDefinedDomains() throws VirtException {
        try {
            return connect.listDefinedDomains();
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    @Override
    public IDomain domainLookupByName(String c) throws VirtException {
        try {
            return new LibVirtDomainImpl(connect.domainLookupByName(c));
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    @Override
    public IDomain domainLookupByID(int c) throws VirtException {
        try {
            return new LibVirtDomainImpl(connect.domainLookupByID(c));
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    @Override
    public void close() throws VirtException {
        try {
            connect.close();
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }

    @Override
    public boolean isConnected() throws VirtException {
        try {
            return connect.isConnected();
        } catch (LibvirtException e) {
            throw new VirtException(e);
        }
    }
}
