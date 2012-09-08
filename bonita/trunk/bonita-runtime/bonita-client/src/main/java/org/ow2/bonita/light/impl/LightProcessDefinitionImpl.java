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
package org.ow2.bonita.light.impl;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition.ProcessState;
import org.ow2.bonita.facade.def.majorElement.impl.NamedElementImpl;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.light.LightProcessDefinition;
import org.ow2.bonita.util.Misc;

public class LightProcessDefinitionImpl extends NamedElementImpl implements LightProcessDefinition {

	private static final long serialVersionUID = -572795239631090498L;

	protected ProcessDefinitionUUID uuid;
	protected String version;
	protected ProcessState state;
	protected ProcessType type;
	protected long deployedDate;
	protected long undeployedDate;
	protected String deployedBy;
	protected String undeployedBy;
	protected Set<String> categories;
	protected long migrationDate;

	protected LightProcessDefinitionImpl() { }

	public LightProcessDefinitionImpl(final ProcessDefinition src) {
		super(src);
		this.uuid = new ProcessDefinitionUUID(src.getUUID());

		this.state = src.getState();
		this.type = src.getType();
		this.version = src.getVersion();

		this.deployedDate = Misc.getTime(src.getDeployedDate());
		this.undeployedDate = Misc.getTime(src.getUndeployedDate());
		this.deployedBy = src.getDeployedBy();
		this.undeployedBy = src.getUndeployedBy();
		this.categories = null;
		if(src.getCategoryNames() != null){
			this.categories = new HashSet<String>(src.getCategoryNames());
		}
		this.migrationDate = Misc.getTime(src.getMigrationDate());
	}

	protected LightProcessDefinitionImpl(String name, final String version) {
    super(name);
    Misc.checkArgsNotNull(name, version);
    this.version = version;
    if (this.version == null) {
      this.version = "1.0";
    }
    this.uuid = new ProcessDefinitionUUID(name, version);
    this.type = ProcessType.PROCESS;
  }

  @Override
	public String toString() {
		String st = this.getClass().getName()
		+ "[uuid: " + getUUID()
		    + ", name:" + getName()
		    + ", description:" + getDescription()
		    + ", version:" + getVersion();
		st +=  "]";
		return st;
	}

	public ProcessState getState() {
		return this.state;
	}

	public String getVersion() {
		return this.version;
	}

	public ProcessDefinitionUUID getUUID() {
		return this.uuid;
	}

	public Date getDeployedDate() {
		return Misc.getDate(this.deployedDate);
	}

	public String getDeployedBy() {
		return this.deployedBy;
	}

	public Date getUndeployedDate() {
		return Misc.getDate(this.undeployedDate);
	}

	public String getUndeployedBy() {
		return this.undeployedBy;
	}

	@Override
  public boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj.getClass().equals(this.getClass()))) {
      return false;
    }
    final LightProcessDefinitionImpl other = (LightProcessDefinitionImpl)obj;
    return other.getUUID().equals(this.uuid);
  }

	@Override
  public int hashCode() {
    return this.uuid.hashCode();
  }

	public Set<String> getCategoryNames() {
	  if (categories == null) {
	    return Collections.emptySet();
	  }
	  return categories;
	}

  public ProcessType getType() {
    return type;
  }

  public Date getMigrationDate() {
    return Misc.getDate(this.migrationDate);
  }

  public void setMigrationDate(Date migrationDate) {
    this.migrationDate = Misc.getTime(migrationDate);
  }

}
