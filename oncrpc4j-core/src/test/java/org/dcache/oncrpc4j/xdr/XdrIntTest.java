package org.dcache.oncrpc4j.xdr;

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

public class XdrIntTest {


    @Test
    public void testDecode() throws IOException {

        int value = 17;

        XdrInt xInt = new XdrInt();
        try(Xdr xdr = new Xdr(8)) {
            xdr.beginEncoding();
            xdr.xdrEncodeInt(value);

            xdr.endEncoding();

            xdr.beginDecoding();
            xInt.xdrDecode(xdr);

            assertEquals("invalid value decode", value, xInt.intValue());

        }
    }

    @Test
    public void testEncode() throws IOException {


        XdrInt xInt = new XdrInt(17);
        try (Xdr xdr = new Xdr(8)) {
            xdr.beginEncoding();
            xInt.xdrEncode(xdr);

            xdr.endEncoding();

            xdr.beginDecoding();

            assertEquals("invalid value decoded", xInt.intValue(), xdr.xdrDecodeInt());

        }
    }

    @Test
    public void testEncodeWellKnown() throws IOException {

        byte[] data = new byte[] {
            0x0, 0x0, 0x0, 0x11 // big endian encoded 17
        };

        XdrInt xInt = new XdrInt(17);
        try (Xdr xdr = new Xdr(8)) {
            xdr.beginEncoding();
            xInt.xdrEncode(xdr);

            xdr.endEncoding();
            assertArrayEquals(data, xdr.getBytes());
        }
    }


    @Test
    public void testDecodeWellKnown() throws IOException {

        byte[] data = new byte[]{
            0x0, 0x0, 0x0, 0x11 // big endian encoded 17
        };

        try (Xdr xdr = new Xdr(data)) {
            xdr.beginDecoding();
            XdrInt xInt = new XdrInt();
            xInt.xdrDecode(xdr);

            assertEquals(17, xInt.intValue());
        }
    }

}
