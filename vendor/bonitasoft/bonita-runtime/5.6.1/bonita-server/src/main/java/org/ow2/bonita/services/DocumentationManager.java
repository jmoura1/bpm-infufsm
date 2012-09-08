/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ow2.bonita.services;

import java.util.Date;
import java.util.List;

import org.ow2.bonita.facade.exception.DocumentAlreadyExistsException;
import org.ow2.bonita.facade.exception.DocumentNotFoundException;
import org.ow2.bonita.facade.exception.DocumentationCreationException;
import org.ow2.bonita.facade.exception.FolderAlreadyExistsException;
import org.ow2.bonita.facade.impl.SearchResult;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.search.DocumentSearchBuilder;

/**
 * 
 * @author Baptiste Mesta, Matthieu Chaffotte
 *
 */
public interface DocumentationManager {

  /**
   * Creates a folder.
   * 
   * @param folderName the folder name.
   * @return the created folder
   */
  Folder createFolder(final String folderName) throws FolderAlreadyExistsException;

  /**
   * Creates a sub-folder into a parent folder. the parent MUST exists
   *
   * @param folderName the folder name.
   * @param parentFolderId the parent folder identifier
   * @return the created sub-folder
   */
  Folder createFolder(final String folderName, final String parentFolderId) throws FolderAlreadyExistsException;

  /**
   * Creates a document with an empty content. This document will be attached to a process definition.
   *
   * @param name the document name
   * @param definitionUUID the process definition UUID
   * @return the created document
   * @throws DocumentAlreadyExistsException if the document already exists.
   * @throws DocumentationCreationException for other any document creation exception
   */
  Document createDocument(final String name, final ProcessDefinitionUUID definitionUUID) throws DocumentationCreationException, DocumentAlreadyExistsException;

  /**
   * Creates a document with an empty content. This document will be attached to a process definition and the process instance.
   *
   * @param name the document name
   * @param definitionUUID the process definition UUID
   * @param instanceUUID the process instance UUID
   * @return the created document
   * @throws DocumentAlreadyExistsException if the document already exists.
   * @throws DocumentationCreationException for other any document creation exception
   */
  Document createDocument(final String name, final ProcessDefinitionUUID definitionUUID, final ProcessInstanceUUID instanceUUID)
  throws DocumentationCreationException, DocumentAlreadyExistsException;

  /**
   * Creates a document and stores the file data.
   *
   * @param name the document name
   * @param definitionUUID the process definition UUID
   * @param fileName the file name
   * @param contentMimeType the content type of the file
   * @param fileContent the content of the file
   * @return the created document
   * @throws DocumentAlreadyExistsException if the document already exists
   * @throws DocumentationCreationException for other any document creation exception
   */
  Document createDocument(final String name, final ProcessDefinitionUUID definitionUUID, String fileName, String contentMimeType, final byte[] fileContent)
  throws DocumentationCreationException, DocumentAlreadyExistsException;

  /**
   * Creates a document and stores the file data.
   *
   * @param name the document name
   * @param definitionUUID the process definition UUID
   * @param instanceUUID the process instance UUID
   * @param fileName the file name
   * @param contentMimeType the content type of the file
   * @param fileContent the content of the file
   * @return the created document
   * @throws DocumentAlreadyExistsException if the document already exists
   * @throws DocumentationCreationException for other any document creation exception
   */
  Document createDocument(final String name, final ProcessDefinitionUUID definitionUUID, final ProcessInstanceUUID instanceUUID, String fileName, String contentMimeType, final byte[] fileContent)
  throws DocumentationCreationException, DocumentAlreadyExistsException;

  /**
   * Create a document and stores the file data in the given folder.
   * 
   * @param name the document name
   * @param folderId the folder identifier
   * @param fileName the file name
   * @param contentMimeType the content type of the file
   * @param fileContent the content of the file
   * @return the created document
   * @throws DocumentAlreadyExistsException if the document already exists
   * @throws DocumentationCreationException for other any document creation exception
   */
  Document createDocument(final String name, final String folderId, String fileName, String contentMimeType, final byte[] fileContent)
  throws DocumentationCreationException, DocumentAlreadyExistsException;

