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
package it.unich.jgmp;

import static it.unich.jgmp.nativelib.LibGMP.__gmp_asprintf;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_printf;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_scanf;
import static it.unich.jgmp.nativelib.LibGMP.__gmp_sscanf;

import java.lang.ref.Cleaner;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import it.unich.jgmp.nativelib.LibGMP;

/**
 * Class collecting global variables and static methods which do no fit in
 * more specific classes.
 */
public class GMP {
    /**
     * A private constructor since this class should never be instantiated.
     */
    private GMP() {  }

    /**
     * Cleaner used by the JGMP library.
     */
    static final Cleaner cleaner = Cleaner.create();

    /**
     * Returns the version of the native GMP library.
     */
    public static String getNativeVersion() {
        return LibGMP.__gmp_version;
    }

    /**
     * Prints to the standard output according to the format specification in
     * {@code fmt} and the additional arguments in {@code args}.
     * <p>
     * This is similar to the C {@code printf} function and the Java
     * {@link java.io.PrintStream#printf(String, Object...)} method. If the format
     * string is invalid, or the arguments don’t match what the format specifies,
     * then the behaviour of any of these functions will be unpredictable. It will
     * return -1 to indicate a write error. Output is not “atomic”, so partial
     * output may be produced if a write error occurs.
     * <p>
     * This method bypasses the standard I/O streams of Java. It is generally better
     * to use the {@link sprintf(String, Object...)} method.
     * <p>
     * See also page
     * <a href="https://gmplib.org/manual/Formatted-Output-Strings" target="_blank">Formatted Output
     * Strings</a> from the GMP manual.
     *
     * @return the number of characters written, -1 if an error occured.
     */
    public static int printf(String format, Object... args) {
        return __gmp_printf(format, args);
    }

    /**
     * Returns a string according to the format specification in {@code fmt} and the
     * additional arguments in {@code args}. This is similar to the C {@code asprintf} function
     * and the Java {@link String#format(String, Object...)} method. If the format
     * string is invalid, or the arguments don’t match what the format specifies,
     * then the behaviour of any of these functions will be unpredictable.
     * <p>
     * See also page
     * <a href="https://gmplib.org/manual/Formatted-Output-Strings" target="_blank">Formatted Output
     * Strings</a> from the GMP manual..org/manual/Formatted-Output-Strings
     */
    public static String sprintf(String format, Object... args) {
        var pp = new PointerByReference();
        __gmp_asprintf(pp, format, args);
        var p = pp.getValue();
        var s = p.getString(0);
        Native.free(Pointer.nativeValue(p));
        return s;
    }

    /**
     * Parses the standard input according to the format specification in
     * {@code fmt} and fills the variables in {@code args}. This is similar to the C
     * {@code scanf} function. If the format string is invalid, or the arguments
     * don’t match what the format specifies, then the behaviour of any of these
     * functions will be unpredictable. No overlap is permitted between the fmt
     * string and any of the results produced.
     * <p>
     * This method bypasses the standard I/O streams of Java. It is generally better
     * to use the {@link sscanf} method.
     * <p>
     * See also pages
     * <a href="https://gmplib.org/manual/Formatted-Input-Strings" target="_blank">Formatted Input
     * Strings</a> and
     * <a href="https://gmplib.org/manual/Formatted-Input-Functions" target="_blank">Formatted Input
     * Functions</a> from the GMP manual.
     *
     * @return the number of fields successfully parsed and stored.
     */
    public static int scanf(String format, Object... args) {
        return __gmp_scanf(format, args);
    }

    /**
     * Parses a string according to the format specification in {@code fmt} and
     * fills the variables in {@code args}. This is similar to the C {@code scanf}
     * function. If the format string is invalid, or the arguments don’t match what
     * the format specifies, then the behaviour of any of these functions will be
     * unpredictable. No overlap is permitted between the fmt string and any of the
     * results produced.
     * <p>
     * See also pages
     * <a href="https://gmplib.org/manual/Formatted-Input-Strings" target="_blank">Formatted Input
     * Strings</a> and
     * <a href="https://gmplib.org/manual/Formatted-Input-Functions" target="_blank">Formatted Input
     * Functions</a> from the GMP manual.
     *
     * @return the number of fields successfully parsed and stored.
     */
    public static int sscanf(String s, String fmt, Object... args) {
        return __gmp_sscanf(s, fmt, args);
    }

}
