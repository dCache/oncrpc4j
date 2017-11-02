/*
 * Copyright (c) 2009 - 2016 Deutsches Elektronen-Synchroton,
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
package org.dcache.xdr.portmap;

import org.dcache.utils.net.InetSocketAddresses;
import org.dcache.xdr.OncRpcException;
import org.dcache.xdr.RpcCall;
import org.dcache.xdr.XdrBoolean;
import org.dcache.xdr.XdrInt;
import org.dcache.xdr.XdrVoid;
import org.dcache.xdr.netid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PortmapV2Client implements OncPortmapClient {

    private final static Logger _log = LoggerFactory.getLogger(PortmapV2Client.class);
    private final RpcCall _call;

    public PortmapV2Client(RpcCall call) {
        _call = call;
    }

    public List<rpcb> dump() throws OncRpcException, IOException, TimeoutException {
        _log.debug("portmap dump");
        pmaplist list_reply = new pmaplist();
        _call.call(OncRpcPortmap.PMAPPROC_DUMP, XdrVoid.XDR_VOID, list_reply);
		List<rpcb> out = new LinkedList<>();
		// walk entries and add to list
		do {
			mapping c = list_reply.getEntry();
			if ( c != null ) {
				out.add( new rpcb(c) );
			}
		}
		while( (list_reply = list_reply.getNext()) != null ); 
		return out;
    }

    public boolean ping() {
        _log.debug("portmap ping");
        boolean pong = false;
        try {
            _call.call(OncRpcPortmap.PMAPPROC_NULL, XdrVoid.XDR_VOID, XdrVoid.XDR_VOID, 2, TimeUnit.SECONDS);
            pong = true;
        }catch (TimeoutException | IOException e) {}

        return pong;
    }

    public boolean setPort(int program, int version, String netids, String addr, String owner)
            throws OncRpcException, IOException, TimeoutException {
        _log.debug("portmap set port: prog: {} vers: {}, netid: {} addr: {}, owner: {}",
                new Object[]{program, version, netids, addr, owner});

        int protocol = netid.idOf(netids);
        if (protocol == -1) {
            return false;
        }
        InetSocketAddress address = org.dcache.xdr.netid.toInetSocketAddress(addr);
        mapping m1 = new mapping(program, version, protocol, address.getPort(),owner);

        XdrBoolean isSet = new XdrBoolean();
        _call.call(OncRpcPortmap.PMAPPROC_SET, m1, isSet);

        return isSet.booleanValue();
    }

    @Override
    public boolean unsetPort(int program, int version, String owner)
            throws OncRpcException, IOException, TimeoutException {
        _log.debug("portmap unset port: prog: {} vers: {}, owner: {}",
                new Object[]{program, version, owner});

        mapping m = new mapping(program, version, 0, -1,owner);

        XdrBoolean isSet = new XdrBoolean();
        _call.call(OncRpcPortmap.PMAPPROC_UNSET, m, isSet);

        return isSet.booleanValue();
    }

    @Override
    public String getPort(int program, int version, String nid)
            throws OncRpcException, IOException, TimeoutException {

        mapping m = new mapping(program, version, netid.idOf(nid), 0);
        XdrInt port = new XdrInt();

        _call.call(OncRpcPortmap.PMAPPROC_GETPORT, m, port);
        return InetSocketAddresses.uaddrOf(_call.getTransport()
                .getRemoteSocketAddress()
                .getAddress()
                .getHostAddress()
            , port.intValue());
    }

}
