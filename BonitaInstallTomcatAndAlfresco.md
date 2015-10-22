## Requirements ##
  1. Alfresco Community 3.4.d
    * http://wiki.alfresco.com/wiki/Community_file_list_3.4.d
    * Direct download: http://dl.alfresco.com/release/community/build-3370/alfresco-community-3.4.d-installer-linux-x64.bin
    * Obs.: New version http://dl.alfresco.com/release/community/build-00007/alfresco-community-4.0.e-installer-linux-x64.bin didn't installed properly (installation process hangs at 50%)
  1. BOS-5.7.2 Tomcat Bundle
    * Direct download: http://www.bonitasoft.com/products/download/tomcat-6.0.33-7

## HowTos ##

  * [Install BOS-Tomcat bundle](http://www.bonitasoft.com/resources/documentation/bos-57/system-administration/installation/install-bos-bundles/install-bos-tomcat-bundle) (authentication required on [BonitaSoft](http://www.bonitasoft.com))
  * All BOS downloads: http://www.bonitasoft.com/products/BPM_downloads/all


## Currently running on ##

Linux bpm 3.3.1-1-ARCH

  * http://bpm.inf.ufsm.br/bonita
  * http://bpm.inf.ufsm.br:9090/alfresco


## Production environment ##

Start server manually:

```
rc.d start alfresco
```

Stop server manually:

```
rc.d stop alfresco
```

## Development environment ##

Start server:

```
/home/jmoura/alfresco-3.4.d/alfresco.sh start
/home/jmoura/novo/BOS-5.6.1-Tomcat-6.0.33/bin/startup.sh
```

Stop server:

```
/home/jmoura/alfresco-3.4.d/alfresco.sh stop
/home/jmoura/novo/BOS-5.6.1-Tomcat-6.0.33/bin/shutdown.sh
```


## Installing Alfresco ##

```
wget http://dl.alfresco.com/release/community/build-3370/alfresco-community-3.4.d-installer-linux-x64.bin
chmod a+x alfresco-community-3.4.d-installer-linux-x64.bin
./alfresco-community-3.4.d-installer-linux-x64.bin

1: English
...
SharePoint [Y/n] : n
Records Management [Y/n] : n
Web Quick Start [Y/n] : n
Web Project Management (AVM) [Y/n] :n
Quickr Connector Support [Y/n] :n
OpenOffice [Y/n] :n
Is the selection above correct? [Y/n]: y
..
Installation Type
[1] Easy
..
Installation Folder
Please choose a folder to install Alfresco Community
Select a folder [/mnt/bpm/alfresco-3.4.d]:
..
Database Installation
[1] I wish to use the bundled MySQL database
Please choose an option [1] :
..
MySQL Credentials
root: <see internal docs>

Admin Password: <see internal docs>
```


## Configuring Alfresco ##

Change Tomcat ports to avoid conflicts between Alfresco and BOS.
```
[bpm@bpm alfresco-3.4.d]$ diff tomcat/conf/server.xml /home/jmoura/alfresco-3.4.d/tomcat/conf/server.xml
22c22
< <Server port="8005" shutdown="SHUTDOWN">
---
> <Server port="8015" shutdown="SHUTDOWN">
69c69
<     <Connector port="8080" URIEncoding="UTF-8" protocol="HTTP/1.1" 
---
>     <Connector port="9090" URIEncoding="UTF-8" protocol="HTTP/1.1" 
84c84
<     <Connector port="8443" protocol="HTTP/1.1" SSLEnabled="true"
---
>     <Connector port="8453" protocol="HTTP/1.1" SSLEnabled="true"
90c90
<     <Connector port="8009" protocol="AJP/1.3" redirectPort="8443" />
---
>     <Connector port="8019" protocol="AJP/1.3" redirectPort="8443" />
```


## Alfresco startup script ##

ArchLinux startup script.
See:
  * https://wiki.archlinux.org/index.php/Daemon
  * https://wiki.archlinux.org/index.php/Writing_rc.d_scripts
  * http://aur.archlinux.org/packages/al/alfresco-community-tomcat/alfresco.rc

/etc/rc.conf
```
...
DAEMONS=(syslog-ng iptables network netfs crond sshd acpid rpcbind nfs-common aut
ofs alfresco)

```

/etc/rc.d/alfresco
```
#!/bin/bash

. /etc/rc.conf
. /etc/rc.d/functions

USER=bpm
case "$1" in
  start)
    stat_busy "Starting Alfresco"
    su -c '/home/bpm/alfresco-3.4.d/alfresco.sh start' ${USER} &> /dev/null
    if [ $? -gt 0 ]; then
      stat_fail
    else
      add_daemon alfresco
      stat_done
    fi
    ;;
  stop)
    stat_busy "Stopping Alfresco"
    su -c '/home/bpm/alfresco-3.4.d/alfresco.sh stop' ${USER} &> /dev/null 
    if [ $? -gt 0 ]; then
      stat_fail
    else
      rm_daemon alfresco
      stat_done
    fi
    ;;
  restart)
    $0 stop
    sleep 1
    $0 start
    ;;
  *)
    echo "usage: $0 {start|stop|restart}"
esac
exit 0

```


## BOS startup script ##

/etc/rc.d/bonita

```
#!/bin/bash

. /etc/rc.conf
. /etc/rc.d/functions
. /etc/profile

USER=bpm
case "$1" in
  start)
    stat_busy "Starting BOS"
    su -c '/home/bpm/BOS-5.7.2-Tomcat-6.0.33/bin/startup.sh' ${USER}
    if [ $? -gt 0 ]; then
      stat_fail
    else
      add_daemon bonita 
      stat_done
    fi
    ;;
  stop)
    stat_busy "Stopping BOS"
    su -c '/home/bpm/BOS-5.7.2-Tomcat-6.0.33/bin/shutdown.sh' ${USER} &> /dev/null 
    if [ $? -gt 0 ]; then
      stat_fail
    else
      rm_daemon bonita
      stat_done
    fi
    ;;
  restart)
    $0 stop
    sleep 1
    $0 start
    ;;
  *)
    echo "usage: $0 {start|stop|restart}"
esac
exit 0
```
Obs.: Foi preciso ler o /etc/profile explicitamente, para forçar a configuração das variáveis de ambiente do Java (no shell aberto por esse script, o /etc/profile não estava sendo lido).

## How do I override the default home page loaded by Tomcat? ##

From: http://wiki.apache.org/tomcat/HowTo#How_do_I_override_the_default_home_page_loaded_by_Tomcat.3F

```
<html>

<head>
<meta http-equiv="refresh" content="0;URL=http://bpm-si.inf.ufsm.br/si/">
</head>

<body>
</body>

</html>
```

## Running Tomcat on port 80 ##

Following these tips: http://hacksforge.com/How-to-run-Tomcat-on-port-80.html

On bpm.inf.ufsm.br:
```
pacman -S iptables
vim /etc/rc.conf
iptables -nvL
rc.d start iptables
iptables -t nat -I PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 8080
rc.d save iptables
rc.d restart iptables
```