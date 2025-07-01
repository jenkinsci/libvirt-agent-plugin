/**
 * Copyright (C) 2010, Byte-Code srl <http://www.byte-code.com>
 * Copyright (C) 2012, Philipp Bartsch <tastybug@tastybug.com>
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package hudson.plugins.libvirt;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.SchemeRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Label;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.plugins.libvirt.lib.ConnectionBuilder;
import hudson.plugins.libvirt.lib.IConnect;
import hudson.plugins.libvirt.lib.IDomain;
import hudson.plugins.libvirt.lib.VirtException;
import hudson.security.ACL;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import hudson.util.FormValidation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;

import hudson.util.ListBoxModel;
import java.util.Arrays;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.verb.POST;
import org.springframework.security.core.Authentication;

/**
 * Represents a virtual datacenter.
 *
 * @author Marco Mornati
 * @author Philipp Bartsch
 */
public class Hypervisor extends Cloud {

    private static final Logger LOGGER = Logger.getLogger(Hypervisor.class.getName());

    private final String hypervisorType;
    private final String hypervisorTransport;
    private final String hypervisorHost;
    private final String hypervisorSystemUrl;
    private int hypervisorSshPort;
    private final String username;
    private final int maxOnlineSlaves;
    private transient int currentOnlineSlaveCount = 0;
    private transient ConcurrentHashMap<String, String> currentOnline;
    private transient IConnect connection;
    private final String credentialsId;

    @DataBoundConstructor
    public Hypervisor(String hypervisorType, String hypervisorTransport, String hypervisorHost,
                      int hypervisorSshPort, String hypervisorSystemUrl,
                      String username, int maxOnlineSlaves,
                      String credentialsId) {
        super("Hypervisor(libvirt)");
        this.hypervisorType = hypervisorType;
        final String[] ht = this.hypervisorType.split("\\+");
        if (ht.length > 1) {
            // convert old format with integrated transport
            hypervisorTransport = ht[1];
        }
        this.hypervisorTransport = hypervisorTransport;
        this.hypervisorHost = hypervisorHost;
        this.hypervisorSystemUrl = Objects.requireNonNullElse(hypervisorSystemUrl, "system");

        if (hypervisorSshPort > 0) {
            this.hypervisorSshPort = hypervisorSshPort;
        }

        this.username = username;
        this.maxOnlineSlaves = maxOnlineSlaves;
        this.credentialsId = credentialsId;
    }

    protected void ensureLists() {
        if (currentOnline == null) {
            currentOnline = new ConcurrentHashMap<>();
        }
    }

    private ConnectionBuilder createBuilder() {
        return new ConnectionBuilder(hypervisorType, hypervisorHost)
                .hypervisorTransport(hypervisorTransport)
                .userName(username)
                .withCredentials(lookupSystemCredentials(credentialsId))
                .hypervisorPort(hypervisorSshPort)
                .hypervisorSysUrl(hypervisorSystemUrl);
    }

    private synchronized IConnect getOrCreateConnection() throws VirtException {

        if (connection == null || !connection.isConnected()) {

            ConnectionBuilder builder = createBuilder();

            LOGGER.log(Level.INFO,
                       "Trying to establish a connection to hypervisor URI: {0} as {1}/******",
                    new Object[]{builder.constructHypervisorURI(), username});

            try {
                connection = builder.build();
                LOGGER.log(Level.INFO,
                           "Established connection to hypervisor URI: {0} as {1}/******",
                        new Object[]{builder.constructHypervisorURI(), username});
            } catch (VirtException e) {
                LogRecord rec =
                        new LogRecord(Level.SEVERE,
                                      "Failed to establish connection to hypervisor URI: {0} as {1}/******");
                rec.setThrown(e);
                rec.setParameters(new Object[]{builder.constructHypervisorURI(), username});
                LOGGER.log(rec);
            }
        } else {
            try {
                // the connection appears to be up but might actually be dead (e.g. due to a restart of libvirtd)
                // lets try a simple function call and see if it turns out ok
                connection.getVersion();
            } catch (VirtException lve) {
                ConnectionBuilder builder = createBuilder();
                LogRecord rec =
                        new LogRecord(Level.WARNING,
                                      "Connection appears to be broken, trying to reconnect: {0} as {1}/******");
                rec.setParameters(new Object[]{builder.constructHypervisorURI(), username});
                LOGGER.log(rec);
                try {
                    connection = builder.build();
                } catch (VirtException lve2) {
                    rec = new LogRecord(Level.SEVERE,
                                        "Failed to re-establish connection to hypervisor URI: {0} as {1}/******");
                    rec.setThrown(lve2);
                    rec.setParameters(new Object[]{builder.constructHypervisorURI(), username});
                    LOGGER.log(rec);
                }
            }
        }
        return connection;
    }

