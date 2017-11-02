package org.dcache.xdr;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeoutException;

import org.dcache.xdr.IpProtocolType;
import org.dcache.xdr.OncRpcClient;
import org.dcache.xdr.OncRpcProgram;
import org.dcache.xdr.OncRpcSvc;
import org.dcache.xdr.OncRpcSvcBuilder;
import org.dcache.xdr.XdrTransport;
import org.dcache.xdr.portmap.GenericPortmapClient;
import org.dcache.xdr.portmap.OncRpcPortmap;
import org.dcache.xdr.portmap.OncRpcbindServer;
import org.dcache.xdr.portmap.rpcb;
import org.junit.Test;

public class TestGitHubIssue56 {

	@Test
	public void DumpTest() throws IOException, TimeoutException {

		OncRpcSvc rpcbindServer = new OncRpcSvcBuilder()
                .withTCP()
                .withUDP()
                .withoutAutoPublish()
                .withRpcService(new OncRpcProgram(OncRpcPortmap.PORTMAP_PROGRAMM, OncRpcPortmap.PORTMAP_V2), new OncRpcbindServer())
                .build();
		rpcbindServer.start();
		int protoType = IpProtocolType.TCP;
		OncRpcClient rpcClient = new OncRpcClient(rpcbindServer.getInetSocketAddress(protoType),protoType );
		XdrTransport transport = rpcClient.connect();
		GenericPortmapClient portmapClient = new GenericPortmapClient(transport);
		for (rpcb r : portmapClient.dump()){
			assertEquals("administrator",r.getOwner());
		}
		
	}

}
