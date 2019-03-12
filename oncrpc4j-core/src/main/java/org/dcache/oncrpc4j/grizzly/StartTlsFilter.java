/*
 * Copyright (c) 2019 Deutsches Elektronen-Synchroton,
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

import com.google.common.annotations.Beta;
import java.io.IOException;
import org.dcache.oncrpc4j.rpc.RpcAuthError;
import org.dcache.oncrpc4j.rpc.RpcAuthException;
import org.dcache.oncrpc4j.rpc.RpcAuthStat;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.ssl.SSLFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A pass-through {@link Filter} that will enable TLS on connection if required.
 */
@Beta
public class StartTlsFilter extends BaseFilter {

    private final static Logger LOGGER = LoggerFactory.getLogger(StartTlsFilter.class);

    private final SSLFilter sslFilter;
    private final boolean isClient;
    private volatile boolean start;

    public StartTlsFilter(SSLFilter sslFilter, boolean isClient) {
        this.sslFilter = sslFilter;
        this.isClient = isClient;
    }

    @Override
    public NextAction handleWrite(FilterChainContext ctx) throws IOException {
        /** After service receives start-TLS it must reply to the client and then
         * enable TLS on the connection.
         */

        NextAction nextAction = super.handleWrite(ctx);
        if (start) {
            enableSSLFilter(ctx.getConnection());
        }

        return nextAction;
    }

    public void startTLS(Connection connection) throws RpcAuthException {
        start = true;
        if (isClient) {
            enableSSLFilter(connection);
            try {
                sslFilter.handshake(connection, new EmptyCompletionHandler<>());
            } catch (IOException e) {
                LOGGER.error("Failed to perform TLS handshake: {}", e.getMessage());
                throw new RpcAuthException("Failed to perform TLS handshake",
                        new RpcAuthError(RpcAuthStat.AUTH_FAILED));
            }
        }
    }

    private void enableSSLFilter(Connection connection) {
        final FilterChain currentChain = (FilterChain) connection.getProcessor();
        FilterChainBuilder chainBuilder = FilterChainBuilder
                .stateless()
                .addAll(currentChain)
                .remove(this)
                .add(1, sslFilter);
        connection.setProcessor(chainBuilder.build());
    }
}
