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
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.dcache.oncrpc4j.rpc.net.IpProtocolType;
import org.dcache.oncrpc4j.rpc.OncRpcClient;
import org.dcache.oncrpc4j.rpc.OncRpcException;
import org.dcache.oncrpc4j.rpc.RpcAuth;
import org.dcache.oncrpc4j.rpc.RpcAuthTypeNone;
import org.dcache.oncrpc4j.rpc.RpcCall;
import org.dcache.oncrpc4j.rpc.RpcProgUnavailable;
import org.dcache.oncrpc4j.rpc.RpcTransport;

import static org.dcache.oncrpc4j.portmap.OncRpcPortmap.PORTMAP_PROGRAMM;
import static org.dcache.oncrpc4j.portmap.OncRpcPortmap.PORTMAP_V4;
import static org.dcache.oncrpc4j.portmap.OncRpcPortmap.PORTMAP_V2;

public class GenericPortmapClient implements OncPortmapClient {

    private final static Logger _log = LoggerFactory.getLogger(GenericPortmapClient.class);
    private final RpcAuth _auth = new RpcAuthTypeNone();
    private final OncPortmapClient _portmapClient;

    public GenericPortmapClient(RpcTransport transport) throws RpcProgUnavailable {

       OncPortmapClient portmapClient = new RpcbindV4Client(new RpcCall(PORTMAP_PROGRAMM, PORTMAP_V4, _auth, transport));
        if( !portmapClient.ping() ) {
            portmapClient = new PortmapV2Client( new RpcCall(PORTMAP_PROGRAMM, PORTMAP_V2, _auth, transport) );
            if(!portmapClient.ping()) {
                // FIXME: return correct exception
                throw new RpcProgUnavailable("portmap service not available");
            }
            _log.debug("Using portmap V2");
        }
        _portmapClient = portmapClient;
    }

    public List<rpcb> dump() throws OncRpcException, IOException, TimeoutException {
        return _portmapClient.dump();
    }

    public boolean ping() {
        return _portmapClient.ping();
    }

    public boolean setPort(int program, int version, String netid, String addr, String owner) throws OncRpcException, IOException, TimeoutException {
        return _portmapClient.setPort(program, version, netid, addr, owner);
    }

    public boolean unsetPort(int program, int version, String owner) throws OncRpcException, IOException, TimeoutException {
        return _portmapClient.unsetPort(program, version, owner);
    }

    public String getPort(int program, int version, String netid) throws OncRpcException, IOException, TimeoutException {
        return _portmapClient.getPort(program, version, netid);
    }

    public static void main(String[] args) throws InterruptedException, IOException, OncRpcException, TimeoutException {

        int protocol = IpProtocolType.TCP;

        OncRpcClient rpcClient = new OncRpcClient(InetAddress.getByName(null), IpProtocolType.UDP, 111);
        RpcTransport transport = rpcClient.connect();

        OncPortmapClient portmapClient = new GenericPortmapClient(transport);

        try {

            int prog = 100009;
            int vers = 4;
            String netid = IpProtocolType.toString(protocol);
            String user = System.getProperty("user.name");
            String addr = "127.0.0.1.8.4";
            /*
             * check for V4
             */
            portmapClient.ping();
            System.out.println(portmapClient.setPort(prog, vers, netid, addr, user));
            System.out.println("getport: " + portmapClient.getPort(prog, vers, netid));
            System.out.println("-------");

      //      System.out.println(portmapClient.unsetPort(prog, vers, user));
            System.out.println("getport: " + portmapClient.getPort(prog, vers, netid));

        } finally {
            rpcClient.close();
        }
    }
}
