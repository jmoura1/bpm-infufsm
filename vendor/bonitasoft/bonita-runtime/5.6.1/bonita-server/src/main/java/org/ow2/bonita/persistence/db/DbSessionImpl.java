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
 * Modified by Charles Souillard, Matthieu Chaffotte, Nicolas Chabanoles, Elias Ricken de Medeiros - 
 * BonitaSoft S.A.
 **/
package org.ow2.bonita.persistence.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.util.Version;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.transform.Transformers;
import org.hibernate.type.Type;
import org.ow2.bonita.facade.def.InternalActivityDefinition;
import org.ow2.bonita.facade.def.InternalProcessDefinition;
import org.ow2.bonita.facade.def.element.impl.MetaDataImpl;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition.ProcessState;
import org.ow2.bonita.facade.exception.BonitaInternalException;
import org.ow2.bonita.facade.identity.impl.GroupImpl;
import org.ow2.bonita.facade.identity.impl.MembershipImpl;
import org.ow2.bonita.facade.identity.impl.ProfileMetadataImpl;
import org.ow2.bonita.facade.identity.impl.RoleImpl;
import org.ow2.bonita.facade.identity.impl.UserImpl;
import org.ow2.bonita.facade.paging.ActivityInstanceCriterion;
import org.ow2.bonita.facade.paging.GroupCriterion;
import org.ow2.bonita.facade.paging.ProcessDefinitionCriterion;
import org.ow2.bonita.facade.paging.ProcessInstanceCriterion;
import org.ow2.bonita.facade.paging.RoleCriterion;
import org.ow2.bonita.facade.paging.UserCriterion;
import org.ow2.bonita.facade.privilege.Rule;
import org.ow2.bonita.facade.privilege.RuleTypePolicy;
import org.ow2.bonita.facade.privilege.Rule.RuleType;
import org.ow2.bonita.facade.runtime.ActivityState;
import org.ow2.bonita.facade.runtime.Category;
import org.ow2.bonita.facade.runtime.Comment;
import org.ow2.bonita.facade.runtime.InstanceState;
import org.ow2.bonita.facade.runtime.TaskInstance;
import org.ow2.bonita.facade.runtime.WebTemporaryToken;
import org.ow2.bonita.facade.runtime.impl.CaseImpl;
import org.ow2.bonita.facade.runtime.impl.CategoryImpl;
import org.ow2.bonita.facade.runtime.impl.InternalActivityInstance;
import org.ow2.bonita.facade.runtime.impl.InternalProcessInstance;
import org.ow2.bonita.facade.runtime.impl.LabelImpl;
import org.ow2.bonita.facade.uuid.ActivityDefinitionUUID;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.uuid.CategoryUUID;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.persistence.EventDbSession;
import org.ow2.bonita.persistence.IdentityDbSession;
import org.ow2.bonita.persistence.JournalDbSession;
import org.ow2.bonita.persistence.PrivilegeDbSession;
import org.ow2.bonita.persistence.QuerierDbSession;
import org.ow2.bonita.persistence.WebDbSession;
import org.ow2.bonita.persistence.WebTokenManagementDbSession;
import org.ow2.bonita.runtime.event.EventCouple;
import org.ow2.bonita.runtime.event.EventInstance;
import org.ow2.bonita.runtime.event.IncomingEventInstance;
import org.ow2.bonita.runtime.event.OutgoingEventInstance;
import org.ow2.bonita.runtime.model.Execution;
import org.ow2.bonita.search.SearchQueryBuilder;
import org.ow2.bonita.search.SearchUtil;
import org.ow2.bonita.util.BonitaConstants;
import org.ow2.bonita.util.BonitaRuntimeException;
import org.ow2.bonita.util.DateUtil;
import org.ow2.bonita.util.Misc;
import org.ow2.bonita.util.ProcessInstanceEndedDateComparatorAsc;
import org.ow2.bonita.util.ProcessInstanceEndedDateComparatorDesc;
import org.ow2.bonita.util.ProcessInstanceLastUpdateComparator;
import org.ow2.bonita.util.ProcessInstanceLastUpdateComparatorAsc;
import org.ow2.bonita.util.ProcessInstanceNbComparatorAsc;
import org.ow2.bonita.util.ProcessInstanceNbComparatorDesc;
import org.ow2.bonita.util.ProcessInstanceStartedDateComparatorAsc;
import org.ow2.bonita.util.ProcessInstanceStartedDateComparatorDesc;
import org.ow2.bonita.util.ProcessInstanceUUIDComparatorAsc;
import org.ow2.bonita.util.ProcessInstanceUUIDComparatorDesc;
import org.ow2.bonita.util.hibernate.GenericEnumUserType;

@SuppressWarnings("deprecation")
public class DbSessionImpl extends HibernateDbSession implements QuerierDbSession, JournalDbSession, WebDbSession, IdentityDbSession, EventDbSession, PrivilegeDbSession, WebTokenManagementDbSession {

  private static final Logger LOG = Logger.getLogger(DbSessionImpl.class.getName());

  private static final int MAX_LOOP = 100;
  private static final long PAUSE_TIME_MILLIS = 100L;

  private static final String METADATA_TABLE = "BN_METADATA";
  private static final String INSERT_METADATA_STATEMENT = "insert into " + METADATA_TABLE + " (key_, value_) values (?,'0')";
  private static final String SELECT_METADATA_STATEMENT = "select value_ from " + METADATA_TABLE + " where key_=?";
  private static final String LOCK_METADATA_STATEMENT = "update " + METADATA_TABLE + " set value_ = value_ where key_=?";
  private static final String UPDATE_METADATA = "update " + METADATA_TABLE + " set value_ = ? where key_=?";
  private static final String REMOVE_METADATA = "delete from " + METADATA_TABLE + " where key_=?";

  public DbSessionImpl(Session session) {
    setSession(session);
    //session.setFlushMode(FlushMode.COMMIT);
  }

  /*
   * QUERIER
   */
  private static final Type definitionStateUserType =
    Hibernate.custom(GenericEnumUserType.class, new String[]{"enumClass"}, new String[]{ProcessState.class.getName()});

  private static final Type activityStateUserType =
    Hibernate.custom(GenericEnumUserType.class, new String[]{"enumClass"}, new String[]{ActivityState.class.getName()});

  private static final Type instanceStateUserType =
    Hibernate.custom(GenericEnumUserType.class, new String[]{"enumClass"}, new String[]{InstanceState.class.getName()});

  //private static Type instanceStateUserType =
  //  Hibernate.custom(GenericEnumUserType.class, new String[]{"enumClass"}, new String[]{InstanceState.class.getName()});

  protected static final Version LUCENE_VERSION = SearchUtil.LUCENE_VERSION;

  public long getLockedMetadata(String key) {
    Connection connection=null;
    try {
      connection = getConnection();
      return getMetadata(connection, key);
    } catch (SQLException e) {
      // FIXME find a more appropriate exception type
      throw new BonitaInternalException("Unexpected DB access exception: " + e, e);
    } finally {
      releaseConnection(connection);
    }
  }

  public void lockMetadata(String key) {
    Connection connection=null;
    try {
      connection = getConnection();
      boolean locked = lockMetadata(connection, key);
      if (!locked) {
        createMetadata(connection, key);
      }
    } catch (SQLException e) {
      // FIXME find a more appropriate exception type
      throw new BonitaInternalException("Unexpected DB access exception: " + e, e);
    } finally {
      releaseConnection(connection);
    }
  }

  public void updateLockedMetadata(String key, long value) {    
    Connection connection=null;
    try {
      connection = getConnection();
      updateMetadata(connection, key, value);
    } catch (SQLException e) {
      // FIXME find a more appropriate exception type
      throw new BonitaInternalException("Unexpected DB access exception: " + e, e);
    } finally {
      releaseConnection(connection);
    }
  }

  public void removeLockedMetadata(String key) {
    Connection connection=null;
    try {
      connection = getConnection();
      removeMetadata(connection, key);
    } catch (SQLException e) {
      // FIXME find a more appropriate exception type
      throw new BonitaInternalException("Unexpected DB access exception: " + e, e);
    } finally {
      releaseConnection(connection);
    }
  }

  private void releaseConnection(Connection connection) {
    if (connection != null && shouldReleaseConnections()) {
      try {
        connection.close();
      }
      catch (SQLException e) {
        if (LOG.isLoggable(Level.FINE)) {
          LOG.fine("SQLException during connection close: " + e);
        }
      }
    }
  }

  private boolean shouldReleaseConnections() {
    return ((SessionFactoryImplementor) session.getSessionFactory()).getSettings().getConnectionReleaseMode() ==
      ConnectionReleaseMode.AFTER_STATEMENT;
  } 

  private Connection getConnection() {
    return session.connection();
  }

  private long getMetadata(Connection connection, String key) throws SQLException {
    PreparedStatement selectStmt
    = connection.prepareStatement(SELECT_METADATA_STATEMENT);
    selectStmt.setString(1, key);

    ResultSet resultSet = selectStmt.executeQuery();
    long nb;
    if (resultSet.next()) {
      String value = resultSet.getString(1);
      nb = Long.valueOf(value);
    } else {
      throw new IllegalStateException("value not found for key=" + key);
    }

    resultSet.close();
    selectStmt.close();
    return nb;
  }

  private void updateMetadata(Connection connection, String key, long nb) throws SQLException {
    PreparedStatement updateStmt = connection.prepareStatement(UPDATE_METADATA);
    updateStmt.setString(1, String.valueOf(nb));
    updateStmt.setString(2, key);
    updateStmt.executeUpdate();

    updateStmt.close();
  }

  private void removeMetadata(Connection connection, String key) throws SQLException {
    PreparedStatement updateStmt = connection.prepareStatement(REMOVE_METADATA);
    updateStmt.setString(1, key);
    updateStmt.executeUpdate();

    updateStmt.close();
  }

  private void createMetadata(Connection connection, String key) throws SQLException {
    int loops = 0;
    do {
      PreparedStatement insertStmt = connection.prepareStatement(INSERT_METADATA_STATEMENT);
      insertStmt.setString(1, key);
      try {
        insertStmt.executeUpdate();
        return;
      } catch (SQLException e) {
        if (isConstraintViolation(e)) {
          try {
            Thread.sleep(PAUSE_TIME_MILLIS);
          } catch (InterruptedException e1) {
            LOG.log(Level.FINE, "interrupted");
          }
          // another process has inserted or is inserting at the same time
          boolean locked = lockMetadata(connection, key);
          if (locked) {
            return;
          }
        } else {
          throw e;
        }
      }
      finally {
        insertStmt.close();
      }
      loops++;
    } while (loops < MAX_LOOP);
    throw new IllegalStateException(" Could not create or lock value for key=" + key + ". Giving up after " + loops + " iterations.");
  }

  private boolean lockMetadata(Connection connection, String key) throws SQLException {
    PreparedStatement updateStmt;
    updateStmt = connection.prepareStatement(LOCK_METADATA_STATEMENT);
    updateStmt.setString(1, key);
    int updateCount = updateStmt.executeUpdate();
    updateStmt.close();
    return updateCount > 0;
  }


  private boolean isConstraintViolation(SQLException e) {
    String sqlState = getSqlState(e);
    boolean isConstraintViolation = false;
    if (sqlState != null && sqlState.length() >= 2) {
      String classCode = sqlState.substring(0, 2);
      if ("23".equals(classCode)) {
        isConstraintViolation = true;
      }
    }
    return isConstraintViolation;
  }

  private String getSqlState(SQLException e) {
    String sqlState = e.getSQLState();
    if (sqlState == null) {
      if (e.getCause() != null) {
        SQLException nextException = e.getNextException();
        if (nextException != null) {
          sqlState = nextException.getSQLState();
        }
      }
    }
    return sqlState;
  }

  public int getNumberOfParentProcessInstances() {
    final Query query = getSession().getNamedQuery("getNumberOfParentProcessInstances");
    query.setCacheable(true);
    return ((Long) query.uniqueResult()).intValue();
  }

  public int getNumberOfProcessInstances() {
    final Query query = getSession().getNamedQuery("getNumberOfProcessInstances");
    query.setCacheable(true);
    return ((Long) query.uniqueResult()).intValue();
  }

  public int getNumberOfProcesses() {
    final Query query = getSession().getNamedQuery("getNumberOfProcesses");
    query.setCacheable(true);
    return ((Long) query.uniqueResult()).intValue();
  }

