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
package org.dcache.oncrpc4j.portmap;

import java.io.IOException;
import org.dcache.oncrpc4j.rpc.OncRpcException;
import org.dcache.oncrpc4j.xdr.XdrAble;
import org.dcache.oncrpc4j.xdr.XdrDecodingStream;
import org.dcache.oncrpc4j.xdr.XdrEncodingStream;

/*
 * A list of mappings.
 */
public class rpcb_list implements XdrAble{

    private rpcb _rpcbMap;

    public void setNext(rpcb_list next) {
        _next = next;
    }

    public void setEntry(rpcb rpcbMap) {
        _rpcbMap = rpcbMap;
    }

	public rpcb getEntry() {
		return _rpcbMap;
	}
	
	public rpcb_list getNext() {
		return _next;
	}
		
    private rpcb_list _next;

    public rpcb_list() {}

    @Override
    public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException {
        boolean hasMap = xdr.xdrDecodeBoolean();
        if(hasMap) {
            _rpcbMap = new rpcb();
            _rpcbMap.xdrDecode(xdr);
            _next = new rpcb_list();
            _next.xdrDecode(xdr);
        }else{
            _rpcbMap = null;
        }
    }

    @Override
    public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException {

        if (_rpcbMap != null) {
            xdr.xdrEncodeBoolean(true);
           _rpcbMap.xdrEncode(xdr);
           _next.xdrEncode(xdr);
        }else{
            xdr.xdrEncodeBoolean(false);
        }
    }

    @Override
    public String toString() {
        return _rpcbMap + "\n" + (_next != null? _next: "");
    }
}
