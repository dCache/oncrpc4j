/*
 * Copyright (c) 2009 - 2025 Deutsches Elektronen-Synchroton,
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.dcache.oncrpc4j.grizzly.GrizzlyMemoryManager;
import org.dcache.oncrpc4j.util.Channels;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.FileChunk;
import org.glassfish.grizzly.asyncqueue.WritableMessage;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.BuffersBuffer;
import org.glassfish.grizzly.memory.ByteBufferWrapper;
import org.glassfish.grizzly.memory.MemoryManager;

import static com.google.common.base.Preconditions.checkState;

public class Xdr implements XdrDecodingStream, XdrEncodingStream, AutoCloseable {

    /**
     * Maximal size of a XDR message.
     */
    public final static int MAX_XDR_SIZE = 512 * 1024;

    /**
     * Initial size of a freshly-allocated Xdr message
     */
    public final static int INITIAL_XDR_SIZE = 1024;

    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /**
     * Byte buffer used by XDR record.
     */
    protected volatile Buffer _buffer;

    /**
     * File chunk to be written after the buffer.
     */
    private volatile FileChunk _fileChunk;

    /**
     * Padding needed after the file chunk.
     */
    private int _padding;

    /**
     * Indicates that encoding/decoding is in progress.
     */
    private boolean _inUse;

    /**
     * Memory manager used to allocate, resize buffers.
     */
    private final MemoryManager _memoryManager;
    /**
     * Create a new Xdr object with a buffer of given size.
     *
     * @param size of the buffer in bytes
     */
    public Xdr(int size) {
        this(GrizzlyMemoryManager.allocate(size), GrizzlyMemoryManager.getDefaultMemoryManager());
    }


    /**
     * Wraps a byte array into a Xdr stream
     *
     * <p> The new Xdr will be backed by the given byte array;
     *  that is, modifications to the Xdr will cause the array to be modified
     * and vice versa.
     *
     * @param  bytes The array that will back this Xdr.
     */
    public Xdr(byte[] bytes) {
        this(GrizzlyMemoryManager.wrap(bytes), GrizzlyMemoryManager.getDefaultMemoryManager());
    }

    /**
     * Create a new XDR back ended with given {@link ByteBuffer}.
     * @param body buffer to use
     * @deprecated this constructor shouldn't be ued
     */
    @Deprecated
    public Xdr(Buffer body) {
        this(body, GrizzlyMemoryManager.getDefaultMemoryManager());
    }

    /**
     * Create a new XDR back ended with given {@link ByteBuffer}.
     * @param body buffer to use
     * @param memoryManager memory manager used to allocate provided buffer.
     */
    public Xdr(Buffer body, MemoryManager memoryManager) {
        _memoryManager = memoryManager;
        _buffer = body;
        _buffer.order(ByteOrder.BIG_ENDIAN);
    }

    @Override
    public void beginDecoding() {
        /*
         * Set position to the beginning of this XDR in back end buffer.
         */
        _buffer.rewind();
        _inUse = true;
    }

    @Override
    public void endDecoding() {
        _buffer.rewind();
        _inUse = false;
    }

    @Override
    public void beginEncoding() {
        _buffer.clear();
        _inUse = true;
    }

    @Override
    public void endEncoding() {
        _buffer.flip();
        _inUse = false;
    }

    /**
     * Tells whether there are any data available in the stream.
     * @return true if, and only if, there is data available in the stream
     */
    public boolean hasMoreData() {
        return _buffer.hasRemaining();
    }

    public boolean hasFileChunk() {
        return _fileChunk != null;
    }

    /**
     * Decodes (aka "deserializes") a "XDR int" value received from a
     * XDR stream. A XDR int is 32 bits wide -- the same width Java's "int"
     * data type has. This method is one of the basic methods all other
     * methods can rely on. Because it's so basic, derived classes have to
     * implement it.
     *
     * @return The decoded int value.
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public int xdrDecodeInt() throws BadXdrOncRpcException {
        ensureBytes(Integer.BYTES);
        int val = _buffer.getInt();
        return val;
    }

    /**
     * Get next array of integers.
     *
     * @return the array on integers
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public int[] xdrDecodeIntVector() throws BadXdrOncRpcException {

        int len = xdrDecodeInt();
        checkArraySize(len);
        int[] ints = new int[len];
        for (int i = 0; i < len; i++) {
            ints[i] = xdrDecodeInt();
        }
        return ints;
    }

    /**
     * Decodes (aka "deserializes") a vector of ints read from a XDR stream.
     *
     * @param length of vector to read.
     *
     * @return Decoded int vector.
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public int[] xdrDecodeIntFixedVector(int length) throws BadXdrOncRpcException {
        int[] value = new int[length];
        for (int i = 0; i < length; ++i) {
            value[i] = xdrDecodeInt();
        }
        return value;
    }

    /**
     * Get next array of long.
     *
     * @return the array on integers
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public long[] xdrDecodeLongVector() throws BadXdrOncRpcException {

        int len = xdrDecodeInt();
        checkArraySize(len);
        long[] longs = new long[len];
        for (int i = 0; i < len; i++) {
            longs[i] = xdrDecodeLong();
        }
        return longs;
    }

    /**
     * Decodes (aka "deserializes") a vector of longs read from a XDR stream.
     *
     * @param length of vector to read.
     *
     * @return Decoded long vector.
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public long[] xdrDecodeLongFixedVector(int length) throws BadXdrOncRpcException {
        long[] value = new long[length];
        for (int i = 0; i < length; ++i) {
            value[i] = xdrDecodeLong();
        }
        return value;
    }

    /**
     * Decodes (aka "deserializes") a float (which is a 32 bits wide floating
     * point entity) read from a XDR stream.
     *
     * @return Decoded float value.rs.
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public float xdrDecodeFloat() throws BadXdrOncRpcException {
        return Float.intBitsToFloat(xdrDecodeInt());
    }

    /**
     * Decodes (aka "deserializes") a double (which is a 64 bits wide floating
     * point entity) read from a XDR stream.
     *
     * @return Decoded double value.rs.
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public double xdrDecodeDouble() throws BadXdrOncRpcException {
        return Double.longBitsToDouble(xdrDecodeLong());
    }

    /**
     * Decodes (aka "deserializes") a vector of doubles read from a XDR stream.
     *
     * @return Decoded double vector.
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public double[] xdrDecodeDoubleVector() throws BadXdrOncRpcException {
        int length = xdrDecodeInt();
        checkArraySize(length);
        return xdrDecodeDoubleFixedVector(length);
    }

    /**
     * Decodes (aka "deserializes") a vector of doubles read from a XDR stream.
     *
     * @param length of vector to read.
     *
     * @return Decoded double vector.
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public double[] xdrDecodeDoubleFixedVector(int length) throws BadXdrOncRpcException {
        double[] value = new double[length];
        for (int i = 0; i < length; ++i) {
            value[i] = xdrDecodeDouble();
        }
        return value;
    }

    /**
     * Decodes (aka "deserializes") a vector of floats read from a XDR stream.
     *
     * @return Decoded float vector.
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public float[] xdrDecodeFloatVector() throws BadXdrOncRpcException {
        int length = xdrDecodeInt();
        checkArraySize(length);
        return xdrDecodeFloatFixedVector(length);
    }

    /**
     * Decodes (aka "deserializes") a vector of floats read from a XDR stream.
     *
     * @param length of vector to read.
     *
     * @return Decoded float vector.
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public float[] xdrDecodeFloatFixedVector(int length) throws BadXdrOncRpcException {
        float[] value = new float[length];
        for (int i = 0; i < length; ++i) {
            value[i] = xdrDecodeFloat();
        }
        return value;
    }

    /**
     * Get next opaque data.  The decoded data
     * is always padded to be a multiple of four.
     *
     * @param buf buffer where date have to be stored
     * @param offset in the buffer.
     * @param len number of bytes to read.
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public void xdrDecodeOpaque(byte[] buf, int offset, int len) throws BadXdrOncRpcException {
        if (len == 0) {
            return;
        }
        int padding = (4 - (len & 3)) & 3;
        ensureBytes(len + padding);
        _buffer.get(buf, offset, len);
        _buffer.position(_buffer.position() + padding);
    }

    public void xdrDecodeOpaque(byte[] buf,  int len) throws BadXdrOncRpcException {
        xdrDecodeOpaque(buf, 0, len);
    }

    @Override
    public byte[] xdrDecodeOpaque(int len) throws BadXdrOncRpcException {
        if (len == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        byte[] opaque = new byte[len];
        xdrDecodeOpaque(opaque, len);
        return opaque;
    }

    /**
     * Decodes (aka "deserializes") a XDR opaque value, which is represented
     * by a vector of byte values. The length of the opaque value to decode
     * is pulled off of the XDR stream, so the caller does not need to know
     * the exact length in advance. The decoded data is always padded to be
     * a multiple of four (because that's what the sender does).
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public byte [] xdrDecodeDynamicOpaque() throws BadXdrOncRpcException {
        int length = xdrDecodeInt();
        if (length == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        checkArraySize(length);
        byte [] opaque = new byte[length];
        xdrDecodeOpaque(opaque, 0, length);
        return opaque;
    }

    /**
     * Get next String.
     *
     * @return decoded string
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public String xdrDecodeString() throws BadXdrOncRpcException {
        int len = xdrDecodeInt();
        if (len == 0) {
          return "";
        }
        checkArraySize(len);
        byte[] bytes = new byte[len];
        xdrDecodeOpaque(bytes, 0, len);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public boolean xdrDecodeBoolean() throws BadXdrOncRpcException {
        int bool = xdrDecodeInt();
        return bool != 0;
    }

    /**
     * Decodes (aka "deserializes") a long (which is called a "hyper" in XDR
     * babble and is 64&nbsp;bits wide) read from a XDR stream.
     *
     * @return Decoded long value.
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public long xdrDecodeLong() throws BadXdrOncRpcException {
        ensureBytes(Long.BYTES);
        return _buffer.getLong();
    }

    @Override
    public ByteBuffer xdrDecodeByteBuffer() throws BadXdrOncRpcException {
        int len = this.xdrDecodeInt();
        checkArraySize(len);
        int padding = (4 - (len & 3)) & 3;

        ensureBytes(len + padding);
       /*
        * as of grizzly 2.2.1 toByteBuffer returns a ByteBuffer view of
        * the backended heap. To be able to use rewind, flip and so on
        * we have to use slice of it.
        */
        ByteBuffer slice = _buffer.toByteBuffer().slice();
        slice.rewind();
        slice.limit(len);
        _buffer.position(_buffer.position() + len + padding);
        return slice;
    }

    /**
     * Decodes (aka "deserializes") a vector of bytes, which is nothing more
     * than a series of octets (or 8 bits wide bytes), each packed into its very
     * own 4 bytes (XDR int). Byte vectors are decoded together with a
     * preceeding length value. This way the receiver doesn't need to know the
     * length of the vector in advance.
     *
     * @return The byte vector containing the decoded data.
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public byte[] xdrDecodeByteVector() throws BadXdrOncRpcException {
        int length = xdrDecodeInt();
        checkArraySize(length);
        return xdrDecodeByteFixedVector(length);
    }

    /**
     * Decodes (aka "deserializes") a vector of bytes, which is nothing more
     * than a series of octets (or 8 bits wide bytes), each packed into its very
     * own 4 bytes (XDR int).
     *
     * @param length of vector to read.
     *
     * @return The byte vector containing the decoded data.
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public byte[] xdrDecodeByteFixedVector(int length) throws BadXdrOncRpcException {

        byte[] bytes = new byte[length];
        for (int i = 0; i < length; ++i) {
            bytes[i] = xdrDecodeByte();
        }
        return bytes;
    }

    /**
     * Decodes (aka "deserializes") a byte read from this XDR stream.
     *
     * @return Decoded byte value.
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public byte xdrDecodeByte() throws BadXdrOncRpcException {
        return (byte) xdrDecodeInt();
    }

    /**
     * Decodes (aka "deserializes") a short (which is a 16 bit quantity) read
     * from this XDR stream.
     *
     * @return Decoded short value.
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public short xdrDecodeShort() throws BadXdrOncRpcException {
        return (short) xdrDecodeInt();
    }

    /**
     * Decodes (aka "deserializes") a vector of short integers read from a XDR
     * stream.
     *
     * @return Decoded vector of short integers.
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public short[] xdrDecodeShortVector() throws BadXdrOncRpcException {
        int length = xdrDecodeInt();
        checkArraySize(length);
        return xdrDecodeShortFixedVector(length);
    }

    /**
     * Decodes (aka "deserializes") a vector of short integers read from a XDR
     * stream.
     *
     * @param length of vector to read.
     *
     * @return Decoded vector of short integers.
     * @throws BadXdrOncRpcException if xdr stream can't be decoded.
     */
    @Override
    public short[] xdrDecodeShortFixedVector(int length) throws BadXdrOncRpcException {
        short[] value = new short[length];
        for (int i = 0; i < length; ++i) {
            value[i] = xdrDecodeShort();
        }
        return value;
    }
    ////////////////////////////////////////////////////////////////////////////
    //
    //         Encoder
    //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Encodes (aka "serializes") a "XDR int" value and writes it down a
     * XDR stream. A XDR int is 32 bits wide -- the same width Java's "int"
     * data type has. This method is one of the basic methods all other
     * methods can rely on.
     */
    @Override
    public void xdrEncodeInt(int value) {
        ensureCapacity(Integer.BYTES);
        _buffer.putInt(value);
    }

    /**
     * Returns the {@link Buffer} that backs this xdr.
     *
     * <p>Modifications to this xdr's content will cause the returned
     * buffer's content to be modified, and vice versa.
     *
     * @return The {@link Buffer} that backs this xdr
     */
    public Buffer asBuffer() {
        if (_fileChunk != null) {
            try {
                var tmp = _memoryManager.allocate(_fileChunk.remaining() + _padding);
                _fileChunk.writeTo(Channels.asWritableChannel(tmp));
                // mind the padding
                tmp.position(tmp.position() + _padding);
                tmp.flip();
                _fileChunk = null;
                _padding = 0;
                _buffer = BuffersBuffer.create(_memoryManager, _buffer, tmp);

            } catch (IOException e) {
                throw new UncheckedIOException("Failed to write file chunk to buffer", e);
            }
        }
        return _buffer;
    }

    /**
     * Returns an array of {@link WritableMessage} that represent this xdr.
     *
     * @return array of {@link WritableMessage} that represent this xdr.
     */
    public WritableMessage[] asBufferWritableMessages() {

        if (_fileChunk == null) {
            return new WritableMessage[]{_buffer};
        }

        WritableMessage[] asWritableMessages;
        if (_padding == 0) {
            asWritableMessages = new WritableMessage[]{_buffer, _fileChunk};
        } else {
            asWritableMessages = new WritableMessage[]{_buffer, _fileChunk, Buffers.wrap(_memoryManager, paddingZeros, 0, _padding)};
        }
        _fileChunk = null;
        _padding = 0;
        return asWritableMessages;
    }

    /**
     * Encodes (aka "serializes") a vector of ints and writes it down
     * this XDR stream.
     *
     * @param values int vector to be encoded.
     *
     */
    @Override
    public void xdrEncodeIntVector(int[] values) {
        ensureCapacity(Integer.BYTES+Integer.BYTES*values.length);
        _buffer.putInt(values.length);
        for (int value: values) {
            _buffer.putInt( value );
        }
    }

    /**
     * Encodes (aka "serializes") a vector of ints and writes it down this XDR
     * stream.
     *
     * @param value int vector to be encoded.
     * @param length of vector to write. This parameter is used as a sanity
     * check.
     */
    @Override
    public void xdrEncodeIntFixedVector(int[] value, int length) {
        if (value.length != length) {
            throw new IllegalArgumentException("array size does not match protocol specification");
        }
        for (int i = 0; i < length; i++) {
            xdrEncodeInt(value[i]);
        }
    }

    /**
     * Encodes (aka "serializes") a vector of longs and writes it down
     * this XDR stream.
     *
     * @param values long vector to be encoded.
     *
     */
    @Override
    public void xdrEncodeLongVector(long[] values) {
        ensureCapacity(Integer.BYTES+Long.BYTES*values.length);
        _buffer.putInt(values.length);
        for (long value : values) {
            _buffer.putLong(value);
        }
    }

    /**
     * Encodes (aka "serializes") a vector of longs and writes it down this XDR
     * stream.
     *
     * @param value long vector to be encoded.
     * @param length of vector to write. This parameter is used as a sanity
     * check.
     */
    @Override
    public void xdrEncodeLongFixedVector(long[] value, int length) {
        if (value.length != length) {
            throw new IllegalArgumentException("array size does not match protocol specification");
        }
        for (int i = 0; i < length; i++) {
            xdrEncodeLong(value[i]);
        }
    }
    
    /**
     * Encodes (aka "serializes") a float (which is a 32 bits wide floating
     * point quantity) and write it down this XDR stream.
     *
     * @param value Float value to encode.
     */
    @Override
    public void xdrEncodeFloat(float value) {
        xdrEncodeInt(Float.floatToIntBits(value));
    }

    /**
     * Encodes (aka "serializes") a double (which is a 64 bits wide floating
     * point quantity) and write it down this XDR stream.
     *
     * @param value Double value to encode.
     */
    @Override
    public void xdrEncodeDouble(double value) {
        xdrEncodeLong(Double.doubleToLongBits(value));
    }

    /**
     * Encodes (aka "serializes") a vector of floats and writes it down this XDR
     * stream.
     *
     * @param value float vector to be encoded.
     */
    @Override
    public void xdrEncodeFloatVector(float[] value) {
        int size = value.length;
        xdrEncodeInt(size);
        for (int i = 0; i < size; i++) {
            xdrEncodeFloat(value[i]);
        }
    }

    /**
     * Encodes (aka "serializes") a vector of floats and writes it down this XDR
     * stream.
     *
     * @param value float vector to be encoded.
     * @param length of vector to write. This parameter is used as a sanity
     * check.
     */
    @Override
    public void xdrEncodeFloatFixedVector(float[] value, int length) {
        if (value.length != length) {
            throw new IllegalArgumentException("array size does not match protocol specification");
        }
        for (int i = 0; i < length; i++) {
            xdrEncodeFloat(value[i]);
        }
    }

    /**
     * Encodes (aka "serializes") a vector of doubles and writes it down this
     * XDR stream.
     *
     * @param value double vector to be encoded.
     */
    @Override
    public void xdrEncodeDoubleVector(double[] value) {
        int size = value.length;
        xdrEncodeInt(size);
        for (int i = 0; i < size; i++) {
            xdrEncodeDouble(value[i]);
        }
    }

    /**
     * Encodes (aka "serializes") a vector of doubles and writes it down this
     * XDR stream.
     *
     * @param value double vector to be encoded.
     * @param length of vector to write. This parameter is used as a sanity
     * check.
     */
    @Override
    public void xdrEncodeDoubleFixedVector(double[] value, int length) {
        if (value.length != length) {
            throw new IllegalArgumentException("array size does not match protocol specification");
        }
        for (int i = 0; i < length; i++) {
            xdrEncodeDouble(value[i]);
        }
    }

    /**
     * Encodes (aka "serializes") a string and writes it down this XDR stream.
     *
     */
    @Override
    public void xdrEncodeString(String string) {
        if( string == null ) string = "";
        xdrEncodeDynamicOpaque(string.getBytes(StandardCharsets.UTF_8));
    }

    private static final byte [] paddingZeros = { 0, 0, 0, 0 };

    /**
     * Encodes (aka "serializes") a XDR opaque value, which is represented
     * by a vector of byte values. Only the opaque value is encoded, but
     * no length indication is preceeding the opaque value, so the receiver
     * has to know how long the opaque value will be. The encoded data is
     * always padded to be a multiple of four. If the length of the given byte
     * vector is not a multiple of four, zero bytes will be used for padding.
     */
    @Override
    public void xdrEncodeOpaque(byte[] bytes, int offset, int len) {
        int padding = (4 - (len & 3)) & 3;
        ensureCapacity(len+padding);
        _buffer.put(bytes, offset, len);
        _buffer.put(paddingZeros, 0, padding);
    }

    @Override
    public void xdrEncodeOpaque(byte[] bytes, int len) {
        xdrEncodeOpaque(bytes, 0, len);
    }

    /**
     * Encodes (aka "serializes") a XDR opaque value, which is represented
     * by a vector of byte values. The length of the opaque value is written
     * to the XDR stream, so the receiver does not need to know
     * the exact length in advance. The encoded data is always padded to be
     * a multiple of four to maintain XDR alignment.
     *
     */
    @Override
    public void xdrEncodeDynamicOpaque(byte [] opaque) {
        xdrEncodeInt(opaque.length);
        xdrEncodeOpaque(opaque, 0, opaque.length);
    }

    @Override
    public void xdrEncodeBoolean(boolean bool) {
        xdrEncodeInt( bool ? 1 : 0);
    }

    /**
     * Encodes (aka "serializes") a long (which is called a "hyper" in XDR
     * babble and is 64&nbsp;bits wide) and write it down this XDR stream.
     */
    @Override
    public void xdrEncodeLong(long value) {
        ensureCapacity(Long.BYTES);
       _buffer.putLong(value);
    }

    /**
     * Encodes (aka "serializes") a sequence of bytes from the given buffer
     * to this Xdr stream.
     *
     * @param buf The buffer from which bytes are to be retrieved.
     */
    @Override
    public void xdrEncodeByteBuffer(ByteBuffer buf) {
        int len = buf.remaining();
        int padding = (4 - (len & 3)) & 3;
        xdrEncodeInt(len);
        ensureCapacity(len+padding);
        _buffer.put(buf);
        _buffer.position(_buffer.position() + padding);
    }

    /**
     * A version of {@link #xdrEncodeByteBuffer(ByteBuffer)} which avoids internal copy.
     * Note: any change to the {@code buf} will cause unpredicted behavior.
     *
     * @param buf The buffer from which bytes are to be retrieved.
     */
    public void xdrEncodeShallowByteBuffer(ByteBuffer buf) {
        var wrap = new ByteBufferWrapper(buf);
        wrap.allowBufferDispose(true);
        xdrEncodeShallowByteBuffer(wrap);
    }

    /**
     * A version of {@link #xdrEncodeShallowByteBuffer(ByteBuffer)} which uses Grizzly {@code buffer}.
     * Note: any change to the {@code buf} will cause unpredicted behavior.
     *
     * @param buf The buffer from which bytes are to be retrieved.
     */
    public void xdrEncodeShallowByteBuffer(Buffer buf) {
        int len = buf.remaining();
        int padding = (4 - (len & 3)) & 3;
        xdrEncodeInt(len);
        int ep = _buffer.position() + buf.remaining();

        var composite = BuffersBuffer.create(_memoryManager);
        composite.allowBufferDispose(true);
        composite.append(_buffer.slice(0, _buffer.position())).append(buf);
        composite.position(ep);
        composite.limit(ep);

        _buffer = composite;
        ensureCapacity(padding);
        _buffer.position(_buffer.position() + padding);
    }

    /**
     * Encodes (aka "serializes") a vector of bytes, which is nothing more than
     * a series of octets (or 8 bits wide bytes), each packed into its very own
     * 4 bytes (XDR int). Byte vectors are encoded together with a preceeding
     * length value. This way the receiver doesn't need to know the length of
     * the vector in advance.
     *
     * @param value Byte vector to encode.
     */
    @Override
    public void xdrEncodeByteVector(byte[] value) {
        int length = value.length; // well, silly optimizations appear here...
        xdrEncodeInt(length);
        //
        // For speed reasons, we do sign extension here, but the higher bits
        // will be removed again when deserializing.
        //
        for (int i = 0; i < length; ++i) {
            xdrEncodeByte(value[i]);
        }
    }

    /**
     * Encodes (aka "serializes") a vector of bytes, which is nothing more than
     * a series of octets (or 8 bits wide bytes), each packed into its very own
     * 4 bytes (XDR int).
     *
     * @param value Byte vector to encode.
     * @param length of vector to write. This parameter is used as a sanity
     * check.
     */
    @Override
    public void xdrEncodeByteFixedVector(byte[] value, int length) {
        if (value.length != length) {
            throw new IllegalArgumentException("array size does not match protocol specification");
        }
        //
        // For speed reasons, we do sign extension here, but the higher bits
        // will be removed again when deserializing.
        //
        for (int i = 0; i < length; ++i) {
            xdrEncodeInt((int) value[i]);
        }
    }

    /**
     * Encodes (aka "serializes") a byte and write it down this XDR stream.
     *
     * @param value byte value to encode.
     */
    @Override
    public void xdrEncodeByte(byte value) {
        //
        // For speed reasons, we do sign extension here, but the higher bits
        // will be removed again when deserializing.
        //
        xdrEncodeInt((int) value);
    }

    /**
     * Encodes (aka "serializes") a short (which is a 16 bits wide quantity) and
     * write it down this XDR stream.
     *
     * @param value Short value to encode.
     */
    @Override
    public void xdrEncodeShort(short value) {
        xdrEncodeInt((int) value);
    }

    /**
     * Encodes (aka "serializes") a vector of short integers and writes it down
     * this XDR stream.
     *
     * @param value short vector to be encoded.
     */
    @Override
    public void xdrEncodeShortVector(short[] value) {
        int size = value.length;
        xdrEncodeInt(size);
        for (int i = 0; i < size; i++) {
            xdrEncodeShort(value[i]);
        }
    }

    /**
     * Encodes (aka "serializes") a vector of short integers and writes it down
     * this XDR stream.
     *
     * @param value short vector to be encoded.
     * @param length of vector to write. This parameter is used as a sanity
     * check.
     */
    @Override
    public void xdrEncodeShortFixedVector(short[] value, int length) {
        if (value.length != length) {
            throw new IllegalArgumentException("array size does not match protocol specification");
        }
        for (int i = 0; i < length; i++) {
            xdrEncodeShort(value[i]);
        }
    }

    /**
     * Encodes (aka "serializes") a file chunk by writing it directly to the
     * underlying buffer. This method avoids an internal copy of the file chunk
     * data.
     *
     * @param chunk The file chunk to be encoded.
     */
    public void xdrEncodeFileChunk(FileChunk chunk) {

        // REVISIT: support multiple file chunks in the future
        if (_fileChunk != null) {
            throw new IllegalArgumentException("Multiple file chunks are not supported");
        }
        var len = chunk.remaining();
        xdrEncodeInt(len);
        _padding = (4 - (len & 3)) & 3;
        _fileChunk = chunk;
    }

    /**
     * Returns the array that contains the xdr encoded data. The changes in the Xdr will not be
     * visible to the array and vice versa. The encoding process must be finished before {@code getBytes}
     * method is called.
     *
     * @return The array that contains buffer's data.
     * @throws IllegalStateException if {@link Xdr#endEncoding()} is not called before.
     */
    public byte[] getBytes() {
        checkState(!_inUse, "getBytes called while buffer in use");
        int size = _buffer.remaining();
        byte[] bytes = new byte[size];
        _buffer.mark();
        _buffer.get(bytes);
        _buffer.reset();
        return bytes;
    }

    /**
     * Closes this stream, relinquishing any underlying resources.
     */
    public void close() {
        _buffer.tryDispose();
        if (_fileChunk != null) {
            _fileChunk.release();
            _fileChunk = null;
            _padding = 0;
        }
    }

    private void ensureCapacity(int size) {
        if(_buffer.remaining() < size) {
            int oldCapacity = _buffer.capacity();
            int newCapacity = Math.max((oldCapacity * 3) / 2 + 1, oldCapacity + size);
            _buffer = GrizzlyMemoryManager.reallocate(_memoryManager, _buffer, newCapacity);
        }
    }

    private void ensureBytes(int size) throws BadXdrOncRpcException {
        if (_buffer.remaining() < size) {
            throw new BadXdrOncRpcException("xdr stream too short");
        }
    }

    private void checkArraySize(int len) throws BadXdrOncRpcException {
        if (len < 0) {
            throw new BadXdrOncRpcException("corrupted xdr");
        }
    }
}
