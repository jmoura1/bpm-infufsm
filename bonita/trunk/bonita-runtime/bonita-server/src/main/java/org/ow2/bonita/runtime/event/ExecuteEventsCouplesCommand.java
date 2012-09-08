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
package org.ow2.bonita.runtime.event;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ow2.bonita.env.Authentication;
import org.ow2.bonita.env.Environment;
import org.ow2.bonita.env.Transaction;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.RuntimeAPI;
import org.ow2.bonita.facade.def.InternalActivityDefinition;
import org.ow2.bonita.facade.exception.ActivityNotFoundException;
import org.ow2.bonita.facade.exception.DocumentNotFoundException;
import org.ow2.bonita.facade.exception.InstanceNotFoundException;
import org.ow2.bonita.facade.exception.ProcessNotFoundException;
import org.ow2.bonita.facade.exception.VariableNotFoundException;
import org.ow2.bonita.facade.impl.StandardAPIAccessorImpl;
import org.ow2.bonita.facade.runtime.impl.InternalActivityInstance;
import org.ow2.bonita.facade.runtime.impl.InternalProcessInstance;
import org.ow2.bonita.facade.uuid.ActivityDefinitionUUID;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.runtime.model.Execution;
import org.ow2.bonita.services.Document;
import org.ow2.bonita.services.DocumentationManager;
import org.ow2.bonita.services.EventService;
import org.ow2.bonita.services.Querier;
import org.ow2.bonita.services.Recorder;
import org.ow2.bonita.util.BonitaConstants;
import org.ow2.bonita.util.BonitaRuntimeException;
import org.ow2.bonita.util.Command;
import org.ow2.bonita.util.DocumentService;
import org.ow2.bonita.util.EnvTool;
import org.ow2.bonita.util.GroovyException;
import org.ow2.bonita.util.ProcessUtil;

/**
 * 
 * @author Charles Souillard, Matthieu Chaffotte
 *
 */
public class ExecuteEventsCouplesCommand implements Command<Void> {

  private static final Logger LOG = Logger.getLogger(ExecuteEventsCouplesCommand.class.getName());

  private static final long serialVersionUID = -479276850307735480L;

  private EventCoupleId eventCouple;
  private EventExecutor eventExecutor;

  public ExecuteEventsCouplesCommand(EventCoupleId eventCouple, EventExecutor eventExecutor) {
    this.eventCouple = eventCouple;
    this.eventExecutor = eventExecutor;
  }

