/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ow2.bonita.services.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.ow2.bonita.facade.exception.DocumentAlreadyExistsException;
import org.ow2.bonita.facade.exception.DocumentNotFoundException;
import org.ow2.bonita.facade.exception.DocumentationCreationException;
import org.ow2.bonita.facade.impl.SearchResult;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.search.DocumentSearchBuilder;
import org.ow2.bonita.search.index.DocumentIndex;
import org.ow2.bonita.services.Document;
import org.ow2.bonita.services.DocumentationManager;
import org.ow2.bonita.services.Folder;

/**
 * @author Baptiste Mesta
 */
public class DocumentationManagerTest extends TestCase {

    private static DocumentationManager manager;

    @Override
    public void setUp() {

        manager = getDocumentationManager();
        try {
            manager.clear();
        } catch (final DocumentNotFoundException e) {
            e.printStackTrace();
        }
    }

    private DocumentationManager getDocumentationManager() {
        final String binding = "ATOM";
        String cmisServer = null;
        final String cmisServerSysProp = System.getProperty("cmis.server.host");
        if (cmisServerSysProp != null && !cmisServerSysProp.trim().equals("")) {
            cmisServer = cmisServerSysProp;
        } else {
            cmisServer = "localhost:8080";
        }
        final String url = "http://" + cmisServer + "/xcmis/rest/cmisatom";
        String repositoryId = System.getProperty("cmis.repository");
        if (repositoryId == null || repositoryId.trim().equals("")) {
            if (isMac()) {
                repositoryId = "mac";
            } else if (isWindows()) {
                repositoryId = "windows";
            } else {
                repositoryId = "linux";
            }
        }
        return new CMISDocumentManager(binding, url, repositoryId, new XCmisUserProvider("root", "exo"));
    }

    private static String os = System.getProperty("os.name").toLowerCase();

    protected static boolean isWindows() {
        return os.contains("win");
    }

    protected static boolean isMac() {
        return os.contains("mac");
    }

    protected static boolean isUnix() {
        return os.contains("nix") || os.contains("nux");
    }

    public void testCreateFolder() throws DocumentationCreationException {
        final Folder folderId = manager.createFolder("testCreateFolder");
        assertNotNull(folderId);
        assertFalse("".equals(folderId.getId()));
    }

    public void testCreateTwoFolders() throws DocumentationCreationException {
        final Folder folderId1 = manager.createFolder("testCreateFolder1");
        final Folder folderId2 = manager.createFolder("testCreateFolder2");
        assertNotSame(folderId1, folderId2);
    }

    public void testCannotCreateTwoFolderWithSameNameAtSameLevel() throws DocumentationCreationException {

        manager.createFolder("testCannotCreateTwoFolderWithSameNameAtSameLevel");
        try {
            manager.createFolder("testCannotCreateTwoFolderWithSameNameAtSameLevel");
            fail("must not be able to create 2 document with same name in same folder");
        } catch (final DocumentationCreationException e) {
        }
    }

    public void testCreateSubFolder() throws DocumentationCreationException {
        final Folder folder1 = manager.createFolder("testCreateSubFolder");
        final Folder subFodler = manager.createFolder("subFolder", folder1.getId());
        assertNotNull(subFodler);
        assertEquals(subFodler, manager.getChildrenFolder(folder1.getId()).get(0));
    }

    public void testCreateSubFolderWithSameName() throws DocumentationCreationException {
        final Folder folder1 = manager.createFolder("testCreateSubFolderWithSameName");
        final Folder subFodler = manager.createFolder("testCreateSubFolderWithSameName", folder1.getId());
        assertNotSame(folder1.getId(), subFodler.getId());
    }

    public void testGetFolderUsingName() throws DocumentationCreationException {

        final String folderName = "nameOfTheFolder";
        final Folder folder1 = manager.createFolder(folderName);

        final List<Folder> folders = manager.getFolders(folderName);
        assertNotNull(folders);
        assertEquals(1, folders.size());
        assertEquals(folder1, folders.get(0));
    }

    public void testGetFolderUsingNameWithUnexistingFolder() throws DocumentationCreationException {

        final String folderName = "nameOfTheUnexistingFolder";
        final List<Folder> folders = manager.getFolders(folderName);
        assertNotNull(folders);
        assertEquals(0, folders.size());
    }

    public void testGetSubFolder() throws DocumentationCreationException {

        final Folder folder1 = manager.createFolder("testGetSubFolder");
        final Folder subFolder = manager.createFolder("subFolder", folder1.getId());

        final List<Folder> subFolders = manager.getChildrenFolder(folder1.getId());
        assertNotNull(subFolders);
        assertEquals(1, subFolders.size());
        assertEquals(subFolder, subFolders.get(0));
    }

    public void testNoDocumentInFolder() throws DocumentationCreationException {

        final Folder folder1 = manager.createFolder("testGetDocument");

        final List<Document> documents = manager.getChildrenDocuments(folder1.getId());
        assertEquals(0, documents.size());
    }

