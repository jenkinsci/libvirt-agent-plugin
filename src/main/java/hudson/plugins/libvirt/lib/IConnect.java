package hudson.plugins.libvirt.lib;

/**
 * Created by magnayn on 04/02/2014.
 */
public interface IConnect {
    long getVersion() throws VirtException;

    int[] listDomains() throws VirtException;

    String[] listDefinedDomains() throws VirtException;

    IDomain domainLookupByName(String c) throws VirtException;

    IDomain domainLookupByID(int c) throws VirtException;

    void close() throws VirtException;

    boolean isConnected() throws VirtException;
}
