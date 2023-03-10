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
 * Multi-precision rational numbers. This class encapsulates the {@code mpq_t}
 * data type, see the
 * <a href="https://gmplib.org/manual/Rational-Number-Functions" target=
 * "_blank">Rational Number Functions</a> page of the GMP manual. In determining
 * the names and signatures of the methods of the {@code MPQ} class, we adopt
 * the rules described in the documentation of the {@link it.unich.jgmp}
 * package, enriched with the following ones:
 * <ul>
 * <li>the function {@code mpq_canonicalize} is only used internally;
 * <li>the functions in the category <em>I/O of Rationals</em>, and the macros
 * {@code mpq_numref} and {@code mpq_denref} are not exposed by the {@code MPQ}
 * class.
 * </ul>
 */
public class MPQ extends Number implements Comparable<MPQ> {

    /**
     * Version for serializability.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The zero multi-precision rational.
     */
    private static final MPQ zero = new MPQ();

    /**
     * The pointer to the native {@code mpq_t} object.
     */
    private transient MpqT mpqNative;

    /**
     * Cleaning action for the {@code MPQ} class.
     */
    private static class MPQCleaner implements Runnable {
        private MpqT mpqNative;

        MPQCleaner(MpqT mpqNative) {
            this.mpqNative = mpqNative;
        }

        @Override
        public void run() {
            mpq_clear(mpqNative);
        }
    }

    /**
     * A private constructor which build an {@code MPQ} starting from a pointer to
     * its native data object. The native object needs to be already initialized.
     */
    private MPQ(MpqT pointer) {
        this.mpqNative = pointer;
        GMP.cleaner.register(this, new MPQCleaner(pointer));
    }

    /**
     * Return the native pointer to the GMP object.
     */
    public MpqT getNative() {
        return mpqNative;
    }

    // Initialization and Assignment Functions

    /**
     * Return an {@code MPQ} whose value is zero.
     */
    static public MPQ init() {
        var mpqNative = new MpqT();
        mpq_init(mpqNative);
        return new MPQ(mpqNative);
    }

    // Assigning Integers

    /**
     * Set this {@code MPQ} to {@code op}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ set(MPQ op) {
        mpq_set(mpqNative, op.mpqNative);
        return this;
    }

    /**
     * Set this {@code MPQ} to {@code op}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ set(MPZ op) {
        mpq_set_z(mpqNative, op.getNative());
        return this;
    }

    /**
     * Set this {@code MPQ} to {@code (<num/>den)}.
     *
     * @return this {@code MPQ}.
     *
     * @apiNote {@code den} should be treated as an unsigned long.
     */
    public MPQ set(long num, long den) {
        mpq_set_si(mpqNative, new NativeLong(num), new NativeLong(den));
        mpq_canonicalize(mpqNative);
        return this;
    }

    /**
     * Set this {@code MPQ} to {@code (num/den)}.
     *
     * @return this {@code MPQ}.
     *
     * @apiNote {@code num} and {@code den} should be treated as an unsigned long.
     */
    public MPQ setUi(long num, long den) {
        mpq_set_ui(mpqNative, new NativeUnsignedLong(num), new NativeUnsignedLong(den));
        mpq_canonicalize(mpqNative);
        return this;
    }

    /**
     * Set this {@code MPQ} to the number represented by the string {@code str} in
     * the specified {@code base}. See the GMP function
     * <a href="https://gmplib.org/manual/Initializing-Rationals" target="
     * _blank">{@code mpq_set_str}</a>.
     *
     * @return 0 if the operation succeeded, -1 otherwise. In the latter case,
     *         {@code this} is not altered.
     */
    public int set(String str, int base) {
        int res = mpq_set_str(mpqNative, str, base);
        if (res == 0)
            mpq_canonicalize(mpqNative);
        return res;
    }

    /**
     * Swap the value of this {@code MPQ} with the value of {@code op}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ swap(MPQ op) {
        mpq_swap(mpqNative, op.mpqNative);
        return this;
    }

    // Conversion Functions

    /**
     * Convert this {@code MPQ} to a double, truncating if necessary. If the
     * exponent from the conversion is too big or too small to fit a double then the
     * result is system dependent. For too big an infinity is returned when
     * available. For too small 0.0 is normally returned. Hardware overflow,
     * underflow and denorm traps may or may not occur.
     */
    public double getD() {
        return mpq_get_d(mpqNative);
    }

