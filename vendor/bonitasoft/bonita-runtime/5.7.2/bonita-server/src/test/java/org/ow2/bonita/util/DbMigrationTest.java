package org.ow2.bonita.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;

public class DbMigrationTest extends TestCase {

    private List<String> getCommands(final String database, final String currentVersion, final String targetVersion) {
        final InputStream stream = DbMigration.findMigrationScript(database, currentVersion, targetVersion);
        byte[] bytes = null;
        try {
            bytes = IoUtil.readBytes(stream);
        } finally {
            try {
                stream.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        final String scriptContent = new String(bytes);
        return DbMigration.getCommands(scriptContent, database);
    }

    private void checkCommands(final String database, final String currentVersion, final String targetVersion, final int nbOfCommands,
            final String forbbidenExpression) {
        final List<String> commands = getCommands(database, currentVersion, targetVersion);
        assertEquals(nbOfCommands, commands.size());
        for (final String command : commands) {
            assertTrue(command, !command.contains(forbbidenExpression));
        }
    }

    public void testMySqlScriptMajor() {
        checkCommands("mysql", "5.6", "5.7", 5, ";");
    }

    public void testSqlServerScriptMajor() {
        checkCommands("sqlserver", "5.6", "5.7", 5, "go");
    }

    public void testOracleScriptMajor() {
        checkCommands("oracle", "5.6", "5.7", 5, ";");
    }

    public void testPostgreScriptMajor() {
        checkCommands("postgresql", "5.6", "5.7", 5, ";");
    }

    public void testH2ScriptMajor() {
        checkCommands("h2", "5.6", "5.7", 5, ";");
    }

}
