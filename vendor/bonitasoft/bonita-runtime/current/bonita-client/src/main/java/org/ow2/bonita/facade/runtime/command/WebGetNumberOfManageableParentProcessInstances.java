/**
 * Copyright (C) 2012  BonitaSoft S.A.
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ow2.bonita.env.Environment;
import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.ManagementAPI;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.impl.StandardAPIAccessorImpl;
import org.ow2.bonita.facade.privilege.PrivilegePolicy;
import org.ow2.bonita.facade.privilege.Rule;
import org.ow2.bonita.facade.privilege.Rule.RuleType;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.util.Command;

public class WebGetNumberOfManageableParentProcessInstances implements Command<Integer> {

	private static final long serialVersionUID = 4223637974438770807L;
	private boolean searchInHistory;
  private final String userID;
  private final Set<String> roleUUIDs;
  private final Set<String> groupUUIDs;
  private final Set<String> membershipUUIDs;
  private final String entityID;

	/**
	 * Default constructor.
	 * 
	 * @param userId
	 * @param userName 
	 * @param userMemberships 
	 * @param userGroups 
	 * @param userRoles 
	 * @param userID 
	 */
	public WebGetNumberOfManageableParentProcessInstances(boolean searchInHistory, String userID, Set<String> userRoles, Set<String> userGroups, Set<String> userMemberships, String userName) {
		this.searchInHistory = searchInHistory;
    this.userID = userID;
    this.roleUUIDs = userRoles;
    this.groupUUIDs = userGroups;
    this.membershipUUIDs = userMemberships;
    this.entityID = userName;
	}

	public Integer execute(Environment environment) throws Exception {
		final APIAccessor accessor = new StandardAPIAccessorImpl();
    final ManagementAPI managementAPI = accessor.getManagementAPI();
		final QueryRuntimeAPI queryRuntimeAPI;
		if(this.searchInHistory) {
			queryRuntimeAPI = accessor.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_HISTORY_KEY);
		} else {
			queryRuntimeAPI = accessor.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_JOURNAL_KEY);
		}
		
		List<Rule> applicableRules = new ArrayList<Rule>();
    Set<String> exceptions = new HashSet<String>();
    applicableRules = managementAPI.getApplicableRules(RuleType.PROCESS_MANAGE, userID, roleUUIDs, groupUUIDs, membershipUUIDs, entityID);
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
        return queryRuntimeAPI.getNumberOfParentProcessInstancesExcept(processUUIDs);
      } else {
        return queryRuntimeAPI.getNumberOfParentProcessInstances();
      }

    case DENY_BY_DEFAULT:
      // The exceptions are the processes the entity can manage.
      if (processUUIDs.size() > 0) {
        return queryRuntimeAPI.getNumberOfParentProcessInstances(processUUIDs);
      } else {
        return 0;
      }
    default:
      throw new IllegalArgumentException();
    }

	}

}
