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
import com.sun.jna.FromNativeConverter;
import com.sun.jna.ToNativeContext;
import com.sun.jna.ToNativeConverter;
import com.sun.jna.TypeMapper;

import it.unich.jgmp.MPF;
import it.unich.jgmp.MPQ;
import it.unich.jgmp.MPZ;

/**
 * A {@link com.sun.jna.TypeMapper} for the types {@link MPZ}, {@link MPQ} and
 * {@link MPF}. It is used by the {@code printf} / {@code scanf} functions to
 * correctly handle tese types.
 */
class GMPTypeMapper implements TypeMapper {

    private static GMPTypeMapper instance = new GMPTypeMapper();

    private GMPTypeMapper() {
    }

    /**
     * A converter from {@link MPZ} to the native pointer type. It is used by the
     * {@code printf} / {@code scanf} functions to correctly handle GMP types.
     */
    private static class MPZToNativeConverter implements ToNativeConverter {
        @Override
        public Class<?> nativeType() {
            return Pointer.class;
        }

        @Override
        public Object toNative(Object value, ToNativeContext context) {
            return ((MPZ) value).getPointer().getPointer();
        }
    }

    /**
     * A converter from {@link MPQ} to the native pointer type. It is used by the
     * {@code printf} / {@code scanf} functions to correctly handle GMP types.
     */
    private static class MPQToNativeConverter implements ToNativeConverter {
        @Override
        public Class<?> nativeType() {
            return Pointer.class;
        }

        @Override
        public Object toNative(Object value, ToNativeContext context) {
            return ((MPQ) value).getPointer().getPointer();
        }
    }

    /**
     * A converter from {@link MPF} to the native pointer type. It is used by the
     * {@code printf} / {@code scanf} functions to correctly handle GMP types.
     */
    private static class MPFToNativeConverter implements ToNativeConverter {
        @Override
        public Class<?> nativeType() {
            return Pointer.class;
        }

        @Override
        public Object toNative(Object value, ToNativeContext context) {
            return ((MPF) value).getPointer().getPointer();
        }
    }

    private final MPZToNativeConverter mpzConverter = new MPZToNativeConverter();
    private final MPQToNativeConverter mpqConverter = new MPQToNativeConverter();
    private final MPFToNativeConverter mpfConverter = new MPFToNativeConverter();

    @Override
    public FromNativeConverter getFromNativeConverter(Class<?> javaType) {
        return null;
    }

    @Override
    public ToNativeConverter getToNativeConverter(Class<?> javaType) {
        if (javaType == MPZ.class) {
            return mpzConverter;
        } else if (javaType == MPQ.class) {
            return mpqConverter;
        } else if (javaType == MPF.class) {
            return mpfConverter;
        }
        return null;
    }

    public static GMPTypeMapper getInstance() {
        return instance;
    }

}
