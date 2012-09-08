/**
 * Copyright (C) 2009-2011 BonitaSoft S.A.
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

package org.bonitasoft.connectors.alfresco.common;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.ExtensibleElement;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.Response.ResponseType;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.runtime.AttachmentInstance;
import org.ow2.bonita.util.AccessorUtil;

/**
 * @author Jordi Anguela, Yanyan Liu
 */
public class AlfrescoRestClient {

    private static Logger LOGGER = Logger.getLogger(AlfrescoRestClient.class.getName());

    public static String NS_CMIS_RESTATOM = "http://docs.oasis-open.org/ns/cmis/restatom/200908/";

    public static String NS_CMIS_CORE = "http://docs.oasis-open.org/ns/cmis/core/200908/";

    public static String CMISRA = "cmisra";

    public static String CMIS = "cmis";

    private String server; // host + port

    private String username;

    private String password;

    public AlfrescoRestClient(String host, String port, String user, String passw) {
        this.username = user;
        this.password = passw;
        this.server = "http://" + host + ":" + port;
    }

    /**
     * STATUS 201 - Folder Created STATUS 500 - Server error (may be caused by: Folder already exists)
     * 
     * @param parentPath
     * @param newFoldersName
     * @param newFoldersDescription
     * @return AlfrescoResponse
     * @throws IOException
     */
    public AlfrescoResponse createFolderByPath(String parentPath, String newFoldersName, String newFoldersDescription) throws IOException {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("createFolderByPath parentPath=" + parentPath + " newFoldersName=" + newFoldersName + " newFoldersDescription=" + newFoldersDescription);
        }

        // Build the input Atom Entry
        Abdera abdera = new Abdera();
        Entry entry = abdera.newEntry();
        entry.setTitle(newFoldersName);
        entry.setSummary(newFoldersDescription);

        ExtensibleElement objElement = (ExtensibleElement) entry.addExtension(NS_CMIS_RESTATOM, "object", CMISRA);
        ExtensibleElement propsElement = objElement.addExtension(NS_CMIS_CORE, "properties", CMIS);
        ExtensibleElement stringElement = propsElement.addExtension(NS_CMIS_CORE, "propertyId", CMIS);
        stringElement.setAttributeValue("propertyDefinitionId", "cmis:objectTypeId");
        Element valueElement = stringElement.addExtension(NS_CMIS_CORE, "value", CMIS);
        valueElement.setText("cmis:folder");

        AbderaClient client = new AbderaClient();

        // Authentication header
        String encodedCredential = Base64Coder.encodeString((username + ":" + password));
        RequestOptions options = new RequestOptions();
        options.setHeader("Authorization", "Basic " + encodedCredential);

