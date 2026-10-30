/*
 * Copyright (c) 2009 - 2026 Deutsches Elektronen-Synchroton,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program (see the file COPYING.LIB for more
 * details); if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.dcache.oncrpc4j.rpc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.Buffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.dcache.oncrpc4j.grizzly.GrizzlyRpcTransport;
import org.dcache.oncrpc4j.xdr.Xdr;
import org.dcache.oncrpc4j.xdr.XdrInt;
import org.dcache.oncrpc4j.xdr.XdrVoid;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RpcCallTest {

    private Xdr _xdr = new Xdr(1024);
    private RpcCall _call = new RpcCall(0, _xdr, null);

    @Test(expected=RpcMismatchReply.class)
    public void testBadRpcVersion() throws Exception {
        _xdr.beginEncoding();

        _xdr.xdrEncodeInt(3); // rpc version
        _xdr.endEncoding();

        _xdr.beginDecoding();
        _call.accept();

    }

    @Test(expected=RpcAuthException.class)
    public void testBadAuthType() throws Exception {
        _xdr.beginEncoding();

        _xdr.xdrEncodeInt(2); // rpc version
        _xdr.xdrEncodeInt(127); // prog
        _xdr.xdrEncodeInt(1); // vers
        _xdr.xdrEncodeInt(0); // proc

        _xdr.xdrEncodeInt(100);
        _xdr.endEncoding();

        _xdr.beginDecoding();
        _call.accept();
    }

    @Test
    public void call_timeoutBeforeWriteFailure_notifiesCallbackOnce() throws IOException {
        ReplyQueue replyQueue = new ReplyQueue();
        RpcTransport transport = mock(RpcTransport.class);
        InetSocketAddress address = new InetSocketAddress("localhost", 2049);
        CompletionHandler<RpcReply, RpcTransport> callback = mock(CompletionHandler.class);
        AtomicReference<CompletionHandler<Integer, InetSocketAddress>> writeHandler =
                new AtomicReference<>();

        when(transport.getReplyQueue()).thenReturn(replyQueue);
        when(transport.getLocalSocketAddress()).thenReturn(address);
        when(transport.getRemoteSocketAddress()).thenReturn(address);
        doAnswer(invocation -> {
            writeHandler.set(invocation.getArgument(2));
            return null;
        }).when(transport).send(any(Xdr.class), eq(address), any(CompletionHandler.class));

        RpcCall call = new RpcCall(1, 1, new RpcAuthTypeNone(), transport);
        call.call(1, XdrVoid.XDR_VOID, callback, 100, TimeUnit.MILLISECONDS);

        verify(callback, timeout(1000)).failed(any(), any());
        writeHandler.get().failed(new IOException("write failed"), address);
        verify(callback, times(1)).failed(any(), any());

        replyQueue.shutdown();
    }


    @Test
    public void send_fullAsyncWriteQueue_failsAndTerminatesConnection()
          throws IOException, ExecutionException, InterruptedException {
        Connection<InetSocketAddress> connection = mock(Connection.class);
        TCPNIOTransport tcpTransport = TCPNIOTransportBuilder.newInstance().build();
        InetSocketAddress localAddress = new InetSocketAddress("localhost", 2049);
        InetSocketAddress remoteAddress = new InetSocketAddress("localhost", 2050);
        CompletionHandler<Integer, InetSocketAddress> handler = mock(CompletionHandler.class);

        when(connection.getTransport()).thenReturn(tcpTransport);
        when(connection.getLocalAddress()).thenReturn(localAddress);
        when(connection.getPeerAddress()).thenReturn(remoteAddress);
        when(connection.getMemoryManager()).thenReturn(MemoryManager.DEFAULT_MEMORY_MANAGER);
        when(connection.canWrite()).thenReturn(false);
        when(connection.getMaxAsyncWriteQueueSize()).thenReturn(1024);

        ReplyQueue replyQueue = new ReplyQueue();
        GrizzlyRpcTransport transport = new GrizzlyRpcTransport(connection, replyQueue);

        RpcCall call = new RpcCall(1, 1, new RpcAuthTypeNone(), transport);
        var future = call.call(1, new XdrInt(42), XdrVoid.class);

        try {
            future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {

            SocketTimeoutException cause = (SocketTimeoutException) e.getCause();

            handler.failed(cause, remoteAddress);
            transport.closeExceptionally(cause);
        }

        verify(connection, never()).write(eq(remoteAddress), any(Buffer.class),
              any(org.glassfish.grizzly.CompletionHandler.class));
        verify(handler).failed(any(SocketTimeoutException.class), eq(remoteAddress));
        verify(connection).terminateWithReason(any());
    }

}
