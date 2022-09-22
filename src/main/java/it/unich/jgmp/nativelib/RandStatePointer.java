package it.unich.jgmp.nativelib;

import com.sun.jna.Memory;
import com.sun.jna.PointerType;

/**
 * Type representing a native pointer to a {@code __gmp_randstate_struct}
 * structure.
 */
public class RandStatePointer extends PointerType {

    /**
     * Allocates the memory needed for an {@code __gmp_randstate_struct} structure
     * and returns the pointer to it.
     */
    public RandStatePointer() {
        setPointer(new Memory(LibGMP.RANDSTATE_SIZE));
    }
}
