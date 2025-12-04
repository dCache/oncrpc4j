/*
 * Copyright (c) 2009 - 2025 Deutsches Elektronen-Synchroton,
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
import java.io.IOException;
import java.nio.ByteOrder;
import org.dcache.oncrpc4j.rpc.ReplyQueue;
import org.dcache.oncrpc4j.rpc.RpcMessageParserTCP;
import org.dcache.oncrpc4j.xdr.Xdr;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;

import org.glassfish.grizzly.memory.BuffersBuffer;
import org.glassfish.grizzly.nio.transport.TCPNIOConnection;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.asyncqueue.WritableMessage;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.ssl.SSLFilter;

import org.dcache.oncrpc4j.rpc.RpcAuthError;
import org.dcache.oncrpc4j.rpc.RpcAuthException;
import org.dcache.oncrpc4j.rpc.RpcAuthStat;
import org.dcache.oncrpc4j.rpc.RpcTransport;

import static java.util.Objects.requireNonNull;

public class GrizzlyRpcTransport implements RpcTransport {

    private final Connection<InetSocketAddress> _connection;
    private final ReplyQueue _replyQueue;
    private final InetSocketAddress _localAddress;
    private final InetSocketAddress _remoteAddress;

    /**
     * If true, then underlying transport is stream-oriented (like TCP) and messages must be separated
     * by record marking.
     */
    private final boolean _isStreaming;

    private final static Logger _log = LoggerFactory.getLogger(GrizzlyRpcTransport.class);

    public GrizzlyRpcTransport(Connection<InetSocketAddress> connection, ReplyQueue replyQueue) {
        this(connection, connection.getPeerAddress(), replyQueue);
    }

    public GrizzlyRpcTransport(Connection<InetSocketAddress> connection, InetSocketAddress remoteAddress, ReplyQueue replyQueue) {
        _connection = connection;
        _replyQueue = replyQueue;
        _localAddress = _connection.getLocalAddress();
        _remoteAddress = remoteAddress;
        _isStreaming = connection.getTransport() instanceof TCPNIOTransport;
    }

    @Override
    public boolean isOpen() {
        return _connection.isOpen();
    }

    @Override
    public <A> void send(final Xdr xdr, A attachment, CompletionHandler<Integer, ? super A> handler) {
        if (xdr.hasFileChunk() && _isStreaming && !isTLS()) {
            sendRawTCP(xdr, attachment, handler);
        } else {
            sendDefault(xdr, attachment, handler);
        }
    }

    /**
     * Send XDR message over UDP or over TLS-protected TCP connection.
     */
    private <A> void sendDefault(final Xdr xdr, A attachment, CompletionHandler<Integer, ? super A> handler) {

        requireNonNull(handler, "CompletionHandler can't be null");
        Buffer buffer = xdr.asBuffer();

        // add record marker, if needed
        if (_isStreaming) {
            int len = buffer.remaining() | RpcMessageParserTCP.RPC_LAST_FRAG;
            Buffer marker = _connection.getMemoryManager().allocate(Integer.BYTES);
            marker.order(ByteOrder.BIG_ENDIAN);
            marker.putInt(len);
            marker.flip();
            buffer = BuffersBuffer.create(_connection.getMemoryManager(), marker, buffer);
        }

        _connection.write(_remoteAddress, buffer, new EmptyCompletionHandler<WriteResult<WritableMessage, InetSocketAddress>>() {

            @Override
            public void failed(Throwable throwable) {
                handler.failed(throwable, attachment);
            }

            @Override
            public void completed(WriteResult<WritableMessage, InetSocketAddress> result) {
                handler.completed((int) result.getWrittenSize(), attachment);
            }
        });
    }


    /**
     * Send XDR message over raw TCP connection. This method is optimized for zero-copy use-case.
     */
    private <A> void sendRawTCP(final Xdr xdr, A attachment, CompletionHandler<Integer, ? super A> handler) {

        requireNonNull(handler, "CompletionHandler can't be null");
        WritableMessage[] messages = xdr.asBufferWritableMessages();

        int len = getMessagesSize(messages) | RpcMessageParserTCP.RPC_LAST_FRAG;
        Buffer marker = _connection.getMemoryManager().allocate(Integer.BYTES);
        marker.order(ByteOrder.BIG_ENDIAN);
        marker.putInt(len);
        marker.flip();

        var v = TCPNIOConnection.class.cast(_connection);
        int written = 0;
        try {
            // as XDR should be delivered as a single RPC message, lock the connection until all parts are written
            synchronized (_connection) {
                written += ((TCPNIOTransport) _connection.getTransport()).write(v, marker, null);
                for (WritableMessage msg : messages) {
                    while (msg.hasRemaining()) {
                        written += ((TCPNIOTransport) _connection.getTransport()).write(v, msg, null);
                    }
                    // as we by-passed Grizzly's Buffer management, we need to release buffers manually
                    msg.release();
                }
            }
            handler.completed(written, attachment);
        } catch (IOException e) {
            // convert ClosedChannelException to EOFException as expected by upper layers
            if (e instanceof ClosedChannelException) {
                e = new EOFException();
            }
            handler.failed(e, attachment);
        }
    }

    @Override
    public InetSocketAddress getLocalSocketAddress() {
        return _localAddress;
    }

    @Override
    public InetSocketAddress getRemoteSocketAddress() {
        return _remoteAddress;
    }

    @Override
    public ReplyQueue getReplyQueue() {
        return _replyQueue;
    }

    @Override
    public RpcTransport getPeerTransport() {
        return new GrizzlyRpcTransport(_connection, getReplyQueue());
    }

    @Override
    public String toString() {
        return getRemoteSocketAddress() + " <=> " + getLocalSocketAddress();
    }

    @Override
    public void startTLS() throws RpcAuthException {
        final FilterChain currentChain = (FilterChain) _connection.getProcessor();
        if (currentChain.indexOfType(SSLFilter.class) >= 0) {
            // already enabled
            throw new IllegalStateException("TLS is already enabled.");
        }

        currentChain.stream()
                .filter(StartTlsFilter.class::isInstance)
                .findAny()
                .map(StartTlsFilter.class::cast)
                .orElseThrow(() -> new RpcAuthException("SSL is not configured",
                        new RpcAuthError(RpcAuthStat.AUTH_FAILED)))
                .startTLS(_connection);
    }

    @Override
    public boolean isTLS() {
        return ((FilterChain) _connection.getProcessor()).stream()
                .anyMatch(SSLFilter.class::isInstance);
    }

    /**
     * Calculate the total size of all messages in the array.
     *
     * @param messages array of messages
     * @return total size of all messages
     */
    private int getMessagesSize(WritableMessage[] messages) {
        requireNonNull(messages);
        int size = 0;
        for (WritableMessage message : messages) {
            size += message.remaining();
        }
        return size;
    }
}
