/*
 * Copyright (c) 2009 - 2022 Deutsches Elektronen-Synchroton,
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

import java.io.IOException;

import org.dcache.oncrpc4j.xdr.Xdr;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.memory.BuffersBuffer;
import org.glassfish.grizzly.memory.MemoryManager;

public class RpcMessageParserTCP extends BaseFilter {

    /**
     * RPC fragment record marker mask
     */
    public final static int RPC_LAST_FRAG = 0x80000000;
    /**
     * RPC fragment size mask
     */
    public final static int RPC_SIZE_MASK = 0x7fffffff;

    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {

        Buffer messageBuffer = ctx.getMessage();
        if (messageBuffer == null) {
            return ctx.getStopAction();
        }

        if (!isAllFragmentsArrived(messageBuffer)) {
            return ctx.getStopAction(messageBuffer);
        }

        ctx.setMessage(assembleXdr(messageBuffer, ctx.getMemoryManager()));

        final Buffer reminder = messageBuffer.hasRemaining()
                ? messageBuffer.split(messageBuffer.position()) : null;

        return ctx.getInvokeAction(reminder);
    }

    private boolean isAllFragmentsArrived(Buffer messageBuffer) {
        final Buffer buffer = messageBuffer;
        if (buffer.remaining() < 4) {
          return false;
        }

        buffer.mark();
        try {
          do {
            int messageMarker = buffer.getInt();
            int size = getMessageSize(messageMarker);

            /*
             * fragment size bigger than we have received
             */
            if (size > buffer.remaining()) {
                return false;
            }

            /*
             * complete fragment received
             */
            if (isLastFragment(messageMarker)) {
                return true;
            }

            /*
             * seek to the end of the current fragment
             */
            buffer.position(buffer.position() + size);
          } while(buffer.remaining() >= 4);
        } finally {
          buffer.reset();
        }

        return false;
    }

    private static int getMessageSize(int marker) {
        return marker & RPC_SIZE_MASK;
    }

    private static boolean isLastFragment(int marker) {
        return (marker & RPC_LAST_FRAG) != 0;
    }

    private Xdr assembleXdr(Buffer messageBuffer, MemoryManager memoryManager) {

        Buffer currentFragment;
        BuffersBuffer multipleFragments = null;

        boolean messageComplete;
        do {
            int messageMarker = messageBuffer.getInt();

            int size = getMessageSize(messageMarker);
            messageComplete = isLastFragment(messageMarker);

            int pos = messageBuffer.position();
            currentFragment = messageBuffer.slice(pos, pos + size);
            currentFragment.limit(size);

            messageBuffer.position(pos + size);
            if (!messageComplete & multipleFragments == null) {
                /*
                 * we use composite buffer only if required
                 * as they not for free.
                 */
                multipleFragments = BuffersBuffer.create(memoryManager);
            }

            if (multipleFragments != null) {
                multipleFragments.append(currentFragment);
            }
        } while (!messageComplete);

        return new Xdr(multipleFragments == null ? currentFragment : multipleFragments, memoryManager);
    }
}
