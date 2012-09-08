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
package org.ow2.bonita.facade.runtime.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ow2.bonita.env.Environment;
import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.ManagementAPI;
import org.ow2.bonita.facade.QueryDefinitionAPI;
import org.ow2.bonita.facade.impl.StandardAPIAccessorImpl;
import org.ow2.bonita.facade.paging.ProcessDefinitionCriterion;
import org.ow2.bonita.facade.privilege.PrivilegePolicy;
import org.ow2.bonita.facade.privilege.Rule;
import org.ow2.bonita.facade.privilege.Rule.RuleType;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.light.LightProcessDefinition;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.util.Command;

public class WebGetManageableProcessesCommand implements Command<List<LightProcessDefinition>> {

  private static final long serialVersionUID = -822497593009305244L;
  private final String userUUID;
  private final Collection<String> roleUUIDs;
  private final Collection<String> groupUUIDs;
  private final Collection<String> membershipUUIDs;
  private final String entityID;
  private final int fromIndex;
  private final int pageSize;
  private final boolean searchInHistory;

  public WebGetManageableProcessesCommand(int fromIndex, int pageSize, boolean searchInHistory, String userUUID, Collection<String> roleUUIDs, Collection<String> groupUUIDs, Collection<String> membershipUUIDs, String entityID) {
    super();
    this.fromIndex = fromIndex;
    this.pageSize = pageSize;
    this.searchInHistory = searchInHistory;
    this.userUUID = userUUID;
    this.roleUUIDs = roleUUIDs;
    this.groupUUIDs = groupUUIDs;
    this.membershipUUIDs = membershipUUIDs;
    this.entityID = entityID;
  }

  public List<LightProcessDefinition> execute(Environment environment) throws Exception {
    final APIAccessor accessor = new StandardAPIAccessorImpl();
    final ManagementAPI managementAPI = accessor.getManagementAPI();
    final QueryDefinitionAPI queryDefinitionAPI;
    if(searchInHistory) {
      queryDefinitionAPI = accessor.getQueryDefinitionAPI(AccessorUtil.QUERYLIST_HISTORY_KEY);
    } else {
      queryDefinitionAPI = accessor.getQueryDefinitionAPI(AccessorUtil.QUERYLIST_JOURNAL_KEY);
    }

    List<Rule> applicableRules = new ArrayList<Rule>();
    Set<String> exceptions = new HashSet<String>();
    applicableRules = managementAPI.getApplicableRules(RuleType.PROCESS_MANAGE, userUUID, roleUUIDs, groupUUIDs, membershipUUIDs, entityID);
    for (Rule rule : applicableRules) {
      exceptions.addAll(rule.getItems());
    }

    Set<ProcessDefinitionUUID> processUUIDs = new HashSet<ProcessDefinitionUUID>();
    for (String processUUID : exceptions) {
      processUUIDs.add(new ProcessDefinitionUUID(processUUID));
    }
    PrivilegePolicy processStartPolicy = managementAPI.getRuleTypePolicy(RuleType.PROCESS_MANAGE);
    switch (processStartPolicy) {
    case ALLOW_BY_DEFAULT:
      // The exceptions are the processes the entity cannot manage.
      if (processUUIDs != null && !processUUIDs.isEmpty()) {
        List<LightProcessDefinition> result = queryDefinitionAPI.getAllLightProcessesExcept(processUUIDs, fromIndex, pageSize);
        return result;
      } else {
        return queryDefinitionAPI.getLightProcesses(fromIndex,pageSize);
      }

    case DENY_BY_DEFAULT:
      // The exceptions are the processes the entity can manage.
      if (processUUIDs.size() > 0) {
        return queryDefinitionAPI.getLightProcesses(processUUIDs, fromIndex, pageSize, ProcessDefinitionCriterion.DEFAULT);
      } else {
        return Collections.emptyList();
      }
    default:
      throw new IllegalArgumentException();
    }

  }
}
