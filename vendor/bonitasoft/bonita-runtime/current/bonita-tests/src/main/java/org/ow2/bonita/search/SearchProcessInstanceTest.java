package org.ow2.bonita.search;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.ow2.bonita.APITestCase;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.identity.User;
import org.ow2.bonita.facade.runtime.ActivityState;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.light.LightProcessInstance;
import org.ow2.bonita.light.LightTaskInstance;
import org.ow2.bonita.search.index.ActivityInstanceIndex;
import org.ow2.bonita.search.index.CommentIndex;
import org.ow2.bonita.search.index.ProcessInstanceIndex;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.util.ProcessBuilder;

public class SearchProcessInstanceTest extends APITestCase {

  public void testIndex() {
    ProcessInstanceIndex index = new ProcessInstanceIndex();
    index.getAllFields();
  }

  public void testSearchInstanceUsingStartedDate() throws Exception {
    ProcessDefinition first = ProcessBuilder.createProcess("first", "1.0")
    .addHuman(getLogin())
    .addHumanTask("step1", getLogin())
    .addActivityPriority(2)
    .done();
    first = getManagementAPI().deploy(getBusinessArchive(first));

    ProcessInstanceUUID firstInstanceUUID = getRuntimeAPI().instantiateProcess(first.getUUID());
    LightProcessInstance firstInstance = getQueryRuntimeAPI().getLightProcessInstance(firstInstanceUUID);
    assertEquals(getLogin(), firstInstance.getStartedBy());

    Calendar c = Calendar.getInstance();
    String beforeString = new SearchDate(new Date(c.getTimeInMillis() - 1000)).toString();
    String afterString = new SearchDate(new Date(c.getTimeInMillis())).toString();

    SearchQueryBuilder searchBuilder = new SearchQueryBuilder(new ProcessInstanceIndex());
    searchBuilder.criterion(ProcessInstanceIndex.STARTED_DATE).ranges(beforeString, afterString, true);

    List<LightProcessInstance> instances = getQueryRuntimeAPI().search(searchBuilder, 0, 10);
    assertEquals(1, instances.size());

    getManagementAPI().deleteProcess(first.getUUID());
  }

  public void testSearchInstanceUsingStartedDateActiveUserAndInvolvedUser() throws Exception {
    ProcessDefinition process = ProcessBuilder.createProcess("nicolas", "1.0")
    .addHuman(getLogin())
    .addHumanTask("step1", getLogin())
    .addActivityPriority(2)
    .done();
    process = getManagementAPI().deploy(getBusinessArchive(process));

    ProcessInstanceUUID firstInstanceUUID = getRuntimeAPI().instantiateProcess(process.getUUID());
    LightProcessInstance firstInstance = getQueryRuntimeAPI().getLightProcessInstance(firstInstanceUUID);
    assertEquals(getLogin(), firstInstance.getStartedBy());

    Calendar c = Calendar.getInstance();
    Date fromDate = new Date(firstInstance.getStartedDate().getTime() - (36 * 60 * 60 * 1000));
    Date toDate = new Date(c.getTimeInMillis() + (12 * 60 * 60 *1000));

    SearchQueryBuilder searchBuilder = new SearchQueryBuilder(new ProcessInstanceIndex());
    searchBuilder.leftParenthesis();
    searchBuilder.criterion(ProcessInstanceIndex.INVOLVED_USER).startsWith(getLogin());
    searchBuilder.rightParenthesis();
    searchBuilder.and();
    searchBuilder.leftParenthesis();
    searchBuilder.criterion(ProcessInstanceIndex.ACTIVE_USER).startsWith(getLogin());
    searchBuilder.rightParenthesis();
    searchBuilder.and();
    searchBuilder.leftParenthesis();
    searchBuilder.criterion(ProcessInstanceIndex.PROCESS_DEFINITION_UUID).equalsTo(process.getUUID().getValue());
    searchBuilder.rightParenthesis();
    searchBuilder.and();
    searchBuilder.criterion(ProcessInstanceIndex.STARTED_DATE).ranges(new SearchDate(fromDate).toString(), new SearchDate(toDate).toString(), false);

    List<LightProcessInstance> instances = getQueryRuntimeAPI().search(searchBuilder, 0, 10);
    assertEquals(1, instances.size());

    getManagementAPI().deleteProcess(process.getUUID());
  }

