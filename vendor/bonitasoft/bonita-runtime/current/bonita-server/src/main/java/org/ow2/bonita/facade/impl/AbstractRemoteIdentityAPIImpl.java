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
import org.ow2.bonita.facade.IdentityAPI;
import org.ow2.bonita.facade.exception.GroupAlreadyExistsException;
import org.ow2.bonita.facade.exception.GroupNotFoundException;
import org.ow2.bonita.facade.exception.MembershipNotFoundException;
import org.ow2.bonita.facade.exception.MetadataAlreadyExistsException;
import org.ow2.bonita.facade.exception.MetadataNotFoundException;
import org.ow2.bonita.facade.exception.RoleAlreadyExistsException;
import org.ow2.bonita.facade.exception.RoleNotFoundException;
import org.ow2.bonita.facade.exception.UserAlreadyExistsException;
import org.ow2.bonita.facade.exception.UserNotFoundException;
import org.ow2.bonita.facade.identity.Group;
import org.ow2.bonita.facade.identity.Membership;
import org.ow2.bonita.facade.identity.ProfileMetadata;
import org.ow2.bonita.facade.identity.Role;
import org.ow2.bonita.facade.identity.User;
import org.ow2.bonita.facade.internal.AbstractRemoteIdentityAPI;
import org.ow2.bonita.facade.paging.GroupCriterion;
import org.ow2.bonita.facade.paging.RoleCriterion;
import org.ow2.bonita.facade.paging.UserCriterion;
import org.ow2.bonita.identity.auth.DomainOwner;
import org.ow2.bonita.identity.auth.PasswordOwner;
import org.ow2.bonita.identity.auth.RESTUserOwner;
import org.ow2.bonita.identity.auth.UserOwner;

/**
 * 
 * @author Elias Ricken de Medeiros
 *
 */
public class AbstractRemoteIdentityAPIImpl implements AbstractRemoteIdentityAPI {

  private static final long serialVersionUID = -8220749024878119018L;

  protected Map<String, IdentityAPI> apis = new HashMap<String, IdentityAPI>();

