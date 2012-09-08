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
package org.ow2.bonita.variable;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bonitasoft.connectors.bonita.joincheckers.FixedNumberJoinChecker;
import org.bonitasoft.connectors.bonita.resolvers.ProcessInitiatorRoleResolver;
import org.ow2.bonita.activity.multipleinstances.instantiator.NoContextMulitpleActivitiesInstantiator;
import org.ow2.bonita.facade.def.element.BusinessArchive;
import org.ow2.bonita.facade.def.element.HookDefinition.Event;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition;
import org.ow2.bonita.facade.def.majorElement.DataFieldDefinition;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.exception.ActivityNotFoundException;
import org.ow2.bonita.facade.exception.VariableNotFoundException;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.ActivityState;
import org.ow2.bonita.facade.runtime.InstanceState;
import org.ow2.bonita.facade.runtime.ProcessInstance;
import org.ow2.bonita.facade.runtime.TaskInstance;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.integration.connector.test.InputOutputConnector;
import org.ow2.bonita.light.LightActivityInstance;
import org.ow2.bonita.light.LightProcessInstance;
import org.ow2.bonita.light.LightTaskInstance;
import org.ow2.bonita.util.BusinessArchiveFactory;
import org.ow2.bonita.util.DateUtil;
import org.ow2.bonita.util.ProcessBuilder;

/**
 * @author Elias Ricken de Medeiros
 *
 */
public class TransientVariablesTest extends VariableTestCase {
  
  public void testActivityDefinitionSetTransient() throws Exception {
    ProcessDefinition process = ProcessBuilder.createProcess("prcssWithTransientData", "1.0")
      .addSystemTask("activityWithTransientData")
        .addStringData("transient")
          .setTransient()
      .done();
    
    process = getManagementAPI().deploy(BusinessArchiveFactory.getBusinessArchive(process));
    
    ActivityDefinition activity = process.getActivity("activityWithTransientData");
    Set<DataFieldDefinition> dataFields = activity.getDataFields();
    assertEquals(1, dataFields.size());
    assertTrue(dataFields.iterator().next().isTransient());
    
    getManagementAPI().deleteProcess(process.getUUID());
  }
  
  public void testActivityDefinitionSetTransientWithTwoTrainsientVariables() throws Exception {
    ProcessDefinition process = ProcessBuilder.createProcess("prcssWithTransientData", "1.1")
      .addSystemTask("activityWithTransientData")
        .addStringData("transient")
          .setTransient()
        .addLongData("longTransient")
          .setTransient()
      .done();
    
    process = getManagementAPI().deploy(BusinessArchiveFactory.getBusinessArchive(process));
    
    ActivityDefinition activity = process.getActivity("activityWithTransientData");
    Set<DataFieldDefinition> dataFields = activity.getDataFields();
    assertEquals(2, dataFields.size());
    assertTrue(dataFields.iterator().next().isTransient());
    assertTrue(dataFields.iterator().next().isTransient());
    
    getManagementAPI().deleteProcess(process.getUUID());
  }
  
  public void testActivityDefinitionSetTransientWithOneTransientVariableAndOnePersitentVariable() throws Exception {
    ProcessDefinition process = ProcessBuilder.createProcess("prcssWithTransientData", "1.2")
    .addSystemTask("activityWithTransientData")
      .addStringData("notTransient")
      .addStringData("transient")
        .setTransient()
    .done();
  
    process = getManagementAPI().deploy(BusinessArchiveFactory.getBusinessArchive(process));
    
    ActivityDefinition activity = process.getActivity("activityWithTransientData");
    Set<DataFieldDefinition> dataFields = activity.getDataFields();
    assertEquals(2, dataFields.size());
    for (DataFieldDefinition dataFieldDefinition : dataFields) {
      if ("notTransient".equals(dataFieldDefinition.getName())) {
        assertFalse(dataFieldDefinition.isTransient());
      } else {
        assertTrue(dataFieldDefinition.isTransient());
      }
    }
    
    getManagementAPI().deleteProcess(process.getUUID());
  }
  
