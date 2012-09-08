/**
 * Copyright (C) 2010  BonitaSoft S.A.
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
package org.ow2.bonita.facade.identity.impl;

import java.util.UUID;

import org.ow2.bonita.facade.def.majorElement.impl.DescriptionElementImpl;
import org.ow2.bonita.facade.identity.Group;
import org.ow2.bonita.util.Misc;

/**
 * @author Anthony Birembaut
 *
 */
public class GroupImpl extends DescriptionElementImpl implements Group {

  /**
   * UID
   */
  private static final long serialVersionUID = -3862582387188894368L;

  protected long dbid;
  
  protected String uuid;
  
  protected String name;
  
  protected String label;
  
  protected Group parentGroup;

  protected GroupImpl() {};
  
  public GroupImpl(String name){
    Misc.checkArgsNotNull(name);
    this.name = name;
    this.uuid = UUID.randomUUID().toString();
  }
  
  public GroupImpl(String uuid, String name) {
    Misc.checkArgsNotNull(uuid, name);
    this.uuid = uuid;
    this.name = name;
  }
  
  public GroupImpl(GroupImpl src) {
    super(src);
    this.uuid = src.getUUID();
    this.name = src.getName();
    this.label = src.getLabel();
    if (src.getParentGroup() != null) {
      this.parentGroup = new GroupImpl((GroupImpl)src.getParentGroup());
    }
  }

  public void setName(final String name) {
    this.name = name;  
  }
  
  public void setLabel(final String label) {
    this.label = label;  
  }
  
  public String getName() {
    return this.name;
  }
  
  public String getLabel() {
    return this.label;
  }
  
  public String getUUID() {
    return this.uuid;
  }

  public Group getParentGroup() {
    return parentGroup;
  }
  
  public void setParentGroup(Group parentGroup) {
    this.parentGroup = parentGroup;
  }

}
