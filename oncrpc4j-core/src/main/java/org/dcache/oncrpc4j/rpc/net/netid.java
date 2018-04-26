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
package org.dcache.oncrpc4j.rpc.net;

import java.net.InetSocketAddress;

public class netid {

    private netid() {}

    public static String toString(int port) {
        int port_part[] = new int[2];
        port_part[0] = (port & 0xff00) >> 8;
        port_part[1] = port & 0x00ff;
        return "0.0.0.0." + (0xFF & port_part[0]) + "." + (0xFF & port_part[1]);
    }

    public static InetSocketAddress toInetSocketAddress(String str) {

        return InetSocketAddresses.forUaddrString(str);

    }

    public static int getPort(String str) {
        return toInetSocketAddress(str).getPort();
    }

    public static int idOf(String id) {
        switch (id) {
            case "tcp":
                return IpProtocolType.TCP;
            case "udp":
                return IpProtocolType.UDP;
            default:
                return -1;
        }
    }

}
