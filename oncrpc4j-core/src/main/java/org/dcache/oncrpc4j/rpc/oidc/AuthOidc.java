package org.dcache.oncrpc4j.rpc.oidc;


import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.security.auth.Subject;

import org.dcache.oncrpc4j.rpc.OncRpcException;
import org.dcache.oncrpc4j.rpc.RpcAuth;

import org.dcache.oncrpc4j.rpc.RpcAuthException;

import org.dcache.oncrpc4j.rpc.RpcAuthType;
import org.dcache.oncrpc4j.rpc.RpcAuthVerifier;
import org.dcache.oncrpc4j.xdr.*;
import org.dcache.oncrpc4j.rpc.RpcTransport;

import org.glassfish.grizzly.Buffer;

public class AuthOidc implements RpcAuth {
    private final static Logger _log = LoggerFactory.getLogger(AuthOidc.class);

    private final int _type = RpcAuthType.RPCOIDC_SEC;
    private RpcAuthVerifier _verifier = new RpcAuthVerifier(_type, new byte[0]);
    private Subject _subject = new Subject();
    private int _version;
    private int _proc;
    private int _sequence;
    private int _service;
    private byte[] _handle;
    private Buffer _header;

    private class CTX {
        String _token;
        long _tokenLen;
        long _bytesSent;
        long _uid;
        OIDC.Hash _hash;

        public CTX() {
            _token = "";
            _tokenLen = 0;
            _bytesSent = 0;
            _uid = 0;
            _hash = new OIDC.Hash(0);       
        } 
    }

    private CTX _ctx;
    private OIDC.Message _oidcMessage;

    public AuthOidc() {
        _ctx = new CTX();
    }

    public AuthOidc(final String oidcToken) {
        _ctx = new CTX();
        _ctx._token = oidcToken;
        _ctx._tokenLen = oidcToken.length();
    }
    
    public OIDC.Message oidcMessage() {
        return _oidcMessage;
    }

    public String oidcToken() {
      //return _oidcToken;
      return _ctx._token;
    } 

    public OIDC.Hash hash() {
      return _ctx._hash;
    } 

    public void uid(long uid) {
      _ctx._uid = uid;
    }

    public byte[] getHandle() {
        return _handle;
    }

    public void setHandle(byte[] handle) {
        _handle = handle;
    }

    public int getProc() {
        return _proc;
    }

    public void setProc(int proc) {
        _proc = proc;
    }

    public int getService() {
        return _service;
    }

    public void setService(int svc) {
        _service = svc;
    }

    public int getVersion() {
        return _version;
    }

    public void setVersion(int version) {
        _version = version;
    }

    @Override
    public Subject getSubject() {
        return _subject;
    }

    @Override
    public int type() {
        return _type;
    }

    @Override
    public RpcAuthVerifier getVerifier() {
        return _verifier;
    }

    public void setVerifier(RpcAuthVerifier verifier) {
        _verifier = verifier;
    }

    public int getSequence() {
        return _sequence;
    }

    /**
     * Get a read-only ByteBuffer containing RPC header including credential.
     */
    ByteBuffer getHeader() {
        return _header.asReadOnlyBuffer().toByteBuffer();
    }

    @Override
    public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException {
        int len = xdr.xdrDecodeInt(); // Length
 
        Xdr tmpxdr = new Xdr(xdr.xdrDecodeOpaque(len));
        tmpxdr.beginDecoding();

        _oidcMessage = OIDC.Message.xdrCreate(tmpxdr);

        tmpxdr.beginDecoding();

        _ctx._uid = _oidcMessage._uid;
        _verifier = new RpcAuthVerifier(xdr); // none
    }

    @Override
    public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException {
        
        OIDC.Message oidcMessage = null;

        // From tirpc: MAX_AUTH_BYTES	400
        Xdr tmpxdr = new Xdr(400);

        xdr.xdrEncodeInt(_type);
        long remaining = _ctx._tokenLen - _ctx._bytesSent;


        if (_ctx._hash.value() == 0 && remaining > 0) {
            final int MAX_OIDC_CHUNK_SIZE = 2;//1024; 
           
            // Casting to int as substring has int params
            // TODO: considering - change _tokenLen and _bytesSent to int in the tirpc and message
            int len = (int)(Math.min(remaining, MAX_OIDC_CHUNK_SIZE));
            int iSIdx = (int)(_ctx._bytesSent);
            int iEIdx = (int)(_ctx._bytesSent + len);

            OIDC.Chunk chunk = new OIDC.Chunk(
                    (remaining <= MAX_OIDC_CHUNK_SIZE) ? OIDC.ChunkState.END : (_ctx._bytesSent == 0) ? OIDC.ChunkState.INIT : OIDC.ChunkState.DATA,
                     len,
                    _ctx._token.substring(iSIdx, iEIdx),
                    _ctx._tokenLen
                );
  
            oidcMessage = OIDC.Message.createChunkMessage(
                    _ctx._uid,
                    chunk
                );

            _ctx._bytesSent += len;

        } else {
            // Hash message
            oidcMessage = OIDC.Message.createHashMessage(
                    _ctx._uid,
                    _ctx._hash
                );
        }
        
        if ( oidcMessage != null) {
            tmpxdr.beginEncoding();
            oidcMessage.xdrEncode(tmpxdr);
            tmpxdr.endEncoding();
        }
        xdr.xdrEncodeInt(tmpxdr.getBytes().length);
        xdr.xdrEncodeOpaque(tmpxdr.getBytes(), tmpxdr.getBytes().length);

        _verifier.xdrEncode(xdr);
    }
}

