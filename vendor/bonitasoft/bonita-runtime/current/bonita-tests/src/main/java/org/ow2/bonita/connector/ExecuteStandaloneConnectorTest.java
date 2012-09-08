package org.ow2.bonita.connector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.bonitasoft.connectors.bonita.SetVarConnector;
import org.bonitasoft.connectors.bonita.filters.RandomMultipleFilter;
import org.bonitasoft.connectors.bonita.filters.UniqueRandomFilter;
import org.bonitasoft.connectors.bonita.resolvers.UserListRoleResolver;
import org.bonitasoft.connectors.scripting.GroovyConnector;
import org.ow2.bonita.APITestCase;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.exception.BonitaInternalException;
import org.ow2.bonita.facade.runtime.ActivityState;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.util.BonitaRuntimeException;
import org.ow2.bonita.util.Misc;
import org.ow2.bonita.util.ProcessBuilder;

public class ExecuteStandaloneConnectorTest extends APITestCase {

  public void testExecuteFilterWithNullParameters() throws Exception {
    byte[] jar = Misc.generateJar(UniqueRandomFilter.class);
    getManagementAPI().deployJar("connect.jar", jar);
    Set<String> members = new HashSet<String>();
    members.add("john");
    members.add("jack");
    members.add("jane");
    members.add("joe");
    Set<String> candidates = getRuntimeAPI().executeFilter(UniqueRandomFilter.class.getName(), null, members);
    assertEquals(1, candidates.size());
    String candidate = candidates.iterator().next();
    assertTrue(members.contains(candidate));
    
    getManagementAPI().removeJar("connect.jar");
  }

  public void testExecuteFilterWithNullMembers() throws Exception {
    byte[] jar = Misc.generateJar(UniqueRandomFilter.class);
    getManagementAPI().deployJar("connect.jar", jar);
    try {
      getRuntimeAPI().executeFilter(UniqueRandomFilter.class.getName(), null, null);
      fail("Members cannot be null");
    }  catch (IllegalArgumentException e) {
    } finally {
      getManagementAPI().removeJar("connect.jar");
    }
  }

  public void testExecuteFilterWithBadParameters() throws Exception {
    byte[] jar = Misc.generateJar(RandomMultipleFilter.class);
    getManagementAPI().deployJar("connect.jar", jar);
    
    Set<String> members = new HashSet<String>();
    members.add("john");
    members.add("jack");
    members.add("jane");
    members.add("joe");

    Map<String, Object[]> parameters = new HashMap<String, Object[]>();
    parameters.put("setMemberNumber", new Object[] {2});

    try {
      getRuntimeAPI().executeFilter(RandomMultipleFilter.class.getName(), parameters, members);
      fail("setMemberNumber is not a method of " + RandomMultipleFilter.class.getName());
    } catch (BonitaRuntimeException e) {
    }  finally {
      getManagementAPI().removeJar("connect.jar");
    }
  }

  public void testExecuteFilterWithParameters() throws Exception {
    byte[] jar = Misc.generateJar(RandomMultipleFilter.class);
    getManagementAPI().deployJar("connect.jar", jar);

    Set<String> members = new HashSet<String>();
    members.add("john");
    members.add("jack");
    members.add("jane");
    members.add("joe");

    Map<String, Object[]> parameters = new HashMap<String, Object[]>();
    parameters.put("candidateNumber", new Object[] {2});

    Set<String> candidates = getRuntimeAPI().executeFilter(RandomMultipleFilter.class.getName(), parameters, members);
    assertEquals(2, candidates.size());
    Iterator<String> iterCandidates = candidates.iterator();
    String candidate1 = iterCandidates.next();
    assertTrue(members.contains(candidate1));
    String candidate2 = iterCandidates.next();
    assertTrue(members.contains(candidate2));
    assertNotSame(candidate1, candidate2);
    
    getManagementAPI().removeJar("connect.jar");
  }

