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
 * Modified by Matthieu Chaffotte, Nicolas Chabanoles, Elias Ricken de Medeiros - BonitaSoft S.A.
 **/
package org.ow2.bonita.definition.activity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ow2.bonita.definition.MultiInstantiator;
import org.ow2.bonita.definition.MultiInstantiatorDescriptor;
import org.ow2.bonita.env.Authentication;
import org.ow2.bonita.facade.def.InternalActivityDefinition;
import org.ow2.bonita.facade.def.InternalConnectorDefinition;
import org.ow2.bonita.facade.def.element.BoundaryEvent;
import org.ow2.bonita.facade.def.element.DeadlineDefinition;
import org.ow2.bonita.facade.def.element.HookDefinition;
import org.ow2.bonita.facade.def.element.HookDefinition.Event;
import org.ow2.bonita.facade.def.element.MultiInstantiationDefinition;
import org.ow2.bonita.facade.def.element.impl.ErrorBoundaryEventImpl;
import org.ow2.bonita.facade.def.element.impl.IterationDescriptor;
import org.ow2.bonita.facade.def.element.impl.MessageBoundaryEventImpl;
import org.ow2.bonita.facade.def.element.impl.SignalBoundaryEventImpl;
import org.ow2.bonita.facade.def.element.impl.TimerBoundaryEventImpl;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition.JoinType;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition.SplitType;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.def.majorElement.TransitionDefinition;
import org.ow2.bonita.facade.exception.BonitaWrapperException;
import org.ow2.bonita.facade.exception.MultiInstantiatorInvocationException;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.ActivityState;
import org.ow2.bonita.facade.runtime.InstanceState;
import org.ow2.bonita.facade.runtime.impl.InternalActivityInstance;
import org.ow2.bonita.facade.runtime.impl.InternalProcessInstance;
import org.ow2.bonita.facade.runtime.impl.InternalProcessInstance.TransitionState;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.light.LightProcessDefinition.ProcessType;
import org.ow2.bonita.runtime.event.EventConstants;
import org.ow2.bonita.runtime.event.IncomingEventInstance;
import org.ow2.bonita.runtime.event.OutgoingEventInstance;
import org.ow2.bonita.runtime.model.Execution;
import org.ow2.bonita.services.EventService;
import org.ow2.bonita.services.Querier;
import org.ow2.bonita.services.Recorder;
import org.ow2.bonita.type.Variable;
import org.ow2.bonita.util.BonitaConstants;
import org.ow2.bonita.util.BonitaRuntimeException;
import org.ow2.bonita.util.EnvTool;
import org.ow2.bonita.util.ExceptionManager;
import org.ow2.bonita.util.GroovyException;
import org.ow2.bonita.util.GroovyExpression;
import org.ow2.bonita.util.GroovyUtil;
import org.ow2.bonita.util.Misc;
import org.ow2.bonita.util.ProcessUtil;
import org.ow2.bonita.util.TransientData;
import org.ow2.bonita.util.VariableUtil;

/**
 * @author Marc Blachon, Guillaume Porcher, Charles Souillard, Miguel Valdes, Pierre Vigneras, Pascal Verdage
 */
/**
 * Activity life cycle: 1- when an instance is created, activity state is
 * {@link ActivityState#INITIAL} 2- when the execution arrives on the node, the
 * activity becomes {@link ActivityState#READY} the activity state is recorded
 * (before start) 3- if the activity is Manual, a task is created. - when the
 * start task is started, the activity state is recorded (after start). 4- the
 * activity becomes {@link ActivityState#EXECUTING}. The business logic is
 * executed. - the activity state is recorded (before stopped) 5- if the
 * activity is Manual, wait for the task to finish. (TODO: change the activity
 * state ?) - when the task is finished, the activity state is recorded (after
 * stopped). 6- the activity becomes {@link ActivityState#FINISHED}.
 */
public abstract class AbstractActivity implements ExternalActivity {

  private static final long serialVersionUID = -2731157748250833266L;

  /** LOG */
  static final Logger LOG = Logger.getLogger(AbstractActivity.class.getName());
  protected long dbid;

  protected String activityName;

  public static final String BODY_FINISHED = "bodyFinished";
  public static final String BODY_SKIPPED = "bodySkipped";
  public static final String ACT_INSTANCE_FINISHED = "instFinished";
  public static final String DEADLINE_EVENT_SIGNAL = "timer";
  public static final String ASYNC_SIGNAL = "async";

  protected AbstractActivity() {
  }

  public AbstractActivity(String activityName) {
    this.activityName = activityName;
  }

  /**
   * Return true if the execution can continue
   */
  protected abstract boolean executeBusinessLogic(Execution execution);

  protected abstract boolean bodyStartAutomatically();

  public void execute(final Execution execution, final boolean checkJoinType) {
    ActivityDefinition activity = execution.getNode();
    if (activity.isAsynchronous()) {
      Authentication.setUserId(BonitaConstants.SYSTEM_USER);
    }
    // If instance is ended, don't execute next node.
    if (execution.getInstance().getInstanceState().equals(InstanceState.FINISHED)) {
      if (AbstractActivity.LOG.isLoggable(Level.FINE)) {
        AbstractActivity.LOG.fine("Instance ended : " + execution.getInstance());
      }
      execution.end();
      final Execution parent = execution.getParent();
      if (parent != null) {
        parent.removeExecution(execution);
      }
      return;
    }
    // Execute node.
    boolean joinOK = true;
    joinOK = ActivityUtil.isJoinOk(execution.getInstance(), execution.getNode());
    if (joinOK || !checkJoinType) {
      if (AbstractActivity.LOG.isLoggable(Level.FINE)) {
        AbstractActivity.LOG.fine("Join for activity " + this + " is OK.");
      }
      if (activity.getJoinType().equals(JoinType.XOR)) {
        cancelJoinXORIncomingTransitions(execution);
      }

      if (AbstractActivity.LOG.isLoggable(Level.FINE)) {
        AbstractActivity.LOG.fine("Creating a new iteration on activity : " + this);
      }
      ActivityUtil.createNewIteration(execution, activity);

      if (AbstractActivity.LOG.isLoggable(Level.FINE)) {
        final String nodeName = activity.getName();
        AbstractActivity.LOG.fine("Executing node: " + nodeName + ", class = " + this.getClass().getSimpleName());
      }

      MultiInstantiationDefinition multiInstantiator = activity.getMultiInstantiationDefinition();
      MultiInstantiationDefinition instantiator = activity.getMultipleInstancesInstantiator();
      if (multiInstantiator != null || instantiator != null) {
        instantiateMultiInstanceActivity(execution);
      } else {

        if (activity.isInALoop() && activity.evaluateLoopConditionBeforeExecution()) {
          if (ActivityUtil.evaluateLoopCondition(activity, execution)) {
            Execution newExecution = execution.createChildExecution(execution.getNode().getName());
            initializeActivityInstance(newExecution, null);
            startActivityInstance(newExecution);
          } else {
            terminateInstanceIfNoOutgoingTransitions(execution);
            executeSplit(execution, false);
          }

        } else {
          Execution newExecution = execution.createChildExecution(execution.getNode().getName());
          initializeActivityInstance(newExecution, null);
          startActivityInstance(newExecution);
        }
      }
    } else {
      execution.end();
      final Execution parent = execution.getParent();
      if (parent != null) {
        parent.removeExecution(execution);
      }
    }
  }

