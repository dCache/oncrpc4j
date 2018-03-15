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
package org.dcache.oncrpc4j.rpc.gss;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.dcache.oncrpc4j.rpc.RpcLoginService;
import org.dcache.oncrpc4j.xdr.XdrOpaque;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import org.dcache.oncrpc4j.rpc.RpcTransport;

public class GssSessionManager {

    private final String KRB5_OID = "1.2.840.113554.1.2.2";

    private static final Logger _log = LoggerFactory.getLogger(GssSessionManager.class);
    private final GSSManager gManager = GSSManager.getInstance();
    private final GSSCredential _serviceCredential;
    private final RpcLoginService _loginService;

    public GssSessionManager(RpcLoginService loginService, String servicePrincipal, String keytab)
            throws GSSException, IOException {
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
        System.setProperty("java.security.auth.login.config",
                JaasConfigGenerator.generateJaasConfig(servicePrincipal, keytab));

        Oid krb5Mechanism = new Oid(KRB5_OID);
        _serviceCredential = gManager.createCredential(null,
                GSSCredential.INDEFINITE_LIFETIME,
                krb5Mechanism, GSSCredential.ACCEPT_ONLY);
        _loginService = loginService;
    }

    public GssSessionManager(RpcLoginService loginService) throws GSSException {
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");

        Oid krb5Mechanism = new Oid(KRB5_OID);
        _serviceCredential = gManager.createCredential(null,
                GSSCredential.INDEFINITE_LIFETIME,
                krb5Mechanism, GSSCredential.ACCEPT_ONLY);
        _loginService = loginService;
    }
    private final Map<XdrOpaque, GSSContext> sessions = new ConcurrentHashMap<>();

    public GSSContext createContext(byte[] handle) throws GSSException {
        GSSContext context = gManager.createContext(_serviceCredential);
        sessions.put(new XdrOpaque(handle), context);
        return context;
    }

    public GSSContext getContext(byte[] handle) throws GSSException {
        GSSContext context = sessions.get(new XdrOpaque(handle));
        if(context == null) {
            throw new GSSException(GSSException.NO_CONTEXT);
        }
        return context;
    }
    public GSSContext getEstablishedContext(byte[] handle) throws GSSException {
        GSSContext context = getContext(handle);
        if (!context.isEstablished()) {
            throw new GSSException(GSSException.NO_CONTEXT);
        }
        return context;
    }

    public GSSContext destroyContext(byte[] handle) throws GSSException {
        GSSContext context = sessions.remove(new XdrOpaque(handle));
        if(context == null || !context.isEstablished()) {
            throw new GSSException(GSSException.NO_CONTEXT);
        }
        return context;
    }

    public Subject subjectOf(RpcTransport transport, GSSContext context) {
        return _loginService.login(transport, context);
    }
}
