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
package org.ow2.bonita.facade.def.majorElement.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ow2.bonita.facade.def.element.BoundaryEvent;
import org.ow2.bonita.facade.def.element.ConnectorDefinition;
import org.ow2.bonita.facade.def.element.DeadlineDefinition;
import org.ow2.bonita.facade.def.element.FilterDefinition;
import org.ow2.bonita.facade.def.element.HookDefinition;
import org.ow2.bonita.facade.def.element.IncomingEventDefinition;
import org.ow2.bonita.facade.def.element.MultiInstantiationDefinition;
import org.ow2.bonita.facade.def.element.OutgoingEventDefinition;
import org.ow2.bonita.facade.def.element.SubflowParameterDefinition;
import org.ow2.bonita.facade.def.element.impl.BoundaryEventImpl;
import org.ow2.bonita.facade.def.element.impl.ConnectorDefinitionImpl;
import org.ow2.bonita.facade.def.element.impl.ErrorBoundaryEventImpl;
import org.ow2.bonita.facade.def.element.impl.IncomingEventDefinitionImpl;
import org.ow2.bonita.facade.def.element.impl.MessageBoundaryEventImpl;
import org.ow2.bonita.facade.def.element.impl.OutgoingEventDefinitionImpl;
import org.ow2.bonita.facade.def.element.impl.SignalBoundaryEventImpl;
import org.ow2.bonita.facade.def.element.impl.SubflowParameterDefinitionImpl;
import org.ow2.bonita.facade.def.element.impl.TimerBoundaryEventImpl;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition;
import org.ow2.bonita.facade.def.majorElement.DataFieldDefinition;
import org.ow2.bonita.facade.def.majorElement.TransitionDefinition;
import org.ow2.bonita.facade.uuid.ActivityDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;

public class ActivityDefinitionImpl extends ProcessElementImpl implements ActivityDefinition {

  private static final long serialVersionUID = 1545928041850807545L;

  protected ActivityDefinitionUUID uuid;
  protected Set<DeadlineDefinition> deadlines;
  protected Set<String> performers;
  protected JoinType joinType;
  protected SplitType splitType;
  protected List<HookDefinition> connectors;
  protected FilterDefinition filter;
  protected Set<DataFieldDefinition> dataFields;
  protected Set<TransitionDefinition> outgoingTransitions;
  protected Set<TransitionDefinition> incomingTransitions;
  protected Map<String, BoundaryEvent> boundaryEvents;

  protected Set<SubflowParameterDefinition> subflowInParameters;
  protected Set<SubflowParameterDefinition> subflowOutParameters;
  protected String subflowProcessName;
  protected String subflowProcessVersion;

  @Deprecated
  protected MultiInstantiationDefinition activityInstantiator;
  protected MultiInstantiationDefinition instantiator;
  protected MultiInstantiationDefinition joinChecker;

  protected boolean asynchronous;
  protected long executingTime;
  protected int priority;
  protected boolean inCycle;
  protected String timerCondition;
  protected IncomingEventDefinition incomingEvent;
  protected Set<OutgoingEventDefinition> outgoingEvents;
  protected Type type;

  protected boolean loop;
  protected String loopCondition;
  protected String loopMaximum;
  protected boolean beforeExecution;
  protected String dynamicLabel;
  protected String dynamicDescription;
  protected String executionSummary;
  
  protected boolean catchEvent;
  protected boolean terminateProcess;

  protected ActivityDefinitionImpl() { }

  public static ActivityDefinitionImpl createAutomaticActivity(final ProcessDefinitionUUID processUUID, final String name) {
  	return new ActivityDefinitionImpl(processUUID, name, Type.Automatic, null, null, null, null, null, null);
  }

  public static ActivityDefinitionImpl createSubflowActivity(final ProcessDefinitionUUID processUUID, final String name, final String subflowProcessName, final String subflowProcessVersion) {
  	return new ActivityDefinitionImpl(processUUID, name, Type.Subflow, null, subflowProcessName, subflowProcessVersion, null, null, null);
  }

