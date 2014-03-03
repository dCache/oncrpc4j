/*
 * Copyright (c) 2009 - 2014 Deutsches Elektronen-Synchroton,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program (see the file COPYING.LIB for more
 * details); if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.dcache.xdr;

import org.glassfish.grizzly.IOStrategy;
import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.UDPNIOTransport;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

/**
 * Class with utility methods for Grizzly
 */
public class GrizzlyUtils {

    /**
     * Minimal number of threads used by selector.
     */
    private final static int MIN_SELECTORS = 2;

    /**
     * Minimal number of threads used by for request execution.
     */
    private final static int MIN_WORKERS = 5;

    final static int CPUS = Runtime.getRuntime().availableProcessors();

    private GrizzlyUtils(){}

    public static Filter rpcMessageReceiverFor(Transport t) {
        if (t instanceof TCPNIOTransport) {
            return new RpcMessageParserTCP();
        }

        if (t instanceof UDPNIOTransport) {
            return new RpcMessageParserUDP();
        }

        throw new RuntimeException("Unsupported transport: " + t.getClass().getName());
    }

    public static Class< ? extends Transport> transportFor(int protocol) {
        switch(protocol) {
            case IpProtocolType.TCP:
                return TCPNIOTransport.class;
            case IpProtocolType.UDP:
                return UDPNIOTransport.class;
        }
        throw new RuntimeException("Unsupported protocol: " + protocol);
    }

    static private int getSelectorPoolSize(IOStrategy ioStrategy) {
	return ioStrategy == WorkerThreadIOStrategy.getInstance()
		? Math.max(MIN_SELECTORS, CPUS / 4) : Math.max(MIN_WORKERS, CPUS);
    }

    static private int getWorkerPoolSize(IOStrategy ioStrategy) {
	return ioStrategy == WorkerThreadIOStrategy.getInstance()
		? Math.max(MIN_WORKERS, (CPUS * 2)) : 0;
    }

    /**
     * Pre-configure Selectors thread pool for given {@link IOStrategy}
     *
     * @param ioStrategy in use
     * @return thread pool configuration.
     */
    static ThreadPoolConfig getSelectorPoolCfg(IOStrategy ioStrategy) {
	final int poolSize = getSelectorPoolSize(ioStrategy);
	final ThreadPoolConfig poolCfg = ThreadPoolConfig.defaultConfig();
	poolCfg.setCorePoolSize(poolSize).setMaxPoolSize(poolSize);
	poolCfg.setPoolName("OncRpcSvc Selector thread");

	return poolCfg;
    }

    /**
     * Pre-configure Worker thread pool for given {@link IOStrategy}
     *
     * @param ioStrategy in use
     * @return thread pool configuration or {@code null}, if ioStrategy don't
     * supports worker threads.
     */
    static ThreadPoolConfig getWorkerPoolCfg(IOStrategy ioStrategy) {

	if (ioStrategy == SameThreadIOStrategy.getInstance()) {
	    return null;
	}

	final int poolSize = getWorkerPoolSize(ioStrategy);
	final ThreadPoolConfig poolCfg = ThreadPoolConfig.defaultConfig();
	poolCfg.setCorePoolSize(poolSize).setMaxPoolSize(poolSize);
	poolCfg.setPoolName("OncRpcSvc Worker thread");

	return poolCfg;
    }
}
