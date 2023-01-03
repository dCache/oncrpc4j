package org.dcache.oncrpc4j.rpcgen;

import org.dcache.oncrpc4j.rpc.net.IpProtocolType;
import org.dcache.oncrpc4j.rpc.OncRpcProgram;
import org.dcache.oncrpc4j.rpc.OncRpcSvc;
import org.dcache.oncrpc4j.rpc.OncRpcSvcBuilder;
import org.junit.After;
import org.junit.Before;

import java.net.InetAddress;

public abstract class AbstractCalculatorTest {
    protected CalculatorServerImpl serverImpl = new CalculatorServerImpl();
    protected OncRpcSvc server;
    protected CalculatorClient client;
    protected String address = "127.0.0.1";
    volatile protected int port = 0;

    @Before
    public void setup() throws Exception{
        server = new OncRpcSvcBuilder()
                .withTCP()
                .withoutAutoPublish() //so we dont need rpcbind
                .withPort(port)
                .withSameThreadIoStrategy()
                .withBindAddress(address)
                .build();
        server.register(new OncRpcProgram(Calculator.CALCULATOR, Calculator.CALCULATORVERS), serverImpl);
        server.start();

        port = server.getInetSocketAddress(IpProtocolType.TCP).getPort();

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
}
