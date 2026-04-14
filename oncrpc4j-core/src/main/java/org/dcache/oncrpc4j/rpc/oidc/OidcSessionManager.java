package org.dcache.oncrpc4j.rpc.oidc;

import org.dcache.oncrpc4j.rpc.ISessionManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OidcSessionManager implements ISessionManager {

    private static final long SESSION_TTL = TimeUnit.MINUTES.toMillis(5);
    private final Map<Long, SessionEntry> _sessions = new ConcurrentHashMap<>();
    
    private final ScheduledExecutorService _scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread t = new Thread(runnable, "OIDC-Session-Cleaner");
        t.setDaemon(true);
        return t;
    });

    public OidcSessionManager() {
        _scheduler.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.MINUTES);
    }

    private static class HASH {
        public static long FNV1a64(byte[] data) {
            if (data == null) return 0;
            final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
            final long FNV_PRIME = 0x100000001b3L;
            long hash = FNV_OFFSET_BASIS;
            for (byte b : data) {
                hash ^= (b & 0xFF);
                hash *= FNV_PRIME;
            }
            return hash;
        }
    }

    private static class SessionEntry {
        final OidcSvcCtx _ctx;
        long _expiryTime;

        SessionEntry(OidcSvcCtx ctx, long expiryTime) {
            _ctx = ctx;
            _expiryTime = expiryTime;
        }

        boolean isExpired() { return System.currentTimeMillis() > _expiryTime; }
        void refresh(long ttlMillis) { _expiryTime = System.currentTimeMillis() + ttlMillis; }
    }

    public OidcSvcCtx createContext(byte[] handle) {
        long uid = HASH.FNV1a64(handle);
        
        // Remove old/expired session if it exists to allow re-init
        _sessions.compute(uid, (key, existing) -> {
            if (existing != null && !existing.isExpired()) {
                throw new OidcException.Protocol("Active session already exists for this handle");
            }
            return new SessionEntry(new OidcSvcCtx().uid(uid), System.currentTimeMillis() + SESSION_TTL);
        });
        
        return _sessions.get(uid)._ctx;
    }

    public OidcSvcCtx getContext(long uid) {
        SessionEntry entry = _sessions.get(uid);
        if (entry == null || entry.isExpired()) {
            if (entry != null) _sessions.remove(uid);
            throw new OidcException.Protocol("Session expired or invalid UID: " + uid);
        }
        entry.refresh(SESSION_TTL);
        return entry._ctx;
    }

    public void removeContext(long uid) {
        _sessions.remove(uid);
    }

    private void cleanup() {
        _sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
