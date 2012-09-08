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
package org.ow2.bonita;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import junit.framework.TestCase;

import org.ow2.bonita.cmd.CreateDatabaseCommand;
import org.ow2.bonita.cmd.DatabaseCleanerCommand;
import org.ow2.bonita.facade.BAMAPI;
import org.ow2.bonita.facade.CommandAPI;
import org.ow2.bonita.facade.Context;
import org.ow2.bonita.facade.IdentityAPI;
import org.ow2.bonita.facade.ManagementAPI;
import org.ow2.bonita.facade.QueryDefinitionAPI;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.RepairAPI;
import org.ow2.bonita.facade.RuntimeAPI;
import org.ow2.bonita.facade.WebAPI;
import org.ow2.bonita.facade.def.element.BoundaryEvent;
import org.ow2.bonita.facade.def.element.BusinessArchive;
import org.ow2.bonita.facade.def.majorElement.ActivityDefinition;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.def.majorElement.TransitionDefinition;
import org.ow2.bonita.facade.exception.ActivityNotFoundException;
import org.ow2.bonita.facade.exception.DeploymentException;
import org.ow2.bonita.facade.exception.IllegalTaskStateException;
import org.ow2.bonita.facade.exception.InstanceNotFoundException;
import org.ow2.bonita.facade.exception.ProcessNotFoundException;
import org.ow2.bonita.facade.exception.TaskNotFoundException;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.ActivityState;
import org.ow2.bonita.facade.runtime.Category;
import org.ow2.bonita.facade.runtime.InstanceState;
import org.ow2.bonita.facade.runtime.ProcessInstance;
import org.ow2.bonita.facade.runtime.TaskInstance;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.light.LightActivityInstance;
import org.ow2.bonita.light.LightProcessInstance;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.util.BonitaConstants;
import org.ow2.bonita.util.BonitaException;
import org.ow2.bonita.util.BonitaRuntimeException;
import org.ow2.bonita.util.BusinessArchiveFactory;
import org.ow2.bonita.util.ClassDataTool;
import org.ow2.bonita.util.Misc;
import org.ow2.bonita.util.ProcessBuilder;
import org.ow2.bonita.util.SimpleCallbackHandler;

public abstract class APITestCase extends TestCase {

  /** LOG. */
  protected static final Logger LOG = Logger.getLogger(APITestCase.class.getName());
  private RuntimeAPI runtimeAPI;
  private ManagementAPI managementAPI;
  private QueryRuntimeAPI queryRuntimeAPI;
  private WebAPI webAPI;
  private BAMAPI bamAPI;
  private QueryDefinitionAPI queryDefinitionAPI;
  private CommandAPI commandAPI;
  private IdentityAPI identityAPI;
  private RepairAPI repairAPI;

  protected LoginContext loginContext;

  protected static final String STANDALONE_ACCESS = "standalone_access";
  protected static final String EJB_ACCESS = "ejb_access";

  // This boolean is set to false is the previous test was successful,
  // this way, the db is recreated only when the previous test has failed
  private static boolean recreateDb = true;

  private boolean testFail = false;

  @Override
  protected void setUp() throws Exception {
    // Initialize the Jaas login configuration with a default value
    // This works only for running tests from eclipse
    final String defaultLoginFile = "src/main/resources/jaas-standard.cfg";
    final String loginFile = System.getProperty(BonitaConstants.JAAS_PROPERTY, defaultLoginFile);
    if (loginFile.equals(defaultLoginFile)) {
      System.setProperty(BonitaConstants.JAAS_PROPERTY, defaultLoginFile);
    }
    Configuration.getConfiguration().refresh();
    final String defaultLoggingFile = "src/main/resources/logging.properties";
    final String loggingFile = System.getProperty(BonitaConstants.LOGGING_PROPERTY, defaultLoggingFile);
    if (loggingFile.equals(defaultLoggingFile)) {
      System.setProperty(BonitaConstants.LOGGING_PROPERTY, defaultLoggingFile);
    }

    if (System.getProperty(BonitaConstants.HOME) == null) {
    	throw new RuntimeException("You must specify the system property " + BonitaConstants.HOME + " in order to execute tests. Please add a -D" +BonitaConstants.HOME + " VM argument in your test configuration." );
    }

    super.setUp();

    if (LOG.isLoggable(Level.WARNING)) {
      LOG.warning("======== Starting test: " + this.getClass().getName() + "." + this.getName() + "() ==========");
    }
    login();
    try {
      getManagementAPI().deployJar("database.jar", Misc.generateJar(CreateDatabaseCommand.class, DatabaseCleanerCommand.class));
    } catch (DeploymentException de) {
      String message = de.getMessage();
      if (!message.contains("database.jar")) {
        throw de;
      }
    }

    getCommandAPI().execute(new CreateDatabaseCommand(recreateDb));
  }