  private void instantiateMultiInstanceActivity(Execution execution) {
    ActivityDefinition activity = execution.getNode();
    MultiInstantiationDefinition instantiator = activity.getMultipleInstancesInstantiator();

    /*
     * if (multiInstantiator != null) { ConnectorDefinitionImpl inst = new
     * ConnectorDefinitionImpl(MultiInstantiatorInstantiator.class.getName());
     * inst.addParameter("setClassName", multiInstantiator.getClassName());
     * inst.addParameter("variableName", multiInstantiator.getVariableName());
     * instantiator = inst; }
     */

    final Recorder recorder = EnvTool.getRecorder();

    if (instantiator != null) {
      if (AbstractActivity.LOG.isLoggable(Level.FINE)) {
        AbstractActivity.LOG.fine("MultipleActivitiesInstantiation not null on activity " + this);
      }
      List<Map<String, Object>> contexts = new ArrayList<Map<String, Object>>();
      try {
        contexts = ConnectorExecutor.executeMultipleInstancesInstantiatior(instantiator, execution.getInstance().getUUID(), activityName, execution.getIterationId());
        if (contexts == null) {
          String message = ExceptionManager.getInstance().getFullMessage("be_AA_8", activity.getName());
          throw new BonitaRuntimeException(message);
        } else if (contexts.isEmpty()) {
          String message = ExceptionManager.getInstance().getFullMessage("be_AA_9", activity.getName());
          throw new BonitaRuntimeException(message);
        }
      } catch (Exception e) {
        throw new BonitaRuntimeException(e.getMessage(), e);
      }
      execution.setWaitingForActivityInstanceNb(contexts.size());
      int childId = 0;
      final List<Execution> activitiesToStart = new ArrayList<Execution>();

      RuntimeException caughtException = null;
      for (Map<String, Object> context : contexts) {
        if (execution.getWaitingForActivityInstanceNb() <= 0) {
          // maybe this execution is ended
          break;
        }
        Execution childExec = createChildExecution(execution, childId);

        Set<Variable> variables = new HashSet<Variable>();
        try {
          if (context != null) {
            for (Entry<String, Object> variable : context.entrySet()) {
              Variable multiInstVar = VariableUtil.createVariable(activity.getProcessDefinitionUUID(), variable.getKey(), variable.getValue());
              variables.add(multiInstVar);
            }
          }
        } catch (final RuntimeException e) {
          if (LOG.isLoggable(Level.SEVERE)) {
            LOG.severe("Error while creating multiInstantiator variables" + e);
          }
          caughtException = e;
        }
        try {
          this.initializeActivityInstance(childExec, variables);
          activitiesToStart.add(childExec);
        } catch (final RuntimeException e) {
          if (LOG.isLoggable(Level.SEVERE)) {
            LOG.severe("Error while initializing multiple instances" + e);
          }
          caughtException = e;
        }
        if (caughtException != null) {
          final InternalActivityInstance activityInstance = childExec.getActivityInstance();
          if (activityInstance != null) {
            recorder.recordActivityFailed(activityInstance);
          } else {
            throw caughtException;
          }
          caughtException = null;
        }
        childId++;
      }
      startActivityInstances(activitiesToStart);

    } else {
      if (AbstractActivity.LOG.isLoggable(Level.FINE)) {
        AbstractActivity.LOG.fine("MultiInstantiation not null on activity " + this);
      }
      MultiInstantiatorDescriptor actInstDescr = null;
      MultiInstantiationDefinition multiDef = activity.getMultiInstantiationDefinition();
      final MultiInstantiator actInstantiator = EnvTool.getClassDataLoader().getInstance(MultiInstantiator.class, execution.getInstance().getProcessDefinitionUUID(), multiDef);
      try {
        actInstDescr = ConnectorExecutor.executeMultiInstantiator(execution, activity.getName(), actInstantiator, multiDef.getParameters());
        if (actInstDescr == null) {
          String message = ExceptionManager.getInstance().getFullMessage("be_AA_3", activity.getName());
          throw new BonitaRuntimeException(message);
        }
      } catch (final Exception e) {
        throw new BonitaWrapperException(new MultiInstantiatorInvocationException("be_AA_4", activity.getMultiInstantiationDefinition().getClassName().toString(), e));
      }

      execution.setWaitingForActivityInstanceNb(actInstDescr.getJoinNumber());
      int childId = 0;
      RuntimeException caughtException = null;
      final List<Execution> activitiesToStart = new ArrayList<Execution>();
      for (final Object value : actInstDescr.getVariableValues()) {
        if (execution.getWaitingForActivityInstanceNb() <= 0) {
          // maybe this execution is ended
          break;
        }
        Execution childExec = createChildExecution(execution, childId);
        Variable multiInstVar = null;
        try {
          multiInstVar = VariableUtil.createVariable(activity.getProcessDefinitionUUID(), activity.getMultiInstantiationDefinition().getVariableName(), value);
        } catch (final RuntimeException e) {
          if (LOG.isLoggable(Level.SEVERE)) {
            LOG.severe("Error while creating multiInstantiator variable" + e);
          }
          caughtException = e;
        }
        final Set<Variable> variables = new HashSet<Variable>();
        if (multiInstVar != null) {
          variables.add(multiInstVar);
        }
        try {
          this.initializeActivityInstance(childExec, variables);
          activitiesToStart.add(childExec);
        } catch (final RuntimeException e) {
          if (LOG.isLoggable(Level.SEVERE)) {
            LOG.severe("Error while initializing multiple instances" + e);
          }
          caughtException = e;
        }

        if (caughtException != null) {
          final InternalActivityInstance activityInstance = childExec.getActivityInstance();
          if (activityInstance != null) {
            recorder.recordActivityFailed(activityInstance);
          } else {
            throw caughtException;
          }
          caughtException = null;
        }

        childId++;
      }
      startActivityInstances(activitiesToStart);
    }
  }

