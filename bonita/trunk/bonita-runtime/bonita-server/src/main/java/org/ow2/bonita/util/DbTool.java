/**
 * Copyright (C) 2007  Bull S. A. S.
 * Bull, Rue Jean Jaures, B.P.68, 78340, Les Clayes-sous-Bois
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA  02110-1301, USA.
 * 
 * Modified by Matthieu Chaffotte - BonitaSoft S.A.
 **/
package org.ow2.bonita.util;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Cache;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.type.TextType;
import org.ow2.bonita.env.EnvConstants;
import org.ow2.bonita.env.EnvironmentFactory;
import org.ow2.bonita.env.GlobalEnvironmentFactory;
import org.ow2.bonita.facade.IdentityAPI;
import org.ow2.bonita.facade.def.element.impl.MetaDataImpl;
import org.ow2.bonita.facade.identity.Group;
import org.ow2.bonita.facade.identity.Membership;
import org.ow2.bonita.facade.identity.impl.GroupImpl;
import org.ow2.bonita.facade.identity.impl.MembershipImpl;
import org.ow2.bonita.facade.identity.impl.RoleImpl;
import org.ow2.bonita.facade.identity.impl.UserImpl;
import org.ow2.bonita.runtime.event.EventExecutor;
import org.ow2.bonita.search.SearchUtil;
import org.ow2.bonita.type.lob.Lob;

/**
 * @author Guillaume Porcher
 * Helper class to initialize DB tables from the environment.
 */
public final class DbTool {

  static final Logger LOG = Logger.getLogger(DbTool.class.getName());

  private DbTool() { }

  protected static Configuration getConfiguration(final String domain, final String configurationName) throws Exception {
    final EnvironmentFactory envFactory = GlobalEnvironmentFactory.getEnvironmentFactory(domain);
    if (envFactory == null) {
      String message = ExceptionManager.getInstance().getFullMessage("bh_DBT_1");
      throw new BonitaRuntimeException(message);
    }
    final Configuration config = (Configuration) envFactory.get(configurationName);
    if (config == null) {
      String message = ExceptionManager.getInstance().getFullMessage("bh_DBT_2", configurationName);
      throw new BonitaRuntimeException(message);
    }
    return config;
  }

  protected static SchemaExport getSchemaExport(final Configuration config) {
    return new SchemaExport(config, config.buildSettings());
  }

  /**
   * Export DB schema to the database defined in the environment.
   */
  public static void recreateDb(final String domain, final String configurationName) throws Exception {
    BonitaConstants.getBonitaHomeFolder();
    final Configuration config = getConfiguration(domain, configurationName);

    if (LOG.isLoggable(Level.FINE)) {
      final SessionFactoryImplementor sessionFactory = getSessionFactory(domain, configurationName.replaceAll("-configuration", "-session-factory"));
      if (sessionFactory != null) {
        final Dialect dialect = sessionFactory.getDialect();
        String[] script = config.generateSchemaCreationScript(dialect);
        for (String s : script) {
          LOG.fine(s); 
        }
      }
    }

    final SchemaExport schemaExport = getSchemaExport(config);
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Creating schema...");
    }

