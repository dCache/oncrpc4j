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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class ReplyQueue {

    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "timeout thread #" + counter.incrementAndGet() + " for ReplyQueue " + ReplyQueue.this);
            t.setDaemon(true);
            return t;
        }
    });
    private final ConcurrentMap<Integer, HandlerTimeoutPair> _queue = new ConcurrentHashMap<>();
    private volatile boolean _isConnected = true;

    public void assertConnected() throws EOFException {
        if (!_isConnected) {
            throw new EOFException("Disconnected");
        }
    }

    /**
     * Creates a placeholder for the specified key, and no timeout
     *
     * @param key key (xid)
     * @param callback
     * @throws EOFException if disconnected
     */
    public void registerKey(final int key, CompletionHandler<RpcReply, XdrTransport> callback) throws EOFException {
        registerKey(key, callback, 0, null);
    }

    /**
     * Create a placeholder for specified key, with the specified timeout to get a response by)
     *
     * @param key key (xid)
     * @param callback
     * @throws EOFException if disconnected
     */
    public void registerKey(final int key, CompletionHandler<RpcReply, XdrTransport> callback, final long timeoutValue, final TimeUnit timeoutUnits) throws EOFException {
        if (!_isConnected) {
            throw new EOFException("Disconnected");
        }
        ScheduledFuture<?> scheduledTimeout = null;
        if (timeoutValue > 0 && timeoutUnits != null) {
            scheduledTimeout = executorService.schedule(new Runnable() {
                @Override
                public void run() {
                    CompletionHandler<RpcReply, XdrTransport> handler = get(key);
                    if (handler != null) { //means we're 1st, no response yet
                        handler.failed(new TimeoutException("did not get a response within " + timeoutValue + " " + timeoutUnits), null);
                    }
                }
            }, timeoutValue, timeoutUnits);
        }
        _queue.put(key, new HandlerTimeoutPair(callback, scheduledTimeout));
    }

    public synchronized void handleDisconnect() {
        _isConnected = false;
        EOFException eofException = new EOFException("Disconnected");
        for (HandlerTimeoutPair handler : _queue.values()) {
            handler.handler.failed(eofException, null);
        }
        for (HandlerTimeoutPair pair : _queue.values()) {
            ScheduledFuture<?> timeoutFuture = pair.scheduledTimeout;
            if (timeoutFuture != null) {
                timeoutFuture.cancel(false);
            }
        }
        _queue.clear();
        executorService.shutdown();
    }

    /**
     * Get value for defined key. The call will block if value is not available yet.
     * On completion key will be unregistered.
     *
     * @param key key (xid)
     */
    public CompletionHandler<RpcReply, XdrTransport> get(int key) {
        HandlerTimeoutPair pair = _queue.remove(key);
        if (pair != null) { //means we're first. call off any pending timeouts
            if (pair.scheduledTimeout != null) {
                pair.scheduledTimeout.cancel(false);
            }
            return pair.handler;
        } else {
            return null;
        }
    }

    private static class HandlerTimeoutPair {
        private final CompletionHandler<RpcReply, XdrTransport> handler;
        private final ScheduledFuture<?> scheduledTimeout;

        public HandlerTimeoutPair(CompletionHandler<RpcReply, XdrTransport> handler, ScheduledFuture<?> scheduledTimeout) {
            this.handler = handler;
            this.scheduledTimeout = scheduledTimeout;
        }
    }
}