  private Execution createChildExecution(Execution execution, int childId) {
    Execution childExec = execution.createChildExecution(execution.getName() + "/" + childId);
    childExec.setActivityInstanceId(Integer.toString(childId));
    return childExec;
  }

  private void startActivityInstances(final List<Execution> activitiesToStart) {
    for (Execution childExec : activitiesToStart) {
      if (childExec.isActive()) {
        // multi is not yet ended
        try {
          startActivityInstance(childExec);
        } catch (RuntimeException e) {
          final InternalActivityInstance activityInstance = childExec.getActivityInstance();
          if (activityInstance != null) {
            EnvTool.getRecorder().recordActivityFailed(activityInstance);
          } else {
            throw e;
          }
        }
      }
    }
  }

  /**
   * @param execution
   */
  private void cancelJoinXORIncomingTransitions(final Execution execution) {
    final InternalActivityDefinition currentNode = execution.getNode();
    final InternalProcessInstance instance = execution.getInstance();
    cancelJoinXORIncomingTransitions(instance, currentNode, new HashSet<String>());
    for (final TransitionDefinition t : currentNode.getIncomingTransitions()) {
      instance.setTransitionState(t.getName(), TransitionState.ABORTED);
    }
  }

  private void cancelJoinXORIncomingTransitions(final InternalProcessInstance instance, final InternalActivityDefinition currentNode, final Set<String> checkedNodes) {
    final Set<TransitionDefinition> incomingTransitions = currentNode.getIncomingTransitions();
    final String currentNodeName = currentNode.getName();
    if (AbstractActivity.LOG.isLoggable(Level.FINE)) {
      AbstractActivity.LOG.fine("Canceling other branches of the join XOR : " + currentNodeName);
    }
    checkedNodes.add(currentNodeName);
    for (final TransitionDefinition incomingTransition : incomingTransitions) {
      final String sourceNodeName = incomingTransition.getFrom();
      final TransitionState transitionState = instance.getTransitionState(incomingTransition.getName());
      if (!checkedNodes.contains(sourceNodeName) && (transitionState == null || transitionState.equals(TransitionState.READY))) {
        boolean enable = false;
        final InternalActivityDefinition sourceNode = instance.getRootExecution().getProcessDefinition().getActivity(sourceNodeName);
        if (transitionState != null) {
          // disable transition
          instance.setTransitionState(incomingTransition.getName(), TransitionState.ABORTED);

          // check if source node is still enabled
          // it is still enabled if it has at least one READY outgoing transition (in the
          // same cycle if in a cycle)
          top:for (final TransitionDefinition tr : sourceNode.getOutgoingTransitions()) {
            final TransitionState ts = instance.getTransitionState(tr.getName());
            if (ts == null || ts.equals(TransitionState.READY)) {
              if (currentNode.isInCycle()) {
                final ProcessDefinition process = EnvTool.getJournalQueriers().getProcess(instance.getProcessDefinitionUUID());
                for (final IterationDescriptor itDesc : process.getIterationDescriptors()) {
                  if (itDesc.containsNode(tr.getTo())) {
                    // stay in same cycle => do not disable node
                    enable = true;
                    break top;
                  }
                }
              }
            }
          }
        }
        if (!enable) {
          // abort sourceNode recursively: sourceNode is not enabled, maybe it doesn't have any
          //activityInstance. Checks if there is one before
          if (AbstractActivity.LOG.isLoggable(Level.FINE)) {
            AbstractActivity.LOG.fine(sourceNodeName + " has no more outgoing transitions enabled.");
          }
          final List<Execution> execToAbortList = instance.getExecOnNode(sourceNodeName);
          for (final Execution execToAbort : execToAbortList) {
            destroyEvents(execToAbort, sourceNodeName);
            if (!execToAbort.isActive()) {
              execToAbort.unlock();
            }
            execToAbort.abort();
          }
          cancelJoinXORIncomingTransitions(instance, sourceNode, checkedNodes);
        }
      }
    }
  }

