/*
 * Copyright (c) 2009 - 2012 Deutsches Elektronen-Synchroton,
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
package org.dcache.xdr;


import java.io.IOException;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.asyncqueue.PushBackContext;
import org.glassfish.grizzly.asyncqueue.PushBackHandler;
import org.glassfish.grizzly.asyncqueue.WritableMessage;
import org.glassfish.grizzly.filterchain.FilterChainContext;

public class GrizzlyXdrTransport implements XdrTransport {

    private final FilterChainContext _context;
    private final Connection<InetSocketAddress> _connection;
    private final ReplyQueue<Integer, RpcReply> _replyQueue;

    private static final PushBackHandler RERTY_ON_PUSHBACK = new RetryPushBackHandler();
    private final static Logger _log = LoggerFactory.getLogger(GrizzlyXdrTransport.class);

    public GrizzlyXdrTransport(FilterChainContext context, ReplyQueue<Integer, RpcReply> replyQueue) {
        _context = context;
        _connection = context.getConnection();
        _replyQueue = replyQueue;
    }

    public GrizzlyXdrTransport(FilterChainContext context) {
        this(context, null);
    }

    @Override
    public void send(Xdr xdr) throws IOException {
        Buffer buffer = xdr.asBuffer();
        buffer.allowBufferDispose(true);

        // pass destination address to handle UDP connections as well
        _context.write(_context.getAddress(), buffer, null, RERTY_ON_PUSHBACK);
    }

    @Override
    public InetSocketAddress getLocalSocketAddress() {
        return (InetSocketAddress)_context.getConnection().getLocalAddress();
    }

    @Override
    public InetSocketAddress getRemoteSocketAddress() {
        return (InetSocketAddress)_context.getAddress();
    }

    @Override
    public ReplyQueue<Integer, RpcReply> getReplyQueue() {
        return _replyQueue;
    }

    @Override
    public XdrTransport getPeerTransport() {
        return new ClientTransport(_connection, getReplyQueue());
    }

    @Override
    public String toString() {
        return getRemoteSocketAddress() + " <=> " + getLocalSocketAddress();
    }

    /**
     * An implementation of {@link PushBackHandler}, which will enforce Grizzly's
     * {@link org.glassfish.grizzly.Writer} to retry when possible if
     * message can not be neither written nor added to write queue due
     * to I/O or memory limitations.
     */
    private static class RetryPushBackHandler implements PushBackHandler {

        @Override
        public void onAccept(Connection connection, WritableMessage message) {
            // NOP
        }

        @Override
        public void onPushBack(Connection connection, WritableMessage message, PushBackContext pushBackContext) {
            pushBackContext.retryWhenPossible();
        }
    }
}
