package org.dcache.oncrpc4j.rpc;

import org.dcache.oncrpc4j.rpc.net.IpProtocolType;
import org.dcache.oncrpc4j.xdr.XdrVoid;
import org.dcache.oncrpc4j.xdr.XdrString;
import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class ClientServerTest {

    private static final int PROGNUM = 100017;
    private static final int PROGVER = 1;

    private static final int ECHO = 1;
    private static final int UPPER = 2;
    private static final int SHUTDOWN = 3;
    private static final int LOST = 4;

    private OncRpcSvc svc;
    private OncRpcClient clnt;
    private RpcCall clntCall;

    @Before
    public void setUp() throws IOException {

        RpcDispatchable echo = (RpcCall call) -> {
            switch (call.getProcedure()) {

                case ECHO: {
                    XdrString s = new XdrString();
                    call.retrieveCall(s);
                    call.reply(s);
                    break;
                }
                case UPPER: {
                    RpcCall cb = new RpcCall(PROGNUM, PROGVER, new RpcAuthTypeNone(), call.getTransport());
                    XdrString s = new XdrString();
                    call.retrieveCall(s);
                    cb.call(ECHO, s, s);
                    call.reply(s);
                    break;
                }
                case SHUTDOWN: {
                    svc.stop();
                    break;
                }
                case LOST: {
                    // no reply
                    break;
                }
            }
        };

        RpcDispatchable upper = (RpcCall call) -> {
            XdrString s = new XdrString();
            call.retrieveCall(s);
            XdrString u = new XdrString(s.stringValue().toUpperCase());
            call.reply(u);
        };

        svc = new OncRpcSvcBuilder()
                .withoutAutoPublish()
                .withTCP()
                .withWorkerThreadIoStrategy()
		.withBindAddress("127.0.0.1")
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), echo)
                .withServiceName("svc")
                .build();
        svc.start();

        clnt = OncRpcClient.newBuilder()
                .withTCP()
                .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), upper)
                .withServiceName("clnt")
                .build(svc.getInetSocketAddress(IpProtocolType.TCP));

        RpcTransport t = clnt.connect();
        clntCall = new RpcCall(PROGNUM, PROGVER, new RpcAuthTypeNone(), t);
    }

    @After
    public void tearDown() throws IOException {
        if (svc != null) {
            svc.stop();
        }
        if (clnt != null) {
            clnt.close();
        }
    }

    @Test
    public void shouldCallCorrectProcedure() throws IOException {
        XdrString s = new XdrString("hello");
        XdrString reply = new XdrString();
        clntCall.call(ECHO, s, reply);

        assertEquals("reply mismatch", s, reply);
    }

    @Test
    public void shouldTriggerClientCallback() throws IOException {
        XdrString s = new XdrString("hello");
        XdrString reply = new XdrString();

        clntCall.call(UPPER, s, reply);

        assertEquals("reply mismatch", s.stringValue().toUpperCase(), reply.stringValue());
    }

    @Test(expected = EOFException.class, timeout = 5000)
    public void shouldFailClientCallWhenServerStopped() throws Throwable {
        XdrString s = new XdrString("hello");

        try {
            // stop the server
            clntCall.call(SHUTDOWN, s, XdrVoid.XDR_VOID);
        } catch (EOFException e) {
            // ignore disconnect error
        }

        AtomicReference<Throwable> t = new AtomicReference<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);

        CompletionHandler<RpcReply, RpcTransport> callback = new CompletionHandler<RpcReply, RpcTransport>() {
            @Override
            public void completed(RpcReply result, RpcTransport attachment) {
                countDownLatch.countDown();
            }

            @Override
            public void failed(Throwable exc, RpcTransport attachment) {
                t.set(exc);
                countDownLatch.countDown();
            }
        };

        clntCall.call(ECHO, s, callback);
        countDownLatch.await();
        throw t.get();
    }

    @Test(expected = EOFException.class, timeout = 5000)
    public void shouldFailClientCallWhileWaitingWhenServerStopped() throws IOException, InterruptedException {
        XdrString s = new XdrString("hello");

        clntCall.call(SHUTDOWN, s, s);
    }

    @Test
    public void shouldTriggerClientCallbackEvenIfOtherClientDisconnected() throws IOException {

        OncRpcSvc clnt2 = new OncRpcSvcBuilder()
                .withTCP()
                .withClientMode()
                .withWorkerThreadIoStrategy()
                .build();
        clnt2.start();
        clnt2.connect(svc.getInetSocketAddress(IpProtocolType.TCP));
        clnt2.stop();

        XdrString s = new XdrString("hello");
        XdrString reply = new XdrString();

        clntCall.call(UPPER, s, reply);

        assertEquals("reply mismatch", s.stringValue().toUpperCase(), reply.stringValue());
    }

    @Test
    public void shouldRemoveRequestFromPendingQueueOnReply() throws IOException, InterruptedException, ExecutionException {

        XdrString s = new XdrString("hello");
        try {
            clntCall.call(LOST, s, XdrString.class).get(100, TimeUnit.MILLISECONDS);
            fail("clntCall.call unexpectedly succeeded");
        } catch (TimeoutException expected) {
        }

        assertTrue("pending queue is not empty",
                clntCall.getTransport().getReplyQueue().getPendingRequests().isEmpty());
    }

}
