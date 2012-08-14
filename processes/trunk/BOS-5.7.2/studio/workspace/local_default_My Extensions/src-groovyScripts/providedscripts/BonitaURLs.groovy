/*METADATA
String getStepURL(String aHost, String aPort, ProcessDefinition aProcessDefinition, ActivityInstance anActivityInstance)=Return the forms URL of the given activity instance. Parameters must be not null. This method only work when a webapp is deployed for a process.
String getProcessURL(String aHost, String aPort, ProcessDefinition aProcessDefinition)=Return the form instanciation URL of the given process
String getUserXPURL(String aHost, String aPort)=Return the UserXP URL
String getUserXPBasedStepURL(String httpOrHttps, String aHost, String aPort, ActivityInstance anActivityInstance, String userXPWebArchiveName, String hostPageName)=Return the User XP based URL of a task. This is the method to use when deploying in a "All-In-The-BAR" mode.
*/
package providedscripts ;

import org.ow2.bonita.facade.QueryDefinitionAPI;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.runtime.ActivityInstance;

public class BonitaURLs {

public static String getTaskURL(QueryDefinitionAPI aQueryDefinitionAPI, ProcessDefinition aProcessDefinition, ActivityInstance anActivityInstance){
  String METADATA = "dedicated_application_URL";
  System.err.println("Looking for: (" + aProcessDefinition.getUUID().getValue() + ","    + METADATA + ")");
        String theMetadataValue = aQueryDefinitionAPI.getProcessMetaData(aProcessDefinition.getUUID(), METADATA);
        System.err.println("Found: (" + aProcessDefinition.getUUID().getValue() + ","    + theMetadataValue + ")");
        if (theMetadataValue == null) {
       
            theMetadataValue = "http://" + getHostIP() + "/bonita-app/application/homepage";
        }
        String theSeparator = "?";
        if(theMetadataValue != null && theMetadataValue.contains("?")){
            theSeparator = "&";
        }
        System.err.println("URL of the task: " + theMetadataValue + theSeparator + "task=" + anActivityInstance.getUUID().getValue());
        return theMetadataValue + theSeparator + "task=" + anActivityInstance.getUUID().getValue();
}


public static String getHostIP() {

try {
    InetAddress addr = InetAddress.getLocalHost();
    byte[] ipAddr = addr.getAddress();

    String ipAddrStr = "";
    for (int i=0; i<ipAddr.length; i++) {
        if (i > 0) {
            ipAddrStr += ".";
        }
        ipAddrStr += ipAddr[i]&0xFF;
    }
    return ipAddrStr;
} catch (UnknownHostException e) {
    return "localhost";
}
}

/**
* /!\ You should not have renamed the war of your application. It means that processUUID and war name is the same.
*
* */
@Deprecated
public static String getStepURL(String aHost, String aPort, ProcessDefinition aProcessDefinition, ActivityInstance anActivityInstance){
	return getOpenStepUrlInBARDeploymentMode(aPort, aHost, aPort, anActivityInstance, aPort, aHost, null);
}

/**
 * 
 * @processApplicationWarName if war of the applicatio nhas been renamed. Otherwise, keep it null.
 * */
public static String getOpenStepURLInWarDeploymentMode(String aHost, String aPort, ProcessDefinition aProcessDefinition, ActivityInstance anActivityInstance, String processApplicationWarName){
	return getStepURLInWarDeploymentMode(aHost, aPort, aProcessDefinition, anActivityInstance, processApplicationWarName, "entry");
}

/**
*
* @processApplicationWarName Precise it if war of the application has been renamed. Otherwise, keep it null.
* */
public static String getFinishedStepURLInWarDeploymentMode(String aHost, String aPort, ProcessDefinition aProcessDefinition, ActivityInstance anActivityInstance, String processApplicationWarName){
	return getStepURLInWarDeploymentMode(aHost, aPort, aProcessDefinition, anActivityInstance, processApplicationWarName, "view");
}

private static getStepURLInWarDeploymentMode(String aHost, String aPort, ProcessDefinition aProcessDefinition, ActivityInstance anActivityInstance, String processApplicationWarName, String formType){
	String warName = processApplicationWarName != null ? processApplicationWarName : aProcessDefinition.getUUID().getValue();
	String theTaskURL = "http://" + aHost + ":"+ aPort + "/" + warName + "/application/homepage";
	
	String theSeparator = "?";
	
	theTaskURL = theTaskURL + theSeparator +
	"theme="+aProcessDefinition.getUUID().getValue()+
	"#mode=app"+
	"&form="+anActivityInstance.getActivityDefinitionUUID()+"\$"+formType+
	"&task=" + anActivityInstance.getUUID().getValue();

	return theTaskURL;
}