  public static ActivityDefinitionImpl createTimerActivity(final ProcessDefinitionUUID processUUID, final String name, final String timerCondition) {
  	return new ActivityDefinitionImpl(processUUID, name, Type.Timer, null, null, null, timerCondition, null, null);
  }

  public static ActivityDefinitionImpl createHumanActivity(final ProcessDefinitionUUID processUUID, final String name, final Set<String> performers) {
  	return new ActivityDefinitionImpl(processUUID, name, Type.Human, performers, null, null, null, null, null);
  }

	public static ActivityDefinitionImpl createSendEventActivity(ProcessDefinitionUUID processUUID, String name) {
		return new ActivityDefinitionImpl(processUUID, name, Type.SendEvents, null, null, null, null, null, null);
  }

	public static ActivityDefinitionImpl createReceiveEventActivity(ProcessDefinitionUUID processUUID, String name, String eventName, final String expression) {
		return new ActivityDefinitionImpl(processUUID, name, Type.ReceiveEvent, null, null, null, null, eventName, expression);
  }

	public static ActivityDefinitionImpl createErrorEventActivity(ProcessDefinitionUUID processUUID, String eventName, String errorCode) {
    return new ActivityDefinitionImpl(processUUID, eventName, Type.ErrorEvent, null, null, null, errorCode, null, null);
  }

  public static ActivityDefinitionImpl createSignalEventActivity(ProcessDefinitionUUID processUUID, String eventName, String signalCode) {
    return new ActivityDefinitionImpl(processUUID, eventName, Type.SignalEvent, null, null, null, signalCode, null, null);
  }

  private ActivityDefinitionImpl(final ProcessDefinitionUUID processUUID, final String name, 
  		final Type type, final Set<String> performers, final String subflowProcessName, final String subFlowProcessVersion, final String timerCondition, final String eventName, final String receiveEventExpression) {
    super(name, processUUID);
    this.uuid = new ActivityDefinitionUUID(processUUID, name);
    this.performers = performers;
    this.subflowProcessName = subflowProcessName;
    this.subflowProcessVersion = subFlowProcessVersion;
    this.timerCondition = timerCondition;

    this.joinType = JoinType.XOR;
    this.splitType = SplitType.AND;
    this.dataFields = new HashSet<DataFieldDefinition>();
    this.asynchronous = false;
    this.loop = false;

    this.type = type;
    if (eventName != null) {
    	this.incomingEvent = new IncomingEventDefinitionImpl(eventName, receiveEventExpression);
    }
  }

