/**
 * Copyright (C) 2006  Bull S. A. S.
 * Bull, Rue Jean Jaures, B.P.68, 78340, Les Clayes-sous-Bois
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA  02110-1301, USA.
 * 
 * Modified by Charles Souillard - BonitaSoft S.A.
 * Modified by Matthieu Chaffotte - BonitaSoft S.A.
 **/
package org.ow2.bonita.services.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ow2.bonita.facade.runtime.Category;
import org.ow2.bonita.facade.runtime.impl.CaseImpl;
import org.ow2.bonita.facade.runtime.impl.LabelImpl;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.persistence.WebDbSession;
import org.ow2.bonita.services.WebService;
import org.ow2.bonita.util.EnvTool;

public class DbWebService implements WebService {

  private String persistenceServiceName;

  protected DbWebService() { }
  
  public DbWebService(String persistenceServiceName) {
    super();
    this.persistenceServiceName = persistenceServiceName;
  }

  protected WebDbSession getDbSession() {
    return EnvTool.getWebServiceDbSession(persistenceServiceName);
  }
  
  public Set<ProcessInstanceUUID> getAllCases() {
    return getDbSession().getAllCases();
  }
  public void deleteAllCases() {
    getDbSession().deleteAllCases();
  }
  
  public void addLabel(LabelImpl label) {
    getDbSession().save(label);
  }
  
  public void addCase(CaseImpl case_) {
    getDbSession().save(case_);
  }
  
  public void removeCase(CaseImpl case_) {
    getDbSession().delete(case_);
  }
  public void removeCase(ProcessInstanceUUID caseUUID, String ownerName, String labelName) {
    final WebDbSession webSession = getDbSession(); 
    final Set<ProcessInstanceUUID> caseList = new HashSet<ProcessInstanceUUID>();
    caseList.add(caseUUID);
    final Set<CaseImpl> cases = webSession.getCases(ownerName, labelName, caseList);
    if (cases != null) {
      for (CaseImpl caseImpl : cases) {
        webSession.delete(caseImpl);   
      }
    }
  }
  
  public List<ProcessInstanceUUID> getLabelsCaseUUIDs(String ownerName, Set<String> labelNames, int fromIndex, int pageSize) {
    return getDbSession().getLabelsCaseUUIDs(ownerName, labelNames, fromIndex, pageSize);
  }
  
  public Set<ProcessInstanceUUID> getLabelCases(String labelName, Set<ProcessInstanceUUID> caseUUIDs) {
    return getDbSession().getLabelCases(labelName, caseUUIDs);
  }
  
  public Set<CaseImpl> getLabelCases(String ownerName, Set<String> labelNames, Set<ProcessInstanceUUID> caseUUIDs) {
    return getDbSession().getLabelCases(ownerName, labelNames, caseUUIDs);
  }
  
  public void removeLabel(String ownerName, String labelName) {
    final WebDbSession webSession = getDbSession(); 
    final LabelImpl label = webSession.getLabel(ownerName, labelName);
    if (label != null) {
      webSession.delete(label);
      for (CaseImpl case_ : webSession.getCases(ownerName, labelName)) {
        webSession.delete(case_);
      }
    }
  }
  
  public void removeCategories(Set<Category> categories) {
    final WebDbSession webSession = getDbSession();
    for (Category category : categories) {
      webSession.delete(category);
    }
  }
  
  public LabelImpl getLabel(String ownerName, String labelName) {
    return getDbSession().getLabel(ownerName, labelName);
  }
  
  public Set<CaseImpl> getCases(Set<ProcessInstanceUUID> caseUUIDs) {
    return getDbSession().getCases(caseUUIDs);
  }
  
  public Set<ProcessInstanceUUID> getCases(String ownerName, Set<String> theLabelsName) {
    return getDbSession().getCases(ownerName, theLabelsName);
  }
  
  public Set<LabelImpl> getLabels(String ownerName) { 
    return getDbSession().getLabels(ownerName);
  }
  
  public List<LabelImpl> getSystemLabels(String ownerName) { 
    return getDbSession().getSystemLabels(ownerName);
  }
  
  public List<LabelImpl> getUserCustomLabels(String ownerName) {
    return getDbSession().getUserCustomLabels(ownerName);
  }
  
  public Set<LabelImpl> getCaseLabels(String ownerName, ProcessInstanceUUID case_) {
    return getDbSession().getCaseLabels(ownerName, case_);
  }
  
  public CaseImpl getCase(ProcessInstanceUUID caseUUID, String ownerName, String labelName) {
    return getDbSession().getCase(caseUUID, ownerName, labelName);
  }
  
  public void removeCase(ProcessInstanceUUID case_) {
    final WebDbSession webSession = getDbSession();
    final Set<CaseImpl> cases = webSession.getCases(case_);
    if (cases != null) {
      for (CaseImpl caseImpl : cases) {
        webSession.delete(caseImpl);   
      }
    }
  }
  
  public void deleteCases(Set<ProcessInstanceUUID> webCases) {
    getDbSession().deleteCases(webCases);
  }
  
  public void removeCasesFromLabel(String ownerName, String labelName, Set<ProcessInstanceUUID> caseList) {

    if (caseList == null || caseList.isEmpty()) {
      return;
    }
    final WebDbSession webSession = getDbSession();
    final Set<CaseImpl> cases = webSession.getCases(ownerName, labelName, caseList);
    if (cases != null) {
      for (CaseImpl caseImpl : cases) {
        webSession.delete(caseImpl);
      }
    }
  }
  
  public Set<LabelImpl> getLabels(String ownerName, Set<String> labelsName) {
    return getDbSession().getLabels(ownerName, labelsName);
  }

  public int getCasesNumber(String ownerName, String labelName) {
    return getDbSession().getCasesNumber(ownerName, labelName);
  }
  
  public int getCasesNumber(String ownerName, String label1Name, String label2Name) {
    return getDbSession().getCasesNumberWithTwoLabels(ownerName, label1Name, label2Name);
  }
  
  public Set<CaseImpl> getCases(String ownerName, String labelName, int limit) {
    return getDbSession().getCases(ownerName, labelName, limit);
  }
  
  public Set<CaseImpl> getCases(String ownerName, String label1Name, String label2Name, int limit) {
    return getDbSession().getCasesWithTwoLabels(ownerName, label1Name, label2Name, limit);
  }
  
  public void updateLabelName(String ownerName, String labelName, String newName) {
    final LabelImpl label = EnvTool.getWebService().getLabel(ownerName, labelName);
    label.setName(newName);
    final Set<CaseImpl> cases = getDbSession().getCases(ownerName, labelName);
    if (cases != null && !cases.isEmpty()) {
      for (CaseImpl case1 : cases) {
        case1.setLabelName(newName);
      }
    } 
  }

  public Set<LabelImpl> getLabelsByName(Set<String> labelNames) {
    return getDbSession().getLabels(labelNames);
  }

  public Set<LabelImpl> getLabelsByNameExcept(Set<String> labelNames) {
    return getDbSession().getLabelsByNameExcept(labelNames);
  }


}
