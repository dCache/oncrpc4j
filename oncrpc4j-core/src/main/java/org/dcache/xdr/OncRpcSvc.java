/*
 * Copyright (c) 2009 - 2015 Deutsches Elektronen-Synchroton,
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
package org.dcache.xdr;

import org.dcache.utils.net.InetSocketAddresses;
import org.dcache.xdr.gss.GssProtocolFilter;
import org.dcache.xdr.gss.GssSessionManager;
import org.dcache.xdr.portmap.GenericPortmapClient;
import org.dcache.xdr.portmap.OncPortmapClient;
import org.dcache.xdr.portmap.OncRpcPortmap;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.PortRange;
import org.glassfish.grizzly.SocketBinder;
import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.jmxbase.GrizzlyJmxManager;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.nio.transport.UDPNIOTransport;
import org.glassfish.grizzly.nio.transport.UDPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.dcache.xdr.GrizzlyUtils.getSelectorPoolCfg;
import static org.dcache.xdr.GrizzlyUtils.rpcMessageReceiverFor;
import static org.dcache.xdr.GrizzlyUtils.transportFor;

public class OncRpcSvc {

    private final static Logger _log = LoggerFactory.getLogger(OncRpcSvc.class);

    private final int _backlog;
    private final boolean _publish;
    private final PortRange _portRange;
    private final String _bindAddress;
    private final List<Transport> _transports = new ArrayList<Transport>();
    private final Set<Connection<InetSocketAddress>> _boundConnections =
            new HashSet<Connection<InetSocketAddress>>();

    private final ExecutorService _requestExecutor;

    public enum IoStrategy {
        SAME_THREAD,
        WORKER_THREAD
    }

    private final ReplyQueue<Integer, RpcReply> _replyQueue = new ReplyQueue<Integer, RpcReply>();
    /**
     * Handle RPCSEC_GSS
     */
    private final GssSessionManager _gssSessionManager;

    /**
     * mapping of registered programs.
     */
    private final Map<OncRpcProgram, RpcDispatchable> _programs =
            new ConcurrentHashMap<OncRpcProgram, RpcDispatchable>();

    /**
     * Create new RPC service with defined configuration.
     * @param builder to build this service
     */
    OncRpcSvc(OncRpcSvcBuilder builder) {
        _publish = builder.isAutoPublish();
        final int protocol = builder.getProtocol();

        if ((protocol & (IpProtocolType.TCP | IpProtocolType.UDP)) == 0) {
            throw new IllegalArgumentException("TCP or UDP protocol have to be defined");
        }

        IoStrategy ioStrategy = builder.getIoStrategy();
        ThreadPoolConfig selectorPoolConfig = getSelectorPoolCfg(ioStrategy);

        if ((protocol & IpProtocolType.TCP) != 0) {
            final TCPNIOTransport tcpTransport = TCPNIOTransportBuilder
                    .newInstance()
                    .setReuseAddress(true)
                    .setIOStrategy(SameThreadIOStrategy.getInstance())
		    .setSelectorThreadPoolConfig(selectorPoolConfig)
                    .build();
            _transports.add(tcpTransport);
        }

        if ((protocol & IpProtocolType.UDP) != 0) {
            final UDPNIOTransport udpTransport = UDPNIOTransportBuilder
                    .newInstance()
                    .setReuseAddress(true)
                    .setIOStrategy(SameThreadIOStrategy.getInstance())
		    .setSelectorThreadPoolConfig(selectorPoolConfig)
                    .build();
            _transports.add(udpTransport);
        }
        _portRange = new PortRange(builder.getMinPort(), builder.getMaxPort());
        _backlog = builder.getBacklog();
        _bindAddress = builder.getBindAddress();

        if (builder.isWithJMX()) {
            final GrizzlyJmxManager jmxManager = GrizzlyJmxManager.instance();
            for (Transport t : _transports) {
                jmxManager.registerAtRoot(t.getMonitoringConfig().createManagementObject(), t.getName() + "-" + _portRange);
            }
        }
        _requestExecutor = builder.getWorkerThreadExecutorService();
        _gssSessionManager = builder.getGssSessionManager();
    }

    /**
     * Register a new PRC service. Existing registration will be overwritten.
     *
     * @param prog program number
     * @param handler RPC requests handler.
     */
    public void register(OncRpcProgram prog, RpcDispatchable handler) {
        _log.info("Registering new program {} : {}", prog, handler);
        _programs.put(prog, handler);
    }

    /**
     * Unregister program.
     *
     * @param prog
     */
    public void unregister(OncRpcProgram prog) {
        _log.info("Unregistering program {}", prog);
        _programs.remove(prog);
    }

    /**
     * Add programs to existing services.
     * @param services
     */
    public void setPrograms(Map<OncRpcProgram, RpcDispatchable> services) {
        _programs.putAll(services);
    }

    /**
     * Register services in portmap.
     *
     * @throws IOException
     * @throws UnknownHostException
     */
    private void publishToPortmap(Connection<InetSocketAddress> connection, Set<OncRpcProgram> programs) throws IOException {

        OncRpcClient rpcClient = new OncRpcClient(InetAddress.getByName(null),
                IpProtocolType.UDP, OncRpcPortmap.PORTMAP_PORT);
        XdrTransport transport = rpcClient.connect();

        try {
            OncPortmapClient portmapClient = new GenericPortmapClient(transport);

            Set<String> netids = new HashSet<String>();
            String username = System.getProperty("user.name");
            Transport t = connection.getTransport();
            String uaddr = InetSocketAddresses.uaddrOf(connection.getLocalAddress());

            String netidBase;
            if (t instanceof TCPNIOTransport) {
                netidBase = "tcp";
            } else if (t instanceof UDPNIOTransport) {
                netidBase = "udp";
            } else {
                // must never happens
                throw new RuntimeException("Unsupported transport type: " + t.getClass().getCanonicalName());
            }

            InetAddress localAddress = connection.getLocalAddress().getAddress();
            if (localAddress instanceof Inet6Address) {
                netids.add(netidBase + "6");
                if (((Inet6Address)localAddress).isIPv4CompatibleAddress()) {
                    netids.add(netidBase);
                }
            } else {
                netids.add(netidBase);
            }

            for (OncRpcProgram program : programs) {
                for (String netid : netids) {
                    try {
                        portmapClient.setPort(program.getNumber(), program.getVersion(),
                                netid, uaddr, username);
                    } catch (OncRpcException ex) {
                        _log.error("Failed to register program", ex);
                    }
                }
            }
        } catch (RpcProgUnavailable e) {
            _log.warn("Failed to register at portmap: ", e.getMessage());
        } finally {
            rpcClient.close();
        }
    }

    /**
     * UnRegister services in portmap.
     *
     * @throws IOException
     * @throws UnknownHostException
     */
    private void clearPortmap(Set<OncRpcProgram> programs) throws IOException {

        OncRpcClient rpcClient = new OncRpcClient(InetAddress.getByName(null),
                IpProtocolType.UDP, OncRpcPortmap.PORTMAP_PORT);
        XdrTransport transport = rpcClient.connect();

        try {
            OncPortmapClient portmapClient = new GenericPortmapClient(transport);

            String username = System.getProperty("user.name");

            for (OncRpcProgram program : programs) {
                try {
                    portmapClient.unsetPort(program.getNumber(),
                            program.getVersion(), username);
                } catch (OncRpcException ex) {
                    _log.info("Failed to unregister program {}", ex.getMessage());
                }
            }
        } catch (RpcProgUnavailable e) {
            _log.info("portmap service not available");
        } finally {
            rpcClient.close();
        }
    }

    public void start() throws IOException {

        if(_publish) {
            clearPortmap(_programs.keySet());
        }

        for (Transport t : _transports) {

            FilterChainBuilder filterChain = FilterChainBuilder.stateless();
            filterChain.add(new TransportFilter());
            filterChain.add(rpcMessageReceiverFor(t));
            filterChain.add(new RpcProtocolFilter(_replyQueue));
            // use GSS if configures
            if (_gssSessionManager != null) {
                filterChain.add(new GssProtocolFilter(_gssSessionManager));
            }
            filterChain.add(new RpcDispatcher(_requestExecutor, _programs));

            final FilterChain filters = filterChain.build();

            t.setProcessor(filters);
            Connection<InetSocketAddress> connection =
                    ((SocketBinder) t).bind(_bindAddress, _portRange, _backlog);
            t.start();

            _boundConnections.add(connection);
            if (_publish) {
                publishToPortmap(connection, _programs.keySet());
            }
        }
    }

    public void stop() throws IOException {

        if (_publish) {
            clearPortmap(_programs.keySet());
        }

        for (Transport t : _transports) {
            t.shutdownNow();
        }

        _requestExecutor.shutdown();
    }

    public void stop(long gracePeriod, TimeUnit timeUnit) throws IOException {

        if (_publish) {
            clearPortmap(_programs.keySet());
        }

        List<GrizzlyFuture<Transport>> transportsShuttingDown = new ArrayList<GrizzlyFuture<Transport>>();
        for (Transport t : _transports) {
            transportsShuttingDown.add(t.shutdown(gracePeriod, timeUnit));
        }

        for (GrizzlyFuture<Transport> transportShuttingDown : transportsShuttingDown) {
            try {
                transportShuttingDown.get();
            } catch (Exception e) {
                _log.warn("Exception while waiting for transport to shut down gracefully",e);
            }
        }

        _requestExecutor.shutdown();
    }

    /**
     * Returns the address of the endpoint this service is bound to,
     * or <code>null</code> if it is not bound yet.
     * @param protocol
     * @return a {@link InetSocketAddress} representing the local endpoint of
     * this service, or <code>null</code> if it is not bound yet.
     */
    public InetSocketAddress getInetSocketAddress(int protocol) {
        Class< ? extends Transport> transportClass = transportFor(protocol);
        for (Connection<InetSocketAddress> connection: _boundConnections) {
            if(connection.getTransport().getClass() == transportClass)
                return connection.getLocalAddress();
        }
        return null;
    }
}
