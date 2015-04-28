/*
 * Copyright (c) 2009 - 2015 Deutsches Elektronen-Synchroton,
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

public class ClientTransport implements XdrTransport {

    private final static Logger _log = LoggerFactory.getLogger(ClientTransport.class);
    private final Connection<InetSocketAddress> _connection;
    private final ReplyQueue _replyQueue;

    public ClientTransport(Connection<InetSocketAddress> connection, ReplyQueue replyQueue) {
        _replyQueue = replyQueue;
        _connection = connection;
    }

    @Override
    public void send(Xdr data) throws IOException {
        Buffer buffer = data.asBuffer();
        _connection.write(buffer);
    }

    @Override
    public InetSocketAddress getLocalSocketAddress() {
        return _connection.getLocalAddress();
    }

    @Override
    public InetSocketAddress getRemoteSocketAddress() {
        return _connection.getPeerAddress();
    }

    public ReplyQueue getReplyQueue() {
        return _replyQueue;
    }

    @Override
    public XdrTransport getPeerTransport() {
        return null;
    }

    @Override
    public String toString() {
        return getRemoteSocketAddress() + " <=> " + getLocalSocketAddress();
    }
}
