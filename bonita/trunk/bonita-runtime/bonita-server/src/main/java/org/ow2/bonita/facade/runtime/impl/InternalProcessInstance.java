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
 **/
package org.ow2.bonita.facade.runtime.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.ow2.bonita.definition.activity.ConnectorExecutor;
import org.ow2.bonita.facade.def.InternalActivityDefinition;
import org.ow2.bonita.facade.def.InternalProcessDefinition;
import org.ow2.bonita.facade.def.element.HookDefinition;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.AttachmentInstance;
import org.ow2.bonita.facade.runtime.Comment;
import org.ow2.bonita.facade.runtime.InstanceState;
import org.ow2.bonita.facade.runtime.InstanceStateUpdate;
import org.ow2.bonita.facade.runtime.ProcessInstance;
import org.ow2.bonita.facade.runtime.VariableUpdate;
import org.ow2.bonita.facade.uuid.ActivityDefinitionUUID;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.runtime.model.Execution;
import org.ow2.bonita.services.handlers.FinishedInstanceHandler;
import org.ow2.bonita.type.Variable;
import org.ow2.bonita.util.EnvTool;
import org.ow2.bonita.util.Misc;
import org.ow2.bonita.util.ProcessUtil;
import org.ow2.bonita.util.TransientData;
import org.ow2.bonita.util.VariableUtil;

public class InternalProcessInstance extends ProcessInstanceImpl {

  private static final long serialVersionUID = 370138202886825855L;
  protected long dbid;

  protected Execution rootExecution;
  protected long userXPRead = 0;
  protected Map<String, String> transitionStates = new HashMap<String, String>();
  protected Map<String, Variable> variables;
  protected int nbOfAttachments;

  public enum TransitionState {
    /**
     * Transition is ready to be taken
     */
    READY,
    /**
     * An execution has taken this transition.
     * However, the destination node has not been executed
     * (e.g. node execution is asynchronous)
     */
    TAKEN,
    /**
     * This transition cannot be taken.
     * This has been aborted by the system (e.g. by a join XOR)
     */
    ABORTED
  }

  //mandatory for hibernate
  protected InternalProcessInstance() {
    super();
  }

  public InternalProcessInstance(final ProcessInstanceUUID instanceUUID, final InternalProcessDefinition process, final ProcessInstanceUUID rootInstanceUUID, final long iterationNb) {
    super(process.getUUID(), instanceUUID, rootInstanceUUID, iterationNb);
    this.nbOfAttachments = 0;
    Map<String, InternalActivityDefinition> initialActivities = process.getInternalInitialActivities();
    if (initialActivities.size() == 1) {
      InternalActivityDefinition initial = initialActivities.values().iterator().next();
      this.rootExecution = new Execution("Instance_" + getUUID().toString(), process, this, initial, Execution.STATE_CREATED, null);
    } else {
      // needs to create one root Execution (parent) and many other
      this.rootExecution = new Execution("Instance_" + getUUID().toString(), process, this, null, Execution.STATE_CREATED, null);
    }
  }

  public InternalProcessInstance(ProcessInstance src) {
    super(src);
    this.activities = new HashSet<ActivityInstance>();
    for (ActivityInstance activity : src.getActivities()) {
      this.activities.add(new InternalActivityInstance(activity));
    }

    this.variableUpdates = null;
    for (VariableUpdate varUpdate : src.getVariableUpdates()) {
    	final Serializable value = varUpdate.getValue();
    	addVariableUpdate(new InternalVariableUpdate(varUpdate.getDate(), varUpdate.getUserId(), varUpdate.getName(), VariableUtil.createVariable(src.getProcessDefinitionUUID(), varUpdate.getName(), value)));
    }
    this.setVariables(src.getInitialVariableValues());

    if (src.getClass().equals(InternalProcessInstance.class)) {
      final InternalProcessInstance other = (InternalProcessInstance) src;
      this.userXPRead = other.getUserXPRead();
      this.nbOfAttachments = other.nbOfAttachments;
    }
  }

  public void addVariableUpdate(final VariableUpdate varUpdate) {
    if (this.variableUpdates == null) {
      this.variableUpdates = new ArrayList<VariableUpdate>();
    }
    this.variableUpdates.add(varUpdate);
  }

  public void setVariableValue(final String variableName, final Variable variable) {
    updateLastUpdateDate();
    this.variables.put(variableName, variable);
  }
  
  public void updateLastUpdateDate() {
    this.lastUpdate = System.currentTimeMillis();
  }
  
  public void addChildInstance(final ProcessInstanceUUID childInstanceUUID) {
    if (this.childrenInstanceUUID == null) {
      this.childrenInstanceUUID = new HashSet<ProcessInstanceUUID>();
    }
    updateLastUpdateDate();
    this.childrenInstanceUUID.add(childInstanceUUID);
  }

  public void addComment(Comment comment) {
    updateLastUpdateDate();
    commentFeed.add(comment);
  }
  
  public void addActivity(final ActivityInstance activity) {
    if (getActivities() == null)  {
      this.activities = new HashSet<ActivityInstance>();
    }
    updateLastUpdateDate();
    this.activities.add(activity);
  }
  
  public void addAttachment(final AttachmentInstance attachment) {
    if (attachments == null)  {
      this.attachments = new ArrayList<AttachmentInstance>();
    }
//    updateLastUpdateDate();
    this.attachments.add(attachment);
  }

