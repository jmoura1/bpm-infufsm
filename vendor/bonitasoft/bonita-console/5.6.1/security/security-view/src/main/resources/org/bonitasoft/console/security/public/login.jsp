<%-- Copyright (C) 2009 BonitaSoft S.A.
 BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 2.0 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. --%>
<%@page language="java"%>
<%@page contentType="text/html; charset=UTF-8"%>
<%@page import="org.bonitasoft.console.security.server.CredentialsEncryptionServlet"%>
<%@page import="java.util.ResourceBundle"%>
<%@page import="java.util.Locale"%>
<%@page import="java.net.URLEncoder"%>
<%
	String userName = (String)session.getAttribute(CredentialsEncryptionServlet.USERNAME_SESSION_PARAM);
	if (userName == null) {
		userName = "";
	}
	String redirectUrl = request.getParameter("redirectUrl");
	String actionUrl = "../security/credentialsencryption";
	if (redirectUrl != null) {
	    actionUrl += "?redirectUrl=" + URLEncoder.encode(redirectUrl, "UTF-8");
	}
	
	Locale userLocale;
	
	String localeCookieName = "BOS_Locale";
	Cookie cookies [] = request.getCookies();
	Cookie cookie = null;
	if (cookies != null) {
		for (int i = 0; i < cookies.length; i++) {
			if (cookies[i].getName().equals (localeCookieName)) {
				cookie = cookies[i];
				break;
			}
		}
	}
	if (cookie != null) {
		if ("default".equals(cookie.getValue())) {
			userLocale = new Locale("en");
		} else {
			userLocale = new Locale(cookie.getValue());
		}
	} else {
		userLocale = request.getLocale();
	}

	ResourceBundle messages = ResourceBundle.getBundle("locale.i18n.LoginLabels", userLocale);
    if (userLocale.getLanguage() != null && messages.getLocale() != null
            && !userLocale.getLanguage().equals(messages.getLocale().getLanguage())) {
        messages = ResourceBundle.getBundle("locale.i18n.LoginLabels", Locale.ENGLISH);
    }

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
	<meta http-equiv="content-type" content="text/html; charset=UTF-8" />
	
	<title>Bonita login</title>
	
	<!--             -->
	<!-- The favicon -->
	<!--        	 -->
	<link rel="icon" type="image/png" href="pictures/favicon2.ico" />
	<link rel="shortcut icon" href="pictures/favicon2.ico" type="image/x-icon" /> 
	
	<link type="text/css" rel="stylesheet" href="css/logon.css" />
	
	<!--                                           -->
	<!-- This script loads your compiled module.   -->
	<!-- If you add any GWT meta tags, they must   -->
	<!-- be added before this line.                -->
	<!--                                           -->
	<script type="text/javascript" language="javascript" src="security.nocache.js"></script>
	
	<script type="text/javascript" language="javascript">
		function login() {
			document.loginForm.submit();     
			return false;              
		}
		
		function submitEnter(field, event) {
			var keyEvent;
			if (window.event)
			{ //for IE
				keyEvent = window.event.keyCode;
			} else  if (event) {
				keyEvent = event.which;
			}
			if (keyEvent == 13) {
				field.form.submit();
				return false;
			} else {
				return true;
			}
		}
				
	</script>
</head>

<body>
	<!-- Logon view -->
	<div id="logon" class="login-background">

		<div id="welcome-container">
			<div class="commercial-link"><a href="http://www.bonitasoft.com" target="_blank"><%=messages.getString("login.header")%></a></div>
			<div id="login-container">
				<div class="login-column-container">
					<div class="login-logo-container">
						<img src="pictures/bonitaOpenSolution.png" alt="Bonita Open Solution"/>
					</div>
					<form class="login-form-container" name="loginForm" action="<%=actionUrl%>" method="post">
						<div class="login-form-label">
							<label for="login"><%=messages.getString("login.username")%></label>
						</div>
						<div id="login_field_container">
							<input id="username" name="username" type="text" tabindex="1" maxlength="50" value="<%=userName%>" onkeypress="return submitEnter(this, event);"/>
						</div>
						<div class="login-form-label">
							<label for="password"><%=messages.getString("login.password")%></label>
						</div>
						<div id="password_field_container">
							<input name="password" type="password" tabindex="2" maxlength="50" value="" onkeypress="return submitEnter(this, event);"/>
						</div>
						<div id="message_container">
						<%
							if(userName!=null && userName.length() > 0) {
						%>
							<div><%=messages.getString("login.error.message")%></div>
						<%
							}
						%>
						</div>
						
						<input name="locale" type="hidden" value="<%=userLocale.getLanguage().toString()%>" />
						
						<div class="login-form-bottom">
							<!-- div class="login-form-rememberme">
								<input type="checkbox" name="rememberme_field"/><label for="rememberme_field">Remember me</label>
							</div>
							<div class="login-form-trouble">
								<a href="#">Trouble loggin in ?</a>
							</div -->
							<div id="login_button_container">
								<div class="login_button_left">
									<div class="login_button_right">
										<div class="login_button_middle" onclick="return login();"><%=messages.getString("login.button.submit")%></div>
									</div>
								</div>
							</div>
						</div>
					</form>
					<!-- div class="login-commercial-container">
						<div class="login-commercial-title">Not using the latest version yet ?</div>
						<div class="login-commercial-content"><br/></div>
						<div class="login-commercial-buttons">
							<div id="commercial_button_learn_left">
								<div id="commercial_button_learn_right">
									<div id="commercial_button_learn_middle">
										<label>Learn More</label>
									</div>
								</div>
							</div>
							<div id="commercial_button_learn_container" onclick="window.location ='http://www.bonitasoft.com/products/bonita-v4.php'"></div>
							<div id="commercial_button_get_left">
								<div id="commercial_button_get_right">
									<div id="commercial_button_get_middle">
										<label>Get it Now !</label>
									</div>
								</div>
							</div>
							<div id="commercial_button_get_container" onclick="window.location ='http://www.bonitasoft.com/products/downloads.php'"></div>
						</div>
					</div -->
				</div>
				<div class="login-column-container">
					<div id="login-news-container">
						<div id="login-news-content">
							<p><br/><%=messages.getString("login.communication")%></p>
						</div>
					</div>
				</div>
			</div>
			<div class="copyright-label"><span><%=messages.getString("login.copyright")%></span></div>
		</div>
	</div>
	<script type='text/javascript' language="javascript">
		document.getElementById("username").focus();
	</script>
</body>
</html>
