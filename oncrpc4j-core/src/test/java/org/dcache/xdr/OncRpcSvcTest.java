/*
 * Copyright (c) 2009 - 2013 Deutsches Elektronen-Synchroton,
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
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class OncRpcSvcTest {

    private OncRpcSvc svc;

    @Test
    public void testBindToInterface() throws IOException {
        svc = new OncRpcSvcBuilder()
                .withTCP()
                .withUDP()
                .withoutAutoPublish()
                .withMinPort(0)
                .withMinPort(4096)
                .withBindAddress("127.0.0.1")
                .build();
        svc.start();

        InetSocketAddress tcpSocketAddresses = svc.getInetSocketAddress(IpProtocolType.TCP);
        InetSocketAddress udpSocketAddresses = svc.getInetSocketAddress(IpProtocolType.UDP);
        assertTrue(!tcpSocketAddresses.getAddress().isAnyLocalAddress());
        assertTrue(!udpSocketAddresses.getAddress().isAnyLocalAddress());
    }

    @Test
    public void testNotBindToInterface() throws IOException {
        svc = new OncRpcSvcBuilder()
                .withTCP()
                .withUDP()
                .withoutAutoPublish()
                .withMinPort(0)
                .withMinPort(4096)
                .build();
        svc.start();

        InetSocketAddress tcpSocketAddresses = svc.getInetSocketAddress(IpProtocolType.TCP);
        InetSocketAddress udpSocketAddresses = svc.getInetSocketAddress(IpProtocolType.UDP);
        assertTrue(tcpSocketAddresses.getAddress().isAnyLocalAddress());
        assertTrue(udpSocketAddresses.getAddress().isAnyLocalAddress());
    }

    @After
    public void tearDown() throws IOException {
        svc.stop();
    }
}
