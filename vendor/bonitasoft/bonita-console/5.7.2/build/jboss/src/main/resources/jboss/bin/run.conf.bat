rem ### -*- batch file -*- ######################################################
rem #                                                                          ##
rem #  JBoss Bootstrap Script Configuration                                    ##
rem #                                                                          ##
rem #############################################################################

rem # $Id: run.conf.bat 88820 2009-05-13 15:25:44Z dimitris@jboss.org $

rem #
rem # This batch file is executed by run.bat to initialize the environment 
rem # variables that run.bat uses. It is recommended to use this file to
rem # configure these variables, rather than modifying run.bat itself. 
rem #

@echo off
rem Java version check
setlocal

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVAVER=%%g
)
set JAVAVER=%JAVAVER:"=%

for /f "delims=. tokens=1-2" %%v in ("%JAVAVER%") do (
	set JAVAVER_MAJOR=%%v
	set JAVAVER_MINOR=%%w
)

set JAVAVER_ERROR=false
if %JAVAVER_MAJOR% neq 1 set JAVAVER_ERROR=true
if %JAVAVER_MINOR% neq 6 set JAVAVER_ERROR=true

if %JAVAVER_ERROR% equ true (
	@echo ERROR: Unsupported Java version
	@echo You are running on Java version %JAVAVER% but Bonita Open Solution requires Java 1.6
	@pause
	exit
)
endlocal


if not "x%JAVA_OPTS%" == "x" goto JAVA_OPTS_SET

rem #
rem # Specify the JBoss Profiler configuration file to load.
rem #
rem # Default is to not load a JBoss Profiler configuration file.
rem #
rem set "PROFILER=%JBOSS_HOME%\bin\jboss-profiler.properties"

rem #
rem # Specify the location of the Java home directory (it is recommended that
rem # this always be set). If set, then "%JAVA_HOME%\bin\java" will be used as
rem # the Java VM executable; otherwise, "%JAVA%" will be used (see below).
rem #
rem set "JAVA_HOME=C:\opt\jdk1.6.0_13"

rem #
rem # Specify the exact Java VM executable to use - only used if JAVA_HOME is
rem # not set. Default is "java".
rem #
rem set "JAVA=C:\opt\jdk1.6.0_13\bin\java"

rem #
rem # Specify options to pass to the Java VM. Note, there are some additional
rem # options that are always passed by run.bat.
rem #

rem # JVM memory allocation pool parameters - modify as appropriate.
set "JAVA_OPTS=-Xms128M -Xmx512M -XX:MaxPermSize=256M"

rem # Reduce the RMI GCs to once per hour for Sun JVMs.
set "JAVA_OPTS=%JAVA_OPTS% -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000"

rem # Warn when resolving remote XML DTDs or schemas.
set "JAVA_OPTS=%JAVA_OPTS% -Dorg.jboss.resolver.warning=true"

rem # Sample JPDA settings for remote socket debugging
rem set "JAVA_OPTS=%JAVA_OPTS% -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n"

rem # Sample JPDA settings for shared memory debugging 
rem set "JAVA_OPTS=%JAVA_OPTS% -Xrunjdwp:transport=dt_shmem,address=jboss,server=y,suspend=n"

:JAVA_OPTS_SET
rem Sets some variables
set tmp_script_home=%CD%
call :setJbossHome %CD%

set "BONITA_OPTS=-DBONITA_HOME=%tmp_jboss_home%bonita -Dorg.ow2.bonita.api-type=EJB3 -Djava.naming.factory.initial=org.jnp.interfaces.NamingContextFactory -Djava.naming.provider.url=jnp://localhost:1099"
set "LOG_OPTS=-Djava.util.logging.config.file=%tmp_jboss_home%external/logging/logging.properties"
set "CMIS_CONFIG=-Dexo.data.dir=%tmp_jboss_home%external/xcmis/ext-exo-data -Dorg.exoplatform.container.standalone.config=%tmp_jboss_home%external/xcmis/ext-exo-conf/exo-configuration-hsql.xml"
set "JAVA_OPTS=%JAVA_OPTS% %BONITA_OPTS% %LOG_OPTS% %CMIS_CONFIG% -Dfile.encoding=UTF-8 -Xshare:auto -Xms512m -Xmx1024m -XX:MaxPermSize=256m -XX:+HeapDumpOnOutOfMemoryError"
goto :eof

:setJbossHome
set tmp_jboss_home=%~dp1
goto :eof