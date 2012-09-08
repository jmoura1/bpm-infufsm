@echo on

rem Sets some variables
set BONITA_HOME="-DBONITA_HOME=%CATALINA_HOME%\bonita"
set LOG_OPTS="-Djava.util.logging.config.file=%CATALINA_HOME%\external\logging\logging.properties"
set SECURITY_OPTS="-Djava.security.auth.login.config=%CATALINA_HOME%\external\security\jaas-standard.cfg"
set CMIS_CONFIG=-Dexo.data.dir="%CATALINA_HOME%/external/xcmis/ext-exo-data" -Dorg.exoplatform.container.standalone.config="%CATALINA_HOME%/external/xcmis/ext-exo-conf/exo-configuration-hsql.xml"
rem Uncomment the line below to use mysql as database of the xcmis server instead of hsqldb
rem set CMIS_CONFIG=-Dexo.data.dir="%CATALINA_HOME%/external/xcmis/ext-exo-data" -Dorg.exoplatform.container.standalone.config="%CATALINA_HOME%/external/xcmis/ext-exo-conf/exo-configuration-mysql.xml"
set CATALINA_OPTS=%CATALINA_OPTS% %LOG_OPTS% %SECURITY_OPTS% %BONITA_HOME% %CMIS_CONFIG% -Dfile.encoding=UTF-8 -Xshare:auto -Xms512m -Xmx1024m -XX:MaxPermSize=256m -XX:+HeapDumpOnOutOfMemoryError

rem install and start MySQL
if exist "%CATALINA_HOME%\bin\scripts\installMySQLAndStart.bat" call "%CATALINA_HOME%\bin\scripts\installMySQLAndStart.bat" goto mysqlHome
:mysqlHome
