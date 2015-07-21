package org.dcache.oncrpc4j.rpcgen;

import org.junit.Assert;
import org.junit.Test;

public class SyncBlobStoreTest extends AbstractBlobStoreTest {

    @Test
    public void testSimpleScenario() throws Exception {
        Key key = new Key();
        key.setData(new byte[]{1,2,3});
        Value value = new Value();
        value.notNull = true;
        value.data = new byte[]{4,5,6};
        client.put_1(key, value, 0, null);
        Value returned = client.get_1(key, 0, null);
        Assert.assertNotNull(returned);
        Assert.assertTrue(returned != value);
        byte[] returnedValue = returned.data;
        Assert.assertArrayEquals(new byte[] {4,5,6}, returnedValue);
    }

    @Test
    public void testNoSuchValue() throws Exception {
        Key key = new Key();
        key.setData(new byte[]{1,2,3});
        Value returned = client.get_1(key, 0, null);
        int g = 8;
    }
}
