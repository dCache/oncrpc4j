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
package org.dcache.oncrpc4j.rpc.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.junit.Test;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;

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
    public void testSocketAddressToUaddrIPv4() throws Exception {
        String uaddr = "127.0.0.1.203.81";
        InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName("127.0.0.1"),52049);

        assertEquals("reverce convertion failed", uaddr,
                InetSocketAddresses.uaddrOf(socketAddress));
    }

    @Test
    public void testSocketAddressToUaddrIPv6() throws Exception {
        String uaddr = "fe80:0:0:0:21c:c0ff:fea0:caf4.203.81";
        InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName("fe80::21c:c0ff:fea0:caf4"), 52049);

        assertEquals("reverce convertion failed", uaddr,
                InetSocketAddresses.uaddrOf(socketAddress));
    }

    @Test
    public void testSocketAddressToUaddrIPv6WithScope() throws Exception {
        String uaddr = "fe80:0:0:0:21c:c0ff:fea0:caf4.203.81";
        InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName("fe80::21c:c0ff:fea0:caf4%1"), 52049);

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

    @Test(expected = IllegalArgumentException.class)
    public void testHostNoPortIPv4() {
        String uaddr = "127.0.0.1";
        InetSocketAddresses.forUaddrString(uaddr);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHostPartiallyNoPortIPv4() {
        String uaddr = "127.0.0.1.1";
        InetSocketAddresses.forUaddrString(uaddr);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHostNoPortIPv6() {
        String uaddr = "fe80::21c:c0ff:fea0:caf4";
        InetSocketAddresses.forUaddrString(uaddr);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHostPartiallyNoPortIPv6() {
        String uaddr = "fe80::21c:c0ff:fea0:caf4.1";
        InetSocketAddresses.forUaddrString(uaddr);
    }

    @Test
    public void testTCPv4Netid() throws UnknownHostException {
        InetAddress address = InetAddress.getByName("1.1.1.1");
        assertEquals("invalid netid", "tcp", InetSocketAddresses.tcpNetidOf(address));
    }

    @Test
    public void testTCPv6Netid() throws UnknownHostException {
        InetAddress address = InetAddress.getByName("fe80::21c:c0ff:fea0:caf4");
        assertEquals("invalid netid", "tcp6", InetSocketAddresses.tcpNetidOf(address));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalitInetaddressType() throws UnknownHostException {
        // java19 have made InetAddress sealed with permitted subtypes Inet4Address, Inet6Address
        assumeThat(Runtime.version().feature(), lessThanOrEqualTo(11));
        InetAddress address = mock(InetAddress.class); // not a direct instance of Inet4/6Address
        assertEquals("invalid netid", "tcp6", InetSocketAddresses.tcpNetidOf(address));
    }

}