        String uri = server + "/alfresco/s/cmis/p" + parentPath + "/children";
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("POST " + uri);
        }

        ClientResponse clientResponse = client.post(uri, entry, options);
        AlfrescoResponse alfResponse = parseResponseWithDocument(clientResponse);
        clientResponse.release();
        return alfResponse;
    }

    /**
     * STATUS 200: SUCCESS
     * 
     * @param folderPath
     * @return AlfrescoResponse
     * @throws IOException
     */
    public AlfrescoResponse listFolderByPath(String folderPath) throws IOException {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("listFolderByPath folderPath=" + folderPath);
        }

        AbderaClient client = new AbderaClient();

        // Authentication header
        String encodedCredential = Base64Coder.encodeString((username + ":" + password));
        RequestOptions options = new RequestOptions();
        options.setHeader("Authorization", "Basic " + encodedCredential);

        String uri = server + "/alfresco/s/cmis/p" + folderPath + "/children";
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("GET " + uri);
        }

        ClientResponse clientResponse = client.get(uri, options);
        AlfrescoResponse alfResponse = parseResponseWithDocument(clientResponse);
        clientResponse.release();
        return alfResponse;
    }

    /**
     * STATUS 204 : SUCCESS STATUS 404 : CLIENT_ERROR - Not found
     * 
     * @param folderPath
     * @return AlfrescoResponse
     * @throws IOException
     */
    public AlfrescoResponse deleteFolderByPath(String folderPath) throws IOException {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("deleteFolderByPath folderPath=" + folderPath);
        }

        // use recursive algorithm to delete subFolder
        AlfrescoResponse response = listFolderByPath(folderPath);
        if (ResponseType.SUCCESS.toString().equals(response.getResponseType())) {
            Document<Element> doc = response.getDocument();
            Feed feed = (Feed) doc.getRoot();
            for (Entry entry : feed.getEntries()) {
                String title = entry.getTitle();
                String subFolderPath = folderPath + "/" + title;
                deleteFolderByPath(subFolderPath);
            }
        } else {
            throw new IOException();
        }

        AbderaClient client = new AbderaClient();

        // Authentication header
        String encodedCredential = Base64Coder.encodeString((username + ":" + password));
        RequestOptions options = new RequestOptions();
        options.setHeader("Authorization", "Basic " + encodedCredential);

        String uri = server + "/alfresco/s/cmis/p" + folderPath;
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("DELETE " + uri);
        }

        ClientResponse clientResponse = client.delete(uri, options);
        AlfrescoResponse alfResponse = parseResponse(clientResponse);
        clientResponse.release();
        return alfResponse;
    }

    /**
     * STATUS 200 : SUCCESS STATUS 404 : CLIENT_ERROR - Not found
     * 
     * @param fileId
     * @param outputFileFolder
     * @param outputFileName
     * @return AlfrescoResponse
     * @throws IOException
     */
    public AlfrescoResponse downloadFileById(String fileId, String outputFileFolder, String outputFileName) throws IOException {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("downloadFileById fileId=" + fileId + " outputFileFolder=" + outputFileFolder + " outputFileName=" + outputFileName);
        }

        AbderaClient client = new AbderaClient();

        // Authentication header
        String encodedCredential = Base64Coder.encodeString((username + ":" + password));
        RequestOptions options = new RequestOptions();
        options.setHeader("Authorization", "Basic " + encodedCredential);

        String uri = server + "/alfresco/service/cmis/i/" + fileId + "/content";
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("GET " + uri);
        }

        ClientResponse clientResponse = client.get(uri, options);
        AlfrescoResponse alfResponse = parseResponseAsOutputFile(clientResponse, outputFileFolder, outputFileName);
        clientResponse.release();
        return alfResponse;
    }

    /**
     * STATUS 200: SUCCESS STATUS 404 : CLIENT_ERROR - Not found
     * 
     * @param fileId
     * @param outputFileFolder
     * @param outputFileName
     * @return AlfrescoResponse
     * @throws IOException
     */
    public AlfrescoResponse downloadFileByStoreAndId(String store, String fileId, String outputFileFolder, String outputFileName) throws IOException {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("downloadFileByStoreAndId store=" + store + " fileId=" + fileId + " outputFileFolder=" + outputFileFolder + " outputFileName="
                    + outputFileName);
        }

        AbderaClient client = new AbderaClient();

        // Authentication header
        String encodedCredential = Base64Coder.encodeString((username + ":" + password));
        RequestOptions options = new RequestOptions();
        options.setHeader("Authorization", "Basic " + encodedCredential);

        String uri = server + "/alfresco/service/cmis/s/" + store + "/i/" + fileId + "/content";
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("GET " + uri);
        }

        ClientResponse clientResponse = client.get(uri, options);
        AlfrescoResponse alfResponse = parseResponseAsOutputFile(clientResponse, outputFileFolder, outputFileName);
        clientResponse.release();
        return alfResponse;
    }

    /**
     * Uploads the file at the given path in Alfresco
     * STATUS 201 : Created STATUS 404 : Not found - May be caused by: Destination directory not found STATUS 500 : Internal
     * server error - May be caused by: File already exist
     * 
     * @param fileAbsolutePath
     * @param description
     * @param mimeType
     * @param destinationFolder
     * @return AlfrescoResponse
     * @throws IOException
     */
    public AlfrescoResponse uploadFileByPath(String fileAbsolutePath, String fileName, String description, String mimeType, String destinationFolder)
            throws IOException {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("uploadFileByPath fileAbsolutePath=" + fileAbsolutePath + " fileName=" + fileName + " description=" + description + " mimeType="
                    + mimeType + " destinationFolder=" + destinationFolder);
        }
        if ("text/plain".equalsIgnoreCase(mimeType)) {
            // Get file content
            File fileToUpload = new File(fileAbsolutePath);
            byte[] fileBytes = getBytesFromFile(fileToUpload);
            // Upload file
            return uploadFile(fileBytes, fileName, description, mimeType, destinationFolder);
        } else {
            // Upload stream
            FileInputStream inputStream = new FileInputStream(fileAbsolutePath);
            return uploadFile(inputStream, fileName, description, mimeType, destinationFolder);
        }

    }

    /**
     * Uploads the given AttachmentInstance in Alfresco
     * STATUS 201 : Created STATUS 404 : Not found - May be caused by: Destination directory not found STATUS 500 : Internal
     * server error - May be caused by: File already exist
     * 
     * @param attachment
     * @param description
     * @param mimeType
     * @param destinationFolder
     * @return AlfrescoResponse
     * @throws IOException
     */
    public AlfrescoResponse uploadFileFromAttachment(AttachmentInstance attachment, String fileName, String description, String mimeType,
            String destinationFolder) throws IOException {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("uploadFileFromAttachment attachmentFileName=" + attachment.getFileName() + " fileName=" + fileName + " description=" + description
                    + " mimeType=" + mimeType + " destinationFolder=" + destinationFolder);
        }

        // Get file content
        QueryRuntimeAPI queryRuntimeAPI = AccessorUtil.getQueryRuntimeAPI();
        byte[] fileBytes = queryRuntimeAPI.getAttachmentValue(attachment);
        if ("text/plain".equalsIgnoreCase(mimeType)) {
            // Upload file
            return uploadFile(fileBytes, fileName, description, mimeType, destinationFolder);
        } else {
            // Upload stream
            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
            return uploadFile(inputStream, fileName, description, mimeType, destinationFolder);
        }
    }

    /**
     * Uploads the given Document in Alfresco
     * STATUS 201 : Created STATUS 404 : Not found - May be caused by: Destination directory not found STATUS 500 : Internal
     * server error - May be caused by: File already exist
     * 
     * @param document
     * @param description
     * @param mimeType
     * @param destinationFolder
     * @return AlfrescoResponse
     * @throws IOException
     * @throws Exception
     */
    public AlfrescoResponse uploadFileFromDocument(org.ow2.bonita.facade.runtime.Document document, String fileName, String description, String mimeType,
            String destinationFolder) throws IOException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("uploadFileFromDocument documentName=" + document.getName() + " fileName=" + fileName + " description=" + description + " mimeType="
                    + mimeType + " destinationFolder=" + destinationFolder);
        }

        // Get file content
        QueryRuntimeAPI queryRuntimeAPI = AccessorUtil.getQueryRuntimeAPI();
        byte[] fileBytes = queryRuntimeAPI.getDocumentContent(document.getUUID());

        if ("text/plain".equalsIgnoreCase(mimeType)) {
            // Upload file
            return uploadFile(fileBytes, fileName, description, mimeType, destinationFolder);
        } else {
            // Upload stream
            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
            return uploadFile(inputStream, fileName, description, mimeType, destinationFolder);
        }

    }

    /**
     * upload non test/plain MimeType file
     * 
     * @param inputStream
     * @param fileName
     * @param description
     * @param mimeType
     * @param destinationFolder
     * @return
     */
    private AlfrescoResponse uploadFile(InputStream inputStream, String fileName, String description, String mimeType, String destinationFolder) {
        // Build the input Atom Entry
        Abdera abdera = new Abdera();
        Entry entry = abdera.newEntry();
        entry.setTitle(fileName);
        entry.setSummary(description);
        entry.setContent(inputStream, mimeType);

        ExtensibleElement objElement = (ExtensibleElement) entry.addExtension(NS_CMIS_CORE, "object", CMIS);
        ExtensibleElement propsElement = objElement.addExtension(NS_CMIS_CORE, "properties", CMIS);
        ExtensibleElement stringElement = propsElement.addExtension(NS_CMIS_CORE, "propertyId", CMIS);
        stringElement.setAttributeValue("cmis:name", "ObjectTypeId");
        Element valueElement = stringElement.addExtension(NS_CMIS_CORE, "value", CMIS);
        valueElement.setText("document"); // This could be changed as an input parameter

        AbderaClient client = new AbderaClient();

        // Authentication header
        String encodedCredential = Base64Coder.encodeString((username + ":" + password));
        RequestOptions options = new RequestOptions();
        options.setHeader("Authorization", "Basic " + encodedCredential);

        String uri = server + "/alfresco/s/cmis/p" + destinationFolder + "/children";
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("POST " + uri);
        }

        ClientResponse clientResponse = client.post(uri, entry, options);
        AlfrescoResponse alfResponse = parseResponseWithDocument(clientResponse);
        clientResponse.release();
        return alfResponse;
    }

    /**
     * Uploads the given file bytes to an Alfresco file<br/>
     * STATUS 201 : Created STATUS 404 : Not found - May be caused by: Destination directory not found STATUS 500 : Internal
     * server error - May be caused by: File already exist
     * 
     * @param fileBytes
     * @param fileName
     * @param description
     * @param mimeType
     * @param destinationFolder
     * @return
     * @throws IOException
     */
    private AlfrescoResponse uploadFile(byte[] fileBytes, String fileName, String description, String mimeType, String destinationFolder) throws IOException {
        // String encodedFileString = new String(fileBytes);
        String encodedFileString = null;
        if (fileBytes != null) {
            encodedFileString = new String(fileBytes);
        }
        // Build the input Atom Entry
        Abdera abdera = new Abdera();
        Entry entry = abdera.newEntry();
        entry.setTitle(fileName);
        entry.setSummary(description);
        entry.setContent(encodedFileString, mimeType);

        ExtensibleElement objElement = (ExtensibleElement) entry.addExtension(NS_CMIS_CORE, "object", CMIS);
        ExtensibleElement propsElement = objElement.addExtension(NS_CMIS_CORE, "properties", CMIS);
        ExtensibleElement stringElement = propsElement.addExtension(NS_CMIS_CORE, "propertyId", CMIS);
        stringElement.setAttributeValue("cmis:name", "ObjectTypeId");
        Element valueElement = stringElement.addExtension(NS_CMIS_CORE, "value", CMIS);
        valueElement.setText("document"); // This could be changed as an input parameter

        AbderaClient client = new AbderaClient();

        // Authentication header
        String encodedCredential = Base64Coder.encodeString((username + ":" + password));
        RequestOptions options = new RequestOptions();
        options.setHeader("Authorization", "Basic " + encodedCredential);

        String uri = server + "/alfresco/s/cmis/p" + destinationFolder + "/children";
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("POST " + uri);
        }

        ClientResponse clientResponse = client.post(uri, entry, options);
        AlfrescoResponse alfResponse = parseResponseWithDocument(clientResponse);
        clientResponse.release();
        return alfResponse;
    }

    /**
     * STATUS 204 : SUCCESS STATUS 404 : CLIENT_ERROR - Not found
     * 
     * @param itemId
     *            if a folderId is specified it also deletes its contents
     * @return AlfrescoResponse
     * @throws IOException
     */
    public AlfrescoResponse deleteItemById(String itemId) throws IOException {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("deleteItemById itemId=" + itemId);
        }

        AbderaClient client = new AbderaClient();

        // Authentication header
        String encodedCredential = Base64Coder.encodeString((username + ":" + password));
        RequestOptions options = new RequestOptions();
        options.setHeader("Authorization", "Basic " + encodedCredential);

        String uri = server + "/alfresco/s/cmis/i/" + itemId;
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("DELETE " + uri);
        }

        ClientResponse clientResponse = client.delete(uri, options);
        AlfrescoResponse alfResponse = parseResponse(clientResponse);
        clientResponse.release();
        return alfResponse;
    }

    /**
     * STATUS 201 : Created - (working copy created) STATUS 404 : CLIENT_ERROR - File not found STATUS 400 : CLIENT_ERROR - Bad
     * request, file already checked-out
     * 
     * @param fileId
     * @return AlfrescoResponse
     * @throws IOException
     */
    public AlfrescoResponse checkout(String fileId) throws IOException {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("checkout fileId=" + fileId);
        }

        // Build the input Atom Entry
        Abdera abdera = new Abdera();
        Entry entry = abdera.newEntry();

        ExtensibleElement objElement = (ExtensibleElement) entry.addExtension(NS_CMIS_RESTATOM, "object", CMISRA);
        ExtensibleElement propsElement = objElement.addExtension(NS_CMIS_CORE, "properties", CMIS);
        ExtensibleElement stringElement = propsElement.addExtension(NS_CMIS_CORE, "propertyId", CMIS);
        stringElement.setAttributeValue("propertyDefinitionId", "cmis:objectId");
        Element valueElement = stringElement.addExtension(NS_CMIS_CORE, "value", CMIS);
        valueElement.setText("workspace://SpacesStore/" + fileId);

        // Post it
        AbderaClient client = new AbderaClient();

        // Authentication header
        String encodedCredential = Base64Coder.encodeString((username + ":" + password));
        RequestOptions options = new RequestOptions();
        options.setHeader("Authorization", "Basic " + encodedCredential);

        String uri = server + "/alfresco/s/cmis/checkedout";
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("POST " + uri);
        }

        ClientResponse clientResponse = client.post(uri, entry, options);
        AlfrescoResponse alfResponse = parseResponseWithDocument(clientResponse);
        clientResponse.release();
        return alfResponse;
    }

    /**
     * STATUS 200: SUCCESS
     * 
     * @return AlfrescoResponse
     * @throws IOException
     */
    public AlfrescoResponse listCheckedOutFiles() throws IOException {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("listCheckedOutFiles");
        }

        AbderaClient client = new AbderaClient();

        // Authentication header
        String encodedCredential = Base64Coder.encodeString((username + ":" + password));
        RequestOptions options = new RequestOptions();
        options.setHeader("Authorization", "Basic " + encodedCredential);

        String uri = server + "/alfresco/s/cmis/checkedout";
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("GET " + uri);
        }

        ClientResponse clientResponse = client.get(uri, options);
        AlfrescoResponse alfResponse = parseResponseWithDocument(clientResponse);
        clientResponse.release();
        return alfResponse;
    }

    /**
     * STATUS 204 : SUCCESS STATUS 404 : CLIENT_ERROR - File not found STATUS 500 : SERVER_ERROR - (may be caused because the
     * file is not checked-out)
     * 
     * @param fileId
     * @return AlfrescoResponse
     * @throws IOException
     */
    public AlfrescoResponse cancelCheckout(String fileId) throws IOException {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("cancelCheckout fileId=" + fileId);
        }

        AbderaClient client = new AbderaClient();

        // Authentication header
        String encodedCredential = Base64Coder.encodeString((username + ":" + password));
        RequestOptions options = new RequestOptions();
        options.setHeader("Authorization", "Basic " + encodedCredential);

        String uri = server + "/alfresco/s/cmis/pwc/i/" + fileId;
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("DELETE " + uri);
        }

        ClientResponse clientResponse = client.delete(uri, options);
        AlfrescoResponse alfResponse = parseResponse(clientResponse);
        clientResponse.release();
        return alfResponse;
    }

    /**
     * STATUS 200 : SUCCESS STATUS 500 : Internal server error - May be caused by: Duplicate child name not allowed (you are
     * trying to use the original file name instead of the checked-out file name) Example: original file name: demo.txt
     * checked-out file name: demo (Working copy).txt
     * 
     * @param fileAbsolutePath
     * @param description
     * @param mimeType
     * @param checkedOutFileId
     * @return AlfrescoResponse
     * @throws IOException
     */
    public AlfrescoResponse updateCheckedOutFile(String fileAbsolutePath, String description, String mimeType, String checkedOutFileId) throws IOException {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("updateCheckedOutFile fileAbsolutePath=" + fileAbsolutePath + " description=" + description + " mimeType=" + mimeType
                    + " checkedOutFileId=" + checkedOutFileId);
        }

        File fileToUpload = new File(fileAbsolutePath);

        byte[] fileBytes = getBytesFromFile(fileToUpload);
        // char[] encodedFile = Base64Coder.encode(fileBytes); This will cause messy code issue.
        String encodedFileString = new String(fileBytes);

        String fileName = fileToUpload.getName();

        // Build the input Atom Entry
        Abdera abdera = new Abdera();
        Entry entry = abdera.newEntry();
        entry.setTitle(fileName);
        entry.setSummary(description);
        entry.setContent(encodedFileString, mimeType);

        ExtensibleElement objElement = (ExtensibleElement) entry.addExtension(NS_CMIS_CORE, "object", CMIS);
        ExtensibleElement propsElement = objElement.addExtension(NS_CMIS_CORE, "properties", CMIS);
        ExtensibleElement stringElement = propsElement.addExtension(NS_CMIS_CORE, "propertyId", CMIS);
        stringElement.setAttributeValue("cmis:name", "ObjectTypeId");
        Element valueElement = stringElement.addExtension(NS_CMIS_CORE, "value", CMIS);
        valueElement.setText("document"); // This could be changed as an input parameter

        AbderaClient client = new AbderaClient();

        // Authentication header
        String encodedCredential = Base64Coder.encodeString((username + ":" + password));
        RequestOptions options = new RequestOptions();
        options.setHeader("Authorization", "Basic " + encodedCredential);

        String uri = server + "/alfresco/s/cmis/i/" + checkedOutFileId;
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("PUT " + uri);
        }

        ClientResponse clientResponse = client.put(uri, entry, options);
        AlfrescoResponse alfResponse = parseResponseWithDocument(clientResponse);
        clientResponse.release();
        return alfResponse;
    }

    /**
     * STATUS 200 : SUCCESS STATUS 404 : CLIENT_ERROR - File not found
     * 
     * @param checkedOutFileId
     * @param isMajorVersion
     * @param checkinComments
     * @return AlfrescoResponse
     * @throws IOException
     */
    public AlfrescoResponse checkin(String checkedOutFileId, boolean isMajorVersion, String checkinComments) throws IOException {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("checkin checkedOutFileId=" + checkedOutFileId + " isMajorVersion=" + isMajorVersion + " checkinComments=" + checkinComments);
        }

        // Replace white spaces from the URI
        checkinComments = checkinComments.replace(" ", "%20");

        // Create an empty Atom Entry
        Abdera abdera = new Abdera();
        Entry entry = abdera.newEntry();

        AbderaClient client = new AbderaClient();

        // Authentication header
        String encodedCredential = Base64Coder.encodeString((username + ":" + password));
        RequestOptions options = new RequestOptions();
        options.setHeader("Authorization", "Basic " + encodedCredential);

        String uri = server + "/alfresco/s/cmis/pwc/i/" + checkedOutFileId + "?checkin=true&major=" + isMajorVersion + "&checkinComment=" + checkinComments;
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("PUT " + uri);
        }

        ClientResponse clientResponse = client.put(uri, entry, options);
        AlfrescoResponse alfResponse = parseResponseWithDocument(clientResponse);
        clientResponse.release();
        return alfResponse;
    }

    /**
     * STATUS 200: SUCCESS STATUS 404 : CLIENT_ERROR - File not found
     * 
     * @param fileId
     * @return AlfrescoResponse
     * @throws IOException
     */
    public AlfrescoResponse fileVersions(String fileId) throws IOException {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("fileVersions fileId=" + fileId);
        }

        AbderaClient client = new AbderaClient();

        // Authentication header
        String encodedCredential = Base64Coder.encodeString((username + ":" + password));
        RequestOptions options = new RequestOptions();
        options.setHeader("Authorization", "Basic " + encodedCredential);

        String uri = server + "/alfresco/s/cmis/i/" + fileId + "/versions";
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("GET " + uri);
        }

        ClientResponse clientResponse = client.get(uri, options);
        AlfrescoResponse alfResponse = parseResponseWithDocument(clientResponse);
        clientResponse.release();
        return alfResponse;
    }

    /**
     * Parse ClientResponse
     * 
     * @param response
     * @return AlfrescoResponse
     */
    private AlfrescoResponse parseResponse(ClientResponse response) {

        AlfrescoResponse alfResponse;

        String responseType = "";
        if (response.getType() != null) {
            responseType = response.getType().toString();
        }
        String statusCode = String.valueOf(response.getStatus());
        String statusText = response.getStatusText();

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Response type : " + responseType);
            LOGGER.info("Status code is: " + statusCode);
            LOGGER.info("Status text is: " + statusText);
        }

        alfResponse = new AlfrescoResponse(responseType, statusCode, statusText);

        if (ResponseType.SUCCESS == response.getType()) {
            // Do nothing
        } else {
            // printStackTrace
            InputStream inputStream;
            try {
                inputStream = response.getInputStream();

                final char[] buffer = new char[0x10000];
                StringBuilder stackTrace = new StringBuilder();
                Reader in = new InputStreamReader(inputStream, "UTF-8");
                int read;
                do {
                    read = in.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        stackTrace.append(buffer, 0, read);
                    }
                } while (read >= 0);
                inputStream.close();

                alfResponse.setStackTrace(stackTrace.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return alfResponse;
    }

    /**
     * Parse ClientResponse
     * 
     * @param response
     * @return AlfrescoResponse
     */
    @SuppressWarnings("unchecked")
    private AlfrescoResponse parseResponseWithDocument(ClientResponse response) {

        AlfrescoResponse alfResponse = parseResponse(response);

        if (ResponseType.SUCCESS == response.getType()) {
            Document<Element> document = response.getDocument();
            if (document != null) {
                alfResponse.setDocument((Document<Element>) document.clone());
            }
        }
        return alfResponse;
    }

    /**
     * Parse ClientResponse
     * 
     * @param response
     * @return
     * @throws IOException
     */
    private AlfrescoResponse parseResponseAsOutputFile(ClientResponse response, String outputFileFolder, String outputFileName) throws IOException {

        AlfrescoResponse alfResponse = parseResponse(response);

        if (ResponseType.SUCCESS == response.getType()) {
            if (response.getContentLength() > 0) {
                InputStream inputStream = response.getInputStream();
                File responseFile = new File(outputFileFolder + outputFileName);
                OutputStream outputStream = new FileOutputStream(responseFile);
                byte buf[] = new byte[1024];
                int len;
                while ((len = inputStream.read(buf)) > 0) {
                    outputStream.write(buf, 0, len);
                }
                outputStream.close();
                inputStream.close();
            }
        }
        return alfResponse;
    }

    private static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);

        // Get the size of the file
        long length = file.length();

        if (length > Integer.MAX_VALUE) {
            // File is too large
            throw new IOException("File too long");
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int) length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        // Close the input stream and return bytes
        is.close();
        return bytes;
    }

}
