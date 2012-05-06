/*
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dcache.utils.net.InetSocketAddresses;
import org.dcache.xdr.gss.GssProtocolFilter;
import org.dcache.xdr.gss.GssSessionManager;
import org.dcache.xdr.portmap.GenericPortmapClient;
import org.dcache.xdr.portmap.OncPortmapClient;
import org.dcache.xdr.portmap.OncRpcPortmap;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.PortRange;
import org.glassfish.grizzly.SocketBinder;
import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.monitoring.jmx.GrizzlyJmxManager;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.nio.transport.UDPNIOTransport;
import org.glassfish.grizzly.nio.transport.UDPNIOTransportBuilder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.dcache.xdr.GrizzlyUtils.*;
import org.glassfish.grizzly.*;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;

public class OncRpcSvc {

    private final static Logger _log = Logger.getLogger(OncRpcSvc.class.getName());
    private final static int BACKLOG = 4096;
    private final boolean _publish;
    private final PortRange _portRange;
    private final List<Transport> _transports = new ArrayList<Transport>();
    private final Set<Connection<InetSocketAddress>> _boundConnections =
            new HashSet<Connection<InetSocketAddress>>();

    public enum IoStrategy {
        SAME_THREAD {
            @Override
            IOStrategy getStrategy() {
                return SameThreadIOStrategy.getInstance();
            }
        },
        WORKER_THREAD {
            @Override
            IOStrategy getStrategy() {
                return WorkerThreadIOStrategy.getInstance();
            }
        };

        abstract IOStrategy getStrategy();
    }

    private final ReplyQueue<Integer, RpcReply> _replyQueue = new ReplyQueue<Integer, RpcReply>();
    /**
     * Handle RPCSEC_GSS
     */
    private GssSessionManager _gssSessionManager;

    /**
     * mapping of registered programs.
     */
    private final Map<OncRpcProgram, RpcDispatchable> _programs =
            new ConcurrentHashMap<OncRpcProgram, RpcDispatchable>();

    /**
     * Create a RPC service. Binds to a given <i>port</i> on all available
     * interfaces and transports ( TCP and UDP ). All programs will be published
     * on the startup.
     *
     * @param port number to bind
     */
    public OncRpcSvc(int port) {
        this(port, IpProtocolType.TCP | IpProtocolType.UDP, true);
    }

    /**
     * Create a RPC service. Binds to a given <i>port</i> on all available
     * interfaces and transports ( TCP and UDP )..
     *
     * @param port number to bind
     * @param publish all services if <i>true</i>
     */
    public OncRpcSvc(int port, boolean publish) {
        this(port, IpProtocolType.TCP | IpProtocolType.UDP, publish);
    }

    /**
     * Create a RPC service. Binds to a given <i>port</i> with a given
     * <i>protocol</i>.
     *
     * @param port number to bind
     * @param protocol to bind
     * @param publish all services if <i>true</i>
     */
    public OncRpcSvc(int port, int protocol, boolean publish) {
        this(new PortRange(port), protocol, publish, IoStrategy.SAME_THREAD);
    }

    /**
     * Create a RPC service. Binds with in a given <i>porRanget</i> with a given
     * <i>protocol</i>.
     *
     * @param portRange to bind
     * @param protocol to bind
     * @param publish all services if <i>true</i>
     */
    public OncRpcSvc(PortRange portRange, int protocol, boolean publish, IoStrategy ioStrategy) {
        _publish = publish;

        if ((protocol & (IpProtocolType.TCP | IpProtocolType.UDP)) == 0) {
            throw new IllegalArgumentException("TCP or UDP protocol have to be defined");
        }

        IOStrategy grizzlyIoStrategy = ioStrategy.getStrategy();
        if ((protocol & IpProtocolType.TCP) != 0) {
            final TCPNIOTransport tcpTransport = TCPNIOTransportBuilder
                    .newInstance()
                    .setReuseAddress(true)
                    .setIOStrategy(grizzlyIoStrategy)
                    .build();
            _transports.add(tcpTransport);
        }

        if ((protocol & IpProtocolType.UDP) != 0) {
            final UDPNIOTransport udpTransport = UDPNIOTransportBuilder
                    .newInstance()
                    .setReuseAddress(true)
                    .setIOStrategy(grizzlyIoStrategy)
                    .build();
            _transports.add(udpTransport);
        }
        _portRange = portRange;
    }

    /**
     * Register a new PRC service. Existing registration will be overwritten.
     *
     * @param prog program number
     * @param handler RPC requests handler.
     */
    public void register(OncRpcProgram prog, RpcDispatchable handler) {
        _log.log(Level.INFO, "Registering new program {0} : {1}",
                new Object[]{prog, handler});
        _programs.put(prog, handler);
    }

    /**
     * Unregister program.
     *
     * @param prog
     */
    public void unregister(OncRpcProgram prog) {
        _log.log(Level.INFO, "Unregistering program {0}", prog);
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
        OncPortmapClient portmapClient = new GenericPortmapClient(transport);

        try {
            String username = System.getProperty("user.name");
            Transport t = connection.getTransport();
            String uaddr = InetSocketAddresses.uaddrOf(connection.getLocalAddress());

            for (OncRpcProgram program : programs) {
                try {
                    if (t instanceof TCPNIOTransport) {
                        portmapClient.setPort(program.getNumber(), program.getVersion(),
                                "tcp", uaddr, username);
                    }
                    if (t instanceof UDPNIOTransport) {
                        portmapClient.setPort(program.getNumber(), program.getVersion(),
                                "udp", uaddr, username);
                    }
                } catch (OncRpcException ex) {
                    _log.log(Level.SEVERE, "Failed to register program", ex);
                }
            }
        } finally {
            rpcClient.close();
        }
    }

    /**
     * Set {@link GssSessionManager} to handle GSS context if RPCSEG_GSS is used.
     * If {@code gssSessionManager} is <i>null</i> GSS authentication will be
     * disabled.
     * @param gssSessionManager
     */
    public void setGssSessionManager(GssSessionManager gssSessionManager) {
        _gssSessionManager = gssSessionManager;
    }

    public void start() throws IOException {

        for (Transport t : _transports) {

            FilterChainBuilder filterChain = FilterChainBuilder.stateless();
            filterChain.add(new TransportFilter());
            filterChain.add(rpcMessageReceiverFor(t));
            filterChain.add(new RpcProtocolFilter(_replyQueue));
            // use GSS if configures
            if (_gssSessionManager != null) {
                filterChain.add(new GssProtocolFilter(_gssSessionManager));
            }
            filterChain.add(new RpcDispatcher(_programs));

            final FilterChain filters = filterChain.build();

            t.setProcessor(filters);
            Connection<InetSocketAddress> connection =
                    ((SocketBinder) t).bind("0.0.0.0", _portRange, BACKLOG);
            t.start();

            _boundConnections.add(connection);
            if (_publish) {
                publishToPortmap(connection, _programs.keySet());
            }
        }

    }

    public void stop() throws IOException {
        for (Transport t : _transports) {
            t.stop();
        }
    }

    public void enableJMX() {
        final GrizzlyJmxManager jmxManager = GrizzlyJmxManager.instance();
        for (Transport t : _transports) {
            jmxManager.registerAtRoot(t.getMonitoringConfig().createManagementObject(), t.getName() + "-" + _portRange);
        }
    }

    /**
     * Returns the address of the endpoint this service is bound to,
     * or <code>null<code> if it is not bound yet.
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
