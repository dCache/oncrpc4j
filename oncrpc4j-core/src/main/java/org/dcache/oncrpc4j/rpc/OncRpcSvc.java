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
import org.dcache.oncrpc4j.rpc.net.InetSocketAddresses;
import org.dcache.oncrpc4j.rpc.gss.GssProtocolFilter;
import org.dcache.oncrpc4j.rpc.gss.GssSessionManager;
import org.dcache.oncrpc4j.portmap.GenericPortmapClient;
import org.dcache.oncrpc4j.portmap.OncPortmapClient;
import org.dcache.oncrpc4j.portmap.OncRpcPortmap;
import org.glassfish.grizzly.CloseType;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.ConnectionProbe;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.PortRange;
import org.glassfish.grizzly.SocketBinder;
import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.jmxbase.GrizzlyJmxManager;
import org.glassfish.grizzly.nio.NIOTransport;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.base.Throwables.propagateIfPossible;
import java.net.SocketAddress;
import java.util.stream.Collectors;
import org.dcache.oncrpc4j.grizzly.GrizzlyRpcTransport;
import static org.dcache.oncrpc4j.grizzly.GrizzlyUtils.getSelectorPoolCfg;
import static org.dcache.oncrpc4j.grizzly.GrizzlyUtils.rpcMessageReceiverFor;
import static org.dcache.oncrpc4j.grizzly.GrizzlyUtils.transportFor;

public class OncRpcSvc {

    private final static Logger _log = LoggerFactory.getLogger(OncRpcSvc.class);

    private final int _backlog;
    private final boolean _publish;
    private final PortRange _portRange;
    private final String _bindAddress;
    private final boolean _isClient;
    private final List<NIOTransport> _transports = new ArrayList<>();
    private final Set<Connection<InetSocketAddress>> _boundConnections =
            new HashSet<>();

    private final ExecutorService _requestExecutor;

    private final ReplyQueue _replyQueue = new ReplyQueue();

    private final boolean _withSubjectPropagation;
    /**
     * Handle RPCSEC_GSS
     */
    private final GssSessionManager _gssSessionManager;

    /**
     * mapping of registered programs.
     */
    private final Map<OncRpcProgram, RpcDispatchable> _programs =
            new ConcurrentHashMap<>();

    /**
     * Name of this service
     */
    private final String _svcName;

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
        String serviceName = builder.getServiceName();
        ThreadPoolConfig selectorPoolConfig = getSelectorPoolCfg(ioStrategy,
                serviceName,
                builder.getSelectorThreadPoolSize());

        if ((protocol & IpProtocolType.TCP) != 0) {
            final TCPNIOTransport tcpTransport = TCPNIOTransportBuilder
                    .newInstance()
                    .setReuseAddress(true)
                    .setIOStrategy(SameThreadIOStrategy.getInstance())
                    .setSelectorThreadPoolConfig(selectorPoolConfig)
                    .setSelectorRunnersCount(selectorPoolConfig.getMaxPoolSize())
                    .build();
            _transports.add(tcpTransport);
        }

        if ((protocol & IpProtocolType.UDP) != 0) {
            final UDPNIOTransport udpTransport = UDPNIOTransportBuilder
                    .newInstance()
                    .setReuseAddress(true)
                    .setIOStrategy(SameThreadIOStrategy.getInstance())
                    .setSelectorThreadPoolConfig(selectorPoolConfig)
                    .setSelectorRunnersCount(selectorPoolConfig.getMaxPoolSize())
                    .build();
            _transports.add(udpTransport);
        }
        _isClient = builder.isClient();
        _portRange = builder.getMinPort() > 0 ?
                new PortRange(builder.getMinPort(), builder.getMaxPort()) : null;

        _backlog = builder.getBacklog();
        _bindAddress = builder.getBindAddress();

