package hudson.plugins.libvirt;

import hudson.Extension;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Job;

import org.kohsuke.stapler.DataBoundConstructor;

public class BeforeJobSnapshotJobProperty extends JobProperty<Job<?, ?>> {

    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return "Pre-execution node snapshot";
        }
    }

    // Required for stapler to work, since "snapshotsEnabled" in the jelly markup encapsulates the data
    public static class EnableData {
        private String snapshotName;
        private boolean snapshotsEnabled;

        @DataBoundConstructor
        public EnableData(boolean snapshotsEnabled, String snapshotName) {
            setSnapshotsEnabled(snapshotsEnabled);
            setSnapshotName(snapshotName);
        }

        public String getSnapshotName() {
            return snapshotName;
        }

        public boolean getSnapshotsEnabled() {
            return snapshotsEnabled;
        }

        private void setSnapshotName(String snapshotName) {
            this.snapshotName = snapshotName;
        }

        private void setSnapshotsEnabled(boolean snapshotsEnabled) {
            this.snapshotsEnabled = snapshotsEnabled;
        }
    }

    private EnableData snapshotsEnabled;

    @DataBoundConstructor
    public BeforeJobSnapshotJobProperty(EnableData snapshots) {
        this.snapshotsEnabled = snapshots;
    }

    public String getSnapshotName() {
        if (!getSnapshotsEnabled()) {
            return null;
        }

        return snapshotsEnabled.getSnapshotName();
    }

    public EnableData getSnapshots() {
        return snapshotsEnabled;
    }

    public boolean getSnapshotsEnabled() {
        return snapshotsEnabled != null;
    }
}
