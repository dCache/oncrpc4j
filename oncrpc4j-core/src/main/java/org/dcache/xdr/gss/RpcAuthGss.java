/*
 * Copyright (c) 2009 - 2014 Deutsches Elektronen-Synchroton,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program (see the file COPYING.LIB for more
 * details); if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.dcache.xdr.gss;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.security.auth.Subject;
import org.dcache.xdr.OncRpcException;
import org.dcache.xdr.RpcAuth;
import org.dcache.xdr.RpcAuthError;
import org.dcache.xdr.RpcAuthException;
import org.dcache.xdr.RpcAuthStat;
import org.dcache.xdr.RpcAuthType;
import org.dcache.xdr.RpcAuthVerifier;
import org.dcache.xdr.Xdr;
import org.dcache.xdr.XdrAble;
import org.dcache.xdr.XdrDecodingStream;
import org.dcache.xdr.XdrEncodingStream;
import org.glassfish.grizzly.Buffer;

public class RpcAuthGss implements RpcAuth, XdrAble {

    private final static Logger _log = LoggerFactory.getLogger(RpcAuthGss.class);

    private final int _type = RpcAuthType.RPCGSS_SEC;
    private RpcAuthVerifier _verifier = new RpcAuthVerifier(_type, new byte[0]);
    private int _version;
    private int _proc;
    private int _sequence;
    private int _service;
    private byte[] _handle;
    private Buffer _header;

    private Subject _subject = new Subject();

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
    Buffer getHeader() {
        return _header.asReadOnlyBuffer();
    }

    public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException {
        int len = xdr.xdrDecodeInt();
        _header = ((Xdr) xdr).asBuffer().duplicate();

        /*
         * header size is RPC header + credential.
         *
         * rpc header is 7 int32: xid type rpcversion prog vers proc auth_flavour
         * credential is 1 int32 + it's value : len + opaque
         *
         * set position to the beginning of rpc message and limit to the end of credential.
         */
        _header.limit( _header.position() + len);
        _header.position( _header.position() - 8*4);

        _version = xdr.xdrDecodeInt();
        if (_version < RpcGssVersion.RPCSEC_GSS_VERS_1 || _version > RpcGssVersion.RPCSEC_GSS_VERS_3) {
            throw new RpcAuthException("Unsupported RPCSEC_GSS version: " + _version,
                    new RpcAuthError(RpcAuthStat.AUTH_REJECTEDCRED));
        }
        _proc = xdr.xdrDecodeInt();
        _sequence = xdr.xdrDecodeInt();
        _service = xdr.xdrDecodeInt();
        _handle = xdr.xdrDecodeDynamicOpaque();
        _verifier.xdrDecode(xdr);
    }

    public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException {
        xdr.xdrEncodeInt(_type);

        _verifier.xdrEncode(xdr);
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
