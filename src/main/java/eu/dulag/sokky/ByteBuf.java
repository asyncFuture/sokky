package eu.dulag.sokky;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ByteBuf {

    private enum DataType {

        BYTE(byte.class, Byte.BYTES, "put", "get"), INT(int.class, Integer.BYTES, "putInt", "getInt"), LONG(long.class, Long.BYTES, "putLong", "getLong"), SHORT(short.class, Short.BYTES, "putShort", "getShort"), CHAR(char.class, Character.BYTES, "putChar", "getChar"), DOUBLE(double.class, Double.BYTES, "putDouble", "getDouble"), FLOAT(float.class, Float.BYTES, "putFloat", "getFloat");

        final Class<?> clazz;
        final int length;
        final String[] invokes;

        DataType(Class<?> clazz, int length, String... invokes) {
            this.clazz = clazz;
            this.length = length;
            this.invokes = invokes;
        }

        @SuppressWarnings("unchecked")
        <T> T get(ByteBuf buf) {
            try {
                if (outOfBounds(buf, length)) {
                    throw new IndexOutOfBoundsException(invokes[1] + "(index:" + buf.readable() + ", newIndex:" + (buf.readable() - length + ") buffer has been exhausted"));
                }
                ByteBuffer buffer = buf.context();
                Method method = buffer.getClass().getMethod(invokes[1]);
                method.setAccessible(true);
                return (T) method.invoke(buffer);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new BufferUnderflowException();
            }
        }

        ByteBuf put(ByteBuf buf, Object value) {
            try {
                if (outOfBounds(buf, length)) buf.enlarge(length);
                ByteBuffer buffer = buf.context();

                Method method = buffer.getClass().getMethod(invokes[0], clazz);
                method.setAccessible(true);
                method.invoke(buffer, value);
                return buf;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new BufferOverflowException();
            }
        }

        private static DataType find(Class<?> clazz) {
            return Arrays.stream(values()).filter(dataType -> dataType.clazz.equals(clazz)).findFirst().orElse(null);
        }
    }

    private static final Blocking<ByteBuf> BLOCKING = new Blocking<>(alloc(1024), alloc(1024));

    public static ByteBuf poll() {
        return BLOCKING.poll();
    }

    public static ByteBuf await() throws InterruptedException {
        return BLOCKING.await();
    }

    protected static ByteBuffer alloc(int capacity, boolean direct) {
        return direct ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity);
    }

    public static ByteBuf alloc(int capacity) {
        return new ByteBuf(capacity, false);
    }

    public static ByteBuf allocDirect(int capacity) {
        return new ByteBuf(capacity, true);
    }

    private static boolean outOfBounds(ByteBuf buf, int size) {
        int remaining = buf.readable();
        return (remaining - size < 0);
    }

    private final boolean direct;
    private ByteBuffer buffer;

    public ByteBuf(int capacity, boolean direct) {
        this.direct = direct;
        this.buffer = alloc(capacity, direct);
    }

    public ByteBuf enlarge(int capacity) {
        int size = (buffer.capacity() + capacity);

        ByteBuffer enlarge = alloc(size, direct);
        enlarge.put((ByteBuffer) buffer.flip());

        buffer = enlarge;

        return this;
    }

    public <T> T get(Class<T> clazz) {
        return DataType.find(clazz).get(this);
    }

    public ByteBuf put(Class<?> clazz, Object value) {
        return DataType.find(clazz).put(this, value);
    }

    public ByteBuf write(ByteBuffer value) {
        int remaining = value.remaining();
        if (outOfBounds(this, remaining)) enlarge(remaining);
        buffer.put(value);
        return this;
    }

    public ByteBuf write(ByteBuf buf) {
        return write(buf.context());
    }

    public ByteBuf writeBytes(byte[] bytes, int offset, int length) {
        if (offset < 0) throw new IndexOutOfBoundsException("writeBytes(offset:" + offset + ") is negative");
        if (length <= 0)
            throw new IndexOutOfBoundsException("writeBytes(length:" + length + ") must be greater than 0");
        int reads = (offset + length);
        if (reads > bytes.length)
            throw new IndexOutOfBoundsException("writeBytes(offset + length:4, bytes:3) added sum is greater than bytes");
        if (outOfBounds(this, reads)) enlarge(reads);
        buffer.put(bytes, offset, length);
        return this;
    }

    public ByteBuf writeBytes(byte[] bytes) {
        return writeBytes(bytes, 0, bytes.length);
    }

    public ByteBuf readBytes(byte[] bytes, int offset, int length) {

        if (offset < 0) throw new IndexOutOfBoundsException("readBytes(offset:" + offset + ") is negative");
        if (length <= 0) throw new IndexOutOfBoundsException("readBytes(length:" + length + ") must be greater than 0");
        if (length > readable()) throw new IndexOutOfBoundsException("buffer is to small");
        if (bytes.length < length) throw new IndexOutOfBoundsException("test");

        if (offset > length) throw new IndexOutOfBoundsException("offset to big");

        //nio method
        buffer.get(bytes, offset, length);
        return this;
    }

    public ByteBuf writeByte(byte value) {
        return put(byte.class, value);
    }

    public byte readByte() {
        return get(byte.class);
    }

    public ByteBuf writeInt(int value) {
        return put(int.class, value);
    }

    public int readInt() {
        return get(int.class);
    }

    public ByteBuf writeLong(long value) {
        return put(long.class, value);
    }

    public long readLong() {
        return get(long.class);
    }

    public ByteBuf writeShort(short value) {
        return put(short.class, value);
    }

    public long readShort() {
        return get(Short.class);
    }

    public ByteBuf writeChar(char value) {
        return put(char.class, value);
    }

    public char readChar() {
        return get(char.class);
    }

    public ByteBuf writeDouble(double value) {
        return put(double.class, value);
    }

    public double readDouble() {
        return get(double.class);
    }

    public ByteBuf writeFloat(float value) {
        return put(float.class, value);
    }

    public float readFloat() {
        return get(float.class);
    }

    public ByteBuf writeString(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        if (outOfBounds(this, Integer.BYTES + bytes.length)) enlarge(Integer.BYTES + bytes.length);
        writeInt(bytes.length);
        writeBytes(bytes);

        return this;
    }

    public String readString() {
        int length = readInt();
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) bytes[i] = readByte();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public ByteBuffer context() {
        return buffer;
    }

    public ByteBuf clear() {
        buffer.clear();
        return this;
    }

    public ByteBuf flip() {
        buffer.flip();
        return this;
    }

    public void detach() {
        BLOCKING.detach(this);
    }

    public byte[] array(boolean raw) {
        if (raw) return buffer.array();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    public byte[] array() {
        return array(false);
    }

    public boolean isDirect() {
        return direct;
    }

    public boolean isReadable() {
        return buffer.hasRemaining();
    }

    public int readable() {
        return buffer.remaining();
    }

    public int capacity() {
        return buffer.capacity();
    }
}