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
 * Modified by Matthieu Chaffotte - BonitaSoft S.A.
 **/
package org.ow2.bonita.facade.def.element.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.ow2.bonita.facade.def.element.ConnectorDefinition;
import org.ow2.bonita.facade.def.element.DeadlineDefinition;
import org.ow2.bonita.facade.def.element.FilterDefinition;
import org.ow2.bonita.facade.def.element.HookDefinition;
import org.ow2.bonita.facade.def.element.MultiInstantiationDefinition;
import org.ow2.bonita.facade.def.element.MultipleActivitiesInstantiatorDefinition;
import org.ow2.bonita.facade.def.element.RoleMapperDefinition;
import org.ow2.bonita.facade.def.majorElement.impl.DescriptionElementImpl;
import org.ow2.bonita.util.ExceptionManager;
import org.ow2.bonita.util.Misc;

public class ConnectorDefinitionImpl extends DescriptionElementImpl
    implements ConnectorDefinition, DeadlineDefinition, FilterDefinition, 
    MultiInstantiationDefinition, RoleMapperDefinition, HookDefinition,
    MultipleActivitiesInstantiatorDefinition {
  
  private static final long serialVersionUID = 4943554886602562216L;
  protected String className;
  protected Map<String, Object[]> clientParameters;
  protected String condition;
  protected String variableName;
  protected Event event;
  protected boolean throwingException = true;
  protected String errorCode;

  protected ConnectorDefinitionImpl() { }

  public ConnectorDefinitionImpl(final String className) {
    super();
    this.className = className;
  }

  public ConnectorDefinitionImpl(final ConnectorDefinition src) {
    super(src);
  	String message = ExceptionManager.getInstance().getFullMessage("baoi_RMDI_1");
    Misc.badStateIfNull(src, message);
    this.className = src.getClassName();
    this.clientParameters = src.getParameters();

    ConnectorDefinitionImpl srcImpl = (ConnectorDefinitionImpl) src;
    this.condition = srcImpl.condition;
    this.variableName = srcImpl.variableName;
    this.event = srcImpl.event;
    this.throwingException = srcImpl.throwingException;
    this.errorCode = srcImpl.errorCode;
  }

  public void setCondition(String condition) {
    this.condition = condition;
  }
  
  public void setVariableName(String variableName) {
    this.variableName = variableName;
  }
  
  public void setEvent(Event event) {
    this.event = event;
  }
  
  public void setThrowingException(boolean throwingException) {
    this.throwingException = throwingException;
  }
  
  public String getClassName() {
    return this.className;
  }

  public Map<String, Object[]> getParameters() {
    if (getClientParameters() != null) {
      return getClientParameters();
    }
    return Collections.emptyMap();
  }

  public void addParameter(String key, Object... value) {
    if (this.clientParameters == null) {
      this.clientParameters = new HashMap<String, Object[]>();
    }
    this.clientParameters.put(key, value); 
  }
  
  public void addParameters(Map<String, Object[]> parameters) {
    if (this.clientParameters == null) {
      this.clientParameters = new HashMap<String, Object[]>();
    }
    this.clientParameters.putAll(parameters); 
  }
  
  public String getCondition() {
    return this.condition;
  }
  
  public String getVariableName() {
    return this.variableName;
  }
  
  public Event getEvent() {
    return this.event;
  }

  public boolean isThrowingException() {
    return this.throwingException;
  }

  public Map<String, Object[]> getClientParameters() {
    return clientParameters;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode= errorCode;
  }

}
