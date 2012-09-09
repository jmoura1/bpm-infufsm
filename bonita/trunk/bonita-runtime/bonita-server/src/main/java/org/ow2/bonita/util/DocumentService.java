package org.ow2.bonita.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ow2.bonita.facade.exception.DocumentNotFoundException;
import org.ow2.bonita.facade.impl.SearchResult;
import org.ow2.bonita.facade.runtime.AttachmentInstance;
import org.ow2.bonita.facade.runtime.impl.AttachmentInstanceImpl;
import org.ow2.bonita.facade.runtime.impl.DocumentImpl;
import org.ow2.bonita.facade.uuid.DocumentUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.search.DocumentSearchBuilder;
import org.ow2.bonita.search.index.DocumentIndex;
import org.ow2.bonita.services.Document;
import org.ow2.bonita.services.DocumentationManager;

public class DocumentService {

  public final static String DEFAULT_MIME_TYPE = "application/octet-stream";
  
  public static DocumentImpl getClientDocument(final DocumentationManager manager, final org.ow2.bonita.services.Document document) {
    final DocumentUUID uuid = new DocumentUUID(document.getId());
    final String name = document.getName();
    final String author = document.getAuthor();
    final Date creationDate = document.getCreationDate();
    final String lastModifiedBy = document.getLastModifiedBy();
    final Date lastModificationDate = document.getLastModificationDate();
    final boolean isLatestVersion = document.isLatestVersion();
    final boolean isMajorVersion = document.isMajorVersion();
    final String versionLabel = document.getVersionLabel();
    final String versionSeriesId = document.getVersionSeriesId();
    final String fileName = document.getContentFileName();
    final String mimeType = document.getContentMimeType();
    final long size = document.getContentSize();
    final ProcessDefinitionUUID definitionUUID = document.getProcessDefinitionUUID();
    final ProcessInstanceUUID instanceUUID = document.getProcessInstanceUUID();
    final DocumentImpl doc = new DocumentImpl(uuid, name, definitionUUID, instanceUUID, author,
        creationDate, lastModifiedBy, lastModificationDate, isLatestVersion, isMajorVersion, versionLabel, versionSeriesId, fileName, mimeType, size);
    return doc;
  }
  
  public static List<org.ow2.bonita.facade.runtime.Document> getClientDocuments(final DocumentationManager manager, final List<Document> documents){
      final List<org.ow2.bonita.facade.runtime.Document> result = new ArrayList<org.ow2.bonita.facade.runtime.Document>();                
      for (final Document document : documents){
          result.add(getClientDocument(manager,document));
      }
      return result;
  }

  public static AttachmentInstance getAttachmentFromDocument(final DocumentationManager manager, final org.ow2.bonita.services.Document document) {
    final String name = document.getName();
    final String author = document.getAuthor();
    final Date creationDate = document.getCreationDate();
    final String fileName = document.getContentFileName();
    final String mimeType = document.getContentMimeType();
    final DocumentUUID documentUUID = new DocumentUUID(document.getId());
    final ProcessInstanceUUID instanceUUID = document.getProcessInstanceUUID();
    final AttachmentInstanceImpl attachment = new AttachmentInstanceImpl(documentUUID, name, instanceUUID, author, creationDate);
    attachment.setFileName(fileName);
    if (mimeType != null) {
      Map<String, String> metadata = new HashMap<String, String>();
      metadata.put("content-type", mimeType);
      attachment.setMetaData(metadata);
    }
    return attachment;
  }

  public static SearchResult getDocuments(final DocumentationManager manager, ProcessDefinitionUUID definitionUUID, String documentName) {
    DocumentSearchBuilder builder = new DocumentSearchBuilder();
    builder.criterion(DocumentIndex.PROCESS_DEFINITION_UUID).equalsTo(definitionUUID.getValue())
    .and().criterion(DocumentIndex.NAME).equalsTo(documentName);
    return manager.search(builder, 0, Integer.MAX_VALUE);
  }

  public static SearchResult getDocuments(final DocumentationManager manager, ProcessInstanceUUID instanceUUID, String documentName) {
    DocumentSearchBuilder builder = new DocumentSearchBuilder();
    builder.criterion(DocumentIndex.PROCESS_INSTANCE_UUID).equalsTo(instanceUUID.getValue())
    .and().criterion(DocumentIndex.NAME).equalsTo(documentName);
    return manager.search(builder, 0, Integer.MAX_VALUE);
  }

