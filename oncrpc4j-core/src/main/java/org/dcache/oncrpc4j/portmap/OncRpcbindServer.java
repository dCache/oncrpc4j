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
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import javax.security.auth.kerberos.KerberosPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.dcache.auth.UidPrincipal;
import org.dcache.oncrpc4j.rpc.OncRpcException;
import org.dcache.oncrpc4j.rpc.RpcCall;
import org.dcache.oncrpc4j.rpc.RpcDispatchable;
import org.dcache.oncrpc4j.rpc.RpcAuthType;
import org.dcache.oncrpc4j.rpc.net.IpProtocolType;
import org.dcache.oncrpc4j.rpc.net.netid;
import org.dcache.oncrpc4j.xdr.XdrBoolean;
import org.dcache.oncrpc4j.xdr.XdrVoid;


public class OncRpcbindServer implements RpcDispatchable {
	static final ArrayList<String> v2NetIDs = new ArrayList<String>() {{
		add("tcp");
		add("udp");
	}};

    private final static Logger _log = LoggerFactory.getLogger(OncRpcbindServer.class);
    private final static String SERVICE_OWNER_UNSPECIFIED = "unspecified";
    private final static String SERVICE_OWNER_SUPER = "superuser";
    /**
     * Set of registered services.
     */
    private final Set<rpcb> _services = new HashSet<>();

    public OncRpcbindServer() {
        _services.add(new rpcb(OncRpcPortmap.PORTMAP_PROGRAMM, OncRpcPortmap.PORTMAP_V2, "tcp", "0.0.0.0.0.111", SERVICE_OWNER_SUPER));
        _services.add(new rpcb(OncRpcPortmap.PORTMAP_PROGRAMM, OncRpcPortmap.PORTMAP_V2, "udp", "0.0.0.0.0.111", SERVICE_OWNER_SUPER));
        //_services.add(new rpcb(100000, 4, "tcp", "0.0.0.0.0.111", "superuser"));
    }
    public void dispatchOncRpcCall(RpcCall call) throws OncRpcException, IOException {
        int version = call.getProgramVersion();

        switch(version) {
            case 2:
                processV2Call(call);
                break;
            case 3:
            case 4:
//                break;
            default:
                call.failProgramMismatch(2, 4);
        }
    }

    private void processV2Call(RpcCall call) throws OncRpcException, IOException {
        switch(call.getProcedure()) {
            case OncRpcPortmap.PMAPPROC_NULL:
                call.reply(XdrVoid.XDR_VOID);
                break;
            case OncRpcPortmap.PMAPPROC_SET:
                mapping newMapping = new mapping();
                call.retrieveCall(newMapping);
                // we store every thing in v4 format
                rpcb rpcbMapping = new rpcb(newMapping.getProg(),
                        newMapping.getVers(),
                        IpProtocolType.toString(newMapping.getProt()),
                        netid.toString(newMapping.getPort()),
                        SERVICE_OWNER_UNSPECIFIED
                );
				Boolean found = false;
                synchronized(_services) {
					for ( rpcb c : _services ) {
						if ( c.getProg() == rpcbMapping.getProg() &&  c.getVers() == rpcbMapping.getVers() && c.getNetid().equals(rpcbMapping.getNetid()) ) {
							found = true;
						}
					}
					if ( found == false) { // only add if not found already
						_services.add(rpcbMapping);
					}
                }
                call.reply( (found?XdrBoolean.False:XdrBoolean.True) );
                break;
            case OncRpcPortmap.PMAPPROC_UNSET:
                mapping unsetMapping = new mapping();
                call.retrieveCall(unsetMapping);
                // we sore everything in v4 format
                rpcb rpcbUnsetMapping = new rpcb(unsetMapping.getProg(),
                        unsetMapping.getVers(),
                        IpProtocolType.toString(unsetMapping.getProt()),
                        netid.toString(unsetMapping.getPort()),
                        getOwner(call)
                );
				Boolean removed = false;
                synchronized(_services) {
					Set<rpcb> target = new HashSet<>();
					// lookup entries
					for ( rpcb c : _services ) {
						if ( c.getProg() == rpcbUnsetMapping.getProg() &&  c.getVers() == rpcbUnsetMapping.getVers() && c.getOwner().equals(rpcbUnsetMapping.getOwner()) ) {
							target.add(c);
						}
					}
					// clear entries
					for ( rpcb c: target ) {
						_services.remove(c);
						removed = true;
					}
                }
                call.reply( (removed?XdrBoolean.True:XdrBoolean.False) );
                break;
            case OncRpcPortmap.PMAPPROC_DUMP:
                pmaplist list = new pmaplist();
                pmaplist next = list;
                synchronized(_services) {
                    for(rpcb m: _services) {
                        if (!v2NetIDs.contains(m.getNetid())) { // skip netid's which are not v2
                            continue;
                        }
                        next.setEntry(new mapping(m.getProg(), m.getVers(), netid.idOf(m.getNetid()), netid.getPort(m.getAddr())));
                        pmaplist n = new pmaplist();
                        next.setNext(n);
                        next = n;
                    }
                }
                call.reply(list);
                break;
            case OncRpcPortmap.PMAPPROC_GETPORT:
                mapping query = new mapping();
                call.retrieveCall(query);
                rpcb result = search(new rpcb(query.getProg(),
                        query.getVers(),
                        IpProtocolType.toString(query.getProt()),
                        netid.toString(query.getPort()),
                        getOwner(call)));
                Port port;
                if(result == null) {
                    port = new Port(0);
                }else{
                    port = new Port(netid.getPort(result.getAddr()));
                }
                call.reply(port);
                break;
            default:
                call.failProcedureUnavailable();
        }
    }

    private rpcb search(rpcb query) {
        synchronized(_services) {
            for( rpcb e: _services) {
                if( e.match(query)) return e;
            }
        }
        return null;
    }

    /*
     * As we can't trust client, then:
     *   - check for privilege port
     *   - check for kerberos principal or numeric uid
     *   - everything else - unspecified.
     */
    private String getOwner(RpcCall call) {

        if (call.getTransport().getRemoteSocketAddress().getPort() < 1024) {
            return SERVICE_OWNER_SUPER;
        }

        Predicate<Principal> filter;
        switch (call.getCredential().type()) {
            case RpcAuthType.RPCGSS_SEC:
                filter = p -> p.getClass() == KerberosPrincipal.class;
                break;
            case RpcAuthType.UNIX:
                filter = p -> p.getClass() == UidPrincipal.class;
                break;
            default:
                filter = p -> false;
        }

        return call.getCredential()
                .getSubject()
                .getPrincipals()
                .stream()
                .filter(filter)
                .findFirst()
                .map(Principal::getName)
                .orElse(SERVICE_OWNER_UNSPECIFIED);
    }
}