  public ActivityDefinitionImpl(ActivityDefinition src) {
    super(src);
    this.uuid = new ActivityDefinitionUUID(src.getUUID());
    this.executingTime = src.getExecutingTime();
    this.priority = src.getPriority();
    this.timerCondition = src.getTimerCondition();

    Set<DeadlineDefinition> deadlines = src.getDeadlines();
    if (deadlines != null) {
      this.deadlines = new HashSet<DeadlineDefinition>();
      for (DeadlineDefinition d : deadlines) {
        this.deadlines.add(new ConnectorDefinitionImpl(d));
      }
    }
    Set<String> performers = src.getPerformers();
    this.performers = new HashSet<String>();
    for (String performer : performers) {
      this.performers.add(performer);
    }
    this.joinType = src.getJoinType();
    this.splitType = src.getSplitType();
    List<HookDefinition> hooks = src.getConnectors();
    if (hooks != null) {
      this.connectors = new ArrayList<HookDefinition>();
      for (HookDefinition d : hooks) {
        this.connectors.add(new ConnectorDefinitionImpl(d));
      }
    }
    if (src.getFilter() != null) {
      this.filter = new ConnectorDefinitionImpl(src.getFilter());
    }
    Set<DataFieldDefinition> dataFields = src.getDataFields();
    if (dataFields != null) {
      this.dataFields = new HashSet<DataFieldDefinition>();
      for (DataFieldDefinition d : dataFields) {
        this.dataFields.add(new DataFieldDefinitionImpl(d));
      }
    }
    Set<TransitionDefinition> ogtransitions = src.getOutgoingTransitions();
    if (ogtransitions != null) {
      this.outgoingTransitions = new HashSet<TransitionDefinition>();
      for (TransitionDefinition d : ogtransitions) {
        this.outgoingTransitions.add(new TransitionDefinitionImpl(d));
      }
    }
    Set<TransitionDefinition> ictransitions = src.getIncomingTransitions();
    if (ictransitions != null) {
      this.incomingTransitions = new HashSet<TransitionDefinition>();
      for (TransitionDefinition d : ictransitions) {
        this.incomingTransitions.add(new TransitionDefinitionImpl(d));
      }
    }
    List<BoundaryEvent> boundaryevents = src.getBoundaryEvents();
    if (boundaryevents != null) {
      this.boundaryEvents = new HashMap<String, BoundaryEvent>();
      for (BoundaryEvent boundaryEvent : boundaryevents) {
        BoundaryEvent event = null;
        if (boundaryEvent instanceof TimerBoundaryEventImpl) {
          TimerBoundaryEventImpl timer = (TimerBoundaryEventImpl) boundaryEvent;
          event = new TimerBoundaryEventImpl(timer);
        } else if (boundaryEvent instanceof MessageBoundaryEventImpl) {
          MessageBoundaryEventImpl message = (MessageBoundaryEventImpl) boundaryEvent;
          event = new MessageBoundaryEventImpl(message);
        } else if (boundaryEvent instanceof ErrorBoundaryEventImpl) {
          ErrorBoundaryEventImpl error = (ErrorBoundaryEventImpl) boundaryEvent;
          event = new ErrorBoundaryEventImpl(error);
        } else if (boundaryEvent instanceof SignalBoundaryEventImpl) {
          SignalBoundaryEventImpl signal = (SignalBoundaryEventImpl) boundaryEvent;
          event = new SignalBoundaryEventImpl(signal);
        }
        this.boundaryEvents.put(boundaryEvent.getName(), event);
      }
    }

    if (src.getIncomingEvent() != null) {
    	this.incomingEvent = new IncomingEventDefinitionImpl(src.getIncomingEvent());
    }
    Set<OutgoingEventDefinition> outgoingEvents = src.getOutgoingEvents();
    if (outgoingEvents != null) {
      this.outgoingEvents = new HashSet<OutgoingEventDefinition>();
      for (OutgoingEventDefinition eventDefinition : outgoingEvents) {
        this.outgoingEvents.add(new OutgoingEventDefinitionImpl(eventDefinition));
      }
    }

    if (src.getMultiInstantiationDefinition() != null) {
      this.activityInstantiator = new ConnectorDefinitionImpl(src.getMultiInstantiationDefinition());
    }

    if (src.getMultipleInstancesInstantiator() != null) {
      this.instantiator = new ConnectorDefinitionImpl(src.getMultipleInstancesInstantiator());
    }
    if (src.getMultipleInstancesJoinChecker() != null) {
      this.joinChecker = new ConnectorDefinitionImpl(src.getMultipleInstancesJoinChecker());
    }

    this.asynchronous = src.isAsynchronous();
    Set<SubflowParameterDefinition> subFlowInParams = src.getSubflowInParameters();
    if (subFlowInParams != null) {
      this.subflowInParameters = new HashSet<SubflowParameterDefinition>();
      for (SubflowParameterDefinition p : subFlowInParams) {
        this.subflowInParameters.add(new SubflowParameterDefinitionImpl(p));
      }
    }
    
    Set<SubflowParameterDefinition> subFlowOutParams = src.getSubflowOutParameters();
    if (subFlowOutParams != null) {
      this.subflowOutParameters = new HashSet<SubflowParameterDefinition>();
      for (SubflowParameterDefinition p : subFlowOutParams) {
        this.subflowOutParameters.add(new SubflowParameterDefinitionImpl(p));
      }
    }
    this.subflowProcessName = src.getSubflowProcessName();
    this.subflowProcessVersion = src.getSubflowProcessVersion();
    this.inCycle = src.isInCycle();
    this.type = src.getType();
    
    this.loop = src.isInALoop();
    this.loopCondition = src.getLoopCondition();
    this.beforeExecution = src.evaluateLoopConditionBeforeExecution();
    this.loopMaximum = src.getLoopMaximum();
    
    this.dynamicDescription = src.getDynamicDescription();
    this.dynamicLabel = src.getDynamicLabel();
    this.executionSummary = src.getDynamicExecutionSummary();
    this.catchEvent = src.catchEvent();
    this.terminateProcess = src.isTerminateProcess();
  }

