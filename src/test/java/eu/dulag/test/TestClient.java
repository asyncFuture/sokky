package eu.dulag.test;

import eu.dulag.sokky.ByteBuf;
import eu.dulag.sokky.channel.Channel;
import eu.dulag.sokky.channel.bootstrap.NioClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;

public class TestClient {

    public static void main(String[] args) {
        try {
            NioClient client = new NioClient();
            client.connect(new Channel.Handler() {
                @Override
                public void connected(Channel channel) {
                    System.out.println(channel.remoteAddress() + " has connected");
                }

                @Override
                public void read(Channel channel, ByteBuf buf) {
                    System.out.println(channel.remoteAddress() + " bytes: " + Arrays.toString(buf.array()));
                }

                @Override
                public void disconnected(Channel channel) {
                    System.out.println(channel.remoteAddress() + " has disconnected");
                }
            }, new InetSocketAddress("127.0.0.1", 25565));
            client.closeFuture();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}