  public void testExecuteRoleResolver() throws Exception {
    byte[] jar = Misc.generateJar(UserListRoleResolver.class);
    getManagementAPI().deployJar("connect.jar", jar);
    
    Map<String, Object[]> parameters = new HashMap<String, Object[]>();
    parameters.put("setUsers", new Object[] {"${def users = ''; ['john', 'jack', 'joe'].each ({item -> users += item +  \", \"}); users += \"jane\" }"});

    Set<String> members = getRuntimeAPI().executeRoleResolver(UserListRoleResolver.class.getName(), parameters);
    assertTrue(members.contains("john"));
    assertTrue(members.contains("jack"));
    assertTrue(members.contains("joe"));
    assertTrue(members.contains("jane"));
    
    getManagementAPI().removeJar("connect.jar");
  }

  public void testDoNotUseExecute() throws Exception {
    byte[] jar = Misc.generateJar(UserListRoleResolver.class);
    getManagementAPI().deployJar("connect.jar", jar);
    String name = UserListRoleResolver.class.getName();
    try {
      getRuntimeAPI().executeConnector(name, null);
      fail(name + "is a RoleResolver not a simple connector");
    } catch (BonitaRuntimeException e) {
    } finally {
      getManagementAPI().removeJar("connect.jar");
    }
  }

  public void testExecuteConnectorNullParameters() throws Exception {
    byte[] jar = Misc.generateJar(SetVarConnector.class);
    getManagementAPI().deployJar("connect.jar", jar);
    try {
      getRuntimeAPI().executeConnector(SetVarConnector.class.getName(), null);
      fail();
    } catch (BonitaInternalException e) {
    }   finally {
      getManagementAPI().removeJar("connect.jar");
    }
  }

  public void testExecuteConnectorEmptyParameters() throws Exception {
    byte[] jar = Misc.generateJar(SetVarConnector.class);
    getManagementAPI().deployJar("connect.jar", jar);
    Map<String, Object[]> parameters = new HashMap<String, Object[]>();
    try {
      getRuntimeAPI().executeConnector(SetVarConnector.class.getName(), parameters);
      fail("Parameters of SetVar Connector are missing. So an exception should be thorwn");
    } catch (BonitaInternalException e) {
      e.printStackTrace();
    }  finally {
      getManagementAPI().removeJar("connect.jar");
    }
  }

  public void testExecuteGroovyConnector() throws Exception {
    byte[] jar = Misc.generateJar(GroovyConnector.class);
    getManagementAPI().deployJar("connect.jar", jar);
    Map<String, Object[]> parameters = new HashMap<String, Object[]>();
    parameters.put("setThing", null);
    try {
      getRuntimeAPI().executeConnector(GroovyConnector.class.getName(), parameters);
      fail();
    } catch (BonitaInternalException e) {
    }   finally {
      getManagementAPI().removeJar("connect.jar");
    }
  }

  public void testExecuteConnector() throws Exception {
    byte[] jar = Misc.generateJar(GroovyConnector.class);
    getManagementAPI().deployJar("connect.jar", jar);
    Map<String, Object[]> parameters = new HashMap<String, Object[]>();
    parameters.put("setScript", null);
    try {
      getRuntimeAPI().executeConnector(GroovyConnector.class.getName(), parameters);
      fail("Script cannot be null");
    } catch (BonitaInternalException e) {
    } finally {
      getManagementAPI().removeJar("connect.jar");
    }
  }

  public void testExecuteGoodGroovyConnector() throws Exception {
    byte[] jar = Misc.generateJar(GroovyConnector.class);
    getManagementAPI().deployJar("connect.jar", jar);
    Map<String, Object[]> parameters = new HashMap<String, Object[]>();
    parameters.put("setScript", new Object[] {"45"});
    Map<String, Object> actual = getRuntimeAPI().executeConnector(GroovyConnector.class.getName(), parameters);
    Map<String, Object> expected = new HashMap<String, Object>();
    expected.put("result", 45);
    assertEquals(expected, actual);
    getManagementAPI().removeJar("connect.jar");
  }

