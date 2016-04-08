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
package org.dcache.jarpcbind;

import org.dcache.xdr.OncRpcProgram;
import org.dcache.xdr.OncRpcSvc;
import org.dcache.xdr.OncRpcSvcBuilder;
import org.dcache.xdr.RpcDispatchable;
import org.dcache.xdr.portmap.OncRpcPortmap;
import org.dcache.xdr.portmap.OncRpcbindServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final Object LOCK = new Object();
    private static volatile boolean on = true;
    private static volatile Thread mainThread;

    public static void main(String[] args) throws Exception {
        mainThread = Thread.currentThread();
        logger.info("starting up");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("interrupt received, shutting down");
                synchronized (LOCK) {
                    on = false;
                    LOCK.notifyAll();
                    mainThread.interrupt();
                }
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    logger.error("interrupted waiting for graceful shutdown", e);
                }
                logger.info("exiting");
            }
        });
        RpcDispatchable rpcbind = new OncRpcbindServer();
        OncRpcSvc server  = new OncRpcSvcBuilder()
                .withPort(OncRpcPortmap.PORTMAP_PORT)
                .withTCP()
                .withUDP()
                .withSameThreadIoStrategy()
                .withoutAutoPublish()
                .build();
        server.register(new OncRpcProgram(OncRpcPortmap.PORTMAP_PROGRAMM, OncRpcPortmap.PORTMAP_V2), rpcbind);
        server.start();
        logger.info("up and running");
        synchronized (LOCK) {
            while (on) {
                try {
                    LOCK.wait();
                } catch (InterruptedException e) {
                    if (on) {
                        logger.error("spurious interruption", e);
                    }
                }
            }
        }
        logger.info("shutting down");
    }
}
