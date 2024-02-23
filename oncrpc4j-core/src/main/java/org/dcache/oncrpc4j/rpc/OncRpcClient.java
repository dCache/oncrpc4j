/*
 * Copyright (c) 2009 - 2024 Deutsches Elektronen-Synchroton,
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class OncRpcClient implements AutoCloseable {

    private static final String DEFAULT_SERVICE_NAME = null;

    private final InetSocketAddress _socketAddress;
    private final OncRpcSvc _rpcsvc;

    public OncRpcClient(InetAddress address, int protocol, int port) {
        this(new InetSocketAddress(address, port), protocol, 0, IoStrategy.SAME_THREAD, DEFAULT_SERVICE_NAME);
    }

    public OncRpcClient(InetAddress address, int protocol, int port, int localPort) {
        this(new InetSocketAddress(address, port), protocol, localPort, IoStrategy.SAME_THREAD, DEFAULT_SERVICE_NAME);
    }

    public OncRpcClient(InetAddress address, int protocol, int port, int localPort, IoStrategy ioStrategy) {
        this(new InetSocketAddress(address, port), protocol, localPort, ioStrategy, DEFAULT_SERVICE_NAME);
    }

    public OncRpcClient(InetAddress address, int protocol, int port, int localPort, IoStrategy ioStrategy, String serviceName) {
        this(new InetSocketAddress(address, port), protocol, localPort, ioStrategy, serviceName);
    }

    public OncRpcClient(InetSocketAddress socketAddress, int protocol) {
        this(socketAddress, protocol, 0, IoStrategy.SAME_THREAD, DEFAULT_SERVICE_NAME);
    }

    public OncRpcClient(InetSocketAddress socketAddress, int protocol, int localPort, IoStrategy ioStrategy, String serviceName) {
        this(socketAddress, new OncRpcSvcBuilder()
                .withClientMode()
                .withPort(localPort)
                .withIpProtocolType(protocol)
                .withIoStrategy(ioStrategy)
                .withServiceName(serviceName)
                .build());
    }


    private OncRpcClient(InetSocketAddress socketAddress, OncRpcSvc clientSvc) {
        _socketAddress = socketAddress;
        _rpcsvc = clientSvc;
    }

    public RpcTransport connect() throws IOException {
        return connect(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    public RpcTransport connect(long timeout, TimeUnit timeUnit) throws IOException {
        RpcTransport t;
        try {
        _rpcsvc.start();
            t =_rpcsvc.connect(_socketAddress, timeout, timeUnit);
        } catch (IOException e ) {
            _rpcsvc.stop();
            throw e;
        }
        return t;
    }

    @Override
    public void close() throws IOException {
        _rpcsvc.stop();
    }

    public static OncRpcClientBuilder newBuilder() {
        return new OncRpcClientBuilder();
    }

    public static class OncRpcClientBuilder {

        private final OncRpcSvcBuilder svcBuilder = new OncRpcSvcBuilder()
                .withClientMode()
                .withWorkerThreadIoStrategy()
                .withSelectorThreadPoolSize(1)
                .withWorkerThreadPoolSize(1)
                .withoutAutoPublish();

        private OncRpcClientBuilder() {
            // no direct instantiation
        }

        public OncRpcClientBuilder withProtocol(int protocol) {
            svcBuilder.withIpProtocolType(protocol);
            return this;
        }

        public OncRpcClientBuilder withLocalPort(int localPort) {
            svcBuilder.withPort(localPort);
            return this;
        }

        public OncRpcClientBuilder withIoStrategy(IoStrategy ioStrategy) {
            svcBuilder.withIoStrategy(ioStrategy);
            return this;
        }

        public OncRpcClientBuilder withServiceName(String serviceName) {
            svcBuilder.withServiceName(serviceName);
            return this;
        }

        public OncRpcClientBuilder withWorkerThreadPoolSize(int size) {
            svcBuilder.withWorkerThreadPoolSize(size);
            return this;
        }

        public OncRpcClientBuilder withSelectorThreadPoolSize(int size) {
            svcBuilder.withSelectorThreadPoolSize(size);
            return this;
        }

        public OncRpcClientBuilder withWorkerThreadIoStrategy() {
            svcBuilder.withWorkerThreadIoStrategy();
            return this;
        }

        public OncRpcClientBuilder withRpcService(OncRpcProgram program, RpcDispatchable dispatchable) {
            svcBuilder.withRpcService(program, dispatchable);
            return this;
        }

        public OncRpcClientBuilder withWorkerThreadExecutionService(ExecutorService executorService) {
            svcBuilder.withWorkerThreadExecutionService(executorService);
            return this;
        }

        public OncRpcClientBuilder withTCP() {
            svcBuilder.withTCP();
            return this;
        }

        public OncRpcClientBuilder withUDP() {
            svcBuilder.withUDP();
            return this;
        }

        /**
         * Build a new {@link OncRpcClient} instance.
         *
         * @param endpoint the socket address of the remote RPC server
         * @return a new {@link OncRpcClient} instance
         */
        public OncRpcClient build(InetSocketAddress endpoint) {
            return new OncRpcClient(endpoint, svcBuilder.build());
        }

        /**
         * Build a new {@link OncRpcClient} instance.
         *
         * @param endpoint the address of the remote RPC server
         * @param port the port of the remote RPC server
         * @return a new {@link OncRpcClient} instance
         */
        public OncRpcClient build(InetAddress endpoint, int port) {
            return build(new InetSocketAddress(endpoint, port));
        }
    }

}
