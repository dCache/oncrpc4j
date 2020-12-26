/*
 * Copyright (c) 2020 Deutsches Elektronen-Synchroton,
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
package org.dcache.oncrpc4j.rpc;

/**
 * Defines how memory allocation should take place.
 *
 * For RPC messages smaller than 1KB a HEAP allocator is recommended. A bigger messages
 * might benefit from pooled allocators as garbage collect doesn't need to allocate big
 * heap regions.
 *
 * If application requires direct buffers, typically to optimize native IO, a direct allocators
 * can be used.
 *
 * @since 3.2
 */
public enum MemoryAllocator {

    /** Use default allocator. Note, that default might change between different version of the library. */
    DEFAULT,

    /** Allocator, that uses JVM heap memory. */
    HEAP,

    /** Allocator, that uses Direct memory. */
    DIRECT,

    /** Heap based allocator, that re-uses previously heap-allocated buffers. */
    POOLED_HEAP,

    /** Heap based allocator, that re-uses previously direct-allocated buffers. */
    POOLED_DIRECT
}
