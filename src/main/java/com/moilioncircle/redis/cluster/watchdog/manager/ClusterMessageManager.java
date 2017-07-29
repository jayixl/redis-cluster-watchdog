/*
 * Copyright 2016 leon chen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.moilioncircle.redis.cluster.watchdog.manager;

import com.moilioncircle.redis.cluster.watchdog.message.ClusterMessage;
import com.moilioncircle.redis.cluster.watchdog.message.ClusterMessageDataGossip;
import com.moilioncircle.redis.cluster.watchdog.state.ClusterLink;
import com.moilioncircle.redis.cluster.watchdog.state.ClusterNode;
import com.moilioncircle.redis.cluster.watchdog.state.ServerState;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

import static com.moilioncircle.redis.cluster.watchdog.ClusterConstants.*;
import static com.moilioncircle.redis.cluster.watchdog.Version.PROTOCOL_V0;
import static com.moilioncircle.redis.cluster.watchdog.Version.PROTOCOL_V1;
import static com.moilioncircle.redis.cluster.watchdog.state.NodeStates.*;

/**
 * @author Leon Chen
 * @since 1.0.0
 */
public class ClusterMessageManager {

    private static final Log logger = LogFactory.getLog(ClusterMessageManager.class);
    private ServerState server;
    private ClusterManagers managers;

    public ClusterMessageManager(ClusterManagers managers) {
        this.managers = managers;
        this.server = managers.server;
    }

    public void clusterSendMessage(ClusterLink link, ClusterMessage hdr) {
        try {
            link.fd.send(hdr).get();
            if (hdr.type < CLUSTERMSG_TYPE_COUNT)
                server.cluster.messagesSent[hdr.type]++;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            logger.error("send RCmb message failed, link: " + link.fd + ",message type:" + hdr.type);
        }
    }

    public void clusterBroadcastMessage(ClusterMessage hdr) {
        server.cluster.nodes.values().stream().
                filter(e -> e.link != null && (e.flags & (CLUSTER_NODE_MYSELF | CLUSTER_NODE_HANDSHAKE)) == 0).
                forEach(e -> clusterSendMessage(e.link, hdr));
    }

    public ClusterMessage clusterBuildMessageHdr(int type) {
        ClusterMessage hdr = new ClusterMessage();
        ClusterNode master = (nodeIsSlave(server.myself) && server.myself.master != null) ? server.myself.master : server.myself;
        hdr.version = managers.configuration.getVersion();
        hdr.signature = "RCmb";
        hdr.type = type;
        hdr.name = server.myself.name;
        hdr.ip = managers.configuration.getClusterAnnounceIp();

        hdr.slots = master.slots;
        if (server.myself.master != null) {
            hdr.master = server.myself.master.name;
        }

        hdr.flags = server.myself.flags;
        hdr.port = managers.configuration.getClusterAnnouncePort();
        hdr.busPort = managers.configuration.getClusterAnnounceBusPort();
        hdr.state = server.cluster.state;

        hdr.currentEpoch = server.cluster.currentEpoch;
        hdr.configEpoch = master.configEpoch;

        if (nodeIsSlave(server.myself))
            hdr.offset = managers.replications.replicationGetSlaveOffset();
        return hdr;
    }

    public boolean clusterNodeIsInGossipSection(ClusterMessage hdr, int count, ClusterNode n) {
        return hdr.data.gossips.stream().limit(count).anyMatch(e -> Objects.equals(e.name, n.name));
    }

    public void clusterSetGossipEntry(ClusterMessage hdr, ClusterNode n) {
        ClusterMessageDataGossip gossip = new ClusterMessageDataGossip();
        gossip.ip = n.ip;
        gossip.name = n.name;
        gossip.port = n.port;
        gossip.flags = n.flags;
        gossip.busPort = n.busPort;
        gossip.pingTime = n.pingTime;
        gossip.pongTime = n.pongTime;
        hdr.data.gossips.add(gossip);
    }

    public void clusterSendPing(ClusterLink link, int type) {
        if (managers.configuration.getVersion() == PROTOCOL_V0) {
            clusterSendPingV0(link, type);
        } else if (managers.configuration.getVersion() == PROTOCOL_V1) {
            clusterSendPingV1(link, type);
        }
    }

