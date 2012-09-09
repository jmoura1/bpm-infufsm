/**
 * Copyright (C) 2009  BonitaSoft S.A.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.ow2.bonita.connector.core.Connector;
import org.ow2.bonita.connector.core.Mapper;
import org.ow2.bonita.definition.activity.ConnectorExecutor;
import org.ow2.bonita.facade.WebAPI;
import org.ow2.bonita.facade.def.InternalProcessDefinition;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.exception.CategoryAlreadyExistsException;
import org.ow2.bonita.facade.exception.CategoryNotFoundException;
import org.ow2.bonita.facade.exception.DocumentAlreadyExistsException;
import org.ow2.bonita.facade.exception.DocumentationCreationException;
import org.ow2.bonita.facade.exception.ProcessNotFoundException;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.Category;
import org.ow2.bonita.facade.runtime.Document;
import org.ow2.bonita.facade.runtime.Label;
import org.ow2.bonita.facade.runtime.WebTemporaryToken;
import org.ow2.bonita.facade.runtime.impl.CaseImpl;
import org.ow2.bonita.facade.runtime.impl.CategoryImpl;
import org.ow2.bonita.facade.runtime.impl.InternalProcessInstance;
import org.ow2.bonita.facade.runtime.impl.LabelImpl;
import org.ow2.bonita.facade.runtime.impl.WebTemporaryTokenImpl;
import org.ow2.bonita.facade.uuid.CategoryUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.light.LightProcessDefinition;
import org.ow2.bonita.light.LightProcessInstance;
import org.ow2.bonita.light.impl.LightProcessDefinitionImpl;
import org.ow2.bonita.light.impl.LightProcessInstanceImpl;
import org.ow2.bonita.services.DocumentationManager;
import org.ow2.bonita.services.WebService;
import org.ow2.bonita.services.WebTokenManagementService;
import org.ow2.bonita.util.BonitaConstants;
import org.ow2.bonita.util.DocumentService;
import org.ow2.bonita.util.EnvTool;
import org.ow2.bonita.util.Misc;

/**
 * @author Charles Souillard, Matthieu Chaffotte, Nicolas Chabanoles, Christophe Leroy
 */
public class WebAPIImpl implements WebAPI {

  private String queryList;

  protected WebAPIImpl(final String queryList) {
    this.queryList = queryList;
  }

  private String getQueryList() {
    return this.queryList;
  }

  public void deletePhantomCases() {
    Set<ProcessInstanceUUID> webCases = EnvTool.getWebService().getAllCases();
    Set<ProcessInstanceUUID> runtimeCases = EnvTool.getAllQueriers().getParentInstancesUUIDs();

    webCases.removeAll(runtimeCases);
    if (!webCases.isEmpty()) {
      EnvTool.getWebService().deleteCases(webCases);
    }
  }

  public void addLabel(String labelName, String ownerName, String editableCSSStyleName, String readonlyCSSStyleName, String previewCSSStyleName, boolean isVisible, boolean hasToBeDisplayed,
      String iconCSSStyle, Set<ProcessInstanceUUID> caseList, int displayOrder, boolean isSystemLabel) {
    final WebService webService = EnvTool.getWebService();
    final LabelImpl label = new LabelImpl(labelName, ownerName, editableCSSStyleName, readonlyCSSStyleName, previewCSSStyleName, isVisible, hasToBeDisplayed, iconCSSStyle, displayOrder, isSystemLabel);
    webService.addLabel(label);

    addCasesToLabel(ownerName, labelName, caseList);
  }

  public void removeLabel(String ownerName, String labelName) {
    EnvTool.getWebService().removeLabel(ownerName, labelName);
  }

  public void removeCasesFromLabel(String ownerName, String labelName, Set<ProcessInstanceUUID> caseList) {
    final Set<InternalProcessInstance> instances = EnvTool.getAllQueriers().getProcessInstances(caseList);
    final WebService webService = EnvTool.getWebService();
    for (InternalProcessInstance parentNewOrUpdatedInstance : instances) {
      webService.removeCase(parentNewOrUpdatedInstance.getUUID(), ownerName, labelName);

    }

    EnvTool.getWebService().removeCasesFromLabel(ownerName, labelName, caseList);
  }

  public void deleteAllCases() {
    EnvTool.getWebService().deleteAllCases();
  }

