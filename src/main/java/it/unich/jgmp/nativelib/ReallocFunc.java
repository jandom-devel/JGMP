/**
* Copyright 2022, 2023 Gianluca Amato <gianluca.amato@unich.it>
*                  and Francesca Scozzari <francesca.scozzari@unich.it>
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
 * Custom reallocator function callback.
 */
public interface ReallocFunc extends Callback {
    /**
     * Resize a previously allocated block {@code ptr} of {@code old_size} bytes to
     * be {@code new_size} bytes.
     *
     * The block may be moved if necessary or if desired, and in that case the
     * smaller of {@code old_size} and {@code new_size} bytes must be copied to the
     * new location. The return value is a pointer to the resized block, that being
     * the new location if moved or just ptr if not.
     * <p>
     * {@code ptr} is never NULL, it’s always a previously allocated block.
     * {@code new_size} may be bigger or smaller than {@code old_size}.
     */
    public Pointer invoke(Pointer ptr, SizeT old_size, SizeT new_size);
}
