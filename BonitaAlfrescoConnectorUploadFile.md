# Configuração de conector para upload de arquivo no Alfresco #

## Name the connector ##
Specify the connector parameters

  * Name: `upload`
  * Description:
  * Select event: `on finish`
  * If connector fails... `Raise exception`
  * Named Error:

## Alfresco Configuration ##
Alfresco host/port and user authorization

  * Host: `bpm`
  * Port: `9090`
  * User name: `admin`
  * Password: omitido por motivo de segurança

## Funcion parameters ##

  * File to upload (File path, Attachment Instance or Document): `${file}`
  * File name: `${GroupsUtil.concat(numeroMatricula,file.getFileName())}`
  * Description:
  * Mime type:
```
${import javax.activation.MimetypesFileTypeMap;

new MimetypesFileTypeMap().getContentType(file.getFileName());}
```
  * Destination folder: `/Guest%20Home`

## Upload a file to a folder (Alfresco community v.3.4) ##
Map outputs of this connector to process variables.

Connector output:
```
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
Document<Element> doc = (Document<Element>)responseDocument;
Entry entry = (Entry)doc.getRoot();
return entry.getId().toString();
```
goes to:
Destination variable: link

Connector output: `responseType`
goes to:

Connector output: `statusCode`
goest to:

Connector output: `statusText`
goes to:

Connector output: `stackTrace`
goes to: