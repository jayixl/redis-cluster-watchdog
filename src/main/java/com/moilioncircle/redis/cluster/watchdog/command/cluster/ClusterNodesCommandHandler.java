/*
 * Copyright 2016-2017 Leon Chen
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

package com.moilioncircle.redis.cluster.watchdog.command.cluster;

import com.moilioncircle.redis.cluster.watchdog.command.AbstractCommandHandler;
import com.moilioncircle.redis.cluster.watchdog.manager.ClusterManagers;
import com.moilioncircle.redis.cluster.watchdog.util.net.transport.Transport;

import static com.moilioncircle.redis.cluster.watchdog.config.ConfigInfo.valueOf;

/**
 * @author Leon Chen
 * @since 1.0.0
 */
public class ClusterNodesCommandHandler extends AbstractCommandHandler {

    public ClusterNodesCommandHandler(ClusterManagers managers) {
        super(managers);
    }

    @Override
    public void handle(Transport<Object> t, String[] message, byte[][] rawMessage) {
        if (message.length != 2) {
            t.write(("-ERR Wrong CLUSTER subcommand or number of arguments\r\n").getBytes(), true);
            return;
        }
        String nodes = managers.configs.clusterGenNodesDescription(valueOf(server.cluster), 0);
        t.write(("$" + nodes.length() + "\r\n" + nodes + "\r\n").getBytes(), true);
    }
}
