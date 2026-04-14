package org.dcache.oncrpc4j.rpc.oidc;

import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.dcache.oncrpc4j.util.Bytes;
import org.dcache.oncrpc4j.rpc.RpcAuthType;
import org.dcache.oncrpc4j.rpc.RpcCall;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.net.URL;
import java.net.MalformedURLException;


public class OidcProtocolFilter extends BaseFilter {
    private static final Logger _log = LoggerFactory.getLogger(OidcProtocolFilter.class);

    private OidcSessionManager _oidcSesMgr;
    private  JwkProvider _jwkProvider;
    private final OidcConfig _config;

    public OidcProtocolFilter(OidcConfig config,
                              OidcSessionManager mgr) {

        _oidcSesMgr = mgr;
        _config = config;

        try {
        _jwkProvider = new JwkProviderBuilder(new URL(_config.jwksUri()))
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build();
        } catch(MalformedURLException e) {
            _jwkProvider = null;
            _log.error("Error while building a JwkProvider");
            _log.error(e.getMessage());
            throw new OidcException(e.getMessage());
        }
    }

    private final OIDC.MessageVisitor<NextAction, Object> _protocolVisitor = OIDC.MessageVisitor.of(
        (OIDC.ChunkMessage msg, Object... params) -> {
            FilterChainContext ctx = OIDC.Params.get(params, 1, FilterChainContext.class);
            RpcCall call = ctx.getMessage();
            OidcSvcCtx oidcSvcCtx;
        
            try {
                // 1. Retrieve or Create Context
                switch (msg.chunk().state()) {
                    case INIT:
                        // Pass byte[] handle to get a new session with hashed long uid
                        oidcSvcCtx = _oidcSesMgr.createContext(generateHandle());
                        break;

                    case DATA:
                    case END:
                        // Use the long uid provided in the message for lookup
                        oidcSvcCtx = _oidcSesMgr.getContext(msg.uid());
                        break;

                    default:
                        throw new OidcException.Protocol("Invalid Chunk State");
               }       
                // 2. Append token data (This may throw IllegalStateException if too large)
                oidcSvcCtx.appendToken(msg.chunk().bytes());
        
                // 3. Handle END state verification
                if (msg.chunk().state() == OIDC.ChunkState.END) {
                    return performVerification(ctx, oidcSvcCtx, call);
                }
        
                // Otherwise, acknowledge the chunk
                call.reply(OIDC.Message.createHashMessage(oidcSvcCtx.uid(), new OIDC.Hash(0)));
                return ctx.getStopAction();
        
            } catch (OidcException.Protocol e) {

                String remoteAddr = "unknown";
                if (ctx.getConnection() != null && ctx.getConnection().getPeerAddress() != null) {
                    remoteAddr = ctx.getConnection().getPeerAddress().toString();
                }
                _log.warn("Protocol violation from client {}: {}", remoteAddr, e.getMessage());
                reject(call, msg.uid());
            } catch (OidcException.Signature e) {
                _log.error("Invalid JWT signature");
                _log.error(e.getMessage());
                reject(call, msg.uid());
            } catch (OidcException.Validation e) {
                _log.info("Authorization denied");
                _log.info(e.getMessage());
                reject(call, msg.uid());
            } catch (Exception e) {
                _log.error("Unexpected OIDC failure", e);
                reject(call, msg.uid());
            }
            
            return ctx.getStopAction();
        },

        (OIDC.HashMessage msg, Object... params) -> {
           FilterChainContext ctx = OIDC.Params.get(params, 1, FilterChainContext.class);
           RpcCall call = ctx.getMessage();

           try {
               // 1. Retrieve the session context using the UID from the message
               OidcSvcCtx oidcSvcCtx = _oidcSesMgr.getContext(msg.uid());
       
               // 2. Perform Hash Verification
               // msg.hash() is the hash sent by the client
               // oidcSvcCtx.hash() is what we calculated or stored during reassembly
               if (msg.hash().value() != oidcSvcCtx.hash()) {
                   throw new OidcException.Validation("Hash mismatch: Client sent " 
                           + msg.hash() + " but session expected " + oidcSvcCtx.hash());
               }
       
               // 3. Hash is valid -> Proceed to invoke the next filter in the chain
               _log.debug("Hash verified for session {}. Proceeding to action.", msg.uid());
               return ctx.getInvokeAction();
       
           } catch (OidcException.Protocol e) {
              _oidcSesMgr.removeContext(msg.uid());
              call.reply(OIDC.Message.createErrorMessage(msg.uid(), new OIDC.Error(OIDC.ErrCode.AUTHERROR)));
           } catch (OidcException.Validation e) {
               String addr = ctx.getConnection().getPeerAddress().toString();
               _log.error("OIDC Hash Validation failed for {}: {}", addr, e.getMessage());
               // Send RPC error and remove context
               _oidcSesMgr.removeContext(msg.uid());
               call.reply(OIDC.Message.createErrorMessage(msg.uid(), new OIDC.Error(OIDC.ErrCode.AUTHERROR)));
           } catch (Exception e) {
             _log.error("Unexpected error in HashMessage processing", e);
           }
 
           return ctx.getStopAction();
         },

         (OIDC.ErrorMessage msg, Object... params) -> {
           OidcSessionManager oidcSesMgr = OIDC.Params.get(params, 0, OidcSessionManager.class);
           FilterChainContext ctx = OIDC.Params.get(params, 1, FilterChainContext.class);

           return ctx.getInvokeAction();
         }
    );

