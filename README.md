# sokky

a simple network library for handling Nio sockets

The Library is based on NIO to achieve the best possible performance.

## Provider

This is the provider system, which allows very simple handling of the processing of the keys. In addition, 
the processing is done via an ExecutorService, which further improves thread safety.

You can define the thread number yourself.
````java
NioProvider provider = new NioProvider(4 -> count of threads, Selector.open());
````
````java
NioProvider provider = new NioProvider(Selector.open());
provider.select(key-> {
    if(key.isAcceptable()){
        //accept a channel with provider
    } else if(key.isReadable()){
        //read channel
    }
});
````
## Buffer
The buffers are based on Nio, so I just wrote an extension of the ByteBuffer. The biggest issue right now is managing the storage I'm already working on.

### Snippet
````java
ByteBuf alloc = ByteBuf.alloc(512);
alloc.writeString("Hello world");
        
alloc.flip();

System.out.println(Arrays.toString(alloc.array()));
````
## Server
````java
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
                    int length = buf.readInt();
                    String string = buf.readString();

                    System.out.println(channel.remoteAddress() + " message: " + string);
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

import java.io.IOException;
import java.net.InetSocketAddress;

public class TestClient {

    public static void main(String[] args) {
        try {
            NioClient client = new NioClient();
            client.connect(new Channel.Handler() {
                @Override
                public void connected(Channel channel) {
                    System.out.println(channel.remoteAddress() + " has connected");

                    ByteBuf buf = ByteBuf.alloc(0);
                    buf.writeString("Hello world");

                    channel.write(buf);
                }

                @Override
                public void read(Channel channel, ByteBuf buf) {
                    int length = buf.readInt();
                    String string = buf.readString();

                    System.out.println(channel.remoteAddress() + " message: " + string);
                }

                @Override
                public void disconnected(Channel channel) {
                    System.out.println(channel.remoteAddress() + " has disconnected");
                }
            }, new InetSocketAddress("127.0.0.1", 25565));
            client.closeFuture();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
````