  @Override
  public String toString() {
    return getUUID().toString();
  }
  
  public String getDynamicDescription() {
    return this.dynamicDescription;
  }
  
  public String getDynamicLabel() {
    return this.dynamicLabel;
  }
  
  public String getDynamicExecutionSummary() {
    return this.executionSummary;
  }
  
  public Set<DeadlineDefinition> getDeadlines() {
    if (deadlines == null) {
      return Collections.emptySet();
    }
    return deadlines;
  }

  public Set<TransitionDefinition> getOutgoingTransitions() {
    if (this.outgoingTransitions == null) {
      return Collections.emptySet();
    }
    return this.outgoingTransitions;
  }

  public Set<TransitionDefinition> getIncomingTransitions() {
    if (this.incomingTransitions == null) {
      return Collections.emptySet();
    }
    return this.incomingTransitions;
  }

  public TransitionDefinition getOutgoingTransition(String transitionName) {
    for (TransitionDefinition transition : getOutgoingTransitions()) {
      if (transition.getName().equals(transitionName)) {
        return transition;
      }
    }
    return null;
  }

  public TransitionDefinition getIncomingTransitions(String transitionName) {
    for (TransitionDefinition transition : getIncomingTransitions()) {
      if (transition.getName().equals(transitionName)) {
        return transition;
      }
    }
    return null;
  }

  public Set<String> getPerformers() {
    if (performers == null) {
      return Collections.emptySet();
    }
    return performers;
  }

  public String getSubflowProcessName() {
    return subflowProcessName;
  }

  public String getSubflowProcessVersion() {
    return subflowProcessVersion;
  }
  
  public List<HookDefinition> getConnectors() {
    if (connectors == null) {
      return Collections.emptyList();
    }
    return connectors;
  }

  public FilterDefinition getFilter() {
    return filter;
  }

  public Set<DataFieldDefinition> getDataFields() {
    if (this.dataFields == null) {
      return Collections.emptySet();
    }
    return dataFields;
  }

  public ActivityDefinitionUUID getUUID() {
    return uuid;
  }

  @Deprecated
  public MultiInstantiationDefinition getMultiInstantiationDefinition() {
    return activityInstantiator;
  }

  public MultiInstantiationDefinition getMultipleInstancesInstantiator() {
    return instantiator;
  }
  
  public MultiInstantiationDefinition getMultipleInstancesJoinChecker() {
    return joinChecker;
  }

  public boolean isAsynchronous() {
    return this.asynchronous;
  }

  public JoinType getJoinType() {
    return this.joinType;
  }

  public SplitType getSplitType() {
    return this.splitType;
  }

  public boolean isAutomatic() {
    return Type.Automatic.equals(getType());
  }

  public boolean isTask() {
  	return Type.Human.equals(getType());
  }
  
  public Type getType() {
    return this.type;
  }

  public void addOutgoingTransition(TransitionDefinition transition) {
    if (outgoingTransitions == null) {
      outgoingTransitions = new HashSet<TransitionDefinition>();
    }
    this.outgoingTransitions.add(transition);
  }

