/*METADATA
String getUserEMail(String userName)=Returns the principal (professional) email address for a user
String getUser(String userName)=Returns the User object for the given user name
*/
package providedscripts ;

import org.ow2.bonita.facade.identity.User
import org.ow2.bonita.facade.identity.ContactInfo

public class BonitaUsers {

	public static String getUserEMail(String userName) {
		User user = getUser(userName);
		if (user != null) {
			ContactInfo professionalContactInfo = user.getProfessionalContactInfo();
			if (professionalContactInfo != null) {
				return professionalContactInfo.getEmail();
			}
		};
		return null
	}
	
	public static User getUser(String userName) {
		return new org.ow2.bonita.facade.impl.StandardAPIAccessorImpl().getIdentityAPI().findUserByUserName(userName);
	}


}