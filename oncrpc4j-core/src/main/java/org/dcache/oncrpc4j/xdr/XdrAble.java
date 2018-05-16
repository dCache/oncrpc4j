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

/**
 * Defines the interface for all classes that should be able to be
 * serialized into XDR streams, and deserialized or constructed from
 * XDR streams.
 */
public interface XdrAble {

    /**
     * Decodes -- that is: deserializes -- an object from a XDR stream in
     * compliance to RFC 1832.
     *
     * @param xdr XDR stream from which decoded information is retrieved.
     *
     * @throws OncRpcException if an ONC/RPC error occurs.
     */
    public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException;

    /**
     * Encodes -- that is: serializes -- an object into a XDR object in
     * compliance to RFC 1832.
     *
     * @return the XDR representation of this object.
     * @throws OncRpcException if an ONC/RPC error occurs.
     */
    public default Xdr xdrEncode() throws OncRpcException, IOException {
        Xdr xdr = new Xdr(Xdr.INITIAL_XDR_SIZE);
        xdr.beginEncoding();
        xdrEncode(xdr);
        xdr.endEncoding();
        return xdr;
    }

    /**
     * Encodes -- that is: serializes -- an object into a XDR stream in
     * compliance to RFC 1832.
     *
     * @param xdr XDR stream to which information is sent for encoding.
     * @throws OncRpcException if an ONC/RPC error occurs.
     */
    public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException;
}
