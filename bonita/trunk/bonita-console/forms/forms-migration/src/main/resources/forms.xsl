<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl = "http://www.w3.org/1999/XSL/Transform" version = "1.0">
  <xsl:output method = "xml" encoding = "UTF-8" standalone = "no" indent = "yes"/>
  <xsl:template match = "/">
  	<xsl:element name = "forms-definition" >
    	<xsl:attribute name = "product-version" ><xsl:value-of select = "forms-definition/@product-version"/></xsl:attribute> 
    	
    	<xsl:copy-of select = "welcome-page" />
    	<xsl:copy-of select = "external-welcome-page" />
    	<xsl:element name = "migration-product-version" >5.6</xsl:element>
    	<xsl:element name = "application" >
    		<xsl:variable name = "name" select = "forms-definition/process/@name"/>
    		<xsl:variable name = "version" select = "forms-definition/process/@version"/>
    		
    		<xsl:attribute name = "name" ><xsl:value-of select = "$name"/></xsl:attribute> 
    		<xsl:attribute name = "version" ><xsl:value-of select = "$version"/></xsl:attribute> 
    		
    		<xsl:element name = "permissions" >process#<xsl:value-of select = "$name"/>--<xsl:value-of select = "$version"/></xsl:element>
    		
            <xsl:if test = "forms-definition/process/process-label != ''">
    			<xsl:element name = "application-label" ><xsl:value-of select = "forms-definition/process/process-label"/></xsl:element>
    		</xsl:if>
    		<xsl:if test = "forms-definition/process/process-template != ''">
    			<xsl:element name = "application-layout" ><xsl:value-of select = "forms-definition/process/process-template"/></xsl:element>
    		</xsl:if>
    		<xsl:copy-of select = "forms-definition/process/mandatory-symbol" />
    		<xsl:copy-of select = "forms-definition/process/mandatory-label" />
    		<xsl:copy-of select = "forms-definition/process/mandatory-style" />
    		<xsl:copy-of select = "forms-definition/process/error-template" />
    		
    		<xsl:element name = "forms" >
	    		<xsl:for-each select = "forms-definition/process" > 
	               	
	               	<xsl:if test = "view-pageflow != ''">
	     	    		<xsl:element name = "form" >
		    				<xsl:attribute name ="first-page" ><xsl:value-of select = "view-pageflow/@first-page"/></xsl:attribute>
		    				<xsl:attribute name ="id" ><xsl:value-of select = "$name"/>--<xsl:value-of select = "$version"/>$view</xsl:attribute> 
		    				
		    				<xsl:element name = "form-type" >view</xsl:element>
			               	<xsl:element name = "permissions" >process#<xsl:value-of select = "$name"/>--<xsl:value-of select = "$version"/></xsl:element>
			               	<xsl:if test = "view-pageflow/view-pages/view-page != ''">
			               		<xsl:element name = "pages" >
			               			<xsl:for-each select = "view-pageflow/view-pages/view-page" > 
			               				<xsl:element name = "page" >
			                				<xsl:attribute name ="id" ><xsl:value-of select = "@id"/></xsl:attribute>
			                				
			                				<xsl:copy-of select = "page-label" />
			                				<xsl:copy-of select = "allow-html-in-label" />
			                				<xsl:if test = "page-template != ''">
					                			<xsl:element name = "page-layout" ><xsl:value-of select = "page-template"/></xsl:element>
					                		</xsl:if>
					                		<xsl:copy-of select = "widgets" />		                		
					                		<xsl:copy-of select = "page-validators" />
					                		<xsl:copy-of select = "actions" />
											<xsl:copy-of select = "next-page" />
				                		</xsl:element>
			                		</xsl:for-each>
			               		</xsl:element>
		               		</xsl:if>
		               		<xsl:if test = "view-pageflow/confirmation-template != ''">
			               		<xsl:element name = "confirmation-layout" ><xsl:value-of select = "view-pageflow/confirmation-template"/></xsl:element>
			               	</xsl:if>
			               	<xsl:copy-of select = "view-pageflow/confirmation-message" />
			               	<xsl:copy-of select = "view-pageflow/redirection-url" />
			               	<xsl:copy-of select = "view-pageflow/transient-data" />
			               	<xsl:copy-of select = "view-pageflow/connectors" />
		               	</xsl:element>
	               	</xsl:if>
	               	
	               	<xsl:if test = "recap-pageflow != ''">
		   				<xsl:element name = "form" >
		    				<xsl:attribute name = "first-page" ><xsl:value-of select = "recap-pageflow/@first-page"/></xsl:attribute>
		    				<xsl:attribute name = "id" ><xsl:value-of select = "$name"/>--<xsl:value-of select = "$version"/>$recap</xsl:attribute> 
		    				
		    				<xsl:element name = "form-type" >view</xsl:element> 
			               	<xsl:element name = "permissions" >process#<xsl:value-of select = "$name"/>--<xsl:value-of select = "$version"/></xsl:element>
			               	<xsl:if test = "recap-pageflow/recap-pages/recap-page != ''">
			               		<xsl:element name = "pages" >
			               			<xsl:for-each select = "recap-pageflow/recap-pages/recap-page" > 
			               				<xsl:element name = "page" >
			                				<xsl:attribute name ="id" ><xsl:value-of select = "@id"/></xsl:attribute>
			                				
			                				<xsl:copy-of select = "page-label" />
			                				<xsl:copy-of select = "allow-html-in-label" />
			                				<xsl:if test = "page-template != ''">
					                			<xsl:element name = "page-layout" ><xsl:value-of select = "page-template"/></xsl:element>
					                		</xsl:if>
					                		<xsl:copy-of select = "widgets" />		                		
					                		<xsl:copy-of select = "page-validators" />
					                		<xsl:copy-of select = "actions" />
					                		<xsl:copy-of select = "next-page" />
				                		</xsl:element>
			                		</xsl:for-each>
			               		</xsl:element>
		               		</xsl:if>
		               		<xsl:if test = "recap-pageflow/confirmation-template != ''">
			               		<xsl:element name = "confirmation-layout" ><xsl:value-of select = "recap-pageflow/confirmation-template"/></xsl:element>
			               	</xsl:if>
			               	<xsl:copy-of select = "recap-pageflow/confirmation-message" />
			               	<xsl:copy-of select = "recap-pageflow/redirection-url" />
			               	<xsl:copy-of select = "recap-pageflow/transient-data" />
			               	<xsl:copy-of select = "recap-pageflow/connectors" />
		               	</xsl:element>
	               	</xsl:if>
	               	
	               	<xsl:choose>
	               		<xsl:when test = "pageflow != ''">
	               			<xsl:element name = "form" >
		    					<xsl:attribute name = "first-page" ><xsl:value-of select = "pageflow/@first-page"/></xsl:attribute>
		    					<xsl:attribute name = "id" ><xsl:value-of select = "$name"/>--<xsl:value-of select = "$version"/>$entry</xsl:attribute> 
		    				
		    					<xsl:element name = "form-type" >entry</xsl:element> 
			               		<xsl:element name = "permissions" >process#<xsl:value-of select = "$name"/>--<xsl:value-of select = "$version"/></xsl:element>
			               		<xsl:if test = "pageflow/pages/page != ''">
			               			<xsl:element name = "pages" >
			               				<xsl:for-each select = "pageflow/pages/page" > 
			               					<xsl:element name = "page" >
			                					<xsl:attribute name ="id" ><xsl:value-of select = "@id"/></xsl:attribute>
			                				
			                					<xsl:copy-of select = "page-label" />
			                					<xsl:copy-of select = "allow-html-in-label" />
			                					<xsl:if test = "page-template != ''">
					                				<xsl:element name = "page-layout" ><xsl:value-of select = "page-template"/></xsl:element>
					                			</xsl:if>
					                			<xsl:copy-of select = "widgets" />		                		
					                			<xsl:copy-of select = "page-validators" />
					                			<xsl:copy-of select = "actions" />
					                			<xsl:copy-of select = "next-page" />
				                			</xsl:element>
			                			</xsl:for-each>
			               			</xsl:element>
		               			</xsl:if>
		               			<xsl:if test = "pageflow/confirmation-template != ''">
			               			<xsl:element name = "confirmation-layout" ><xsl:value-of select = "pageflow/confirmation-template"/></xsl:element>
			               		</xsl:if>
			               		<xsl:copy-of select = "pageflow/confirmation-message" />
			               		<xsl:copy-of select = "pageflow/redirection-url" />
			               		<xsl:copy-of select = "pageflow/transient-data" />
			               		<xsl:copy-of select = "pageflow/connectors" />
		               		</xsl:element>
	               		</xsl:when>
	               		<xsl:when test = "pageflow = ''">
	               			<xsl:element name = "form" >
		    					<xsl:attribute name ="id" ><xsl:value-of select = "$name"/>--<xsl:value-of select = "$version"/>$entry</xsl:attribute> 
		    				
		    					<xsl:element name = "form-type" >entry</xsl:element>
			               		<xsl:element name = "permissions" >process#<xsl:value-of select = "$name"/>--<xsl:value-of select = "$version"/></xsl:element>
		               		</xsl:element>
		               	</xsl:when>
	               	</xsl:choose>
	     	    </xsl:for-each>
     	    
	     	    <xsl:for-each select = "forms-definition/process/activities/activity" > 
   					<xsl:variable name = "activityname" select = "@name"/>
		   					
	               	<xsl:if test = "view-pageflow != ''">
	     	    		<xsl:element name = "form" >
		    				<xsl:attribute name ="first-page" ><xsl:value-of select = "view-pageflow/@first-page"/></xsl:attribute>
		    				<xsl:attribute name ="id" ><xsl:value-of select = "$name"/>--<xsl:value-of select = "$version"/>--<xsl:value-of select = "$activityname"/>$view</xsl:attribute> 
		    				
		    				<xsl:element name = "form-type" >view</xsl:element>
			               	<xsl:element name = "permissions" >activity#<xsl:value-of select = "$name"/>--<xsl:value-of select = "$version"/>--<xsl:value-of select = "$activityname"/></xsl:element>
			               	<xsl:if test = "view-pageflow/view-pages/view-page != ''">
			               		<xsl:element name = "pages" >
			               			<xsl:for-each select = "view-pageflow/view-pages/view-page" > 
			               				<xsl:element name = "page" >
			                				<xsl:attribute name ="id" ><xsl:value-of select = "@id"/></xsl:attribute>
			                				
			                				<xsl:copy-of select = "page-label" />
			                				<xsl:copy-of select = "allow-html-in-label" />
			                				<xsl:if test = "page-template != ''">
					                			<xsl:element name = "page-layout" ><xsl:value-of select = "page-template"/></xsl:element>
					                		</xsl:if>
					                		<xsl:copy-of select = "widgets" />		                		
					                		<xsl:copy-of select = "page-validators" />
					                		<xsl:copy-of select = "actions" />
											<xsl:copy-of select = "next-page" />
				                		</xsl:element>
			                		</xsl:for-each>
			               		</xsl:element>
		               		</xsl:if>
		               		<xsl:if test = "view-pageflow/confirmation-template != ''">
			               		<xsl:element name = "confirmation-layout" ><xsl:value-of select = "view-pageflow/confirmation-template"/></xsl:element>
			               	</xsl:if>
			               	<xsl:copy-of select = "view-pageflow/confirmation-message" />
			               	<xsl:copy-of select = "view-pageflow/redirection-url" />
			               	<xsl:copy-of select = "view-pageflow/transient-data" />
			               	<xsl:copy-of select = "view-pageflow/connectors" />
		               	</xsl:element>
	               	</xsl:if>
	               	
	     	    	<xsl:choose>
	     	    		<xsl:when test = "pageflow != ''">
	     	    			<xsl:element name = "form" >
		    					<xsl:attribute name ="first-page" ><xsl:value-of select = "pageflow/@first-page"/></xsl:attribute>
		    					<xsl:attribute name ="id" ><xsl:value-of select = "$name"/>--<xsl:value-of select = "$version"/>--<xsl:value-of select = "$activityname"/>$entry</xsl:attribute> 
		    				
		    					<xsl:element name = "form-type" >entry</xsl:element>
			               		<xsl:element name = "permissions" >activity#<xsl:value-of select = "$name"/>--<xsl:value-of select = "$version"/>--<xsl:value-of select = "$activityname"/></xsl:element>
			               		<xsl:if test = "pageflow/pages/page != ''">
			               			<xsl:element name = "pages" >
			               				<xsl:for-each select = "pageflow/pages/page" > 
			               					<xsl:element name = "page" >
			                					<xsl:attribute name ="id" ><xsl:value-of select = "@id"/></xsl:attribute>
			                				
			                					<xsl:copy-of select = "page-label" />
			                					<xsl:copy-of select = "allow-html-in-label" />
			                					<xsl:if test = "page-template != ''">
					                				<xsl:element name = "page-layout" ><xsl:value-of select = "page-template"/></xsl:element>
					                			</xsl:if>
					                			<xsl:copy-of select = "widgets" />		                		
					                			<xsl:copy-of select = "page-validators" />
					                			<xsl:copy-of select = "actions" />
												<xsl:copy-of select = "next-page" />
				                			</xsl:element>
			                			</xsl:for-each>
			               			</xsl:element>
		               			</xsl:if>
		               			<xsl:if test = "pageflow/confirmation-template != ''">
			               			<xsl:element name = "confirmation-layout" ><xsl:value-of select = "pageflow/confirmation-template"/></xsl:element>
			               		</xsl:if>
			               		<xsl:copy-of select = "pageflow/confirmation-message" />
			               		<xsl:copy-of select = "pageflow/redirection-url" />
			               		<xsl:copy-of select = "pageflow/transient-data" />
			               		<xsl:copy-of select = "pageflow/connectors" />
		               		</xsl:element>
	               		</xsl:when>
	               		<xsl:when test = "pageflow = ''">
	               			<xsl:element name = "form" >
		    					<xsl:attribute name ="id" ><xsl:value-of select = "$name"/>--<xsl:value-of select = "$version"/>--<xsl:value-of select = "$activityname"/>$entry</xsl:attribute> 
		    				
		    					<xsl:element name = "form-type" >entry</xsl:element>
			               		<xsl:element name = "permissions" >activity#<xsl:value-of select = "$name"/>--<xsl:value-of select = "$version"/>--<xsl:value-of select = "$activityname"/></xsl:element>
		               		</xsl:element>
		               	</xsl:when>
	               	</xsl:choose>
	     	    </xsl:for-each>
    		</xsl:element>
    	</xsl:element>
    </xsl:element>
  </xsl:template>
</xsl:stylesheet>
