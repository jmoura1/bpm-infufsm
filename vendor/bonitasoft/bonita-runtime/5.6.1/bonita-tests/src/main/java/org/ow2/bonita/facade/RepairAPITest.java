package org.ow2.bonita.facade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.login.LoginContext;

import org.ow2.bonita.APITestCase;
import org.ow2.bonita.definition.MultiInstantiator;
import org.ow2.bonita.definition.MultiInstantiatorDescriptor;
import org.ow2.bonita.facade.def.element.BusinessArchive;
import org.ow2.bonita.facade.def.element.HookDefinition.Event;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition.JoinType;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.ActivityState;
import org.ow2.bonita.facade.runtime.AttachmentInstance;
import org.ow2.bonita.facade.runtime.InitialAttachment;
import org.ow2.bonita.facade.runtime.InstanceState;
import org.ow2.bonita.facade.runtime.ProcessInstance;
import org.ow2.bonita.facade.runtime.TaskInstance;
import org.ow2.bonita.facade.runtime.impl.InitialAttachmentImpl;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.util.ProcessBuilder;
import org.ow2.bonita.util.SimpleCallbackHandler;

public class RepairAPITest extends APITestCase {

  private ProcessDefinition buildSimpleProcess() throws Exception {
    ProcessDefinition simpleProcess = ProcessBuilder.createProcess("simple_process", "1.0")
    .addIntegerData("variable1", 0)
    .addIntegerData("variable2", 0)
    .addAttachment("attachment1")
    .addAttachment("attachment2")
    .addHuman("admin")
    .addHumanTask("task1", "admin")
    .addHumanTask("task2", "admin")
    .addTransition("transition1", "task1", "task2")
    .done();
    
    BusinessArchive businessArchive = getBusinessArchive(simpleProcess);
    return getManagementAPI().deploy(businessArchive);
  }

  private ProcessDefinition buildAutomaticProcess() throws Exception {
    ProcessDefinition autoProcess = ProcessBuilder.createProcess("simple_process", "1.0")
    .addSystemTask("activity1")
    .asynchronous()
    .addSystemTask("activity2")
    .asynchronous()
    .addConnector(Event.automaticOnExit, ErrorConnector.class.getName(), true)
    .addSystemTask("activity3")
    .asynchronous()
    .addTransition("transition1", "activity1", "activity2")
    .addTransition("transition2", "activity2", "activity3")
    .done();
    
    BusinessArchive businessArchive = getBusinessArchive(autoProcess, null, ErrorConnector.class);
    return getManagementAPI().deploy(businessArchive);
  }
  
  private ProcessDefinition buildComplexProcess() throws Exception {
    ProcessDefinition simpleProcess = ProcessBuilder.createProcess("complex_process", "1.0")
    .addIntegerData("number", 0)
    .addStringData("str", "")
    .addHuman("admin")
    .addHumanTask("task1", "admin")
    .addHumanTask("task2", "admin")
    .addSystemTask("task3")
    .addHumanTask("task4", "admin")
    .addTransition("transition1", "task1", "task2")
    .addCondition("number == 0")
    .addTransition("transition2", "task1", "task3")
    .addCondition("number != 0")
    .addTransition("transition3", "task2", "task4")
    .addTransition("transition4", "task3", "task4")
    .done();
    
    BusinessArchive businessArchive = getBusinessArchive(simpleProcess);
    return getManagementAPI().deploy(businessArchive);
  }
  