    public void testCreateDocument() throws Exception {
        final String docName = "theDoc";
        final Document doc = manager.createDocument(docName, new ProcessDefinitionUUID("a"), new ProcessInstanceUUID("b"));
        assertNotNull(doc);
        assertNotNull(doc.getId());
        assertEquals(docName, doc.getName());
        assertNotNull(doc.getCreationDate());
        assertEquals(doc.getAuthor(), doc.getLastModifiedBy());
        assertNotNull(doc.getVersionLabel());
        assertNotNull(doc.getVersionSeriesId());
        assertTrue(doc.isMajorVersion());
        assertTrue(doc.isLatestVersion());
    }

    public void testCreateADocumentThatAlreadyExists() throws Exception {
        final String docName = "myDocument";
        manager.createDocument(docName, new ProcessDefinitionUUID("a"), new ProcessInstanceUUID("b"));
        try {
            manager.createDocument(docName, new ProcessDefinitionUUID("a"), new ProcessInstanceUUID("b"));
            fail("must throw a document creation exception");
        } catch (final DocumentAlreadyExistsException e) {
        }
    }

    public void testCreateDocumentWithContent() throws Exception {
        final byte[] contents = "The doc contents".getBytes();
        final String fileName = "testFile.txt";
        final String docName = "theDoc";
        final String contentMimeType = "kikoo/text";
        final ProcessDefinitionUUID definitionUUID = new ProcessDefinitionUUID("a");
        final ProcessInstanceUUID instanceUUID = new ProcessInstanceUUID("b");
        final Document doc = manager.createDocument(docName, definitionUUID, instanceUUID, fileName, contentMimeType, contents);
        assertNotNull(doc);
        assertNotNull(doc.getId());
        assertEquals(docName, doc.getName());
        assertNotNull(doc.getCreationDate());
        assertEquals(doc.getAuthor(), doc.getLastModifiedBy());
        assertNotNull(doc.getVersionLabel());
        assertNotNull(doc.getVersionSeriesId());
        assertTrue(doc.isMajorVersion());
        assertEquals(fileName, doc.getContentFileName());
        assertTrue(doc.isLatestVersion());
        assertNotNull(doc.getContentFileName());
        assertNotNull(doc.getContentMimeType());
        assertTrue(0 <= doc.getContentSize());
        assertEquals(contentMimeType, doc.getContentMimeType());
        assertEquals(contents.length, doc.getContentSize());
        assertEquals(definitionUUID, doc.getProcessDefinitionUUID());
        assertEquals(instanceUUID, doc.getProcessInstanceUUID());
        assertEquals("The doc contents", new String(manager.getContent(doc)));
    }

    public void testCreateDocumentWithContentWithoutContentSize() throws Exception {
        final byte[] contents = "The doc contents".getBytes();
        final String fileName = "testFile.txt";
        final String docName = "theDoc";
        final String contentMimeType = "kikoo/text";
        final Document doc = manager.createDocument(docName, new ProcessDefinitionUUID("myprocessxx"), new ProcessInstanceUUID("instancexx"), fileName,
                contentMimeType, contents);
        assertEquals(contents.length, doc.getContentSize());
        assertEquals("The doc contents", new String(manager.getContent(doc)));
    }

    public void testCreateDocumentWithEmptyContent() throws Exception {
        final byte[] contents = new byte[1];
        final String fileName = "testFile.txt";
        final String docName = "theDoc";
        final String contentMimeType = "plain/text";
        final ProcessDefinitionUUID definitionUUID = new ProcessDefinitionUUID("a");
        final ProcessInstanceUUID instanceUUID = new ProcessInstanceUUID("b");
        final Document doc = manager.createDocument(docName, definitionUUID, instanceUUID, fileName, contentMimeType, contents);
        assertNotNull(doc);
        assertNotNull(doc.getId());
        assertEquals(docName, doc.getName());
        assertNotNull(doc.getCreationDate());
        assertEquals(doc.getAuthor(), doc.getLastModifiedBy());
        assertNotNull(doc.getVersionLabel());
        assertNotNull(doc.getVersionSeriesId());
        assertTrue(doc.isMajorVersion());
        assertTrue(doc.isLatestVersion());
        assertEquals(fileName, doc.getContentFileName());
        assertNotNull(doc.getContentFileName());
        assertNotNull(doc.getContentMimeType());
        assertTrue(0 <= doc.getContentSize());
        assertEquals(contentMimeType, doc.getContentMimeType());
        assertEquals(definitionUUID, doc.getProcessDefinitionUUID());
        assertEquals(instanceUUID, doc.getProcessInstanceUUID());
        assertEquals(contents.length, manager.getContent(doc).length);
    }

    public void testGetDocument() throws Exception {
        final Document doc = manager.createDocument("theDoc", new ProcessDefinitionUUID("a"), new ProcessInstanceUUID("b"), "testFile.txt", "plain/text",
                "The doc contents".getBytes());
        final Document document = manager.getDocument(doc.getId());
        assertNotNull(document);
        assertEquals(doc, document);
    }

    public void testDeleteAllVersionsOfDocumentById() throws Exception {
        final Document doc = manager.createDocument("theDoc", new ProcessDefinitionUUID("a"), new ProcessInstanceUUID("b"), "testFile.txt", "plain/text",
                "The doc contents".getBytes());
        manager.deleteDocument(doc.getId(), true);
        try {
            manager.getDocument(doc.getId());
            fail("should throw DocumentNotFoundException");
        } catch (final DocumentNotFoundException e) {
        }
    }