        if (builder.isWithJMX()) {
            final GrizzlyJmxManager jmxManager = GrizzlyJmxManager.instance();
	    _transports.forEach((t) -> {
		jmxManager.registerAtRoot(t.getMonitoringConfig().createManagementObject(), t.getName() + "-" + _portRange);
	    });
        }
        _requestExecutor = builder.getWorkerThreadExecutorService();
        _gssSessionManager = builder.getGssSessionManager();
        _programs.putAll(builder.getRpcServices());
        _withSubjectPropagation = builder.getSubjectPropagation();
	_svcName = builder.getServiceName();
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
     * @deprecated use {@link OncRpcSvcBuilder#withRpcService} instead.
     */
    @Deprecated
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
        RpcTransport transport = rpcClient.connect();

        try {
            OncPortmapClient portmapClient = new GenericPortmapClient(transport);

            Set<String> netids = new HashSet<>();
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
                    } catch (OncRpcException | TimeoutException e) {
                        _log.warn("Failed to register program: {}", e.getMessage());
                    }
                }
            }
        } catch (RpcProgUnavailable e) {
            _log.warn("Failed to register at portmap: {}", e.getMessage());
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
        RpcTransport transport = rpcClient.connect();

        try {
            OncPortmapClient portmapClient = new GenericPortmapClient(transport);

            String username = System.getProperty("user.name");

            for (OncRpcProgram program : programs) {
                try {
                    portmapClient.unsetPort(program.getNumber(),
                            program.getVersion(), username);
                } catch (OncRpcException | TimeoutException e) {
                    _log.info("Failed to unregister program: {}", e.getMessage());
                }
            }
        } catch (RpcProgUnavailable e) {
            _log.info("portmap service not available");
        } finally {
            rpcClient.close();
        }
    }

    public void start() throws IOException {

        if(!_isClient && _publish) {
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
            filterChain.add(new RpcDispatcher(_requestExecutor, _programs, _withSubjectPropagation));

            final FilterChain filters = filterChain.build();

            t.setProcessor(filters);
            t.getConnectionMonitoringConfig().addProbes(new ConnectionProbe.Adapter() {
                @Override
                public void onCloseEvent(Connection connection) {
                    if (connection.getCloseReason().getType() == CloseType.REMOTELY) {
                        _replyQueue.handleDisconnect((SocketAddress)connection.getLocalAddress());
                    }
                }
            });

            if(!_isClient) {
                Connection<InetSocketAddress> connection = _portRange == null ?
                        ((SocketBinder) t).bind(_bindAddress, 0, _backlog) :
                        ((SocketBinder) t).bind(_bindAddress, _portRange, _backlog);

                _boundConnections.add(connection);

                if (_publish) {
                    publishToPortmap(connection, _programs.keySet());
                }
            }
            t.start();

        }
    }

    public void stop() throws IOException {

        if (!_isClient && _publish) {
            clearPortmap(_programs.keySet());
        }

        for (Transport t : _transports) {
            t.shutdownNow();
        }

        _replyQueue.shutdown();
        _requestExecutor.shutdown();
    }

    public void stop(long gracePeriod, TimeUnit timeUnit) throws IOException {

        if (!_isClient && _publish) {
            clearPortmap(_programs.keySet());
        }

        List<GrizzlyFuture<Transport>> transportsShuttingDown = new ArrayList<>();
        for (Transport t : _transports) {
            transportsShuttingDown.add(t.shutdown(gracePeriod, timeUnit));
        }

        for (GrizzlyFuture<Transport> transportShuttingDown : transportsShuttingDown) {
            try {
                transportShuttingDown.get();
            } catch (InterruptedException e) {
                _log.info("Waiting for graceful shut down interrupted");
            } catch (ExecutionException e) {
                Throwable t = getRootCause(e);
                _log.warn("Exception while waiting for transport to shut down gracefully",t);
            }
        }

        _requestExecutor.shutdown();
    }

    public RpcTransport connect(InetSocketAddress socketAddress) throws IOException {
        return connect(socketAddress, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    public RpcTransport connect(InetSocketAddress socketAddress, long timeout, TimeUnit timeUnit) throws IOException {

        // in client mode only one transport is defined
        NIOTransport transport = _transports.get(0);

        Future<Connection> connectFuture;
        if (_portRange != null) {
            InetSocketAddress localAddress = new InetSocketAddress(_portRange.getLower());
            connectFuture = transport.connect(socketAddress, localAddress);
        } else {
            connectFuture = transport.connect(socketAddress);
        }

        try {
            //noinspection unchecked
            Connection<InetSocketAddress> connection = connectFuture.get(timeout, timeUnit);
            return new GrizzlyRpcTransport(connection, _replyQueue);
        } catch (ExecutionException e) {
            Throwable t = getRootCause(e);
            propagateIfPossible(t, IOException.class);
            throw new IOException(e.toString(), e);
        } catch (TimeoutException | InterruptedException e) {
            throw new IOException(e.toString(), e);
        }
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
	return _boundConnections.stream()
		.filter(c -> c.getTransport().getClass() == transportClass)
		.map(Connection::getLocalAddress)
		.findAny()
		.orElse(null);
    }

    /**
     * Get name of this service.
     * @return name of this service.
     */
    public String getName() {
	return _svcName;
    }

    @Override
    public String toString() {
	return _boundConnections.stream()
		.map(Connection::getLocalAddress)
		.map(Object::toString)
		.collect(Collectors.joining(",", getName() +"-[", "]"));
    }
}
