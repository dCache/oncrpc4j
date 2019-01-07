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
package org.dcache.oncrpc4j.grizzly;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.BuffersBuffer;
import org.glassfish.grizzly.memory.CompositeBuffer;
import org.glassfish.grizzly.memory.MemoryManager;

/**
 * Class to manage memory buffer allocation and reallocation
 */
public class GrizzlyMemoryManager {

    private static final MemoryManager GRIZZLY_MM =
            MemoryManager.DEFAULT_MEMORY_MANAGER;

    // Utility class
    private GrizzlyMemoryManager() {}

    public static Buffer allocate(int size) {
        return GRIZZLY_MM.allocate(size);
    }

    public static Buffer reallocate(Buffer oldBuffer, int newSize) {
        if (oldBuffer.isComposite()) {
            Buffer addon = allocate(newSize-oldBuffer.capacity());
            ((CompositeBuffer)oldBuffer).append(addon);
            return oldBuffer;
        }
        return GRIZZLY_MM.reallocate(oldBuffer, newSize);
    }

    public static Buffer wrap(byte[] bytes) {
        return Buffers.wrap(GRIZZLY_MM, bytes);
    }

    public static Buffer createComposite(Buffer...buffers) {
        return BuffersBuffer.create(GRIZZLY_MM, buffers);
    }

    public static BuffersBuffer create() {
        return BuffersBuffer.create();
    }
}
