/*
 * Copyright (c) 2019 Deutsches Elektronen-Synchroton,
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
import javax.security.auth.Subject;
import org.dcache.oncrpc4j.xdr.XdrAble;
import org.dcache.oncrpc4j.xdr.XdrDecodingStream;
import org.dcache.oncrpc4j.xdr.XdrEncodingStream;

import org.dcache.auth.Subjects;

/**
 *
 */
public class RpcAuthTypeTls implements RpcAuth, XdrAble {

    private final RpcAuthVerifier verifier = new RpcAuthVerifier(RpcAuthType.NONE, new byte[0]);

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
        return Subjects.NOBODY;
    }

    @Override
    public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException {
        byte[] opaque = xdr.xdrDecodeDynamicOpaque();
        verifier.xdrDecode(xdr);
    }

    @Override
    public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException {
            xdr.xdrEncodeInt(type());
            xdr.xdrEncodeInt(0); // spec: credential size must be zero
            verifier.xdrEncode(xdr);
    }

}
