package it.unich.jgmp.nativelib;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

/**
 * Custom reallocator function callback.
 */
public interface ReallocFunc extends Callback {
    /**
     * Resize a previously allocated block {@code ptr} of {@code old_size} bytes to
     * be {@code new_size} bytes.
     *
     * The block may be moved if necessary or if desired, and in that case the
     * smaller of {@code old_size} and {@code new_size} bytes must be copied to the
     * new location. The return value is a pointer to the resized block, that being
     * the new location if moved or just ptr if not.
     * <p>
     * {@code ptr} is never NULL, it’s always a previously allocated block.
     * {@code new_size} may be bigger or smaller than {@code old_size}.
     */
    public Pointer invoke(Pointer ptr, SizeT old_size, SizeT new_size);
}
