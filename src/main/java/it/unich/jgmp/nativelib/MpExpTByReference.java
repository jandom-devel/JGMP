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

import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByReference;

/**
 * A reference to the native {@code mp_exp_t} data type.
 */
public class MpExpTByReference extends ByReference {
    /**
     * Create a reference to a newly allocated {@code mp_exp_t} object.
     */
    public MpExpTByReference() {
        this(new MpExpT());
    }

    /**
     * Create a reference to a newly allocated {@code mp_exp_t} object, which is
     * initialized with {@code value}.
     */
    public MpExpTByReference(MpExpT value) {
        super(MpExpT.SIZE);
        setValue(value);
    }

    /**
     * Change the value of the {@code mp_exp_t} object pointed by this reference.
     */
    public void setValue(MpExpT value) {
        Pointer p = getPointer();
        if (MpExpT.SIZE == 8) {
            p.setLong(0, value.longValue());
        } else {
            p.setInt(0, value.intValue());
        }
    }

    /**
     * Get the value of {@code mp_exp_t} object pointed by this reference.
     */
    public MpExpT getValue() {
        Pointer p = getPointer();
        return new MpExpT(MpExpT.SIZE == 8 ? p.getLong(0) : p.getInt(0));
    }
}
