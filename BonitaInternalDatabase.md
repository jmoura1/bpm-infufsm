# Introduction #

Como modificar o banco interno do Bonita.

# Details #
## **Pré-requisitos:** ##
Possuir a versão 'Bundle' do BOS (esse passo-a-passo foi testado com a versão 5.7.2 e Apache Tomcat, encontrada aqui http://www.bonitasoft.com/products/download/tomcat-6.0.33-7).

Instalar o banco de dados escolhido (recomenda-se o MySQL).

## Passo-a-Passo ##

1- Criar dois 'databases' com o nome _bonita\_history\_e_ bonita\_journal _;_

2- Criar um usuário chamado _bonita_ com o password _bpm_ e conceder todas as permissões sobre os databases criados acima;

3- Editar os arquivos _bonita-journal.properties_ e   _bonita-history.properties_ localizados em BOS-5.7.2-Tomcat-6.0.32\bonita\server\default\conf :

  1. Comentar as linhas

> _"hibernate.dialect                       org.hibernate.dialect.H2Dialect"_ e

> _"bonita.hibernate.interceptor             org.ow2.bonita.env.interceptor.H2DescNullFirstInterceptor"_

> de ambos os arquivos;

> 2. No final dos arquivos, estão comentadas as configurações de vários tipos de BD, tire o comentário da configuração do banco que você escolheu, no caso do MySQL as configurações são:

> _"hibernate.dialect                        org.hibernate.dialect.MySQL5InnoDBDialect"_

> _"bonita.hibernate.interceptor             org.ow2.bonita.env.interceptor.MySQLDescNullFirstInterceptor"_

> 3.Agora é preciso editar o arquivo context.xml localizado em BOS-5.7.2-Tomcat-6.0.32\conf, nesse arquivo encontram-se as configurações do hibernate. Basta comentar as configurações que incluem o h2 (existem duas, uma para cada database) e criar uma nova configuração (no caso do MySQL, essa configuração já se encontra, comentada, no arquivo, basta tirar o comentário), como a seguinte:


> _<Resource name="bonita/default/journal"
> > auth="Container"
> > type="javax.sql.DataSource"
> > maxActive="100"
> > minIdle="10"
> > maxWait="10000"
> > initialSize="1"
> > maxPoolSize="15"
> > minPoolSize="3"
> > maxConnectionAge="0"
> > maxIdleTime="1800"
> > maxIdleTimeExcessConnections="120"
> > idleConnectionTestPeriod="30"
> > acquireIncrement="3"
> > testConnectionOnCheckout="true"
> > removeAbandoned="true"
> > logAbandoned="true"
> > username="bonita"
> > password="bpm"_


> driverClassName="com.mysql.jdbc.Driver"
> url="jdbc:mysql://localhost:3306/bonita\_journal?dontTrackOpenResources=true"/>

**Precisa existir uma configuração para cada database.**

4. Após isso, é necessário adicionar o mysql-jdbc driver (aqui : http://dev.mysql.com/downloads/connector/j/ ) na pasta BOS-5.5.2-Tomcat-6.0.32\lib\bonita e remover o arquivo h2-1.2.139.jar.

Então o novo banco já está configurado. Após a primeira execução, deve-se comentar o comando hibernate.hbm2ddl.auto dos arquivos bonita-journal.properties e bonita-history.properties.