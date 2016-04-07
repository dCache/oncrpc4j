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

/**
 *
 * Why authentication failed.
 */
public final class RpcAuthStat {

    private RpcAuthStat() {
    }

    /*
     * failed on remote end
     */
    /** success */
    public static final int AUTH_OK = 0;
    /** bad credential (seal broken) */
    public static final int AUTH_BADCRED = 1;
    /** client must begin new session */
    public static final int AUTH_REJECTEDCRED = 2;
    /** bad verifier (seal broken) */
    public static final int AUTH_BADVERF = 3;
    /** verifier expired or replayed */
    public static final int AUTH_REJECTEDVERF = 4;
    /** rejected for security reasons */
    public static final int AUTH_TOOWEAK = 5;

    /*
     * failed locally
     */
    /** bogus response verifier */
    public static final int AUTH_INVALIDRESP = 6;
    /** reason unknown */
    public static final int AUTH_FAILED = 7;

    /*
     * AUTH_KERB errors; deprecated.  See [RFC2695]
     */
    /** kerberos generic error */
    public static final int AUTH_KERB_GENERIC = 8;
    /** time of credential expired */
    public static final int AUTH_TIMEEXPIRE = 9;
    /** problem with ticket file */
    public static final int AUTH_TKT_FILE = 10;
    /** can't decode authenticator */
    public static final int AUTH_DECODE = 11;
    /** wrong net address in ticket */
    public static final int AUTH_NET_ADDR = 12;

    /*
     * RPCSEC_GSS GSS related errors
     */
    /** no credentials for user */
    public static final int RPCSEC_GSS_CREDPROBLEM = 13;
    /** problem with context */
    public static final int RPCSEC_GSS_CTXPROBLEM = 14;

    /*
     * RPCSEC_GSSv3 errors
     */
    public static final int RPCSEC_GSS_INNER_CREDPROBLEM = 15;
    public static final int RPCSEC_GSS_LABEL_PROBLEM = 16;
    public static final int RPCSEC_GSS_PRIVILEGE_PROBLEM = 17;
    public static final int RPCSEC_GSS_UNKNOWN_MESSAGE = 18;

    /**
     * Get human readable {@link String} representation of error code.
     */
    public static String toString(int i) {
        switch (i) {
            case AUTH_OK:
                return "OK";
            case AUTH_BADCRED:
                return "AUTH_BADCRED";
            case AUTH_REJECTEDCRED:
                return "AUTH_REJECTEDCRED";
            case AUTH_BADVERF:
                return "AUTH_BADVERF";
            case AUTH_REJECTEDVERF:
                return "AUTH_REJECTEDVERF";
            case AUTH_TOOWEAK:
                return "AUTH_TOOWEAK";
            case AUTH_INVALIDRESP:
                return "AUTH_INVALIDRESP";
            case AUTH_FAILED:
                return "AUTH_FAILED";
            case AUTH_KERB_GENERIC:
                return "AUTH_KERB_GENERIC";
            case AUTH_TIMEEXPIRE:
                return "AUTH_TIMEEXPIRE";
            case AUTH_TKT_FILE:
                return "AUTH_TKT_FILE";
            case AUTH_DECODE:
                return "AUTH_DECODE";
            case AUTH_NET_ADDR:
                return "AUTH_NET_ADDR";
            case RPCSEC_GSS_CREDPROBLEM:
                return "RPCSEC_GSS_CREDPROBLEM";
            case RPCSEC_GSS_CTXPROBLEM:
                return "RPCSEC_GSS_CTXPROBLEM";
            case RPCSEC_GSS_INNER_CREDPROBLEM:
                return "RPCSEC_GSS_INNER_CREDPROBLEM";
            case RPCSEC_GSS_LABEL_PROBLEM:
                return "RPCSEC_GSS_LABEL_PROBLEM";
            case RPCSEC_GSS_PRIVILEGE_PROBLEM:
                return "RPCSEC_GSS_PRIVILEGE_PROBLEM";
            case RPCSEC_GSS_UNKNOWN_MESSAGE:
                return "RPCSEC_GSS_UNKNOWN_MESSAGE";
        }
        return "Unknow state " + i;
    }
}
