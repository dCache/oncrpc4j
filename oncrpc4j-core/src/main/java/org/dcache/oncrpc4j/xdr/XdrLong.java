package org.dcache.oncrpc4j.xdr;

import org.dcache.oncrpc4j.rpc.OncRpcException;
import java.io.IOException;

public class XdrLong implements XdrAble {

    private long _value;

    public XdrLong() {
    }

    public XdrLong(long _value) {
        this._value = _value;
    }

    public long longValue() {
        return _value;
    }

    public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException {
        _value = xdr.xdrDecodeLong();
    }

    public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException {
        xdr.xdrEncodeLong(_value);
    }
}
