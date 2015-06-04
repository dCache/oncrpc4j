package org.dcache.oncrpc4j.rpcgen;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SyncCalculatorTest extends AbstractCalculatorTest {

    @Test
    public void testSyncAdd() throws Exception {
        CalculationResult result = client.add_1(1, 2, 0, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.getResult());
    }

    @Test
    public void testSyncAddSimple() throws Exception {
        Assert.assertEquals(3, client.addSimple_1(1, 2, 0, null));
    }

    @Test(expected = TimeoutException.class)
    public void testSyncAddTimeout() throws Exception {
        client.add_1(3, 4, CalculatorServerImpl.SLEEP_MILLIS/10, TimeUnit.MILLISECONDS);
    }
}
