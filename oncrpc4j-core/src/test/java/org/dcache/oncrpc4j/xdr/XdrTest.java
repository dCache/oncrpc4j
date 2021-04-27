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
import org.dcache.oncrpc4j.util.Bytes;
import java.nio.ByteOrder;
import java.util.Arrays;
import org.dcache.oncrpc4j.grizzly.GrizzlyMemoryManager;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.memory.BuffersBuffer;
import org.glassfish.grizzly.memory.CompositeBuffer;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class XdrTest {

    private Buffer _buffer;


    @Before
    public void setUp() {
        _buffer = allocateBuffer(1024);
        _buffer.order(ByteOrder.BIG_ENDIAN);
    }

    @Test
    public void testDecodeInt() throws BadXdrOncRpcException {

        int value = 17;
        _buffer.putInt(value);

        Xdr xdr = new Xdr(_buffer);
        xdr.beginDecoding();

        assertEquals("Decode value incorrect", 17, xdr.xdrDecodeInt());
    }

    @Test
    public void testEncodeDecodeOpaque() throws BadXdrOncRpcException {

        byte[] data = "some random data".getBytes();
        XdrEncodingStream encoder = new Xdr(_buffer);
        encoder.beginEncoding();
        encoder.xdrEncodeDynamicOpaque(data);
        encoder.endEncoding();

        XdrDecodingStream decoder = new Xdr(_buffer);
        decoder.beginDecoding();
        byte[] decoded = decoder.xdrDecodeDynamicOpaque();

        assertArrayEquals("encoded/decoded data do not match", data, decoded);
    }

    @Test
    public void testEncodeDecodeOpaque2() throws BadXdrOncRpcException {

        byte[] data = "some random data".getBytes();
        XdrEncodingStream encoder = new Xdr(_buffer);
        encoder.beginEncoding();
        encoder.xdrEncodeOpaque(data, data.length);
        encoder.endEncoding();

        XdrDecodingStream decoder = new Xdr(_buffer);
        decoder.beginDecoding();
        byte[] decoded = decoder.xdrDecodeOpaque(data.length);

        assertArrayEquals("encoded/decoded data do not match", data, decoded);
    }

    @Test
    public void testDecodeBooleanTrue() throws BadXdrOncRpcException {

        _buffer.putInt(1);

        Xdr xdr = new Xdr(_buffer);
        xdr.beginDecoding();
        assertTrue("Decoded value incorrect", xdr.xdrDecodeBoolean() );
    }

    @Test
    public void testEncodeDecodeBooleanTrue() throws BadXdrOncRpcException {

        boolean value = true;
        XdrEncodingStream encoder = new Xdr(_buffer);
        encoder.beginEncoding();
        encoder.xdrEncodeBoolean(value);
        encoder.endEncoding();

        XdrDecodingStream decoder = new Xdr(_buffer);
        decoder.beginDecoding();

        boolean decoded = decoder.xdrDecodeBoolean();
        assertEquals("Decoded boolean value incorrect", value, decoded );
    }

    @Test
    public void testEncodeDecodeBooleanFalse() throws BadXdrOncRpcException {

        boolean value = false;
        XdrEncodingStream encoder = new Xdr(_buffer);
        encoder.beginEncoding();
        encoder.xdrEncodeBoolean(value);
        encoder.endEncoding();

        XdrDecodingStream decoder = new Xdr(_buffer);
        decoder.beginDecoding();

        boolean decoded = decoder.xdrDecodeBoolean();
        assertEquals("Decoded boolean value incorrect", value, decoded );
    }

    @Test
    public void testDecodeBooleanFale() throws BadXdrOncRpcException {

        _buffer.putInt(0);

        Xdr xdr = new Xdr(_buffer);
        xdr.beginDecoding();
        assertFalse("Decoded value incorrect", xdr.xdrDecodeBoolean() );
    }


    @Test
    public void testEncodeDecodeString() throws BadXdrOncRpcException {

        String original = "some random data";
        XdrEncodingStream encoder = new Xdr(_buffer);
        encoder.beginEncoding();
        encoder.xdrEncodeString(original);
        encoder.endEncoding();

        XdrDecodingStream decoder = new Xdr(_buffer);
        decoder.beginDecoding();

        String decoded = decoder.xdrDecodeString();

        assertEquals("encoded/decoded string do not match", original, decoded);
    }

    @Test
    public void testEncodeDecodeEmptyString() throws BadXdrOncRpcException {

        String original = "";
        XdrEncodingStream encoder = new Xdr(_buffer);
        encoder.beginEncoding();
        encoder.xdrEncodeString(original);
        encoder.endEncoding();

        XdrDecodingStream decoder = new Xdr(_buffer);
        decoder.beginDecoding();

        String decoded = decoder.xdrDecodeString();

        assertEquals("encoded/decoded string do not match", original, decoded);
    }

    @Test
    public void testEncodeDecodeNullString() throws BadXdrOncRpcException {

        String original = null;
        XdrEncodingStream encoder = new Xdr(_buffer);
        encoder.beginEncoding();
        encoder.xdrEncodeString(original);
        encoder.endEncoding();

        XdrDecodingStream decoder = new Xdr(_buffer);
        decoder.beginDecoding();

        String decoded = decoder.xdrDecodeString();

        assertEquals("encoded/decoded string do not match", "", decoded);
    }

    @Test
    public void testEncodeDecodeLong() throws BadXdrOncRpcException {

        long value = 7L << 32;
        XdrEncodingStream encoder = new Xdr(_buffer);
        encoder.beginEncoding();
        encoder.xdrEncodeLong(value);
        encoder.endEncoding();

        XdrDecodingStream decoder = new Xdr(_buffer);
        decoder.beginDecoding();

        long decoded = decoder.xdrDecodeLong();

        assertEquals("encoded/decoded long do not match", value, decoded);
    }

    @Test
    public void testEncodeDecodeMaxLong() throws BadXdrOncRpcException {

        long value = Long.MAX_VALUE;
        XdrEncodingStream encoder = new Xdr(_buffer);
        encoder.beginEncoding();
        encoder.xdrEncodeLong(value);
        encoder.endEncoding();

        XdrDecodingStream decoder = new Xdr(_buffer);
        decoder.beginDecoding();

        long decoded = decoder.xdrDecodeLong();

        assertEquals("encoded/decoded long do not match", value, decoded);
    }

    @Test
    public void testEncodeDecodeMinLong() throws BadXdrOncRpcException {

        long value = Long.MIN_VALUE;
        XdrEncodingStream encoder = new Xdr(_buffer);
        encoder.beginEncoding();
        encoder.xdrEncodeLong(value);
        encoder.endEncoding();

        XdrDecodingStream decoder = new Xdr(_buffer);
        decoder.beginDecoding();

        long decoded = decoder.xdrDecodeLong();

        assertEquals("encoded/decoded long do not match", value, decoded);
    }

    @Test
    public void testSizeConstructor() {

        Xdr xdr = new Xdr(1024);

        assertEquals("encode/decode buffer size mismatch", 1024, xdr.asBuffer().capacity());
    }

    @Test
    public void testAutoGrow() {
        Xdr xdr = new Xdr(10);
        xdr.beginEncoding();
        xdr.xdrEncodeLong(1);
        xdr.xdrEncodeLong(1);
    }

    @Test
    public void testAutoGrowWthCompositeBuffer() {
        CompositeBuffer buffer = BuffersBuffer.create();
        buffer.append( allocateBuffer(10));
        Xdr xdr = new Xdr(buffer);
        xdr.beginEncoding();
        xdr.xdrEncodeLong(1);
        xdr.xdrEncodeLong(1);
    }

    @Test(expected = BadXdrOncRpcException.class)
    public void testBadXdrWithInt() throws BadXdrOncRpcException {
        CompositeBuffer buffer = BuffersBuffer.create();
        buffer.append(allocateBuffer(10));
        Xdr xdr = new Xdr(buffer);
        xdr.beginEncoding();
        xdr.xdrEncodeInt(1);
        xdr.endEncoding();
        xdr.beginDecoding();
        xdr.xdrDecodeLong();
    }

    @Test(expected = BadXdrOncRpcException.class)
    public void testBadXdrWithOpaque() throws BadXdrOncRpcException {
        CompositeBuffer buffer = BuffersBuffer.create();
        buffer.append(allocateBuffer(10));
        byte[] b = new byte[10];
        Xdr xdr = new Xdr(buffer);
        xdr.beginEncoding();
        xdr.xdrEncodeOpaque(b, b.length);
        xdr.endEncoding();
        xdr.beginDecoding();
        xdr.xdrDecodeOpaque(b.length +5);
    }

    @Test(expected = BadXdrOncRpcException.class)
    public void testBadXdrOnCorrption() throws BadXdrOncRpcException {
        CompositeBuffer buffer = BuffersBuffer.create();
        buffer.append(allocateBuffer(10));
        int[] b = new int[10];
        Xdr xdr = new Xdr(buffer);
        xdr.beginEncoding();
        xdr.xdrEncodeIntVector(b);
        xdr.asBuffer().limit( xdr.asBuffer().position() -4);
        xdr.endEncoding();
        xdr.beginDecoding();
        xdr.xdrDecodeIntVector();
    }

    @Test(expected = BadXdrOncRpcException.class)
    public void testBadXdrOnNegativeArraySize() throws BadXdrOncRpcException {
        CompositeBuffer buffer = BuffersBuffer.create();
        buffer.append(allocateBuffer(10));
        int[] b = new int[10];
        Xdr xdr = new Xdr(buffer);
        xdr.beginEncoding();
        xdr.xdrEncodeInt(-2);  // len
        xdr.xdrEncodeInt(1);   // first int
        xdr.xdrEncodeInt(2);   // second int
        xdr.endEncoding();
        xdr.beginDecoding();
        xdr.xdrDecodeIntVector();
    }

    @Test
    public void testAvailalbleData() throws BadXdrOncRpcException {
        CompositeBuffer buffer = BuffersBuffer.create();
        buffer.append(allocateBuffer(10));
        Xdr xdr = new Xdr(buffer);
        xdr.beginEncoding();
        xdr.xdrEncodeInt(1);   // first int
        xdr.xdrEncodeInt(2);   // second int
        xdr.endEncoding();
        xdr.beginDecoding();

        assertTrue("available data not detected", xdr.hasMoreData());
        xdr.xdrDecodeInt();
        xdr.xdrDecodeInt();
        assertFalse("empty stream not detected", xdr.hasMoreData());
    }

    @Test
    public void testGetBytes() {
        Xdr xdr = new Xdr(128);

        xdr.beginEncoding();

        xdr.xdrEncodeBoolean(true);
        xdr.xdrEncodeLong(17);
        xdr.endEncoding();

        byte[] bytes = xdr.getBytes();


        assertEquals("Invalid array size", 4 + 8, bytes.length);
        assertEquals("invalid value", 1, Bytes.getInt(bytes, 0));
        assertEquals("invalid value", 17, Bytes.getLong(bytes, 4));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetBytesInUse() {
        Xdr xdr = new Xdr(128);

        xdr.beginEncoding();
        xdr.xdrEncodeBoolean(true);

        xdr.getBytes();
    }

    @Test
    public void testFixedFloatVector() throws BadXdrOncRpcException {
        float[] floats = new float[]{1.0f, 2.0f};

        Xdr xdr = new Xdr(128);
        xdr.beginEncoding();
        xdr.xdrEncodeFloatFixedVector(floats, 2);

        xdr.endEncoding();

        xdr = new Xdr(xdr.getBytes());
        xdr.beginDecoding();

        float[] decoded = xdr.xdrDecodeFloatFixedVector(2);
        assertArrayEquals(floats, decoded, 0.0000001f);
    }

    @Test
    public void testFloatVector() throws BadXdrOncRpcException {
        float[] floats = new float[]{1.0f, 2.0f, 3.5f};

        Xdr xdr = new Xdr(128);
        xdr.beginEncoding();
        xdr.xdrEncodeFloatVector(floats);

        xdr.endEncoding();

        xdr = new Xdr(xdr.getBytes());
        xdr.beginDecoding();

        float[] decoded = xdr.xdrDecodeFloatVector();
        assertArrayEquals(floats, decoded, 0.0000001f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalFixedFloatVector() throws BadXdrOncRpcException {
        Xdr xdr = new Xdr(128);
        xdr.beginEncoding();
        xdr.xdrEncodeFloatFixedVector(new float[]{0.1f, 0.2f}, 3);
    }

    @Test
    public void testFixedDoubleVector() throws BadXdrOncRpcException {
        double[] doubles = new double[]{1.0, 2.0};

        Xdr xdr = new Xdr(128);
        xdr.beginEncoding();
        xdr.xdrEncodeDoubleFixedVector(doubles, 2);

        xdr.endEncoding();

        xdr = new Xdr(xdr.getBytes());
        xdr.beginDecoding();

        double[] decoded = xdr.xdrDecodeDoubleFixedVector(2);
        assertArrayEquals(doubles, decoded, 0.0000001);
    }

    @Test
    public void testDoubleVector() throws BadXdrOncRpcException {
        double[] doubles = new double[]{1.0, 2.0, 3.5};

        Xdr xdr = new Xdr(128);
        xdr.beginEncoding();
        xdr.xdrEncodeDoubleVector(doubles);

        xdr.endEncoding();

        xdr = new Xdr(xdr.getBytes());
        xdr.beginDecoding();

        double[] decoded = xdr.xdrDecodeDoubleVector();
        assertArrayEquals(doubles, decoded, 0.0000001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalFixedDoubleVector() throws BadXdrOncRpcException {
        Xdr xdr = new Xdr(128);
        xdr.beginEncoding();
        xdr.xdrEncodeDoubleFixedVector(new double[]{0.1, 0.2}, 3);
    }

    @Test
    public void testIntVector() throws BadXdrOncRpcException {
        int[] ints = new int[]{1, 2, 3};

        Xdr xdr = new Xdr(128);
        xdr.beginEncoding();
        xdr.xdrEncodeIntVector(ints);

        xdr.endEncoding();

        xdr = new Xdr(xdr.getBytes());
        xdr.beginDecoding();

        int[] decoded = xdr.xdrDecodeIntVector();
        assertArrayEquals(ints, decoded);
    }

    @Test
    public void testFixedIntVector() throws BadXdrOncRpcException {
        int[] ints = new int[]{1, 2};

        Xdr xdr = new Xdr(128);
        xdr.beginEncoding();
        xdr.xdrEncodeIntFixedVector(ints, 2);

        xdr.endEncoding();

        xdr = new Xdr(xdr.getBytes());
        xdr.beginDecoding();

        int[] decoded = xdr.xdrDecodeIntFixedVector(2);
        assertArrayEquals(ints, decoded);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalFixedIntVector() throws BadXdrOncRpcException {
        Xdr xdr = new Xdr(128);
        xdr.beginEncoding();
        xdr.xdrEncodeIntFixedVector(new int[]{1, 2}, 3);
    }

    @Test
    public void testLongVector() throws BadXdrOncRpcException {
        long[] longs = new long[]{1, 2, 3};

        Xdr xdr = new Xdr(128);
        xdr.beginEncoding();
        xdr.xdrEncodeLongVector(longs);

        xdr.endEncoding();

        xdr = new Xdr(xdr.getBytes());
        xdr.beginDecoding();

        long[] decoded = xdr.xdrDecodeLongVector();
        assertArrayEquals(longs, decoded);
    }

    @Test
    public void testFixedLongVector() throws BadXdrOncRpcException {
        long[] longs = new long[]{1, 2};

        Xdr xdr = new Xdr(128);
        xdr.beginEncoding();
        xdr.xdrEncodeLongFixedVector(longs, 2);

        xdr.endEncoding();

        xdr = new Xdr(xdr.getBytes());
        xdr.beginDecoding();

        long[] decoded = xdr.xdrDecodeLongFixedVector(2);
        assertArrayEquals(longs, decoded);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalFixedLongVector() throws BadXdrOncRpcException {
        Xdr xdr = new Xdr(128);
        xdr.beginEncoding();
        xdr.xdrEncodeLongFixedVector(new long[]{1, 2}, 3);
    }

    @Test
    public void testShortVector() throws BadXdrOncRpcException {
        short[] shorts = new short[]{1, 2, 3};

        Xdr xdr = new Xdr(128);
        xdr.beginEncoding();
        xdr.xdrEncodeShortVector(shorts);

        xdr.endEncoding();

        xdr = new Xdr(xdr.getBytes());
        xdr.beginDecoding();

        short[] decoded = xdr.xdrDecodeShortVector();
        assertArrayEquals(shorts, decoded);
    }

    @Test
    public void testFixedShortVector() throws BadXdrOncRpcException {
        short[] shorts = new short[]{1, 2};

        Xdr xdr = new Xdr(128);
        xdr.beginEncoding();
        xdr.xdrEncodeShortFixedVector(shorts, 2);

        xdr.endEncoding();

        xdr = new Xdr(xdr.getBytes());
        xdr.beginDecoding();

        short[] decoded = xdr.xdrDecodeShortFixedVector(2);
        assertArrayEquals(shorts, decoded);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalFixedShortVector() throws BadXdrOncRpcException {

        Xdr xdr = new Xdr(128);
        xdr.beginEncoding();
        xdr.xdrEncodeShortFixedVector(new short[]{1, 2}, 3);
    }

    @Test
    public void testByteVector() throws BadXdrOncRpcException {
        byte[] bytes = new byte[]{1, 2, 3};

        Xdr xdr = new Xdr(128);
        xdr.beginEncoding();
        xdr.xdrEncodeByteVector(bytes);

        xdr.endEncoding();

        xdr = new Xdr(xdr.getBytes());
        xdr.beginDecoding();

        byte[] decoded = xdr.xdrDecodeByteVector();
        assertArrayEquals(bytes, decoded);
    }

    @Test
    public void testFixedByteVector() throws BadXdrOncRpcException {
        byte[] bytes = new byte[]{1, 2};

        Xdr xdr = new Xdr(128);
        xdr.beginEncoding();
        xdr.xdrEncodeByteFixedVector(bytes, 2);

        xdr.endEncoding();

        xdr = new Xdr(xdr.getBytes());
        xdr.beginDecoding();

        byte[] decoded = xdr.xdrDecodeByteFixedVector(2);
        assertArrayEquals(bytes, decoded);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalByteShortVector() throws BadXdrOncRpcException {

        Xdr xdr = new Xdr(128);
        xdr.beginEncoding();
        xdr.xdrEncodeByteFixedVector(new byte[]{1, 2}, 3);
    }

    @Test
    public void testByteBuffer() throws BadXdrOncRpcException {
        ByteBuffer buf = ByteBuffer.allocate(128);
        buf.putInt(1);
        buf.putDouble(3.14);
        buf.putChar('a');
        buf.flip();

        Xdr xdr = new Xdr(128);
        xdr.beginEncoding();
        xdr.xdrEncodeByteBuffer(buf);
        xdr.endEncoding();

        xdr = new Xdr(xdr.getBytes());
        xdr.beginDecoding();
        ByteBuffer decoded = xdr.xdrDecodeByteBuffer();

        assertEquals(decoded.getInt(), 1);
        assertEquals(decoded.getDouble(), 3.14, 0.00000001);
        assertEquals(decoded.getChar(), 'a');
    }

    @Test
    public void testRelaseBufferOnClose() {

        Buffer b = mock(Buffer.class);
        Xdr xdr = new Xdr(b);
        xdr.close();

        verify(b, times(1)).tryDispose();
    }

    private static Buffer allocateBuffer(int size) {
        return GrizzlyMemoryManager.allocate(size);
    }
}
