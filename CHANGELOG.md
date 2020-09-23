# Changelog

### 1.9.0

Release date: _Sep 28, 2020_

-   Fix [SECURITY-1014 (2)](https://www.jenkins.io/security/advisory/2019-10-23/#SECURITY-1014%20(2))
-   Fix [JENKINS-27640](https://issues.jenkins-ci.org/browse/JENKINS-27640) by removing jLibVirt:
    net.schmizz.sshj.userauth.UserAuthException: Exhausted available authentication methods
-   Fix [JENKINS-63027](https://issues.jenkins-ci.org/browse/JENKINS-63027):
    Libvirt-agent plugin is not populating the credentials drop-down

### 1.8.6

Release date: _Jul 6, 2020_

-   Fix [SECURITY-1014 (1)](https://www.jenkins.io/security/advisory/2019-10-23/#SECURITY-1014%20(1))
-   Fix [JENKINS-23205](https://issues.jenkins-ci.org/browse/JENKINS-23205):
    NPE Causes Jenkins Job to Fail
    (Thanks to Sebastian Menski for the contribution)
-   Fix [JENKINS-24654](https://issues.jenkins-ci.org/browse/JENKINS-24654):
    Using libvirt slave 'Before Job Snapshot' causes NPE
-   Fix [JENKINS-25034](https://issues.jenkins-ci.org/browse/JENKINS-25034):
    Credentials metadata leak in Hypervisor
-   Fix [JENKINS-32944](https://issues.jenkins-ci.org/browse/JENKINS-32944):
    if only one hypervisor, there are no virtual machines selectable
    (Thanks to Benedikt Heine for the contribution)
-   Fix [JENKINS-35564](https://issues.jenkins-ci.org/browse/JENKINS-35564):
    Upgrade to Credentials 2.1.0+ API for populating credentials drop-down
-   Fix [JENKINS-40675](https://issues.jenkins-ci.org/browse/JENKINS-40675):
    libvirt-slave-plugin prevents Jenkins from restarting because of java.lang.NullPointerExcetion
-   Fix [JENKINS-50427](https://issues.jenkins-ci.org/browse/JENKINS-50427):
    Libvirt Slaves Plugin needs updating to 2.x

### 1.8.5

Release date: _Apr 01, 2015_

-   Fix [JENKINS-12523](https://issues.jenkins-ci.org/browse/JENKINS-12523):
    Could not initialize class org.libvirt.Connect

### 1.8.4

Release date: _Apr 13, 2014_

-   Add bhyve support
    (Thanks to Roman Bogorodskiy for the contribution)

### 1.8.3

Release date: _Apr 10, 2014_

-   Add LXC support
    (Thanks to Arvid E. Picciani for the contribution)
-   Add shutdown and reboot support
    (Thanks to Arvid E. Picciani for the contribution)
-   Add job-level snapshots
    (Thanks to Alex Szczuczko for the contribution)
-   Fix shutdown handling
    (Thanks to Predrag Knezevic for the contribution)
-   Introduce Java-only libvirt binding jLibVirt
-   Introduce list view of a hypervisor's running domains
-   Bumped required core to 1.546

### 1.8.1

Release date: _Mar 21, 2013_

-   Increased robustness of the hypervisor session handling:
    explicit disconnect upon Jenkins shutdown, auto-reconnect upon libvirtd restarts
-   Various minor improvements to the logging, chattiness decreased

### 1.8

Release date: _Mar 20, 2013_

-   Fix [JENKINS-16889](https://issues.jenkins-ci.org/browse/JENKINS-16889):
    Fixed Hypervisor session management
-   Feature [JENKINS-17293](https://issues.jenkins-ci.org/browse/JENKINS-17293):
    Concurrent slave threshold, configurable per Hypervisor
    (Big thanks to Bryan Dagnin for his contribution!)
-   Feature [JENKINS-16583](https://issues.jenkins-ci.org/browse/JENKINS-16583):
    Support for libvirt snapshot mechanism
-   Feature [JENKINS-16581](https://issues.jenkins-ci.org/browse/JENKINS-16581):
    Modernized maven groupId to org.jenkins-ci.plugins
-   Various minor improvements, mostly help/documentation related
-   Latest libvirt java binding. Note: upgrading users will have to
    reconfigure their hypervisor and node settings!

### 1.7

Release date: _Jan 31, 2013_

-   Fixed [JENKINS-14617](https://issues.jenkins-ci.org/browse/JENKINS-14617)
-   Fixed [JENKINS-14468](https://issues.jenkins-ci.org/browse/JENKINS-14468)
-   Fixed [JENKINS-12523](https://issues.jenkins-ci.org/browse/JENKINS-12523)
-   Fixed [JENKINS-9471](https://issues.jenkins-ci.org/browse/JENKINS-9471)
-   Improved JavaDoc
-   Various minor improvements
-   Bumped required core to 1.420

### 1.6

Release date: _Apr 2, 2010_

-   Fixed problems with Libvirt on RedHat / Centos operating System
-   Added control for machine without libvirt library installed

### 1.5

Release date: _Mar 30, 2010_

-   Fixed problem on Null object after Hypervisor reconnection

### 1.4

Release date: _Mar 25, 2010_

-   Improvements in Hypervisor connection using libvirt
-   Added a missing help file

### 1.3

Release date: _Mar 20, 2010_

-   Fixed problem with hypervisor reconnection after Hudson restart
-   Add a wait time before launching slave agent

### 1.2

Release date: _Mar 10, 2010_

-   Fixed problem in contextual help files-
-   Catch exception on machine without libvirt installed

### 1.1

Release date: _Mar 08, 2010_

-   No change in sources. A version just to fix a problem during release process.

### 1.0

Release date: _Mar 07, 2010_

-   First version published
