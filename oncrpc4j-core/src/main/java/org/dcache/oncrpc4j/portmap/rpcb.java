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

/**
 *
 * A mapping of (program, version, network ID) to address.
 */
public class rpcb implements XdrAble {

    /**
     * program number
     */
    private int _prog;
    /**
     * version number
     */
    private int _vers;
    /**
     * network id
     */
    private String _netid;
    /**
     * universal address
     */
    private String _addr;
    /**
     * owner of this service
     */
    private String _owner;

    public rpcb() {}

    public rpcb(int prog, int vers, String netid, String addr, String owner) {
        _prog = prog;
        _vers = vers;
        _netid = netid;
        _addr = addr;
        _owner = owner;
    }
	
	public int getProg() {
		return _prog;
	}
	
	public int getVers() {
		return _vers;
	}
	
	public String getNetid() {
		return _netid;
	}	

	public String getOwner() {
		return _owner;
	}	

    public String getAddr() {
        return _addr;
    }

    public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException {
        _prog = xdr.xdrDecodeInt();
        _vers = xdr.xdrDecodeInt();

        _netid = xdr.xdrDecodeString();
        _addr = xdr.xdrDecodeString();
        _owner = xdr.xdrDecodeString();

    }

    public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException {
        xdr.xdrEncodeInt(_prog);
        xdr.xdrEncodeInt(_vers);

        xdr.xdrEncodeString(_netid);
        xdr.xdrEncodeString(_addr);
        xdr.xdrEncodeString(_owner);
    }

    @Override
    public String toString() {
        return String.format("prog: %d, vers: %d, netid: %s, addr: %s, owner: %s",
                _prog, _vers, _netid, _addr, _owner);
    }

    boolean match(rpcb query) {
        return query._prog == _prog &&
                query._vers == _vers &&
                query._netid.equals(_netid);
    }
}
