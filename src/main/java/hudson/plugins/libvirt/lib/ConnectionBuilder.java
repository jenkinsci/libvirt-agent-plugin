package hudson.plugins.libvirt.lib;

import hudson.plugins.libvirt.lib.libvirt.LibvirtConnectAuth;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.plugins.libvirt.lib.libvirt.LibVirtConnectImpl;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.libvirt.ConnectAuth;

/**
 * @author Nigel Magnay
 * @version 05/02/2014
 */
public class ConnectionBuilder {
    private static final Logger LOGGER = Logger.getLogger(ConnectionBuilder.class.getName());

    private String uri;
    private boolean readOnly = false;

    private String hypervisorType;
    private String hypervisorTransport;
    private String userName;
    private String hypervisorHost;
    private int    hypervisorPort;
    private String hypervisorSysUrl;

    private StandardUsernamePasswordCredentials credentials;

    public static ConnectionBuilder newBuilder() {
        return new ConnectionBuilder();
    }

    public ConnectionBuilder hypervisorType(String type) {
        this.hypervisorType = type;
        return this;
    }

    public ConnectionBuilder hypervisorTransport(String transport) {
        this.hypervisorTransport = transport;
        return this;
    }

    public ConnectionBuilder userName(String user) {
        this.userName = user;
        return this;
    }

    public ConnectionBuilder hypervisorHost(String host) {
        this.hypervisorHost = host;
        return this;
    }

    public ConnectionBuilder hypervisorPort(int port) {
        this.hypervisorPort = port;
        return this;
    }

    public ConnectionBuilder hypervisorSysUrl(String sysUrl) {
        this.hypervisorSysUrl = sysUrl;
        return this;
    }

    public ConnectionBuilder withCredentials(StandardUsernamePasswordCredentials userPwCredentials) {
        this.credentials = userPwCredentials;
        return this;
    }

    public ConnectionBuilder useUri(String newuri) {
        this.uri = newuri;
        return this;
    }

    public ConnectionBuilder readOnly() {
        this.readOnly = true;
        return this;
    }

    public boolean isLibraryTransport() {
        return hypervisorTransport != null && hypervisorTransport.startsWith("libssh");
    }

    public IConnect build() throws VirtException {
        if (uri == null) {
            uri = constructHypervisorURI();
        }
        ConnectAuth auth = null;
        if (isLibraryTransport() && credentials != null) {
            auth = new LibvirtConnectAuth(credentials, userName);
        }
        return new LibVirtConnectImpl(uri, auth, readOnly);
    }

    public String constructHypervisorURI() {
        String url = hypervisorType.toLowerCase(Locale.ENGLISH);
        if (hypervisorTransport != null && !hypervisorTransport.isEmpty()) {
            url += "+" + hypervisorTransport;
        }
        url += "://";
        // Fixing JENKINS-14617
        if (userName != null && !userName.isEmpty()) {
            url += userName + "@";
        } else if (credentials != null) {
            url += credentials.getUsername() + "@";
        }

        url += hypervisorHost;
        if (hypervisorPort != 0) {
            url += ":" + hypervisorPort;
        }

        if (hypervisorSysUrl != null && !hypervisorSysUrl.isEmpty()) {
            url += "/" + hypervisorSysUrl;
            if (!hypervisorSysUrl.contains("?")) {
                url += "?";
            }
        } else {
            url += "?";
        }
        if ((hypervisorSysUrl == null || !hypervisorSysUrl.contains("no_tty="))
                && "ssh".equals(hypervisorTransport)) {
            url += "no_tty=1";
        }
        if ((hypervisorSysUrl == null || !hypervisorSysUrl.contains("sshauth="))
                && isLibraryTransport() && credentials != null) {
            url += "&sshauth=password";
        }
        if ((hypervisorSysUrl == null || !hypervisorSysUrl.contains("known_hosts_verify="))
                && isLibraryTransport()) {
            url += "&known_hosts_verify=auto";
        }

        LogRecord rec = new LogRecord(Level.INFO, "hypervisor: {0}");
        rec.setParameters(new Object[]{url});
        LOGGER.log(rec);

        return url;
    }
}