  public boolean hasIncomingTransitions() {
    return getIncomingTransitions().size() > 0;
  }

  public boolean hasOutgoingTransitions() {
    return getOutgoingTransitions().size() > 0;
  }

  public void addIncomingTransition(TransitionDefinition transition) {
    if (incomingTransitions == null) {
      incomingTransitions = new HashSet<TransitionDefinition>();
    }
    this.incomingTransitions.add(transition);
  }

  public void addData(DataFieldDefinition data) {
    if (dataFields == null) {
      dataFields = new HashSet<DataFieldDefinition>();
    }
    dataFields.add(data); 
  }

  public void setFilter(FilterDefinition filter) {
    this.filter = filter;
  }

  @Deprecated
  public void setMultiInstanciation(MultiInstantiationDefinition multiInstanciation) {
    this.activityInstantiator = multiInstanciation;
  }

  public void setMultipleInstancesInstantiator(MultiInstantiationDefinition instantiator) {
    this.instantiator = instantiator;
  }

  public void setMultipleInstancesJoinChecker(MultiInstantiationDefinition joinChecker) {
    this.joinChecker= joinChecker ;
  }

  public void addDeadline(DeadlineDefinition deadline) {
    if (deadlines == null) {
      deadlines = new HashSet<DeadlineDefinition>();
    }
    this.deadlines.add(deadline);
  }

  public void addConnector(HookDefinition connector) {
    if (connectors == null) {
      connectors = new ArrayList<HookDefinition>();
    }
    this.connectors.add(connector);
  }

  public void setJoinType(JoinType join) {
    this.joinType = join;
  }

  public void setSplitType(SplitType split) {
    this.splitType = split;
  }

  public void setAsynchronous(boolean asynchronous) {
    this.asynchronous = asynchronous;
  }

  public Set<SubflowParameterDefinition> getSubflowInParameters() {
    if (this.subflowInParameters == null) {
      return Collections.emptySet();
    }
    return this.subflowInParameters;
  }

  public Set<SubflowParameterDefinition> getSubflowOutParameters() {
    if (this.subflowOutParameters == null) {
      return Collections.emptySet();
    }
    return this.subflowOutParameters;
  }

  public boolean isSubflow() {
  	return Type.Subflow.equals(getType());
  }

  public void addSubflowOutParameter(final SubflowParameterDefinition param) {
    if (this.subflowOutParameters == null) {
      this.subflowOutParameters = new HashSet<SubflowParameterDefinition>();
    }
    this.subflowOutParameters.add(param);
  }

  public void addSubflowInParameter(final SubflowParameterDefinition param) {
    if (this.subflowInParameters == null) {
      this.subflowInParameters = new HashSet<SubflowParameterDefinition>();
    }
    this.subflowInParameters.add(param);
  }

  public void setExecutingTime(long executingTime) {
    this.executingTime =  executingTime;
  }