  protected void initializeActivityInstance(final Execution internalExecution, final Set<Variable> multiInstanceVariables) {
    final InternalActivityDefinition activity = internalExecution.getNode();
    final ProcessInstanceUUID instanceUUID = internalExecution.getInstance().getUUID();

    final Recorder recorder = EnvTool.getRecorder();

    Map<String, Variable> initialVariables = null;
    RuntimeException exception = null;
    try {
      initialVariables = VariableUtil.createVariables(activity.getDataFields(), instanceUUID, null);
    } catch (RuntimeException t) {
      if (LOG.isLoggable(Level.SEVERE)) {
        LOG.severe("Error while initializingVariables " + t);
      }
      exception = t;
    }
    if (multiInstanceVariables != null) {
      if (initialVariables == null) {
        initialVariables = new HashMap<String, Variable>();
      }
      for (Variable variable : multiInstanceVariables) {
        initialVariables.put(variable.getKey(), variable);
      }
    }
    String loopId = "noLoop";
    if (activity.isInALoop()) {
      loopId = Misc.getUniqueId("lp");
    }

    final String iterationId = internalExecution.getIterationId();
    final String activityInstanceId = internalExecution.getActivityInstanceId();
    final ActivityInstanceUUID activityUUID = new ActivityInstanceUUID(instanceUUID, activity.getName(), iterationId, activityInstanceId, loopId);

    final InternalActivityInstance activityInstance = new InternalActivityInstance(activityUUID, activity, instanceUUID, internalExecution.getInstance()
        .getRootInstanceUUID(), iterationId, activityInstanceId, loopId);

    if(exception == null) {
      activityInstance.setActivityState(ActivityState.READY, BonitaConstants.SYSTEM_USER);
      activityInstance.setVariables(initialVariables);
      //add transient data
      TransientData.addTransientVariables(activityUUID, VariableUtil.createTransientVariables(activity.getDataFields(), instanceUUID));

      recorder.recordEnterActivity(activityInstance);

      if (activity.getDynamicDescription() != null) {
        try {
          if(GroovyExpression.isGroovyExpression(activity.getDynamicDescription())) {
            final Object dynamicDescription = GroovyUtil.evaluate(activity.getDynamicDescription(), null, activityUUID, false, false);
            if (dynamicDescription != null) {
              activityInstance.setDynamicDescription(dynamicDescription.toString());
            }
          } else {
            activityInstance.setDynamicDescription(activity.getDynamicDescription());
          }
        } catch (Exception e) {
          internalExecution.setActivityInstance(activityInstance);
          throw new BonitaWrapperException(new BonitaRuntimeException("Error while evaluating dynamic description: " + activity.getDynamicDescription(), e));
        }
      }
      if (activity.getDynamicLabel() != null) {
        try {
          if(GroovyExpression.isGroovyExpression(activity.getDynamicLabel())) {
            final Object dynamicLabel = GroovyUtil.evaluate(activity.getDynamicLabel(), null, activityUUID, false, false);
            if (dynamicLabel != null) {
              activityInstance.setDynamicLabel(dynamicLabel.toString());
            }
          } else {
            activityInstance.setDynamicLabel(activity.getDynamicLabel());
          }
        } catch (Exception e) {
          internalExecution.setActivityInstance(activityInstance);
          throw new BonitaWrapperException(new BonitaRuntimeException("Error while evaluating dynamic label: " + activity.getDynamicLabel(), e));
        }
      }

      internalExecution.setActivityInstance(activityInstance);
    } else {
      //recordFailed
      if (LOG.isLoggable(Level.SEVERE)) {
        LOG.log(Level.SEVERE, exception.getMessage(), exception);
      }
      recorder.recordEnterActivity(activityInstance);
      recorder.recordActivityFailed(activityInstance);
      internalExecution.setActivityInstance(activityInstance);
      throw exception;
    }
  }

  private void startActivityInstance(final Execution internalExecution) {
    final InternalActivityDefinition activity = internalExecution.getNode();
    final ProcessInstanceUUID instanceUUID = internalExecution.getInstance().getProcessInstanceUUID();
    final ActivityInstanceUUID activityUUID = internalExecution.getActivityInstanceUUID();
    try {
      initializeEvents(internalExecution);
    } catch (GroovyException e) {
      final String message = "Error while initializing events: ";
      throw new BonitaWrapperException(new BonitaRuntimeException(message, e));
    }
    if (activity.isAsynchronous()) {
      final EventService eventService = EnvTool.getEventService();
      String uuid = internalExecution.getEventUUID();
      if (uuid == null) {
        uuid = UUID.randomUUID().toString();
      }
      final String processName = internalExecution.getProcessDefinition().getName();
      final String activityName = activity.getName();
      final String eventName = BonitaConstants.ASYNC_EVENT_PREFIX + activityUUID;
      final long overdue = -1;
      final IncomingEventInstance incoming = new IncomingEventInstance(eventName, null, instanceUUID, activity.getUUID(), activityUUID, processName, activityName, uuid, AbstractActivity.ASYNC_SIGNAL,
          System.currentTimeMillis(), true);
      final OutgoingEventInstance outgoing = new OutgoingEventInstance(eventName, processName, activityName, null, instanceUUID, activityUUID, overdue);
      internalExecution.setEventUUID(uuid);

      eventService.fire(outgoing);
      eventService.subscribe(incoming);
      internalExecution.lock("async continuation " + eventName);
    } else {
      executeActivityInstance(internalExecution);
    }
  }

  protected void executeActivityInstance(final Execution internalExecution) {
    final boolean canContinue = this.executeBody(internalExecution);
    if (canContinue) {
      this.end(internalExecution);
    } else {
      internalExecution.waitForSignal();
    }
  }

  protected void end(final Execution internalExecution) {
    try {
      ActivityDefinition activity = internalExecution.getNode();

      if (activity.getDynamicExecutionSummary() != null) {
        try {
          if(GroovyExpression.isGroovyExpression(activity.getDynamicExecutionSummary())) {
            final Object dynamicExecutionSummary = GroovyUtil.evaluate(activity.getDynamicExecutionSummary(), null, internalExecution.getActivityInstanceUUID(), false, false);
            if (dynamicExecutionSummary != null) {
              internalExecution.getActivityInstance().setDynamicExecutionSummary(dynamicExecutionSummary.toString());
            }
          } else {
            internalExecution.getActivityInstance().setDynamicExecutionSummary(activity.getDynamicExecutionSummary());
          }
        } catch (Exception e) {
          throw new BonitaWrapperException(new BonitaRuntimeException("Error while evaluating dynamic execution summary: " + activity.getDynamicExecutionSummary(), e));
        }
      }

      EnvTool.getRecorder().recordBodyEnded(internalExecution.getActivityInstance());

      if (activity.getMultiInstantiationDefinition() != null || activity.getMultipleInstancesInstantiator() != null) {
        endMultiInstantiation(internalExecution);
      } else if (activity.isInALoop()) {
        endLoop(internalExecution);
      } else {
        terminateInstanceIfNoOutgoingTransitions(internalExecution);
        executeSplit(internalExecution, true);
      }
    } catch (final RuntimeException e) {
      if (LOG.isLoggable(Level.SEVERE)) {
        LOG.log(Level.SEVERE, e.getMessage(), e);
      }
      final InternalActivityInstance activityInstance = internalExecution.getActivityInstance();
      EnvTool.getRecorder().recordActivityFailed(activityInstance);
    }
    finally {
      TransientData.removeTransientData(internalExecution.getActivityInstance().getUUID());
    }
  }