  public Map<String, Variable> getInitialVariables() {
    return this.variables;
  }

  public Map<String, Object> getInitialVariableValues() {
    if (this.variables != null) {
      return VariableUtil.getVariableValues(this.variables);
    }
    return null;
  }
  
  private void setVariables(Map<String, Object> variables) {
	  this.variables = VariableUtil.createVariableMap(getProcessDefinitionUUID(), variables);
	  this.clientVariables = null;
  }

	public void begin(final ActivityDefinitionUUID activityUUID) {
    if (this.rootExecution.getNode() != null) {
      this.rootExecution.beginWithOneStartNode();
    } else {
      this.rootExecution.beginWithManyStartNodes(activityUUID);
    }
  }
  
  public void setInitialVaribales(Map<String, Variable> initialVariables) {
    this.variables = initialVariables;
  }

  public boolean isInstanceState(final InstanceState state) {
    return getInstanceState().equals(state);
  }

  public TransitionState getTransitionState(final String transitionName) {
    if (!transitionStates.containsKey(transitionName)) {
      return null;
    }
    return TransitionState.valueOf(transitionStates.get(transitionName));
  }

  public void setTransitionState(final String transitionName, final TransitionState state) {
    transitionStates.put(transitionName, state.toString());
  }

  public void removeTransitionState(final String transitionName) {
    transitionStates.remove(transitionName);
  }
  
  public Map<String, String> getTransitionsStates(){
    return Collections.unmodifiableMap(transitionStates);
  }
  
  public Execution getRootExecution() {
    return this.rootExecution;
  }

  @Override
  public String toString() {
    String value = "Instance " + getUUID() + "(state:" + getInstanceState();
    if (getParentInstanceUUID() != null) {
      value += ", child of " + getParentInstanceUUID();
    }
    value += ")";
    return value;
  }

  public List<Execution> getExecOnNode(final String nodeName) {
    return this.getExecOnNode(this.getRootExecution(), nodeName);
  }

  private List<Execution> getExecOnNode(
      final Execution exec,
      final String nodeName) {
    Misc.checkArgsNotNull(exec, nodeName);
    final List<Execution> res = new ArrayList<Execution>();
    if (exec.getExecutions() == null || exec.getExecutions().isEmpty()) {
      if (exec.getNode() != null && exec.getNode().getName().equals(nodeName)) {
        res.add(exec);
      }
    }
    if (exec.getExecutions() != null) {
      for (final Execution child : exec.getExecutions()) {
        res.addAll(this.getExecOnNode((Execution) child, nodeName));
      }
    }
    return res;
  }

  public void cancel() {
    ConnectorExecutor.executeConnectors(this.getRootExecution(), HookDefinition.Event.instanceOnCancel);
    // cancel execution
    this.getRootExecution().cancel();
    // record cancel
    EnvTool.getRecorder().recordInstanceCancelled(this.getUUID(), EnvTool.getUserId());
    ProcessUtil.removeInternalInstanceEvents(getUUID());
    // execute finished instance handler
    if (this.getParentInstanceUUID() == null) {
      this.finish();
    }
  }

  public void finish() {
    final FinishedInstanceHandler handler = EnvTool.getFinishedInstanceHandler();
    handler.handleFinishedInstance(this);
  }

  public void setInstanceState(final InstanceState newState, final String userId) {
    updateLastUpdateDate();
    InstanceState oldState = getInstanceState();
    this.state = newState;
    if (getInstanceStateUpdates() == null) {
      this.instanceStateUpdates = new ArrayList<InstanceStateUpdate>();
    }
    //add a state update
    Date newDate = new Date();
    this.getInstanceStateUpdates().add(new InstanceStateUpdateImpl(newDate, userId, oldState, newState));
    if (newState.equals(InstanceState.STARTED)) {
      startedDate = newDate.getTime();
      startedBy = userId;
    } else {
      endedDate = newDate.getTime();
      endedBy = userId;
      if (activities != null) {
        for (ActivityInstance activity : activities) {
          if (activity.getEndedDate() == null) {
        	EnvTool.getRecorder().recordBodyAborted(activity);
            TransientData.removeTransientData(activity.getUUID());
          }
        }
      }
    }
  }

  public void setUserXPRead(Date d) {
    this.userXPRead = d.getTime();
  }

  public long getUserXPRead() {
    return userXPRead;
  }

  public void setParentUUIDs(ProcessInstanceUUID instanceUUID, ActivityInstanceUUID activityUUID) {
    this.parentInstanceUUID = instanceUUID;
    this.parentActivityUUID = activityUUID;
  }

  public void removeAttachment(String name) {
    List<AttachmentInstance> removeAttachments = new ArrayList<AttachmentInstance>();
    for (AttachmentInstance attachment : attachments) {
      if(attachment.getName().equals(name)) {
        removeAttachments.add(attachment);
      }
    }
    attachments.removeAll(removeAttachments);
    updateLastUpdateDate();
  }

  public void removeAttachments() {
    attachments = null;
  }

  public int getNbOfAttachments() {
    return nbOfAttachments;
  }

  public void setNbOfAttachments(int nbOfAttachments) {
    this.nbOfAttachments = nbOfAttachments;
  }
  
}