  public void testSearchInAnArchivedProcess() throws Exception {
    ProcessDefinition first = ProcessBuilder.createProcess("first", "1.0")
    .addHuman(getLogin())
    .addHumanTask("step1", getLogin())
    .addActivityPriority(2)
    .done();
    first = getManagementAPI().deploy(getBusinessArchive(first));
    ProcessInstanceUUID firstInstanceUUID = getRuntimeAPI().instantiateProcess(first.getUUID());


    SearchQueryBuilder searchBuilder = new SearchQueryBuilder(new ProcessInstanceIndex());
    searchBuilder.criterion(ProcessInstanceIndex.STARTED_BY).equalsTo(getLogin());
    List<LightProcessInstance> instances = getQueryRuntimeAPI().search(searchBuilder, 0, 10);
    assertEquals(1, instances.size());

    executeTask(firstInstanceUUID, "step1");

    getManagementAPI().disable(first.getUUID());
    getManagementAPI().archive(first.getUUID());

    QueryRuntimeAPI journalQueryRuntimeAPI = AccessorUtil.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_JOURNAL_KEY);
    instances = journalQueryRuntimeAPI.search(searchBuilder, 0, 10);
    assertEquals(0, instances.size());
    QueryRuntimeAPI historyQueryRuntimeAPI = AccessorUtil.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_HISTORY_KEY);
    instances = historyQueryRuntimeAPI.search(searchBuilder, 0, 10);
    assertEquals(1, instances.size());

    getManagementAPI().deleteProcess(first.getUUID());
  }

  public void testSearchInstancesOfAnInvolvedUser() throws Exception {
    ProcessDefinition first = ProcessBuilder.createProcess("first", "1.0")
    .addHuman(getLogin())
    .addHumanTask("step1", getLogin())
    .done();
    first = getManagementAPI().deploy(getBusinessArchive(first));

    ProcessDefinition second = ProcessBuilder.createProcess("second", "1.0")
    .addHuman("john")
    .addHumanTask("step1", "john")
    .done();
    second = getManagementAPI().deploy(getBusinessArchive(second));

    getRuntimeAPI().instantiateProcess(first.getUUID());
    getRuntimeAPI().instantiateProcess(second.getUUID());

    SearchQueryBuilder searchBuilder = new SearchQueryBuilder(new ProcessInstanceIndex());
    searchBuilder.criterion(ProcessInstanceIndex.INVOLVED_USER).equalsTo(getLogin());
    List<LightProcessInstance> instances = getQueryRuntimeAPI().search(searchBuilder, 0, 10);
    assertEquals(1, instances.size());

    getRuntimeAPI().instantiateProcess(first.getUUID());
    instances = getQueryRuntimeAPI().search(searchBuilder, 0, 10);
    assertEquals(2, instances.size());

    getManagementAPI().deleteProcess(first.getUUID());
    getManagementAPI().deleteProcess(second.getUUID());
  }

  public void testSearchInstances() throws Exception {
    ProcessDefinition first = ProcessBuilder.createProcess("first", "1.0")
    .addHuman(getLogin())
    .addHumanTask("step1", getLogin())
    .done();
    first = getManagementAPI().deploy(getBusinessArchive(first));

    ProcessDefinition second = ProcessBuilder.createProcess("second", "1.0")
    .addHuman("john")
    .addHumanTask("step1", "john")
    .done();
    second = getManagementAPI().deploy(getBusinessArchive(second));

    ProcessInstanceUUID firstUUID = getRuntimeAPI().instantiateProcess(first.getUUID());
    getRuntimeAPI().instantiateProcess(second.getUUID());

    SearchQueryBuilder query = new SearchQueryBuilder(new ProcessInstanceIndex());
    query.criterion(ProcessInstanceIndex.ACTIVITY + ActivityInstanceIndex.NAME).equalsTo("step1");
    List<LightProcessInstance> instances = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(2, instances.size());

    query = new SearchQueryBuilder(new ProcessInstanceIndex());
    query.criterion(ProcessInstanceIndex.ACTIVITY + ActivityInstanceIndex.STATE).equalsTo("READY");
    instances = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(2, instances.size());

    executeTask(firstUUID, "step1");
    query = new SearchQueryBuilder(new ProcessInstanceIndex());
    query.criterion(ProcessInstanceIndex.ACTIVITY + ActivityInstanceIndex.STATE).equalsTo("READY");
    instances = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(1, instances.size());

    getManagementAPI().deleteProcess(first.getUUID());
    getManagementAPI().deleteProcess(second.getUUID());
  }

  public void testComments() throws Exception {
    ProcessDefinition first = ProcessBuilder.createProcess("first", "1.0")
    .addHuman(getLogin())
    .addHumanTask("step1", getLogin())
    .done();
    first = getManagementAPI().deploy(getBusinessArchive(first));

    ProcessInstanceUUID firstUUID = getRuntimeAPI().instantiateProcess(first.getUUID());
    getRuntimeAPI().addComment(firstUUID, "ok", getLogin());

    Collection<LightTaskInstance> tasks = getQueryRuntimeAPI().getLightTaskList(firstUUID, ActivityState.READY);
    assertEquals(1, tasks.size());
    ActivityInstanceUUID taskUUID = tasks.iterator().next().getUUID();

    getRuntimeAPI().addComment(taskUUID, "one", "john");

    SearchQueryBuilder query = new SearchQueryBuilder(new ProcessInstanceIndex());
    query.criterion(ProcessInstanceIndex.COMMENT + CommentIndex.MESSAGE).equalsTo("ok");
    List<LightProcessInstance> instances = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(1, instances.size());

    query = new SearchQueryBuilder(new ProcessInstanceIndex());
    query.criterion(ProcessInstanceIndex.COMMENT + CommentIndex.AUTHOR).equalsTo(getLogin());
    instances = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(1, instances.size());

    query = new SearchQueryBuilder(new ProcessInstanceIndex());
    query.criterion(ProcessInstanceIndex.COMMENT + CommentIndex.AUTHOR).equalsTo("john");
    instances = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(1, instances.size());

    getManagementAPI().deleteProcess(first.getUUID());
  }

  public void testSearchVariables() throws Exception {
    ProcessDefinition defintion = ProcessBuilder.createProcess("variables", "2.4")
    .addStringData("pString", "process")
    .addIntegerData("pInteger", 9)
    .addBooleanData("pBoolean", true)
    .addHuman(getLogin())
    .addHumanTask("activity", getLogin())
    .addStringData("aString", "test")
    .addBooleanData("aBoolean", false)
    .done();

    getManagementAPI().deploy(getBusinessArchive(defintion));
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(defintion.getUUID());

    SearchQueryBuilder query = new SearchQueryBuilder(new ProcessInstanceIndex());
    query.criterion(ProcessInstanceIndex.VARIABLE_NAME).equalsTo("pString");
    List<LightProcessInstance> instances = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(1, instances.size());
    assertEquals(instanceUUID, instances.get(0).getUUID());

    query = new SearchQueryBuilder(new ProcessInstanceIndex());
    query.criterion(ProcessInstanceIndex.VARIABLE_VALUE).equalsTo("process");
    instances = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(1, instances.size());

    query = new SearchQueryBuilder(new ProcessInstanceIndex());
    query.criterion(ProcessInstanceIndex.ACTIVITY + ActivityInstanceIndex.VARIABLE_NAME).equalsTo("aString");
    instances = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(1, instances.size());

    Collection<LightTaskInstance> tasks = getQueryRuntimeAPI().getLightTaskList(instanceUUID, ActivityState.READY);
    assertEquals(1, tasks.size());
    ActivityInstanceUUID taskUUID = tasks.iterator().next().getUUID();

    getRuntimeAPI().setActivityInstanceVariable(taskUUID, "aString", "updated");
    String value = (String) getQueryRuntimeAPI().getActivityInstanceVariable(taskUUID, "aString");
    assertEquals("updated", value);

    query = new SearchQueryBuilder(new ProcessInstanceIndex());
    query.criterion(ProcessInstanceIndex.ACTIVITY + ActivityInstanceIndex.VARIABLE_VALUE).equalsTo("test");
    instances = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(0, instances.size());

    query = new SearchQueryBuilder(new ProcessInstanceIndex());
    query.criterion(ProcessInstanceIndex.ACTIVITY + ActivityInstanceIndex.VARIABLE_VALUE).equalsTo("updated");
    instances = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(1, instances.size());

    getManagementAPI().deleteProcess(defintion.getUUID());
  }

  public void testSearchProcessDefUUIDStartsWith() throws Exception {
    ProcessDefinition first = ProcessBuilder.createProcess("first", "1.0")
    .addHuman(getLogin())
    .addHumanTask("step1", getLogin())
    .done();
    first = getManagementAPI().deploy(getBusinessArchive(first));

    ProcessDefinition second = ProcessBuilder.createProcess("second", "1.0")
    .addHuman("john")
    .addHumanTask("step1", "john")
    .done();
    second = getManagementAPI().deploy(getBusinessArchive(second));

    ProcessInstanceUUID firstUUID = getRuntimeAPI().instantiateProcess(first.getUUID());

    SearchQueryBuilder query = new SearchQueryBuilder(new ProcessInstanceIndex());
    query.criterion(ProcessInstanceIndex.PROCESS_DEFINITION_UUID).startsWith("fir");
    List<LightProcessInstance> instances = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(1, instances.size());
    LightProcessInstance instance = instances.get(0);
    assertEquals(firstUUID, instance.getUUID());

    getManagementAPI().deleteProcess(first.getUUID());
    getManagementAPI().deleteProcess(second.getUUID());
  }

  public void testSearchProcessDefUUIDEquals() throws Exception {
    ProcessDefinition first = ProcessBuilder.createProcess("first", "1.0")
    .addHuman(getLogin())
    .addHumanTask("step1", getLogin())
    .done();
    first = getManagementAPI().deploy(getBusinessArchive(first));

    ProcessDefinition second = ProcessBuilder.createProcess("second", "1.0")
    .addHuman("john")
    .addHumanTask("step1", "john")
    .done();
    second = getManagementAPI().deploy(getBusinessArchive(second));

    ProcessInstanceUUID secondUUID = getRuntimeAPI().instantiateProcess(second.getUUID());

    SearchQueryBuilder query = new SearchQueryBuilder(new ProcessInstanceIndex());
    query.criterion(ProcessInstanceIndex.PROCESS_DEFINITION_UUID).equalsTo(second.getUUID().getValue());
    List<LightProcessInstance> instances = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(1, instances.size());
    LightProcessInstance instance = instances.get(0);
    assertEquals(secondUUID, instance.getUUID());

    getManagementAPI().deleteProcess(first.getUUID());
    getManagementAPI().deleteProcess(second.getUUID());
  }

  public void testSearchProcessDefUUIDAndActivityDescription() throws Exception {
    ProcessDefinition first = ProcessBuilder.createProcess("first", "1.0")
    .addHuman(getLogin())
    .addHumanTask("step1", getLogin())
    .done();
    first = getManagementAPI().deploy(getBusinessArchive(first));

    ProcessDefinition second = ProcessBuilder.createProcess("second", "1.0")
    .addHuman("john")
    .addHumanTask("step1", "john")
    .addDescription("this is a description")
    .done();
    second = getManagementAPI().deploy(getBusinessArchive(second));

    getRuntimeAPI().instantiateProcess(first.getUUID());
    ProcessInstanceUUID secondUUID = getRuntimeAPI().instantiateProcess(second.getUUID());

    SearchQueryBuilder query = new SearchQueryBuilder(new ProcessInstanceIndex());
    query.criterion(ProcessInstanceIndex.PROCESS_DEFINITION_UUID).equalsTo(second.getUUID().getValue());
    query.and().criterion(ProcessInstanceIndex.ACTIVITY + ActivityInstanceIndex.DESCRIPTION).startsWith("this is");
    List<LightProcessInstance> instances = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(1, instances.size());
    LightProcessInstance instance = instances.get(0);
    assertEquals(secondUUID, instance.getUUID());

    getManagementAPI().deleteProcess(first.getUUID());
    getManagementAPI().deleteProcess(second.getUUID());
  }

  public void testSearchProcessInstanceUUID() throws Exception {
    ProcessDefinition first = ProcessBuilder.createProcess("first", "1.0")
    .addHuman(getLogin())
    .addHumanTask("step1", getLogin())
    .done();
    first = getManagementAPI().deploy(getBusinessArchive(first));

    ProcessDefinition second = ProcessBuilder.createProcess("second", "1.0")
    .addHuman("john")
    .addHumanTask("step1", "john")
    .done();
    second = getManagementAPI().deploy(getBusinessArchive(second));

    getRuntimeAPI().instantiateProcess(first.getUUID());
    ProcessInstanceUUID secondUUID = getRuntimeAPI().instantiateProcess(second.getUUID());

    SearchQueryBuilder query = new SearchQueryBuilder(new ProcessInstanceIndex());
    query.criterion(ProcessInstanceIndex.PROCESS_INSTANCE_UUID).equalsTo(secondUUID.getValue());
    List<LightProcessInstance> instances = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(1, instances.size());
    LightProcessInstance instance = instances.get(0);
    assertEquals(secondUUID, instance.getUUID());

    getManagementAPI().deleteProcess(first.getUUID());
    getManagementAPI().deleteProcess(second.getUUID());
  }

  public void testSearchProcessRootInstanceUUID() throws Exception {
    ProcessDefinition first = ProcessBuilder.createProcess("first", "1.0")
    .addHuman(getLogin())
    .addHumanTask("step1", getLogin())
    .done();
    first = getManagementAPI().deploy(getBusinessArchive(first));

    ProcessDefinition second = ProcessBuilder.createProcess("second", "1.0")
    .addHuman("john")
    .addSubProcess("step1", "first")
    .done();
    second = getManagementAPI().deploy(getBusinessArchive(second));

    getRuntimeAPI().instantiateProcess(first.getUUID());
    ProcessInstanceUUID secondUUID = getRuntimeAPI().instantiateProcess(second.getUUID());

    SearchQueryBuilder query = new SearchQueryBuilder(new ProcessInstanceIndex());
    query.criterion(ProcessInstanceIndex.PROCESS_ROOT_INSTANCE_UUID).equalsTo(secondUUID.getValue());
    List<LightProcessInstance> instances = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(2, instances.size());

    getManagementAPI().deleteProcess(second.getUUID());
    getManagementAPI().deleteProcess(first.getUUID());
  }

  public void testSearchInstancesOfAnActiveUser() throws Exception {
    ProcessDefinition first = ProcessBuilder.createProcess("first", "1.0")
    .addHuman(getLogin())
    .addHumanTask("step1", getLogin())
    .done();
    first = getManagementAPI().deploy(getBusinessArchive(first));

    ProcessDefinition second = ProcessBuilder.createProcess("second", "1.0")
    .addHuman("john")
    .addHumanTask("step1", "john")
    .done();
    second = getManagementAPI().deploy(getBusinessArchive(second));

    getRuntimeAPI().instantiateProcess(first.getUUID());
    getRuntimeAPI().instantiateProcess(second.getUUID());

    SearchQueryBuilder searchBuilder = new SearchQueryBuilder(new ProcessInstanceIndex());
    searchBuilder.criterion(ProcessInstanceIndex.ACTIVE_USER).equalsTo(getLogin());
    List<LightProcessInstance> instances = getQueryRuntimeAPI().search(searchBuilder, 0, 10);
    assertEquals(1, instances.size());

    getRuntimeAPI().instantiateProcess(first.getUUID());
    instances = getQueryRuntimeAPI().search(searchBuilder, 0, 10);
    assertEquals(2, instances.size());

    getManagementAPI().deleteProcess(first.getUUID());
    getManagementAPI().deleteProcess(second.getUUID());
  }

  public void testSearchInstancesWithPagination() throws Exception {
    ProcessDefinition first = ProcessBuilder.createProcess("first", "1.0")
    .addHuman(getLogin())
    .addHumanTask("step1", getLogin())
    .done();
    first = getManagementAPI().deploy(getBusinessArchive(first));

    ProcessDefinition second = ProcessBuilder.createProcess("second", "1.0")
    .addHuman("john")
    .addHumanTask("step1", "john")
    .done();
    second = getManagementAPI().deploy(getBusinessArchive(second));

    getRuntimeAPI().instantiateProcess(first.getUUID());
    getRuntimeAPI().instantiateProcess(second.getUUID());

    SearchQueryBuilder query = new SearchQueryBuilder(new ProcessInstanceIndex());
    query.criterion(ProcessInstanceIndex.ACTIVITY + ActivityInstanceIndex.NAME).equalsTo("step1");
    List<LightProcessInstance> instances = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(2, instances.size());

    query = new SearchQueryBuilder(new ProcessInstanceIndex());
    query.criterion(ProcessInstanceIndex.ACTIVITY + ActivityInstanceIndex.NAME).equalsTo("step1");
    instances = getQueryRuntimeAPI().search(query, 1, 10);
    assertEquals(1, instances.size());

    query = new SearchQueryBuilder(new ProcessInstanceIndex());
    query.criterion(ProcessInstanceIndex.ACTIVITY + ActivityInstanceIndex.NAME).equalsTo("step1");
    instances = getQueryRuntimeAPI().search(query, 0, 1);
    assertEquals(1, instances.size());

    getManagementAPI().deleteProcess(first.getUUID());
    getManagementAPI().deleteProcess(second.getUUID());
  }

