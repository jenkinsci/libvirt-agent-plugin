/**
 * Copyright (C) 2010, Byte-Code srl <http://www.byte-code.com>
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

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Marco Mornati
 */
public class VirtualMachine implements Comparable<VirtualMachine> {

    private final String name;
    private final Hypervisor hypervisor;

    @DataBoundConstructor
    public VirtualMachine(Hypervisor hypervisor, String name) {
        this.hypervisor = hypervisor;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Hypervisor getHypervisor() {
        return hypervisor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VirtualMachine that)) {
            return false;
        }

        if (hypervisor == null) {
            if (that.hypervisor != null) {
                return false;
            }
        } else {
            if (!hypervisor.equals(that.hypervisor)) {
                return false;
            }
        }

        if (name == null) {
            return that.name == null;
        } else {
            return name.equals(that.name);
        }
    }

    @Override
    public int hashCode() {
        int result = 0;

        if (name != null) {
            result = result + 31 * name.hashCode();
        }

        if (hypervisor != null) {
            result = result + hypervisor.hashCode();
        }

        return result;
    }

    public String getDisplayName() {
        return name + "@" + hypervisor.getHypervisorHost();
    }

    public int compareTo(VirtualMachine o) {
        return name.compareTo(o.getName());
    }

    @Override
    public String toString() {
        return "VirtualComputer{name='" + name + "' hypervisor=" + hypervisor + '}';
    }
}
