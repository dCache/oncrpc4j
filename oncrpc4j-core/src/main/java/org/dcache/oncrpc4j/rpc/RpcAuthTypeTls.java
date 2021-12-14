/*
 * Copyright (c) 2019 - 2021 Deutsches Elektronen-Synchroton,
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
package org.dcache.oncrpc4j.rpc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.security.auth.Subject;

import org.dcache.oncrpc4j.xdr.XdrAble;
import org.dcache.oncrpc4j.xdr.XdrDecodingStream;
import org.dcache.oncrpc4j.xdr.XdrEncodingStream;


/**
 *
 */
public class RpcAuthTypeTls implements RpcAuth, XdrAble {

    public static final RpcAuthVerifier STARTTLS_VERIFIER = new RpcAuthVerifier(RpcAuthType.NONE, "STARTTLS".getBytes(StandardCharsets.US_ASCII));
    public static final RpcAuthVerifier EMPTY_VERIFIER = new RpcAuthVerifier(RpcAuthType.NONE, new byte[0]);

    private final RpcAuthVerifier verifier;
    private final Subject _subject;

    public RpcAuthTypeTls() {
        this(EMPTY_VERIFIER);
    }

    public RpcAuthTypeTls(RpcAuthVerifier verifier) {
        _subject = new Subject();
        _subject.setReadOnly();
        this.verifier = verifier;
    }

    @Override
    public int type() {
        return RpcAuthType.TLS;
    }

    @Override
    public RpcAuthVerifier getVerifier() {
        return verifier;
    }

    @Override
    public Subject getSubject() {
        return _subject;
    }

    @Override
    public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException {

        byte[] opaque = xdr.xdrDecodeDynamicOpaque();

        // we are not interested in the content of the verifier, but have to consume it
        int type = xdr.xdrDecodeInt();
        byte[] rawVerifier = xdr.xdrDecodeDynamicOpaque();
    }

    @Override
    public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException {
            xdr.xdrEncodeInt(type());
            xdr.xdrEncodeInt(0); // spec: credential size must be zero
            verifier.xdrEncode(xdr);
    }

}