  @SuppressWarnings("unchecked")
  public Set<TaskInstance> getUserInstanceTasks(String userId,
      ProcessInstanceUUID instanceUUID, ActivityState taskState) {
    Set<TaskInstance> result = new HashSet<TaskInstance>();
    Query query = getSession().getNamedQuery("getUserInstanceTasksWithState");
    query.setCacheable(true);
    query.setString("userId", userId);
    query.setString("instanceUUID", instanceUUID.toString());
    query.setParameter("state", taskState, activityStateUserType);
    result.addAll(query.list());
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<TaskInstance> getUserTasks(String userId, Collection<ActivityState> taskStates) {
    Query query = getSession().getNamedQuery("getUserTasksWithStates");
    query.setCacheable(true);
    query.setString("userId", userId);
    query.setParameterList("states", taskStates, activityStateUserType);
    Set<TaskInstance> result = new HashSet<TaskInstance>();
    result.addAll(query.list());
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<InternalActivityInstance> getActivityInstances(ProcessInstanceUUID instanceUUID, String activityName) {
    Query query = getSession().getNamedQuery("getActivityInstancesWithName");
    query.setCacheable(true);
    query.setString("instanceUUID", instanceUUID.getValue());
    query.setString("name", activityName);
    Set<InternalActivityInstance> result = new HashSet<InternalActivityInstance>();
    result.addAll(query.list());
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<InternalActivityInstance> getActivityInstances(ProcessInstanceUUID instanceUUID, String activityName, String iterationId) {
    Query query = getSession().getNamedQuery("getActivityInstances");
    query.setCacheable(true);
    query.setString("instanceUUID", instanceUUID.getValue());
    query.setString("activityName", activityName);
    query.setString("iterationId", iterationId);
    Set<InternalActivityInstance> result = new HashSet<InternalActivityInstance>();
    result.addAll(query.list());
    return result;
  }

  @SuppressWarnings("unchecked")
  public List<InternalActivityInstance> getActivityInstancesFromRoot(ProcessInstanceUUID rootInstanceUUID) {
    if (rootInstanceUUID == null) {
      return Collections.emptyList();
    }
    Query query = getSession().getNamedQuery("getActivityInstancesFromRoot");
    query.setCacheable(true);
    query.setString("rootInstanceUUID", rootInstanceUUID.getValue());
    return (List<InternalActivityInstance>) query.list();
  }

  @SuppressWarnings("unchecked")
  public List<InternalActivityInstance> getActivityInstancesFromRoot(Set<ProcessInstanceUUID> rootInstanceUUIDs) {
    if (rootInstanceUUIDs == null || rootInstanceUUIDs.isEmpty()) {
      return Collections.emptyList();
    }
    Collection<String> uuids = new HashSet<String>();
    for (ProcessInstanceUUID processInstanceUUID : rootInstanceUUIDs) {
      uuids.add(processInstanceUUID.getValue());
    }

    Query query = getSession().getNamedQuery("getMatchingActivityInstancesFromRoot");
    query.setCacheable(true);
    query.setParameterList("rootInstanceUUIDs", uuids);
    return (List<InternalActivityInstance>) query.list();
  }

  @SuppressWarnings("unchecked")
  public Map<ProcessInstanceUUID, InternalActivityInstance> getLastUpdatedActivityInstanceFromRoot(Set<ProcessInstanceUUID> rootInstanceUUIDs, boolean considerSystemTaks) {
    if (rootInstanceUUIDs == null || rootInstanceUUIDs.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<ProcessInstanceUUID, InternalActivityInstance> result = new HashMap<ProcessInstanceUUID, InternalActivityInstance>();
    Query query;
    for (ProcessInstanceUUID processInstanceUUID : rootInstanceUUIDs) {
      if(considerSystemTaks){
        query = getSession().getNamedQuery("getMatchingActivityInstancesFromRoot");
      } else {
        query = getSession().getNamedQuery("getMatchingHumanTaskInstancesFromRoot");
      }

      query.setCacheable(true);
      query.setParameterList("rootInstanceUUIDs", Arrays.asList(processInstanceUUID.getValue()));
      query.setMaxResults(1);

      List<InternalActivityInstance> tmp = (List<InternalActivityInstance>)query.list(); 
      if(tmp!=null && tmp.size()>0){
        result.put(processInstanceUUID,(InternalActivityInstance)tmp.get(0));    
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  public List<InternalActivityInstance> getActivityInstancesFromRoot(Set<ProcessInstanceUUID> rootInstanceUUIDs, ActivityState state) {
    if (rootInstanceUUIDs == null || rootInstanceUUIDs.isEmpty()) {
      return Collections.emptyList();
    }
    Collection<String> uuids = new HashSet<String>();
    for (ProcessInstanceUUID processInstanceUUID : rootInstanceUUIDs) {
      uuids.add(processInstanceUUID.getValue());
    }

    Query query = getSession().getNamedQuery("getMatchingActivityInstancesWithStateFromRoot");
    query.setCacheable(true);
    query.setParameterList("rootInstanceUUIDs", uuids);
    query.setParameter("state", state, activityStateUserType);
    return (List<InternalActivityInstance>) query.list();
  }

  public TaskInstance getOneTask(String userId, ActivityState taskState) {
    Query query = getSession().getNamedQuery("getOneTask");
    query.setCacheable(true);
    query.setString("userId", userId);
    query.setParameter("state", taskState, activityStateUserType);
    query.setMaxResults(1);
    return (TaskInstance) query.uniqueResult();
  }

  public TaskInstance getOneTask(String userId, ProcessDefinitionUUID processUUID, ActivityState taskState) {
    Query query = getSession().getNamedQuery("getOneTaskOfProcess");
    query.setCacheable(true);
    query.setString("userId", userId);
    query.setString("processUUID", processUUID.getValue());
    query.setParameter("state", taskState, activityStateUserType);
    query.setMaxResults(1);
    return (TaskInstance) query.uniqueResult();
  }

  public TaskInstance getOneTask(String userId, ProcessInstanceUUID instanceUUID, ActivityState taskState) {
    Query query = getSession().getNamedQuery("getOneTaskOfInstance");
    query.setCacheable(true);
    query.setString("userId", userId);
    query.setString("instanceUUID", instanceUUID.getValue());
    query.setParameter("state", taskState, activityStateUserType);
    query.setMaxResults(1);
    return (TaskInstance) query.uniqueResult();
  }

  @SuppressWarnings("unchecked")
  public Set<InternalProcessInstance> getUserInstances(String userId) {
    Query query = getSession().getNamedQuery("getUserInstances");
    query.setCacheable(true);
    query.setString("userId", userId);
    Set<InternalProcessInstance> result = new HashSet<InternalProcessInstance>();
    result.addAll(query.list());
    return result;
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentUserInstances(String userId, int fromIndex, int pageSize) {
    final Query query = getSession().getNamedQuery("getParentUserInstancesPage");
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    query.setString("userId", userId);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;

  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentUserInstances(String userId,
      int startingIndex, int pageSize, ProcessInstanceCriterion pagingCriterion) {
    Query query = null;
    switch (pagingCriterion) {
      case LAST_UPDATE_ASC:
        query = getSession().getNamedQuery("getParentUserInstancesPageOrderByLastUpdateAsc");
        break;
      case STARTED_DATE_ASC:
        query = getSession().getNamedQuery("getParentUserInstancesPageOrderByStartedDateAsc");
        break;
      case ENDED_DATE_ASC:
        query = getSession().getNamedQuery("getParentUserInstancesPageOrderByEndedDateAsc");
        break;
      case INSTANCE_NUMBER_ASC:
        query = getSession().getNamedQuery("getParentUserInstancesPageOrderByInstanceNumberAsc");
        break;
      case INSTANCE_UUID_ASC:
        query = getSession().getNamedQuery("getParentUserInstancesPageOrderByInstanceUUIDAsc");
        break;
      case LAST_UPDATE_DESC:
        query = getSession().getNamedQuery("getParentUserInstancesPageOrderByLastUpdateDesc");
        break;
      case STARTED_DATE_DESC:
        query = getSession().getNamedQuery("getParentUserInstancesPageOrderByStartedDateDesc");
        break;
      case ENDED_DATE_DESC:
        query = getSession().getNamedQuery("getParentUserInstancesPageOrderByEndedDateDesc");
        break;
      case INSTANCE_NUMBER_DESC:
        query = getSession().getNamedQuery("getParentUserInstancesPageOrderByInstanceNumberDesc");
        break;
      case INSTANCE_UUID_DESC:
        query = getSession().getNamedQuery("getParentUserInstancesPageOrderByInstanceUUIDDesc");
        break;
      case DEFAULT:
        query = getSession().getNamedQuery("getParentUserInstancesPage");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(startingIndex);
    query.setMaxResults(pageSize);
    query.setString("userId", userId);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public Set<InternalProcessInstance> getUserInstances(String userId, Date minStartDate) {
    Query query = getSession().getNamedQuery("getUserInstancesAfterDate");
    query.setCacheable(true);
    query.setString("userId", userId);
    query.setLong("minStartDate", minStartDate.getTime());
    Set<InternalProcessInstance> result = new HashSet<InternalProcessInstance>();
    result.addAll(query.list());
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<InternalProcessInstance> getUserParentInstances(String userId, Date minStartDate) {
    Query query = getSession().getNamedQuery("getUserParentInstancesAfterDate");
    query.setCacheable(true);
    query.setString("userId", userId);
    query.setLong("minStartDate", minStartDate.getTime());
    Set<InternalProcessInstance> result = new HashSet<InternalProcessInstance>();
    result.addAll(query.list());
    return result;
  }

  public Set<InternalProcessInstance> getUserInstancesExcept(String userId, Set<ProcessInstanceUUID> instances) {
    if (instances == null || instances.isEmpty()) {
      return getUserInstances(userId);
    }
    Collection<String> uuids = new HashSet<String>();
    if (instances != null) {
      for (ProcessInstanceUUID processInstanceUUID : instances) {
        uuids.add(processInstanceUUID.getValue());
      }
    }
    Query query = getSession().getNamedQuery("getUserInstancesExcept");
    query.setCacheable(true);
    query.setString("userId", userId);
    return executeSplittedQuery(InternalProcessInstance.class, query, "uuids", uuids);
  }

  @SuppressWarnings("unchecked")
  public Set<InternalActivityInstance> getActivityInstances(ProcessInstanceUUID instanceUUID) {
    Query query = getSession().getNamedQuery("findActivityInstances");
    query.setCacheable(true);
    query.setString("instanceUUID", instanceUUID.toString());
    List<InternalActivityInstance> results = query.list();
    if (results != null) {
      return new HashSet<InternalActivityInstance>(results);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public List<InternalActivityInstance> getActivityInstances(
      ProcessInstanceUUID instanceUUID, int fromIndex, int pageSize,
      ActivityInstanceCriterion pagingCriterion) {
    Query query = null;  	

    switch (pagingCriterion) {
      case LAST_UPDATE_ASC:
        query = getSession().getNamedQuery("findActivityInstancesOrderByLastUpdateAsc");
        break;
      case STARTED_DATE_ASC:
        query = getSession().getNamedQuery("findActivityInstancesOrderByStartedDateAsc");
        break;			
      case ENDED_DATE_ASC:
        query = getSession().getNamedQuery("findActivityInstancesOrderByEndedDateAsc");
        break;
      case NAME_ASC:
        query = getSession().getNamedQuery("findActivityInstancesOrderByNameAsc");
        break;	
      case PRIORITY_ASC:
        query = getSession().getNamedQuery("findActivityInstancesOrderByPriorityAsc");
        break;
      case LAST_UPDATE_DESC:
        query = getSession().getNamedQuery("findActivityInstancesOrderByLastUpdateDesc");
        break;
      case STARTED_DATE_DESC:
        query = getSession().getNamedQuery("findActivityInstancesOrderByStartedDateDesc");
        break;			
      case ENDED_DATE_DESC:
        query = getSession().getNamedQuery("findActivityInstancesOrderByEndedDateDesc");
        break;
      case NAME_DESC:
        query = getSession().getNamedQuery("findActivityInstancesOrderByNameDesc");
        break;	
      case PRIORITY_DESC:
        query = getSession().getNamedQuery("findActivityInstancesOrderByPriorityDesc");
        break;
      case DEFAULT:
        query = getSession().getNamedQuery("findActivityInstancesOrderByLastUpdateDesc");
        break;
    }

    query.setCacheable(true);
    query.setString("instanceUUID", instanceUUID.toString());
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);

    List<InternalActivityInstance> results = query.list();
    if (results != null) {
      return new ArrayList<InternalActivityInstance>(results);
    }
    return null;
  }

  public long getLastProcessInstanceNb(ProcessDefinitionUUID processUUID) {
    Query query = getSession().getNamedQuery("getLastProcessInstanceNb");
    query.setCacheable(true);
    query.setString("processUUID", processUUID.getValue());
    query.setMaxResults(1);
    Long result = (Long) query.uniqueResult();
    if (result == null) {
      return -1;
    }
    return result;
  }

  public InternalProcessInstance getProcessInstance(ProcessInstanceUUID instanceUUID) {
    Query query = getSession().getNamedQuery("findProcessInstance");
    query.setCacheable(true);
    query.setString("instanceUUID", instanceUUID.toString());
    query.setMaxResults(1);
    return (InternalProcessInstance) query.uniqueResult();
  }

  @SuppressWarnings("unchecked")
  public Set<InternalProcessInstance> getProcessInstances() {
    final Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    final Query query = getSession().getNamedQuery("findAllProcessInstances");
    query.setCacheable(true);
    final List<InternalProcessInstance> results = query.list();
    if (results != null) {
      processInsts.addAll(results);
    }
    return processInsts;
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getProcessInstances(int fromIndex, int pageSize) {
    final Query query = getSession().getNamedQuery("findAllProcessInstances");
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getProcessInstances(int fromIndex, int pageSize, 
      ProcessInstanceCriterion pagingCriterion) {
    Query query = null;
    switch (pagingCriterion) {
      case LAST_UPDATE_ASC:
        query = getSession().getNamedQuery("findAllProcessInstancesOrderByLastUpdateAsc");
        break;
      case STARTED_DATE_ASC:
        query = getSession().getNamedQuery("findAllProcessInstancesOrderByStartedDateAsc");
        break;
      case ENDED_DATE_ASC:
        query = getSession().getNamedQuery("findAllProcessInstancesOrderByEndedDateAsc");
        break;
      case INSTANCE_NUMBER_ASC:
        query = getSession().getNamedQuery("findAllProcessInstancesOrderByInstanceNumberAsc");
        break;
      case INSTANCE_UUID_ASC:
        query = getSession().getNamedQuery("findAllProcessInstancesOrderByInstanceUUIDAsc");
        break;
      case LAST_UPDATE_DESC:
        query = getSession().getNamedQuery("findAllProcessInstancesOrderByLastUpdateDesc");
        break;
      case STARTED_DATE_DESC:
        query = getSession().getNamedQuery("findAllProcessInstancesOrderByStartedDateDesc");
        break;
      case ENDED_DATE_DESC:
        query = getSession().getNamedQuery("findAllProcessInstancesOrderByEndedDateDesc");
        break;
      case INSTANCE_NUMBER_DESC:
        query = getSession().getNamedQuery("findAllProcessInstancesOrderByInstanceNumberDesc");
        break;
      case INSTANCE_UUID_DESC:
        query = getSession().getNamedQuery("findAllProcessInstancesOrderByInstanceUUIDDesc");
        break;
      case DEFAULT:
        query = getSession().getNamedQuery("findAllProcessInstances");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getMostRecentProcessInstances(int maxResults, long time) {
    final Query query = getSession().getNamedQuery("getMostRecentProcessInstances");
    query.setCacheable(true);
    query.setFirstResult(0);
    query.setLong("time", time);
    query.setMaxResults(maxResults);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getMostRecentProcessInstances(
      int maxResults, long time, ProcessInstanceCriterion pagingCriterion) {
    Query query = null;
    switch (pagingCriterion) {
      case LAST_UPDATE_ASC:
        query = getSession().getNamedQuery("getMostRecentProcessInstancesOrderByLastUpdateAsc");
        break;
      case STARTED_DATE_ASC:
        query = getSession().getNamedQuery("getMostRecentProcessInstancesOrderByStartedDateAsc");
        break;
      case ENDED_DATE_ASC:
        query = getSession().getNamedQuery("getMostRecentProcessInstancesOrderByEndedDateAsc");
        break;
      case INSTANCE_NUMBER_ASC:
        query = getSession().getNamedQuery("getMostRecentProcessInstancesOrderByInstanceNumberAsc");
        break;
      case INSTANCE_UUID_ASC:
        query = getSession().getNamedQuery("getMostRecentProcessInstancesOrderByInstanceUUIDAsc");
        break;
      case LAST_UPDATE_DESC:
        query = getSession().getNamedQuery("getMostRecentProcessInstancesOrderByLastUpdateDesc");
        break;
      case STARTED_DATE_DESC:
        query = getSession().getNamedQuery("getMostRecentProcessInstancesOrderByStartedDateDesc");
        break;
      case ENDED_DATE_DESC:
        query = getSession().getNamedQuery("getMostRecentProcessInstancesOrderByEndedDateDesc");
        break;
      case INSTANCE_NUMBER_DESC:
        query = getSession().getNamedQuery("getMostRecentProcessInstancesOrderByInstanceNumberDesc");
        break;
      case INSTANCE_UUID_DESC:
        query = getSession().getNamedQuery("getMostRecentProcessInstancesOrderByInstanceUUIDDesc");
        break;
      case DEFAULT:
        query = getSession().getNamedQuery("getMostRecentProcessInstances");
        break;
    }
    query.setCacheable(true);
    query.setFirstResult(0);
    query.setLong("time", time);
    query.setMaxResults(maxResults);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getMostRecentParentProcessInstances(int maxResults, long time) {
    final Query query = getSession().getNamedQuery("getMostRecentParentProcessInstances");
    query.setCacheable(true);
    query.setFirstResult(0);
    query.setLong("time", time);
    query.setMaxResults(maxResults);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getMostRecentParentProcessInstances(
      int maxResults, long time, ProcessInstanceCriterion pagingCriterion) {
    Query query = null;
    switch (pagingCriterion) {
      case LAST_UPDATE_ASC:
        query = getSession().getNamedQuery("getMostRecentParentProcessInstancesOrderByLastUpdateAsc");
        break;
      case STARTED_DATE_ASC:
        query = getSession().getNamedQuery("getMostRecentParentProcessInstancesOrderByStartedDateAsc");
        break;
      case ENDED_DATE_ASC:
        query = getSession().getNamedQuery("getMostRecentParentProcessInstancesOrderByEndedDateAsc");
        break;
      case INSTANCE_NUMBER_ASC:
        query = getSession().getNamedQuery("getMostRecentParentProcessInstancesOrderByInstanceNumberAsc");
        break;
      case INSTANCE_UUID_ASC:
        query = getSession().getNamedQuery("getMostRecentParentProcessInstancesOrderByInstanceUUIDAsc");
        break;
      case LAST_UPDATE_DESC:
        query = getSession().getNamedQuery("getMostRecentParentProcessInstancesOrderByLastUpdateDesc");
        break;
      case STARTED_DATE_DESC:
        query = getSession().getNamedQuery("getMostRecentParentProcessInstancesOrderByStartedDateDesc");
        break;
      case ENDED_DATE_DESC:
        query = getSession().getNamedQuery("getMostRecentParentProcessInstancesOrderByEndedDateDesc");
        break;
      case INSTANCE_NUMBER_DESC:
        query = getSession().getNamedQuery("getMostRecentParentProcessInstancesOrderByInstanceNumberDesc");
        break;
      case INSTANCE_UUID_DESC:
        query = getSession().getNamedQuery("getMostRecentParentProcessInstancesOrderByInstanceUUIDDesc");
        break;
      case DEFAULT:
        query = getSession().getNamedQuery("getMostRecentParentProcessInstances");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(0);
    query.setLong("time", time);
    query.setMaxResults(maxResults);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  public List<InternalProcessInstance> getMostRecentMatchingProcessInstances(Collection<ProcessInstanceUUID> instanceUUIDs, int maxResults, long time) {
    final Query query = getSession().getNamedQuery("getMostRecentMatchingProcessInstances");
    query.setCacheable(true);
    query.setFirstResult(0);
    query.setLong("time", time);
    query.setMaxResults(maxResults);

    Set<String> uuids = new HashSet<String>();
    for (ProcessInstanceUUID uuid : instanceUUIDs) {
      uuids.add(uuid.toString());
    }

    List<InternalProcessInstance> allInstances = new ArrayList<InternalProcessInstance>();
    allInstances.addAll(executeSplittedQuery(InternalProcessInstance.class, query, "instanceUUIDs", uuids));
    Collections.sort(allInstances, new ProcessInstanceLastUpdateComparator());
    List<InternalProcessInstance> results = Misc.subList(InternalProcessInstance.class, allInstances, 0, maxResults);
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  public List<InternalProcessInstance> getMostRecentMatchingProcessInstances(
      Set<ProcessInstanceUUID> instanceUUIDs, int maxResults, long time,
      ProcessInstanceCriterion pagingCriterion) {
    Query query = null;
    Comparator<InternalProcessInstance> comparator = null;
    switch (pagingCriterion) {
      case LAST_UPDATE_ASC:
        comparator = new ProcessInstanceLastUpdateComparatorAsc();
        query = getSession().getNamedQuery("getMostRecentMatchingProcessInstancesOrderByLastUpdateAsc");
        break;
      case STARTED_DATE_ASC:
        comparator = new ProcessInstanceStartedDateComparatorAsc();
        query = getSession().getNamedQuery("getMostRecentMatchingProcessInstancesOrderByStartedDateAsc");
        break;
      case ENDED_DATE_ASC:
        comparator = new ProcessInstanceEndedDateComparatorAsc();
        query = getSession().getNamedQuery("getMostRecentMatchingProcessInstancesOrderByEndedDateAsc");
        break;
      case INSTANCE_NUMBER_ASC:
        comparator = new ProcessInstanceNbComparatorAsc();
        query = getSession().getNamedQuery("getMostRecentMatchingProcessInstancesOrderByInstanceNumberAsc");
        break;
      case INSTANCE_UUID_ASC:
        comparator = new ProcessInstanceUUIDComparatorAsc();
        query = getSession().getNamedQuery("getMostRecentMatchingProcessInstancesOrderByInstanceUUIDAsc");
        break;
      case LAST_UPDATE_DESC:
        comparator = new ProcessInstanceLastUpdateComparator();
        query = getSession().getNamedQuery("getMostRecentMatchingProcessInstancesOrderByLastUpdateDesc");
        break;
      case STARTED_DATE_DESC:
        comparator = new ProcessInstanceStartedDateComparatorDesc();
        query = getSession().getNamedQuery("getMostRecentMatchingProcessInstancesOrderByStartedDateDesc");
        break;
      case ENDED_DATE_DESC:
        comparator = new ProcessInstanceEndedDateComparatorDesc();
        query = getSession().getNamedQuery("getMostRecentMatchingProcessInstancesOrderByEndedDateDesc");
        break;
      case INSTANCE_NUMBER_DESC:
        comparator = new ProcessInstanceNbComparatorDesc();
        query = getSession().getNamedQuery("getMostRecentMatchingProcessInstancesOrderByInstanceNumberDesc");
        break;
      case INSTANCE_UUID_DESC:
        comparator = new ProcessInstanceUUIDComparatorDesc();
        query = getSession().getNamedQuery("getMostRecentMatchingProcessInstancesOrderByInstanceUUIDDesc");
        break;
      case DEFAULT:
        comparator = new ProcessInstanceLastUpdateComparator();
        query = getSession().getNamedQuery("getMostRecentMatchingProcessInstances");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(0);
    query.setLong("time", time);
    query.setMaxResults(maxResults);

    Set<String> uuids = new HashSet<String>();
    for (ProcessInstanceUUID uuid : instanceUUIDs) {
      uuids.add(uuid.toString());
    }

    List<InternalProcessInstance> allInstances = new ArrayList<InternalProcessInstance>();
    allInstances.addAll(executeSplittedQuery(InternalProcessInstance.class, query, "instanceUUIDs", uuids));
    Collections.sort(allInstances, comparator);
    List<InternalProcessInstance> results = Misc.subList(InternalProcessInstance.class, allInstances, 0, maxResults);
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }


  public List<InternalProcessInstance> getMostRecentProcessesProcessInstances(Collection<ProcessDefinitionUUID> definitionUUIDs, 
      int maxResults, long time, ProcessInstanceCriterion pagingCriterion) {
    Query query = null;
    Comparator<InternalProcessInstance> comparator = null;
    switch (pagingCriterion) {
      case LAST_UPDATE_ASC:
        comparator = new ProcessInstanceLastUpdateComparatorAsc();
        query = getSession().getNamedQuery("getMostRecentProcessesProcessInstancesOrderByLastUpdateAsc");
        break;
      case STARTED_DATE_ASC:
        comparator = new ProcessInstanceStartedDateComparatorAsc();
        query = getSession().getNamedQuery("getMostRecentProcessesProcessInstancesOrderByStartedDateAsc");
        break;
      case ENDED_DATE_ASC:
        comparator = new ProcessInstanceEndedDateComparatorAsc();
        query = getSession().getNamedQuery("getMostRecentProcessesProcessInstancesOrderByEndedDateAsc");
        break;
      case INSTANCE_NUMBER_ASC:
        comparator = new ProcessInstanceNbComparatorAsc();
        query = getSession().getNamedQuery("getMostRecentProcessesProcessInstancesOrderByInstanceNumberAsc");
        break;
      case INSTANCE_UUID_ASC:
        comparator = new ProcessInstanceUUIDComparatorAsc();
        query = getSession().getNamedQuery("getMostRecentProcessesProcessInstancesOrderByInstanceUUIDAsc");
        break;
      case LAST_UPDATE_DESC:
        comparator = new ProcessInstanceLastUpdateComparator();
        query = getSession().getNamedQuery("getMostRecentProcessesProcessInstancesOrderByLastUpdateDesc");
        break;
      case STARTED_DATE_DESC:
        comparator = new ProcessInstanceStartedDateComparatorDesc();
        query = getSession().getNamedQuery("getMostRecentProcessesProcessInstancesOrderByStartedDateDesc");
        break;
      case ENDED_DATE_DESC:
        comparator = new ProcessInstanceEndedDateComparatorDesc();
        query = getSession().getNamedQuery("getMostRecentProcessesProcessInstancesOrderByEndedDateDesc");
        break;
      case INSTANCE_NUMBER_DESC:
        comparator = new ProcessInstanceNbComparatorDesc();
        query = getSession().getNamedQuery("getMostRecentProcessesProcessInstancesOrderByInstanceNumberDesc");
        break;
      case INSTANCE_UUID_DESC:
        comparator = new ProcessInstanceUUIDComparatorDesc();
        query = getSession().getNamedQuery("getMostRecentProcessesProcessInstancesOrderByInstanceUUIDDesc");
        break;
      case DEFAULT:
        comparator = new ProcessInstanceLastUpdateComparator();
        query = getSession().getNamedQuery("getMostRecentProcessesProcessInstances");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(0);
    query.setLong("time", time);
    query.setMaxResults(maxResults);

    Set<String> uuids = new HashSet<String>();
    if (definitionUUIDs != null) {
      for (ProcessDefinitionUUID uuid : definitionUUIDs) {
        uuids.add(uuid.toString());
      }
    }

    List<InternalProcessInstance> allInstances = new ArrayList<InternalProcessInstance>();
    allInstances.addAll(executeSplittedQuery(InternalProcessInstance.class, query, "processUUIDs", uuids));
    Collections.sort(allInstances, comparator);
    List<InternalProcessInstance> results = Misc.subList(InternalProcessInstance.class, allInstances, 0, maxResults);
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  public List<InternalProcessInstance> getMostRecentProcessesProcessInstances(Collection<ProcessDefinitionUUID> definitionUUIDs, int maxResults, long time) {
    final Query query = getSession().getNamedQuery("getMostRecentProcessesProcessInstances");
    query.setCacheable(true);
    query.setFirstResult(0);
    query.setLong("time", time);
    query.setMaxResults(maxResults);

    Set<String> uuids = new HashSet<String>();
    if (definitionUUIDs != null) {
      for (ProcessDefinitionUUID uuid : definitionUUIDs) {
        uuids.add(uuid.toString());
      }
    }

    List<InternalProcessInstance> allInstances = new ArrayList<InternalProcessInstance>();
    allInstances.addAll(executeSplittedQuery(InternalProcessInstance.class, query, "processUUIDs", uuids));
    Collections.sort(allInstances, new ProcessInstanceLastUpdateComparator());
    List<InternalProcessInstance> results = Misc.subList(InternalProcessInstance.class, allInstances, 0, maxResults);
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }


  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentProcessInstances(int fromIndex, int pageSize) {
    final Query query = getSession().getNamedQuery("getParentInstances");
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentProcessInstances(int fromIndex,
      int pageSize, ProcessInstanceCriterion pagingCriterion) {
    Query query = null;  	
    switch (pagingCriterion) {
      case LAST_UPDATE_ASC:			
        query = getSession().getNamedQuery("getParentInstancesOrderByLastUpdateAsc");
        break;
      case STARTED_DATE_ASC:
        query = getSession().getNamedQuery("getParentInstancesOrderByStartedDateAsc");
        break;
      case ENDED_DATE_ASC:			
        query = getSession().getNamedQuery("getParentInstancesOrderByEndedDateAsc");
        break;
      case INSTANCE_NUMBER_ASC:
        query = getSession().getNamedQuery("getParentInstancesOrderByInstanceNumberAsc");
        break;
      case INSTANCE_UUID_ASC:
        query = getSession().getNamedQuery("getParentInstancesOrderByInstanceUUIDAsc");
        break;
      case LAST_UPDATE_DESC:			
        query = getSession().getNamedQuery("getParentInstancesOrderByLastUpdateDesc");
        break;
      case STARTED_DATE_DESC:
        query = getSession().getNamedQuery("getParentInstancesOrderByStartedDateDesc");
        break;
      case ENDED_DATE_DESC:			
        query = getSession().getNamedQuery("getParentInstancesOrderByEndedDateDesc");
        break;
      case INSTANCE_NUMBER_DESC:
        query = getSession().getNamedQuery("getParentInstancesOrderByInstanceNumberDesc");
        break;
      case INSTANCE_UUID_DESC:
        query = getSession().getNamedQuery("getParentInstancesOrderByInstanceUUIDDesc");
        break;
      case DEFAULT:
        query = getSession().getNamedQuery("getParentInstances");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  public List<InternalProcessInstance> getParentProcessInstances(
      Set<ProcessDefinitionUUID> processUUIDs, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion) {
    Query query = null;   
    switch (pagingCriterion) {
      case LAST_UPDATE_ASC:     
        query = getSession().getNamedQuery("getParentInstancesFromProcessUUIDsOrderByLastUpdateAsc");
        break;
      case STARTED_DATE_ASC:
        query = getSession().getNamedQuery("getParentInstancesFromProcessUUIDsOrderByStartedDateAsc");
        break;
      case ENDED_DATE_ASC:      
        query = getSession().getNamedQuery("getParentInstancesFromProcessUUIDsOrderByEndedDateAsc");
        break;
      case INSTANCE_NUMBER_ASC:
        query = getSession().getNamedQuery("getParentInstancesFromProcessUUIDsOrderByInstanceNumberAsc");
        break;
      case INSTANCE_UUID_ASC:
        query = getSession().getNamedQuery("getParentInstancesFromProcessUUIDsOrderByInstanceUUIDAsc");
        break;
      case LAST_UPDATE_DESC:      
        query = getSession().getNamedQuery("getParentInstancesFromProcessUUIDsOrderByLastUpdateDesc");
        break;
      case STARTED_DATE_DESC:
        query = getSession().getNamedQuery("getParentInstancesFromProcessUUIDsOrderByStartedDateDesc");
        break;
      case ENDED_DATE_DESC:     
        query = getSession().getNamedQuery("getParentInstancesFromProcessUUIDsOrderByEndedDateDesc");
        break;
      case INSTANCE_NUMBER_DESC:
        query = getSession().getNamedQuery("getParentInstancesFromProcessUUIDsOrderByInstanceNumberDesc");
        break;
      case INSTANCE_UUID_DESC:
        query = getSession().getNamedQuery("getParentInstancesFromProcessUUIDsOrderByInstanceUUIDDesc");
        break;
      case DEFAULT:
        query = getSession().getNamedQuery("getParentInstancesFromProcessUUIDsOrderByLastUpdateDesc");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    List<String> uuids = new ArrayList<String>();
    for (ProcessDefinitionUUID uuid : processUUIDs) {
      uuids.add(uuid.toString());
    }

    final List<InternalProcessInstance> results = executeSplittedQueryList(InternalProcessInstance.class, query, "processUUIDs", uuids);
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  public List<InternalProcessInstance> getParentProcessInstancesExcept(
      Set<ProcessDefinitionUUID> exceptions, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion) {
    Query query = null;   
    switch (pagingCriterion) {
      case LAST_UPDATE_ASC:     
        query = getSession().getNamedQuery("getParentInstancesExceptOrderByLastUpdateAsc");
        break;
      case STARTED_DATE_ASC:
        query = getSession().getNamedQuery("getParentInstancesExceptOrderByStartedDateAsc");
        break;
      case ENDED_DATE_ASC:      
        query = getSession().getNamedQuery("getParentInstancesExceptOrderByEndedDateAsc");
        break;
      case INSTANCE_NUMBER_ASC:
        query = getSession().getNamedQuery("getParentInstancesExceptOrderByInstanceNumberAsc");
        break;
      case INSTANCE_UUID_ASC:
        query = getSession().getNamedQuery("getParentInstancesExceptOrderByInstanceUUIDAsc");
        break;
      case LAST_UPDATE_DESC:      
        query = getSession().getNamedQuery("getParentInstancesExceptOrderByLastUpdateDesc");
        break;
      case STARTED_DATE_DESC:
        query = getSession().getNamedQuery("getParentInstancesExceptOrderByStartedDateDesc");
        break;
      case ENDED_DATE_DESC:     
        query = getSession().getNamedQuery("getParentInstancesExceptOrderByEndedDateDesc");
        break;
      case INSTANCE_NUMBER_DESC:
        query = getSession().getNamedQuery("getParentInstancesExceptOrderByInstanceNumberDesc");
        break;
      case INSTANCE_UUID_DESC:
        query = getSession().getNamedQuery("getParentInstancesExceptOrderByInstanceUUIDDesc");
        break;
      case DEFAULT:
        query = getSession().getNamedQuery("getParentInstancesExceptOrderByLastUpdateDesc");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    List<String> uuids = new ArrayList<String>();
    for (ProcessDefinitionUUID uuid : exceptions) {
      uuids.add(uuid.toString());
    }

    final List<InternalProcessInstance> results = executeSplittedQueryList(InternalProcessInstance.class, query, "processUUIDs", uuids);
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessDefinition> getProcesses(int fromIndex, int pageSize) {
    final Query query = getSession().getNamedQuery("getAllProcesses");
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    final List<InternalProcessDefinition> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessDefinition> getProcesses(int fromIndex,
      int pageSize, ProcessDefinitionCriterion pagingCriterion) {
    Query query = null;
    switch (pagingCriterion) {
      case NAME_ASC:
        query = getSession().getNamedQuery("getAllProcessesOrderByNameAsc");
        break;

      case LABEL_ASC:
        query = getSession().getNamedQuery("getAllProcessesOrderByLabelAsc");
        break;

      case VERSION_ASC:
        query = getSession().getNamedQuery("getAllProcessesOrderByVersionAsc");
        break;

      case STATE_ASC:
        query = getSession().getNamedQuery("getAllProcessesOrderByStateAsc");
        break;

      case NAME_DESC:
        query = getSession().getNamedQuery("getAllProcessesOrderByNameDesc");
        break;

      case LABEL_DESC:
        query = getSession().getNamedQuery("getAllProcessesOrderByLabelDesc");
        break;

      case VERSION_DESC:
        query = getSession().getNamedQuery("getAllProcessesOrderByVersionDesc");
        break;

      case STATE_DESC:
        query = getSession().getNamedQuery("getAllProcessesOrderByStateDesc");
        break;

      case DEFAULT:
        query = getSession().getNamedQuery("getAllProcesses");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    final List<InternalProcessDefinition> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  public List<InternalProcessInstance> getProcessInstances(Collection<ProcessInstanceUUID> instanceUUIDs, int fromIndex, int pageSize) {
    final Query query = getSession().getNamedQuery("findMatchingProcessInstances");
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    Set<String> uuids = new HashSet<String>();
    if (instanceUUIDs != null) {
      for (ProcessInstanceUUID uuid : instanceUUIDs) {
        uuids.add(uuid.toString());
      }
    }

    List<InternalProcessInstance> allInstances = new ArrayList<InternalProcessInstance>();
    allInstances.addAll(executeSplittedQuery(InternalProcessInstance.class, query, "instanceUUIDs", uuids));
    Collections.sort(allInstances, new ProcessInstanceLastUpdateComparator());
    List<InternalProcessInstance> results = Misc.subList(InternalProcessInstance.class, allInstances, 0, pageSize);
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  public List<InternalProcessInstance> getProcessInstancesWithInstanceUUIDs(
      Set<ProcessInstanceUUID> instanceUUIDs, int fromIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion) {
    Query query = null;
    Comparator<InternalProcessInstance> comparator = null;
    switch (pagingCriterion) {
      case LAST_UPDATE_ASC:
        comparator = new ProcessInstanceLastUpdateComparatorAsc();
        query = getSession().getNamedQuery("findMatchingProcessInstancesOrderByLastUpdateAsc");
        break;
      case STARTED_DATE_ASC:
        comparator = new ProcessInstanceStartedDateComparatorAsc();
        query = getSession().getNamedQuery("findMatchingProcessInstancesOrderByStartedDateAsc");
        break;
      case ENDED_DATE_ASC:
        comparator = new ProcessInstanceEndedDateComparatorAsc();
        query = getSession().getNamedQuery("findMatchingProcessInstancesOrderByEndedDateAsc");
        break;
      case INSTANCE_NUMBER_ASC:
        comparator = new ProcessInstanceNbComparatorAsc();
        query = getSession().getNamedQuery("findMatchingProcessInstancesOrderByInstanceNumberAsc");
        break;
      case INSTANCE_UUID_ASC:
        comparator = new ProcessInstanceUUIDComparatorAsc();
        query = getSession().getNamedQuery("findMatchingProcessInstancesOrderByIntanceUUIDAsc");
        break;
      case LAST_UPDATE_DESC:
        comparator = new ProcessInstanceLastUpdateComparator();
        query = getSession().getNamedQuery("findMatchingProcessInstancesOrderByLastUpdateDesc");
        break;
      case STARTED_DATE_DESC:
        comparator = new ProcessInstanceStartedDateComparatorDesc();
        query = getSession().getNamedQuery("findMatchingProcessInstancesOrderByStartedDateDesc");
        break;
      case ENDED_DATE_DESC:
        comparator = new ProcessInstanceEndedDateComparatorDesc();
        query = getSession().getNamedQuery("findMatchingProcessInstancesOrderByEndedDateDesc");
        break;
      case INSTANCE_NUMBER_DESC:
        comparator = new ProcessInstanceNbComparatorDesc();
        query = getSession().getNamedQuery("findMatchingProcessInstancesOrderByInstanceNumberDesc");
        break;
      case INSTANCE_UUID_DESC:
        comparator = new ProcessInstanceUUIDComparatorDesc();
        query = getSession().getNamedQuery("findMatchingProcessInstancesOrderByIntanceUUIDDesc");
        break;		
      case DEFAULT:
        comparator = new ProcessInstanceLastUpdateComparator();
        query = getSession().getNamedQuery("findMatchingProcessInstances");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    Set<String> uuids = new HashSet<String>();
    if (instanceUUIDs != null) {
      for (ProcessInstanceUUID uuid : instanceUUIDs) {
        uuids.add(uuid.toString());
      }
    }

    List<InternalProcessInstance> allInstances = new ArrayList<InternalProcessInstance>();
    allInstances.addAll(executeSplittedQuery(InternalProcessInstance.class, query, "instanceUUIDs", uuids));
    Collections.sort(allInstances, comparator);
    List<InternalProcessInstance> results = Misc.subList(InternalProcessInstance.class, allInstances, 0, pageSize);
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public List<ProcessInstanceUUID> getLabelsCaseUUIDs(String ownerName, Set<String> labelNames, int fromIndex, int pageSize) {
    final Query query = getSession().getNamedQuery("getLabelsCaseUUIDs");
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    query.setString("ownerName", ownerName);
    query.setParameterList("labelNames", labelNames);
    return query.list();
  }

  @SuppressWarnings("unchecked")
  public Set<InternalProcessInstance> getParentInstances() {
    final Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    final Query query = getSession().getNamedQuery("getParentInstances");
    query.setCacheable(true);
    final List<InternalProcessInstance> results = query.list();
    if (results != null) {
      processInsts.addAll(results);
    }
    return processInsts;
  }

  @SuppressWarnings("unchecked")
  public Set<ProcessInstanceUUID> getParentInstancesUUIDs() {
    final Set<ProcessInstanceUUID> processInsts = new HashSet<ProcessInstanceUUID>();
    final Query query = getSession().getNamedQuery("getParentInstancesUUIDs");
    query.setCacheable(true);
    final List<ProcessInstanceUUID> results = query.list();
    if (results != null) {
      processInsts.addAll(results);
    }
    return processInsts;
  }

  public Set<InternalProcessInstance> getProcessInstances(Collection<ProcessInstanceUUID> instanceUUIDs) {
    final Query query = getSession().getNamedQuery("findMatchingProcessInstances");
    query.setCacheable(true);
    Set<String> uuids = new HashSet<String>();
    for (ProcessInstanceUUID uuid : instanceUUIDs) {
      uuids.add(uuid.toString());
    }
    return executeSplittedQuery(InternalProcessInstance.class, query, "instanceUUIDs", uuids);
  }

  @SuppressWarnings("unchecked")
  public Set<InternalProcessInstance> getProcessInstancesWithTaskState(Collection<ActivityState> activityStates) {
    final Query getActivitiesQuery = getSession().getNamedQuery("findActivityInstancesWithTaskState");
    getActivitiesQuery.setCacheable(true);
    getActivitiesQuery.setParameterList("activityStates", activityStates, activityStateUserType);
    final List<String> uuids = getActivitiesQuery.list();

    final Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    if (uuids.isEmpty()) {
      return processInsts;
    }

    final Query query = getSession().getNamedQuery("findMatchingProcessInstances");
    query.setCacheable(true);
    return executeSplittedQuery(InternalProcessInstance.class, query, "instanceUUIDs", uuids);
  }

  @SuppressWarnings("unchecked")
  public Set<InternalProcessInstance> getProcessInstancesWithInstanceStates(Collection<InstanceState> instanceStates) {
    final Query getProcessInstancesQuery = getSession().getNamedQuery("findProcessInstancesWithInstanceStates");
    getProcessInstancesQuery.setCacheable(true);
    getProcessInstancesQuery.setParameterList("instanceStates", instanceStates, definitionStateUserType);
    final List<String> uuids = getProcessInstancesQuery.list();

    final Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    if (uuids.isEmpty()) {
      return processInsts;
    }

    final Query query = getSession().getNamedQuery("findMatchingProcessInstances");
    query.setCacheable(true);
    return executeSplittedQuery(InternalProcessInstance.class, query, "instanceUUIDs", uuids);
  }

  @SuppressWarnings("unchecked")
  public Set<InternalProcessInstance> getProcessInstances(final ProcessDefinitionUUID processUUID) {
    final Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    final Query query = getSession().getNamedQuery("findProcessInstances");
    query.setCacheable(true);
    query.setString("processUUID", processUUID.toString());
    final List<InternalProcessInstance> results = query.list();
    if (results != null) {
      processInsts.addAll(results);
    }
    return processInsts;
  }

  @SuppressWarnings("unchecked")
  public Set<InternalProcessInstance> getProcessInstances(ProcessDefinitionUUID processUUID, InstanceState instanceState) {
    final Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    final Query query = getSession().getNamedQuery("findProcessInstancesWithState");
    query.setCacheable(true);
    query.setString("processUUID", processUUID.toString());
    query.setParameter("state", instanceState, instanceStateUserType);
    final List<InternalProcessInstance> results = query.list();
    if (results != null) {
      processInsts.addAll(results);
    }
    return processInsts;
  }

  public TaskInstance getTaskInstance(ActivityInstanceUUID taskUUID) {
    Query query = getSession().getNamedQuery("findTaskInstance");
    query.setCacheable(true);
    query.setString("taskUUID", taskUUID.toString());
    query.setMaxResults(1);
    return (TaskInstance) query.uniqueResult();
  }

  @SuppressWarnings("unchecked")
  public Set<TaskInstance> getTaskInstances(final ProcessInstanceUUID instanceUUID) {
    final Query query = getSession().getNamedQuery("findTaskInstances");
    query.setCacheable(true);
    query.setString("instanceUUID", instanceUUID.toString());
    final Set<TaskInstance> taskInstances = new HashSet<TaskInstance>();
    final List<TaskInstance> results = query.list();
    if (results != null) {
      taskInstances.addAll(results);
    }
    return taskInstances;
  }

  @SuppressWarnings("unchecked")
  public Set<TaskInstance> getTaskInstances(final ProcessInstanceUUID instanceUUID, Set<String> taskNames) {
    final Query query = getSession().getNamedQuery("getTasksFromNames");
    query.setCacheable(true);
    query.setString("instanceUUID", instanceUUID.getValue());
    query.setParameterList("taskNames", taskNames);
    final Set<TaskInstance> taskInstances = new HashSet<TaskInstance>();
    final List<TaskInstance> results = query.list();
    if (results != null) {
      taskInstances.addAll(results);
    }
    return taskInstances;
  }

  /*
   * DEFINITION
   */
  @SuppressWarnings("unchecked")
  private Set<InternalProcessDefinition> getProcessSet(final Query query) {
    final List<InternalProcessDefinition> results = query.list();
    if (results != null) {
      return new HashSet<InternalProcessDefinition>(results);
    }
    return null;
  }

  public Set<InternalProcessDefinition> getProcesses() {
    final Query query = getSession().getNamedQuery("getAllProcesses");
    query.setCacheable(true);
    return getProcessSet(query);
  }

  public Set<InternalProcessDefinition> getProcesses(String processId) {
    final Query query = getSession().getNamedQuery("getProcesses2");
    query.setCacheable(true);
    query.setString("processId", processId);
    return getProcessSet(query);
  }

  public InternalProcessDefinition getProcess(ProcessDefinitionUUID processUUID) {
    final Query query = getSession().getNamedQuery("getProcess");
    query.setCacheable(true);
    query.setString("processUUID", processUUID.toString());
    query.setMaxResults(1);
    return (InternalProcessDefinition) query.uniqueResult();
  }

  public InternalProcessDefinition getProcess(String processId, String version) {
    final Query query = getSession().getNamedQuery("getProcessFromIdAndVersion");
    query.setCacheable(true);
    query.setString("processId", processId);
    query.setString("version", version);
    return (InternalProcessDefinition) query.uniqueResult();
  }

  public Set<InternalProcessDefinition> getProcesses(ProcessState processState) {
    Query query = getSession().getNamedQuery("getProcessesFromState");
    query.setParameter("state", processState, definitionStateUserType);
    query.setCacheable(true);
    return getProcessSet(query);
  }

  public Set<InternalProcessDefinition> getProcesses(String processId, ProcessState processState) {
    Query query = getSession().getNamedQuery("getProcessesFromProcessIdAndState");
    query.setParameter("state", processState, definitionStateUserType);
    query.setCacheable(true);
    query.setString("processId", processId);
    return getProcessSet(query);
  }

  public String getLastProcessVersion(String processName) {
    Query query = getSession().getNamedQuery("getLastProcessVersion");
    query.setCacheable(true);
    query.setString("name", processName);
    query.setMaxResults(1);
    return (String) query.uniqueResult();
  }

  public InternalProcessDefinition getLastProcess(String processId, ProcessState processState) {
    Query query = getSession().getNamedQuery("getLastProcessFromProcessIdAndState");
    query.setParameter("state", processState, definitionStateUserType);
    query.setCacheable(true);
    query.setString("processId", processId);
    query.setMaxResults(1);
    return (InternalProcessDefinition) query.uniqueResult();
  }

  public InternalActivityInstance getActivityInstance(
      ProcessInstanceUUID instanceUUID, String activityId, String iterationId, String activityInstanceId, String loopId) {
    Query query = getSession().getNamedQuery("getActivityInstance");
    query.setCacheable(true);
    query.setString("instanceUUID", instanceUUID.toString());
    query.setString("activityId", activityId);
    query.setString("iterationId", iterationId);
    query.setString("activityInstanceId", activityInstanceId);
    query.setString("loopId", loopId);
    return (InternalActivityInstance)query.uniqueResult();
  }

  public InternalActivityInstance getActivityInstance(
      ActivityInstanceUUID activityInstanceUUID) {
    Query query = getSession().getNamedQuery("getActivityInstanceFromUUID");
    query.setCacheable(true);
    query.setString("activityUUID", activityInstanceUUID.toString());
    return (InternalActivityInstance)query.uniqueResult();
  }

  public ActivityState getActivityInstanceState(ActivityInstanceUUID activityInstanceUUID) {
    Query query = getSession().getNamedQuery("getActivityInstanceStateFromUUID");
    query.setCacheable(true);
    query.setString("activityUUID", activityInstanceUUID.toString());
    return (ActivityState)query.uniqueResult();
  }

  public InternalActivityDefinition getActivityDefinition(ActivityDefinitionUUID activityDefinitionUUID) {
    final Query query = getSession().getNamedQuery("getActivityDefinition");
    query.setCacheable(true);
    query.setString("activityUUID", activityDefinitionUUID.toString());
    query.setMaxResults(1);
    return (InternalActivityDefinition) query.uniqueResult();
  }

  public Execution getExecutionWithEventUUID(final String eventUUID) {
    final Query query = getSession().getNamedQuery("getExecutionWithEventUUID");
    query.setCacheable(true);
    query.setString("eventUUID", eventUUID);
    return (Execution) query.uniqueResult();
  }

  @SuppressWarnings("unchecked")
  public Set<Execution> getExecutions(final ProcessInstanceUUID instanceUUID) {
    final Query query = getSession().getNamedQuery("getInstanceExecutions");
    query.setCacheable(true);
    query.setString("instanceUUID", instanceUUID.getValue());
    final Set<Execution> executions = new HashSet<Execution>();
    executions.addAll(query.list());
    return executions;
  }

  public Execution getExecutionPointingOnNode(final ActivityInstanceUUID activityUUID) {
    final Query query = getSession().getNamedQuery("findInstanceExecutionPointingOnNode");
    query.setCacheable(true);
    query.setString("activityUUID", activityUUID.toString());
    return (Execution) query.uniqueResult();
  }

  /*
   * RUNTIME
   */
  public MetaDataImpl getMetaData(String key) {
    final Query query = getSession().getNamedQuery("getMetaData");
    query.setCacheable(true);
    query.setString("key", key);
    return (MetaDataImpl) query.uniqueResult();
  }

  @SuppressWarnings("unchecked")
  public Set<ProcessInstanceUUID> getAllCases() {
    Set<ProcessInstanceUUID> result = new HashSet<ProcessInstanceUUID>();
    Query query = getSession().getNamedQuery("getAllCases");
    query.setCacheable(true);
    result.addAll(query.list());
    return result;
  }

  public Set<CaseImpl> getCases(Set<ProcessInstanceUUID> caseUUIDs) {
    Collection<String> uuids = new HashSet<String>();
    if (caseUUIDs != null) {
      for (ProcessInstanceUUID processInstanceUUID : caseUUIDs) {
        uuids.add(processInstanceUUID.getValue());
      }
    }
    Query query = getSession().getNamedQuery("getMatchingCases");
    query.setCacheable(true);
    return executeSplittedQuery(CaseImpl.class, query, "uuids", uuids);
  }

  @SuppressWarnings("unchecked")
  public List<LabelImpl> getUserCustomLabels(final String ownerName) {

    List<LabelImpl> result = new ArrayList<LabelImpl>();
    Query query = getSession().getNamedQuery("getUserCustomLabels");
    query.setCacheable(true);
    query.setString("ownerName", ownerName);
    result.addAll(query.list());

    return result;
  }

  @SuppressWarnings("unchecked")
  public List<LabelImpl> getSystemLabels(final String ownerName) {

    List<LabelImpl> result = new ArrayList<LabelImpl>();
    Query query = getSession().getNamedQuery("getSystemLabels");
    query.setString("ownerName", ownerName);
    query.setCacheable(true);
    List<LabelImpl> theTempResult = query.list();
    result.addAll(theTempResult);

    return result;
  }

  public LabelImpl getLabel(final String ownerName, final String labelName) {
    final Query query = getSession().getNamedQuery("getLabelByID");
    query.setString("ownerName", ownerName);
    query.setString("labelName", labelName);
    query.setCacheable(true);
    query.setMaxResults(1);
    LabelImpl theResult = (LabelImpl) query.uniqueResult();

    return theResult;
  }

  @SuppressWarnings("unchecked")
  public Set<LabelImpl> getLabels(final String ownerName) {
    Set<LabelImpl> result = new HashSet<LabelImpl>();
    Query query = getSession().getNamedQuery("getAllLabels");
    query.setString("ownerName", ownerName);
    query.setCacheable(true);
    List<LabelImpl> theTempResult = query.list();
    result.addAll(theTempResult);

    return result;
  }


  @SuppressWarnings("unchecked")
  public Set<LabelImpl> getCaseLabels(String ownerName, ProcessInstanceUUID case_) {
    //select case.labelName from case where case.ownerName = :ownerName and case.uuid = :caseUUID
    //select label from label where label.ownerName = :ownerName and label.labelName in (:labelNames)

    Query getUserCases = getSession().getNamedQuery("getUserCases");
    getUserCases.setString("ownerName", ownerName);
    getUserCases.setString("caseId", case_.getValue());
    getUserCases.setCacheable(true);
    List<String> caseLabelsNames = getUserCases.list();

    Set<LabelImpl> result = new HashSet<LabelImpl>();
    if (!caseLabelsNames.isEmpty()) {
      Query query = getSession().getNamedQuery("getLabels");
      query.setString("ownerName", ownerName);
      query.setCacheable(true);
      result = executeSplittedQuery(LabelImpl.class, query, "labelNames", caseLabelsNames);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private <T> Set<T> executeSplittedQuery(Class<T> clazz, final Query query, final String parameterName, final Collection<? extends Object> values) {
    if (values == null || values.isEmpty()) {
      return Collections.emptySet();
    }
    final Set<T> result = new HashSet<T>();
    if (values.size() <= BonitaConstants.MAX_QUERY_SIZE) {
      query.setParameterList(parameterName, values);
      List<T> l = query.list();
      result.addAll(l);
      return result;
    }

    List<Collection<Object>> newValues = Misc.splitCollection(values, BonitaConstants.MAX_QUERY_SIZE);
    for (Collection<Object> set : newValues) {
      query.setParameterList(parameterName, set);
      List<T> l = query.list();
      result.addAll(l);
    }
    return result;
  }

  private <T> List<T> executeSplittedQueryList(Class<T> clazz, final Query query, final String parameterName, final Collection<? extends Object> values) {
    return executeSplittedQueryList(clazz, query, parameterName, values, BonitaConstants.MAX_QUERY_SIZE);
  }

  @SuppressWarnings("unchecked")
  private <T> List<T> executeSplittedQueryList(Class<T> clazz, final Query query, final String parameterName, final Collection<? extends Object> values, final int size) {
    if (values == null || values.isEmpty()) {
      return Collections.emptyList();
    }
    final List<T> result = new ArrayList<T>();
    if (values.size() <= size) {
      query.setParameterList(parameterName, values);
      return query.list();
    }

    List<Collection<Object>> newValues = Misc.splitCollection(values, size);
    for (Collection<Object> set : newValues) {
      query.setParameterList(parameterName, set);
      List<T> l = query.list();
      result.addAll(l);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<CaseImpl> getCases(String ownerName, String labelName) {
    Set<CaseImpl> result = new HashSet<CaseImpl>();
    Query query = getSession().getNamedQuery("getLabelCases");
    query.setString("ownerName", ownerName);
    query.setString("labelName", labelName);
    query.setCacheable(true);
    List<CaseImpl> theTempResult = (List<CaseImpl>) query.list();
    result.addAll(theTempResult);
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<CaseImpl> getCases(ProcessInstanceUUID case_) {
    Set<CaseImpl> result = new HashSet<CaseImpl>();
    Query query = getSession().getNamedQuery("getCases");
    query.setString("caseId", case_.getValue());
    query.setCacheable(true);
    List<CaseImpl> theTempResult = (List<CaseImpl>) query.list();
    result.addAll(theTempResult);
    return result;
  }

  public CaseImpl getCase(ProcessInstanceUUID caseUUID, String ownerName, String labelName) {
    Query query = getSession().getNamedQuery("getCase");
    query.setString("caseUUID", caseUUID.getValue());
    query.setString("ownerName", ownerName);
    query.setString("labelName", labelName);
    query.setCacheable(true);
    return (CaseImpl) query.uniqueResult();
  }

  public Set<LabelImpl> getLabels(String ownerName, Set<String> labelsName) {
    Query query = getSession().getNamedQuery("getLabels");
    query.setString("ownerName", ownerName);
    query.setCacheable(true);
    Set<LabelImpl> result = executeSplittedQuery(LabelImpl.class, query, "labelNames", labelsName);
    return result;
  }

  public Set<LabelImpl> getLabels(Set<String> labelsName) {
    Query query = getSession().getNamedQuery("getLabelsWithName");
    query.setCacheable(true);
    Set<LabelImpl> result = executeSplittedQuery(LabelImpl.class, query, "labelNames", labelsName);
    return result;
  }

  public Set<LabelImpl> getLabelsByNameExcept(Set<String> labelNames) {
    Query query = getSession().getNamedQuery("getLabelsByNameExcept");
    query.setCacheable(true);
    Set<LabelImpl> result = executeSplittedQuery(LabelImpl.class, query, "labelNames", labelNames);
    return result;
  }


  public int getCasesNumberWithTwoLabels(String ownerName, String label1Name, String label2Name) {
    Query query = getSession().getNamedQuery("getCasesNumberWithTwoLabels");
    query.setString("label1", CaseImpl.buildLabel(ownerName, label1Name));
    query.setString("label2", CaseImpl.buildLabel(ownerName, label2Name));
    query.setReadOnly(true);

    int result = ((Long) query.uniqueResult()).intValue();
    return result;
  }

  public int getCasesNumber(String ownerName, String labelName) {
    Query query = getSession().getNamedQuery("getCasesNumber");
    query.setString("ownerName", ownerName);
    query.setString("label", labelName);
    query.setReadOnly(true);
    int result = ((Long) query.uniqueResult()).intValue();
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<CaseImpl> getCasesWithTwoLabels(String ownerName, String label1Name, String label2Name, int limit) {
    Query query = getSession().getNamedQuery("getCasesWithTwoLabelsWithLimit");
    query.setString("label1", CaseImpl.buildLabel(ownerName, label1Name));
    query.setString("label2", CaseImpl.buildLabel(ownerName, label2Name));
    query.setReadOnly(true);
    query.setMaxResults(limit);

    final List<CaseImpl> results = query.list();
    if (results != null) {
      return new HashSet<CaseImpl>(results);
    } else {
      return new HashSet<CaseImpl>();
    }
  }

  @SuppressWarnings("unchecked")
  public Set<CaseImpl> getCases(String ownerName, String labelName, int limit) {
    Query query = getSession().getNamedQuery("getLabelCases");
    query.setString("ownerName", ownerName);
    query.setString("labelName", labelName);
    query.setMaxResults(limit);
    final List<CaseImpl> results = query.list();
    if (results != null) {
      return new HashSet<CaseImpl>(results);
    } else {
      return new HashSet<CaseImpl>();
    }
  }

  public Set<ProcessInstanceUUID> getCases(String ownerName, Set<String> theLabelsName) {
    Query query = getSession().getNamedQuery("getLabelsCases");
    query.setString("ownerName", ownerName);
    query.setCacheable(true);
    return executeSplittedQuery(ProcessInstanceUUID.class, query, "labelNames", theLabelsName);
  }

  //getAllWebCases
  //getMatchingCases

  public void deleteAllCases() {
    final Session session = getSession();
    Query query = session.getNamedQuery("getAllWebCases");
    query.setCacheable(true);

    for (Object webCase : query.list()) {
      session.delete(webCase);
    }
  }

  public void deleteCases(Set<ProcessInstanceUUID> webCases) {
    final Session session = getSession();
    Query query = session.getNamedQuery("getMatchingCases");
    query.setCacheable(true);

    Set<String> uuids = new HashSet<String>();
    for (ProcessInstanceUUID webCase : webCases) {
      uuids.add(webCase.getValue());
    }
    for (CaseImpl webCase : executeSplittedQuery(CaseImpl.class, query, "uuids", uuids)) {
      session.delete(webCase);
    }
  }

  public Set<ProcessInstanceUUID> getCasesUUIDs(String ownerName, String labelName, Set<ProcessInstanceUUID> cases) {
    Collection<String> uuids = new HashSet<String>();
    if (cases != null) {
      for (ProcessInstanceUUID processInstanceUUID : cases) {
        uuids.add(processInstanceUUID.getValue());
      }
    }

    Query query = getSession().getNamedQuery("getLabelCasesUUIDsSublist");
    query.setString("ownerName", ownerName);
    query.setString("labelName", labelName);
    query.setCacheable(true);
    return executeSplittedQuery(ProcessInstanceUUID.class, query, "caseUUIDs", uuids);
  }

  public Set<ProcessInstanceUUID> getLabelCases(String labelName, Set<ProcessInstanceUUID> caseUUIDs) {
    if (caseUUIDs == null || caseUUIDs.isEmpty()) {
      return Collections.emptySet();
    }
    Collection<String> uuids = new HashSet<String>();
    for (ProcessInstanceUUID processInstanceUUID : caseUUIDs) {
      uuids.add(processInstanceUUID.getValue());
    }

    Query query = getSession().getNamedQuery("getLabelNameCases");
    query.setString("labelName", labelName);
    query.setCacheable(true);
    return executeSplittedQuery(ProcessInstanceUUID.class, query, "uuids", uuids);
  }

  public Set<CaseImpl> getLabelCases(String ownerName, Set<String> labelsNames, Set<ProcessInstanceUUID> caseUUIDs) {
    if (caseUUIDs == null || caseUUIDs.isEmpty()) {
      return Collections.emptySet();
    }
    Collection<String> uuids = new HashSet<String>();
    for (ProcessInstanceUUID processInstanceUUID : caseUUIDs) {
      uuids.add(processInstanceUUID.getValue());
    }

    Query query = getSession().getNamedQuery("getLabelsNameCases");
    query.setParameterList("labelsNames", labelsNames);
    query.setString("ownerName", ownerName);
    query.setCacheable(true);
    return executeSplittedQuery(CaseImpl.class, query, "uuids", uuids);
  }

  public Set<CaseImpl> getCases(String ownerName, String labelName, Set<ProcessInstanceUUID> caseList) {
    Collection<String> uuids = new HashSet<String>();
    if (caseList != null) {
      for (ProcessInstanceUUID processInstanceUUID : caseList) {
        uuids.add(processInstanceUUID.getValue());
      }
    }

    Query query = getSession().getNamedQuery("getLabelCasesSublist");
    query.setString("ownerName", ownerName);
    query.setString("labelName", labelName);
    query.setCacheable(true);
    return executeSplittedQuery(CaseImpl.class, query, "caseUUIDs", uuids);
  }

  public List<Integer> getNumberOfExecutingCasesPerDay(Date since, Date to) {
    List<Integer> executingCases = new ArrayList<Integer>();
    while (since.before(to) || since.equals(to)) {
      Date nextDayBegining = DateUtil.getBeginningOfTheDay(DateUtil.getNextDay(since));
      final Query query = getSession().getNamedQuery("getNumberOfExecutingCases");
      query.setCacheable(true);
      query.setLong("date", nextDayBegining.getTime());
      executingCases.add(((Long) query.uniqueResult()).intValue());
      since = DateUtil.getNextDay(since);
    }
    return executingCases;
  }

  public List<Integer> getNumberOfFinishedCasesPerDay(Date since, Date to) {
    List<Integer> finishedCases = new ArrayList<Integer>();
    while (since.before(to) || since.equals(to)) {
      Date beginningOfTheDay = DateUtil.getBeginningOfTheDay(since);
      Date nextBeginningOfTheDay = DateUtil.getBeginningOfTheDay(DateUtil.getNextDay(since));
      final Query query = getSession().getNamedQuery("getNumberOfFinishedCases");
      query.setCacheable(true);
      query.setLong("beginningOfTheDay", beginningOfTheDay.getTime());
      query.setLong("nextBeginningOfTheDay", nextBeginningOfTheDay.getTime());
      finishedCases.add(((Long) query.uniqueResult()).intValue());
      since = DateUtil.getNextDay(since);
    }
    return finishedCases;
  }

  public List<Integer> getNumberOfOpenStepsPerDay(Date since, Date to) {
    List<Integer> finishedCases = new ArrayList<Integer>();
    while (since.before(to) || since.equals(to)) {
      Date nextDayBegining = DateUtil.getBeginningOfTheDay(DateUtil.getNextDay(since));
      final Query query = getSession().getNamedQuery("getNumberOfOpenSteps2");
      query.setCacheable(true);
      query.setLong("date", nextDayBegining.getTime());
      finishedCases.add(((Long) query.uniqueResult()).intValue());
      since = DateUtil.getNextDay(since);
    }
    return finishedCases;
  }

  public int getNumberOfOpenSteps() {
    final Query query = getSession().getNamedQuery("getNumberOfOpenSteps");
    query.setCacheable(true);
    return ((Long) query.uniqueResult()).intValue();
  }

  public int getNumberOfOverdueSteps(Date currentDate) {
    final Query query = getSession().getNamedQuery("getNumberOfOverdueSteps");
    query.setCacheable(true);
    query.setLong("currentDate", currentDate.getTime());
    return ((Long) query.uniqueResult()).intValue();
  }

  public int getNumberOfStepsAtRisk(Date currentDate, Date atRisk) {
    final Query query = getSession().getNamedQuery("getNumberOfStepsAtRisk");
    query.setCacheable(true);
    query.setLong("atRisk", atRisk.getTime());
    query.setLong("currentDate", currentDate.getTime());
    return ((Long) query.uniqueResult()).intValue();
  }

  public int getNumberOfUserOpenSteps(String userId) {
    final Query query = getSession().getNamedQuery("getNumberOfUserOpenSteps");
    query.setCacheable(true);
    query.setString("userId", userId);
    return ((Long) query.uniqueResult()).intValue();
  }

  public int getNumberOfUserOverdueSteps(String userId, Date currentDate) {
    final Query query = getSession().getNamedQuery("getNumberOfUserOverdueSteps");
    query.setCacheable(true);
    query.setString("userId", userId);
    query.setLong("currentDate", currentDate.getTime());
    return ((Long) query.uniqueResult()).intValue();
  }

  public int getNumberOfUserStepsAtRisk(String userId, Date currentDate, Date atRisk) {
    final Query query = getSession().getNamedQuery("getNumberOfUserStepsAtRisk");
    query.setCacheable(true);
    query.setString("userId", userId);
    query.setLong("atRisk", atRisk.getTime());
    query.setLong("currentDate", currentDate.getTime());
    return ((Long) query.uniqueResult()).intValue();
  }

  public int getNumberOfFinishedSteps(int priority, Date since) {
    final Query query = getSession().getNamedQuery("getNumberOfFinishedSteps");
    query.setCacheable(true);
    query.setInteger("priority", priority);
    query.setLong("since", since.getTime());
    return ((Long) query.uniqueResult()).intValue();
  }

  public int getNumberOfOpenSteps(int priority) {
    final Query query = getSession().getNamedQuery("getNumberOfPriorityOpenSteps");
    query.setCacheable(true);
    query.setInteger("priority", priority);
    return ((Long) query.uniqueResult()).intValue();
  }

  public int getNumberOfUserFinishedSteps(String userId, int priority, Date since) {
    final Query query = getSession().getNamedQuery("getNumberOfUserFinishedSteps");
    query.setCacheable(true);
    query.setInteger("priority", priority);
    query.setLong("since", since.getTime());
    query.setString("userId", userId);
    return ((Long) query.uniqueResult()).intValue();
  }

  public int getNumberOfUserOpenSteps(String userId, int priority) {
    final Query query = getSession().getNamedQuery("getNumberOfPriorityUserOpenSteps");
    query.setCacheable(true);
    query.setInteger("priority", priority);
    query.setString("userId", userId);
    return ((Long) query.uniqueResult()).intValue();
  }

  @SuppressWarnings("unchecked")
  public Set<IncomingEventInstance> getIncomingEvents(ProcessInstanceUUID instanceUUID) {
    Set<IncomingEventInstance> result = new HashSet<IncomingEventInstance>();
    final Query query = getSession().getNamedQuery("getInstanceIncomingEvents");
    query.setCacheable(true);
    query.setString("instanceUUID", instanceUUID.getValue());
    List<IncomingEventInstance> theTempResult = query.list();
    result.addAll(theTempResult);
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<IncomingEventInstance> getPermanentIncomingEvents(ActivityDefinitionUUID activityUUID) {
    Set<IncomingEventInstance> result = new HashSet<IncomingEventInstance>();
    final Query query = getSession().getNamedQuery("getPermanentIncomingEvents");
    query.setCacheable(true);
    query.setString("activityUUID", activityUUID.getValue());
    List<IncomingEventInstance> theTempResult = query.list();
    result.addAll(theTempResult);
    return result;
  }

  public IncomingEventInstance getIncomingEvent(ProcessInstanceUUID instanceUUID, String name) {
    final Query query = getSession().getNamedQuery("getUniqueInstanceIncomingEvent");
    query.setCacheable(true);
    query.setString("instanceUUID", instanceUUID.getValue());
    query.setString("eventName", name);
    return (IncomingEventInstance) query.uniqueResult();
  }

  @SuppressWarnings("unchecked")
  public Set<IncomingEventInstance> getIncomingEvents(ActivityDefinitionUUID activityUUID) {
    Set<IncomingEventInstance> result = new HashSet<IncomingEventInstance>();
    final Query query = getSession().getNamedQuery("getActivityDefinitionIncomingEvents");
    query.setCacheable(true);
    query.setString("activityUUID", activityUUID.getValue());
    List<IncomingEventInstance> theTempResult = query.list();
    result.addAll(theTempResult);
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<IncomingEventInstance> getIncomingEvents(ActivityInstanceUUID activityUUID) {
    Set<IncomingEventInstance> result = new HashSet<IncomingEventInstance>();
    final Query query = getSession().getNamedQuery("getActivityInstanceIncomingEvents");
    query.setCacheable(true);
    query.setString("activityUUID", activityUUID.getValue());
    List<IncomingEventInstance> theTempResult = query.list();
    result.addAll(theTempResult);
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<IncomingEventInstance> getBoundaryIncomingEvents(ActivityInstanceUUID activityUUID) {
    Set<IncomingEventInstance> result = new HashSet<IncomingEventInstance>();
    final Query query = getSession().getNamedQuery("getActivityBoudaryIncomingEvents");
    query.setCacheable(true);
    query.setString("activityUUID", activityUUID.getValue());
    List<IncomingEventInstance> theTempResult = query.list();
    result.addAll(theTempResult);
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<OutgoingEventInstance> getBoundaryOutgoingEvents(ActivityInstanceUUID activityUUID) {
    Set<OutgoingEventInstance> result = new HashSet<OutgoingEventInstance>();
    final Query query = getSession().getNamedQuery("getActivityBoundaryOutgoingEvents");
    query.setCacheable(true);
    query.setString("activityUUID", activityUUID.getValue());
    List<OutgoingEventInstance> theTempResult = query.list();
    result.addAll(theTempResult);
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<OutgoingEventInstance> getOutgoingEvents(ProcessInstanceUUID instanceUUID) {
    Set<OutgoingEventInstance> result = new HashSet<OutgoingEventInstance>();
    final Query query = getSession().getNamedQuery("getInstanceOutgoingEvents");
    query.setCacheable(true);
    query.setString("instanceUUID", instanceUUID.getValue());
    List<OutgoingEventInstance> theTempResult = query.list();
    result.addAll(theTempResult);
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<OutgoingEventInstance> getOutgoingEvents() {
    Set<OutgoingEventInstance> result = new HashSet<OutgoingEventInstance>();
    final Query query = getSession().getNamedQuery("getOutgoingEvents");
    query.setCacheable(true);
    List<OutgoingEventInstance> theTempResult = query.list();
    result.addAll(theTempResult);
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<OutgoingEventInstance> getOutgoingEvents(String eventName, String toProcessName, String toActivityName, ActivityInstanceUUID activityUUID) {
    Set<OutgoingEventInstance> result = new HashSet<OutgoingEventInstance>();
    Query query = null;
    query = getSession().getNamedQuery("getOutgoingEventsWithUUID");
    query.setString("processName", toProcessName);
    query.setString("activityName", toActivityName);
    query.setString("activityUUID", activityUUID == null ? null : activityUUID.getValue());
    query.setString("name", eventName);
    query.setCacheable(true);
    List<OutgoingEventInstance> theTempResult = query.list();
    result.addAll(theTempResult);
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<IncomingEventInstance> getIncomingEvents(String eventName, String toProcessName, String toActivityName, ActivityInstanceUUID actiivtyUUID) {
    Set<IncomingEventInstance> result = new HashSet<IncomingEventInstance>();
    final Query query = getSession().getNamedQuery("getIncomingEventsWithUUID");
    query.setString("name", eventName);
    query.setString("processName", toProcessName);
    query.setString("activityName", toActivityName);
    query.setString("activityUUID", actiivtyUUID.getValue());
    query.setCacheable(true);
    List<IncomingEventInstance> theTempResult = query.list();
    result.addAll(theTempResult);
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<IncomingEventInstance> getSignalIncomingEvents(String signal) {
    Set<IncomingEventInstance> result = new HashSet<IncomingEventInstance>();
    final Query query = getSession().getNamedQuery("getSignalIncomingEvents");
    query.setString("eventName", signal);
    query.setCacheable(true);
    List<IncomingEventInstance> theTempResult = query.list();
    result.addAll(theTempResult);
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<EventInstance> getConsumedEvents() {
    final Set<EventInstance> result = new HashSet<EventInstance>();

    final Query oeiQuery = getSession().getNamedQuery("getOutgoingConsumedEvents");
    oeiQuery.setCacheable(true);
    result.addAll(oeiQuery.list());

    final Query ieiQuery = getSession().getNamedQuery("getIncomingConsumedEvents");
    ieiQuery.setCacheable(true);
    result.addAll(ieiQuery.list());
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<OutgoingEventInstance> getOverdueEvents() {
    final long currentTime = System.currentTimeMillis();

    final Set<OutgoingEventInstance> result = new HashSet<OutgoingEventInstance>();

    final Query oeiQuery = getSession().getNamedQuery("getOverdueEvents");
    oeiQuery.setLong("current", currentTime);
    oeiQuery.setCacheable(true);
    result.addAll(oeiQuery.list());

    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<IncomingEventInstance> getIncomingEvents() {
    Set<IncomingEventInstance> result = new HashSet<IncomingEventInstance>();
    final Query query = getSession().getNamedQuery("getIncomingEvents");
    query.setCacheable(true);
    List<IncomingEventInstance> theTempResult = query.list();
    result.addAll(theTempResult);
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<IncomingEventInstance> getActivityIncomingEvents(ActivityInstanceUUID activityUUID) {
    Set<IncomingEventInstance> result = new HashSet<IncomingEventInstance>();
    final Query query = getSession().getNamedQuery("getActivityIncomingEvents");
    query.setString("activityUUID", activityUUID.getValue());
    query.setCacheable(true);
    List<IncomingEventInstance> theTempResult = query.list();
    result.addAll(theTempResult);
    return result;
  }

  @SuppressWarnings("unchecked")
  public Set<EventCouple> getEventsCouples() {
    final Query query = getSession().getNamedQuery("getEventsCouples");
    query.setCacheable(true);
    query.setLong("current", System.currentTimeMillis());
    query.setResultTransformer(Transformers.aliasToBean(EventCouple.class));
    List<EventCouple> couples = query.list();

    Set<EventCouple> result = new HashSet<EventCouple>();
    result.addAll(couples);
    return result;
  }

  public IncomingEventInstance getIncomingEvent(long incomingId) {
    final Query query = getSession().getNamedQuery("getIncomingEvent");
    query.setCacheable(true);
    query.setLong("id", incomingId);
    return (IncomingEventInstance) query.uniqueResult();
  }

  public OutgoingEventInstance getOutgoingEvent(long outgoingId) {
    final Query query = getSession().getNamedQuery("getOutgoingEvent");
    query.setCacheable(true);
    query.setLong("id", outgoingId);
    return (OutgoingEventInstance) query.uniqueResult();
  }

  public Long getNextDueDate() {
    final Query query = getSession().getNamedQuery("getNextDueDate");
    query.setCacheable(true);
    query.setMaxResults(1);
    Object result = query.uniqueResult();
    if (result != null) {
      return (Long) result;
    }
    return null;
  }

  public Set<InternalProcessDefinition> getProcesses(Set<ProcessDefinitionUUID> definitionUUIDs) {
    Query query = getSession().getNamedQuery("getProcessesList");
    query.setCacheable(true);
    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : definitionUUIDs) {
      uuids.add(uuid.toString());
    }
    return executeSplittedQuery(InternalProcessDefinition.class, query, "definitionsUUIDs", uuids);
  }

  public Set<InternalProcessDefinition> getProcesses(Set<ProcessDefinitionUUID> definitionUUIDs, ProcessState processState) {
    Query query = getSession().getNamedQuery("getProcessesListByState");
    query.setParameter("state", processState);
    query.setCacheable(true);
    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : definitionUUIDs) {
      uuids.add(uuid.toString());
    }
    return executeSplittedQuery(InternalProcessDefinition.class, query, "definitionsUUIDs", uuids);
  }

  public List<InternalProcessDefinition> getProcesses(Set<ProcessDefinitionUUID> definitionUUIDs, int fromIndex, int pageSize) {
    final Query query = getSession().getNamedQuery("getAllProcessesList");
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : definitionUUIDs) {
      uuids.add(uuid.toString());
    }
    return executeSplittedQueryList(InternalProcessDefinition.class, query, "definitionsUUIDs", uuids);
  }

  public List<InternalProcessDefinition> getProcesses(
      Set<ProcessDefinitionUUID> definitionUUIDs, int fromIndex, int pageSize,
      ProcessDefinitionCriterion pagingCriterion) {
    Query query = null;
    switch (pagingCriterion) {
      case NAME_ASC:
        query = getSession().getNamedQuery("getAllProcessesListOrderByNameAsc");
        break;

      case LABEL_ASC:
        query = getSession().getNamedQuery("getAllProcessesListOrderByLabelAsc");
        break;

      case VERSION_ASC:
        query = getSession().getNamedQuery("getAllProcessesListOrderByVersionAsc");
        break;

      case STATE_ASC:
        query = getSession().getNamedQuery("getAllProcessesListOrderByStateAsc");
        break;

      case NAME_DESC:
        query = getSession().getNamedQuery("getAllProcessesListOrderByNameDesc");
        break;

      case LABEL_DESC:
        query = getSession().getNamedQuery("getAllProcessesListOrderByLabelDesc");
        break;

      case VERSION_DESC:
        query = getSession().getNamedQuery("getAllProcessesListOrderByVersionDesc");
        break;

      case STATE_DESC:
        query = getSession().getNamedQuery("getAllProcessesListOrderByStateDesc");
        break;

      case DEFAULT:
        query = getSession().getNamedQuery("getAllProcessesList");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : definitionUUIDs) {
      uuids.add(uuid.toString());
    }
    return executeSplittedQueryList(InternalProcessDefinition.class, query, "definitionsUUIDs", uuids);
  }

  public InternalProcessDefinition getLastProcess(Set<ProcessDefinitionUUID> definitionUUIDs, ProcessState processState) {
    Query query = getSession().getNamedQuery("getLastProcessFromProcessSetAndState");
    query.setParameter("state", processState, definitionStateUserType);
    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : definitionUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setCacheable(true);
    query.setMaxResults(1);
    Set<InternalProcessDefinition> result = executeSplittedQuery(InternalProcessDefinition.class, query, "definitionsUUIDs", uuids);
    return result.iterator().next();
  }

  public Set<InternalProcessInstance> getProcessInstances(Set<ProcessDefinitionUUID> definitionUUIDs) {
    final Query query = getSession().getNamedQuery("getProcessInstancesFromDefinitionUUIDs");
    query.setCacheable(true);
    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : definitionUUIDs) {
      uuids.add(uuid.toString());
    }
    return executeSplittedQuery(InternalProcessInstance.class, query, "definitionsUUIDs", uuids);
  }

  public Set<InternalProcessInstance> getUserInstances(String userId, Set<ProcessDefinitionUUID> definitionUUIDs) {
    Query query = getSession().getNamedQuery("getUserInstancesFromDefinitionUUIDs");
    query.setCacheable(true);
    query.setString("userId", userId);
    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : definitionUUIDs) {
      uuids.add(uuid.toString());
    }
    return executeSplittedQuery(InternalProcessInstance.class, query, "definitionsUUIDs", uuids);
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentUserInstances(String userId, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> definitionUUIDs) {
    final Query query = getSession().getNamedQuery("getParentUserInstancesPageFromDefinitionUUIDs");
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    query.setString("userId", userId);
    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : definitionUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setParameterList("definitionsUUIDs", uuids);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentUserInstances(String userId,
      int startingIndex, int pageSize,
      Set<ProcessDefinitionUUID> definitionUUIDs,
      ProcessInstanceCriterion pagingCriterion) {
    Query query = null;  	
    switch (pagingCriterion) {
      case LAST_UPDATE_ASC:			
        query = getSession().getNamedQuery("getParentUserInstancesPageFromDefinitionUUIDsOrderByLastUpdateAsc");
        break;
      case STARTED_DATE_ASC:
        query = getSession().getNamedQuery("getParentUserInstancesPageFromDefinitionUUIDsOrderByStartedDateAsc");
        break;
      case ENDED_DATE_ASC:			
        query = getSession().getNamedQuery("getParentUserInstancesPageFromDefinitionUUIDsOrderByEndedDateAsc");
        break;
      case INSTANCE_NUMBER_ASC:
        query = getSession().getNamedQuery("getParentUserInstancesPageFromDefinitionUUIDsOrderByInstanceNumberAsc");
        break;
      case INSTANCE_UUID_ASC:
        query = getSession().getNamedQuery("getParentUserInstancesPageFromDefinitionUUIDsOrderByInstanceUUIDAsc");
        break;
      case LAST_UPDATE_DESC:			
        query = getSession().getNamedQuery("getParentUserInstancesPageFromDefinitionUUIDsOrderByLastUpdateDesc");
        break;
      case STARTED_DATE_DESC:
        query = getSession().getNamedQuery("getParentUserInstancesPageFromDefinitionUUIDsOrderByStartedDateDesc");
        break;
      case ENDED_DATE_DESC:			
        query = getSession().getNamedQuery("getParentUserInstancesPageFromDefinitionUUIDsOrderByEndedDateDesc");
        break;
      case INSTANCE_NUMBER_DESC:
        query = getSession().getNamedQuery("getParentUserInstancesPageFromDefinitionUUIDsOrderByInstanceNumberDesc");
        break;
      case INSTANCE_UUID_DESC:
        query = getSession().getNamedQuery("getParentUserInstancesPageFromDefinitionUUIDsOrderByInstanceUUIDDesc");
        break;
      case DEFAULT:
        query = getSession().getNamedQuery("getParentUserInstancesPageFromDefinitionUUIDs");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(startingIndex);
    query.setMaxResults(pageSize);
    query.setString("userId", userId);
    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : definitionUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setParameterList("definitionsUUIDs", uuids);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentProcessInstancesWithActiveUser(String userId, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
    final Query query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserFromDefinitionUUIDs");
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    query.setString("userId", userId);
    List<String> uuids = new ArrayList<String>();
    for (ProcessDefinitionUUID uuid : visibleProcessUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setParameterList("definitionsUUIDs", uuids);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentProcessInstancesWithActiveUser(
      String aUserId, int aStartingIndex, int aPageSize,
      Set<ProcessDefinitionUUID> aVisibleProcessUUIDs,
      ProcessInstanceCriterion pagingCriterion) {
    Query query = null;  	
    switch (pagingCriterion) {
      case LAST_UPDATE_ASC:			
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserFromDefinitionUUIDsOrderByLastUpdateAsc");
        break;
      case STARTED_DATE_ASC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserFromDefinitionUUIDsOrderByStartedDateAsc");
        break;
      case ENDED_DATE_ASC:			
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserFromDefinitionUUIDsOrderByEndedDateAsc");
        break;
      case INSTANCE_NUMBER_ASC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserFromDefinitionUUIDsOrderByInstanceNumberAsc");
        break;
      case INSTANCE_UUID_ASC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserFromDefinitionUUIDsOrderByInstanceUUIDAsc");
        break;
      case LAST_UPDATE_DESC:			
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserFromDefinitionUUIDsOrderByLastUpdateDesc");
        break;
      case STARTED_DATE_DESC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserFromDefinitionUUIDsOrderByStartedDateDesc");
        break;
      case ENDED_DATE_DESC:			
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserFromDefinitionUUIDsOrderByEndedDateDesc");
        break;
      case INSTANCE_NUMBER_DESC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserFromDefinitionUUIDsOrderByInstanceNumberDesc");
        break;
      case INSTANCE_UUID_DESC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserFromDefinitionUUIDsOrderByInstanceUUIDDesc");
        break;
      case DEFAULT:
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserFromDefinitionUUIDs");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(aStartingIndex);
    query.setMaxResults(aPageSize);
    query.setString("userId", aUserId);

    List<String> uuids = new ArrayList<String>();
    for (ProcessDefinitionUUID uuid : aVisibleProcessUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setParameterList("definitionsUUIDs", uuids);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentProcessInstancesWithActiveUser(String userId, int fromIndex, int pageSize) {
    final Query query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUser");
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    query.setString("userId", userId);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentProcessInstancesWithActiveUser(
      String aUserId, int aStartingIndex, int aPageSize,
      ProcessInstanceCriterion pagingCriterion) {
    Query query = null;  	
    switch (pagingCriterion) {
      case LAST_UPDATE_ASC:			
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserOrderByLastUpdateAsc");
        break;
      case STARTED_DATE_ASC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserOrderByStartedDateAsc");
        break;
      case ENDED_DATE_ASC:			
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserOrderByEndedDateAsc");
        break;
      case INSTANCE_NUMBER_ASC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserOrderByInstanceNumberAsc");
        break;
      case INSTANCE_UUID_ASC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserOrderByInstanceUUIDAsc");
        break;
      case LAST_UPDATE_DESC:			
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserOrderByLastUpdateDesc");
        break;
      case STARTED_DATE_DESC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserOrderByStartedDateDesc");
        break;
      case ENDED_DATE_DESC:			
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserOrderByEndedDateDesc");
        break;
      case INSTANCE_NUMBER_DESC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserOrderByInstanceNumberDesc");
        break;
      case INSTANCE_UUID_DESC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserOrderByInstanceUUIDDesc");
        break;
      case DEFAULT:
        query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUser");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(aStartingIndex);
    query.setMaxResults(aPageSize);
    query.setString("userId", aUserId);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId, Date currentDate, Date atRisk, int startingIndex, int pageSize,
      Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
    final Query query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDateFromDefinitionUUIDs");
    query.setCacheable(true);    
    query.setString("userId", userId);
    List<String> uuids = new ArrayList<String>();
    for (ProcessDefinitionUUID uuid : visibleProcessUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setParameterList("definitionsUUIDs", uuids);
    query.setLong("atRisk", atRisk.getTime());
    query.setLong("currentDate", currentDate.getTime());
    final List<ProcessInstanceUUID> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return new ArrayList<InternalProcessInstance>(getProcessInstances(new HashSet<ProcessInstanceUUID>(results), startingIndex, pageSize));
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
      String userId, Date currentDate, Date atRisk, int startingIndex,
      int pageSize, Set<ProcessDefinitionUUID> visibleProcessUUIDs,
      ProcessInstanceCriterion pagingCriterion) {
    final Query query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDateFromDefinitionUUIDs");
    query.setCacheable(true);
    query.setString("userId", userId);
    List<String> uuids = new ArrayList<String>();
    for (ProcessDefinitionUUID uuid : visibleProcessUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setParameterList("definitionsUUIDs", uuids);
    query.setLong("atRisk", atRisk.getTime());
    query.setLong("currentDate", currentDate.getTime());
    final List<ProcessInstanceUUID> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return getProcessInstancesWithInstanceUUIDs(new HashSet<ProcessInstanceUUID>(results), startingIndex, pageSize, pagingCriterion);
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId, Date currentDate, Date atRisk, int startingIndex, int pageSize) {
    final Query query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate");
    query.setCacheable(true);
    query.setString("userId", userId);
    query.setLong("atRisk", atRisk.getTime());
    query.setLong("currentDate", currentDate.getTime());
    final List<ProcessInstanceUUID> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return new ArrayList<InternalProcessInstance>(getProcessInstances(new HashSet<ProcessInstanceUUID>(results), startingIndex, pageSize));

  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(
      String userId, Date currentDate, Date atRisk, int startingIndex,
      int pageSize, ProcessInstanceCriterion pagingCriterion) {
    final Query query = getSession().getNamedQuery("getParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate");
    query.setCacheable(true);
    query.setString("userId", userId);
    query.setLong("atRisk", atRisk.getTime());
    query.setLong("currentDate", currentDate.getTime());
    final List<ProcessInstanceUUID> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return new ArrayList<InternalProcessInstance>(getProcessInstancesWithInstanceUUIDs(
        new HashSet<ProcessInstanceUUID>(results), startingIndex, pageSize, pagingCriterion));
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentProcessInstancesWithOverdueTasks(String userId, Date currentDate, int startingIndex, int pageSize, Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
    final Query query = getSession().getNamedQuery("getParentProcessInstancesWithOverdueTasksFromDefinitionUUIDs");
    query.setCacheable(true);
    query.setString("userId", userId);
    List<String> uuids = new ArrayList<String>();
    for (ProcessDefinitionUUID uuid : visibleProcessUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setParameterList("definitionsUUIDs", uuids);
    query.setLong("currentDate", currentDate.getTime());
    final List<ProcessInstanceUUID> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return getProcessInstances(new HashSet<ProcessInstanceUUID>(results), startingIndex, pageSize);
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentProcessInstancesWithOverdueTasks(
      String userId, Date currentDate, int startingIndex, int pageSize,
      Set<ProcessDefinitionUUID> visibleProcessUUIDs,
      ProcessInstanceCriterion pagingCriterion) {
    final Query query = getSession().getNamedQuery("getParentProcessInstancesWithOverdueTasksFromDefinitionUUIDs");
    query.setCacheable(true);
    query.setString("userId", userId);
    List<String> uuids = new ArrayList<String>();
    for (ProcessDefinitionUUID uuid : visibleProcessUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setParameterList("definitionsUUIDs", uuids);
    query.setLong("currentDate", currentDate.getTime());
    final List<ProcessInstanceUUID> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return getProcessInstancesWithInstanceUUIDs(new HashSet<ProcessInstanceUUID>(results), startingIndex, pageSize, pagingCriterion);
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentProcessInstancesWithOverdueTasks(String userId, Date currentDate, int startingIndex, int pageSize) {
    final Query query = getSession().getNamedQuery("getParentProcessInstancesWithOverdueTasks");
    query.setCacheable(true);
    query.setString("userId", userId);
    query.setLong("currentDate", currentDate.getTime());
    final List<ProcessInstanceUUID> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return getProcessInstances(new HashSet<ProcessInstanceUUID>(results), 
        startingIndex, pageSize);  
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentProcessInstancesWithOverdueTasks(
      String userId, Date currentDate, int startingIndex, int pageSize,
      ProcessInstanceCriterion pagingCriterion) {
    final Query query = getSession().getNamedQuery("getParentProcessInstancesWithOverdueTasks");
    query.setCacheable(true);
    query.setString("userId", userId);
    query.setLong("currentDate", currentDate.getTime());
    final List<ProcessInstanceUUID> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return getProcessInstancesWithInstanceUUIDs(new HashSet<ProcessInstanceUUID>(results), 
        startingIndex, pageSize, pagingCriterion);  
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentProcessInstancesWithInvolvedUser(String userId, int fromIndex, int pageSize, Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
    final Query query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserFromDefinitionUUIDs");
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    query.setString("userId", userId);
    List<String> uuids = new ArrayList<String>();
    for (ProcessDefinitionUUID uuid : visibleProcessUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setParameterList("definitionsUUIDs", uuids);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentProcessInstancesWithInvolvedUser(
      String userId, int startingIndex, int pageSize,
      Set<ProcessDefinitionUUID> visibleProcessUUIDs,
      ProcessInstanceCriterion pagingCriterion) {
    Query query = null;  	
    switch (pagingCriterion) {
      case LAST_UPDATE_ASC:			
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserFromDefinitionUUIDsOrderByLastUpdateAsc");
        break;
      case STARTED_DATE_ASC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserFromDefinitionUUIDsOrderByStartedDateAsc");
        break;
      case ENDED_DATE_ASC:			
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserFromDefinitionUUIDsOrderByEndedDateAsc");
        break;
      case INSTANCE_NUMBER_ASC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserFromDefinitionUUIDsOrderByInstanceNumberAsc");
        break;
      case INSTANCE_UUID_ASC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserFromDefinitionUUIDsOrderByInstanceUUIDAsc");
        break;
      case LAST_UPDATE_DESC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserFromDefinitionUUIDsOrderByLastUpdateDesc");
        break;
      case STARTED_DATE_DESC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserFromDefinitionUUIDsOrderByStartedDateDesc");
        break;
      case ENDED_DATE_DESC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserFromDefinitionUUIDsOrderByEndedDateDesc");
        break;
      case INSTANCE_NUMBER_DESC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserFromDefinitionUUIDsOrderByInstanceNumberDesc");
        break;
      case INSTANCE_UUID_DESC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserFromDefinitionUUIDsOrderByInstanceUUIDDesc");
        break;
      case DEFAULT:
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserFromDefinitionUUIDs");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(startingIndex);
    query.setMaxResults(pageSize);
    query.setString("userId", userId);
    List<String> uuids = new ArrayList<String>();
    for (ProcessDefinitionUUID uuid : visibleProcessUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setParameterList("definitionsUUIDs", uuids);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;		
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentProcessInstancesWithInvolvedUser(String userId, int fromIndex, int pageSize) {
    final Query query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUser");
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    query.setString("userId", userId);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public List<InternalProcessInstance> getParentProcessInstancesWithInvolvedUser(
      String aUserId, int aStartingIndex, int aPageSize,
      ProcessInstanceCriterion pagingCriterion) {
    Query query = null;  	
    switch (pagingCriterion) {
      case LAST_UPDATE_ASC:			
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserOrderByLastUpdateAsc");
        break;
      case STARTED_DATE_ASC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserOrderByStartedDateAsc");
        break;
      case ENDED_DATE_ASC:			
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserOrderByEndedDateAsc");
        break;
      case INSTANCE_NUMBER_ASC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserOrderByInstanceNumberAsc");
        break;
      case INSTANCE_UUID_ASC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserOrderByInstanceUUIDAsc");
        break;
      case LAST_UPDATE_DESC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserOrderByLastUpdateDesc");
        break;
      case STARTED_DATE_DESC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserOrderByStartedDateDesc");
        break;
      case ENDED_DATE_DESC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserOrderByEndedDateDesc");
        break;
      case INSTANCE_NUMBER_DESC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserOrderByInstanceNumberDesc");
        break;
      case INSTANCE_UUID_DESC:
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUserOrderByInstanceUUIDDesc");
        break;
      case DEFAULT:
        query = getSession().getNamedQuery("getParentProcessInstancesWithInvolvedUser");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(aStartingIndex);
    query.setMaxResults(aPageSize);
    query.setString("userId", aUserId);
    final List<InternalProcessInstance> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  public Integer getNumberOfParentProcessInstancesWithActiveUser(String userId, Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
    final Query query = getSession().getNamedQuery("getNumberOfParentProcessInstancesWithActiveUserFromDefinitionUUIDs");
    query.setCacheable(true);
    query.setString("userId", userId);
    List<String> uuids = new ArrayList<String>();
    for (ProcessDefinitionUUID uuid : visibleProcessUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setParameterList("definitionsUUIDs", uuids);
    return ((Long)query.uniqueResult()).intValue();

  }
  public Integer getNumberOfParentProcessInstancesWithActiveUser(String userId){
    final Query query = getSession().getNamedQuery("getNumberOfParentProcessInstancesWithActiveUser");
    query.setCacheable(true);
    query.setString("userId", userId);
    return ((Long)query.uniqueResult()).intValue();
  }

  public Integer getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId, Date currentDate, Date atRisk, Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
    final Query query = getSession().getNamedQuery("getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDateFromDefinitionUUIDs");
    query.setCacheable(true);
    query.setString("userId", userId);
    query.setLong("atRisk", atRisk.getTime());
    query.setLong("currentDate", currentDate.getTime());
    List<String> uuids = new ArrayList<String>();
    for (ProcessDefinitionUUID uuid : visibleProcessUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setParameterList("definitionsUUIDs", uuids);
    return ((Long)query.uniqueResult()).intValue();
  }

  public Integer getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate(String userId, Date currentDate, Date atRisk) {
    final Query query = getSession().getNamedQuery("getNumberOfParentProcessInstancesWithActiveUserAndActivityInstanceExpectedEndDate");
    query.setCacheable(true);
    query.setString("userId", userId);
    query.setLong("atRisk", atRisk.getTime());
    query.setLong("currentDate", currentDate.getTime());
    return ((Long)query.uniqueResult()).intValue();
  }
  public Integer getNumberOfParentProcessInstancesWithOverdueTasks(String userId, Date currentDate, Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
    final Query query = getSession().getNamedQuery("getNumberOfParentProcessInstancesWithOverdueTasksFromDefinitionUUIDs");
    query.setCacheable(true);
    query.setString("userId", userId);
    query.setLong("currentDate", currentDate.getTime());
    List<String> uuids = new ArrayList<String>();
    for (ProcessDefinitionUUID uuid : visibleProcessUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setParameterList("definitionsUUIDs", uuids);
    return ((Long)query.uniqueResult()).intValue();
  }


  public Integer getNumberOfParentProcessInstancesWithOverdueTasks(String userId, Date currentDate) {
    final Query query = getSession().getNamedQuery("getNumberOfParentProcessInstancesWithOverdueTasks");
    query.setCacheable(true);
    query.setString("userId", userId);
    query.setLong("currentDate", currentDate.getTime());
    return ((Long)query.uniqueResult()).intValue();
  }

  public Integer getNumberOfParentProcessInstancesWithInvolvedUser(String userId, Set<ProcessDefinitionUUID> visibleProcessUUIDs){
    final Query query = getSession().getNamedQuery("getNumberOfParentProcessInstancesWithInvolvedUserFromDefinitionUUIDs");
    query.setCacheable(true);
    query.setString("userId", userId);
    List<String> uuids = new ArrayList<String>();
    for (ProcessDefinitionUUID uuid : visibleProcessUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setParameterList("definitionsUUIDs", uuids);
    return ((Long)query.uniqueResult()).intValue();
  }

  public Integer getNumberOfParentProcessInstancesWithInvolvedUser(String userId){
    final Query query = getSession().getNamedQuery("getNumberOfParentProcessInstancesWithInvolvedUser");
    query.setCacheable(true);
    query.setString("userId", userId);
    return ((Long)query.uniqueResult()).intValue();
  }
  public Integer getNumberOfParentProcessInstancesWithStartedBy(String userId, Set<ProcessDefinitionUUID> visibleProcessUUIDs){
    final Query query = getSession().getNamedQuery("getNumberOfParentProcessInstancesWithStartedByFromDefinitionUUIDs");
    query.setCacheable(true);
    query.setString("userId", userId);
    List<String> uuids = new ArrayList<String>();
    for (ProcessDefinitionUUID uuid : visibleProcessUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setParameterList("definitionsUUIDs", uuids);
    return ((Long)query.uniqueResult()).intValue();
  }
  public Integer getNumberOfParentProcessInstancesWithStartedBy(String userId){
    final Query query = getSession().getNamedQuery("getNumberOfParentProcessInstancesWithStartedBy");
    query.setCacheable(true);
    query.setString("userId", userId);
    return ((Long)query.uniqueResult()).intValue();
  }

  public int getNumberOfProcessInstances(Set<ProcessDefinitionUUID> definitionUUIDs) {
    final Query query = getSession().getNamedQuery("getNumberOfProcessInstancesFromDefinitionUUIDs");
    query.setCacheable(true);
    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : definitionUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setParameterList("definitionsUUIDs", uuids);
    return ((Long) query.uniqueResult()).intValue();
  }

  public int getNumberOfParentProcessInstances(Set<ProcessDefinitionUUID> definitionUUIDs) {
    final Query query = getSession().getNamedQuery("getNumberOfParentProcessInstancesFromDefinitionUUIDs");
    query.setCacheable(true);
    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : definitionUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setParameterList("definitionsUUIDs", uuids);
    return ((Long) query.uniqueResult()).intValue();
  }

  @SuppressWarnings("unchecked")
  public Set<InternalProcessInstance> getProcessInstancesWithTaskState(Collection<ActivityState> activityStates,
      Set<ProcessDefinitionUUID> definitionUUIDs) {
    final Query getActivitiesQuery = getSession().getNamedQuery("findActivityInstancesWithTaskStateAndDefinitionsUUIDs");
    getActivitiesQuery.setCacheable(true);
    getActivitiesQuery.setParameterList("activityStates", activityStates, activityStateUserType);
    List<String> uuids = new ArrayList<String>();
    for (ProcessDefinitionUUID uuid : definitionUUIDs) {
      uuids.add(uuid.toString());
    }
    getActivitiesQuery.setParameterList("definitionsUUIDs", uuids);
    uuids = getActivitiesQuery.list();

    final Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    if (uuids.isEmpty()) {
      return processInsts;
    }

    final Query query = getSession().getNamedQuery("findMatchingProcessInstances");
    query.setCacheable(true);
    return executeSplittedQuery(InternalProcessInstance.class, query, "instanceUUIDs", uuids);
  }

  @SuppressWarnings("unchecked")
  public Set<InternalProcessInstance> getProcessInstancesWithInstanceStates(Collection<InstanceState> instanceStates,
      Set<ProcessDefinitionUUID> visibleProcessUUIDs) {
    final Query getProcessInstancesQuery = getSession().getNamedQuery("ProcessInstancesWithInstanceStatesAndDefinitionsUUIDs");
    getProcessInstancesQuery.setCacheable(true);
    getProcessInstancesQuery.setParameterList("instanceStates", instanceStates, instanceStateUserType);
    List<String> uuids = new ArrayList<String>();
    for (ProcessDefinitionUUID uuid : visibleProcessUUIDs) {
      uuids.add(uuid.toString());
    }
    getProcessInstancesQuery.setParameterList("definitionsUUIDs", uuids);
    uuids = getProcessInstancesQuery.list();

    final Set<InternalProcessInstance> processInsts = new HashSet<InternalProcessInstance>();
    if (uuids.isEmpty()) {
      return processInsts;
    }

    final Query query = getSession().getNamedQuery("findMatchingProcessInstances");
    query.setCacheable(true);
    return executeSplittedQuery(InternalProcessInstance.class, query, "instanceUUIDs", uuids);
  }

  public TaskInstance getOneTask(String userId, ActivityState taskState, Set<ProcessDefinitionUUID> definitionUUIDs) {
    Query query = getSession().getNamedQuery("getOneTaskOfProcessFromDefinitionUUIDs");
    query.setCacheable(true);
    query.setString("userId", userId);
    List<String> uuids = new ArrayList<String>();
    for (ProcessDefinitionUUID uuid : definitionUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setParameterList("definitionsUUIDs", uuids);
    query.setParameter("state", taskState, activityStateUserType);
    query.setMaxResults(1);
    return (TaskInstance) query.uniqueResult();
  }

  public List<InternalProcessInstance> getProcessInstances(Set<ProcessDefinitionUUID> definitionUUIDs,
      int fromIndex, int pageSize) {
    final Query query = getSession().getNamedQuery("getProcessInstancesWithDefinitionUUIDs");
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : definitionUUIDs) {
      uuids.add(uuid.toString());
    }
    return executeSplittedQueryList(InternalProcessInstance.class, query, "definitionsUUIDs", uuids);
  }


  public List<InternalProcessInstance> getProcessInstances(Set<ProcessDefinitionUUID> definitionUUIDs,
      int fromIndex, int pageSize, ProcessInstanceCriterion pagingCriterion) {
    Query query = null;
    switch (pagingCriterion) {
      case LAST_UPDATE_ASC:
        query = getSession().getNamedQuery("getProcessInstancesWithDefinitionUUIDsOrderByLastUpdateAsc");
        break;
      case STARTED_DATE_ASC:
        query = getSession().getNamedQuery("getProcessInstancesWithDefinitionUUIDsOrderByStartedDateAsc");
        break;
      case ENDED_DATE_ASC:
        query = getSession().getNamedQuery("getProcessInstancesWithDefinitionUUIDsOrderByEndedDateAsc");
        break;
      case INSTANCE_NUMBER_ASC:
        query = getSession().getNamedQuery("getProcessInstancesWithDefinitionUUIDsOrderByInstanceNumberAsc");
        break;
      case INSTANCE_UUID_ASC:
        query = getSession().getNamedQuery("getProcessInstancesWithDefinitionUUIDsOrderByInstanceUUIDAsc");
        break;
      case LAST_UPDATE_DESC:
        query = getSession().getNamedQuery("getProcessInstancesWithDefinitionUUIDsOrderByLastUpdateDesc");
        break;
      case STARTED_DATE_DESC:
        query = getSession().getNamedQuery("getProcessInstancesWithDefinitionUUIDsOrderByStartedDateDesc");
        break;
      case ENDED_DATE_DESC:
        query = getSession().getNamedQuery("getProcessInstancesWithDefinitionUUIDsOrderByEndedDateDesc");
        break;
      case INSTANCE_NUMBER_DESC:
        query = getSession().getNamedQuery("getProcessInstancesWithDefinitionUUIDsOrderByInstanceNumberDesc");
        break;
      case INSTANCE_UUID_DESC:
        query = getSession().getNamedQuery("getProcessInstancesWithDefinitionUUIDsOrderByInstanceUUIDDesc");
        break;
      case DEFAULT:
        query = getSession().getNamedQuery("getProcessInstancesWithDefinitionUUIDs");
        break;
    }  	
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : definitionUUIDs) {
      uuids.add(uuid.toString());
    }
    return executeSplittedQueryList(InternalProcessInstance.class, query, "definitionsUUIDs", uuids);
  }

  public Rule findRuleByName(String name) {
    Query query = getSession().getNamedQuery("getRuleByName");
    query.setCacheable(true);
    query.setString("name", name);
    query.setMaxResults(1);
    return (Rule) query.uniqueResult();
  }

  public Rule getRule(String ruleUUID) {
    Query query = getSession().getNamedQuery("getRule");
    query.setCacheable(true);
    query.setString("ruleUUID", ruleUUID);
    query.setMaxResults(1);
    return (Rule) query.uniqueResult();
  }

  @SuppressWarnings("unchecked")
  public List<Rule> getRules() {
    Query query = getSession().getNamedQuery("getAllRules");
    query.setCacheable(true);
    List<Rule> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @Deprecated
  @SuppressWarnings("unchecked")
  public Set<Rule> findRulesByNames(Set<String> rulesNames) {
    Query query = getSession().getNamedQuery("findRulesByNames");
    query.setCacheable(true);
    query.setParameterList("names", rulesNames);
    List<Rule> results = query.list();
    if (results != null) {
      return new HashSet<Rule>(results);
    } else {
      return Collections.emptySet();
    }
  }

  @SuppressWarnings("unchecked")
  public List<Rule> getRules(Collection<String> ruleUUIDs) {
    Query query = getSession().getNamedQuery("getRules");
    query.setCacheable(true);
    query.setParameterList("uuids", ruleUUIDs);
    List<Rule> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<Rule> getAllApplicableRules(String userUUID, Collection<String> roleUUIDs, Collection<String> groupUUIDs,
      Collection<String> membershipUUIDs, String entityID) {

    Set<Rule> rules = new HashSet<Rule>();
    if (userUUID != null) {
      Query usersQuery = getSession().getNamedQuery("getAllApplicableRulesForUser");
      usersQuery.setCacheable(true);
      usersQuery.setParameter("userUUID", userUUID);
      List<Rule> userResults = usersQuery.list();
      if (userResults != null) {
        rules.addAll(userResults);
      }
    }
    if (entityID != null) {
      Query entityQuery = getSession().getNamedQuery("getAllApplicableRulesForEntity");
      entityQuery.setCacheable(true);
      entityQuery.setParameter("entityID", entityID);
      List<Rule> entityResults = entityQuery.list();
      if (entityResults != null) {
        rules.addAll(entityResults);
      }
    }
    if (groupUUIDs != null && !groupUUIDs.isEmpty()) {
      Query groupsQuery = getSession().getNamedQuery("getAllApplicableRulesForGroups");
      groupsQuery.setCacheable(true);
      groupsQuery.setParameterList("groupUUIDs", groupUUIDs);
      List<Rule> groupsResults = groupsQuery.list();
      if (groupsResults != null) {
        rules.addAll(groupsResults);
      }
    }
    if (roleUUIDs != null && !roleUUIDs.isEmpty()) {
      Query rolesQuery = getSession().getNamedQuery("getAllApplicableRulesForRoles");
      rolesQuery.setCacheable(true);
      rolesQuery.setParameterList("roleUUIDs", roleUUIDs);
      List<Rule> rolesResults = rolesQuery.list();
      if (rolesResults != null) {
        rules.addAll(rolesResults);
      }
    }
    if (membershipUUIDs != null && !membershipUUIDs.isEmpty()) {
      Query membershipsQuery = getSession().getNamedQuery("getAllApplicableRulesForMemberships");
      membershipsQuery.setCacheable(true);
      membershipsQuery.setParameterList("membershipUUIDs", membershipUUIDs);
      List<Rule> membershipsResults = membershipsQuery.list();
      if (membershipsResults != null) {
        rules.addAll(membershipsResults);
      }
    }
    List<Rule> rulesList = new ArrayList<Rule>(rules);
    Collections.sort(rulesList);
    return rulesList;
  }

  @SuppressWarnings("unchecked")
  public List<Rule> getApplicableRules(RuleType ruleType, String userUUID, Collection<String> roleUUIDs,
      Collection<String> groupUUIDs, Collection<String> membershipUUIDs, String entityID) {

    Set<Rule> rules = new HashSet<Rule>();
    if (userUUID != null) {
      Query usersQuery = getSession().getNamedQuery("getApplicableRulesForUser");
      usersQuery.setCacheable(true);
      usersQuery.setParameter("ruleType", ruleType.name());
      usersQuery.setParameter("userUUID", userUUID);
      List<Rule> userResults = usersQuery.list();
      if (userResults != null) {
        rules.addAll(userResults);
      }
    }
    if (entityID != null) {
      Query entityQuery = getSession().getNamedQuery("getApplicableRulesForEntity");
      entityQuery.setCacheable(true);
      entityQuery.setParameter("ruleType", ruleType.name());
      entityQuery.setParameter("entityID", entityID);
      List<Rule> entityResults = entityQuery.list();
      if (entityResults != null) {
        rules.addAll(entityResults);
      }
    }
    if (groupUUIDs != null && !groupUUIDs.isEmpty()) {
      Query groupsQuery = getSession().getNamedQuery("getApplicableRulesForGroups");
      groupsQuery.setCacheable(true);
      groupsQuery.setParameter("ruleType", ruleType.name());
      groupsQuery.setParameterList("groupUUIDs", groupUUIDs);
      List<Rule> groupsResults = groupsQuery.list();
      if (groupsResults != null) {
        rules.addAll(groupsResults);
      }
    }
    if (roleUUIDs != null && !roleUUIDs.isEmpty()) {
      Query rolesQuery = getSession().getNamedQuery("getApplicableRulesForRoles");
      rolesQuery.setCacheable(true);
      rolesQuery.setParameter("ruleType", ruleType.name());
      rolesQuery.setParameterList("roleUUIDs", roleUUIDs);
      List<Rule> rolesResults = rolesQuery.list();
      if (rolesResults != null) {
        rules.addAll(rolesResults);
      }
    }
    if (membershipUUIDs != null && !membershipUUIDs.isEmpty()) {
      Query membershipsQuery = getSession().getNamedQuery("getApplicableRulesForMemberships");
      membershipsQuery.setCacheable(true);
      membershipsQuery.setParameter("ruleType", ruleType.name());
      membershipsQuery.setParameterList("membershipUUIDs", membershipUUIDs);
      List<Rule> membershipsResults = membershipsQuery.list();
      if (membershipsResults != null) {
        rules.addAll(membershipsResults);
      }
    }
    List<Rule> rulesList = new ArrayList<Rule>(rules);
    Collections.sort(rulesList);
    return rulesList;
  }

  @SuppressWarnings("unchecked")
  public Set<Rule> getRulesByType(Set<String> ruleTypes) {
    Query query = getSession().getNamedQuery("getRuleListByType");
    query.setCacheable(true);
    query.setParameterList("types", ruleTypes);
    List<Rule> results = query.list();
    if (results != null) {
      return new HashSet<Rule>(results);
    } else {
      return Collections.emptySet();
    }
  }

  public RuleTypePolicy getRuleTypePolicy(RuleType ruleType) {
    Query query = getSession().getNamedQuery("getRuleTypePolicy");
    query.setCacheable(true);
    query.setParameter("type", ruleType.name());
    query.setMaxResults(1);
    return (RuleTypePolicy) query.uniqueResult();
  }

  @Deprecated
  @SuppressWarnings("unchecked")
  public Set<Rule> getAllApplicableRules(String entityID) {
    Query query = getSession().getNamedQuery("getRuleListByEntity");
    query.setCacheable(true);
    query.setString("entity", entityID);
    List<Rule> results = query.list();
    if (results != null) {
      return new HashSet<Rule>(results);
    } else {
      return Collections.emptySet();
    }
  }

  @Deprecated
  @SuppressWarnings("unchecked")
  public Set<Rule> getAllApplicableRules(String entityID, RuleType ruleType) {
    Query query = getSession().getNamedQuery("getRuleListByEntityAndType");
    query.setCacheable(true);
    query.setString("entity", entityID);
    String name = ruleType.name();
    query.setString("ruletype", name);
    List<Rule> results = query.list();
    if (results != null) {
      return new HashSet<Rule>(results);
    } else {
      return Collections.emptySet();
    }
  }

  public List<InternalProcessDefinition> getProcessesExcept(
      Set<ProcessDefinitionUUID> processUUIDs, int fromIndex, int pageSize) {
    final Query query = getSession().getNamedQuery("getAllProcessesListExcept");
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : processUUIDs) {
      uuids.add(uuid.toString());
    }
    return executeSplittedQueryList(InternalProcessDefinition.class, query, "definitionsUUIDs", uuids);
  }

  public List<InternalProcessDefinition> getProcessesExcept(
      Set<ProcessDefinitionUUID> processUUIDs, int fromIndex, int pageSize,
      ProcessDefinitionCriterion pagingCriterion) {
    Query query = null;
    switch (pagingCriterion) {
      case NAME_ASC:
        query = getSession().getNamedQuery("getAllProcessesListExceptOrderByNameAsc");
        break;

      case LABEL_ASC:
        query = getSession().getNamedQuery("getAllProcessesListExceptOrderByLabelAsc");
        break;

      case VERSION_ASC:
        query = getSession().getNamedQuery("getAllProcessesListExceptOrderByVersionAsc");
        break;

      case STATE_ASC:
        query = getSession().getNamedQuery("getAllProcessesListExceptOrderByStateAsc");
        break;

      case NAME_DESC:
        query = getSession().getNamedQuery("getAllProcessesListExceptOrderByNameDesc");
        break;

      case LABEL_DESC:
        query = getSession().getNamedQuery("getAllProcessesListExceptOrderByLabelDesc");
        break;

      case VERSION_DESC:
        query = getSession().getNamedQuery("getAllProcessesListExceptOrderByVersionDesc");
        break;

      case STATE_DESC:
        query = getSession().getNamedQuery("getAllProcessesListExceptOrderByStateDesc");
        break;

      case DEFAULT:
        query = getSession().getNamedQuery("getAllProcessesListExcept");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : processUUIDs) {
      uuids.add(uuid.toString());
    }
    return executeSplittedQueryList(InternalProcessDefinition.class, query, "definitionsUUIDs", uuids);
  }

  public int getNumberOfActivityInstanceComments(ActivityInstanceUUID activityUUID) {
    final Query query = getSession().getNamedQuery("getNumberOfActivityComments");
    query.setCacheable(true);
    query.setString("activityUUID", activityUUID.getValue());
    return ((Long) query.uniqueResult()).intValue();
  }

  public int getNumberOfComments(ProcessInstanceUUID instanceUUID) {
    final Query query = getSession().getNamedQuery("getNumberOfAllComments");
    query.setCacheable(true);
    query.setString("instanceUUID", instanceUUID.getValue());
    return ((Long) query.uniqueResult()).intValue();
  }

  public int getNumberOfProcessInstanceComments(ProcessInstanceUUID instanceUUID) {
    final Query query = getSession().getNamedQuery("getNumberOfInstanceComments");
    query.setCacheable(true);
    query.setString("instanceUUID", instanceUUID.getValue());
    return ((Long) query.uniqueResult()).intValue();
  }

  @SuppressWarnings("unchecked")
  public Map<ActivityInstanceUUID, Integer> getNumberOfActivityInstanceComments(Set<ActivityInstanceUUID> activityUUIDs) {
    final Query query = getSession().getNamedQuery("getActivitiesComments");
    query.setCacheable(true);
    List<String> uuids = new ArrayList<String>();
    for (ActivityInstanceUUID uuid : activityUUIDs) {
      uuids.add(uuid.toString());
    }
    query.setParameterList("uuids", uuids);
    List<Comment> comments = (List<Comment>) query.list();

    Map<ActivityInstanceUUID, Integer> result = new HashMap<ActivityInstanceUUID, Integer>();
    for (Comment comment : comments) {
      final ActivityInstanceUUID activityUUID = comment.getActivityUUID();
      if (result.containsKey(activityUUID)) {
        result.put(activityUUID, result.get(activityUUID) + 1);
      } else {
        result.put(activityUUID, 1);
      }

    }
    return  result;
  }

  @SuppressWarnings("unchecked")
  public List<Comment> getActivityInstanceCommentFeed(ActivityInstanceUUID activityUUID) {
    final Query query = getSession().getNamedQuery("getActivityComments");
    query.setCacheable(true);
    query.setString("activityUUID", activityUUID.getValue());
    return (List<Comment>) query.list();
  }

  @SuppressWarnings("unchecked")
  public List<Comment> getProcessInstanceCommentFeed(ProcessInstanceUUID instanceUUID) {
    final Query query = getSession().getNamedQuery("getInstanceComments");
    query.setCacheable(true);
    query.setString("instanceUUID", instanceUUID.getValue());
    return (List<Comment>) query.list();
  }

  @SuppressWarnings("unchecked")
  public List<Comment> getCommentFeed(ProcessInstanceUUID instanceUUID) {
    final Query query = getSession().getNamedQuery("getAllComments");
    query.setCacheable(true);
    query.setString("instanceUUID", instanceUUID.getValue());
    return (List<Comment>) query.list();
  }


  @SuppressWarnings("unchecked")
  public List<Rule> getRules(RuleType ruleType, int fromIndex, int pageSize) {
    final Query query = getSession().getNamedQuery("getRuleListByExactType");
    query.setCacheable(true);
    query.setString("ruletype", ruleType.name());
    query.setFirstResult(fromIndex);
    query.setMaxResults(pageSize);
    final List<Rule> results = query.list();
    if (results == null) {
      return Collections.emptyList();
    }
    return results;
  }

  @SuppressWarnings("unchecked")
  public Set<String> getAllExceptions(String entityID, RuleType ruleType) {
    final Query query = getSession().getNamedQuery("getExceptionListByEntityAndRuleType");
    query.setString("entity", entityID);
    query.setString("ruletype", ruleType.name());
    query.setCacheable(true);
    List<String> results = query.list();
    if (results != null) {
      return new HashSet<String>(results);
    } else {
      return Collections.emptySet();
    }

  }

  @SuppressWarnings("unchecked")
  public Set<ProcessDefinitionUUID> getAllProcessDefinitionUUIDs() {
    final Query query = getSession().getNamedQuery("getAllProcessDefinitionUUIDs");
    query.setCacheable(true);
    List<ProcessDefinitionUUID> results = query.list();
    if (results != null) {
      return new HashSet<ProcessDefinitionUUID>(results);
    } else {
      return Collections.emptySet();
    }

  }

  public Set<ProcessDefinitionUUID> getAllProcessDefinitionUUIDsExcept(
      Set<ProcessDefinitionUUID> processUUIDs) {
    final Query query = getSession().getNamedQuery("getAllProcessDefinitionUUIDsExcept");
    query.setCacheable(true);
    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : processUUIDs) {
      uuids.add(uuid.toString());
    }
    return executeSplittedQuery(ProcessDefinitionUUID.class, query, "definitionsUUIDs", uuids);

  }

  public long getNumberOfRules(RuleType ruleType) {
    final Query query = getSession().getNamedQuery("getNumberOfRulesForType");
    query.setString("ruletype", ruleType.name());
    query.setCacheable(true);
    return ((Long) query.uniqueResult()).intValue();
  }

  //-- Web token management.
  public WebTemporaryToken getToken(String token) {
    final Query query = getSession().getNamedQuery("getTemporaryTokenFromKey");
    query.setString("tokenKey", token);
    query.setCacheable(true);
    query.setMaxResults(1);
    return ((WebTemporaryToken) query.uniqueResult());
  }

  @SuppressWarnings("unchecked")
  public Set<WebTemporaryToken> getExpiredTokens() {
    final Query query = getSession().getNamedQuery("getExpiredTemporaryTokens");
    query.setLong("currentDate", new Date().getTime()); 
    query.setCacheable(true);
    List<WebTemporaryToken> results = query.list();
    if (results != null) {
      return new HashSet<WebTemporaryToken>(results);
    } else {
      return Collections.emptySet();
    }
  }

  @SuppressWarnings("unchecked")
  public Set<Category> getCategories(Collection<String> categoryNames) {
    final Query query = getSession().getNamedQuery("getCategoriesByName");
    query.setParameterList("names", categoryNames);
    query.setCacheable(true);
    List<Category> results = query.list();
    if (results != null) {
      return new HashSet<Category>(results);
    } else {
      return Collections.emptySet();
    }
  }

  @SuppressWarnings("unchecked")
  public Set<Category> getAllCategories() {
    final Query query = getSession().getNamedQuery("getAllCategories");
    query.setCacheable(true);
    List<Category> results = query.list();
    if (results != null) {
      return new HashSet<Category>(results);
    } else {
      return Collections.emptySet();
    }
  }

  @SuppressWarnings("unchecked")
  public Set<Category> getAllCategoriesExcept(Set<String> uuids) {
    final Query query = getSession().getNamedQuery("getAllCategoriesExcept");
    query.setParameterList("uuids", uuids);
    query.setCacheable(true);
    List<Category> results = query.list();
    if (results != null) {
      return new HashSet<Category>(results);
    } else {
      return Collections.emptySet();
    }
  }

  @SuppressWarnings("unchecked")
  public Set<CategoryImpl> getCategoriesByUUIDs(Set<CategoryUUID> uuids) {
    if (uuids == null || uuids.isEmpty()) {
      return Collections.emptySet();
    }
    final Collection<String> ids = new HashSet<String>();
    for (CategoryUUID categoryUUID : uuids) {
      ids.add(categoryUUID.getValue());
    }
    final Query query = getSession().getNamedQuery("getCategoriesByUUID");
    query.setParameterList("uuids", ids);
    query.setCacheable(true);
    List<CategoryImpl> results = query.list();
    if (results != null) {
      return new HashSet<CategoryImpl>(results);
    } else {
      return Collections.emptySet();
    }
  }

  public CategoryImpl getCategoryByUUID(String uuid) {
    final Query query = getSession().getNamedQuery("getCategoryByUUID");
    query.setString("uuid", uuid);
    query.setCacheable(true);
    return (CategoryImpl)query.uniqueResult();
  }

  @SuppressWarnings("unchecked")
  public Set<ProcessDefinitionUUID> getProcessUUIDsFromCategory(String category) {
    final Query query = getSession().getNamedQuery("getProcessUUIDsFromCategory");
    query.setString("category", category);
    query.setCacheable(true);
    List<ProcessDefinitionUUID> results = query.list();
    if (results != null) {
      return new HashSet<ProcessDefinitionUUID>(results);
    } else {
      return Collections.emptySet();
    }
  }

  /*
   * SEARCH
   */

  @SuppressWarnings("unchecked")
  public List<Object> search(SearchQueryBuilder query, int firstResult, int maxResults, Class<?> indexClass) {
    List<Object> result = new ArrayList<Object>();
    FullTextQuery hibernateQuery = createFullTextQuery(query, indexClass);
    if (hibernateQuery != null) {
      if (firstResult >= 0) {
        hibernateQuery.setFirstResult(firstResult);
        hibernateQuery.setMaxResults(maxResults);
      }
      result = hibernateQuery.list();
    }
    return result;
  }

  public int search(SearchQueryBuilder query, Class<?> indexClass) {
    int result = 0;
    FullTextQuery hibernateQuery = createFullTextQuery(query, indexClass);
    if (hibernateQuery != null) {
      result = hibernateQuery.getResultSize();
    }
    return result;
  }

  private FullTextQuery createFullTextQuery(SearchQueryBuilder query, Class<?> indexClass) {
    QueryParser parser = null;
    String expression = query.getQuery();
    if (!expression.contains("\\:")) {
      parser = new QueryParser(LUCENE_VERSION, query.getIndex().getDefaultField(), new StandardAnalyzer(LUCENE_VERSION));
    } else {
      List<String> list = query.getIndex().getAllFields();
      String[] fields = list.toArray(new String[list.size()]);
      parser = new MultiFieldQueryParser(LUCENE_VERSION, fields, new StandardAnalyzer(LUCENE_VERSION));
    }
    FullTextSession searchSession = Search.getFullTextSession(getSession());
    try {
      org.apache.lucene.search.Query luceneQuery = parser.parse(query.getQuery());
      FullTextQuery hibernateQuery = searchSession.createFullTextQuery(luceneQuery, indexClass);
      return hibernateQuery;
    } catch (ParseException e) {
      throw new BonitaRuntimeException(e.getMessage());
    }
  }

  /*
   * IDENTITY
   */
  public RoleImpl findRoleByName(String name) {
    final Query query = getSession().getNamedQuery("findRoleByName");
    query.setCacheable(true);
    query.setString("roleName", name);
    return (RoleImpl) query.uniqueResult();
  }

  public UserImpl findUserByUsername(String username) {
    final Query query = getSession().getNamedQuery("findUserByUsername");
    query.setCacheable(true);
    query.setString("username", username);
    return (UserImpl) query.uniqueResult();
  }

  @SuppressWarnings("unchecked")
  public Set<GroupImpl> findGroupsByName(String name) {
    final Query query = getSession().getNamedQuery("findGroupsByName");
    query.setCacheable(true);
    query.setString("groupName", name);
    final List<GroupImpl> results = query.list();
    if (results != null) {
      return new HashSet<GroupImpl>(results);
    } else {
      return Collections.emptySet();
    }
  }

  public ProfileMetadataImpl findProfileMetadataByName(String metadataName) {
    final Query query = getSession().getNamedQuery("findProfileMetadataByName");
    query.setCacheable(true);
    query.setString("metadataName", metadataName);
    return (ProfileMetadataImpl) query.uniqueResult();
  }

  @SuppressWarnings("unchecked")
  public List<GroupImpl> getAllGroups() {
    final Query query = getSession().getNamedQuery("getAllGroups");
    query.setCacheable(true);
    final List<GroupImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public Set<MembershipImpl> getAllMemberships() {
    final Query query = getSession().getNamedQuery("getMemberships");
    query.setCacheable(true);
    final List<MembershipImpl> results = query.list();
    if (results != null) {
      return new HashSet<MembershipImpl>(results);
    } else {
      return Collections.emptySet();
    }
  }

  @SuppressWarnings("unchecked")
  public List<ProfileMetadataImpl> getAllProfileMetadata() {
    final Query query = getSession().getNamedQuery("getProfileMetadata");
    query.setCacheable(true);
    final List<ProfileMetadataImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<RoleImpl> getAllRoles() {
    final Query query = getSession().getNamedQuery("getAllRoles");
    query.setCacheable(true);
    final List<RoleImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<UserImpl> getAllUsers() {
    final Query query = getSession().getNamedQuery("getAllUsers");
    query.setCacheable(true);
    final List<UserImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<GroupImpl> getGroupChildren(String parentGroupUUID) {
    final Query query;
    if (parentGroupUUID == null) {
      query = getSession().getNamedQuery("getRootGroups");
    } else {
      query = getSession().getNamedQuery("getGroupChildren");
      query.setString("parentGroupUUID", parentGroupUUID);
    }
    query.setCacheable(true);
    final List<GroupImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  public MembershipImpl findMembershipByRoleAndGroup(String roleUUID, String groupUUID) {
    final Query query = getSession().getNamedQuery("findMembershipByRoleAndGroup");
    query.setCacheable(true);
    query.setString("roleUUID", roleUUID);
    query.setString("groupUUID", groupUUID);
    return (MembershipImpl)query.uniqueResult();
  }

  @SuppressWarnings("unchecked")
  public Set<MembershipImpl> getMembershipsByGroup(String groupUUID) {
    final Query query = getSession().getNamedQuery("getMembershipsByGroup");
    query.setCacheable(true);
    query.setString("groupUUID", groupUUID);
    final List<MembershipImpl> results = query.list();
    if (results != null) {
      return new HashSet<MembershipImpl>(results);
    } else {
      return Collections.emptySet();
    }
  }

  @SuppressWarnings("unchecked")
  public Set<MembershipImpl> getMembershipsByRole(String roleUUID) {
    final Query query = getSession().getNamedQuery("getMembershipsByRole");
    query.setCacheable(true);
    query.setString("roleUUID", roleUUID);
    final List<MembershipImpl> results = query.list();
    if (results != null) {
      return new HashSet<MembershipImpl>(results);
    } else {
      return Collections.emptySet();
    }
  }

  public GroupImpl getGroup(String groupUUID) {
    final Query query = getSession().getNamedQuery("getGroup");
    query.setCacheable(true);
    query.setString("groupUUID", groupUUID);
    return (GroupImpl) query.uniqueResult();
  }

  public RoleImpl getRole(String roleUUID) {
    final Query query = getSession().getNamedQuery("getRole");
    query.setCacheable(true);
    query.setString("roleUUID", roleUUID);
    return (RoleImpl) query.uniqueResult();
  }

  public UserImpl getUser(String userUUID) {
    final Query query = getSession().getNamedQuery("getUser");
    query.setCacheable(true);
    query.setString("userUUID", userUUID);
    return (UserImpl) query.uniqueResult();
  }

  public MembershipImpl getMembership(String membershipUUID) {
    final Query query = getSession().getNamedQuery("getMembership");
    query.setCacheable(true);
    query.setString("membershipUUID", membershipUUID);
    return (MembershipImpl) query.uniqueResult();
  }

  public ProfileMetadataImpl getProfileMetadata(String profileMetadataUUID) {
    final Query query = getSession().getNamedQuery("getProfileMetadataByUUID");
    query.setCacheable(true);
    query.setString("profileMetadataUUID", profileMetadataUUID);
    return (ProfileMetadataImpl) query.uniqueResult();
  }

  @SuppressWarnings("unchecked")
  public List<UserImpl> getUsersByGroup(String groupUUID) {
    final Query query = getSession().getNamedQuery("getUsersByGroup");
    query.setCacheable(true);
    query.setString("groupUUID", groupUUID);
    final List<UserImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<UserImpl> getUsersByMembership(String membershipUUID) {
    final Query query = getSession().getNamedQuery("getUsersByMembership");
    query.setCacheable(true);
    query.setString("membershipUUID", membershipUUID);
    final List<UserImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<UserImpl> getUsersByRole(String roleUUID) {
    final Query query = getSession().getNamedQuery("getUsersByRole");
    query.setCacheable(true);
    query.setString("roleUUID", roleUUID);
    final List<UserImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<GroupImpl> getGroups(int fromIndex, int numberOfGroups) {
    final Query query = getSession().getNamedQuery("getAllGroups");
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(numberOfGroups);
    final List<GroupImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<GroupImpl> getGroups(int fromIndex, int numberOfGroups,
      GroupCriterion pagingCriterion) {
    Query query = null;
    switch (pagingCriterion) {
      case NAME_ASC:
        query = getSession().getNamedQuery("getAllGroupsOrderByNameAsc");
        break;

      case LABEL_ASC:
        query = getSession().getNamedQuery("getAllGroupsOrderByLabelAsc");
        break;

      case NAME_DESC:
        query = getSession().getNamedQuery("getAllGroupsOrderByNameDesc");
        break;

      case LABEL_DESC:
        query = getSession().getNamedQuery("getAllGroupsOrderByLabelDesc");
        break;

      case DEFAULT:
        query = getSession().getNamedQuery("getAllGroups");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(numberOfGroups);
    final List<GroupImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  public int getNumberOfGroups() {
    final Query query = getSession().getNamedQuery("getNumberOfGroups");
    query.setCacheable(true);
    return ((Long) query.uniqueResult()).intValue();
  }

  public int getNumberOfRoles() {
    final Query query = getSession().getNamedQuery("getNumberOfRoles");
    query.setCacheable(true);
    return ((Long) query.uniqueResult()).intValue();
  }

  public int getNumberOfUsers() {
    final Query query = getSession().getNamedQuery("getNumberOfUsers");
    query.setCacheable(true);
    return ((Long) query.uniqueResult()).intValue();
  }

  public int getNumberOfUsersByGroup(String groupUUID) {
    final Query query = getSession().getNamedQuery("getNumberOfUsersByGroup");
    query.setCacheable(true);
    query.setString("groupUUID", groupUUID);
    return ((Long) query.uniqueResult()).intValue();
  }

  public int getNumberOfUsersByRole(String roleUUID) {
    final Query query = getSession().getNamedQuery("getNumberOfUsersByRole");
    query.setCacheable(true);
    query.setString("roleUUID", roleUUID);
    return ((Long) query.uniqueResult()).intValue();
  }

  @SuppressWarnings("unchecked")
  public List<RoleImpl> getRoles(int fromIndex, int numberOfRoles) {
    final Query query = getSession().getNamedQuery("getAllRoles");
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(numberOfRoles);
    final List<RoleImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<RoleImpl> getRoles(int fromIndex, int numberOfRoles,
      RoleCriterion pagingCriterion) {
    Query query = null;
    switch (pagingCriterion) {
      case NAME_ASC:
        query = getSession().getNamedQuery("getAllRolesOrderByNameAsc");
        break;

      case LABEL_ASC:
        query = getSession().getNamedQuery("getAllRolesOrderByLabelAsc");
        break;

      case NAME_DESC:
        query = getSession().getNamedQuery("getAllRolesOrderByNameDesc");
        break;

      case LABEL_DESC:
        query = getSession().getNamedQuery("getAllRolesOrderByLabelDesc");
        break;

      case DEFAULT:
        query = getSession().getNamedQuery("getAllRoles");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(numberOfRoles);
    final List<RoleImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<UserImpl> getUsers(int fromIndex, int numberOfUsers) {
    final Query query = getSession().getNamedQuery("getAllUsers");
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(numberOfUsers);
    final List<UserImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<UserImpl> getUsers(int fromIndex, int numberOfUsers,
      UserCriterion pagingCriterion) {
    Query query = null; 
    switch (pagingCriterion) {
      case FIRST_NAME_ASC:
        query = getSession().getNamedQuery("getAllUsersOderByFirstNameAsc");
        break;
      case LAST_NAME_ASC:
        query = getSession().getNamedQuery("getAllUsersOderByLastNameAsc");
        break;
      case USER_NAME_ASC:
        query = getSession().getNamedQuery("getAllUsersOderByUserNameAsc");
        break;
      case FIRST_NAME_DESC:
        query = getSession().getNamedQuery("getAllUsersOderByFirtNameDesc");
        break;
      case LAST_NAME_DESC:
        query = getSession().getNamedQuery("getAllUsersOderByLastNameDesc");
        break;
      case USER_NAME_DESC:
        query = getSession().getNamedQuery("getAllUsersOderByUserNameDesc");
        break;
      case DEFAULT:
        query = getSession().getNamedQuery("getAllUsers");
        break;
    }
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(numberOfUsers);
    final List<UserImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<UserImpl> getUsersByGroup(String groupUUID, int fromIndex, int numberOfUsers) {
    final Query query = getSession().getNamedQuery("getUsersByGroup");
    query.setCacheable(true);
    query.setString("groupUUID", groupUUID);
    query.setFirstResult(fromIndex);
    query.setMaxResults(numberOfUsers);
    final List<UserImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<UserImpl> getUsersByGroup(String groupUUID, int fromIndex,
      int numberOfUsers, UserCriterion pagingCriterion) {
    Query query = null; 
    switch (pagingCriterion) {
      case FIRST_NAME_ASC:
        query = getSession().getNamedQuery("getUsersByGroupOrderByFirstNameAsc");
        break;
      case LAST_NAME_ASC:
        query = getSession().getNamedQuery("getUsersByGroupOrderByLastNameAsc");
        break;
      case USER_NAME_ASC:
        query = getSession().getNamedQuery("getUsersByGroupOrderByUserNameAsc");
        break;
      case FIRST_NAME_DESC:
        query = getSession().getNamedQuery("getUsersByGroupOrderByFirstNameDesc");
        break;
      case LAST_NAME_DESC:
        query = getSession().getNamedQuery("getUsersByGroupOrderByLastNameDesc");
        break;
      case USER_NAME_DESC:
        query = getSession().getNamedQuery("getUsersByGroupOrderByUserNameDesc");
        break;
      case DEFAULT:
        query = getSession().getNamedQuery("getUsersByGroup");
        break;
    }

    query.setCacheable(true);
    query.setString("groupUUID", groupUUID);
    query.setFirstResult(fromIndex);
    query.setMaxResults(numberOfUsers);
    final List<UserImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<UserImpl> getUsersByRole(String roleUUID, int fromIndex, int numberOfUsers) {
    final Query query = getSession().getNamedQuery("getUsersByRole");
    query.setCacheable(true);
    query.setString("roleUUID", roleUUID);
    query.setFirstResult(fromIndex);
    query.setMaxResults(numberOfUsers);
    final List<UserImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<UserImpl> getUsersByRole(String roleUUID, int fromIndex,
      int numberOfUsers, UserCriterion pagingCriterion) {
    Query query = null; 
    switch (pagingCriterion) {
      case FIRST_NAME_ASC:
        query = getSession().getNamedQuery("getUsersByRoleOrderByFirstNameAsc");
        break;
      case LAST_NAME_ASC:
        query = getSession().getNamedQuery("getUsersByRoleOrderByLastNameAsc");
        break;
      case USER_NAME_ASC:
        query = getSession().getNamedQuery("getUsersByRoleOrderByUserNameAsc");
        break;
      case FIRST_NAME_DESC:
        query = getSession().getNamedQuery("getUsersByRoleOrderByFirstNameDesc");
        break;
      case LAST_NAME_DESC:
        query = getSession().getNamedQuery("getUsersByRoleOrderByLastNameDesc");
        break;
      case USER_NAME_DESC:
        query = getSession().getNamedQuery("getUsersByRoleOrderByUserNameDesc");
        break;
      case DEFAULT:
        query = getSession().getNamedQuery("getUsersByRole");
        break;
    }

    query.setCacheable(true);
    query.setString("roleUUID", roleUUID);
    query.setFirstResult(fromIndex);
    query.setMaxResults(numberOfUsers);
    final List<UserImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<UserImpl> getUsersByManager(String managerUUID) {
    final Query query = getSession().getNamedQuery("getUsersByManager");
    query.setCacheable(true);
    query.setString("managerUUID", managerUUID);
    final List<UserImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<UserImpl> getUsersByDelegee(String delegeeUUID) {
    final Query query = getSession().getNamedQuery("getUsersByDelegee");
    query.setCacheable(true);
    query.setString("delegeeUUID", delegeeUUID);
    final List<UserImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<ProfileMetadataImpl> getProfileMetadata(int fromIndex, int numberOfMetadata) {
    final Query query = getSession().getNamedQuery("getProfileMetadata");
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(numberOfMetadata);
    final List<ProfileMetadataImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  public int getNumberOfProfileMetadata() {
    final Query query = getSession().getNamedQuery("getNumberOfProfileMetadata");
    query.setCacheable(true);
    return ((Long) query.uniqueResult()).intValue();
  }

  @SuppressWarnings("unchecked")
  public List<GroupImpl> getGroupChildren(String parentGroupUUID, int fromIndex, int numberOfGroups) {
    final Query query = getSession().getNamedQuery("getGroupChildren");
    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(numberOfGroups);
    query.setString("parentGroupUUID", parentGroupUUID);
    final List<GroupImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<GroupImpl> getGroupChildren(String parentGroupUUID,
      int fromIndex, int numberOfGroups, GroupCriterion pagingCriterion) {
    Query query = null;
    switch (pagingCriterion) {
      case NAME_ASC:
        query = getSession().getNamedQuery("getGroupChildrenOrderByNameAsc");
        break;

      case LABEL_ASC:
        query = getSession().getNamedQuery("getGroupChildrenOrderByLabelAsc");
        break;

      case NAME_DESC:
        query = getSession().getNamedQuery("getGroupChildrenOrderByNameDesc");
        break;

      case LABEL_DESC:
        query = getSession().getNamedQuery("getGroupChildrenOrderByLabelDesc");
        break;

      case DEFAULT:
        query = getSession().getNamedQuery("getGroupChildren");
        break;
    }

    query.setCacheable(true);
    query.setFirstResult(fromIndex);
    query.setMaxResults(numberOfGroups);
    query.setString("parentGroupUUID", parentGroupUUID);
    final List<GroupImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  public int getNumberOfGroupChildren(String parentGroupUUID) {
    final Query query = getSession().getNamedQuery("getNumberOfGroupChildren");
    query.setCacheable(true);
    query.setString("parentGroupUUID", parentGroupUUID);
    return ((Long) query.uniqueResult()).intValue();
  }

  @SuppressWarnings("unchecked")
  public List<GroupImpl> getGroups(Collection<String> groupUUIDs) {
    final Query query = getSession().getNamedQuery("getGroups");
    query.setCacheable(true);
    query.setParameterList("groupUUIDs", groupUUIDs);
    final List<GroupImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<MembershipImpl> getMemberships(Collection<String> membershipUUIDs) {
    final Query query = getSession().getNamedQuery("getMemberships");
    query.setCacheable(true);
    query.setParameterList("membershipUUIDs", membershipUUIDs);
    final List<MembershipImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<RoleImpl> getRoles(Collection<String> roleUUIDs) {
    final Query query = getSession().getNamedQuery("getRoles");
    query.setCacheable(true);
    query.setParameterList("roleUUIDs", roleUUIDs);
    final List<RoleImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<UserImpl> getUsers(Collection<String> userUUIDs) {
    final Query query = getSession().getNamedQuery("getUsers");
    query.setCacheable(true);
    query.setParameterList("userUUIDs", userUUIDs);
    final List<UserImpl> results = query.list();
    if (results != null) {
      return results;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public Set<ActivityDefinitionUUID> getProcessTaskUUIDs(ProcessDefinitionUUID definitionUUID) {
    final Query query = getSession().getNamedQuery("getProcessTaskUUIDs");
    query.setCacheable(true);
    query.setString("uuid", definitionUUID.getValue());
    Set<ActivityDefinitionUUID> activityUUIDs = new HashSet<ActivityDefinitionUUID>();
    final List<ActivityDefinitionUUID> uuids = query.list();
    if (uuids != null) {
      activityUUIDs.addAll(uuids);
    }
    return activityUUIDs;
  }

  public boolean processExists(ProcessDefinitionUUID definitionUUID) {
    final Query query = getSession().getNamedQuery("processExists");
    query.setCacheable(true);
    query.setString("uuid", definitionUUID.getValue());
    Long uuid = (Long) query.uniqueResult();
    boolean exists = true;
    if (uuid != 1) {
      exists = false;
    }
    return exists;
  }

  @SuppressWarnings("unchecked")
  public List<Long> getProcessInstancesDuration(Date since, Date until) {
    final Query query = getSession().getNamedQuery("getProcessInstancesDuration");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    List<Long> durations = query.list();
    if (durations != null) {
      return durations;
    } else {
      return Collections.emptyList();
    }    
  }

  @SuppressWarnings("unchecked")
  public List<Long> getProcessInstancesDuration(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getProcessInstancesDurationFromDefinitionUUID");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("processUUID", processUUID.getValue());
    List<Long> durations = query.list();
    if (durations != null) {
      return durations;
    } else {
      return Collections.emptyList();
    }  
  }

  public List<Long> getProcessInstancesDurationFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getProcessInstancesDurationFromDefinitionUUIDs");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());    
    Set<String> uuids = new HashSet<String>();    
    for (ProcessDefinitionUUID uuid : processUUIDs) {
      uuids.add(uuid.toString());
    }    

    List<Long> durations = executeSplittedQueryList(Long.class, query, "processUUIDs", uuids);  
    if (durations != null) {
      Collections.sort(durations);
      return durations;
    } else {
      return Collections.emptyList();
    }  
  }

  @SuppressWarnings("unchecked")
  public List<Long> getActivityInstancesExecutionTime(Date since, Date until) {
    final Query query = getSession().getNamedQuery("getActivityInstancesExecutionTime");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    List<Long> executionTimes = query.list();
    if (executionTimes != null) {
      return executionTimes;
    } else {
      return Collections.emptyList();
    }  
  }

  @SuppressWarnings("unchecked")
  public List<Long> getActivityInstancesExecutionTime(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getActivityInstancesExecutionTimeFromProcessDefinitionUUID");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("processUUID", processUUID.getValue());
    List<Long> executionTimes = query.list();
    if (executionTimes != null) {
      return executionTimes;
    } else {
      return Collections.emptyList();
    } 
  }

  public List<Long> getActivityInstancesExecutionTimeFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getActivityInstancesExecutionTimeFromProcessDefinitionUUIDs");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    Set<String> uuids = new HashSet<String>();    
    for (ProcessDefinitionUUID uuid : processUUIDs) {
      uuids.add(uuid.toString());
    }    

    List<Long> executionTimes = executeSplittedQueryList(Long.class, query, "processUUIDs", uuids);  
    if (executionTimes != null) {
      Collections.sort(executionTimes);
      return executionTimes;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<Long> getActivityInstancesExecutionTime(
      ActivityDefinitionUUID activityUUID, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getActivityInstancesExecutionTimeFromActivityDefinitionUUID");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("activityUUID", activityUUID.getValue());
    List<Long> executionTimes = query.list();
    if (executionTimes != null) {
      return executionTimes;
    } else {
      return Collections.emptyList();
    } 
  }

  public List<Long> getActivityInstancesExecutionTimeFromActivityUUIDs(
      Set<ActivityDefinitionUUID> activityUUIDs, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getActivityInstancesExecutionTimeFromActivityDefinitionUUIDs");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    Set<String> uuids = new HashSet<String>();    
    for (ActivityDefinitionUUID uuid : activityUUIDs) {
      uuids.add(uuid.toString());
    }    

    List<Long> executionTimes = executeSplittedQueryList(Long.class, query, "activityUUIDs", uuids);  
    if (executionTimes != null) {
      Collections.sort(executionTimes);
      return executionTimes;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<Long> getTaskInstancesWaitingTime(Date since, Date until) {
    final Query query = getSession().getNamedQuery("getTaskInstancesWaitingTime");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    List<Long> waitingTimes = query.list();
    if (waitingTimes != null) {
      return waitingTimes;
    } else {
      return Collections.emptyList();
    }  
  }

  @SuppressWarnings("unchecked")
  public List<Long> getTaskInstancesWaitingTime(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getTaskInstancesWaitingTimeFromProcessDefinitionUUID");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("processUUID", processUUID.getValue());
    List<Long> waitingTimes = query.list();
    if (waitingTimes != null) {
      return waitingTimes;
    } else {
      return Collections.emptyList();
    } 
  }

  public List<Long> getTaskInstancesWaitingTimeFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getTaskInstancesWaitingTimeFromProcessDefinitionUUIDs");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    Set<String> uuids = new HashSet<String>();    

    for (ProcessDefinitionUUID uuid : processUUIDs) {
      uuids.add(uuid.toString());
    }    

    List<Long> waitingTimes = executeSplittedQueryList(Long.class, query, "processUUIDs", uuids);  
    if (waitingTimes != null) {
      Collections.sort(waitingTimes);
      return waitingTimes;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<Long> getTaskInstancesWaitingTime(
      ActivityDefinitionUUID taskUUID, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getTaskInstancesWaitingTimeFromActivityDefinitionUUID");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("activityUUID", taskUUID.getValue());
    List<Long> waitingTimes = query.list();
    if (waitingTimes != null) {
      return waitingTimes;
    } else {
      return Collections.emptyList();
    } 
  }

  public List<Long> getTaskInstancesWaitingTimeFromTasksUUIDs(
      Set<ActivityDefinitionUUID> tasksUUIDs, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getTaskInstancesWaitingTimeFromActivityDefinitionUUIDs");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());

    final Set<String> uuids = new HashSet<String>();    
    for (ActivityDefinitionUUID uuid : tasksUUIDs) {
      uuids.add(uuid.toString());
    }    

    List<Long> waitingTimes = executeSplittedQueryList(Long.class, query, "activityUUIDs", uuids);  
    if (waitingTimes != null) {
      Collections.sort(waitingTimes);
      return waitingTimes;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<Long> getTaskInstancesWaitingTimeOfUser(String username,
      Date since, Date until) {
    final Query query = getSession().getNamedQuery("getTaskInstancesWaitingTimeOfUser");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("userId", username);
    List<Long> waitingTimes = query.list();
    if (waitingTimes != null) {
      return waitingTimes;
    } else {
      return Collections.emptyList();
    } 
  }

  @SuppressWarnings("unchecked")
  public List<Long> getTaskInstancesWaitingTimeOfUser(String username,
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getTaskInstancesWaitingTimeOfUserFromProcessDefinitionUUID");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("processUUID", processUUID.getValue());
    query.setString("userId", username);
    List<Long> waitingTimes = query.list();
    if (waitingTimes != null) {
      return waitingTimes;
    } else {
      return Collections.emptyList();
    } 
  }

  public List<Long> getTaskInstancesWaitingTimeOfUserFromProcessUUIDs(
      String username, Set<ProcessDefinitionUUID> processUUIDs, Date since,
      Date until) {
    final Query query = getSession().getNamedQuery("getTaskInstancesWaitingTimeOfUserFromProcessDefinitionUUIDs");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("userId", username);

    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : processUUIDs) {
      uuids.add(uuid.toString());
    }    

    List<Long> waitingTimes = executeSplittedQueryList(Long.class, query, "processUUIDs", uuids);  
    if (waitingTimes != null) {
      Collections.sort(waitingTimes);
      return waitingTimes;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<Long> getTaskInstancesWaitingTimeOfUser(String username,
      ActivityDefinitionUUID taskUUID, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getTaskInstancesWaitingTimeOfUserFromActivityDefinitionUUID");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("activityUUID", taskUUID.getValue());
    query.setString("userId", username);
    List<Long> waitingTimes = query.list();
    if (waitingTimes != null) {
      return waitingTimes;
    } else {
      return Collections.emptyList();
    } 
  }

  public List<Long> getTaskInstancesWaitingTimeOfUserFromTaskUUIDs(
      String username, Set<ActivityDefinitionUUID> taskUUIDs, Date since,
      Date until) {
    final Query query = getSession().getNamedQuery("getTaskInstancesWaitingTimeOfUserFromActivityDefinitionUUIDs");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("userId", username);

    Set<String> uuids = new HashSet<String>();
    for (ActivityDefinitionUUID uuid : taskUUIDs) {
      uuids.add(uuid.toString());
    }    

    List<Long> waitingTimes = executeSplittedQueryList(Long.class, query, "activityUUIDs", uuids);  
    if (waitingTimes != null) {
      Collections.sort(waitingTimes);
      return waitingTimes;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<Long> getActivityInstancesDuration(Date since, Date until) {
    final Query query = getSession().getNamedQuery("getActivityInstancesDuration");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    List<Long> duration = query.list();
    if (duration != null) {
      return duration;
    } else {
      return Collections.emptyList();
    } 
  }

  @SuppressWarnings("unchecked")
  public List<Long> getActivityInstancesDuration(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getActivityInstancesDurationFromProcessDefinitionUUID");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("processUUID", processUUID.getValue());
    List<Long> duration = query.list();
    if (duration != null) {
      return duration;
    } else {
      return Collections.emptyList();
    } 
  }

  public List<Long> getActivityInstancesDurationFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getActivityInstancesDurationFromProcessDefinitionUUIDs");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());    

    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : processUUIDs) {
      uuids.add(uuid.toString());
    }    

    List<Long> durations = executeSplittedQueryList(Long.class, query, "processUUIDs", uuids);  
    if (durations != null) {
      Collections.sort(durations);
      return durations;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<Long> getActivityInstancesDuration(
      ActivityDefinitionUUID activityUUID, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getActivityInstancesDurationFromActivityDefinitionUUID");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("activityUUID", activityUUID.getValue());
    List<Long> durations = query.list();
    if (durations != null) {
      return durations;
    } else {
      return Collections.emptyList();
    } 
  }

  public List<Long> getActivityInstancesDurationFromActivityUUIDs(
      Set<ActivityDefinitionUUID> activityUUIDs, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getActivityInstancesDurationFromActivityDefinitionUUIDs");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());    

    Set<String> uuids = new HashSet<String>();
    for (ActivityDefinitionUUID uuid : activityUUIDs) {
      uuids.add(uuid.toString());
    }    

    List<Long> durations = executeSplittedQueryList(Long.class, query, "activityUUIDs", uuids);  
    if (durations != null) {
      Collections.sort(durations);
      return durations;
    } else {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public List<Long> getActivityInstancesDurationByActivityType(
      org.ow2.bonita.facade.def.majorElement.ActivityDefinition.Type activityType,
      Date since, Date until) {
    final Query query = getSession().getNamedQuery("getActivityInstancesDurationByActivityType");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("activityType", activityType.toString());
    List<Long> durations = query.list();
    if (durations != null) {
      return durations;
    } else {
      return Collections.emptyList();
    } 
  }

  @SuppressWarnings("unchecked")
  public List<Long> getActivityInstancesDurationByActivityType(
      org.ow2.bonita.facade.def.majorElement.ActivityDefinition.Type activityType,
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getActivityInstancesDurationByActivityTypeFromProcessDefinitionUUID");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("activityType", activityType.toString());
    query.setString("processUUID", processUUID.getValue());
    List<Long> durations = query.list();
    if (durations != null) {
      return durations;
    } else {
      return Collections.emptyList();
    } 
  }

  public List<Long> getActivityInstancesDurationByActivityTypeFromProcessUUIDs(
      org.ow2.bonita.facade.def.majorElement.ActivityDefinition.Type activityType,
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getActivityInstancesDurationByActivityTypeFromProcessDefinitionUUIDs");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("activityType", activityType.toString());

    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : processUUIDs) {
      uuids.add(uuid.toString());
    }    

    List<Long> durations = executeSplittedQueryList(Long.class, query, "processUUIDs", uuids);  
    if (durations != null) {
      Collections.sort(durations);
      return durations;
    } else {
      return Collections.emptyList();
    }
  }

  public long getNumberOfCreatedProcessInstances(Date since, Date until) {
    final Query query = getSession().getNamedQuery("getNumberOfCreatedProcessInstances");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    return ((Long) query.uniqueResult());
  }

  public long getNumberOfCreatedProcessInstances(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getNumberOfCreatedProcessInstancesFromProcessUUID");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("processUUID", processUUID.getValue());
    return ((Long) query.uniqueResult());
  }

  public long getNumberOfCreatedActivityInstances(Date since, Date until) {
    final Query query = getSession().getNamedQuery("getNumberOfCreatedActivityInstances");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    return ((Long) query.uniqueResult());
  }

  public long getNumberOfCreatedActivityInstances(
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getNumberOfCreatedActivityInstancesFromProcessUUID");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("processUUID", processUUID.getValue());
    return ((Long) query.uniqueResult());
  }

  public long getNumberOfCreatedActivityInstancesFromProcessUUIDs(
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getNumberOfCreatedActivityInstancesFromProcessUUIDs");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : processUUIDs) {
      uuids.add(uuid.getValue());
    }
    query.setParameterList("processUUIDs", uuids);
    return ((Long) query.uniqueResult());
  }

  public long getNumberOfCreatedActivityInstances(
      ActivityDefinitionUUID activityUUID, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getNumberOfCreatedActivityInstancesFromActivityDefUUID");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("activityUUID", activityUUID.getValue());
    return ((Long) query.uniqueResult());
  }

  public long getNumberOfCreatedActivityInstancesFromActivityUUIDs(
      Set<ActivityDefinitionUUID> activityUUIDs, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getNumberOfCreatedActivityInstancesFromActivityDefUUIDs");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    Set<String> uuids = new HashSet<String>();
    for (ActivityDefinitionUUID uuid : activityUUIDs) {
      uuids.add(uuid.getValue());
    }
    query.setParameterList("activityUUIDs", uuids);
    return ((Long) query.uniqueResult());
  }

  public long getNumberOfCreatedActivityInstancesByActivityType(
      org.ow2.bonita.facade.def.majorElement.ActivityDefinition.Type activityType,
      Date since, Date until) {
    final Query query = getSession().getNamedQuery("getNumberOfCreatedActivityInstancesByActivityType");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("activityType", activityType.toString());
    return ((Long) query.uniqueResult());
  }

  public long getNumberOfCreatedActivityInstancesByActivityType(
      org.ow2.bonita.facade.def.majorElement.ActivityDefinition.Type activityType,
      ProcessDefinitionUUID processUUID, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getNumberOfCreatedActivityInstancesByActivityTypeFromProcessUUID");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("activityType", activityType.toString());
    query.setString("processUUID", processUUID.getValue());
    return ((Long) query.uniqueResult());
  }

  public long getNumberOfCreatedActivityInstancesByActivityTypeFromProcessUUIDs(
      org.ow2.bonita.facade.def.majorElement.ActivityDefinition.Type activityType,
      Set<ProcessDefinitionUUID> processUUIDs, Date since, Date until) {
    final Query query = getSession().getNamedQuery("getNumberOfCreatedActivityInstancesByActivityTypeFromProcessUUIDs");
    query.setCacheable(true);
    query.setLong("since", since.getTime());
    query.setLong("until", until.getTime());
    query.setString("activityType", activityType.toString());
    Set<String> uuids = new HashSet<String>();
    for (ProcessDefinitionUUID uuid : processUUIDs) {
      uuids.add(uuid.getValue());
    }
    query.setParameterList("processUUIDs", uuids);
    return ((Long) query.uniqueResult());
  }

  public boolean containsOtherActiveActivities(ProcessInstanceUUID instanceUUID, ActivityInstanceUUID activityUUID) {
    final Query query = getSession().getNamedQuery("getNumberOfOtherActiveActivities");
    query.setCacheable(true);
    query.setString("instanceUUID", instanceUUID.getValue());
    String actUUID = "null";
    if (activityUUID != null) {
      actUUID = activityUUID.getValue();
    }
    query.setString("activityUUID", actUUID);
    Collection<ActivityState> taskStates = new ArrayList<ActivityState>();
    taskStates.add(ActivityState.READY);
    taskStates.add(ActivityState.EXECUTING);
    taskStates.add(ActivityState.FAILED);
    query.setParameterList("states", taskStates, activityStateUserType);
    Long count = (Long) query.uniqueResult();
    boolean contains = true;
    if (count == 0) {
      contains = false;
    }
    return contains;
  }

  public IncomingEventInstance getSignalStartIncomingEvent(final List<String> processNames, final String signalCode) {
    final Query query = getSession().getNamedQuery("getSignalStartIncomingEvent");
    query.setParameterList("processNames", processNames);
    query.setString("signalCode", signalCode);
    return (IncomingEventInstance) query.uniqueResult();
  }

  @SuppressWarnings("unchecked")
  public List<IncomingEventInstance> getMessageStartIncomingEvents(final Set<String> processNames) {
    final Query query = getSession().getNamedQuery("getMessageStartIncomingEvents");
    query.setParameterList("processNames", processNames);
    return query.list();
  }

}
