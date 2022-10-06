package eu.dulag.sokky.channel;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.function.Consumer;

public class NioProvider implements Closeable {

    private final Selector selector;

    public NioProvider(Selector selector) {
        this.selector = selector;
    }

    public void select(Consumer<SelectionKey> consumer) {
        while (selector.isOpen()) {
            try {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isValid()) consumer.accept(key);
                    Thread.sleep(1);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() throws IOException {
        selector.close();
    }

    public boolean isOpen() {
        return selector.isOpen();
    }
}