#!/bin/sh

REQUIRED_VERSION=1.6

# Transform the required version string into a number that can be used in comparisons
R_VERSION=`echo $REQUIRED_VERSION | sed -e 's;\.;0;g'`
# Check JAVA_HOME directory to see if Java version is adequate
JAVA_VERSION=`java -version 2>&1 | head -n 1 | cut -d\" -f 2`
VERSION=`echo $JAVA_VERSION | awk '{ print substr($1, 1, 3); }' | sed -e 's;\.;0;g'`

if [ $VERSION ]
then
  if [ $VERSION = $R_VERSION ]
  then
    :
  else
    echo "Error: unsupported Java version"
    echo "You are running on Java $JAVA_VERSION but Bonita Open Solution requires Java $REQUIRED_VERSION"
    exit 1
  fi
else
  echo "Warning: Unable to detect your java version"
  echo "Bonita Open Solution requires Java $REQUIRED_VERSION."
  echo "Bonita Open Solution may not work properly"
fi

# Sets some variables
BONITA_HOME="-DBONITA_HOME=$CATALINA_HOME/bonita"
LOG_OPTS="-Djava.util.logging.config.file=$CATALINA_HOME/external/logging/logging.properties"
SECURITY_OPTS="-Djava.security.auth.login.config=$CATALINA_HOME/external/security/jaas-standard.cfg"
CMIS_CONFIG="-Dexo.data.dir=$CATALINA_HOME/external/xcmis/ext-exo-data -Dorg.exoplatform.container.standalone.config=$CATALINA_HOME/external/xcmis/ext-exo-conf/exo-configuration-hsql.xml"
# Uncomment the line below to use mysql as database of the xcmis server instead of hsqldb
# CMIS_CONFIG="-Dexo.data.dir=$CATALINA_HOME/external/xcmis/ext-exo-data -Dorg.exoplatform.container.standalone.config=$CATALINA_HOME/external/xcmis/ext-exo-conf/exo-configuration-mysql.xml"


CATALINA_OPTS="$CATALINA_OPTS $BONITA_HOME $LOG_OPTS $SECURITY_OPTS $CMIS_CONFIG -Dfile.encoding=UTF-8 -Xshare:auto -Xms512m -Xmx1024m -XX:MaxPermSize=256m -XX:+HeapDumpOnOutOfMemoryError"
export CATALINA_OPTS