  public Void execute(Environment environment) throws Exception {
    OutgoingEventInstance outgoing = null;
    IncomingEventInstance incoming = null;
    try {
      final EventService eventService = EnvTool.getEventService();
      outgoing = eventService.getOutgoingEvent(eventCouple.getOutgoing());
      incoming = eventService.getIncomingEvent(eventCouple.getIncoming());

      if (incoming != null && outgoing != null) {
        //it is a process start.
        if (incoming.isPermanent() || incoming.getSignal().contains(EventConstants.START_EVENT)) {
          executeStartEvent(incoming, outgoing);
        } else {
          final String signal = incoming.getSignal();
          if (signal.startsWith(EventConstants.BOUNDARY_EVENT)) {
            executeBoundaryEvent(incoming, outgoing);
          } else {
            executeEventCouple(incoming, outgoing);
          }
        }
      }
    } catch (Throwable exception) {
      final ActivityInstanceUUID activityUUID = incoming.getActivityUUID();
      //if there are no more retries put in failed state
      if (incoming.getRetries() == 1 && activityUUID != null) { // compares with 1 because number of retries will be decremented in handle exception.
          InternalActivityInstance activityInstance = EnvTool.getJournal().getActivityInstance(activityUUID);
          Recorder recorder = EnvTool.getRecorder();
          recorder.recordActivityFailed(activityInstance);
          if (LOG.isLoggable(Level.SEVERE)) {
            LOG.severe("The activity \"" + activityUUID + "\" is in the state FAILED because an exception caught while executing eventCouple: " + exception + ". Exception: " + exception.getMessage());
          }
      } else {
          if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Exception caught while executing eventCouple: " + exception + ". Exception: " + exception.getMessage());
          }
          if (LOG.isLoggable(Level.INFO)) {
            LOG.info("handling events, incoming: " + incoming + ", outgoing: " + outgoing + " exception: " + exception.getMessage());
          }
          handleException(environment, eventCouple, exception);
      }
    }
    return null;
  }

  private void executeBoundaryEvent(IncomingEventInstance incoming, OutgoingEventInstance outgoing) throws ActivityNotFoundException {
    final String signal = incoming.getSignal();
    Map<String, Object> parameters = new HashMap<String, Object>();
    if (EventConstants.MESSAGE_BOUNDARY_EVENT.equals(signal)) {
      parameters = outgoing.getParameters();
    }
    if (EventConstants.ERROR_BOUNDARY_EVENT.equals(signal)) {
      String incomingEventName = incoming.getName();
      int separator = incomingEventName.indexOf(EventConstants.SEPARATOR);
      String eventName = incomingEventName.substring(0, separator);
      parameters.put("eventName", eventName);
    } else if (EventConstants.SIGNAL_BOUNDARY_EVENT.equals(signal)) {
      parameters.put("eventName", incoming.getActivityName());
    } else {
      parameters.put("eventName", incoming.getName());
    }
    final EventService eventService = EnvTool.getEventService();
    eventService.removeEvent(outgoing);
    eventService.removeEvent(incoming);

    Authentication.setUserId(BonitaConstants.SYSTEM_USER);
    final Querier journal = EnvTool.getJournalQueriers();
    Execution execution = journal.getExecutionWithEventUUID(incoming.getExecutionUUID());
    if (execution == null) {
      if (LOG.isLoggable(Level.INFO)) {
        LOG.info("No active execution found for uuid: " + incoming.getExecutionUUID());
      }
      throw new BonitaRuntimeException("No active execution found with uuid: " + incoming.getExecutionUUID());
    } else {
      if (LOG.isLoggable(Level.INFO)) {
        LOG.info("Execution found for uuid: " + incoming.getExecutionUUID());
      }
      if (!execution.isActive()) {
        execution.unlock();
      }
      execution.signal(signal, parameters);
    }
  }

  private void executeStartEvent(IncomingEventInstance incoming, OutgoingEventInstance outgoing)
  throws ProcessNotFoundException, GroovyException, InterruptedException, InstanceNotFoundException, VariableNotFoundException {
    final EventService eventService = EnvTool.getEventService();
    final InternalActivityDefinition activity = EnvTool.getJournal().getActivity(incoming.getActivityDefinitionUUID());
    final ProcessDefinitionUUID processUUID = activity.getProcessDefinitionUUID();

    Authentication.setUserId(BonitaConstants.SYSTEM_USER);
    final StandardAPIAccessorImpl accessor = new StandardAPIAccessorImpl();
    final RuntimeAPI runtimeAPI = accessor.getRuntimeAPI();
    final ProcessInstanceUUID eventSubProcessRootInstanceUUID = incoming.getEventSubProcessRootInstanceUUID();
    if (eventSubProcessRootInstanceUUID != null) {
      final Querier journal = EnvTool.getJournalQueriers();
      Set<Execution> executions = journal.getExecutions(eventSubProcessRootInstanceUUID);
      try {
        final Execution current = executions.iterator().next();
        current.abort();
        final Recorder recorder = EnvTool.getRecorder();
        recorder.recordInstanceAborted(eventSubProcessRootInstanceUUID, EnvTool.getUserId());
      } catch (BonitaRuntimeException e) {
        if (LOG.isLoggable(Level.WARNING)) {
          LOG.log(Level.WARNING, "The process: " + eventSubProcessRootInstanceUUID + " has already been aborted by another start event.", e);  
        }
      }
      final QueryRuntimeAPI queryRuntimeAPI = accessor.getQueryRuntimeAPI();
      final Map<String, Object> variables = queryRuntimeAPI.getProcessInstanceVariables(eventSubProcessRootInstanceUUID);

      final InternalProcessInstance eventSubProcessInstance = instantiateEventSubProcess(processUUID, variables, eventSubProcessRootInstanceUUID);
      final ProcessInstanceUUID instanceUUID = eventSubProcessInstance.getUUID();
      final InternalProcessInstance processInstance = EnvTool.getAllQueriers().getProcessInstance(eventSubProcessRootInstanceUUID);
      if (processInstance.getNbOfAttachments() > 0) {
        final DocumentationManager manager = EnvTool.getDocumentationManager();
        final List<Document> lastAttachments = DocumentService.getAllDocumentVersions(manager, eventSubProcessRootInstanceUUID);
        for (Document document : lastAttachments) {
          try {
            manager.attachDocumentTo(processUUID, instanceUUID, document.getId());
          } catch (DocumentNotFoundException e) {
            new BonitaRuntimeException(e);
          }
        }
        eventSubProcessInstance.setNbOfAttachments(processInstance.getNbOfAttachments());
      }

      if (LOG.isLoggable(Level.INFO)) {
        LOG.info("An event started a new process instance of " + processUUID);
      }
      if (EventConstants.MESSAGE_START_EVENT.equals(incoming.getSignal())) {
        outgoing.setLockOwner(instanceUUID.toString());
        incoming.setLockOwner(null);
        incoming.unlock();
        final IncomingEventInstance newIncoming = eventService.getIncomingEvent(instanceUUID, incoming.getName());
        executeEventCouple(newIncoming, outgoing);
      } else {
        eventService.removeEvent(outgoing);
        eventService.removeEvent(incoming);
      }
    } else {
      String signalType = incoming.getSignal();
      if (EventConstants.TIMER_START_EVENT.equals(signalType)) {
        executeStartTimerEvent(incoming, outgoing, eventService, processUUID);
      } else {
        final ActivityDefinitionUUID activityUUID = incoming.getActivityDefinitionUUID();
        ProcessInstanceUUID instanceUUID = runtimeAPI.instantiateProcess(processUUID, activityUUID); 
        if (LOG.isLoggable(Level.INFO)) {
          LOG.info("An event started a new process instance of " + processUUID);
        }
        if (EventConstants.SIGNAL_START_EVENT.equals(signalType)) {
          eventService.removeEvent(outgoing);
          incoming.unlock();
        } else {
          outgoing.setLockOwner(instanceUUID.toString());
          incoming.setLockOwner(null);
          incoming.unlock();
          final IncomingEventInstance newIncoming = eventService.getIncomingEvent(instanceUUID, incoming.getName());
          executeEventCouple(newIncoming, outgoing);
        }
      }
    }
  }

  private InternalProcessInstance instantiateEventSubProcess(final ProcessDefinitionUUID processUUID, final Map<String, Object> variables, final ProcessInstanceUUID rootEventSubProcessInstanceUUID)
  throws ProcessNotFoundException {
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Starting a new instance of process : " + processUUID);
    }
    final Querier journal = EnvTool.getJournalQueriers();
    final InternalProcessInstance rootInstance = journal.getProcessInstance(rootEventSubProcessInstanceUUID);
    final Execution rootExecution = ProcessUtil.createProcessInstance(processUUID, variables, null, rootEventSubProcessInstanceUUID, rootInstance.getRootInstanceUUID(), null, null);
    final InternalProcessInstance instance = rootExecution.getInstance();
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Started: " + instance);
    }
    instance.begin(null);
    return instance;
  }

  private void executeStartTimerEvent(IncomingEventInstance incoming, OutgoingEventInstance outgoing, EventService eventService, final ProcessDefinitionUUID processUUID)
  throws GroovyException, InterruptedException {
    try {
      final ActivityDefinitionUUID activityUUID = incoming.getActivityDefinitionUUID();
      final StandardAPIAccessorImpl accessor = new StandardAPIAccessorImpl();
      accessor.getCommandAPI().execute(new InstantiateProcessCommand(processUUID, activityUUID));
    } catch (Exception e) {
      if (LOG.isLoggable(Level.WARNING)) {
        LOG.log(Level.WARNING, "The start timer event of process " + processUUID + " cannot start a new instance.", e);
      }
    }
    final String condition = incoming.getExpression();
    final long lastExecution = incoming.getEnableTime();
    final Date nextTime = ProcessUtil.getTimerDate(condition, processUUID, lastExecution);
    if (incoming.getEnableTime() == nextTime.getTime()) {
      if (LOG.isLoggable(Level.WARNING)) {
        LOG.warning("A start timer event of process " + incoming.getProcessName() + " ends its cycle.");
      }
      eventService.removeEvent(outgoing);
      eventService.removeEvent(incoming);
    } else {
      incoming.setEnableTime(nextTime.getTime());
      incoming.unlock();
      outgoing.unlock();
    }
  }

  private void executeEventCouple(final IncomingEventInstance incoming, final OutgoingEventInstance outgoing) {
    final Querier journal = EnvTool.getJournalQueriers();

    final Execution execution = journal.getExecutionWithEventUUID(incoming.executionUUID);
    if (execution == null) {
      if (LOG.isLoggable(Level.INFO)) {
        LOG.info("No active execution found for uuid: " + incoming.getExecutionUUID());
      }
      throw new BonitaRuntimeException("No active execution found for uuid: " + incoming.getExecutionUUID());
    } else {
      final String signal = incoming.getSignal();
      if (LOG.isLoggable(Level.INFO)) {
        LOG.info("Execution found for uuid: " + incoming.getExecutionUUID());
      }
      Map<String, Object> parameters = outgoing.getParameters();
      if (incoming.isExecutionLocked()) {
        if (!Execution.STATE_ACTIVE.equals(execution.getState())) {
          execution.unlock();
        }
      }
      final EventService eventService = EnvTool.getEventService();
      eventService.removeEvent(outgoing);
      eventService.removeEvent(incoming);
      execution.signal(signal, parameters);
    }
  }

  protected void handleException(Environment environment, EventCoupleId eventCouple, Throwable exception) {
    Transaction transaction = environment.get(Transaction.class);
    EventCoupleExceptionHandler handler = new EventCoupleExceptionHandler(eventCouple, exception, eventExecutor.getCommandService());
    transaction.registerSynchronization(handler);
    if (exception instanceof RuntimeException) {
      throw (RuntimeException) exception;
    }
    throw new BonitaRuntimeException("Execution of event couple (" + eventCouple + ") failed: " + exception.getMessage(), exception);
  }

}
