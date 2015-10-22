# Groovy LDAP #

  * [Download Groovy LDAP](http://directory.apache.org/api/1-download-groovy-ldap.html)
  * [Groovy LDAP](http://directory.apache.org/api/groovy-ldap.html)

# Code #

Get username from LDAP (based on http://www.bonitasoft.org/forum/viewtopic.php?pid=14798#p14798)

```
${import org.apache.directory.groovyldap.LDAP
import org.apache.directory.groovyldap.Search
import org.apache.directory.groovyldap.SearchScope

ldap = LDAP.newInstance('ldap://ldap.inf.ufsm.br:389/')
params = new Search()

params.filter='(uid='+GroupsUtil.nameOfLoggedUser()+')'
params.base = 'ou=People,dc=inf,dc=ufsm,dc=br'
params.scope=SearchScope.SUB
def resultado = ''
try {
	results = ldap.search(params)
        resultado = results.cn[0]
} catch (Exception e) {
	resultado = ''
}
return resultado}
```


# References #

  * [Groovy LDAP User Guide - LDAP search operation](http://directory.apache.org/api/2-groovy-ldap-user-guide.html#2.GroovyLDAPUserGuide-LDAPsearchoperation)
  * [Email connector : get email from LDAP Actor](http://www.bonitasoft.org/forum/viewtopic.php?pid=14748#p14748)