    public void testCreateDocumentInSubFolder() throws DocumentationCreationException, DocumentationCreationException, DocumentAlreadyExistsException {
        final Folder folder = manager.createFolder("testCreateDocumentInSubFolder");
        final Folder subFolder = manager.createFolder("subFolder", folder.getId());

        final Document doc = manager.createDocument("theDoc", new ProcessDefinitionUUID("testCreateDocumentInSubFolder"), new ProcessInstanceUUID("subFolder"),
                "testFile.txt", "plain/text", "The doc contents".getBytes());
        assertEquals(subFolder.getId(), doc.getParentFolderId());
    }

    public void testCreateNewVersionOfDocument() throws DocumentationCreationException, DocumentationCreationException, DocumentAlreadyExistsException,
            DocumentNotFoundException {
        final byte[] contents = "The doc contents".getBytes();
        final String fileName = "testFile.txt";
        final String docName = "theDoc";
        final String contentMimeType = "plain/text";
        final Document doc = manager.createDocument(docName, new ProcessDefinitionUUID("a"), new ProcessInstanceUUID("b"), fileName, contentMimeType, contents);
        final Document newDoc = manager.createVersion(doc.getId(), true, "testFile2.txt", "plain/text", "The new doc contents".getBytes());

        assertNotNull(newDoc);
        assertEquals(doc.getName(), newDoc.getName());
        // assertEquals("The doc contents", new String(manager.getContent(doc))); //not supported by xCmis because of id
        // modifications
        assertEquals("The new doc contents", new String(manager.getContent(newDoc)));
        assertEquals(doc.getAuthor(), newDoc.getAuthor());
        assertEquals("testFile2.txt", newDoc.getContentFileName());
        assertEquals("plain/text", newDoc.getContentMimeType());
    }

    public void testCreateNewVersionOfDocumentWithoutContent() throws DocumentationCreationException, DocumentationCreationException,
            DocumentAlreadyExistsException, DocumentNotFoundException {
        final byte[] contents = "The doc contents".getBytes();
        final String fileName = "testFile.txt";
        final String docName = "theDoc";
        final String contentMimeType = "plain/text";
        final Document doc = manager.createDocument(docName, new ProcessDefinitionUUID("a"), new ProcessInstanceUUID("b"), fileName, contentMimeType, contents);

        final Document newDoc = manager.createVersion(doc.getId(), true);

        assertNotNull(newDoc);
        assertEquals(null, newDoc.getContentFileName());
        assertEquals(null, manager.getContent(newDoc));
    }

    public void testCreateNewVersionOfDocumentWithSameName() throws Exception {
        final String fileName = "testFile.txt";
        final String docName = "theDoc";
        final String contentMimeType = "plain/text";
        final Document doc = manager.createDocument(docName, new ProcessDefinitionUUID("a"), new ProcessInstanceUUID("b"), fileName, contentMimeType,
                "The doc contents".getBytes());

        final Document newDoc = manager.createVersion(doc.getId(), true, "testFile2.txt", "plain/text", "The new doc contents".getBytes());

        assertNotNull(newDoc);
        assertEquals(doc.getName(), newDoc.getName());
        assertEquals(doc.getAuthor(), newDoc.getAuthor());
        assertEquals("testFile2.txt", newDoc.getContentFileName());
        assertEquals("plain/text", newDoc.getContentMimeType());
        assertEquals("The new doc contents", new String(manager.getContent(newDoc)));
    }

    public void testGetVersionsOfDocument() throws DocumentationCreationException, DocumentAlreadyExistsException, DocumentationCreationException,
            DocumentNotFoundException {
        final Folder folder = manager.createFolder("testGetOldVersionOfDocument");
        final String fileName = "testFile.txt";
        final String docName = "theDoc";
        final String contentMimeType = "plain/text";
        final Document doc = manager.createDocument(docName, folder.getId(), fileName, contentMimeType, "The doc contents".getBytes());

        final Document newDoc = manager.createVersion(doc.getId(), true, "testFile2.txt", "test/text", "The new doc contents".getBytes());

        final List<Document> versions = manager.getVersionsOfDocument(newDoc.getId());
        assertEquals(2, versions.size());
        assertEquals(newDoc, versions.get(0));

        assertTrue(doNotCheckId(doc, versions.get(1)));
    }

