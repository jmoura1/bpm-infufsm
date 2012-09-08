package org.bonitasoft.connectors.sap;


import java.util.ArrayList;
import java.util.List;

public class BAPI_MATERIAL_GETLIST {

  private static final boolean releaseClient = false;
  private static boolean commitOnSuccess = true;
  private static final boolean rollbackOnFailure = true;
  private static final boolean printHtml = false;

  public static void main(String args[]) throws Exception {
    final String existingConnectionName = SAPUtil.createClient();
    SAPUtil.commit(existingConnectionName, releaseClient);
    SAPUtil.rollback(existingConnectionName, releaseClient);
    callFunction(existingConnectionName);
    SAPUtil.releaseClient(existingConnectionName); 
  }


  private static void callFunction(final String existingConnectionName) throws Exception {

    final List<List<Object>> inputParameters = new ArrayList<List<Object>>();
    SAPUtil.addInputRowow(inputParameters, SAPCallFunctionConnector.TABLE_INPUT, "MATNRSELECTION", "SIGN", "I");
    SAPUtil.addInputRowow(inputParameters, SAPCallFunctionConnector.TABLE_INPUT, "MATNRSELECTION", "OPTION", "EQ");
    SAPUtil.addInputRowow(inputParameters, SAPCallFunctionConnector.TABLE_INPUT, "MATNRSELECTION", "MATNR_LOW", "P1001087");
    SAPUtil.addInputRowow(inputParameters, SAPCallFunctionConnector.TABLE_INPUT, "MATNRSELECTION", "MATNR_HIGH", "");
    SAPUtil.addInputRowow(inputParameters, SAPCallFunctionConnector.TABLE_INPUT, "MATNRSELECTION", "SIGN", "I");
    SAPUtil.addInputRowow(inputParameters, SAPCallFunctionConnector.TABLE_INPUT, "MATNRSELECTION", "OPTION", "EQ");
    SAPUtil.addInputRowow(inputParameters, SAPCallFunctionConnector.TABLE_INPUT, "MATNRSELECTION", "MATNR_LOW", "P1001089");
    SAPUtil.addInputRowow(inputParameters, SAPCallFunctionConnector.TABLE_INPUT, "MATNRSELECTION", "MATNR_HIGH", "");

    final List<List<String>> outputParameters = new ArrayList<List<String>>();
    SAPUtil.addOutputRow(outputParameters, SAPCallFunctionConnector.TABLE_OUTPUT, "RETURN", "MESSAGE");

    SAPUtil.callFunction(existingConnectionName, releaseClient, commitOnSuccess, rollbackOnFailure, printHtml, "BAPI_MATERIAL_GETLIST", inputParameters, outputParameters);
  }

  /*


			JCO.Table returnTable1 =
				function.getTableParameterList().getTable("MATERIALSHORTDESCSEL");

			JCO.Table returnTable2 =
				function.getTableParameterList().getTable("MATNRLIST");

			// Output result Material list table
			System.out.println(">>>>>>>>CCA3\n MATNRLIST" + function.getTableParameterList().getValue("MATNRLIST"));
			System.out.println(">>>>>>>>CCA4\n MATNRLIST" + function.getTableParameterList().getString("MATNRLIST"));

			System.out.println(">>>>>>>>CCA5\n + MATERIALSHORTDESCSEL" + function.getTableParameterList().getValue("MATERIALSHORTDESCSEL"));

 		System.out.println(">>>>>>>>CCA6\n + MATERIALSHORTDESCSEL" + function.getTableParameterList().getString("MATERIALSHORTDESCSEL"));

			// Output Selection table
			System.out.println(">>>>>>>>CCA7\n + MATNRSELECTION" + function.getTableParameterList().getValue("MATNRSELECTION"));
			System.out.println(">>>>>>>>CCA8\n MATNRSELECTION" + function.getTableParameterList().getString("MATNRSELECTION"));
   */

}