  public void updateLabelCSS(String ownerName, String labelName, String aEditableCSSStyle, String aPreviewCSSStyle, String aReadOnlyCSSStyle) {
    LabelImpl label = EnvTool.getWebService().getLabel(ownerName, labelName);
    label.setEditableCSSStyleName(aEditableCSSStyle);
    label.setPreviewCSSStyleName(aPreviewCSSStyle);
    label.setReadonlyCSSStyleName(aReadOnlyCSSStyle);
  }

  public void addCasesToLabel(String ownerName, String labelName, Set<ProcessInstanceUUID> caseList) {
    final Set<InternalProcessInstance> instances = EnvTool.getAllQueriers().getProcessInstances(caseList);
    final WebService webService = EnvTool.getWebService();
    for (InternalProcessInstance internalProcessInstance : instances) {
      if (webService.getCase(internalProcessInstance.getUUID(), ownerName, labelName) == null) {
        webService.addCase(new CaseImpl(internalProcessInstance.getUUID(), ownerName, labelName));
      }
    }
  }

  public Label getLabel(String ownerName, String labelName) {
    LabelImpl label = EnvTool.getWebService().getLabel(ownerName, labelName);
    if (label == null) {
      return null;
    }
    return new LabelImpl(label);
  }

  public Set<Label> getLabels(String ownerName) {
    Set<LabelImpl> labels = EnvTool.getWebService().getLabels(ownerName);
    return getCopy(labels);
  }

  public List<Label> getSystemLabels(String ownerName) {
    List<LabelImpl> labels = EnvTool.getWebService().getSystemLabels(ownerName);
    return getCopy(labels);
  }

  public List<Label> getUserCustomLabels(String ownerName) {
    List<LabelImpl> labels = EnvTool.getWebService().getUserCustomLabels(ownerName);
    return getCopy(labels);
  }

  public Set<Label> getCaseLabels(String ownerName, ProcessInstanceUUID case_) {
    Set<LabelImpl> labels = EnvTool.getWebService().getCaseLabels(ownerName, case_);
    return getCopy(labels);
  }

