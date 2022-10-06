package eu.dulag.sokky.channel.bootstrap;

import eu.dulag.sokky.ByteBuf;
import eu.dulag.sokky.channel.Channel;
import eu.dulag.sokky.channel.NioChannel;
import eu.dulag.sokky.channel.NioProvider;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class NioClient implements Channel {

    private final Selector selector;
    private final NioProvider provider;

    private final SocketChannel socket;
    private NioChannel channel;
    private Channel.Handler handler;

    public NioClient() throws IOException {
        this.selector = Selector.open();
        this.provider = new NioProvider(selector);
        this.socket = SocketChannel.open();
    }

    public void connect(Channel.Handler handler, SocketAddress address) throws IOException {
        this.socket.configureBlocking(false).register(selector, SelectionKey.OP_CONNECT);
        this.socket.connect(address);
        this.handler = handler;
        this.provider.select(key -> {
            if (key.isConnectable()) {
                try {
                    socket.register(selector, SelectionKey.OP_READ);
                    socket.finishConnect();

                    channel = new NioChannel(socket);
                    if (handler != null) handler.connected(channel);
                } catch (IOException e) {
                    key.cancel();
                }
            } else if (key.isReadable()) {
                ByteBuf buf = channel.read();
                if (buf == null) {
                    key.cancel();
                    close();
                    return;
                }
                if (handler != null) handler.read(channel, buf);
                buf.clear();
            }
        });
    }

    public void connect(SocketAddress address) throws IOException {
        connect(null, address);
    }

    @Override
    public void write(ByteBuf buf) {
        channel.write(buf);
    }

    @Override
    public ByteBuf read() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        try {
            if (channel == null) return;
            if (!isConnected()) return;

            channel.close();

            provider.close();
            if (handler != null) handler.disconnected(channel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void closeFuture() {
        channel.closeFuture();
    }

    @Override
    public boolean isConnected() {
        return channel.isConnected();
    }

    @Override
    public SocketAddress localAddress() {
        return channel.localAddress();
    }

    @Override
    public SocketAddress remoteAddress() {
        return channel.remoteAddress();
    }
}