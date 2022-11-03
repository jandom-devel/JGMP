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

import static it.unich.jgmp.nativelib.LibGmp.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

import it.unich.jgmp.nativelib.MpBitcntT;
import it.unich.jgmp.nativelib.MpqT;
import it.unich.jgmp.nativelib.NativeUnsignedLong;

/**
 * The class encapsulating the {@code mpq_t} data type, i.e., multi-precision
 * rational numbers.
 *
 * <p>
 * An element of {@code MPQ} contains a pointer to a native {@code mpq_t}
 * variable and registers itself with {@link GMP#cleaner} for freeing all
 * allocated memory during garbage collection.
 * <p>
 * In determining the names and prototypes of the methods of the {@code MPQ}
 * class, we adopted the following rules:
 * <ul>
 * <li>functions {@code mpq_inits}, {@code mpq_clear}, {@code mpq_clears} and
 * {@code mpq_canonicalize} are only used internally and are not exposed by the
 * {@code MPQ} class;
 * <li>functions in the category <em>Input and Output Functions</em>, and macros
 * {@code mpq_numref} and {@code mpq_denref} are not exposed by the {@code MPQ}
 * class;
 * <li>if {@code baseName} begins with {@code set} or {@code swap}, we create a
 * method called {@code baseName} which calls the original function, implicitly
 * using {@code this} as the first {@code mpq_t} parameter;
 * <li>if {@code baseName} begins with {@code init}, we create a side-effect
 * free static method (see later);
 * <li>for all the other functions:
 * <ul>
 * <li>if the function has a non constant {@code mpq_t} parameter, then we
 * create a method {@code baseNameAssign} which calls the original function,
 * implicitly using {@code this} as the non-constant {@code mpq_t} parameter;
 * <li>we create e side-effect free method called {@code baseName}, with the
 * exception of a few cases where such as a method would not be particularly
 * useful.
 * </ul>
 * </ul>
 * <p>
 * In general, all the parameters which are not provided implicitly to the
 * original GMP function through {@code this} should be provided explicitly by
 * having them in the method prototype.
 * <p>
 * The side-effect free methods are designed as follows. First of all, we
 * distinguish between input and output parameters for the GMP function. The
 * side-effect free method takes all input parameters in its prototype, with the
 * exception of the first input {@code mpq_t} parameter which is mapped to
 * {@code this}. If there are no input {@code mpq_t} parameters, the method will
 * be static. The method creates a new object for the output parameter,
 * eventually cloning the ones also used as an input. After calling the GMP
 * functions, the return value or the output parameter is returned by the
 * method.
 */
public class MPQ extends Number implements Comparable<MPQ> {

    /**
     * Version for serializability.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The pointer to the native {@code mpq_t} object.
     */
    private transient MpqT mpqPointer;

    /**
     * Cleaning action for the {@code MPQ} class.
     */
    private static class MPQCleaner implements Runnable {
        private MpqT mpqPointer;

        MPQCleaner(MpqT mpqPointer) {
            this.mpqPointer = mpqPointer;
        }

        @Override
        public void run() {
            mpq_clear(mpqPointer);
        }
    }

    /**
     * Returns the native pointer to the GMP object.
     */
    public MpqT getPointer() {
        return mpqPointer;
    }

    // Initializing Integers

    /**
     * Returns an {@code MPQ} whose value is zero.
     */
    static public MPQ init() {
        return new MPQ();
    }

    // Assigning Integers

    /**
     * Sets this {@code MPQ} to {@code op}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ set(MPQ op) {
        mpq_set(mpqPointer, op.mpqPointer);
        return this;
    }

    /**
     * Sets this {@code MPQ} to {@code op}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ set(MPZ op) {
        mpq_set_z(mpqPointer, op.getPointer());
        return this;
    }

    /**
     * Sets this {@code MPQ} to {@code num/den}.
     *
     * @return this {@code MPQ}.
     *
     * @apiNote {@code den} should be treated as an unsigned long.
     */
    public MPQ set(long num, long den) {
        mpq_set_si(mpqPointer, new NativeLong(num), new NativeLong(den));
        mpq_canonicalize(mpqPointer);
        return this;
    }

