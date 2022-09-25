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