    /**
     * @param doc
     * @param document
     * @return
     */
    private boolean doNotCheckId(final Document doc, final Document obj) {

        if (doc == obj)
            return true;
        if (obj == null)
            return false;
        if (doc.getClass() != obj.getClass())
            return false;
        final DocumentImpl other = (DocumentImpl) obj;
        if (doc.getAuthor() == null) {
            if (other.getAuthor() != null)
                return false;
        } else if (!doc.getAuthor().equals(other.getAuthor()))
            return false;
        if (doc.getContentFileName() == null) {
            if (other.getContentFileName() != null)
                return false;
        } else if (!doc.getContentFileName().equals(other.getContentFileName()))
            return false;
        if (doc.getContentMimeType() == null) {
            if (other.getContentMimeType() != null)
                return false;
        } else if (!doc.getContentMimeType().equals(other.getContentMimeType()))
            return false;
        if (doc.getContentSize() != other.getContentSize())
            return false;
        if (doc.getCreationDate() == null) {
            if (other.getCreationDate() != null)
                return false;
        } else if (!doc.getCreationDate().equals(other.getCreationDate()))
            return false;
        if (doc.getParentFolderId() == null) {
            if (other.getParentFolderId() != null)
                return false;
        } else if (!doc.getParentFolderId().equals(other.getParentFolderId()))
            return false;
        if (doc.getLastModificationDate() == null) {
            if (other.getLastModificationDate() != null)
                return false;
        } else if (!doc.getLastModificationDate().equals(other.getLastModificationDate()))
            return false;
        if (doc.getLastModifiedBy() == null) {
            if (other.getLastModifiedBy() != null)
                return false;
        } else if (!doc.getLastModifiedBy().equals(other.getLastModifiedBy()))
            return false;
        if (doc.isLatestVersion() == other.isLatestVersion())
            return false;
        if (doc.isMajorVersion() != other.isMajorVersion())
            return false;
        if (doc.getName() == null) {
            if (other.getName() != null)
                return false;
        } else if (!doc.getName().equals(other.getName()))
            return false;
        if (doc.getVersionLabel() == null) {
            if (other.getVersionLabel() != null)
                return false;
        }
        if (doc.getVersionSeriesId() == null) {
            if (other.getVersionSeriesId() != null)
                return false;
        } else if (!doc.getVersionSeriesId().equals(other.getVersionSeriesId()))
            return false;
        return true;
    }

    public void testSearchDocumentByName() throws DocumentationCreationException, DocumentationCreationException, DocumentAlreadyExistsException {
        final Folder folder = manager.createFolder("testSearchDocumentById");
        manager.createDocument("theDoc1", folder.getId(), "testFile.txt", "plain/text", "The doc contents1".getBytes());
        final Document doc2 = manager.createDocument("theDoc2", folder.getId(), "testFile.txt", "plain/text", "The doc contents2".getBytes());
        manager.createDocument("theDoc3", folder.getId(), "testFile.txt", "plain/text", "The doc contents3".getBytes());
        final DocumentSearchBuilder searchBuilder = new DocumentSearchBuilder();
        searchBuilder.criterion(DocumentIndex.NAME).equalsTo("theDoc2");
        final SearchResult searchResult = manager.search(searchBuilder, 0, 10);
        assertEquals(1, searchResult.getCount());
        final List<Document> results = searchResult.getDocuments();
        assertEquals(1, results.size());
        assertEquals(doc2, results.get(0));
    }

    public void testSearchWithNoResults() throws DocumentationCreationException, DocumentationCreationException, DocumentAlreadyExistsException {
        final Folder folder = manager.createFolder("testSearchWithNoResults");
        manager.createDocument("theDoc1", folder.getId(), "testFile.txt", "plain/text", "The doc contents1".getBytes());
        manager.createDocument("theDoc2", folder.getId(), "testFile.txt", "plain/text", "The doc contents2".getBytes());
        manager.createDocument("theDoc3", folder.getId(), "testFile.txt", "plain/text", "The doc contents3".getBytes());
        final DocumentSearchBuilder searchBuilder = new DocumentSearchBuilder();
        searchBuilder.criterion(DocumentIndex.NAME).equalsTo("theDoc4");
        final SearchResult searchResult = manager.search(searchBuilder, 0, 10);
        assertEquals(0, searchResult.getCount());
        final List<Document> results = searchResult.getDocuments();
        assertEquals(0, results.size());
    }

    public void testSearchDocumentByCreationDate() throws DocumentationCreationException, DocumentationCreationException, DocumentAlreadyExistsException,
            InterruptedException {
        final Folder folder = manager.createFolder("testSearchDocumentById");

        final Document doc1 = manager.createDocument("theDoc1", folder.getId(), "testFile.txt", "plain/text", "The doc contents1".getBytes());
        final long diff = (new Date().getTime()) - doc1.getCreationDate().getTime();
        Thread.sleep(300);
        final Date date1 = new Date(new Date().getTime() - diff);
        Thread.sleep(300);
        final Document doc2 = manager.createDocument("theDoc2", folder.getId(), "testFile.txt", "plain/text", "The doc contents2".getBytes());
        Thread.sleep(300);
        final Date date2 = new Date(new Date().getTime() - diff);
        Thread.sleep(300);
        manager.createDocument("theDoc3", folder.getId(), "testFile.txt", "plain/text", "The doc contents3".getBytes());
        final DocumentSearchBuilder searchBuilder = new DocumentSearchBuilder();
        searchBuilder.criterion(DocumentIndex.CREATION_DATE).between(date1, date2);
        final SearchResult searchResult = manager.search(searchBuilder, 0, 10);
        assertEquals(1, searchResult.getCount());
        final List<Document> results = searchResult.getDocuments();
        assertEquals(1, results.size());
        assertEquals(doc2, results.get(0));
    }