  public void testAsyncActivityGoToFailed() throws Exception {
    final String activityName = "activity";
    
    ProcessDefinition process = ProcessBuilder.createProcess("asyncFailProcess", "1.0")
    .addSystemTask(activityName)
    .asynchronous()
    .addConnector(Event.automaticOnExit, ErrorConnector.class.getName(), true)
    .done();
    
    process =  getManagementAPI().deploy(getBusinessArchive(process, null, ErrorConnector.class));
    final ProcessDefinitionUUID processUUID = process.getUUID();
    
    ProcessInstanceUUID processInstanceUUID = getRuntimeAPI().instantiateProcess(processUUID);
    
     Thread.sleep(10000);
     checkActivityFailed(processInstanceUUID, activityName);
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  private void checkActivityFailed(final ProcessInstanceUUID processInstanceUUID, final String activityName) throws Exception {
    final Set<ActivityInstance> activityInstances = getQueryRuntimeAPI().getActivityInstances(processInstanceUUID, activityName);
    assertEquals(1, activityInstances.size());
    final ActivityInstance activity = activityInstances.iterator().next();
    assertEquals(ActivityState.FAILED, activity.getState());
  }
  
  private ProcessDefinition buildParallelProcess() throws Exception {
    ProcessDefinition simpleProcess = ProcessBuilder.createProcess("complex_parallel_process", "1.0")
    .addIntegerData("number", 0)
    .addHuman("admin")
    .addHumanTask("task1", "admin")
    .addHumanTask("task2", "admin")
    .addHumanTask("task3", "admin")
    .addDecisionNode("GateWay")
    .addJoinType(JoinType.AND)
    .addHumanTask("task4", "admin")
    .addTransition("transition1", "task1", "task2")
    .addTransition("transition2", "task1", "task3")
    .addTransition("transition3", "task2", "GateWay")
    .addTransition("transition4", "task3", "GateWay")
    .addTransition("transition5", "GateWay", "task4")
    .done();
    
    BusinessArchive businessArchive = getBusinessArchive(simpleProcess);
    return getManagementAPI().deploy(businessArchive);
  }
  
  private ProcessDefinition buildIterationProcess() throws Exception {
    ProcessDefinition simpleProcess = ProcessBuilder.createProcess("complex_iteration_process", "1.0")
    .addIntegerData("number", 0)
    .addHuman("admin")
    .addHumanTask("task1", "admin")
    .addDecisionNode("GateWay")
    .addJoinType(JoinType.XOR)
    .addHumanTask("task2", "admin")
    .addHumanTask("task3", "admin")
    .addHumanTask("task4", "admin")
    .addTransition("transition1", "task1", "GateWay")
    .addTransition("transition2", "GateWay", "task2")
    .addTransition("transition3", "task2", "task3")
    .addCondition("number == 0")
    .addTransition("transition4", "task3", "GateWay")
    .addTransition("transition5", "task2", "task4")
    .addCondition("number != 0")
    .done();
    
    BusinessArchive businessArchive = getBusinessArchive(simpleProcess);
    return getManagementAPI().deploy(businessArchive);
  }
  
  private ProcessDefinition buildMultiInstProcess() throws Exception {
    ProcessDefinition simpleProcess = ProcessBuilder.createProcess("complex_multi_process", "1.0")
    .addHuman("admin")
    .addHumanTask("task1", "admin")
    .addHumanTask("task2", "admin")
    .addIntegerData("number", 0)
    .addMultiInstanciation("number", SimpleMultiInstantiator.class.getName())
    .addHumanTask("task3", "admin")
    .addTransition("transition1", "task1", "task2")
    .addTransition("transition2", "task2", "task3")
    .done();
    
    BusinessArchive businessArchive = getBusinessArchive(simpleProcess, null, SimpleMultiInstantiator.class);
    return getManagementAPI().deploy(businessArchive);
  }
  
  private ProcessDefinition buildProcessWithSubProcess() throws Exception {
    ProcessDefinition simpleProcess = ProcessBuilder.createProcess("sub_process", "1.0")
    .addHuman("admin")
    .addHumanTask("task1", "admin")
    .addSubProcess("subprocess", "simple_process")
    .addHumanTask("task3", "admin")
    .addTransition("transition1", "task1", "subprocess")
    .addTransition("transition2", "subprocess", "task3")
    .done();
    
    BusinessArchive businessArchive = getBusinessArchive(simpleProcess, null, SimpleMultiInstantiator.class);
    return getManagementAPI().deploy(businessArchive);
  }
  
  public void testSimpleStopStartExecution() throws Exception {
    ProcessDefinition simpleProcess = buildSimpleProcess();
    ProcessInstanceUUID processInstanceUUID = getRuntimeAPI().instantiateProcess(simpleProcess.getUUID());
    
    getRepairAPI().stopExecution(processInstanceUUID, "task1");
    
    assertEquals(InstanceState.STARTED, getQueryRuntimeAPI().getProcessInstance(processInstanceUUID).getInstanceState());
    
    getRepairAPI().startExecution(processInstanceUUID, "task2");
    
    assertEquals(1, getQueryRuntimeAPI().getTaskList(ActivityState.READY).size());
    
    Set<TaskInstance> taskInstances = getQueryRuntimeAPI().getTasks(processInstanceUUID);
    assertEquals(2, taskInstances.size());
    for (TaskInstance taskInstance : taskInstances) {
      if(taskInstance.getActivityName().equals("task1")) {
        assertEquals(ActivityState.CANCELLED, taskInstance.getState());
      } else {
        assertEquals(ActivityState.READY, taskInstance.getState());
      }
    }
    
    ActivityInstanceUUID currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    ActivityInstance currentActivityInstance = getQueryRuntimeAPI().getTask(currentActivityInstanceUUID);
    assertEquals("task2", currentActivityInstance.getActivityName());
    
    currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    assertEquals(InstanceState.FINISHED, getQueryRuntimeAPI().getProcessInstance(processInstanceUUID).getInstanceState());
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  public void testSimpleCopyProcessInstance() throws Exception {
    ProcessDefinition simpleProcess = buildSimpleProcess();
    Map<String, Object> processVariables = new HashMap<String, Object>();
    processVariables.put("variable1", 1);
    ProcessInstanceUUID processInstanceUUID = getRuntimeAPI().instantiateProcess(simpleProcess.getUUID(), processVariables);
    ActivityInstanceUUID currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    ProcessInstance processInstance = getQueryRuntimeAPI().getProcessInstance(processInstanceUUID);
    
    loginContext.logout();
    loginContext = new LoginContext("BonitaStore", new SimpleCallbackHandler("john", "bpm"));
    loginContext.login();
    
    Set<InitialAttachment> initialAttachments = new HashSet<InitialAttachment>();
    
    ProcessInstanceUUID copyProcessInstanceUUID = getRepairAPI().copyProcessInstance(processInstanceUUID, new HashMap<String, Object>(), initialAttachments);
    
    Set<TaskInstance> taskInstances = getQueryRuntimeAPI().getTasks(copyProcessInstanceUUID);
    assertEquals(1, taskInstances.size());
    assertEquals("task2", taskInstances.iterator().next().getActivityName());
    assertEquals(ActivityState.READY, taskInstances.iterator().next().getState());
    
    Integer variable1 = (Integer)getQueryRuntimeAPI().getProcessInstanceVariable(copyProcessInstanceUUID, "variable1");
    assertEquals(1, variable1.intValue());
    
    ProcessInstance copyProcessInstance = getQueryRuntimeAPI().getProcessInstance(copyProcessInstanceUUID);
    assertEquals(processInstance.getStartedBy(), copyProcessInstance.getStartedBy());
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getRuntimeAPI().deleteProcessInstance(copyProcessInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  public void testSimpleCopyProcessInstanceAndSetVariables() throws Exception {
    ProcessDefinition simpleProcess = buildSimpleProcess();
    Map<String, Object> processVariables = new HashMap<String, Object>();
    processVariables.put("variable1", 1);
    ProcessInstanceUUID processInstanceUUID = getRuntimeAPI().instantiateProcess(simpleProcess.getUUID(), processVariables);
    ActivityInstanceUUID currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);

    processVariables.put("variable1", 2);
    
    Set<InitialAttachment> initialAttachments = new HashSet<InitialAttachment>();
    InitialAttachmentImpl initialAttachment = new InitialAttachmentImpl("attachment1", new byte[1]);
    initialAttachment.setLabel("label");
    initialAttachment.setFileName("fileName");
    initialAttachments.add(initialAttachment);
    
    ProcessInstanceUUID copyProcessInstanceUUID = getRepairAPI().copyProcessInstance(processInstanceUUID, processVariables, initialAttachments);
    
    Integer variable1 = (Integer)getQueryRuntimeAPI().getProcessInstanceVariable(copyProcessInstanceUUID, "variable1");
    assertEquals(2, variable1.intValue());
    
    AttachmentInstance attachment1 = getQueryRuntimeAPI().getLastAttachment(copyProcessInstanceUUID, "attachment1");
    assertEquals("fileName", attachment1.getFileName());
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getRuntimeAPI().deleteProcessInstance(copyProcessInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  public void testSimpleCopyProcessInstanceAtDate() throws Exception {
    final String attachmentName = "attachment1";
    ProcessDefinition simpleProcess = buildSimpleProcess();
    Map<String, Object> processVariables = new HashMap<String, Object>();
    processVariables.put("variable1", 1);
    Set<InitialAttachment> initialAttachments1 = new HashSet<InitialAttachment>();
    InitialAttachmentImpl initialAttachment1 = new InitialAttachmentImpl(attachmentName, new byte[1]);
    initialAttachment1.setLabel("label1");
    initialAttachment1.setFileName("fileName1");
    initialAttachments1.add(initialAttachment1);
    ProcessInstanceUUID processInstanceUUID = getRuntimeAPI().instantiateProcess(simpleProcess.getUUID(), processVariables, initialAttachments1);

    ActivityInstanceUUID currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);

    Thread.sleep(10);
    getRuntimeAPI().setProcessInstanceVariable(processInstanceUUID, "variable1", 2);
    getRuntimeAPI().addAttachment(processInstanceUUID, attachmentName, "fileName2", new byte[1]);

    long activityEndDate = getQueryRuntimeAPI().getActivityInstance(currentActivityInstanceUUID).getEndedDate().getTime();

    ProcessInstanceUUID copyProcessInstanceUUID = getRepairAPI().copyProcessInstance(processInstanceUUID, new HashMap<String, Object>(), new HashSet<InitialAttachment>(), new Date(activityEndDate));

    Integer variable1 = (Integer)getQueryRuntimeAPI().getProcessInstanceVariable(copyProcessInstanceUUID, "variable1");
    assertEquals(1, variable1.intValue());

    AttachmentInstance attachment1 = getQueryRuntimeAPI().getLastAttachment(copyProcessInstanceUUID, attachmentName);
    assertNull(attachment1.getFileName());

    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getRuntimeAPI().deleteProcessInstance(copyProcessInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }

  public void testSimpleCopyProcessInstanceAtDateAndSetVariables() throws Exception {
    ProcessDefinition simpleProcess = buildSimpleProcess();
    Map<String, Object> processVariables = new HashMap<String, Object>();
    processVariables.put("variable1", 1);
    Set<InitialAttachment> initialAttachments1 = new HashSet<InitialAttachment>();
    InitialAttachmentImpl initialAttachment1 = new InitialAttachmentImpl("attachment1", new byte[1]);
    initialAttachment1.setLabel("label1");
    initialAttachment1.setFileName("fileName1");
    initialAttachments1.add(initialAttachment1);
    ProcessInstanceUUID processInstanceUUID = getRuntimeAPI().instantiateProcess(simpleProcess.getUUID(), processVariables, initialAttachments1);
    
    ActivityInstanceUUID currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    Thread.sleep(2);
    getRuntimeAPI().setProcessInstanceVariable(processInstanceUUID, "variable1", 2);
    getRuntimeAPI().addAttachment(processInstanceUUID, "attachment1", "fileName2", new byte[1]);
    
    long activityEndDate = getQueryRuntimeAPI().getActivityInstance(currentActivityInstanceUUID).getEndedDate().getTime();
    
    processVariables.put("variable2", 3);
    Set<InitialAttachment> initialAttachments3 = new HashSet<InitialAttachment>();
    InitialAttachmentImpl initialAttachment3 = new InitialAttachmentImpl("attachment2", new byte[1]);
    initialAttachment3.setLabel("label3");
    initialAttachment3.setFileName("fileName3");
    initialAttachments3.add(initialAttachment3);
    initialAttachments3.add(initialAttachment1);
    
    ProcessInstanceUUID copyProcessInstanceUUID = getRepairAPI().copyProcessInstance(processInstanceUUID, processVariables, initialAttachments3, new Date(activityEndDate));
    
    Integer variable1 = (Integer)getQueryRuntimeAPI().getProcessInstanceVariable(copyProcessInstanceUUID, "variable1");
    assertEquals(1, variable1.intValue());
    
    AttachmentInstance attachment1 = getQueryRuntimeAPI().getLastAttachment(copyProcessInstanceUUID, "attachment1");
    assertEquals("fileName1", attachment1.getFileName());
    
    Integer variable2 = (Integer)getQueryRuntimeAPI().getProcessInstanceVariable(copyProcessInstanceUUID, "variable2");
    assertEquals(3, variable2.intValue());
    
    AttachmentInstance attachment2 = getQueryRuntimeAPI().getLastAttachment(copyProcessInstanceUUID, "attachment2");
    assertEquals("fileName3", attachment2.getFileName());
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getRuntimeAPI().deleteProcessInstance(copyProcessInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  public void testSimpleInstantiateProcess() throws Exception {
    ProcessDefinition simpleProcess = buildSimpleProcess();
    
    Map<String, Object> processVariables = new HashMap<String, Object>();
    processVariables.put("variable1", 1);
    Set<InitialAttachment> initialAttachments = new HashSet<InitialAttachment>();
    InitialAttachmentImpl initialAttachment1 = new InitialAttachmentImpl("attachment1", new byte[1]);
    initialAttachment1.setLabel("label1");
    initialAttachment1.setFileName("fileName1");
    initialAttachments.add(initialAttachment1);
    List<String> activityExecutionsToStart = new ArrayList<String>();
    activityExecutionsToStart.add("task2");
    ProcessInstanceUUID processInstanceUUID = getRepairAPI().instantiateProcess(simpleProcess.getUUID(), processVariables, initialAttachments, activityExecutionsToStart);
    
    Set<TaskInstance> taskInstances = getQueryRuntimeAPI().getTasks(processInstanceUUID);
    assertEquals(1, taskInstances.size());
    assertEquals("task2", taskInstances.iterator().next().getActivityName());
    assertEquals(ActivityState.READY, taskInstances.iterator().next().getState());
    
    Integer variable1 = (Integer)getQueryRuntimeAPI().getProcessInstanceVariable(processInstanceUUID, "variable1");
    assertEquals(1, variable1.intValue());
    
    AttachmentInstance attachment1 = getQueryRuntimeAPI().getLastAttachment(processInstanceUUID, "attachment1");
    assertEquals("fileName1", attachment1.getFileName());
    
    assertEquals(InstanceState.STARTED, getQueryRuntimeAPI().getProcessInstance(processInstanceUUID).getInstanceState());
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  public void testSimpleInstantiateProcessWithUserId() throws Exception {
    ProcessDefinition simpleProcess = buildSimpleProcess();
    
    Map<String, Object> processVariables = new HashMap<String, Object>();
    Set<InitialAttachment> initialAttachments = new HashSet<InitialAttachment>();
    List<String> activityExecutionsToStart = new ArrayList<String>();
    activityExecutionsToStart.add("task2");
    ProcessInstanceUUID processInstanceUUID = getRepairAPI().instantiateProcess(simpleProcess.getUUID(), processVariables, initialAttachments, activityExecutionsToStart, "jack");
    
    Set<TaskInstance> taskInstances = getQueryRuntimeAPI().getTasks(processInstanceUUID);
    assertEquals(1, taskInstances.size());
    assertEquals("task2", taskInstances.iterator().next().getActivityName());
    assertEquals(ActivityState.READY, taskInstances.iterator().next().getState());
    
    assertEquals(InstanceState.STARTED, getQueryRuntimeAPI().getProcessInstance(processInstanceUUID).getInstanceState());
    
    ProcessInstance processInstance = getQueryRuntimeAPI().getProcessInstance(processInstanceUUID);
    assertEquals("jack", processInstance.getStartedBy());
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  public void testComplexStopStartExecution() throws Exception {
    ProcessDefinition complexProcess = buildComplexProcess();
    ProcessInstanceUUID processInstanceUUID = getRuntimeAPI().instantiateProcess(complexProcess.getUUID());
    
    getRepairAPI().stopExecution(processInstanceUUID, "task1");

    getRepairAPI().startExecution(processInstanceUUID, "task2");
    
    Set<TaskInstance> taskInstances = getQueryRuntimeAPI().getTasks(processInstanceUUID);
    assertEquals(2, taskInstances.size());
    for (TaskInstance taskInstance : taskInstances) {
      if(taskInstance.getActivityName().equals("task1")) {
        assertEquals(ActivityState.CANCELLED, taskInstance.getState());
      } else {
        assertEquals(ActivityState.READY, taskInstance.getState());
      }
    }
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  public void testComplexInstantiateProcess() throws Exception {
    ProcessDefinition complexProcess = buildComplexProcess();
    
    List<String> activityExecutionsToStart = new ArrayList<String>();
    activityExecutionsToStart.add("task2");
    ProcessInstanceUUID processInstanceUUID = getRepairAPI().instantiateProcess(complexProcess.getUUID(), new HashMap<String, Object>(), new HashSet<InitialAttachment>(), activityExecutionsToStart);
    
    Set<TaskInstance> taskInstances = getQueryRuntimeAPI().getTasks(processInstanceUUID);
    assertEquals(1, taskInstances.size());
    assertEquals("task2", taskInstances.iterator().next().getActivityName());
    assertEquals(ActivityState.READY, taskInstances.iterator().next().getState());
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  public void testComplexCopyProcessInstance() throws Exception {
    ProcessDefinition complexProcess = buildComplexProcess();

    ProcessInstanceUUID processInstanceUUID = getRuntimeAPI().instantiateProcess(complexProcess.getUUID(), new HashMap<String, Object>());
    
    ActivityInstanceUUID currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    ProcessInstanceUUID copyProcessInstanceUUID = getRepairAPI().copyProcessInstance(processInstanceUUID, new HashMap<String, Object>(), new HashSet<InitialAttachment>());
    
    Set<TaskInstance> taskInstances = getQueryRuntimeAPI().getTasks(copyProcessInstanceUUID);
    assertEquals(1, taskInstances.size());
    assertEquals("task2", taskInstances.iterator().next().getActivityName());
    assertEquals(ActivityState.READY, taskInstances.iterator().next().getState());
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getRuntimeAPI().deleteProcessInstance(copyProcessInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  public void testComplexCopyProcessInstanceAfterBranch() throws Exception {
    ProcessDefinition complexProcess = buildComplexProcess();

    ProcessInstanceUUID processInstanceUUID = getRuntimeAPI().instantiateProcess(complexProcess.getUUID(), new HashMap<String, Object>());
    
    ActivityInstanceUUID currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    
    ProcessInstanceUUID copyProcessInstanceUUID = getRepairAPI().copyProcessInstance(processInstanceUUID, new HashMap<String, Object>(), new HashSet<InitialAttachment>());
    
    Set<TaskInstance> taskInstances = getQueryRuntimeAPI().getTasks(copyProcessInstanceUUID);
    assertEquals(1, taskInstances.size());
    assertEquals("task4", taskInstances.iterator().next().getActivityName());
    assertEquals(ActivityState.READY, taskInstances.iterator().next().getState());
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getRuntimeAPI().deleteProcessInstance(copyProcessInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  public void testParallelInstantiateProcess() throws Exception {
    ProcessDefinition parallelProcess = buildParallelProcess();
    
    List<String> activityExecutionsToStart = new ArrayList<String>();
    activityExecutionsToStart.add("task2");
    activityExecutionsToStart.add("task3");
    ProcessInstanceUUID processInstanceUUID = getRepairAPI().instantiateProcess(parallelProcess.getUUID(), new HashMap<String, Object>(), new HashSet<InitialAttachment>(), activityExecutionsToStart);
    
    Set<TaskInstance> taskInstances = getQueryRuntimeAPI().getTasks(processInstanceUUID);
    assertEquals(2, taskInstances.size());
    for (TaskInstance taskInstance : taskInstances) {
      if (taskInstance.getActivityName().equals("task2") || taskInstance.getActivityName().equals("task3")) {
        assertEquals(ActivityState.READY, taskInstance.getState());
      } else {
        fail("only task 2 and 3 should be present in the task instances list.");
      }
    }
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  public void testParallelInstantiateProcessInABranch() throws Exception {
    ProcessDefinition parallelProcess = buildParallelProcess();
    
    List<String> activityExecutionsToStart = new ArrayList<String>();
    activityExecutionsToStart.add("task2");
    ProcessInstanceUUID processInstanceUUID = getRepairAPI().instantiateProcess(parallelProcess.getUUID(), new HashMap<String, Object>(), new HashSet<InitialAttachment>(), activityExecutionsToStart);
    
    Collection<TaskInstance> taskInstances = getQueryRuntimeAPI().getTasks(processInstanceUUID);
    assertEquals(1, taskInstances.size());
    TaskInstance taskInstance = taskInstances.iterator().next();
    assertEquals(ActivityState.READY, taskInstance.getState());
    getRuntimeAPI().executeTask(taskInstance.getUUID(), true);
    
    taskInstances = getQueryRuntimeAPI().getTaskList(processInstanceUUID, ActivityState.READY);
    assertEquals(0, taskInstances.size());
    
    getRepairAPI().startExecution(processInstanceUUID, "task3");
    taskInstances = getQueryRuntimeAPI().getTaskList(processInstanceUUID, ActivityState.READY);
    taskInstance = taskInstances.iterator().next();
    getRuntimeAPI().executeTask(taskInstance.getUUID(), true);
    
    taskInstances = getQueryRuntimeAPI().getTaskList(processInstanceUUID, ActivityState.READY);
    assertEquals(1, taskInstances.size());
    taskInstance = taskInstances.iterator().next();
    assertEquals("task4", taskInstance.getActivityName());
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  public void testParallelCopyProcessInstance() throws Exception {
    ProcessDefinition parallelProcess = buildParallelProcess();
    ProcessInstanceUUID processInstanceUUID = getRuntimeAPI().instantiateProcess(parallelProcess.getUUID());
    
    ActivityInstanceUUID currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);

    ProcessInstanceUUID copyProcessInstanceUUID = getRepairAPI().copyProcessInstance(processInstanceUUID, new HashMap<String, Object>(), new HashSet<InitialAttachment>());
    
    Collection<TaskInstance> taskInstances = getQueryRuntimeAPI().getTasks(copyProcessInstanceUUID);
    assertEquals(2, taskInstances.size());
    for (TaskInstance taskInstance : taskInstances) {
      if (taskInstance.getActivityName().equals("task2") || taskInstance.getActivityName().equals("task3")) {
        assertEquals(ActivityState.READY, taskInstance.getState());
      } else {
        fail("only task 2 and 3 should be present in the task instances list.");
      }
    }
    
    currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(copyProcessInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(copyProcessInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    taskInstances = getQueryRuntimeAPI().getTaskList(copyProcessInstanceUUID, ActivityState.READY);
    assertEquals(1, taskInstances.size());
    assertEquals("task4", taskInstances.iterator().next().getActivityName());
    
    currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(copyProcessInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    assertEquals(InstanceState.FINISHED, getQueryRuntimeAPI().getProcessInstance(copyProcessInstanceUUID).getInstanceState());
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getRuntimeAPI().deleteProcessInstance(copyProcessInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  public void testIterationInstantiateProcess() throws Exception {
    ProcessDefinition iterationProcess = buildIterationProcess();

    List<String> activityExecutionsToStart = new ArrayList<String>();
    activityExecutionsToStart.add("task2");
    ProcessInstanceUUID processInstanceUUID = getRepairAPI().instantiateProcess(iterationProcess.getUUID(), new HashMap<String, Object>(), new HashSet<InitialAttachment>(), activityExecutionsToStart);
    
    Collection<TaskInstance> taskInstances = getQueryRuntimeAPI().getTasks(processInstanceUUID);
    assertEquals(1, taskInstances.size());
    assertEquals("task2", taskInstances.iterator().next().getActivityName());
    assertEquals(ActivityState.READY, taskInstances.iterator().next().getState());

    ActivityInstanceUUID currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    taskInstances = getQueryRuntimeAPI().getTaskList(ActivityState.READY);
    assertEquals(1, taskInstances.size());
    assertEquals("task3", taskInstances.iterator().next().getActivityName());
    
    currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    taskInstances = getQueryRuntimeAPI().getTaskList(ActivityState.READY);
    assertEquals(1, taskInstances.size());
    assertEquals("task2", taskInstances.iterator().next().getActivityName());
    
    currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    getRuntimeAPI().setVariable(currentActivityInstanceUUID, "number", 1);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    taskInstances = getQueryRuntimeAPI().getTaskList(ActivityState.READY);
    assertEquals(1, taskInstances.size());
    assertEquals("task4", taskInstances.iterator().next().getActivityName());
    
    currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    assertEquals(InstanceState.FINISHED, getQueryRuntimeAPI().getProcessInstance(processInstanceUUID).getInstanceState());
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  public void testIterationCopyProcessInstance() throws Exception {
    ProcessDefinition iterationProcess = buildIterationProcess();

    ProcessInstanceUUID processInstanceUUID = getRuntimeAPI().instantiateProcess(iterationProcess.getUUID());

    ActivityInstanceUUID currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    Map<String, Object> processVariables = new HashMap<String, Object>();
    processVariables.put("number", 1);
    ProcessInstanceUUID copyProcessInstanceUUID = getRepairAPI().copyProcessInstance(processInstanceUUID, processVariables, new HashSet<InitialAttachment>());
    
    Collection<TaskInstance> taskInstances = getQueryRuntimeAPI().getTasks(copyProcessInstanceUUID);
    assertEquals(1, taskInstances.size());
    assertEquals("task2", taskInstances.iterator().next().getActivityName());
    assertEquals(ActivityState.READY, taskInstances.iterator().next().getState());
    
    currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(copyProcessInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    taskInstances = getQueryRuntimeAPI().getTaskList(copyProcessInstanceUUID, ActivityState.READY);
    assertEquals(1, taskInstances.size());
    assertEquals("task4", taskInstances.iterator().next().getActivityName());
    
    currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(copyProcessInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    assertEquals(InstanceState.FINISHED, getQueryRuntimeAPI().getProcessInstance(copyProcessInstanceUUID).getInstanceState());
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getRuntimeAPI().deleteProcessInstance(copyProcessInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  public void testMultiInstInstantiateProcess() throws Exception {
    ProcessDefinition multiInstProcess = buildMultiInstProcess();
    
    List<String> activityExecutionsToStart = new ArrayList<String>();
    activityExecutionsToStart.add("task2");
    ProcessInstanceUUID processInstanceUUID = getRepairAPI().instantiateProcess(multiInstProcess.getUUID(), new HashMap<String, Object>(), new HashSet<InitialAttachment>(), activityExecutionsToStart);
    
    Collection<TaskInstance> taskInstances = getQueryRuntimeAPI().getTasks(processInstanceUUID);
    assertEquals(3, taskInstances.size());
    Integer number = null;
    for (TaskInstance taskInstance : taskInstances) {
      assertEquals("task2", taskInstance.getActivityName());
      assertEquals(ActivityState.READY, taskInstance.getState());
      Integer variable = (Integer)getQueryRuntimeAPI().getActivityInstanceVariable(taskInstance.getUUID(), "number");
      assertNotSame(number, variable);
      number = variable;
      getRuntimeAPI().executeTask(taskInstance.getUUID(), true);
    }
    
    taskInstances = getQueryRuntimeAPI().getTaskList(processInstanceUUID, ActivityState.READY);
    assertEquals(1, taskInstances.size());
    assertEquals("task3", taskInstances.iterator().next().getActivityName());
    
    ActivityInstanceUUID currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    assertEquals(InstanceState.FINISHED, getQueryRuntimeAPI().getProcessInstance(processInstanceUUID).getInstanceState());
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  public void testMultiInstCopyProcessInstance() throws Exception {
    ProcessDefinition multiInstProcess = buildMultiInstProcess();

    ProcessInstanceUUID processInstanceUUID = getRuntimeAPI().instantiateProcess(multiInstProcess.getUUID());

    ActivityInstanceUUID currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    ProcessInstanceUUID copyProcessInstanceUUID = getRepairAPI().copyProcessInstance(processInstanceUUID, new HashMap<String, Object>(), new HashSet<InitialAttachment>());
    
    Collection<TaskInstance> taskInstances = getQueryRuntimeAPI().getTasks(copyProcessInstanceUUID);
    assertEquals(3, taskInstances.size());
    Integer number = null;
    for (TaskInstance taskInstance : taskInstances) {
      assertEquals("task2", taskInstance.getActivityName());
      assertEquals(ActivityState.READY, taskInstance.getState());
      Integer variable = (Integer)getQueryRuntimeAPI().getActivityInstanceVariable(taskInstance.getUUID(), "number");
      assertNotSame(number, variable);
      number = variable;
      getRuntimeAPI().executeTask(taskInstance.getUUID(), true);
    }
    
    taskInstances = getQueryRuntimeAPI().getTaskList(copyProcessInstanceUUID, ActivityState.READY);
    assertEquals(1, taskInstances.size());
    assertEquals("task3", taskInstances.iterator().next().getActivityName());
    
    currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(copyProcessInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    assertEquals(InstanceState.FINISHED, getQueryRuntimeAPI().getProcessInstance(copyProcessInstanceUUID).getInstanceState());
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getRuntimeAPI().deleteProcessInstance(copyProcessInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  public void testSubprocessInstantiateProcess() throws Exception {
    buildSimpleProcess();
    ProcessDefinition processWithSubProcess = buildProcessWithSubProcess();
    
    List<String> activityExecutionsToStart = new ArrayList<String>();
    activityExecutionsToStart.add("subprocess");
    ProcessInstanceUUID processInstanceUUID = getRepairAPI().instantiateProcess(processWithSubProcess.getUUID(), new HashMap<String, Object>(), new HashSet<InitialAttachment>(), activityExecutionsToStart);
    
    Set<ActivityInstance> activityInstances = getQueryRuntimeAPI().getActivityInstances(processInstanceUUID);
    assertEquals(1, activityInstances.size());
    assertEquals("subprocess", activityInstances.iterator().next().getActivityName());
    assertEquals(ActivityState.EXECUTING, activityInstances.iterator().next().getState());
    
    ProcessInstance processInstance = getQueryRuntimeAPI().getProcessInstance(processInstanceUUID);
    ProcessInstanceUUID childInstanceUUID = processInstance.getChildrenInstanceUUID().iterator().next();
    Collection<TaskInstance> taskInstances = getQueryRuntimeAPI().getTaskList(childInstanceUUID, ActivityState.READY);
    assertEquals(1, taskInstances.size());
    TaskInstance childTaskInstance = taskInstances.iterator().next();
    assertEquals("task1", childTaskInstance.getActivityName());
    assertEquals(ActivityState.READY, childTaskInstance.getState());
    getRuntimeAPI().executeTask(childTaskInstance.getUUID(), true);
    
    taskInstances = getQueryRuntimeAPI().getTaskList(childInstanceUUID, ActivityState.READY);
    assertEquals(1, taskInstances.size());
    childTaskInstance = taskInstances.iterator().next();
    assertEquals("task2", childTaskInstance.getActivityName());
    assertEquals(ActivityState.READY, childTaskInstance.getState());
    getRuntimeAPI().executeTask(childTaskInstance.getUUID(), true);

    assertEquals(InstanceState.FINISHED, getQueryRuntimeAPI().getProcessInstance(childInstanceUUID).getInstanceState());
    
    taskInstances = getQueryRuntimeAPI().getTaskList(processInstanceUUID, ActivityState.READY);
    assertEquals(1, taskInstances.size());
    TaskInstance taskInstance = taskInstances.iterator().next();
    assertEquals("task3", taskInstance.getActivityName());
    assertEquals(ActivityState.READY, taskInstance.getState());
    getRuntimeAPI().executeTask(taskInstance.getUUID(), true);
  
    assertEquals(InstanceState.FINISHED, getQueryRuntimeAPI().getProcessInstance(processInstanceUUID).getInstanceState());
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  public void testSubprocessCopyProcessInstance() throws Exception {
    buildSimpleProcess();
    ProcessDefinition processWithSubProcess = buildProcessWithSubProcess();

    ProcessInstanceUUID processInstanceUUID = getRuntimeAPI().instantiateProcess(processWithSubProcess.getUUID());

    ActivityInstanceUUID currentActivityInstanceUUID = getQueryRuntimeAPI().getOneTask(processInstanceUUID, ActivityState.READY);
    getRuntimeAPI().executeTask(currentActivityInstanceUUID, true);
    
    ProcessInstanceUUID copyProcessInstanceUUID = getRepairAPI().copyProcessInstance(processInstanceUUID, new HashMap<String, Object>(), new HashSet<InitialAttachment>());

    Set<ActivityInstance> activityInstances = getQueryRuntimeAPI().getActivityInstances(copyProcessInstanceUUID);
    assertEquals(1, activityInstances.size());
    assertEquals("subprocess", activityInstances.iterator().next().getActivityName());
    assertEquals(ActivityState.EXECUTING, activityInstances.iterator().next().getState());
    
    ProcessInstance processInstance = getQueryRuntimeAPI().getProcessInstance(copyProcessInstanceUUID);
    ProcessInstanceUUID childInstanceUUID = processInstance.getChildrenInstanceUUID().iterator().next();
    Collection<TaskInstance> taskInstances = getQueryRuntimeAPI().getTaskList(childInstanceUUID, ActivityState.READY);
    assertEquals(1, taskInstances.size());
    TaskInstance childTaskInstance = taskInstances.iterator().next();
    assertEquals("task1", childTaskInstance.getActivityName());
    assertEquals(ActivityState.READY, childTaskInstance.getState());
    getRuntimeAPI().executeTask(childTaskInstance.getUUID(), true);
    
    taskInstances = getQueryRuntimeAPI().getTaskList(childInstanceUUID, ActivityState.READY);
    assertEquals(1, taskInstances.size());
    childTaskInstance = taskInstances.iterator().next();
    assertEquals("task2", childTaskInstance.getActivityName());
    assertEquals(ActivityState.READY, childTaskInstance.getState());
    getRuntimeAPI().executeTask(childTaskInstance.getUUID(), true);

    assertEquals(InstanceState.FINISHED, getQueryRuntimeAPI().getProcessInstance(childInstanceUUID).getInstanceState());
    
    taskInstances = getQueryRuntimeAPI().getTaskList(copyProcessInstanceUUID, ActivityState.READY);
    assertEquals(1, taskInstances.size());
    TaskInstance taskInstance = taskInstances.iterator().next();
    assertEquals("task3", taskInstance.getActivityName());
    assertEquals(ActivityState.READY, taskInstance.getState());
    getRuntimeAPI().executeTask(taskInstance.getUUID(), true);
  
    assertEquals(InstanceState.FINISHED, getQueryRuntimeAPI().getProcessInstance(copyProcessInstanceUUID).getInstanceState());
    
    getRuntimeAPI().deleteProcessInstance(copyProcessInstanceUUID);
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }

  public void testStopExecutionAfterError() throws Exception {
    ProcessDefinition autoProcess = buildAutomaticProcess();
    ProcessInstanceUUID processInstanceUUID = getRuntimeAPI().instantiateProcess(autoProcess.getUUID());
    
    Set<ActivityInstance> activityInstances = null;
    
//    Thread.sleep(100);
//    
//    activityInstances = getQueryRuntimeAPI().getActivityInstances(processInstanceUUID);
//    assertEquals(2, activityInstances.size());
//    for (ActivityInstance activityInstance : activityInstances) {
//      System.out.println(activityInstance.getActivityName() + " - " + activityInstance.getState());
//    }
//    
//    Thread.sleep(900);
//
//    activityInstances = getQueryRuntimeAPI().getActivityInstances(processInstanceUUID);
//    assertEquals(2, activityInstances.size());
//    for (ActivityInstance activityInstance : activityInstances) {
//      System.out.println(activityInstance.getActivityName() + " - " + activityInstance.getState());
//    }
    
    Thread.sleep(1000);
    
    getRepairAPI().stopExecution(processInstanceUUID, "activity2");
    
    activityInstances = getQueryRuntimeAPI().getActivityInstances(processInstanceUUID);
    assertEquals(2, activityInstances.size());
    for (ActivityInstance activityInstance : activityInstances) {
      if(activityInstance.getActivityName().equals("activity2")) {
        assertEquals(ActivityState.CANCELLED, activityInstance.getState());
      }
    }
    
    getRuntimeAPI().deleteProcessInstance(processInstanceUUID);
    getManagementAPI().deleteAllProcesses();
  }
  
  public static class  SimpleMultiInstantiator implements MultiInstantiator {
    public MultiInstantiatorDescriptor execute(QueryAPIAccessor arg0, ProcessInstanceUUID arg1, String arg2, String arg3) throws Exception {
      List<Object> objects = new ArrayList<Object>();
      objects.add(1);
      objects.add(2);
      objects.add(3);
      return new MultiInstantiatorDescriptor(objects.size(), objects);
    }
  }
 
}