  public void testActivityDefinitionSetTransientWithTwoActivities() throws Exception {
    ProcessDefinition process = ProcessBuilder.createProcess("prcssWithTransientData", "1.3")
      .addSystemTask("activity1WithTransientData")
        .addStringData("transient")
          .setTransient()
      .addSystemTask("activity2WithTransientData")
        .addBooleanData("transientbool")
          .setTransient()
      .done();
    
    process = getManagementAPI().deploy(BusinessArchiveFactory.getBusinessArchive(process));
    
    ActivityDefinition activity = process.getActivity("activity1WithTransientData");
    Set<DataFieldDefinition> dataFields = activity.getDataFields();
    assertEquals(1, dataFields.size());
    assertTrue(dataFields.iterator().next().isTransient());
    
    activity = process.getActivity("activity2WithTransientData");
    dataFields = activity.getDataFields();
    assertEquals(1, dataFields.size());
    assertTrue(dataFields.iterator().next().isTransient());
    
    getManagementAPI().deleteProcess(process.getUUID());
  }
  
  public void testActivityInstanceWithTransientVariable() throws Exception {
    ProcessDefinition process = ProcessBuilder.createProcess("prcssWithTransientData", "1.4")
      .addHuman(getLogin())
      .addHumanTask("taskWithTransientData", getLogin())
        .addStringData("notTransient", "hello")
        .addStringData("transient", "not persited")
          .setTransient()
      .addHumanTask("simpleTask", getLogin())
      .addTransition("taskWithTransientData", "simpleTask")
      .done();
    
    process = getManagementAPI().deploy(BusinessArchiveFactory.getBusinessArchive(process));
    
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    
    Set<LightActivityInstance> activityInstances = getQueryRuntimeAPI().getLightActivityInstances(instanceUUID, "taskWithTransientData");
    assertEquals(1, activityInstances.size());
    
    LightActivityInstance activityInstance = activityInstances.iterator().next();
    assertEquals(ActivityState.READY, activityInstance.getState());
    String variable = (String) getQueryRuntimeAPI().getActivityInstanceVariable(activityInstance.getUUID(), "notTransient");
    assertEquals("hello", variable);
    
    variable = (String) getQueryRuntimeAPI().getActivityInstanceVariable(activityInstance.getUUID(), "transient");
    assertEquals("not persited", variable);
    
    executeTask(instanceUUID, "taskWithTransientData");
    
    variable = (String) getQueryRuntimeAPI().getActivityInstanceVariable(activityInstance.getUUID(), "notTransient");
    assertEquals("hello", variable);
    
    try {
      getQueryRuntimeAPI().getActivityInstanceVariable(activityInstance.getUUID(), "transient");
      fail("A transient variable must not exist after the end of the activity");
    } catch (VariableNotFoundException e) {
    } 
    
    getManagementAPI().deleteProcess(process.getUUID());
  }
  
  public void testActivityInstancWitheTransientVariableForCancelledActivity() throws Exception {
    ProcessDefinition process = ProcessBuilder.createProcess("prcssWithTransientData", "1.5")
      .addHuman(getLogin())
      .addHumanTask("taskWithTransientData", getLogin())
        .addStringData("transient", "not persited")
          .setTransient()
      .done();
    
    process = getManagementAPI().deploy(BusinessArchiveFactory.getBusinessArchive(process));
    
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    
    Set<LightActivityInstance> activityInstances = getQueryRuntimeAPI().getLightActivityInstances(instanceUUID, "taskWithTransientData");
    assertEquals(1, activityInstances.size());
    LightActivityInstance activityInstance = activityInstances.iterator().next();
    
    String variable = (String) getQueryRuntimeAPI().getActivityInstanceVariable(activityInstance.getUUID(), "transient");
    assertEquals("not persited", variable);

    getRuntimeAPI().cancelProcessInstance(instanceUUID);
    
    try {
      getQueryRuntimeAPI().getActivityInstanceVariable(activityInstance.getUUID(), "transient");
      fail("A transient variable must not exist if the activity was cancelled");
    } catch (VariableNotFoundException e) {
    } 
    
    getManagementAPI().deleteProcess(process.getUUID());
  }
  
  public void testActivityInstanceWithTransientVariableForSkippedActivity() throws Exception {
    ProcessDefinition process = ProcessBuilder.createProcess("prcssWithTransientData", "1.6")
      .addHuman(getLogin())
      .addHumanTask("taskWithTransientData", getLogin())
        .addStringData("transient", "not persited")
          .setTransient()
      .done();
    
    process = getManagementAPI().deploy(BusinessArchiveFactory.getBusinessArchive(process));
    
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    
    Set<LightActivityInstance> activityInstances = getQueryRuntimeAPI().getLightActivityInstances(instanceUUID, "taskWithTransientData");
    assertEquals(1, activityInstances.size());
    LightActivityInstance activityInstance = activityInstances.iterator().next();
    
    String variable = (String) getQueryRuntimeAPI().getActivityInstanceVariable(activityInstance.getUUID(), "transient");
    assertEquals("not persited", variable);

    getRuntimeAPI().skipTask(activityInstance.getUUID(), null);
    
    try {
      getQueryRuntimeAPI().getActivityInstanceVariable(activityInstance.getUUID(), "transient");
      fail("A transient variable must not exist if the task was skipped");
    } catch (VariableNotFoundException e) {
    } 
    
    getManagementAPI().deleteProcess(process.getUUID());
  }
  
