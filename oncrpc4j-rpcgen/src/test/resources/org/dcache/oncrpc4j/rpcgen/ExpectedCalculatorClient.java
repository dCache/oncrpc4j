package org.dcache.oncrpc4j.rpcgen;
import org.dcache.xdr.*;
import java.io.IOException;

import java.io.Closeable;
import java.net.InetAddress;
import java.util.concurrent.Future;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/*
   Minimal spoon valid file allowing to test some results from
   rpcgen applied to Calculator.x
   
*/


public class ExpectedCalculatorClient implements Closeable {
            final OncRpcClient rpcClient = null;
             private final RpcCall client = null;
/*
  Following code is expected for the Calculator "add"  remote 
  procedure call
*/       

      
public CalculationResult add_1(long arg1, long arg2, long _timeoutValue, TimeUnit _timeoutUnit, RpcAuth _auth)
           throws OncRpcException, IOException, TimeoutException {
        class XdrAble$ implements XdrAble {
            public long arg1;
            public long arg2;
            public void xdrEncode(XdrEncodingStream xdr)
                throws OncRpcException, IOException {
                xdr.xdrEncodeLong(arg1);
                xdr.xdrEncodeLong(arg2);
            }
            public void xdrDecode(XdrDecodingStream xdr)
                throws OncRpcException, IOException {
            }
        };
        XdrAble$ args$ = new XdrAble$();
        args$.arg1 = arg1;
        args$.arg2 = arg2;
        CalculationResult result$ = new CalculationResult();
        client.call(Calculator.add_1, args$, result$, _timeoutValue, _timeoutUnit, _auth);
        return result$;
    }
    public void close(){
    }
}