    @Override
    public NextAction handleRead(FilterChainContext ctx) throws java.io.IOException {
        RpcCall call = ctx.getMessage();
        if (call.getCredential().type() != RpcAuthType.RPCOIDC_SEC) {
          return ctx.getInvokeAction();
        }
        return ((AuthOidc) call.getCredential()).oidcMessage().accept(_protocolVisitor, _oidcSesMgr, ctx);
    }

    private byte[] generateHandle() {
        UUID uuid = UUID.randomUUID();
        byte[] handle = new byte[16];
        Bytes.putLong(handle, 0, uuid.getLeastSignificantBits());
        Bytes.putLong(handle, 8, uuid.getMostSignificantBits());
        return handle;
    }

    private NextAction performVerification(FilterChainContext ctx,
                                           OidcSvcCtx oidcSvcCtx,
                                           RpcCall call) throws Exception {
        try {
            String token = oidcSvcCtx.oidcToken();
            DecodedJWT jwt = JWT.decode(token);
            // Verify Signature
            try {
                RSAPublicKey publicKey = (RSAPublicKey) _jwkProvider.get(jwt.getKeyId()).getPublicKey();
                JWT.require(Algorithm.RSA256(publicKey, null))
                    .withIssuer(_config.iss())
                    .withAudience(_config.aud())
                    .acceptLeeway(_config.timeSkewExp())// default time window
                    .acceptExpiresAt(_config.timeSkewExp()) // This line effectively skips expiration checks
                    .acceptNotBefore(_config.timeSkewNbf())
                    .build()
                    .verify(jwt);
            } catch (Exception e) {
                // Throw Signature specific exception
                throw new OidcException.Signature("JWT signature verification failed: " + e.getMessage(), e);
            }
            // Execute Visitor over claims
            try {
                for (VisitableClaim vc : _config.registry()) {
                    vc.accept(jwt);
                }
            } catch (Exception e) {
                // Throw Validation specific exception
                throw new OidcException.Validation("Claim validation failed: " + e.getMessage());         
            }
        
            // Success
            //ctx.getAttributes().setAttribute("OIDC_SUBJECT", jwt.getSubject());

            //TODO: generate proper hash
            oidcSvcCtx.hash(111);
            call.reply(OIDC.Message.createHashMessage(oidcSvcCtx.uid(), new OIDC.Hash(oidcSvcCtx.hash())));
            
            return ctx.getInvokeAction();
        } catch (OidcException.Signature | OidcException.Validation e) {
            // These are caught by the main filter catch block to handle rejection
            throw e; 
        } catch (Exception e) {
            throw new OidcException.Protocol("Unexpected error during verification phase: " + e.getMessage());
        }
    }

    private void reject(RpcCall call, long uid) {
        call.reply(OIDC.Message.createErrorMessage(uid, new OIDC.Error(OIDC.ErrCode.AUTHERROR)));
        _oidcSesMgr.removeContext(uid);
    }
}
