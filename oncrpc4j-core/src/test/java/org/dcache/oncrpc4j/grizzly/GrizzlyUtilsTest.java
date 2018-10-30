package org.dcache.oncrpc4j.grizzly;

import org.dcache.oncrpc4j.rpc.IoStrategy;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.junit.Test;
import static org.junit.Assert.*;

public class GrizzlyUtilsTest {

    @Test
    public void shouldSReturnDefaultValueOnZeroForSelector() {
        ThreadPoolConfig tpc = GrizzlyUtils.getSelectorPoolCfg(IoStrategy.SAME_THREAD, "aService", 0);
        assertTrue(tpc.getMaxPoolSize() > 0);
    }

    @Test
    public void shouldReturnMinValueIfTooSmallForSelector() {
        ThreadPoolConfig tpc = GrizzlyUtils.getSelectorPoolCfg(IoStrategy.SAME_THREAD, "aService", GrizzlyUtils.MIN_SELECTORS - 1);
        assertEquals("Must return minimal value", GrizzlyUtils.MIN_SELECTORS, tpc.getMaxPoolSize());
    }

    @Test
    public void shouldReturnExpectedValueForSelector() {
        ThreadPoolConfig tpc = GrizzlyUtils.getSelectorPoolCfg(IoStrategy.SAME_THREAD, "aService", GrizzlyUtils.MIN_SELECTORS + 1);
        assertEquals("Must return provided value", GrizzlyUtils.MIN_SELECTORS + 1, tpc.getMaxPoolSize());
    }

    @Test
    public void shouldSReturnDefaultValueOnZeroForWorker() {
        ThreadPoolConfig tpc = GrizzlyUtils.getWorkerPoolCfg(IoStrategy.WORKER_THREAD, "aService", 0);
        assertTrue(tpc.getMaxPoolSize() > 0);
    }

    @Test
    public void shouldReturnMinValueIfTooSmallForWorker() {
        ThreadPoolConfig tpc = GrizzlyUtils.getWorkerPoolCfg(IoStrategy.WORKER_THREAD, "aService", GrizzlyUtils.MIN_WORKERS - 1);
        assertEquals("Must return minimal value", GrizzlyUtils.MIN_WORKERS, tpc.getMaxPoolSize());
    }

    @Test
    public void shouldReturnExpectedValueForWorker() {
        ThreadPoolConfig tpc = GrizzlyUtils.getWorkerPoolCfg(IoStrategy.WORKER_THREAD, "aService", GrizzlyUtils.MIN_WORKERS + 1);
        assertEquals("Must return provided value", GrizzlyUtils.MIN_WORKERS + 1, tpc.getMaxPoolSize());
    }

    @Test
    public void shouldReturnNullIfNowWorkerThreadConfigured() {
        ThreadPoolConfig tpc = GrizzlyUtils.getWorkerPoolCfg(IoStrategy.SAME_THREAD, "aService", 1);
        assertNull("Must return null if no worker thread configured", tpc);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfNegativeSizeProvidedForWorker() {
        ThreadPoolConfig tpc = GrizzlyUtils.getWorkerPoolCfg(IoStrategy.WORKER_THREAD, "aService", -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfNegativeSizeProvidedForSelector() {
        ThreadPoolConfig tpc = GrizzlyUtils.getSelectorPoolCfg(IoStrategy.WORKER_THREAD, "aService", -1);
    }

}
