package hudson.plugins.libvirt.lib;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.plugins.libvirt.lib.jlibvirt.JLibVirtConnectImpl;
import hudson.plugins.libvirt.lib.libvirt.LibVirtConnectImpl;

/**
 * Created by magnayn on 05/02/2014.
 */
public class ConnectionBuilder {

    private boolean useNativeJava = false;
    private String uri;
    private boolean readOnly = false;

    private String hypervisorType;
    private String hypervisorHost;
    private int    hypervisorPort = 22;
    private String hypervisorSysUrl;

    private String protocol = "ssh://";
    private StandardUsernameCredentials credentials;

    public static ConnectionBuilder newBuilder() { return new ConnectionBuilder(); }

    public ConnectionBuilder hypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
        return this;
    }

    public ConnectionBuilder hypervisorHost(String hypervisorHost) {
        this.hypervisorHost = hypervisorHost;
        return this;
    }

    public ConnectionBuilder hypervisorPort(int hypervisorPort) {
        this.hypervisorPort = hypervisorPort;
        return this;
    }

    public ConnectionBuilder hypervisorSysUrl(String hypervisorSysUrl) {
        this.hypervisorSysUrl = hypervisorSysUrl;
        return this;
    }

    public ConnectionBuilder protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public ConnectionBuilder withCredentials(StandardUsernameCredentials standardUsernameCredentials) {
        this.credentials = standardUsernameCredentials;
        return this;
    }

    public ConnectionBuilder useUri(String uri) {
        this.uri = uri;
        return this;
    }

    public ConnectionBuilder readOnly() {
        this.readOnly = true;
        return this;
    }

    public ConnectionBuilder useNativeJava(boolean b) {
        this.useNativeJava = b;
        return this;
    }

    public IConnect build() throws VirtException {

        if( useNativeJava ) {

            if( uri == null )
                uri = constructNativeHypervisorURI();


            StandardUsernamePasswordCredentials standardUsernamePasswordCredentials = (StandardUsernamePasswordCredentials) credentials;
            return new JLibVirtConnectImpl(hypervisorHost,
                    hypervisorPort,
                    credentials.getUsername(),
                    standardUsernamePasswordCredentials.getPassword().getPlainText(),
                    uri, readOnly);
        }
        else
        {
            if( uri == null )
                uri = constructHypervisorURI();
            return new LibVirtConnectImpl(uri, readOnly);
        }
    }

    public String constructHypervisorURI () {
        // Fixing JENKINS-14617
        final String separator = (hypervisorSysUrl.contains("?")) ? "&" : "?";
        return hypervisorType.toLowerCase() + "+" + protocol + this.credentials.getUsername() + "@" + hypervisorHost + ":" + hypervisorPort + "/" + hypervisorSysUrl + separator + "no_tty=1";
    }

    public String constructNativeHypervisorURI () {
        // Fixing JENKINS-14617
        final String separator = (hypervisorSysUrl.contains("?")) ? "&" : "?";
        return hypervisorType.toLowerCase() + ":///" + hypervisorSysUrl + separator + "no_tty=1";
    }


}
