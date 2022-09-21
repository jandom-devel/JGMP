package it.unich.jgmp.nativelib;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByReference;

/**
 * A reference to the native {@code size_t} data type.
 */
public class SizeTByReference extends ByReference {
    /**
     * Creates a reference to a newly allocated {@code size_t} object.
     */
    public SizeTByReference() {
        this(new SizeT());
    }

    /**
     * Creates a reference to a newly allocated {@code size_t} object, which is
     * initialized with {@code value}.
     */
    public SizeTByReference(SizeT value) {
        super(Native.SIZE_T_SIZE);
        setValue(value);
    }

    /**
     * Change the value of the {@code size_t} object pointed by this reference.
     */
    public void setValue(SizeT value) {
        Pointer p = getPointer();
        if (Native.SIZE_T_SIZE == 8) {
            p.setLong(0, value.longValue());
        } else {
            p.setInt(0, value.intValue());
        }
    }

    /**
     * Get the value of {@code size_t} object pointed by this reference.
     */
    public SizeT getValue() {
        Pointer p = getPointer();
        return new SizeT(Native.SIZE_T_SIZE == 8 ? p.getLong(0) : p.getInt(0));
    }
}
