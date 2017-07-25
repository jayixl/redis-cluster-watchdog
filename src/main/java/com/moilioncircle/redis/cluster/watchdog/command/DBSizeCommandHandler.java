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

package com.moilioncircle.redis.cluster.watchdog.command;

import com.moilioncircle.redis.cluster.watchdog.util.net.transport.Transport;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class DBSizeCommandHandler implements CommandHandler {
    @Override
    public void handle(Transport<Object> t, String[] message, byte[][] rawMessage) {
        if (message.length == 1) {
            replyNumber(t, "0");
        } else {
            replyError(t, "wrong number of arguments for 'dbsize' command");
        }
    }
}
