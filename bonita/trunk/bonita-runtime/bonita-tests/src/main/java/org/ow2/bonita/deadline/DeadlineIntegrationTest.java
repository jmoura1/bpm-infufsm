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
package org.ow2.bonita.deadline;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.bonitasoft.connectors.bonita.ExecuteTaskConnector;
import org.bonitasoft.connectors.bonita.FinishTaskConnector;
import org.bonitasoft.connectors.bonita.SetVarConnector;
import org.bonitasoft.connectors.bonita.StartTaskConnector;
import org.bonitasoft.connectors.scripting.GroovyConnector;
import org.ow2.bonita.APITestCase;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.runtime.ActivityState;
import org.ow2.bonita.facade.runtime.InstanceState;
import org.ow2.bonita.facade.runtime.ProcessInstance;
import org.ow2.bonita.facade.runtime.TaskInstance;
import org.ow2.bonita.facade.runtime.VariableUpdate;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.util.BonitaException;
import org.ow2.bonita.util.Misc;
import org.ow2.bonita.util.ProcessBuilder;

/**
 * @author Pascal Verdage
 */
public class DeadlineIntegrationTest extends APITestCase {

  public void testTaskOnDeadline() throws BonitaException {
    URL xpdlUrl = this.getClass().getResource("taskOnDeadline.xpdl");
    ProcessDefinition process = getManagementAPI().deploy(
        getBusinessArchiveFromXpdl(xpdlUrl, RepeatHook.class, SearchDemandHook.class));
    ProcessDefinitionUUID processUUID = process.getUUID();
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID);
    try {
      Thread.sleep(10000);
    } catch (Exception e) {
      fail(Misc.getStackTraceFrom(e));
    }
    checkExecutedOnce(instanceUUID, new String[]{"Wait", "BonitaEnd", "Initial"});
    checkExecutedManyTimes(instanceUUID, new String[]{"searchDemand"}, 2);
    getRuntimeAPI().deleteProcessInstance(instanceUUID);
    getManagementAPI().disable(processUUID);
    getManagementAPI().deleteProcess(processUUID);
  }

  public void testMoreThanADeadline() throws Exception {
    ProcessDefinition definition =
      ProcessBuilder.createProcess("moreThanADeadline", "1.1")
      .addStringData("Abc", "bye")
      .addHuman("john")
      .addHumanTask("start", "john")
        .addDeadline("1000", SetVarConnector.class.getName())
          .addInputParameter("variableName", "Abc")
          .addInputParameter("value", "hello")
        .addDeadline("5000", FinishTaskConnector.class.getName())
      .done();
    ProcessDefinition process = getManagementAPI().deploy(getBusinessArchive(definition, null, SetVarConnector.class, FinishTaskConnector.class));
    ProcessDefinitionUUID processUUID = process.getUUID();
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID);
    loginAs("john", "bpm");
    Set<TaskInstance> tasks = getQueryRuntimeAPI().getTasks(instanceUUID);
    TaskInstance task = tasks.iterator().next();
    getRuntimeAPI().startTask(task.getUUID(), true);    
    waitForInstanceEnd(10000l, 2000l, instanceUUID);
    String actual = (String) getQueryRuntimeAPI().getProcessInstanceVariable(instanceUUID, "Abc");
    Assert.assertEquals("hello", actual);
    waitForInstanceEnd(10000l, 4000l, instanceUUID);
    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    Assert.assertEquals("instance is finished", InstanceState.FINISHED, instance.getInstanceState());
    getManagementAPI().disable(processUUID);
    getManagementAPI().deleteProcess(processUUID);
  }

  public void testStartAndFinishTask() throws Exception {
    ProcessDefinition definition =
      ProcessBuilder.createProcess("moreThanADeadline", "1.1")
      .addHuman("john")
      .addHumanTask("start", "john")
        .addDeadline("1000", StartTaskConnector.class.getName())
        .addDeadline("5000", FinishTaskConnector.class.getName())
      .done();
    ProcessDefinition process = getManagementAPI().deploy(getBusinessArchive(definition, null, FinishTaskConnector.class, StartTaskConnector.class));
    ProcessDefinitionUUID processUUID = process.getUUID();
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID);
    Assert.assertEquals("instance must be finished", InstanceState.STARTED, getQueryRuntimeAPI().getProcessInstance(instanceUUID).getInstanceState());
    Set<TaskInstance> tasks = getQueryRuntimeAPI().getTasks(instanceUUID);
    TaskInstance task = tasks.iterator().next();
    TaskInstance currentTask = getQueryRuntimeAPI().getTask(task.getUUID());
    Assert.assertEquals(ActivityState.READY, currentTask.getState());

    Thread.sleep(3000);
    
    currentTask = getQueryRuntimeAPI().getTask(task.getUUID());
    Assert.assertEquals(ActivityState.EXECUTING, currentTask.getState());
    
    Thread.sleep(5000);
    Assert.assertEquals("instance must be finished", InstanceState.FINISHED, getQueryRuntimeAPI().getProcessInstance(instanceUUID).getInstanceState());
    getManagementAPI().disable(processUUID);
    getManagementAPI().deleteProcess(processUUID);
  }

  public void testExecuteTask() throws Exception {
    ProcessDefinition definition =
      ProcessBuilder.createProcess("moreThanADeadline", "1.1")
      .addHuman("john")
      .addHumanTask("start", "john")
        .addDeadline("1000", ExecuteTaskConnector.class.getName())
      .done();
    ProcessDefinition process = getManagementAPI().deploy(getBusinessArchive(definition, null, ExecuteTaskConnector.class));
    ProcessDefinitionUUID processUUID = process.getUUID();
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID);
    Set<TaskInstance> tasks = getQueryRuntimeAPI().getTasks(instanceUUID);
    TaskInstance task = tasks.iterator().next();
    TaskInstance currentTask = getQueryRuntimeAPI().getTask(task.getUUID());
    Assert.assertEquals(ActivityState.READY, currentTask.getState());
    waitForInstanceEnd(3000l, 2000l, instanceUUID);
    currentTask = getQueryRuntimeAPI().getTask(task.getUUID());
    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    Assert.assertEquals("instance is finished", InstanceState.FINISHED, instance.getInstanceState());
    getManagementAPI().disable(processUUID);
    getManagementAPI().deleteProcess(processUUID);
  }

  public void testFinishTaskWithGroovyOutput() throws Exception {
  	ProcessDefinition definition =
      ProcessBuilder.createProcess("moreThanADeadline", "1.1")
      .addIntegerData("count", 0)
      .addHuman("john")
      .addHumanTask("start", "john")
        .addDeadline("1000", GroovyConnector.class.getName())
          .addInputParameter("script", "1")
          .addOutputParameter("${(result + result) * 4}", "count")
        .addDeadline("5000", FinishTaskConnector.class.getName())
      .done();
    ProcessDefinition process = getManagementAPI().deploy(getBusinessArchive(definition, null, GroovyConnector.class, FinishTaskConnector.class));
    ProcessDefinitionUUID processUUID = process.getUUID();
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID);
    loginAs("john", "bpm");
    Set<TaskInstance> tasks = getQueryRuntimeAPI().getTasks(instanceUUID);
    TaskInstance task = tasks.iterator().next();
    getRuntimeAPI().startTask(task.getUUID(), true);
    waitForInstanceEnd(7000, 50, instanceUUID);
    Integer actual = (Integer) getQueryRuntimeAPI().getProcessInstanceVariable(instanceUUID, "count");
    Assert.assertEquals(Integer.valueOf(8), actual);
    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    Assert.assertEquals("instance is finished", InstanceState.FINISHED, instance.getInstanceState());
    getManagementAPI().disable(processUUID);
    getManagementAPI().deleteProcess(processUUID);
  }

  public void testExecuteTaskInACycle() throws Exception {
    long waitingTime = 1500;

    ProcessDefinition definition =
      ProcessBuilder.createProcess("DeadlineAndCycle", "1.1")
      .addHuman(getLogin())
      .addBooleanData("testdata", false)
      .addSystemTask("start")
      .addHumanTask("step1", getLogin())
        .addDeadline("1000", ExecuteTaskConnector.class.getName())
          .addInputParameter("setActivityName", "step1")
      .addHumanTask("step2", getLogin())
      .addHumanTask("step3", getLogin())
      .addSystemTask("end")
      .addTransition("start", "step1")
      .addTransition("step1", "step2")
        .addCondition("testdata")
      .addTransition("step1", "step3")
        .addCondition("!testdata")
      .addTransition("step2", "end")
      .addTransition("step3", "step1")
      .done();

    ProcessDefinition process = getManagementAPI().deploy(getBusinessArchive(definition, null, ExecuteTaskConnector.class));
    ProcessDefinitionUUID processUUID = process.getUUID();
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID);
    Thread.sleep(waitingTime);

    Collection<TaskInstance> tasks = getQueryRuntimeAPI().getTaskList(instanceUUID, ActivityState.READY);
    assertEquals(1, tasks.size());
    TaskInstance task = tasks.iterator().next();
    assertEquals("step3", task.getActivityName());
    executeTask(instanceUUID, "step3");
    Thread.sleep(waitingTime);
    tasks = getQueryRuntimeAPI().getTaskList(instanceUUID, ActivityState.READY);
    assertEquals(1, tasks.size());
    task = tasks.iterator().next();
    assertEquals("step3", task.getActivityName());
    getRuntimeAPI().setProcessInstanceVariable(instanceUUID, "testdata", true);
    executeTask(instanceUUID, "step3");
    Thread.sleep(waitingTime);

    tasks = getQueryRuntimeAPI().getTaskList(instanceUUID, ActivityState.READY);
    assertEquals(1, tasks.size());
    task = tasks.iterator().next();
    assertEquals("step2", task.getActivityName());

    getManagementAPI().deleteProcess(processUUID);
  }

  public void testStartAndFinishTaskInACycle() throws Exception {
    ProcessDefinition definition =
      ProcessBuilder.createProcess("DeadlineAndCycle", "1.2")
      .addHuman(getLogin())
      .addBooleanData("testdata", false)
      .addSystemTask("start")
      .addHumanTask("step1", getLogin())
        .addDeadline("1000", StartTaskConnector.class.getName())
          .addInputParameter("setActivityName", "step1")
        .addDeadline("4000", FinishTaskConnector.class.getName())
          .addInputParameter("setActivityName", "step1")
      .addHumanTask("step2", getLogin())
      .addHumanTask("step3", getLogin())
      .addSystemTask("end")
      .addTransition("start", "step1")
      .addTransition("step1", "step2")
        .addCondition("testdata")
      .addTransition("step1", "step3")
        .addCondition("!testdata")
      .addTransition("step2", "end")
      .addTransition("step3", "step1")
      .done();

    ProcessDefinition process = getManagementAPI().deploy(getBusinessArchive(definition, null, FinishTaskConnector.class, StartTaskConnector.class));
    ProcessDefinitionUUID processUUID = process.getUUID();
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID);
    Collection<TaskInstance> tasks = getQueryRuntimeAPI().getTaskList(instanceUUID, ActivityState.READY);
    assertEquals(1, tasks.size());
    TaskInstance task = tasks.iterator().next();
    assertEquals(ActivityState.READY, task.getState());

    Thread.sleep(1500);
    task = getQueryRuntimeAPI().getTask(task.getUUID());
    assertEquals(ActivityState.EXECUTING, task.getState());

    Thread.sleep(3000);
    tasks = getQueryRuntimeAPI().getTaskList(instanceUUID, ActivityState.READY);
    assertEquals(1, tasks.size());
    task = tasks.iterator().next();
    assertEquals("step3", task.getActivityName());
    executeTask(instanceUUID, "step3");
    Thread.sleep(4500);
    tasks = getQueryRuntimeAPI().getTaskList(instanceUUID, ActivityState.READY);
    assertEquals(1, tasks.size());
    task = tasks.iterator().next();
    assertEquals("step3", task.getActivityName());
    getRuntimeAPI().setProcessInstanceVariable(instanceUUID, "testdata", true);
    executeTask(instanceUUID, "step3");
    Thread.sleep(4500);

    tasks = getQueryRuntimeAPI().getTaskList(instanceUUID, ActivityState.READY);
    assertEquals(1, tasks.size());
    task = tasks.iterator().next();
    assertEquals("step2", task.getActivityName());

    getManagementAPI().deleteProcess(processUUID);
  }

  public void testTwoDeadlinesUsingTheSameConnector() throws Exception {
    ProcessDefinition definition =
      ProcessBuilder.createProcess("TwoConnectors", "1.2")
      .addHuman(getLogin())
      .addSystemTask("start")
      .addHumanTask("step1", getLogin())
        .addStringData("text")
        .addDeadline("1000", SetVarConnector.class.getName())
          .addInputParameter("variableName", "text")
          .addInputParameter("value", "hello")
        .addDeadline("4000", SetVarConnector.class.getName())
          .addInputParameter("variableName", "text")
          .addInputParameter("value", "world")
      .addSystemTask("end")
      .addTransition("start", "step1")
      .addTransition("step1", "end")
      .done();

    ProcessDefinition process = getManagementAPI().deploy(getBusinessArchive(definition, null, SetVarConnector.class));
    ProcessDefinitionUUID processUUID = process.getUUID();
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID);
    Thread.sleep(5000);

    Collection<TaskInstance> tasks = getQueryRuntimeAPI().getTaskList(instanceUUID, ActivityState.READY);
    assertEquals(1, tasks.size());
    TaskInstance task = tasks.iterator().next();
    assertEquals(ActivityState.READY, task.getState());

    List<VariableUpdate> variables = task.getVariableUpdates();
    assertEquals(2, variables.size());
    VariableUpdate hello = variables.get(0);
    VariableUpdate world = variables.get(1);

    assertTrue(hello.getDate().getTime() < world.getDate().getTime());
    assertEquals("hello", hello.getValue());
    assertEquals("world", world.getValue());

    getManagementAPI().deleteProcess(processUUID);
  }

}