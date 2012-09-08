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
package org.ow2.bonita.facade.monitoring.model;

import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.ow2.bonita.facade.monitoring.model.impl.MBeanUtil;

/**
 * @author Christophe Havard
 * 
 */
public class Jvm implements JvmMBean {

	private final MBeanServer mbserver;
	private final ObjectName name;
	private ObjectName nameAfterRegistration;
	private final MemoryMXBean memoryMB;
	private final OperatingSystemMXBean osMB;
	private final RuntimeMXBean runtimeMB;
	private final ThreadMXBean threadMB;
	private long uptime = 0;
	private double systemLoad = 0;
	private long usage = 0;
	private long startTime = 0;
	private int threadCount = 0;

	protected static final Logger LOG = Logger.getLogger(Jvm.class.getName());

	/**
	 * Default constructor.
	 * 
	 * @throws NullPointerException
	 * @throws MalformedObjectNameException
	 */
	public Jvm() throws MalformedObjectNameException, NullPointerException {
		this.mbserver = MBeanUtil.getMBeanServer();
		this.name = new ObjectName(JVM_MBEAN_NAME);
		this.memoryMB = MBeanUtil.getMemoryMXBean();
		this.osMB = MBeanUtil.getOSMXBean();
		this.runtimeMB = MBeanUtil.getRuntimeMXBean();
		this.threadMB = MBeanUtil.getThreadMXBean();
	}

	public void start() throws MBeanStartException {
		try {
			// register the MXBean
			if (!mbserver.isRegistered(name)) {
				// Some application server rename the mbean while registering
				// it.
				final ObjectInstance objectInstance = mbserver.registerMBean(
						this, name);
				nameAfterRegistration = objectInstance.getObjectName();
				if (LOG.isLoggable(Level.INFO)) {
					LOG.log(Level.INFO, "Registered MBean: "
							+ nameAfterRegistration.getCanonicalName());
				}
			}
		} catch (final Exception e) {
			throw new MBeanStartException(e);
		}

	}

	public void stop() throws MBeanStopException {
		try {
			// Unregister the MXBean
			if (mbserver.isRegistered(name)) {
				mbserver.unregisterMBean(name);
				if (LOG.isLoggable(Level.INFO)) {
					LOG.log(Level.INFO, "Un-registered MBean: " + name);
				}
			}
			if (mbserver.isRegistered(nameAfterRegistration)) {
				mbserver.unregisterMBean(nameAfterRegistration);
				if (LOG.isLoggable(Level.INFO)) {
					LOG.log(Level.INFO, "Un-registered MBean: "
							+ nameAfterRegistration.getCanonicalName());
				}
			}
		} catch (final Exception e) {
			throw new MBeanStopException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bonitasoft.engine.monitoring.model.SJvmMXBean#getMemoryUsage()
	 */
	public long getCurrentMemoryUsage() {
		usage = (this.memoryMB.getHeapMemoryUsage().getUsed() + this.memoryMB
				.getNonHeapMemoryUsage().getUsed());
		return usage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.bonitasoft.engine.monitoring.model.SJvmMXBean#getSystemLoadAverage()
	 */
	public double getSystemLoadAverage() {
		systemLoad = this.osMB.getSystemLoadAverage();
		return systemLoad;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bonitasoft.engine.monitoring.model.SJvmMXBean#getUpTime()
	 */
	public long getUpTime() {
		uptime = this.runtimeMB.getUptime();
		return uptime;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bonitasoft.engine.monitoring.model.SJvmMXBean#getStartTime()
	 */
	@Override
	public long getStartTime() {
		startTime = this.runtimeMB.getStartTime();
		return startTime;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bonitasoft.engine.monitoring.model.SJvmMXBean#getThreadCpuTime()
	 */
	@Override
	public long getTotalThreadsCpuTime() {
		long cpuTimeSum = -1;
		// fetch the threadCpuTime only if it's available
		if (this.threadMB.isThreadCpuTimeSupported()
				&& this.threadMB.isThreadCpuTimeEnabled()) {
			// take the total number of thread and sum the cpu time for each.
			final long[] threadIds = this.threadMB.getAllThreadIds();
			cpuTimeSum = 0;
			for (final long id : threadIds) {
				cpuTimeSum += this.threadMB.getThreadCpuTime(id);
			}
		}
		return cpuTimeSum;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bonitasoft.engine.monitoring.model.SJvmMXBean#getThreadCount()
	 */
	@Override
	public int getThreadCount() {
		threadCount = this.threadMB.getThreadCount();
		return threadCount;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ow2.bonita.facade.monitoring.model.SJvmMXBean#getMemoryUsagePercentage
	 * ()
	 */
	@Override
	public float getMemoryUsagePercentage() {
		float currentUsage = (this.memoryMB.getHeapMemoryUsage().getUsed() + this.memoryMB
				.getNonHeapMemoryUsage().getUsed());
		float maxMemory = (this.memoryMB.getHeapMemoryUsage().getMax() + this.memoryMB
				.getNonHeapMemoryUsage().getMax());
		float percentage = currentUsage / maxMemory;
		return (percentage * 100);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ow2.bonita.facade.monitoring.model.JvmMBean#getOSArch()
	 */
	@Override
	public String getOSArch() {
		return this.osMB.getArch();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ow2.bonita.facade.monitoring.model.JvmMBean#getAvailableProcessors()
	 */
	@Override
	public int getAvailableProcessors() {
		return this.osMB.getAvailableProcessors();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ow2.bonita.facade.monitoring.model.JvmMBean#getOS()
	 */
	@Override
	public String getOSName() {
		return (this.osMB.getName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ow2.bonita.facade.monitoring.model.JvmMBean#getOSVersion()
	 */
	@Override
	public String getOSVersion() {
		return (this.osMB.getVersion());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ow2.bonita.facade.monitoring.model.JvmMBean#getJvmName()
	 */
	@Override
	public String getJvmName() {
		return (this.runtimeMB.getVmName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ow2.bonita.facade.monitoring.model.JvmMBean#getJvmVendor()
	 */
	@Override
	public String getJvmVendor() {
		return (this.runtimeMB.getVmVendor());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ow2.bonita.facade.monitoring.model.JvmMBean#getJvmVersion()
	 */
	@Override
	public String getJvmVersion() {
		return (this.runtimeMB.getVmVersion());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ow2.bonita.facade.monitoring.model.JvmMBean#getJvmSystemProperties()
	 */
	@Override
	public Map<String, String> getJvmSystemProperties() {
		return (this.runtimeMB.getSystemProperties());
	}

}
