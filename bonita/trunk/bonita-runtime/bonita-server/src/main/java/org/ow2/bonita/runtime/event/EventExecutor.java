/**
 * Copyright (C) 2009  BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA  02110-1301, USA.
 **/
package org.ow2.bonita.runtime.event;

import java.io.Serializable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ow2.bonita.services.CommandService;
import org.ow2.bonita.util.Command;
import org.ow2.bonita.util.EnvTool;
import org.ow2.bonita.util.Misc;

public class EventExecutor implements Serializable {

  private static final Logger LOG = Logger.getLogger(EventExecutor.class.getName());
  private static final long serialVersionUID = 1L;

  //injected
  transient CommandService commandService;
  int nbrOfThreads = 3;
  int idleMillis = 5000; // default normal poll interval is 5 seconds
  int lockMillis = 1000 * 60 * 2; // default max lock time is 2 minutes
  int minimumInterval = 50;
  int retries = 1;
  String name;

  private transient ExecutorService threadPool;
  private transient EventDispatcherThread dispatcherThread = null;
  private boolean isActive = false;

  //commands
  Command<Long> nextDueDateCommand;

  public synchronized void start() {
    final String domain = commandService.execute(new GetDomainCommand());
    name = EventExecutor.class.getSimpleName() + "-" + Misc.getHostName() + "-" + domain;
    if (isActive) {
      LOG.severe("Cannot start event executor '" + name + "' because it is already running...");
      return;
    }
    if (!isActive) {
      nextDueDateCommand = new GetNextDueDateCmd();
      isActive = true;
      if (LOG.isLoggable(Level.INFO)) {
        LOG.info("starting event executor threads for event executor '" + name + "'...");
      }
      threadPool = new ThreadPoolExecutor(nbrOfThreads, 
          nbrOfThreads, 
          0L, 
          TimeUnit.MILLISECONDS,
          new ArrayBlockingQueue<Runnable>(nbrOfThreads), 
          EventRejectionHandler.INSTANCE);

      if (LOG.isLoggable(Level.INFO)) {
        LOG.info("starting dispatcher thread for event executor '" + name + "'...");
      }
      dispatcherThread = new EventDispatcherThread(this, EventDispatcherThread.class.getSimpleName() + "-" + Misc.getHostName() + "-" + domain);
      dispatcherThread.start();
    }
  }

  static final class EventRejectionHandler implements RejectedExecutionHandler {

    static final EventRejectionHandler INSTANCE = new EventRejectionHandler();

    public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
      try {
        executor.getQueue().put(task);
      } catch (InterruptedException e) {
        throw new RejectedExecutionException("queuing " + task + " got interrupted", e);
      }
    }
  }

  public boolean isActive() {
    return isActive;
  }

  void internalRefresh() {
    if (dispatcherThread != null && dispatcherThread.isActive()) {
      dispatcherThread.refresh();
    }
  }

  public void refresh() {
    EnvTool.getTransaction().registerSynchronization(new EventAddedNotification(this));
  }

  public void notifyThreadFinished() {
    new EventDispatcherRefreshThread(dispatcherThread).start();
  }

  public synchronized void stop() {
    stop(false);
  }

  public synchronized void stop(boolean join) {
    if (LOG.isLoggable(Level.INFO)) {
      LOG.info("stopping event executor");
    }

    if (isActive) {
      isActive = false;
      if (LOG.isLoggable(Level.INFO)) {
        LOG.info("Event executor: deactivating dispatcher thread...");
      }
      dispatcherThread.deactivate(true);
      if (LOG.isLoggable(Level.INFO)) {
        LOG.info("Event executor: dispatcher thread deactivated...");
      }
      if (LOG.isLoggable(Level.INFO)) {
        LOG.info("Event executor: shutdown threadpool...");
      }
      threadPool.shutdown();
      if (LOG.isLoggable(Level.INFO)) {
        LOG.info("Event executor: threadpool shutdowned...");
      }
      if (join) {
        try {
          if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Event executor: waiting for threadPool termination...");
          }
          threadPool.awaitTermination(1000 * 60 * 5, TimeUnit.MILLISECONDS);
          if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Event executor: threadPool termination OK...");
          }
        } catch (InterruptedException e) {
          LOG.severe("joining got interrupted");
        }
      }
    } else if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("ignoring stop: event executor '" + name + "' not started");
    }
    if (LOG.isLoggable(Level.INFO)) {
      LOG.info("Event executor stopped");
    }
  }

  public int getNbrOfThreads() {
    return nbrOfThreads;
  }

  public ExecutorService getThreadPool() {
    return threadPool;
  }

  public int getIdleMillis() {
    return idleMillis;
  }

  public int getLockMillis() {
    return lockMillis;
  }

  public CommandService getCommandService() {
    return commandService;
  }

  public Command<Long> getNextDueDateCommand() {
    return nextDueDateCommand;
  }

  public int getMinimumInterval() {
    return minimumInterval;
  }

  public int getRetries() {
	return retries;
  }

  public void setCommandService(CommandService commandService) {
    this.commandService = commandService;
  }

}