    public void testSearchDocumentByProcessDefUUID() throws DocumentationCreationException, DocumentationCreationException, DocumentAlreadyExistsException,
            InterruptedException {
        final Folder folder = manager.createFolder("theProcessDefUUID");
        final Folder subFolder = manager.createFolder("theProcessInstUUID", folder.getId());
        final Document doc1 = manager.createDocument("theDoc1", subFolder.getId(), "testFile.txt", "plain/text", "The doc contents1".getBytes());
        final Document doc2 = manager.createDocument("theDoc2", subFolder.getId(), "testFile.txt", "plain/text", "The doc contents2".getBytes());
        final Document doc3 = manager.createDocument("theDoc3", subFolder.getId(), "testFile.txt", "plain/text", "The doc contents3".getBytes());
        final DocumentSearchBuilder searchBuilder = new DocumentSearchBuilder();
        searchBuilder.criterion(DocumentIndex.PROCESS_DEFINITION_UUID).equalsTo("theProcessDefUUID");
        final SearchResult searchResult = manager.search(searchBuilder, 0, 10);
        assertEquals(3, searchResult.getCount());
        final List<Document> results = searchResult.getDocuments();
        assertEquals(3, results.size());
        assertEquals(doc1, results.get(0));
        assertEquals(doc2, results.get(1));
        assertEquals(doc3, results.get(2));
    }

    public void testSearchDocumentByProcessInstUUID() throws DocumentationCreationException, DocumentationCreationException, DocumentAlreadyExistsException,
            InterruptedException {
        final Folder folder = manager.createFolder("theProcessDefUUID");
        final Folder subFolder = manager.createFolder("theProcessInstUUID", folder.getId());
        final Document doc1 = manager.createDocument("theDoc1", subFolder.getId(), "testFile.txt", "plain/text", "The doc contents1".getBytes());
        final Document doc2 = manager.createDocument("theDoc2", subFolder.getId(), "testFile.txt", "plain/text", "The doc contents2".getBytes());
        final Document doc3 = manager.createDocument("theDoc3", subFolder.getId(), "testFile.txt", "plain/text", "The doc contents3".getBytes());
        final DocumentSearchBuilder searchBuilder = new DocumentSearchBuilder();
        searchBuilder.criterion(DocumentIndex.PROCESS_INSTANCE_UUID).equalsTo("theProcessInstUUID");
        final SearchResult searchResult = manager.search(searchBuilder, 0, 10);
        assertEquals(3, searchResult.getCount());
        final List<Document> results = searchResult.getDocuments();
        assertEquals(3, results.size());
        assertEquals(doc1, results.get(0));
        assertEquals(doc2, results.get(1));
        assertEquals(doc3, results.get(2));
    }

    // not supported with not modified authenticator
    public void testSearchDocumentByAuthor() throws DocumentationCreationException, DocumentationCreationException, DocumentAlreadyExistsException,
            InterruptedException {
        final ProcessDefinitionUUID definitionUUID = new ProcessDefinitionUUID("AuthorProcess");
        final ProcessInstanceUUID instanceUUID = new ProcessInstanceUUID(definitionUUID, 1);
        final Document doc1 = manager.createDocument("theDoc1", definitionUUID, instanceUUID, "john", new Date(), "testFile.txt", "plain/text",
                "The doc contents1".getBytes());
        final Document doc2 = manager.createDocument("theDoc2", definitionUUID, instanceUUID, "john", new Date(), "testFile.txt", "plain/text",
                "The doc contents2".getBytes());
        final Document doc3 = manager.createDocument("theDoc3", definitionUUID, instanceUUID, "john", new Date(), "testFile.txt", "plain/text",
                "The doc contents3".getBytes());
        final DocumentSearchBuilder searchBuilder = new DocumentSearchBuilder();
        searchBuilder.criterion(DocumentIndex.AUTHOR).equalsTo("john");
        final SearchResult searchResult = manager.search(searchBuilder, 0, 10);
        assertEquals(3, searchResult.getCount());
        final List<Document> results = searchResult.getDocuments();
        assertEquals(3, results.size());
        assertEquals(doc1, results.get(0));
        assertEquals(doc2, results.get(1));
        assertEquals(doc3, results.get(2));
    }

    public void testSearchByProcessDefUUIDAndName() throws DocumentationCreationException, DocumentationCreationException, DocumentAlreadyExistsException,
            InterruptedException {
        final Folder proc1Folder = manager.createFolder("proc1");
        final Folder proc1InstFolder = manager.createFolder("inst1", proc1Folder.getId());
        final Folder proc2Folder = manager.createFolder("proc2");
        final Folder proc2InstFolder = manager.createFolder("inst1", proc2Folder.getId());
        manager.createDocument("theDoc1", proc1InstFolder.getId(), "testFile.txt", "plain/text", "The doc contents11".getBytes());
        manager.createDocument("theDoc2", proc1InstFolder.getId(), "testFile.txt", "plain/text", "The doc contents12".getBytes());
        manager.createDocument("theDoc1", proc2InstFolder.getId(), "testFile.txt", "plain/text", "The doc contents21".getBytes());
        final Document doc22 = manager.createDocument("theDoc2", proc2InstFolder.getId(), "testFile.txt", "plain/text", "The doc contents22".getBytes());
        final DocumentSearchBuilder searchBuilder = new DocumentSearchBuilder();
        searchBuilder.criterion(DocumentIndex.PROCESS_DEFINITION_UUID).equalsTo("proc2").and().criterion(DocumentIndex.NAME).equalsTo("theDoc2");
        final SearchResult searchResult = manager.search(searchBuilder, 0, 10);
        assertEquals(1, searchResult.getCount());
        final List<Document> results = searchResult.getDocuments();
        assertEquals(1, results.size());
        assertEquals(doc22, results.get(0));
    }

