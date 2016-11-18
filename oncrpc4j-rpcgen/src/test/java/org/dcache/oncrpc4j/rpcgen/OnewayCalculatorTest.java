package org.dcache.oncrpc4j.rpcgen;

import org.junit.Assert;
import org.junit.Test;

import java.io.EOFException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OnewayCalculatorTest extends AbstractCalculatorTest {

    @Test
    public void testOnewayAdd() throws Exception {
        List<MethodCall> calls = serverImpl.getMethodCalls();
        Assert.assertTrue(calls.isEmpty());
        long callTime = System.currentTimeMillis();
        client.add_1_oneway(1, 2, null);
        long retTime = System.currentTimeMillis();
        serverImpl.awaitMethodCalls(TimeUnit.SECONDS.toMillis(1)); //server has a 100 milli sleep
        calls = serverImpl.getMethodCalls();
        Assert.assertEquals(1, calls.size());
        MethodCall call = calls.get(0);
        Assert.assertEquals(3L, call.getReturnValue());
        Assert.assertTrue(retTime < call.getFinishTimestamp());
    }

    @Test(expected = EOFException.class)
    public void testDisconnection() throws Exception {
        client.add_1_oneway(1, 2, null);
        Thread.sleep(100);
        server.stop();
        Thread.sleep(100);
        client.add_1_oneway(1, 2, null);
    }
}
