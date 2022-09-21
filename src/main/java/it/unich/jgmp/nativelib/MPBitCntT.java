package it.unich.jgmp.nativelib;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;

/**
 * The native {@code mp_size_t} data type, which may be 32 or 64 bits.
 */
public class MPBitCntT extends IntegerType {

    private static final int SIZE = Native.LONG_SIZE;

    /**
     * Creates a {@code size_t} with value 0
     */
    public MPBitCntT() {
        this(0);
    }

    /**
     * Creates a {@code mp_size_t} with the specified {@code value}. The value is
     * truncated when {@code size_t} is a 32 bit integer.
     */
    public MPBitCntT(long value) {
        super(SIZE, value, true);
    }
}
