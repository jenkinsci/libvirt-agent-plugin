package hudson.plugins.libvirt.lib.jlibvirt;


import com.nirima.libvirt.remote.ILibVirt;
import hudson.plugins.libvirt.lib.IConnect;
import hudson.plugins.libvirt.lib.IDomain;
import hudson.plugins.libvirt.lib.VirtException;

import com.nirima.libvirt.Connect;

/**
 * @author Nigel Magnay
 */
public class JLibVirtConnectImpl implements IConnect {

    private final Connect connect;

    public JLibVirtConnectImpl(Connect connect) {
        this.connect = connect;
    }

    public JLibVirtConnectImpl(String host, int port, String username, String password, String hypervisorUri, boolean b) throws VirtException {
        try
        {
            this.connect = new Connect(host, port, username, password, hypervisorUri, b);
        } catch (Exception e) {
            throw new VirtException(e);
        }
    }

    public long getVersion() throws VirtException {
        try {
            return getLibVirt().connectGetVersion();
        } catch (Exception e) {
            throw new VirtException(e);
        }
    }

    public int[] listDomains() throws VirtException {
        try {
            return connect.listDomains();
        } catch (Exception e) {
            throw new VirtException(e);
        }
    }

    public String[] listDefinedDomains() throws VirtException {
        try {
            return connect.listDefinedDomains();
        } catch (Exception e) {
            throw new VirtException(e);
        }
    }

    public IDomain domainLookupByName(String c) throws VirtException {
        try {
            return new JLibVirtDomainImpl(connect.domainLookupByName(c));
        } catch (Exception e) {
            throw new VirtException(e);
        }
    }

    public IDomain domainLookupByID(int c) throws VirtException {
        try {
            return new JLibVirtDomainImpl(connect.domainLookupById(c));
        } catch (Exception e) {
            throw new VirtException(e);
        }
    }

    public void close() throws VirtException {
        try {
            getLibVirt().connectClose();
        } catch (Exception e) {
            throw new VirtException(e);
        }
    }

    public boolean isConnected() throws VirtException {
        try {
            return connect.isConnected();
        } catch (Exception e) {
            throw new VirtException(e);
        }
    }

    public ILibVirt getLibVirt() {
        return connect.getLibVirt();
    }
}