    /**
     * Set this {@code MPQ} to {@code op}. There is no rounding, this conversion is
     * exact.
     *
     * @throws ArithmeticException if {@code op} is not a finite number. In this
     *                             case, {@code this} is not altered.
     * @return this {@code MPQ}.
     */
    public MPQ set(double op) {
        if (!Double.isFinite(op))
            throw new ArithmeticException(GMP.MSG_FINITE_DOUBLE_REQUIRED);
        mpq_set_d(mpqNative, op);
        return this;
    }

    /**
     * Set this {@code MPQ} to {@code op}. There is no rounding, this conversion is
     * exact.
     *
     * @return this {@code MPQ}.
     */
    public MPQ set(MPF op) {
        mpq_set_f(mpqNative, op.getNative());
        return this;
    }

    /**
     * Return the String representation of this {@code MPQ} in the specified
     * {@code base}, or {@code null} if the base is not valid. See the GMP function
     * <a href="https://gmplib.org/manual/Rational-Conversions" target=
     * "_blank">{@code mpq_get_str}</a>.
     */
    public String getStr(int base) {
        Pointer ps = mpq_get_str(null, base, mpqNative);
        if (ps == null)
            return null;
        var s = ps.getString(0);
        Native.free(Pointer.nativeValue(ps));
        return s;
    }

    // Rational Arithmetic

    /**
     * Set this {@code MPQ} to {@code (op1 + op2)}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ addAssign(MPQ op1, MPQ op2) {
        mpq_add(mpqNative, op1.mpqNative, op2.mpqNative);
        return this;
    }

    /**
     * Set this {@code MPQ} to {@code (this + op)}
     *
     * @return this {@code MPQ}
     */
    public MPQ addAssign(MPQ op) {
        return addAssign(this, op);
    }

    /**
     * Return an {@code MPQ} whose value is {@code (this + op)}.
     */
    public MPQ add(MPQ op) {
        return new MPQ().addAssign(this, op);
    }

    /**
     * Set this {@code MPQ} to {@code (op1 - op2)}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ subAssign(MPQ op1, MPQ op2) {
        mpq_sub(mpqNative, op1.mpqNative, op2.mpqNative);
        return this;
    }

    /**
     * Set this {@code MPQ} to {@code (this - op)}
     *
     * @return this {@code MPQ}
     */
    public MPQ subAssign(MPQ op) {
        return subAssign(this, op);
    }

    /**
     * Return an {@code MPQ} whose value is {@code (this - op)}.
     */
    public MPQ sub(MPQ op) {
        return new MPQ().subAssign(this, op);
    }

    /**
     * Set this {@code MPQ} to {@code (op1 * op2)}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ mulAssign(MPQ op1, MPQ op2) {
        mpq_mul(mpqNative, op1.mpqNative, op2.mpqNative);
        return this;
    }

    /**
     * Set this {@code MPQ} to {@code (this * op)}
     *
     * @return this {@code MPQ}
     */
    public MPQ mulAssign(MPQ op) {
        return mulAssign(this, op);
    }

    /**
     * Return an {@code MPQ} whose value is {@code (this * op)}.
     */
    public MPQ mul(MPQ op) {
        return new MPQ().mulAssign(this, op);
    }

    /**
     * Set this {@code MPQ} to {@code (op1 / op2)}.
     *
     * @throws ArithmeticException if {@code op2} is zero.
     *
     * @return this {@code MPQ}.
     */
    public MPQ divAssign(MPQ op1, MPQ op2) {
        if (op2.isZero())
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        mpq_div(mpqNative, op1.mpqNative, op2.mpqNative);
        return this;
    }

    /**
     * Set this {@code MPQ} to {@code (this / op)}
     *
     * @throws ArithmeticException if {@code op} is zero.
     *
     * @return this {@code MPQ}
     */
    public MPQ divAssign(MPQ op) {
        return divAssign(this, op);
    }

    /**
     * Return an {@code MPQ} whose value is {@code (this / op)}.
     *
     * @throws ArithmeticException if {@code op} is zero.
     */
    public MPQ div(MPQ op) {
        return new MPQ().divAssign(this, op);
    }

