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
package org.dcache.oncrpc4j.util;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tigran
 */
public class BytesTest {


    private byte[] _b;
    @Before
    public void setUp() {
        _b = new byte[8];
    }

    @Test(expected=IllegalArgumentException.class)
    public void testPutLongIntoSmall() {
        Bytes.putLong(_b, 6, 0);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testPutIntIntoSmall() {
        Bytes.putInt(_b, 6, 0);
    }

    @Test
    public void testPutLongExact() {
        Bytes.putLong(_b, _b.length - 8, 0);
    }

    @Test
    public void testPutIntExact() {
        Bytes.putInt(_b, _b.length - 4, 0);
    }

    @Test
    public void testPutGetLong() {
        long value = 1717;
        Bytes.putLong(_b, 0, value);
        assertEquals("put/get mismatch", value, Bytes.getLong(_b, 0));
    }

    @Test
    public void testPutGetInt() {
        int value = 1717;
        Bytes.putInt(_b, 0, value);
        assertEquals("put/get mismatch", value, Bytes.getInt(_b, 0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetLongTooSmallArray() {
        int value = 1717;
        Bytes.putLong(_b, 0, value);

        Bytes.getLong(_b, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetIntTooSmallArray() {
        int value = 1717;
        Bytes.putInt(_b, 0, value);

        Bytes.getInt(_b, 5);
    }
}