/**
 * Copyright (C) 2007  Bull S. A. S.
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
package org.ow2.bonita.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ow2.bonita.facade.def.InternalProcessDefinition;
import org.ow2.bonita.facade.def.element.AttachmentDefinition;
import org.ow2.bonita.facade.def.element.BusinessArchive;
import org.ow2.bonita.facade.def.element.IncomingEventDefinition;
import org.ow2.bonita.facade.def.element.impl.BusinessArchiveImpl;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition.Type;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition.ProcessState;
import org.ow2.bonita.facade.def.majorElement.impl.ProcessDefinitionImpl;
import org.ow2.bonita.facade.exception.DeploymentException;
import org.ow2.bonita.facade.runtime.Category;
import org.ow2.bonita.facade.runtime.impl.CategoryImpl;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.light.LightProcessDefinition.ProcessType;
import org.ow2.bonita.runtime.event.EventConstants;
import org.ow2.bonita.runtime.event.IncomingEventInstance;
import org.ow2.bonita.runtime.event.OutgoingEventInstance;
import org.ow2.bonita.services.DocumentationManager;
import org.ow2.bonita.services.EventService;
import org.ow2.bonita.services.LargeDataRepository;
import org.ow2.bonita.services.Querier;
import org.ow2.bonita.services.Recorder;
import org.ow2.bonita.util.ArchiveTool;
import org.ow2.bonita.util.BonitaConstants;
import org.ow2.bonita.util.BonitaRuntimeException;
import org.ow2.bonita.util.DocumentService;
import org.ow2.bonita.util.EnvTool;
import org.ow2.bonita.util.ExceptionManager;
import org.ow2.bonita.util.GroovyException;
import org.ow2.bonita.util.Misc;
import org.ow2.bonita.util.ProcessUtil;

/**
 * Takes {@link BusinessArchiveImpl}s as input, parses the contents into a
 * {@link ProcessDefinition}, stores the {@link ProcessDefinition} into the
 *
 * <p>
 * This class is thread safe and can be shared between multiple threads.
 * </p>
 *
 * @author Marc Blachon, Guillaume Porcher, Charles Souillard, Miguel Valdes, Pierre Vigneras
 */
public final class Deployer {

  private static final Logger LOG = Logger.getLogger(Deployer.class.getName());

  public static final String START_INSTANCE_SIGNAL = "start_event";

  private Deployer() { }

  public static ProcessDefinition deploy(final BusinessArchive businessArchive) throws DeploymentException {
    final Recorder recorder = EnvTool.getRecorder();
    final ProcessDefinitionUUID processUUID = businessArchive.getProcessUUID();
    if (processUUID == null) {
      throw new DeploymentException("The given businessArchive does not contain any process.");
    }
    final LargeDataRepository ldr = EnvTool.getLargeDataRepository();
    for (Map.Entry<String, byte[]> resource : businessArchive.getResources().entrySet()) {
      ldr.storeData(Misc.getBusinessArchiveCategories(processUUID), resource.getKey(), resource.getValue(), true);
    }
    boolean removeDeps = true;
    try {
      ProcessDefinition process = null;
      ClassLoader current = Thread.currentThread().getContextClassLoader();
      try {
        ClassLoader processClassloader = EnvTool.getClassDataLoader().getProcessClassLoader(processUUID);
        Thread.currentThread().setContextClassLoader(processClassloader);
        process = (ProcessDefinition) businessArchive.getProcessDefinition();
      } finally {
        Thread.currentThread().setContextClassLoader(current);
      }
      if (process == null) {
        throw new DeploymentRuntimeException("The given bar archive does not contain any process file");
      }
      final String lastProcessVersion = EnvTool.getAllQueriers().getLastProcessVersion(process.getName());
      if (lastProcessVersion != null && lastProcessVersion.compareTo(process.getVersion()) >= 0) {
        if (lastProcessVersion.equals(process.getVersion())) {
          removeDeps = false;
        }
        String message = ExceptionManager.getInstance().getFullMessage(
            "bd_D_3", process.getName(), lastProcessVersion, process.getVersion(), lastProcessVersion);
        throw new DeploymentRuntimeException(message);
      }

      if (process.getClassDependencies() != null) {
        for (final String className : process.getClassDependencies()) {
          try {
            EnvTool.getClassDataLoader().getClass(process.getUUID(), className);
          } catch (final ClassNotFoundException e) {
            String message = ExceptionManager.getInstance().getFullMessage(
                "bd_D_7", process.getName(), className);
            throw new DeploymentRuntimeException(message);
          }
        }
      }

      final DocumentationManager manager = EnvTool.getDocumentationManager();
      //at the end, stores data
      for (AttachmentDefinition attachment : process.getAttachments().values()) {
        if (attachment.getFilePath() != null) {
          byte[] content = businessArchive.getResource(attachment.getFilePath());
          try {
            manager.createDocument(attachment.getName(), attachment.getProcessDefinitionUUID(), attachment.getFileName(), DocumentService.DEFAULT_MIME_TYPE, content);
          } catch (Exception e) {
            throw new BonitaRuntimeException(e);
          }
        } else {
          try {
            manager.createDocument(attachment.getName(), attachment.getProcessDefinitionUUID());
          } catch (Exception e) {
            throw new BonitaRuntimeException(e);
          }
        }
      }
      InternalProcessDefinition internalProcess = null;
      try {
        Thread.currentThread().setContextClassLoader(EnvTool.getClassDataLoader().getProcessClassLoader(processUUID));
        internalProcess = new InternalProcessDefinition(process);

        if (internalProcess.getCategoryNames()!=null && !internalProcess.getCategoryNames().isEmpty()) {
          Collection<String> categoryNamesToBind = new ArrayList<String>(internalProcess.getCategoryNames());
          Set<Category> persistedCategories = EnvTool.getJournalQueriers().getCategories(categoryNamesToBind);
          if (persistedCategories.size()<categoryNamesToBind.size()) {
            // some categories do not yet exist
            for (Category category : persistedCategories) {
              categoryNamesToBind.remove(category.getName());
            }
            // create missing categories
            for (String name : categoryNamesToBind) {
              recorder.recordNewCategory(new CategoryImpl(name));
            }
          }
        }
        // Keep tracking of number of attachments.
        internalProcess.setNbOfAttachments(process.getAttachments().size());
        recorder.recordProcessDeployed(internalProcess, EnvTool.getUserId());

        addStartEvents(internalProcess);
      } finally {
        Thread.currentThread().setContextClassLoader(current);
      }
      return new ProcessDefinitionImpl(internalProcess);
    } catch (RuntimeException re) {
      if (processUUID != null) {
        EnvTool.getClassDataLoader().removeProcessClassLoader(processUUID);
        if (removeDeps) {
          ldr.deleteData(Misc.getBusinessArchiveCategories(processUUID));
          ldr.deleteData(Misc.getAttachmentCategories(processUUID));
        }
      }
      throw re;
    }
  }

