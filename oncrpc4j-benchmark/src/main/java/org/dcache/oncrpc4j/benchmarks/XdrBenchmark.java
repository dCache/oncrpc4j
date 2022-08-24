package org.dcache.oncrpc4j.benchmarks;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;
import org.dcache.oncrpc4j.xdr.Xdr;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
public class XdrBenchmark {


    @Param({"1024", "8192", "262144", "1048576"})
    private String size;

    private Xdr xdr;

    private ByteBuffer bb;

    @Setup
    public void setUp() {

        byte[] buf = new byte[Integer.parseInt(size)];
        ThreadLocalRandom.current().nextBytes(buf);

        bb = ByteBuffer.wrap(buf);
        xdr = new Xdr(256);
    }


    @Benchmark
    public void encodeByteBuffer(Blackhole blackhole) {

        xdr.beginEncoding();
        bb.clear().limit(bb.capacity());
        xdr.xdrEncodeByteBuffer(bb);
        xdr.endEncoding();

        blackhole.consume(xdr);
    }

    @Benchmark
    public void encodeByteBufferShallow(Blackhole blackhole) {

        xdr.beginEncoding();
        bb.clear().limit(bb.capacity());
        xdr.xdrEncodeShallowByteBuffer(bb);
        xdr.endEncoding();

        blackhole.consume(xdr);
    }

}