  public void testExecuteGoodGroovyConnector2() throws Exception {
    byte[] jar = Misc.generateJar(GroovyConnector.class);
    getManagementAPI().deployJar("connect.jar", jar);
    Map<String, Object[]> parameters = new HashMap<String, Object[]>();
    parameters.put("script", new Object[] {"45"});
    Map<String, Object> actual = getRuntimeAPI().executeConnector(GroovyConnector.class.getName(), parameters);
    Map<String, Object> expected = new HashMap<String, Object>();
    expected.put("result", 45);
    assertEquals(expected, actual);
    getManagementAPI().removeJar("connect.jar");
  }

  public void testExecuteGroovyConnectorWithAProcessDefinition() throws Exception {
    Class<GroovyConnector> connectorClass = GroovyConnector.class;
    
    ProcessDefinition definition =
      ProcessBuilder.createProcess("kiitettävä", "1.0")
      .addHuman(getLogin())
      .addHumanTask("yksi", getLogin())
      .done();
    definition = getManagementAPI().deploy(getBusinessArchive(definition, getResourcesFromConnector(connectorClass), connectorClass));

    Map<String, Object[]> parameters = new HashMap<String, Object[]>();
    parameters.put("script", new Object[] {"45"});
    Map<String, Object> actual = getRuntimeAPI().executeConnector(connectorClass.getName(), parameters, definition.getUUID());
    Map<String, Object> expected = new HashMap<String, Object>();
    expected.put("result", 45);
    assertEquals(expected, actual);

    getManagementAPI().deleteProcess(definition.getUUID());
  }
  
  public void testExecuteGroovyConnectorWithAProcessDefinitionAndAContext() throws Exception {
    Class<GroovyConnector> connectorClass = GroovyConnector.class;
    
    ProcessDefinition definition =
      ProcessBuilder.createProcess("kiitettävä", "1.0")
      .addHuman(getLogin())
      .addHumanTask("yksi", getLogin())
      .done();
    definition = getManagementAPI().deploy(getBusinessArchive(definition, getResourcesFromConnector(connectorClass), connectorClass));

    Map<String, Object> context = new HashMap<String, Object>();
    context.put("field1", "5 + ");
    
    Map<String, Object[]> parameters = new HashMap<String, Object[]>();
    parameters.put("script", new Object[] {"${field1 + \"45\"}"});
    Map<String, Object> actual = getRuntimeAPI().executeConnector(connectorClass.getName(), parameters, definition.getUUID(), context);
    Map<String, Object> expected = new HashMap<String, Object>();
    expected.put("result", 50);
    assertEquals(expected, actual);

    getManagementAPI().deleteProcess(definition.getUUID());
  }

  public void testExecuteRoleResolverWithAProcessDefinition() throws Exception {
    Class<UserListRoleResolver> connectorClass = UserListRoleResolver.class;
    ProcessDefinition definition =
      ProcessBuilder.createProcess("kiitettävä", "1.0")
      .addHuman(getLogin())
      .addHumanTask("yksi", getLogin())
      .done();
    definition = getManagementAPI().deploy(getBusinessArchive(
        definition, getResourcesFromConnector(connectorClass), connectorClass));

    Map<String, Object[]> parameters = new HashMap<String, Object[]>();
    parameters.put("setUsers", new Object[] {
        "${def users = ''; ['john', 'jack', 'joe'].each ({item -> users += item +  \", \"}); users += \"jane\" }"});

    Set<String> members = getRuntimeAPI().executeRoleResolver(connectorClass.getName(), parameters, definition.getUUID());
    assertTrue(members.contains("john"));
    assertTrue(members.contains("jack"));
    assertTrue(members.contains("joe"));
    assertTrue(members.contains("jane"));

    getManagementAPI().deleteProcess(definition.getUUID());
  }

