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
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.impl.StandardAPIAccessorImpl;
import org.ow2.bonita.facade.privilege.PrivilegePolicy;
import org.ow2.bonita.facade.privilege.Rule;
import org.ow2.bonita.facade.privilege.Rule.RuleType;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.light.LightProcessInstance;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.util.Command;

/**
 * List the process instances from the list of active users. There are only
 * active users on running instances, consequently this command only queries the
 * Journal.
 * 
 * @author Nicolas Chabanoles
 * 
 */
public class WebGetLightProcessInstancesFromActiveUsers implements Command<List<LightProcessInstance>> {

  private static final long serialVersionUID = 6000496066108490089L;
  private String userId;
  private final Collection<String> roleUUIDs;
  private final Collection<String> groupUUIDs;
  private final Collection<String> membershipUUIDs;
  private final String username;
  private int fromIndex;
  private int pageSize;

  /**
   * Default constructor.
   * 
   * @param userId
   * @param fromIndex
   * @param pageSize
   */
  public WebGetLightProcessInstancesFromActiveUsers(String userUUID, Collection<String> roleUUIDs, Collection<String> groupUUIDs, Collection<String> membershipUUIDs, String username, int fromIndex,
      int pageSize) {
    this.userId = userUUID;
    this.roleUUIDs = roleUUIDs;
    this.groupUUIDs = groupUUIDs;
    this.membershipUUIDs = membershipUUIDs;
    this.username = username;
    this.fromIndex = fromIndex;
    this.pageSize = pageSize;
  }

  public List<LightProcessInstance> execute(Environment environment) throws Exception {
    final APIAccessor accessor = new StandardAPIAccessorImpl();
    final QueryRuntimeAPI queryRuntimeAPI = accessor.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_JOURNAL_KEY);
    final ManagementAPI managementAPI = accessor.getManagementAPI();

    List<Rule> applicableRules = new ArrayList<Rule>();
    Set<String> exceptions = new HashSet<String>();
    applicableRules = managementAPI.getApplicableRules(RuleType.PROCESS_READ, userId, roleUUIDs, groupUUIDs, membershipUUIDs, username);
    for (Rule rule : applicableRules) {
      exceptions.addAll(rule.getItems());
    }

    Set<ProcessDefinitionUUID> processUUIDs = new HashSet<ProcessDefinitionUUID>();
    for (String processUUID : exceptions) {
      processUUIDs.add(new ProcessDefinitionUUID(processUUID));
    }
    PrivilegePolicy processStartPolicy = managementAPI.getRuleTypePolicy(RuleType.PROCESS_READ);
    switch (processStartPolicy) {
    case ALLOW_BY_DEFAULT:
      // The exceptions are the processes the entity cannot see.
      if (processUUIDs != null && !processUUIDs.isEmpty()) {
        List<LightProcessInstance> result;
        result = queryRuntimeAPI.getLightParentProcessInstancesWithActiveUserExcept(username, fromIndex, pageSize, processUUIDs);
        return result;
      } else {
        List<LightProcessInstance> result;
        result = queryRuntimeAPI.getLightParentProcessInstancesWithActiveUser(username, fromIndex, pageSize);
        return result;
      }

    case DENY_BY_DEFAULT:
      // The exceptions are the processes the entity can see.
      if (processUUIDs.size() > 0) {
        List<LightProcessInstance> result;
        result = queryRuntimeAPI.getLightParentProcessInstancesWithActiveUser(username, fromIndex, pageSize, processUUIDs);
        return result;
      } else {
        return Collections.emptyList();
      }
    default:
      throw new IllegalArgumentException();
    }
  }

}
