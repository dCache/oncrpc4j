/*
 * Copyright (c) 2009 - 2022 Deutsches Elektronen-Synchroton,
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
import java.io.IOException;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

/**
 * {@code Filter} to receive RPC message over UDP connection.
 * According to RFC 1831 RPC message over UDP arrived in a single
 * UDP packet.
 */
public class RpcMessageParserUDP extends BaseFilter {

    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        Buffer messageBuffer = ctx.getMessage();

        Xdr xdr = new Xdr(messageBuffer, ctx.getMemoryManager());
        ctx.setMessage(xdr);

        return ctx.getInvokeAction();
    }
}
