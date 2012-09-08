/**
 * Copyright (C) 2011 BonitaSoft S.A.
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
package org.ow2.bonita.services.impl;

import java.util.Date;

import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.services.Document;
import org.ow2.bonita.util.xml.XStreamUtil;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * @author Matthieu Chaffotte
 *
 */
public class DocumentImpl implements Document {

  private String id;
  private String name;
  private String folderId;
  private String author;
  private Date creationDate;
  private String lastModifiedBy;
  private Date lastModificationDate;
  private boolean latestVersion;
  private boolean majorVersion;
  private String versionLabel;
  

private String versionSeriesId;
  private String contentMimeType;
  private String contentFileName;
  private long contentSize;
  private ProcessInstanceUUID processInstanceUUID;
  private ProcessDefinitionUUID processDefinitionUUID;

  public DocumentImpl(final String name, final String folderId, final String author,
      final Date creationDate, Date lastModificationDate, final boolean latestVersion, final boolean majorVersion,final String versionLabel, final String versionSeriesId,
      final String contentFileName, final String contentMimeType, final long contentSize, ProcessDefinitionUUID processDefinitionUUID, ProcessInstanceUUID processInstanceUUID) {
    this.name = name;
    this.folderId = folderId;
    this.author = author;
    this.creationDate = creationDate;
    this.lastModifiedBy = author;
    this.lastModificationDate = lastModificationDate;
    this.latestVersion = latestVersion;
    this.majorVersion = majorVersion;
    this.versionLabel = versionLabel;
    this.versionSeriesId = versionSeriesId;
    this.contentFileName = contentFileName;
    this.contentMimeType = contentMimeType;
    this.contentSize = contentSize;
    this.processInstanceUUID = processInstanceUUID;
    this.processDefinitionUUID = processDefinitionUUID;
  }

  /**
   * @param name
   * @param versionLabel
   */
  public DocumentImpl(String name) {
    this.name = name;
  }

  /**
   * @param name
   * @param contentFileName
   * @param contentMimeType
   * @param contentSize
   * @param versionLabel
   */
  public DocumentImpl(String name, String contentFileName, String contentMimeType, long contentSize) {
    this.name = name;
    this.contentFileName = contentFileName;
    this.contentMimeType = contentMimeType;
    this.contentSize = contentSize;

  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }
  public void setFolderId(String folderId) {
      this.folderId = folderId;
  }
  
  
  public void setProcessInstanceUUID(ProcessInstanceUUID processInstanceUUID) {
      this.processInstanceUUID = processInstanceUUID;
  }
  
  
  public void setProcessDefinitionUUID(ProcessDefinitionUUID processDefinitionUUID) {
      this.processDefinitionUUID = processDefinitionUUID;
  }

  public String getParentFolderId() {
    return folderId;
  }

  public String getAuthor() {
    return author;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public Date getLastModificationDate() {
    return lastModificationDate;
  }

  public boolean isLatestVersion() {
    return latestVersion;
  }

  public boolean isMajorVersion() {
    return majorVersion;
  }

  public String getVersionLabel() {
    return versionLabel;
  }

  public String getVersionSeriesId() {
    return versionSeriesId;
  }

  public String getContentMimeType() {
    return contentMimeType;
  }

  public String getContentFileName() {
    return contentFileName;
  }

  public long getContentSize() {
    return contentSize;
  }

  /**
   * @return the processInstanceUUID
   */
  public ProcessInstanceUUID getProcessInstanceUUID() {
    return processInstanceUUID;
  }

  /**
   * @return the processDefinitionUUID
   */
  public ProcessDefinitionUUID getProcessDefinitionUUID() {
    return processDefinitionUUID;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((author == null) ? 0 : author.hashCode());
    result = prime * result + ((contentFileName == null) ? 0 : contentFileName.hashCode());
    result = prime * result + ((contentMimeType == null) ? 0 : contentMimeType.hashCode());
    result = prime * result + (int) (contentSize ^ (contentSize >>> 32));
    result = prime * result + ((creationDate == null) ? 0 : creationDate.hashCode());
    result = prime * result + ((folderId == null) ? 0 : folderId.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((lastModificationDate == null) ? 0 : lastModificationDate.hashCode());
    result = prime * result + ((lastModifiedBy == null) ? 0 : lastModifiedBy.hashCode());
    result = prime * result + (latestVersion ? 1231 : 1237);
    result = prime * result + (majorVersion ? 1231 : 1237);
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((versionLabel == null) ? 0 : versionLabel.hashCode());
    result = prime * result + ((versionSeriesId == null) ? 0 : versionSeriesId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    DocumentImpl other = (DocumentImpl) obj;
    if (author == null) {
      if (other.author != null)
        return false;
    } else if (!author.equals(other.author))
      return false;
    if (contentFileName == null) {
      if (other.contentFileName != null)
        return false;
    } else if (!contentFileName.equals(other.contentFileName))
      return false;
    if (contentMimeType == null) {
      if (other.contentMimeType != null)
        return false;
    } else if (!contentMimeType.equals(other.contentMimeType))
      return false;
    if (contentSize != other.contentSize)
      return false;
    if (creationDate == null) {
      if (other.creationDate != null)
        return false;
    } else if (!creationDate.equals(other.creationDate))
      return false;
    if (folderId == null) {
      if (other.folderId != null)
        return false;
    } else if (!folderId.equals(other.folderId))
      return false;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    if (lastModificationDate == null) {
      if (other.lastModificationDate != null)
        return false;
    } else if (!lastModificationDate.equals(other.lastModificationDate))
      return false;
    if (lastModifiedBy == null) {
      if (other.lastModifiedBy != null)
        return false;
    } else if (!lastModifiedBy.equals(other.lastModifiedBy))
      return false;
    if (latestVersion != other.latestVersion)
      return false;
    if (majorVersion != other.majorVersion)
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (versionLabel == null) {
      if (other.versionLabel != null)
        return false;
    } else if (!versionLabel.equals(other.versionLabel))
      return false;
    if (versionSeriesId == null) {
      if (other.versionSeriesId != null)
        return false;
    } else if (!versionSeriesId.equals(other.versionSeriesId))
      return false;
    return true;
  }

  @Override
  public String toString() {
    XStream xstream = XStreamUtil.getDefaultXstream();
    return xstream.toXML(this);
  }

}
