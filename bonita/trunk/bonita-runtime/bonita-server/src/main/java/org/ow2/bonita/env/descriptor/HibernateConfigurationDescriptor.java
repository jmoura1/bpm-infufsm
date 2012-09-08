/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.ow2.bonita.env.descriptor;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.JDBCException;
import org.hibernate.cfg.Configuration;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.ConnectionProviderFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.util.JDBCExceptionReporter;
import org.ow2.bonita.env.WireContext;
import org.ow2.bonita.env.WireDefinition;
import org.ow2.bonita.env.WireException;
import org.ow2.bonita.env.operation.Operation;
import org.ow2.bonita.util.BonitaRuntimeException;
import org.ow2.bonita.util.ExceptionManager;
import org.ow2.bonita.util.ReflectUtil;

/**
 * @author Tom Baeyens
 */
public class HibernateConfigurationDescriptor extends AbstractDescriptor {

  private static final long serialVersionUID = 1L;

  private static final Logger LOG = Logger.getLogger(HibernateConfigurationDescriptor.class.getName());

  String className;
  String namingStrategyClassName;
  List<Operation> mappingOperations;
  List<Operation> cacheOperations;
  PropertiesDescriptor propertiesDescriptor;
  private Operation schemaOperation;

  public Object construct(WireContext wireContext) {
    // instantiation of the configuration
    Configuration configuration = null;
    if (className != null) {
      ClassLoader classLoader = wireContext.getClassLoader();
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("instantiating hibernate configation class " + className);
      }
      Class<?> configurationClass = ReflectUtil.loadClass(classLoader,
          className);
      configuration = (Configuration) ReflectUtil
          .newInstance(configurationClass);
    } else {
    	if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("instantiating default hibernate configation");
    	}
      configuration = new Configuration();
    }
    return configuration;
  }

  public void initialize(Object object, WireContext wireContext) {
    Configuration configuration = (Configuration) object;
    apply(mappingOperations, configuration, wireContext);
    apply(cacheOperations, configuration, wireContext);
    if (propertiesDescriptor != null) {
      Properties properties = (Properties) wireContext.create(
          propertiesDescriptor, false);
      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("adding properties to hibernate configuration: " + properties);
      }
      configuration.addProperties(properties);
    }
    if (schemaOperation != null) {
      schemaOperation.apply(configuration, wireContext);
    }
  }

  private void apply(List<Operation> operations, Configuration configuration,
      WireContext wireContext) {
    if (operations != null) {
      for (Operation operation : operations) {
      	if (LOG.isLoggable(Level.FINE)) {
          LOG.fine(operation.toString());
      	}
        operation.apply(configuration, wireContext);
      }
    }
  }

  public Class<?> getType(WireDefinition wireDefinition) {
    if (className != null) {
      try {
        return ReflectUtil
            .loadClass(wireDefinition.getClassLoader(), className);
      } catch (BonitaRuntimeException e) {
      	String message = ExceptionManager.getInstance().getFullMessage(
      			"bp_HCD_1", className, e.getMessage());
        throw new WireException(message, e.getCause());
      }
    }
    return Configuration.class;
  }

  public void addMappingOperation(Operation operation) {
    if (mappingOperations == null) {
      mappingOperations = new ArrayList<Operation>();
    }
    mappingOperations.add(operation);
  }

  public void addCacheOperation(Operation operation) {
    if (cacheOperations == null) {
      cacheOperations = new ArrayList<Operation>();
    }
    cacheOperations.add(operation);
  }

  // operations ///////////////////////////////////////////////////////////////

  public static class AddResource implements Operation {
    private static final long serialVersionUID = 1L;
    String resource;

    public AddResource(String resource) {
      this.resource = resource;
    }

    public void apply(Object target, WireContext wireContext) {
      Configuration configuration = (Configuration) target;
      configuration.addResource(resource, wireContext.getClassLoader());
    }

    public String toString() {
      return "adding mapping resource " + resource
          + " to hibernate configuration";
    }
  }

  public static class AddFile implements Operation {
    private static final long serialVersionUID = 1L;
    String fileName;

    public AddFile(String fileName) {
      this.fileName = fileName;
    }

    public void apply(Object target, WireContext wireContext) {
      Configuration configuration = (Configuration) target;
      configuration.addFile(fileName);
    }

    public String toString() {
      return "adding hibernate mapping file " + fileName + " to configuration";
    }
  }

  public static class AddClass implements Operation {
    private static final long serialVersionUID = 1L;
    String className;

    public AddClass(String className) {
      this.className = className;
    }

    public void apply(Object target, WireContext wireContext) {
      Configuration configuration = (Configuration) target;
      try {
        Class<?> persistentClass = wireContext.getClassLoader().loadClass(
            className);
        configuration.addClass(persistentClass);
      } catch (Exception e) {
      	String message = ExceptionManager.getInstance().getFullMessage("bp_HCD_2", className);
        throw new BonitaRuntimeException(message, e);
      }
    }

    public String toString() {
      return "adding persistent class " + className
          + " to hibernate configuration";
    }
  }

  public static class AddUrl implements Operation {
    private static final long serialVersionUID = 1L;
    String url;

    public AddUrl(String url) {
      this.url = url;
    }

    public void apply(Object target, WireContext wireContext) {
      Configuration configuration = (Configuration) target;
      try {
        configuration.addURL(new URL(url));
      } catch (Exception e) {
      	String message = ExceptionManager.getInstance().getFullMessage("bp_HCD_3", url);
        throw new BonitaRuntimeException(message, e);
      }
    }
  }

  public static class SetCacheConcurrencyStrategy implements Operation {
    private static final long serialVersionUID = 1L;
    String className;
    String concurrencyStrategy;

    public SetCacheConcurrencyStrategy(String className,
        String concurrencyStrategy) {
      this.className = className;
      this.concurrencyStrategy = concurrencyStrategy;
    }

    public void apply(Object target, WireContext wireContext) {
      Configuration configuration = (Configuration) target;
      configuration.setCacheConcurrencyStrategy(className, concurrencyStrategy);
    }

    public String toString() {
      return "setting cache concurrency strategy on class " + className
          + " to " + concurrencyStrategy + " on hibernate configuration";
    }
  }

  public static class SetCollectionCacheConcurrencyStrategy implements
      Operation {
    private static final long serialVersionUID = 1L;
    String collection;
    String concurrencyStrategy;

    public SetCollectionCacheConcurrencyStrategy(String collection,
        String concurrencyStrategy) {
      this.collection = collection;
      this.concurrencyStrategy = concurrencyStrategy;
    }

    public void apply(Object target, WireContext wireContext) {
      Configuration configuration = (Configuration) target;
      configuration.setCollectionCacheConcurrencyStrategy(collection,
          concurrencyStrategy);
    }

    public String toString() {
      return "setting cache concurrency strategy on collection " + collection
          + " to " + concurrencyStrategy + " on hibernate configuration";
    }
  }

  public static class CreateSchema implements Operation {

    private static final long serialVersionUID = 1L;

    /** The sole instance of this class */
    private static final Operation instance = new CreateSchema();

    private CreateSchema() {
      // suppress default constructor, ensuring non-instantiability
    }

    public void apply(Object target, WireContext wireContext) {
      Configuration configuration = (Configuration) target;
      Properties cfgProperties = configuration.getProperties();
      Dialect dialect = Dialect.getDialect(cfgProperties);
      ConnectionProvider connectionProvider = ConnectionProviderFactory
          .newConnectionProvider(cfgProperties);
      try {
        Connection connection = connectionProvider.getConnection();
        try {
        	if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("dropping db schema");
        	}
          String[] dropScript = configuration.generateDropSchemaScript(dialect);
          executeScript(connection, dropScript);
          if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("creating db schema");
          }
          String[] createScript = configuration
              .generateSchemaCreationScript(dialect);
          executeScript(connection, createScript);
        } finally {
          connectionProvider.closeConnection(connection);
        }
      } catch (SQLException e) {
      	String message = ExceptionManager.getInstance().getFullMessage("bp_HCD_4");
        throw new JDBCException(message, e);
      } finally {
        connectionProvider.close();
      }
    }

    /** Returns the sole instance of this class */
    public static Operation getInstance() {
      return instance;
    }
  }

  public static class UpdateSchema implements Operation {

    private static final long serialVersionUID = 1L;

    private static final Operation instance = new UpdateSchema();

    private UpdateSchema() {
      // suppress default constructor, ensuring non-instantiability
    }

    public void apply(Object target, WireContext wireContext) {
      Configuration configuration = (Configuration) target;
      Properties cfgProperties = configuration.getProperties();
      Dialect dialect = Dialect.getDialect(cfgProperties);
      ConnectionProvider connectionProvider = ConnectionProviderFactory
          .newConnectionProvider(cfgProperties);
      try {
        Connection connection = connectionProvider.getConnection();
        try {
          DatabaseMetadata metadata = new DatabaseMetadata(connection, dialect);
          String[] updateScript = configuration.generateSchemaUpdateScript(
              dialect, metadata);
          if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("updating db schema");
          }
          executeScript(connection, updateScript);
        } finally {
          connectionProvider.closeConnection(connection);
        }
      } catch (SQLException e) {
      	String message = ExceptionManager.getInstance().getFullMessage("bp_HCD_5");
        throw new JDBCException(message, e);
      } finally {
        connectionProvider.close();
      }
    }

    public static Operation getInstance() {
      return instance;
    }
  }

  private static List<SQLException> executeScript(Connection connection,
      String[] script) throws SQLException {
    List<SQLException> exceptions = Collections.emptyList();
    Statement statement = connection.createStatement();
    try {
      for (String line : script) {
      	if (LOG.isLoggable(Level.FINE)) {
          LOG.fine(line);
      	}
        try {
          statement.executeUpdate(line);
          if (statement.getWarnings() != null) {
            JDBCExceptionReporter.logAndClearWarnings(connection);
          }
        } catch (SQLException e) {
          if (exceptions.isEmpty()) {
            exceptions = new ArrayList<SQLException>();
          }
          exceptions.add(e);
        }
      }
    } finally {
      statement.close();
    }
    return exceptions;
  }

  // getters and setters //////////////////////////////////////////////////////

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public PropertiesDescriptor getPropertiesDescriptor() {
    return propertiesDescriptor;
  }

  public void setPropertiesDescriptor(PropertiesDescriptor propertiesDescriptor) {
    this.propertiesDescriptor = propertiesDescriptor;
  }

  public String getNamingStrategyClassName() {
    return namingStrategyClassName;
  }

  public void setNamingStrategyClassName(String namingStrategyClassName) {
    this.namingStrategyClassName = namingStrategyClassName;
  }

  public Operation getSchemaOperation() {
    return schemaOperation;
  }

  public void setSchemaOperation(Operation schemaOperation) {
    this.schemaOperation = schemaOperation;
  }
}