  /**
   * Create a document.
   *
   * @param name the document name
   * @param definitionUUID the process definition UUID
   * @param instanceUUID the process instance UUID
   * @param author the file author
   * @param versionDate the date (last update) of the file
   * @return the created document
   * @throws DocumentAlreadyExistsException if the document already exists
   * @throws DocumentationCreationException for other any document creation exception
   */
  Document createDocument(String name, ProcessDefinitionUUID definitionUUID, ProcessInstanceUUID instanceUUID, String author, Date versionDate)
  throws DocumentationCreationException, DocumentAlreadyExistsException;

  /**
   * Create a document.
   *
   * @param name the document name
   * @param definitionUUID the process definition UUID
   * @param instanceUUID the process instance UUID
   * @param author the file author
   * @param versionDate the date (last update) of the file
   * @param fileName the file name
   * @param mimeType the content type of the file
   * @param content the content of the file
   * @return the created document
   * @throws DocumentAlreadyExistsException if the document already exists
   * @throws DocumentationCreationException for other any document creation exception
   */
  Document createDocument(String name, ProcessDefinitionUUID definitionUUID, ProcessInstanceUUID instanceUUID, String author, Date versionDate, String fileName, String mimeType, byte[] content)
  throws DocumentationCreationException, DocumentAlreadyExistsException;

  /**
   * Create a document. this document will be stored inside the given folder.
   *  
   * @param definitionUUID the process definition UUID
   * @param name the document name
   * @param subFolderName the sub-folder name
   * @param fileName the file name
   * @param contentMimeType the content type of the file
   * @param fileContent the content of the file
   * @return the created document
   * @throws DocumentAlreadyExistsException if the document already exists
   * @throws DocumentationCreationException for other any document creation exception
   */
  Document createDocument(ProcessDefinitionUUID definitionUUID, String name, String subFolderName, String fileName, String contentMimeType, byte[] fileContent)
  throws DocumentationCreationException, DocumentAlreadyExistsException;

  /**
   * Gets the document according to its identifier.
   * @param documentId the document identifier
   * @return the document
   * @throws DocumentNotFoundException if the document is not found
   */
  Document getDocument(final String documentId) throws DocumentNotFoundException;

  /**
   * Deletes a document and its version if allVersions is true.
   * 
   * @param documentId the document identifier
   * @param allVersions true to delete all versions of the given document.
   * @throws DocumentNotFoundException if the document is not found
   */
  void deleteDocument(final String documentId, boolean allVersions) throws DocumentNotFoundException;

  /**
   * Delete a folder.
   * 
   * @param folder the folder to delete
   */
  void deleteFolder(Folder folder);

  /**
   * Get the content of a document.
   * 
   * @param document the document
   * @return the content of the document
   * @throws DocumentNotFoundException if the document is not found
   */
  byte[] getContent(final Document document) throws DocumentNotFoundException;

  /**
   * Get all the folder having the given name or an empty list.
   * 
   * @param folderName the name of the folder
   * @return a list of folders having the given name
   */
  List<Folder> getFolders(String folderName);

  /**
   * Gets the root folder of the repository.
   * 
   * @return the root folder of the repository
   */
  Folder getRootFolder();

  /**
   * Returns the documents of the given folder (only one level)
   * or an empty list if the folder id does not refer to a folder.
   * 
   * @param folderId the folder identifier.
   * @return the documents of the given folder
   */
  List<Document> getChildrenDocuments(String folderId);

  /**
   * Returns the documents of the given folder (only one level)
   * or an empty list if the folder id does not refer to a folder.
   * 
   * @param folderId the folder identifier.
   * @return the documents of the given folder
   */
  List<Document> getChildrenDocuments(ProcessDefinitionUUID processDefinitionUUID,String subfolderName)
  throws DocumentationCreationException;  

  /**
   * Returns the sub folders of the given folder (only one level)
   * or an empty list if the folder id does not refer to a folder.
   * 
   * @param folderId the folder identifier.
   * @return the sub folders of the given folder
   */
  List<Folder> getChildrenFolder(String folderId);