  @Override
  protected void tearDown() throws Exception {
    String message = getCommandAPI().execute(new DatabaseCleanerCommand(this.testFail));
    if (!this.testFail && !message.equals(BonitaConstants.DEFAULT_DOMAIN)) {
      throw new BonitaRuntimeException(message);
    }

    if (this.loginContext != null) {
      this.loginContext.logout();
    }
    if (LOG.isLoggable(Level.WARNING)) {
      LOG.warning("======== Ending test: " + this.getName() + "==========");
    }
    this.runtimeAPI = null;
    this.managementAPI = null;
    this.queryRuntimeAPI = null;
    this.queryDefinitionAPI = null;
    this.commandAPI = null;
    this.loginContext = null;
  }

  @Override
  protected void runTest() throws Throwable {
    try {
      super.runTest();
      // All went well, don't need to recreate db
      recreateDb = false;
    } catch (final Throwable e) {
      // There is a failure, we need to recreate db
      recreateDb = true;
      this.testFail = true;
      throw e;
    }
  }

  /*
   * APIs
   */
  protected synchronized RuntimeAPI getRuntimeAPI() {
    if (this.runtimeAPI == null) {
      this.runtimeAPI = AccessorUtil.getRuntimeAPI();
    }
    return this.runtimeAPI;
  }

  protected synchronized ManagementAPI getManagementAPI() {
    if (this.managementAPI == null) {
      this.managementAPI = AccessorUtil.getManagementAPI();
    }
    return this.managementAPI;
  }

  protected synchronized QueryRuntimeAPI getQueryRuntimeAPI() {
    if (this.queryRuntimeAPI == null) {
      this.queryRuntimeAPI = AccessorUtil.getQueryRuntimeAPI();
    }
    return this.queryRuntimeAPI;
  }

  protected synchronized WebAPI getWebAPI() {
    if (this.webAPI == null) {
      this.webAPI = AccessorUtil.getWebAPI();
    }
    return this.webAPI;
  }

  protected synchronized BAMAPI getBAMAPI() {
    if (this.bamAPI == null) {
      this.bamAPI = AccessorUtil.getBAMAPI();
    }
    return this.bamAPI;
  }

  protected synchronized QueryDefinitionAPI getQueryDefinitionAPI() {
    if (this.queryDefinitionAPI == null) {
      this.queryDefinitionAPI = AccessorUtil.getQueryDefinitionAPI();
    }
    return this.queryDefinitionAPI;
  }

  protected synchronized CommandAPI getCommandAPI() {
    if (this.commandAPI == null) {
      this.commandAPI = AccessorUtil.getCommandAPI();
    }
    return this.commandAPI;
  }

  protected synchronized IdentityAPI getIdentityAPI() {
    if (this.identityAPI == null) {
      this.identityAPI = AccessorUtil.getIdentityAPI();
    }
    return this.identityAPI;
  }

  protected synchronized RepairAPI getRepairAPI() {
    if (this.repairAPI == null) {
      this.repairAPI = AccessorUtil.getRepairAPI();
    }
    return this.repairAPI;
  }

  /*
   * BEGIN OF TO BE REMOVED
   */
  protected void checkExecutedOnce(final ProcessInstance instance, final String[] activityIds)
  throws InstanceNotFoundException, ActivityNotFoundException {
    checkExecutedOnce(instance.getUUID(), activityIds);
  }

