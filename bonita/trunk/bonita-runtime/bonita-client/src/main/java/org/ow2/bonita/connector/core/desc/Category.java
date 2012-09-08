/**
 * Copyright (C) 2009  BonitaSoft S.A..
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

import java.io.InputStream;

/**
 * 
 * @author Matthieu Chaffotte
 *
 */
public class Category {

  private String name;
  private String icon;
  private ClassLoader classLoader;

  public Category(String name, String icon, ClassLoader classLoader) {
    this.name = name;
    this.icon = icon;
    this.classLoader = classLoader;
  }

  public String getName() {
    return name;
  }

  public String getIconPath() {
    return icon;
  }

  public InputStream getIcon() {
    if (icon != null && !"".equals(icon.trim())) {
      ClassLoader loader = classLoader;
      if (loader == null) {
        loader = Thread.currentThread().getContextClassLoader();
      }
      return loader.getResourceAsStream(icon);
    }
    return null;
  }

  public boolean equals(Object object) {
    if (object instanceof Category) {
      Category cat = (Category) object;
      String catName = cat.getName();
      return (name == null && catName == null) || (name != null && name.equals(cat.getName()));
    }
    return false;
  }

}
