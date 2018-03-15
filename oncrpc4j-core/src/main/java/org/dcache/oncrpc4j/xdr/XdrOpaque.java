/*
 * Copyright (c) 2009 - 2018 Deutsches Elektronen-Synchroton,
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
package org.dcache.oncrpc4j.xdr;

import org.dcache.oncrpc4j.rpc.OncRpcException;
import com.google.common.io.BaseEncoding;
import java.io.IOException;
import java.util.Arrays;

/**
 * A wrapper class that makes dynamic opaque data suitable for xdr encoding.
 * The wrapper provides equals and hashCode methods to make use in collections
 * more effective.
 */
public class XdrOpaque implements XdrAble {

    private byte[] _opaque;

    public XdrOpaque(byte[] opaque) {
        _opaque = opaque;
    }

    public XdrOpaque(XdrDecodingStream xdr) throws IOException {
        xdrDecode(xdr);
    }

    public byte[] getOpaque() {
        return _opaque;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(_opaque);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof XdrOpaque)) {
            return false;
        }

        return Arrays.equals(_opaque, ((XdrOpaque) o)._opaque);
    }

    @Override
    public String toString() {
        return  new StringBuilder()
            .append('[')
            .append(BaseEncoding.base16().upperCase().encode(_opaque))
            .append(']')
            .toString();
    }

    @Override
    public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException {
        _opaque = xdr.xdrDecodeDynamicOpaque();
    }

    @Override
    public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException {
        xdr.xdrEncodeDynamicOpaque(_opaque);
    }
}
