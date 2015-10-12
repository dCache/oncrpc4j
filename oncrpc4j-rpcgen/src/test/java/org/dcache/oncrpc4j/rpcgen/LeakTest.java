package org.dcache.oncrpc4j.rpcgen;

import com.google.common.base.Throwables;
import org.junit.Assert;
import org.junit.Test;

import java.net.ConnectException;
import java.net.InetAddress;

public class LeakTest {

    @Test
    public void testLeakOnConnectionRefused() throws Throwable {
        InetAddress localAddress = InetAddress.getByName("localhost");
        for (int i=0; i<10000; i++) {
            try {
                new BlobStoreClient(localAddress, 60666);
                Assert.fail("connection expected to fail");
            } catch (Throwable t) {
                Throwable cause = Throwables.getRootCause(t);
                if ((cause instanceof ConnectException) && "Connection refused".equals(cause.getMessage())) {
                    //this is expected
                    continue;
                }
                throw t;
            }
        }
    }
}
