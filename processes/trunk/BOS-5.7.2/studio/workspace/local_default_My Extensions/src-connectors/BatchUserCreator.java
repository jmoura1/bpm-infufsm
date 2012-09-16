import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.ow2.bonita.connector.core.ConnectorError;
import org.ow2.bonita.connector.core.ProcessConnector;

import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.IdentityAPI;
import org.ow2.bonita.facade.impl.StandardAPIAccessorImpl;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.facade.exception.RoleNotFoundException;
import org.ow2.bonita.facade.exception.UserNotFoundException;
import org.ow2.bonita.facade.exception.MembershipNotFoundException;
import org.ow2.bonita.facade.identity.Group;
import org.ow2.bonita.facade.identity.User;
import org.ow2.bonita.facade.identity.Role;
import org.ow2.bonita.facade.identity.Membership;

/**
 * @author Andrea Charao
 * 
 */
public class BatchUserCreator extends ProcessConnector {

    private static final Log LOGGER = LogFactory.getLog(BatchUserCreator.class.getName());
    private List<List<String>> memberList;
    private IdentityAPI identityAPI = null;
    // DO NOT REMOVE NOR RENAME THIS FIELD
    private java.lang.String memberListVar;

    private User getUser(String username) throws Exception {
        User user = null;
        assert identityAPI != null;
        try {
            user = identityAPI.findUserByUserName(username);
        } catch (UserNotFoundException e) {
            LOGGER.info("User " + username + " not found, so create it");
            user = identityAPI.addUser(username, "bpm");
        }
        return user;
    }

    private Group getGroup(String groupname) throws Exception {
        assert identityAPI != null;
        Group group = identityAPI.getGroupUsingPath(Arrays.asList(groupname));
        if (group == null) {
            LOGGER.info("Group " + groupname + " not found, so create it");
            group = identityAPI.addGroup(groupname, groupname, "", null);
        }
        return group;
    }

    private Role getRole(String rolename) throws Exception {
        assert identityAPI != null;
        Role role = null;
        try {
            role = identityAPI.findRoleByName(rolename);
        } catch (RoleNotFoundException e) {
            LOGGER.info("Role " + rolename + " not found, so create it");
            role = identityAPI.addRole(rolename, rolename, "");
        }
        return role;
    }

    /*
     * Example user/membership lists: ${[["joana","tutors:user","board:admin"],
     * ["john","staff:user","trainers:user","board:user"],
     * ["james","staff:user","trainers:user"],
     * ["jack","staff:user","trainers:user"]]}
     * 
     * ${[["joana"], ["john"], ["james"], ["jack"]]}
     * 
     * ${[["joana","tutors","board:admin"], ["john","staff","trainers","board"],
     * ["james","staff","trainers"], ["jack","staff","trainers"]]}
     */

    private void addUserAndMembership(List<String> mbshipStrings) throws Exception {

        assert identityAPI != null;
        String username = mbshipStrings.get(0); // username
        User user = getUser(username);

        if (mbshipStrings.size() == 1) // only username
            return;
        
        ListIterator<String> itr = mbshipStrings.listIterator(1); // group:role pairs
        while (itr.hasNext()) {
            String strGroupRole = (String) itr.next();
            String[] pair = strGroupRole.split(":");
            Group group = getGroup(pair[0]);
            Role role = (pair.length > 1) ? getRole(pair[1]) : getRole(identityAPI.USER_ROLE_NAME);
            Membership mbship = identityAPI.getMembershipForRoleAndGroup(role.getUUID(), group.getUUID());
            try {
                identityAPI.addMembershipToUser(user.getUUID(), mbship.getUUID());
            } catch (MembershipNotFoundException e) {
                LOGGER.error("Membership not found");
            }
        }
    }

    @Override
    protected void executeConnector() throws Exception {

        // get identityAPI for further use
        identityAPI = new StandardAPIAccessorImpl().getIdentityAPI(); 

        for (List<String> mbshipStrings : memberList)
            addUserAndMembership(mbshipStrings);
    }

    @Override
    protected List<ConnectorError> validateValues() {

        List<ConnectorError> errors = new ArrayList<ConnectorError>();

        for (List<String> mbshipStrings : memberList) {
            if (mbshipStrings.size() < 1) {
                ConnectorError error = new ConnectorError("memberListVar",
                        new IllegalArgumentException("Missing username"));
                errors.add(error);
            }
        }
        return errors;

    }

    /**
     * Setter for input argument 'memberListVar' DO NOT REMOVE NOR RENAME THIS
     * SETTER, unless you also change the related entry in the XML descriptor
     * file
     */
    public void setMemberListVar(java.lang.String memberListVar)
    throws Exception {
        this.memberListVar = memberListVar;
        APIAccessor accessor = new StandardAPIAccessorImpl();
        QueryRuntimeAPI queryRuntimeAPI = accessor.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_JOURNAL_KEY);
        this.memberList = (List<List<String>>) queryRuntimeAPI.getVariable(
                getActivityInstanceUUID(), memberListVar);
    }

}
