# sokky
a simple network library for handling Nio sockets

## Server
````java
package eu.dulag.test;

import eu.dulag.sokky.ByteBuf;
import eu.dulag.sokky.channel.Channel;
import eu.dulag.sokky.channel.bootstrap.NioServer;

import java.net.InetSocketAddress;
import java.util.Arrays;

public class TestServer {

    public static void main(String[] args) {
        try {
            NioServer server = new NioServer(4);
            server.bind(new Channel.Handler() {
                @Override
                public void connected(Channel channel) {
                    System.out.println(channel.remoteAddress() + " has connected");
                }

                @Override
                public void read(Channel channel, ByteBuf buf) {
                    System.out.println(channel.remoteAddress() + " bytes" + Arrays.toString(buf.array()));
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
````

## Client

````java
package eu.dulag.test;

import eu.dulag.sokky.ByteBuf;
import eu.dulag.sokky.channel.Channel;
import eu.dulag.sokky.channel.bootstrap.NioClient;

import java.net.InetSocketAddress;
import java.util.Arrays;

public class TestClient {

    public static void main(String[] args) {
        try {
            NioClient client = new NioClient(4);
            client.connect(new Channel.Handler() {
                @Override
                public void connected(Channel channel) {
                    System.out.println(channel.remoteAddress() + " has connected");

                    ByteBuf buf = ByteBuf.allocDirect(0);
                    buf.writeString("Hello world");

                    client.close();
                    channel.write(buf);
                }

                @Override
                public void read(Channel channel, ByteBuf buf) {
                    System.out.println(channel.remoteAddress() + " bytes" + Arrays.toString(buf.array()));
                }

                @Override
                public void disconnected(Channel channel) {
                    System.out.println(channel.remoteAddress() + " has disconnected");
                }
            }, new InetSocketAddress("127.0.0.1", 25565));
            client.closeFuture();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
````
