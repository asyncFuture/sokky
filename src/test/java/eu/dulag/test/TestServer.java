package eu.dulag.test;

import eu.dulag.sokky.ByteBuf;
import eu.dulag.sokky.channel.Channel;
import eu.dulag.sokky.channel.bootstrap.NioServer;

import java.net.InetSocketAddress;

public class TestServer {

    public static void main(String[] args) {
        try {
            NioServer server = new NioServer();
            server.bind(new Channel.Handler() {
                @Override
                public void connected(Channel channel) {
                    System.out.println(channel.remoteAddress() + " has connected");
                }

                @Override
                public void read(Channel channel, ByteBuf buf) {
                    while (buf.isReadable()) {
                        int length = buf.readInt();
                        String string = buf.readString();

                        System.out.println(channel.remoteAddress() + " message: " + string);
                    }
                }

                @Override
                public void disconnected(Channel channel) {
                    System.out.println(channel.remoteAddress() + " has disconnected");
                }
            }, new InetSocketAddress(25565));
            server.closeFuture();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}