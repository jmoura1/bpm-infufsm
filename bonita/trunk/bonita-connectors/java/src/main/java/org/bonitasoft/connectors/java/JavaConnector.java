/**
 * Copyright (C) 2009 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.connectors.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ow2.bonita.connector.core.Connector;
import org.ow2.bonita.connector.core.ConnectorError;

/**
 * 
 * @author Matthieu Chaffotte
 *
 */
public class JavaConnector extends Connector {
  private String className;
  private Map<String, Object> fields;
  private List<Object> constructorParameters;
  private List<MethodCall> methods;

  public JavaConnector() {
    fields = new HashMap<String, Object>();
    methods = new ArrayList<MethodCall>();
  }

  public String getClassName() {
    return className;
  }

  public List<Object> getConstructorParameters() {
    return constructorParameters;
  }

  public Map<String, Object> getFields() {
    return fields;
  }

  public List<MethodCall> getMethods() {
    return methods;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public void setConstructorParameters(List<Object> constructorParamerters) {
    this.constructorParameters = constructorParamerters;
  }

  public void setFields(Map<String, Object> fields) {
    this.fields = fields;
  }

  public void setMethods(List<List<Object>> methods) {
    List<MethodCall> result = null;
    if (methods != null) {
      result = new ArrayList<MethodCall>();
      for (List<Object> list : methods) {
        List<Object> temp = null;
        if (list.size() > 1) {
          temp = list.subList(1, list.size());
        }
        MethodCall call = new MethodCall((String)list.get(0), temp);
        result.add(call);
      }
    }
    this.methods = result;
  }

  @Override
  protected void executeConnector() throws Exception {
    Class<?> connectorClass = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
    Object connector = null;
    if (constructorParameters != null) {
      Class<?>[] constructorParametersClasses = new Class[constructorParameters.size()];
      for (int i = 0; i < constructorParameters.size(); i++) {
        constructorParametersClasses[i] = constructorParameters.get(i).getClass();
      }
      Constructor<?> constructor = connectorClass.getDeclaredConstructor(constructorParametersClasses);
      constructor.setAccessible(true);
      connector = constructor.newInstance(constructorParameters.toArray());
    } else {
      Constructor<?>[] constructors = connectorClass.getConstructors();
      if (constructors != null && constructors.length > 0) {
        connector = connectorClass.newInstance();
      }
    }

    if (connector != null) {
      if (fields != null) {
        for (Entry<String, Object> field : fields.entrySet()) {
          String fieldName = field.getKey();
          Object fieldValue = field.getValue();
          Field f = searchField(connectorClass, fieldName);
          f.setAccessible(true);
          f.set(connector, fieldValue);
        }
      }
    }

    for (MethodCall method : methods) {
      Class<?>[] parametersClass = null;
      Object[] parametersObjet = null;
      if (method.getParameters() != null) {
        List<Object> parameters = method.getParameters();
        parametersClass = new Class[parameters.size()];
        parametersObjet = parameters.toArray();
        for (int i = 0; i < parameters.size(); i++) {
          if (parameters.get(i) == null) {
            parametersClass[i] = null;
          } else {
            parametersClass[i] = parameters.get(i).getClass();
          }
        }
      } else {
        parametersClass = new Class[0];
        parametersObjet = new Object[0];
      }
      Method setter = searchMethod(connectorClass, method.getMethodName(), parametersClass);
      setter.invoke(connector, parametersObjet);
    }
  }

  @Override
  protected List<ConnectorError> validateValues() {
    List<ConnectorError> errors = new ArrayList<ConnectorError>();
    if (className.length() == 0) {
      errors.add(new ConnectorError("className",
          new IllegalArgumentException("is empty")));
    } else if (!classExists()) {
      errors.add(new ConnectorError("className",
          new IllegalArgumentException("is not a real class")));
    } else {
      if (fields != null) {
        for (Entry<String, Object> field : fields.entrySet()) {
          String fieldName = field.getKey();
          if ("".equals(fieldName.trim())) {
            errors.add(new ConnectorError("fields",
                new IllegalArgumentException("A field is empty")));
          } else if (!fieldExists(field.getKey())) {
            errors.add(new ConnectorError("fields",
                new IllegalArgumentException(fieldName + " does not exist in " + className)));
          }
          //check values
        }
      }
      
      if (constructorParameters != null) {
        Class<?>[] constructorParametersClasses = new Class[constructorParameters.size()];
        for (int i = 0; i < constructorParameters.size(); i++) {
          if (constructorParameters.get(i) == null) {
            constructorParametersClasses[i] = null;
          } else {
            constructorParametersClasses[i] = constructorParameters.get(i).getClass();
          }
        }
        if (!constructorExists(constructorParametersClasses)) {
          errors.add(new ConnectorError("constructor",
              new IllegalArgumentException("The constructor using " + constructorParametersClasses + " does not exist")));
        }
        
      } else if (!constructorExists(null)) {
        errors.add(new ConnectorError("constructor",
            new IllegalArgumentException("The default constructor does not exist")));
      }
      
      for (MethodCall method : methods) {
        String methodName = method.getMethodName();
        if (methodName == null) {
          errors.add(new ConnectorError("methods",
              new IllegalArgumentException("A method name is null")));
        } else if ("".equals(methodName.trim())) {
          errors.add(new ConnectorError("methods",
              new IllegalArgumentException("A method name is empty")));
        } else {
          List<Object> parameters = method.getParameters();
          Class<?>[] parametersClass = null;
          if (parameters != null) {
            parametersClass = new Class[parameters.size()];
            for (int i = 0; i < parameters.size(); i++) {
              if (parameters.get(i) == null) {
                parametersClass[i] = null;
              } else {
                parametersClass[i] = parameters.get(i).getClass();
              }
            }
          }
          if (!methodExists(methodName, parametersClass)) {
            errors.add(new ConnectorError("methods",
                new IllegalArgumentException(methodName + " does no exist using parameters " + parameters)));
          }
        }
      }
    }
    return errors;
  }

  private boolean constructorExists(Class<?>[] parameters) {
    try {
      Class<?> connectorClass = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
      connectorClass.getDeclaredConstructor(parameters);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private boolean fieldExists(String fieldName) {
    try {
      Class<?> connectorClass = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
      Field f = searchField(connectorClass, fieldName);
      if (f != null) {
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      return false;
    }
  }

  private boolean methodExists(String methodName, Class<?>[] parameters) {
    try {
      Class<?> connectorClass = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
      if (parameters == null) {
        parameters = new Class[0];
      }
      Method method = searchMethod(connectorClass, methodName, parameters);
      if (method != null) {
        return true;
      } else {
        return false; 
      }
    } catch (Exception e) {
      return false;
    }
  }

  private boolean classExists() {
    try {
      Class.forName(className, true, Thread.currentThread().getContextClassLoader());
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static boolean ArraysEqual(Class<?>[] one, Class<?>[] two) {
    boolean equals = false;
    if(one != null && two != null && one.length == two.length) {
      for (int i = 0; i < one.length; i++) {
        if (one[i] != two[i]) {
          break;
        }
      }
      equals = true;
    }    
    return equals;
  }

  private static Method searchMethod(final Class<?> c, final String methodName, final Class<?>[] parameters) {
    Method method = null;
    if (c != null) {
      int i = 0;
      Method[] methods = c.getDeclaredMethods();
      while (i < methods.length && method == null) {
        if (methods[i].getName().equals(methodName)) {
          Method m = methods[i];
          m.setAccessible(true);
          Class<?>[] mParams = m.getParameterTypes();
          Class<?>[] cParams = parameters;
          if (ArraysEqual(mParams, cParams)) {
            method = m;
          }
        }
        i++;
      }
      // let us check if the field is in the upper class
      if (method == null) {
        return searchMethod(c.getSuperclass(), methodName, parameters);
      }
    }
    return method;
  }

  private static Field searchField(final Class<?> c, final String fieldName) {
    Field field = null;
    if (c != null) {
      int i = 0;
      Field[] fields = c.getDeclaredFields();
      while (i < fields.length && field == null) {
        if (fields[i].getName().equals(fieldName)) {
          field = fields[i];
        }
        i++;
      }
      // let us check if the field is in the upper class
      if (field == null) {
        return searchField(c.getSuperclass(), fieldName);
      }
    }
    return field;
  }
}
