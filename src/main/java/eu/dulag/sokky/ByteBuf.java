package eu.dulag.sokky;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ByteBuf {

    public static final ByteBuffer BUFFER = ByteBuffer.allocateDirect(1024);

    public static int length(int value) {
        if ((value & 0xFFFFFF80) == 0) return 1;
        if ((value & 0xFFFFC000) == 0) return 2;
        if ((value & 0xFFE00000) == 0) return 3;
        if ((value & 0xF0000000) == 0) return 4;
        return 5;
    }

    public static ByteBuf alloc(int capacity) {
        return new ByteBuf(false, capacity);
    }

    public static ByteBuf allocDirect(int capacity) {
        return new ByteBuf(true, capacity);
    }

    protected final boolean direct;
    protected ByteBuffer buffer;

    protected int offset;

    protected ByteBuf(boolean direct, int capacity) {
        this.direct = direct;
        this.buffer = direct ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity);
    }

    public ByteBuf flip() {
        buffer.flip();
        return this;
    }

    public void clear() {
        buffer.clear();
    }

    public ByteBuf enlarge(int capacity) {
        int size = (buffer.capacity() + capacity);
        ByteBuffer enlarge = direct ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);

        enlarge.put((ByteBuffer) buffer.flip());

        buffer = enlarge;

        return this;
    }

    public ByteBuf write(ByteBuf value) {
        if (buffer.remaining() - 1 < value.capacity()) enlarge(value.capacity());
        while (value.isReadable()) writeByte(value.readByte());
        return this;
    }

    public ByteBuf write(ByteBuffer value) {
        if (buffer.remaining() - 1 < value.capacity()) enlarge(value.capacity());
        buffer.put(value);
        return this;
    }

    public ByteBuf writeBytes(byte[] bytes) {
        if (buffer.remaining() - bytes.length < 0) enlarge(bytes.length);
        buffer.put(bytes);
        return this;
    }

    public ByteBuf readBytes(byte[] bytes) {
        buffer.get(bytes);
        return this;
    }

    public ByteBuf writeByte(byte value) {
        if (buffer.remaining() - Byte.BYTES < 0) enlarge(Byte.BYTES);
        buffer.put(value);
        return this;
    }

    public byte readByte() {
        int remaining = buffer.remaining();
        if (remaining - Byte.BYTES < 0) {
            throw new RuntimeException("underflow(index:" + remaining + ", newIndex:" + (remaining - Byte.BYTES) + ")");
        }
        return buffer.get();
    }

    public ByteBuf writeInt(int value) {
        int length = length(value);
        if (buffer.remaining() - length < 0) enlarge(length);

        do {
            int part = value & 0x7F;
            value >>>= 7;
            if (value != 0) part |= 0x80;
            writeByte((byte) part);
        } while (value != 0);
        return this;
    }

    public int readInt() {
        int value = 0, bytes = 0;
        byte b;
        do {
            b = readByte();
            value |= (b & 0x7F) << (bytes++ * 7);
            if (bytes > 5) return value;
        } while ((b & 0x80) == 0x80);
        return value;
    }

    public ByteBuf writeLong(long value) {
        if (buffer.remaining() < Long.BYTES) enlarge(Long.BYTES);
        buffer.putLong(value);
        return this;
    }

    public long readLong() {
        int remaining = buffer.remaining();
        if (remaining - Long.BYTES < 0) {
            throw new RuntimeException("underflow(index:" + remaining + ", newIndex:" + (remaining - Long.BYTES) + ")");
        }
        return buffer.getLong();
    }

    public ByteBuf writeShort(short value) {
        if (buffer.remaining() < Short.BYTES) enlarge(Short.BYTES);
        buffer.putShort(value);
        return this;
    }

    public short readShort() {
        int remaining = buffer.remaining();
        if (remaining - Short.BYTES < 0) {
            throw new RuntimeException("underflow(index:" + remaining + ", newIndex:" + (remaining - Short.BYTES) + ")");
        }
        return buffer.getShort();
    }

    public ByteBuf writeString(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int size = (bytes.length + length(bytes.length));
        if (buffer.remaining() < size) enlarge(size);
        writeInt(bytes.length);
        return writeBytes(bytes);
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

    public byte[] array() {
        if (direct) throw new UnsupportedOperationException();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
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