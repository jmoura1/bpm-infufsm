import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.RuntimeAPI;
import org.ow2.bonita.facade.impl.StandardAPIAccessorImpl;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.facade.uuid.ActivityInstanceUUID;
import org.ow2.bonita.facade.runtime.ActivityInstance;
import org.ow2.bonita.facade.runtime.ProcessInstance;

public class QueryH2 {

    public static HashTable<String, String> getCurrentValues(ProcessInstance processInstance, ArrayList<String> varNames) {

        APIAccessor accessor = new StandardAPIAccessorImpl();
        QueryRuntimeAPI queryRuntimeAPI = accessor.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_JOURNAL_KEY);

        HashTable<String, String> result = new HashTable<String, String>();
        for (String var : varNames) {
            String currValue = (String) queryRuntimeAPI.getProcessInstanceVariable(processInstance.getProcessInstanceUUID(), var);
            result.put(var, currValue);
        }
        return result;
    }

    public static String formatValues(HashTable<String,String> values) {
    }

    public static String getQuerySolicita(ProcessInstance processInstance) {

        def vars = ["timeStamp", "login", "numeroMatricula", "name", "subtipoACG", "tipoACG", "descricao", "nomeTutor", "dataInicio", "dataFim", "cargaHoraria", "comprovante", "estado", "chAprovada", "tipoAprovado"]

        def intvars = ["cargaHoraria","chAprovada"]

	HashTable<String, String> values = getCurrentValues(processInstance, vars);

        String query = "INSERT INTO ACG (DATA_SOLIC, LOGIN, MATRICULA, NOME, SUBTIPO, TIPO, DESCRICAO, PROFESSOR, DATA_INI, DATA_FIM, CH, COMPROVANTE, ESTADO,CH_APROVADA,TIPO_APROVADO) VALUES('%s','%s','%s','%s','%s','%s','asdfsdf','utor','777','77', 6,'sdfg','Solicitada-Pendente',0,'Em andamento');";
('${timeStamp}','${login}','${numeroMatricula}','${name}','${subtipoACG}','${tipoACG}','${descricao}','_','${dataInicio}','${dataFim}',${cargaHoraria},'${GroupsUtil.createLink2(GroupsUtil.concat3(timeStamp,numeroMatricula,file.getFileName()), link)}','Solicitada-Pendente',0,'Em andamento');
        return String.format(query, values[0]);
    }

}



