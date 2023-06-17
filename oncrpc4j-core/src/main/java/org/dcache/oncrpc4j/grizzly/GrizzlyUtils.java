/*
 * Copyright (c) 2009 - 2023 Deutsches Elektronen-Synchroton,
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
package org.dcache.oncrpc4j.grizzly;

import org.dcache.oncrpc4j.rpc.IoStrategy;
import org.dcache.oncrpc4j.rpc.MemoryAllocator;
import org.dcache.oncrpc4j.rpc.RpcMessageParserTCP;
import org.dcache.oncrpc4j.rpc.RpcMessageParserUDP;
import org.dcache.oncrpc4j.rpc.net.IpProtocolType;
import org.glassfish.grizzly.IOStrategy;
import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.memory.ByteBufferManager;
import org.glassfish.grizzly.memory.HeapMemoryManager;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.memory.PooledMemoryManager;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.UDPNIOTransport;
import org.glassfish.grizzly.strategies.LeaderFollowerNIOStrategy;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

import static com.google.common.base.Preconditions.checkArgument;
import static org.dcache.oncrpc4j.rpc.IoStrategy.WORKER_THREAD;

/**
 * Class with utility methods for Grizzly
 */
public class GrizzlyUtils {

    /**
     * Minimal number of threads used by selector.
     */
    final static int MIN_SELECTORS = 2;

    /**
     * Minimal number of threads used by for request execution.
     */
    final static int MIN_WORKERS = 5;

    /**
     * Number of available CPUs.
     */
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

    static private int getSelectorPoolSize(IoStrategy ioStrategy) {
        return ioStrategy == WORKER_THREAD
                ? Math.max(MIN_SELECTORS, CPUS / 4) : Math.max(MIN_WORKERS, CPUS);
    }

    /**
     * Get recommended default number of worker threads. The recommended number is based
     * on number of available CPUs and at least equal to {@link #MIN_WORKERS}.
     *
     * @return recommended number of worker threads.
     */
    public static int getDefaultWorkerPoolSize() {
        return Math.max(MIN_WORKERS, (CPUS * 2));
    }

    /**
     * Pre-configure Selectors thread pool for given {@link IOStrategy},
     * {@code serviceName} and {@code poolSize}. If {@code poolSize} is zero,
     * then default value will be used. If {@code poolSize} is smaller than minimal
     * allowed number of threads, then {@link #MIN_SELECTORS} will be used.
     *
     * @param ioStrategy to use
     * @param serviceName service name (affects thread names)
     * @param poolSize thread pool size. If zero, default thread pool is used.
     * @return thread pool configuration.
     */
    public static ThreadPoolConfig getSelectorPoolCfg(IoStrategy ioStrategy, String serviceName, int poolSize) {

        checkArgument(poolSize >= 0, "Negative  thread pool size");

        final int threadPoolSize = poolSize > 0 ? Math.max(poolSize, MIN_SELECTORS) : getSelectorPoolSize(ioStrategy);
        final ThreadPoolConfig poolCfg = ThreadPoolConfig.defaultConfig();
        poolCfg.setCorePoolSize(threadPoolSize).setMaxPoolSize(threadPoolSize);
        if (serviceName != null) {
            poolCfg.setPoolName(serviceName); //grizzly will add "SelectorRunner"
        }

        return poolCfg;
    }

    /**
     * Get Grizzly Memory manager matching specified allocator.
     * @param allocator allocator policy
     * @return Grizzly Memory manager matching specified allocator
     */
    public static MemoryManager getMemoryManager(MemoryAllocator allocator) {

        switch (allocator) {
            case DEFAULT:
                return MemoryManager.DEFAULT_MEMORY_MANAGER;
            case HEAP:
                return new HeapMemoryManager();
            case DIRECT:
                return new ByteBufferManager(true);
            case POOLED_HEAP:
                return new PooledMemoryManager(false);
            case POOLED_DIRECT:
                return new PooledMemoryManager(true);
            default:
                throw new RuntimeException("Unexpected memory allocator.");
        }
    }

    /**
     * Convert an oncrpc4j IoStrategy enum value into a grizzly NIOStrategy instance. Note that the only two that matter
     * here are single-thread and leader-follower. Worker threads should not be used in the grizzly layer as they would
     * just inject more context switching overhead to no benefit.
     *
     * @param ioStrategy the oncrpc4j IoStategy to map to an NIOStrategy instance.
     * @return the matching NIOStrategy instance for the specified IoStrategy value.
     */
    public static IOStrategy getNIOStrategy(IoStrategy ioStrategy) {
        switch (ioStrategy) {
            case LEADER_FOLLOWER:
                return LeaderFollowerNIOStrategy.getInstance();
            case WORKER_THREAD:
            case SAME_THREAD:
            default:
                return SameThreadIOStrategy.getInstance();
        }
    }
}