    private void clusterSendPingV0(ClusterLink link, int type) {
        int actives = server.cluster.nodes.size() - 2;
        int wanted = server.cluster.nodes.size() / 10;
        wanted = Math.min(Math.max(wanted, 3), actives);

        if (link.node != null && type == CLUSTERMSG_TYPE_PING)
            link.node.pingTime = System.currentTimeMillis();

        ClusterMessage hdr = clusterBuildMessageHdr(type);
        int max = wanted * 3, gossips = 0;
        while (actives > 0 && gossips < wanted && max-- > 0) {
            List<ClusterNode> list = new ArrayList<>(server.cluster.nodes.values());
            ClusterNode node = list.get(ThreadLocalRandom.current().nextInt(list.size()));

            if (Objects.equals(node, server.myself)) continue;

            if (max > wanted * 2 && (node.flags & (CLUSTER_NODE_PFAIL | CLUSTER_NODE_FAIL)) == 0)
                continue;

            if ((node.flags & (CLUSTER_NODE_HANDSHAKE | CLUSTER_NODE_NOADDR)) != 0 || (node.link == null && node.assignedSlots == 0))
                continue;

            if (clusterNodeIsInGossipSection(hdr, gossips, node)) continue;

            clusterSetGossipEntry(hdr, node);
            actives--;
            gossips++;
        }

        hdr.count = gossips;
        clusterSendMessage(link, hdr);
    }

    private void clusterSendPingV1(ClusterLink link, int type) {
        int actives = server.cluster.nodes.size() - 2;
        int wanted = server.cluster.nodes.size() / 10;
        int fWanted = (int) server.cluster.pFailNodes;
        wanted = Math.min(Math.max(wanted, 3), actives);

        if (link.node != null && type == CLUSTERMSG_TYPE_PING)
            link.node.pingTime = System.currentTimeMillis();

        ClusterMessage hdr = clusterBuildMessageHdr(type);
        int max = wanted * 3, gossips = 0;
        while (actives > 0 && gossips < wanted && max-- > 0) {
            List<ClusterNode> list = new ArrayList<>(server.cluster.nodes.values());
            ClusterNode node = list.get(ThreadLocalRandom.current().nextInt(list.size()));

            if (Objects.equals(node, server.myself)) continue;

            if ((node.flags & CLUSTER_NODE_PFAIL) != 0) continue;

            if ((node.flags & (CLUSTER_NODE_HANDSHAKE | CLUSTER_NODE_NOADDR)) != 0 || (node.link == null && node.assignedSlots == 0))
                continue;

            if (clusterNodeIsInGossipSection(hdr, gossips, node)) continue;

            clusterSetGossipEntry(hdr, node);
            actives--;
            gossips++;
        }

        if (fWanted != 0) {
            List<ClusterNode> nodes = new ArrayList<>(server.cluster.nodes.values());
            for (int i = 0; i < nodes.size() && fWanted > 0; i++) {
                ClusterNode node = nodes.get(i);
                if (nodeInHandshake(node) || nodeWithoutAddr(node) || !nodePFailed(node)) continue;
                clusterSetGossipEntry(hdr, node);
                actives--;
                gossips++;
                fWanted--;
            }
        }

        hdr.count = gossips;
        clusterSendMessage(link, hdr);
    }

    public void clusterBroadcastPong(int target) {
        for (ClusterNode node : server.cluster.nodes.values()) {
            if (node.link == null) continue;
            if (Objects.equals(node, server.myself) || nodeInHandshake(node)) continue;
            if (target == CLUSTER_BROADCAST_LOCAL_SLAVES) {
                boolean local = nodeIsSlave(node)
                        && node.master != null
                        && (Objects.equals(node.master, server.myself) || Objects.equals(node.master, server.myself.master));
                if (!local) continue;
            }
            clusterSendPing(node.link, CLUSTERMSG_TYPE_PONG);
        }
    }

    public void clusterSendFail(String name) {
        ClusterMessage hdr = clusterBuildMessageHdr(CLUSTERMSG_TYPE_FAIL);
        hdr.data.fail.name = name;
        clusterBroadcastMessage(hdr);
    }

    public void clusterSendUpdate(ClusterLink link, ClusterNode node) {
        if (link == null) return;
        ClusterMessage hdr = clusterBuildMessageHdr(CLUSTERMSG_TYPE_UPDATE);
        hdr.data.config.name = node.name;
        hdr.data.config.configEpoch = node.configEpoch;
        hdr.data.config.slots = node.slots;
        clusterSendMessage(link, hdr);
    }

    public void clusterSendFailoverAuth(ClusterNode node) {
        if (node.link == null) return;
        clusterSendMessage(node.link, clusterBuildMessageHdr(CLUSTERMSG_TYPE_FAILOVER_AUTH_ACK));
    }

    public void clusterRequestFailoverAuth() {
        clusterBroadcastMessage(clusterBuildMessageHdr(CLUSTERMSG_TYPE_FAILOVER_AUTH_REQUEST));
    }
}