  public Map<ProcessInstanceUUID, Set<Label>> getCasesLabels(String ownerName, Set<ProcessInstanceUUID> cases) {
    if (cases == null || cases.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<ProcessInstanceUUID, Set<Label>> result = new HashMap<ProcessInstanceUUID, Set<Label>>();
    for (ProcessInstanceUUID case_ : cases) {
      result.put(case_, getCaseLabels(ownerName, case_));
    }
    return result;
  }

  public void removeAllCasesFromLabels(Set<ProcessInstanceUUID> caseList) {
    if (caseList != null && !caseList.isEmpty()) {
      final WebService webService = EnvTool.getWebService();
      for (ProcessInstanceUUID case_ : caseList) {
        webService.removeCase(case_);
      }
    }
  }

  public Set<ProcessInstanceUUID> getCases(String ownerName, Set<String> labels) {
    if (ownerName != null && labels != null && !labels.isEmpty()) {
      final WebService webService = EnvTool.getWebService();
      Set<ProcessInstanceUUID> cases = webService.getCases(ownerName, labels);
      if (cases == null) {
        return Collections.emptySet();
      }
      Set<ProcessInstanceUUID> result = new HashSet<ProcessInstanceUUID>();
      for (ProcessInstanceUUID instanceUUID : cases) {
        result.add(new ProcessInstanceUUID(instanceUUID));
      }
      return result;
    }
    return Collections.emptySet();
  }

  public List<LightProcessInstance> getLightProcessInstances(final String ownerName, final Set<String> theLabelsName, final int fromIndex, final int pageSize) {
    if (ownerName == null || theLabelsName == null || theLabelsName.isEmpty()) {
      return Collections.emptyList();
    }

    List<ProcessInstanceUUID> caseUUIDs = EnvTool.getWebService().getLabelsCaseUUIDs(ownerName, theLabelsName, fromIndex, pageSize);
    Set<InternalProcessInstance> internalList = EnvTool.getAllQueriers().getProcessInstances(caseUUIDs);

    if (internalList == null || internalList.isEmpty()) {
      return Collections.emptyList();
    }
    Comparator<InternalProcessInstance> comparator = new Comparator<InternalProcessInstance>() {
      public int compare(InternalProcessInstance o1, InternalProcessInstance o2) {
        return o2.getLastUpdate().compareTo(o1.getLastUpdate());
      }
    };
    List<InternalProcessInstance> sortedInstances = new ArrayList<InternalProcessInstance>(internalList);
    Collections.sort(sortedInstances, comparator);

    List<LightProcessInstance> list = new ArrayList<LightProcessInstance>();
    for (InternalProcessInstance internalProcessInstance : sortedInstances) {
      list.add(new LightProcessInstanceImpl(internalProcessInstance));
    }

    return list;
  }

  public Set<Label> getLabels(String ownerName, Set<String> labelsName) {
    Set<LabelImpl> labels = EnvTool.getWebService().getLabels(ownerName, labelsName);
    return getCopy(labels);
  }

  private Set<Label> getCopy(Set<LabelImpl> labels) {
    if (labels == null || labels.isEmpty()) {
      return Collections.emptySet();
    }
    Set<Label> result = new HashSet<Label>();
    for (Label label : labels) {
      result.add(new LabelImpl(label));
    }
    return result;
  }

  private List<Label> getCopy(List<LabelImpl> labels) {
    if (labels == null || labels.isEmpty()) {
      return Collections.emptyList();
    }
    List<Label> result = new ArrayList<Label>();
    for (Label label : labels) {
      result.add(new LabelImpl(label));
    }
    return result;
  }

  public void removeLabels(String ownerName, Collection<String> labelNames) {
    for (String labelName : labelNames) {
      removeLabel(ownerName, labelName);
    }
  }

  public void updateLabelName(String ownerName, String labelName, String newName) {
    EnvTool.getWebService().updateLabelName(ownerName, labelName, newName);
  }

  public void updateLabelVisibility(String ownerName, String labelName, boolean isVisible) {
    LabelImpl label = EnvTool.getWebService().getLabel(ownerName, labelName);
    label.setVisible(isVisible);
  }

  public void updateLabelVisibility(String ownerName, Map<String, Boolean> labelvisibilities) {
    for (Entry<String, Boolean> labelVisibility : labelvisibilities.entrySet()) {
      updateLabelVisibility(ownerName, labelVisibility.getKey(), labelVisibility.getValue());
    }
  }

  public Map<String, Integer> getCasesNumber(String ownerName, Collection<String> labelNames, int limit) {
    final Map<String, Integer> result = new HashMap<String, Integer>();
    final WebService webService = EnvTool.getWebService();
    for (String label : labelNames) {
      int number = 0;
      if (limit <= BonitaConstants.MAX_LIST_SIZE) {
        final Set<CaseImpl> cases = webService.getCases(ownerName, label, limit + 1);
        number = cases.size();
        if (number > limit) {
          number = -limit;
        }
      } else {
        number = webService.getCasesNumber(ownerName, label);
      }
      result.put(label, number);
    }
    return result;
  }

  public Map<String, Integer> getCasesNumber(String ownerName, Collection<String> labelNames) {
    final Map<String, Integer> result = new HashMap<String, Integer>();
    final WebService webService = EnvTool.getWebService();
    for (String label : labelNames) {
      int number = webService.getCasesNumber(ownerName, label);
      result.put(label, number);
    }
    return result;
  }

  public Map<String, Integer> getCasesNumber(String ownerName, String labelName, Collection<String> labelNames, int limit) {
    final Map<String, Integer> result = new HashMap<String, Integer>();
    final WebService webService = EnvTool.getWebService();
    for (String label : labelNames) {
      int number = 0;
      if (limit <= BonitaConstants.MAX_LIST_SIZE) {
        final Set<CaseImpl> cases = webService.getCases(ownerName, labelName, label, limit + 1);
        number = cases.size();
        if (number > limit) {
          number = -limit;
        }
      } else {
        number = webService.getCasesNumber(ownerName, labelName, label);
      }
      result.put(label, number);
    }
    return result;
  }

  public String generateTemporaryToken(String identityKey) {
    FacadeUtil.checkArgsNotNull(identityKey);
    if (identityKey.length() == 0) {
      throw new IllegalArgumentException();
    }
    WebTokenManagementService webTokenManagementService = EnvTool.getWebTokenManagementService();
    String tokenKey = Misc.getUniqueId("");
    // Token will expire in 1 hour.
    long expirationDate = new Date().getTime() + (1000 * 60 * 60);
    WebTemporaryTokenImpl temporaryToken = new WebTemporaryTokenImpl(tokenKey, expirationDate, identityKey);
    webTokenManagementService.addTemporaryToken(temporaryToken);
    return temporaryToken.getToken();
  }

  public String getIdentityKeyFromTemporaryToken(String token) {
    WebTokenManagementService webTokenManagementService = EnvTool.getWebTokenManagementService();
    WebTemporaryToken temporaryToken = webTokenManagementService.getToken(token);
    if (temporaryToken == null) {
      return null;
    }
    // Token can be used only once.
    webTokenManagementService.deleteToken((WebTemporaryTokenImpl) temporaryToken);

    // Clean all expired tokens
    Set<WebTemporaryToken> expiredTokens = webTokenManagementService.getExpiredTokens();
    for (WebTemporaryToken expiredToken : expiredTokens) {
      webTokenManagementService.deleteToken((WebTemporaryTokenImpl) expiredToken);
    }

    return temporaryToken.getIdentityKey();
  }

  public Set<Category> getAllCategories() {
    Set<Category> categories = EnvTool.getJournalQueriers().getAllCategories();
    Set<Category> result = new HashSet<Category>();
    for (Category category : categories) {
      result.add(new CategoryImpl(category));
    }
    return result;
  }

  public Set<Category> getCategories(Set<String> names) {
    Set<Category> categories = EnvTool.getJournalQueriers().getCategories(names);
    Set<Category> result = new HashSet<Category>();
    for (Category category : categories) {
      result.add(new CategoryImpl(category));
    }
    return result;
  }

  public void deleteCategoriesByUUIDs(Set<CategoryUUID> uuids) {
    Set<CategoryImpl> categories = EnvTool.getJournalQueriers().getCategoriesByUUIDs(uuids);
    EnvTool.getWebService().removeCategories(new HashSet<Category>(categories));
  }

  public Set<Category> getCategoriesByUUIDs(Set<CategoryUUID> uuids) {
    Set<CategoryImpl> categories = EnvTool.getJournalQueriers().getCategoriesByUUIDs(uuids);
    Set<Category> result = new HashSet<Category>();
    for (Category category : categories) {
      result.add(new CategoryImpl(category));
    }
    return result;
  }

  public void removeAllLabelsExcept(Set<String> labelNames) {
    Set<LabelImpl> labels = EnvTool.getWebService().getLabelsByNameExcept(labelNames);
    for (Label label : labels) {
      removeLabel(label.getOwnerName(), label.getName());
    }
  }

  public void removeLabels(Set<String> labelNames) {
    Set<LabelImpl> labels = EnvTool.getWebService().getLabelsByName(labelNames);
    for (Label label : labels) {
      removeLabel(label.getOwnerName(), label.getName());
    }
  }

  public void deleteCategories(Set<String> categoryNames) {
    Set<Category> categories = EnvTool.getJournalQueriers().getCategories(categoryNames);
    EnvTool.getWebService().removeCategories(categories);
  }

  public void executeConnectorAndSetVariables(String connectorClassName, Map<String, Object[]> parameters, ActivityInstance activityInstance, Map<String, Object> context) throws Exception {
    ProcessDefinitionUUID processDefinitionUUID = activityInstance.getProcessDefinitionUUID();
    Connector connector = (Connector) EnvTool.getClassDataLoader().getInstance(processDefinitionUUID, connectorClassName);
    if (connector instanceof Mapper) {
      throw new IllegalAccessException(connectorClassName + " is a instance of RoleResolver or Filter");
    }
    ConnectorExecutor.executeConnector(connector, parameters, activityInstance.getProcessInstanceUUID(), activityInstance, context);
  }

  public Map<String, Object> executeConnectorAndGetVariablesToSet(String connectorClassName, Map<String, Object[]> parameters, ProcessDefinitionUUID processDefinitionUUID, Map<String, Object> context)
      throws Exception {
    Connector connector = (Connector) EnvTool.getClassDataLoader().getInstance(processDefinitionUUID, connectorClassName);
    if (connector instanceof Mapper) {
      throw new IllegalAccessException(connectorClassName + " is a instance of RoleResolver or Filter");
    }
    return ConnectorExecutor.executeConnector(connector, parameters, processDefinitionUUID, context);
  }

  public LightProcessDefinition setProcessCategories(ProcessDefinitionUUID aProcessUUID, Set<String> aCategoriesName) throws ProcessNotFoundException {
    FacadeUtil.checkArgsNotNull(aProcessUUID);
    InternalProcessDefinition internalProcess = EnvTool.getAllQueriers().getProcess(aProcessUUID);
    if (internalProcess == null) {
      throw new ProcessNotFoundException("web_1", aProcessUUID);
    }
    internalProcess.setCategories(aCategoriesName);
    return new LightProcessDefinitionImpl(internalProcess);
  }

  public void addCategory(String name, String iconCSSStyle, String previewCSSStyleName, String cssStyleName) throws CategoryAlreadyExistsException {
    FacadeUtil.checkArgsNotNull(name);
    Set<Category> categories = EnvTool.getJournalQueriers().getCategories(Arrays.asList(name));
    if (categories != null && !categories.isEmpty()) {
      throw new CategoryAlreadyExistsException(name);
    }
    final CategoryImpl newCategory = new CategoryImpl(name);
    newCategory.setIconCSSStyle(iconCSSStyle);
    newCategory.setPreviewCSSStyleName(previewCSSStyleName);
    newCategory.setReadonlyCSSStyleName(cssStyleName);
    EnvTool.getRecorder().recordNewCategory(newCategory);
  }

  public Category updateCategoryByUUID(String uuid, String name, String iconCSSStyle, String previewCSSStyleName, String cssStyleName) throws CategoryNotFoundException, CategoryAlreadyExistsException {
    FacadeUtil.checkArgsNotNull(uuid, name);
    CategoryImpl category = EnvTool.getJournalQueriers().getCategoryByUUID(uuid);
    if (category == null) {
      throw new CategoryNotFoundException(name);
    }
    Set<Category> categories = EnvTool.getJournalQueriers().getCategories(Arrays.asList(name));
    if (categories != null && !categories.isEmpty()) {
      if (!categories.iterator().next().getUUID().equals(uuid)) {
        throw new CategoryAlreadyExistsException(name);
      }
    }
    category.setIconCSSStyle(iconCSSStyle);
    category.setPreviewCSSStyleName(previewCSSStyleName);
    category.setReadonlyCSSStyleName(cssStyleName);
    category.setName(name);
    return new CategoryImpl(category);
  }

  /* (non-Javadoc)
   * @see org.ow2.bonita.facade.WebAPI#getAllCategoriesByUUIDExcept(java.util.Set)
   */
  public Set<Category> getAllCategoriesByUUIDExcept(Set<CategoryUUID> uuids) {
    
    final Set<String> ids = new HashSet<String>();
    for (CategoryUUID categoryUUID : uuids) {
      ids.add(categoryUUID.getValue());
    }
    Set<Category> categories = EnvTool.getJournalQueriers().getAllCategoriesExcept(ids);
    Set<Category> result = new HashSet<Category>();
    for (Category category : categories) {
      result.add(new CategoryImpl(category));
    }
    return result;
  }

/* (non-Javadoc)
 * @see org.ow2.bonita.facade.WebAPI#addProcessDocumentTemplate(java.lang.String, org.ow2.bonita.facade.uuid.ProcessDefinitionUUID, java.lang.String, java.lang.String, byte[])
 */
@Override
public Document addProcessDocumentTemplate(final String name, final ProcessDefinitionUUID processDefinitionUUID, final String fileName, final String mimeType, final byte[] content) throws ProcessNotFoundException, DocumentAlreadyExistsException, DocumentationCreationException {
    final ProcessDefinition process = EnvTool.getAllQueriers().getProcess(processDefinitionUUID);
    if (process == null) {
      throw new ProcessNotFoundException("bai_RAPII_29", processDefinitionUUID);
    } else if (content != null && (fileName == null || mimeType == null)) {
      new DocumentationCreationException("");
    }
    final DocumentationManager manager = EnvTool.getDocumentationManager();    
    return DocumentService.getClientDocument(manager,manager.createDocument(processDefinitionUUID, name, "template", fileName, mimeType, content));     
}

/* (non-Javadoc)
 * @see org.ow2.bonita.facade.WebAPI#getProcessDocumentTemplates(org.ow2.bonita.facade.uuid.ProcessDefinitionUUID)
 */
@Override
public List<Document> getProcessDocumentTemplates(final ProcessDefinitionUUID processDefinitionUUID) throws ProcessNotFoundException, DocumentationCreationException {
    List<Document> result = new ArrayList<Document>();
    final ProcessDefinition process = EnvTool.getAllQueriers().getProcess(processDefinitionUUID);
    if (process == null) {
      throw new ProcessNotFoundException("bai_RAPII_29", processDefinitionUUID);
    }
    final DocumentationManager manager = EnvTool.getDocumentationManager();    
    result = DocumentService.getClientDocuments(manager, manager.getChildrenDocuments(processDefinitionUUID,"template"));
    return result;
}

}