    /**
     * Set this {@code MPQ} to <code>(op * 2<sup>b</sup>)</code>.
     *
     * @return this {@code MPQ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPQ mul2ExpAssign(MPQ op, long b) {
        mpq_mul_2exp(mpqNative, op.mpqNative, new MpBitcntT(b));
        return this;
    }

    /**
     * Set this {@code MPQ} to <code>(this * 2<sup>b</sup>)</code>.
     *
     * @return this {@code MPQ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPQ mul2ExpAssign(long b) {
        return mul2ExpAssign(this, b);
    }

    /**
     * Return an {@code MPQ} whose value is <code>(this * 2<sup>b</sup>)</code>.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPQ mul2Exp(long b) {
        return new MPQ().mul2ExpAssign(this, b);
    }

    /**
     * Set this {@code MPQ} to <code>(op / 2<sup>b</sup>)</code>.
     *
     * @return this {@code MPQ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPQ div2ExpAssign(MPQ op, long b) {
        mpq_div_2exp(mpqNative, op.mpqNative, new MpBitcntT(b));
        return this;
    }

    /**
     * Set this {@code MPQ} to <code>(this / 2<sup>b</sup>)</code>.
     *
     * @return this {@code MPQ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPQ div2ExpAssign(long b) {
        return div2ExpAssign(this, b);
    }

    /**
     * Return an {@code MPQ} whose value is <code>(this / 2<sup>b</sup>)</code>.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPQ div2Exp(long b) {
        return new MPQ().div2ExpAssign(this, b);
    }

    /**
     * Set this {@code MPQ} to {@code (- op)}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ negAssign(MPQ op) {
        mpq_neg(mpqNative, op.mpqNative);
        return this;
    }

    /**
     * Set this {@code MPQ} to its opposite.
     *
     * @return this {@code MPQ}.
     */
    public MPQ negAssign() {
        return negAssign(this);
    }

    /**
     * Return an {@code MPQ} whose value is the quotient of {@code (- this)}.
     */
    public MPQ neg() {
        return new MPQ().negAssign(this);
    }

    /**
     * Set this {@code MPQ} to the absolute value of {@code op}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ absAssign(MPQ op) {
        mpq_abs(mpqNative, op.mpqNative);
        return this;
    }

    /**
     * Set this {@code MPQ} to its absolute value.
     *
     * @return this {@code MPQ}.
     */
    public MPQ absAssign() {
        return absAssign(this);
    }

    /**
     * Return an {@code MPQ} whose value is the absolute value of {@code this}.
     */
    public MPQ abs() {
        return new MPQ().absAssign(this);
    }

    /**
     * Set this {@code MPQ} to {@code 1/op}.
     *
     * @throws ArithmeticException if {@code op} is zero.
     *
     * @return this {@code MPQ}.
     */
    public MPQ invAssign(MPQ op) {
        if (op.isZero())
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        mpq_inv(mpqNative, op.mpqNative);
        return this;
    }

    /**
     * Set this {@code MPQ} to its inverse.
     *
     * @throws ArithmeticException if {@code op} is zero.
     *
     * @return this {@code MPQ}.
     */
    public MPQ invAssign() {
        return invAssign(this);
    }

    /**
     * Return an {@code MPQ} whose value is {@code (1/this)}.
     *
     * @throws ArithmeticException if {@code op} is zero.
     */
    public MPQ inv() {
        return new MPQ().invAssign(this);
    }

    // Comparison Functions

    /**
     * Compare {@code this} with {@code op}. Return a positive value if
     * {@code (this > op)}, zero if {@code (this = op)}, or a negative value if
     * {@code (this < op)}.
     */
    public int cmp(MPQ op) {
        return mpq_cmp(mpqNative, op.mpqNative);
    }

    /**
     * Compare {@code this} with {@code op}. Return a positive value if
     * {@code (this > op)}, zero if {@code this = op}, or a negative value if
     * {@code this < op}.
     */
    public int cmp(MPZ op) {
        return mpq_cmp_z(mpqNative, op.getNative());
    }

    /**
     * Compare {@code this} with {@code (num/dem)}. Return a positive value if
     * {@code (this > num/dem)}, zero if {@code (this = num/dem)}, or a negative
     * value if {@code (this < num/dem)}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public int cmp(long num, long den) {
        return mpq_cmp_si(mpqNative, new NativeLong(num), new NativeUnsignedLong(den));
    }

    /**
     * Compare {@code this} with {@code (num/dem)}. Return a positive value if
     * {@code (this > num/dem)}, zero if {@code (this = num/dem)}, or a negative
     * value if {@code (this < num/dem)}.
     *
     * @apiNote {@code num} and {@code den} should be treated as an unsigned long.
     */
    public int cmpUi(long num, long den) {
        return mpq_cmp_ui(mpqNative, new NativeUnsignedLong(num), new NativeUnsignedLong(den));
    }

    /**
     * Return {@code +1} if {@code (this > 0)}, {@code 0} if {@code (this = 0)} and
     * {@code -1} if {@code (this < 0)}.
     */
    public int sgn() {
        return mpq_sgn(mpqNative);
    }

