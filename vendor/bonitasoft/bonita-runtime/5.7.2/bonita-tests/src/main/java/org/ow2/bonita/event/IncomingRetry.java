package org.ow2.bonita.event;

public class IncomingRetry {

  private String processName;
  private int retry;
  
  
  public IncomingRetry(String processName, int retry) {
    super();
    this.processName = processName;
    this.retry = retry;
  }
  public String getProcessName() {
    return processName;
  }
  public int getRetry() {
    return retry;
  }
  
}
