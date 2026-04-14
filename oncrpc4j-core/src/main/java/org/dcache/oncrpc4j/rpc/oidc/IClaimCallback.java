package org.dcache.oncrpc4j.rpc.oidc;


public interface IClaimCallback<T> { 
    boolean call(T value); 
}


