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
package org.dcache.oncrpc4j.xdr;

import java.nio.ByteBuffer;

/**
 * Defines interface for decoding XDR stream. A decoding
 * XDR stream returns data in the form of Java data types which it reads
 * from a data source (for instance, network or memory buffer) in the
 * platform-independent XDR format.
 */
public interface XdrDecodingStream {


    void beginDecoding();
    void endDecoding();
    int xdrDecodeInt() throws BadXdrOncRpcException;
    int[] xdrDecodeIntVector() throws BadXdrOncRpcException;
    int[] xdrDecodeIntFixedVector(int len) throws BadXdrOncRpcException;
    byte[] xdrDecodeDynamicOpaque() throws BadXdrOncRpcException;
    byte[] xdrDecodeOpaque(int size) throws BadXdrOncRpcException;
    void xdrDecodeOpaque(byte[] data, int offset, int len) throws BadXdrOncRpcException;
    boolean xdrDecodeBoolean() throws BadXdrOncRpcException;
    String xdrDecodeString() throws BadXdrOncRpcException;
    long xdrDecodeLong() throws BadXdrOncRpcException;
    long[] xdrDecodeLongVector() throws BadXdrOncRpcException;
    long[] xdrDecodeLongFixedVector(int len) throws BadXdrOncRpcException;
    ByteBuffer xdrDecodeByteBuffer() throws BadXdrOncRpcException;
    float xdrDecodeFloat() throws BadXdrOncRpcException;
    double xdrDecodeDouble() throws BadXdrOncRpcException;
    double[] xdrDecodeDoubleVector() throws BadXdrOncRpcException;
    double[] xdrDecodeDoubleFixedVector(int length) throws BadXdrOncRpcException;
    float[] xdrDecodeFloatVector() throws BadXdrOncRpcException;
    float[] xdrDecodeFloatFixedVector(int length) throws BadXdrOncRpcException;
    byte[] xdrDecodeByteVector() throws BadXdrOncRpcException;
    byte[] xdrDecodeByteFixedVector(int length) throws BadXdrOncRpcException;
    byte xdrDecodeByte() throws BadXdrOncRpcException;
    short xdrDecodeShort() throws BadXdrOncRpcException;
    short[] xdrDecodeShortVector() throws BadXdrOncRpcException;
    short[] xdrDecodeShortFixedVector(int length) throws BadXdrOncRpcException;

    /*
     * Fake interface for compatibility with Remote Tea RPC library
     *
     */
}
