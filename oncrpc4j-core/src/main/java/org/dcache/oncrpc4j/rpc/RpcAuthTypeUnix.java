/*
 * Copyright (c) 2009 - 2019 Deutsches Elektronen-Synchroton,
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

import com.sun.security.auth.UnixNumericGroupPrincipal;
import com.sun.security.auth.UnixNumericUserPrincipal;
import com.sun.security.auth.module.UnixSystem;
import org.dcache.oncrpc4j.xdr.XdrAble;
import org.dcache.oncrpc4j.xdr.XdrDecodingStream;
import org.dcache.oncrpc4j.xdr.XdrEncodingStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.io.IOException;
import java.net.InetAddress;
import java.security.Principal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;

public class RpcAuthTypeUnix implements RpcAuth, XdrAble {

    private final int _type =  RpcAuthType.UNIX;
    private RpcAuthVerifier _verifier = new RpcAuthVerifier(RpcAuthType.NONE, new byte[0]);

    private int _len;
    private int _uid;
    private int _gid;
    private int _gids[];
    private int _stamp;
    private String _machine;
    private Subject _subject;

    private final static Logger _log = LoggerFactory.getLogger(RpcAuthTypeUnix.class);

    public RpcAuthTypeUnix() {}

    public RpcAuthTypeUnix(int uid, int gid, int[] gids, int stamp, String machine) {
        _uid = uid;
        _gid = gid;
        _gids = gids;
        _stamp = stamp;
        _machine = machine;
        _len = 4 /*uid*/ + 4/*gid*/ + 4/*gids len place holder*/ + 4*_gids.length +
                4/*machine len place holder*/ + _machine.length() +
                ((4 - (_machine.length() & 3)) & 3) /*padding bytes*/+
                 + 4/*stamp*/;
        _subject = buildUnixSubject(uid, gid, gids);
    }

    public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException {

        _len = xdr.xdrDecodeInt();
        _stamp = xdr.xdrDecodeInt();
        _machine = xdr.xdrDecodeString();
        _uid = xdr.xdrDecodeInt();
        _gid = xdr.xdrDecodeInt();
        _gids = xdr.xdrDecodeIntVector();
        _verifier.xdrDecode(xdr);

        _subject = buildUnixSubject(_uid, _gid, _gids);
    }

    private static Subject buildUnixSubject(int uid, int gid, int[] gids) {
        final Subject unixSubject = new Subject();
        final Set<Principal> principals = unixSubject.getPrincipals();
        principals.add(new UnixNumericUserPrincipal(uid));
        principals.add(new UnixNumericGroupPrincipal(gid, true));
        for (int gidElem : gids) {
            principals.add(new UnixNumericGroupPrincipal(gidElem, false));
        }
        return unixSubject;
    }

    @Override
    public Subject getSubject() {
        return _subject;
    }

    @Override
    public int type() {
        return _type;
    }

    @Override
    public RpcAuthVerifier getVerifier() {
        return _verifier;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Host: ").append(_machine).append("\n");
        sb.append("timestamp: ").append(_stamp).append("\n");
        sb.append("uid: ").append(_uid).append("\n");
        sb.append("gid: ").append(_gid).append("\n");
        sb.append("gids: ").append(Arrays.toString(_gids)).append("\n");

        return sb.toString();
    }

    @Override
    public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException {
       xdr.xdrEncodeInt(_type);
       xdr.xdrEncodeInt(_len);
       xdr.xdrEncodeInt(_stamp);
       xdr.xdrEncodeString(_machine);
       xdr.xdrEncodeInt(_uid);
       xdr.xdrEncodeInt(_gid);
       xdr.xdrEncodeIntVector(_gids);
       _verifier.xdrEncode(xdr);
    }

    public int uid() {
        return _uid;
    }

    public int gid() {
        return _gid;
    }

    public int[] gids() {
        return _gids;
    }

    /**
     * Get {@link RpcAuthTypeUnix} corresponding to the UNIX user running
     * this application.
     * @return RPC auth corresponding to UNIX credentials.
     * @throws IllegalStateException
     * @throws IOException if failed to get information to build  UNIX credentials.
     */
    public static RpcAuthTypeUnix ofCurrentUnixUser() throws
            IllegalStateException, IOException {

        String os = System.getProperty("os.name");
        checkState(!os.toLowerCase().startsWith("windows"), "%s platform not supported", os);

        UnixSystem user = new UnixSystem();

        long uid = user.getUid();
        long gid = user.getGid();
        long[] gids = user.getGroups();

        return new RpcAuthTypeUnix(
                (int)uid,
                (int)gid,
                Arrays.stream(gids).mapToInt(l -> (int)l).toArray(),
                (int)Instant.now().getEpochSecond(),
                InetAddress.getLocalHost().getHostName()
        );
    }
}