  protected void skip(final Execution internalExecution){
    ActivityDefinition activity = internalExecution.getNode();

    if (activity.getDynamicExecutionSummary() != null) {
      try {
        if(GroovyExpression.isGroovyExpression(activity.getDynamicExecutionSummary())) {
          final Object dynamicExecutionSummary = GroovyUtil.evaluate(activity.getDynamicExecutionSummary(), 
              null, internalExecution.getActivityInstanceUUID(), false, false);
          if (dynamicExecutionSummary != null) {
            internalExecution.getActivityInstance().setDynamicExecutionSummary(dynamicExecutionSummary.toString());
          }
        } else {
          internalExecution.getActivityInstance().setDynamicExecutionSummary(activity.getDynamicExecutionSummary());
        }
      } catch (Exception e) {
        throw new BonitaWrapperException(new BonitaRuntimeException("Error while evaluating dynamic execution summary: " + activity.getDynamicExecutionSummary(), e));
      }
    }

    if (activity.getMultiInstantiationDefinition() != null || activity.getMultipleInstancesInstantiator() != null) {
      removeChildrenActivityInstances(internalExecution);
      terminateInstanceIfNoOutgoingTransitions(internalExecution);
      executeSplit(internalExecution, false);
    } else {
      terminateInstanceIfNoOutgoingTransitions(internalExecution);
      executeSplit(internalExecution, true);
    }

  }

  protected void endMultiInstantiation(final Execution internalExecution) {
    final ActivityDefinition activity = internalExecution.getNode();
    final Execution parent = internalExecution.getParent();
    MultiInstantiationDefinition joinChecker = activity.getMultipleInstancesJoinChecker();
    if (joinChecker != null) {
      boolean join = false;
      try {
        join = ConnectorExecutor.executeMultipleInstancesJoinChecker(joinChecker, internalExecution.getActivityInstance().getUUID());
      } catch (Exception e) {
        throw new BonitaRuntimeException(e.getMessage(), e);
      }
      if (join) {
        parent.setWaitingForActivityInstanceNb(1);
      } else {
        if (parent.getWaitingForActivityInstanceNb() == 1) {
          parent.setWaitingForActivityInstanceNb(2);
        }
      }
    }
    destroyEvents(internalExecution, internalExecution.getNodeName());
    internalExecution.end();
    parent.removeExecution(internalExecution);
    this.signal(parent, AbstractActivity.ACT_INSTANCE_FINISHED, null);
  }

  protected void endLoop(final Execution internalExecution) {
    final ActivityDefinition activity = internalExecution.getNode();
    final Execution parent = internalExecution.getParent();
    int maxIterations = 0;
    String maxIterationsExpr = activity.getLoopMaximum();
    if (maxIterationsExpr != null) {
      try {
        if (Misc.isJustAGroovyExpression(maxIterationsExpr)) {
          final ProcessInstanceUUID instanceUUID = internalExecution.getInstance().getUUID();
          final Querier journal = EnvTool.getJournalQueriers();
          final ActivityInstance activityInstance = journal.getActivityInstance(instanceUUID, internalExecution.getNodeName(), internalExecution.getIterationId(), internalExecution
              .getActivityInstanceId(), internalExecution.getActivityInstance().getLoopId());
          maxIterations = (Integer) GroovyUtil.evaluate(maxIterationsExpr, null, activityInstance.getUUID(), false, false);
        } else {
          maxIterations = Integer.parseInt(maxIterationsExpr);
        }
      } catch (Exception e) {
        AbstractActivity.LOG.log(Level.SEVERE, "The maximum number of loop iterations for activity " + activityName + " must be an integer or an expression that evaluates to an integer", e);
      }
    }
    parent.setWaitingForActivityInstanceNb(maxIterations);

    this.signal(parent, AbstractActivity.ACT_INSTANCE_FINISHED, null);
    if (!internalExecution.isFinished()) {
      // the number of iteration reached the maximum loop iterations authorized
      boolean loop = true;
      if (!activity.evaluateLoopConditionBeforeExecution()) {
        loop = ActivityUtil.evaluateLoopCondition(activity, internalExecution);
      }
      if (loop) {
        parent.removeExecution(internalExecution);
        execute(parent, false);
      } else {
        terminateInstanceIfNoOutgoingTransitions(internalExecution);
        executeSplit(internalExecution, true);
      }
    }
  }

  private void terminateInstanceIfNoOutgoingTransitions(final Execution internalExecution) {
    ActivityDefinition activity = internalExecution.getNode();
    final InternalProcessInstance instance = internalExecution.getInstance();
    final ProcessInstanceUUID instanceUUID = instance.getUUID();
    final ActivityInstanceUUID activityUUID = internalExecution.getActivityInstanceUUID();
    if (activity.isTerminateProcess()
        || !(activity.hasOutgoingTransitions()  || hasStillReadyActivities(instanceUUID, activityUUID)
            || hasStillReadyTransitions(instance))) {

      final ProcessInstanceUUID parentInstanceUUID = instance.getParentInstanceUUID();
      ConnectorExecutor.executeConnectors(internalExecution, HookDefinition.Event.instanceOnFinish);
      final Recorder recorder = EnvTool.getRecorder();
      recorder.recordInstanceEnded(instance.getUUID(), EnvTool.getUserId());
      ProcessUtil.removeInternalInstanceEvents(instance.getUUID());

      if (parentInstanceUUID != null) {
        if(ProcessType.EVENT_SUB_PROCESS.equals(internalExecution.getProcessDefinition().getType())) {
          final InternalProcessInstance parentInstance = EnvTool.getJournalQueriers().getProcessInstance(parentInstanceUUID);
          if (parentInstance.getParentActivityUUID() != null) {
            final Execution rootExecution = EnvTool.getAllQueriers().getExecutionOnActivity(parentInstance.getParentInstanceUUID(), parentInstance.getParentActivityUUID());
            try {
              rootExecution.getNode().getBehaviour().signal(rootExecution, BODY_FINISHED, null);
            } catch (Exception e) {
              throw new BonitaRuntimeException(e.getMessage(), e);
            }
          } else {
            parentInstance.finish();
          }
        } else {
          // We are in a subflow
          final Map<String, Object> signalParameters = new HashMap<String, Object>();
          signalParameters.put("childInstanceUUID", instanceUUID);

          final InternalProcessInstance parentInstance = EnvTool.getJournalQueriers().getProcessInstance(parentInstanceUUID);
          final Execution parentRootExecution = parentInstance.getRootExecution();
          final Execution execToSignal = getSubflowExecution(parentRootExecution, instanceUUID);

          try {
            execToSignal.getNode().getBehaviour().signal(execToSignal, SubFlow.SUBFLOW_SIGNAL, signalParameters);
          } catch (Exception e) {
            throw new BonitaRuntimeException(e.getMessage(), e);
          }
        }
      } else {
        instance.finish();
      }
    }
  }