  public void testExecuteFilterWithAProcessDefinition() throws Exception {
    Class<UniqueRandomFilter> connectorClass = UniqueRandomFilter.class;
    ProcessDefinition definition =
      ProcessBuilder.createProcess("kiitettävä", "1.0")
      .addHuman(getLogin())
      .addHumanTask("yksi", getLogin())
      .done();
    definition = getManagementAPI().deploy(getBusinessArchive(
        definition, getResourcesFromConnector(connectorClass), connectorClass));

    Set<String> members = new HashSet<String>();
    members.add("john");
    members.add("jack");
    members.add("jane");
    members.add("joe");
    Set<String> candidates = getRuntimeAPI().executeFilter(connectorClass.getName(), null, members, definition.getUUID());
    assertEquals(1, candidates.size());
    String candidate = candidates.iterator().next();
    assertTrue(members.contains(candidate));
    
    getManagementAPI().deleteProcess(definition.getUUID());
  }

  public void testExecuteGroovyConnectorWithAProcessInstance() throws Exception {
    Class<GroovyConnector> connectorClass = GroovyConnector.class;
    
    ProcessDefinition definition =
      ProcessBuilder.createProcess("simpleProcess", "1.0")
      .addIntegerData("processVar", 10)
      .addHuman(getLogin())
      .addHumanTask("task1", getLogin())
      .addHumanTask("task2", getLogin())
      .addTransition("task1", "task2")
      .done();
    definition = getManagementAPI().deploy(getBusinessArchive(definition, getResourcesFromConnector(connectorClass), connectorClass));

    ProcessInstanceUUID processInstanceUUID = getRuntimeAPI().instantiateProcess(definition.getUUID());
    ActivityInstanceUUID activityInstanceUUID = getQueryRuntimeAPI().getOneTask(ActivityState.READY);
    getRuntimeAPI().executeTask(activityInstanceUUID, true);
    getRuntimeAPI().setProcessInstanceVariable(processInstanceUUID, "processVar", 20);
    
    Map<String, Object> context = new HashMap<String, Object>();
    context.put("field1", "5 + ");
    
    Map<String, Object[]> parameters = new HashMap<String, Object[]>();
    parameters.put("script", new Object[] {"${field1 + \"45 + \" + processVar}"});
    Map<String, Object> actual = getRuntimeAPI().executeConnector(connectorClass.getName(), parameters, processInstanceUUID, context, false);
    Map<String, Object> expected = new HashMap<String, Object>();
    expected.put("result", 60);
    assertEquals(expected, actual);

    getManagementAPI().deleteProcess(definition.getUUID());
  }
  
  public void testExecuteGroovyConnectorWithAProcessInstanceUsingCurrentProcessValues() throws Exception {
    Class<GroovyConnector> connectorClass = GroovyConnector.class;
    
    ProcessDefinition definition =
      ProcessBuilder.createProcess("simpleProcess", "1.0")
      .addIntegerData("processVar", 10)
      .addHuman(getLogin())
      .addHumanTask("task1", getLogin())
      .addHumanTask("task2", getLogin())
      .addTransition("task1", "task2")
      .done();
    definition = getManagementAPI().deploy(getBusinessArchive(definition, getResourcesFromConnector(connectorClass), connectorClass));

    ProcessInstanceUUID processInstanceUUID = getRuntimeAPI().instantiateProcess(definition.getUUID());
    ActivityInstanceUUID activityInstanceUUID = getQueryRuntimeAPI().getOneTask(ActivityState.READY);
    getRuntimeAPI().executeTask(activityInstanceUUID, true);
    getRuntimeAPI().setProcessInstanceVariable(processInstanceUUID, "processVar", 20);
    
    Map<String, Object> context = new HashMap<String, Object>();
    context.put("field1", "5 + ");
    
    Map<String, Object[]> parameters = new HashMap<String, Object[]>();
    parameters.put("script", new Object[] {"${field1 + \"45 + \" + processVar}"});
    Map<String, Object> actual = getRuntimeAPI().executeConnector(connectorClass.getName(), parameters, processInstanceUUID, context, true);
    Map<String, Object> expected = new HashMap<String, Object>();
    expected.put("result", 70);
    assertEquals(expected, actual);

    getManagementAPI().deleteProcess(definition.getUUID());
  }
  