    // Not supported
    // public void testSearchOnAllVersion() throws DocumentationCreationException, DocumentationCreationException,
    // DocumentAlreadyExistsException,
    // InterruptedException{
    // Folder folder = manager.createFolder("allVersion");
    // Document document = manager.createDocument("theDoc1", folder.getId(), "testFile.txt", "plain/text",
    // -1, "The doc contents11".getBytes());
    // Document lastVersion = manager.createVersion(document.getId(), true);
    //
    // DocumentSearchBuilder searchBuilder = new DocumentSearchBuilder();
    // searchBuilder.criterion(DocumentIndex.PROCESS_DEFINITION_UUID).equalsTo("allVersion").allVersion();
    // SearchResult searchResult = manager.search(searchBuilder,0,10);
    // assertEquals(2, searchResult.getCount());
    // List<Document> results = searchResult.getDocuments();
    // assertEquals(2, results.size());
    // }

    public void testSearchOnLatestVersion() throws DocumentationCreationException, DocumentationCreationException, DocumentAlreadyExistsException,
            InterruptedException {
        final Folder folder = manager.createFolder("latestVersion");
        final Document document = manager.createDocument("theDoc1", folder.getId(), "testFile.txt", "plain/text", "The doc contents11".getBytes());
        final Document lastVersion = manager.createVersion(document.getId(), true);

        final DocumentSearchBuilder searchBuilder = new DocumentSearchBuilder();
        searchBuilder.criterion(DocumentIndex.PROCESS_DEFINITION_UUID).equalsTo("latestVersion").latestVersion();
        final SearchResult searchResult = manager.search(searchBuilder, 0, 10);
        assertEquals(1, searchResult.getCount());
        final List<Document> results = searchResult.getDocuments();
        assertEquals(1, results.size());
        assertEquals(lastVersion, results.get(0));
    }

    public void testCreationDate() throws DocumentAlreadyExistsException, DocumentationCreationException, InterruptedException {
        final ProcessDefinitionUUID definitionUUID = new ProcessDefinitionUUID("plop1");
        final Document document = manager.createDocument("myDocument", definitionUUID, new ProcessInstanceUUID(definitionUUID, 1));
        Thread.sleep(10);
        final long now = new Date().getTime();
        final long docTime = document.getCreationDate().getTime();
        final long abs = Math.abs(docTime - now);
        assertTrue("diff was " + abs, abs < 180000);
    }

    public void testEmptySearch() throws DocumentAlreadyExistsException, DocumentationCreationException, DocumentationCreationException {
        final SearchResult search = manager.search(new DocumentSearchBuilder(), 0, 100);
        assertEquals(0, search.getCount());
    }

    public void testSearchWithInClause() throws DocumentAlreadyExistsException, DocumentationCreationException, DocumentationCreationException {
        final Folder folder = manager.createFolder("theProcessDefUUID");
        final Folder subFolder = manager.createFolder("theProcessInstUUID", folder.getId());
        final Document doc1 = manager.createDocument("theDoc1", subFolder.getId(), "testFile.txt", "plain/text", "The doc contents1".getBytes());
        manager.createDocument("theDoc2", subFolder.getId(), "testFile.txt", "plain/text", "The doc contents2".getBytes());
        final Document doc3 = manager.createDocument("theDoc3", subFolder.getId(), "testFile.txt", "plain/text", "The doc contents3".getBytes());
        final DocumentSearchBuilder searchBuilder = new DocumentSearchBuilder();

        searchBuilder.criterion(DocumentIndex.ID).in(Arrays.asList(new String[] { doc1.getId(), doc3.getId() }));
        final SearchResult searchResult = manager.search(searchBuilder, 0, 10);
        assertEquals(2, searchResult.getCount());
        final List<Document> results = searchResult.getDocuments();
        assertEquals(2, results.size());
        assertEquals(doc1, results.get(0));
        assertEquals(doc3, results.get(1));
    }

    public void testCacheWorksIfFolderAreDeleted() throws DocumentAlreadyExistsException, DocumentationCreationException, DocumentationCreationException,
            DocumentNotFoundException {
        final ProcessDefinitionUUID definitionUUID = new ProcessDefinitionUUID("plop1");
        final ProcessInstanceUUID instanceUUID = new ProcessInstanceUUID(definitionUUID, 1);
        manager.createDocument("myDocument", definitionUUID, instanceUUID);
        // create a new service that delete all
        getDocumentationManager().clear();
        assertEquals(0, manager.getFolders(definitionUUID.getValue()).size());
        manager.createDocument("myDocument", definitionUUID, new ProcessInstanceUUID(definitionUUID, 1));// should work

    }

