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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.ow2.bonita.facade.exception.DocumentAlreadyExistsException;
import org.ow2.bonita.facade.exception.DocumentNotFoundException;
import org.ow2.bonita.facade.exception.DocumentationCreationException;
import org.ow2.bonita.facade.exception.FolderAlreadyExistsException;
import org.ow2.bonita.facade.impl.SearchResult;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.search.DocumentCriterion;
import org.ow2.bonita.search.DocumentSearchBuilder;
import org.ow2.bonita.services.CmisUserProvider;
import org.ow2.bonita.services.Document;
import org.ow2.bonita.services.DocumentationManager;
import org.ow2.bonita.util.BonitaConstants;
import org.ow2.bonita.util.BonitaRuntimeException;
import org.ow2.bonita.util.EnvTool;
import org.ow2.bonita.util.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CMIS implementation of the document manager
 * 
 * @author Baptiste Mesta, Christophe Leroy
 */
public class CMISDocumentManager implements DocumentationManager {

    
    /**
     * @author Baptiste Mesta
     *
     */
    protected abstract class CacheErrorRetrier<T> {

        /**
         * @param subFolder
         * @return
         * @throws DocumentationCreationException
         */
        public abstract T doIt(String subFolder) throws DocumentationCreationException;

        /**
         * @param session
         * @param definitionUUID
         * @param object
         * @return
         * @throws DocumentationCreationException 
         */
        public T execute(Session session, ProcessDefinitionUUID definitionUUID, String subFolderName) throws DocumentationCreationException {
            try {
                String subFolderId = createPath(session, definitionUUID, subFolderName);
                return doIt(subFolderId);
            } catch (DocumentAlreadyExistsException e) {
                throw e;
            } catch (Exception e) {
                clearCache();
                String subFolderId = createPath(session, definitionUUID, subFolderName);
                return doIt(subFolderId);
            }
        }

    }

    private static ThreadLocal<SimpleDateFormat> CMIS_DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {

        @Override
        protected synchronized SimpleDateFormat initialValue() {
            final SimpleDateFormat gmtTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
            gmtTime.setTimeZone(TimeZone.getTimeZone("GMT"));
            return gmtTime;
        }

    };

    private final String binding;

    private final String url;

    private final String repositoryId;

    private String rootFolderId;

    private final Map<String, Session> sessionsMap = new HashMap<String, Session>();

    private final Map<ProcessDefinitionUUID, String> processDefinitionMap = new HashMap<ProcessDefinitionUUID, String>();

    private final Map<String, String> subFolderMap = new HashMap<String, String>();

    private final Map<String, String> processInstanceMap = new HashMap<String, String>();

    private final CmisUserProvider userProvider;

    private static final Logger LOGGER = LoggerFactory.getLogger(CMISDocumentManager.class);

    private final String pathOfRootFolder;

    private final Comparator<Folder> comparator = new Comparator<Folder>() {

        @Override
        public int compare(final Folder o1, final Folder o2) {
            return o2.getCreationDate().compareTo(o1.getCreationDate());
        }
    };

    public CMISDocumentManager(final String binding, final String url, final String repositoryId, final CmisUserProvider userProvider) {
        this(binding, url, repositoryId, userProvider, "/");
    }

    public CMISDocumentManager(final String binding, final String url, final String repositoryId, final CmisUserProvider userProvider,
            final String pathOfRootFolder) {
        this.binding = binding;
        this.url = url;
        this.repositoryId = repositoryId;
        this.userProvider = userProvider;
        this.pathOfRootFolder = pathOfRootFolder;
        final AuthAwareCookieManager cm = new AuthAwareCookieManager(null, java.net.CookiePolicy.ACCEPT_ALL);
        java.net.CookieHandler.setDefault(cm);
    }

    public synchronized Session createSessionById(final String repositoryId, final String userId2) {
        final SessionFactory f = SessionFactoryImpl.newInstance();
        String userId;
        if (userId2 == null) {
            userId = BonitaConstants.SYSTEM_USER;
            try {
                userId = EnvTool.getUserId();
            } catch (final Exception e) {
            }
        } else {
            userId = userId2;
        }
        Session session = sessionsMap.get(userId);
        if (session == null) {
            final Map<String, String> parameter = fixParameters(userProvider.getUser(userId), userProvider.getPassword(userId));
            parameter.put(SessionParameter.REPOSITORY_ID, repositoryId);
            session = f.createSession(parameter);
            if (rootFolderId == null) {
                final CmisObject rootFolder = session.getObjectByPath(pathOfRootFolder);
                rootFolderId = rootFolder.getId();
            }
            sessionsMap.put(userId, session);
        }
        return session;
    }

    public synchronized Session getSession() {
        return createSessionById(repositoryId, null);
    }

    public synchronized Session getSession(final String userId) {
        return createSessionById(repositoryId, userId);
    }

    protected Map<String, String> fixParameters(final String username, final String password) {
        final Map<String, String> parameter = new HashMap<String, String>();

        if ("ATOM".equals(binding)) {
            parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
        } else if (binding.toLowerCase().startsWith("WebService".toLowerCase())) {
            parameter.put(SessionParameter.WEBSERVICES_ACL_SERVICE, url + "/ACLService/AccessControlServicePort?wsdl");
            parameter.put(SessionParameter.WEBSERVICES_DISCOVERY_SERVICE, url + "/DiscoveryService/DiscoveryServicePort?wsdl");
            parameter.put(SessionParameter.WEBSERVICES_MULTIFILING_SERVICE, url + "/MultiFilingService/MultiFilingServicePort?wsdl");
            parameter.put(SessionParameter.WEBSERVICES_NAVIGATION_SERVICE, url + "/NavigationService/NavigationServicePort?wsdl");
            parameter.put(SessionParameter.WEBSERVICES_OBJECT_SERVICE, url + "/ObjectService/ObjectServicePort?wsdl");
            parameter.put(SessionParameter.WEBSERVICES_POLICY_SERVICE, url + "/PolicyService/PolicyServicePort?wsdl");
            parameter.put(SessionParameter.WEBSERVICES_RELATIONSHIP_SERVICE, url + "/RelationshipService/RelationshipServicePort?wsdl");
            parameter.put(SessionParameter.WEBSERVICES_REPOSITORY_SERVICE, url + "/RepositoryService/RepositoryServicePort?wsdl");
            parameter.put(SessionParameter.WEBSERVICES_VERSIONING_SERVICE, url + "/VersioningService/VersioningServicePort?wsdl");
            parameter.put(SessionParameter.AUTH_HTTP_BASIC, "false");
            parameter.put(SessionParameter.AUTH_SOAP_USERNAMETOKEN, "false");
            parameter.put(SessionParameter.BINDING_TYPE, BindingType.WEBSERVICES.value());
        }
        parameter.put(SessionParameter.ATOMPUB_URL, url);
        parameter.put(SessionParameter.USER, username);
        parameter.put(SessionParameter.PASSWORD, password);
        parameter.put(SessionParameter.CACHE_SIZE_OBJECTS, "0");
        parameter.put(SessionParameter.AUTHENTICATION_PROVIDER_CLASS, StandardAuthenticationProviderWithUserInHeaders.class.getName());
        return parameter;
    }

