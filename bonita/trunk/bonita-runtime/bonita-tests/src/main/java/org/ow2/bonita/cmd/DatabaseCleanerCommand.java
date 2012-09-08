package org.ow2.bonita.cmd;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;
import org.ow2.bonita.env.Environment;
import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.ManagementAPI;
import org.ow2.bonita.facade.QueryDefinitionAPI;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.RuntimeAPI;
import org.ow2.bonita.facade.WebAPI;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.exception.DeploymentException;
import org.ow2.bonita.facade.exception.InstanceNotFoundException;
import org.ow2.bonita.facade.exception.ProcessNotFoundException;
import org.ow2.bonita.facade.exception.RuleNotFoundException;
import org.ow2.bonita.facade.exception.UndeletableInstanceException;
import org.ow2.bonita.facade.exception.UndeletableProcessException;
import org.ow2.bonita.facade.impl.StandardAPIAccessorImpl;
import org.ow2.bonita.facade.privilege.Rule;
import org.ow2.bonita.facade.runtime.Category;
import org.ow2.bonita.facade.runtime.ProcessInstance;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.runtime.event.EventExecutor;
import org.ow2.bonita.runtime.event.EventInstance;
import org.ow2.bonita.runtime.event.IncomingEventInstance;
import org.ow2.bonita.runtime.event.OutgoingEventInstance;
import org.ow2.bonita.runtime.model.Execution;
import org.ow2.bonita.services.DocumentationManager;
import org.ow2.bonita.services.EventService;
import org.ow2.bonita.services.LargeDataRepository;
import org.ow2.bonita.util.BonitaConstants;
import org.ow2.bonita.util.Command;
import org.ow2.bonita.util.EnvTool;
import org.ow2.bonita.util.Misc;

public class DatabaseCleanerCommand implements Command<String> {

  private static final long serialVersionUID = -7646602102047126698L;
  private boolean testFailure;
  private QueryDefinitionAPI queryDefinitionAPI;
  private QueryRuntimeAPI queryRuntimeAPI;
  private ManagementAPI managementAPI;
  private RuntimeAPI runtimeAPI;
  private WebAPI webAPI;

  private StringBuilder message;

  public DatabaseCleanerCommand(boolean testFailure) {
    this.testFailure = testFailure;
  }

  public String execute(Environment environment) throws Exception {
    APIAccessor accessor = new StandardAPIAccessorImpl();
    queryDefinitionAPI = accessor.getQueryDefinitionAPI();
    queryRuntimeAPI = accessor.getQueryRuntimeAPI();
    managementAPI = accessor.getManagementAPI();
    runtimeAPI = accessor.getRuntimeAPI();
    webAPI = accessor.getWebAPI();
    message = new StringBuilder(BonitaConstants.DEFAULT_DOMAIN);
    final EventExecutor eventExecutor = EnvTool.getEventExecutor();
    if (eventExecutor != null && eventExecutor.isActive()) {
      eventExecutor.stop();
    }
    return checkEverythingCleaned(!testFailure, environment);
  }

  private String checkEverythingCleaned(final boolean failIfNotClean, Environment environment) throws Exception {
    try {
      final DocumentationManager manager = EnvTool.getDocumentationManager();
      manager.clear();
    } catch (Exception e) {
      // connection refused
    }

    cleanProcessInstances();
    cleanProcessDefinitions();
    cleanAvailableJars();
    cleanActiveProcessClassLoaders();
    cleanClassDataLoader();
    cleanExecutions(environment);
    cleanIncomingEvents();
    cleanOverdueEvents();
    cleanOutgoingEvents();
    clearProcessDefinitions();
    cleanLargeDataRepository();
    cleanRules();
    cleanCategories();
    return message.toString();
  }

  private void clearProcessDefinitions() throws ProcessNotFoundException, UndeletableProcessException, UndeletableInstanceException {
    final Set<ProcessDefinition> processes = queryDefinitionAPI.getProcesses();
    if (processes != null && !processes.isEmpty()) {
      final Set<String> processIds = new HashSet<String>();
      for (final ProcessDefinition processDef : processes) {
        processIds.add(processDef.getName());
        managementAPI.deleteProcess(processDef.getUUID());
      }
      message.append("Some processes are still found. You must write a test which cleans everything. Process ids = ")
      .append(processIds);
    }
    
  }

  private void cleanCategories() {
    Set<Category> categories = webAPI.getAllCategories();
    if (categories != null && !categories.isEmpty()) {
      Set<String> categoriesNames = new HashSet<String>();
      for (Category cat : categories) {
        categoriesNames.add(cat.getName());
      }
      webAPI.deleteCategories(categoriesNames);
      message.append("\nSome categories are still found. You must write a test which cleans everything. Categories names= ")
      .append(categoriesNames);
    }
  }

  private void cleanRules() throws RuleNotFoundException {
    List<Rule> rules = managementAPI.getAllRules();
    if (rules != null && !rules.isEmpty()) {
      Set<String> ruleUUIDs = new HashSet<String>();
      for (Rule rule : rules) {
        ruleUUIDs.add(rule.getUUID());
        managementAPI.deleteRuleByUUID(rule.getUUID());
      }
      message.append("\nSome rules are still found. You must write a test which cleans everything. Rule names= ").append(ruleUUIDs);
    }
  }

