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

import javax.security.auth.Subject;
import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import static java.util.Objects.requireNonNull;

public class RpcDispatcher extends BaseFilter {

    private final static Logger _log = LoggerFactory.getLogger(RpcDispatcher.class);
    /**
     * List of registered RPC services
     *
     */
    private final Map<OncRpcProgram, RpcDispatchable> _programs;

    /**
     * {@link ExecutorService} used for request processing
     */
    private final ExecutorService _asyncExecutorService;

    /**
     * If {@code true}, then request will be performed as {@link Subject} created
     * from request credentials.
     */
    private final boolean _withSubjectPropagation;
    /**
     * Create new RPC dispatcher for given program.
     *
     * @param executor {@link ExecutorService} to use for request processing
     * @param programs {@link Map}
     *     with a mapping between program number and program
     *     handler.
     * @param withSubjectPropagation use {@link Subject#doAs} to exacerbate request.
     *
     * @throws NullPointerException if executor or program is null
     */
    public RpcDispatcher(ExecutorService executor, Map<OncRpcProgram,
            RpcDispatchable> programs, boolean withSubjectPropagation)
            throws NullPointerException {

        _programs = requireNonNull(programs, "Programs is NULL");
        _asyncExecutorService = requireNonNull(executor, "ExecutorService is NULL");
        _withSubjectPropagation = withSubjectPropagation;
    }

    @Override
    public NextAction handleRead(final FilterChainContext ctx) throws IOException {

        final RpcCall call = ctx.getMessage();
        final int prog = call.getProgram();
        final int vers = call.getProgramVersion();
        final int proc = call.getProcedure();

        _log.debug("processing request {}", call);

        final RpcDispatchable program = _programs.get(new OncRpcProgram(prog, vers));
        if (program == null) {
            call.failProgramUnavailable();
        } else {
            _asyncExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (_withSubjectPropagation) {
                            Subject subject = call.getCredential().getSubject();

                            try {
                                Subject.doAs(subject, (PrivilegedExceptionAction<Void>) () -> {
                                    program.dispatchOncRpcCall(call);
                                    return null;
                                });
                            } catch (PrivilegedActionException e) {
                                Throwable t = e.getCause();
                                Throwables.throwIfInstanceOf(t, IOException.class);
                                Throwables.throwIfUnchecked(t);
                                throw new RuntimeException("Unexpected exception", e);
                            }
                        } else {
                            program.dispatchOncRpcCall(call);
                        }
                    } catch (RpcException e) {
                        call.reject(e.getStatus(), e.getRpcReply());
                        _log.warn("Failed to process RPC request: {}", e.getMessage());
                    } catch (OncRpcException e) {
                        call.failRpcGarbage();
                        _log.warn("Failed to process RPC request: {}", e.getMessage());
                    } catch (IOException e) {
                        call.failRpcGarbage();
                        _log.warn("Failed to process RPC request: {}", e.getMessage());
                    } catch (RuntimeException e) {
                        /*
                         * This looks like a bug in dispatcher implementation.
                         * Log the error and tell client that we fail.
                         */
                        _log.error("Failed to process RPC request:", e);
                        call.failRpcSystem();
                        throw e;
                    }
                }

                @Override
                public String toString() {
                    return call.toString();
                }
            });
        }
        return ctx.getInvokeAction();
    }
}
