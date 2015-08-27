package org.dcache.oncrpc4j.rpcgen;

import org.dcache.xdr.RpcReply;
import org.dcache.xdr.XdrLong;
import org.dcache.xdr.XdrTransport;
import org.junit.Assert;
import org.junit.Test;

import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class CallbackCalculatorTest extends AbstractCalculatorTest {

    @Test
    public void testCallbackAdd() throws Exception {
        final AtomicLong callbackTimeRef = new AtomicLong();
        final AtomicReference<CalculationResult> resultRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        long callTime = System.currentTimeMillis();
        client.add_1_callback(1, 2, new CompletionHandler<RpcReply, XdrTransport>() {
            @Override
            public void completed(RpcReply result, XdrTransport attachment) {
                callbackTimeRef.set(System.currentTimeMillis());
                CalculationResult calculationResult = new CalculationResult();
                try {
                    result.getReplyResult(calculationResult);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                resultRef.set(calculationResult);
                latch.countDown();
            }

            @Override
            public void failed(Throwable exc, XdrTransport attachment) {
                throw new IllegalStateException(exc);
            }
        }, 0, null, null);
        long retTime = System.currentTimeMillis();
        latch.await(3, TimeUnit.SECONDS);
        long resTime = System.currentTimeMillis();
        CalculationResult result = resultRef.get();
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.getResult());
        long callbackTime = callbackTimeRef.get();
        //to prove async operation (there's a sleep() server-side)
        //call <= start < finish <= callback <= res
        //ret < finish
        Assert.assertTrue(callTime <= result.startMillis);
        Assert.assertTrue(result.startMillis < result.finishMillis);
        Assert.assertTrue(result.finishMillis <= callbackTime);
        Assert.assertTrue(callbackTime <= resTime);
        Assert.assertTrue(retTime < result.finishMillis);
    }

    @Test
    public void testCallbackAddSimple() throws Exception {
        final AtomicLong callbackTimeRef = new AtomicLong();
        final AtomicReference<XdrLong> resultRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        long callTime = System.currentTimeMillis();
        client.addSimple_1_callback(1, 2, new CompletionHandler<RpcReply, XdrTransport>() {
            @Override
            public void completed(RpcReply result, XdrTransport attachment) {
                callbackTimeRef.set(System.currentTimeMillis());
                XdrLong calculationResult = new XdrLong();
                try {
                    result.getReplyResult(calculationResult);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                resultRef.set(calculationResult);
                latch.countDown();
            }

            @Override
            public void failed(Throwable exc, XdrTransport attachment) {
                throw new IllegalStateException(exc);
            }
        }, 0, null, null);
        long retTime = System.currentTimeMillis();
        latch.await(3, TimeUnit.SECONDS);
        long resTime = System.currentTimeMillis();
        XdrLong result = resultRef.get();
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.longValue());
        long callbackTime = callbackTimeRef.get();
        //to prove async operation (there's a sleep() server-side)
        //call < callback <= res
        //call <= ret < callback
        Assert.assertTrue(callTime < callbackTime);
        Assert.assertTrue(callbackTime <= resTime);
        Assert.assertTrue(callTime <= retTime);
        Assert.assertTrue(retTime < callbackTime);
    }

    @Test
    public void testCallbackAddTimeout() throws Exception {
        final AtomicReference<String> failureMsgRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        client.addSimple_1_callback(1, 2, new CompletionHandler<RpcReply, XdrTransport>() {
            @Override
            public void completed(RpcReply result, XdrTransport attachment) {
                failureMsgRef.set("call should have timed out");
                latch.countDown();
            }

            @Override
            public void failed(Throwable exc, XdrTransport attachment) {
                if (!(exc instanceof TimeoutException)) {
                    exc.printStackTrace();
                    failureMsgRef.set("expected TimeoutException. got "+exc);
                }
                latch.countDown();
            }
        }, CalculatorServerImpl.SLEEP_MILLIS/10, TimeUnit.MILLISECONDS, null);
        latch.await(1, TimeUnit.SECONDS);
        Assert.assertNull(failureMsgRef.get());
    }
}
