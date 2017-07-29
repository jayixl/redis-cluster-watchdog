package com.moilioncircle.redis.cluster.watchdog.message;

/**
 * @author Leon Chen
 * @since 1.0.0
 */
public class ClusterMessageDataGossip {
    public int port;
    public int flags;
    public String ip;
    public String name;
    public int busPort;
    public long pingTime;
    public long pongTime;
}
