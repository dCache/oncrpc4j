/*
 * Copyright (c) 2009 - 2012 Deutsches Elektronen-Synchroton,
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
package org.dcache.utils.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.junit.Test;
import static org.junit.Assert.*;

public class InetSocketAddressesTest {

    @Test
    public void testLocalHostV4() throws Exception {
        String uaddr = "127.0.0.1.203.81";
        InetSocketAddress socketAddress = InetSocketAddresses.forUaddrString(uaddr);
        assertEquals("port mismatch", 52049, socketAddress.getPort());
        assertEquals("host mismatch", InetAddress.getByName("127.0.0.1"),
                socketAddress.getAddress());
    }

    @Test
    public void testLocalHostV6() throws Exception {
        String uaddr = "::1.203.81";
        InetSocketAddress socketAddress = InetSocketAddresses.forUaddrString(uaddr);
        assertEquals("port mismatch", 52049, socketAddress.getPort());
        assertEquals("host mismatch", InetAddress.getByName("::1"),
                socketAddress.getAddress());
    }

    @Test
    public void testLocalHostV4Revert() throws Exception {
        String uaddr = "127.0.0.1.203.81";
        InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName("127.0.0.1"),52049);

        assertEquals("reverce convertion failed", uaddr,
                InetSocketAddresses.uaddrOf(socketAddress));
    }

    @Test
    public void testHostAndPortIpv4() throws Exception {
        String hostAndPort = "127.0.0.1:1111";
        InetSocketAddress address = InetSocketAddresses.inetAddressOf(hostAndPort);

        assertEquals(InetAddress.getByName("127.0.0.1"), address.getAddress());
        assertEquals(1111, address.getPort());
    }

    @Test
    public void testHostAndPortIpv6() throws Exception {
        String hostAndPort = "[fe80::21c:c0ff:fea0:caf4]:1111";
        InetSocketAddress address = InetSocketAddresses.inetAddressOf(hostAndPort);

        assertEquals(InetAddress.getByName("fe80::21c:c0ff:fea0:caf4"), address.getAddress());
        assertEquals(1111, address.getPort());
    }
}