    final String dbVendor = getDatabaseVendor(config);
    StringBuilder fileNamebuilder = new StringBuilder("/script/post-initdb-").append(dbVendor).append(".sql");
    schemaExport.setImportFile(fileNamebuilder.toString());
    schemaExport.create(true, true);
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Schema created.");
      LOG.fine("Adding default users...");
    }
    if (EnvConstants.HB_CONFIG_CORE.equals(configurationName)) {
      addDefaultUsers(domain, configurationName);
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("Default users added.");
      }
    }
  }

  protected static SessionFactoryImplementor getSessionFactory(final String domain, final String sessionFactoryName) throws Exception {
    final SessionFactory sessionFactory = (SessionFactory) GlobalEnvironmentFactory.getEnvironmentFactory(domain).get(sessionFactoryName);
    if (sessionFactory != null && sessionFactory instanceof SessionFactoryImplementor) {
      return (SessionFactoryImplementor) sessionFactory;
    }
    return null;
  }

  public static void addDefaultUsers(final String domain, final String configurationName) throws Exception {
    final SessionFactoryImplementor sessionFactory = getSessionFactory(domain, configurationName.replaceAll("-configuration", "-session-factory"));
    if (sessionFactory != null) {
      Session session = null;
      try {
        session = sessionFactory.openSession();
        final Transaction tx = session.beginTransaction();

        final RoleImpl memberRole = createDefaultRole(session, IdentityAPI.USER_ROLE_NAME, IdentityAPI.USER_ROLE_LABEL,
            IdentityAPI.USER_ROLE_DESCRIPTION);
        final RoleImpl adminRole = createDefaultRole(session, IdentityAPI.ADMIN_ROLE_NAME, IdentityAPI.ADMIN_ROLE_LABEL,
            IdentityAPI.ADMIN_ROLE_DESCRIPTION);

        final GroupImpl defaultGroup = createDefaultGroup(session, IdentityAPI.DEFAULT_GROUP_NAME, IdentityAPI.DEFAULT_GROUP_LABEL, IdentityAPI.DEFAULT_GROUP_DESCRIPTION, null);

        final MembershipImpl memberMembership = createDefaultMembership(session, defaultGroup, memberRole);
        final Set<Membership> memberMemberships = new HashSet<Membership>();
        memberMemberships.add(memberMembership);
        final MembershipImpl adminMembership = createDefaultMembership(session, defaultGroup, adminRole);
        final Set<Membership> adminMemberships = new HashSet<Membership>();
        adminMemberships.add(adminMembership);

        addDefaultUser(session, "admin", null, null, "bpm", null, null, adminMemberships);
        final UserImpl user1 = addDefaultUser(session, "john", "John", "Doe", "bpm", null, null, memberMemberships);
        final UserImpl user2 = addDefaultUser(session, "jack", "Jack", "Doe", "bpm", user1.getUUID(), user1.getUUID(), memberMemberships);
        addDefaultUser(session, "james", "James", "Doe", "bpm", user1.getUUID(), user2.getUUID(), memberMemberships);

        final MetaDataImpl defaultUsersAddedMetadata = new MetaDataImpl("DEFAULT_USERS_CREATED", "true");

        session.save(defaultUsersAddedMetadata);
        tx.commit();
      } catch (HibernateException e) {
        LOG.log(Level.WARNING, "Unable to add the default users in the DB", e);
      } finally {
        if (session != null) {
          session.close();
        }
      }
    }
  }

  public static MembershipImpl createDefaultMembership(final Session session, final GroupImpl group, final RoleImpl role) throws HibernateException {
    final MembershipImpl membership = new MembershipImpl();
    membership.setGroup(group);
    membership.setRole(role);
    session.save(membership);
    return membership;
  }

  public static GroupImpl createDefaultGroup(final Session session, final String name, final String label, final String description, final Group parentGroup) throws HibernateException {
    final GroupImpl group = new GroupImpl(name);
    group.setLabel(label);
    group.setDescription(description);
    group.setParentGroup(parentGroup);
    session.save(group);
    return group;
  }

  public static RoleImpl createDefaultRole(final Session session, final String name, final String label, final String description) throws HibernateException {
    final RoleImpl role = new RoleImpl(name);
    role.setLabel(label);
    role.setDescription(description);
    session.save(role);
    return role;
  }

  public static UserImpl addDefaultUser(final Session session, final String username, final String firstName, final String lastName, final String password, final String manager, final String delegee, final Set<Membership> memberships) throws HibernateException {
    final UserImpl user = new UserImpl(username, Misc.hash(password));
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setMemberships(memberships);
    user.setManagerUUID(manager);
    user.setDelegeeUUID(delegee);
    session.save(user);
    return user;
  }

  public static void dropDb(final String domain, final String configurationName) throws Exception {
    final EventExecutor eventExecutor = GlobalEnvironmentFactory.getEnvironmentFactory(BonitaConstants.DEFAULT_DOMAIN).get(EventExecutor.class);
    eventExecutor.stop();
    final Configuration configuration = getConfiguration(domain, configurationName);
    final SchemaExport schemaExport = getSchemaExport(configuration);
    schemaExport.drop(false, true);
    final String indexesDirecoryPath = SearchUtil.getIndexesDirectoryPath(configuration);
    if (indexesDirecoryPath != null) {
      Misc.deleteDir(new File(indexesDirecoryPath));
    }
  }

  public static void main(final String[] args) throws Exception {
    if (args == null || args.length < 2) {
      final String message = ExceptionManager.getInstance().getFullMessage(
          "bh_DBT_3", EnvConstants.HB_CONFIG_CORE, EnvConstants.HB_CONFIG_HISTORY);
      throw new IllegalArgumentException(message);
    }
    final String domain = args[0];
    String disableSearch = "bonita.search.use";
    System.setProperty(disableSearch, "false");
    try {
      for (int i = 1 ; i < args.length ; i++) {
        recreateDb(domain, args[i]);
      }
    } finally {
      System.clearProperty(disableSearch);
    }
  }

  public static boolean isOnDb(final String dbName, final Configuration config) {
    final String lowerDbName = dbName.toLowerCase();
    return isOnDatabaseAccordingToDialect(lowerDbName, config) || isOnDatabaseAccordingToURL(lowerDbName, config);
  }

  private static boolean isOnDatabaseAccordingToDialect(final String dbName, final Configuration config) {
    final String dialect = getDbDialect(config);
    if (dialect == null) {
      throw new BonitaRuntimeException("The 'hibernate.dialect' property must be set");
    }
    final String lowerDialectName = dialect.toLowerCase();
    return lowerDialectName.contains(dbName);
  }

  private static String getDatabaseVendor(final Configuration config) {
    if (isOnDb("oracle", config)) {
      return "oracle";
    } else if (isOnDb("mysql", config)) {
      return "mysql";
    } else if (isOnDb("h2", config)) {
      return "h2";
    } else if (isOnDb("postgres", config)) {
      return "postgres";
    } else if (isOnDb("sqlserver", config)) {
      return "sqlserver";
    } else {
      throw new BonitaRuntimeException("Database unknown.");
    }    
  }

  private static boolean isOnDatabaseAccordingToURL(final String lowerDbName, final Configuration config) {
    String url = getDbUrl(config);
    if (url != null) {
      int index = url.indexOf(":/");
      if (index == -1) {
        url = url.split(":")[1];
      } else {
        url = url.substring(0, index);
      }
      url = url.toLowerCase();
    }
    return url != null && url.contains(lowerDbName);
  }

  public static String getDbDialect(final Configuration config) {
    return config.getProperty("hibernate.dialect");
  }

  public static String getDbUrl(final Configuration config) {
    return config.getProperty("hibernate.connection.url");
  }

  public static String getDbUseQueryCache(final Configuration config) {
    return config.getProperty("hibernate.cache.use_query_cache");
  }

  public static String getDbUseSecondLeveleCache(final Configuration config) {
    return config.getProperty("hibernate.cache.use_second_level_cache");
  }

  public static String getDbDriverClass(final Configuration config) {
    return config.getProperty("hibernate.connection.driver_class");
  }

  public static void cleanCache(final String domain, final String sessionFactoryName) throws Exception {
    // clean 2nd level cache:
    final SessionFactoryImplementor sessionFactory = getSessionFactory(domain, sessionFactoryName);
    if (sessionFactory != null) {
      Cache cache = sessionFactory.getCache();
      if (cache != null) {
        cache.evictDefaultQueryRegion();
        cache.evictQueryRegions();
        cache.evictCollectionRegions();
        cache.evictEntityRegions();
      }
    }
  }

  @SuppressWarnings("unchecked")
  public static void updateDatabaseSchema(Configuration configuration) {
    if (isOnDb("mysql", configuration)) {
      LOG.severe("Running on MySQL database, updating schema...");
      final PersistentClass pc = configuration.getClassMapping(Lob.class.getName());
      final Table table = pc.getTable();
      final Iterator<Column> columns = (Iterator<Column>) table.getColumnIterator();
      while (columns.hasNext()) {
        final Column column = columns.next();
        final String columnName = "BLOB_VALUE_";
        if (column.getName().equals(columnName)) {
          LOG.severe("Updating " + columnName + " column...");
          column.setSqlType("LONGBLOB");
          column.setLength(518576);
        }
      }
    } else if (DbTool.isOnDb("oracle", configuration)) {
      LOG.severe("Running on Oracle database, updating schema...");
      final Iterator<Table> tables = (Iterator<Table>) configuration.getTableMappings();
      while (tables.hasNext()) {
        final Table table = tables.next();
        final Iterator<Column> columns = (Iterator<Column>) table.getColumnIterator();
        while (columns.hasNext()) {
          final Column column = columns.next();
          final Value value = column.getValue();
          // Prevent ORA-01754: a table may contain only one column of type LONG
          if (value.getType() instanceof TextType) {
            column.setSqlType("CLOB");
          }
        }
      }
    } /*else if (isOnDb("sybase", config)) {
      LOG.severe("Running on Sybase DB, updating schema...");
      //iterate over all tables and all columns to replace type=text
      final Iterator<Table> tables = (Iterator<Table>) config.getTableMappings();
      while (tables.hasNext()) {
        final Table table = tables.next();
        final Iterator<Column> columns = table.getColumnIterator();
        while (columns.hasNext()) {
          final Column column = columns.next();
          System.err.println("Column.name=" + column.getName() + ", column=" + column.getDefaultValue());
          if (!column.getName().equals("BLOB_VALUE_") && column.getSqlType() != null && column.getSqlType().equals("CLOB")) {
            LOG.severe("Updating " + column.getName() + " column...");
            column.setSqlType("LONGVARCHAR");
          }
        }
        
      }
    } */
  }

}