    public void testCreateVersionWithNonLatestDocument() throws DocumentAlreadyExistsException, DocumentationCreationException, DocumentationCreationException,
            DocumentNotFoundException {
        final ProcessDefinitionUUID definitionUUID = new ProcessDefinitionUUID("testCreateVersionWithNonLatestDocument");
        final ProcessInstanceUUID instanceUUID = new ProcessInstanceUUID(definitionUUID, 1);
        final String fileName = "testFile.txt";
        final String docName = "theDoc";
        final String contentMimeType = "plain/text";
        Document doc = manager.createDocument(docName, definitionUUID, instanceUUID, fileName, contentMimeType, "The doc contents".getBytes());

        final Document newDoc = manager.createVersion(doc.getId(), true, "testFile2.txt", "test/text", "The new doc contents".getBytes());

        final List<Document> versions = manager.getVersionsOfDocument(newDoc.getId());
        doc = versions.get(1);
        final Document createVersion = manager.createVersion(doc.getId(), true, "testFile3.txt", "test/text", "The new new doc contents".getBytes());

        assertEquals("The new new doc contents", new String(manager.getContent(createVersion)));
    }

    public void testSearchWithEmptyContent() throws DocumentAlreadyExistsException, DocumentationCreationException, DocumentationCreationException,
            DocumentNotFoundException {
        final ProcessDefinitionUUID definitionUUID = new ProcessDefinitionUUID("testCreateVersionWithNonLatestDocument");
        final ProcessInstanceUUID instanceUUID = new ProcessInstanceUUID(definitionUUID, 1);
        final String fileName = "testFile.txt";
        final String docName = "theDoc";
        final String contentMimeType = "plain/text";
        manager.createDocument(docName, definitionUUID, instanceUUID, fileName, contentMimeType, "The doc contents".getBytes());
        manager.createDocument("emptyDoc", definitionUUID, instanceUUID);

        final DocumentSearchBuilder builder = new DocumentSearchBuilder();
        builder.criterion(DocumentIndex.IS_EMPTY).equalsTo(true);

        final SearchResult search = manager.search(builder, 0, 10);
        assertEquals(1, search.getCount());
        assertEquals("emptyDoc", search.getDocuments().get(0).getName());

    }

    public void testSearchWithNonEmptyContent() throws DocumentAlreadyExistsException, DocumentationCreationException, DocumentationCreationException,
            DocumentNotFoundException {
        final ProcessDefinitionUUID definitionUUID = new ProcessDefinitionUUID("testCreateVersionWithNonLatestDocument");
        final ProcessInstanceUUID instanceUUID = new ProcessInstanceUUID(definitionUUID, 1);
        final String fileName = "testFile.txt";
        final String docName = "theDoc";
        final String contentMimeType = "plain/text";
        manager.createDocument(docName, definitionUUID, instanceUUID, fileName, contentMimeType, "The doc contents".getBytes());
        manager.createDocument("emptyDoc", definitionUUID, instanceUUID);

        final DocumentSearchBuilder builder = new DocumentSearchBuilder();
        builder.criterion(DocumentIndex.IS_EMPTY).equalsTo(false);

        final SearchResult search = manager.search(builder, 0, 10);
        assertEquals(1, search.getCount());
        assertEquals(docName, search.getDocuments().get(0).getName());

    }

    public void testSearchCount() throws DocumentAlreadyExistsException, DocumentationCreationException, DocumentationCreationException,
            DocumentNotFoundException {
        final ProcessDefinitionUUID definitionUUID = new ProcessDefinitionUUID("testSearchCount");
        final ProcessInstanceUUID instanceUUID = new ProcessInstanceUUID(definitionUUID, 1);
        manager.createDocument("emptyDoc1", definitionUUID, instanceUUID);
        manager.createDocument("emptyDoc2", definitionUUID, instanceUUID);
        manager.createDocument("emptyDoc3", definitionUUID, instanceUUID);
        manager.createDocument("emptyDoc4", definitionUUID, instanceUUID);
        manager.createDocument("emptyDoc5", definitionUUID, instanceUUID);
        manager.createDocument("emptyDoc6", definitionUUID, instanceUUID);

        final DocumentSearchBuilder builder = new DocumentSearchBuilder();
        builder.criterion(DocumentIndex.IS_EMPTY).equalsTo(true);

        final SearchResult search = manager.search(builder, 0, 3);
        assertEquals(6, search.getCount());

    }

