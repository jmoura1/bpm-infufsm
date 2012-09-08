/**
 * Copyright (C) 2010 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.ow2.bonita.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.ow2.bonita.env.EnvConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Anthony Birembaut, Matthieu Chaffotte
 */
public final class DbMigration {

    private static final Logger LOG = LoggerFactory.getLogger(DbMigration.class);

    private DbMigration() {
    }

    /**
     * Migrate DB schema of the database defined in the environment.
     */
    public static void migrateDb(final String domain, final String configurationName, final String db, final String currentVersion, final String targetVersion)
            throws Exception {
        BonitaConstants.getBonitaHomeFolder();
        final SessionFactory sessionFactory = DbTool.getSessionFactory(domain, configurationName.replaceAll("-configuration", "-session-factory"));
        final String database = db.toLowerCase();
        LOG.info("Running " + database + " " + configurationName + " DB migration...");
        InputStream inputStream = null;
        try {
            inputStream = findMigrationScript(database, currentVersion, targetVersion);
            executeScript(sessionFactory, inputStream, database);
            LOG.info("---------------Schema migrated---------------");
            sessionFactory.close();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    public static void executeScript(final SessionFactory sessionFactory, final InputStream inputStream, final String db) {
        final byte[] bytes = IoUtil.readBytes(inputStream);
        final String scriptContent = new String(bytes);
        final List<String> commands = getCommands(scriptContent, db);

        final Session session = sessionFactory.openSession();
        session.getTransaction().begin();

        LOG.info("DB Commands Execution: " + commands.size());
        for (final String command : commands) {
            LOG.info("Executing command : " + command);
            try {
                session.createSQLQuery(command).executeUpdate();
            } catch (final Exception e) {
                System.err.println("Error while executing command: " + command);
                LOG.error(e.getMessage(), e.getCause());
            }
        }
        session.getTransaction().commit();
        session.close();
    }

    public static List<String> getCommands(final String scriptContent, final String db) {
        String delimiter = ";";
        if (db.equals("sqlserver") || db.equals("sybase")) {
            delimiter = "go";
        }
        final String regex = delimiter.concat("\r?\n");
        final List<String> commands = new ArrayList<String>();
        final String[] tmp = scriptContent.split(regex);
        for (final String command : tmp) {
            if (command.trim().length() > 0) {
                commands.add(command.trim());
            }
        }
        final int lastIndex = commands.size() - 1;
        if (lastIndex >= 0) {
            String lastCommand = commands.get(lastIndex);
            final int index = lastCommand.lastIndexOf(delimiter);
            if (index > 0) {
                lastCommand = lastCommand.substring(0, index);
                commands.remove(lastIndex);
                commands.add(lastCommand);
            }
        }
        return commands;
    }

    public static InputStream findMigrationScript(final String db, final String currentVersion, final String targetVersion) {
        final StringBuilder migrationScript = new StringBuilder();
        migrationScript.append("/migration/").append(db).append("-").append(currentVersion).append("-").append(targetVersion).append(".sql");
        LOG.info("Loading Script " + migrationScript.toString());
        final InputStream inputStream = DbMigration.class.getResourceAsStream(migrationScript.toString());
        if (inputStream == null) {
            final String message = ExceptionManager.getInstance().getFullMessage("bh_DBM_2");
            throw new IllegalArgumentException(message);
        }
        return inputStream;
    }

    public static void main(final String[] args) throws Exception {
        if (args == null || args.length != 3) {
            final String message = ExceptionManager.getInstance().getFullMessage("bh_DBM_1");
            throw new IllegalArgumentException(message);
        }
        final String domain = args[0];
        final String db = args[1].toLowerCase();
        final String currentVersion = args[2];
        final String targetVersion = "5.7";
        final String disableSearch = "bonita.search.use";
        System.setProperty(disableSearch, "false");
        LOG.info("Starting History DB migration");
        migrateDb(domain, EnvConstants.HB_CONFIG_HISTORY, db, currentVersion, targetVersion);
        LOG.info("Starting Core DB migration");
        migrateDb(domain, EnvConstants.HB_CONFIG_CORE, db, currentVersion, targetVersion);
        System.clearProperty(disableSearch);
    }

}