  public static boolean enableProcess(final ProcessDefinitionUUID processUUID) {
    Misc.badStateIfNull(processUUID, "Impossible to enable a processUUID from a null uuid");
    final Querier journal  = EnvTool.getJournalQueriers();
    final InternalProcessDefinition processDef = journal.getProcess(processUUID);
    if (processDef == null) {
      String message = ExceptionManager.getInstance().getFullMessage("bd_D_8", processUUID);
      throw new DeploymentRuntimeException(message);
    }
    if (!processDef.getState().equals(ProcessState.DISABLED)) {
      throw new DeploymentRuntimeException("Impossible to enable a process which is state is not disable");
    } else {
      final Recorder recorder = EnvTool.getRecorder();
      recorder.recordProcessEnable(processDef);
      addStartEvents(processDef);
      if (LOG.isLoggable(Level.INFO)) {
        final StringBuilder logMsg = new StringBuilder();
        logMsg.append("Process ")
        .append(processDef.getName())
        .append(" enable (UUID: ")
        .append(processDef.getUUID())
        .append(").");
        LOG.info(logMsg.toString());
      }
      return true;
    }
  }

  public static boolean disableProcess(final ProcessDefinitionUUID processUUID) {
    Misc.badStateIfNull(processUUID, "Impossible to disable a processUUID from a null uuid");
    final Querier journal  = EnvTool.getJournalQueriers();
    final InternalProcessDefinition processDef = journal.getProcess(processUUID);
    if (processDef == null) {
      String message = ExceptionManager.getInstance().getFullMessage("bd_D_8", processUUID);
      throw new DeploymentRuntimeException(message);
    }
    if (!processDef.getState().equals(ProcessState.ENABLED)) {
      throw new DeploymentRuntimeException("Impossible to disable a process which is state is not enable");
    } else {
      removeStartEvents(processDef);

      final Recorder recorder = EnvTool.getRecorder();
      recorder.recordProcessDisable(processDef);
      if (LOG.isLoggable(Level.INFO)) {
        final StringBuilder logMsg = new StringBuilder();
        logMsg.append("Process ")
        .append(processDef.getName())
        .append(" disable (UUID: ")
        .append(processDef.getUUID())
        .append(").");
        LOG.info(logMsg.toString());
      }
      return true;
    }
  }

