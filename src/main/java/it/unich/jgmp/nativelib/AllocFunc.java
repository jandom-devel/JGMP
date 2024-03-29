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

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

/**
 * Custom allocator function callback.
 */
public interface AllocFunc extends Callback {
    /**
     * Return a pointer to newly allocated space with at least {@code alloc_size} bytes.
     */
    public Pointer invoke(SizeT alloc_size);

}
