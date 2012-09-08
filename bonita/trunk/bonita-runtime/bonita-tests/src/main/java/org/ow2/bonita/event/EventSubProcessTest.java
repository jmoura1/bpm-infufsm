package org.ow2.bonita.event;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bonitasoft.connectors.bonita.SetVarConnector;
import org.bonitasoft.connectors.scripting.GroovyConnector;
import org.ow2.bonita.APITestCase;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.def.element.HookDefinition.Event;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.exception.ProcessNotFoundException;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.ActivityState;
import org.ow2.bonita.facade.runtime.AttachmentInstance;
import org.ow2.bonita.facade.runtime.Document;
import org.ow2.bonita.facade.runtime.InstanceState;
import org.ow2.bonita.facade.runtime.ProcessInstance;
import org.ow2.bonita.facade.runtime.TaskInstance;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.light.LightActivityInstance;
import org.ow2.bonita.light.LightProcessInstance;
import org.ow2.bonita.light.LightTaskInstance;
import org.ow2.bonita.search.DocumentResult;
import org.ow2.bonita.search.DocumentSearchBuilder;
import org.ow2.bonita.search.index.DocumentIndex;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.util.ProcessBuilder;

public class EventSubProcessTest extends APITestCase {

  public void testAnEventSubProcessCannotBeInstantiateAutomaticallyAfterDeployement() throws Exception {
    ProcessDefinition eventSubProcess =
      ProcessBuilder.createProcess("ESB", "0.9")
      .setEventSubProcess()
      .addTimerTask("Wait_a_second", "1000")
      .addHuman(getLogin())
      .addHumanTask("event", getLogin())
      .addTransition("Wait_a_second", "event")
      .done();

    getManagementAPI().deploy(getBusinessArchive(eventSubProcess));
    Thread.sleep(2000);

    Set<ProcessInstance> instances = getQueryRuntimeAPI().getProcessInstances(eventSubProcess.getUUID());
    assertEquals(0, instances.size());

    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
  }

