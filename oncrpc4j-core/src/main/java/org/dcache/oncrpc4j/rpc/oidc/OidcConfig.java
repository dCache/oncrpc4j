package org.dcache.oncrpc4j.rpc.oidc;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;


public class OidcConfig {
    private String _iss;
    private String _aud;
    private String _jwksUri;
    private long _jwksRefreshInterval = 3600;
    private long _timeSkewExp = 60;
    private long _timeSkewNbf = 60;
    private final List<VisitableClaim> _registry = new ArrayList<>();

    public OidcConfig addVisitableClaim(VisitableClaim vc) {
      _registry.add(vc);
      return this;
    }

    public OidcConfig jwksUri(String jwksUri) {
        _jwksUri = jwksUri;
        return this;
    }

    public OidcConfig iss(String iss) {
        _iss = iss;
        return this;
    }

    public OidcConfig aud(String aud) {
        _aud = aud;
        return this;
    }

    public OidcConfig timeSkewExp(int timeSkewExp) {
        _timeSkewExp = timeSkewExp;
        return this;
    }

    public OidcConfig timeSkewNbf(int timeSkewNbf) {
        _timeSkewNbf = timeSkewNbf;
        return this;
    }

    public List<VisitableClaim> registry() {
      return _registry;
    } 

   
    //TODO: add:
    // withClaim(String name, Boolean value) //Verifies whether the claim is equal to the given Boolean value.
    // etc ...
    // https://javadoc.io/static/com.auth0/java-jwt/4.2.1/com/auth0/jwt/interfaces/Verification.html#withClaim(java.lang.String,java.lang.Boolean)
    //
    // Getters and Seters for URI, ISS, AUD, etc.
    public String iss() { return _iss; }
    public String aud() { return _aud; }
    public String jwksUri() { return _jwksUri; }
    public long jwksRefreshInterval() { return _jwksRefreshInterval; }
    public long timeSkewExp() { return _timeSkewExp; }
    public long timeSkewNbf() { return _timeSkewNbf; }

}
