package org.ow2.bonita.search;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ow2.bonita.APITestCase;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.runtime.impl.CaseImpl;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.search.index.CaseIndex;
import org.ow2.bonita.util.ProcessBuilder;

public class SearchCaseTest extends APITestCase {

  @Override
  protected void tearDown() throws Exception {
    getWebAPI().deletePhantomCases();
    getWebAPI().deleteAllCases();
    super.tearDown();
  }

  public void testSearchCaseUsingLabel() throws Exception {
    ProcessDefinition yksi = ProcessBuilder.createProcess("yksi","1.0")
    .addHuman(getLogin())
    .addHumanTask("step1", getLogin())
    .done();

    getManagementAPI().deploy(getBusinessArchive(yksi));
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(yksi.getUUID());

    String label = "starred";
    Set<ProcessInstanceUUID> instanceUUIDs = new HashSet<ProcessInstanceUUID>();
    instanceUUIDs.add(instanceUUID);

    getWebAPI().addLabel(label, getLogin(), null, null, null, true, true, null, null, 0, true);
    getWebAPI().addCasesToLabel(getLogin(), label, instanceUUIDs);

    SearchQueryBuilder query = new SearchQueryBuilder(new CaseIndex());
    query.criterion(CaseIndex.LABEL_NAME).equalsTo(label);
    List<CaseImpl> cases = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(1, cases.size());

    getManagementAPI().deleteProcess(yksi.getUUID());
  }

  public void testSearchCaseUsingOwner() throws Exception {
    ProcessDefinition yksi = ProcessBuilder.createProcess("yksi","1.0")
    .addHuman(getLogin())
    .addHumanTask("step1", getLogin())
    .done();

    getManagementAPI().deploy(getBusinessArchive(yksi));
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(yksi.getUUID());

    String label = "starred";
    Set<ProcessInstanceUUID> instanceUUIDs = new HashSet<ProcessInstanceUUID>();
    instanceUUIDs.add(instanceUUID);

    getWebAPI().addLabel(label, getLogin(), null, null, null, true, true, null, null, 0, true);
    getWebAPI().addCasesToLabel(getLogin(), label, instanceUUIDs);

    SearchQueryBuilder query = new SearchQueryBuilder(new CaseIndex());
    query.criterion(CaseIndex.OWNER_NAME).equalsTo(getLogin());
    List<CaseImpl> cases = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(1, cases.size());

    getManagementAPI().deleteProcess(yksi.getUUID());
  }

  public void testSearchCaseUsingInstanceUUID() throws Exception {
    ProcessDefinition yksi = ProcessBuilder.createProcess("yksi","1.0")
    .addHuman(getLogin())
    .addHumanTask("step1", getLogin())
    .done();

    getManagementAPI().deploy(getBusinessArchive(yksi));
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(yksi.getUUID());

    String label = "starred";
    Set<ProcessInstanceUUID> instanceUUIDs = new HashSet<ProcessInstanceUUID>();
    instanceUUIDs.add(instanceUUID);

    getWebAPI().addLabel(label, getLogin(), null, null, null, true, true, null, null, 0, true);
    getWebAPI().addCasesToLabel(getLogin(), label, instanceUUIDs);

    SearchQueryBuilder query = new SearchQueryBuilder(new CaseIndex());
    query.criterion(CaseIndex.PROCESS_INSTANCE_UUID).equalsTo(instanceUUID.getValue());
    List<CaseImpl> cases = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(1, cases.size());

    getWebAPI().deleteAllCases();
    getManagementAPI().deleteProcess(yksi.getUUID());
  }

  public void testSearchCaseUsingLabels() throws Exception {
    ProcessDefinition yksi = ProcessBuilder.createProcess("yksi","1.0")
    .addHuman(getLogin())
    .addHumanTask("step1", getLogin())
    .done();

    ProcessDefinition kaksi = ProcessBuilder.createProcess("kaksi","1.0")
    .addHuman(getLogin())
    .addHumanTask("step1", getLogin())
    .done();

    getManagementAPI().deploy(getBusinessArchive(yksi));
    ProcessInstanceUUID yksiUUID = getRuntimeAPI().instantiateProcess(yksi.getUUID());
    getManagementAPI().deploy(getBusinessArchive(kaksi));
    ProcessInstanceUUID kaksiUUID = getRuntimeAPI().instantiateProcess(kaksi.getUUID());

    String starred = "starred";
    String inbox = "inbox";
    Set<ProcessInstanceUUID> instanceUUIDs = new HashSet<ProcessInstanceUUID>();
    instanceUUIDs.add(yksiUUID);

    getWebAPI().addLabel(starred, getLogin(), null, null, null, true, true, null, null, 0, true);
    getWebAPI().addCasesToLabel(getLogin(), starred, instanceUUIDs);

    instanceUUIDs = new HashSet<ProcessInstanceUUID>();
    instanceUUIDs.add(yksiUUID);
    instanceUUIDs.add(kaksiUUID);
    getWebAPI().addLabel(inbox, getLogin(), null, null, null, true, true, null, null, 0, true);
    getWebAPI().addCasesToLabel(getLogin(), inbox, instanceUUIDs);

    SearchQueryBuilder query = new SearchQueryBuilder(new CaseIndex());
    query.criterion(CaseIndex.LABEL_NAME).equalsTo(inbox);
    List<CaseImpl> cases = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(2, cases.size());

    query = new SearchQueryBuilder(new CaseIndex());
    query.criterion(CaseIndex.LABEL_NAME).equalsTo(starred);
    cases = getQueryRuntimeAPI().search(query, 0, 10);
    assertEquals(1, cases.size());

    getManagementAPI().deleteProcess(yksi.getUUID());
    getManagementAPI().deleteProcess(kaksi.getUUID());
  }

}
