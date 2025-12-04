/*
 * Copyright (c) 2025 Deutsches Elektronen-Synchroton,
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

import org.glassfish.grizzly.Buffer;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Utility class for working with {@link java.nio.channels.Channels}.
 */
public class Channels {

    private Channels() {
        // utility class
    }

    /**
     * This method takes {@link ByteBuffer} and wraps it into {@link WritableByteChannel}.
     * The returned channel allows writing data to the given buffer.
     *
     * @param buffer the buffer to write to
     * @return a writable channel that writes to the given buffer.
     */
    public static WritableByteChannel asWritableChannel(ByteBuffer buffer) {


        return new WritableByteChannel() {
            private volatile boolean isOpen = true;

            @Override
            public int write(ByteBuffer src) {

                if (!isOpen) {
                    throw new IllegalStateException("Channel is closed");
                }

                if (src.remaining() > buffer.remaining()) {
                    throw new BufferOverflowException();
                }

                int bytes = src.remaining();
                buffer.put(src);

                return bytes;
            }

            @Override
            public boolean isOpen() {
                return isOpen;
            }

            @Override
            public void close() {
                isOpen = false;
            }
        };
    }

    /**
     * This method takes {@link Buffer} and wraps it into {@link WritableByteChannel}.
     * The returned channel allows writing data to the given buffer.
     *
     * @param buffer the buffer to write to
     * @return a writable channel that writes to the given buffer.
     */
    public static WritableByteChannel asWritableChannel(Buffer buffer) {

        return new WritableByteChannel() {
            private volatile boolean isOpen = true;

            @Override
            public int write(ByteBuffer src) {

                if (!isOpen) {
                    throw new IllegalStateException("Channel is closed");
                }

                if (src.remaining() > buffer.remaining()) {
                    throw new BufferOverflowException();
                }

                int bytes = src.remaining();
                buffer.put(src);

                return bytes;
            }

            @Override
            public boolean isOpen() {
                return isOpen;
            }

            @Override
            public void close() {
                isOpen = false;
            }
        };
    }
}