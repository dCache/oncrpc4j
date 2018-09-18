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

public class pmaplist implements XdrAble {
    private mapping _mapping;
    private pmaplist _next;

    public pmaplist() {}

    public void setEntry(mapping mapping) {
        _mapping = mapping;
    }

    public void setNext(pmaplist next) {
        _next = next;
    }
	
	public mapping getEntry() {
		return _mapping;
	}
	
	public pmaplist getNext() {
		return _next;
	}
	
    public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException {
         boolean hasMap = xdr.xdrDecodeBoolean();
         if(hasMap) {
             _mapping = new mapping();
             _mapping.xdrDecode(xdr);
             _next = new pmaplist();
             _next.xdrDecode(xdr);
         }else{
             _mapping = null;
         }
    }

    public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException {
        if (_mapping != null) {
            xdr.xdrEncodeBoolean(true);
            _mapping.xdrEncode(xdr);
            _next.xdrEncode(xdr);
        } else {
            xdr.xdrEncodeBoolean(false);
        }
    }

    @Override
    public String toString() {
        return _mapping + "\n" + (_next != null ? _next : "");
    }
}
