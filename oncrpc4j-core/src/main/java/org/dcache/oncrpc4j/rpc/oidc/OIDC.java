package org.dcache.oncrpc4j.rpc.oidc;

import org.dcache.oncrpc4j.xdr.XdrAble;
import org.dcache.oncrpc4j.xdr.XdrDecodingStream;
import org.dcache.oncrpc4j.xdr.XdrEncodingStream;

import org.dcache.oncrpc4j.rpc.OncRpcException;

import java.io.IOException;


public class OIDC {

    public enum ChunkState {
        NONE(0),
        INIT(1),     // Initial chunk
        DATA(2),     // Regular data chunk
        END(3);      // Final chunk

        public int _value;

        private ChunkState(int value) {
          _value = value;
        }

        public static ChunkState decode(int i) {
            switch (i) {
                case 0:
                    return ChunkState.NONE;
                case 1:
                    return ChunkState.INIT;
                case 2:
                    return ChunkState.DATA;
                case 3:
                    return ChunkState.END;
            
                default:
                    // No default
                    break;
            }
            return ChunkState.NONE;
        }

    }

    public enum Payload {
        NONE(0),
        CHUNK(1),  // Chunk
        HASH(2),   // Hash
        ERR(3);    // Error code

        public int _value;

        private Payload(int value) {
          _value = value;
        }

        public static Payload decode(int i) {
            switch (i) {
                case 0:
                    return Payload.NONE;
                case 1:
                    return Payload.CHUNK;
                case 2:
                    return Payload.HASH;
                case 3:
                    return Payload.ERR;
            
                default:
                    // No default
                    break;
            }
            return Payload.NONE;
        }
    }

    public enum ErrCode {
        // TODO: Example valuess need to be changed !!!
        NONE(0),      // RPC_SUCCESS
        SUCCESS(1),   // RPC_SUCCESS
        AUTHERROR(2), // RPC_AUTHERROR
        TIMEOUT(3);   // RPC_TIMEDOUT  -  CTX pruned


        public int _value;

        private ErrCode(int value) {
          _value = value;
        }

        public static ErrCode decode(int i) {
            switch (i) {
                case 0:
                    return ErrCode.NONE;
                case 1:
                    return ErrCode.SUCCESS;
                case 2:
                    return ErrCode.AUTHERROR;
                case 3:
                    return ErrCode.TIMEOUT;
            
                default:
                    // No default
                    break;
            }
            return ErrCode.NONE;
        }

    }

    //
    public static class Chunk {
        private ChunkState _state;
        private int _len;
        private String _data;
        private long _tokenLen;

        public Chunk() {
            this(ChunkState.NONE, 0, "", 0);
        }

        public Chunk(ChunkState state, int len, String data, long tokenLen) {
            _state = state;
            _len = len;
            _data = data;
            _tokenLen = tokenLen;
        }

        public ChunkState state() {
          return _state;
        }
        
        public void state(ChunkState state) {
          _state = state;
        }
        
        public int len() {
          return _len;
        }
        
        public void len(int len) {
          _len = len;
        }
        
        public String data() {
          return _data;
        }
        
        public void data(String data) {
          _data = data;
        }
        
        public byte[] bytes() {
          if (_data == null || _data.isEmpty()) {
            return new byte[0];
          }
          // Explicitly convert the String chunk to UTF-8 bytes
          return _data.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        public long tokenLen() {
          return _tokenLen;
        }
        
        public void tokenLen(long tokenLen) {
          _tokenLen = tokenLen;
        }

        @Override
        public String toString() {
            return String.format("OidcChunk{state=%s, len=%d, data='%s', tokenLen=%d}", 
                               _state, _len, _data, _tokenLen);
        }

    }

    public static class Hash {
        private long _value;

        public Hash() {
            this(0);
        }

        public Hash(long value) {
            _value = value;
        }

        public long value() {
          return _value;
        }
        public void value(long value) {
          _value = value;
        }

