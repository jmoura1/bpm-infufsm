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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.services.EventService;
import org.ow2.bonita.util.Command;
import org.ow2.bonita.util.EnvTool;

public class EventDispatcherThread extends Thread {

  static final Logger LOG = Logger.getLogger(EventDispatcherThread.class.getName());

  protected volatile boolean isActive = true;
  private EventExecutor executor;
  private boolean refresh;
  private int currentIdleInterval;
  private int minimumInterval;
  private Object semaphore = new Object();
  private Object threadSemaphore = new Object();

  EventDispatcherThread(EventExecutor executor, String name) {
    super(name);
    this.executor = executor;
  }

  public void run() {
    if (LOG.isLoggable(Level.INFO)) {
      LOG.info("starting...");
    }
    currentIdleInterval = executor.getIdleMillis();
    minimumInterval = executor.getMinimumInterval();
    try {
      while (isActive()) {
        try {
          // refresh is set to true in refresh() below
          refresh = false;
          currentIdleInterval = executor.getIdleMillis();

          final Map<ProcessInstanceUUID, Set<EventCoupleId>> validCouplesMap = executor.getCommandService().execute(new GetEventsCouplesCommand());
          
          
          if (validCouplesMap != null && (!validCouplesMap.isEmpty())) {
            for (Set<EventCoupleId> validCouples : validCouplesMap.values()) {
                final EventExecutorThread thread = new EventExecutorThread(executor, validCouples);
                executor.getThreadPool().submit(thread);
            }
            synchronized (threadSemaphore) {
              threadSemaphore.wait(executor.getLockMillis());
            }
          }
          executor.getCommandService().execute(new RemoveOverdueEvents());
          if (isActive()) {
            long waitPeriod = getWaitPeriod();
            if (waitPeriod > 0) {
              synchronized (semaphore) {
                if (!refresh) {
                  if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine(getName() + " will wait for max " + waitPeriod + "ms on " + executor);
                  }
                  semaphore.wait(waitPeriod);
                  if (LOG.isLoggable(Level.INFO)) {
                    LOG.info(getName() + " woke up, refresh=" + refresh);
                  }
                } else {
                  if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("skipped wait because new message arrived");
                  }
                }
              }

            }
          }

        } catch (InterruptedException e) {
          LOG.info((isActive() ? "active" : "inactive") + " event dispatcher thread '" + getName() + "' got interrupted");
        } catch (Exception e) {
          if (LOG.isLoggable(Level.SEVERE)) {
            LOG.severe("exception in event executor thread. waiting " + currentIdleInterval + " milliseconds: " + e.getMessage());
            e.printStackTrace();
          }
          try {
            synchronized (semaphore) {
              semaphore.wait(currentIdleInterval);
            }
          } catch (InterruptedException e2) {
            if (LOG.isLoggable(Level.FINE)) {
              LOG.fine("delay after exception got interrupted: " + e2);
            }
          }
          // after an exception, the current idle interval is doubled to prevent
          // continuous exception generation when e.g. the db is unreachable
          currentIdleInterval = currentIdleInterval * 2;
        }
      }
    } catch (Throwable t) {
      t.printStackTrace();
    } finally {
      if (LOG.isLoggable(Level.INFO)) {
        LOG.info(getName() + " leaves cyberspace");
      }
    }
  }

  protected Long getNextDueDate() {
    final Command<Long> getNextDueDate = executor.getNextDueDateCommand();
    return executor.getCommandService().execute(getNextDueDate);
  }

  protected long getWaitPeriod() {
    long interval = executor.getIdleMillis();
    final Long nextDueDate = getNextDueDate();

    if (nextDueDate != null) {
      final long currentTimeMillis = System.currentTimeMillis();
      if (nextDueDate < (currentTimeMillis + currentIdleInterval)) {
        interval = nextDueDate - currentTimeMillis;
      }
    }
    if (interval <= minimumInterval) {
      interval = minimumInterval;
    }
    return interval;
  }

  public void refresh() {
    if (LOG.isLoggable(Level.INFO)) {
      LOG.info("notifying Event executor dispatcher thread of new Event");
    }
    synchronized (semaphore) {
      refresh = true;
      semaphore.notify();
    }
  }

  public void notifyThreadFinished() {
    if (LOG.isLoggable(Level.INFO)) {
      LOG.info("notifying Event executor dispatcher thread of new Event");
    }
    synchronized (threadSemaphore) {
      threadSemaphore.notify();
    }
  }

  public void deactivate(boolean join) {
    if (isActive()) {
      if (LOG.isLoggable(Level.INFO)) {
        LOG.info("deactivating "+getName());
      }
      setIsActive(false);
      interrupt();
      if (join) {
        try {
          if (LOG.isLoggable(Level.INFO)) {
            LOG.info("joining "+getName());
          }
          join(1000 * 60 * 1);
        } catch (InterruptedException e) {
          LOG.severe("joining "+getName()+" got interrupted");
        }
      }
    } else {
      if (LOG.isLoggable(Level.INFO)) {
        LOG.info("ignoring deactivate: "+getName()+" is not active");
      }
    }
    if (LOG.isLoggable(Level.INFO)) {
      LOG.info("Event dispatcher thread: "+getName()+" deactivated");
    }
  }

  private synchronized void setIsActive(final boolean value) {
    this.isActive = false;
  }

  public synchronized boolean isActive() {
    return isActive;
  }

}
