package eu.dulag.sokky.channel;

import eu.dulag.sokky.ByteBuf;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;

public interface Channel extends Closeable {

    void write(ByteBuf buf);

    ByteBuf read();

    @Override
    void close() throws IOException;

    void closeFuture() throws IOException;

    boolean isConnected();

    SocketAddress localAddress();

    SocketAddress remoteAddress();

    interface Handler {

        default void connected(Channel channel) {
        }

        void read(Channel channel, ByteBuf buf);

        default void disconnected(Channel channel) {
        }
    }
}