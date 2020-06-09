# Changelog

### 1.8.5

Release date: _Apr 01, 2015_

-   Fix [JENKINS-12523](https://issues.jenkins-ci.org/browse/JENKINS-12523):
    Could not initialize class org.libvirt.Connect

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