  public static boolean archiveProcess(final ProcessDefinitionUUID processUUID, final String userId) {
    Misc.badStateIfNull(processUUID, "Impossible to archive a processUUID from a null uuid");
    final Querier journal  = EnvTool.getJournalQueriers();
    final InternalProcessDefinition processDef = journal.getProcess(processUUID);
    if (processDef == null) {
      String message = ExceptionManager.getInstance().getFullMessage("bd_D_8", processUUID);
      throw new DeploymentRuntimeException(message);
    }
    if (!processDef.getState().equals(ProcessState.DISABLED)) {
      throw new DeploymentRuntimeException("Impossible to archive a process which its state is not disable");
    } else {
      Set<ProcessDefinitionUUID> definitionUUIDs = new HashSet<ProcessDefinitionUUID>();
      definitionUUIDs.add(processUUID);
      int numberOfProcessInstances = journal.getNumberOfProcessInstances(definitionUUIDs);
      if (numberOfProcessInstances != 0) {
        throw new DeploymentRuntimeException("Impossible to archive a process with " + numberOfProcessInstances + " running instance(s)");
      } else {
        removeStartEvents(processDef);
        final Recorder recorder = EnvTool.getRecorder();
        recorder.recordProcessArchive(processDef, userId);
        ArchiveTool.atomicArchive(processDef);
        if (LOG.isLoggable(Level.INFO)) {
          final StringBuilder logMsg = new StringBuilder();
          logMsg.append("Process ")
          .append(processDef.getName())
          .append(" archive (UUID: ")
          .append(processDef.getUUID())
          .append(").");
          LOG.info(logMsg.toString());
        }
        EnvTool.getClassDataLoader().removeProcessClassLoader(processDef.getUUID());
        EnvTool.getUUIDService().archiveOrDeleteProcess(processUUID);
        return true;
      }
    }
  }

  private static void addStartEvents(final ProcessDefinition processDef) throws DeploymentRuntimeException {
    //install "start events" if necessary
    if (ProcessType.PROCESS.equals(processDef.getType())) {
      for (ActivityDefinition activity : processDef.getActivities()) {
        if (activity.getIncomingTransitions().isEmpty()) {
          Type activityType = activity.getType();
          if (activityType.equals(Type.ReceiveEvent)) {
            final IncomingEventDefinition event = activity.getIncomingEvent();
            if (event != null) {
              final EventService eventService = EnvTool.getEventService();
              IncomingEventInstance incomingEventInstance = new IncomingEventInstance(event.getName(), event.getExpression(), null, activity.getUUID(), null, processDef.getName(), activity.getName(), null, START_INSTANCE_SIGNAL, System.currentTimeMillis(), false);
              incomingEventInstance.setPermanent(true);
              eventService.subscribe(incomingEventInstance);
            }
          } else if (activityType.equals(Type.Timer)) {
            final EventService eventService = EnvTool.getEventService();
            final String eventName = BonitaConstants.TIMER_EVENT_PREFIX + activity.getUUID();
            final ProcessDefinitionUUID definitionUUID = activity.getProcessDefinitionUUID();
            final String condition = activity.getTimerCondition();
            try {
              final OutgoingEventInstance outgoing = new OutgoingEventInstance(eventName, null, null, null, null, null, -1);
              eventService.fire(outgoing);
              final Date date = (Date) ProcessUtil.getTimerDate(condition, definitionUUID, System.currentTimeMillis());
              IncomingEventInstance timerEventInstance = new IncomingEventInstance(eventName, condition, null, activity.getUUID(), null, processDef.getName(), activity.getName(), null, EventConstants.TIMER_START_EVENT, date.getTime(), false);
              timerEventInstance.setPermanent(true);
              eventService.subscribe(timerEventInstance);
            } catch (GroovyException e) {
              throw new DeploymentRuntimeException(e.getMessage(), e.getCause());
            }
          } else if (activityType.equals(Type.SignalEvent)) {
            final String eventName = activity.getTimerCondition();
            IncomingEventInstance signalEventInstance = new IncomingEventInstance(eventName, null, null, activity.getUUID(), null, processDef.getName(), activity.getName(), null, EventConstants.SIGNAL_START_EVENT, -1, false);
            signalEventInstance.setPermanent(true);
            final EventService eventService = EnvTool.getEventService();
            eventService.subscribe(signalEventInstance);
          }
        }
      }
    }
  }

  public static void removeStartEvents(final ProcessDefinition processDef) {
    //remove "start events" if necessary
    for (ActivityDefinition activity : processDef.getActivities()) {
      if (activity.getIncomingTransitions().isEmpty()) {
        Type activityType = activity.getType();
        final IncomingEventDefinition event = activity.getIncomingEvent();
        if (event != null) {
          final EventService eventService = EnvTool.getEventService();
          eventService.removeSubscriptions(activity.getUUID());
        } else if (activityType.equals(Type.Timer)) {
          final EventService eventService = EnvTool.getEventService();
          final String eventName = BonitaConstants.TIMER_EVENT_PREFIX + activity.getUUID();
          eventService.removeSubscriptions(activity.getUUID());
          Set<OutgoingEventInstance> outgoingTimerEvents = eventService.getOutgoingEvents(eventName, null, null, null);
          if (!outgoingTimerEvents.isEmpty()) {
            eventService.removeEvent(outgoingTimerEvents.iterator().next());
          }
        } else if (activityType.equals(Type.SignalEvent)) {
          final EventService eventService = EnvTool.getEventService();
          eventService.removeSubscriptions(activity.getUUID());
        }
      }
    }
  }

}
