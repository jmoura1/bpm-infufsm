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
package org.ow2.bonita.facade.privilege.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.ow2.bonita.facade.def.majorElement.impl.DescriptionElementImpl;
import org.ow2.bonita.facade.privilege.Rule;
import org.ow2.bonita.facade.uuid.AbstractUUID;
import org.ow2.bonita.util.CopyTool;
import org.ow2.bonita.util.Misc;

/**
 * @author Nicolas Chabanoles
 * 
 */
public class RuleImpl extends DescriptionElementImpl implements Rule {

  /**
   * UID
   */
  private static final long serialVersionUID = -5403223858809095337L;

  protected long dbid;
  
  protected String uuid;

  protected String name;

  protected String label;

  protected Set<String> exceptions;

  protected String type;

  protected Set<String> entities;
  
  protected Set<String> users;
  
  protected Set<String> roles;
  
  protected Set<String> groups;
  
  protected Set<String> memberships;

  protected RuleImpl() {}

  protected RuleImpl(RuleType type) {
    Misc.checkArgsNotNull(type);
    this.uuid = UUID.randomUUID().toString();
    this.type = type.name();
  };

  protected RuleImpl(String name, String label, String description, RuleType type,
      Set<? extends AbstractUUID> items) {
    Misc.checkArgsNotNull(name, type);
    this.uuid = UUID.randomUUID().toString();
    this.name = name;
    this.label = label;
    if(items != null){
      this.exceptions = new HashSet<String>();
      for (AbstractUUID abstractUUID : items) {
        this.exceptions.add(abstractUUID.getValue());
      }
      
    } else {
      this.exceptions = null;
    }

    this.type = type.name();
    setDescription(description);
  }

  protected RuleImpl(Rule src) {
    super(src);
    this.dbid = src.getId();
    this.uuid = src.getUUID();
    this.name = src.getName();
    this.label = src.getLabel();
    this.exceptions = new HashSet<String>();
    Set<String> items_ = src.getItems();
    for (String item : items_) {
      this.exceptions.add(item);
    }
    this.type = src.getType().name();
    this.entities = CopyTool.copy(src.getEntities());
    this.users = CopyTool.copy(src.getUsers());
    this.roles = CopyTool.copy(src.getRoles());
    this.groups = CopyTool.copy(src.getGroups());
    this.memberships = CopyTool.copy(src.getMemberships());
  }

  @Deprecated
  public long getId() {
    return dbid;
  }
  
  public String getUUID() {
    return uuid;
  }

