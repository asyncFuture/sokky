package eu.dulag.sokky;

import java.util.ArrayList;
import java.util.List;

public class Blocking<T> {

    private final List<Element<T>> elements = new ArrayList<>();

    @SafeVarargs
    public Blocking(T... objects) {
        for (T object : objects) elements.add(new Element<>(object));
    }

    public Blocking<T> add(T object) {
        elements.add(new Element<>(object));
        return this;
    }

    public boolean remove(T object) {
        for (Element<T> element : elements) {
            if (element.object == object) return elements.remove(element);
        }
        return false;
    }

    public T poll() {
        for (Element<T> element : elements) {
            if (!element.blocking) return element.block();
        }
        return null;
    }

    public T await() throws InterruptedException {
        while (true) {
            for (Element<T> element : elements) {
                if (!element.blocking) {
                    return element.block();
                }
            }
            Thread.sleep(1);
        }
    }

    public boolean detach(T object) {
        for (Element<T> element : elements) {
            if (element.object == object) {
                element.blocking = false;
                return true;
            }
        }
        return false;
    }

    public void clear() {
        elements.clear();
    }

    static class Element<T> {

        final T object;

        boolean blocking = false;

        public Element(T object) {
            this.object = object;
        }

        public T block() {
            blocking = !blocking;
            return object;
        }
    }
}