  public void testAnEventSubProcessCanBeInstantiateManually() throws Exception {
    ProcessDefinition eventSubProcess =
      ProcessBuilder.createProcess("ESB", "1.0")
      .setEventSubProcess()
      .addSignalEventTask("Wait", "go", true)
      .addHuman(getLogin())
      .addHumanTask("event", getLogin())
      .addTransition("Wait", "event")
      .done();

    getManagementAPI().deploy(getBusinessArchive(eventSubProcess));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(eventSubProcess.getUUID());
    Set<TaskInstance> tasks = getQueryRuntimeAPI().getTasks(instanceUUID);
    assertEquals(1, tasks.size());
    TaskInstance task = tasks.iterator().next();
    getRuntimeAPI().executeTask(task.getUUID(), true);

    ProcessInstance instance = AccessorUtil.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_HISTORY_KEY).getProcessInstance(instanceUUID);
    assertEquals(InstanceState.FINISHED, instance.getInstanceState());

    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
  }

  public void testStartTimerEventSubProcess() throws Exception {
    ProcessDefinition eventSubProcess =
      ProcessBuilder.createProcess("ESB", "1.0")
      .setEventSubProcess()
      .addTimerTask("Wait_4_seconds", "4000")
      .addHuman(getLogin())
      .addHumanTask("event", getLogin())
      .addTransition("Wait_4_seconds", "event")
      .done();

    ProcessDefinition process =
      ProcessBuilder.createProcess("process", "1.0")
      .addHuman(getLogin())
      .addHumanTask("wait", getLogin())
      .addEventSubProcess("ESB", "1.0")
      .done();

    getManagementAPI().deploy(getBusinessArchive(eventSubProcess));
    getManagementAPI().deploy(getBusinessArchive(process));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    waitForInstance(5000, 50, instanceUUID,InstanceState.ABORTED);
    Set<ProcessInstance> eventSubProcesses = getQueryRuntimeAPI().getProcessInstances(eventSubProcess.getUUID());
    assertEquals(1, eventSubProcesses.size());
    ProcessInstanceUUID eventSubProcessUUID = eventSubProcesses.iterator().next().getUUID();

    Set<TaskInstance> tasks = getQueryRuntimeAPI().getTasks(eventSubProcessUUID);
    assertEquals(1, tasks.size());
    assertEquals("event", tasks.iterator().next().getActivityName());

    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.ABORTED, instance.getInstanceState());

    executeTask(eventSubProcessUUID, "event");
    QueryRuntimeAPI historyQueryRuntimeAPI = AccessorUtil.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_HISTORY_KEY);

    instance = historyQueryRuntimeAPI.getProcessInstance(eventSubProcessUUID);
    assertEquals(InstanceState.FINISHED, instance.getInstanceState());
    instance = historyQueryRuntimeAPI.getProcessInstance(instanceUUID);
    assertEquals(InstanceState.ABORTED, instance.getInstanceState());
    
    getManagementAPI().deleteProcess(process.getUUID());
    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
  }

  public void testNotStartTimerEventSubProcess() throws Exception {
    ProcessDefinition eventSubProcess =
      ProcessBuilder.createProcess("ESB", "1.0")
      .setEventSubProcess()
      .addTimerTask("Wait_a_minute", "60000")
      .addHuman(getLogin())
      .addHumanTask("event", getLogin())
      .addTransition("Wait_a_minute", "event")
      .done();

    ProcessDefinition process =
      ProcessBuilder.createProcess("process", "1.0")
      .addHuman(getLogin())
      .addHumanTask("wait", getLogin())
      .addEventSubProcess("ESB", "1.0")
      .done();

    getManagementAPI().deploy(getBusinessArchive(eventSubProcess));
    getManagementAPI().deploy(getBusinessArchive(process));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    Thread.sleep(3000);
    Set<ProcessInstance> eventSubProcesses = getQueryRuntimeAPI().getProcessInstances(eventSubProcess.getUUID());
    assertEquals(0, eventSubProcesses.size());

    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.STARTED, instance.getInstanceState());

    getManagementAPI().deleteProcess(process.getUUID());
    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
  }

  public void testNotStartSignalEventSubProcess() throws Exception {
    ProcessDefinition eventSubProcess =
      ProcessBuilder.createProcess("ESB", "1.0")
      .setEventSubProcess()
      .addSignalEventTask("ready", "go", true)
      .addHuman(getLogin())
      .addHumanTask("event", getLogin())
      .addTransition("ready", "event")
      .done();

    ProcessDefinition process =
      ProcessBuilder.createProcess("process", "1.0")
      .addHuman(getLogin())
      .addHumanTask("wait", getLogin())
      .addEventSubProcess("ESB", "1.0")
      .done();

    getManagementAPI().deploy(getBusinessArchive(eventSubProcess));
    getManagementAPI().deploy(getBusinessArchive(process));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    Thread.sleep(3000);
    Set<ProcessInstance> eventSubProcesses = getQueryRuntimeAPI().getProcessInstances(eventSubProcess.getUUID());
    assertEquals(0, eventSubProcesses.size());

    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.STARTED, instance.getInstanceState());

    getManagementAPI().deleteProcess(process.getUUID());
    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
  }

  public void testShareProcessContextWithEventSubProcess() throws Exception {
    ProcessDefinition eventSubProcess =
      ProcessBuilder.createProcess("ESB", "1.0")
      .setEventSubProcess()
      .addTimerTask("Wait_3_seconds", "3000")
      .addHuman(getLogin())
      .addHumanTask("event", getLogin())
      .addTransition("Wait_3_seconds", "event")
      .done();

    ProcessDefinition process =
      ProcessBuilder.createProcess("process", "1.0")
      .addHuman(getLogin())
      .addStringData("name", "bonita")
      .addAttachment("myAttachment")
        .addLabel("attach")
        .addDescription("attachDesc")
      .addHumanTask("wait", getLogin())
      .addEventSubProcess("ESB", "1.0")
      .done();

    getManagementAPI().deploy(getBusinessArchive(eventSubProcess));
    getManagementAPI().deploy(getBusinessArchive(process));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    waitForInstance(20000, 50, instanceUUID, InstanceState.ABORTED);
    ProcessInstance instance = AccessorUtil.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_JOURNAL_KEY).getProcessInstance(instanceUUID);
    assertEquals(InstanceState.ABORTED, instance.getInstanceState());

    Set<ProcessInstance> eventSubProcesses = getQueryRuntimeAPI().getProcessInstances(eventSubProcess.getUUID());
    assertEquals(1, eventSubProcesses.size());
    ProcessInstanceUUID eventSubProcessUUID = eventSubProcesses.iterator().next().getUUID();
    String name = (String) getQueryRuntimeAPI().getProcessInstanceVariable(eventSubProcessUUID, "name");
    assertEquals("bonita", name);
    Set<String> attachmentNames = getQueryRuntimeAPI().getAttachmentNames(eventSubProcessUUID);
    assertEquals(1, attachmentNames.size());
    String attachmentName = attachmentNames.iterator().next();
    assertTrue(attachmentNames.contains("myAttachment"));
    List<AttachmentInstance> attachments = getQueryRuntimeAPI().getAttachments(eventSubProcessUUID, attachmentName);
    AttachmentInstance attachment = attachments.get(0);
    assertEquals("admin", attachment.getAuthor());
    assertEquals("myAttachment", attachment.getName());

    Set<TaskInstance> tasks = getQueryRuntimeAPI().getTasks(eventSubProcessUUID);
    assertEquals(1, tasks.size());
    assertEquals("event", tasks.iterator().next().getActivityName());

    getManagementAPI().deleteProcess(process.getUUID());
    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
  }

  public void testInstantiateAProcessWithoutStartingTheEventSubProcess() throws Exception {
    ProcessDefinition eventSubProcess =
      ProcessBuilder.createProcess("ESP", "1.1")
      .setEventSubProcess()
      .addSignalEventTask("Wait_signal", "goGogo", true)
      .addHuman(getLogin())
      .addHumanTask("event", getLogin())
      .addTransition("Wait_signal", "event")
      .done();

    ProcessDefinition process =
      ProcessBuilder.createProcess("process", "1.1")
      .addHuman(getLogin())
      .addStringData("name", "bonita")
      .addHumanTask("wait", getLogin())
      .addEventSubProcess("ESP", "1.1")
      .done();

    getManagementAPI().deploy(getBusinessArchive(eventSubProcess));
    getManagementAPI().deploy(getBusinessArchive(process));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());

    Set<ProcessInstance> eventSubProcesses = getQueryRuntimeAPI().getProcessInstances(eventSubProcess.getUUID());
    assertEquals(0, eventSubProcesses.size());

    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.STARTED, instance.getInstanceState());

    getManagementAPI().deleteProcess(process.getUUID());
    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
  }

  public void testStartSignalEventSubProcessFromAnExternalProcess() throws Exception {
    final String signalCode = "GoGoGo";

    ProcessDefinition signalProcess =
      ProcessBuilder.createProcess("signal", "1.5")
      .addHuman(getLogin())
      .addHumanTask("start", getLogin())
      .addSignalEventTask("go", signalCode)
      .addTransition("start", "go")
      .done();

    ProcessDefinition eventSubProcess =
      ProcessBuilder.createProcess("ESB", "1.0")
      .setEventSubProcess()
      .addSignalEventTask("go", signalCode, true)
      .addHuman(getLogin())
      .addHumanTask("event", getLogin())
      .addTransition("go", "event")
      .done();

    ProcessDefinition process =
      ProcessBuilder.createProcess("process", "1.0")
      .addHuman(getLogin())
      .addStringData("name", "bonita")
      .addHumanTask("wait", getLogin())
      .addEventSubProcess("ESB", "1.0")
      .done();

    getManagementAPI().deploy(getBusinessArchive(eventSubProcess));
    getManagementAPI().deploy(getBusinessArchive(process));
    getManagementAPI().deploy(getBusinessArchive(signalProcess));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());

    ProcessInstanceUUID signalUUID = getRuntimeAPI().instantiateProcess(signalProcess.getUUID());
    executeTask(signalUUID, "start");
    waitForInstance(4000, 50, instanceUUID,InstanceState.ABORTED);

    Set<ProcessInstance> eventSubProcesses = getQueryRuntimeAPI().getProcessInstances(eventSubProcess.getUUID());
    assertEquals(1, eventSubProcesses.size());
    ProcessInstanceUUID eventSubProcessUUID = eventSubProcesses.iterator().next().getUUID();

    Set<TaskInstance> tasks = getQueryRuntimeAPI().getTasks(eventSubProcessUUID);
    assertEquals(1, tasks.size());
    assertEquals("event", tasks.iterator().next().getActivityName());

    ProcessInstance instance = AccessorUtil.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_JOURNAL_KEY).getProcessInstance(instanceUUID);
    assertEquals(InstanceState.ABORTED, instance.getInstanceState());

    getManagementAPI().deleteProcess(process.getUUID());
    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
    getManagementAPI().deleteProcess(signalProcess.getUUID());
  }

  public void testStartMessageEventSubProcess() throws Exception {
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
      .addEventSubProcess("ESB", "1.0")
      .done();

    getManagementAPI().deploy(getBusinessArchive(eventSubProcess));
    getManagementAPI().deploy(getBusinessArchive(process));
    getManagementAPI().deploy(getBusinessArchive(messageProcess));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());

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

    getManagementAPI().deleteProcess(process.getUUID());
    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
    getManagementAPI().deleteProcess(messageProcess.getUUID());
  }

  public void testAnyEventSubprocessTypeCanRethrowErrorToBoundaryInARow() throws Exception {
    ProcessDefinition requestExtension = ProcessBuilder.createProcess("requestExtension", "1.0")
    .setEventSubProcess()
    .addBooleanData("granted", false)
    .addTimerTask("In_48_hrs", "2000")
    .addSystemTask("Request_extension")
    .addDecisionNode("Granted")
    .addSystemTask("end")
    .addErrorEventTask("End_negociations", "endNego")
    .addTransition("In_48_hrs", "Request_extension")
    .addTransition("Request_extension", "Granted")
    .addTransition("Granted", "end")
    .addCondition("granted")
    .addTransition("Granted", "End_negociations")
    .addCondition("!granted")
    .done();

    ProcessDefinition negociateContract = ProcessBuilder.createProcess("negociateContract", "0.5")
    .addHuman(getLogin())
    .addSystemTask("start")
    .addHumanTask("Negociate_contrat", getLogin())
    .addSystemTask("end")
    .addTransition("start", "Negociate_contrat")
    .addTransition("Negociate_contrat", "end")
    .addEventSubProcess("requestExtension", "1.0")
    .done();

    ProcessDefinition mainProcess = ProcessBuilder.createProcess("main", "7.5")
    .addSubProcess("Negociation", "negociateContract")
    .addErrorBoundaryEvent("End_negociations", "endNego")
    .addSystemTask("No_negociation")
    .addSystemTask("NegociationOK")
    .addTransition("Negociation", "NegociationOK")
    .addExceptionTransition("Negociation", "End_negociations", "No_negociation")
    .done();

    getManagementAPI().deploy(getBusinessArchive(requestExtension));
    getManagementAPI().deploy(getBusinessArchive(negociateContract));
    getManagementAPI().deploy(getBusinessArchive(mainProcess));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(mainProcess.getUUID());
    waitForInstanceEnd(4000, 50, instanceUUID);

    Set<ProcessInstance> requestExtensionProcesses = getQueryRuntimeAPI().getProcessInstances(requestExtension.getUUID());
    assertEquals(1, requestExtensionProcesses.size());
    ProcessInstance requestExtensionInstance = requestExtensionProcesses.iterator().next();
    assertEquals(InstanceState.ABORTED, requestExtensionInstance.getInstanceState());

    Set<ProcessInstance> negociateContractProcesses = getQueryRuntimeAPI().getProcessInstances(negociateContract.getUUID());
    assertEquals(1, negociateContractProcesses.size());
    ProcessInstance negociateContractInstance = negociateContractProcesses.iterator().next();
    assertEquals(InstanceState.ABORTED, negociateContractInstance.getInstanceState());

    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.FINISHED, instance.getInstanceState());
    Set<ActivityInstance> activities = instance.getActivities();
    assertEquals(2, activities.size());
    ActivityInstance nego = getActivityInstance(activities, "Negociation");
    assertEquals(ActivityState.ABORTED, nego.getState());
    assertNotNull(getActivityInstance(activities, "No_negociation"));

    getManagementAPI().deleteProcess(mainProcess.getUUID());
    getManagementAPI().deleteProcess(requestExtension.getUUID());
    getManagementAPI().deleteProcess(negociateContract.getUUID());
  }

  public void testTimerEventSubprocessTypeCanContinueInNormalFlowInARow() throws Exception {
    ProcessDefinition requestExtension = ProcessBuilder.createProcess("requestExtension", "1.0")
    .setEventSubProcess()
    .addTimerTask("In_48_hrs", "2000")
    .addSystemTask("Request_extension")
    .addSystemTask("end")
    .addTransition("In_48_hrs", "Request_extension")
    .addTransition("Request_extension", "end")
    .done();

    ProcessDefinition negociateContract = ProcessBuilder.createProcess("negociateContract", "0.5")
    .addHuman(getLogin())
    .addSystemTask("start")
    .addHumanTask("Negociate_contrat", getLogin())
    .addSystemTask("end")
    .addTransition("start", "Negociate_contrat")
    .addTransition("Negociate_contrat", "end")
    .addEventSubProcess("requestExtension", "1.0")
    .done();

    ProcessDefinition mainProcess = ProcessBuilder.createProcess("main", "7.5")
    .addSubProcess("Negociation", "negociateContract")
    .addErrorBoundaryEvent("End_negociations", "endNego")
    .addSystemTask("No_negociation")
    .addSystemTask("NegociationOK")
    .addTransition("Negociation", "NegociationOK")
    .addExceptionTransition("Negociation", "End_negociations", "No_negociation")
    .done();

    getManagementAPI().deploy(getBusinessArchive(requestExtension));
    getManagementAPI().deploy(getBusinessArchive(negociateContract));
    getManagementAPI().deploy(getBusinessArchive(mainProcess));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(mainProcess.getUUID());
    waitForInstanceEnd(4000, 50, instanceUUID);

    Set<ProcessInstance> requestExtensionProcesses = getQueryRuntimeAPI().getProcessInstances(requestExtension.getUUID());
    assertEquals(1, requestExtensionProcesses.size());
    ProcessInstance requestExtensionInstance = requestExtensionProcesses.iterator().next();
    assertEquals(InstanceState.FINISHED, requestExtensionInstance.getInstanceState());

    Set<ProcessInstance> negociateContractProcesses = getQueryRuntimeAPI().getProcessInstances(negociateContract.getUUID());
    assertEquals(1, negociateContractProcesses.size());
    ProcessInstance negociateContractInstance = negociateContractProcesses.iterator().next();
    assertEquals(InstanceState.ABORTED, negociateContractInstance.getInstanceState());

    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.FINISHED, instance.getInstanceState());
    Set<ActivityInstance> activities = instance.getActivities();
    assertEquals(2, activities.size());
    ActivityInstance nego = getActivityInstance(activities, "Negociation");
    assertEquals(ActivityState.FINISHED, nego.getState());
    assertNotNull(getActivityInstance(activities, "NegociationOK"));

    getManagementAPI().deleteProcess(mainProcess.getUUID());
    getManagementAPI().deleteProcess(requestExtension.getUUID());
    getManagementAPI().deleteProcess(negociateContract.getUUID());
  }

  public void testSignalEventSubprocessTypeCanContinueInNormalFlowInARow() throws Exception {
    ProcessDefinition requestExtension = ProcessBuilder.createProcess("requestExtension", "1.0")
    .setEventSubProcess()
    .addBooleanData("granted", true)
    .addSignalEventTask("No_more_negociation", "noNego", true)
    .addSystemTask("Request_extension")
    .addDecisionNode("Granted")
    .addSystemTask("end")
    .addErrorEventTask("End_negociations", "endNego")
    .addTransition("No_more_negociation", "Request_extension")
    .addTransition("Request_extension", "Granted")
    .addTransition("Granted", "end")
    .addCondition("granted")
    .addTransition("Granted", "End_negociations")
    .addCondition("!granted")
    .done();

    ProcessDefinition negociateContract = ProcessBuilder.createProcess("negociateContract", "0.5")
    .addHuman(getLogin())
    .addSystemTask("start")
    .addSignalEventTask("No_negociation", "noNego", false)
    .addTransition("start", "No_negociation")
    .addEventSubProcess("requestExtension", "1.0")
    .done();

    ProcessDefinition mainProcess = ProcessBuilder.createProcess("main", "7.5")
    .addSubProcess("Negociation", "negociateContract")
    .addErrorBoundaryEvent("End_negociations", "endNego")
    .addSystemTask("No_negociation")
    .addSystemTask("NegociationOK")
    .addTransition("Negociation", "NegociationOK")
    .addExceptionTransition("Negociation", "End_negociations", "No_negociation")
    .done();

    getManagementAPI().deploy(getBusinessArchive(requestExtension));
    getManagementAPI().deploy(getBusinessArchive(negociateContract));
    getManagementAPI().deploy(getBusinessArchive(mainProcess));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(mainProcess.getUUID());
    waitForInstanceEnd(4000, 50, instanceUUID);

    Set<ProcessInstance> requestExtensionProcesses = AccessorUtil.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_HISTORY_KEY).getProcessInstances(requestExtension.getUUID());
    assertEquals(1, requestExtensionProcesses.size());
    ProcessInstance requestExtensionInstance = requestExtensionProcesses.iterator().next();
    assertEquals(InstanceState.FINISHED, requestExtensionInstance.getInstanceState());

    Set<ProcessInstance> negociateContractProcesses = AccessorUtil.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_HISTORY_KEY).getProcessInstances(negociateContract.getUUID());
    assertEquals(1, negociateContractProcesses.size());
    ProcessInstance negociateContractInstance = negociateContractProcesses.iterator().next();
    assertEquals(InstanceState.ABORTED, negociateContractInstance.getInstanceState());

    ProcessInstance instance = AccessorUtil.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_HISTORY_KEY).getProcessInstance(instanceUUID);
    assertEquals(InstanceState.FINISHED, instance.getInstanceState());
    Set<ActivityInstance> activities = instance.getActivities();
    assertEquals(2, activities.size());
    ActivityInstance nego = getActivityInstance(activities, "Negociation");
    assertEquals(ActivityState.FINISHED, nego.getState());
    assertNotNull(getActivityInstance(activities, "NegociationOK"));

    getManagementAPI().deleteProcess(mainProcess.getUUID());
    getManagementAPI().deleteProcess(requestExtension.getUUID());
    getManagementAPI().deleteProcess(negociateContract.getUUID());
  }

  public void testMessageEventSubprocessTypeCanContinueInNormalFlowInARow() throws Exception {
    ProcessDefinition sendMessage = ProcessBuilder.createProcess("sendMessage", "1.0")
    .addHuman(getLogin())
    .addSendEventTask("send")
      .addOutgoingEvent("Customer_cancel_event", "customerCancel", "Custom_cancel", null)
    .done();

    ProcessDefinition customerCancel = ProcessBuilder.createProcess("customerCancel", "1.0")
    .setEventSubProcess()
    .addReceiveEventTask("Custom_cancel", "Customer_cancel_event")
    .addSystemTask("Calculate_billable_items")
    .addSystemTask("end")
    .addTransition("Custom_cancel", "Calculate_billable_items")
    .addTransition("Calculate_billable_items", "end")
    .done();

    ProcessDefinition nonWarrantyRepair = ProcessBuilder.createProcess("noWarrantyRepair", "1.0")
    .addHuman(getLogin())
    .addSystemTask("start")
    .addHumanTask("Order_parts", getLogin())
    .addHumanTask("Install_and_test", getLogin())
    .addSystemTask("end")
    .addTransition("start", "Order_parts")
    .addTransition("Order_parts", "Install_and_test")
    .addTransition("Install_and_test", "end")
    .addEventSubProcess("customerCancel", "1.0")
    .done();

    ProcessDefinition mainProcess = ProcessBuilder.createProcess("main", "7.5")
    .addSubProcess("No_warranty_repair", "noWarrantyRepair")
    .addSystemTask("Send_invoice")
    .addTransition("No_warranty_repair", "Send_invoice")
    .done();

    getManagementAPI().deploy(getBusinessArchive(sendMessage));
    getManagementAPI().deploy(getBusinessArchive(customerCancel));
    getManagementAPI().deploy(getBusinessArchive(nonWarrantyRepair));
    getManagementAPI().deploy(getBusinessArchive(mainProcess));

    getRuntimeAPI().instantiateProcess(sendMessage.getUUID());
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(mainProcess.getUUID());

    waitForInstanceEnd(4000, 50, instanceUUID);
    Set<ProcessInstance> customerCancelProcesses = getQueryRuntimeAPI().getProcessInstances(customerCancel.getUUID());
    assertEquals(1, customerCancelProcesses.size());
    ProcessInstance customerCancelInstance = customerCancelProcesses.iterator().next();
    assertEquals(InstanceState.FINISHED, customerCancelInstance.getInstanceState());

    Set<ProcessInstance> nonWarrantyRepairProcesses = getQueryRuntimeAPI().getProcessInstances(nonWarrantyRepair.getUUID());
    assertEquals(1, nonWarrantyRepairProcesses.size());
    ProcessInstance nonWarrantyRepairInstance = nonWarrantyRepairProcesses.iterator().next();
    assertEquals(InstanceState.ABORTED, nonWarrantyRepairInstance.getInstanceState());

    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.FINISHED, instance.getInstanceState());
    Set<ActivityInstance> activities = instance.getActivities();
    assertEquals(2, activities.size());
    ActivityInstance nego = getActivityInstance(activities, "No_warranty_repair");
    assertEquals(ActivityState.FINISHED, nego.getState());
    assertNotNull(getActivityInstance(activities, "Send_invoice"));

    getManagementAPI().deleteProcess(sendMessage.getUUID());
    getManagementAPI().deleteProcess(mainProcess.getUUID());
    getManagementAPI().deleteProcess(nonWarrantyRepair.getUUID());
    getManagementAPI().deleteProcess(customerCancel.getUUID());
  }

  public void testAnyEventSubprocessTypeCanContinueInNormalFlow() throws Exception {
    ProcessDefinition requestExtension = ProcessBuilder.createProcess("requestExtension", "1.0")
    .setEventSubProcess()
    .addBooleanData("granted", true)
    .addTimerTask("In_48_hrs", "2000")
    .addSystemTask("Request_extension")
    .addDecisionNode("Granted")
    .addSystemTask("end")
    .addErrorEventTask("End_negociations", "endNego")
    .addTransition("In_48_hrs", "Request_extension")
    .addTransition("Request_extension", "Granted")
    .addTransition("Granted", "end")
    .addCondition("granted")
    .addTransition("Granted", "End_negociations")
    .addCondition("!granted")
    .done();

    ProcessDefinition negociateContract = ProcessBuilder.createProcess("negociateContract", "0.5")
    .addHuman(getLogin())
    .addSystemTask("start")
    .addHumanTask("Negociate_contrat", getLogin())
    .addSystemTask("end")
    .addTransition("start", "Negociate_contrat")
    .addTransition("Negociate_contrat", "end")
    .addEventSubProcess("requestExtension", "1.0")
    .done();

    ProcessDefinition mainProcess = ProcessBuilder.createProcess("main", "7.5")
    .addSubProcess("Negociation", "negociateContract")
    .addErrorBoundaryEvent("End_negociations", "endNego")
    .addSystemTask("No_negociation")
    .addSystemTask("NegociationOK")
    .addTransition("Negociation", "NegociationOK")
    .addExceptionTransition("Negociation", "End_negociations", "No_negociation")
    .done();

    getManagementAPI().deploy(getBusinessArchive(requestExtension));
    getManagementAPI().deploy(getBusinessArchive(negociateContract));
    getManagementAPI().deploy(getBusinessArchive(mainProcess));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(mainProcess.getUUID());
    
    waitForInstanceEnd(10000, 50, instanceUUID);
//    Set<ProcessInstance> instances = getQueryRuntimeAPI().getProcessInstances(requestExtension.getUUID());
//    executeTask(instances.iterator().next().getUUID(), "Request_extension");

    Set<ProcessInstance> requestExtensionProcesses = getQueryRuntimeAPI().getProcessInstances(requestExtension.getUUID());
    assertEquals(1, requestExtensionProcesses.size());
    ProcessInstance requestExtensionInstance = requestExtensionProcesses.iterator().next();
    assertEquals(InstanceState.FINISHED, requestExtensionInstance.getInstanceState());

    Set<ProcessInstance> negociateContractProcesses = getQueryRuntimeAPI().getProcessInstances(negociateContract.getUUID());
    assertEquals(1, negociateContractProcesses.size());
    ProcessInstance negociateContractInstance = negociateContractProcesses.iterator().next();
    assertEquals(InstanceState.ABORTED, negociateContractInstance.getInstanceState());

    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.FINISHED, instance.getInstanceState());
    Set<ActivityInstance> activities = instance.getActivities();
    assertEquals(2, activities.size());
    ActivityInstance nego = getActivityInstance(activities, "Negociation");
    assertEquals(ActivityState.FINISHED, nego.getState());
    assertNotNull(getActivityInstance(activities, "NegociationOK"));

    getManagementAPI().deleteProcess(mainProcess.getUUID());
    getManagementAPI().deleteProcess(requestExtension.getUUID());
    getManagementAPI().deleteProcess(negociateContract.getUUID());
  }

  public void testErrorEventSubprocessTypeCanContinueInNormalFlowInARow() throws Exception {
    ProcessDefinition requestExtension = ProcessBuilder.createProcess("requestExtension", "1.1")
    .setEventSubProcess()
    .addBooleanData("granted", true)
    .addErrorEventTask("No_more_negociation", "noNego")
    .addSystemTask("Request_extension")
    .addDecisionNode("Granted")
    .addSystemTask("end")
    .addErrorEventTask("End_negociations", "endNego")
    .addTransition("No_more_negociation", "Request_extension")
    .addTransition("Request_extension", "Granted")
    .addTransition("Granted", "end")
    .addCondition("granted")
    .addTransition("Granted", "End_negociations")
    .addCondition("!granted")
    .done();

    ProcessDefinition negociateContract = ProcessBuilder.createProcess("negociateContract", "1.0")
    .addHuman(getLogin())
    .addSystemTask("start")
    .addErrorEventTask("No_negociation", "noNego")
    .addTransition("start", "No_negociation")
    .addEventSubProcess("requestExtension", "1.1")
    .done();

    ProcessDefinition mainProcess = ProcessBuilder.createProcess("main", "7.5")
    .addSubProcess("Negociation", "negociateContract")
    .addErrorBoundaryEvent("End_negociations", "endNego")
    .addSystemTask("No_negociation")
    .addSystemTask("NegociationOK")
    .addTransition("Negociation", "NegociationOK")
    .addExceptionTransition("Negociation", "End_negociations", "No_negociation")
    .done();

    getManagementAPI().deploy(getBusinessArchive(requestExtension));
    getManagementAPI().deploy(getBusinessArchive(negociateContract));
    getManagementAPI().deploy(getBusinessArchive(mainProcess));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(mainProcess.getUUID());
    waitForInstanceEnd(4000, 50, instanceUUID);

    Set<ProcessInstance> requestExtensionProcesses = getQueryRuntimeAPI().getProcessInstances(requestExtension.getUUID());
    assertEquals(1, requestExtensionProcesses.size());
    ProcessInstance requestExtensionInstance = requestExtensionProcesses.iterator().next();
    assertEquals(InstanceState.FINISHED, requestExtensionInstance.getInstanceState());

    Set<ProcessInstance> negociateContractProcesses = getQueryRuntimeAPI().getProcessInstances(negociateContract.getUUID());
    assertEquals(1, negociateContractProcesses.size());
    ProcessInstance negociateContractInstance = negociateContractProcesses.iterator().next();
    assertEquals(InstanceState.ABORTED, negociateContractInstance.getInstanceState());

    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.FINISHED, instance.getInstanceState());
    Set<ActivityInstance> activities = instance.getActivities();
    assertEquals(2, activities.size());
    ActivityInstance nego = getActivityInstance(activities, "Negociation");
    assertEquals(ActivityState.FINISHED, nego.getState());
    assertNotNull(getActivityInstance(activities, "NegociationOK"));

    getManagementAPI().deleteProcess(mainProcess.getUUID());
    getManagementAPI().deleteProcess(requestExtension.getUUID());
    getManagementAPI().deleteProcess(negociateContract.getUUID());
  }

  
  public void test() throws Exception {
    ProcessDefinition requestExtension = ProcessBuilder.createProcess("requestExtension", "1.0")
    .setEventSubProcess()
    .addHuman(getLogin())
    .addBooleanData("granted", true)
    .addTimerTask("In_48_hrs", "2000")
    .addHumanTask("Request_extension", getLogin())
    .addDecisionNode("Granted")
    .addSystemTask("end")
    .addErrorEventTask("End_negociations", "endNego")
    .addTransition("In_48_hrs", "Request_extension")
    .addTransition("Request_extension", "Granted")
    .addTransition("Granted", "end")
    .addCondition("granted")
    .addTransition("Granted", "End_negociations")
    .addCondition("!granted")
    .done();

    ProcessDefinition negociateContract = ProcessBuilder.createProcess("negociateContract", "0.5")
    .addHuman(getLogin())
    .addSystemTask("start")
    .addHumanTask("Negociate_contrat", getLogin())
    .addSystemTask("end")
    .addTransition("start", "Negociate_contrat")
    .addTransition("Negociate_contrat", "end")
    .addEventSubProcess("requestExtension", "1.0")
    .done();

    ProcessDefinition mainProcess = ProcessBuilder.createProcess("main", "7.5")
    .addSubProcess("Negociation", "negociateContract")
    .addErrorBoundaryEvent("End_negociations", "endNego")
    .addSystemTask("No_negociation")
    .addSystemTask("NegociationOK")
    .addTransition("Negociation", "NegociationOK")
    .addExceptionTransition("Negociation", "End_negociations", "No_negociation")
    .done();

    getManagementAPI().deploy(getBusinessArchive(requestExtension));
    getManagementAPI().deploy(getBusinessArchive(negociateContract));
    getManagementAPI().deploy(getBusinessArchive(mainProcess));

    getRuntimeAPI().instantiateProcess(mainProcess.getUUID());
    Thread.sleep(5000);

    Set<LightProcessInstance> processes = getQueryRuntimeAPI().getLightProcessInstances();
    assertEquals(3, processes.size());
    List<LightProcessInstance> pro = getQueryRuntimeAPI().getLightParentProcessInstances(0, 10);
    assertEquals(1, pro.size());
    List<LightActivityInstance> activities = getQueryRuntimeAPI().getLightActivityInstancesFromRoot(pro.iterator().next().getUUID());
    assertEquals(5, activities.size());
    assertNotNull(getLightActivityInstance(activities, "Request_extension"));

    getManagementAPI().deleteProcess(mainProcess.getUUID());
    getManagementAPI().deleteProcess(requestExtension.getUUID());
    getManagementAPI().deleteProcess(negociateContract.getUUID());
  }

  public void testUnableToStartAnEventSubProcessIfNotDeployed() throws Exception {
    ProcessDefinition process =
      ProcessBuilder.createProcess("process", "1.0")
      .addHuman(getLogin())
      .addHumanTask("wait", getLogin())
      .addEventSubProcess("ESB", "1.0")
      .done();

    getManagementAPI().deploy(getBusinessArchive(process));

    try {
      getRuntimeAPI().instantiateProcess(process.getUUID());
      fail("The event sub-process is not deployed");
    } catch (ProcessNotFoundException e) {

    }

    getManagementAPI().deleteProcess(process.getUUID());
  }

  public void testStartErrorEventSubProcessUsingAConnectorError() throws Exception {
    ProcessDefinition eventSubProcess =
      ProcessBuilder.createProcess("ESB", "1.0")
      .setEventSubProcess()
      .addErrorEventTask("Error1", "code")
      .addHuman(getLogin())
      .addHumanTask("event", getLogin())
      .addTransition("Error1", "event")
      .done();

    ProcessDefinition process =
      ProcessBuilder.createProcess("process", "1.0")
      .addHuman(getLogin())
      .addHumanTask("wait", getLogin())
        .addConnector(Event.taskOnReady, SetVarConnector.class.getName(), false)
          .addInputParameter("setVariableName", "unknown")
          .addInputParameter("setValue", "5")
          .throwCatchError("code")
      .addEventSubProcess("ESB", "1.0")
      .done();

    getManagementAPI().deploy(getBusinessArchive(eventSubProcess));
    getManagementAPI().deploy(getBusinessArchive(process, null , SetVarConnector.class));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    Thread.sleep(4000);
    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.ABORTED, instance.getInstanceState());

    Set<ProcessInstance> eventSubProcesses = getQueryRuntimeAPI().getProcessInstances(eventSubProcess.getUUID());
    assertEquals(1, eventSubProcesses.size());
    ProcessInstanceUUID eventSubProcessUUID = eventSubProcesses.iterator().next().getUUID();

    Set<TaskInstance> tasks = getQueryRuntimeAPI().getTasks(eventSubProcessUUID);
    assertEquals(1, tasks.size());
    assertEquals("event", tasks.iterator().next().getActivityName());

    executeTask(eventSubProcessUUID, "event");
    QueryRuntimeAPI historyQueryRuntimeAPI = AccessorUtil.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_HISTORY_KEY);

    instance = historyQueryRuntimeAPI.getProcessInstance(eventSubProcessUUID);
    assertEquals(InstanceState.FINISHED, instance.getInstanceState());
    instance = historyQueryRuntimeAPI.getProcessInstance(instanceUUID);
    assertEquals(InstanceState.ABORTED, instance.getInstanceState());

    getManagementAPI().deleteProcess(process.getUUID());
    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
  }

  public void testStartSignalExecuteEventSubProcess() throws Exception {
    ProcessDefinition eventSubProcess =
      ProcessBuilder.createProcess("ESB", "1.0")
      .setEventSubProcess()
      .addSignalEventTask("ready", "go", true)
      .addHuman(getLogin())
      .addHumanTask("event", getLogin())
      .addTransition("ready", "event")
      .done();

    ProcessDefinition process =
      ProcessBuilder.createProcess("process", "1.0")
      .addHuman(getLogin())
      .addHumanTask("wait", getLogin())
      .addEventSubProcess("ESB", "1.0")
      .addSignalEventTask("signal", "go")
      .addHumanTask("afterSignal", getLogin())
      .addTransition("wait", "signal")
      .addTransition("signal", "afterSignal")
      .done();
    
    getManagementAPI().deploy(getBusinessArchive(eventSubProcess));
    getManagementAPI().deploy(getBusinessArchive(process));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    Thread.sleep(3000);
    executeTask(instanceUUID, "wait");
    Thread.sleep(3000);
    final Collection<LightTaskInstance> tasks = getQueryRuntimeAPI().getLightTaskList(ActivityState.READY);
    assertEquals(1, tasks.size());
    assertEquals("event", tasks.iterator().next().getActivityName());

    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.ABORTED, instance.getInstanceState());

    getManagementAPI().deleteProcess(process.getUUID());
    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
  }

  public void testEventSubProcessWithDataInitializedUsingParentProcessData() throws Exception {
    ProcessDefinition eventSubProcess =
      ProcessBuilder.createProcess("ESB", "1.0")
      .setEventSubProcess()
      .addStringDataFromScript("bb","${aa}")
      .addSignalEventTask("ready", "go", true)
      .addHuman(getLogin())
      .addHumanTask("event", getLogin())
      .addTransition("ready", "event")
      .done();

    ProcessDefinition process =
      ProcessBuilder.createProcess("process", "1.0")
      .addStringData("aa","plop")
      .addHuman(getLogin())
      .addHumanTask("wait", getLogin())
      .addEventSubProcess("ESB", "1.0")
      .addSignalEventTask("signal", "go")
      .addHumanTask("afterSignal", getLogin())
      .addTransition("wait", "signal")
      .addTransition("signal", "afterSignal")
      .done();
    
    getManagementAPI().deploy(getBusinessArchive(eventSubProcess));
    getManagementAPI().deploy(getBusinessArchive(process));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    Thread.sleep(3000);
    executeTask(instanceUUID, "wait");
    Thread.sleep(3000);
    final Collection<LightTaskInstance> tasks = getQueryRuntimeAPI().getLightTaskList(ActivityState.READY);
    assertEquals(1, tasks.size());
    LightTaskInstance taskInstance = tasks.iterator().next();
    assertEquals("event", taskInstance.getActivityName());
    Object variable = getQueryRuntimeAPI().getVariable(taskInstance.getUUID(), "bb");
    assertEquals("plop",variable);

    getManagementAPI().deleteProcess(process.getUUID());
    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
  }

  public void testInterruptProcessWhenThrowingAMessageEventToASubProcess() throws Exception {
    ProcessDefinition eventSubProcess =
      ProcessBuilder.createProcess("ESB", "1.0")
      .setEventSubProcess()
      .addReceiveEventTask("startOnError", "msg")
      .addHuman(getLogin())
      .addHumanTask("event", getLogin())
      .addTransition("startOnError", "event")
      .done();

    ProcessDefinition process =
      ProcessBuilder.createProcess("process", "1.0")
      .addSystemTask("start")
      .addSendEventTask("Error_message")
        .addOutgoingEvent("msg", "ESB", "startOnError", null)
      .addTransition("start", "Error_message")
      .addEventSubProcess("ESB", "1.0")
      .done();

    getManagementAPI().deploy(getBusinessArchive(eventSubProcess));
    getManagementAPI().deploy(getBusinessArchive(process, null, SetVarConnector.class));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    Thread.sleep(4000);
    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.ABORTED, instance.getInstanceState());

    Set<ProcessInstance> eventSubProcesses = getQueryRuntimeAPI().getProcessInstances(eventSubProcess.getUUID());
    assertEquals(1, eventSubProcesses.size());
    ProcessInstanceUUID eventSubProcessUUID = eventSubProcesses.iterator().next().getUUID();

    Set<TaskInstance> tasks = getQueryRuntimeAPI().getTasks(eventSubProcessUUID);
    assertEquals(1, tasks.size());
    assertEquals("event", tasks.iterator().next().getActivityName());

    executeTask(eventSubProcessUUID, "event");
    QueryRuntimeAPI historyQueryRuntimeAPI = AccessorUtil.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_HISTORY_KEY);

    instance = historyQueryRuntimeAPI.getProcessInstance(eventSubProcessUUID);
    assertEquals(InstanceState.FINISHED, instance.getInstanceState());
    instance = historyQueryRuntimeAPI.getProcessInstance(instanceUUID);
    assertEquals(InstanceState.ABORTED, instance.getInstanceState());

    getManagementAPI().deleteProcess(process.getUUID());
    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
  }

  public void testInterruptProcessWhenThrowingASignalEventToASubProcess() throws Exception {
    ProcessDefinition eventSubProcess =
      ProcessBuilder.createProcess("ESB", "1.0")
      .setEventSubProcess()
      .addSignalEventTask("ready", "go", true)
      .addHuman(getLogin())
      .addHumanTask("event", getLogin())
      .addTransition("ready", "event")
      .done();

    ProcessDefinition process =
      ProcessBuilder.createProcess("process", "1.0")
      .addSystemTask("start")
      .addSignalEventTask("Error_signal", "go", false)
      .addTransition("start", "Error_signal")
      .addEventSubProcess("ESB", "1.0")
      .done();

    getManagementAPI().deploy(getBusinessArchive(eventSubProcess));
    getManagementAPI().deploy(getBusinessArchive(process, null, SetVarConnector.class));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    Thread.sleep(4000);
    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.ABORTED, instance.getInstanceState());

    Set<ProcessInstance> eventSubProcesses = getQueryRuntimeAPI().getProcessInstances(eventSubProcess.getUUID());
    assertEquals(1, eventSubProcesses.size());
    ProcessInstanceUUID eventSubProcessUUID = eventSubProcesses.iterator().next().getUUID();

    Set<TaskInstance> tasks = getQueryRuntimeAPI().getTasks(eventSubProcessUUID);
    assertEquals(1, tasks.size());
    assertEquals("event", tasks.iterator().next().getActivityName());

    executeTask(eventSubProcessUUID, "event");
    QueryRuntimeAPI historyQueryRuntimeAPI = AccessorUtil.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_HISTORY_KEY);

    instance = historyQueryRuntimeAPI.getProcessInstance(eventSubProcessUUID);
    assertEquals(InstanceState.FINISHED, instance.getInstanceState());
    instance = historyQueryRuntimeAPI.getProcessInstance(instanceUUID);
    assertEquals(InstanceState.ABORTED, instance.getInstanceState());

    getManagementAPI().deleteProcess(process.getUUID());
    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
  }

  public void testInterruptProcessWhenThrowingAnErrorEventToASubProcess() throws Exception {
    ProcessDefinition eventSubProcess =
      ProcessBuilder.createProcess("ESB", "1.0")
      .setEventSubProcess()
      .addErrorEventTask("startOnError", "code")
      .addHuman(getLogin())
      .addHumanTask("event", getLogin())
      .addTransition("startOnError", "event")
      .done();

    ProcessDefinition process =
      ProcessBuilder.createProcess("process", "1.0")
      .addSystemTask("start")
      .addErrorEventTask("Error", "code")
      .addTransition("start", "Error")
      .addEventSubProcess("ESB", "1.0")
      .done();

    getManagementAPI().deploy(getBusinessArchive(eventSubProcess));
    getManagementAPI().deploy(getBusinessArchive(process, null, SetVarConnector.class));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    Thread.sleep(4000);
    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.ABORTED, instance.getInstanceState());

    Set<ProcessInstance> eventSubProcesses = getQueryRuntimeAPI().getProcessInstances(eventSubProcess.getUUID());
    assertEquals(1, eventSubProcesses.size());
    ProcessInstanceUUID eventSubProcessUUID = eventSubProcesses.iterator().next().getUUID();

    Set<TaskInstance> tasks = getQueryRuntimeAPI().getTasks(eventSubProcessUUID);
    assertEquals(1, tasks.size());
    assertEquals("event", tasks.iterator().next().getActivityName());

    executeTask(eventSubProcessUUID, "event");
    QueryRuntimeAPI historyQueryRuntimeAPI = AccessorUtil.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_HISTORY_KEY);

    instance = historyQueryRuntimeAPI.getProcessInstance(eventSubProcessUUID);
    assertEquals(InstanceState.FINISHED, instance.getInstanceState());
    instance = historyQueryRuntimeAPI.getProcessInstance(instanceUUID);
    assertEquals(InstanceState.ABORTED, instance.getInstanceState());

    getManagementAPI().deleteProcess(process.getUUID());
    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
  }

  public void testInterruptProcessWhenThrowingAnErrorEventFromAConnectorToASubProcess() throws Exception {
    ProcessDefinition eventSubProcess =
      ProcessBuilder.createProcess("ESB", "1.0")
      .setEventSubProcess()
      .addErrorEventTask("startOnError", "code")
      .addHuman(getLogin())
      .addHumanTask("event", getLogin())
      .addTransition("startOnError", "event")
      .done();
    
    ProcessDefinition process =
      ProcessBuilder.createProcess("process", "1.0")
      .addHuman(getLogin())
      .addHumanTask("wait", getLogin())
        .addConnector(Event.taskOnReady, SetVarConnector.class.getName(), false)
          .addInputParameter("setVariableName", "unknown")
          .addInputParameter("setValue", "5")
        .throwCatchError("code")
      .addEventSubProcess("ESB", "1.0")
      .done();

    getManagementAPI().deploy(getBusinessArchive(eventSubProcess));
    getManagementAPI().deploy(getBusinessArchive(process, null, SetVarConnector.class));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    Thread.sleep(4000);
    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.ABORTED, instance.getInstanceState());

    Set<ProcessInstance> eventSubProcesses = getQueryRuntimeAPI().getProcessInstances(eventSubProcess.getUUID());
    assertEquals(1, eventSubProcesses.size());
    ProcessInstanceUUID eventSubProcessUUID = eventSubProcesses.iterator().next().getUUID();

    Set<TaskInstance> tasks = getQueryRuntimeAPI().getTasks(eventSubProcessUUID);
    assertEquals(1, tasks.size());
    assertEquals("event", tasks.iterator().next().getActivityName());

    executeTask(eventSubProcessUUID, "event");
    QueryRuntimeAPI historyQueryRuntimeAPI = AccessorUtil.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_HISTORY_KEY);

    instance = historyQueryRuntimeAPI.getProcessInstance(eventSubProcessUUID);
    assertEquals(InstanceState.FINISHED, instance.getInstanceState());
    instance = historyQueryRuntimeAPI.getProcessInstance(instanceUUID);
    assertEquals(InstanceState.ABORTED, instance.getInstanceState());

    getManagementAPI().deleteProcess(process.getUUID());
    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
  }

  public void testInterruptProcessWhenThrowingMessagesEventToASubProcess() throws Exception {
    ProcessDefinition eventSubProcess =
      ProcessBuilder.createProcess("ESB", "1.0")
      .setEventSubProcess()
      .addReceiveEventTask("startOnError", "msg1")
      .addHuman(getLogin())
      .addHumanTask("event", getLogin())
      .addTransition("startOnError", "event")
      .done();

    ProcessDefinition eventSubProcess2 =
      ProcessBuilder.createProcess("other", "1.0")
      .setEventSubProcess()
      .addReceiveEventTask("startOnError", "msg2")
      .addHuman(getLogin())
      .addHumanTask("event", getLogin())
      .addTransition("startOnError", "event")
      .done();

    ProcessDefinition process =
      ProcessBuilder.createProcess("process", "1.0")
      .addSystemTask("start")
      .addSendEventTask("Error_message")
        .addOutgoingEvent("msg1", "ESB", "startOnError", null)
        .addOutgoingEvent("msg2", "other", "startOnError", null)
      .addTransition("start", "Error_message")
      .addEventSubProcess("ESB", "1.0")
      .addEventSubProcess("other", "1.0")
      .done();

    getManagementAPI().deploy(getBusinessArchive(eventSubProcess));
    getManagementAPI().deploy(getBusinessArchive(eventSubProcess2));
    getManagementAPI().deploy(getBusinessArchive(process, null, SetVarConnector.class));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    waitForInstance(6000, 50, instanceUUID, InstanceState.ABORTED);

    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.ABORTED, instance.getInstanceState());

    waitForStartingInstance(8000, 50, eventSubProcess.getUUID());
    Set<ProcessInstance> eventSubProcesses = getQueryRuntimeAPI().getProcessInstances(eventSubProcess.getUUID());
    assertEquals(1, eventSubProcesses.size());

    waitForStartingInstance(8000, 50, eventSubProcess2.getUUID());
    Set<ProcessInstance> eventSubProcesses2 = getQueryRuntimeAPI().getProcessInstances(eventSubProcess2.getUUID());
    assertEquals(1, eventSubProcesses2.size());

    getManagementAPI().deleteProcess(process.getUUID());
    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
    getManagementAPI().deleteProcess(eventSubProcess2.getUUID());
  }

  public void testAttachmentsInMainProcessAndEventSubProcess() throws Exception {
    final File subFile = File.createTempFile("subAttachment",".txt");
    ProcessDefinition eventSubProcess =
      ProcessBuilder.createProcess("ESB", "1.0")
      .addAttachment("subAttachment", subFile.getAbsolutePath(), subFile.getName())
      .setEventSubProcess()
      .addErrorEventTask("startOnError", "code")
      .addHuman(getLogin())
      .addHumanTask("event", getLogin())
      .addTransition("startOnError", "event")
      .done();

    final File mainFile = File.createTempFile("mainAttachment",".txt");
    ProcessDefinition process =
      ProcessBuilder.createProcess("process", "1.0")
      .addHuman(getLogin())
      .addAttachment("mainAttachment", mainFile.getAbsolutePath(), mainFile.getName())
      .addHumanTask("wait", getLogin())
        .addConnector(Event.taskOnReady, SetVarConnector.class.getName(), false)
          .addInputParameter("setVariableName", "unknown")
          .addInputParameter("setValue", "5")
        .throwCatchError("code")
      .addEventSubProcess("ESB", "1.0")
      .done();

    getManagementAPI().deploy(getBusinessArchive(eventSubProcess));
    getManagementAPI().deploy(getBusinessArchive(process, null, SetVarConnector.class));

    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    Thread.sleep(4000);
    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.ABORTED, instance.getInstanceState());

    Set<ProcessInstance> eventSubProcesses = getQueryRuntimeAPI().getProcessInstances(eventSubProcess.getUUID());
    assertEquals(1, eventSubProcesses.size());
    ProcessInstanceUUID eventSubProcessUUID = eventSubProcesses.iterator().next().getUUID();

    List<AttachmentInstance> attachments= getQueryRuntimeAPI().getAttachments(eventSubProcessUUID, "subAttachment");
    assertEquals(1, attachments.size());

    attachments= getQueryRuntimeAPI().getAttachments(eventSubProcessUUID, "mainAttachment");
    assertEquals(1, attachments.size());

    DocumentSearchBuilder builder = new DocumentSearchBuilder();
    builder.criterion(DocumentIndex.PROCESS_INSTANCE_UUID).equalsTo(eventSubProcessUUID.getValue());

    DocumentResult searchDocuments = getQueryRuntimeAPI().searchDocuments(builder, 0, 10);
    List<Document> documents = searchDocuments.getDocuments();
    assertEquals(2, documents.size());
    
    getManagementAPI().deleteProcess(process.getUUID());
    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
    mainFile.delete();
    subFile.delete();
  }
  
  public void testUseGlobalVariablesInStartErrorEventSubProcess() throws Exception {
	  ProcessDefinition eventSubProcess =
	      ProcessBuilder.createProcess("ESB", "1.0")
	      .setEventSubProcess()
	      .addErrorEventTask("Error1", "code")
	      .addHuman(getLogin())
	      .addHumanTask("event", getLogin())
	      	.addConnector(Event.taskOnFinish, GroovyConnector.class.getName(), true)
            .addInputParameter("script", "'b'")
            .addOutputParameter("${result}", "globalVar")
	      .addTransition("Error1", "event")
	      .done();

	    ProcessDefinition process =
	      ProcessBuilder.createProcess("process", "1.0")
	      .addStringData("globalVar", "a")
	      .addHuman(getLogin())
	      .addHumanTask("wait", getLogin())
	        .addConnector(Event.taskOnReady, SetVarConnector.class.getName(), false)
	          .addInputParameter("setVariableName", "unknown")
	          .addInputParameter("setValue", "5")
	          .throwCatchError("code")
	      .addEventSubProcess("ESB", "1.0")
	      .done();

	    getManagementAPI().deploy(getBusinessArchive(eventSubProcess, null, GroovyConnector.class));
	    getManagementAPI().deploy(getBusinessArchive(process, null, SetVarConnector.class));

	    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
	    Thread.sleep(4000);
	    ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
	    assertEquals(InstanceState.ABORTED, instance.getInstanceState());

	    Set<ProcessInstance> eventSubProcesses = getQueryRuntimeAPI().getProcessInstances(eventSubProcess.getUUID());
	    assertEquals(1, eventSubProcesses.size());
	    ProcessInstanceUUID eventSubProcessUUID = eventSubProcesses.iterator().next().getUUID();

	    Set<TaskInstance> tasks = getQueryRuntimeAPI().getTasks(eventSubProcessUUID);
	    assertEquals(1, tasks.size());
	    assertEquals("event", tasks.iterator().next().getActivityName());

	    String variable = (String) getQueryRuntimeAPI().getProcessInstanceVariable(eventSubProcessUUID, "globalVar");
	    assertEquals("a", variable);
	    executeTask(eventSubProcessUUID, "event");

	    instance = getQueryRuntimeAPI().getProcessInstance(eventSubProcessUUID);
	    assertEquals(InstanceState.FINISHED, instance.getInstanceState());
	    instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
	    assertEquals(InstanceState.ABORTED, instance.getInstanceState());
	    
	    variable = (String) getQueryRuntimeAPI().getProcessInstanceVariable(eventSubProcessUUID, "globalVar");
      assertEquals("b", variable);

	    getManagementAPI().deleteProcess(process.getUUID());
	    getManagementAPI().deleteProcess(eventSubProcess.getUUID());
	  }

}
