package hudson.plugins.libvirt.lib;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.plugins.libvirt.lib.libvirt.LibVirtConnectImpl;
import static hudson.plugins.libvirt.util.Consts.SSH_PORT;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Created by magnayn on 05/02/2014.
 */
public class ConnectionBuilder {
    private static final Logger LOGGER = Logger.getLogger(ConnectionBuilder.class.getName());

    private String uri;
    private boolean readOnly = false;

    private String hypervisorType;
    private String userName;
    private String hypervisorHost;
    private int    hypervisorPort = SSH_PORT;
    private String hypervisorSysUrl;

    private StandardUsernameCredentials credentials;

    public static ConnectionBuilder newBuilder() {
        return new ConnectionBuilder();
    }

    public ConnectionBuilder hypervisorType(String type) {
        this.hypervisorType = type;
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

    public ConnectionBuilder withCredentials(StandardUsernameCredentials standardUsernameCredentials) {
        this.credentials = standardUsernameCredentials;
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

    public IConnect build() throws VirtException {
        if (uri == null) {
            uri = constructHypervisorURI();
        }

        return new LibVirtConnectImpl(uri, readOnly);
    }

    public String constructHypervisorURI() {
        String url = hypervisorType.toLowerCase() + "://";
        // Fixing JENKINS-14617
        if (userName != null && !userName.isEmpty()) {
            url += userName + "@";
        }

        url += hypervisorHost;
        if (hypervisorPort != 0) {
            url += ":" + hypervisorPort;
        }

        if (hypervisorSysUrl != null && !hypervisorSysUrl.isEmpty()) {
            url += "/" + hypervisorSysUrl;
        }

        LogRecord rec = new LogRecord(Level.INFO, "hypervisor: {0}");
        rec.setParameters(new Object[]{url});
        LOGGER.log(rec);

        return url;
    }
}
