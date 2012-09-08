/**
 * Copyright (C) 2009  Bull S. A. S.
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
package org.ow2.bonita.connector.core;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.ow2.bonita.connector.core.desc.Category;
import org.ow2.bonita.util.Misc;

/**
 * 
 * @author Matthieu Chaffotte
 *
 */
public class ConnectorAPI {

  protected final static String OTHER_CATEGORY = "Other";
  protected final static String OTHER_ICON = "org/ow2/bonita/connector/core/other.png";
  public static final Category other = new Category(OTHER_CATEGORY, OTHER_ICON, Thread.currentThread().getContextClassLoader());

  private List<ConnectorDescription> connectors;
  private Locale currentLocale;
  private ClassLoader classLoader;
  private Collection<ConnectorException> exceptions;

  public ConnectorAPI(ClassLoader classLoader, List<String> classNames) throws Exception {
    this(classLoader, classNames, Locale.getDefault());
  }

  public ConnectorAPI(ClassLoader classLoader, List<String> classNames, Locale locale) throws Exception {
    Misc.checkArgsNotNull(classLoader, classNames, locale);
    this.currentLocale = locale;
    this.connectors = new ArrayList<ConnectorDescription>();
    this.classLoader = classLoader;
    this.exceptions = new ArrayList<ConnectorException>();
    getConnectors(classNames);
  }

  private void getConnectors(List<String> classNames) throws Exception {
    for (String className : classNames) {
      Class<?> javaClass = getClass(className);
      if (javaClass != null) {
        addClass(javaClass);
      }
    }
  }

