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
package org.ow2.bonita.facade.rest.httpurlconnection;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.ow2.bonita.facade.rest.HttpRESTUtil;

/**
 * 
 * @author Elias Ricken de Medeiros
 *
 */
public class HttpURLConnectionUtil {
		
	public static HttpURLConnection getConnection (final String uri, final String parameters, 
	    final String contentType, final Map<String, String> headers) throws Exception{
		String serverAddress = HttpRESTUtil.getRESTServerAddress();
		URL url = new URL(serverAddress + uri);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		connection.setUseCaches (false);
    connection.setDoInput(true);
		connection.setDoOutput(true);
		
		connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", contentType);
		
		if (headers!= null) {
		  for (final String headerKey : headers.keySet()) {
        connection.setRequestProperty(headerKey, headers.get(headerKey));
      }
		}
		
		DataOutputStream output = new DataOutputStream(connection.getOutputStream());
		output.writeBytes(parameters);
		output.flush();
		output.close();
		
		return connection;
	}
	
	public static String getResponseContent (HttpURLConnection connection) throws IOException {
		InputStream is = connection.getInputStream();
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String line;
    StringBuffer response = new StringBuffer();
    try {
      while((line = reader.readLine()) != null) {
        response.append(line);
        response.append('\n');
      }
    } finally {
      reader.close();
      is.close();
    }
    return response.toString().trim();
	}
	
}
