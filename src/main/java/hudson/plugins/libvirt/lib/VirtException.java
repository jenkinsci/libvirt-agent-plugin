package hudson.plugins.libvirt.lib;

/**
 * Created by magnayn on 04/02/2014.
 */
public class VirtException extends Exception {

    public VirtException() {
    }

    public VirtException(String s) {
        super(s);
    }

    public VirtException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public VirtException(Throwable throwable) {
        super(throwable);
    }
}