    /**
     * Sets this {@code MPQ} to {@code op}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ set(long op) {
        mpq_set_si(mpqPointer, new NativeLong(op), new NativeLong(1));
        return this;
    }

    /**
     * Sets this {@code MPQ} to {@code num/den}.
     *
     * @return this {@code MPQ}.
     *
     * @apiNote {@code num} and {@code den} should be treated as an unsigned long.
     */
    public MPQ setUi(long num, long den) {
        mpq_set_ui(mpqPointer, new NativeUnsignedLong(num), new NativeUnsignedLong(den));
        return this;
    }

    /**
     * Sets this {@code MPQ} to the number represented by the string {@code str} in
     * the specified {@code base}. See the GMP function
     * <a href="https://gmplib.org/manual/Initializing-Rationals" target="
     * _blank">{@code mpq_set_str}</a>.
     *
     * @return 0 if the operation succeeded, -1 otherwise. In the latter case,
     *         {@code this} is not altered.
     */
    public int set(String str, int base) {
        int res = mpq_set_str(mpqPointer, str, base);
        if (res == 0)
            mpq_canonicalize(mpqPointer);
        return res;
    }

    /**
     * Swap the value of this {@code MPQ} with the value of {@code op}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ swap(MPQ op) {
        mpq_swap(mpqPointer, op.mpqPointer);
        return this;
    }

    // Conversion Functions

    /**
     * Converts this {@code MPQ} to a double, truncating if necessary. If the
     * exponent from the conversion is too big or too small to fit a double then the
     * result is system dependent. For too big an infinity is returned when
     * available. For too small 0.0 is normally returned. Hardware overflow,
     * underflow and denorm traps may or may not occur.
     */
    public double getD() {
        return mpq_get_d(mpqPointer);
    }

    /**
     * Sets this {@code MPQ} to {@code op}. There is no rounding, this conversion is
     * exact.
     *
     * @throws IllegalArgumentException if {@code op} is not a finite number. In
     *                                  this case, {@code this} is not altered.
     * @return this {@code MPQ}.
     */
    public MPQ set(double op) {
        if (!Double.isFinite(op))
            throw new IllegalArgumentException("op should be a finite number");
        mpq_set_d(mpqPointer, op);
        return this;
    }

    /**
     * Sets this {@code MPQ} to {@code op}. There is no rounding, this conversion is
     * exact.
     *
     * @return this {@code MPQ}.
     */
    public MPQ set(MPF op) {
        mpq_set_f(mpqPointer, op.getPointer());
        return this;
    }

    /**
     * Returns the String representation of this {@code MPQ} in the specified
     * {@code base}, or {@code null} if the base is not valid. See the GMP function
     * <a href="https://gmplib.org/manual/Rational-Conversions" target=
     * "_blank">{@code mpq_get_str}</a>.
     */
    public String getStr(int base) {
        Pointer ps = mpq_get_str(null, base, mpqPointer);
        if (ps == null)
            return null;
        var s = ps.getString(0);
        Native.free(Pointer.nativeValue(ps));
        return s;
    }

    // Rational Arithmetic

    /**
     * Sets this {@code MPQ} to {@code (op1 + op2)}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ addAssign(MPQ op1, MPQ op2) {
        mpq_add(mpqPointer, op1.mpqPointer, op2.mpqPointer);
        return this;
    }

    /**
     * Returns an {@code MPQ} whose value is {@code (this + op)}.
     */
    public MPQ add(MPQ op) {
        return new MPQ().addAssign(this, op);
    }

    /**
     * Sets this {@code MPQ} to {@code (op1 - op2)}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ subAssign(MPQ op1, MPQ op2) {
        mpq_sub(mpqPointer, op1.mpqPointer, op2.mpqPointer);
        return this;
    }

    /**
     * Returns an {@code MPQ} whose value is {@code (this - op)}.
     */
    public MPQ sub(MPQ op) {
        return new MPQ().subAssign(this, op);
    }

    /**
     * Sets this {@code MPQ} to {@code (op1 * op2)}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ mulAssign(MPQ op1, MPQ op2) {
        mpq_mul(mpqPointer, op1.mpqPointer, op2.mpqPointer);
        return this;
    }

    /**
     * Returns an {@code MPQ} whose value is {@code (this * op)}.
     */
    public MPQ mul(MPQ op) {
        return new MPQ().mulAssign(this, op);
    }