  public void testExecuteGroovyConnectorWithAnActivityInstance() throws Exception {
    Class<GroovyConnector> connectorClass = GroovyConnector.class;
    
    ProcessDefinition definition =
      ProcessBuilder.createProcess("simpleProcess", "1.0")
      .addIntegerData("processVar", 10)
      .addHuman(getLogin())
      .addHumanTask("task1", getLogin())
      .addIntegerData("activityVar", 10)
      .addHumanTask("task2", getLogin())
      .addTransition("task1", "task2")
      .done();
    definition = getManagementAPI().deploy(getBusinessArchive(definition, getResourcesFromConnector(connectorClass), connectorClass));

    ProcessInstanceUUID processInstanceUUID = getRuntimeAPI().instantiateProcess(definition.getUUID());
    ActivityInstanceUUID activityInstanceUUID = getQueryRuntimeAPI().getOneTask(ActivityState.READY);
    getRuntimeAPI().setActivityInstanceVariable(activityInstanceUUID, "activityVar", 20);
    getRuntimeAPI().executeTask(activityInstanceUUID, true);
    getRuntimeAPI().setProcessInstanceVariable(processInstanceUUID, "processVar", 20);
    
    Map<String, Object> context = new HashMap<String, Object>();
    context.put("field1", "5 + ");
    
    Map<String, Object[]> parameters = new HashMap<String, Object[]>();
    parameters.put("script", new Object[] {"${field1 + \"45 + \" + processVar + \" + \" + activityVar}"});
    Map<String, Object> actual = getRuntimeAPI().executeConnector(connectorClass.getName(), parameters, activityInstanceUUID, context, false);
    Map<String, Object> expected = new HashMap<String, Object>();
    expected.put("result", 80);
    assertEquals(expected, actual);

    getManagementAPI().deleteProcess(definition.getUUID());
  }
  
  public void testExecuteGroovyConnectorWithAnActivityInstanceUsingCurrentProcessValues() throws Exception {
    Class<GroovyConnector> connectorClass = GroovyConnector.class;
    
    ProcessDefinition definition =
      ProcessBuilder.createProcess("simpleProcess", "1.0")
      .addIntegerData("processVar", 10)
      .addHuman(getLogin())
      .addHumanTask("task1", getLogin())
      .addIntegerData("activityVar", 10)
      .addHumanTask("task2", getLogin())
      .addTransition("task1", "task2")
      .done();
    definition = getManagementAPI().deploy(getBusinessArchive(definition, getResourcesFromConnector(connectorClass), connectorClass));

    ProcessInstanceUUID processInstanceUUID = getRuntimeAPI().instantiateProcess(definition.getUUID());
    ActivityInstanceUUID activityInstanceUUID = getQueryRuntimeAPI().getOneTask(ActivityState.READY);
    getRuntimeAPI().setActivityInstanceVariable(activityInstanceUUID, "activityVar", 20);
    getRuntimeAPI().executeTask(activityInstanceUUID, true);
    getRuntimeAPI().setProcessInstanceVariable(processInstanceUUID, "processVar", 20);
    
    Map<String, Object> context = new HashMap<String, Object>();
    context.put("field1", "5 + ");
    
    Map<String, Object[]> parameters = new HashMap<String, Object[]>();
    parameters.put("script", new Object[] {"${field1 + \"45 + \" + processVar + \" + \" + activityVar}"});
    Map<String, Object> actual = getRuntimeAPI().executeConnector(connectorClass.getName(), parameters, activityInstanceUUID, context, true);
    Map<String, Object> expected = new HashMap<String, Object>();
    expected.put("result", 90);
    assertEquals(expected, actual);

    getManagementAPI().deleteProcess(definition.getUUID());
  }
}