  private boolean hasStillReadyActivities(final ProcessInstanceUUID instanceUUID, final ActivityInstanceUUID activityUUID) {
    return EnvTool.getJournalQueriers().containsOtherActiveActivities(instanceUUID, activityUUID);
  }

  private boolean hasStillReadyTransitions(final InternalProcessInstance instance) {
    boolean hasStillReadyTransitions = false;
    final Iterator<String> iterator = instance.getTransitionsStates().values().iterator();
    while (!hasStillReadyTransitions && iterator.hasNext()) {
      final String state = iterator.next();
      if(TransitionState.READY.toString().equals(state)){
        hasStillReadyTransitions = true;
      }
    }
    return hasStillReadyTransitions;
  }

  private Execution getSubflowExecution(Execution exec, ProcessInstanceUUID subflowInstanceUUID) {
    if (exec.getActivityInstance() != null && exec.getActivityInstance().getSubflowProcessInstanceUUID() != null
        && exec.getActivityInstance().getSubflowProcessInstanceUUID().equals(subflowInstanceUUID)) {
      return exec;
    }
    for (Execution child : exec.getExecutions()) {
      final Execution e = getSubflowExecution(child, subflowInstanceUUID);
      if (e != null) {
        return e;
      }
    }
    return null;
  }

  @SuppressWarnings("deprecation")
  public void signal(final Execution execution, final String signal, final Map<String, Object> parameters) {
    final Execution internalExecution = (Execution) execution;
    InternalActivityDefinition activity = execution.getNode();
    if (AbstractActivity.BODY_FINISHED.equals(signal)) {
      this.end(internalExecution);
    } else if (AbstractActivity.ACT_INSTANCE_FINISHED.equals(signal)) {
      if (activity.getMultiInstantiationDefinition() != null || activity.getMultipleInstancesInstantiator() != null) {
        // in case of a multi-instantiation
        internalExecution.setWaitingForActivityInstanceNb(internalExecution.getWaitingForActivityInstanceNb() - 1);
        if (internalExecution.getWaitingForActivityInstanceNb() == 0) {
          removeChildrenActivityInstances(internalExecution);
          terminateInstanceIfNoOutgoingTransitions(internalExecution);
          executeSplit(internalExecution, false);
        }
      } else {
        // in case of a loop
        internalExecution.setActivityInstanceNb(internalExecution.getActivityInstanceNb() + 1);
        int maxIterations = internalExecution.getWaitingForActivityInstanceNb();
        if (0 < maxIterations && maxIterations <= internalExecution.getActivityInstanceNb()) {
          endChildrenActivityInstances(internalExecution);
          terminateInstanceIfNoOutgoingTransitions(internalExecution);
          executeSplit(internalExecution, false);
        }
      }
    } else if (ASYNC_SIGNAL.equals(signal)) {
      Authentication.setUserId(BonitaConstants.SYSTEM_USER);
      executeActivityInstance(internalExecution);
    } else if (DEADLINE_EVENT_SIGNAL.equals(signal)) {
      final Long id = (Long) parameters.get("id");
      DeadlineDefinition deadline = null;
      if (id != null) {
        deadline = getMatchingDeadline(id, activity.getDeadlines());
      } else {
        final String className = (String) parameters.get("className");
        deadline = getCompatibleDeadline(className, activity.getDeadlines());
      }
      if (deadline != null) {
        Authentication.setUserId(BonitaConstants.SYSTEM_USER);
        // By default, a deadline does not propagate execution
        internalExecution.waitForSignal();
        final String activityId = internalExecution.getNode().getName();
        ConnectorExecutor.executeConnector(internalExecution, activityId, deadline);
      }
    } else if (signal.startsWith(EventConstants.BOUNDARY_EVENT)) {
      destroyEvents(internalExecution, activityName);
      InternalActivityInstance activityInstance = internalExecution.getActivityInstance();
      EnvTool.getRecorder().recordBodyAborted(activityInstance);
      TransientData.removeTransientData(activityInstance.getUUID());
      if (activity.isSubflow()) {
        final InternalProcessInstance subprocessInstance = EnvTool.getJournalQueriers().getProcessInstance(activityInstance.getSubflowProcessInstanceUUID());
        EnvTool.getRecorder().recordInstanceAborted(subprocessInstance.getUUID(), BonitaConstants.SYSTEM_USER);
      }
      internalExecution.setActivityInstance(null);
      if (EventConstants.MESSAGE_BOUNDARY_EVENT.equals(signal)) {
        ConnectorExecutor.executeConnectors(activity, execution, Event.onEvent, parameters);
      }
      final BoundaryEvent event = activity.getBoundaryEvent(parameters.get("eventName").toString());
      final TransitionDefinition exceptionTransition = event.getTransition();
      internalExecution.take(exceptionTransition);
    } else if (AbstractActivity.BODY_SKIPPED.equals(signal)) {
      this.skip(internalExecution);
    }
  }

  private DeadlineDefinition getCompatibleDeadline(final String className, final Set<DeadlineDefinition> deadlines) {
    for (DeadlineDefinition deadline : deadlines) {
      if (deadline.getClassName().equals(className)) {
        return deadline;
      }
    }
    return null;
  }

  private DeadlineDefinition getMatchingDeadline(final Long id, final Set<DeadlineDefinition> deadlines) {
    for (DeadlineDefinition d : deadlines) {
      InternalConnectorDefinition deadline = (InternalConnectorDefinition) d;
      if (deadline.getDbid() == id) {
        return deadline;
      }
    }
    return null;
  }

  private void removeChildrenActivityInstances(final Execution execution) {
    if (execution.getExecutions() != null) {
      for (final Execution execToAbort : new ArrayList<Execution>(execution.getExecutions())) {
        execToAbort.abort();
      }
    }
  }

  private void endChildrenActivityInstances(final Execution execution) {
    if (execution.getExecutions() != null) {
      for (final Execution execToEnd : new ArrayList<Execution>(execution.getExecutions())) {
        execToEnd.end();
        execution.removeExecution(execToEnd);
      }
    }
  }

