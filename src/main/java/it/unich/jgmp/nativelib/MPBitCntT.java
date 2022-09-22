package it.unich.jgmp.nativelib;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;

/**
 * The native {@code mp_bitcnt_t} data type, which may be a 32 or 64 bit
 * unsigned integer.
 */
public class MPBitCntT extends IntegerType {

    /**
     * The size of the native {@code mp_bitcnt_t} data type. This is equal to the
     * size of a native long.
     */
    static final int SIZE = Native.LONG_SIZE;

    /**
     * Creates an {@code mp_bitcnt_t} with value 0
     */
    public MPBitCntT() {
        this(0);
    }

    /**
     * Creates a {@code mp_bitcnt_t} with the specified {@code value}. The value is
     * truncated when {@code mp_bitcnt_t} is a 32 bit integer.
     */
    public MPBitCntT(long value) {
        super(SIZE, value, true);
    }
}
