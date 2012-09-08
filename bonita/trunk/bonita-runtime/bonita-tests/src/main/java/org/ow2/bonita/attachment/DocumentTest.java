package org.ow2.bonita.attachment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ow2.bonita.APITestCase;
import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.def.element.BusinessArchive;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.exception.DocumentAlreadyExistsException;
import org.ow2.bonita.facade.impl.StandardAPIAccessorImpl;
import org.ow2.bonita.facade.runtime.AttachmentInstance;
import org.ow2.bonita.facade.runtime.Document;
import org.ow2.bonita.facade.runtime.ProcessInstance;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.search.DocumentResult;
import org.ow2.bonita.search.DocumentSearchBuilder;
import org.ow2.bonita.search.index.DocumentIndex;
import org.ow2.bonita.util.BusinessArchiveFactory;
import org.ow2.bonita.util.ProcessBuilder;

public class DocumentTest extends APITestCase {
  
  private final String mimeType = "plain/text";
  
  public void testCreateAnInstanceDocument() throws Exception {
    ProcessDefinition definition = ProcessBuilder.createProcess("doc", "1.0")
      .addHuman(getLogin())
      .addHumanTask("step1", getLogin())
    .done();

    getManagementAPI().deploy(getBusinessArchive(definition));
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(definition.getUUID());

    String content = "Aleksi\nEljas\nHeikki\nMatti";
    Document document = getRuntimeAPI().createDocument("myDocument1", instanceUUID, "names.txt", mimeType, content.getBytes());
    byte[] actualContent = getQueryRuntimeAPI().getDocumentContent(document.getUUID());
    assertEquals(content, new String(actualContent));

    getRuntimeAPI().deleteDocuments(true, document.getUUID());
    getManagementAPI().deleteProcess(definition.getUUID());
  }
  
  public void testCreateAProcessDocument() throws Exception {
    ProcessDefinition definition = ProcessBuilder.createProcess("doc", "1.0")
      .addHuman(getLogin())
      .addHumanTask("step1", getLogin())
    .done();

    ProcessDefinition process = getManagementAPI().deploy(getBusinessArchive(definition));

    String content = "Aleksi\nEljas\nHeikki\nMatti";
    Document document = getRuntimeAPI().createDocument("myDocument1", process.getUUID(), "names.txt", mimeType, content.getBytes());
    byte[] actualContent = getQueryRuntimeAPI().getDocumentContent(document.getUUID());
    assertEquals(content, new String(actualContent));
    
    getRuntimeAPI().deleteDocuments(true, document.getUUID());
    getManagementAPI().deleteProcess(definition.getUUID());
  }

  public void testThrowAnExceptionIfADocumentAlreadyExists() throws Exception {
    ProcessDefinition definition = ProcessBuilder.createProcess("doc", "1.0")
      .addHuman(getLogin())
      .addHumanTask("step1", getLogin())
    .done();

    getManagementAPI().deploy(getBusinessArchive(definition));
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(definition.getUUID());

    String content = "Aleksi\nEljas\nHeikki\nMatti";
    Document document = getRuntimeAPI().createDocument("myDocument1", instanceUUID, "names.txt", mimeType, content.getBytes());
    try {
      getRuntimeAPI().createDocument("myDocument1", instanceUUID, "names.txt", mimeType, content.getBytes());
      fail("Document already exists");
    } catch (DocumentAlreadyExistsException e) {
      
    }
    getRuntimeAPI().deleteDocuments(true, document.getUUID());
    getManagementAPI().deleteProcess(definition.getUUID());
  }
  
