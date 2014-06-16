/*
 * Copyright (c) 2009 - 2014 Deutsches Elektronen-Synchroton,
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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import static com.google.common.base.Preconditions.checkNotNull;

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
     * Create new RPC dispatcher for given program.
     *
     * @param executor {@link ExecutorService} to use for request processing
     * @param programs {@link Map}
     *     with a mapping between program number and program
     *     handler.
     *
     * @throws IllegalArgumentException if executor or program is null
     */
    public RpcDispatcher(ExecutorService executor, Map<OncRpcProgram, RpcDispatchable> programs)
            throws IllegalArgumentException {

        checkNotNull(programs != null, "Programs is NULL");
        checkNotNull(executor != null, "ExecutorService is NULL");

        _asyncExecutorService = executor;
        _programs = programs;
    }

    @Override
    public NextAction handleRead(final FilterChainContext ctx) throws IOException {

        final RpcCall call = ctx.getMessage();
        final int prog = call.getProgram();
        final int vers = call.getProgramVersion();
        final int proc = call.getProcedure();

        _log.debug("processing request {}", call);

        final RpcDispatchable program = _programs.get(new OncRpcProgram(prog, vers));
        _asyncExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                if (program == null) {
                    call.failProgramUnavailable();
                } else {
                    try {
                        program.dispatchOncRpcCall(call);
                    } catch (RpcException e) {
                        call.reject(e.getStatus(), e.getRpcReply());
                        _log.warn("Failed to process RPC request: {}", e.getMessage());
                    } catch (OncRpcException e) {
                        call.failRpcGarbage();
                        _log.warn("Failed to process RPC request: {}", e.getMessage());
                    } catch (IOException e) {
                        call.failRpcGarbage();
                        _log.warn("Failed to process RPC request: {}", e.getMessage());
                    }
                }
            }
        });
        return ctx.getInvokeAction();
    }
}
