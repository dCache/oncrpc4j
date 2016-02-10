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
package org.dcache.xdr;

import org.springframework.beans.factory.FactoryBean;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.dcache.xdr.gss.GssSessionManager;

/**
 * A {@link FactoryBean} to use {@link OncRpcSvcBuilder}
 * within Spring framework.
 *
 * @since 2.1
 */
public class OncRpcSvcFactoryBean implements FactoryBean<OncRpcSvcBuilder> {

    private final OncRpcSvcBuilder builder;

    public OncRpcSvcFactoryBean() {
        builder = new OncRpcSvcBuilder();
    }

    @Override
    public OncRpcSvcBuilder getObject() throws Exception {
        return builder;
    }

    @Override
    public Class<? extends OncRpcSvcBuilder> getObjectType() {
        return OncRpcSvcBuilder.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    public void setAutoPublish(boolean autopublish) {
        if (autopublish) {
            builder.withAutoPublish();
        } else {
            builder.withoutAutoPublish();
        }
    }

    public void setMaxPort(int maxPort) {
        builder.withMaxPort(maxPort);
    }

    public void setMinPort(int minPort) {
        builder.withMinPort(minPort);
    }

    public void setPort(int port) {
        builder.withPort(port);
    }

    public void setUseTCP(boolean useTCP) {
        if (useTCP) {
            builder.withTCP();
        }
    }

    public void setUseUDP(boolean useUDP) {
        if (useUDP) {
            builder.withUDP();
        }
    }

    public void setUseWorkerPool(boolean useWorkerPool) {
        if (useWorkerPool) {
            builder.withWorkerThreadIoStrategy();
        } else {
            builder.withSameThreadIoStrategy();
        }
    }

    public void setEnableJmx(boolean enable) {
        if (enable) {
            builder.withJMX();
        }
    }

    public void setGssSessionManager(GssSessionManager gssSessionManager) {
        builder.withGssSessionManager(gssSessionManager);
    }

    public void setWorkerThreadExecutionService(ExecutorService executorService) {
        builder.withWorkerThreadExecutionService(executorService);
    }

    public void setRpcServices(Map<OncRpcProgram, RpcDispatchable> services) {
        for(Map.Entry<OncRpcProgram, RpcDispatchable> program: services.entrySet()) {
            builder.withRpcService(program.getKey(), program.getValue());
        }
    }

    public void setServiceName(String serviceName) {
        builder.withServiceName(serviceName);
    }

    public void setSelectorThreadPoolSize(int poolSize) {
        builder.withSelectorThreadPoolSize(poolSize);
    }

    public void setWorkerThreadPoolSize(int poolSize) {
        builder.withWorkerThreadPoolSize(poolSize);
    }
}