  public void testUpdateADocument() throws Exception {
    ProcessDefinition definition = ProcessBuilder.createProcess("doc", "1.0")
      .addHuman(getLogin())
      .addHumanTask("step1", getLogin())
    .done();

    getManagementAPI().deploy(getBusinessArchive(definition));
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(definition.getUUID());

    String content = "Aleksi\nEljas\nHeikki\nMatti";
    Document document = getRuntimeAPI().createDocument("myDocument1", instanceUUID, "names.txt", mimeType, content.getBytes());
    String updatedContent = content.concat("\nTiina");
    Document update = getRuntimeAPI().addDocumentVersion(document.getUUID(), true, "names(1).txt", mimeType, updatedContent.getBytes());

    byte[] actualUpdatedContent = getQueryRuntimeAPI().getDocumentContent(update.getUUID());
    assertEquals(updatedContent, new String(actualUpdatedContent));
    document = getQueryRuntimeAPI().getDocumentVersions(update.getUUID()).get(1);//FIXME should not have to do that but there is a bg on xcmis ( CMIS-507)
    byte[] actualContent = getQueryRuntimeAPI().getDocumentContent(document.getUUID());
    assertEquals(content, new String(actualContent));

    getRuntimeAPI().deleteDocuments(true, update.getUUID());
    getManagementAPI().deleteProcess(definition.getUUID());
  }

  public void testGetDocumentsOfAnInstance() throws Exception {
    ProcessDefinition definition = ProcessBuilder.createProcess("doc", "1.0")
      .addHuman(getLogin())
      .addHumanTask("step1", getLogin())
    .done();

    getManagementAPI().deploy(getBusinessArchive(definition));
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(definition.getUUID());

    String content1 = "Aleksi\nEljas\nHeikki\nMatti";
    Document document1 = getRuntimeAPI().createDocument("myDoc", instanceUUID, "names.txt", mimeType, content1.getBytes());
    String content2 = content1.concat("\nTiina");
    Document document2 = getRuntimeAPI().createDocument("update1.0", instanceUUID, "names(1).txt", mimeType, content2.getBytes());

    DocumentSearchBuilder builder = new DocumentSearchBuilder();
    builder.criterion(DocumentIndex.PROCESS_INSTANCE_UUID).equalsTo(instanceUUID.getValue());

    DocumentResult result = getQueryRuntimeAPI().searchDocuments(builder, 0, 10);
    assertNotNull(result);
    assertEquals(2, result.getCount());
    List<Document> documents = result.getDocuments();
    Document searchdocument1 = documents.get(0);
    Document searchdocument2 = documents.get(1);
    if ("myDoc".equals(searchdocument1.getName())) {
      assertEquals(document1, searchdocument1);
      assertEquals(document2, searchdocument2);
    } else {
      assertEquals(document2, searchdocument1);
      assertEquals(document1, searchdocument2);
    }
    getRuntimeAPI().deleteDocuments(true, document1.getUUID(), document2.getUUID());
    getManagementAPI().deleteProcess(definition.getUUID());
  }
  
  public void testGetDocumentsOfAProcess() throws Exception {
    ProcessDefinition definition = ProcessBuilder.createProcess("doc", "1.0")
      .addHuman(getLogin())
      .addHumanTask("step1", getLogin())
    .done();

    ProcessDefinition process = getManagementAPI().deploy(getBusinessArchive(definition));
    ProcessDefinitionUUID processDefinitionUUID = process.getUUID();

    String content1 = "Aleksi\nEljas\nHeikki\nMatti";
    Document document1 = getRuntimeAPI().createDocument("myDoc", processDefinitionUUID, "names.txt", mimeType, content1.getBytes());
    String content2 = content1.concat("\nTiina");
    Document document2 = getRuntimeAPI().createDocument("update1.0", processDefinitionUUID, "names(1).txt", mimeType, content2.getBytes());

    DocumentSearchBuilder builder = new DocumentSearchBuilder();
    builder.criterion(DocumentIndex.PROCESS_DEFINITION_UUID_WITHOUT_INSTANCES).equalsTo(processDefinitionUUID.getValue());

    DocumentResult result = getQueryRuntimeAPI().searchDocuments(builder, 0, 10);
    assertNotNull(result);
    assertEquals(2, result.getCount());
    List<Document> documents = result.getDocuments();
    Document searchdocument1 = documents.get(0);
    Document searchdocument2 = documents.get(1);
    if ("myDoc".equals(searchdocument1.getName())) {
      assertEquals(document1, searchdocument1);
      assertEquals(document2, searchdocument2);
    } else {
      assertEquals(document2, searchdocument1);
      assertEquals(document1, searchdocument2);
    }
    assertNotNull(searchdocument1.getProcessDefinitionUUID());
    assertNotNull(searchdocument2.getProcessDefinitionUUID());
    getRuntimeAPI().deleteDocuments(true, document1.getUUID(), document2.getUUID());
    getManagementAPI().deleteProcess(definition.getUUID());
  }

