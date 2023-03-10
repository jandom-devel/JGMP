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
 * The native {@code mp_bitcnt_t} data type, which may be a 32 or 64 bit
 * unsigned integer.
 */
public class MpBitcntT extends IntegerType {

    /**
     * The size of the native {@code mp_bitcnt_t} data type. This is equal to the
     * size of a native long.
     */
    static final int SIZE = Native.LONG_SIZE;

    /**
     * Create an {@code mp_bitcnt_t} with value 0
     */
    public MpBitcntT() {
        this(0);
    }

    /**
     * Create a {@code mp_bitcnt_t} with the specified {@code value}. The value is
     * truncated when {@code mp_bitcnt_t} is a 32 bit integer.
     */
    public MpBitcntT(long value) {
        super(SIZE, value, true);
    }
}
