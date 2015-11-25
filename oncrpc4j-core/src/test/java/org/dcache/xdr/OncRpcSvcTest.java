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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;
import org.dcache.utils.net.InetSocketAddresses;
import org.dcache.xdr.portmap.GenericPortmapClient;
import org.dcache.xdr.portmap.OncPortmapClient;
import org.dcache.xdr.portmap.OncRpcPortmap;
import org.dcache.xdr.portmap.OncRpcbindServer;
import org.dcache.xdr.portmap.rpcb;
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
	
	@Test
    public void testPortmapSetApplication() throws IOException, TimeoutException {
		int TEST_PROG = 100024;
		int TEST_PROG_VER = 1;
		String TEST_PROG_OWNER = "superuser";
		OncRpcbindServer bindService = new OncRpcbindServer();
		OncRpcProgram portMapProg = new OncRpcProgram(OncRpcPortmap.PORTMAP_PROGRAMM, OncRpcPortmap.PORTMAP_V2);
        svc = new OncRpcSvcBuilder()
                .withTCP()
                .withUDP()
                .withoutAutoPublish()
                .withMinPort(0)
                .withMinPort(4096)
                .withBindAddress("127.0.0.1")
                .build();
		svc.register(portMapProg,bindService);
        svc.start();
        try ( OncRpcClient rpcClient = new OncRpcClient(InetAddress.getByName(null), IpProtocolType.UDP, svc.getInetSocketAddress(IpProtocolType.UDP).getPort() ) ) {
			OncPortmapClient portmapClient = new GenericPortmapClient(rpcClient.connect()); // init portmapper (only v2 atm)
            assertTrue(portmapClient.ping()); // ping portmap
			assertTrue( portmapClient.getPort(OncRpcPortmap.PORTMAP_PROGRAMM, OncRpcPortmap.PORTMAP_V2, "tcp").equals("127.0.0.1.0.111") ); // check port
			String addr = InetSocketAddresses.uaddrOf(new InetSocketAddress("127.0.0.1",1234)); 
			assertTrue( portmapClient.setPort(TEST_PROG, TEST_PROG_VER, IpProtocolType.toString(IpProtocolType.TCP),addr, TEST_PROG_OWNER) ); // reg app with tcp and udp
			assertTrue( portmapClient.setPort(TEST_PROG, TEST_PROG_VER, IpProtocolType.toString(IpProtocolType.UDP),addr, TEST_PROG_OWNER) ); // reg app with udp and udp
			assertFalse( portmapClient.setPort(TEST_PROG, TEST_PROG_VER, IpProtocolType.toString(IpProtocolType.TCP),addr, TEST_PROG_OWNER) ); // try again app with tcp 
			assertFalse( portmapClient.setPort(TEST_PROG, TEST_PROG_VER, IpProtocolType.toString(IpProtocolType.UDP),addr, TEST_PROG_OWNER) ); // try again app with udp 
			assertTrue( addr.equals( portmapClient.getPort(TEST_PROG,TEST_PROG_VER, IpProtocolType.toString(IpProtocolType.TCP) ) ) ); // check tcp address match
			assertTrue( portmapClient.unsetPort(TEST_PROG, TEST_PROG_VER, TEST_PROG_OWNER) ); // remove app 
			assertFalse( portmapClient.unsetPort(TEST_PROG, TEST_PROG_VER,TEST_PROG_OWNER) ); // remove app again
			// do dump lookup test
			boolean found = false;
			for ( rpcb current : portmapClient.dump() ) {
				if ( current.getProg() == 100024 && current.getVers() == 1 ) {
					found = true;
				}
			}
			assertTrue(!found); // we should not find one anymore
			svc.unregister(portMapProg); // just remove portmap
        }
    }
	
    @After
    public void tearDown() throws IOException {
        svc.stop();
    }
}
