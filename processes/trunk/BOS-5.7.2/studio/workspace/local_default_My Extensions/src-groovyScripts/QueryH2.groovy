/*METADATAString getValuesFormat(ArrayList<String> allvarslist, ArrayList<String> intvarslist)=No Documentation
String getQuerySolicita(ProcessInstance processInstance)=No Documentation
ArrayList<String> getCurrentValues(ProcessInstance processInstance, ArrayList<String> varNames)=No Documentation
ArrayList<Integer> searchSubList(List<String> list, List<String> sub)=No Documentation
*/import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.RuntimeAPI;
import org.ow2.bonita.facade.impl.StandardAPIAccessorImpl;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.ProcessInstance;
import java.util.regex.*;
import java.util.Collections;

public class QueryH2 {

	public static String getValuesFormat(ArrayList<String> allvarslist, ArrayList<String> intvarslist) {
		//List<String> allvarslist = Arrays.asList(allvars);
		//List<String> intvarslist = Arrays.asList(intvars);
	
		// Gera um formato com um '%s,'  para cada variavel
		String format = String.format("%0" + allvarslist.size() + "d", 0).replace("0","'%s',");
	
		// Ajusta o formato para as variaveis inteiras
		// Exemplo para 4 variaveis, com variaveis 1 e 3 inteiras:
		// tmpformat inicial = "'%s','%s','%s','%s'"
		// tmpformat final = "'%s',%s,'%s',%s"
		ArrayList<Integer> indices = searchSubList(allvarslist, intvarslist);
		for (Integer i : indices) {
		  // Padrao: GrupoIni = sequencia que nao comeca por virgula, repetida varias vezes ate
		  // encontrar uma virgula. Grupo 1 = GrupoIni repetido i vezes. Grupo 1 é seguido por
		  // sequencia que nao comeca por virgula, repetida varias vezes ate encontrar virgula
		  // Replace: copia Grupo1 e substitui o restante por %s (ou
		  String patternstr = "(([^,]*[,]){" + i + "})[^,]*[,]";
		  Pattern pattern = Pattern.compile(patternstr);
		  Matcher matcher = pattern.matcher(format);
		  format = matcher.replaceFirst("\$1%s,"); // sql integer field
		}
		format = "(" + format.replaceAll(",\$", ")");
		return format;
	  }

    public static ArrayList<String> getCurrentValues(ProcessInstance processInstance, ArrayList<String> varNames) {

		APIAccessor accessor = new StandardAPIAccessorImpl();
		QueryRuntimeAPI queryRuntimeAPI = accessor.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_JOURNAL_KEY);

		ArrayList<String> result = new ArrayList<String>();
		for (String var : varNames) {
			String currValue = (String) queryRuntimeAPI.getProcessInstanceVariable(processInstance.getProcessInstanceUUID(), var);
			result.add(currValue);
		}
		return result;
	}	

	private static ArrayList<Integer> searchSubList(List<String> list, List<String> sub) {
		ArrayList<Integer> indices = new ArrayList<Integer>();
		Collections.sort(sub);
	
		for (String s : list) {
		  if (Collections.binarySearch(sub, s) >= 0) {
			indices.add(list.indexOf(s));
		  }
		}
		return indices;
	  }	
	
	  public static String getQuerySolicita(ProcessInstance processInstance) {
	
		def allvars = ["timeStamp", "login", "numeroMatricula", "name", "subtipoACG", "tipoACG", "descricao", "nomeTutor", "dataInicio", "dataFim", "cargaHoraria", "acgComprovante", "acgEstado", "acgChAprovada", "acgTipoAprovado"];
		def intvars = ["cargaHoraria", "acgChAprovada"];
	
		// Obtem os valores das variaveis
		ArrayList<String> values = getCurrentValues(processInstance, allvars);
		// Obtem um formato tipo ('%s,%s,...) a ser preenchido com valores que vao para o banco
		String strformat = getValuesFormat(allvars, intvars);
	
		String query = "INSERT INTO ACG (DATA_SOLIC, LOGIN, MATRICULA, NOME, SUBTIPO, TIPO, DESCRICAO, PROFESSOR, DATA_INI, DATA_FIM, CH, COMPROVANTE, ESTADO,CH_APROVADA,TIPO_APROVADO) VALUES " + String.format(strformat, (java.lang.Object[]) values.toArray()) + ";";
	
		return query;
	  }

}
