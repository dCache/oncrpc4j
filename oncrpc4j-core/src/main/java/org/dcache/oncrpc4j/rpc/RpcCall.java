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
package org.dcache.oncrpc4j.rpc;

import org.dcache.oncrpc4j.xdr.Xdr;
import org.dcache.oncrpc4j.xdr.XdrVoid;
import org.dcache.oncrpc4j.xdr.XdrEncodingStream;
import org.dcache.oncrpc4j.xdr.XdrAble;
import com.google.common.base.Throwables;
import java.io.EOFException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.channels.CompletionHandler;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public class RpcCall {

    private final static Logger _log = LoggerFactory.getLogger(RpcCall.class);

    private final static Random RND = new Random();

    /**
     * XID number generator
     */
    private final AtomicInteger xidGenerator = new AtomicInteger(RND.nextInt());

    private int _xid;

    /**
     * Supported RPC protocol version
     */
    private final static int RPCVERS = 2;

    /**
     * RPC program number
     */
    private int _prog;

    /**
     * RPC program version number
     */
    private int _version;

    /**
     * RPC program procedure number
     */
    private int _proc;

    /**
     *  RPC protocol version number
     */
    private int _rpcvers;

    /**
     * Authentication credential.
     */
    private RpcAuth _cred;

    /**
     * RPC call transport.
     */
    private final RpcTransport _transport;

    /**
     * Call body.
     */
    private final Xdr _xdr;

    /**
     * Object used to synchronize access to sendListeners.
     */
    private final Object _listenerLock = new Object();

    /**
     * The {link CompletionHandler} which is used to notify all registered
     * completion listeners.
     */
    private class NotifyListenersCompletionHandler implements CompletionHandler<Integer, InetSocketAddress> {

        @Override
        public void completed(Integer result, InetSocketAddress attachment) {
            synchronized (_listenerLock) {
                if (_sendListeners != null) {
                    _sendListeners
                            .parallelStream()
                            .forEach(l -> l.completed(result, attachment));
                }

                if (_sendOnceListeners != null) {
                    _sendOnceListeners
                            .parallelStream()
                            .forEach(l -> l.completed(result, attachment));
                    _sendOnceListeners = null;
                }
            }
        }

        @Override
        public void failed(Throwable t, InetSocketAddress attachment) {
            _log.error("Failed to send RPC to {} : {}", attachment, t.getMessage());
            synchronized (_listenerLock) {
                if (_sendListeners != null) {
                    _sendListeners
                            .parallelStream()
                            .forEach(l -> l.failed(t, attachment));
                }

                if (_sendOnceListeners != null) {
                    _sendOnceListeners
                            .parallelStream()
                            .forEach(l -> l.failed(t, attachment));
                    _sendOnceListeners = null;
                }
            }
        }
    }

    /**
     * A {@link List} of registered {@link CompletionHandler} to be notified when
     * send request complete.
     */
    private List<CompletionHandler<Integer, InetSocketAddress>> _sendListeners;

    /**
     * A {@link List} of registered {@link CompletionHandler} to be notified
     * when send request complete. The listeners will be removed from the list
     * after notification.
     */
    private List<CompletionHandler<Integer, InetSocketAddress>> _sendOnceListeners;

    private final CompletionHandler<Integer, InetSocketAddress> _sendNotificationHandler
            = new NotifyListenersCompletionHandler();

    public RpcCall(int prog, int ver, RpcAuth cred, RpcTransport transport) {
        this(prog, ver, cred, new Xdr(Xdr.INITIAL_XDR_SIZE), transport);
    }

    public RpcCall(int prog, int ver, RpcAuth cred, Xdr xdr, RpcTransport transport) {
        _prog = prog;
        _version = ver;
        _cred = cred;
        _transport = transport;
        _xdr = xdr;
        _proc = 0;
    }

    public RpcCall(int xid, Xdr xdr, RpcTransport transport) {
        _xid = xid;
        _xdr = xdr;
        _transport = transport;
    }

    public RpcCall(int xid, int prog, int ver, int proc, RpcAuth cred, Xdr xdr, RpcTransport transport) {
        _xid = xid;
        _prog = prog;
        _version = ver;
        _proc = proc;
        _cred = cred;
        _xdr = xdr;
        _transport = transport;
        _rpcvers = RPCVERS;
    }

    /**
     * Accept message. Have to be called prior processing RPC call.
     * @throws IOException
     * @throws OncRpcException
     */
    public void accept() throws IOException, OncRpcException {
         _rpcvers = _xdr.xdrDecodeInt();
         if (_rpcvers != RPCVERS) {
            throw new RpcMismatchReply(_rpcvers, 2);
         }

        _prog = _xdr.xdrDecodeInt();
        _version = _xdr.xdrDecodeInt();
        _proc = _xdr.xdrDecodeInt();
        _cred = RpcCredential.decode(_xdr);
     }

    /**
     * Get RPC call program number.
     *
     * @return version number
     */
    public int getProgram() {
        return _prog;
    }

    /**
     * @return the RPC call program version
     */
    public int getProgramVersion() {
        return _version;
    }

    /**
     * @return the RPC call program procedure
     */
    public int getProcedure() {
        return _proc;
    }

    public RpcAuth getCredential() {
        return _cred;
    }

    /**
     * Get RPC {@XdrTransport} used by this call.
     * @return transport
     */
    public RpcTransport getTransport() {
        return _transport;
    }

    /**
     * Get xid associated with this rpc message.
     */
    public int getXid() {
        return _xid;
    }

    /**
     * Get {@link Xdr} stream used by this message.
     * @return xdr stream
     */
    public Xdr getXdr() {
        return _xdr;
    }

    @Override
    public String toString() {
        return String.format("RPCv%d call: program=%d, version=%d, procedure=%d",
                _rpcvers, _prog, _version, _proc);
    }

    /**
     * Reject the request with given status. The call can be rejected for two
     * reasons: either the server is not running a compatible version of the
     * RPC protocol (RPC_MISMATCH), or the server rejects the identity of the
     * caller (AUTH_ERROR).
     *
     * @see RpcRejectStatus
     * @param status
     * @param reason
     */
    public void reject(int status, XdrAble reason) {
        XdrEncodingStream xdr = _xdr;
        try {
            RpcMessage replyMessage = new RpcMessage(_xid, RpcMessageType.REPLY);
            xdr.beginEncoding();
            replyMessage.xdrEncode(_xdr);
            xdr.xdrEncodeInt(RpcReplyStatus.MSG_DENIED);
            xdr.xdrEncodeInt(status);
            reason.xdrEncode(_xdr);
            xdr.endEncoding();

            _transport.send((Xdr)xdr, _transport.getRemoteSocketAddress(), _sendNotificationHandler);

        } catch (OncRpcException e) {
            _log.warn("Xdr exception: ", e);
        } catch (IOException e) {
            _log.error("Failed send reply: ", e);
        }
    }
    /**
     * Send accepted reply to the client.
     *
     * @param reply
     */
    public void reply(XdrAble reply) {
        acceptedReply(RpcAccepsStatus.SUCCESS, reply);
    }

    public void acceptedReply(int state, XdrAble reply) {

        XdrEncodingStream xdr = _xdr;
        try {
            RpcMessage replyMessage = new RpcMessage(_xid, RpcMessageType.REPLY);
            xdr.beginEncoding();
            replyMessage.xdrEncode(_xdr);
            xdr.xdrEncodeInt(RpcReplyStatus.MSG_ACCEPTED);
            _cred.getVerifier().xdrEncode(xdr);
            xdr.xdrEncodeInt(state);
            reply.xdrEncode(xdr);
            xdr.endEncoding();

            _transport.send((Xdr)xdr, _transport.getRemoteSocketAddress(), _sendNotificationHandler);

        } catch (OncRpcException e) {
            _log.warn("Xdr exception: ", e);
        } catch (IOException e) {
            _log.error("Failed send reply: ", e);
        }
    }

    /**
     * Retrieves the parameters sent within an ONC/RPC call message.
     *
     * @param args the call argument do decode
     * @throws OncRpcException
     */
    public void retrieveCall(XdrAble args) throws OncRpcException, IOException {
        args.xdrDecode(_xdr);
        _xdr.endDecoding();
    }

    /**
     * Reply to client with error program version mismatch.
     * Accepted message sent.
     *
     * @param min minimal supported version
     * @param max maximal supported version
     */
    public void failProgramMismatch(int min, int max) {
        acceptedReply(RpcAccepsStatus.PROG_MISMATCH, new MismatchInfo(min, max));
    }

    /**
     * Reply to client with error program unavailable.
     * Accepted message sent.
     */
    public void failProgramUnavailable() {
        acceptedReply(RpcAccepsStatus.PROG_UNAVAIL, XdrVoid.XDR_VOID);
    }

    /**
     * Reply to client with error procedure unavailable.
     */
    public void failProcedureUnavailable() {
        acceptedReply(RpcAccepsStatus.PROC_UNAVAIL, XdrVoid.XDR_VOID);
    }

    /**
     * Reply to client with error garbage args.
     */
    public void failRpcGarbage() {
        acceptedReply(RpcAccepsStatus.GARBAGE_ARGS, XdrVoid.XDR_VOID);
    }

    /**
     * Reply to client with error system error.
     */
    public void failRpcSystem() {
        acceptedReply(RpcAccepsStatus.SYSTEM, XdrVoid.XDR_VOID);
    }

    /**
     * Send asynchronous RPC request to a remove server.
     *
     * This method initiates an asynchronous RPC request. The handler parameter
     * is a completion handler that is invoked when the RPC operation completes
     * (or fails/times-out). The result passed to the completion handler is the
     * RPC result returned by server.
     *
     * @param procedure The number of the procedure.
     * @param args The argument of the procedure.
     * @param callback The completion handler.
     * @param timeoutValue timeout value. 0 means no timeout
     * @param timeoutUnits units for timeout value
     * @param auth auth to use for the call
     * @throws OncRpcException
     * @throws IOException
     * @since 2.4.0
     */
    public void call(int procedure, XdrAble args, CompletionHandler<RpcReply, RpcTransport> callback, long timeoutValue, TimeUnit timeoutUnits, RpcAuth auth)
            throws IOException {
        callInternal(procedure, args, callback, timeoutValue, timeoutUnits, auth);
    }

    /**
     * convenience version of {@link #call(int, XdrAble, CompletionHandler, long, TimeUnit, RpcAuth)} with no auth
     */
    public void call(int procedure, XdrAble args, CompletionHandler<RpcReply, RpcTransport> callback, long timeoutValue, TimeUnit timeoutUnits)
            throws IOException {
        call(procedure, args, callback, timeoutValue, timeoutUnits, null);
    }

    /**
     * convenience version of {@link #call(int, XdrAble, CompletionHandler, long, TimeUnit, RpcAuth)} with no timeout
     */
    public void call(int procedure, XdrAble args, CompletionHandler<RpcReply, RpcTransport> callback, RpcAuth auth)
            throws IOException {
        call(procedure, args, callback, 0, null, auth);
    }

    /**
     * convenience version of {@link #call(int, XdrAble, CompletionHandler, long, TimeUnit, RpcAuth)} with no timeout or auth
     */
    public void call(int procedure, XdrAble args, CompletionHandler<RpcReply, RpcTransport> callback)
            throws IOException {
        call(procedure, args, callback, 0, null, null);
    }

    /**
     * executes an RPC. returns the (internally generated) xid for the call
     * @param procedure The number of the procedure.
     * @param args The argument of the procedure.
     * @param callback The completion handler.
     * @param timeoutValue timeout value. 0 means no timeout
     * @param timeoutUnits units for timeout value
     * @param auth auth to use for this call. null for constructor-provided default
     * @return the xid for the call
     * @throws OncRpcException
     * @throws IOException
     */
    private int callInternal(int procedure, XdrAble args, CompletionHandler<RpcReply, RpcTransport> callback,
                             long timeoutValue, TimeUnit timeoutUnits, RpcAuth auth)
            throws IOException {

        int xid = nextXid();

        Xdr xdr = new Xdr(Xdr.INITIAL_XDR_SIZE);
        xdr.beginEncoding();
        RpcMessage rpcMessage = new RpcMessage(xid, RpcMessageType.CALL);
        rpcMessage.xdrEncode(xdr);
        xdr.xdrEncodeInt(RPCVERS);
        xdr.xdrEncodeInt(_prog);
        xdr.xdrEncodeInt(_version);
        xdr.xdrEncodeInt(procedure);
        if (auth != null) {
            auth.xdrEncode(xdr);
        } else {
            _cred.xdrEncode(xdr);
        }
        args.xdrEncode(xdr);
        xdr.endEncoding();

        ReplyQueue replyQueue = _transport.getReplyQueue();
        if (callback != null) {
            replyQueue.registerKey(xid, _transport.getLocalSocketAddress(), callback, timeoutValue, timeoutUnits);
        } else {
            //no handler, so we wont get any errors if connection was dropped. have to check.
            if (!_transport.isOpen()) {
                throw new EOFException("XdrTransport is not open");
            }
        }

        _transport.send(xdr, _transport.getRemoteSocketAddress(), new NotifyListenersCompletionHandler() {

            @Override
            public void failed(Throwable t, InetSocketAddress attachment) {
                super.failed(t, attachment);
                if (callback != null) {
                    replyQueue.get(xid);
                    callback.failed(t, _transport);
                }
            }
        });
        return xid;
    }

    /**
     * Send asynchronous RPC request to a remove server.
     *
     * This method initiates an asynchronous RPC request. The method behaves in
     * exactly the same manner as the {@link #call(int, XdrAble, CompletionHandler, long, TimeUnit)}
     * method except that instead of specifying a completion handler, this method
     * returns a Future representing the pending result. The Future's get method
     * returns the RPC reply responded by server.
     *
     * @param <T> The result type of RPC call.
     * @param procedure The number of the procedure.
     * @param args The argument of the procedure.
     * @param type The expected type of the reply
     * @param auth auth to use for the call
     * @return A CompletableFuture representing the result of the operation.
     * @throws OncRpcException
     * @throws IOException
     * @since 2.4.0
     */
    public <T extends XdrAble> CompletableFuture<T> call(int procedure, XdrAble args, final Class<T> type, final RpcAuth auth)
            throws IOException {
        try {
            T result = type.getDeclaredConstructor().newInstance();
            return getCallFuture(procedure, args, result, 0, null, auth);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            // this exceptions point to bugs
            throw new RuntimeException("Failed to create in instance of " + type, e);
        }
    }

    /**
     */

    /**
     * Send asynchronous RPC request to a remove server.
     *
     * This method initiates an asynchronous RPC request. The method behaves in
     * exactly the same manner as the {@link #call(int, XdrAble, CompletionHandler, long, TimeUnit)}
     * method except that instead of specifying a completion handler, this method
     * returns a CompletableFuture representing the pending result. The Future's get method
     * returns the RPC reply responded by server.
     *
     * Convenience version of {@link #call(int, XdrAble, Class, RpcAuth)} with no auth
     *
     * @param <T> The result type of RPC call.
     * @param procedure The number of the procedure.
     * @param args The argument of the procedure.
     * @param type The expected type of the reply
     * @return A CompletableFuture representing the result of the operation.
     * @throws OncRpcException
     * @throws IOException
     * @since 2.4.0
     */
    public <T extends XdrAble> CompletableFuture<T> call(int procedure, XdrAble args, final Class<T> type)
            throws IOException {
        return call(procedure, args, type, null);
    }

    /**
     * Send call to remove RPC server.
     *
     * @param procedure the number of the procedure.
     * @param args the argument of the procedure.
     * @param result the result of the procedure
     * @param timeoutValue timeout value. 0 means no timeout
     * @param timeoutUnits units for timeout value
     * @param auth auth to use for the call
     * @throws OncRpcException
     * @throws IOException
     */
    public void call(int procedure, XdrAble args, XdrAble result, long timeoutValue, TimeUnit timeoutUnits, RpcAuth auth)
            throws IOException, TimeoutException {
        try {
            Future<XdrAble> future = getCallFuture(procedure, args, result, timeoutValue, timeoutUnits, auth);
            future.get();
        } catch (InterruptedException e) {
            // workaround missing chained constructor
            IOException ioe = new InterruptedIOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        } catch (ExecutionException e) {
            Throwable t = Throwables.getRootCause(e);
            Throwables.throwIfInstanceOf(t, OncRpcException.class);
            Throwables.throwIfInstanceOf(t, IOException.class);
            Throwables.throwIfInstanceOf(t, TimeoutException.class);
            throw new IOException(t);
        }
    }

    /**
     * convenience version of {@link #call(int, XdrAble, XdrAble, long, TimeUnit, RpcAuth)} with default auth
     */
    public void call(int procedure, XdrAble args, XdrAble result, long timeoutValue, TimeUnit timeoutUnits)
            throws IOException, TimeoutException {
        call(procedure, args, result, timeoutValue, timeoutUnits, null);
    }

    /**
     * convenience version of {@link #call(int, XdrAble, XdrAble, long, TimeUnit, RpcAuth)} with no timeout
     */
    public void call(int procedure, XdrAble args, XdrAble result, RpcAuth auth)
            throws IOException {
        try {
            call(procedure, args, result, 0, null, auth);
        } catch (TimeoutException e) {
            throw new IllegalStateException(e); //theoretically impossible
        }
    }

    /**
     * convenience version of {@link #call(int, XdrAble, XdrAble, long, TimeUnit, RpcAuth)} with no timeout or auth
     */
    public void call(int procedure, XdrAble args, XdrAble result)
            throws IOException {
        try {
            call(procedure, args, result, 0, null, null);
        } catch (TimeoutException e) {
            throw new IllegalStateException(e); //theoretically impossible
        }
    }

    private <T extends XdrAble> CompletableFuture<T> getCallFuture(int procedure, XdrAble args, final T result, long timeoutValue, TimeUnit timeoutUnits, RpcAuth auth)
            throws IOException {

        final CompletableFuture<T> future = new CompletableFuture<>();
        CompletionHandler<RpcReply, RpcTransport> callback = new CompletionHandler<RpcReply, RpcTransport>() {

            @Override
            public void completed(RpcReply reply, RpcTransport attachment) {
                try {
                    reply.getReplyResult(result);
                    future.complete(result);
                } catch (IOException e) {
                    failed(e, attachment);
                }
            }

            @Override
            public void failed(Throwable exc, RpcTransport attachment) {
                future.completeExceptionally(exc);
            }
        };

        int xid = callInternal(procedure, args, callback, timeoutValue, timeoutUnits, auth);
        //wrap the future if no timeout provided up-front to properly un-register
        //the handler if a timeout is later provided to Future.get()
        return timeoutValue > 0 ? future : new TimeoutAwareFuture<>(future, xid);
    }

    private class TimeoutAwareFuture<T> extends CompletableFuture<T> {
        private final Future<T> delegate;
        private final int xid;

        public TimeoutAwareFuture(Future<T> delegate, int xid) {
            this.delegate = delegate;
            this.xid = xid;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            try {
                return delegate.cancel(mayInterruptIfRunning);
            } finally {
                if (mayInterruptIfRunning) {
                    unregisterXid();
                }
            }
        }

        @Override
        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        @Override
        public boolean isDone() {
            return delegate.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            try {
                return delegate.get();
            } finally {
                unregisterXid();
            }
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            try {
                return delegate.get(timeout, unit);
            } finally {
                unregisterXid();
            }
        }

        private void unregisterXid() {
            _transport.getReplyQueue().get(xid); //make sure its removed from the reply queue
        }
    }


    private int nextXid() {
        return xidGenerator.incrementAndGet();
    }

    /**
     * Register {@link CompletionHandler} to receive notification when message
     * send is complete. NOTICE: when processing rpc call on the server side
     * the @{code registerSendListener} has the same effect as {@link #registerSendOnceListener}
     * as a new instance of {@link RpcCall} is used to process the request.
     * @param listener the message sent listener
     */
    public void registerSendListener(CompletionHandler<Integer, InetSocketAddress> listener) {
        synchronized (_listenerLock) {
            if (_sendListeners == null) {
                _sendListeners = new ArrayList<>();
            }
            _sendListeners.add(listener);
        }
    }

    /**
     * Register {@link CompletionHandler} to receive notification when message
     * send is complete. The listener will be removed after next send event.
     *
     * @param listener the message sent listener
     */
    public void registerSendOnceListener(CompletionHandler<Integer, InetSocketAddress> listener) {
        synchronized (_listenerLock) {
            if (_sendOnceListeners == null) {
                _sendOnceListeners = new ArrayList<>();
            }
            _sendOnceListeners.add(listener);
        }
    }
}
