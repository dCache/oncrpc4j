package org.dcache.oncrpc4j.rpcgen;

import org.dcache.xdr.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.util.concurrent.Future;

public class FutureCalculatorTest {
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
    public void testFutureAdd() throws Exception {
        long callTime = System.currentTimeMillis();
        Future<CalculationResult> future = client.add_1_future(1, 2);
        long retTime = System.currentTimeMillis();
        Assert.assertNotNull(future);
        CalculationResult result = future.get();
        long resTime = System.currentTimeMillis();
        Assert.assertEquals(3, result.getResult());
        //to prove async operation (there's a sleep() server-side)
        //call <= start < finish <= res
        //ret < finish
        Assert.assertTrue(callTime <= result.startMillis);
        Assert.assertTrue(result.startMillis < result.finishMillis);
        Assert.assertTrue(result.finishMillis <= resTime);
        Assert.assertTrue(retTime < result.finishMillis);
    }

    @Test
    public void testFutureAddSimple() throws Exception {
        long callTime = System.nanoTime();
        Future<XdrLong> future = client.addSimple_1_future(1, 2);
        long retTime = System.nanoTime();
        XdrLong result = future.get();
        long resTime = System.nanoTime();
        long invocationTime = retTime-callTime;
        long waitTime = resTime-retTime;
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.longValue());
        //not really proof of async, but good enough?
        Assert.assertTrue(waitTime > invocationTime);
    }
}