	/**
	 * If you want to have the form displayed in standalone please use getOpenStepUrlInBARDeploymentMode instead
	 * if you want to have the form displayed inside the User XP please use getOpenCaseUrlOpenedInsideUserXP
	 * */
@Deprecated
public static String getUserXPBasedStepURL(String httpOrHttps, String aHost, String aPort, ActivityInstance anActivityInstance, String userXPWebArchiveName, String hostPageName) {
	return getOpenStepUrlInBARDeploymentMode(httpOrHttps, aHost, aPort, anActivityInstance, userXPWebArchiveName, hostPageName);
}

private static appendHostPortURL(StringBuilder builder, String httpOrHttps, String aHost, String aPort){
	if (httpOrHttps != null) {
		builder.append(httpOrHttps);
	} else {
		builder.append("http");
	}
	builder.append("://");
	if (aHost != null) {
		builder.append(aHost);
	} else {
		builder.append("localhost");
	}
	if (aPort != null) {
		builder.append(":");
		builder.append(aPort);
	}
	builder.append("/");
}

	/**
	 * userXPWebArchiveName if userXP Archive Name has been changed. By default it will used bonita. (null value)
	 * 
	 * */
public static String getOpenStepUrlInBARDeploymentMode(String httpOrHttps, String aHost, String aPort, ActivityInstance anActivityInstance, String userXPWebArchiveName, String hostPageName){
	return getStepUrlInBARDeploymentMode(httpOrHttps, aHost, aPort, anActivityInstance, userXPWebArchiveName, hostPageName, "entry");
}

	/**
	 * userXPWebArchiveName if userXP Archive Name has been changed. By default it will used bonita. (null value)
	 * 
	 * */
public static String getFinishStepUrlInBARDeploymentMode(String httpOrHttps, String aHost, String aPort, ActivityInstance anActivityInstance, String userXPWebArchiveName, String hostPageName){
	return getStepUrlInBARDeploymentMode(httpOrHttps, aHost, aPort, anActivityInstance, userXPWebArchiveName, hostPageName, "view");
}
	
private static String getStepUrlInBARDeploymentMode(String httpOrHttps, String aHost, String aPort, ActivityInstance anActivityInstance, String userXPWebArchiveName, String hostPageName, String formType){
	StringBuilder builder = new StringBuilder();
	appendHostPortURL(builder, httpOrHttps, aHost, aPort);
	if (userXPWebArchiveName != null) {
		builder.append(userXPWebArchiveName);
	} else {
		builder.append("bonita");
	}
	builder.append("/console/");
	if (hostPageName != null) {
		builder.append(hostPageName);
	} else {
		builder.append("homepage");
	}
	builder.append("?theme="+anActivityInstance.getProcessDefinitionUUID().getValue());
	builder.append("#mode=app&form="+anActivityInstance.getActivityDefinitionUUID()+"\$"+formType);
	builder.append("&task="+anActivityInstance.getUUID());
	return builder.toString();
}
	/**
	 * 
	 * @param httpOrHttps
	 * @param aHost
	 * @param aPort
	 * @param anActivityInstance
	 * @param userXPWebArchiveName
	 * @param hostPageName
	 * @param String
	 * @param theme
	 * @param activtyType jou if open, his if finished
	 * @return
	 */
	private static String getCaseUrlInsideUserXP(String httpOrHttps, String aHost, String aPort, ActivityInstance anActivityInstance, String userXPWebArchiveName, String hostPageName, String theme, String activtyType){
		StringBuilder builder = new StringBuilder();
		appendHostPortURL(builder, httpOrHttps, aHost, aPort);
		if (userXPWebArchiveName != null) {
			builder.append(userXPWebArchiveName);
		} else {
			builder.append("bonita");
		}
		builder.append("/console/");
		if (hostPageName != null) {
			builder.append(hostPageName);
		} else {
			builder.append("homepage");
		}
		builder.append("?");
		if(theme != null){
			builder.append("theme="+theme+"&");
		}
		builder.append("ui=user");
		builder.append("#CaseEditor/"+activtyType+":"+anActivityInstance.getProcessInstanceUUID());
		return builder.toString();
	}
	
	
	
	public static getOpenCaseUrlInsideUserXP(String httpOrHttps, String aHost, String aPort, ActivityInstance anActivityInstance, String userXPWebArchiveName, String hostPageName, String theme){
		return getCaseUrlInsideUserXP(httpOrHttps, aHost, aPort, anActivityInstance, userXPWebArchiveName, hostPageName, theme, "jou");
	}
	
	public static getFinishCaseUrlInsideUserXP(String httpOrHttps, String aHost, String aPort, ActivityInstance anActivityInstance, String userXPWebArchiveName, String hostPageName, String theme){
		return getCaseUrlInsideUserXP(httpOrHttps, aHost, aPort, anActivityInstance, userXPWebArchiveName, hostPageName, theme, "his");
	}
	
public static String getUserXPURL(String aHost, String aPort){
 return "http://" + aHost + ":"+ aPort + "/bonita/console/BonitaConsole.html";  
}

public static String getProcessURL(String aHost, String aPort, ProcessDefinition aProcessDefinition){
  return "http://" + aHost + ":"+ aPort + "/" + aProcessDefinition.getUUID().getValue() + "/application/BonitaApplication.html?process=" +aProcessDefinition.getUUID().getValue();    
}

}