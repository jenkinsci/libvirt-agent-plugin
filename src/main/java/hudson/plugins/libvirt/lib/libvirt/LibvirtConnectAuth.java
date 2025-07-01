package hudson.plugins.libvirt.lib.libvirt;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import org.libvirt.ConnectAuth;

/**
 * Implements password-based authentication for libvirt based on Jenkins'
 * Credentials plugin.
 *
 * @author Bastian Germann
 */
public final class LibvirtConnectAuth extends ConnectAuth {

    private final StandardUsernamePasswordCredentials auth;
    private final String overrideUser;

    private static final ConnectAuth.CredentialType[] CRED_TYPE = {
        ConnectAuth.CredentialType.VIR_CRED_USERNAME,
        ConnectAuth.CredentialType.VIR_CRED_PASSPHRASE
    };

    public LibvirtConnectAuth(StandardUsernamePasswordCredentials auth, String overrideUser) {
        this.credType = CRED_TYPE;
        this.auth = auth;
        this.overrideUser = overrideUser;
    }

    @Override
    public int callback(final Credential[] cred) {
        for (Credential c : cred) {
            String response = "";
            switch (c.type) {
                case VIR_CRED_USERNAME:
                    response = overrideUser;
                    if (response == null || response.isEmpty()) {
                        response = auth.getUsername();
                    }
                    break;
                case VIR_CRED_PASSPHRASE:
                    response = auth.getPassword().getPlainText();
                    break;
                default:
                    // ignore
            }
            if (response.isEmpty() && (c.defresult == null || !c.defresult.isEmpty())) {
                c.result = c.defresult;
            } else {
                c.result = response;
            }
            if (c.result == null || c.result.isEmpty()) {
                return -1;
            }
        }
        return 0;
    }

}
