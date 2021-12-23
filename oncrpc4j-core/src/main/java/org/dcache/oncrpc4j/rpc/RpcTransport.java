/*
 * Copyright (c) 2009 - 2019 Deutsches Elektronen-Synchroton,
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

import com.google.common.annotations.Beta;
import java.net.InetSocketAddress;
import java.nio.channels.CompletionHandler;
import org.dcache.oncrpc4j.xdr.Xdr;

/**
 *
 * Abstraction for sending reply to clients
 *
 */
public interface RpcTransport {

    /**
     * Send data to remote end point. The handler parameter is a completion
     * handler that is invoked when the send operation completes (or fails). The
     * result passed to the completion handler is the number of bytes sent.
     *
     * @param <A> the type of the attachment.
     * @param xdr message to send.
     * @param attachment the object to attach to the I/O operation; can be null
     * @param handler the handler for consuming the result.
     */
    <A> void send(Xdr xdr, A attachment,  CompletionHandler<Integer, ? super A> handler);

    ReplyQueue getReplyQueue();

    /**
     * Returns is this transport is open and ready.
     *
     * @return <tt>true</tt>, if connection is open and ready, or <tt>false</tt>
     * otherwise.
     */
    boolean isOpen();

    /**
     * Get local end point.
     *
     * @return InetSocketAddress of local socket end point
     */
    InetSocketAddress getLocalSocketAddress();

    /**
     * Get remote end point.
     *
     * @return InetSocketAddress of remote socket end point.
     */
    InetSocketAddress getRemoteSocketAddress();

    /**
     * Get {@link RpcTransport} for to sent/receive requests in opposite direction.
     * The returned transport can be used by servers to send RPC calls to clients and
     * can be used by clients to receive RPC calls from servers.
     *
     * @return {@code RpcTransport} connected to the remote peer.
     */
    public RpcTransport getPeerTransport();


    /**
     * Enable TLS on this transport.
     * @throws RpcAuthException if handshake can't be initialized (due to missing configuration).
     * @throws IllegalStateException if TLS is already enabled.
     */
    @Beta
    void startTLS() throws RpcAuthException, IllegalStateException;

    /**
     * Check whatever this transport is protected by TLS.
     * @return {@code true} if and only if this transport is connected and protected by TLS.
     */
    @Beta
    boolean isTLS();
}
