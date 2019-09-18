package org.dcache.oncrpc4j.rpc;

import java.io.EOFException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ReplyQueueTest {

    private ReplyQueue replyQueue;
    private SocketAddress addr;
    private CompletionHandler<RpcReply, RpcTransport> handler;

    @Before
    public void setUp() {
        replyQueue = new ReplyQueue();
        addr = mock(InetSocketAddress.class);
        handler = mock(CompletionHandler.class);
    }

    @Test
    public void testRemoveCancel() throws EOFException {

        replyQueue.registerKey(1, addr, handler, 1, TimeUnit.MINUTES);

        assertFalse(replyQueue.getTimeoutQueue().isEmpty());

        replyQueue.get(1);

        assertTrue(replyQueue.getTimeoutQueue().isEmpty());
    }

    @Test
    public void testInvokeHandlerOnTimeout() throws EOFException, InterruptedException {

        replyQueue.registerKey(1, addr, handler, 1, TimeUnit.NANOSECONDS);

        TimeUnit.SECONDS.sleep(1);
        assertTrue(replyQueue.getPendingRequests().isEmpty());
        assertTrue(replyQueue.getTimeoutQueue().isEmpty());
        verify(handler).failed(any(), any());
    }

    @Test
    public void testRequestWithoutOnTimeout() throws EOFException, InterruptedException {

        replyQueue.registerKey(1, addr, handler);
        assertFalse(replyQueue.getPendingRequests().isEmpty());
        assertTrue(replyQueue.getTimeoutQueue().isEmpty());
    }

}
