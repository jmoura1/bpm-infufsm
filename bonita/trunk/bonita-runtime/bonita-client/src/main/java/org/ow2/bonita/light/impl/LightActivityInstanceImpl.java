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

import java.util.Date;

import org.ow2.bonita.facade.def.majorElement.ActivityDefinition;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition.Type;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.ActivityState;
import org.ow2.bonita.facade.runtime.TaskInstance;
import org.ow2.bonita.facade.runtime.impl.RuntimeRecordImpl;
import org.ow2.bonita.facade.uuid.ActivityDefinitionUUID;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.light.LightTaskInstance;
import org.ow2.bonita.util.Misc;

/**
 * @author Pierre Vigneras
 */
public class LightActivityInstanceImpl extends RuntimeRecordImpl implements LightTaskInstance {
  private static final long serialVersionUID = -8515098234372896097L;

  protected ActivityInstanceUUID uuid;
  protected String iterationId;
  protected String activityInstanceId;
  protected String loopId;

  protected ActivityState state;
  protected String userId;
  protected long lastUpdate;
  
  protected String label;
  protected String description;
  protected String dynamicLabel;
  protected String dynamicDescription;
  protected String name;
  protected long startedDate;
  protected long endedDate;
  protected long readyDate;
  protected String endedBy;
  protected String startedBy;
  protected String executionSummary;

  protected ActivityDefinitionUUID activityDefinitionUUID;
  protected long expectedEndDate;
  protected int priority;
  protected ActivityDefinition.Type type;
  protected ProcessInstanceUUID subflowProcessInstanceUUID;
  protected boolean human = false;
  
  protected LightActivityInstanceImpl() { }
  
  public LightActivityInstanceImpl(final ActivityInstanceUUID uuid, final ActivityDefinition activityDefinition,
      final ProcessDefinitionUUID processUUID, final ProcessInstanceUUID instanceUUID, final ProcessInstanceUUID rootInstanceUUID,
      final String iterationId, final String activityInstanceId, final String loopId) {
    super(processUUID, instanceUUID, rootInstanceUUID);
    Misc.checkArgsNotNull(uuid, activityDefinition);
    this.uuid = uuid;
    this.iterationId = iterationId;
    this.activityInstanceId = activityInstanceId;
    this.loopId = loopId;
    this.state = ActivityState.READY;
    this.lastUpdate = System.currentTimeMillis();
    this.activityDefinitionUUID = activityDefinition.getUUID();
    this.priority = activityDefinition.getPriority();
    this.type = activityDefinition.getType();
    this.name = activityDefinition.getName();
    this.description = activityDefinition.getDescription();
    this.label = activityDefinition.getLabel();
    this.human = isTask();
    long executingTime = activityDefinition.getExecutingTime();
    if (executingTime > 0) {
      this.expectedEndDate = System.currentTimeMillis() + executingTime;
    }
  }

  public LightActivityInstanceImpl(final ActivityInstance src) {
    super(src);
    this.uuid = new ActivityInstanceUUID(src.getUUID());
    this.iterationId = src.getIterationId();
    this.activityInstanceId = src.getActivityInstanceId();
    this.loopId = src.getLoopId();
    this.lastUpdate = Misc.getTime(src.getLastUpdateDate());
    this.startedDate = Misc.getTime(src.getStartedDate());
    this.endedDate = Misc.getTime(src.getEndedDate());
    this.expectedEndDate = Misc.getTime(src.getExpectedEndDate());
    this.readyDate = Misc.getTime(src.getReadyDate());
    this.activityDefinitionUUID = new ActivityDefinitionUUID(src.getActivityDefinitionUUID());
    if (src.getSubflowProcessInstanceUUID() != null) {
      this.subflowProcessInstanceUUID = new ProcessInstanceUUID(src.getSubflowProcessInstanceUUID());
    }

    this.priority = src.getPriority();
    this.type = src.getType();
    this.name = src.getActivityName();
    this.description = src.getActivityDescription();
    this.label = src.getActivityLabel();
    this.dynamicDescription = src.getDynamicDescription();
    this.dynamicLabel = src.getDynamicLabel();
    this.executionSummary = src.getDynamicExecutionSummary();

    if (src.isTask()) {
      TaskInstance task = src.getTask();
      this.human = true;
      this.startedBy = task.getStartedBy();
      this.endedBy = task.getEndedBy();
      this.userId = task.getTaskUser();
    }
    this.state = src.getState();
  }

  public String getActivityLabel() {
    return this.label;
  }

  public String getActivityDescription() {
    return this.description;
  }
  
  public String getDynamicDescription() {
    return this.dynamicDescription;
  }
  
  public String getDynamicLabel() {
    return this.dynamicLabel;
  }

  public String toString() {
    String userId;
    try {
      userId = getTaskUser();
    } catch (IllegalStateException e) {
      userId = null;
    }

    String st = this.getClass().getName()
    + "[uuid: " + getUUID()
    + ", activityId: " + getActivityName()
    + ", iterationId: " + getIterationId()
    + ", processDefinitionUUID: " + getProcessDefinitionUUID()
    + ", processUUID: " + getProcessInstanceUUID()
    + ", startedDate: " + getStartedDate()
    + ", endedDate: " + getEndedDate()
    + ", readyDate: " + getReadyDate()
    + ", userId: " + userId
    + ", state: " + getState()
    + ", createdDate: " + getCreatedDate()
    + ", startedBy: " + getStartedBy()
    + ", startedDate: " + getStartedDate()
    + ", endedDate: " + getEndedDate()
    + ", endedBy: " + getEndedBy()
    + "]";
    return st;
  }

  public LightTaskInstance getTask() {
    if (isTask()) {
      return (LightTaskInstance) this;
    }
    return null;
  }

  public String getIterationId() {
    return this.iterationId;
  }

  public String getActivityInstanceId() {
    return activityInstanceId;
  }

  public String getLoopId() {
    return this.loopId;
  }

  public String getActivityName() {
    return this.name;
  }

  public ActivityInstanceUUID getUUID() {
    return uuid;
  }

  public Date getStartedDate() {
    return Misc.getDate(this.startedDate);
  }

  public Date getEndedDate() {
    return Misc.getDate(this.endedDate);
  }

  public Date getReadyDate() {
    return Misc.getDate(this.readyDate);
  }
  
  public Date getCreatedDate() {
    return getReadyDate();
  }

  public String getEndedBy() {
    return this.endedBy;
  }

  public String getStartedBy() {
    return this.startedBy;
  }

  public ActivityState getState() {
    return this.state;
  }

  public String getTaskUser() {
    return this.userId;
  }

  public boolean isTaskAssigned() {
    return this.userId != null;
  }

  public Date getLastUpdateDate() {
    return Misc.getDate(this.lastUpdate);
  }
  
  public ActivityDefinitionUUID getActivityDefinitionUUID() {
    return this.activityDefinitionUUID;
  }

  public int getPriority() {
    return this.priority;
  }

  public Date getExpectedEndDate() {
    return Misc.getDate(expectedEndDate);
  }
  
  public ProcessInstanceUUID getSubflowProcessInstanceUUID() {
    return this.subflowProcessInstanceUUID;
  }
  
  public boolean isAutomatic() {
    return Type.Automatic.equals(getType());
  }

  public boolean isSubflow() {
    return Type.Subflow.equals(getType());
  }

  public boolean isTimer() {
    return Type.Timer.equals(getType());
  }
  
  public boolean isTask() {
    return Type.Human.equals(getType());
  }

  public Type getType() {
    return this.type;
  }

  public String getDynamicExecutionSummary() {
    return this.executionSummary;
  }

}
