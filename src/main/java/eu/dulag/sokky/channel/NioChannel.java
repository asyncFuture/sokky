package eu.dulag.sokky.channel;

import eu.dulag.sokky.ByteBuf;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NioChannel implements Channel {

    private final SocketChannel socket;

    private final SocketAddress localAddress, remoteAddress;

    private boolean connected;

    private ByteBuf alloc = ByteBuf.alloc(0);

    public NioChannel(SocketChannel socket) throws IOException {
        this.socket = socket;
        this.localAddress = socket.getLocalAddress();
        this.remoteAddress = socket.getRemoteAddress();
        this.connected = true;
    }

    @Override
    public void write(ByteBuf buf) {
        try {
            if (buf.readable() == 0) buf.flip();

            int readable = buf.readable();
            int size = (ByteBuf.length(readable) + readable);

            ByteBuf alloc = ByteBuf.alloc(size);
            alloc.writeInt(readable);
            alloc.write(buf);

            socket.write(buf.flip().context());
            buf.flip();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ByteBuf read() {
        while (connected) {
            try {
                int read = socket.read(ByteBuf.BUFFER);
                if (read == -1) break;

                if (read == 0) return alloc.flip();
                alloc.enlarge(read).write((ByteBuffer) ByteBuf.BUFFER.flip());
                ByteBuf.BUFFER.clear();
                if (read < ByteBuf.BUFFER.remaining()) return alloc.flip();
            } catch (Exception exception) {
                return null;
            }
        }
        return null;
    }

    @Override
    public void close() {
        try {
            connected = false;
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void closeFuture() {
        alloc = null;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public SocketAddress localAddress() {
        return localAddress;
    }

    @Override
    public SocketAddress remoteAddress() {
        return remoteAddress;
    }
}