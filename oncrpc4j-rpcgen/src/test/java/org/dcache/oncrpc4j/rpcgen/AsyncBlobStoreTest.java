package org.dcache.oncrpc4j.rpcgen;

import org.dcache.xdr.RpcAuthTypeNone;
import org.junit.Test;

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
    }
}
