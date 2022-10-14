package eu.dulag.sokky.channel.bootstrap;

import eu.dulag.sokky.ByteBuf;
import eu.dulag.sokky.channel.Channel;
import eu.dulag.sokky.channel.NioChannel;
import eu.dulag.sokky.channel.NioProvider;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class NioServer implements Channel {

    private boolean connected;

    private final Selector selector;
    private final NioProvider provider;

    private final ServerSocketChannel socket;
    private Channel.Handler handler;

    private SocketAddress address;

    private final Map<SocketChannel, NioChannel> channels = new HashMap<>();

    public NioServer(int threads) throws IOException {
        this.selector = Selector.open();
        this.provider = new NioProvider(threads, selector);
        this.socket = ServerSocketChannel.open();
    }

    public NioServer() throws IOException {
        this(8);
    }

    public void bind(Channel.Handler handler, SocketAddress address) throws IOException {
        this.socket.configureBlocking(false).register(selector, SelectionKey.OP_ACCEPT);
        this.socket.bind(this.address = address, 128);
        this.handler = handler;
        this.connected = true;
        this.provider.select(key -> {
            if (key.isAcceptable()) {
                try {
                    SocketChannel accept = socket.accept();
                    accept.configureBlocking(false).register(selector, SelectionKey.OP_READ);

                    NioChannel channel = new NioChannel(provider, accept);
                    channels.put(accept, channel);
                    if (handler != null) handler.connected(channel);
                } catch (IOException e) {
                    key.cancel();
                }
            } else if (key.isReadable()) {
                SocketChannel socket = (SocketChannel) key.channel();
                NioChannel channel = channels.get(socket);

                ByteBuf buf = channel.read();
                if (buf == null) {
                    key.cancel();
                    channel.close();
                    if (handler != null) handler.disconnected(channel);
                    channels.remove(socket).closeFuture();
                    return;
                }
                if (handler != null) handler.read(channel, buf);
                buf.clear();
            }
        });
    }

    @Override
    public void write(ByteBuf buf) {
        for (NioChannel channel : channels.values()) channel.write(buf);
    }

    @Override
    public ByteBuf read() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        try {
            if (!connected) return;
            connected = false;

            provider.close();
            socket.close();

            channels.forEach((socket, channel) -> {
                channel.close();
                if (handler != null) handler.disconnected(channel);
                channels.remove(socket).closeFuture();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void closeFuture() {
        channels.clear();
    }

    public boolean isMultithreading() {
        return provider.isMultithreading();
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public SocketAddress localAddress() {
        return address;
    }

    @Override
    public SocketAddress remoteAddress() {
        throw new UnsupportedOperationException();
    }
}