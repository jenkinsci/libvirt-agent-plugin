/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.libvirt;

import org.jvnet.hudson.test.JenkinsRule;
import org.junit.Rule;
import org.junit.Test;

/**
 *
 * @author mmornati
 */
public class HypervisorTest {
    @Rule public JenkinsRule j = new JenkinsRule();

    @Test
    public void testDummy() {
        // placeholder while there are no real tests
    }
    // this doesnt work for everyone ...
    //@Test
    //public void testCreation() {
    //    Hypervisor hp = new Hypervisor("test", "localhost", 22, "default", "philipp", 1, false, null);
    //    assertEquals("Wrong Virtual Machines Size", 4, hp.getVirtualMachines().size());
    //    //assertEquals("Wrong Virtual Machine Name", "test", hp.getVirtualMachines().get(0).getName());
    //    assertEquals("Wrong Hypervisor", hp, hp.getVirtualMachines().get(0).getHypervisor());
    //}
}
