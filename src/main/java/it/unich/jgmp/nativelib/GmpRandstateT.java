/**
* Copyright 2022, 2023 Gianluca Amato <gianluca.amato@unich.it>
*
* This file is part of JGMP. JGMP is free software: you can
* redistribute it and/or modify it under the terms of the GNU General Public
* License as published by the Free Software Foundation, either version 3 of the
* License, or (at your option) any later version.
*
* JGMP is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of a MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*
* You should have received a copy of the GNU General Public License along with
* JGMP. If not, see <http://www.gnu.org/licenses/>.
*/
package it.unich.jgmp.nativelib;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

/**
 * Type representing an object of the {@code gmp_randstate_t} native type.
 */
public class GmpRandstateT extends PointerType {

    /**
     * The size of the {@code gmp_randstate_t} structure.
     */
    static final int RANDSTATE_SIZE = 2 * MpzT.MPZ_SIZE + 4 + Native.POINTER_SIZE;

    /**
     * Allocate the memory needed for an {@code gmp_randstate_t} structure and
     * return the pointer to it.
     */
    public GmpRandstateT() {
        setPointer(new Memory(RANDSTATE_SIZE));
    }

    /**
     * Create a new {@code GmpRandstateT} corresponding to the pointer {@code p}.
     */
    public GmpRandstateT(Pointer p) {
        super(p);
    }
}
