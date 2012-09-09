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

import org.ow2.bonita.facade.def.majorElement.ActivityDefinition;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.ProcessInstance;
import org.ow2.bonita.facade.runtime.impl.InternalProcessInstance;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.light.LightProcessDefinition.ProcessType;
import org.ow2.bonita.runtime.event.OutgoingEventInstance;
import org.ow2.bonita.runtime.model.Execution;
import org.ow2.bonita.services.Recorder;
import org.ow2.bonita.util.EnvTool;
import org.ow2.bonita.util.ProcessUtil;

/**
 * 
 * @author Matthieu Chaffotte
 *
 */
public class ThrowingErrorEvent extends AbstractActivity {

  private static final long serialVersionUID = -7250991226062578318L;

  protected ThrowingErrorEvent() {
    super();
  }

  public ThrowingErrorEvent(String eventName) {
    super(eventName);
  }

  @Override
  protected boolean bodyStartAutomatically() {
    return true;
  }

  @Override
  protected boolean executeBusinessLogic(Execution execution) {
    final ProcessDefinition process = execution.getProcessDefinition();
    ActivityDefinition targetActivityDefinition = null;
    ActivityInstanceUUID targetActivityUUID = null;
    final ActivityDefinition activity = execution.getNode();
    final String throwingErrorCode = activity.getTimerCondition();
    
    final ActivityDefinition eventSubProcessActivity = ActivityUtil.getMatchingErrorEvenSubProcessActivity(execution, throwingErrorCode);
    if (eventSubProcessActivity != null) {
      targetActivityDefinition = eventSubProcessActivity;
    } else {
      final ProcessInstance instance = execution.getInstance();
      ActivityInstanceUUID parentActivityUUID = null; 
      if (ProcessType.EVENT_SUB_PROCESS.equals(process.getType())) {
        InternalProcessInstance rootEventSubProcess = EnvTool.getJournalQueriers().getProcessInstance(instance.getParentInstanceUUID());
        parentActivityUUID = rootEventSubProcess.getParentActivityUUID();
      } else {
        parentActivityUUID = instance.getParentActivityUUID();
      }
      if (parentActivityUUID != null) {
        ActivityInstance parentActivity = EnvTool.getJournalQueriers().getActivityInstance(parentActivityUUID);
        targetActivityDefinition = EnvTool.getJournalQueriers().getActivity(parentActivity.getActivityDefinitionUUID());
        targetActivityUUID = parentActivityUUID;
      }
    }

    final Recorder recorder = EnvTool.getRecorder();
    final InternalProcessInstance instance = execution.getInstance();
    if (targetActivityDefinition != null) {
      final String errorCode = activity.getTimerCondition();
      final String eventName = ActivityUtil.getErrorEventName(targetActivityDefinition, errorCode);
      final String targetActivityName = targetActivityDefinition.getName();
      final ProcessDefinition targetProcess = EnvTool.getJournalQueriers().getProcess(targetActivityDefinition.getProcessDefinitionUUID());
      final String targetProcessName = targetProcess.getName();
      ProcessInstanceUUID targetInstanceUUID = null;
      if (targetActivityUUID != null) {
        final ActivityInstance targetActivity = EnvTool.getJournalQueriers().getActivityInstance(targetActivityUUID);
        targetInstanceUUID = targetActivity.getProcessInstanceUUID();
      }
      OutgoingEventInstance outgoing = new OutgoingEventInstance(eventName, targetProcessName, targetActivityName, null, targetInstanceUUID, targetActivityUUID, -1);
      EnvTool.getEventService().fire(outgoing);
      execution.abort();
      recorder.recordInstanceAborted(instance.getUUID(), EnvTool.getUserId());
    } else {
      execution.abort();
      recorder.recordInstanceAborted(instance.getUUID(), EnvTool.getUserId());
      ProcessUtil.removeInternalInstanceEvents(instance.getUUID());
      instance.finish();
    }
    return false;
  }

}
