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

import com.sun.jna.IntegerType;
import com.sun.jna.Native;

/**
 * The native {@code mp_exp_t} data type, which may be a 32, 48 or 64 bit
 * signed integer. Here we assume that its size is the same of a native long.
 * This should work on almost every system, with the exception of some Cray's,
 * where the native size of {@code mp_size_t} is 48 bit.
 */
public class MPExpT extends IntegerType {

    /**
     * The size of the native {@code mp_exp_t} data type. We assume it to be equal
     * to the size of a native long.
     */
    static final int SIZE = Native.LONG_SIZE;

    /**
     * Creates an {@code mp_size_t} with value 0
     */
    public MPExpT() {
        this(0);
    }

    /**
     * Creates a {@code mp_exp_t} with the specified {@code value}. The value is
     * truncated when {@code mp_exp_t} is not a 32 bit integer.
     */
    public MPExpT(long value) {
        super(SIZE, value, false);
    }
}
