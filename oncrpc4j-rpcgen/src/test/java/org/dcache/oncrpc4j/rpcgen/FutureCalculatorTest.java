package org.dcache.oncrpc4j.rpcgen;

import org.dcache.xdr.XdrLong;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FutureCalculatorTest extends AbstractCalculatorTest {

    @Test
    public void testFutureAdd() throws Exception {
        long callTime = System.currentTimeMillis();
        Future<CalculationResult> future = client.add_1_future(1, 2);
        long retTime = System.currentTimeMillis();
        Assert.assertNotNull(future);
        CalculationResult result = future.get();
        long resTime = System.currentTimeMillis();
        Assert.assertEquals(3, result.getResult());
        //to prove async operation (there's a sleep() server-side)
        //call <= start < finish <= res
        //ret < finish
        Assert.assertTrue(callTime <= result.startMillis);
        Assert.assertTrue(result.startMillis < result.finishMillis);
        Assert.assertTrue(result.finishMillis <= resTime);
        Assert.assertTrue(retTime < result.finishMillis);
    }

    @Test
    public void testFutureAddSimple() throws Exception {
        long callTime = System.nanoTime();
        Future<XdrLong> future = client.addSimple_1_future(1, 2);
        long retTime = System.nanoTime();
        XdrLong result = future.get();
        long resTime = System.nanoTime();
        long invocationTime = retTime-callTime;
        long waitTime = resTime-retTime;
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.longValue());
        //not really proof of async, but good enough?
        //the condition below is a bit fragile and relies on the fact
        //that there's a 10-milli sleep server side and the invocation
        //is likely to take much less
        Assert.assertTrue(waitTime > invocationTime);
    }

    @Test(expected = TimeoutException.class)
    public void testFutureAddTimeout() throws Exception {
        Future<CalculationResult> future = client.add_1_future(3, 4);
        future.get(CalculatorServerImpl.SLEEP_MILLIS / 10, TimeUnit.MILLISECONDS);
    }
}
