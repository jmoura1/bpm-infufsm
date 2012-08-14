/*METADATASql newInstance(String url, String user, String password, Driver driver)=Creates a new Sql instance given a JDBC connection URL.   Parameters:   url a database url of the form jdbc:subprotocol:subname   Returns:   a new Sql instance with a connection
*/
package providedscripts ;

import java.sql.Driver;
import groovy.sql.Sql;

public class BonitaSql {
public static Sql newInstance(String url,String user,String password,Driver driver){

Properties p= new Properties()
p.setProperty("user",user)
p.setProperty("password",password)

return new Sql(driver.connect(url, p))

}
}

