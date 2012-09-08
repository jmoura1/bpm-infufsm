package org.ow2.bonita.services;


public interface Journal extends Querier, Recorder {

  /*
   * Meta
   */
  void storeMetaData(String key, String value);
  String getMetaData(String key);
  void deleteMetaData(String key);
  
  void lockMetadata(String seqName);
  long getLockedMetadata(String seqName);
  void updateLockedMetadata(String seqName, long value);
  void removeLockedMetadata(String seqName);
}