  private void cleanLargeDataRepository() throws Exception {
    LargeDataRepository ldr = EnvTool.getLargeDataRepository();
    final Set<String> keys = ldr.getKeys();
    keys.remove("database.jar");
    if (!keys.isEmpty()) {
      if (!Misc.isOnWindows()) {
        message.append("Some data are still found in Large Data Repository. You must write a test which cleans everything.\n");
        for (String key : keys) {
          message.append(key).append("\n");
        }
      }
    }
    ldr.clean();
  }

  private void cleanOutgoingEvents() {
    final EventService eventService = EnvTool.getEventService();
    Set<String> outgoings = new HashSet<String>();
    Set<OutgoingEventInstance> outgoingInstances = eventService.getOutgoingEvents();
    for (OutgoingEventInstance outgoing : outgoingInstances) {
      outgoings.add(outgoing.toString());
      eventService.removeEvent(outgoing);
    }

    if (outgoings.size() > 0) {
      message.append("There are still OutgoingEventInstances in db. Please remove them: ")
      .append(outgoings);
    }
  }

  private void cleanOverdueEvents() {
    final EventService eventService = EnvTool.getEventService();
    for (EventInstance event : eventService.getOverdueEvents()) {
      eventService.removeEvent(event);
    }
  }

  private void cleanIncomingEvents() {
    final EventService eventService = EnvTool.getEventService();
    Set<String> incomings = new HashSet<String>();
    Set<IncomingEventInstance> incomingInstances = eventService.getIncomingEvents();
    for (IncomingEventInstance incoming : incomingInstances) {
      incomings.add(incoming.toString());
      eventService.removeEvent(incoming);
    }
    if (incomings.size() > 0) {
      message.append("There are still IncomingEventInstances in db. Please remove them: ")
      .append(incomings);
    }
  }

  private void cleanActiveProcessClassLoaders() {
    final Set<ProcessDefinitionUUID> processClassLoaders = EnvTool.getClassDataLoader().getActiveProcessClassLoaders();
    if (processClassLoaders != null && !processClassLoaders.isEmpty()) {
      message.append("Some process class loaders are still deployed. You must write a test which cleans everything. Please, remove them: ")
      .append(processClassLoaders);
    }
  }

  private void cleanAvailableJars() throws DeploymentException {
    //check no jar is available 
    Set<String> jars = managementAPI.getAvailableJars();
    jars.remove("database.jar");
    if (!jars.isEmpty()) {
      for (String jar : jars) {
        managementAPI.removeJar(jar);
      }
      message.append("Some jar files are still deployed. Please remove them: ").append(jars);
    }
  }

  private void cleanProcessDefinitions() throws ProcessNotFoundException, UndeletableProcessException, UndeletableInstanceException {
    final Set<ProcessDefinition> remainingProcesses = queryDefinitionAPI.getProcesses();
    if (remainingProcesses != null && !remainingProcesses.isEmpty()) {
      final Set<String> packageIds = new HashSet<String>();
      for (final ProcessDefinition processDef : remainingProcesses) {
        packageIds.add(processDef.getName());
      }
      message.append("Some processes are still found. You must write a test which cleans everything. Processes ids = ")
      .append(packageIds);

      // handle dependencies when removing processes.
      Set<ProcessDefinition> processes = queryDefinitionAPI.getProcesses();
      while (processes != null && !processes.isEmpty()) {
        // create list of all available processes
        final Set<ProcessDefinitionUUID> loopProcessIds = new HashSet<ProcessDefinitionUUID>();
        for (final ProcessDefinition processDef : processes) {
          loopProcessIds.add(processDef.getUUID());
        }
        // remove from list package that are dependencies of other processes
        for (final ProcessDefinition processDef : processes) {
          for (final String dep : processDef.getProcessDependencies()) {
            Set<ProcessDefinition> depProcesses = queryDefinitionAPI.getProcesses(dep);
            loopProcessIds.removeAll(depProcesses);
          }
        }
        // remove processes which do not have dependencies
        for (final ProcessDefinitionUUID processUUID : loopProcessIds) {
          managementAPI.deleteProcess(processUUID);
        }
        // do the same with remaining processes
        processes = queryDefinitionAPI.getProcesses();
      }
    }
  }

  private void cleanProcessInstances() throws InstanceNotFoundException, UndeletableInstanceException {
    final Set<ProcessInstance> instances = queryRuntimeAPI.getProcessInstances();
    if (instances != null && !instances.isEmpty()) {
      final Set<String> instanceUUIDs = new HashSet<String>();
      for (final ProcessInstance processInst : instances) {
        instanceUUIDs.add(processInst.getUUID().toString());
        if (processInst.getParentInstanceUUID() == null) {
          runtimeAPI.deleteProcessInstance(processInst.getUUID());
        }
      }
      message.append("Some instances are still found. You must write a test which cleans everything. Instance UUIDs = ")
      .append(instanceUUIDs);
    }
  }

  private void cleanClassDataLoader() {
    EnvTool.getClassDataLoader().clear();
  }

  @SuppressWarnings("unchecked")
  private void cleanExecutions(Environment environment) {
    Session session = (Session) environment.get("hibernate-session:core");
    Query query = session.createQuery("SELECT exec FROM org.ow2.bonita.runtime.model.Execution AS exec");
    List<Execution> executions = query.list();
    if (executions != null && executions.size() > 0) {
      message.append("Some executions are still found in the journal: \n");
      for (Execution exec : executions) {
        message.append(exec).append("\n");
      }
    }
  }

}
