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
import java.util.Objects;

public class XdrString implements XdrAble {

    private String _value;

    public XdrString() {
    }

    public XdrString(String value) {
        _value = value;
    }

    /**
     * Returns the value of this <code>XdrString</code> object as a {@code String}.
     */
    public String stringValue() {
        return _value;
    }

    public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException {
        _value = xdr.xdrDecodeString();
    }

    public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException {
        xdr.xdrEncodeString(_value);
    }

    @Override
    public String toString() {
        return _value;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this._value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final XdrString other = (XdrString) obj;
        return Objects.equals(this._value, other._value);
    }
}
