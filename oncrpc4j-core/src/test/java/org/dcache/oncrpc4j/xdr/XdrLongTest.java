package org.dcache.oncrpc4j.xdr;

import java.io.IOException;
import org.junit.Test;

import static org.junit.Assert.*;

public class XdrLongTest {

    @Test
    public void testDecode() throws IOException {

        long value = 2018L;

        XdrLong xLong = new XdrLong();
        try(Xdr xdr = new Xdr(8)) {
            xdr.beginEncoding();
            xdr.xdrEncodeLong(value);

            xdr.endEncoding();
            xdr.beginDecoding();
            xLong.xdrDecode(xdr);

            assertEquals("invalid value decode", value, xLong.longValue());
        }
    }

    @Test
    public void testEncode() throws IOException {


        XdrLong xLong = new XdrLong(2018);
        try (Xdr xdr = new Xdr(8)) {
            xdr.beginEncoding();
            xLong.xdrEncode(xdr);

            xdr.endEncoding();
            xdr.beginDecoding();

            assertEquals("invalid value decoded", xLong.longValue(), xdr.xdrDecodeLong());
        }
    }

    @Test
    public void testEncodeWellKnown() throws IOException {

        byte[] data = new byte[]{
            0x04, 0x21, 0x0, 0x02,
            0x54, 0x0b, 0x14, 0x11 // big endian encoded 297519060383110161
        };

        XdrLong xLong = new XdrLong(297519060383110161L);
        try (Xdr xdr = new Xdr(8)) {
            xdr.beginEncoding();
            xLong.xdrEncode(xdr);

            xdr.endEncoding();
            assertArrayEquals(data, xdr.getBytes());
        }
    }

    @Test
    public void testDecodeWellKnown() throws IOException {

        byte[] data = new byte[]{
            0x04, 0x21, 0x0, 0x02,
            0x54, 0x0b, 0x14, 0x11 // big endian encoded 297519060383110161
        };

        try (Xdr xdr = new Xdr(data)) {
            xdr.beginDecoding();
            XdrLong xLong = new XdrLong();
            xLong.xdrDecode(xdr);

            assertEquals(297519060383110161L, xLong.longValue());
        }
    }
}
