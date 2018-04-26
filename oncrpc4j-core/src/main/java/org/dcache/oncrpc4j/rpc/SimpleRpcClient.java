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
package org.dcache.oncrpc4j.rpc;

import org.dcache.oncrpc4j.rpc.net.IpProtocolType;
import org.dcache.oncrpc4j.xdr.XdrVoid;
import java.net.InetAddress;
import java.util.concurrent.Future;

public class SimpleRpcClient {

    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.err.println("usage: SimpleRpcClient host port");
            System.exit(1);
        }

        InetAddress address = InetAddress.getByName(args[0]);
        int port = Integer.parseInt(args[1]);

	try (OncRpcClient rpcClient = new OncRpcClient(address, IpProtocolType.TCP, port)) {
	    RpcTransport transport = rpcClient.connect();
	    RpcAuth auth = new RpcAuthTypeNone();

	    RpcCall call = new RpcCall(100017, 1, auth, transport);

	    /*
	    * call PROC_NULL (ping)
	    */
	    call.call(0, XdrVoid.XDR_VOID, XdrVoid.XDR_VOID);

	    Future<XdrVoid> r = call.call(0, XdrVoid.XDR_VOID, XdrVoid.class);
	    r.get();
	}
    }
}
