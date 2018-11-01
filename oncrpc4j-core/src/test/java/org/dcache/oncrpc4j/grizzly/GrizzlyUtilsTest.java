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

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfNegativeSizeProvidedForSelector() {
        ThreadPoolConfig tpc = GrizzlyUtils.getSelectorPoolCfg(IoStrategy.WORKER_THREAD, "aService", -1);
    }

}
