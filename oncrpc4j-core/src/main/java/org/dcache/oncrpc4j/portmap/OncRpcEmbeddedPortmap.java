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

import org.dcache.oncrpc4j.rpc.net.IpProtocolType;
import org.dcache.oncrpc4j.rpc.OncRpcClient;
import org.dcache.oncrpc4j.rpc.OncRpcException;
import org.dcache.oncrpc4j.rpc.OncRpcProgram;
import org.dcache.oncrpc4j.rpc.OncRpcSvc;
import org.dcache.oncrpc4j.rpc.OncRpcSvcBuilder;
import org.dcache.oncrpc4j.rpc.RpcAuth;
import org.dcache.oncrpc4j.rpc.RpcAuthTypeNone;
import org.dcache.oncrpc4j.rpc.RpcCall;
import org.dcache.oncrpc4j.xdr.XdrVoid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.dcache.oncrpc4j.rpc.RpcTransport;

/**
 * An instance of this class will create an embedded rpc portmap
 * service if OS does not provides one.
 */
public class OncRpcEmbeddedPortmap {

    private static final Logger LOG = LoggerFactory.getLogger(OncRpcEmbeddedPortmap.class);

    private static final RpcAuth _auth = new RpcAuthTypeNone();
    private OncRpcSvc optionalEmbeddedServer = null;

    public OncRpcEmbeddedPortmap() {
        this(2, TimeUnit.SECONDS);
    }

    public OncRpcEmbeddedPortmap(long timeoutValue, TimeUnit timeoutUnit) {

        // we start embedded portmap only if there no other one is running
        boolean localPortmapperRunning = false;
        try(OncRpcClient rpcClient = new OncRpcClient(InetAddress.getByName(null), IpProtocolType.UDP, OncRpcPortmap.PORTMAP_PORT)) {

            RpcTransport transport = rpcClient.connect();
            /* check for version 2, 3 and 4 */
            for (int i = 4; i > 1 && !localPortmapperRunning; i--) {
                RpcCall call = new RpcCall(OncRpcPortmap.PORTMAP_PROGRAMM, i, _auth, transport);
                try {
                    call.call(0, XdrVoid.XDR_VOID, XdrVoid.XDR_VOID, timeoutValue, timeoutUnit);
                    localPortmapperRunning = true;
                    LOG.info("Local portmap service v{} detected", i);
                } catch (TimeoutException | OncRpcException e) {
                    LOG.debug("portmap ping failed: {}", e.getMessage());
                }

            }
        } catch (IOException e) {
        }

        if (!localPortmapperRunning) {
            try {
                LOG.info("Starting embedded portmap service");
                OncRpcSvc rpcbindServer = new OncRpcSvcBuilder()
                        .withPort(OncRpcPortmap.PORTMAP_PORT)
                        .withTCP()
                        .withUDP()
                        .withoutAutoPublish()
                        .withRpcService(new OncRpcProgram(OncRpcPortmap.PORTMAP_PROGRAMM, OncRpcPortmap.PORTMAP_V2), new OncRpcbindServer())
                        .build();
                rpcbindServer.start();
                optionalEmbeddedServer = rpcbindServer;
            } catch (IOException e) {
                LOG.error("Failed to start embedded portmap service: {}", e.getMessage());
            }
        }
    }
	
	/**
	 * Check if running Embedded <tt>portmap</tt> service (for JUnit assume)
	 * @return if embedded port mapper
	 */
	public boolean isEmbeddedPortmapper() {
		return (optionalEmbeddedServer!=null);
	}
    /**
     * Shutdown embedded <tt>portmap</tt> service if running.
     * @throws IOException
     */
    public void shutdown() throws IOException {
        if (optionalEmbeddedServer != null) {
            optionalEmbeddedServer.stop();
        }
    }


}