  private void initializeEvents(final Execution execution) throws GroovyException {
    final InternalActivityDefinition activity = execution.getNode();
    final Set<DeadlineDefinition> deadlines = activity.getDeadlines();
    final List<BoundaryEvent> boundaryEvents = activity.getBoundaryEvents();
    if (!deadlines.isEmpty() || !boundaryEvents.isEmpty()) {
      final String executionEventUUID = "event-" + UUID.randomUUID().toString();
      execution.setEventUUID(executionEventUUID);
      initializeTimers(execution, executionEventUUID);
      initializeBoundaryEvents(execution, executionEventUUID);
    }
  }

  private void initializeTimers(final Execution execution, final String executionEventUUID) throws GroovyException {
    // initialize the timers
    final InternalActivityDefinition activity = execution.getNode();
    final ActivityInstanceUUID activityUUID = execution.getActivityInstanceUUID();
    final ActivityInstance activityInstance = execution.getActivityInstance();
    final Set<DeadlineDefinition> deadlines = activity.getDeadlines();
    if (!deadlines.isEmpty()) {
      final EventService eventService = EnvTool.getEventService();
      final String processName = execution.getProcessDefinition().getName();
      final ProcessInstanceUUID instanceUUID = activityInstance.getProcessInstanceUUID();
      final String activityName = activity.getName();
      final String eventName = BonitaConstants.DEADLINE_EVENT_PREFIX + activityUUID;
      for (final DeadlineDefinition d : deadlines) {
        InternalConnectorDefinition deadline = (InternalConnectorDefinition) d;
        final String condition = deadline.getCondition();
        long enableTime = ProcessUtil.getTimerDate(condition, activityUUID).getTime();
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("id", deadline.getDbid());

        final IncomingEventInstance incoming = new IncomingEventInstance(eventName, "id == " + deadline.getDbid(), instanceUUID, activity.getUUID(), activityUUID, processName,
            activityName, executionEventUUID, DEADLINE_EVENT_SIGNAL, enableTime, false);
        final OutgoingEventInstance outgoing = new OutgoingEventInstance(eventName, processName, activityName, parameters, instanceUUID, activityUUID, -1);
        eventService.subscribe(incoming);
        eventService.fire(outgoing);
      }
    }
  }

  private void initializeBoundaryEvents(final Execution execution, final String executionEventUUID) throws GroovyException {
    final InternalActivityDefinition activity = execution.getNode();
    final List<BoundaryEvent> boundaryEvents = activity.getBoundaryEvents();
    if (!boundaryEvents.isEmpty()) {
      final ActivityInstanceUUID activityUUID = execution.getActivityInstanceUUID();
      final ActivityInstance activityInstance = execution.getActivityInstance();
      final ProcessInstanceUUID instanceUUID = activityInstance.getProcessInstanceUUID();
      final EventService eventService = EnvTool.getEventService();
      final String processName = execution.getProcessDefinition().getName();
      for (BoundaryEvent boundaryEvent : boundaryEvents) {
        if (boundaryEvent instanceof TimerBoundaryEventImpl) {
          final TimerBoundaryEventImpl timer = (TimerBoundaryEventImpl) boundaryEvent;
          final String eventName = timer.getName();
          final String condition = timer.getCondition();
          final OutgoingEventInstance outgoing = new OutgoingEventInstance(eventName, processName, activityName, null, instanceUUID, activityUUID, -1);
          eventService.fire(outgoing);
          final Date date = (Date) ProcessUtil.getTimerDate(condition, activityUUID);
          IncomingEventInstance timerEventInstance = new IncomingEventInstance(eventName, null, instanceUUID, activity.getUUID(), activityUUID, processName, activityName, executionEventUUID, EventConstants.TIMER_BOUNDARY_EVENT, date.getTime(), false);
          eventService.subscribe(timerEventInstance);
        } else if (boundaryEvent instanceof MessageBoundaryEventImpl) {
          final MessageBoundaryEventImpl message = (MessageBoundaryEventImpl) boundaryEvent;
          final String expression = message.getExpression();
          IncomingEventInstance incomingEventInstance = new IncomingEventInstance(message.getName(), expression, instanceUUID,
              activity.getUUID(), activityUUID, processName, activityName,
              executionEventUUID, EventConstants.MESSAGE_BOUNDARY_EVENT, System.currentTimeMillis(), true);
          eventService.subscribe(incomingEventInstance);
        } else if (boundaryEvent instanceof ErrorBoundaryEventImpl) {
          final ErrorBoundaryEventImpl error = (ErrorBoundaryEventImpl) boundaryEvent;
          final String errorCode = error.getErrorCode();
          StringBuilder builder = new StringBuilder(error.getName());
          builder.append(EventConstants.SEPARATOR);
          if (errorCode != null) {
            builder.append(errorCode);
          } else {
            builder.append("all");
          }
          IncomingEventInstance incomingEventInstance = new IncomingEventInstance(builder.toString(), null, instanceUUID,
              activity.getUUID(), activityUUID, processName, activityName, executionEventUUID, EventConstants.ERROR_BOUNDARY_EVENT,
              System.currentTimeMillis(), true);
          eventService.subscribe(incomingEventInstance);
        } else if (boundaryEvent instanceof SignalBoundaryEventImpl) {
          final SignalBoundaryEventImpl signal = (SignalBoundaryEventImpl) boundaryEvent;
          final String signalName = signal.getSignalCode();
          IncomingEventInstance signalEventInstance = new IncomingEventInstance(signalName, null, instanceUUID, activity.getUUID(),
              null, processName, signal.getName(), executionEventUUID, EventConstants.SIGNAL_BOUNDARY_EVENT, -1, false);
          eventService.subscribe(signalEventInstance);
        }
      }
    }
  }

