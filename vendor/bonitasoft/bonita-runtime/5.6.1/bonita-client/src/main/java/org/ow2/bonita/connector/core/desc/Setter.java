/**
 * Copyright (C) 2009  BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
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
package org.ow2.bonita.connector.core.desc;

/**
 * 
 * @author Matthieu Chaffotte
 *
 */
public class Setter {

  private String setterName;
  private String required;
  private String forbidden;
  private Object[] parameters;

  public Setter(String setterName, String required, String forbidden,
      Object[] hostParameter) {
    super();
    this.setterName = setterName;
    this.required = required;
    this.forbidden = forbidden;
    this.parameters = hostParameter;
  }

  public String getSetterName() {
    return setterName;
  }

  public String getRequired() {
    return required;
  }

  public String getForbidden() {
    return forbidden;
  }

  public Object[] getParameters() {
    return parameters;
  }
}
