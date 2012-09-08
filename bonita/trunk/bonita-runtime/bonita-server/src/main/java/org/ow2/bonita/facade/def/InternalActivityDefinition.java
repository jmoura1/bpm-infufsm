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
package org.ow2.bonita.facade.def;

import org.ow2.bonita.definition.activity.ExternalActivity;
import org.ow2.bonita.facade.def.element.ConnectorDefinition;
import org.ow2.bonita.facade.def.element.DeadlineDefinition;
import org.ow2.bonita.facade.def.element.OutgoingEventDefinition;
import org.ow2.bonita.facade.def.element.impl.IncomingEventDefinitionImpl;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition;
import org.ow2.bonita.facade.def.majorElement.DataFieldDefinition;
import org.ow2.bonita.facade.def.majorElement.impl.ActivityDefinitionImpl;
import org.ow2.bonita.runtime.model.ObjectReference;
import org.ow2.bonita.util.BonitaRuntimeException;
import org.ow2.bonita.util.ExceptionManager;

public class InternalActivityDefinition extends ActivityDefinitionImpl {

  private static final long serialVersionUID = 7575413369114996514L;

  public ObjectReference<ExternalActivity> behaviourReference;

  //mandatory for hibernate
  protected InternalActivityDefinition() {}

  public InternalActivityDefinition(ActivityDefinition activity, ExternalActivity behaviour) {
    super(activity);
    behaviourReference = new ObjectReference<ExternalActivity>(behaviour);

    this.deadlines = null;
    for (DeadlineDefinition deadline : activity.getDeadlines()) {
      addDeadline(new InternalConnectorDefinition(deadline, activity.getProcessDefinitionUUID()));
    }

    this.dataFields = null;
    for (DataFieldDefinition dataField : activity.getDataFields()) {
      addData(new InternalDataFieldDefinition(dataField, activity.getProcessDefinitionUUID()));
    }
    
    this.connectors = null;
    for (ConnectorDefinition connector : activity.getConnectors()) {
      addConnector(new InternalConnectorDefinition(connector, activity.getProcessDefinitionUUID()));
    }

    if (activity.getIncomingEvent() != null) {
      this.incomingEvent = new IncomingEventDefinitionImpl(activity.getIncomingEvent());
      for (ConnectorDefinition connector : activity.getIncomingEvent().getConnectors()) {
        ((IncomingEventDefinitionImpl)this.incomingEvent).addConnector(new InternalConnectorDefinition(connector, activity.getProcessDefinitionUUID()));
      }
    }
    
    this.outgoingEvents = null;
    for (OutgoingEventDefinition outgoing : activity.getOutgoingEvents()) {
      addOutgoingEvent(new InternalOutgoingEventDefinition(outgoing, activity.getProcessDefinitionUUID()));
    }
    
    if (activity.getMultiInstantiationDefinition() != null) {
      setMultiInstanciation(new InternalConnectorDefinition(activity.getMultiInstantiationDefinition(), activity.getProcessDefinitionUUID()));
    }
    if (activity.getFilter() != null) {
      setFilter(new InternalConnectorDefinition(activity.getFilter(), activity.getProcessDefinitionUUID()));
    }
    if (activity.getMultipleInstancesInstantiator() != null) {
      setMultipleInstancesInstantiator(new InternalConnectorDefinition(activity.getMultipleInstancesInstantiator(), activity.getProcessDefinitionUUID()));
    }
    if (activity.getMultipleInstancesJoinChecker() != null) {
      setMultipleInstancesJoinChecker(new InternalConnectorDefinition(activity.getMultipleInstancesJoinChecker(), activity.getProcessDefinitionUUID()));
    }

  }

  public ExternalActivity getBehaviour() {
    ExternalActivity behaviour = (behaviourReference != null ? behaviourReference.get() : null);
    if (behaviour == null) {
      String message = ExceptionManager.getInstance().getFullMessage("bp_NI_1", this);
      throw new BonitaRuntimeException(message);
    }
    return behaviour;
  }

}
