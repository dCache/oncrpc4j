package org.dcache.oncrpc4j.rpcgen;

import org.dcache.xdr.IpProtocolType;
import org.dcache.xdr.OncRpcProgram;
import org.dcache.xdr.OncRpcSvc;
import org.dcache.xdr.OncRpcSvcBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;

public class SyncCalculatorTest {
    private OncRpcSvc server;
    private CalculatorClient client;
    private String address = "127.0.0.1";
    private int port = 6666;

    @Before
    public void setup() throws Exception{
        server = new OncRpcSvcBuilder()
                .withTCP()
                .withoutAutoPublish() //so we dont need rpcbind
                .withPort(port)
                .withSameThreadIoStrategy()
                .withBindAddress(address)
                .build();
        server.register(new OncRpcProgram(Calculator.CALCULATOR, Calculator.CALCULATORVERS), new CalculatorServerImpl());
        server.start();
        client = new CalculatorClient(
                InetAddress.getByName(address),
                port,
                Calculator.CALCULATOR,
                Calculator.CALCULATORVERS,
                IpProtocolType.TCP);
    }

    @After
    public void teardown() throws Exception {
        server.stop();
    }

    @Test
    public void testSyncAdd() throws Exception {
        CalculationResult result = client.add_1(1, 2);
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.getResult());
    }
}
