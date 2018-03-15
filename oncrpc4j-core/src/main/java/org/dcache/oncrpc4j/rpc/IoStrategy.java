package org.dcache.oncrpc4j.rpc;

public enum IoStrategy {
    SAME_THREAD,
    WORKER_THREAD,
    LEADER_FOLLOWER
}
