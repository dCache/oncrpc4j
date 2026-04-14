package org.dcache.oncrpc4j.rpc.oidc;

public class OidcException extends RuntimeException {
    public OidcException(String message) { super(message); }
    public OidcException(String message, Throwable cause) { super(message, cause); }

    /** Thrown for protocol/size violations */
    public static class Protocol extends OidcException {
        public Protocol(String message) { super(message); }
    }

    /** Thrown for cryptographic failures */
    public static class Signature extends OidcException {
        public Signature(String message, Throwable cause) { super(message, cause); }
    }

    /** Thrown for business logic/claim failures */
    public static class Validation extends OidcException {
        public Validation(String message) { super(message); }
    }
}
