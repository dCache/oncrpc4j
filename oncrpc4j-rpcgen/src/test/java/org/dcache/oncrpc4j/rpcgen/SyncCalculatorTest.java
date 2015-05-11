package org.dcache.oncrpc4j.rpcgen;

import org.junit.Assert;
import org.junit.Test;

public class SyncCalculatorTest extends AbstractCalculatorTest {

    @Test
    public void testSyncAdd() throws Exception {
        CalculationResult result = client.add_1(1, 2);
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.getResult());
    }

    @Test
    public void testSyncAddSimple() throws Exception {
        Assert.assertEquals(3, client.addSimple_1(1, 2));
    }
}