    public void testSearchPaginated() throws DocumentAlreadyExistsException, DocumentationCreationException, DocumentationCreationException,
            DocumentNotFoundException {
        final ProcessDefinitionUUID definitionUUID = new ProcessDefinitionUUID("testSearchPaginated");
        final ProcessInstanceUUID instanceUUID = new ProcessInstanceUUID(definitionUUID, 1);
        manager.createDocument("emptyDoc1", definitionUUID, instanceUUID);
        manager.createDocument("emptyDoc2", definitionUUID, instanceUUID);
        manager.createDocument("emptyDoc3", definitionUUID, instanceUUID);
        manager.createDocument("emptyDoc4", definitionUUID, instanceUUID);
        manager.createDocument("emptyDoc5", definitionUUID, instanceUUID);
        manager.createDocument("emptyDoc6", definitionUUID, instanceUUID);

        final DocumentSearchBuilder builder = new DocumentSearchBuilder();
        builder.criterion(DocumentIndex.IS_EMPTY).equalsTo(true);

        SearchResult search = manager.search(builder, 0, 5);
        final List<Document> documents = search.getDocuments();
        assertEquals(5, documents.size());
        final Document d1 = documents.get(0);
        search = manager.search(builder, 2, 5);
        final List<Document> documents2 = search.getDocuments();
        assertEquals(4, documents2.size());
        final Document d2 = documents2.get(0);
        assertNotSame(d1.getName(), d2.getName());

    }

    public void testSearchEmptyResult() throws DocumentAlreadyExistsException, DocumentationCreationException, DocumentationCreationException,
            DocumentNotFoundException {
        final ProcessDefinitionUUID definitionUUID = new ProcessDefinitionUUID("testSearchEmptyResult");
        final ProcessInstanceUUID instanceUUID = new ProcessInstanceUUID(definitionUUID, 1);
        manager.createDocument("emptyDoc1", definitionUUID, instanceUUID);
        manager.createDocument("emptyDoc2", definitionUUID, instanceUUID);

        final DocumentSearchBuilder builder = new DocumentSearchBuilder();
        builder.criterion(DocumentIndex.NAME).equalsTo("kikoo");

        final SearchResult search = manager.search(builder, 0, 3);
        assertEquals(0, search.getCount());

    }

    public void testSearchOnDocumentsInMultiplesFolders() throws DocumentAlreadyExistsException, DocumentationCreationException,
            DocumentationCreationException, DocumentNotFoundException {
        final ProcessDefinitionUUID definitionUUID = new ProcessDefinitionUUID("testSearchOnDocumentsInMultiplesFolders");
        final ProcessInstanceUUID instanceUUID = new ProcessInstanceUUID(definitionUUID, 1);
        final ProcessDefinitionUUID otherDef = new ProcessDefinitionUUID("event");
        final ProcessInstanceUUID otherInst = new ProcessInstanceUUID(otherDef, 1);
        final Document document = manager.createDocument("emptyDoc1", definitionUUID, instanceUUID);
        manager.attachDocumentTo(otherDef, otherInst, document.getId());
        final DocumentSearchBuilder builder = new DocumentSearchBuilder();
        builder.criterion(DocumentIndex.PROCESS_INSTANCE_UUID).equalsTo(instanceUUID.getValue());

        final SearchResult search = manager.search(builder, 0, 5);
        final List<Document> documents = search.getDocuments();
        assertEquals(1, search.getCount());
        assertEquals(1, documents.size());
    }

    public void testGetDocumentsInMultiplesFolders() throws DocumentAlreadyExistsException, DocumentationCreationException, DocumentationCreationException,
            DocumentNotFoundException {
        final ProcessDefinitionUUID definitionUUID = new ProcessDefinitionUUID("testGetDocumentsInMultiplesFolders");
        final ProcessInstanceUUID instanceUUID = new ProcessInstanceUUID(definitionUUID, 1);
        final ProcessDefinitionUUID otherDef = new ProcessDefinitionUUID("sub");
        final ProcessInstanceUUID otherInst = new ProcessInstanceUUID(otherDef, 1);
        final Document document = manager.createDocument("emptyDoc1", definitionUUID, instanceUUID);
        manager.attachDocumentTo(otherDef, otherInst, document.getId());
        final DocumentSearchBuilder builder = new DocumentSearchBuilder();
        builder.criterion(DocumentIndex.PROCESS_INSTANCE_UUID).equalsTo(otherInst.getValue());

        final SearchResult search = manager.search(builder, 0, 5);
        final List<Document> documents = search.getDocuments();
        assertEquals(1, search.getCount());
        assertEquals(1, documents.size());
        assertEquals(otherInst, documents.get(0).getProcessInstanceUUID());// it get the last created folder as process
                                                                           // instance UUID
    }

    // public void testStress() throws DocumentAlreadyExistsException, DocumentationCreationException,
    // InterruptedException {
    // System.out.println("start");
    // int nbFiles = 0;
    // for (int i = 1; i < 10; i++) {
    //
    // System.out.println("-==== p "+i+"/10 ====-");
    // ProcessDefinitionUUID pdef = new ProcessDefinitionUUID("procName",String.valueOf(i));
    // long max = 15;
    // long max2 = 10;
    // for (int j = 1; j < max; j++) {
    // ProcessInstanceUUID idef = new ProcessInstanceUUID(pdef, j);
    // for (int k = 1; k < max2; k++) {
    // manager.createDocument("myDocument"+i+j+k, pdef, idef, "theFile"+i+j+k+".txt", "plain/text",
    // "fskqjsghnisrb,ùazel,f sdlkg,dlkgj aù dpfl;zaùfdskgdklmdslkngf dslkgn sdù akdgmqslgsd mskdgfd".getBytes());
    // nbFiles ++ ;
    // }
    // System.out.println("-==== "+nbFiles+" files ====-");
    // }
    // }
    // }

}