        @Override
        public String toString() {
            return String.format("OidcHash{value=%d}", _value);
        }
    }

    public static class Error {
        private ErrCode _code;

        public Error() {
            this(ErrCode.NONE);
        }

        public Error(ErrCode code) {
            _code = code;
        }

        public ErrCode code() {
          return _code;
        }
        public void code(ErrCode code) {
          _code = code;
        }

        @Override
        public String toString() {
            return String.format("OidcErr{code=%s}", _code);
        }
    }

    // Base abstract class for all OIDC messages
    public abstract static class Message implements XdrAble {
        protected long _uid;
        protected Payload _type;

        protected Message(long uid, Payload type) {
            _uid = uid;
            _type = type;
        }

        public long uid() {
          return _uid;
        }
        
        public Payload type() {
          return _type;
        }

        // Factory method for creating messages
        public static Message createChunkMessage(long uid, Chunk chunk) {
            return new ChunkMessage(uid, chunk);
        }

        public static Message createHashMessage(long uid, Hash hash) {
            return new HashMessage(uid, hash);
        }

        public static Message createErrorMessage(long uid, Error error) {
            return new ErrorMessage(uid, error);
        }

        public static Message xdrCreate(XdrDecodingStream xdr) throws OncRpcException, IOException {
            Message oidcMessage = null;
            long uid = xdr.xdrDecodeLong();
            Payload type = Payload.decode(xdr.xdrDecodeInt());
 
            switch (type) {
                case NONE:
                  // if NONE thne null 
                  break;
                case CHUNK:
                   oidcMessage = OIDC.Message.createChunkMessage(
                          uid,
                          new OIDC.Chunk()
                      );
                   oidcMessage.xdrDecode(xdr);
                  break;
                case HASH:
                  oidcMessage = OIDC.Message.createHashMessage(
                          uid,
                          new OIDC.Hash()
                      );
                  oidcMessage.xdrDecode(xdr);
                  break;
                case ERR:
                   oidcMessage = OIDC.Message.createErrorMessage(
                          uid,
                          new OIDC.Error()
                      );
                  oidcMessage.xdrDecode(xdr);
                  break;
                default:
                  // np default
                    break;
            }

            return oidcMessage;
        }


        // Visitor pattern support for type-safe processing
        public abstract <R, P> R accept(MessageVisitor<R, P> visitor, P... params);

        @Override
        public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException {

            _uid = xdr.xdrDecodeLong();
            _type = Payload.decode(xdr.xdrDecodeInt());
        
        }

        @Override
        public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException {
        
            xdr.xdrEncodeLong(_uid);
            xdr.xdrEncodeInt(_type._value);
        
        }

        @Override
        public abstract String toString();

    }   

    // Helper class for decoding messages
    public static class MessageDecoder implements XdrAble {

        private Message _msg = null;

        public Message message() {
          return _msg;
        }

        @Override
        public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException {

            _msg = Message.xdrCreate(xdr);
        
        }

        @Override
        public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException {
        
            // no encoding

        }

    }

    // Specific message implementations
    public static class ChunkMessage extends Message {
        private Chunk _chunk;

        public ChunkMessage(long uid, Chunk chunk) {
            super(uid, Payload.CHUNK);
            _chunk = chunk;
        }

        public Chunk chunk() {
          return _chunk;
        }

        @Override
        public <R, P> R accept(MessageVisitor<R, P> visitor, P... params) {
            return visitor.visit(this, params);
        }

        @Override
        public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException {
         // super.xdrDecode(xdr); 
          _chunk.state(ChunkState.decode(xdr.xdrDecodeInt()));
          _chunk.len(xdr.xdrDecodeInt());
          _chunk.data(xdr.xdrDecodeString());
          _chunk.tokenLen(xdr.xdrDecodeLong());

        }
    
        @Override
        public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException {
          
            super.xdrEncode(xdr); 
            xdr.xdrEncodeInt(_chunk.state()._value);
            xdr.xdrEncodeInt(_chunk.len());
            xdr.xdrEncodeString(_chunk.data());
            xdr.xdrEncodeLong(_chunk.tokenLen());

        }

