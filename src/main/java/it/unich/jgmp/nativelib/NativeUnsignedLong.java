package it.unich.jgmp.nativelib;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;

/**
 * The native unsigned long data type, which may be a 32 or 64 bit integer.
 */
public class NativeUnsignedLong extends IntegerType {

    /**
     * Creates a native {@code unsigned long} with value 0
     */
    public NativeUnsignedLong() {
        this(0);
    }

    /**
     * Creates a native {@code unsigned long} with the specified {@code value}. The
     * value is truncated when the native {@code unsigned long} is a 32 bit integer.
     */
    public NativeUnsignedLong(long value) {
        super(Native.LONG_SIZE, value, true);
    }
}