  private Class<?> getClass(final String className) {
    try {
      return classLoader.loadClass(className);
    } catch (Throwable e) {
      ConnectorException exception = new ConnectorException(e, "Cannot load " + className, null);
      exceptions.add(exception);
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private void addClass(Class<?> javaClass) {
    try {
      if (isAnInstanceOfConnector(javaClass)) {
        ConnectorDescription temp = new ConnectorDescription((Class<? extends Connector>)javaClass, currentLocale);
        String id = temp.getId();
        if (idExists(id)) {
          exceptions.add(new ConnectorException("Cannot load " + javaClass.getName() + " because a similar id exists",
              id, javaClass.getName(), null));
        }
        connectors.add(temp);
      }
    } catch (ConnectorException e) {
      exceptions.add(e);
    } catch (Throwable e) {
      String className = "null";
      if (javaClass != null) {
        className = javaClass.getName();
      }
      ConnectorException exception = new ConnectorException(e, className, null);
      exceptions.add(exception);
    }
  }

  private boolean idExists(String id) {
    boolean exists = false;
    for (ConnectorDescription connector : connectors) {
      if (id.equals(connector.getId())) {
        exists = true;
        break;
      }
    }
    return exists;
  }

  public List<ConnectorDescription> getAllConnectors() {
    return connectors;
  }

  public List<ConnectorDescription> getJavaConnectors() {
    List<ConnectorDescription> javaconnectors = new ArrayList<ConnectorDescription>();
    for (ConnectorDescription connector : connectors) {
      Class<? extends Connector> clazz = connector.getConnectorClass();
      if (isAConnector(clazz) && !isMapper(clazz) && !isMultiInstantiator(clazz)
          && !isInstantiator(clazz) && !isJoinChecker(clazz)) {
        javaconnectors.add(connector);
      }
    }
    return javaconnectors;
  }

  public List<ConnectorDescription> getRoleResolvers() {
    List<ConnectorDescription> roleResolvers = new ArrayList<ConnectorDescription>();
    for (ConnectorDescription connector : connectors) {
      Class<? extends Connector> clazz = connector.getConnectorClass();
      if (isRoleResolver(clazz)) {
        roleResolvers.add(connector);
      }
    }
    return roleResolvers;
  }

  public List<ConnectorDescription> getFilters() {
    List<ConnectorDescription> filters = new ArrayList<ConnectorDescription>();
    for (ConnectorDescription connector : connectors) {
      Class<? extends Connector> clazz = connector.getConnectorClass();
      if (isFilter(clazz)) {
        filters.add(connector);
      }
    }
    return filters;
  }

  @Deprecated
  public List<ConnectorDescription> getMultiInstantiators() {
    List<ConnectorDescription> multis = new ArrayList<ConnectorDescription>();
    for (ConnectorDescription connector : connectors) {
      Class<? extends Connector> clazz = connector.getConnectorClass();
      if (isMultiInstantiator(clazz)) {
        multis.add(connector);
      }
    }
    return multis;
  }

  public List<ConnectorDescription> getInstantiators() {
    List<ConnectorDescription> instantiators = new ArrayList<ConnectorDescription>();
    for (ConnectorDescription connector : connectors) {
      Class<? extends Connector> clazz = connector.getConnectorClass();
      if (isInstantiator(clazz)) {
        instantiators.add(connector);
      }
    }
    return instantiators;
  }

  public List<ConnectorDescription> getJoinCheckers() {
    List<ConnectorDescription> joinCheckers = new ArrayList<ConnectorDescription>();
    for (ConnectorDescription connector : connectors) {
      Class<? extends Connector> clazz = connector.getConnectorClass();
      if (isJoinChecker(clazz)) {
        joinCheckers.add(connector);
      }
    }
    return joinCheckers;
  }

  private boolean isJoinChecker(Class<?> clazz) {
    try {
      Object connector = clazz.newInstance();
      return connector instanceof MultipleInstancesJoinChecker;
    } catch (Throwable e) {
      return false;
    }
  }
  
  public List<ConnectorDescription> getAllConnectors(String categoryName) {
    return getConnectors(connectors, categoryName);
  }

  public List<ConnectorDescription> getJavaConnectors(String categoryName) {
    List<ConnectorDescription> javaConnectors = getJavaConnectors();
    return getConnectors(javaConnectors, categoryName);
  }

  public List<ConnectorDescription> getRoleResolverConnectors(String categoryName) {
    List<ConnectorDescription> roleMappers = getRoleResolvers();
    return getConnectors(roleMappers, categoryName);
  }

  public List<ConnectorDescription> getFilterConnectors(String categoryName) {
    List<ConnectorDescription> mappers = getFilters();
    return getConnectors(mappers, categoryName);
  }

  @Deprecated
  public List<ConnectorDescription> getMultiInstantiatorConnectors(String categoryName) {
    List<ConnectorDescription> multi = getMultiInstantiators();
    return getConnectors(multi, categoryName);
  }

  public List<ConnectorDescription> getInstantiatorConnectors(String categoryName) {
    List<ConnectorDescription> multi = getInstantiators();
    return getConnectors(multi, categoryName);
  }

  public List<ConnectorDescription> getJoinCheckerConnectors(String categoryName) {
    List<ConnectorDescription> multi = getJoinCheckers();
    return getConnectors(multi, categoryName);
  }

  private List<ConnectorDescription> getConnectors(List<ConnectorDescription> list, String categoryName) {
    List<ConnectorDescription> connectors = new ArrayList<ConnectorDescription>();
    for (ConnectorDescription connector : list) {
      List<Category> categories = connector.getCategories();
      for (Category category : categories) {
        String categ = getPropertyValue(connector.getConnectorClass(), category.getName());
        if (categ == null) {
          categ = category.getName();
        }
        if (categoryName.equals(categ)) {
          connectors.add(connector);
        } 
      }
    }
    return connectors;
  }

  public ConnectorDescription getConnector(String id) {
    return getConnector(connectors, id);
  }

  public ConnectorDescription getRoleResolverConnector(String id) {
    List<ConnectorDescription> roleMappers = getRoleResolvers();
    return getConnector(roleMappers, id);
  }

  public ConnectorDescription getJavaConnector(String id) {
    List<ConnectorDescription> javaConnectors = getJavaConnectors();
    return getConnector(javaConnectors, id);
  }

  public ConnectorDescription getFilterConnector(String id) {
    List<ConnectorDescription> mappers = getFilters();
    return getConnector(mappers, id);
  }

  private ConnectorDescription getConnector(List<ConnectorDescription> list, String id) {
    ConnectorDescription desc = null;
    int size = list.size();
    int i = 0;
    boolean found = false;
    while (i < size && !found) {
      ConnectorDescription temp = list.get(i);
      if (id.equals(temp.getId())) {
        desc = temp;
        found = true;
      }
      i++;
    }
    if (!found) {
      throw new IllegalArgumentException("The identifier " + id + " does not refer to a Connector");
    }
    return desc;
  }

  public List<Category> getAllCategories() {
    return getCategories(connectors);
  }

  public List<Category> getJavaConnectorsCategories() {
    List<ConnectorDescription> javaConnectors = getJavaConnectors();
    return getCategories(javaConnectors);
  }

  public List<Category> getRoleResolversCategories() {
    List<ConnectorDescription> roleMappers = getRoleResolvers();
    return getCategories(roleMappers);
  }

  public List<Category> getFiltersCategories() {
    List<ConnectorDescription> mappers = getFilters();
    return getCategories(mappers);
  }

  @Deprecated
  public List<Category> getMulitInstantiatorCategories() {
    List<ConnectorDescription> multis = getMultiInstantiators();
    return getCategories(multis);
  }

  public List<Category> getInstantiatorCategories() {
    List<ConnectorDescription> multis = getInstantiators();
    return getCategories(multis);
  }

  public List<Category> getJoinCheckerCategories() {
    List<ConnectorDescription> multis = getJoinCheckers();
    return getCategories(multis);
  }

  private Map<String, String> getCategoriesMap(List<ConnectorDescription> list) {
    Map<String, String> categories = new HashMap<String, String>();
    for (ConnectorDescription connector : list) {
      List<Category> connectorCategories = connector.getCategories();
      if (connectorCategories.isEmpty()) {
        connectorCategories.add(other);
      }
      for (Category category : connectorCategories) {
        String categoryId = category.getName();
        String categoryName = connector.getCategoryName(categoryId);
        if (categoryName == null) {
          categoryName = categoryId;
        }
        if (!categories.containsKey(categoryName)) {
          categories.put(categoryName, category.getIconPath());
        } else {
          String icon = categories.get(categoryName);
          InputStream categoryIcon = category.getIcon();
          if (icon == null && categoryIcon != null) {
            categories.put(categoryName, category.getIconPath());
          }
        }
      }
    }
    return categories;
  }

  private List<Category> getCategories(List<ConnectorDescription> list) {
    Map<String, String> categoriesMap = getCategoriesMap(list);
    Set<String> categoryNames = categoriesMap.keySet();
    List<String> cat = new ArrayList<String>(categoryNames);
    Collections.sort(cat);
    List<Category> categories = new ArrayList<Category>();
    for (String categoryName : cat) {
      String icon = categoriesMap.get(categoryName);
      Category category = new Category(categoryName, icon, classLoader);
      categories.add(category);
    }
    return categories;
  }

  private String getPropertyValue(Class<? extends Connector> connectorClass, String property) {
    try {
      ResourceBundle bundle =
        ResourceBundle.getBundle(connectorClass.getName(), currentLocale, connectorClass.getClassLoader());
      return bundle.getString(property);
    } catch (Exception e) {
      return null;
    }
  }

  private boolean isAnInstanceOfConnector(Class<?> c) {
    if (Modifier.isAbstract(c.getModifiers()) || c.equals(Connector.class)) {
      return false;
    }
    return isAConnector(c);
  }

  private boolean isAssignable(Class<?> clazz, Class<?> assignableClass) {
    if (assignableClass.equals(clazz)) {
      return true;
    }
    final Class<?> superClass = clazz.getSuperclass();
    if (superClass != null) {
      return isAssignable(superClass, assignableClass);
    }
    return false;
  }

  private boolean isAConnector(Class<?> c) {
    return isAssignable(c, Connector.class);
  }

  private boolean isMapper(Class<?> clazz) {
    return isAssignable(clazz, Mapper.class);
  }

  private boolean isFilter(Class<?> clazz) {
    return isAssignable(clazz, Filter.class);
  }

  private boolean isRoleResolver(Class<?> clazz) {
    return isAssignable(clazz, RoleResolver.class);
  }

  private boolean isMultiInstantiator(Class<?> clazz) {
    return isAssignable(clazz, MultiInstantiator.class);
  }

  private boolean isInstantiator(Class<?> clazz) {
    return isAssignable(clazz, MultipleInstancesInstantiator.class);
  }

  public void setCurrentLocale(Locale currentLocale) {
    this.currentLocale = currentLocale;
  }

  public Locale getCurrentLocale() {
    return currentLocale;
  }

  public Collection<ConnectorException> getExcpetions() {
    return exceptions;
  }

}
