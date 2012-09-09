/**
 * Copyright (C) 2009 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.console.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.console.client.common.Environment;
import org.bonitasoft.console.client.common.RpcConsoleServices;
import org.bonitasoft.console.client.common.data.AsyncHandler;
import org.bonitasoft.console.client.controller.AbstractViewController;
import org.bonitasoft.console.client.controller.UserRightsManager;
import org.bonitasoft.console.client.i18n.ConsoleMessages;
import org.bonitasoft.console.client.users.UserProfile;
import org.bonitasoft.console.security.client.users.User;
import org.bonitasoft.forms.client.view.common.DOMUtils;
import org.bonitasoft.forms.client.view.common.RpcFormsServices;
import org.bonitasoft.forms.client.view.common.URLUtils;
import org.bonitasoft.forms.client.view.common.URLUtilsFactory;
import org.bonitasoft.forms.client.view.controller.FormViewControllerFactory;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class BonitaConsole implements EntryPoint {

    protected static final String CONSOLE_STATIC_CONTENT_ELEMENT_ID = "static_console";

    public static final String UIMODE_PARAM = "ui";

    public static final String DEFAULT_THEME = "BonitaConsole"; // TODO internationalisation...
    
    /**
     * Utility Class form DOM manipulation
     */
    protected URLUtils urlUtils = URLUtilsFactory.getInstance();

    public static UserProfile userProfile = null;
    protected String myIdentityKey;
    protected boolean myAutoLogin = false;
    protected String formID ;
    protected Map<String, Object> urlContext;
    protected String myApplicationMode;
    protected boolean themeLoaded = false;

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        parseHashParameters();
        String autoLoginStr = Window.Location.getParameter(URLUtils.AUTO_LOGIN_PARAM);
        if (autoLoginStr != null) {
            myAutoLogin = Boolean.parseBoolean(autoLoginStr);
        }
        String locale = urlUtils.getLocale();
        urlUtils.saveLocale(locale);

        login(locale);
    }


    /**
     * Performs the login
     * 
     * @param locale
     */
    protected void login(final String locale) {

        myIdentityKey = URLUtils.getInstance().getHashParameter(URLUtils.USER_CREDENTIALS_PARAM);
        if (URLUtils.FORM_ONLY_APPLICATION_MODE.equals(myApplicationMode) || URLUtils.FULL_FORM_APPLICATION_MODE.equals(myApplicationMode)) {
            GWT.runAsync(new RunAsyncCallback() {
                public void onSuccess() {                    
                    final UserLoginHandler myUserLoginCallBack = new UserLoginHandler();
                    if (myIdentityKey != null) {
                        RpcFormsServices.getLoginService().formLogin(myIdentityKey, locale, myUserLoginCallBack);
                    } else {
                        RpcFormsServices.getLoginService().formIsAlreadyLoggedIn(locale, formID, urlContext, myUserLoginCallBack);
                    }
                }

                public void onFailure(Throwable aT) {
                    GWT.log("Unable to load asynchronous script!", aT);
                    Window.alert("Unable to load asynchronous script!" + aT.getMessage());
                }
            });

        } else {
            GWT.runAsync(new RunAsyncCallback() {
                public void onSuccess() {
                    final UserProfileLoginHandler theUserProfileLoginCallBack = new UserProfileLoginHandler();
                    if (myIdentityKey != null) {
                        RpcConsoleServices.getLoginService().consoleLogin(myIdentityKey, locale, theUserProfileLoginCallBack);
                    } else {
                        RpcConsoleServices.getLoginService().consoleIsAlreadyLoggedIn(locale, formID,urlContext , theUserProfileLoginCallBack);
                    }
                }

                public void onFailure(Throwable aT) {
                    GWT.log("Unable to load asynchronous script!", aT);
                    Window.alert("Unable to load asynchronous script!" + aT.getMessage());
                }
            });

        }

    }

    /**
     * Parses the hash parameters
     */
    protected final void parseHashParameters() {
        formID = urlUtils.getFormID();
        urlContext = urlUtils.getHashParameters();
        myApplicationMode = (String) urlContext.get(URLUtils.VIEW_MODE_PARAM);
    }

    /**
     * Handler allowing to know if whether a user is already logged in or not
     */
    protected final class UserProfileLoginHandler implements AsyncCallback<UserProfile> {

        public void onSuccess(final UserProfile aUserProfile) {
            handleLogin(aUserProfile);
        }

        public void onFailure(Throwable aCaught) {
            urlUtils.showLoginView();
        }
    }
    
    
    /**
     * Get any todolist Form URL
     */
    protected final class GetAnyTodolistFormHandler implements AsyncCallback<Map<String, Object>> {

        public void onFailure(final Throwable aT) {
            GWT.log("Unable to get any todolist form URL", aT);
            List<String> hashParamsToRemove = new ArrayList<String>();
            hashParamsToRemove.add(URLUtils.TODOLIST_PARAM);
            String urlString = urlUtils.rebuildUrl(null, null, hashParamsToRemove, null);
            urlUtils.windowRedirect(urlString);
        }

        public void onSuccess(final Map<String, Object> newUrlContext) {
        	String urlString = null;
        	String themeName = (String) newUrlContext.get(URLUtils.THEME);
        	if (themeName == null || themeName.isEmpty()) {
        		themeName = (String) urlContext.get(URLUtils.THEME);
        	}
        	Map<String, String> paramsToAdd = new HashMap<String, String>();
        	paramsToAdd.put(URLUtils.THEME, themeName);
        	
    		List<String> hashParamsToRemove = new ArrayList<String>();
            hashParamsToRemove.add(URLUtils.VIEW_MODE_PARAM);
            hashParamsToRemove.add(URLUtils.TODOLIST_PARAM);
            hashParamsToRemove.add(URLUtils.FORM_ID);
            hashParamsToRemove.add(URLUtils.TASK_ID_PARAM);
            hashParamsToRemove.add(URLUtils.PROCESS_ID_PARAM);
    		urlString = urlUtils.rebuildUrl(null, paramsToAdd, hashParamsToRemove, null);
    		if (urlString.indexOf("#") < 0) {
    			urlString += "#";
    		} else {
    			urlString += "&";
    		}
            final String hash = urlUtils.getFormRedirectionHash(newUrlContext);
            urlString = urlString + hash;
            urlUtils.windowRedirect(urlString);
        }

    }

    /**
     * Handler allowing to know if whether a user is already logged in or not
     */
    protected final class UserLoginHandler implements AsyncCallback<User> {

        public void onSuccess(final User aUser) {
              handleLogin(aUser);
        }

        public void onFailure(Throwable aCaught) {
            urlUtils.showLoginView();
        }
    }

    /**
     * Performs the post-login operations
     * 
     * @param aUserProfile
     */
    private void handleLogin(final UserProfile aUserProfile) {
        if (aUserProfile != null) {
            userProfile = aUserProfile;
            if (myIdentityKey != null) {
                List<String> hashParamsToRemove = new ArrayList<String>();
                hashParamsToRemove.add(URLUtils.USER_CREDENTIALS_PARAM);
                String newHash = urlUtils.rebuildHash(hashParamsToRemove, null);
                if (newHash == null || newHash.length() == 0) {
                    // for IE that does not support an empty history token
                    History.newItem(URLUtils.DEFAULT_HISTORY_TOKEN);
                } else {
                    History.newItem(newHash);
                }
                urlContext.remove(URLUtils.USER_CREDENTIALS_PARAM);
            }
            final String theUIMode = Window.Location.getParameter(UIMODE_PARAM);
            final UserRightsManager theUserRigthsManager = UserRightsManager.getInstance();
            theUserRigthsManager.updateUserRights(userProfile);
            if ((theUIMode == null || !"admin".equals(theUIMode)) || (theUserRigthsManager.hasSomeAdminRights())) {
                DOMUtils.getInstance().cleanBody(DOMUtils.STATIC_CONTENT_ELEMENT_ID);
                Environment.getViewController(theUIMode, new AsyncHandler<AbstractViewController>() {

                    public void handleSuccess(AbstractViewController aController) {
                        if (aController != null) {                           
                            aController.showView(userProfile);
                        }
                    }

                    public void handleFailure(Throwable aT) {
                        GWT.log("Unable to load view controller", aT);
                        Window.alert("Unable to load view controller" + aT.getMessage());
                    }
                });
            } else {
                final ConsoleMessages messages = (ConsoleMessages) GWT.create(ConsoleMessages.class);
                Window.alert(messages.accessDeniedToAdminView());
                urlUtils.showLoginView();
            }
        } else {
            urlUtils.showLoginView();
        }
    }

    /**
     * Performs the post-login operations
     * 
     * @param aUser
     */
    private void handleLogin(final User aUser) {
        if (aUser != null) {
            if (myIdentityKey != null) {
                final List<String> hashParamsToRemove = new ArrayList<String>();
                hashParamsToRemove.add(URLUtils.USER_CREDENTIALS_PARAM);
                final String newHash = urlUtils.rebuildHash(hashParamsToRemove, null);
                if (newHash == null || newHash.length() == 0) {
                    // for IE that does not support an empty history token
                    History.newItem(URLUtils.VIEW_MODE_PARAM + "=" + URLUtils.FULL_FORM_APPLICATION_MODE);
                } else {
                    History.newItem(newHash);
                }
                urlContext.remove(URLUtils.USER_CREDENTIALS_PARAM);
            }
            History.addValueChangeHandler(new ValueChangeHandler<String>() {
                public void onValueChange(final ValueChangeEvent<String> event) {
                    parseHashParameters();
                    createApplicationView(aUser);
                }
            });
            boolean isTodolist = Boolean.valueOf((String) urlContext.get(URLUtils.TODOLIST_PARAM));
            if (isTodolist) {
             	final String themeName = Window.Location.getParameter(URLUtils.THEME);
            	if(themeName != null){
            		urlContext.put(URLUtils.THEME, themeName);
            	}
                final GetAnyTodolistFormHandler getAnyTodolistFormHandler = new GetAnyTodolistFormHandler();
                RpcFormsServices.getFormsService().getAnyTodoListForm(urlContext, getAnyTodolistFormHandler);
                
            } else {
                createApplicationView(aUser);
            }
        } else {
            urlUtils.showLoginView();
        }
    }

    /**
     * Create the process application view
     * 
     * @param aUser
     */
    protected void createApplicationView(final User aUser) {
        DOMUtils.getInstance().cleanBody(CONSOLE_STATIC_CONTENT_ELEMENT_ID);
        if (URLUtils.FULL_FORM_APPLICATION_MODE.equals(myApplicationMode)) {
            FormViewControllerFactory.getFormApplicationViewController(formID, urlContext, aUser).createInitialView(DOMUtils.DEFAULT_FORM_ELEMENT_ID);
        } else {
            FormViewControllerFactory.getFormApplicationViewController(formID, urlContext, aUser).createFormInitialView();
        }
    }
    
}