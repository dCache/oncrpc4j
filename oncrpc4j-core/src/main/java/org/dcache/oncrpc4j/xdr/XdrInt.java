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
import java.io.IOException;

public class XdrInt implements XdrAble {

    private int _value;

    public XdrInt() {
    }

    public XdrInt(int value) {
        _value = value;
    }

    /**
     * Returns the value of this <code>XdrInt</code> object as an {@code int}.
     */
    public int intValue() {
        return _value;
    }

    public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException {
        _value = xdr.xdrDecodeInt();
    }

    public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException {
        xdr.xdrEncodeInt(_value);
    }
}