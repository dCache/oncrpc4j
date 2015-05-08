package org.dcache.oncrpc4j.rpcgen;

import org.dcache.xdr.RpcCall;

public class CalculatorServerImpl extends CalculatorServer {
    @Override
    public CalculationResult add_1(RpcCall call$, long arg1, long arg2) {
        long start = System.currentTimeMillis();
        CalculationResult result = new CalculationResult();
        result.setStartMillis(start);
        result.setResult(arg1 + arg2);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        result.setFinishMillis(System.currentTimeMillis());
        return result;
    }

    @Override
    public long addSimple_1(RpcCall call$, long arg1, long arg2) {
        return arg1 + arg2;
    }
}