  private static void destroyEvents(final Execution execution, final String activityName) {
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("destroying events of " + execution.toString());
    }
    final ActivityInstanceUUID activityUUID = execution.getActivityInstanceUUID();
    final String processName = execution.getProcessDefinition().getName();
    if (activityUUID != null) {
      ActivityUtil.deleteBoundaryEvents(activityUUID);
      ActivityUtil.deleteEvents(BonitaConstants.TIMER_EVENT_PREFIX + activityUUID, processName, activityName, activityUUID);
      ActivityUtil.deleteEvents(BonitaConstants.DEADLINE_EVENT_PREFIX + activityUUID, processName, activityName, activityUUID);
      ActivityUtil.deleteEvents(BonitaConstants.ASYNC_EVENT_PREFIX + activityUUID, processName, activityName, activityUUID);
    }
  }

  private boolean executeBody(final Execution internalExecution) {
    if (this.bodyStartAutomatically()) {
      EnvTool.getRecorder().recordBodyStarted(internalExecution.getActivityInstance());
    }
    return this.executeBusinessLogic(internalExecution);
  }

  @Override
  public String toString() {
    final StringBuffer buffer = new StringBuffer();
    buffer.append(this.getClass().getName());
    buffer.append(": activtyName: " + getActivityName());
    return buffer.toString();
  }

  public String getActivityName() {
    return activityName;
  }

  public void executeSplit(final Execution execution, final boolean removeScope) {
    final ActivityDefinition activity = execution.getNode();
    Execution internalExecution = (Execution) execution;
    final InternalActivityDefinition currentNode = internalExecution.getNode();
    if (AbstractActivity.LOG.isLoggable(Level.FINE)) {
      AbstractActivity.LOG.fine("node = " + currentNode.getName() + " - splitType = " + activity.getSplitType() + " - execution = " + execution.getName());
    }
    final Set<TransitionDefinition> transitions = currentNode.getOutgoingTransitions();
    if (transitions == null) {
      if (AbstractActivity.LOG.isLoggable(Level.FINE)) {
        AbstractActivity.LOG.fine("node = " + currentNode.getName() + " - splitType = " + activity.getSplitType() + " - execution = " + execution.getName()
            + " no transition available. Ending execution");
      }
      internalExecution.end();
      final Execution parent = internalExecution.getParent();
      if (parent != null) {
        parent.removeExecution(internalExecution);
      }
    } else {
      TransitionDefinition defaultTransition = null;
      final List<TransitionDefinition> transitionsToTake = new ArrayList<TransitionDefinition>();
      for (final TransitionDefinition t : transitions) {
        if (t.isDefault()) {
          defaultTransition = t;
        } else if (ActivityUtil.evaluateTransition(t, internalExecution)) {
          final TransitionState transitionState = internalExecution.getInstance().getTransitionState(t.getName());
          if (transitionState == null || transitionState.equals(TransitionState.READY)) {
            internalExecution.getInstance().setTransitionState(t.getName(), TransitionState.READY);
            transitionsToTake.add(t);
          }
        }
      }
      if (defaultTransition != null && transitionsToTake.size() == 0) {
        final TransitionState transitionState = internalExecution.getInstance().getTransitionState(defaultTransition.getName());
        if (transitionState == null) {
          internalExecution.getInstance().setTransitionState(defaultTransition.getName(), TransitionState.READY);
          transitionsToTake.add(defaultTransition);
        }
      }
      // remove not propagated variables
      if (removeScope) {
        destroyEvents(internalExecution, activityName);
        internalExecution = internalExecution.backToParent();
      }
      internalExecution.setActivityInstance(null);
      if (transitionsToTake.size() == 0) {
        internalExecution.end();
        final Execution parent = internalExecution.getParent();
        if (parent != null) {
          parent.removeExecution(internalExecution);
        }
      } else {
        Set<IterationDescriptor> iterationDescriptors = null;
        // check we are leaving a cycle
        if (activity.isInCycle()) {
          final ProcessDefinition process = EnvTool.getJournalQueriers().getProcess(activity.getProcessDefinitionUUID());
          iterationDescriptors = process.getIterationDescriptors();
          for (final IterationDescriptor itD : iterationDescriptors) {
            boolean isLeaving = false;
            for (final TransitionDefinition t : transitionsToTake) {
              if (!itD.containsNode(t.getTo()) && itD.containsNode(t.getFrom())) {
                isLeaving = true;
              }
            }
            if (isLeaving) {
              // abort execution of other nodes.
              if (AbstractActivity.LOG.isLoggable(Level.FINE)) {
                AbstractActivity.LOG.fine(activity.getName() + " is leaving a cycle, aborting other nodes in cycle.");
              }
              for (final String nodeToAbort : itD.getCycleNodes()) {
                if (!nodeToAbort.equals(currentNode.getName())) {
                  final List<Execution> execToAbortList = internalExecution.getInstance().getExecOnNode(nodeToAbort);
                  for (final Execution execToAbort : execToAbortList) {
                    if (execToAbort.isActive()) {
                      execToAbort.abort();
                    }
                  }
                }
              }
            }
          }
        }
        if (transitionsToTake.size() == 1 || SplitType.XOR.equals(activity.getSplitType())) {
          // We are in a Split/AND and only one transition is true,
          // or we are in a Split/XOR, so we take the first one that is true.
          final TransitionDefinition t = transitionsToTake.get(0);
          if (AbstractActivity.LOG.isLoggable(Level.FINE)) {
            AbstractActivity.LOG.fine("Taking transition " + t);
          }
          internalExecution.take(t);
        } else {
          // We are in a Split/AND and more than one transition is true.
          // check we are not leaving a cycle and staying in the cycle at the
          // same time
          if (activity.isInCycle()) {
            for (final IterationDescriptor itD : iterationDescriptors) {
              boolean isLeaving = false;
              boolean isStaying = false;
              for (final TransitionDefinition t : transitionsToTake) {
                if (!itD.containsNode(t.getTo())) {
                  isLeaving = true;
                } else {
                  isStaying = true;
                }
              }
              if (isStaying && isLeaving) {
                String message = ExceptionManager.getInstance().getFullMessage("be_AA_5");
                throw new BonitaWrapperException(new BonitaRuntimeException(message));
              }
            }
          }
          final List<Execution> children = new ArrayList<Execution>();
          for (int i = 0; i < transitionsToTake.size(); i++) {
            final TransitionDefinition t = transitionsToTake.get(i);
            final String name = t.getFrom() + "_to_" + t.getTo();
            final Execution childExecution = internalExecution.createChildExecution(name);
            children.add(childExecution);
          }
          for (int i = 0; i < transitionsToTake.size(); i++) {
            final Execution childExecution = children.get(i);
            final TransitionDefinition t = transitionsToTake.get(i);
            if (!childExecution.isFinished()) {
              if (AbstractActivity.LOG.isLoggable(Level.FINE)) {
                AbstractActivity.LOG.fine("Execution " + childExecution.getName() + " is taking transition " + t);
              }
              childExecution.take(t);
            }
          }
        }
      }
    }
  }

}
