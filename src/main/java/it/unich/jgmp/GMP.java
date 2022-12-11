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

import static it.unich.jgmp.nativelib.LibGmp.gmp_asprintf;
import static it.unich.jgmp.nativelib.LibGmp.gmp_printf;
import static it.unich.jgmp.nativelib.LibGmp.gmp_scanf;
import static it.unich.jgmp.nativelib.LibGmp.gmp_sscanf;

import java.io.IOException;
import java.lang.ref.Cleaner;
import java.util.Properties;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import it.unich.jgmp.nativelib.LibGmp;

/**
 * Collects global variables and static methods which do no fit in more specific
 * classes.
 */
public class GMP {
    /**
     * A private constructor since this class should never be instantiated.
     */
    private GMP() {
    }

    /**
     * Java Properties used by JGMP.
     */
    private static final Properties properties = new Properties();

    static {
        final var resource = GMP.class.getClassLoader().getResourceAsStream("project.properties");
        if (resource != null)
            try {
                properties.load(resource);
            } catch (IOException e) {
            }
        ;
    }

    /**
     * Exception message for division by zero.
     */
    static final String MSG_DIVIDE_BY_ZERO = "/ by zero";

    /**
     * Exception message for even root of negative number.
     */
    static final String MSG_EVEN_ROOT_OF_NEGATIVE = "even root of a negative number";

    /**
     * Exception message for a non-finite double.
     */
    static final String MSG_FINITE_DOUBLE_REQUIRED = "Non-finite number is not allowed here";

    /**
     * Exception message for a NaN double.
     */
    static final String MSG_NAN_NOT_ALLOWED = "NaN is not allowed here";

    /**
     * Error message when arguments this and r cannot be the same object.
     */
    static final String MSG_SAME_OBJECT = "This and r cannot be the same object";

    /**
     * Error messages for invalid base.
     */
    static final String MSG_INVALID_BASE = "base is not valid";

    /**
     * Error messages for invalid conversion from strings.
     */
    static final String MSG_INVALID_STRING_CONVERSION = "either base is not valid or str is not a valid number in the specified base";

    /**
     * Error message for parameter size too big.
     */
    static final String MSG_SIZE_TOO_BIG = "size is too big";

    /**
     * Cleaner used by the JGMP library.
     */
    static final Cleaner cleaner = Cleaner.create();

    /**
     * Returns the version of the native GMP library.
     */
    public static String getVersion() {
        return LibGmp.gmp_version;
    }

    /**
     * Returns the JGMP library version.
     */
    public static String getJGmpVersion() {
        return properties.getProperty("jgmp.version");
    }

    /**
     * Returns the major version of the native GMP library.
     */
    public static int getMajorVersion() {
        return LibGmp.__GNU_MP_VERSION;
    }

    /**
     * Returns the minor version of the native GMP library.
     */
    public static int getMinorVersion() {
        return LibGmp.__GNU_MP_VERSION_MINOR;
    }

    /**
     * Returns the patch level of the native GMP library.
     */
    public static int getPatchLevel() {
        return LibGmp.__GNU_MP_VERSION_PATCHLEVEL;
    }

    /**
     * Returns the number of bits per limb. A limb means the part of a
     * multi-precision number that fits in a single machine word.
     */
    public static int getBitsPerLimb() {
        return LibGmp.mp_bits_per_limb;
    }

    /**
     * Returns the system decimal separator, as used by the
     * {@link GMP#sscanf(String, String, Object...)} and
     * {@link GMP#sprintf(String, Object...)} methods. This might not correspond to
     * the decimal separator of the current Java locale, since native locales are
     * more fine-grained then Java ones.
     */
    public static String getDecimalSeparator() {
        return LibGmp.decimalSeparator;
    }

    /**
     * Prints to the standard output according to the format specification in
     * {@code fmt} and the additional arguments in {@code args}. This is similar to
     * the C {@code printf} function and the Java
     * {@link java.io.PrintStream#printf(String, Object...)} method. If the format
     * string is invalid, or the arguments don't match what the format specifies,
     * then the behaviour of this function will be unpredictable. It will
     * return -1 to indicate a write error. Output is not "atomic", so partial
     * output may be produced if a write error occurs.
     * <p>
     * This method bypasses the standard I/O procedures of the JVM. It is generally
     * better to use the {@link GMP#sprintf(String, Object...)} method.
     * <p>
     * See also the page
     * <a href="https://gmplib.org/manual/Formatted-Output-Strings" target=
     * "_blank">Formatted Output Strings</a> from the GMP manual.
     *
     * @return the number of characters written, -1 if an error occured.
     */
    public static int printf(String format, Object... args) {
        return gmp_printf(format, args);
    }

    /**
     * Returns a string according to the format specification in {@code fmt} and the
     * additional arguments in {@code args}. This is similar to the C
     * {@code asprintf} function and the Java
     * {@link String#format(String, Object...)} method. If the format string is
     * invalid, or the arguments don't match what the format specifies, then the
     * behaviour of this function will be unpredictable.
     * <p>
     * See also the page
     * <a href="https://gmplib.org/manual/Formatted-Output-Strings" target=
     * "_blank">Formatted Output Strings</a> from the GMP
     * manual..org/manual/Formatted-Output-Strings
     */
    public static String sprintf(String format, Object... args) {
        var pp = new PointerByReference();
        gmp_asprintf(pp, format, args);
        var p = pp.getValue();
        var s = p.getString(0);
        Native.free(Pointer.nativeValue(p));
        return s;
    }

    /**
     * Parses the standard input according to the format specification in
     * {@code fmt}, filling the variables in {@code args}. This is similar to the C
     * {@code scanf} function. If the format string is invalid, or the arguments
     * don't match what the format specifies, then the behaviour of this
     * function will be unpredictable.
     * <p>
     * This method bypasses the standard I/O procedures of the JVM. It is generally
     * better to use the {@link GMP#sscanf(String, String, Object...)} method.
     * <p>
     * See also the pages
     * <a href="https://gmplib.org/manual/Formatted-Input-Strings" target=
     * "_blank">Formatted Input Strings</a> and
     * <a href="https://gmplib.org/manual/Formatted-Input-Functions" target=
     * "_blank">Formatted Input Functions</a> from the GMP manual.
     *
     * @return the number of fields successfully parsed and stored.
     */
    public static int scanf(String format, Object... args) {
        return gmp_scanf(format, args);
    }

    /**
     * Parses the string {@code s} according to the format specification in
     * {@code fmt}, filling the variables in {@code args}. This is similar to the C
     * {@code sscanf} function. If the format string is invalid, or the arguments
     * don't match what the format specifies, then the behaviour of this
     * function will be unpredictable.
     * <p>
     * See also the pages
     * <a href="https://gmplib.org/manual/Formatted-Input-Strings" target=
     * "_blank">Formatted Input Strings</a> and
     * <a href="https://gmplib.org/manual/Formatted-Input-Functions" target=
     * "_blank">Formatted Input Functions</a> from the GMP manual.
     *
     * @return the number of fields successfully parsed and stored.
     */
    public static int sscanf(String s, String fmt, Object... args) {
        return gmp_sscanf(s, fmt, args);
    }

}
