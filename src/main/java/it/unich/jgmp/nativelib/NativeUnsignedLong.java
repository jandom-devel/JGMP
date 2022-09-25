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
 * The native unsigned long data type, which may be a 32 or 64 bit integer.
 */
public class NativeUnsignedLong extends IntegerType {

    /**
     * Creates a native {@code unsigned long} with value 0
     */
    public NativeUnsignedLong() {
        this(0);
    }

    /**
     * Creates a native {@code unsigned long} with the specified {@code value}. The
     * value is truncated when the native {@code unsigned long} is a 32 bit integer.
     */
    public NativeUnsignedLong(long value) {
        super(Native.LONG_SIZE, value, true);
    }
}
