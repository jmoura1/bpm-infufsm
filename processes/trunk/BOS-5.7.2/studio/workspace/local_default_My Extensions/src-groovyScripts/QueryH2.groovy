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

    private static ArrayList<String> values;
    private static String strformat;

    // Retorna string de formato para as variaves passadas como argumento
    public static String getValuesFormat(ArrayList<String> allvars, 
                                        ArrayList<String> intvars) {
        // Gera um formato com um '%s,'  para cada variavel
        String format = String.format("%0" + allvars.size() + "d", 0).replace("0","'%s',");

        // Ajusta o formato para as variaveis inteiras
        // Exemplo para 4 variaveis, com variaveis 1 e 3 inteiras:
        // tmpformat inicial = "'%s','%s','%s','%s'"
        // tmpformat final = "'%s',%s,'%s',%s"
        ArrayList<Integer> indices = searchSubList(allvars, intvars);
        for (Integer i : indices) {
            // Padrao: GrupoInicial = sequencia que nao comeca por virgula, 
            // repetida varias vezes ate encontrar uma virgula. 
            // Grupo1 = GrupoInicial repetido i vezes. Grupo1 é seguido por
            // sequencia que nao comeca por virgula, repetida varias vezes 
            // ate encontrar virgula
            // Replace: copia Grupo1 e substitui o restante por %s,
          String patternstr = "(([^,]*[,]){" + i + "})[^,]*[,]";
          Pattern pattern = Pattern.compile(patternstr);
          Matcher matcher = pattern.matcher(format);
          format = matcher.replaceFirst("\$1%s,"); // sql integer field
        }
        format = "(" + format.replaceAll(",\$", ")");
        return format;
      }

    // Obtem valores das variaveis numa dada instancia de processo
    public static ArrayList<String> getCurrentValues(ProcessInstance processInstance,
                                                     ArrayList<String> varNames) {

        APIAccessor accessor = new StandardAPIAccessorImpl();
        QueryRuntimeAPI queryRuntimeAPI = accessor.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_JOURNAL_KEY);

        ArrayList<String> result = new ArrayList<String>();
        for (String var : varNames) {
            String currValue = (String) queryRuntimeAPI.getProcessInstanceVariable(processInstance.getProcessInstanceUUID(), var);
            result.add(currValue);
        }
        return result;
    }

    // Retorna os indices dos elementos da lista sub dentro da lista list
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


    // Seta variaveis estaticas
    public static setValuesAndFormat(ProcessInstance processInstance, ArrayList<String> allvars, ArrayList<String> intvars) {
        // Obtem os valores das variaveis
        values = getCurrentValues(processInstance, allvars);
        // Obtem um formato tipo ('%s,%s,...) 
        // a ser preenchido com valores que vao para o banco
        strformat = getValuesFormat(allvars, intvars);

    }


    public static String getQuerySolicita(ProcessInstance processInstance) {
        def allvars = ["timeStamp", "login", "numeroMatricula", "name", "subtipoACG",
                     "tipoACG", "descricao", "nomeTutor", "dataInicio", "dataFim",
                     "cargaHoraria", "acgComprovante", "acgEstado", "acgChAprovada",
                     "acgTipoAprovado"];
        def intvars = ["cargaHoraria", "acgChAprovada"];
        setValuesAndFormat(processInstance, allvars, intvars);
        String query = "INSERT INTO ACG (DATA_SOLIC, LOGIN, MATRICULA, NOME, SUBTIPO, TIPO, DESCRICAO, PROFESSOR, DATA_INI, DATA_FIM, CH, COMPROVANTE, ESTADO,CH_APROVADA,TIPO_APROVADO) VALUES " + String.format(strformat, (java.lang.Object[]) values.toArray()) + ";";
        return query;
    }


    public static String getQueryTutorAvalia(ProcessInstance processInstance) {
        def allvars = ["timeStamp", "login", "nomeTutor", "parecerTutor", "respostaTutor", "data_aval_tutor"];
        def intvars = [];
        setValuesAndFormat(processInstance, allvars, intvars);
        String query = "INSERT INTO AVALIACAO_TUTOR (DATA_SOLIC, LOGIN_ALUNO, LOGIN_TUTOR, PARECER, DECISAO, DATA_AVAL) VALUES " + 
            String.format(strformat, (java.lang.Object[]) values.toArray()) + ";";
        return query;
    }

    public static String getQueryRelatorAvalia(ProcessInstance processInstance) {
        def allvars = ["timeStamp", "login", "idRelator", "parecerRelator", "respostaRelator", "data_aval_tutor"];
        def intvars = [];
        setValuesAndFormat(processInstance, allvars, intvars);
        String query = "INSERT INTO AVALIACAO_RELATOR (DATA_SOLIC, LOGIN_ALUNO, LOGIN_RELATOR, PARECER, DECISAO, DATA_AVAL) VALUES " + 
            String.format(strformat, (java.lang.Object[]) values.toArray()) + ";";
        return query;
    }


    public static String getQueryUpdateEstado(ProcessInstance processInstance) {
        def allvars = ["acgEstado", "login", "timeStamp"];
        values = getCurrentValues(processInstance, allvars);
        strformat = "UPDATE ACG SET ESTADO='%s' WHERE LOGIN='%s' AND DATA_SOLIC='%s';";
        String query = String.format(strformat, (java.lang.Object[]) values.toArray());
        return query;
    }

/*
INSERT INTO AVALIACAO_RELATOR VALUES('${timeStamp}','${login}','${idRelator}','${parecerRelator}','${respostaRelator}','${data_aval_relator}');

Task: Avisa ao aluno (indeferido)
UPDATE ACG SET ESTADO='Terminada-Recusada pelo tutor' WHERE LOGIN='${login}' AND DATA_SOLIC='${timeStamp}'; 

*/

}
