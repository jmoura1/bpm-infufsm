package org.bonitasoft.filters;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ow2.bonita.connector.core.Filter;
import org.ow2.bonita.util.AccessorUtil;

public class GroovyFilter extends Filter {

	// DO NOT REMOVE NOR RENAME THIS FIELD
	private String script;

	@Override
	protected Set<String> getCandidates(Set<String> arg0) throws Exception {

		Object result = new Object(); 
		Map<String,Object> context = new HashMap<String, Object>();
		context.put("candidates", arg0);
		if (getActivitytInstanceUUID() != null) {
			result = AccessorUtil.getRuntimeAPI().evaluateGroovyExpression("${"+script+"}",getActivitytInstanceUUID(), context,true,false);
		} else {
		    result = AccessorUtil.getRuntimeAPI().evaluateGroovyExpression("${"+script+"}",getProcessInstanceUUID(), context,true);
		}
	
		try{
			
			if(result instanceof Set){
				return (Set<String>) result;
			}
			
			Set<String> resultSet = new HashSet<String>();

			if (result instanceof String) {
			    resultSet.add((String) result);
			    return resultSet;
			}
			
			if (result instanceof Collection) {
				resultSet.addAll((Collection)result);
			    return resultSet;
			}
			
		}catch (Exception e) {
			throw e ;
		}
		return Collections.EMPTY_SET;
	}

	/**
	 * Setter for input argument 'script'
	 * DO NOT REMOVE NOR RENAME THIS SETTER, unless you also change the related entry in the XML descriptor file
	 */
	public void setScript(String script) {
		this.script = script;
	}

}
