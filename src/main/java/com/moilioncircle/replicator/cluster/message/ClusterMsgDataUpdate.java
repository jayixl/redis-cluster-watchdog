package com.moilioncircle.replicator.cluster.message;

import static com.moilioncircle.replicator.cluster.ClusterConstants.CLUSTER_SLOTS;

/**
 * Created by Baoyi Chen on 2017/7/6.
 */
public class ClusterMsgDataUpdate {
    public long configEpoch;
    public String nodename;
    public byte[] slots = new byte[CLUSTER_SLOTS / 8];
}