        @Override
        public String toString() {
            return String.format("ChunkMessage{uid=%d, chunk=%s}", _uid, _chunk);
        }
    }

    public static class HashMessage extends Message {
        private Hash _hash;

        public HashMessage(long uid, Hash hash) {
            super(uid, Payload.HASH);
            _hash = hash;
        }

        public Hash hash() {
          return _hash;
        }

        @Override
        public <R, P> R accept(MessageVisitor<R, P> visitor, P... params) {
            return visitor.visit(this, params);
        }

        @Override
        public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException {
 
            _hash.value(xdr.xdrDecodeLong());
        
        }
    
        @Override
        public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException {

            super.xdrEncode(xdr); 
            xdr.xdrEncodeLong(_hash.value());
        
        }

        @Override
        public String toString() {
            return String.format("HashMessage{uid=%d, hash=%s}", _uid, _hash);
        }
    }

    public static class ErrorMessage extends Message {
        private Error _error;

        public ErrorMessage(long uid, Error error) {
            super(uid, Payload.ERR);
            _error = error;
        }

        public Error error() { return _error; }

        @Override
        public <R, P> R accept(MessageVisitor<R, P> visitor, P... params) {
            return visitor.visit(this, params);
        }

        @Override
        public void xdrDecode(XdrDecodingStream xdr) throws OncRpcException, IOException {

            _error.code(ErrCode.decode(xdr.xdrDecodeInt()));
 
        }
    
        @Override
        public void xdrEncode(XdrEncodingStream xdr) throws OncRpcException, IOException {

            super.xdrEncode(xdr); 
            xdr.xdrEncodeInt(_error.code()._value);
 
        }

        @Override
        public String toString() {
            return String.format("ErrorMessage{uid=%d, error=%s}", _uid, _error);
        }
    }

    public interface MessageVisitor<R, P> {
        R visit(ChunkMessage message, P... params);
        R visit(HashMessage message, P... params);
        R visit(ErrorMessage message, P... params);
        
        // Generic handler interface
        @FunctionalInterface
        interface Handler<R, M, P> {
            R handle(M message, P... params);
        }
        
        // Static factory with varargs support
        static <R, P> MessageVisitor<R, P> of(
            Handler<R, ChunkMessage, P> chunkHandler,
            Handler<R, HashMessage, P> hashHandler,
            Handler<R, ErrorMessage, P> errorHandler) {
            
            return new MessageVisitor<R, P>() {
                @Override
                public R visit(ChunkMessage message, P... params) {
                    return chunkHandler.handle(message, params);
                }
                
                @Override
                public R visit(HashMessage message, P... params) {
                    return hashHandler.handle(message, params);
                }
                
                @Override
                public R visit(ErrorMessage message, P... params) {
                    return errorHandler.handle(message, params);
                }
            };
        }
    }
    // Utility class for type-safe parameter access
    public static class Params {
        //@SuppressWarnings("unchecked")
        public static <T> T get(Object[] params, int index, Class<T> type) {
            if (index >= params.length) {
                throw new IllegalArgumentException("Parameter index out of bounds: " + index);
            }
            Object param = params[index];
            if (!type.isInstance(param)) {
                throw new ClassCastException("Parameter at index " + index + 
                    " is not of type " + type.getName() + ", got: " + 
                    (param != null ? param.getClass().getName() : "null"));
            }
            return (T) param;
        }
    }

    public static class MessageProcessor implements MessageVisitor<Void, Void> {
        @Override
        public Void visit(ChunkMessage message, Void... params) {
            // Process chunk data
            return null;
        }

        @Override
        public Void visit(HashMessage message, Void... params) {
            // Process hash
            return null;
        }

        @Override
        public Void visit(ErrorMessage message, Void... params) {
            // Handle error
            return null;
        }
    }

}

