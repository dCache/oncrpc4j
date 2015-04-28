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

import java.io.EOFException;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.Map;

public class ReplyQueue {

    private final Map<Integer, CompletionHandler<RpcReply, XdrTransport>> _queue = new HashMap<>();
    private boolean _isConnected = true;

    /**
     * Create a placeholder for specified key.
     * @param key
     */
    public synchronized void registerKey(int key, CompletionHandler<RpcReply, XdrTransport> callback) throws EOFException {
        if (!_isConnected) {
            throw new EOFException("Disconnected");
        }
        _queue.put(key, callback);
    }


    public synchronized void handleDisconnect() {
        _isConnected = false;
        for(CompletionHandler<RpcReply, XdrTransport> handler: _queue.values()) {
            handler.failed(new EOFException("Disconnected") , null);
        }
        _queue.clear();
    }
    /**
     * Get value for defined key. The call will block if value is not available yet.
     * On completion key will be unregistered.
     *
     * @param key
     */
    public synchronized CompletionHandler<RpcReply, XdrTransport> get(int key) {
        return _queue.remove(key);
    }
}
