/**
 * Copyright (C) 2024 Michael Jeanson <mjeanson@efficios.com>
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

import hudson.slaves.OfflineCause;

public abstract class OfflineClause {
    /**
     * Node is taken offline to revert to a snapshot.
     */
    public static class RevertSnapshot extends OfflineCause.ByCLI {
        public RevertSnapshot(String message) {
            super(message);
        }
    }

    /**
     * Node is taken offline for a reboot.
     */
    public static class Reboot extends OfflineCause.ByCLI {
        public Reboot(String message) {
            super(message);
        }
    }
}