  public String getName() {
    return this.name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getLabel() {
    return this.label;
  }

  public void setLabel(final String label) {
    this.label = label;
  }

  /**
   * @return a non null set.
   */
  public Set<String> getItems() {
    if (this.exceptions == null) {
      this.exceptions = new HashSet<String>();
    }
    return this.exceptions;
  }

  public void setItems(Set<String> items) {
    Misc.checkArgsNotNull(items);
    this.exceptions = items;
  }

  public RuleType getType() {
    return RuleType.valueOf(this.type);
  }
  
  /**
   * @param exceptions must be not null
   */
  protected <E extends AbstractUUID> void addExceptions(Collection<E> exceptions) {
    Misc.checkArgsNotNull(exceptions);
    if (this.exceptions == null) {
      this.exceptions = new HashSet<String>();
    }
    for (AbstractUUID exception : exceptions) {
      this.exceptions.add(exception.getValue());
    }
  }

  /**
   * @param exceptions must be not null
   */
  protected <E extends AbstractUUID> void removeExceptions(Collection<E> exceptions) {
    Misc.checkArgsNotNull(exceptions);
    if (this.exceptions != null && !this.exceptions.isEmpty()) {
      for (AbstractUUID exception : exceptions) {
        this.exceptions.remove(exception.getValue());
      }
    }
  }

  protected <E extends AbstractUUID> void setExceptions(Set<E> exceptions) {
    Misc.checkArgsNotNull(exceptions);
    Set<String> exceptionValues = new HashSet<String>();
    for (AbstractUUID exception : exceptions) {
      exceptionValues.add(exception.getValue());
    }
    this.exceptions = exceptionValues;
  }

  public Set<String> getEntities() {
    if (this.entities == null) {
      this.entities = new HashSet<String>();
    }
    return this.entities;
  }
  
  /**
   * @param entities must be not null
   */
  public void addEntities(Collection<String> entities) {
    Misc.checkArgsNotNull(entities);
    if (this.entities == null) {
      this.entities = new HashSet<String>();
    }
    this.entities.addAll(entities);
  }

  /**
   * @param entities must be not null
   */
  public void removeEntities(Collection<String> entities) {
    Misc.checkArgsNotNull(entities);
    if (this.entities != null && !this.entities.isEmpty()) {
      this.entities.removeAll(entities);
    }
  }

  public Set<String> getGroups() {
    if (this.groups == null) {
      this.groups = new HashSet<String>();
    }
    return this.groups;
  }
  
  /**
   * @param groups must be not null
   */
  public void addGroups(Collection<String> groups) {
    Misc.checkArgsNotNull(groups);
    if (this.groups == null) {
      this.groups = new HashSet<String>();
    }
    this.groups.addAll(groups);
  }

  /**
   * @param groups must be not null
   */
  public void removeGroups(Collection<String> groups) {
    Misc.checkArgsNotNull(groups);
    if (this.groups != null && !this.groups.isEmpty()) {
      this.groups.removeAll(groups);
    }
  }

  public Set<String> getMemberships() {
    if (this.memberships == null) {
      this.memberships = new HashSet<String>();
    }
    return this.memberships;
  }
  
  /**
   * @param memberships must be not null
   */
  public void addMemberships(Collection<String> memberships) {
    Misc.checkArgsNotNull(memberships);
    if (this.memberships == null) {
      this.memberships = new HashSet<String>();
    }
    this.memberships.addAll(memberships);
  }

  /**
   * @param memberships must be not null
   */
  public void removeMemberships(Collection<String> memberships) {
    Misc.checkArgsNotNull(memberships);
    if (this.memberships != null && !this.memberships.isEmpty()) {
      this.memberships.removeAll(memberships);
    }
  }

  public Set<String> getRoles() {
    if (this.roles == null) {
      this.roles = new HashSet<String>();
    }
    return this.roles;
  }
  
  /**
   * @param roles must be not null
   */
  public void addRoles(Collection<String> roles) {
    Misc.checkArgsNotNull(roles);
    if (this.roles == null) {
      this.roles = new HashSet<String>();
    }
    this.roles.addAll(roles);
  }

  /**
   * @param roles must be not null
   */
  public void removeRoles(Collection<String> roles) {
    Misc.checkArgsNotNull(roles);
    if (this.roles != null && !this.roles.isEmpty()) {
      this.roles.removeAll(roles);
    }
  }

  public Set<String> getUsers() {
    if (this.users == null) {
      this.users = new HashSet<String>();
    }
    return this.users;
  }

  /**
   * @param users must be not null
   */
  public void addUsers(Collection<String> users) {
    Misc.checkArgsNotNull(users);
    if (this.users == null) {
      this.users = new HashSet<String>();
    }
    this.users.addAll(users);
  }

  /**
   * @param users must be not null
   */
  public void removeUsers(Collection<String> users) {
    Misc.checkArgsNotNull(users);
    if (this.users != null && !this.users.isEmpty()) {
      this.users.removeAll(users);
    }
  }
  
  @Override
  public String toString() {
    return this.name + " - " + this.label + " - " + this.type;
  }
  
  public static Rule createRule(Rule source) {
    Rule result = new RuleImpl(source);
    return result;
  }

  public int compareTo(Rule rule) {
    return this.getName().compareTo(rule.getName());
  }
}