  protected IdentityAPI getAPI(final Map<String, String> options) {
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
      apis.put(queryList, new StandardAPIAccessorImpl().getIdentityAPI(queryList));
    }
    return apis.get(queryList);
  }

  public Role addRole(String name, final Map<String, String> options) throws RemoteException, RoleAlreadyExistsException {
    return getAPI(options).addRole(name);
  }

  public Role addRole(String name, String label, String description, final Map<String, String> options) throws RemoteException, RoleAlreadyExistsException {
    return getAPI(options).addRole(name, label, description);
  }

  @Deprecated
  public void addRoleToUser(String roleName, String username, final Map<String, String> options) throws RemoteException, UserNotFoundException,
      RoleNotFoundException {
    getAPI(options).addRoleToUser(roleName, username);
  }

  @Deprecated
  public void setUserRoles(String username, Set<String> roleNames, final Map<String, String> options) throws RemoteException, UserNotFoundException, RoleNotFoundException {
    getAPI(options).setUserRoles(username, roleNames);
  }
  
  public User addUser(String username, String password, final Map<String, String> options) throws RemoteException, UserAlreadyExistsException {
    return getAPI(options).addUser(username, password);
  }

  @Deprecated
  public User addUser(String username, String password, String firstName, String lastName, String email, final Map<String, String> options)
      throws RemoteException, UserAlreadyExistsException {
    return getAPI(options).addUser(username, password, firstName, lastName, email);
  }

  @Deprecated
  public Role getRole(String name, final Map<String, String> options) throws RemoteException, RoleNotFoundException {
    return getAPI(options).getRole(name);
  }
  
  @Deprecated
  public Set<Role> getRoles(final Map<String, String> options) throws RemoteException {
    return getAPI(options).getRoles();
  }

  @Deprecated
  public User getUser(String username, final Map<String, String> options) throws RemoteException, UserNotFoundException {
    return getAPI(options).getUser(username);
  }

  @Deprecated
  public Set<Role> getUserRoles(String username, final Map<String, String> options) throws RemoteException, UserNotFoundException {
    return getAPI(options).getUserRoles(username);
  }

  @Deprecated
  public Set<User> getUsers(final Map<String, String> options) throws RemoteException {
    return getAPI(options).getUsers();
  }

  @Deprecated
  public Set<User> getUsersInRole(String name, final Map<String, String> options) throws RemoteException, RoleNotFoundException {
    return getAPI(options).getUsersInRole(name);
  }

  @Deprecated
  public void removeRole(String name, final Map<String, String> options) throws RemoteException, RoleNotFoundException {
    getAPI(options).removeRole(name);
  }

  @Deprecated
  public void removeRoleFromUser(String roleName, String username, final Map<String, String> options) throws RemoteException, UserNotFoundException,
      RoleNotFoundException {
    getAPI(options).removeRoleFromUser(roleName, username);
  }

  @Deprecated
  public void removeUser(String username, final Map<String, String> options) throws RemoteException, UserNotFoundException {
    getAPI(options).removeUser(username);
  }

  @Deprecated
  public Role updateRole(String oldName, String name, String label, String description, final Map<String, String> options) throws RemoteException,
      RoleNotFoundException, RoleAlreadyExistsException {
    return getAPI(options).updateRole(oldName, name, label, description);
  }

  @Deprecated
  public User updateUser(String oldUsername, String username, String password, String firstName, String lastName,
    String email, final Map<String, String> options) throws RemoteException, UserNotFoundException, UserAlreadyExistsException {
    return getAPI(options).updateUser(oldUsername, username, password, firstName, lastName, email);
  }

  public User updateUserPassword(String userUUID, String password, final Map<String, String> options) throws RemoteException, UserNotFoundException {
	  return getAPI(options).updateUserPassword(userUUID, password);
  }

  public Group addGroup(String name, String parentGroupUUID, final Map<String, String> options) throws RemoteException, GroupAlreadyExistsException, GroupNotFoundException {
    return getAPI(options).addGroup(name, parentGroupUUID);
  }

  public Group addGroup(String name, String label, String description, String parentGroupUUID, final Map<String, String> options) throws RemoteException,
      GroupAlreadyExistsException, GroupNotFoundException {
    return getAPI(options).addGroup(name, label, description, parentGroupUUID);
  }

  public void addMembershipToUser(String userUUID, String membershipUUID, final Map<String, String> options) throws RemoteException,
      UserNotFoundException, MembershipNotFoundException {
    getAPI(options).addMembershipToUser(userUUID, membershipUUID);
  }

  public ProfileMetadata addProfileMetadata(String name, final Map<String, String> options) throws RemoteException, MetadataAlreadyExistsException {
    return getAPI(options).addProfileMetadata(name);
  }

  public ProfileMetadata addProfileMetadata(String name, String label, final Map<String, String> options) throws RemoteException,
      MetadataAlreadyExistsException {
    return getAPI(options).addProfileMetadata(name, label);
  }

  public User addUser(String username, String password, String firstName, String lastName, String title,
      String jobTitle, String managerUserUUID, Map<String, String> profileMetadata, final Map<String, String> options) throws RemoteException,
      UserAlreadyExistsException, UserNotFoundException, MetadataNotFoundException {
    return getAPI(options).addUser(username, password, firstName, lastName, title, jobTitle, managerUserUUID, profileMetadata);
  }

  public ProfileMetadata findProfileMetadataByName(String metadataName, final Map<String, String> options) throws RemoteException,
      MetadataNotFoundException {
    return getAPI(options).findProfileMetadataByName(metadataName);
  }

  public Role findRoleByName(String name, final Map<String, String> options) throws RemoteException, RoleNotFoundException {
    return getAPI(options).findRoleByName(name);
  }

  public User findUserByUserName(String username, final Map<String, String> options) throws RemoteException, UserNotFoundException {
    return getAPI(options).findUserByUserName(username);
  }

  public List<Group> getAllGroups(final Map<String, String> options) throws RemoteException {
    return getAPI(options).getAllGroups();
  }

  public List<ProfileMetadata> getAllProfileMetadata(final Map<String, String> options) throws RemoteException {
    return getAPI(options).getAllProfileMetadata();
  }

  public List<Role> getAllRoles(final Map<String, String> options) throws RemoteException {
    return getAPI(options).getAllRoles();
  }

  public List<User> getAllUsers(final Map<String, String> options) throws RemoteException {
    return getAPI(options).getAllUsers();
  }

  public List<User> getAllUsersInGroup(String groupUUID, final Map<String, String> options) throws RemoteException, GroupNotFoundException {
    return getAPI(options).getAllUsersInGroup(groupUUID);
  }

  public List<User> getAllUsersInMembership(String membershipUUID, final Map<String, String> options) throws RemoteException, MembershipNotFoundException {
    return getAPI(options).getAllUsersInMembership(membershipUUID);
  }

  public List<User> getAllUsersInRole(String roleUUID, final Map<String, String> options) throws RemoteException, RoleNotFoundException {
    return getAPI(options).getAllUsersInRole(roleUUID);
  }

  public List<User> getAllUsersInRoleAndGroup(String roleUUID, String groupUUID, final Map<String, String> options) throws RemoteException,
      RoleNotFoundException, GroupNotFoundException {
    return getAPI(options).getAllUsersInRoleAndGroup(roleUUID, groupUUID);
  }

  public List<Group> getChildrenGroupsByUUID(String groupUUID, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getChildrenGroupsByUUID(groupUUID);
  }

  public List<Group> getChildrenGroups(String groupUUID, int fromIndex, int numberOfGroups, final Map<String, String> options) throws RemoteException,
      GroupNotFoundException {
    return getAPI(options).getChildrenGroups(groupUUID, fromIndex, numberOfGroups);
  }

  public List<Group> getChildrenGroups(String groupUUID, int fromIndex,
      int numberOfGroups, GroupCriterion pagingCriterion,
      Map<String, String> options) throws GroupNotFoundException,
      RemoteException {
    return getAPI(options).getChildrenGroups(groupUUID, fromIndex, numberOfGroups, pagingCriterion);
  }

  public int getNumberOfChildrenGroups(String groupUUID, final Map<String, String> options) throws RemoteException, GroupNotFoundException {
    return getAPI(options).getNumberOfChildrenGroups(groupUUID);
  }
  
  public Group getGroupByUUID(String groupUUID, final Map<String, String> options) throws RemoteException, GroupNotFoundException {
    return getAPI(options).getGroupByUUID(groupUUID);
  }

  public List<Group> getGroups(int fromIndex, int numberOfGroups, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getGroups(fromIndex, numberOfGroups);
  }

  public List<Group> getGroups(int fromIndex, int numberOfGroups,
      GroupCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getGroups(fromIndex, numberOfGroups, pagingCriterion);    
  }

  public Membership getMembershipByUUID(String membershipUUID, final Map<String, String> options) throws RemoteException, MembershipNotFoundException {
    return getAPI(options).getMembershipByUUID(membershipUUID);
  }

  public Membership getMembershipForRoleAndGroup(String roleUUID, String groupUUID, final Map<String, String> options) throws RemoteException,
      RoleNotFoundException, GroupNotFoundException {
    return getAPI(options).getMembershipForRoleAndGroup(roleUUID, groupUUID);
  }

  public int getNumberOfGroups(final Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfGroups();
  }

  public int getNumberOfRoles(final Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfRoles();
  }

  public int getNumberOfUsers(final Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfUsers();
  }

  public int getNumberOfUsersInGroup(String groupUUID, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfUsersInGroup(groupUUID);
  }

  public int getNumberOfUsersInRole(String roleUUID, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfUsersInRole(roleUUID);
  }

  public ProfileMetadata getProfileMetadataByUUID(String metadataUUID, final Map<String, String> options) throws RemoteException,
      MetadataNotFoundException {
    return getAPI(options).getProfileMetadataByUUID(metadataUUID);
  }

  public List<ProfileMetadata> getProfileMetadata(int fromIndex, int numberOfMetadata, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getProfileMetadata(fromIndex, numberOfMetadata);
  }
  
  public int getNumberOfProfileMetadata(final Map<String, String> options) throws RemoteException {
    return getAPI(options).getNumberOfProfileMetadata();
  }

  public Role getRoleByUUID(String roleUUID, final Map<String, String> options) throws RemoteException, RoleNotFoundException {
    return getAPI(options).getRoleByUUID(roleUUID);
  }

  public List<Role> getRoles(int fromIndex, int numberOfRoles, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getRoles(fromIndex, numberOfRoles);
  }

  public List<Role> getRoles(int fromIndex, int numberOfRoles,
      RoleCriterion pagingCriterion, Map<String, String> options)
      throws RemoteException {
    return getAPI(options).getRoles(fromIndex, numberOfRoles, pagingCriterion);
  }

  public User getUserByUUID(String userUUID, final Map<String, String> options) throws RemoteException, UserNotFoundException {
    return getAPI(options).getUserByUUID(userUUID);
  }

  public List<User> getUsers(int fromIndex, int numberOfUsers, final Map<String, String> options) throws RemoteException {
    return getAPI(options).getUsers(fromIndex, numberOfUsers);
  }
  
  public List<User> getUsers(int fromIndex, int numberOfUsers,
			UserCriterion pagingCriterion, Map<String, String> options)
			throws RemoteException {
  	return getAPI(options).getUsers(fromIndex, numberOfUsers, pagingCriterion);
	}

  public List<User> getUsersInGroup(String groupUUID, int fromIndex, int numberOfUsers, final Map<String, String> options) throws RemoteException,
      GroupNotFoundException {
    return getAPI(options).getUsersInGroup(groupUUID, fromIndex, numberOfUsers);
  }

  public List<User> getUsersInGroup(String groupUUID, int fromIndex,
      int numberOfUsers, UserCriterion pagingCriterion,
      Map<String, String> options) throws GroupNotFoundException,
      RemoteException {
    return getAPI(options).getUsersInGroup(groupUUID, fromIndex, numberOfUsers, pagingCriterion);
  }

  public List<User> getUsersInRole(String roleUUID, int fromIndex, int numberOfUsers, final Map<String, String> options) throws RemoteException,
      RoleNotFoundException {
    return getAPI(options).getUsersInRole(roleUUID, fromIndex, numberOfUsers);
  }

  public List<User> getUsersInRole(String roleUUID, int fromIndex,
      int numberOfUsers, UserCriterion pagingCriterion,
      Map<String, String> options) throws RoleNotFoundException,
      RemoteException {
    return getAPI(options).getUsersInRole(roleUUID, fromIndex, numberOfUsers, pagingCriterion);
  }

  public void removeGroupByUUID(String groupUUID, final Map<String, String> options) throws RemoteException, GroupNotFoundException {
    getAPI(options).removeGroupByUUID(groupUUID);
  }

  public void removeMembershipFromUser(String userUUID, String membershipUUID, final Map<String, String> options) throws RemoteException,
      UserNotFoundException, MembershipNotFoundException {
    getAPI(options).removeMembershipFromUser(userUUID, membershipUUID);
  }

  public void removeProfileMetadataByUUID(String profileMetadataUUID, final Map<String, String> options) throws RemoteException, MetadataNotFoundException {
    getAPI(options).removeProfileMetadataByUUID(profileMetadataUUID);
  }

  public void removeRoleByUUID(String roleUUID, final Map<String, String> options) throws RemoteException, RoleNotFoundException {
    getAPI(options).removeRoleByUUID(roleUUID);
  }

  public void removeUserByUUID(String userUUID, final Map<String, String> options) throws RemoteException, UserNotFoundException {
    getAPI(options).removeUserByUUID(userUUID);
  }

  public Group updateGroupByUUID(String groupUUID, String name, String label, String description, String parentGroupUUID, final Map<String, String> options)
      throws RemoteException, GroupNotFoundException, GroupAlreadyExistsException {
    return getAPI(options).updateGroupByUUID(groupUUID, name, label, description, parentGroupUUID);
  }

  public ProfileMetadata updateProfileMetadataByUUID(String profileMetadataUUID, String name, String label, final Map<String, String> options)
      throws RemoteException, MetadataNotFoundException, MetadataAlreadyExistsException {
    return getAPI(options).updateProfileMetadataByUUID(profileMetadataUUID, name, label);
  }

  public Role updateRoleByUUID(String roleUUID, String name, String label, String description, final Map<String, String> options) throws RemoteException,
      RoleNotFoundException, RoleAlreadyExistsException {
    return getAPI(options).updateRoleByUUID(roleUUID, name, label, description);
  }

  public User updateUserByUUID(String userUUID, String username, String firstName, String lastName, String title,
      String jobTitle, String managerUserUUID, Map<String, String> profileMetadata, final Map<String, String> options) throws RemoteException,
      UserNotFoundException, UserAlreadyExistsException, MetadataNotFoundException {
    return getAPI(options).updateUserByUUID(userUUID, username, firstName, lastName, title, jobTitle, managerUserUUID, profileMetadata);
  }

  public void updateUserDelegee(String userUUID, String delegeeUserUUID, final Map<String, String> options) throws RemoteException, UserNotFoundException {
    getAPI(options).updateUserDelegee(userUUID, delegeeUserUUID);
  }

  public void updateUserPersonalContactInfo(String userUUID, String email, String phoneNumber, String mobileNumber,
      String faxNumber, String building, String room, String address, String zipCode, String city, String state,
      String country, String website, final Map<String, String> options) throws RemoteException, UserNotFoundException {
    getAPI(options).updateUserPersonalContactInfo(userUUID, email, phoneNumber, mobileNumber, faxNumber, building, room, address, zipCode, city, state, country, website);
  }

  public void updateUserProfessionalContactInfo(String userUUID, String email, String phoneNumber, String mobileNumber,
      String faxNumber, String building, String room, String address, String zipCode, String city, String state,
      String country, String website, final Map<String, String> options) throws RemoteException, UserNotFoundException {
    getAPI(options).updateUserProfessionalContactInfo(userUUID, email, phoneNumber, mobileNumber, faxNumber, building, room, address, zipCode, city, state, country, website);
  }

  public List<User> getUsersByManagerUUID(String managerUUID, final Map<String, String> options) throws RemoteException, UserNotFoundException {
    return getAPI(options).getUsersByManagerUUID(managerUUID);
  }

  public User importUser(String userUUID, String username, String passwordHash, String firstName, String lastName,
      String title, String jobTitle, String managerUserUUID, Map<String, String> profileMetadata,
      Map<String, String> options) throws RemoteException, UserAlreadyExistsException, MetadataNotFoundException {
    return getAPI(options).importUser(userUUID, username, passwordHash, firstName, lastName, title, jobTitle, managerUserUUID, profileMetadata);
  }

  public Group importGroup(String uuid, String name, String label, String description, String parentGroupUUID,
      Map<String, String> options) throws RemoteException, GroupAlreadyExistsException, GroupNotFoundException {
    return getAPI(options).importGroup(uuid, name, label, description, parentGroupUUID);
  }

  public Role importRole(String uuid, String name, String label, String description, Map<String, String> options)
      throws RemoteException, RoleAlreadyExistsException {
    return getAPI(options).importRole(uuid, name, label, description);
  }  

  public Boolean groupExists(String groupUUID, Map<String, String> options) throws RemoteException {
    return getAPI(options).groupExists(groupUUID);
  }
  
  public Group getGroupUsingPath(List<String> path, Map<String, String> options) throws RemoteException {
    return getAPI(options).getGroupUsingPath(path);
  }

}
