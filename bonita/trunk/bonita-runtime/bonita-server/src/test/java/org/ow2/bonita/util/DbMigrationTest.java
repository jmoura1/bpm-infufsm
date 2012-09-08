package org.ow2.bonita.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;

public class DbMigrationTest extends TestCase {

  private List<String> getCommands(String database, final String currentVersion, final String targetVersion) {
    InputStream stream = DbMigration.findMigrationScript(database, currentVersion, targetVersion);
    byte[] bytes = null;
    try {
      bytes = IoUtil.readBytes(stream);
    } finally {
      try {
        stream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    final String scriptContent = new String(bytes);
    return DbMigration.getCommands(scriptContent, database);
  }

  private void checkCommands(final String database, final String currentVersion, final String targetVersion, final int nbOfCommands, final String forbbidenExpression) {
    final List<String> commands = getCommands(database, currentVersion, targetVersion);
    assertEquals(nbOfCommands, commands.size());
    for (String command : commands) {
      assertTrue(command, !command.contains(forbbidenExpression));
    }
  }
  
  public void testMySqlScriptMajor() {
    checkCommands("mysql", "5.5.x", "5.6.1", 6, ";");
  }

  public void testSqlServerScriptMajor() {
    checkCommands("sqlserver", "5.5.x", "5.6.1", 6, "go");
  }

  public void testOracleScriptMajor() {
    checkCommands("oracle", "5.5.x", "5.6.1", 6, "go");
  }
  
  public void testMySqlScriptMinor() {
    checkCommands("mysql", "5.6", "5.6.1", 4, ";");
  }
  
  public void testSqlServerScriptMinor() {
    checkCommands("sqlserver", "5.6", "5.6.1", 4, "go");
  }

  public void testOracleScriptMinor() {
    checkCommands("oracle", "5.6", "5.6.1", 4, "go");
  }
  
}
