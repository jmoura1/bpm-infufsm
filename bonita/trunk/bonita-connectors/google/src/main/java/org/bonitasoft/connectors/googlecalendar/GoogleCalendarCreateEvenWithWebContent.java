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
package org.bonitasoft.connectors.googlecalendar;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.bonitasoft.connectors.googlecalendar.common.GoogleCalendarClient;
import org.ow2.bonita.connector.core.ConnectorError;
import org.ow2.bonita.connector.core.ProcessConnector;

import com.google.gdata.util.AuthenticationException;

/**
 * 
 * @author Jordi Anguela
 *
 */
public class GoogleCalendarCreateEvenWithWebContent extends ProcessConnector {

	private String userEmail;
	private String password;
	private String calendarUrl;

	private String title;
	private String content;
	private String startTime;
	private String endTime;

	private String icon;
	private String iconTitle;
	private String contentType;
	private String url;
	private String width;
	private String height;
	
	GoogleCalendarClient gcc;

	@Override
	protected List<ConnectorError> validateValues() {
		List<ConnectorError> errors = new ArrayList<ConnectorError>();
		
		try {
			gcc = new GoogleCalendarClient(userEmail, password, calendarUrl); 
		} 
		catch (AuthenticationException e) {
			errors.add(new ConnectorError("userEmail", new IllegalArgumentException("User credentials not valid!")));
			errors.add(new ConnectorError("password", new IllegalArgumentException("User credentials not valid!")));
		} 
		catch (MalformedURLException e) {
			errors.add(new ConnectorError("calendarURL", new IllegalArgumentException("Malformed URL!")));
		}
		
		return errors;
	}
	
	@Override
	protected void executeConnector() throws Exception {
		gcc.createEvenWithWebContent(title, content, startTime, endTime, icon, iconTitle, contentType, url, width, height, null);
	}


	/**
	 * Setter for field 'icon'
	 * DO NOT RENAME THIS SETTER, unless you also change the related field in XML descriptor file
	 */
	public void setIcon(String icon) {
		this.icon = icon;
	}

	/**
	 * Setter for field 'width'
	 * DO NOT RENAME THIS SETTER, unless you also change the related field in XML descriptor file
	 */
	public void setWidth(String width) {
		this.width = width;
	}

	/**
	 * Setter for field 'userEmail'
	 * DO NOT RENAME THIS SETTER, unless you also change the related field in XML descriptor file
	 */
	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	/**
	 * Setter for field 'iconTitle'
	 * DO NOT RENAME THIS SETTER, unless you also change the related field in XML descriptor file
	 */
	public void setIconTitle(String iconTitle) {
		this.iconTitle = iconTitle;
	}

	/**
	 * Setter for field 'contentType'
	 * DO NOT RENAME THIS SETTER, unless you also change the related field in XML descriptor file
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * Setter for field 'endTime'
	 * DO NOT RENAME THIS SETTER, unless you also change the related field in XML descriptor file
	 */
	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	/**
	 * Setter for field 'password'
	 * DO NOT RENAME THIS SETTER, unless you also change the related field in XML descriptor file
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Setter for field 'calendarUrl'
	 * DO NOT RENAME THIS SETTER, unless you also change the related field in XML descriptor file
	 */
	public void setCalendarUrl(String calendarUrl) {
		this.calendarUrl = calendarUrl;
	}

	/**
	 * Setter for field 'url'
	 * DO NOT RENAME THIS SETTER, unless you also change the related field in XML descriptor file
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Setter for field 'content'
	 * DO NOT RENAME THIS SETTER, unless you also change the related field in XML descriptor file
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * Setter for field 'startTime'
	 * DO NOT RENAME THIS SETTER, unless you also change the related field in XML descriptor file
	 */
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	/**
	 * Setter for field 'title'
	 * DO NOT RENAME THIS SETTER, unless you also change the related field in XML descriptor file
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Setter for field 'height'
	 * DO NOT RENAME THIS SETTER, unless you also change the related field in XML descriptor file
	 */
	public void setHeight(String height) {
		this.height = height;
	}

}
