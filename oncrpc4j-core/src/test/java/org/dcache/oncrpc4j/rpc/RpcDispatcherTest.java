package org.dcache.oncrpc4j.rpc;

import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.security.auth.Subject;
import org.dcache.oncrpc4j.xdr.XdrVoid;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RpcDispatcherTest {

    private Map<OncRpcProgram, RpcDispatchable> programs;
    private RpcDispatcher dispatcher;

    private FilterChainContext context;

    private RpcCall call;

    private Consumer callInterceptor;

    private OncRpcProgram PROG_ONE = new OncRpcProgram(1, 1);

    @Before
    public void setUp() {
        programs = new HashMap<>();
        callInterceptor = mock(Consumer.class);
        dispatcher = new RpcDispatcher(MoreExecutors.newDirectExecutorService(), programs, true,
              callInterceptor);

        Subject s = new Subject();
        RpcAuth auth = mock(RpcAuth.class);
        when(auth.getSubject()).thenReturn(s);

        call = mock(RpcCall.class);
        when(call.getProgramVersion()).thenReturn(1);
        when(call.getProgram()).thenReturn(1);
        when(call.getCredential()).thenReturn(auth);

        context = mock(FilterChainContext.class);
        when(context.getMessage()).thenReturn(call);
    }

    @Test
    public void testSubjectPropagation() throws IOException {

        AtomicReference<Subject> callSubject = new AtomicReference<>();

        programs.put(PROG_ONE, (call) -> {
            Subject subject = Subject.getSubject(AccessController.getContext());
            callSubject.set(subject);
        });

        dispatcher.handleRead(context);

        assertSame("subject not propagated", call.getCredential().getSubject(), callSubject.get());
    }

    @Test
    public void testProgramUnavailable() throws IOException {

        dispatcher.handleRead(context);

        verify(call).failProgramUnavailable();
        verify(call).failProgramUnavailable();
    }

    @Test
    public void testRejected() throws IOException {

        programs.put(PROG_ONE, (call) -> {
            throw new RpcException(RpcRejectStatus.RPC_MISMATCH, "", XdrVoid.XDR_VOID);
        });

        dispatcher.handleRead(context);
        verify(call, atLeastOnce()).reject(anyInt(), any());
    }

    @Test
    public void testRpcGarbage() throws IOException {

        programs.put(PROG_ONE, (call) -> {
            throw new OncRpcRejectedException(1);
        });

        dispatcher.handleRead(context);
        verify(call).failRpcGarbage();
    }

    @Test
    public void testRpcGarbageOnIOException() throws IOException {

        programs.put(PROG_ONE, (call) -> {
            throw new IOException();
        });

        dispatcher.handleRead(context);
        verify(call).failRpcGarbage();
    }

    @Test
    public void testSystemError() throws IOException {

        programs.put(PROG_ONE, (call) -> {
            throw new RuntimeException();
        });

        try {
            dispatcher.handleRead(context);
            fail("runtime exception not propagated");
        } catch (RuntimeException e) {

        }

        verify(call).failRpcSystem();
    }

    @Test
    public void testCallInterceptor() throws IOException {

        programs.put(PROG_ONE, (call) -> {
        });

        dispatcher.handleRead(context);
        verify(callInterceptor).accept(any());
    }
}
