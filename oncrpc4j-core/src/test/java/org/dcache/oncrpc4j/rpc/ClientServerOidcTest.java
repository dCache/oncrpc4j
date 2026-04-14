package org.dcache.oncrpc4j.rpc;

import org.dcache.oncrpc4j.rpc.net.IpProtocolType;
import org.dcache.oncrpc4j.xdr.XdrVoid;
import org.dcache.oncrpc4j.xdr.XdrString;

import org.dcache.oncrpc4j.rpc.oidc.OidcSessionManager;
import org.dcache.oncrpc4j.rpc.oidc.AuthOidc;
import org.dcache.oncrpc4j.rpc.oidc.OidcConfig;
import org.dcache.oncrpc4j.rpc.oidc.VisitableClaim;
import org.dcache.oncrpc4j.rpc.oidc.ClaimType;

import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class ClientServerOidcTest {

    private static final int PROGNUM = 100017;
    private static final int PROGVER = 1;

    private static final int ECHO = 1;
    private static final int UPPER = 2;
    private static final int SHUTDOWN = 3;
    private static final int LOST = 4;

    private OncRpcSvc svc;
    private OncRpcClient clnt;
    private RpcCall clntCall;

    @Before
    public void setUp() throws IOException {

        RpcDispatchable echo = (RpcCall call) -> {
            
          System.out.println("---IN TEST: ???");


            switch (call.getProcedure()) {

                case 0: {
                    System.out.println("---IN TEST::PROC::NULLPROC ???");

                    break;
                }
                case ECHO: {

                    System.out.println("---IN TEST::PROC::ECHO ???");

                    XdrString s = new XdrString();
                    call.retrieveCall(s);
                    call.reply(s);
                    break;
                }
                case UPPER: {
                    RpcCall cb = new RpcCall(PROGNUM, PROGVER, new RpcAuthTypeNone(), call.getTransport());
                    XdrString s = new XdrString();
                    call.retrieveCall(s);
                    cb.call(ECHO, s, s);
                    call.reply(s);
                    break;
                }
                case SHUTDOWN: {
                    svc.stop();
                    break;
                }
                case LOST: {
                    // no reply
                    break;
                }
            }
        };

        RpcDispatchable upper = (RpcCall call) -> {
            XdrString s = new XdrString();
            call.retrieveCall(s);
            XdrString u = new XdrString(s.stringValue().toUpperCase());
            call.reply(u);
        };

        OidcConfig oidcConfig = new OidcConfig();

        oidcConfig.jwksUri("https://gitlab.desy.de/oauth/discovery/keys")
                  .iss("https://gitlab.desy.de")
                  .aud("613a30a514a97c108a56f2f400e4f125f3c4953baf90330e588cfe9e361a9714")
                  .timeSkewExp(0)
                  .timeSkewNbf(0)
                  .addVisitableClaim(
                     // MANDATORY: Authentication fails if 'sub' is missing
                     new VisitableClaim("sub", ClaimType.CLAIM_STR, true,
                          (val) -> {
                              System.out.println("TEST: calim sub: " + val);
                              return true;
                          },
                          null
                       )
                   )
                  .addVisitableClaim (
                      new VisitableClaim("iss", ClaimType.CLAIM_STR, true,
                         (val) -> {
                             System.out.println("TEST: calim iss: " + val);
                             return true;
                         },
                         null
                      ) 
                  )
                  ;

        svc = new OncRpcSvcBuilder()
                .withoutAutoPublish()
                .withTCP()
                .withWorkerThreadIoStrategy()
		            .withBindAddress("127.0.0.1")
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), echo)
                .withServiceName("svc")
                .withSessionManager(new OidcSessionManager())
                .withOidcConfig(oidcConfig)
                .build();
        svc.start();

        clnt = OncRpcClient.newBuilder()
                .withTCP()
                .withRpcService(new OncRpcProgram(PROGNUM, PROGVER), upper)
                .withServiceName("clnt")
                .build(svc.getInetSocketAddress(IpProtocolType.TCP));

        RpcTransport t = clnt.connect();
        clntCall = new RpcCall(PROGNUM, PROGVER, new AuthOidc("oidc_token_test"), t);
   }

    @After
    public void tearDown() throws IOException {
        if (svc != null) {
            svc.stop();
        }
        if (clnt != null) {
            clnt.close();
        }
    }

    @Test
    public void shouldCallCorrectProcedure() throws IOException {
        XdrString s = new XdrString("hello");
        XdrString reply = new XdrString();
        
        // An extra cal to init the handshake
        clntCall.credRefreshCall();
        clntCall.call(ECHO, s, reply);
        assertEquals("reply mismatch", s, reply);
    }

}
