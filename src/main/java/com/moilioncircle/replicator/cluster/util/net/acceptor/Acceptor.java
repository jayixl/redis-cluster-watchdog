package com.moilioncircle.replicator.cluster.util.net.acceptor;

import com.moilioncircle.replicator.cluster.util.concurrent.future.CompletableFuture;
import com.moilioncircle.replicator.cluster.util.marshaller.Marshaller;
import com.moilioncircle.replicator.cluster.util.net.transport.TransportListener;

/**
 * Created by Baoyi Chen on 2017/7/7.
 */
public interface Acceptor<T> extends TransportListener<T> {
    CompletableFuture<?> shutdown();

    void setMarshaller(Marshaller<T> marshaller);

    CompletableFuture<Void> bind(String host, int port);

    TransportListener<T> setTransportListener(TransportListener<T> listener);
}
