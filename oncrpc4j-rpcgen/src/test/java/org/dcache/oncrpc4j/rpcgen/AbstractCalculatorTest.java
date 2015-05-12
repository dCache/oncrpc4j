package org.dcache.oncrpc4j.rpcgen;

import org.dcache.xdr.IpProtocolType;
import org.dcache.xdr.OncRpcProgram;
import org.dcache.xdr.OncRpcSvc;
import org.dcache.xdr.OncRpcSvcBuilder;
import org.junit.After;
import org.junit.Before;

import java.net.InetAddress;

/**
 * @author Radai Rosenblatt
 * @version May 10, 2015
 * @since Phase1
 */
public class AbstractCalculatorTest {
    protected CalculatorServerImpl serverImpl = new CalculatorServerImpl();
    protected OncRpcSvc server;
    protected CalculatorClient client;
    protected String address = "127.0.0.1";
    protected int port = 6666;

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
