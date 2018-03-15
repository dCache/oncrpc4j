package org.dcache.oncrpc4j.rpcgen;

import org.dcache.oncrpc4j.rpc.RpcAuthTypeNone;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author Radai Rosenblatt
 */
public class AsyncBlobStoreTest extends AbstractBlobStoreTest {

    @Test
    public void testTonsOfQueuedRequests() throws Exception {
        Key k = new Key();
        byte[] blob = {1, 2, 3, 4};
        k.setData(blob);
        Value v = new Value();
        v.notNull = true;
        v.data = blob;
        serverImpl.setSleepFor(0);
        int size = 150000;
        RpcAuthTypeNone auth = new RpcAuthTypeNone();
        int issued = 0;
        int collected = 0;
        try {
            for (int i = 0; i < size; i++) {
                client.put_1_oneway(k, v, auth);
                issued++;
            }
            System.err.println(size + " requests enqueued");
        } finally {
            System.err.println("issued " + issued + " collected " + collected);
        }
        long timeout = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1L);
        while (System.currentTimeMillis() < timeout) {
            Thread.sleep(100);
            if (serverImpl.getNumRequestsProcessed() >= size) {
                break;
            }
        }
        Assert.assertTrue("not all requests sent were processed within the timeout", serverImpl.getNumRequestsProcessed() >= size);
    }
}