  public void testAttachmentFromProcessDefinitionToDocument() throws Exception {
    final String attachmentName = "aName";
    final byte[] initialValue = new byte[] {1, 2, 3, 4, 5, 6};
    Map<String, byte[]> resources = new HashMap<String, byte[]>();
    resources.put(attachmentName, initialValue);

    final ProcessDefinition process = ProcessBuilder.createProcess("rmAttchmnt", "1.0")
    .addAttachment(attachmentName)
    .addHuman(getLogin())
    .addHumanTask("step", getLogin())
    .done();

    getManagementAPI().deploy(getBusinessArchive(process, resources));
    final ProcessDefinitionUUID processUUID = process.getUUID();
    final ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID);

    final Set<String> attachmentNames = getQueryRuntimeAPI().getAttachmentNames(instanceUUID);
    assertEquals(1, attachmentNames.size());
    assertTrue(attachmentNames.contains(attachmentName));

    final DocumentSearchBuilder builder = new DocumentSearchBuilder();
    builder.criterion(DocumentIndex.NAME).equalsTo(attachmentName)
    .and().criterion(DocumentIndex.PROCESS_INSTANCE_UUID).equalsTo(instanceUUID.toString());
    final DocumentResult result = getQueryRuntimeAPI().searchDocuments(builder, 0, 10);
    assertEquals(1, result.getCount());
    final List<Document> documents = result.getDocuments();
    assertEquals(1, documents.size());

    final List<AttachmentInstance> attachments = getQueryRuntimeAPI().getAttachments(instanceUUID, attachmentName);
    assertEquals(attachments.get(0), documents.get(0));

