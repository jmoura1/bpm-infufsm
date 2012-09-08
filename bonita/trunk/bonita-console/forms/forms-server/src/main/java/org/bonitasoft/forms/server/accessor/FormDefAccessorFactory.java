/**
 * Copyright (C) 2010 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.forms.server.accessor;

import java.util.Date;

import org.bonitasoft.forms.server.accessor.impl.EngineActivityFormDefAccessorImpl;
import org.bonitasoft.forms.server.accessor.impl.EngineApplicationFormDefAccessorImpl;
import org.bonitasoft.forms.server.accessor.impl.EngineProcessFormDefAccessorImpl;
import org.bonitasoft.forms.server.accessor.impl.XMLActivityFormDefAccessorImpl;
import org.bonitasoft.forms.server.accessor.impl.XMLApplicationFormDefAccessorImpl;
import org.bonitasoft.forms.server.accessor.impl.XMLProcessFormDefAccessorImpl;
import org.bonitasoft.forms.server.exception.ActivityFormDefinitionNotFoundException;
import org.bonitasoft.forms.server.exception.ApplicationFormDefinitionNotFoundException;
import org.bonitasoft.forms.server.exception.ProcessFormDefinitionNotFoundException;
import org.ow2.bonita.facade.exception.ProcessNotFoundException;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.w3c.dom.Document;

/**
 * @author Anthony Birembaut
 *
 */
public class FormDefAccessorFactory {

    @Deprecated
    public static XMLActivityFormDefAccessorImpl getXMLActivityFormDefAccessor(final Document document, final ProcessDefinitionUUID processDefinitionUUID, final String locale, final Date processDeploymentDate, final String activityName, final boolean isEditMode) throws ActivityFormDefinitionNotFoundException {
        return new XMLActivityFormDefAccessorImpl(document, processDefinitionUUID, locale, processDeploymentDate, activityName, isEditMode);
    }
    
    @Deprecated
    public static EngineActivityFormDefAccessorImpl getEngineActivityFormDefAccessor(final ProcessDefinitionUUID processDefinitionUUID, final String activityName, final boolean includeProcessVariables, final boolean isEditMode) {
        return new EngineActivityFormDefAccessorImpl(processDefinitionUUID, activityName, includeProcessVariables, isEditMode);
    }
    
    @Deprecated
    public static XMLProcessFormDefAccessorImpl getXMLProcessFormDefAccessor(final Document document, final ProcessDefinitionUUID processDefinitionUUID, final String locale, final Date processDeploymentDate, final boolean isInstanceRecap, final boolean isEditMode) throws ProcessFormDefinitionNotFoundException {
        return new XMLProcessFormDefAccessorImpl(document, processDefinitionUUID, locale, processDeploymentDate, isInstanceRecap, isEditMode);
    }
    
    @Deprecated
    public static EngineProcessFormDefAccessorImpl getEngineProcessFormDefAccessor(final ProcessDefinitionUUID processDefinitionUUID, final boolean isInstanceRecap, final boolean isEditMode) throws ProcessNotFoundException {
        return new EngineProcessFormDefAccessorImpl(processDefinitionUUID, isInstanceRecap, isEditMode);
    }
    
    public static XMLApplicationFormDefAccessorImpl getXMLApplicationFormDefAccessor(final String formId, final Document document, final String locale, final Date processDeploymentDate) throws ApplicationFormDefinitionNotFoundException {
        return new XMLApplicationFormDefAccessorImpl(formId, document, locale, processDeploymentDate);
    }
    
    public static EngineApplicationFormDefAccessorImpl getEngineApplicationFormDefAccessor(final ProcessDefinitionUUID processDefinitionUUID, final String activityName, final boolean includeApplicationVariables, final boolean isEditMode, final boolean isRecap) {
        return new EngineApplicationFormDefAccessorImpl(processDefinitionUUID, activityName, includeApplicationVariables, isEditMode,isRecap);
    }
    
}
