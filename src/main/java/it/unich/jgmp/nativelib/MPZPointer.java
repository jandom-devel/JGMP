package it.unich.jgmp.nativelib;

import com.sun.jna.Memory;
import com.sun.jna.PointerType;

/**
 * Type representing a native pointer to an {@code mpz_t} native type.
 */
public class MPZPointer extends PointerType {

    /**
     * Allocates the memory needed for an {@code mpz_t} native type and returns the
     * pointer to it.
     */
    public MPZPointer() {
        setPointer(new Memory(LibGMP.MPZ_SIZE));
    }
}
