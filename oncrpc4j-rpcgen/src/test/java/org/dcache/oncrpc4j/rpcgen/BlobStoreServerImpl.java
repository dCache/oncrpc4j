package org.dcache.oncrpc4j.rpcgen;

import org.dcache.xdr.RpcCall;

import java.util.concurrent.ConcurrentHashMap;

public class BlobStoreServerImpl extends BlobStoreServer {
    private final ConcurrentHashMap<String, byte[]> store = new ConcurrentHashMap<>();

    @Override
    public void put_1(RpcCall call$, Key key, Value value) {
        String hexKey = bytesToHex(key.data);
        store.put(hexKey, value.data);
    }

    @Override
    public Value get_1(RpcCall call$, Key key) {
        String hexKey = bytesToHex(key.data);
        byte[] value = store.get(hexKey);
        Value res = new Value();
        if (value != null) {
            res.notNull = true;
            res.data = value;
        }
        return res;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
