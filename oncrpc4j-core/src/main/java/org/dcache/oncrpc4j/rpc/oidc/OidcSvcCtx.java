package org.dcache.oncrpc4j.rpc.oidc;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class OidcSvcCtx {
    // 16KB is a generous limit for most JWTs. 
    // Adjust based on your IdP (e.g., Keycloak tokens with many roles can be large).
    private static final int MAX_TOKEN_SIZE = 16384; 

    private long _uid = 0;
    private final ByteArrayOutputStream _tokenBuffer = new ByteArrayOutputStream();
    private long _tokenLen = 0; 
    private long _hash = 0;
    private final long _createdAt;

    public OidcSvcCtx() {
        _createdAt = System.currentTimeMillis();
    }

    /**
     * Appends a chunk of bytes to the internal buffer with a size guard.
     */
    public OidcSvcCtx appendToken(byte[] chunk) {
        if (chunk != null && chunk.length > 0) {
            // Enforcement: Prevent buffer overflow attacks
            if (_tokenBuffer.size() + chunk.length > MAX_TOKEN_SIZE) {
                // Use specific protocol exception instead of generic IllegalState
                throw new OidcException.Protocol("OIDC token size limit exceeded");
            }
            _tokenBuffer.write(chunk, 0, chunk.length);
        }
        return this;
    }
 
    public long uid() {
        return _uid;
    }

    public OidcSvcCtx uid(long uid) {
        this._uid = uid;
        return this;
    }

    /**
     * Converts bytes to String for JWT decoding.
     */
    public String oidcToken() {
        return _tokenBuffer.toString(StandardCharsets.UTF_8);
    }

    public long tokenLen() {
        return _tokenLen;
    }

    public OidcSvcCtx tokenLen(long tokenLen) {
        this._tokenLen = tokenLen;
        return this;
    }

    public long bytesReceived() {
        return _tokenBuffer.size();
    }

    public long hash() {
        return _hash;
    }

    public OidcSvcCtx hash(long hash) {
        this._hash = hash;
        return this;
    }

    public long createdAt() {
        return _createdAt;
    }

    public boolean isComplete() {
        return _tokenLen > 0 && bytesReceived() >= _tokenLen;
    }
}

