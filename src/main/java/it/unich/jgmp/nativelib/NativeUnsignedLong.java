package it.unich.jgmp.nativelib;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;

/**
 * The native unsigned long data type, which may be 32 or 64 bits.
 */
public class NativeUnsignedLong extends IntegerType {

    private static final int SIZE = Native.LONG_SIZE;

    /**
     * Creates a {@code size_t} with value 0
     */
    public NativeUnsignedLong() {
        this(0);
    }

    /**
     * Creates a {@code mp_size_t} with the specified {@code value}. The value is
     * truncated when {@code size_t} is a 32 bit integer.
     */
    public NativeUnsignedLong(long value) {
        super(SIZE, value, true);
    }
}