  protected void checkExecutedOnce(final ProcessInstanceUUID instanceUUID, final String... activityIds)
  throws InstanceNotFoundException, ActivityNotFoundException {
    checkExecuted(instanceUUID, activityIds);
    final ProcessInstance processInstance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.FINISHED, processInstance.getInstanceState());
  }

  protected void checkNotExecuted(final ProcessInstance processInstance, final String[] activityIds)
  throws InstanceNotFoundException, ActivityNotFoundException {
    checkNotExecuted(processInstance.getUUID(), activityIds);
  }

  protected void checkNotExecuted(final ProcessInstanceUUID instanceUUID, final String... activityIds)
  throws InstanceNotFoundException, ActivityNotFoundException {
    for (final String activityId : activityIds) {
      try {
        getQueryRuntimeAPI().getActivityInstances(instanceUUID, activityId);
        fail("Activity : " + activityId + " must not be executed in this instance!");
      } catch (final ActivityNotFoundException e) {
        //nothing, we expect an exception
      } 
    }
    final ProcessInstance processInstance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertEquals(InstanceState.FINISHED, processInstance.getInstanceState());
  }

  protected void checkStopped(final ProcessInstance instance, final String[] activityIds)
  throws InstanceNotFoundException, ActivityNotFoundException {
    checkStopped(instance.getUUID(), activityIds);
  }

  protected void checkStopped(final ProcessInstanceUUID instanceUUID, final String[] activityIds)
  throws InstanceNotFoundException, ActivityNotFoundException {
    final ProcessInstance instance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertNotNull("Can't find an instance with uuid : " + instanceUUID, instance);
    checkExecuted(instanceUUID, activityIds);
  }

  private void checkExecuted(final ProcessInstanceUUID instanceUUID, final String[] activityIds)
  throws InstanceNotFoundException, ActivityNotFoundException {
    for (final String activityId : activityIds) {
      final Set<ActivityInstance> activityInsts =
        getQueryRuntimeAPI().getActivityInstances(instanceUUID, activityId);
      assertEquals("Activity " + activityId + " executed more than once.", 1, activityInsts.size());
      final ActivityInstance activityInst = activityInsts.iterator().next();
      assertNotNull("Activity " + activityId + " not executed", activityInst);
      assertNotNull("Bad state for activity " + activityId, activityInst.getEndedDate());
    }
  }

  protected void checkExecutedManyTimes(final ProcessInstance instance, final String[] activityIds, final int execNb)
  throws InstanceNotFoundException, ActivityNotFoundException {
    checkExecutedManyTimes(instance.getUUID(), activityIds, execNb);
  }

  protected void checkExecutedManyTimes(final ProcessInstanceUUID instanceUUID, final String[] activityIds, final int execNb)
  throws InstanceNotFoundException, ActivityNotFoundException {
    for (final String activityId : activityIds) {
      final Set<ActivityInstance> activities = getQueryRuntimeAPI().getActivityInstances(instanceUUID, activityId);
      assertNotNull("Activity " + activityId + " not executed", activities);
      assertTrue("Activity " + activityId + " not executed", activities.size() > 0);
      assertEquals("Wrong number of executions for activity : " + activityId, execNb, activities.size());
      for (final ActivityInstance activityInst : activities) {
        assertNotNull("Bad state for activity " + activityId, activityInst.getStartedDate());
        assertNotNull("Bad state for activity " + activityId, activityInst.getEndedDate());
      }
    }
  }

  protected void checkOnlyOneExecuted(final ProcessInstanceUUID instanceUUID, final String[] activityIds)
  throws InstanceNotFoundException, ActivityNotFoundException {
    checkAtLeastOneExecuted(instanceUUID, activityIds);
    checkAtMostOneExecuted(instanceUUID, activityIds);
  }

  protected void checkAtMostOneExecuted(final ProcessInstanceUUID instanceUUID, final String[] activityIds)
  throws InstanceNotFoundException {
    boolean found = false;
    for (final String activityId : activityIds) {
      Set<ActivityInstance> activityInsts = null;
      try {
        activityInsts = getQueryRuntimeAPI().getActivityInstances(instanceUUID, activityId);
        for (final ActivityInstance activityInst : activityInsts) {
          if (activityInst.getEndedDate() != null) {
            assertFalse("More than one activity in "
                + Misc.componentsToString(activityIds, false)
                + " has been executed.", found);
            found = true;
          }
        }
      } catch (final ActivityNotFoundException e) {
        // nothing to do
      } 
    }
  }

  protected void checkAtLeastOneExecuted(final ProcessInstanceUUID instanceUUID, final String[] activityIds)
  throws InstanceNotFoundException {
    for (final String activityId : activityIds) {
      try {
        final Set<ActivityInstance> activityInsts =
          getQueryRuntimeAPI().getActivityInstances(instanceUUID, activityId);
        for (final ActivityInstance activityInst : activityInsts) {
          if (activityInst.getEndedDate() != null) {
            return;
          }
        }
      } catch (final ActivityNotFoundException e) {
        // nothing to do
      } 
    }
    fail("At least one activity of " + Misc.componentsToString(activityIds, false)
        + " has to be executed, none was found.");
  }

  /*
   * END OF TO BE REMOVED
   */
  protected void executeTask(final ProcessInstanceUUID instanceUUID, final String activityId)
  throws InstanceNotFoundException, TaskNotFoundException, IllegalTaskStateException {
    TaskInstance matchingActivity;
    final ActivityInstanceUUID taskUUID = executeTaskWithoutCheckingState(
        instanceUUID, activityId);

    matchingActivity = getQueryRuntimeAPI().getTask(taskUUID);
    assertEquals(ActivityState.FINISHED, matchingActivity.getState());

    assertNotNull(matchingActivity.getStartedDate());
    assertNotNull(matchingActivity.getEndedDate());

    final long startTime = matchingActivity.getStartedDate().getTime();
    final long endTime = matchingActivity.getEndedDate().getTime();
    assertTrue(endTime >= startTime);
  }

  protected ActivityInstanceUUID executeTaskWithoutCheckingState(
      final ProcessInstanceUUID instanceUUID, final String activityId)
      throws InstanceNotFoundException, TaskNotFoundException,
      IllegalTaskStateException {
    TaskInstance matchingActivity;
    final ActivityInstanceUUID taskUUID = startTaskWithoutVerifiyAfterState(
        instanceUUID, activityId);

    matchingActivity = getQueryRuntimeAPI().getTask(taskUUID);
    assertEquals(ActivityState.EXECUTING, matchingActivity.getState());

    assertNotNull(matchingActivity.getStartedDate());
    assertNull(matchingActivity.getEndedDate());

    getRuntimeAPI().finishTask(taskUUID, true);
    return taskUUID;
  }

  protected ActivityInstanceUUID startTaskWithoutVerifiyAfterState(
      final ProcessInstanceUUID instanceUUID, final String activityId)
      throws InstanceNotFoundException, TaskNotFoundException,
      IllegalTaskStateException {
    final Collection<TaskInstance> taskActivities = getQueryRuntimeAPI().getTaskList(instanceUUID, ActivityState.READY);

    final Collection<ActivityInstanceUUID> tasksToDo = new ArrayList<ActivityInstanceUUID>();
    TaskInstance matchingActivity = null;
    for (final TaskInstance task : taskActivities) {
      tasksToDo.add(task.getUUID());
      if (task.getProcessInstanceUUID().equals(instanceUUID)
          && task.getActivityName().equals(activityId)) {
        matchingActivity = task;
        break;
      }
    }
    assertFalse("No tasks in instance " + instanceUUID + " to do for the current user.", tasksToDo.isEmpty());

    assertNotNull("no activity with activityId : " + activityId + " is defined within instance : " + instanceUUID
        + " for the current user. Available tasks: " + taskActivities, matchingActivity);

    final ActivityInstanceUUID taskUUID = matchingActivity.getUUID();

    assertNull(matchingActivity.getStartedDate());
    assertNull(matchingActivity.getEndedDate());

    assertEquals(ActivityState.READY, matchingActivity.getState());

    getRuntimeAPI().startTask(taskUUID, true);
    return taskUUID;
  }

  protected void waitForActivity(long maxWait, long sleepTime, ProcessInstanceUUID instanceUUID, final String activityId)
  throws BonitaException {
    long maxDate = System.currentTimeMillis() + maxWait;
    ActivityInstance activity = null;
    while (System.currentTimeMillis() < maxDate) {
      activity = null;
      try {
        Set<ActivityInstance> activities = getQueryRuntimeAPI().getActivityInstances(instanceUUID, activityId);
        for (ActivityInstance act : activities) {
          if (act.getActivityName().equals(activityId)) {
            activity = act;
            break;
          }
        }
      } catch (ActivityNotFoundException anfe) {
        //maybe instance move from journal to history
      }
      try {
        Thread.sleep(sleepTime);
      } catch (Exception e) {
        throw new BonitaRuntimeException(e);
      }
    }
    assertNotNull(activity);
  }

  protected void waitForInstanceEnd(long maxWait, long sleepTime, ProcessInstanceUUID instanceUUID) throws BonitaException {
    waitForInstance(maxWait, sleepTime, instanceUUID, InstanceState.FINISHED);
  }

  protected void waitForInstance(final long maxWait, final long sleepTime, final ProcessInstanceUUID instanceUUID, final InstanceState state) throws BonitaException {
    final long maxDate = System.currentTimeMillis() + maxWait;
    LightProcessInstance processInstance = null;
    LightProcessInstance temp = null;
    while (System.currentTimeMillis() < maxDate && processInstance == null) {
      try {
        temp = getQueryRuntimeAPI().getLightProcessInstance(instanceUUID);
        if (state == temp.getInstanceState()) {
          processInstance = temp;
        }
      } catch (Exception infe) {
        //maybe instance move from journal to history due to handler
        System.err.println("Warning: Instance not found. Probably due to FinishedProcessInstanceHandler that archives Process instances.");
      }
      try {
        Thread.sleep(sleepTime);
      } catch (Exception e) {
        // still waiting
        System.err.println("Warning: Thread sleep error.");
      }
    }
    StringBuilder stb = new StringBuilder("No instance found with desired State: ");
    stb.append(state);
    if(temp != null) {
      stb.append("Actual state: ");
      stb.append(temp.getInstanceState());
    }
    assertNotNull(stb.toString(), processInstance);
  }

  protected void waitForStartingInstance(long maxWait, long sleepTime, ProcessDefinitionUUID definitionUUID) throws BonitaException {
    long maxDate = System.currentTimeMillis() + maxWait;
    ProcessInstance processInstance = null;
    while (System.currentTimeMillis() < maxDate) {
      processInstance = null;
      try {
        Set<ProcessInstance> processInstances = getQueryRuntimeAPI().getProcessInstances(definitionUUID);
        if (!processInstances.isEmpty()) {
          processInstance = processInstances.iterator().next();
        }
      } catch (Exception infe) {
        //maybe instance move from journal to history
      }
      try {
        Thread.sleep(sleepTime);
      } catch (Exception e) {
        throw new BonitaRuntimeException(e);
      }
    }
    assertNotNull(processInstance);
  }

  protected void waitForChildToBeFinished(long maxWait, long sleepTime, ProcessInstanceUUID instanceUUID) throws BonitaException {
    long maxDate = System.currentTimeMillis() + maxWait;
    ProcessInstance processInstance = null;
    while (System.currentTimeMillis() < maxDate) {
      try {
        processInstance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);

        Set<ProcessInstanceUUID> childrenUUID = processInstance.getChildrenInstanceUUID();
        if (childrenUUID != null && childrenUUID.size() == 1) {
          final ProcessInstance childInstance = getQueryRuntimeAPI().getProcessInstance(childrenUUID.iterator().next());
          if (childInstance.getInstanceState().equals(InstanceState.FINISHED)) {
            break;
          }
        }
      } catch (Exception infe) {
        //maybe instance move from journal to history
      }
      try {
        Thread.sleep(sleepTime);
      } catch (Exception e) {
        throw new BonitaRuntimeException(e);
      }
      processInstance = null;
    }
  }

  protected void waitForTask(long maxWait, long sleepTime, ProcessInstanceUUID instanceUUID, String activityId)
  throws BonitaException {
    long maxDate = System.currentTimeMillis() + maxWait;
    global:while (System.currentTimeMillis() < maxDate) {
      final Collection<TaskInstance> taskActivities =
        getQueryRuntimeAPI().getTaskList(instanceUUID, ActivityState.READY);

      final Collection<ActivityInstanceUUID> tasksToDo = new ArrayList<ActivityInstanceUUID>();

      for (final TaskInstance task : taskActivities) {
        tasksToDo.add(task.getUUID());
        if (task.getProcessInstanceUUID().equals(instanceUUID)
            && task.getActivityName().equals(activityId)) {
          break global;
        }
      }
      try {
        Thread.sleep(sleepTime);
      } catch (Exception e) {
        throw new BonitaRuntimeException(e);
      }
    }
  }

  protected boolean causeContains(final String message, final Throwable e) {
    if (e == null) {
      return false;
    }
    if (e.getMessage() != null && e.getMessage().contains(message)) {
      return true;
    }
    if (e.getCause() != null) {
      return causeContains(message, e.getCause());
    }
    return false;
  }

  protected Map<String, byte[]> getResourcesFromConnector(Class<?>... clazz) throws IOException {
    Map<String, byte[]> resources = new HashMap<String, byte[]>();
    for (Class<?> class1 : clazz) {
      String resourceName = class1.getName().replace('.', '/') + ".xml";
      String resource = class1.getSimpleName() + ".xml";
      URL resourcePath = class1.getResource(resource);
      resources.put(resourceName, Misc.getAllContentFrom(resourcePath));
    }
    return resources;
  }

  /*
   * LOGIN
   */
  protected String getLogin() {
    return "admin";
  }
  protected String getPassword() {
    return "bpm";
  }
  protected void login() throws LoginException {
    this.loginContext = new LoginContext("BonitaAuth",
        new SimpleCallbackHandler(getLogin(), getPassword()));
    this.loginContext.login();
    this.loginContext.logout();

    this.loginContext = new LoginContext("BonitaStore",
        new SimpleCallbackHandler(getLogin(), getPassword()));
    this.loginContext.login();
  }

  protected BusinessArchive getBusinessArchive(final ProcessDefinition process) {
    return getBusinessArchive(process, null, (Class< ? >) null);
  }

  protected BusinessArchive getBusinessArchive(final ProcessDefinition process, final Map<String, byte[]> resources, final Class< ? >... classes) {
    try {
      return BusinessArchiveFactory.getBusinessArchive(process, resources, classes);
    } catch (final Exception e) {
      fail("Unable to build businessArchive: " + Misc.getStackTraceFrom(e));
      return null;
    }
  }

  protected BusinessArchive getBusinessArchiveFromXpdl(final URL xpdlURL) {
    return getBusinessArchiveFromXpdl(xpdlURL, (Class< ? >) null);
  }

  protected BusinessArchive getBusinessArchiveFromXpdl(final URL xpdlURL, final Class< ? >... classes) {
    return getBusinessArchiveFromXpdl(xpdlURL, null, classes);
  }

  protected BusinessArchive getBusinessArchiveFromXpdl(final URL xpdlURL, final Map<String, byte[]> resources, final Class< ? >... classes) {
    ProcessDefinition process = ProcessBuilder.createProcessFromXpdlFile(xpdlURL);
    try {
      return BusinessArchiveFactory.getBusinessArchive(process, resources, classes);
    } catch (final Exception e) {
      fail("Unable to build businessArchive: " + e.getMessage() + " "+ Misc.getStackTraceFrom(e));
      return null;
    }
  }

  protected Map<String, byte[]> getResources(String... resources) throws IOException {
    Map<String, byte[]> result = new HashMap<String, byte[]>();
    for (String jar : resources) {
      URL jarUrl = this.getClass().getResource(jar);
      result.put(jar, Misc.getAllContentFrom(jarUrl));
    }
    return result;
  }
  protected Set<byte[]> getClasses(final Class< ? >... classes) {
    try {
      return ClassDataTool.getClasses(classes);
    } catch (final Exception e) {
      fail("Unable to build businessArchive: " + Misc.getStackTraceFrom(e));
      return null;
    }
  }

  protected void loginAs(String loginName, String password) throws LoginException {
    loginContext.logout();
    try {
      loginContext = new LoginContext("BonitaAuth",
          new SimpleCallbackHandler(loginName, password));
      loginContext.login();
      loginContext.logout();

      loginContext = new LoginContext("BonitaStore", new SimpleCallbackHandler(loginName, password));
      loginContext.login();
    } catch (LoginException e) {
      throw new RuntimeException("Please, configure a JAAS test user with login: "
          + loginName + " and password: " + password, e);
    }
  }

  protected boolean isREST() {
    String apiType = System.getProperty(AccessorUtil.API_TYPE_PROPERTY);
    return (apiType != null && apiType.equalsIgnoreCase(Context.REST.toString()));
  }

  protected boolean executesTestsRemotely() {
    String apiType = System.getProperty(AccessorUtil.API_TYPE_PROPERTY);
    if (apiType == null) {
      apiType = Context.Standard.name();
    }
    Context context = Misc.stringToEnum(Context.class, apiType);
    return Context.EJB2.equals(context) || Context.EJB3.equals(context) || Context.REST.equals(context);
  }

  public LightProcessInstance getLightProcessInstance(Set<LightProcessInstance> instances, String processName, String version)
  throws ProcessNotFoundException {
    for (LightProcessInstance instance : instances) {
      ProcessDefinitionUUID definitionUUID = instance.getProcessDefinitionUUID();
      ProcessDefinition definition = getQueryDefinitionAPI().getProcess(definitionUUID);
      if (processName.equals(definition.getName()) && version.equals(definition.getVersion())) {
        return instance;
      }
    }
    return null;
  }

  public static LightActivityInstance getLightActivityInstance(Collection<LightActivityInstance> activities, String activityName) {
    for (LightActivityInstance activity : activities) {
      if (activityName.equals(activity.getActivityName())) {
        return activity;
      }
    }
    return null;
  }

  public static ActivityInstance getActivityInstance(Set<ActivityInstance> activities, String activityName) {
    for (ActivityInstance activity : activities) {
      if (activityName.equals(activity.getActivityName())) {
        return activity;
      }
    }
    return null;
  }

  public static ActivityDefinition getActivityDefinition(Set<ActivityDefinition> activities, String activityName) {
    for (ActivityDefinition activity : activities) {
      if (activityName.equals(activity.getName())) {
        return activity;
      }
    }
    return null;
  }

  public static TransitionDefinition getTransition(Set<TransitionDefinition> transitions, String tranistionName) {
    for (TransitionDefinition tranistion : transitions) {
      if (tranistionName.equals(tranistion.getName())) {
        return tranistion;
      }
    }
    return null;
  }

  public static BoundaryEvent getBoundaryEvent(List<BoundaryEvent> events, String eventName) {
    for (BoundaryEvent event : events) {
      if (eventName.equals(event.getName())) {
        return event;
      }
    }
    return null;
  }

  public static Category getCategory(Set<Category> categories, String categoryName) {
    for (Category category : categories) {
      if (categoryName.equals(category.getName())) {
        return category;
      }
    }
    return null;
  }
  
  protected void checkState(final ProcessInstanceUUID instanceUUID, ActivityState activityState, final String... activityIds)
  throws InstanceNotFoundException, ActivityNotFoundException {
    for (final String activityId : activityIds) {
      final Set<ActivityInstance> activityInsts =
        getQueryRuntimeAPI().getActivityInstances(instanceUUID, activityId);
      assertEquals("Activity " + activityId + " executed more than once.", 1, activityInsts.size());
      final ActivityInstance activityInst = activityInsts.iterator().next();
      assertEquals(activityState, activityInst.getState());
    }
  }
  
  protected void checkState(final ProcessInstanceUUID instanceUUID, ActivityState activityState, final String activityId, final int nb)
  throws InstanceNotFoundException, ActivityNotFoundException {
      final Set<ActivityInstance> activityInsts =
        getQueryRuntimeAPI().getActivityInstances(instanceUUID, activityId);
      assertEquals("Activity " + activityId, nb, activityInsts.size());
      for (final ActivityInstance activityInstance : activityInsts) {
        assertEquals(activityState, activityInstance.getState());
      }
  }
  
  protected void checkActivityInstanceNotExist(final ProcessInstanceUUID instanceUUID, final String... activityIds)
  throws InstanceNotFoundException {
    for (final String activityId : activityIds) {
        try {
          getQueryRuntimeAPI().getActivityInstances(instanceUUID, activityId);
          fail("This activity has already created instances");
        } catch (ActivityNotFoundException e) {
          //OK
        }
    }
  }
  
  protected void checkState(final ProcessInstanceUUID instanceUUID, InstanceState instanceState)
  throws InstanceNotFoundException, ActivityNotFoundException {
    final LightProcessInstance instance = getQueryRuntimeAPI().getLightProcessInstance(instanceUUID);
    assertEquals(instanceState, instance.getInstanceState());
  }
}
