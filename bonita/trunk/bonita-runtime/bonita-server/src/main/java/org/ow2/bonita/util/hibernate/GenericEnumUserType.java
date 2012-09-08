/**
 * Copyright (C) 2006  Bull S. A. S.
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
 **/
package org.ow2.bonita.util.hibernate;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.type.StringType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.ow2.bonita.util.ExceptionManager;

/**
 * @author Marc Blachon, Guillaume Porcher, Charles Souillard, Miguel Valdes, Pierre Vigneras
 */
public class GenericEnumUserType implements UserType, ParameterizedType, Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -1330133661718663399L;

  private static final String DEFAULT_IDENTIFIER_METHOD_NAME = "name";

  private static final String DEFAULT_VALUE_OF_METHOD_NAME = "valueOf";

  @SuppressWarnings("rawtypes")
  private Class< ? extends Enum> enumClass;

  private Class< ? > identifierType;

  private Method identifierMethod;

  private Method valueOfMethod;

  private StringType type = new StringType();

  private int[] sqlTypes;

  public void setParameterValues(Properties parameters) {
    String enumClassName = parameters.getProperty("enumClass");
    try {
      enumClass = Class.forName(enumClassName).asSubclass(Enum.class);
    } catch (ClassNotFoundException cfne) {
    	String message = ExceptionManager.getInstance().getFullMessage("bh_GEUT_1");
      throw new HibernateException(message, cfne);
    }

    String identifierMethodName = parameters.getProperty("identifierMethod", DEFAULT_IDENTIFIER_METHOD_NAME);

    try {
      identifierMethod = enumClass .getMethod(identifierMethodName, new Class[0]);
      identifierType = identifierMethod.getReturnType();
    } catch (Exception e) {
    	String message = ExceptionManager.getInstance().getFullMessage("bh_GEUT_2");
      throw new HibernateException(message, e);
    }

    sqlTypes = new int[] { type.sqlType() };

    String valueOfMethodName = parameters.getProperty("valueOfMethod",
        DEFAULT_VALUE_OF_METHOD_NAME);

    try {
      valueOfMethod = enumClass.getMethod(valueOfMethodName,
          new Class[] { identifierType });
    } catch (Exception e) {
    	String message = ExceptionManager.getInstance().getFullMessage("bh_GEUT_4");
      throw new HibernateException(message, e);
    }
  }

  public Class< ? > returnedClass() {
    return enumClass;
  }

  public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
    throws SQLException {
    Object identifier = type.get(rs, names[0]);
    if (identifier == null) {
      return null;
    }

    try {
      return valueOfMethod.invoke(enumClass, new Object[] { identifier });
    } catch (Exception e) {
    	String message = ExceptionManager.getInstance().getFullMessage(
    			"bh_GEUT_5", valueOfMethod.getName(), enumClass);
      throw new HibernateException(message, e);
    }
  }

  public void nullSafeSet(PreparedStatement st, Object value, int index)
    throws SQLException {
    try {
      if (value == null) {
        st.setNull(index, type.sqlType());
      } else {
        String identifier = (String) identifierMethod.invoke(value, new Object[0]);
        type.set(st, identifier, index);
      }
    } catch (Exception e) {
    	String message = ExceptionManager.getInstance().getFullMessage(
    			"bh_GEUT_6", identifierMethod.getName(), enumClass);
      throw new HibernateException(message, e);
    }
  }

  public int[] sqlTypes() {
    return sqlTypes;
  }

  public Object assemble(Serializable cached, Object owner) {
    return cached;
  }

  public Object deepCopy(Object value) {
    return value;
  }

  public Serializable disassemble(Object value) {
    return (Serializable) value;
  }

  public boolean equals(Object x, Object y) {
    return x == y;
  }

  public int hashCode(Object x) {
    return x.hashCode();
  }

  public boolean isMutable() {
    return false;
  }

  public Object replace(Object original, Object target, Object owner) {
    return original;
  }
}
