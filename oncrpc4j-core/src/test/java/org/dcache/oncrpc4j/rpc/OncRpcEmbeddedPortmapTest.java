package org.dcache.oncrpc4j.rpc;

import org.dcache.oncrpc4j.rpc.OncRpcSvc;
import org.dcache.oncrpc4j.rpc.OncRpcClient;
import org.dcache.oncrpc4j.rpc.OncRpcProgram;
import org.dcache.oncrpc4j.rpc.OncRpcSvcBuilder;
import org.dcache.oncrpc4j.rpc.net.IpProtocolType;
import org.dcache.oncrpc4j.xdr.XdrVoid;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import org.dcache.oncrpc4j.portmap.GenericPortmapClient;
import org.dcache.oncrpc4j.portmap.OncPortmapClient;
import org.dcache.oncrpc4j.portmap.OncRpcEmbeddedPortmap;
import org.dcache.oncrpc4j.portmap.rpcb;
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
	private static final int PROGNUM = "OncRpcEmbeddedPortmapTest".hashCode();

	private OncRpcEmbeddedPortmap portmap = null;

	@Test
	public void testEmbeddedPortmapWithDummyService() throws IOException, OncRpcException, TimeoutException {
		portmap = new OncRpcEmbeddedPortmap();
		assumeTrue(portmap.isEmbeddedPortmapper()); // skip test if not embedded portmapper

		OncRpcSvc svc = new OncRpcSvcBuilder()
			.withTCP()
			.withAutoPublish()
			.withSameThreadIoStrategy()
			.withRpcService(new OncRpcProgram(PROGNUM, 1), call -> call.reply(XdrVoid.XDR_VOID))
			.build();
		svc.start();
		// Open portmap and check nedtid content with dump
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
		if ( portmap != null ) {
			portmap.shutdown();
		}
	}
}
