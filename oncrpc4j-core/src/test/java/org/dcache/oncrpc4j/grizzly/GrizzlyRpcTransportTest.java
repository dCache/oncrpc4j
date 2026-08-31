/*
 * Copyright (c) 2026 Deutsches Elektronen-Synchroton,
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
package org.dcache.oncrpc4j.grizzly;

import java.io.EOFException;
import java.net.InetSocketAddress;
import java.nio.channels.CompletionHandler;
import org.dcache.oncrpc4j.rpc.ReplyQueue;
import org.dcache.oncrpc4j.xdr.Xdr;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GrizzlyRpcTransportTest {

    @Test
    public void send_fullAsyncWriteQueue_failsAndTerminatesConnection() {
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
        Xdr xdr = new Xdr(16);
        xdr.beginEncoding();
        xdr.xdrEncodeInt(42);
        xdr.endEncoding();

        transport.send(xdr, remoteAddress, handler);

        verify(connection, never()).write(eq(remoteAddress), any(Buffer.class),
                any(org.glassfish.grizzly.CompletionHandler.class));
        verify(handler).failed(any(EOFException.class), eq(remoteAddress));
        verify(connection).terminateWithReason(any());
        replyQueue.shutdown();
    }
}
