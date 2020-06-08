### Changelog

##### Version 1.8.5 (Apr 01, 2015)

-   Fix
    [JENKINS-12523](https://issues.jenkins-ci.org/browse/JENKINS-12523):
    Could not initialize class org.libvirt.Connect

##### Version 1.8.1 (Mar 21, 2013)

-   Increased robustness of the hypervisor session handling: explicit
    disconnect upon Jenkins shutdown, auto-reconnect upon libvirtd
    restarts
-   Various minor improvements to the logging, chattiness decreased

##### Version 1.8 (Mar 20, 2013)

-   Fix [JENKINS-16889](https://issues.jenkins-ci.org/browse/JENKINS-16889):
    Fixed Hypervisor session management
-   Feature [JENKINS-17293](https://issues.jenkins-ci.org/browse/JENKINS-17293):
    Concurrent slave threshold, configurable per Hypervisor (Big thanks
    to Bryan Dagnin for his contribution!)
-   Feature [JENKINS-16583](https://issues.jenkins-ci.org/browse/JENKINS-16583):
    Support for libvirt snapshot mechanism
-   Feature [JENKINS-16581](https://issues.jenkins-ci.org/browse/JENKINS-16581):
    Modernized maven groupId to org.jenkins-ci.plugins
-   Various minor improvements, mostly help/documentation related
-   Latest libvirt java binding. Note: upgrading users will have to
    reconfigure their hypervisor and node settings!

##### Version 1.7 (Jan 31, 2013)

-   Fixed [JENKINS-14617](https://issues.jenkins-ci.org/browse/JENKINS-14617)
-   Fixed [JENKINS-14468](https://issues.jenkins-ci.org/browse/JENKINS-14468)
-   Fixed [JENKINS-12523](https://issues.jenkins-ci.org/browse/JENKINS-12523)
-   Fixed [JENKINS-9471](https://issues.jenkins-ci.org/browse/JENKINS-9471)
-   Improved JavaDoc
-   Various minor improvements
-   Bumped required core to 1.420

##### Version 1.6 (Apr 2, 2010)

-   Fixed problems with Libvirt on RedHat / Centos operating System
-   Added control for machine without libvirt library installed

##### Version 1.5 (Mar 30, 2010)

-   Fixed problem on Null object after Hypervisor reconnection

##### Version 1.4 (Mar 25, 2010)

-   Improvements in Hypervisor connection using libvirt
-   Added a missing help file

##### Version 1.3 (Mar 20, 2010)

-   Fixed problem with hypervisor reconnection after Hudson restart
-   Add a wait time before launching slave agent

##### Version 1.2 (Mar 10, 2010)

-   Fixed problem in contextual help files-
-   Catch exception on machine without libvirt installed

##### Version 1.1 (Mar 08, 2010)

-   No change in sources. A version just to fix a problem during release
    process.

##### Version 1.0 (Mar 07, 2010)

-   First version published