    getManagementAPI().deleteProcess(processUUID);
  }

  public void testAttachmentToDocument() throws Exception {
    final String attachment = "aName";
    final byte[] initialValue = new byte[] {1, 2, 3, 4, 5, 6};
    Map<String, byte[]> resources = new HashMap<String, byte[]>();
    resources.put(attachment, initialValue);

    final ProcessDefinition process = ProcessBuilder.createProcess("rmAttchmnt", "1.0")
    .addHuman(getLogin())
    .addHumanTask("step", getLogin())
    .done();

    getManagementAPI().deploy(getBusinessArchive(process, resources));
    ProcessDefinitionUUID processUUID = process.getUUID();
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID);

    final Map<String, String> metadata = new HashMap<String, String>();
    metadata.put("mime-type", mimeType);
    getRuntimeAPI().addAttachment(instanceUUID, attachment, "label", "description", "toto", metadata, "hello".getBytes());

    final Set<String> attachmentNames = getQueryRuntimeAPI().getAttachmentNames(instanceUUID);
    assertEquals(1, attachmentNames.size());
    assertTrue(attachmentNames.contains(attachment));

    List<AttachmentInstance> attachments = getQueryRuntimeAPI().getAttachments(instanceUUID, attachment);
    final DocumentSearchBuilder builder = new DocumentSearchBuilder();
    builder.criterion(DocumentIndex.NAME).equalsTo(attachment);
    final DocumentResult result = getQueryRuntimeAPI().searchDocuments(builder, 0, 10);
    assertEquals(1, result.getCount());
    final List<Document> documents = result.getDocuments();
    assertEquals(1, documents.size());
    assertEquals(attachments.get(0), documents.get(0));
    getManagementAPI().deleteProcess(processUUID);
  }

  public void testDocumentToAttachment() throws Exception {
    final ProcessDefinition process = ProcessBuilder.createProcess("rmAttchmnt", "1.0")
    .addHuman(getLogin())
    .addHumanTask("step", getLogin())
    .done();

    getManagementAPI().deploy(getBusinessArchive(process));
    ProcessDefinitionUUID processUUID = process.getUUID();
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(processUUID);

    Document doc = getRuntimeAPI().createDocument("doc", instanceUUID, "fileName", mimeType, "content".getBytes());
    final List<AttachmentInstance> attachments = getQueryRuntimeAPI().getAttachments(instanceUUID, "doc");
    assertEquals(1, attachments.size());

    assertEquals(attachments.get(0), doc);
    getManagementAPI().deleteProcess(processUUID);
  }

  private void assertEquals(AttachmentInstance attachment, Document document) {
    assertEquals(attachment.getName(), document.getName());
    assertEquals(attachment.getAuthor(), document.getAuthor());
    assertEquals(attachment.getFileName(), document.getContentFileName());
    assertEquals(attachment.getProcessInstanceUUID(), document.getProcessInstanceUUID());
    assertEquals(attachment.getVersionDate(), document.getCreationDate());

    String mimeType = attachment.getMetaData().get("content-type");
    if (mimeType != null) {
      assertEquals(mimeType, document.getContentMimeType());
    }
  }

  public void testDeleteADocumentOfAnArchivedProcess() throws Exception {
    ProcessDefinition definition = ProcessBuilder.createProcess("doc", "1.0")
      .addHuman(getLogin())
      .addHumanTask("step1", getLogin())
    .done();

    getManagementAPI().deploy(getBusinessArchive(definition));
    ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(definition.getUUID());

    String content = "Aleksi\nEljas\nHeikki\nMatti";
    Document document = getRuntimeAPI().createDocument("myDocument1", instanceUUID, "names.txt", mimeType, content.getBytes());

    executeTask(instanceUUID, "step1");
    getRuntimeAPI().deleteDocuments(true, document.getUUID());
    getManagementAPI().deleteProcess(definition.getUUID());
  }
  
  public void testCreateDocumentAndAddDocumentVersionChangesLastUpdate() throws Exception {
    ProcessDefinition attachmentProcess = ProcessBuilder.createProcess("p", "1.0")
    .addHuman(getLogin())
    .addHumanTask("task1", getLogin())
    .done();

    final BusinessArchive businessArchive = BusinessArchiveFactory.getBusinessArchive(attachmentProcess);
    attachmentProcess = getManagementAPI().deploy(businessArchive);

    final ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(attachmentProcess.getUUID());    
    final ProcessInstance initialInstance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    Thread.sleep(100);
    final Document document = getRuntimeAPI().createDocument("doc", instanceUUID, "doc1.txt", mimeType, "content".getBytes());
    final ProcessInstance instanceAfterCreateDocument = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertFalse(initialInstance.getLastUpdate().equals(instanceAfterCreateDocument.getLastUpdate()));
    Thread.sleep(100);
    getRuntimeAPI().addDocumentVersion(document.getUUID(), true, "doc1(1).txt", mimeType, "update".getBytes());
    final ProcessInstance instanceAfterAddVersion =  getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertFalse(instanceAfterCreateDocument.getLastUpdate().equals(instanceAfterAddVersion.getLastUpdate()));
    getManagementAPI().deleteProcess(attachmentProcess.getUUID());
  }

  public void testCanAddAVersionOnAnArchivedProcessInstance() throws Exception {
    ProcessDefinition attachmentProcess = ProcessBuilder.createProcess("p", "1.0")
    .addHuman(getLogin())
    .addHumanTask("task1", getLogin())
    .done();

    final BusinessArchive businessArchive = BusinessArchiveFactory.getBusinessArchive(attachmentProcess);
    attachmentProcess = getManagementAPI().deploy(businessArchive);
    final ProcessInstanceUUID instanceUUID = getRuntimeAPI().instantiateProcess(attachmentProcess.getUUID());    
    final ProcessInstance initialInstance = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    Thread.sleep(100);
    final Document document = getRuntimeAPI().createDocument("doc", instanceUUID, "doc1.txt", mimeType, "content".getBytes());
    final ProcessInstance instanceAfterCreateDocument = getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertFalse(initialInstance.getLastUpdate().equals(instanceAfterCreateDocument.getLastUpdate()));

    executeTask(instanceUUID, "task1");
    getRuntimeAPI().addDocumentVersion(document.getUUID(), true, "doc1(1).txt", mimeType, "update".getBytes());
    final ProcessInstance instanceAfterAddVersion =  getQueryRuntimeAPI().getProcessInstance(instanceUUID);
    assertFalse(instanceAfterCreateDocument.getLastUpdate().equals(instanceAfterAddVersion.getLastUpdate()));
    getManagementAPI().deleteProcess(attachmentProcess.getUUID());
  }
  
  public void testSearchWithInClauseInProcessInstance() throws Exception {
      //create an instance document
      ProcessDefinition definition1 = ProcessBuilder.createProcess("doc1", "1.0")
              .addHuman(getLogin())
              .addHumanTask("step1", getLogin())
              .done();
      ProcessDefinition definition2 = ProcessBuilder.createProcess("doc2", "1.0")
              .addHuman(getLogin())
              .addHumanTask("step2", getLogin())
              .done();
      
      getManagementAPI().deploy(getBusinessArchive(definition1));
      ProcessInstanceUUID instanceUUID1 = getRuntimeAPI().instantiateProcess(definition1.getUUID());
      getManagementAPI().deploy(getBusinessArchive(definition2));
      ProcessInstanceUUID instanceUUID2 = getRuntimeAPI().instantiateProcess(definition2.getUUID());
      

      String content = "Aleksi\nEljas\nHeikki\nMatti";
      Document document1 = getRuntimeAPI().createDocument("myDocument1", instanceUUID1, "names.txt", mimeType, content.getBytes());
      Document document2 = getRuntimeAPI().createDocument("myDocument2", instanceUUID2, "names.txt", mimeType, content.getBytes());

       //Delete It by using 
      //"documentSearchBuilder.criterion(DocumentIndex.PROCESS_INSTANCE_UUID).in(set<ProcessesInstancesUuid>)"
      final DocumentSearchBuilder documentSearchBuilder = new DocumentSearchBuilder();
      String[] myProcessInstanceUUIDs = {instanceUUID1.getValue(),instanceUUID2.getValue()};
      @SuppressWarnings("unchecked")
      HashSet<String> myProcessInstanceUUIDsSet = new HashSet<String>(Arrays.asList(myProcessInstanceUUIDs));
      documentSearchBuilder.criterion(DocumentIndex.PROCESS_INSTANCE_UUID).in(myProcessInstanceUUIDsSet);
      
      final APIAccessor accessor = new StandardAPIAccessorImpl();
      final QueryRuntimeAPI queryRuntimeAPI = accessor.getQueryRuntimeAPI();
      DocumentResult searchResult;
      List<Document> documentsFound;

      searchResult = queryRuntimeAPI.searchDocuments(documentSearchBuilder, 0, 100);
      documentsFound = searchResult.getDocuments();
       
      assertEquals(2, documentsFound.size());


      getRuntimeAPI().deleteDocuments(true, document1.getUUID());
      getRuntimeAPI().deleteDocuments(true, document2.getUUID());
      getManagementAPI().deleteProcess(definition1.getUUID());
      getManagementAPI().deleteProcess(definition2.getUUID());
      
  }
}