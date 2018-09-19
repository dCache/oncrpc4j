package org.dcache.oncrpc4j.rpc;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.dcache.oncrpc4j.rpc.net.IpProtocolType;
import org.dcache.oncrpc4j.portmap.GenericPortmapClient;
import org.dcache.oncrpc4j.portmap.OncRpcPortmap;
import org.dcache.oncrpc4j.portmap.OncRpcbindServer;
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
        OncRpcClient rpcClient = new OncRpcClient(rpcbindServer.getInetSocketAddress(protoType), protoType);
        RpcTransport transport = rpcClient.connect();
        GenericPortmapClient portmapClient = new GenericPortmapClient(transport);
        assertFalse("Newly started OncRpcbindServer must publish himself", portmapClient.dump().isEmpty());
    }

    @Test
    public void SetUnsetTest() throws IOException, TimeoutException {

        OncRpcSvc rpcbindServer = new OncRpcSvcBuilder()
                .withTCP()
                .withUDP()
                .withoutAutoPublish()
                .withRpcService(new OncRpcProgram(OncRpcPortmap.PORTMAP_PROGRAMM, OncRpcPortmap.PORTMAP_V2), new OncRpcbindServer())
                .build();
        rpcbindServer.start();
        int protoType = IpProtocolType.TCP;
        OncRpcClient rpcClient = new OncRpcClient(rpcbindServer.getInetSocketAddress(protoType), protoType);
        RpcTransport transport = rpcClient.connect();
        GenericPortmapClient portmapClient = new GenericPortmapClient(transport);

        int n = portmapClient.dump().size();
        assertTrue(portmapClient.setPort(9000001, 1, "tcp", "127.0.0.1.0.234", "superuser"));
        assertEquals(n + 1, portmapClient.dump().size());
        assertTrue(portmapClient.unsetPort(9000001, 1, "superuser"));
        assertEquals(n, portmapClient.dump().size());
    }
}
