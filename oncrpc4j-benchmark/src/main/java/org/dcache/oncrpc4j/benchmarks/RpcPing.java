package org.dcache.oncrpc4j.benchmarks;

import org.dcache.oncrpc4j.rpc.OncRpcClient;
import org.dcache.oncrpc4j.rpc.OncRpcProgram;
import org.dcache.oncrpc4j.rpc.OncRpcSvc;
import org.dcache.oncrpc4j.rpc.OncRpcSvcBuilder;
import org.dcache.oncrpc4j.rpc.RpcAuthTypeNone;
import org.dcache.oncrpc4j.rpc.RpcCall;
import org.dcache.oncrpc4j.rpc.RpcTransport;
import org.dcache.oncrpc4j.rpc.net.IpProtocolType;
import org.dcache.oncrpc4j.xdr.XdrAble;
import org.dcache.oncrpc4j.xdr.XdrVoid;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
public class RpcPing {

    private static final int PROG_NUMBER = 100017;
    private static final int PROG_VERS = 1;
    private static final OncRpcProgram prog = new OncRpcProgram(PROG_NUMBER, PROG_VERS);

    private OncRpcSvc svc;
    private OncRpcClient rpcClient;
    private RpcCall call;

    @Setup
    public void setUp() throws IOException {

        svc = new OncRpcSvcBuilder()
                .withTCP()
                .withoutAutoPublish()
                .withPort(0)
                .withSameThreadIoStrategy()
                .withRpcService(prog, call -> call.reply(XdrVoid.XDR_VOID))
                .build();

        svc.start();

        InetSocketAddress socketAddress = svc.getInetSocketAddress(IpProtocolType.TCP);
        rpcClient = new OncRpcClient(socketAddress, IpProtocolType.TCP);
        RpcTransport transport = rpcClient.connect();
        call = new RpcCall(prog.getNumber(), prog.getVersion(), new RpcAuthTypeNone(), transport);
    }

    @Benchmark
    public XdrAble rpcPingSingle() throws IOException, ExecutionException, InterruptedException {
        return call.call(0, XdrVoid.XDR_VOID, XdrVoid.class).get();
    }

    @TearDown
    public void tearDown() throws IOException {
        rpcClient.close();
        svc.stop();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(RpcPing.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