  public void testSetActivityInstanceVariableWithTransientVariable() throws Exception {
    ProcessDefinition process = ProcessBuilder.createProcess("prcssWithTransientData", "1.7")
      .addHuman(getLogin())
      .addHumanTask("taskWithTransientData", getLogin())
        .addStringData("transient", "not persited")
          .setTransient()
      .done();
    
    process = getManagementAPI().deploy(BusinessArchiveFactory.getBusinessArchive(process));
    
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    
    Set<LightActivityInstance> activityInstances = getQueryRuntimeAPI().getLightActivityInstances(instanceUUID, "taskWithTransientData");
    assertEquals(1, activityInstances.size());
    LightActivityInstance activityInstance = activityInstances.iterator().next();
    
    String variable = (String) getQueryRuntimeAPI().getActivityInstanceVariable(activityInstance.getUUID(), "transient");
    assertEquals("not persited", variable);
    
    getRuntimeAPI().setActivityInstanceVariable(activityInstance.getUUID(), "transient", "notPersitedButUpdated");
    
    variable = (String) getQueryRuntimeAPI().getActivityInstanceVariable(activityInstance.getUUID(), "transient");
    assertEquals("notPersitedButUpdated", variable);

    executeTask(instanceUUID, "taskWithTransientData");
    
    try {
      getRuntimeAPI().setActivityInstanceVariable(activityInstance.getUUID(), "transient", "hello");
      fail("A transient variable must not exist if the task was skipped");
    } catch (ActivityNotFoundException e) {
    } 
    
    try {
      getQueryRuntimeAPI().getActivityInstanceVariable(activityInstance.getUUID(), "transient");
      fail("A transient variable must not exist if the task was skipped");
    } catch (VariableNotFoundException e) {
    } 
    
    getManagementAPI().deleteProcess(process.getUUID());
  }
  
  public void testTransientVariableWithGroovyExpression() throws Exception {
    ProcessDefinition process = ProcessBuilder.createProcess("prcssWithTransientDataGroovy", "1.0")
      .addHuman(getLogin())
      .addHumanTask("taskWithTransientData", getLogin())
        .addStringData("transientvar", "not persited")
          .setTransient()
      .done();
    
    process = getManagementAPI().deploy(BusinessArchiveFactory.getBusinessArchive(process));
    
    final ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    
    Set<LightActivityInstance> activityInstances = getQueryRuntimeAPI().getLightActivityInstances(instanceUUID, "taskWithTransientData");
    assertEquals(1, activityInstances.size());
    final LightActivityInstance activityInstance = activityInstances.iterator().next();
    
    Object result = getRuntimeAPI().evaluateGroovyExpression("this data is ${transientvar}", activityInstance.getUUID(), true, true);
    assertEquals("this data is not persited", result);
    
    getRuntimeAPI().evaluateGroovyExpression("${transientvar='updatedvalue'}", activityInstance.getUUID(), true, true);
    String variable = (String) getQueryRuntimeAPI().getActivityInstanceVariable(activityInstance.getUUID(), "transientvar");
    assertEquals("updatedvalue", variable);
    
    getRuntimeAPI().evaluateGroovyExpression("${transientvar='updatedtwice'}", activityInstance.getUUID(), false, false);
    variable = (String) getQueryRuntimeAPI().getActivityInstanceVariable(activityInstance.getUUID(), "transientvar");
    assertEquals("updatedvalue", variable);
    
    executeTask(instanceUUID, "taskWithTransientData");
    
    try{
      getQueryRuntimeAPI().getActivityInstanceVariable(activityInstance.getUUID(), "transientvar");
      fail("A transient variable must not be persisted in the data base");
    } catch (VariableNotFoundException e) {
    } 

    getManagementAPI().deleteProcess(process.getUUID());
  }
  
