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

import java.lang.reflect.Method;

import com.sun.jna.NativeLibrary;

/**
 * A {@link com.sun.jna.FunctionMapper} which converts the official GMP
 * function names (beginning with {@code mpz}, {@code mpq}, {@code mpf} and
 * {@code gmp}) to the names used by C library (beginning with
 * {@code __gmp}).
 */
class GmpFunctionMapper implements com.sun.jna.FunctionMapper {

    private static GmpFunctionMapper instance = new GmpFunctionMapper();

    private GmpFunctionMapper() {
    }

    @Override
    public String getFunctionName(NativeLibrary library, Method method) {
        var methodName = method.getName();
        var nativeName = methodName.charAt(0) == 'g' ? "__" + methodName : "__g" + methodName;
        return nativeName;
    }

    public static GmpFunctionMapper getInstance() {
        return instance;
    }

}
