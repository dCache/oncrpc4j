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
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class RpcProtocolFilterTest {

    private final static int INVOKE = 0;
    private final static int STOP = 1;
    private Filter filter;
    private FilterChainContext mockedContext;

    @Before
    public void setUp() {
        filter = new RpcProtocolFilter( new ReplyQueue());
        mockedContext = FilterChainContext.create(mock(Connection.class));
    }

    @Test
    public void testSomeMethod() throws IOException {
        mockedContext.setMessage( createBadXdr() );
        assertEquals(STOP, filter.handleRead(mockedContext).type());
    }

    private Xdr createBadXdr() {
        Xdr xdr = new Xdr(32);
        xdr.beginEncoding();
        RpcMessage rpcMessage = new RpcMessage(1, 2); // xdr, type 0 = call, 1 = reply, 2 = not allowed
        rpcMessage.xdrEncode(xdr);
        return xdr;
    }
}