    public String getHypervisorHost() {
        return hypervisorHost;
    }

    public int getHypervisorSshPort() {
        return hypervisorSshPort;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public String getHypervisorTransport() {
        return hypervisorTransport;
    }

    public String getHypervisorSystemUrl() {
        return hypervisorSystemUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public int getMaxOnlineSlaves() {
        return maxOnlineSlaves;
    }

    public synchronized int getCurrentOnlineSlaveCount() {
        return currentOnlineSlaveCount;
    }

    public String getHypervisorDescription() {
        return getHypervisorType() + "+" + getHypervisorTransport() + " - " + getHypervisorHost();
    }

    public synchronized Map<String, IDomain> getDomains() throws VirtException {
        Map<String, IDomain> domains = new HashMap<>();
        IConnect con = getOrCreateConnection();
        LogRecord info = new LogRecord(Level.FINE, "Getting hypervisor domains.");
        LOGGER.log(info);
        if (con != null) {
            for (String c : con.listDefinedDomains()) {
                if (c != null && !c.isEmpty()) {
                    IDomain domain;
                    try {
                        domain = con.domainLookupByName(c);
                        domains.put(domain.getName(), domain);
                    } catch (VirtException e) {
                        LogRecord rec =
                                new LogRecord(Level.WARNING,
                                              "Error retrieving information for domain with name: {0}.");
                        rec.setParameters(new Object[]{c});
                        rec.setThrown(e);
                        LOGGER.log(rec);
                    }
                }
            }
            for (int c : con.listDomains()) {
                IDomain domain;
                try {
                    domain = con.domainLookupByID(c);
                    domains.put(domain.getName(), domain);
                } catch (VirtException e) {
                    LogRecord rec =
                            new LogRecord(Level.WARNING,
                                          "Error retrieving information for domain with id: {0}.");
                    rec.setParameters(new Object[]{c});
                    rec.setThrown(e);
                    LOGGER.log(rec);
                }
            }
        } else {
            LogRecord rec =
                    new LogRecord(Level.SEVERE,
                                  "Cannot connect to Hypervisor {0} as {1}/******");
            rec.setParameters(new Object[]{hypervisorHost, username});
            LOGGER.log(rec);
        }

        return domains;
    }

    /**
     * Returns a <code>List</code> of VMs configured on the hypervisor. This
     * method always retrieves the current list of VMs to ensure that newly
     * available instances show up right away.
     *
     * @return the virtual machines
     */
    public synchronized List<VirtualMachine> getVirtualMachines() {
        List<VirtualMachine> vmList = new ArrayList<>();
        try {
            Map<String, IDomain> domains = getDomains();
            for (String domainName : domains.keySet()) {
                vmList.add(new VirtualMachine(this, domainName));
            }
        } catch (VirtException e) {
            LogRecord rec =
                    new LogRecord(Level.SEVERE,
                                  "Cannot connect to datacenter {0} as {1}/******");
            rec.setThrown(e);
            rec.setParameters(new Object[]{hypervisorHost, username});
            LOGGER.log(rec);
        }
        return vmList;
    }

    /**
     * Returns an array of snapshots names/ids of a given VM as found by
     * libvirt.
     *
     * @param virtualMachineName the name of the vm
     * @return the array of snapshot ids (can be empty)
     */
    public synchronized String[] getSnapshots(String virtualMachineName) {
        try {
            for (IDomain domain : getDomains().values()) {
                if (domain.getName().equals(virtualMachineName)) {
                    LogRecord rec =
                            new LogRecord(Level.FINE,
                                          "Fetching snapshots for " + virtualMachineName + ": " + domain.snapshotNum());
                    LOGGER.log(rec);
                    return domain.snapshotListNames();
                }
            }
        } catch (VirtException lve) {
            LogRecord rec =
                    new LogRecord(Level.SEVERE,
                                  "Failed to fetch snapshot ids for VM {0} at datacenter {1} as {2}/******");
            rec.setThrown(lve);
            rec.setParameters(new Object[]{virtualMachineName, hypervisorHost, username});
            LOGGER.log(rec);
        }
        return new String[0];
    }

    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(Label label, int i) {
        return Collections.emptySet();
    }

    @Override
    public boolean canProvision(Label label) {
        return false;
    }

    @Override
    public String toString() {
        return "Hypervisor{hypervisorUri='" + hypervisorHost + "', username='" + username + "'}";
    }

    public synchronized Boolean canMarkVMOnline(String slaveName, String vmName) {
        ensureLists();

        // Don't allow more than max.
        if (maxOnlineSlaves > 0 && currentOnline.size() == maxOnlineSlaves) {
            return Boolean.FALSE;
        }

        // Don't allow two slaves to the same VM to fire up.
        if (currentOnline.containsValue(vmName)) {
            return Boolean.FALSE;
        }

        // Don't allow two instances of the same slave, although Jenkins will
        // probably not encounter this.
        if (currentOnline.containsKey(slaveName)) {
            return Boolean.FALSE;
        }

        // Don't allow a misconfigured slave to try start
        if ("".equals(vmName) || "".equals(slaveName)) {
            LogRecord rec = new LogRecord(Level.WARNING, "Agent '" + slaveName
                                          + "' (using VM '" + vmName
                                          + "') appears to be misconfigured.");
            LOGGER.log(rec);
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    public synchronized Boolean markVMOnline(String slaveName, String vmName) {
        ensureLists();

        // If the combination is already in the list, it's good.
        if (currentOnline.containsKey(slaveName) && currentOnline.get(slaveName).equals(vmName)) {
            return Boolean.TRUE;
        }

        if (!canMarkVMOnline(slaveName, vmName)) {
            return Boolean.FALSE;
        }

        currentOnline.put(slaveName, vmName);
        currentOnlineSlaveCount++;

        return Boolean.TRUE;
    }

    public synchronized void markVMOffline(String slaveName, String vmName)
            throws VirtException {
        ensureLists();

        if (currentOnline.remove(slaveName) != null) {
            currentOnlineSlaveCount--;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (connection != null) {
            connection.close();
        }
        super.finalize();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public static StandardUsernamePasswordCredentials lookupSystemCredentials(String credentialsId) {
        if (Strings.isNullOrEmpty(credentialsId)) {
            return null;
        }
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider
                        .lookupCredentialsInItemGroup(StandardUsernamePasswordCredentials.class,
                                           Jenkins.get(), ACL.SYSTEM2,
                                List.of(new SchemeRequirement("ssh"))),
                CredentialsMatchers.withId(credentialsId)
        );
    }

    public String getHypervisorURI() {
        return createBuilder().constructHypervisorURI();
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Cloud> {
        private String type;
        private String transport;
        private String hvHost;
        private String systemUrl;
        private int sshPort;
        private String user;

        @NonNull
        @Override
        public String getDisplayName() {
            return "Hypervisor (via libvirt)";
        }

        @Override
        public boolean configure(StaplerRequest2 req, JSONObject o)
                throws FormException {
            type = o.getString("hypervisorType");
            transport = o.getString("hypervisorTransport");
            hvHost = o.getString("hypervisorHost");
            systemUrl = o.getString("hypervisorSystemUrl");
            sshPort = o.getInt("hypervisorSshPort");
            user = o.getString("username");
            save();
            return super.configure(req, o);
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context,
                                                     @QueryParameter String hypervisorType,
                                                     @QueryParameter String hypervisorTransport,
                                                     @QueryParameter String hypervisorHost,
                                                     @QueryParameter String hypervisorSshPort,
                                                     @QueryParameter String username,
                                                     @QueryParameter String hypervisorSystemUrl,
                                                     @QueryParameter String credentialsId) {
            StandardListBoxModel model = new StandardListBoxModel();

            if (context == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                LOGGER.log(Level.INFO, "no Jenkins.ADMINISTER permissions");

                return model.includeCurrentValue(credentialsId);
            }

            if (context != null && !context.hasPermission(Computer.CONFIGURE)) {
                LOGGER.log(Level.INFO, "no Computer.CONFIGURE permissions");

                return model.includeCurrentValue(credentialsId);
            }

            ConnectionBuilder builder = new ConnectionBuilder(hypervisorType, hypervisorHost)
                        .hypervisorTransport(hypervisorTransport)
                        .userName(username)
                        .withCredentials(lookupSystemCredentials(credentialsId))
                        .hypervisorPort(Integer.parseInt(hypervisorSshPort))
                        .hypervisorSysUrl(hypervisorSystemUrl);

            String hypervisorUri = builder.constructHypervisorURI();

            Authentication auth;
            if (context instanceof Queue.Task) {
                auth = Tasks.getAuthenticationOf2((Queue.Task) context);
            } else {
                auth = ACL.SYSTEM2;
            }
            return model
                    .includeEmptyValue()
                    .includeMatchingAs(
                        auth,
                        context,
                        StandardUsernamePasswordCredentials.class,
                        URIRequirementBuilder.fromUri(hypervisorUri).build(),
                        CredentialsMatchers.anyOf(
                                CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class)
                        ))
                    .includeCurrentValue(credentialsId);
        }

        public FormValidation doCheckCredentialsId(@AncestorInPath Item item,
                                                   @QueryParameter String hypervisorType,
                                                   @QueryParameter String hypervisorTransport,
                                                   @QueryParameter String hypervisorHost,
                                                   @QueryParameter String hypervisorSshPort,
                                                   @QueryParameter String username,
                                                   @QueryParameter String hypervisorSystemUrl,
                                                   @QueryParameter String credentialsId) {

            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok();
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ)
                    && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return FormValidation.ok();
                }
            }

            if (StringUtils.isBlank(credentialsId)) {
                return FormValidation.ok();
            }

            if (credentialsId.startsWith("${") && credentialsId.endsWith("}")) {
                return FormValidation.warning("Cannot validate expression based credentials");
            }

            if (lookupSystemCredentials(credentialsId) == null) {
                return FormValidation.error("Cannot find currently selected credentials");
            }

            return FormValidation.ok();
        }

        @POST
        public FormValidation doTestConnection(@QueryParameter String hypervisorType,
                                               @QueryParameter String hypervisorTransport,
                                               @QueryParameter String hypervisorHost,
                                               @QueryParameter String hypervisorSshPort,
                                               @QueryParameter String username,
                                               @QueryParameter String hypervisorSystemUrl,
                                               @QueryParameter String credentialsId)
                throws Exception, ServletException {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);

            try {
                if (hypervisorHost == null) {
                    return FormValidation.error("Hypervisor Host is not specified!");
                }
                if (hypervisorType == null) {
                    return FormValidation.error("Hypervisor type is not specified!");
                }

                ConnectionBuilder builder = new ConnectionBuilder(hypervisorType, hypervisorHost)
                        .hypervisorTransport(hypervisorTransport)
                        .userName(username)
                        .withCredentials(lookupSystemCredentials(credentialsId))
                        .hypervisorPort(Integer.parseInt(hypervisorSshPort))
                        .hypervisorSysUrl(hypervisorSystemUrl);

                String hypervisorUri = builder.constructHypervisorURI();

                LogRecord rec =
                        new LogRecord(Level.FINE,
                                      "Testing connection to hypervisor: {0}");
                rec.setParameters(new Object[]{hypervisorUri});
                LOGGER.log(rec);

                IConnect hypervisorConnection = builder.build();
                long version = hypervisorConnection.getVersion();
                hypervisorConnection.close();
                return FormValidation.ok("OK: " + hypervisorUri + ", version=" + version);
            } catch (VirtException e) {
                LogRecord rec =
                        new LogRecord(Level.WARNING,
                                      "Failed to check hypervisor connection to {0} as {1}/******");
                rec.setThrown(e);
                rec.setParameters(new Object[]{hypervisorHost, username});
                LOGGER.log(rec);
                return FormValidation.error(e.getMessage());
            } catch (UnsatisfiedLinkError | Exception e) {
                LogRecord rec =
                        new LogRecord(Level.WARNING,
                                      "Failed to connect to hypervisor. "
                                    + "Check libvirt installation on jenkins machine!");
                rec.setThrown(e);
                rec.setParameters(new Object[]{hypervisorHost, username});
                LOGGER.log(rec);
                return FormValidation.error(e.getMessage());
            }
        }

        public String getHypervisorHost() {
            return hvHost;
        }

        public int getHypervisorSshPort() {
            return sshPort;
        }

        public String getHypervisorSystemUrl() {
            return systemUrl;
        }

        public String getHypervisorType() {
            return type;
        }

        public String getHypervisorTransport() {
            return transport;
        }

        public String getUsername() {
            return user;
        }

        public List<String> getHypervisorTypes() {
            return Arrays.asList("QEMU", "XEN", "LXC", "BHYVE", "OPENVZ", "VZ", "R4D");
        }

        public List<String> getHypervisorTransports() {
            return Arrays.asList("", "tls", "unix", "ssh", "ext", "tcp", "libssh", "libssh2");
        }
    }
}
