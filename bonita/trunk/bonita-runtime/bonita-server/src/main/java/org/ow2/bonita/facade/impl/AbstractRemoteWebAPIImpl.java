/**
 * Copyright (C) 2010  BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
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
package org.ow2.bonita.facade.impl;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.WebAPI;
import org.ow2.bonita.facade.exception.CategoryAlreadyExistsException;
import org.ow2.bonita.facade.exception.CategoryNotFoundException;
import org.ow2.bonita.facade.exception.DocumentAlreadyExistsException;
import org.ow2.bonita.facade.exception.DocumentationCreationException;
import org.ow2.bonita.facade.exception.ProcessNotFoundException;
import org.ow2.bonita.facade.internal.AbstractRemoteWebAPI;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.Category;
import org.ow2.bonita.facade.runtime.Document;
import org.ow2.bonita.facade.runtime.Label;
import org.ow2.bonita.facade.uuid.CategoryUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.identity.auth.DomainOwner;
import org.ow2.bonita.identity.auth.PasswordOwner;
import org.ow2.bonita.identity.auth.RESTUserOwner;
import org.ow2.bonita.identity.auth.UserOwner;
import org.ow2.bonita.light.LightProcessDefinition;
import org.ow2.bonita.light.LightProcessInstance;

/**
 * 
 * @author Elias Ricken de Medeiros, Matthieu Chaffotte
 * 
 */
public class AbstractRemoteWebAPIImpl implements AbstractRemoteWebAPI {

  private static final long serialVersionUID = 4856106680913239049L;

  protected Map<String, WebAPI> apis = new HashMap<String, WebAPI>();

  protected WebAPI getAPI(final Map<String, String> options) {
    final String queryList = options.get(APIAccessor.QUERYLIST_OPTION);
    final String user = options.get(APIAccessor.USER_OPTION);
    final String domain = options.get(APIAccessor.DOMAIN_OPTION);
    UserOwner.setUser(user);
    DomainOwner.setDomain(domain);
    
    final String restUser = options.get(APIAccessor.REST_USER_OPTION);
    if (restUser != null) {
      RESTUserOwner.setUser(restUser);
      String restPswd = options.get(APIAccessor.PASSWORD_HASH_OPTION);
      PasswordOwner.setPassword(restPswd);
    }

    if (!apis.containsKey(queryList)) {
      apis.put(queryList, new StandardAPIAccessorImpl().getWebAPI(queryList));
    }
    return apis.get(queryList);
  }

  public void deletePhantomCases(final Map<String, String> options) throws RemoteException {
    getAPI(options).deletePhantomCases();
  }

  public void addCasesToLabel(String ownerName, String labelName, Set<ProcessInstanceUUID> caseList, final Map<String, String> options) throws RemoteException {
    getAPI(options).addCasesToLabel(ownerName, labelName, caseList);
  }

  public void addLabel(String labelName, String ownerName, String editableCSSStyleName, String readonlyCSSStyleName, String previewCSSStyleName, boolean isVisible, boolean hasToBeDisplayed,
      String iconCSSStyle, Set<ProcessInstanceUUID> caseList, int displayOrder, boolean isSystemLabel, final Map<String, String> options) throws RemoteException {
    getAPI(options).addLabel(labelName, ownerName, editableCSSStyleName, readonlyCSSStyleName, previewCSSStyleName, isVisible, hasToBeDisplayed, iconCSSStyle, caseList, displayOrder, isSystemLabel);
  }

  public Label getLabel(String ownerName, String labelName, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getLabel(ownerName, labelName);
  }

  public Set<Label> getLabels(String ownerName, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getLabels(ownerName);
  }

  public List<Label> getSystemLabels(String ownerName, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getSystemLabels(ownerName);
  }

  public List<Label> getUserCustomLabels(String ownerName, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getUserCustomLabels(ownerName);
  }

  public Set<Category> getAllCategories(final Map<String, String> options) throws RemoteException {
    return getAPI(options).getAllCategories();
  }

  public Set<Category> getAllCategoriesByUUIDExcept(final Set<CategoryUUID> uuids, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getAllCategoriesByUUIDExcept(uuids);
  }

  public Set<Category> getCategories(final Set<String> names, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getCategories(names);
  }

  public Set<Category> getCategoriesByUUIDs(final Set<CategoryUUID> uuids, Map<String, String> options) throws RemoteException {
    return getAPI(options).getCategoriesByUUIDs(uuids);
  }

  public void deleteCategories(final Set<String> categoryNames, final Map<String, String> options) throws RemoteException {
    getAPI(options).deleteCategories(categoryNames);
  }

  public void deleteCategoriesByUUIDs(Set<CategoryUUID> uuids, final Map<String, String> options) throws RemoteException {
    getAPI(options).deleteCategoriesByUUIDs(uuids);
  }

  public List<LightProcessInstance> getLightProcessInstances(String ownerName, Set<String> labelNames, int startingIndex, int maxElementCount, final Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getLightProcessInstances(ownerName, labelNames, startingIndex, maxElementCount);
  }

  public Set<ProcessInstanceUUID> getCases(String ownerName, Set<String> labels, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getCases(ownerName, labels);
  }

