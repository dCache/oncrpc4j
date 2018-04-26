/*
 * Copyright (c) 2009 - 2018 Deutsches Elektronen-Synchroton,
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


import org.dcache.oncrpc4j.rpc.ReplyQueue;
import org.dcache.oncrpc4j.xdr.Xdr;
import java.net.InetSocketAddress;
import java.nio.channels.CompletionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.asyncqueue.WritableMessage;

import org.dcache.oncrpc4j.rpc.RpcTransport;

import static java.util.Objects.requireNonNull;

public class GrizzlyRpcTransport implements RpcTransport {

    private final Connection<InetSocketAddress> _connection;
    private final ReplyQueue _replyQueue;
    private final InetSocketAddress _localAddress;
    private final InetSocketAddress _remoteAddress;

    private final static Logger _log = LoggerFactory.getLogger(GrizzlyRpcTransport.class);

    public GrizzlyRpcTransport(Connection<InetSocketAddress> connection, ReplyQueue replyQueue) {
        this(connection, connection.getPeerAddress(), replyQueue);
    }

    public GrizzlyRpcTransport(Connection<InetSocketAddress> connection, InetSocketAddress remoteAddress, ReplyQueue replyQueue) {
        _connection = connection;
        _replyQueue = replyQueue;
        _localAddress = _connection.getLocalAddress();
        _remoteAddress = remoteAddress;
    }

    @Override
    public boolean isOpen() {
        return _connection.isOpen();
    }

    @Override
    public <A> void send(final Xdr xdr, A attachment, CompletionHandler<Integer, ? super A> handler) {
        final Buffer buffer = xdr.asBuffer();
        buffer.allowBufferDispose(true);

        requireNonNull(handler, "CompletionHandler can't be null");

        // pass destination address to handle UDP connections as well
        _connection.write(_remoteAddress, buffer,
                new EmptyCompletionHandler<WriteResult<WritableMessage, InetSocketAddress>>() {

                    @Override
                    public void failed(Throwable throwable) {
                        handler.failed(throwable, attachment);
                    }

                    @Override
                    public void completed(WriteResult<WritableMessage, InetSocketAddress> result) {
                        handler.completed((int)result.getWrittenSize(), attachment);
                    }
        });
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
}
