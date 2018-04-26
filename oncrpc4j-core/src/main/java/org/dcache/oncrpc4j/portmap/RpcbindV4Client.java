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

import org.dcache.oncrpc4j.rpc.OncRpcException;
import org.dcache.oncrpc4j.rpc.RpcAuth;
import org.dcache.oncrpc4j.rpc.RpcAuthTypeNone;
import org.dcache.oncrpc4j.rpc.RpcCall;
import org.dcache.oncrpc4j.xdr.XdrBoolean;
import org.dcache.oncrpc4j.xdr.XdrString;
import org.dcache.oncrpc4j.xdr.XdrVoid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RpcbindV4Client implements OncPortmapClient {

    private final static Logger _log = LoggerFactory.getLogger(RpcbindV4Client.class);

    private final RpcAuth _auth = new RpcAuthTypeNone();
    private final RpcCall _call;

    public RpcbindV4Client(RpcCall call) {
        _call = call;
    }

    public boolean ping() {

        _log.debug("portmap ping");
        boolean pong = false;

        try {
            _call.call(OncRpcPortmap.PMAPPROC_NULL, XdrVoid.XDR_VOID, XdrVoid.XDR_VOID, 2, TimeUnit.SECONDS);
            pong = true;
        }catch(IOException | TimeoutException e) {
        }

        return pong;
    }

    public boolean setPort(int program, int version, String netid, String addr, String owner)
            throws OncRpcException, IOException, TimeoutException {

        _log.debug("portmap set port: prog: {} vers: {}, netid: {} addr: {}, owner: {}",
                new Object[] {program, version, netid, addr, owner});

        rpcb m1 = new rpcb(program, version, netid, addr, owner);

        XdrBoolean isSet = new XdrBoolean();
        _call.call(OncRpcPortmap.RPCBPROC_SET, m1, isSet);
        return isSet.booleanValue();

    }

    @Override
    public boolean unsetPort(int program, int version, String owner)
            throws OncRpcException, IOException, TimeoutException {

        _log.debug("portmap unset port: prog: {} vers: {}, owner: {}",
                new Object[]{program, version, owner});

        rpcb m = new rpcb(program, version, "", "", owner);

        XdrBoolean isSet = new XdrBoolean();
        _call.call(OncRpcPortmap.RPCBPROC_UNSET, m, isSet);
        return isSet.booleanValue();

    }

    @Override
    public String getPort(int program, int version, String netid)
            throws OncRpcException, IOException, TimeoutException {
        rpcb arg = new rpcb(program, version, netid, "", "");
        XdrString xdrString = new XdrString();
        _call.call(OncRpcPortmap.RPCBPROC_GETADDR, arg, xdrString);
        return xdrString.stringValue();
    }

    public List<rpcb> dump() throws OncRpcException, IOException, TimeoutException {
        _log.debug("portmap dump");
        rpcb_list rpcb_list_reply = new rpcb_list();
        _call.call(OncRpcPortmap.RPCBPROC_DUMP, XdrVoid.XDR_VOID, rpcb_list_reply);
		List<rpcb> out = new LinkedList<>();
		// walk entries and add to list
		out.add(rpcb_list_reply.getEntry());
		while( (rpcb_list_reply=rpcb_list_reply.getNext()) != null ) {
			rpcb c = rpcb_list_reply.getEntry();
			if ( c != null ) {
				out.add(c);
			}
		}
		return out;
    }
}
