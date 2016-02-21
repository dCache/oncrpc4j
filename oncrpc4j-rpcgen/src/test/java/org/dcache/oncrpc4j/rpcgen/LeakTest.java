package org.dcache.oncrpc4j.rpcgen;

import com.google.common.base.Throwables;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.net.ConnectException;
import java.net.InetAddress;
import java.util.Locale;

public class LeakTest {

    @Before
    public void checkForWindows() {
        String osName = System.getProperty("os.name");
        //the test below runs unreasonably slow on windows
        Assume.assumeTrue(osName != null && !osName.toLowerCase(Locale.ROOT).contains("windows"));
    }

    @Test
    public void testLeakOnConnectionRefused() throws Throwable {
        InetAddress localAddress = InetAddress.getByName("localhost");
        for (int i=0; i<10000; i++) {
            try {
                new BlobStoreClient(localAddress, 666); //<1024 target port so that we dont "succeed" in connecting with the outgoing socket assigned to a prev iteration
                Assert.fail("connection expected to fail");
            } catch (Throwable t) {
                Throwable cause = Throwables.getRootCause(t);
                if ((cause instanceof ConnectException) && cause.getMessage().startsWith("Connection refused")) {
                    //this is expected
                    continue;
                }
                throw t;
            }
        }
    }
}
