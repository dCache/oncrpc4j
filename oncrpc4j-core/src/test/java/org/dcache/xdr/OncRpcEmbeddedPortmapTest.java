/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dcache.xdr;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import org.dcache.xdr.portmap.GenericPortmapClient;
import org.dcache.xdr.portmap.OncPortmapClient;
import org.dcache.xdr.portmap.OncRpcEmbeddedPortmap;
import org.dcache.xdr.portmap.rpcb;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import org.junit.Test;

/**
 * Quick testing for OncRpcEmbeddedPortmap
 */
public class OncRpcEmbeddedPortmapTest {
	// https://tools.ietf.org/html/draft-ietf-nfsv4-rpc-netid-06
	public static final String[] NETID_NAMES = new String[] {"-","ticlts","ticots","ticotsord","tcp","tcp6","udp","udp6","rdma","rdma6","sctp","sctp6"};
	private OncRpcEmbeddedPortmap portmap;
	
	@Test
    public void testEmbeddedPortmapWithDummyService() throws IOException, OncRpcException, TimeoutException {
		portmap = new OncRpcEmbeddedPortmap();
		assumeTrue(portmap.isEmbeddedPortmapper()); // skip test if not embedded portmapper
		
		RpcDispatchable dummy = new RpcDispatchable() {
            @Override
            public void dispatchOncRpcCall(RpcCall call) throws OncRpcException, IOException {
                call.reply(XdrVoid.XDR_VOID);
            }
        };
		OncRpcSvc svc = new OncRpcSvcBuilder()
                .withTCP()
                .withAutoPublish()
                .withSameThreadIoStrategy()
                .withRpcService(new OncRpcProgram(100017, 1), dummy)
                .build();
        svc.start();
		// Open portmap and check content with dump
		try ( OncRpcClient rpcClient = new OncRpcClient(InetAddress.getLocalHost(), IpProtocolType.UDP,111) ) {
			OncPortmapClient portmapClient = new GenericPortmapClient(rpcClient.connect()); // init portmapper (only v2 atm)		
			portmapClient.ping();
			for ( rpcb current : portmapClient.dump() ) {
				assertTrue("NedId value incorrect: "+current.getNetid(), Arrays.asList(NETID_NAMES).contains(current.getNetid()) );
			}
		}
		svc.stop();
	}
	
	@After
    public void tearDown() throws IOException {
        portmap.shutdown();
    }
}