    /**
     * Return true if {@code this} is equal to {@code op}, false otherwise.
     * Although {@code cmp} can be used for the same purpose, this method should be
     * faster.
     */
    public boolean equal(MPQ op) {
        return mpq_equal(mpqNative, op.mpqNative);
    }

    // Applying Integer Functions to Rationals

    /**
     * Return the numerator of {@code this}.
     */
    public MPZ getNum() {
        MPZ res = new MPZ();
        mpq_get_num(res.getNative(), mpqNative);
        return res;
    }

    /**
     * Return the denominator of {@code this}.
     */
    public MPZ getDen() {
        MPZ res = new MPZ();
        mpq_get_den(res.getNative(), mpqNative);
        return res;
    }

    /**
     * Set the numerator of {@code this} to the value {@code num}.
     */
    public MPQ setNum(MPZ num) {
        mpq_set_num(mpqNative, num.getNative());
        mpq_canonicalize(mpqNative);
        return this;
    }

    /**
     * Set the denominator of {@code this} to the value {@code den}.
     */
    public MPQ setDen(MPZ den) {
        mpq_set_den(mpqNative, den.getNative());
        mpq_canonicalize(mpqNative);
        return this;
    }

    /**
     * Return true if and only if {@code this} MPQ is zero.
     */
    public boolean isZero() {
        return mpq_cmp(mpqNative, zero.mpqNative) == 0;
    }

    // Constructors

    /**
     * Build an {@code MPQ} whose value is zero.
     */
    public MPQ() {
        mpqNative = new MpqT();
        mpq_init(mpqNative);
        GMP.cleaner.register(this, new MPQCleaner(mpqNative));
    }

    /**
     * Build an {@code MPQ} whose value is {@code op}.
     */
    public MPQ(MPQ op) {
        this();
        set(op);
    }

    /**
     * Build an {@code MPQ} whose value is {@code op}.
     */
    public MPQ(MPZ op) {
        this();
        set(op);
    }

    /**
     * Build an {@code MPQ} whose value is {@code (num/dem)}.
     *
     * @apiNote {@code den} should be treated as an unsigned long.
     *
     */
    public MPQ(long num, long dem) {
        this();
        set(num, dem);
    }

    /**
     * Build an {@code MPQ} whose value is {@code op}.
     */
    public MPQ(long num) {
        this();
        set(num, 1);
    }

    /**
     * Build an {@code MPQ} whose value is {@code op}. There is no rounding, this
     * conversion is exact.
     *
     * @throws ArithmeticException if {@code op} is not a finite number.
     */
    public MPQ(double op) {
        this();
        set(op);
    }

    /**
     * Build an {@code MPQ} whose value is {@code op}. There is no rounding, this
     * conversion is exact.
     */
    public MPQ(MPF op) {
        this();
        set(op);
    }

    /**
     * Build an {@code MPQ} whose value is the number represented by the string
     * {@code str} in the specified {@code base}. See the GMP function
     * <a href="https://gmplib.org/manual/Initializing-Rationals" target=
     * "_blank">{@code mpq_set_str}</a>.
     *
     * @throws NumberFormatException if either {@code base} is not valid or
     *                               {@code str} is not a valid string in the
     *                               specified {@code base}.
     *
     */
    public MPQ(String str, int base) {
        this();
        if (set(str, base) == -1)
            throw new NumberFormatException(GMP.MSG_INVALID_STRING_CONVERSION);

    }

    /**
     * Build an {@code MPQ} whose value is the number represented by the string
     * {@code str} in decimal base. See the GMP function
     * <a href="https://gmplib.org/manual/Initializing-Rationals" target=
     * "_blank">{@code mpq_set_str}</a>.
     *
     * @throws NumberFormatException if {@code str} is not a valid number
     *                               representation in decimal base.
     */
    public MPQ(String str) {
        this();
        if (set(str, 10) == -1)
            throw new NumberFormatException(GMP.MSG_INVALID_STRING_CONVERSION);
    }

    // setValue functions

    /**
     * Set this {@code MPQ} to {@code op}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ setValue(MPQ op) {
        return set(op);
    }

    /**
     * Set this {@code MPQ} to {@code op}.
     *
     * @return this {@code MPQ}.
     */
    public MPQ setValue(long op) {
        return set(op);
    }

    /**
     * Set this {@code MPQ} to op {@code op}. There is no rounding, this conversion
     * is exact.
     *
     * @throws ArithmeticException if {@code op} is not a finite number. In this
     *                             case, {@code this} is not altered.
     *
     * @return this {@code MPQ}.
     */
    public MPQ setValue(double op) {
        return set(op);
    }