    /**
     * Sets this {@code MPQ} to {@code (op1 / op2)}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ divAssign(MPQ op1, MPQ op2) {
        mpq_div(mpqPointer, op1.mpqPointer, op2.mpqPointer);
        return this;
    }

    /**
     * Returns an {@code MPQ} whose value is {@code (this / op)}.
     */
    public MPQ div(MPQ op) {
        return new MPQ().divAssign(this, op);
    }

    /**
     * Sets this {@code MPQ} to <code>(op * 2<sup>b</sup>)</code>.
     *
     * @return this {@code MPQ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPQ mul2ExpAssign(MPQ op, long b) {
        mpq_mul_2exp(mpqPointer, op.mpqPointer, new MpBitcntT(b));
        return this;
    }

    /**
     * Returns an {@code MPQ} whose value is <code>(this * 2<sup>b</sup>)</code>.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPQ mul2Exp(long b) {
        return new MPQ().mul2ExpAssign(this, b);
    }

    /**
     * Sets this {@code MPQ} to <code>(op / 2<sup>b</sup>)</code>.
     *
     * @return this {@code MPQ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPQ div2ExpAssign(MPQ op, long b) {
        mpq_div_2exp(mpqPointer, op.mpqPointer, new MpBitcntT(b));
        return this;
    }

    /**
     * Returns an {@code MPQ} whose value is <code>(this / 2<sup>b</sup>)</code>.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPQ div2Exp(long b) {
        return new MPQ().div2ExpAssign(this, b);
    }

    /**
     * Sets this {@code MPQ} to {@code (- op)}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ negAssign(MPQ op) {
        mpq_neg(mpqPointer, op.mpqPointer);
        return this;
    }

    /**
     * Returns an {@code MPQ} whose value is the quotient of {@code (- this)}.
     */
    public MPQ neg() {
        return new MPQ().negAssign(this);
    }

    /**
     * Sets this {@code MPQ} to the absolute value of {@code op}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ absAssign(MPQ op) {
        mpq_abs(mpqPointer, op.mpqPointer);
        return this;
    }

    /**
     * Returns an {@code MPQ} whose value is the absolute value of {@code this}.
     */
    public MPQ abs() {
        return new MPQ().absAssign(this);
    }

    /**
     * Sets this {@code MPQ} to {@code 1/op}. If the new denominator is zero, this
     * routine will divide by zero.
     *
     * @return this {@code MPQ}.
     */
    public MPQ invAssign(MPQ op) {
        mpq_inv(mpqPointer, op.mpqPointer);
        return this;
    }

    /**
     * Returns an {@code MPQ} whose value is {@code 1/this}. If the new denominator
     * is zero, this routine will divide by zero.
     */
    public MPQ inv() {
        return new MPQ().invAssign(this);
    }

    // Comparison Functions

    /**
     * Compares {@code this} with {@code op}. Returns a positive value if
     * {@code (this > op)}, zero if {@code this = op}, or a negative value if
     * {@code this < op}.
     */
    public int cmp(MPQ op) {
        return mpq_cmp(mpqPointer, op.mpqPointer);
    }

    /**
     * Compares {@code this} with {@code op}. Returns a positive value if
     * {@code (this > op)}, zero if {@code this = op}, or a negative value if
     * {@code this < op}.
     */
    public int cmp(MPZ op) {
        return mpq_cmp_z(mpqPointer, op.getPointer());
    }

    /**
     * Compares {@code this} with {@code num/dem}. Returns a positive value if
     * {@code (this > num/dem)}, zero if {@code this = num/dem}, or a negative value
     * if {@code this < num/dem}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public int cmp(long num, long den) {
        return mpq_cmp_si(mpqPointer, new NativeLong(num), new NativeUnsignedLong(den));
    }

    /**
     * Compares {@code this} with {@code num/dem}. Returns a positive value if
     * {@code (this > num/dem)}, zero if {@code this = num/dem}, or a negative value
     * if {@code this < num/dem}.
     *
     * @apiNote {@code num} and {@code den} should be treated as an unsigned long.
     */
    public int cmpUi(long num, long den) {
        return mpq_cmp_ui(mpqPointer, new NativeUnsignedLong(num), new NativeUnsignedLong(den));
    }

