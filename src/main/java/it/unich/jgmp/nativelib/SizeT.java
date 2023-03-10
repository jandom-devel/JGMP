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

import com.sun.jna.IntegerType;
import com.sun.jna.Native;

/**
 * The native {@code size_t} data type, which may be a 32 or 64 bit unsigned
 * integer.
 */
public class SizeT extends IntegerType {
    /**
     * Create a {@code size_t} with value 0
     */
    public SizeT() {
        this(0);
    }

    /**
     * Create a {@code size_t} with the specified {@code value}. The value is
     * truncated when {@code size_t} is a 32 bit integer.
     */
    public SizeT(long value) {
        super(Native.SIZE_T_SIZE, value, true);
    }
}