  public void testTransientVariableWithConnector() throws Exception {
    ProcessDefinition process = getManagementAPI().deploy(getBusinessArchiveForTwoChainedConnectors());
    final ProcessDefinitionUUID processUUID = process.getUUID();
    final ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID);
    
    Set<ActivityInstance> activityInstances = getQueryRuntimeAPI().getActivityInstances(instanceUUID, "Request");
    assertEquals(1, activityInstances.size());
    ActivityInstance activityInstance = activityInstances.iterator().next();
    final ActivityInstanceUUID activityUUID = activityInstance.getUUID();
    assertEquals(5, getQueryRuntimeAPI().getActivityInstanceVariables(activityUUID).size());
    String message = (String) getQueryRuntimeAPI().getActivityInstanceVariable(activityUUID, "message");
    String exptectedMessage = 848754512 + " SunnyTown" + " " + (new Double(20.1)) + " " + DateUtil.parseDate("2010-12-02T13:12:54.000+0200");
    assertEquals(exptectedMessage, message);
    
    getRuntimeAPI().startTask(activityUUID, false);
    
    message = (String) getQueryRuntimeAPI().getActivityInstanceVariable(activityUUID, "message");
    exptectedMessage = 848754512 + " " + exptectedMessage + " " + (new Double(20.1)) + " " + DateUtil.parseDate("2010-12-02T13:12:54.000+0200");
    assertEquals(exptectedMessage, message);
    
    getRuntimeAPI().finishTask(activityUUID, false);
    assertEquals(0, getQueryRuntimeAPI().getActivityInstanceVariables(activityUUID).size());
    