  public static SearchResult getDocuments(final DocumentationManager manager, ProcessInstanceUUID instanceUUID) {
    DocumentSearchBuilder builder = new DocumentSearchBuilder();
    builder.criterion(DocumentIndex.PROCESS_INSTANCE_UUID).equalsTo(instanceUUID.getValue());
    return manager.search(builder, 0, Integer.MAX_VALUE);
  }
  public static SearchResult getDocuments(final DocumentationManager manager, ProcessDefinitionUUID definitionUUID) {
      DocumentSearchBuilder builder = new DocumentSearchBuilder();
      builder.criterion(DocumentIndex.PROCESS_DEFINITION_UUID_WITHOUT_INSTANCES).equalsTo(definitionUUID.getValue());
      return manager.search(builder, 0, Integer.MAX_VALUE);
    }

  public static List<org.ow2.bonita.services.Document> getAllDocumentVersions(final DocumentationManager manager, ProcessInstanceUUID instanceUUID) {
    DocumentSearchBuilder builder = new DocumentSearchBuilder();
    builder.criterion(DocumentIndex.PROCESS_INSTANCE_UUID).equalsTo(instanceUUID.getValue());
    SearchResult search = manager.search(builder, 0, Integer.MAX_VALUE);
    List<org.ow2.bonita.services.Document> result = new ArrayList<org.ow2.bonita.services.Document>();
    List<org.ow2.bonita.services.Document> documents = search.getDocuments();
    for (org.ow2.bonita.services.Document document : documents) {
      List<org.ow2.bonita.services.Document> versionsOfDocument;
      try {
        versionsOfDocument = manager.getVersionsOfDocument(document.getId());
        result.addAll(versionsOfDocument);
      } catch (DocumentNotFoundException e) {
        throw new BonitaRuntimeException(e);
      }
    }
    return result;
  }

  private static List<Document> getAllDocumentVersions(DocumentationManager manager, ProcessInstanceUUID instanceUUID, String attachmentName) {
    SearchResult search = getDocuments(manager, instanceUUID, attachmentName);
    List<org.ow2.bonita.services.Document> result = new ArrayList<org.ow2.bonita.services.Document>();
    List<org.ow2.bonita.services.Document> documents = search.getDocuments();
    for (org.ow2.bonita.services.Document document : documents) {
      List<org.ow2.bonita.services.Document> versionsOfDocument;
      try {
        versionsOfDocument = manager.getVersionsOfDocument(document.getId());
        result.addAll(versionsOfDocument);
      } catch (DocumentNotFoundException e) {
        throw new BonitaRuntimeException(e);
      }
    }
    return result;
  }

  public static List<AttachmentInstance> getAllAttachmentVersions(final DocumentationManager manager, ProcessInstanceUUID instanceUUID) {
    final List<AttachmentInstance> attachments = new ArrayList<AttachmentInstance>();
    List<Document> allDocumentVersions = DocumentService.getAllDocumentVersions(manager, instanceUUID);
    for (Document document : allDocumentVersions) {
      attachments.add(DocumentService.getAttachmentFromDocument(manager, document));
    }
    return attachments;
  }

  public static List<AttachmentInstance> getAllAttachmentVersions(final DocumentationManager manager, ProcessInstanceUUID instanceUUID, final String attachmentName) {
    final List<AttachmentInstance> attachments = new ArrayList<AttachmentInstance>();
    List<Document> allDocumentVersions = DocumentService.getAllDocumentVersions(manager, instanceUUID, attachmentName);
    for (Document document : allDocumentVersions) {
      attachments.add(DocumentService.getAttachmentFromDocument(manager, document));
    }
    return attachments;
  }

  public static List<AttachmentInstance> getLastAttachments(final DocumentationManager manager, ProcessInstanceUUID instanceUUID) {
    final List<AttachmentInstance> attachments = new ArrayList<AttachmentInstance>();
    final SearchResult result = DocumentService.getDocuments(manager, instanceUUID);
    List<Document> lastDocuments = result.getDocuments();
    for (Document document : lastDocuments) {
      attachments.add(DocumentService.getAttachmentFromDocument(manager, document));
    }
    return attachments;
  }

}
