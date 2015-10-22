## H2 database ##

### Files ###
```
./BOS-5.6.1/studio/workspace/bonita/server/default/work/databases/bonita_history.db.h2.db
./BOS-5.6.1/studio/workspace/bonita/server/default/work/databases/bonita_journal.db.h2.db
```


### Jar location ###
```
./studio/workspace/local_default_My Extensions/provided-libs/database/lib/h2-1.1.106.jar
./studio/plugins/org.bonitasoft.studio.engine.libs_1.0.0.5_6_1/lib/h2-1.2.139.jar
```

### Browsing ###

Start up the built-in org.h2.tools.Server and browse port 8082:
```
java -cp /home/andrea/tools/BOS-5.6.1/studio/plugins/org.bonitasoft.studio.engine.libs_1.0.0.5_6_1/lib/h2-1.2.139.jar org.h2.tools.Server

jdbc:h2:/home/andrea/tools/BOS-5.6.1/studio/workspace/bonita/server/default/work/databases/bonita_history.db

jdbc:h2:/home/andrea/tools/BOS-5.6.1/studio/workspace/bonita/server/default/work/databases/bonita_journal.db

User Name: bonita
Password: bpm
```

### Recovering ###
http://infocenter.pentaho.com/help/index.jsp?topic=%2Fpdi_admin_guide%2Ftask_h2_db_recovery_tool.html

## References ##

  * How to create reports about the data collected in the forms: http://www.bonitasoft.org/forum/viewtopic.php?id=2907
  * Back-loading of User Account to bn\_user: http://www.bonitasoft.org/forum/viewtopic.php?id=3338