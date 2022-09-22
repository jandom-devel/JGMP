package it.unich.jgmp.nativelib;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;

/**
 * The native {@code size_t} data type, which may be a 32 or 64 bit unsigned
 * integer.
 */
public class SizeT extends IntegerType {
    /**
     * Creates a {@code size_t} with value 0
     */
    public SizeT() {
        this(0);
    }

    /**
     * Creates a {@code size_t} with the specified {@code value}. The value is
     * truncated when {@code size_t} is a 32 bit integer.
     */
    public SizeT(long value) {
        super(Native.SIZE_T_SIZE, value, true);
    }
}
