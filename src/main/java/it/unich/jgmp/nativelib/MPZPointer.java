/**
* Copyright 2022 Gianluca Amato <gianluca.amato@unich.it>
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
import com.sun.jna.PointerType;

/**
 * Type representing a native pointer to an {@code mpz_t} native type.
 */
public class MPZPointer extends PointerType {

    /**
     * The size of the {@code mpz_t} native type.
     */
    static final int MPZ_SIZE = 4 + 4 + Native.POINTER_SIZE;

    /**
     * Allocates the memory needed for an {@code mpz_t} native type and returns the
     * pointer to it.
     */
    public MPZPointer() {
        setPointer(new Memory(MPZ_SIZE));
    }
}