    getManagementAPI().deleteProcess(processUUID);
  }
  
  private BusinessArchive getBusinessArchiveForTwoChainedConnectors() throws IOException {
    ProcessDefinition process =
      ProcessBuilder.createProcess("InputOutputWithTransientVariable", "1.0")
      .addHuman(getLogin())
      .addHumanTask("Request", getLogin())
        .addDateData("myDate", DateUtil.parseDate("2010-12-02T13:12:54.000+0200"))
          .setTransient()
        .addIntegerData("phoneNumber", 848754512)
          .setTransient()
        .addStringData("Where", "SunnyTown")
          .setTransient()
        .addStringData("message", "Nothing")
          .setTransient()
        .addDoubleData("price", new Double(20.1))
          .setTransient()
        .addConnector(Event.taskOnReady, InputOutputConnector.class.getName(), true)
          .addOutputParameter("${output}", "message")
          .addInputParameter("thirdInput", "${price}")
          .addInputParameter("firstInput", "${phoneNumber}")
          .addInputParameter("secondInput", "${Where}")
          .addInputParameter("fourthInput", "${myDate}")
        .addConnector(Event.taskOnStart, InputOutputConnector.class.getName(), true)
          .addOutputParameter("${output}", "message")
          .addInputParameter("thirdInput", "${price}")
          .addInputParameter("firstInput", "${phoneNumber}")
          .addInputParameter("secondInput", "${message}")
          .addInputParameter("fourthInput", "${myDate}")
      .done();
    return getBusinessArchive(process, getResourcesFromConnector(InputOutputConnector.class), InputOutputConnector.class, ProcessInitiatorRoleResolver.class);
  }
  
  public void testTransientVariableWithConnectorOnFinish() throws Exception {
    ProcessDefinition process = getManagementAPI().deploy(getBusinessArchiveForConnector(Event.taskOnFinish));
    final ProcessDefinitionUUID processUUID = process.getUUID();
    final ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID);
    
    Set<ActivityInstance> activityInstances = getQueryRuntimeAPI().getActivityInstances(instanceUUID, "Request");
    assertEquals(1, activityInstances.size());
    ActivityInstance activityInstance = activityInstances.iterator().next();
    final ActivityInstanceUUID activityUUID = activityInstance.getUUID();
    assertEquals(5, getQueryRuntimeAPI().getActivityInstanceVariables(activityUUID).size());
    String message = (String) getQueryRuntimeAPI().getActivityInstanceVariable(activityUUID, "message");
    assertEquals("Nothing", message);
    
    getRuntimeAPI().executeTask(activityUUID, false);
    
    assertEquals(1, getQueryRuntimeAPI().getActivityInstanceVariables(activityUUID).size());
    message = (String) getQueryRuntimeAPI().getActivityInstanceVariable(activityUUID, "message");
    String exptectedMessage = 848754512 + " SunnyTown" + " " + (new Double(20.1)) + " " + DateUtil.parseDate("2010-12-02T13:12:54.000+0200");
    assertEquals(exptectedMessage, message);
    
    getManagementAPI().deleteProcess(processUUID);
  }
  
  private BusinessArchive getBusinessArchiveForConnector(Event event) throws IOException {
    ProcessDefinition process =
      ProcessBuilder.createProcess("InputOutputWithTransientVariable", "1.0")
      .addHuman(getLogin())
      .addHumanTask("Request", getLogin())
        .addDateData("myDate", DateUtil.parseDate("2010-12-02T13:12:54.000+0200"))
          .setTransient()
        .addIntegerData("phoneNumber", 848754512)
          .setTransient()
        .addStringData("Where", "SunnyTown")
          .setTransient()
        .addStringData("message", "Nothing")
        .addDoubleData("price", new Double(20.1))
          .setTransient()
        .addConnector(event, InputOutputConnector.class.getName(), true)
          .addOutputParameter("${output}", "message")
          .addInputParameter("thirdInput", "${price}")
          .addInputParameter("firstInput", "${phoneNumber}")
          .addInputParameter("secondInput", "${Where}")
          .addInputParameter("fourthInput", "${myDate}")
      .done();
    return getBusinessArchive(process, getResourcesFromConnector(InputOutputConnector.class), InputOutputConnector.class, ProcessInitiatorRoleResolver.class);
  }
  
  public void testTransientVariableWithConnectorOnCancel() throws Exception {
    ProcessDefinition process = getManagementAPI().deploy(getBusinessArchiveForConnector(Event.taskOnCancel));
    final ProcessDefinitionUUID processUUID = process.getUUID();
    final ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID);
    
    Set<ActivityInstance> activityInstances = getQueryRuntimeAPI().getActivityInstances(instanceUUID, "Request");
    assertEquals(1, activityInstances.size());
    ActivityInstance activityInstance = activityInstances.iterator().next();
    final ActivityInstanceUUID activityUUID = activityInstance.getUUID();
    assertEquals(5, getQueryRuntimeAPI().getActivityInstanceVariables(activityUUID).size());
    String message = (String) getQueryRuntimeAPI().getActivityInstanceVariable(activityUUID, "message");
    assertEquals("Nothing", message);
    
    getRuntimeAPI().cancelProcessInstance(instanceUUID);
    
    //connectors are not executed on cancel task
    assertEquals(1, getQueryRuntimeAPI().getActivityInstanceVariables(activityUUID).size());
    message = (String) getQueryRuntimeAPI().getActivityInstanceVariable(activityUUID, "message");
    assertEquals("Nothing", message);
    
    getManagementAPI().deleteProcess(processUUID);
  }
  
  public void testTransientVariableWithConnectorAutomaticOnEnter() throws Exception {
    ProcessDefinition process = getManagementAPI().deploy(getBusinessArchiveForConnectorAutomatic(Event.automaticOnEnter));
    final ProcessDefinitionUUID processUUID = process.getUUID();
    final ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID);
    
    Set<ActivityInstance> activityInstances = getQueryRuntimeAPI().getActivityInstances(instanceUUID, "Request");
    assertEquals(1, activityInstances.size());
    ActivityInstance activityInstance = activityInstances.iterator().next();
    assertEquals(ActivityState.FINISHED, activityInstance.getState());

    final ActivityInstanceUUID activityUUID = activityInstance.getUUID();
    
    String message = (String) getQueryRuntimeAPI().getActivityInstanceVariable(activityUUID, "message");
    String exptectedMessage = 848754512 + " SunnyTown" + " " + (new Double(20.1)) + " " + DateUtil.parseDate("2010-12-02T13:12:54.000+0200");
    assertEquals(exptectedMessage, message);
    
    getManagementAPI().deleteProcess(processUUID);
  }
  
  public void testTransientVariableWithConnectorAutomaticOnExit() throws Exception {
    ProcessDefinition process = getManagementAPI().deploy(getBusinessArchiveForConnectorAutomatic(Event.automaticOnExit));
    final ProcessDefinitionUUID processUUID = process.getUUID();
    final ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID);
    
    Set<ActivityInstance> activityInstances = getQueryRuntimeAPI().getActivityInstances(instanceUUID, "Request");
    assertEquals(1, activityInstances.size());
    ActivityInstance activityInstance = activityInstances.iterator().next();
    assertEquals(ActivityState.FINISHED, activityInstance.getState());
    
    final ActivityInstanceUUID activityUUID = activityInstance.getUUID();
    
    String message = (String) getQueryRuntimeAPI().getActivityInstanceVariable(activityUUID, "message");
    String exptectedMessage = 848754512 + " SunnyTown" + " " + (new Double(20.1)) + " " + DateUtil.parseDate("2010-12-02T13:12:54.000+0200");
    assertEquals(exptectedMessage, message);
    
    getManagementAPI().deleteProcess(processUUID);
  }
  
  private BusinessArchive getBusinessArchiveForConnectorAutomatic(Event event) throws IOException {
    ProcessDefinition process =
      ProcessBuilder.createProcess("InputOutputWithTransientVariable", "1.0")
      .addHuman(getLogin())
      .addSystemTask("Request")
        .addDateData("myDate", DateUtil.parseDate("2010-12-02T13:12:54.000+0200"))
          .setTransient()
        .addIntegerData("phoneNumber", 848754512)
          .setTransient()
        .addStringData("Where", "SunnyTown")
          .setTransient()
        .addStringData("message", "Nothing")
        .addDoubleData("price", new Double(20.1))
          .setTransient()
        .addConnector(event, InputOutputConnector.class.getName(), true)
          .addOutputParameter("${output}", "message")
          .addInputParameter("thirdInput", "${price}")
          .addInputParameter("firstInput", "${phoneNumber}")
          .addInputParameter("secondInput", "${Where}")
          .addInputParameter("fourthInput", "${myDate}")
      .done();
    return getBusinessArchive(process, getResourcesFromConnector(InputOutputConnector.class), InputOutputConnector.class, ProcessInitiatorRoleResolver.class);
  }

  public void testTransientVariableWithConnectorOnTimer() throws Exception {
    ProcessDefinition process =
      ProcessBuilder.createProcess("InputOutputWithTransientVariable", "1.0")
      .addSystemTask("start")
      .addTimerTask("Timer", "${100}")
        .addDateData("myDate", DateUtil.parseDate("2010-12-02T13:12:54.000+0200"))
          .setTransient()
        .addIntegerData("phoneNumber", 848754512)
          .setTransient()
        .addStringData("Where", "SunnyTown")
          .setTransient()
        .addStringData("message", "Nothing")
        .addDoubleData("price", new Double(20.1))
          .setTransient()
        .addConnector(Event.onTimer, InputOutputConnector.class.getName(), true)
          .addOutputParameter("${output}", "message")
          .addInputParameter("thirdInput", "${price}")
          .addInputParameter("firstInput", "${phoneNumber}")
          .addInputParameter("secondInput", "${Where}")
          .addInputParameter("fourthInput", "${myDate}")
      .addSystemTask("end")
      .addTransition("start", "Timer")
      .addTransition("Timer", "end")
      .done();

    BusinessArchive archive = getBusinessArchive(process, getResourcesFromConnector(InputOutputConnector.class), InputOutputConnector.class);
    getManagementAPI().deploy(archive);
    final ProcessDefinitionUUID processUUID = process.getUUID();
    final ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID);
    waitForInstanceEnd(3000, 100, instanceUUID);

    Set<ActivityInstance> activityInstances = getQueryRuntimeAPI().getActivityInstances(instanceUUID, "Timer");
    assertEquals(1, activityInstances.size());
    ActivityInstance activityInstance = activityInstances.iterator().next();
    assertEquals(ActivityState.FINISHED, activityInstance.getState());

    final ActivityInstanceUUID activityUUID = activityInstance.getUUID();

    String message = (String) getQueryRuntimeAPI().getActivityInstanceVariable(activityUUID, "message");
    String exptectedMessage = 848754512 + " SunnyTown" + " " + (new Double(20.1)) + " " + DateUtil.parseDate("2010-12-02T13:12:54.000+0200");
    assertEquals(exptectedMessage, message);

    getManagementAPI().deleteProcess(processUUID);
  }

  public void testVariableTransientWithMultipleInstanceTask() throws Exception {
    ProcessDefinition definition =
      ProcessBuilder.createProcess("multiple", "1.0")
      .addHuman(getLogin())
      .addHumanTask("multiWithTransientData", getLogin())
        .addStringData("text", "hello")
          .setTransient()
      .addMultipleActivitiesInstantiator(NoContextMulitpleActivitiesInstantiator.class.getName())
      .addInputParameter("number", 10)
      .addMultipleActivitiesJoinChecker(FixedNumberJoinChecker.class.getName())
      .addInputParameter("activityNumber", 3)
      .done();

    definition = getManagementAPI().deploy(getBusinessArchive(definition, null,
        NoContextMulitpleActivitiesInstantiator.class, FixedNumberJoinChecker.class));
    final ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(definition.getUUID());
    
    Collection<LightTaskInstance> tasks = getQueryRuntimeAPI().getLightTaskList(getLogin(), ActivityState.FINISHED);
    assertEquals(0, tasks.size());
    tasks = getQueryRuntimeAPI().getLightTaskList(getLogin(), ActivityState.READY);
    assertEquals(10, tasks.size());
    for (LightTaskInstance lightTaskInstance : tasks) {
      assertEquals("multiWithTransientData", lightTaskInstance.getActivityName());
      String text = (String) getQueryRuntimeAPI().getActivityInstanceVariable(lightTaskInstance.getUUID(), "text");
      assertEquals("hello", text);
      getRuntimeAPI().setActivityInstanceVariable(lightTaskInstance.getUUID(), "text", "updated");
    }

    Iterator<LightTaskInstance> iterator = tasks.iterator();
    for (int i = 0; i < 3; i++) {
      getRuntimeAPI().executeTask(iterator.next().getUUID(), false);
    }
    
    tasks = getQueryRuntimeAPI().getLightTaskList(getLogin(), ActivityState.READY);
    assertEquals(0, tasks.size());
    
    Set<ActivityState> taskStates = new HashSet<ActivityState>();
    taskStates.add(ActivityState.FINISHED);
    taskStates.add(ActivityState.ABORTED);
    tasks = getQueryRuntimeAPI().getLightTaskList(instanceUUID, taskStates);
    assertEquals(10, tasks.size());
    
    for (LightTaskInstance lightTaskInstance : tasks) {
      assertEquals("multiWithTransientData", lightTaskInstance.getActivityName());
      try {
        getQueryRuntimeAPI().getActivityInstanceVariable(lightTaskInstance.getUUID(), "text");
        fail("Some transient variables were not remove after the of the activity");
      } catch (VariableNotFoundException e) {
        
      } 
    }
       
    getManagementAPI().deleteProcess(definition.getUUID());
  }
  
  public void testVariableTransientWithLoopTask() throws Exception {
    ProcessDefinition definition = 
      ProcessBuilder.createProcess("loopState", "1.0")
      .addIntegerData("counter", 0)
      .addHuman(getLogin())
      .addHumanTask("loop", getLogin())
        .addLoop("counter != 1", true)
        .addStringData("text", "hello")
          .setTransient()
      .addSystemTask("end")
      .addTransition("loop", "end")
      .done();

    definition = getManagementAPI().deploy(getBusinessArchive(definition));

    Collection<LightTaskInstance> tasks = null;
    LightTaskInstance looptask = null;
    
    final ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(definition.getUUID());
    
    tasks = getQueryRuntimeAPI().getLightTaskList(instanceUUID, ActivityState.READY);
    assertEquals(1, tasks.size());
    looptask = tasks.iterator().next();    
    assertEquals("loop", looptask.getActivityName());
    getRuntimeAPI().setActivityInstanceVariable(looptask.getUUID(), "text", "updated");
    getRuntimeAPI().executeTask(looptask.getUUID(), false);
    try {
      getQueryRuntimeAPI().getActivityInstanceVariable(looptask.getUUID(), "text");
      fail("Transient variables were not removed after the end of a loop activity");
    } catch (VariableNotFoundException e) {
      
    } 
    
    tasks = getQueryRuntimeAPI().getLightTaskList(instanceUUID, ActivityState.READY);
    assertEquals(1, tasks.size());
    looptask = tasks.iterator().next();    
    assertEquals("loop", looptask.getActivityName());
    String text = (String) getQueryRuntimeAPI().getActivityInstanceVariable(looptask.getUUID(), "text");
    assertEquals("hello", text);
    getRuntimeAPI().setProcessInstanceVariable(instanceUUID, "counter", 1);
    
    getRuntimeAPI().executeTask(looptask.getUUID(), false);
    try {
      getQueryRuntimeAPI().getActivityInstanceVariable(looptask.getUUID(), "text");
      fail("Transient variables were not removed after the end of a loop activity");
    } catch (VariableNotFoundException e) {
      
    } 
    
    tasks = getQueryRuntimeAPI().getLightTaskList(instanceUUID, ActivityState.READY);
    assertEquals(0, tasks.size());
    
    LightProcessInstance instance = getQueryRuntimeAPI().getLightProcessInstance(instanceUUID);
    assertEquals(InstanceState.FINISHED, instance.getInstanceState());
    
    getRuntimeAPI().deleteProcessInstance(instanceUUID);    
    getManagementAPI().deleteProcess(definition.getUUID());
  }
  
  public void testTransientVariableWithEventSubProcess() throws Exception {
    ProcessDefinition messageProcess =
      ProcessBuilder.createProcess("signal", "1.5")
      .addHuman(getLogin())
      .addHumanTask("start", getLogin())
      .addSendEventTask("msg")
      .addOutgoingEvent("go", "ESB", "start", null)
      .addTransition("start", "msg")
      .done();

    ProcessDefinition eventSubProcess =
      ProcessBuilder.createProcess("ESB", "1.0")
      .setEventSubProcess()
      .addReceiveEventTask("start", "go")
      .addHuman(getLogin())
      .addHumanTask("event", getLogin())
      .addTransition("start", "event")
      .done();

    ProcessDefinition process =
      ProcessBuilder.createProcess("process", "1.0")
      .addHuman(getLogin())
      .addStringData("name", "bonita")
      .addHumanTask("wait", getLogin())
        .addStringData("transientVar", "transient")
          .setTransient()
      .addEventSubProcess("ESB", "1.0")
      .done();

    getManagementAPI().deploy(getBusinessArchive(eventSubProcess));
    getManagementAPI().deploy(getBusinessArchive(process));
    getManagementAPI().deploy(getBusinessArchive(messageProcess));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    Set<ActivityInstance> activityInstances = getQueryRuntimeAPI().getActivityInstances(instanceUUID, "wait");
    assertEquals(1, activityInstances.size());
    
    ActivityInstance actityIntance = activityInstances.iterator().next();
    String transientVar = (String) getQueryRuntimeAPI().getActivityInstanceVariable(actityIntance.getUUID(), "transientVar");
    assertEquals("transient", transientVar);
    
    ProcessInstanceUUID signalUUID = getRuntimeAPI().instantiateProcess(messageProcess.getUUID());
    executeTask(signalUUID, "start");
    waitForInstance(4000, 50, instanceUUID,InstanceState.ABORTED);

    Set<ProcessInstance> eventSubProcesses = getQueryRuntimeAPI().getProcessInstances(eventSubProcess.getUUID());
    assertEquals(1, eventSubProcesses.size());
    ProcessInstanceUUID eventSubProcessUUID = eventSubProcesses.iterator().next().getUUID();

    Set<TaskInstance> tasks = getQueryRuntimeAPI().getTasks(eventSubProcessUUID);
    assertEquals(1, tasks.size());
    assertEquals("event", tasks.iterator().next().getActivityName());

    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.ABORTED, instance.getInstanceState());
    
    try {
      getQueryRuntimeAPI().getActivityInstanceVariable(actityIntance.getUUID(), "transientVar");
    } catch (VariableNotFoundException e) {
      
    } 

    getManagementAPI().deleteProcess(process.getUUID());
    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
    getManagementAPI().deleteProcess(messageProcess.getUUID());
  }
  
  public void testCanUseTransientVariableInTransitionCondition() throws Exception {
    ProcessDefinition process = ProcessBuilder.createProcess("prcssWithTransientData", "1.4")
      .addHuman(getLogin())
      .addHumanTask("taskWithTransientData", getLogin())
        .addBooleanData("doTransition", true)
          .setTransient()
      .addHumanTask("simpleTask", getLogin())
      .addTransition("taskWithTransientData", "simpleTask")
        .addCondition("doTransition")
      .done();
    
    process = getManagementAPI().deploy(BusinessArchiveFactory.getBusinessArchive(process));
    
    final ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    
    Set<LightActivityInstance> activityInstances = getQueryRuntimeAPI().getLightActivityInstances(instanceUUID, "taskWithTransientData");
    assertEquals(1, activityInstances.size());
    
    LightActivityInstance activityInstance = activityInstances.iterator().next();
    assertEquals(ActivityState.READY, activityInstance.getState());
    
    executeTask(instanceUUID, "taskWithTransientData");
    
    activityInstances = getQueryRuntimeAPI().getLightActivityInstances(instanceUUID, "simpleTask");
    assertEquals(1, activityInstances.size());
    
    activityInstance = activityInstances.iterator().next();
    assertEquals(ActivityState.READY, activityInstance.getState());
    
    getManagementAPI().deleteProcess(process.getUUID());
  }
}
