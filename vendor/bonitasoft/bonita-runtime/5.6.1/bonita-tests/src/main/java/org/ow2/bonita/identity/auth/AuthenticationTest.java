package org.ow2.bonita.identity.auth;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import junit.framework.TestCase;

import org.ow2.bonita.facade.Context;
import org.ow2.bonita.facade.IdentityAPI;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.identity.User;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.util.BonitaConstants;
import org.ow2.bonita.util.SimpleCallbackHandler;

/**
 * Unit test for authentication with the login modules
 * 
 * @author Anthony Birembaut
 * 
 */
public class AuthenticationTest extends TestCase {

  static {
    // Initialize the Jaas login configuration with a default value
    final String defaultLoginFile = "src/main/resources/jaas-standard.cfg";
    final String loginFile = System.getProperty(BonitaConstants.JAAS_PROPERTY, defaultLoginFile);
    if (loginFile.equals(defaultLoginFile)) {
      System.setProperty(BonitaConstants.JAAS_PROPERTY, defaultLoginFile);
    }
  }

  public void testAuthLogin() throws Exception {
    LoginContext loginContext = login("BonitaAuth", "john", "bpm");
    loginContext.logout();
  }

  public void testAuthWrongLogin() throws Exception {
    try {
      LoginContext loginContext = login("BonitaAuth", "chuck", "norris");
      loginContext.logout();
      fail("The login should have failed");
    } catch (LoginException e) {
    }
  }

  public void testStoreLogin() throws Exception {
  	boolean isREST = isREST();
  	IdentityAPI identityAPI = null;
  	User user = null;
  	LoginContext loginContext;
  	//in REST the user must exist in the database
  	if (isREST){
  		loginContext = login("BonitaStore", "admin", "bpm");
  		identityAPI = AccessorUtil.getIdentityAPI();
  		user = identityAPI.addUser("chuck", "norris");
  		loginContext.logout();  		
  	}

		loginContext = login("BonitaStore", "chuck", "norris");
		QueryRuntimeAPI queryRuntimeAPI = AccessorUtil.getQueryRuntimeAPI();
		queryRuntimeAPI.getProcessInstances();
		loginContext.logout();

		if (isREST){
			loginContext = login("BonitaStore", "admin", "bpm");
			identityAPI.removeUserByUUID(user.getUUID());
			loginContext.logout();
		}
  }

  private LoginContext login(String contextName, String userName, String password) throws LoginException {
    LoginContext loginContext = new LoginContext(contextName, new SimpleCallbackHandler(userName, password));
    loginContext.login();
    return loginContext;
  }

  protected boolean isREST(){
    String apiType = System.getProperty(AccessorUtil.API_TYPE_PROPERTY);
    return (apiType != null && apiType.equalsIgnoreCase(Context.REST.toString()));
  }

}
