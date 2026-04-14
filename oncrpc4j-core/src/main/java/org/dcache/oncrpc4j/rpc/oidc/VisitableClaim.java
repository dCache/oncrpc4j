package org.dcache.oncrpc4j.rpc.oidc;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

public class VisitableClaim {
    private final String _name;
    private final ClaimType _type;
    private final boolean _isRequired;
    private final IClaimCallback _callback;
    private final IClaimCallback _fallback;

    public VisitableClaim(String name, ClaimType type, boolean isRequired, IClaimCallback callback, IClaimCallback fallback) {
        _name = name;
        _type = type;
        _isRequired = isRequired;
        _callback = callback;
        _fallback = fallback;
    }

    public boolean accept(DecodedJWT jwt) {
        Claim claim = jwt.getClaim(_name);

        if (claim.isMissing() || claim.isNull()) {
            if (_isRequired) return false;
            return (_fallback == null) || _fallback.call(null);
        }

        Object value = cast(claim);

        return (_callback == null) || _callback.call(value);
    }

    private Object cast(Claim claim) {
        return switch (_type) {
            case CLAIM_STR        -> claim.asString();
            case CLAIM_INT        -> claim.asInt();
            case CLAIM_JSON_ARRAY -> claim.asList(String.class);
            case CLAIM_BOOL       -> claim.asBoolean();
        };
    }

    public String getName() { return _name; }
}