    /**
     * Returns {@code +1} if {@code (this > 0)}, {@code 0} if {@code (this = 0)} and
     * {@code -1} if {@code this < 0}.
     */
    public int sgn() {
        return mpq_sgn(mpqPointer);
    }

    /**
     * Returns true if {@code this} is equal to {@code op}, false otherwise.
     * Although {@code cmp} can be used for the same purpose, this method should be
     * faster.
     */
    public boolean equal(MPQ op) {
        return mpq_equal(mpqPointer, op.mpqPointer);
    }

    // Applying Integer Functions to Rationals

    /**
     * Returns the numerator of {@code this}.
     */
    public MPZ getNum() {
        MPZ res = new MPZ();
        mpq_get_num(res.getPointer(), mpqPointer);
        return res;
    }

    /**
     * Returns the denominator of {@code this}.
     */
    public MPZ getDen() {
        MPZ res = new MPZ();
        mpq_get_den(res.getPointer(), mpqPointer);
        return res;
    }

    /**
     * Sets the numerator of {@code this} to the value {@code num}.
     */
    public MPQ setNum(MPZ num) {
        mpq_set_num(mpqPointer, num.getPointer());
        mpq_canonicalize(mpqPointer);
        return this;
    }

    /**
     * Sets the denominator of {@code this} to the value {@code den}.
     */
    public MPQ setDen(MPZ den) {
        mpq_set_den(mpqPointer, den.getPointer());
        mpq_canonicalize(mpqPointer);
        return this;
    }

    // Java name aliases

    /**
     * Builds an {@code MPQ} whose value is zero.
     */
    public MPQ() {
        mpqPointer = new MpqT();
        mpq_init(mpqPointer);
        GMP.cleaner.register(this, new MPQCleaner(mpqPointer));
    }

    /**
     * Builds an {@code MPQ} whose value is {@code op}.
     */
    public MPQ(MPQ op) {
        this();
        set(op);
    }

    /**
     * Builds an {@code MPQ} whose value is {@code op}.
     */
    public MPQ(MPZ op) {
        this();
        set(op);
    }

    /**
     * Builds an {@code MPQ} whose value is {@code num/dem}.
     *
     * @apiNote {@code den} should be treated as an unsigned long.
     *
     */
    public MPQ(long num, long dem) {
        this();
        set(num, dem);
    }

    /**
     * Builds an {@code MPQ} whose value is {@code op}.
     */
    public MPQ(long num) {
        this();
        set(num);
    }

    /**
     * Builds an {@code MPQ} whose value is {@code op}. There is no rounding, this
     * conversion is exact.
     *
     * @throws IllegalArgumentException if {@code op} is not a finite number. In
     *                                  this case, {@code this} is not altered.
     */
    public MPQ(double op) {
        this();
        set(op);
    }

    /**
     * Builds an {@code MPQ} whose value is {@code op}. There is no rounding, this
     * conversion is exact.
     */
    public MPQ(MPF op) {
        this();
        set(op);
    }

    /**
     * Builds an {@code MPQ} whose value is the number represented by the string
     * {@code str} in the specified {@code base}. See the GMP function
     * <a href="https://gmplib.org/manual/Initializing-Rationals" target=
     * "_blank">{@code mpq_set_str}</a>.
     *
     * @throws IllegalArgumentException if either {@code base} is not valid or
     *                                  {@code str} is not a valid string in the
     *                                  specified {@code base}.
     *
     */
    public MPQ(String str, int base) {
        this();
        set(str, base);
    }

    /**
     * Builds an {@code MPQ} whose value is the number represented by the string
     * {@code str} in decimal base. See the GMP function
     * <a href="https://gmplib.org/manual/Initializing-Rationals" target=
     * "_blank">{@code mpq_set_str}</a>.
     *
     * @throws IllegalArgumentException if {@code str} is not a valid number
     *                                  representation in decimal base.
     */
    public MPQ(String str) {
        this(str, 10);
    }

    /**
     * Sets this {@code MPQ} to {@code op}.
     */
    public MPQ setValue(MPQ op) {
        return set(op);
    }

    /**
     * Sets this {@code MPQ} to signed long {@code op}.
     */
    public MPQ setValue(long op) {
        return set(op);
    }