  public void removeCasesFromLabel(String ownerName, String labelName, Set<ProcessInstanceUUID> caseList, final Map<String, String> options) throws RemoteException {
    getAPI(options).removeCasesFromLabel(ownerName, labelName, caseList);
  }

  public void removeLabel(String ownerName, String labelName, final Map<String, String> options) throws RemoteException {
    getAPI(options).removeLabel(ownerName, labelName);
  }

  public void updateLabelCSS(String ownerName, String labelName, String aEditableCSSStyle, String aPreviewCSSStyle, String aReadOnlyCSSStyle, final Map<String, String> options) throws RemoteException {
    getAPI(options).updateLabelCSS(ownerName, labelName, aEditableCSSStyle, aPreviewCSSStyle, aReadOnlyCSSStyle);
  }

  public void updateLabelName(String ownerName, String labelName, String newName, final Map<String, String> options) throws RemoteException {
    getAPI(options).updateLabelName(ownerName, labelName, newName);
  }

  public Set<Label> getCaseLabels(String ownerName, ProcessInstanceUUID case_, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getCaseLabels(ownerName, case_);
  }

  public Map<ProcessInstanceUUID, Set<Label>> getCasesLabels(String ownerName, Set<ProcessInstanceUUID> cases, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getCasesLabels(ownerName, cases);
  }

  public void removeAllCasesFromLabels(Set<ProcessInstanceUUID> caseList, final Map<String, String> options) throws RemoteException {
    getAPI(options).removeAllCasesFromLabels(caseList);
  }

  public Set<Label> getLabels(String ownerName, Set<String> labelsName, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getLabels(ownerName, labelsName);
  }

  public void deleteAllCases(final Map<String, String> options) throws RemoteException {
    getAPI(options).deleteAllCases();
  }

  public void updateLabelVisibility(String ownerName, String labelName, boolean isVisible, final Map<String, String> options) throws RemoteException {
    getAPI(options).updateLabelVisibility(ownerName, labelName, isVisible);
  }

  public void updateLabelVisibility(String ownerName, Map<String, Boolean> labelvisibilities, final Map<String, String> options) throws RemoteException {
    getAPI(options).updateLabelVisibility(ownerName, labelvisibilities);
  }

  public String generateTemporaryToken(String identityKey, final Map<String, String> options) throws RemoteException {
    return getAPI(options).generateTemporaryToken(identityKey);
  }

  public String getIdentityKeyFromTemporaryToken(String token, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getIdentityKeyFromTemporaryToken(token);
  }

  public void removeAllLabelsExcept(Set<String> labelNames, final Map<String, String> options) throws RemoteException {
    getAPI(options).removeAllLabelsExcept(labelNames);
  }

  public void removeLabels(Set<String> labelNames, final Map<String, String> options) throws RemoteException {
    getAPI(options).removeLabels(labelNames);
  }

  public void executeConnectorAndSetVariables(String connectorClassName, Map<String, Object[]> parameters, ActivityInstance activityInstance, Map<String, Object> context, Map<String, String> options)
      throws RemoteException, Exception {
    getAPI(options).executeConnectorAndSetVariables(connectorClassName, parameters, activityInstance, context);
  }

  public Map<String, Object> executeConnectorAndGetVariablesToSet(String connectorClassName, Map<String, Object[]> parameters, ProcessDefinitionUUID processDefinitionUUID,
      Map<String, Object> context, final Map<String, String> options) throws RemoteException, Exception {
    return getAPI(options).executeConnectorAndGetVariablesToSet(connectorClassName, parameters, processDefinitionUUID, context);
  }

  public LightProcessDefinition setProcessCategories(ProcessDefinitionUUID processUUID, Set<String> categoryNames, Map<String, String> options) throws RemoteException, ProcessNotFoundException {
    return getAPI(options).setProcessCategories(processUUID, categoryNames);
  }

  public void addCategory(String name, String iconCSSStyle, String previewCSSStyleName, String cssStyleName, Map<String, String> options) throws RemoteException, CategoryAlreadyExistsException {
    getAPI(options).addCategory(name, iconCSSStyle, previewCSSStyleName, cssStyleName);
  }

  public Category updateCategoryByUUID(String value, String name, String iconCSSStyle, String previewCSSStyleName, String cssStyleName, Map<String, String> options) throws RemoteException,
      CategoryNotFoundException, CategoryAlreadyExistsException {
    return getAPI(options).updateCategoryByUUID(value, name, iconCSSStyle, previewCSSStyleName, cssStyleName);
  }

  @Override
  public Document addProcessDocumentTemplate(String name, ProcessDefinitionUUID processDefinitionUUID, String fileName, 
      String mimeType, byte[] content, Map<String, String> options)
  throws RemoteException, ProcessNotFoundException, DocumentAlreadyExistsException, DocumentationCreationException {
    return getAPI(options).addProcessDocumentTemplate(name, processDefinitionUUID, fileName, mimeType, content);
  }

  @Override
  public List<Document> getProcessDocumentTemplates(ProcessDefinitionUUID processDefinitionUUID, Map<String, String> options)
  throws RemoteException, ProcessNotFoundException, DocumentationCreationException {
    return getAPI(options).getProcessDocumentTemplates(processDefinitionUUID);
  }

}