  public long getExecutingTime() {
    return this.executingTime;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public int getPriority() {
    return priority;
  }

  public Set<String> getClassDependencies() {
    Set<String> classDependencies = new HashSet<String>();
    for (DeadlineDefinition deadline : getDeadlines()) {
      classDependencies.add(deadline.getClassName());
    }
    for (ConnectorDefinition connector : getConnectors()) {
      classDependencies.add(connector.getClassName());
    }
    if (getMultiInstantiationDefinition() != null) {
      classDependencies.add(getMultiInstantiationDefinition().getClassName());
    }
    if (getFilter() != null) {
      classDependencies.add(getFilter().getClassName());
    }
    return classDependencies;
  }
  
  public void setInCycle(boolean inCycle) {
  	this.inCycle = inCycle;
  }
  
  public boolean isInCycle() {
  	return this.inCycle;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof ActivityDefinition)) {
      return false;
    }
    ActivityDefinition other = (ActivityDefinition) obj;
    if (other.getUUID() == null) {
      return uuid == null;
    }
    return other.getUUID().equals(uuid);
  }

  public IncomingEventDefinition getIncomingEvent() {
    return incomingEvent;
  }

  public String getTimerCondition() {
    return this.timerCondition;
  }
  
  public boolean isTimer() {
    return Type.Timer.equals(getType());
  }

  public boolean isSendEvents() {
  	return Type.SendEvents.equals(getType());
  }

  public boolean isReceiveEvent() {
  	return Type.ReceiveEvent.equals(getType());
  }

  public boolean isThrowingErrorEvent() {
    return Type.ErrorEvent.equals(getType()) && this.getOutgoingTransitions().isEmpty();
  }

  public boolean isSignalEvent() {
    return Type.SignalEvent.equals(getType());
  }

  public boolean isThrowingSignalEvent() {
    return isSignalEvent() && !catchEvent;
  }

  public boolean isCatchingSignalEvent() {
    return isSignalEvent() && catchEvent;
  }

  public boolean isCatchingErrorEvent() {
    return Type.ErrorEvent.equals(getType()) && this.getIncomingTransitions().isEmpty();
  }

  public Set<OutgoingEventDefinition> getOutgoingEvents() {
    if (outgoingEvents == null) {
      return Collections.emptySet();
    }
    return this.outgoingEvents;
  }

  public void addOutgoingEvent(OutgoingEventDefinition oged) {
    if (this.outgoingEvents == null) {
      this.outgoingEvents = new HashSet<OutgoingEventDefinition>();
    }
    this.outgoingEvents.add(oged);
  }

  public boolean evaluateLoopConditionBeforeExecution() {
    return beforeExecution;
  }

  public String getLoopCondition() {
    return loopCondition;
  }

  public String getLoopMaximum () {
    return loopMaximum;
  }

  public boolean isInALoop() {
    return loop;
  }

  public void setLoop(String condition, boolean beforeExecution, String loopMaximum) {
    this.loop = true;
    this.loopCondition = condition;
    this.beforeExecution = beforeExecution;
    this.loopMaximum = loopMaximum;
  }

  public void setDynamicDescription(String dynamicDescription) {
	  this.dynamicDescription = dynamicDescription;
  }

  public void setDynamicLabel(String dynamicLabel) {
	  this.dynamicLabel = dynamicLabel;
  }

  public void setDynamicExecutionSummary(String expression) {
    this.executionSummary = expression;
  }

  public void addBoundaryEvent(BoundaryEvent event) {
    if (boundaryEvents == null) {
      boundaryEvents = new HashMap<String, BoundaryEvent>();
    }
    boundaryEvents.put(event.getName(), event);
  }

  public void addExceptionTransition(String eventName, TransitionDefinition transition) {
    if (boundaryEvents != null) {
      BoundaryEventImpl event = (BoundaryEventImpl) boundaryEvents.get(eventName);
      if (event != null) {
        event.setExceptionTransition(transition);
        boundaryEvents.put(eventName, event);
      }
    }
  }

  public BoundaryEvent getBoundaryEvent(String eventName) {
    if (boundaryEvents == null) {
      return null;
    }
    return boundaryEvents.get(eventName);
  }
  
  public List<BoundaryEvent> getBoundaryEvents() {
    List<BoundaryEvent> events = new ArrayList<BoundaryEvent>();
    if (boundaryEvents != null) {
      events.addAll(boundaryEvents.values());
    }
    return events;
  }

  public boolean hasBoundaryEvents() {
    return getBoundaryEvents().size() > 0;
  }

  public boolean catchEvent() {
    return catchEvent;
  }

  public void setCatchEvent(boolean catchEvent) {
    this.catchEvent = catchEvent;
  }

  public boolean isTerminateProcess() {
    return terminateProcess;
  }

  public void setTerminateProcess(boolean terminateProcess) {
    this.terminateProcess = terminateProcess;
  }

}
