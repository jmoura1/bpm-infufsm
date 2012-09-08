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
 * 
 * Modified by Matthieu Chaffotte, Elias Ricken de Medeiros - BonitaSoft S.A.
 **/
package org.ow2.bonita.facade.def.majorElement.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.ow2.bonita.facade.def.majorElement.DataFieldDefinition;
import org.ow2.bonita.facade.uuid.ActivityDefinitionUUID;
import org.ow2.bonita.facade.uuid.DataFieldDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;

public class DataFieldDefinitionImpl extends ProcessElementImpl implements DataFieldDefinition {

	private static final long serialVersionUID = 3520847216843051032L;
	protected DataFieldDefinitionUUID uuid;
	protected String dataTypeClassName;
	protected String scriptingValue;
	protected String enumerationValues;
	protected Serializable clientInitialValue;
	private boolean isTransient;

	protected DataFieldDefinitionImpl() { }

	public DataFieldDefinitionImpl(final ProcessDefinitionUUID processUUID, final String name, final String dataTypeClassName) {
		super(name, processUUID);
		this.uuid = new DataFieldDefinitionUUID(processUUID, name);
		this.dataTypeClassName = dataTypeClassName;
		this.isTransient = false;
	}

	public DataFieldDefinitionImpl(final ProcessDefinitionUUID processUUID, final ActivityDefinitionUUID activityUUID, final String name, final String dataTypeClassName) {
		super(name, processUUID);
		this.uuid = new DataFieldDefinitionUUID(activityUUID, name);
		this.dataTypeClassName = dataTypeClassName;
		this.isTransient = false;
	}

	public DataFieldDefinitionImpl(DataFieldDefinition src) {
		super(src);
		this.uuid = new DataFieldDefinitionUUID(src.getUUID());
		this.dataTypeClassName = src.getDataTypeClassName();

		DataFieldDefinitionImpl srcImpl = (DataFieldDefinitionImpl) src;
		this.enumerationValues = srcImpl.enumerationValues;
		this.clientInitialValue = src.getInitialValue();
		this.scriptingValue = src.getScriptingValue();
		this.isTransient = src.isTransient();
	}

	public String getDataTypeClassName() {
		return dataTypeClassName;
	}

	public Serializable getInitialValue() {
    return getClientInitialValue();
	}

	public DataFieldDefinitionUUID getUUID() {
		return uuid;
	}

	public Set<String> getEnumerationValues() {
		if (this.enumerationValues == null) {
			return Collections.emptySet();
		}
		String[] values = enumerationValues.split(",");
		Set<String> result = new HashSet<String>();
		for (String s : values) {
			result.add(s);
		}
		return result;
	}

	public void setEnumerationValues(Set<String> enumerationValues) {
		if (enumerationValues != null) {
			this.enumerationValues = "";
			Iterator<String> it = enumerationValues.iterator();
			while (it.hasNext()) {
				this.enumerationValues += it.next();
				if (it.hasNext()) {
					this.enumerationValues += ",";
				}
			}
		}
	}

	public void setInitialValue(Serializable initialValue) {
		this.clientInitialValue = initialValue;
	}

	public boolean isEnumeration() {
		return dataTypeClassName.equals(String.class.getName()) && enumerationValues != null;
	}
	
	private Serializable getClientInitialValue() {
	  return clientInitialValue;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (!o.getClass().equals(this.getClass())) {
			return false;
		}
		DataFieldDefinitionImpl other = (DataFieldDefinitionImpl) o;
		return other.getUUID().equals(this.getUUID());
	}

	public void setScriptingValue(String scriptingValue) {
	  this.scriptingValue = scriptingValue;
	}
	
  public String getScriptingValue() {
    return scriptingValue;
  }

  public boolean isTransient() {
    return this.isTransient;
  }
  
  public void setTransient(boolean isTransient){
    this.isTransient = isTransient;
  }

}