  /**
   * Gets all the versions of the given document.
   * 
   * @param documentId the document identifier
   * @return all the versions of a document.
   * @throws DocumentNotFoundException if the document is not found
   */
  List<Document> getVersionsOfDocument(String documentId) throws DocumentNotFoundException;

  /**
   * Returns the path where the document is stored. The path looks like "firstFodler/sub/Folder/...".
   * 
   * @param documentId the document identifier
   * @return the path
   * @throws DocumentNotFoundException if the document is not found
   */
  String getDocumentPath(String documentId) throws DocumentNotFoundException;

  /**
   * Create a new version of a document. It creates a new copy of the document and define whether the new version is a major one.
   * 
   * @param documentId the document identifier
   * @param isMajorVersion true to make the new version a major one
   * @return the new document version
   * @throws DocumentationCreationException for any document creation exception
   */
  Document createVersion(String documentId, boolean isMajorVersion) throws DocumentationCreationException;

  /**
   * Create a new version of a document. It creates a new copy of the document and define whether the new version is a major one.
   * 
   * @param documentId the document identifier
   * @param isMajorVersion true to make the new version a major one
   * @param author the author of the new document
   * @param versionDate the last update date of the new document
   * @return the new document version
   * @throws DocumentationCreationException for any document creation exception
   */
  Document createVersion(String documentId, boolean isMajorVersion, String author, Date versionDate) throws DocumentationCreationException;

  /**
   * Create a new version of a document. It creates a new copy of the document and define whether the new version is a major one.
   * 
   * @param documentId the document identifier
   * @param isMajorVersion true to make the new version a major one
   * @param fileName the file name
   * @param mimeType the content type of the file
   * @param content the content of the file
   * @return the new document version
   * @throws DocumentationCreationException for any document creation exception
   */
  Document createVersion(String documentId, boolean isMajorVersion, String fileName, String mimeType, byte[] content) throws DocumentationCreationException;

  /**
   * Create a new version of a document. It creates a new copy of the document and define whether the new version is a major one.
   * 
   * @param documentId the document identifier
   * @param isMajorVersion true to make the new version a major one
   * @param author the author of the new document
   * @param versionDate the last update date of the new document
   * @param fileName the file name
   * @param mimeType the content type of the file
   * @param content the content of the file
   * @return the new document version
   * @throws DocumentationCreationException for any document creation exception
   */
  Document createVersion(String documentId, boolean isMajorVersion, String author, Date versionDate, String fileName, String mimeType, byte[] content)
  throws DocumentationCreationException;

  /**
   * Searches documents according to the query given by the builder.
   * 
   * @param builder the builder which contains the query
   * @param fromResult the first document to retrieve
   * @param maxResults the number of documents to retrieve
   * @return the found documents according to the query
   */
  SearchResult search(DocumentSearchBuilder builder, int fromResult, int maxResults);

  /**
   * Clear all the repository: folders and documents.
   * @throws DocumentNotFoundException if a document is not found during deletion
   */
  void clear() throws DocumentNotFoundException;

  /**
   * Updates the file data of a document.
   * 
   * @param documentId the document identifier
   * @param fileName the new file name
   * @param mimeType the new content type of the file
   * @param size the new size of the file
   * @param content the new content of the file
   * @throws DocumentNotFoundException if the document is not found
   */
  void updateDocumentContent(final String documentId, final String fileName, final String mimeType, final int size, final byte[] content)
  throws DocumentNotFoundException;

  /**
   * Attaches a document to a process definition UUID.
   * 
   * @param processDefinitionUUID the process definition UUID
   * @param documentId the document identifier
   * @throws DocumentNotFoundException if the document is not found
   */
  public void attachDocumentTo(final ProcessDefinitionUUID processDefinitionUUID, final String documentId) throws DocumentNotFoundException;

  /**
   * Attaches a document to a process definition UUID and a process instance UUID.
   * 
   * @param processDefinitionUUID the process definition UUID
   * @param processInstanceUUID the process instance UUID
   * @param documentId the document identifier
   * @throws DocumentNotFoundException if the document is not found
   */
  public void attachDocumentTo(final ProcessDefinitionUUID processDefinitionUUID, final ProcessInstanceUUID processInstanceUUID, final String documentId)
  throws DocumentNotFoundException;

}