//  public void testSearchProcessesWithAtLeastAnOverdueTask() throws Exception {
//    SearchDate from = new SearchDate(new Date(System.currentTimeMillis() - 10000));
//    SearchDate to = new SearchDate(new Date(System.currentTimeMillis() + 20000));
//
//    ProcessDefinition definition = ProcessBuilder.createProcess("expected", "1.0")
//    .addHuman(getLogin())
//    .addHumanTask("task1", getLogin())
//      .addActivityExecutingTime(10000)
//    .done();
//
//    getManagementAPI().deploy(getBusinessArchive(definition));
//    getRuntimeAPI().instantiateProcess(definition.getUUID());
//    final SearchQueryBuilder query = new SearchQueryBuilder(new ProcessInstanceIndex());
//    query.criterion(ProcessInstanceIndex.ACTIVITY + ActivityInstanceIndex.EXPECTED_END_DATE).ranges(from.toString(), to.toString(), true);
//    List<LightProcessInstance> instances = getQueryRuntimeAPI().search(query, 0, 10);
//    assertEquals(1, instances.size());
//    getManagementAPI().deleteProcess(definition.getUUID());
//  }

  //  public void testSearchVars() throws Exception {
  //    ProcessDefinition definitionA = ProcessBuilder.createProcess("varA", "2.4")
  //    .addIntegerData("A", 1)
  //    .addIntegerData("B", 2)
  //    .done();
  //
  //    ProcessDefinition definitionB = ProcessBuilder.createProcess("varB", "2.4")
  //    .addIntegerData("A", 2)
  //    .addIntegerData("B", 1)
  //    .done();
  //
  //    getManagementAPI().deploy(getBusinessArchive(definitionA));
  //    ProcessInstanceUUID instanceAUUID = getRuntimeAPI().instantiateProcess(definitionA.getUUID());
  //    getManagementAPI().deploy(getBusinessArchive(definitionB));
  //    getRuntimeAPI().instantiateProcess(definitionB.getUUID());
  //
  //    SearchQueryBuilder query = new SearchQueryBuilder(new ProcessInstanceIndex());
  //    query.criterion(ProcessInstanceIndex.VARIABLE_NAME).equalsTo("A").and()
  //    .criterion(ProcessInstanceIndex.VARIABLE_VALUE).equalsTo("1");
  //    List<LightProcessInstance> instances = getQueryRuntimeAPI().search(query, 0, 10);
  //    assertEquals(1, instances.size());
  //    assertEquals(instanceAUUID, instances.get(0).getUUID());
  //
  //    getManagementAPI().deleteProcess(definitionA.getUUID());
  //    getManagementAPI().deleteProcess(definitionB.getUUID());
  //  }

  public void testSearchInstancesOfInitiator() throws Exception {
    ProcessDefinition first = ProcessBuilder.createProcess("first", "1.0")
    .addHuman(getLogin())
    .addHumanTask("step1", getLogin())
    .done();
    first = getManagementAPI().deploy(getBusinessArchive(first));

    ProcessDefinition second = ProcessBuilder.createProcess("second", "1.0")
    .addHuman("john")
    .addHumanTask("step1", "john")
    .done();
    second = getManagementAPI().deploy(getBusinessArchive(second));
    getRuntimeAPI().instantiateProcess(first.getUUID());

    final String specialLogin = "test%test_";
    final User user = getIdentityAPI().addUser(specialLogin, "bpm");
    loginAs(specialLogin, "bpm");
    getRuntimeAPI().instantiateProcess(second.getUUID());

    login();
    SearchQueryBuilder searchBuilder = new SearchQueryBuilder(new ProcessInstanceIndex());
    searchBuilder.criterion(ProcessInstanceIndex.STARTED_BY).equalsTo(specialLogin);
    List<LightProcessInstance> instances = getQueryRuntimeAPI().search(searchBuilder, 0, 10);
    assertEquals(1, instances.size());
    assertEquals(second.getUUID(), instances.get(0).getProcessDefinitionUUID());

    getIdentityAPI().removeUserByUUID(user.getUUID());
    getManagementAPI().deleteProcess(first.getUUID());
    getManagementAPI().deleteProcess(second.getUUID());
  }

}