    @Override
    public org.ow2.bonita.services.Folder createFolder(final String folderName) throws FolderAlreadyExistsException {
        try {
            return createFolder(getSession(), folderName);
        } catch (final CmisRuntimeException e) {
            throw new FolderAlreadyExistsException(folderName, e);
        }
    }

    private org.ow2.bonita.services.Folder createFolder(final Session session, final String folderName) throws FolderAlreadyExistsException {
        try {
            session.getObject(session.createObjectId(rootFolderId));
        } catch (final CmisObjectNotFoundException e) {
            // we don't have the good id anymore (because of a hard reset of the
            // cmis server and because of bonita was not restarted)
            rootFolderId = session.getObjectByPath(pathOfRootFolder).getId();
        }
        ObjectId createFolder = createFolder(session, folderName, rootFolderId);
        return convertFolder((Folder) session.getObject(createFolder));
    }

    private ObjectId createFolder(final Session session, final String folderName, final String parentFolderId)
            throws FolderAlreadyExistsException {
        final HashMap<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.NAME, folderName);
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
        try {
            ObjectId createFolder = session.createFolder(properties, session.createObjectId(parentFolderId));
            return createFolder;
        } catch (final CmisRuntimeException e) {
            LOGGER.error("Can't create folder", e);
            throw new FolderAlreadyExistsException(folderName);
        }
    }

    @Override
    public org.ow2.bonita.services.Folder createFolder(final String folderName, final String parentFolderId) throws FolderAlreadyExistsException {
        Session session = getSession();
        ObjectId folderId = createFolder(session, folderName, parentFolderId);
        CmisObject object = session.getObject(folderId);
        return convertFolder((Folder) object);
    }

    private Document createDocument(final Session session, final String name, final String parentFolderId) throws DocumentationCreationException {
        try {
            final Map<String, String> newDocProps = new HashMap<String, String>();
            newDocProps.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
            newDocProps.put(PropertyIds.NAME, name);
            final ObjectId docId = session.createDocument(newDocProps, session.createObjectId(parentFolderId), null, null);
            return convertDocument((org.apache.chemistry.opencmis.client.api.Document) session.getObject(docId), null, null, null);
        } catch (final CmisConstraintException e) {
            throw new DocumentAlreadyExistsException(name, name);
        } catch (final CmisBaseException e) {
            throw checkExceptionAndThrowIt(session, name, parentFolderId, e);
        }
    }

    @Override
    public Document getDocument(final String documentId) throws DocumentNotFoundException {
        final Session session2 = getSession();
        try {
            final org.apache.chemistry.opencmis.client.api.Document doc = (org.apache.chemistry.opencmis.client.api.Document) session2.getObject(session2
                    .createObjectId(documentId));
            return convertDocument(doc, null, null, null);
        } catch (final CmisObjectNotFoundException e) {
            throw new DocumentNotFoundException(documentId);
        }
    }

    @Override
    public Document createDocument(final String name, final String parentFolderId, final String fileName, final String contentMimeType, final byte[] fileContent)
            throws DocumentationCreationException {
        final Session session = getSession();
        return createDocument(session, name, parentFolderId, fileName, contentMimeType, fileContent, null, null);
    }

    private Document createDocument(final Session session, final String name, final String parentFolderId, final String fileName, final String contentMimeType,
            final byte[] fileContent, ProcessDefinitionUUID processDefnitionUUID, ProcessInstanceUUID processInstanceUUID) throws DocumentationCreationException {
        if (contentMimeType != null) {
            try {
                new MimeType(contentMimeType);
            } catch (final MimeTypeParseException e1) {
                throw new DocumentationCreationException("Mime type not valid", e1);
            }
        }
        try {
            final Map<String, String> newDocProps = new HashMap<String, String>();
            newDocProps.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
            newDocProps.put(PropertyIds.NAME, name);
            newDocProps.put(PropertyIds.CONTENT_STREAM_FILE_NAME, fileName);

            BigInteger length;
            if (fileContent != null && fileContent.length > 0) {
                length = BigInteger.valueOf(fileContent.length);
            } else {
                length = null;
            }
            final ContentStream contentStream;
            if (fileContent == null || fileContent.length <= 0) {
                contentStream = null;
            } else {
                final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileContent);
                try {
                    contentStream = new ContentStreamImpl(fileName, length, contentMimeType, byteArrayInputStream);
                } catch (final CmisBaseException e) {
                    throw new DocumentationCreationException("Can't create the content of the document " + name + "\n" + e.getMessage());
                } finally {
                    try {
                        byteArrayInputStream.close();
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            ObjectId objectId = session.createDocument(newDocProps, session.createObjectId(parentFolderId), contentStream, null);
            return convertDocument((org.apache.chemistry.opencmis.client.api.Document) session.getObject(objectId), processDefnitionUUID, processInstanceUUID, parentFolderId);
        } catch (final CmisContentAlreadyExistsException e) {
            throw new DocumentAlreadyExistsException(name, name);
        } catch (final CmisBaseException e) {
            throw checkExceptionAndThrowIt(session, name, parentFolderId, e);
        }
    }

    /**
     * Check if the exception is due to a document already exists or if it's not
     * (due to xcmis server not throwing back the good exception)
     * @param session
     * @param name
     * @param parentFolderId
     * @param e
     * @return 
     * @throws DocumentAlreadyExistsException
     *          if the base exception is due to a document that already exists
     * @throws DocumentationCreationException
     *          in other cases
     */
    private DocumentationCreationException checkExceptionAndThrowIt(final Session session, final String name, final String parentFolderId, final CmisBaseException e){
        try{
        Folder parent = (Folder) session.getObject(session.createObjectId(parentFolderId));
            CmisObject objectByPath = session.getObjectByPath(parent.getPath()+'/'+name);
            if(objectByPath != null){
                return new DocumentAlreadyExistsException(name, name);
            }
        }catch(CmisBaseException e1){
            //do nothing, will throw exception
        }
        return new DocumentationCreationException("Can't create a document named: " + name + "\n" + e.getMessage());
    }

    private org.ow2.bonita.services.Folder convertFolder(final Folder cmisFolder) {
        final List<Folder> parents = cmisFolder.getParents();
        String parentId;
        if (parents != null && parents.size() > 0) {
            parentId = parents.get(0).getId();
        } else {
            parentId = null;
        }
        final FolderImpl folderImpl = new FolderImpl(cmisFolder.getName(), parentId);
        folderImpl.setId(cmisFolder.getId());
        return folderImpl;
    }

    private DocumentImpl convertDocument(final org.apache.chemistry.opencmis.client.api.Document document, ProcessDefinitionUUID processDefinitionUUID, ProcessInstanceUUID processInstanceUUID, String folderId) {
        final Boolean latestVersion = document.isLatestVersion();
        final Boolean majorVersion = document.isMajorVersion();
        if(processDefinitionUUID == null){
          final List<Folder> parents = document.getParents();
          Folder folder = null;
          if (parents.size() > 0) {
            Collections.sort(parents, comparator);
            folder = parents.get(0);
            final String path = folder.getPath().substring(pathOfRootFolder.length());
            final String[] split = path.split("/");
            if (split.length >= 1) {
                processDefinitionUUID = new ProcessDefinitionUUID(split[0]);
            }
            if (split.length >= 2) {// will work only if children of the folder
                processInstanceUUID = new ProcessInstanceUUID(split[1]);
            }
            folderId = folder.getId();
          }
        }
        final DocumentImpl doc = new DocumentImpl(document.getName(), folderId, document.getCreatedBy(), convertDate(document.getCreationDate()),
                convertDate(document.getLastModificationDate()), latestVersion != null ? latestVersion : false, majorVersion != null ? majorVersion : false,
                document.getVersionLabel(), document.getVersionSeriesId(), document.getContentStreamFileName(), document.getContentStreamMimeType(),
                document.getContentStreamLength(), processDefinitionUUID, processInstanceUUID);
        doc.setId(document.getId());
        return doc;
    }

    private Date convertDate(final GregorianCalendar creationDate) {
        Date convertedDate;
        if (creationDate != null) {
            final long timeInMillis = creationDate.getTimeInMillis();
            convertedDate = new Date(timeInMillis);
        } else {
            convertedDate = null;
        }
        return convertedDate;
    }

    @Override
    public List<Document> getChildrenDocuments(final String folderId) {
        final Session session2 = getSession();
        return getChildrenDocuments(session2, folderId);
    }

    private List<Document> getChildrenDocuments(final Session session, final String folderId) {
        final Folder folder = (Folder) session.getObject(session.createObjectId(folderId));
        final List<Document> documents = new ArrayList<Document>();
        for (final CmisObject children : folder.getChildren()) {
            if (children instanceof org.apache.chemistry.opencmis.client.api.Document) {
                documents.add(convertDocument((org.apache.chemistry.opencmis.client.api.Document) children, null, null, folderId));
            }
        }
        return documents;
    }

    @Override
    public List<Document> getChildrenDocuments(final ProcessDefinitionUUID processDefinitionUUID, final String subfolderName)
            throws DocumentationCreationException {
        final Session session = getSession();
        CacheErrorRetrier<List<Document>> errorRetrier = new CacheErrorRetrier<List<Document>>(){
            @Override
            public List<Document> doIt(String subFolder) throws DocumentationCreationException{
                return getChildrenDocuments(subFolder);
            }
        };
        return errorRetrier.execute(session, processDefinitionUUID, subfolderName);
    }

    private List<org.ow2.bonita.services.Folder> getChildrenFolder(final Session session, final String folderId) {
        try {
            final Folder folder = (Folder) session.getObject(session.createObjectId(folderId));
            final List<org.ow2.bonita.services.Folder> subFolders = new ArrayList<org.ow2.bonita.services.Folder>();
            for (final CmisObject children : folder.getChildren()) {
                if (children instanceof Folder) {
                    subFolders.add(convertFolder((Folder) children));
                }
            }
            return subFolders;
        } catch (final Throwable t) {
            LOGGER.error("Error while listing folders of folder with id=" + folderId, t);
            throw new BonitaRuntimeException("Error while listing folders of folder with id=" + folderId + "\n" + t.getMessage());
        }
    }

    @Override
    public List<org.ow2.bonita.services.Folder> getChildrenFolder(final String folderId) {
        final Session session2 = getSession();
        return getChildrenFolder(session2, folderId);
    }

    @Override
    public org.ow2.bonita.services.Folder getRootFolder() {
        final Session session2 = getSession();
        return getRootFolder(session2);
    }

    public org.ow2.bonita.services.Folder getRootFolder(final Session session3) {
        Folder folder;
        try {
            folder = (Folder) session3.getObject(session3.createObjectId(rootFolderId));
        } catch (final CmisObjectNotFoundException e) {
            // we don't have the good id anymore (because of a hard reset of the
            // cmis server and because of bonita was not restarted)
            final CmisObject rootFolder = session3.getObjectByPath(pathOfRootFolder);
            rootFolderId = rootFolder.getId();
            folder = null;
        }
        return convertFolder(folder);
    }

    @Override
    public void deleteFolder(final org.ow2.bonita.services.Folder folder) {
        final Session session2 = getSession();
        deleteFolder(folder, session2);
    }

    private void deleteFolder(final org.ow2.bonita.services.Folder folder, final Session session2) {
        final String id = folder.getId();
        try {
            ((Folder)session2.getObject(session2.createObjectId(id))).deleteTree(true, UnfileObject.DELETE, true);
        } catch (final Throwable e) {
            LOGGER.error("can't delete folder " + folder.getName() + " with id " + folder.getId(), e);
            throw new BonitaRuntimeException("can't delete folder " + folder.getName() + " with id " + folder.getId() + "\n" + e.getMessage());
        }
        Entry<ProcessDefinitionUUID, String> entryToDelete = null;
        for (final Entry<ProcessDefinitionUUID, String> entry : processDefinitionMap.entrySet()) {
            if (id.equals(entry.getValue())) {
                entryToDelete = entry;
                break;
            }
        }
        if (entryToDelete != null) {
            processDefinitionMap.remove(entryToDelete.getKey());
            return;
        }
        Entry<String, String> entryToDelete2 = null;
        for (final Entry<String, String> entry : subFolderMap.entrySet()) {
            if (id.equals(entry.getValue())) {
                entryToDelete2 = entry;
                break;
            }
        }
        Entry<String, String> entryToDelete3 = null;
        for (final Entry<String, String> entry : processInstanceMap.entrySet()) {
            if (id.equals(entry.getValue())) {
                entryToDelete3 = entry;
                break;
            }
        }
        if (entryToDelete2 != null) {
            subFolderMap.remove(entryToDelete2.getKey());
        }
        if (entryToDelete3 != null) {
            processInstanceMap.remove(entryToDelete3.getKey());
        }
    }

    @Override
    public void deleteDocument(final String documentId, final boolean allVersions) throws DocumentNotFoundException {
        deleteDocument(getSession(), documentId, allVersions);
    }

    private void deleteDocument(final Session session, final String documentId, final boolean allVersions) throws DocumentNotFoundException {
        try {
            session.getBinding().getObjectService().deleteObject(repositoryId, documentId, allVersions, null);
        } catch (final CmisObjectNotFoundException e) {
            throw new DocumentNotFoundException(documentId);
        }
    }

    @Override
    public byte[] getContent(final Document document) throws DocumentNotFoundException {
        final Session session2 = getSession();
        final org.apache.chemistry.opencmis.client.api.Document doc;
        try {
            doc = (org.apache.chemistry.opencmis.client.api.Document) session2.getObject(session2.createObjectId(document.getId()));
        } catch (final CmisBaseException e) {
            throw new DocumentNotFoundException(document.getId());
        }
        if (doc.getContentStreamLength() == 0) {
            return null;// no contents
        } else {
            final ContentStream contentStream = doc.getContentStream();
            if (contentStream != null) {
                final InputStream stream = contentStream.getStream();
                byte[] byteArray;
                try {
                    byteArray = Misc.getAllContentFrom(stream);
                    return byteArray;
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    @Override
    public String getDocumentPath(final String documentId) throws DocumentNotFoundException {
        final Session session2 = getSession();
        try {
            final org.apache.chemistry.opencmis.client.api.Document object = (org.apache.chemistry.opencmis.client.api.Document) session2.getObject(session2
                    .createObjectId(documentId));
            return object.getParents().get(0).getPath() + "/" + object.getName();
        } catch (final CmisObjectNotFoundException e) {
            throw new DocumentNotFoundException(documentId);
        }
    }

    @Override
    public Document createVersion(final String documentId, final boolean isMajorVersion) throws DocumentationCreationException {
        final Session session2 = getSession();
        return createVersion(session2, documentId, isMajorVersion, null, "application/octet-stream", null);
    }

    @Override
    public Document createVersion(final String documentId, final boolean isMajorVersion, final String fileName, final String mimeType, final byte[] content)
            throws DocumentationCreationException {
        final Session session2 = getSession();
        return createVersion(session2, documentId, isMajorVersion, fileName, mimeType, content);
    }

    private Document createVersion(final Session session, final String documentId, final boolean isMajorVersion, final String fileName, final String mimeType,
            final byte[] content) throws DocumentationCreationException {
        org.apache.chemistry.opencmis.client.api.Document cmisDoc;
        try {
            cmisDoc = (org.apache.chemistry.opencmis.client.api.Document) session.getObject(session.createObjectId(documentId));
        } catch (final CmisObjectNotFoundException e) {
            throw new DocumentationCreationException("can't find the document", new DocumentNotFoundException(documentId));
        }
        if (!cmisDoc.isLatestVersion()) {
            cmisDoc = cmisDoc.getObjectOfLatestVersion(true);
        }
        final ObjectId pwcid;
        try {
            pwcid = cmisDoc.checkOut();
        } catch (final CmisRuntimeException e) {
            LOGGER.error("Unable to create document", e);
            throw new DocumentationCreationException("Unable to create document\n" + e.getMessage());
        }
        ObjectId newVersion = null;
        ByteArrayInputStream insputStream = null;
        final ContentStream contentStream;
        try {
            final org.apache.chemistry.opencmis.client.api.Document pwc = (org.apache.chemistry.opencmis.client.api.Document) session.getObject(pwcid);
            if (content != null && content.length > 0) {
                if (mimeType != null) {
                    try {
                        new MimeType(mimeType);
                    } catch (final MimeTypeParseException e1) {
                        LOGGER.error("Mime type not valid ", e1);
                        throw new DocumentationCreationException("Mime type not valid\n" + e1.getMessage());
                    }
                }
                insputStream = new ByteArrayInputStream(content);
                contentStream = session.getBinding().getObjectFactory()
                        .createContentStream(fileName, BigInteger.valueOf(content.length), mimeType, insputStream);
            } else {
                insputStream = new ByteArrayInputStream(new byte[0]);
                contentStream = session.getBinding().getObjectFactory().createContentStream(fileName, BigInteger.valueOf(0), mimeType, insputStream);
            }
            final Map<String, Object> newDocProps = new HashMap<String, Object>();
            newDocProps.put(PropertyIds.CONTENT_STREAM_FILE_NAME, fileName);
            newDocProps.put(PropertyIds.CONTENT_STREAM_MIME_TYPE, mimeType);
            newVersion = pwc.checkIn(isMajorVersion, newDocProps, contentStream, null, null, null, null);
        } catch (final Throwable t) {
            session.getBinding().getVersioningService().cancelCheckOut(repositoryId, pwcid.getId(), null);
            LOGGER.error("Unable to create document", t);
            throw new DocumentationCreationException("Unable to create document\n" + t.getMessage());
        } finally {
            if (insputStream != null) {
                try {
                    insputStream.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
        session.clear();// must clear it because xcmis change ids of documents
        return convertDocument((org.apache.chemistry.opencmis.client.api.Document) session.getObject(newVersion), null, null, null);
    }

    @Override
    public List<org.ow2.bonita.services.Folder> getFolders(final String folderName) {
        final Session session2 = getSession();
        return getFolders(session2, folderName);
    }

    private List<org.ow2.bonita.services.Folder> getFolders(final Session session, final String folderName) {
        final String statement = "SELECT * FROM cmis:folder WHERE cmis:name = '" + folderName + "'";
        final ItemIterable<QueryResult> query = session.query(statement, false);
        final ArrayList<org.ow2.bonita.services.Folder> folders = new ArrayList<org.ow2.bonita.services.Folder>();
        try {
            for (final QueryResult queryResult : query) {
                final PropertyData<Object> propertyById = queryResult.getPropertyById("cmis:objectId");
                final String objectId = (String) propertyById.getValues().get(0);
                final Folder cmisFolder = (Folder) session.getObject(session.createObjectId(objectId));
                folders.add(convertFolder(cmisFolder));
            }
        } catch (final CmisObjectNotFoundException e) {
            LOGGER.debug("can't find object with query: " + statement, e);
        }
        return folders;
    }

    @Override
    public List<Document> getVersionsOfDocument(final String documentId) throws DocumentNotFoundException {
        final Session session2 = getSession();
        final List<DocumentImpl> versions = new ArrayList<DocumentImpl>();
        org.apache.chemistry.opencmis.client.api.Document document;
        try {
            document = (org.apache.chemistry.opencmis.client.api.Document) session2.getObject(session2.createObjectId(documentId));
        } catch (final CmisObjectNotFoundException e) {
            throw new DocumentNotFoundException(documentId);
        }
        final List<org.apache.chemistry.opencmis.client.api.Document> allVersions2 = document.getAllVersions();
        for (final org.apache.chemistry.opencmis.client.api.Document oldDoc : allVersions2) {
            versions.add(convertDocument(oldDoc, null, null, null));
        }
        ProcessInstanceUUID processInstanceUUID = null;
        ProcessDefinitionUUID processDefinitionUUID = null;
        String folderId = null;
        for (final Document version : versions) {
            if (version.getProcessInstanceUUID() != null) {
                folderId = version.getParentFolderId();
                processDefinitionUUID = version.getProcessDefinitionUUID();
                processInstanceUUID = version.getProcessInstanceUUID();
                break;
            }
        }
        if (processInstanceUUID != null) {
            for (final DocumentImpl version : versions) {
                if (version.getProcessInstanceUUID() == null) {
                    version.setFolderId(folderId);
                    version.setProcessDefinitionUUID(processDefinitionUUID);
                    version.setProcessInstanceUUID(processInstanceUUID);
                }
            }
        }
        return new ArrayList<Document>(versions);
    }

    @Override
    public Document createDocument(final String name, final ProcessDefinitionUUID definitionUUID) throws DocumentationCreationException,
            DocumentAlreadyExistsException {
        final Session session = getSession();
        CacheErrorRetrier<Document> errorRetrier = new CacheErrorRetrier<Document>(){
            @Override
            public Document doIt(String subFolder) throws DocumentationCreationException{
                return CMISDocumentManager.this.createDocument(session, name, subFolder);
            }
        };
        return errorRetrier.execute(session, definitionUUID, null);
    }

    @Override
    public Document createDocument(final String name, final ProcessDefinitionUUID definitionUUID, final ProcessInstanceUUID instanceUUID)
            throws DocumentationCreationException, DocumentAlreadyExistsException {
        final Session session = getSession();
        String folderName = null;
        if (instanceUUID != null) {
            folderName = instanceUUID.getValue();
        }
        CacheErrorRetrier<Document> errorRetrier = new CacheErrorRetrier<Document>(){
            @Override
            public Document doIt(String subFolder) throws DocumentationCreationException{
                return CMISDocumentManager.this.createDocument(session, name, subFolder);
            }
        };
        return errorRetrier.execute(session, definitionUUID, folderName);
    }

    @Override
    public Document createDocument(final String name, final ProcessDefinitionUUID definitionUUID, final String fileName, final String contentMimeType,
            final byte[] fileContent) throws DocumentationCreationException, DocumentAlreadyExistsException {
        final Session session = getSession();
        CacheErrorRetrier<Document> errorRetrier = new CacheErrorRetrier<Document>(){
            @Override
            public Document doIt(String subFolder) throws DocumentationCreationException{
                return CMISDocumentManager.this.createDocument(session, name, subFolder, fileName, contentMimeType, fileContent, definitionUUID, null);
            }
        };
        return errorRetrier.execute(session, definitionUUID, null);
    }

    @Override
    public Document createDocument(final String name, final ProcessDefinitionUUID definitionUUID, final ProcessInstanceUUID instanceUUID,
            final String fileName, final String contentMimeType, final byte[] fileContent) throws DocumentationCreationException,
            DocumentAlreadyExistsException {
        final Session session = getSession();
        String folderName = null;
        if (instanceUUID != null) {
            folderName = instanceUUID.getValue();
        }
        CacheErrorRetrier<Document> errorRetrier = new CacheErrorRetrier<Document>(){
            @Override
            public Document doIt(String subFolder) throws DocumentationCreationException{
                return CMISDocumentManager.this.createDocument(session, name, subFolder, fileName, contentMimeType, fileContent, definitionUUID, instanceUUID);
            }
        };
        return errorRetrier.execute(session, definitionUUID, folderName);
    }

    private String createPath(final Session session, final ProcessDefinitionUUID definitionUUID, final String subFolder) throws DocumentationCreationException {
        if(subFolder == null){
            if (processDefinitionMap.containsKey(definitionUUID)) {
                return processDefinitionMap.get(definitionUUID);
            }
        }else{
            if (subFolderMap.containsKey(definitionUUID.getValue() + subFolder)) {
                return subFolderMap.get(definitionUUID.getValue() + subFolder);
            }
        }
        //not in cache because the method would have ended in this case
        
        //create or get the process def folder:
        final String processDefUUIDValue = definitionUUID.getValue();
        List<org.ow2.bonita.services.Folder> childrenFolder;
        try {
            childrenFolder = getChildrenFolder(session, rootFolderId);
        } catch (final BonitaRuntimeException e) {
            // we don't have the good id anymore (because of a hard reset of
            // the cmis server and because of bonita was not restarted)
            final CmisObject rootFolder = session.getObjectByPath(pathOfRootFolder);
            rootFolderId = rootFolder.getId();
            childrenFolder = getChildrenFolder(session, rootFolderId);
        }
        String mainFolderId = null;
        for (final org.ow2.bonita.services.Folder folder : childrenFolder) {
            if (processDefUUIDValue.equals(folder.getName())) {
                mainFolderId = folder.getId();
                break;
            }
        }
        if (mainFolderId == null) {
            try {
                mainFolderId = createFolder(session, processDefUUIDValue, rootFolderId).getId();
            } catch (final FolderAlreadyExistsException e) {
                 e.printStackTrace();
            }
        }
        processDefinitionMap.put(definitionUUID, mainFolderId);
        if (subFolder == null) {
            return mainFolderId;
        }
        
        
        String subFolderId = null;
        Folder mainFolder = (Folder) session.getObject(session.createObjectId(mainFolderId));
        if (subFolderId == null) {
            // final String processInstValue = instanceUUID.getValue();
            for (final CmisObject object : mainFolder.getChildren()) {
                if (object instanceof Folder && subFolder.equals(object.getName())) {
                    subFolderId = object.getId();
                    break;
                }
            }
            if (subFolderId == null) {
                try {
                    subFolderId = createFolder(session, subFolder, mainFolderId).getId();
                } catch (final FolderAlreadyExistsException e) {
                    throw new DocumentationCreationException("Folder already exists", e);
                }
            }
            subFolderMap.put(definitionUUID.getValue() + subFolder, subFolderId);
            processInstanceMap.put(definitionUUID.getValue(), subFolderId);
        }
        return subFolderId;
    }

    @Override
    public SearchResult search(final DocumentSearchBuilder builder, final int fromResult, final int maxResults) {
        final Session session2 = getSession();
        ProcessDefinitionUUID processDefinitionUUID = null;
        ProcessInstanceUUID processInstanceUUID = null;
        boolean noInstance = false;
        final StringBuilder whereClause = new StringBuilder();
        whereClause.append("SELECT * FROM cmis:document");
        final List<Object> query = builder.getQuery();
        final boolean isNotAtRootOfTheDrive = pathOfRootFolder.length() > 1;
        if (!query.isEmpty() || isNotAtRootOfTheDrive) {
            whereClause.append(" WHERE ");
        }
        if (isNotAtRootOfTheDrive) {
            whereClause.append(" IN_TREE('");
            whereClause.append(rootFolderId);// might be wrong if the cmis
                                             // server was
            // hard reseted (deletion of all data)
            // but bonita was not restarted, however
            // it happens only in the studio and the
            // studio do not use subfolder of the
            // xcmis server as root folder for bonita
            // document
            whereClause.append("')");
            if (!query.isEmpty()) {
                whereClause.append(" AND (");
            }
        }
        for (final Object object : query) {
            if (object instanceof DocumentCriterion) {
                final DocumentCriterion criterion = (DocumentCriterion) object;
                switch (criterion.getField()) {
                    case ID:
                        createEqualsOrInClause(whereClause, criterion, "cmis:objectId");
                        break;
                    case PROCESS_DEFINITION_UUID:
                        final String idOfProcessDefinitionUUID = getIdOfProcessDefinitionUUID(session2, (String) criterion.getValue());
                        if (idOfProcessDefinitionUUID == null) {
                            final List<Document> list = Collections.emptyList();
                            return new SearchResult(list, 0);
                        }
                        processDefinitionUUID = new ProcessDefinitionUUID((String) criterion.getValue());
                        whereClause.append(" IN_TREE('");
                        whereClause.append(idOfProcessDefinitionUUID);
                        whereClause.append("') ");
                        break;
                    case PROCESS_DEFINITION_UUID_WITHOUT_INSTANCES:
                        final String idOfProcessDefinitionUUID2 = getIdOfProcessDefinitionUUID(session2, (String) criterion.getValue());
                        if (idOfProcessDefinitionUUID2 == null) {
                            final List<Document> list = Collections.emptyList();
                            return new SearchResult(list, 0);
                        }
                        noInstance = true;
                        processDefinitionUUID = new ProcessDefinitionUUID((String) criterion.getValue());
                        whereClause.append(" IN_FOLDER('");
                        whereClause.append(idOfProcessDefinitionUUID2);
                        whereClause.append("') ");
                        break;
                    case PROCESS_INSTANCE_UUID:

                        final String value = (String) criterion.getValue();
                        final List<String> ids = new ArrayList<String>();
                        if (value != null) {
                            // equals
                            final String folderIfOfProcessInstance = getFolderIfOfProcessInstance(session2, value);
                            if (folderIfOfProcessInstance != null) {
                                ids.add(folderIfOfProcessInstance);
                            }
                            processInstanceUUID = new ProcessInstanceUUID(value);
                        } else {
                            // in clause
                            for (final Object inValue : criterion.getValues()) {
                                final String folderIfOfProcessInstance = getFolderIfOfProcessInstance(session2, (String) inValue);
                                if (folderIfOfProcessInstance != null) {
                                    ids.add(folderIfOfProcessInstance);
                                }
                            }
                        }

                        if (ids.size() == 0) {
                            final List<Document> list = Collections.emptyList();
                            return new SearchResult(list, 0);
                        } else {
                            for (final Iterator<String> iterator = ids.iterator(); iterator.hasNext();) {
                                final String id = iterator.next();
                                whereClause.append(" IN_FOLDER('");
                                whereClause.append(id);
                                whereClause.append("') ");
                                if (iterator.hasNext()) {
                                    whereClause.append(" OR ");
                                }

                            }

                        }
                        break;
                    case NAME:
                        createEqualsOrInClause(whereClause, criterion, "cmis:name");
                        break;
                    case FILENAME:
                        createEqualsOrInClause(whereClause, criterion, "cmis:contentStreamFileName");
                        break;
                    case CREATION_DATE:
                        getTimeComparison(whereClause, criterion, "cmis:creationDate");
                        break;
                    case AUTHOR:
                        createEqualsOrInClause(whereClause, criterion, "cmis:createdBy");
                        break;
                    case LAST_MODIFICATION_DATE:
                        getTimeComparison(whereClause, criterion, "cmis:lastModificationDate");
                        break;
                    case IS_EMPTY:
                        if ((Boolean) criterion.getValue()) {
                            whereClause.append(" cmis:contentStreamLength = 0 ");
                        } else {
                            whereClause.append(" cmis:contentStreamLength > 0 ");
                        }
                        break;
                }
            } else {
                whereClause.append(" ").append(object).append(" ");
            }
        }

        if (isNotAtRootOfTheDrive && !query.isEmpty()) {
            whereClause.append(" )");
        }
        ItemIterable<QueryResult> queryResult = session2.query(whereClause.toString(), builder.isSearchAllVersions());
        queryResult = queryResult.skipTo(fromResult);
        final ItemIterable<QueryResult> page = queryResult.getPage(maxResults);
        final List<Document> documents = new ArrayList<Document>();
        boolean resolveParents = processDefinitionUUID ==null || (processInstanceUUID == null && !noInstance);
        for (final QueryResult queryResult2 : page) {
            final PropertyData<Object> propertyById = queryResult2.getPropertyById("cmis:objectId");
            final org.apache.chemistry.opencmis.client.api.Document doc = (org.apache.chemistry.opencmis.client.api.Document) session2.getObject(session2
                    .createObjectId((String) propertyById.getValues().get(0)));
            DocumentImpl convertDocument;
            if(resolveParents){
                convertDocument = convertDocument(doc, null, null, null);
            }else{
                if(processInstanceUUID != null){
                    convertDocument = convertDocument(doc,processDefinitionUUID,processInstanceUUID, processInstanceMap.get(processInstanceUUID.getValue()));
                }else{
                    convertDocument = convertDocument(doc,processDefinitionUUID,processInstanceUUID, processDefinitionMap.get(processDefinitionUUID.getValue()));
                }
            }
            documents.add(convertDocument);
        }
        final int totalNumItems = (int) queryResult.getTotalNumItems();
        final SearchResult result = new SearchResult(documents, Math.max(totalNumItems, documents.size()));
        return result;
    }

    /**
     * @param session2
     * @param processInstanceUUID
     * @return
     */
    private String getFolderIfOfProcessInstance(final Session session2, final String processInstanceUUIDAsString) {
        final String id2;
        final ProcessInstanceUUID processInstanceUUID = new ProcessInstanceUUID(processInstanceUUIDAsString);
        if (processInstanceMap.containsKey(processInstanceUUID)) {
            id2 = processInstanceMap.get(processInstanceUUID);
        } else {
            final List<org.ow2.bonita.services.Folder> folders2 = getFolders(session2, processInstanceUUID.getValue());
            if (folders2.size() == 0) {
                return null;
            }
            id2 = folders2.get(0).getId();
        }
        return id2;
    }

    @Override
    public void deleteFolderOfProcessInstance(final ProcessInstanceUUID instanceUUID) {
        final Session session = getSession();
        final String id = getFolderIfOfProcessInstance(session, instanceUUID.getValue());
        if (id != null) {
            final FolderImpl folderImpl = new FolderImpl(null, null);
            folderImpl.setId(id);
            deleteFolder(folderImpl, session);
        }
    }

    @Override
    public void deleteFolderOfProcessDefinition(final ProcessDefinitionUUID definitionUUID) {
        final Session session = getSession();
        final String id = getIdOfProcessDefinitionUUID(session, definitionUUID.getValue());
        if (id != null) {
            final FolderImpl folderImpl = new FolderImpl(null, null);
            folderImpl.setId(id);
            deleteFolder(folderImpl, session);
        }
    }

    private void createEqualsOrInClause(final StringBuilder whereClause, final DocumentCriterion criterion, final String field) {
        if (criterion.getValues() != null) {
            whereClause.append(" " + field + " IN (");
            final Collection<?> values = criterion.getValues();
            for (final Iterator<?> iterator = values.iterator(); iterator.hasNext();) {
                final Object object2 = iterator.next();
                whereClause.append("'");
                whereClause.append(object2);
                whereClause.append("'");
                if (iterator.hasNext()) {
                    whereClause.append(",");
                }
            }
            whereClause.append(") ");
        } else {
            whereClause.append(" " + field + " = '");
            whereClause.append(criterion.getValue());
            whereClause.append("' ");
        }
    }

    private String getIdOfProcessDefinitionUUID(final Session session2, final String definitionUUID) {
        final String id;
        final ProcessDefinitionUUID processDef = new ProcessDefinitionUUID(definitionUUID);
        if (processDefinitionMap.containsKey(processDef)) {
            id = processDefinitionMap.get(processDef);
        } else {
            final List<org.ow2.bonita.services.Folder> folders = getFolders(session2, processDef.getValue());
            if (folders.size() == 0) {
                return null;
            }
            id = folders.get(0).getId();
        }
        return id;
    }

    private void getTimeComparison(final StringBuilder whereClause, final DocumentCriterion criterion, final String attribute) {
        final SimpleDateFormat cmisDateFormat = CMISDocumentManager.CMIS_DATE_FORMAT.get();
        if (criterion.getValue() != null) {
            whereClause.append(attribute);
            whereClause.append(" = TIMESTAMP '");
            final Date value = (Date) criterion.getValue();
            final String fromDate = cmisDateFormat.format(value);
            whereClause.append(fromDate);
            whereClause.append("' ");
        } else {
            whereClause.append(" (");
            whereClause.append(attribute);
            whereClause.append(" >= TIMESTAMP '");
            final Date from = (Date) criterion.getFrom();
            final String fromDate = cmisDateFormat.format(from);
            whereClause.append(fromDate);
            whereClause.append("' AND ");
            whereClause.append(attribute);
            whereClause.append(" <= TIMESTAMP '");
            final Date to = (Date) criterion.getTo();
            final String toDate = cmisDateFormat.format(to);
            whereClause.append(toDate);
            whereClause.append("') ");
        }
    }

    @Override
    public void clear() throws DocumentNotFoundException {
        Session session = getSession();
        clear(session, getRootFolder(session));
        // sessionsMap.clear();
        clearCache();
        session = null;
    }

    /**
     * 
     */
    private void clearCache() {
        processDefinitionMap.clear();
        subFolderMap.clear();
        processInstanceMap.clear();
    }

    public void clear(final org.ow2.bonita.services.Folder folder) throws DocumentNotFoundException {
        final Session session = getSession();
        clear(session, folder);
    }

    public void clear(final Session session2, final org.ow2.bonita.services.Folder folder) throws DocumentNotFoundException {
        Folder cmisFolder = null;
        try {
            for (final org.ow2.bonita.services.Folder subFolder : getChildrenFolder(session2, folder.getId())) {
                cmisFolder = (Folder) session2.getObject(session2.createObjectId(subFolder.getId()));
                cmisFolder.deleteTree(true, null, true);
            }
            for (final Document doc : getChildrenDocuments(session2, folder.getId())) {
                deleteDocument(session2, doc.getId(), true);
            }
        } catch (final CmisBaseException e) {
            throw new DocumentNotFoundException(folder.getId());
        }
    }

    @Override
    public void updateDocumentContent(final String documentId, final String fileName, final String mimeType, final int size, final byte[] content)
            throws DocumentNotFoundException {
        final Session session2 = getSession();
        org.apache.chemistry.opencmis.client.api.Document document = null;
        try {
            document = (org.apache.chemistry.opencmis.client.api.Document) session2.getObject(session2.createObjectId(documentId));
        } catch (final Exception e) {
            throw new DocumentNotFoundException(documentId, e);
        }
        if (content != null) {
            ByteArrayInputStream inputStream = null;
            try {
                inputStream = new ByteArrayInputStream(content);
                final ContentStream contentStream = session2.getBinding().getObjectFactory()
                        .createContentStream(fileName, BigInteger.valueOf(size), mimeType, inputStream);
                document.setContentStream(contentStream, true);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void attachDocumentTo(final ProcessDefinitionUUID processDefinitionUUID, final String documentId) throws DocumentNotFoundException {
        attachDocumentTo(processDefinitionUUID, null, documentId);
    }

    @Override
    public void attachDocumentTo(final ProcessDefinitionUUID processDefinitionUUID, final ProcessInstanceUUID processInstanceUUID, final String documentId)
            throws DocumentNotFoundException {
        final Session session2 = getSession();
        try {
            

            CacheErrorRetrier<Void> errorRetrier = new CacheErrorRetrier<Void>(){
                @Override
                public Void doIt(String subFolder) throws DocumentationCreationException{
                    org.apache.chemistry.opencmis.client.api.Document document;
                    document = (org.apache.chemistry.opencmis.client.api.Document) session2.getObject(session2.createObjectId(documentId));
                    final Folder cmisFolder = (Folder) session2.getObject(session2.createObjectId(subFolder));
                    document.addToFolder(cmisFolder, true);
                    return null;
                }
            };
            errorRetrier.execute(session2, processDefinitionUUID, processInstanceUUID.getValue());
        } catch (final CmisRuntimeException e) {
            throw new DocumentNotFoundException(documentId);
        } catch (final DocumentationCreationException dce) {
            throw new DocumentNotFoundException(documentId, dce);
        }
    }

    @Override
    public Document createDocument(final String name, final ProcessDefinitionUUID definitionUUID, final ProcessInstanceUUID instanceUUID, final String author,
            final Date versionDate) throws DocumentationCreationException, DocumentAlreadyExistsException {
        final Session session = getSession(author);
        String folderName = null;
        if (instanceUUID != null) {
            folderName = instanceUUID.getValue();
        }

        CacheErrorRetrier<Document> errorRetrier = new CacheErrorRetrier<Document>(){
            @Override
            public Document doIt(String subFolder) throws DocumentationCreationException{
                return CMISDocumentManager.this.createDocument(session, name, subFolder);
            }
        };
        return errorRetrier.execute(session, definitionUUID, folderName);
    }

    @Override
    public Document createDocument(final String name, final ProcessDefinitionUUID definitionUUID, final ProcessInstanceUUID instanceUUID, final String author,
            final Date versionDate, final String fileName, final String mimeType, final byte[] content) throws DocumentationCreationException,
            DocumentAlreadyExistsException {
        final Session session = getSession(author);
        String folderName = null;
        if (instanceUUID != null) {
            folderName = instanceUUID.getValue();
        }
        CacheErrorRetrier<Document> errorRetrier = new CacheErrorRetrier<Document>(){
            @Override
            public Document doIt(String subFolder) throws DocumentationCreationException{
                return CMISDocumentManager.this.createDocument(session, name, subFolder, fileName, mimeType, content, definitionUUID, instanceUUID);
            }
        };
        return errorRetrier.execute(session, definitionUUID, folderName);
    }

    @Override
    public Document createVersion(final String documentId, final boolean isMajorVersion, final String author, final Date versionDate)
            throws DocumentationCreationException {
        // versionDate can't be set manually
        final Session session2 = getSession(author);
        return createVersion(session2, documentId, isMajorVersion, "", "application/octet-stream", null);
    }

    @Override
    public Document createVersion(final String documentId, final boolean isMajorVersion, final String author, final Date versionDate, final String fileName,
            final String mimeType, final byte[] content) throws DocumentationCreationException {
        final Session session2 = getSession(author);
        return createVersion(session2, documentId, isMajorVersion, fileName, mimeType, content);
    }

    @Override
    public Document createDocument(final ProcessDefinitionUUID definitionUUID, final String name, final String subFolderName, final String fileName,
            final String contentMimeType, final byte[] fileContent) throws DocumentationCreationException, DocumentAlreadyExistsException {
        final Session session = getSession();
        CacheErrorRetrier<Document> errorRetrier = new CacheErrorRetrier<Document>(){
            @Override
            public Document doIt(String subFolder) throws DocumentationCreationException{
                return CMISDocumentManager.this.createDocument(session, name, subFolder, fileName, contentMimeType, fileContent, definitionUUID, null);
            }
        };
        return errorRetrier.execute(session, definitionUUID, subFolderName);
    }

}
