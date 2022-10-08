package eu.dulag.sokky;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ByteBuf {

    public static int BUFFER_LENGTH = 12;

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
        return write(value.context());
    }

    public ByteBuf write(ByteBuffer value) {
        int size = value.remaining();
        if ((readable() - size) <= 0) enlarge(size + BUFFER_LENGTH);
        buffer.put(value);
        return this;
    }

    public ByteBuf writeByte(byte value) {
        if (buffer.remaining() <= 0) enlarge(1 + BUFFER_LENGTH);
        buffer.put(value);
        return this;
    }

    public byte readByte() {
        int remaining = buffer.remaining();
        if (remaining <= 0) {
            throw new IndexOutOfBoundsException("underflow(index:" + remaining + ", newIndex:" + (remaining - 1) + ")");
        }
        return buffer.get();
    }

    public ByteBuf writeInt(int value) {
        int size = length(value);
        if ((readable() - size) <= 0) enlarge(size + BUFFER_LENGTH);

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

    public ByteBuf writeShort(short value) {
        int size = Short.BYTES;
        if ((readable() - size) <= 0) enlarge(size + BUFFER_LENGTH);

        buffer.putShort(value);
        return this;
    }

    public long readLong() {
        int readable = readable();
        if ((readable - Long.BYTES) < 0)
            throw new IndexOutOfBoundsException("underflow(index:" + readable + ", newIndex:" + (readable - Long.BYTES) + ")");
        return buffer.getLong();
    }

    public ByteBuf writeLong(long value) {
        int size = Long.BYTES;
        if ((readable() - size) <= 0) enlarge(size + BUFFER_LENGTH);

        buffer.putLong(value);
        return this;
    }

    public short readShort() {
        int readable = readable();
        if ((readable - Short.BYTES) < 0)
            throw new IndexOutOfBoundsException("underflow(index:" + readable + ", newIndex:" + (readable - Short.BYTES) + ")");
        return buffer.getShort();
    }

    public ByteBuf writeBool(boolean value) {
        writeInt(value ? 1 : 0);
        return this;
    }

    public boolean readBool() {
        return readInt() == 1;
    }

    public ByteBuf writeString(String value) {
        byte[] bytes = value.getBytes();
        if ((readable() - bytes.length) <= 0) enlarge(length(bytes.length) + bytes.length + BUFFER_LENGTH);
        writeInt(bytes.length);
        for (byte aByte : bytes) writeByte(aByte);
        return this;
    }

    public String readString() {
        int length = readInt();
        int readable = readable();
        if ((readable - length) < 0)
            throw new IndexOutOfBoundsException("underflow(index:" + readable + ", newIndex:" + (readable - length) + ")");
        byte[] bytes = new byte[length];
        buffer.get(bytes);
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