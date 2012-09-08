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

import java.util.List;

/**
 * @author Matthieu Chaffotte
 *
 */
public class Group extends Component {

  private boolean optional;
  private List<WidgetComponent> widgets;

//used by XStream
  protected Group() {}
  
  public Group(String labelId, boolean optional, List<WidgetComponent> widgets) {
    super(labelId);
    this.optional = optional;
    this.widgets = widgets;
  }

  public boolean isOptional() {
    return optional;
  }

  public List<WidgetComponent> getWidgets() {
    return widgets;
  }
  
  @Override
  public boolean equals(Object obj) {
    boolean equals = super.equals(obj);
    if (equals && obj instanceof Group) {
      Group temp = (Group) obj;
      return (temp.isOptional() == this.isOptional()
          && temp.getWidgets().equals(this.getWidgets()));
    } else {
      return false;
    }
  }
}
