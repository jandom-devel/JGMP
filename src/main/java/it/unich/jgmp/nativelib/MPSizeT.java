package it.unich.jgmp.nativelib;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;

/**
 * The native {@code mp_size_t} data type, which may be a 32, 48 or 64 bit
 * signed integer. Here we assume that its size is the same of a native long.
 * This should work on almost every system, with the exception of some Cray's,
 * where the native size of {@code mp_size_t} is 48 bit.
 */
public class MPSizeT extends IntegerType {

    /**
     * The size of the native {@code mp_size_t} data type. We assume it to be equal
     * to the size of a native long.
     */
    static final int SIZE = Native.LONG_SIZE;

    /**
     * Creates an {@code mp_size_t} with value 0
     */
    public MPSizeT() {
        this(0);
    }

    /**
     * Creates a {@code mp_size_t} with the specified {@code value}. The value is
     * truncated when {@code mp_size_t} is not a 32 bit integer.
     */
    public MPSizeT(long value) {
        super(SIZE, value, false);
    }
}
