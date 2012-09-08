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
package org.ow2.bonita.definition.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.ow2.bonita.facade.def.InternalActivityDefinition;
import org.ow2.bonita.facade.def.majorElement.EventProcessDefinition;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.def.majorElement.TransitionDefinition;
import org.ow2.bonita.facade.runtime.ProcessInstance;
import org.ow2.bonita.runtime.event.IncomingEventInstance;
import org.ow2.bonita.runtime.event.OutgoingEventInstance;
import org.ow2.bonita.runtime.model.Execution;
import org.ow2.bonita.services.EventService;
import org.ow2.bonita.util.EnvTool;

/**
 * 
 * @author Matthieu Chaffotte
 *
 */
public class ThrowingSignalEvent extends AbstractActivity {

  private static final long serialVersionUID = 964406244645103100L;

  protected ThrowingSignalEvent() {
    super();
  }

  public ThrowingSignalEvent(String eventName) {
    super(eventName);
  }

  @Override
  protected boolean bodyStartAutomatically() {
    return true;
  }

  @Override
  protected boolean executeBusinessLogic(Execution execution) {
    final ProcessDefinition processDefinition = execution.getProcessDefinition();
    final EventService eventService = EnvTool.getEventService();
    final InternalActivityDefinition activity = execution.getNode();
    final List<EventProcessDefinition> eventSubProcesses = processDefinition.getEventSubProcesses();
    if (!eventSubProcesses.isEmpty()) {
      final List<String> processNames = new ArrayList<String>();
      for (EventProcessDefinition eventProcessDefinition : eventSubProcesses) {
        final String processName = eventProcessDefinition.getName();
        processNames.add(processName);
      }
      final String signalCode = activity.getTimerCondition();
      final IncomingEventInstance signalSubProcess = eventService.getSignalStartIncomingEvent(processNames, signalCode);
      if (signalSubProcess != null) {
        throwEvent(eventService, activity);
        return false;
      }
    }

    final Set<TransitionDefinition> outgoingTransitions = activity.getOutgoingTransitions();
    final Set<TransitionDefinition> incomingTransitions = activity.getIncomingTransitions();
    if (!outgoingTransitions.isEmpty() && !incomingTransitions.isEmpty()) {
      throwEvent(eventService, activity);
    } else if (outgoingTransitions.isEmpty()) {
      throwEvent(eventService, activity);
      final ProcessInstance instance = execution.getInstance();
      if (instance.getParentInstanceUUID() != null) {
        return false;
      }
    }
    return true;
  }

  private void throwEvent(final EventService eventService, final InternalActivityDefinition activity) {
    final String signal = activity.getTimerCondition();
    Set<IncomingEventInstance> signalIncomingEvents = eventService.getSignalIncomingEvents(signal);
    if (!signalIncomingEvents.isEmpty()) {
      for (IncomingEventInstance signalIncomingEvent : signalIncomingEvents) {
        final OutgoingEventInstance outgoing = new OutgoingEventInstance(signal, signalIncomingEvent.getProcessName(), signalIncomingEvent.getActivityName(), null, signalIncomingEvent.getInstanceUUID(), signalIncomingEvent.getActivityUUID(), System.currentTimeMillis() + 5000);
        eventService.fire(outgoing);
      }
    }
  }

}
