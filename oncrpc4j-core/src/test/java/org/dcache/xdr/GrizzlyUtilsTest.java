package org.dcache.xdr;

import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.junit.Test;
import static org.junit.Assert.*;

public class GrizzlyUtilsTest {

    @Test
    public void souldSReturnDefaultValueOnZeroForSelector() {
        ThreadPoolConfig tpc = GrizzlyUtils.getSelectorPoolCfg(IoStrategy.SAME_THREAD, "aService", 0);
        assertTrue(tpc.getMaxPoolSize() > 0);
    }

    @Test
    public void souldReturnMinValueIfTooSmallForSelector() {
        ThreadPoolConfig tpc = GrizzlyUtils.getSelectorPoolCfg(IoStrategy.SAME_THREAD, "aService", GrizzlyUtils.MIN_SELECTORS - 1);
        assertEquals("Must return minimal value", GrizzlyUtils.MIN_SELECTORS, tpc.getMaxPoolSize());
    }

    @Test
    public void souldReturnExpectedValueForSelector() {
        ThreadPoolConfig tpc = GrizzlyUtils.getSelectorPoolCfg(IoStrategy.SAME_THREAD, "aService", GrizzlyUtils.MIN_SELECTORS + 1);
        assertEquals("Must return provided value", GrizzlyUtils.MIN_SELECTORS + 1, tpc.getMaxPoolSize());
    }

    @Test
    public void souldSReturnDefaultValueOnZeroForWorker() {
        ThreadPoolConfig tpc = GrizzlyUtils.getWorkerPoolCfg(IoStrategy.WORKER_THREAD, "aService", 0);
        assertTrue(tpc.getMaxPoolSize() > 0);
    }

    @Test
    public void souldReturnMinValueIfTooSmallForWorker() {
        ThreadPoolConfig tpc = GrizzlyUtils.getWorkerPoolCfg(IoStrategy.WORKER_THREAD, "aService", GrizzlyUtils.MIN_WORKERS - 1);
        assertEquals("Must return minimal value", GrizzlyUtils.MIN_WORKERS, tpc.getMaxPoolSize());
    }

    @Test
    public void souldReturnExpectedValueForWorker() {
        ThreadPoolConfig tpc = GrizzlyUtils.getWorkerPoolCfg(IoStrategy.WORKER_THREAD, "aService", GrizzlyUtils.MIN_WORKERS + 1);
        assertEquals("Must return provided value", GrizzlyUtils.MIN_WORKERS + 1, tpc.getMaxPoolSize());
    }

    @Test
    public void souldReturnNullIfNowWorkerThreadConfigured() {
        ThreadPoolConfig tpc = GrizzlyUtils.getWorkerPoolCfg(IoStrategy.SAME_THREAD, "aService", 1);
        assertNull("Must return null if no worker thread configured", tpc);
    }

    @Test(expected = IllegalArgumentException.class)
    public void souldThrowExceptionIfNegativeSizeProvidedForWorker() {
        ThreadPoolConfig tpc = GrizzlyUtils.getWorkerPoolCfg(IoStrategy.WORKER_THREAD, "aService", -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void souldThrowExceptionIfNegativeSizeProvidedForSelector() {
        ThreadPoolConfig tpc = GrizzlyUtils.getSelectorPoolCfg(IoStrategy.WORKER_THREAD, "aService", -1);
    }

}