    /**
     * Set this {@code MPQ} to {@code op}. There is no rounding, this conversion is
     * exact.
     *
     * @return this {@code MPQ}.
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
     * @throws NumberFormatException if either {@code base} is not valid or
     *                               {@code str} is not a valid number
     *                               representation in the specified base. In this
     *                               case, {@code this} is not altered.
     *
     * @return this {@code MPQ}.
     */
    public MPQ setValue(String str, int base) {
        var result = set(str, base);
        if (result == -1)
            throw new NumberFormatException(GMP.MSG_INVALID_STRING_CONVERSION);
        return this;
    }

    /**
     * Set this {@code MPQ} to the value represented by the string {@code str} in
     * decimal base. See the GMP function
     * <a href="https://gmplib.org/manual/Initializing-Rationals" target="
     * _blank">{@code mpq_set_str}</a>.
     *
     * @throws NumberFormatException if {@code str} is not a valid number
     *                               representation in decimal base.
     *
     * @return this {@code MPQ}.
     */
    public MPQ setValue(String str) {
        var result = set(str, 10);
        if (result == -1)
            throw new NumberFormatException(GMP.MSG_INVALID_STRING_CONVERSION);
        return this;
    }

    // Interface methods

    /**
     * Compare this {@code MPQ} with {@code op}. Return a positive value if
     * {@code (this > op)}, zero if {@code (this = op)}, or a negative value if
     * {@code (this < op)}. This order is compatible with equality.
     */
    @Override
    public int compareTo(MPQ op) {
        return mpq_cmp(mpqNative, op.mpqNative);
    }

    /**
     * Compare this {@code MPQ} with the object {@code op} for equality. It returns
     * {@code true} if and only if {@code op} is an {@code MPQ} with the same value
     * of {@code this}.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof MPQ) {
            var q = (MPQ) obj;
            return mpq_equal(mpqNative, q.mpqNative);
        }
        return false;
    }

    /***
     * Return a hash code value for this {@code MPQ}.
     */
    @Override
    public int hashCode() {
        var num = mpz_get_si(mpq_numref(mpqNative)).intValue();
        var den = mpz_get_si(mpq_numref(mpqNative)).intValue();
        return num ^ den;
    }

    /**
     * Convert this {@code MPQ} to an int, truncating if necessary.
     *
     * @implNote Return the result of {@link doubleValue} cast to an {@code int}.
     */
    public int intValue() {
        return (int) getD();
    }

    /**
     * Convert this {@code MPQ} to an long, truncating if necessary.
     *
     * @implNote Return the result of {@link doubleValue} cast to a {@code long}.
     */
    public long longValue() {
        return (long) getD();
    }

    /**
     * Convert this {@code MPQ} to a double, truncating if necessary. If the
     * exponent from the conversion is too big or too small to fit a double then the
     * result is system dependent. For too big an infinity is returned when
     * available. For too small 0.0 is normally returned. Hardware overflow,
     * underflow and denorm traps may or may not occur.
     */
    public double doubleValue() {
        return getD();
    }

    /**
     * Convert this {@code MPQ} to a float, truncating if necessary.
     *
     * @implNote Return the result of {@link doubleValue} cast to a {@code float}.
     */
    public float floatValue() {
        return (float) getD();
    }

    /**
     * Convert this {@code MPQ} to its string representation in the specified
     * {@code base}, or {@code null} if the base is not valid. See the GMP function
     * <a href="https://gmplib.org/manual/Initializing-Rationals" target=
     * "_blank">{@code mpq_get_str}</a>.
     *
     * @throws IllegalArgumentException if the base is not valid.
     */
    public String toString(int base) {
        var s = getStr(base);
        if (s == null)
            throw new IllegalArgumentException(GMP.MSG_INVALID_BASE);
        return s;
    }

    /**
     * Convert this {@code MPQ} to its decimal string representation.
     */
    @Override
    public String toString() {
        return getStr(10);
    }

    // Serialization

    private void writeObject(ObjectOutputStream out) throws IOException {
        // writeUTF seems more efficient, but has a limit of 64Kb
        // use base 62 to have a more compact representation
        out.writeObject(toString(62));
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        mpqNative = new MpqT();
        mpq_init(mpqNative);
        mpq_set_str(mpqNative, (String) in.readObject(), 62);
        GMP.cleaner.register(this, new MPQCleaner(mpqNative));
    }

    @SuppressWarnings("unused")
    private void readObjectNoData() throws ObjectStreamException {
        mpqNative = new MpqT();
        mpq_init(mpqNative);
        GMP.cleaner.register(this, new MPQCleaner(mpqNative));
    }

}