    /**
     * Sets this {@code MPQ} to op {@code op}. There is no rounding, this conversion
     * is exact.
     *
     * @throws IllegalArgumentException if {@code op} is not a finite number. In
     *                                  this case, {@code this} is not altered.
     */
    public MPQ setValue(double op) {
        return set(op);
    }

    /**
     * Sets this {@code MPQ} to op {@code op}. There is no rounding, this conversion
     * is exact.
     */
    public MPQ setValue(MPF op) {
        return set(op);
    }

    /**
     * Set this {@code MPQ} to the number represented by the string {@code str} in
     * the specified {@code base}. See the GMP function
     * <a href="https://gmplib.org/manual/Initializing-Rationals" target="
     * _blank">{@code mpq_set_str}</a>.
     *
     * @throws IllegalArgumentException if either {@code base} is not valid or
     *                                  {@code str} is not a valid number
     *                                  representation in the specified base. In
     *                                  this case, {@code this} is not altered.
     */
    public MPQ setValue(String str, int base) {
        var result = set(str, base);
        if (result == -1)
            throw new IllegalArgumentException(
                    "either base is not valid or str is not a valid number in the specified base");
        return this;
    }

    /**
     * Set this {@code MPQ} to the value represented by the string {@code str} in
     * decimal base.
     *
     * @throws IllegalArgumentException if {@code str} is not a valid number
     *                                  representation in decimal base.
     * @see setValue(String, int)
     */
    public MPQ setValue(String str) {
        var result = set(str, 10);
        if (result == -1)
            throw new IllegalArgumentException("str is not a valid number in decimal base");
        return this;
    }

    /**
     * Compares this {@code MPQ} with {@code op}. Returns a positive value if
     * {@code (this > op)}, zero if {@code this = op}, or a negative value if
     * {@code this < op}. This order is compatible with equality.
     */
    @Override
    public int compareTo(MPQ op) {
        return mpq_cmp(mpqPointer, op.mpqPointer);
    }

    /**
     * Compares this {@code MPQ} with the object {@code op} for equality. It returns
     * {@code true} if and only if {@code op} is an {@code MPQ} with the same value
     * of {@code this}.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof MPQ) {
            var q = (MPQ) obj;
            return mpq_equal(mpqPointer, q.mpqPointer);
        }
        return false;
    }

    /***
     * Returns a hash code value for this {@code MPQ}.
     */
    @Override
    public int hashCode() {
        return 0;
    }

    /**
     * Converts this {@code MPQ} to an integer, truncating if necessary.
     */
    public int intValue() {
        return (int) doubleValue();
    }

    /**
     * Converts this {@code MPQ} to an long, truncating if necessary.
     */
    public long longValue() {
        return (long) doubleValue();
    }

    /**
     * Converts this {@code MPQ} to a double, truncating if necessary.
     *
     * @see getD
     */
    public double doubleValue() {
        return getD();
    }

    /**
     * Converts this {@code MPQ} to a float, truncating if necessary.
     *
     * @see getD
     */
    public float floatValue() {
        return (float) getD();
    }

    /**
     * Converts this {@code MPQ} to its string representation in the specified
     * {@code base}, or {@code null} if the base is not valid. See the GMP function
     * <a href="https://gmplib.org/manual/Initializing-Rationals" target=
     * "_blank">{@code mpq_get_str}</a>.
     */
    public String toString(int base) {
        return getStr(base);
    }

    /**
     * Converts this {@code MPQ} to its decimal string representation.
     */
    @Override
    public String toString() {
        return getStr(10);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        // writeUTF seems more efficient, but has a limit of 64Kb
        // use base 62 to have a more compact representation
        out.writeObject(toString(62));
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        mpqPointer = new MpqT();
        mpq_init(mpqPointer);
        mpq_set_str(mpqPointer, (String) in.readObject(), 62);
        GMP.cleaner.register(this, new MPQCleaner(mpqPointer));
    }

    @SuppressWarnings("unused")
    private void readObjectNoData() throws ObjectStreamException {
        mpqPointer = new MpqT();
        mpq_init(mpqPointer);
        GMP.cleaner.register(this, new MPQCleaner(mpqPointer));
    }

}
