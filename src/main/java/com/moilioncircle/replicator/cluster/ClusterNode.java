package com.moilioncircle.replicator.cluster;

import java.util.List;

import static com.moilioncircle.replicator.cluster.ClusterConstants.CLUSTER_SLOTS;

/**
 * Created by Baoyi Chen on 2017/7/6.
 */
public class ClusterNode {
    public int port;
    public int cport;
    public String ip;
    public int flags;
    public long ctime;
    public String name;
    public int numslots;
    public int numslaves;
    public long pingSent;
    public long failTime;
    public long replOffset;
    public long configEpoch;
    public long pongReceived;
    public long orphanedTime;
    public ClusterNode slaveof;
    public List<ClusterNode> slaves;
    public volatile ClusterLink link;
    public List<ClusterNodeFailReport> failReports;
    public byte[] slots = new byte[CLUSTER_SLOTS / 8];

    @Override
    public String toString() {
        return "ClusterNode{" +
                "port=" + port +
                ", cport=" + cport +
                ", ip='" + ip + '\'' +
                ", flags=" + flags +
                ", ctime=" + ctime +
                ", name='" + name + '\'' +
                ", numslaves=" + numslaves +
                ", pingSent=" + pingSent +
                ", failTime=" + failTime +
                ", replOffset=" + replOffset +
                ", configEpoch=" + configEpoch +
                ", pongReceived=" + pongReceived +
                ", orphanedTime=" + orphanedTime +
                '}';
    }
}
