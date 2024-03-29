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
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Optional;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.NativeLongByReference;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import it.unich.jgmp.nativelib.MpBitcntT;
import it.unich.jgmp.nativelib.MpSizeT;
import it.unich.jgmp.nativelib.MpzT;
import it.unich.jgmp.nativelib.NativeUnsignedLong;
import it.unich.jgmp.nativelib.SizeT;
import it.unich.jgmp.nativelib.SizeTByReference;

/**
 * Multi-precision integer number. This class encapsulates the {@code mpz_t}
 * data type, see the
 * <a href="https://gmplib.org/manual/Integer-Functions" target="_blank">Integer
 * Functions</a> page of the GMP manual. In determining the names and prototypes
 * of the methods of the {@code MPZ} class, we adopt the rules described in the
 * documentation of the {@link it.unich.jgmp} package, enriched with the
 * following ones:
 * <ul>
 * <li>the functions in the categories <em>I/O of Integers</em>, <em>Integer
 * Import and Export</em> and <em>Special Functions</em> are not exposed by the
 * {@code MPZ} class.
 * </ul>
 */
public class MPZ extends Number implements Comparable<MPZ> {

    /**
     * Version for serializability.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The pointer to the native {@code mpz_t} object.
     */
    private transient MpzT mpzNative;

    /**
     * Result enumeration for the {@link isProbabPrime} method.
     */
    public static enum PrimalityStatus {
        /**
         * The tested number is definitely non-prime.
         */
        NON_PRIME,
        /**
         * The tested number is probably prime.
         */
        PROBABLY_PRIME,
        /**
         * The tested number is definitely prime.
         */
        PRIME
    }

    /**
     * Cleaning action for the {@code MPZ} class.
     */
    private static class MPZCleaner implements Runnable {
        private Pointer mpzPointer;

        MPZCleaner(MpzT mpzNative) {
            mpzPointer = mpzNative.getPointer();
        }

        @Override
        public void run() {
            __gmpz_clear(mpzPointer);
        }
    }

    /**
     * A private constructor which build an {@code MPZ} starting from a pointer to
     * its native data object. The native object needs to be already initialized.
     */
    private MPZ(MpzT pointer) {
        mpzNative = pointer;
        GMP.cleaner.register(mpzNative, new MPZCleaner(pointer));
    }

    /**
     * Return the native pointer to the GMP object.
     */
    public MpzT getNative() {
        return mpzNative;
    }

    // Initializing Integers

    /**
     * Return an {@code MPZ} whose value is zero.
     */
    static public MPZ init() {
        var mpzNative = new MpzT();
        mpz_init(mpzNative);
        return new MPZ(mpzNative);
    }

    /**
     * Return an {@code MPZ} whose value is zero, with pre-allocated space for
     * {@code n}-bit numbers.
     *
     * Calling this method is never necessary; reallocation is handled automatically
     * by GMP when needed. See the GMP function
     * <a href= "https://gmplib.org/manual/Initializing-Integers" target=
     * "_blank">{@code mpz_init2}</a>.
     *
     * @apiNote {@code n} should be treated as an unsigned long
     */
    static public MPZ init2(long n) {
        var mpzNative = new MpzT();
        mpz_init2(mpzNative, new MpBitcntT(n));
        return new MPZ(mpzNative);
    }

    /**
     * Changes the space allocated for this number to {@code n} bits. The value is
     * preserved if it fits, otherwise it is set to 0.
     *
     * Calling this function is never necessary; reallocation is handled
     * automatically by GMP when needed. This function can be used to increase the
     * space for a variable in order to avoid repeated automatic reallocations, or
     * to decrease it to give memory back to the heap.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public MPZ realloc2(long n) {
        mpz_realloc2(mpzNative, new MpBitcntT(n));
        return this;
    }

    // Assigning Integers

    /**
     * Set this {@code MPZ} to {@code op}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ set(MPZ op) {
        mpz_set(mpzNative, op.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to {@code op}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ set(long op) {
        mpz_set_si(mpzNative, new NativeLong(op));
        return this;
    }

    /**
     * Set this {@code MPZ} to {@code op}.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPZ setUi(long op) {
        mpz_set_ui(mpzNative, new NativeUnsignedLong(op));
        return this;
    }

    /**
     * Set this {@code MPZ} to the truncation of {@code op}.
     *
     * @throws ArithmeticException if {@code op} is not a finite number. In this
     *                             case, {@code this} is not altered.
     * @return this {@code MPZ}.
     */
    public MPZ set(double op) {
        if (!Double.isFinite(op))
            throw new ArithmeticException(GMP.MSG_FINITE_DOUBLE_REQUIRED);
        mpz_set_d(mpzNative, op);
        return this;
    }

    /**
     * Set this {@code MPZ} to the truncation of {@code op}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ set(MPQ op) {
        mpz_set_q(mpzNative, op.getNative());
        return this;
    }

    /**
     * Set this {@code MPZ} to the truncation of {@code op}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ set(MPF op) {
        mpz_set_f(mpzNative, op.getNative());
        return this;
    }

    /**
     * Set this {@code MPZ} to the number represented by the string {@code str} in
     * the specified {@code base}. See the GMP function
     * <a href="https://gmplib.org/manual/Assigning-Integers" target="
     * _blank">{@code mpz_set_str}</a>.
     *
     * @return 0 if the operation succeeded, -1 otherwise. In the latter case,
     *         {@code this} is not altered.
     */
    public int set(String str, int base) {
        return mpz_set_str(mpzNative, str, base);
    }

    static void set(MpzT mpzNative, BigInteger op) {
        ByteBuffer buffer = ByteBuffer.wrap(op.abs().toByteArray());
        mpz_import(mpzNative, new SizeT(buffer.capacity()), 1, new SizeT(1), 0, new SizeT(0), buffer);
        if (op.signum() < 0)
            mpz_neg(mpzNative, mpzNative);
    }

    /**
     * Sets this {@code MPZ} to {@code op}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ set(BigInteger op) {
        set(mpzNative, op);
        return this;
    }

    /**
     * Swap the value of this {@code MPZ} with the value of {@code op}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ swap(MPZ op) {
        mpz_swap(mpzNative, op.mpzNative);
        return this;
    }

    // Simultaneous Integer Init & Assign

    /**
     * Return an {@code MPZ} whose value is {@code op}.
     */
    public static MPZ initSet(MPZ op) {
        var mpzNative = new MpzT();
        mpz_init_set(mpzNative, op.mpzNative);
        return new MPZ(mpzNative);
    }

    /**
     * Return an {@code MPZ} whose value is {@code op}.
     */
    public static MPZ initSet(long op) {
        var mpzNative = new MpzT();
        mpz_init_set_si(mpzNative, new NativeLong(op));
        return new MPZ(mpzNative);
    }

    /**
     * Return an {@code MPZ} whose value is {@code op}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public static MPZ initSetUi(long op) {
        var mpzNative = new MpzT();
        mpz_init_set_ui(mpzNative, new NativeUnsignedLong(op));
        return new MPZ(mpzNative);
    }

    /**
     * Return an {@code MPZ} whose value is the truncation of {@code op}.
     *
     * @throws ArithmeticException if {@code op} is not a finite number.
     */
    public static MPZ initSet(double op) {
        var mpzNative = new MpzT();
        if (!Double.isFinite(op))
            throw new ArithmeticException(GMP.MSG_FINITE_DOUBLE_REQUIRED);
        mpz_init_set_d(mpzNative, op);
        return new MPZ(mpzNative);
    }

    /**
     * Return an {@code MPZ} whose value is the number represented by the string
     * {@code str} in the specified {@code base}. See the GMP function
     * <a href="https://gmplib.org/manual/Simultaneous-Integer-Init-_0026-Assign"
     * target="_blank">{@code mpz_init_set_str}</a>.
     *
     * @return a pair whose first component is {@code 0} if the operation succeeded,
     *         and {@code -1} if either {@code base} is not valid, or {@code str} is
     *         not a valid numeric representation in the specified base. The second
     *         component of the pair is the number represented in {@code str} if the
     *         operation succeeded, {@code 0} otherwise.
     */

    public static Pair<Integer, MPZ> initSet(String str, int base) {
        var mpzNative = new MpzT();
        var result = mpz_init_set_str(mpzNative, str, base);
        return new Pair<>(result, new MPZ(mpzNative));
    }

    // Converting Integers

    /**
     * Convert this {@code MPZ} to an unsigned long. If this number is too big to
     * fit a native unsigned long, then just the least significant bits that do fit
     * are returned. The sign of this number is ignored, only the absolute value is
     * used.
     *
     * @apiNote the return value should be treated as an unsigned long.
     */
    public long getUi() {
        return mpz_get_ui(mpzNative).longValue();
    }

    /**
     * Convert this {@code MPZ} to a signed long. If this number is too big to fit a
     * native signed long, return the least significant part, preserving the sign.
     */
    public long getSi() {
        return mpz_get_si(mpzNative).longValue();
    }

    /**
     * Convert this {@code MPZ} to a double, truncating if necessary. If the
     * exponent from the conversion is too big, the result is system dependent. An
     * infinity is returned where available. A hardware overflow trap may or may not
     * occur.
     */
    public double getD() {
        return mpz_get_d(mpzNative);
    }

    /**
     * Convert this {@code MPZ} to a pair made of mantissa and exponent, truncating
     * if necessary. See the GMP function
     * <a href="https://gmplib.org/manual/Converting-Integers" target=
     * "_blank">{@code mpz_get_d_2exp}</a>.
     */
    public Pair<Double, Long> getD2Exp() {
        var pexp = new NativeLongByReference();
        var d = mpz_get_d_2exp(pexp, mpzNative);
        return new Pair<>(d, pexp.getValue().longValue());
    }

    /**
     * Return the String representation of this {@code MPZ} in the specified
     * {@code base}, or {@code null} if the base is not valid. See the GMP function
     * <a href="https://gmplib.org/manual/Converting-Integers" target=
     * "_blank">{@code mpz_get_str}</a>.
     */
    public String getStr(int base) {
        var ps = mpz_get_str(null, base, mpzNative);
        if (ps == null)
            return null;
        var s = ps.getString(0);
        deallocate(ps, new SizeT(s.length() + 1));
        return s;
    }

    /**
     * Converts this {@code MPZ} to BigInteger.
     */
    public BigInteger getBigInteger() {
        ByteBuffer buffer = bufferExport(1, 1, 0, 0);
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new BigInteger(this.sgn(), bytes);
    }

    // Integer Arithmetic

    /**
     * Set this {@code MPZ} to {@code (op1 + op2)}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ addAssign(MPZ op1, MPZ op2) {
        mpz_add(mpzNative, op1.mpzNative, op2.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to {@code (this + op)}
     *
     * @return this {@code MPZ}
     */
    public MPZ addAssign(MPZ op) {
        return addAssign(this, op);
    }

    /**
     * Return an {@code MPZ} whose value is {@code (this + op)}.
     */
    public MPZ add(MPZ op) {
        return new MPZ().addAssign(this, op);
    }

    /**
     * Set this {@code MPZ} to {@code (op1 + op2)}.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code op2} should be treated as an unsigned long.
     */
    public MPZ addUiAssign(MPZ op1, long op2) {
        mpz_add_ui(mpzNative, op1.mpzNative, new NativeUnsignedLong(op2));
        return this;
    }

    /**
     * Set this {@code MPZ} to {@code (this + op)}
     *
     * @return this {@code MPZ}
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPZ addUiAssign(long op) {
        return addUiAssign(this, op);
    }

    /**
     * Return an {@code MPZ} whose value is {@code (this + op)}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPZ addUi(long op) {
        return new MPZ().addUiAssign(this, op);
    }

    /**
     * Set this {@code MPZ} to {@code (op1 - op2)}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ subAssign(MPZ op1, MPZ op2) {
        mpz_sub(mpzNative, op1.mpzNative, op2.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to {@code (this - op)}
     *
     * @return this {@code MPZ}
     */
    public MPZ subAssign(MPZ op) {
        return subAssign(this, op);
    }

    /**
     * Return an {@code MPZ} whose value is {@code (this - op)}.
     */
    public MPZ sub(MPZ op) {
        return new MPZ().subAssign(this, op);
    }

    /**
     * Set this {@code MPZ} to {@code (op1 - op2)}.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code op2} should be treated as an unsigned long.
     */
    public MPZ subUiAssign(MPZ op1, long op2) {
        mpz_sub_ui(mpzNative, op1.mpzNative, new NativeUnsignedLong(op2));
        return this;
    }

    /**
     * Set this {@code MPZ} to {@code (this - op)}
     *
     * @return this {@code MPZ}
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPZ subUiAssign(long op) {
        return subUiAssign(this, op);
    }

    /**
     * Return an {@code MPZ} whose value is {@code (this - op)}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPZ subUi(long op) {
        return new MPZ().subUiAssign(this, op);
    }

    /**
     * Set this {@code MPZ} to {@code (op1 - op2)}.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code op1} should be treated as an unsigned long.
     */
    public MPZ uiSubAssign(long op1, MPZ op2) {
        mpz_ui_sub(mpzNative, new NativeUnsignedLong(op1), op2.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to {@code (op - this)}
     *
     * @return this {@code MPZ}
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPZ uiSubAssign(long op) {
        return uiSubAssign(op, this);
    }

    /**
     * Return an {@code MPZ} whose value is {@code (op - this)}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPZ uiSub(long op) {
        return new MPZ().uiSubAssign(op, this);
    }

    /**
     * Set this {@code MPZ} to {@code (op1 * op2)}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ mulAssign(MPZ op1, MPZ op2) {
        mpz_mul(mpzNative, op1.mpzNative, op2.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to {@code (this * op)}
     *
     * @return this {@code MPZ}
     */
    public MPZ mulAssign(MPZ op) {
        return mulAssign(this, op);
    }

    /**
     * Return an {@code MPZ} whose value is {@code (this * op)}.
     */
    public MPZ mul(MPZ op) {
        return new MPZ().mulAssign(this, op);
    }

    /**
     * Set this {@code MPZ} to {@code (op1 * op2)}.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code op2} should be treated as an unsigned long.
     */
    public MPZ mulUiAssign(MPZ op1, long op2) {
        mpz_mul_ui(mpzNative, op1.mpzNative, new NativeUnsignedLong(op2));
        return this;
    }

    /**
     * Set this {@code MPZ} to {@code (this * op)}
     *
     * @return this {@code MPZ}
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPZ mulUiAssign(long op) {
        return mulUiAssign(this, op);
    }

    /**
     * Return an {@code MPZ} whose value is {@code (this * op)}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPZ mulUi(long op) {
        return new MPZ().mulUiAssign(this, op);
    }

    /**
     * Set this {@code MPZ} to {@code (op1 * op2)}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ mulAssign(MPZ op1, long op2) {
        mpz_mul_si(mpzNative, op1.mpzNative, new NativeLong(op2));
        return this;
    }

    /**
     * Set this {@code MPZ} to {@code (this * op)}
     *
     * @return this {@code MPZ}
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPZ mulAssign(long op) {
        return mulAssign(this, op);
    }

    /**
     * Return an {@code MPZ} whose value is {@code (this * op)}.
     */
    public MPZ mul(long op) {
        return new MPZ().mulAssign(this, op);
    }

    /**
     * Add {@code (op1 * op2)} to this {@code MPZ}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ addmulAssign(MPZ op1, MPZ op2) {
        mpz_addmul(mpzNative, op1.mpzNative, op2.mpzNative);
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is {@code (this + op1 * op2)}.
     */
    public MPZ addmul(MPZ op1, MPZ op2) {
        return new MPZ(this).addmulAssign(op1, op2);
    }

    /**
     * Add {@code (op1 * op2)} to this {@code MPZ}.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code op2} should be treated as an unsigned long.
     */
    public MPZ addmulUiAssign(MPZ op1, long op2) {
        mpz_addmul_ui(mpzNative, op1.mpzNative, new NativeUnsignedLong(op2));
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is {@code (this + op1 * op2)}.
     *
     * @apiNote {@code op2} should be treated as an unsigned long.
     */
    public MPZ addmulUi(MPZ op1, long op2) {
        return new MPZ(this).addmulUiAssign(op1, op2);
    }

    /**
     * Subtract {@code (op1 * op2)} to this {@code MPZ}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ submulAssign(MPZ op1, MPZ op2) {
        mpz_submul(mpzNative, op1.mpzNative, op2.mpzNative);
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is {@code (this - op1 * op2)}.
     */
    public MPZ submul(MPZ op1, MPZ op2) {
        return new MPZ(this).submulAssign(op1, op2);
    }

    /**
     * Subtract {@code (op1 * op2)} to this {@code MPZ}.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code op2} should be treated as an unsigned long.
     */
    public MPZ submulUiAssign(MPZ op1, long op2) {
        mpz_submul_ui(mpzNative, op1.mpzNative, new NativeUnsignedLong(op2));
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is {@code (this - op1 * op2)}.
     *
     * @apiNote {@code op2} should be treated as an unsigned long.
     */
    public MPZ submulUi(MPZ op1, long op2) {
        return new MPZ(this).submulUiAssign(op1, op2);
    }

    /**
     * Set this {@code MPZ} to <code>(op * 2<sup>b</sup>)</code>.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ mul2ExpAssign(MPZ op, long b) {
        mpz_mul_2exp(mpzNative, op.mpzNative, new MpBitcntT(b));
        return this;
    }

    /**
     * Set this {@code MPZ} to <code>(this * 2<sup>b</sup>)</code>.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ mul2ExpAssign(long b) {
        return mul2ExpAssign(this, b);
    }

    /**
     * Return an {@code MPZ} whose value is <code>(this * 2<sup>b</sup>)</code>.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ mul2Exp(long b) {
        return new MPZ().mul2ExpAssign(this, b);
    }

    /**
     * Set this {@code MPZ} to {@code (- op)}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ negAssign(MPZ op) {
        mpz_neg(mpzNative, op.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to its opposite.
     *
     * @return this {@code MPZ}.
     */
    public MPZ negAssign() {
        return negAssign(this);
    }

    /**
     * Return an {@code MPZ} whose value is the quotient of {@code (- this)}.
     */
    public MPZ neg() {
        return new MPZ().negAssign(this);
    }

    /**
     * Set this {@code MPZ} to the absolute value of {@code op}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ absAssign(MPZ op) {
        mpz_abs(mpzNative, op.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to its absolute value.
     *
     * @return this {@code MPZ}.
     */
    public MPZ absAssign() {
        return absAssign(this);
    }

    /**
     * Return an {@code MPZ} whose value is the absolute value of {@code this}.
     */
    public MPZ abs() {
        return new MPZ().absAssign(this);
    }

    // Integer Division

    /**
     * Set this {@code MPZ} to the quotient of the integer division {@code (n / d)},
     * rounded towards +∞.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @return this {@code MPZ}.
     */
    public MPZ cdivqAssign(MPZ n, MPZ d) {
        if (d.isZero())
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        mpz_cdiv_q(mpzNative, n.mpzNative, d.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to the quotient of the integer division
     * {@code (this / d)}, rounded towards +∞.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @return this {@code MPZ}.
     */
    public MPZ cdivqAssign(MPZ d) {
        return cdivqAssign(this, d);
    }

    /**
     * Set this {@code MPZ} to the remainder of the integer division
     * {@code (n / d)}, rounded towards +∞.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @return this {@code MPZ}.
     */
    public MPZ cdivrAssign(MPZ n, MPZ d) {
        if (d.isZero())
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        mpz_cdiv_r(mpzNative, n.mpzNative, d.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to the remainder of the integer division
     * {@code (this / d)}, rounded towards +∞.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @return this {@code MPZ}.
     */
    public MPZ cdivrAssign(MPZ d) {
        return cdivrAssign(this, d);
    }

    /**
     * Set this {@code MPZ} and {@code r} to the quotient and remainder of the
     * integer division {@code (n / d)}, rounded towards +∞.
     *
     * @throws ArithmeticException      if {@code d} is zero.
     * @throws IllegalArgumentException if {@code this} and {@code r} are the same
     *                                  object.
     *
     * @return this {@code MPZ}.
     */
    public MPZ cdivqrAssign(MPZ r, MPZ n, MPZ d) {
        if (d.isZero())
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        if (mpzNative == r.mpzNative)
            throw new IllegalArgumentException(GMP.MSG_SAME_OBJECT);
        mpz_cdiv_qr(mpzNative, r.mpzNative, n.mpzNative, d.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} and {@code r} to the quotient and remainder of the
     * integer division {@code (this / d)}, rounded towards +∞.
     *
     * @throws ArithmeticException      if {@code d} is zero.
     * @throws IllegalArgumentException if {@code this} and {@code r} are the same
     *                                  object.
     *
     * @return this {@code MPZ}.
     */
    public MPZ cdivqrAssign(MPZ r, MPZ d) {
        return cdivqrAssign(r, this, d);
    }

    /**
     * Set this {@code MPZ} to the quotient of the integer division {@code (n / d)},
     * rounded towards +∞; it also Return the remainder.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @apiNote both {@code d} and the return value should be treated as unsigned
     *          longs.
     */
    public long cdivqUiAssign(MPZ n, long d) {
        if (d == 0l)
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        return mpz_cdiv_q_ui(mpzNative, n.mpzNative, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Set this {@code MPZ} to the quotient of the integer division
     * {@code (this / d)}, rounded towards +∞; it also returns the remainder.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @apiNote both {@code d} and the return value should be treated as unsigned
     *          longs.
     */
    public long cdivqUiAssign(long d) {
        return cdivqUiAssign(this, d);
    }

    /**
     * Set this {@code MPZ} to the remainder of the integer division
     * {@code (n / d)}, rounded towards +∞; it also returns the remainder.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @apiNote both {@code d} and the return value should be treated as unsigned
     *          longs.
     */
    public long cdivrUiAssign(MPZ n, long d) {
        if (d == 0l)
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        return mpz_cdiv_r_ui(mpzNative, n.mpzNative, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Set this {@code MPZ} to the remainder of the integer division
     * {@code (this / d)}, rounded towards +∞; it also returns the remainder.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @apiNote both {@code d} and the return value should be treated as unsigned
     *          longs.
     */
    public long cdivrUiAssign(long d) {
        return cdivrUiAssign(this, d);
    }

    /**
     * Set this {@code MPZ} and {@code r} to the quotient and remainder of the
     * integer division {@code (n / d)}, rounded towards +∞; it also returns the
     * remainder.
     *
     * @throws ArithmeticException      if {@code d} is zero.
     * @throws IllegalArgumentException if {@code this} and {@code r} are the same
     *                                  object.
     *
     * @apiNote both {@code d} and the return value should be treated as unsigned
     *          longs.
     */
    public long cdivqrUiAssign(MPZ r, MPZ n, long d) {
        if (d == 0l)
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        if (mpzNative == r.mpzNative)
            throw new IllegalArgumentException(GMP.MSG_SAME_OBJECT);
        return mpz_cdiv_qr_ui(mpzNative, r.mpzNative, n.mpzNative, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Set this {@code MPZ} and {@code r} to the quotient and remainder of the
     * integer division {@code (this / d)}, rounded towards +∞; it also returns the
     * remainder.
     *
     * @throws ArithmeticException      if {@code d} is zero.
     * @throws IllegalArgumentException if {@code this} and {@code r} are the same
     *                                  object.
     *
     * @apiNote both {@code d} and the return value should be treated as unsigned
     *          longs.
     */
    public long cdivqrUiAssign(MPZ r, long d) {
        return cdivqrUiAssign(r, this, d);
    }

    /**
     * Return the remainder of the integer division {@code (this / d)}, rounded
     * towards +∞.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @apiNote {@code d} should be treated as an unsigned long.
     */
    public long cdivUi(long d) {
        if (d == 0l)
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        return mpz_cdiv_ui(mpzNative, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Set this {@code MPZ} to the quotient of the integer division
     * <code>(n / 2<sup>b</sup>)</code>, rounded toward +∞.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ cdivq2ExpAssign(MPZ n, long b) {
        mpz_cdiv_q_2exp(mpzNative, n.mpzNative, new MpBitcntT(b));
        return this;
    }

    /**
     * Set this {@code MPZ} to the quotient of the integer division
     * <code>(this / 2<sup>b</sup>)</code>, rounded toward +∞.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ cdivq2ExpAssign(long b) {
        return cdivq2ExpAssign(this, b);
    }

    /**
     * Set this {@code MPZ} to the remainder of the integer division
     * <code>(n / 2<sup>b</sup>)</code>, rounded toward +∞.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ cdivr2ExpAssign(MPZ n, long b) {
        mpz_cdiv_r_2exp(mpzNative, n.mpzNative, new MpBitcntT(b));
        return this;
    }

    /**
     * Set this {@code MPZ} to the remainder of the integer division
     * <code>(this / 2<sup>b</sup>)</code>, rounded toward +∞.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ cdivr2ExpAssign(long b) {
        return cdivr2ExpAssign(this, b);
    }

    /**
     * Return an {@code MPZ} whose value is the quotient of the integer division
     * {@code (this / d)}, rounded towards +∞.
     *
     * @throws ArithmeticException if {@code d} is zero.
     */
    public MPZ cdivq(MPZ d) {
        return new MPZ().cdivqAssign(this, d);
    }

    /**
     * Return an {@code MPZ} whose value is the remainder of the integer division
     * {@code (this / d)}, rounded towards +∞.
     *
     * @throws ArithmeticException if {@code d} is zero.
     */
    public MPZ cdivr(MPZ d) {
        return new MPZ().cdivrAssign(this, d);
    }

    /**
     * Return a pair of {@code MPZ}s whose values are the quotient and remainder of
     * the integer division {@code (this / d)}, rounded towards +∞.
     *
     * @throws ArithmeticException if {@code d} is zero.
     */
    public Pair<MPZ, MPZ> cdivqr(MPZ d) {
        MPZ q = new MPZ(), r = new MPZ();
        q.cdivqrAssign(r, this, d);
        return new Pair<>(q, r);
    }

    /**
     * Return an {@code MPZ} whose value is the quotient of the integer division
     * <code>(this / 2<sup>b</sup>)</code>, rounded towards +∞.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ cdivq2Exp(long b) {
        return new MPZ().cdivq2ExpAssign(this, b);
    }

    /**
     * Return an {@code MPZ} whose value is the remainder of the integer division
     * <code>(this / 2<sup>b</sup>)</code>, rounded towards +∞.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ cdivr2Exp(long b) {
        return new MPZ().cdivr2ExpAssign(this, b);
    }

    /**
     * Set this {@code MPZ} to the quotient of the integer division {@code (n / d)},
     * rounded towards -∞.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @return this {@code MPZ}.
     */
    public MPZ fdivqAssign(MPZ n, MPZ d) {
        if (d.isZero())
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        mpz_fdiv_q(mpzNative, n.mpzNative, d.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to the quotient of the integer division
     * {@code (this / d)}, rounded towards -∞.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @return this {@code MPZ}.
     */
    public MPZ fdivqAssign(MPZ d) {
        return fdivqAssign(this, d);
    }

    /**
     * Set this {@code MPZ} to the remainder of the integer division
     * {@code (n / d)}, rounded towards -∞.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @return this {@code MPZ}.
     */
    public MPZ fdivrAssign(MPZ n, MPZ d) {
        if (d.isZero())
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        mpz_fdiv_r(mpzNative, n.mpzNative, d.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to the remainder of the integer division
     * {@code (this / d)}, rounded towards -∞.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @return this {@code MPZ}.
     */
    public MPZ fdivrAssign(MPZ d) {
        return fdivrAssign(this, d);
    }

    /**
     * Set this {@code MPZ} and {@code r} to the quotient and remainder of the
     * integer division {@code (n / d)}, rounded towards -∞.
     *
     * @throws ArithmeticException      if {@code d} is zero.
     * @throws IllegalArgumentException if {@code this} and {@code r} are the same
     *                                  object.
     *
     * @return this {@code MPZ}.
     */
    public MPZ fdivqrAssign(MPZ r, MPZ n, MPZ d) {
        if (d.isZero())
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        if (mpzNative == r.mpzNative)
            throw new IllegalArgumentException(GMP.MSG_SAME_OBJECT);
        mpz_fdiv_qr(mpzNative, r.mpzNative, n.mpzNative, d.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} and {@code r} to the quotient and remainder of the
     * integer division {@code (this / d)}, rounded towards -∞.
     *
     * @throws ArithmeticException      if {@code d} is zero.
     * @throws IllegalArgumentException if {@code this} and {@code r} are the same
     *                                  object.
     *
     * @return this {@code MPZ}.
     */
    public MPZ fdivqrAssign(MPZ r, MPZ d) {
        return fdivqrAssign(r, this, d);
    }

    /**
     * Set this {@code MPZ} to the quotient of the integer division {@code (n / d)},
     * rounded towards -∞; it also returns the absolute value of the remainder.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @apiNote {@code d} should be treated as an unsigned long.
     */
    public long fdivqUiAssign(MPZ n, long d) {
        if (d == 0l)
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        return mpz_fdiv_q_ui(mpzNative, n.mpzNative, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Set this {@code MPZ} to the quotient of the integer division
     * {@code (this / d)}, rounded towards -∞; it also returns the absolute value of
     * the remainder.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @apiNote {@code d} should be treated as an unsigned long.
     */
    public long fdivqUiAssign(long d) {
        return fdivqUiAssign(this, d);
    }

    /**
     * Set this {@code MPZ} to the remainder of the integer division
     * {@code (n / d)}, rounded towards -∞; it also returns the absolute value of
     * the remainder.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @apiNote {@code d} should be treated as an unsigned long.
     */
    public long fdivrUiAssign(MPZ n, long d) {
        if (d == 0l)
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        return mpz_fdiv_r_ui(mpzNative, n.mpzNative, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Set this {@code MPZ} to the remainder of the integer division
     * {@code (this / d)}, rounded towards -∞; it also returns the absolute value of
     * the remainder.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @apiNote {@code d} should be treated as an unsigned long.
     */
    public long fdivrUiAssign(long d) {
        return fdivrUiAssign(this, d);
    }

    /**
     * Set this {@code MPZ} and {@code r} to the quotient and remainder of the
     * integer division {@code (n / d)}, rounded towards -∞; it also returns the
     * absolute value of the remainder.
     *
     * @throws ArithmeticException      if {@code d} is zero.
     * @throws IllegalArgumentException if {@code this} and {@code r} are the same
     *                                  object.
     *
     * @apiNote {@code d} should be treated as an unsigned long.
     */
    public long fdivqrUiAssign(MPZ r, MPZ n, long d) {
        if (d == 0l)
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        if (mpzNative == r.mpzNative)
            throw new IllegalArgumentException(GMP.MSG_SAME_OBJECT);
        return mpz_fdiv_qr_ui(mpzNative, r.mpzNative, n.mpzNative, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Set this {@code MPZ} and {@code r} to the quotient and remainder of the
     * integer division {@code (this / d)}, rounded towards -∞; it also returns the
     * absolute value of the remainder.
     *
     * @throws ArithmeticException      if {@code d} is zero.
     * @throws IllegalArgumentException if {@code this} and {@code r} are the same
     *                                  object.
     *
     * @apiNote {@code d} should be treated as an unsigned long.
     */
    public long fdivqrUiAssign(MPZ r, long d) {
        return fdivqrUiAssign(r, this, d);
    }

    /**
     * Return the remainder of the integer division {@code (this / d)}, rounded
     * towards -∞.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @apiNote {@code d} should be treated as an unsigned long.
     */
    public long fdivUi(long d) {
        if (d == 0l)
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        return mpz_fdiv_ui(mpzNative, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Set this {@code MPZ} to the quotient of the integer division
     * <code>(n / 2<sup>b</sup>)</code>, rounded toward -∞.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ fdivq2ExpAssign(MPZ n, long b) {
        mpz_fdiv_q_2exp(mpzNative, n.mpzNative, new NativeUnsignedLong(b));
        return this;
    }

    /**
     * Set this {@code MPZ} to the quotient of the integer division
     * <code>(this / 2<sup>b</sup>)</code>, rounded toward -∞.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ fdivq2ExpAssign(long b) {
        return fdivq2ExpAssign(this, b);
    }

    /**
     * Set this {@code MPZ} to the remainder of the integer division
     * <code>(n / 2<sup>b</sup>)</code>, rounded toward -∞.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ fdivr2ExpAssign(MPZ n, long b) {
        mpz_fdiv_r_2exp(mpzNative, n.mpzNative, new NativeUnsignedLong(b));
        return this;
    }

    /**
     * Set this {@code MPZ} to the remainder of the integer division
     * <code>(this / 2<sup>b</sup>)</code>, rounded toward -∞.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ fdivr2ExpAssign(long b) {
        return fdivr2ExpAssign(this, b);
    }

    /**
     * Return an {@code MPZ} whose value is the quotient of the integer division
     * {@code (this / d)}, rounded towards -∞.
     *
     * @throws ArithmeticException if {@code d} is zero.
     */
    public MPZ fdivq(MPZ d) {
        return new MPZ().fdivqAssign(this, d);
    }

    /**
     * Return an {@code MPZ} whose value is the remainder of the integer division
     * {@code (this / d)}, rounded towards -∞.
     *
     * @throws ArithmeticException if {@code d} is zero.
     */
    public MPZ fdivr(MPZ d) {
        return new MPZ().fdivrAssign(this, d);
    }

    /**
     * Return two {@code MPZ}s whose values are the quotient and remainder of the
     * integer division {@code (this / d)}, rounded towards -∞.
     *
     * @throws ArithmeticException if {@code d} is zero.
     */
    public Pair<MPZ, MPZ> fdivqr(MPZ d) {
        MPZ q = new MPZ(), r = new MPZ();
        q.fdivqrAssign(r, this, d);
        return new Pair<>(q, r);
    }

    /**
     * Return an {@code MPZ} whose value is the quotient of the integer division
     * <code>(this / 2<sup>b</sup>)</code>, rounded towards -∞.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ fdivq2Exp(long b) {
        return new MPZ().fdivq2ExpAssign(this, b);
    }

    /**
     * Return an {@code MPZ} whose value is the remainder of the integer division
     * <code>(this / 2<sup>b</sup>)</code>, rounded towards -∞.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ fdivr2Exp(long b) {
        return new MPZ().fdivr2ExpAssign(this, b);
    }

    /**
     * Set this {@code MPZ} to the quotient of the integer division {@code (n / d)},
     * rounded towards zero.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @return this {@code MPZ}.
     */
    public MPZ tdivqAssign(MPZ n, MPZ d) {
        if (d.isZero())
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        mpz_tdiv_q(mpzNative, n.mpzNative, d.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to the quotient of the integer division
     * {@code (this / d)}, rounded towards zero.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @return this {@code MPZ}.
     */
    public MPZ tdivqAssign(MPZ d) {
        return tdivqAssign(this, d);
    }

    /**
     * Set this {@code MPZ} to the remainder of the integer division
     * {@code (n / d)}, rounded towards zero.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @return this {@code MPZ}.
     */
    public MPZ tdivrAssign(MPZ n, MPZ d) {
        if (d.isZero())
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        mpz_tdiv_r(mpzNative, n.mpzNative, d.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to the remainder of the integer division
     * {@code (this / d)}, rounded towards zero.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @return this {@code MPZ}.
     */
    public MPZ tdivrAssign(MPZ d) {
        return tdivrAssign(this, d);
    }

    /**
     * Set this {@code MPZ} and {@code r} to the quotient and remainder of the
     * integer division {@code (n / d)}, rounded towards zero.
     *
     * @throws ArithmeticException      if {@code d} is zero.
     * @throws IllegalArgumentException if {@code this} and {@code r} are the same
     *                                  object.
     *
     * @return this {@code MPZ}.
     */
    public MPZ tdivqrAssign(MPZ r, MPZ n, MPZ d) {
        if (d.isZero())
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        if (mpzNative == r.mpzNative)
            throw new IllegalArgumentException(GMP.MSG_SAME_OBJECT);
        mpz_tdiv_qr(mpzNative, r.mpzNative, n.mpzNative, d.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} and {@code r} to the quotient and remainder of the
     * integer division {@code (this / d)}, rounded towards zero.
     *
     * @throws ArithmeticException      if {@code d} is zero.
     * @throws IllegalArgumentException if {@code this} and {@code r} are the same
     *                                  object.
     *
     * @return this {@code MPZ}.
     */
    public MPZ tdivqrAssign(MPZ r, MPZ d) {
        return tdivqrAssign(r, this, d);
    }

    /**
     * Set this {@code MPZ} to the quotient of the integer division {@code (n / d)},
     * rounded towards zero; it also returns the absolute value of the remainder.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @apiNote both {@code d} and the returned value should be treateds as unsigned
     *          longs.
     */
    public long tdivqUiAssign(MPZ n, long d) {
        if (d == 0l)
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        return mpz_tdiv_q_ui(mpzNative, n.mpzNative, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Set this {@code MPZ} to the quotient of the integer division
     * {@code (this / d)}, rounded towards zero; it also returns the absolute value
     * of the remainder.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @apiNote both {@code d} and the returned value should be treateds as unsigned
     *          longs.
     */
    public long tdivqUiAssign(long d) {
        return tdivqUiAssign(this, d);
    }

    /**
     * Set this {@code MPZ} to the remainder of the integer division
     * {@code (n / d)}, rounded towards zero; it also returns the absolute value of
     * the remainder.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @apiNote both {@code d} and the returned value should be treated as unsigned
     *          longs.
     */
    public long tdivrUiAssign(MPZ n, long d) {
        if (d == 0l)
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        return mpz_tdiv_r_ui(mpzNative, n.mpzNative, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Set this {@code MPZ} to the remainder of the integer division
     * {@code (this / d)}, rounded towards zero; it also returns the absolute value
     * of the remainder.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @apiNote both {@code d} and the returned value should be treated as unsigned
     *          longs.
     */
    public long tdivrUiAssign(long d) {
        return tdivrUiAssign(this, d);
    }

    /**
     * Set this {@code MPZ} and {@code r} to the quotient and remainder of the
     * integer division {@code (n / d)}, rounded towards zero; it also returns the
     * absolute value of the remainder.
     *
     * @throws ArithmeticException      if {@code d} is zero.
     * @throws IllegalArgumentException if {@code this} and {@code r} are the same
     *                                  object.
     *
     * @apiNote both {@code d} and the returned value should be treated as unsigned
     *          longs.
     */
    public long tdivqrUiAssign(MPZ r, MPZ n, long d) {
        if (d == 0l)
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        if (mpzNative == r.mpzNative)
            throw new IllegalArgumentException(GMP.MSG_SAME_OBJECT);
        return mpz_tdiv_qr_ui(mpzNative, r.mpzNative, n.mpzNative, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Set this {@code MPZ} and {@code r} to the quotient and remainder of the
     * integer division {@code (this / d)}, rounded towards zero; it also returns
     * the absolute value of the remainder.
     *
     * @throws ArithmeticException      if {@code d} is zero.
     * @throws IllegalArgumentException if {@code this} and {@code r} are the same
     *                                  object.
     *
     * @apiNote both {@code d} and the returned value should be treated as unsigned
     *          longs.
     */
    public long tdivqrUiAssign(MPZ r, long d) {
        return tdivqrUiAssign(r, this, d);
    }

    /**
     * Return the remainder of the integer division {@code (this / d)}, rounded
     * towards zero.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @apiNote both {@code d} and the returned value should be treated as unsigned
     *          longs.
     */
    public long tdivUi(long d) {
        if (d == 0l)
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        return mpz_tdiv_ui(mpzNative, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Set this {@code MPZ} to the quotient of the integer division
     * <code>(n / 2<sup>b</sup>)</code>, rounded toward zero.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ tdivq2ExpAssign(MPZ n, long b) {
        mpz_tdiv_q_2exp(mpzNative, n.mpzNative, new NativeUnsignedLong(b));
        return this;
    }

    /**
     * Set this {@code MPZ} to the quotient of the integer division
     * <code>(this / 2<sup>b</sup>)</code>, rounded toward zero.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ tdivq2ExpAssign(long b) {
        return tdivq2ExpAssign(this, b);
    }

    /**
     * Set this {@code MPZ} to the remainder of the integer division
     * <code>(n / 2<sup>b</sup>)</code>, rounded toward zero.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ tdivr2ExpAssign(MPZ n, long b) {
        mpz_tdiv_r_2exp(mpzNative, n.mpzNative, new NativeUnsignedLong(b));
        return this;
    }

    /**
     * Set this {@code MPZ} to the remainder of the integer division
     * <code>(this / 2<sup>b</sup>)</code>, rounded toward zero.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ tdivr2ExpAssign(long b) {
        return tdivr2ExpAssign(this, b);
    }

    /**
     * Return an {@code MPZ} whose value is the quotient of the integer division
     * {@code (this / d)}, rounded towards zero.
     *
     * @throws ArithmeticException if {@code d} is zero.
     */
    public MPZ tdivq(MPZ d) {
        return new MPZ().tdivqAssign(this, d);
    }

    /**
     * Return an {@code MPZ} whose value is the remainder of the integer division
     * {@code (this / d)}, rounded towards zero.
     *
     * @throws ArithmeticException if {@code d} is zero.
     */
    public MPZ tdivr(MPZ d) {
        return new MPZ().tdivrAssign(this, d);
    }

    /**
     * Return two {@code MPZ}s whose values are the quotient and remainder of the
     * integer division {@code (this / d)}, rounded towards zero.
     *
     * @throws ArithmeticException if {@code d} is zero.
     */
    public Pair<MPZ, MPZ> tdivqr(MPZ d) {
        MPZ q = new MPZ(), r = new MPZ();
        q.tdivqrAssign(r, this, d);
        return new Pair<>(q, r);
    }

    /**
     * Return an {@code MPZ} whose value is the quotient of the integer division
     * <code>(this / 2<sup>b</sup>)</code>, rounded towards zero.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ tdivq2Exp(long b) {
        return new MPZ().tdivq2ExpAssign(this, b);
    }

    /**
     * Return an {@code MPZ} whose value is the remainder of the integer division
     * <code>(this / 2<sup>b</sup>)</code>, rounded towards zero.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPZ tdivr2Exp(long b) {
        return new MPZ().tdivr2ExpAssign(this, b);
    }

    /**
     * Set this {@code MPZ} to {@code (n mod d)}. The sign of the divisor is
     * ignored, the result is always non-negative.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @return this {@code MPZ}.
     */
    public MPZ modAssign(MPZ n, MPZ d) {
        if (d.isZero())
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        mpz_mod(mpzNative, n.mpzNative, d.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to {@code (this mod d)}. The sign of the divisor is
     * ignored, the result is always non-negative.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @return this {@code MPZ}.
     */
    public MPZ modAssign(MPZ d) {
        return modAssign(this, d);
    }

    /**
     * Return an {@code MPZ} whose value is {@code (this mod d)}. The sign of the
     * divisor is ignored, the result is always non-negative.
     */
    public MPZ mod(MPZ d) {
        if (d.isZero())
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        return new MPZ().modAssign(this, d);
    }

    /**
     * Set this {@code MPZ} to {@code (n mod d)}; it also returns the result. The
     * sign of the divisor is ignored, the result is always non-negative.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @apiNote both {@code d} and the retuened value should be treated as unsigned
     *          longs.
     */
    public long modUiAssign(MPZ n, long d) {
        if (d == 0l)
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        return mpz_mod_ui(mpzNative, n.mpzNative, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Set this {@code MPZ} to {@code (n mod d)}; it also returns the result. The
     * sign of the divisor is ignored, the result is always non-negative.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @apiNote both {@code d} and the retuened value should be treated as unsigned
     *          longs.
     */
    public long modUiAssign(long d) {
        return modUiAssign(this, d);
    }

    /**
     * Return an {@code MPZ} whose value is {@code (this mod d)}. The sign of the
     * divisor is ignored, the result is always non-negative.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @apiNote both {@code d} and the retuened value should be treated as unsigned
     *          longs.
     */
    public long modUi(long d) {
        return fdivUi(d);
    }

    /**
     * Set this {@code MPZ} to the quotient of {@code (n / d)}. This method produces
     * correct results only when it is known in advance that {@code d} divides
     * {@code n}.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @return this {@code MPZ}.
     */
    public MPZ divexactAssign(MPZ n, MPZ d) {
        if (d.isZero())
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        mpz_divexact(mpzNative, n.mpzNative, d.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to the quotient of {@code (this / d)}. This method
     * produces correct results only when it is known in advance that {@code d}
     * divides {@code this}.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @return this {@code MPZ}.
     */
    public MPZ divexactAssign(MPZ d) {
        return divexactAssign(this, d);
    }

    /**
     * Return an {@code MPZ} whose value is the quotient of {@code (this / d)}. This
     * method produces correct results only when it is known in advance that
     * {@code d} divides {@code this}.
     *
     * @throws ArithmeticException if {@code d} is zero.
     */
    public MPZ divexact(MPZ d) {
        if (d.isZero())
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        return new MPZ().divexactAssign(this, d);
    }

    /**
     * Set this {@code MPZ} to the quotient of {@code (n / d)}. This method produces
     * correct results only when it is known in advance that {@code d} divides
     * {@code n}.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code d} should be treated as an unsigned long.
     */
    public MPZ divexactUiAssign(MPZ n, long d) {
        if (d == 0l)
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        mpz_divexact_ui(mpzNative, n.mpzNative, new NativeUnsignedLong(d));
        return this;
    }

    /**
     * Set this {@code MPZ} to the quotient of {@code (this / d)}. This method
     * produces correct results only when it is known in advance that {@code d}
     * divides {@code this}.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code d} should be treated as an unsigned long.
     */
    public MPZ divexactUiAssign(long d) {
        return divexactUiAssign(this, d);
    }

    /**
     * Return an {@code MPZ} whose value is the quotient of {@code (this / d)}. This
     * method produces correct results only when it is known in advance that
     * {@code d} divides {@code this}.
     *
     * @throws ArithmeticException if {@code d} is zero.
     *
     * @apiNote {@code d} should be treated as an unsigned long.
     */
    public MPZ divexactUi(long d) {
        if (d == 0l)
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        return new MPZ().divexactUiAssign(this, d);
    }

    /**
     * Return {@code true} if and only if {@code this} is exactly divisible by
     * {@code d}. This means that there exists an integer {@code q}satisfying
     * {@code (n
     * = q*d)}. Unlike the other division functions, the case {@code d=0} is
     * accepted and following the rule it can be seen that only {@code 0} is
     * considered divisible by {@code 0}.
     */
    public boolean isDivisible(MPZ d) {
        return mpz_divisible_p(mpzNative, d.mpzNative);
    }

    /**
     * Return {@code true} if and only if {@code this} is exactly divisible by
     * {@code d}. This means that there exists an integer {@code q} satisfying
     * {@code (n = q*d)}. The case {@code d=0} is accepted and following the rule it
     * can be seen that only {@code 0} is considered divisible by {@code 0}.
     *
     * @apiNote {@code d} should be treated as an unsigned long.
     */
    public boolean isDivisibleUi(long d) {
        return mpz_divisible_ui_p(mpzNative, new NativeUnsignedLong(d));
    }

    /**
     * Return {@code true} if and only if {@code this} is exactly divisible by
     * <code>2<sup>b</sup></code>. This means that there exists an integer {@code q}
     * satisfying {@code (n = q * 2^b)}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public boolean isDivisible2Exp(long b) {
        return mpz_divisible_2exp_p(mpzNative, new MpBitcntT(b));
    }

    /**
     * Return {@code true} if and only if {@code this} is congruent to {@code c}
     * modulo {@code d}. This means that there exists an integer {@code q}
     * satisfying {@code (n = c + q*d)}. Unlike the other division functions,
     * {@code d=0} is accepted and following the rule it can be seen that {@code n}
     * and {@code c} are considered congruent modulo {@code 0} only when exactly
     * equal.
     */
    public boolean isCongruent(MPZ c, MPZ d) {
        return mpz_congruent_p(mpzNative, c.mpzNative, d.mpzNative);
    }

    /**
     * Return {@code true} if and only if {@code this} is congruent to {@code c}
     * modulo {@code d}. This means that there exists an integer {@code q}
     * satisfying {@code (n = c + q*d)}. Unlike the other division functions,
     * {@code d=0} is accepted and following the rule it can be seen that {@code n}
     * and {@code c} are considered congruent modulo {@code 0} only when exactly
     * equal.
     *
     * @apiNote {@code c} and {@code d} should be treated as unsigned longs.
     */
    public boolean isCongruentUi(long c, long d) {
        return mpz_congruent_ui_p(mpzNative, new NativeUnsignedLong(c), new NativeUnsignedLong(d));
    }

    /**
     * Return {@code true} if and only if {@code this} is congruent to {@code c}
     * modulo <code>2<sup>b</sup></code>. This means that there exists an integer
     * {@code q} satisfying {@code (n = c + q*2^b)}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public boolean isCongruent2Exp(MPZ c, long b) {
        return mpz_congruent_2exp_p(mpzNative, c.mpzNative, new MpBitcntT(b));
    }

    // Integer Exponentiation

    /**
     * Set this {@code MPZ} to <code>(base<sup>exp</sup>)</code> modulo {@code mod}.
     * Negative {@code exp} is supported if the inverse of {@code base} modulo
     * {@code mod} exists, otherwise an {@code ArithmeticExpection} is thrown.
     *
     * @throws ArithmeticException if {@code mod} is zero or {@code base} has no
     *                             inverse modulo {@code mod}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ powmAssign(MPZ base, MPZ exp, MPZ mod) {
        if (mod.isZero())
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        if (exp.sgn() < 0) {
            var invBase = base.invert(mod);
            if (invBase.isEmpty())
                throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
            var posExp = exp.neg();
            mpz_powm(mpzNative, invBase.get().mpzNative, posExp.mpzNative, mod.mpzNative);
        } else
            mpz_powm(mpzNative, base.mpzNative, exp.mpzNative, mod.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to <code>(this<sup>exp</sup>)</code> modulo {@code mod}.
     *
     * @throws ArithmeticException if {@code mod} is zero.
     *
     * @return this {@code MPZ}.
     */
    public MPZ powmAssign(MPZ exp, MPZ mod) {
        return powmAssign(this, exp, mod);
    }

    /**
     * Return an {@code MPZ} whose value is <code>(this<sup>exp</sup>)</code> modulo
     * {@code mod}.
     */
    public MPZ powm(MPZ exp, MPZ mod) {
        return new MPZ().powmAssign(this, exp, mod);
    }

    /**
     * Set this {@code MPZ} to <code>(base<sup>exp</sup>)</code> modulo {@code mod}.
     *
     * @throws ArithmeticException if {@code mod} is zero.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code exp} should be treated as an unsigned long.
     */
    public MPZ powmUiAssign(MPZ base, long exp, MPZ mod) {
        if (mod.isZero())
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        mpz_powm_ui(mpzNative, base.mpzNative, new NativeUnsignedLong(exp), mod.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to <code>(this<sup>exp</sup>)</code> modulo {@code mod}.
     *
     * @throws ArithmeticException if {@code mod} is zero.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code exp} should be treated as an unsigned long.
     */
    public MPZ powmUiAssign(long exp, MPZ mod) {
        return powmUiAssign(this, exp, mod);
    }

    /**
     * Return an {@code MPZ} whose value is <code>(this<sup>exp</sup>)</code> modulo
     * {@code mod}.
     *
     * @apiNote {@code exp} should be treated as an unsigned long.
     */
    public MPZ powmUi(long exp, MPZ mod) {
        return new MPZ().powmUiAssign(this, exp, mod);
    }

    /**
     * Set this {@code MPZ} to <code>(base<sup>exp</sup>)</code> modulo {@code mod}.
     * It is required that {@code (exp > 0} and that {@code mod} is odd. This
     * function is intended for cryptographic purposes, where resilience to
     * side-channel attacks is desired.
     *
     * @throws ArithmeticException if {@code mod} is even or {@code exp} is
     *                             negative.
     *
     * @return this {@code MPZ}.
     */
    public MPZ powmSecAssign(MPZ base, MPZ exp, MPZ mod) {
        if (mod.isEven())
            throw new ArithmeticException(GMP.MSG_EVEN_MODULUS);
        if (exp.sgn() < 0)
            throw new ArithmeticException(GMP.MSG_NEGATIVE_EXPONENT);
        mpz_powm_sec(mpzNative, base.mpzNative, exp.mpzNative, mod.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to <code>(this<sup>exp</sup>)</code> modulo {@code mod}.
     * It is required that {@code (exp > 0} and that {@code mod} is odd. This
     * function is intended for cryptographic purposes, where resilience to
     * side-channel attacks is desired.
     *
     * @throws ArithmeticException if {@code mod} is zero.
     *
     * @return this {@code MPZ}.
     */
    public MPZ powmSecAssign(MPZ exp, MPZ mod) {
        return powmSecAssign(this, exp, mod);
    }

    /**
     * Return an {@code MPZ} whose value is <code>(this<sup>exp</sup>)</code> modulo
     * {@code mod}. It is required that {@code (exp > 0)} and that {@code mod} is
     * odd. This function is intended for cryptographic purposes, where resilience
     * to side-channel attacks is desired.
     */
    public MPZ powmSec(MPZ exp, MPZ mod) {
        return new MPZ().powmSecAssign(this, exp, mod);
    }

    /**
     * Set this {@code MPZ} to <code>(base<sup>exp</sup>)</code>. The case
     * <code>0<sup>0</sup></code> yields {@code 1}.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code exp} should be treated as an unsigned long.
     */
    public MPZ powUiAssign(MPZ base, long exp) {
        mpz_pow_ui(mpzNative, base.mpzNative, new NativeUnsignedLong(exp));
        return this;
    }

    /**
     * Set this {@code MPZ} to <code>(this<sup>exp</sup>)</code>. The case
     * <code>0<sup>0</sup></code> yields {@code 1}.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code exp} should be treated as an unsigned long.
     */
    public MPZ powUiAssign(long exp) {
        return powUiAssign(this, exp);
    }

    /**
     * Return an {@code MPZ} whose value is <code>(this<sup>exp</sup>)</code>. The
     * case <code>0<sup>0</sup></code> yields {@code 1}.
     *
     * @apiNote {@code exp} should be treated as an unsigned long.
     */
    public MPZ powUi(long exp) {
        return new MPZ().powUiAssign(this, exp);
    }

    /**
     * Set this {@code MPZ} to <code>(base<sup>exp</sup>)</code>. The case
     * <code>0<sup>0</sup></code> yields {@code 1}.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code exp} should be treated as an unsigned long.
     */
    public MPZ powUiAssign(long base, long exp) {
        mpz_ui_pow_ui(mpzNative, new NativeUnsignedLong(base), new NativeUnsignedLong(exp));
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is <code>(base<sup>exp</sup>)</code>. The
     * case <code>0<sup>0</sup></code> yields {@code 1}.
     *
     * @apiNote {@code exp} should be treated as an unsigned long.
     */
    public static MPZ powUi(long base, long exp) {
        return new MPZ().powUiAssign(base, exp);
    }

    // Integer Roots

    /**
     * Set this {@code MPZ} to the truncated integer part of the {@code n}th root of
     * {@code op}.
     *
     * @throws ArithmeticException if n is even and op is negative.
     *
     * @return true if the computation is exact.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public boolean rootAssign(MPZ op, long n) {
        if (n % 2 == 0 && op.sgn() < 0)
            throw new ArithmeticException(GMP.MSG_EVEN_ROOT_OF_NEGATIVE);
        return mpz_root(mpzNative, op.mpzNative, new NativeUnsignedLong(n));
    }

    /**
     * Set this {@code MPZ} to the truncated integer part of its {@code n}th root.
     *
     * @throws ArithmeticException if n is even and this is negative.
     *
     * @return true if the computation is exact.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public boolean rootAssign(long n) {
        return rootAssign(this, n);
    }

    /**
     * Return an {@code MPZ} whose value is the truncated integer part of the
     * {@code n}th root of {@code this}, and a boolean flag which is true when the
     * result is exact.
     *
     * @throws ArithmeticException if n is even and this is negative.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public Pair<Boolean, MPZ> root(long n) {
        var root = new MPZ();
        var exact = root.rootAssign(this, n);
        return new Pair<>(exact, root);
    }

    /**
     * Set this {@code MPZ} to the truncated integer part of the {@code n}th root of
     * {@code u} and {@code rem} to the remainder, i.e.,
     * <code>(u - root<sup>n</sup>)</code>.
     *
     * @throws ArithmeticException if n is even and u is negative.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public MPZ rootremAssign(MPZ rem, MPZ u, long n) {
        if (n % 2 == 0 && u.sgn() < 0)
            throw new ArithmeticException(GMP.MSG_EVEN_ROOT_OF_NEGATIVE);
        mpz_rootrem(mpzNative, rem.mpzNative, u.mpzNative, new NativeUnsignedLong(n));
        return this;
    }

    /**
     * Set this {@code MPZ} to the truncated integer part of the its {@code n}th
     * root and {@code rem} to the remainder, i.e.,
     * <code>(this - root<sup>n</sup>)</code>.
     *
     * @throws ArithmeticException if n is even and this is negative.
     *
     * @return this {@code MPZ}.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public MPZ rootremAssign(MPZ rem, long n) {
        return rootremAssign(rem, this, n);
    }

    /**
     * Return two {@code MPZ}s whose values are the truncated integer part of the
     * {@code n}th root of {@code this} and the remainder, i.e.,
     * <code>(u - root<sup>n</sup>)</code>.
     *
     * @throws ArithmeticException if n is even and this is negative.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public Pair<MPZ, MPZ> rootrem(long n) {
        MPZ res = new MPZ(), rem = new MPZ();
        res.rootremAssign(rem, this, n);
        return new Pair<>(res, rem);
    }

    /**
     * Set this {@code MPZ} to the truncated integer part of the square root of
     * {@code op}.
     *
     * @throws ArithmeticException if op is negative.
     *
     * @return this {@code MPZ}.
     */
    public MPZ sqrtAssign(MPZ op) {
        if (op.sgn() < 0)
            throw new ArithmeticException(GMP.MSG_EVEN_ROOT_OF_NEGATIVE);
        mpz_sqrt(mpzNative, op.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to the truncated integer part of its square root.
     *
     * @throws ArithmeticException if this is negative.
     *
     * @return this {@code MPZ}.
     */
    public MPZ sqrtAssign() {
        return sqrtAssign(this);
    }

    /**
     * Return an {@code MPZ} whose value is the truncated integer part of the square
     * root of {@code this}.
     *
     * @throws ArithmeticException if this is negative.
     */
    public MPZ sqrt() {
        return new MPZ().sqrtAssign(this);
    }

    /**
     * Set this {@code MPZ} to the truncated integer part of the square root of
     * {@code op} and {@code rem} to the remainder, i.e.,
     * <code>(op - root<sup>2</sup>)</code>.
     *
     * @throws ArithmeticException if op is negative.
     *
     * @return this {@code MPZ}.
     */
    public MPZ sqrtremAssign(MPZ rem, MPZ op) {
        if (op.sgn() < 0)
            throw new ArithmeticException(GMP.MSG_EVEN_ROOT_OF_NEGATIVE);
        mpz_sqrtrem(mpzNative, rem.mpzNative, op.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to the truncated integer part of its square root and
     * {@code rem} to the remainder, i.e., <code>(this - root<sup>2</sup>)</code>.
     *
     * @throws ArithmeticException if this is negative.
     *
     * @return this {@code MPZ}.
     */
    public MPZ sqrtremAssign(MPZ rem) {
        return sqrtremAssign(rem, this);
    }

    /**
     * Return two {@code MPZ}s whose values are the truncated integer part of the
     * square root of {@code this} and the remainder, i.e.,
     * <code>(op - root<sup>2</sup>)</code>.
     *
     * @throws ArithmeticException if this is negative.
     */
    public Pair<MPZ, MPZ> sqrtrem() {
        MPZ res = new MPZ(), rem = new MPZ();
        res.sqrtremAssign(rem, this);
        return new Pair<>(res, rem);
    }

    /**
     * Return {@code true} if and only if this number is a perfect power, i.e., if
     * there exist integers {@code a} and {@code b}, with {@code (b > 1)}, such that
     * {@code this} equals <code>(a<sup>b</sup>)</code>. Under this definition both
     * {@code 0} and {@code 1} are considered to be perfect powers. Negative values
     * are accepted, but of course can only be odd perfect powers.
     */
    public boolean isPerfectPower() {
        return mpz_perfect_power_p(mpzNative);
    }

    /**
     * Return {@code true} if and only if this number is a perfect square. Under
     * this definition both {@code 0} and {@code 1} are considered to be perfect
     * squares.
     */
    public boolean isPerfectSquare() {
        return mpz_perfect_square_p(mpzNative);
    }

    // Number Theoretic Functions

    /**
     * Return {@code true} if and only if {@code this} is prime. See the GMP
     * function
     * <a href="https://gmplib.org/manual/Number-Theoretic-Functions" target=
     * "_blank">{@code mpz_probab_prime_p}</a>.
     *
     * @param reps can be used to tune the probability of a non-prime being
     *             identified as “probably prime”. Reasonable values of reps are
     *             between 15 and 50.
     * @return an instance of the {@link PrimalityStatus} enum, telling whether
     *         {@code this} is definitely prime, probably prime or definitely
     *         non-prime.
     */
    public PrimalityStatus isProbabPrime(int reps) {
        var res = mpz_probab_prime_p(mpzNative, reps);
        return PrimalityStatus.values()[res];
    }

    /**
     * Set this {@code MPZ} to the next prime greater then {@code op}. This function
     * uses a probabilistic algorithm to identify primes. For practical purposes
     * it’s adequate, the chance of a composite passing will be extremely small.
     */
    public MPZ nextprimeAssign(MPZ op) {
        mpz_nextprime(mpzNative, op.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to the next prime greater then itself. This function
     * uses a probabilistic algorithm to identify primes. For practical purposes
     * it’s adequate, the chance of a composite passing will be extremely small.
     */
    public MPZ nextprimeAssign() {
        return nextprimeAssign(this);
    }

    /**
     * Return an {@code MPZ} whose value is the next prime greater then
     * {@code this}.
     *
     * @see nextprimeAssign(MPZ)
     */
    public MPZ nextprime() {
        return new MPZ().nextprimeAssign(this);
    }

    /**
     * Set this {@code MPZ} to the greatest commond divisor of {@code op1} and
     * {@code op2}. The result is always positive even if one or both input operands
     * are negative. Except if both inputs are zero; then this function defines
     * {@code gcd(0,0) = 0}.
     */
    public MPZ gcdAssign(MPZ op1, MPZ op2) {
        mpz_gcd(mpzNative, op1.mpzNative, op2.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to the greatest commond divisor of {@code this} and
     * {@code op}. The result is always positive even if one or both operands are
     * negative. Except if both {@code this} and {@code op} are zero; then this
     * function Return zero.
     */
    public MPZ gcdAssign(MPZ op) {
        return gcdAssign(this, op);
    }

    /**
     * Return an {@code MPZ} whose value is the greatest commond divisor of
     * {@code this} and {@code op}.
     *
     * @see gcdAssign(MPZ, MPZ)
     */
    public MPZ gcd(MPZ op) {
        return new MPZ().gcdAssign(this, op);
    }

    /**
     * Set this {@code MPZ} to the greatest commond divisor of {@code op1} and
     * {@code op2}, and return it. If the result does not fit into an unsigned long,
     * then 0 si returned.
     *
     * @see gcdAssign(MPZ, MPZ)
     *
     * @apiNote both {@code op2} and the returned value should be treated as
     *          unsigned longs.
     */
    public long gcdUiAssign(MPZ op1, long op2) {
        return mpz_gcd_ui(mpzNative, op1.mpzNative, new NativeUnsignedLong(op2)).longValue();
    }

    /**
     * Set this {@code MPZ} to the greatest commond divisor of {@code op1} and
     * {@code op2}, and returns it. If the result does not fit into an unsigned
     * long, then 0 si returned.
     *
     * @see gcdAssign(MPZ, MPZ)
     *
     * @apiNote both {@code op2} and the returned value should be treated as
     *          unsigned longs.
     */
    public long gcdUiAssign(long op) {
        return gcdUiAssign(this, op);
    }

    /**
     * Return the greatest commond divisor of {@code this} and {@code op}. If the
     * result does not fit into an unsigned long, 0 is returned.
     *
     * @apiNote both {@code op2} and the returned value should be treated as
     *          unsigned longs.
     */
    public long gcdUi(long op) {
        return mpz_gcd_ui(null, mpzNative, new NativeUnsignedLong(op)).longValue();
    }

    /**
     * Set this {@code MPZ} to the greatest common divisor of {@code a} and
     * {@code b}, and in addition Set {@code s} and {@code t} to coefficients
     * satisfying {@code (a*s + b*t = gcd)}. If {@code s} or {@code t} is null, that
     * value is not computed. See the GMP function
     * <a href="https://gmplib.org/manual/Number-Theoretic-Functions" target=
     * "_blank">{@code mpz_gcdext}</a>.
     */
    public MPZ gcdextAssign(MPZ s, MPZ t, MPZ a, MPZ b) {
        mpz_gcdext(mpzNative, s == null ? null : s.mpzNative, t == null ? null : t.mpzNative, a.mpzNative, b.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to the greatest common divisor of {@code this} and
     * {@code op}, and in addition Set {@code s} and {@code t} to coefficients
     * satisfying {@code (this*s + op*t = gcd)}. If {@code s} or {@code t} is null,
     * that value is not computed. See the GMP function
     * <a href="https://gmplib.org/manual/Number-Theoretic-Functions" target=
     * "_blank">{@code mpz_gcdext}</a>.
     */
    public MPZ gcdextAssign(MPZ s, MPZ t, MPZ op) {
        return gcdextAssign(s, t, this, op);
    }

    /**
     * Return the greatest common divisor of {@code this} and {@code op}, together
     * with numbers {@code s} and {@code t} satisfying {@code (a*this + b*op = g)}
     * See the GMP function
     * <a href="https://gmplib.org/manual/Number-Theoretic-Functions" target=
     * "_blank">{@code mpz_gcdext}</a>.
     */
    public Triplet<MPZ, MPZ, MPZ> gcdext(MPZ op) {
        MPZ r = new MPZ(), s = new MPZ(), t = new MPZ();
        r.gcdextAssign(s, t, this, op);
        return new Triplet<>(r, s, t);
    }

    /**
     * Set this {@code MPZ} to the least common multiple of {@code op1} and
     * {@code op2}. The result is always non-negative even if one or both input
     * operands are negative. The result will be zero if either {@code op1} or
     * {@code op2} is zero.
     */
    public MPZ lcmAssign(MPZ op1, MPZ op2) {
        mpz_lcm(mpzNative, op1.mpzNative, op2.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to the least common multiple of {@code this} and
     * {@code op}. The result is always non-negative even if one or both input
     * operands are negative. The result will be zero if either {@code this} or
     * {@code op} is zero.
     */
    public MPZ lcmAssign(MPZ op) {
        return lcmAssign(this, op);
    }

    /**
     * Return an {@code MPZ} whose value is the least common multiple of
     * {@code this} and {@code op}.
     *
     * @see lcmAssign(MPZ, MPZ)
     */
    public MPZ lcm(MPZ op) {
        return new MPZ().lcmAssign(this, op);
    }

    /**
     * Set this {@code MPZ} to the least common multiple of {@code op1} and
     * {@code op2}.
     *
     * @see lcmAssign(MPZ, MPZ)
     *
     * @apiNote {@code op2} should be treated as an unsigned long.
     */
    public MPZ lcmUiAssign(MPZ op1, long op2) {
        mpz_lcm_ui(mpzNative, op1.mpzNative, new NativeUnsignedLong(op2));
        return this;
    }

    /**
     * Set this {@code MPZ} to the least common multiple of {@code this} and
     * {@code op}.
     *
     * @see lcmAssign(MPZ, MPZ)
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPZ lcmUiAssign(long op) {
        return lcmUiAssign(this, op);
    }

    /**
     * Return the least common multiple of {@code this} and {@code op}.
     *
     * @see lcmAssign(MPZ, MPZ)
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPZ lcmUi(long op) {
        return new MPZ().lcmUiAssign(this, op);
    }

    /**
     * Set this {@code MPZ} to the inverse of {@code op1} modulo {@code op2}. If the
     * inverse does not exist, the new value of this {@code MPZ} is undefined.
     *
     * @apiNote Differently from the original GMP function, the result is
     *          {@code false} when {@code op2} is zero.
     *
     * @return true if the inverse exists, false otherwise.
     */
    public boolean invertAssign(MPZ op1, MPZ op2) {
        if (op2.isZero())
            return false;
        return mpz_invert(mpzNative, op1.mpzNative, op2.mpzNative);
    }

    /**
     * Set this {@code MPZ} to the inverse of {@code this} modulo {@code op}. If the
     * inverse does not exist, the new value of this {@code MPZ} is undefined.
     *
     * @apiNote Differently from the original GMP function, the result is
     *          {@code false} when {@code op} is zero.
     *
     * @return true if the inverse exists, false otherwise.
     */
    public boolean invertAssign(MPZ op) {
        return invertAssign(this, op);
    }

    /**
     * Optionally return, when it exists, an {@code MPZ} whose value is the inverse
     * of {@code this} modulo {@code op}.
     *
     * @apiNote Differently from the original GMP function, the result is empty when
     *          {@code op} is zero.
     */
    public Optional<MPZ> invert(MPZ op) {
        if (op.isZero())
            return Optional.empty();
        var p = new MpzT();
        mpz_init(p);
        var exists = mpz_invert(p, this.mpzNative, op.mpzNative);
        if (exists)
            return Optional.of(new MPZ(p));
        else {
            mpz_clear(p);
            return Optional.empty();
        }
    }

    /**
     * Return the Jacobi symbol {@code (this / b)}. This is defined only for
     * {@code b} odd.
     */
    public int jacobi(MPZ b) {
        return mpz_jacobi(mpzNative, b.mpzNative);
    }

    /**
     * Return the Legendre symbol {@code (this / p)}. This is defined only for
     * {@code p} an odd positive prime, and for such {@code p} it’s identical to the
     * Jacobi symbol.
     */
    public int legendre(MPZ p) {
        return mpz_legendre(mpzNative, p.mpzNative);
    }

    /**
     * Return the Jacobi symbol {@code (this / n)} with the Kronecker extension
     * {@code (this/2)=(2/this)} when {@code this} is odd, or {@code (this/2)=0 }
     * when {@code this} is even. When {@code b} is odd the Jacobi symbol and
     * Kronecker symbol are identical. See the GMP function {@code mpz_kronecker}.
     */
    public int kronecker(MPZ b) {
        // the jacobi GMP function already implements the Kronecker extension
        return mpz_jacobi(mpzNative, b.mpzNative);
    }

    /**
     * Return the Jacobi symbol {@code (this / b)} with the Kronecker extension.
     *
     * @see kronecker(MPZ)
     */
    public int kronecker(long b) {
        return mpz_kronecker_si(mpzNative, new NativeLong(b));
    }

    /**
     * Return the Jacobi symbol {@code (this / b)} with the Kronecker extension.
     *
     * @see kronecker(MPZ)
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public int kroneckerUi(long b) {
        return mpz_kronecker_ui(mpzNative, new NativeUnsignedLong(b));
    }

    /**
     * Return the Jacobi symbol {@code (a / this)} with the Kronecker extension.
     *
     * @see kronecker(MPZ)
     */
    public int kroneckerReverse(long a) {
        return mpz_si_kronecker(new NativeLong(a), mpzNative);
    }

    /**
     * Return the Jacobi symbol {@code (a / this)} with the Kronecker extension.
     *
     * @see kronecker(MPZ)
     *
     * @apiNote {@code a} should be treated as an unsigned long.
     */
    public int uiKronecker(long a) {
        return mpz_ui_kronecker(new NativeUnsignedLong(a), mpzNative);
    }

    /**
     * Remove all occurrences of the factor {@code f} from {@code op} and stores the
     * result in this {@code MPZ}. The return value is the number of occurrences of
     * {@code f} which were removed.
     */
    public long removeAssign(MPZ op, MPZ f) {
        return mpz_remove(mpzNative, op.mpzNative, f.mpzNative).longValue();
    }

    /**
     * Remove all occurrences of the factor {@code f} from {@code this} MPZ. The
     * return value is the number of occurrences of {@code f} which were removed.
     */
    public long removeAssign(MPZ f) {
        return removeAssign(this, f);
    }

    /**
     * Return the result of removing the factor{@code f} from {@code this}, together
     * with the number of occurrences which were removed.
     *
     * @apiNote the first element of the returned value should be treated as an
     *          unsigned long.
     */
    public Pair<Long, MPZ> remove(MPZ f) {
        var res = new MPZ();
        var count = mpz_remove(res.mpzNative, mpzNative, f.mpzNative);
        return new Pair<>(count.longValue(), res);
    }

    /**
     * Set this {@code MPZ} to the factorial of {@code n}.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public MPZ facUiAssign(long n) {
        mpz_fac_ui(mpzNative, new NativeUnsignedLong(n));
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is the factorial of {@code n}.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public static MPZ facUi(long n) {
        return new MPZ().facUiAssign(n);
    }

    /**
     * Set this {@code MPZ} to the double factorial of {@code n}.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public MPZ dfacUiAssign(long n) {
        mpz_2fac_ui(mpzNative, new NativeUnsignedLong(n));
        return this;
    }

    /**
     * Return an {@code MPZ} whose value the double factorial of {@code n}.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public static MPZ dfacUi(long n) {
        return new MPZ().dfacUiAssign(n);
    }

    /**
     * Set this {@code MPZ} to the {@code m}-multi factorial of {@code n}.
     *
     * @apiNote both {@code n} and {@code m} should be treated as unsigned longs.
     */
    public MPZ mfacUiUiAssign(long n, long m) {
        mpz_mfac_uiui(mpzNative, new NativeUnsignedLong(n), new NativeUnsignedLong(m));
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is the {@code m}-multi factorial of
     * {@code n}.
     *
     * @apiNote both {@code n} and {@code m} should be treated as unsigned longs.
     */
    public static MPZ mfacUiUi(long n, long m) {
        return new MPZ().mfacUiUiAssign(n, m);
    }

    /**
     * Set this {@code MPZ} to the primorial of {@code n}, i.e., the product of all
     * positive prime numbers {@code <= n}.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public MPZ primorialUiAssign(long n) {
        mpz_primorial_ui(mpzNative, new NativeUnsignedLong(n));
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is the primorial of {@code n}, i.e., the
     * product of all positive prime numbers {@code <= n}.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public static MPZ primorialUi(long n) {
        return new MPZ().primorialUiAssign(n);
    }

    /**
     * Set this {@code MPZ} to the binomial coefficient {@code n} over {@code k}.
     * Negative values of {@code n} are supported using the identity
     * {@code (bin(-n,k) = (-1)^k * bin(n+k-1,k))}, see Knuth volume 1 section 1.2.6
     * part G.
     *
     * @apiNote {@code k} should be treated as an unsigned long.
     */
    public MPZ binUiAssign(MPZ n, long k) {
        mpz_bin_ui(mpzNative, n.mpzNative, new NativeUnsignedLong(k));
        return this;
    }

    /**
     * Set this {@code MPZ} to the binomial coefficient {@code this} over {@code k}.
     * Negative values of {@code this} are supported using the identity
     * {@code (bin(-n,k) = (-1)^k * bin(n+k-1,k))}, see Knuth volume 1 section 1.2.6
     * part G.
     *
     * @apiNote {@code k} should be treated as an unsigned long.
     */
    public MPZ binUiAssign(long k) {
        return binUiAssign(this, k);
    }

    /**
     * Return an {@code MPZ} whose value is the binomial coefficient {@code this}
     * over {@code k}. Negative values of {@code this} are supported as in
     * {@link binUiAssign(MPZ, long)}.
     *
     * @apiNote {@code k} should be treated as an unsigned long.
     */
    public MPZ binUi(long k) {
        return new MPZ().binUiAssign(this, k);
    }

    /**
     * Set this {@code MPZ} to the binomial coefficient {@code n} over {@code k}.
     *
     * @apiNote both {@code n} and {@code k} should be treated as unsigned longs.
     */
    public MPZ binUiUiAssign(long n, long k) {
        mpz_bin_uiui(mpzNative, new NativeUnsignedLong(n), new NativeUnsignedLong(k));
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is the binomial coefficient {@code n} over
     * {@code k}.
     *
     * @apiNote both {@code n} and {@code k} should be treated as unsigned longs.
     */
    public static MPZ binUiUi(long n, long k) {
        return new MPZ().binUiUiAssign(n, k);
    }

    /**
     * Set this {@code MPZ} to the {@code n}-th Fibonacci number.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public MPZ fibUiAssign(long n) {
        mpz_fib_ui(mpzNative, new NativeUnsignedLong(n));
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is the {@code n}-th Fibonacci number.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public static MPZ fibUi(long n) {
        return new MPZ().fibUiAssign(n);
    }

    /**
     * Set the value of {@code this} and {@code fnsub1} to the {@code n}-th and
     * {@code (n-1)}-th Fibonacci numbers respecively.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public MPZ fib2UiAssign(MPZ fnsub1, long n) {
        mpz_fib2_ui(mpzNative, fnsub1.mpzNative, new NativeUnsignedLong(n));
        return this;
    }

    /**
     * Return two {@code MPZ} whose values are the {@code n}-th and {@code (n-1)}-th
     * Fibonacci numbers.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public static Pair<MPZ, MPZ> fib2Ui(long n) {
        MPZ fnsub1 = new MPZ(), fn = new MPZ();
        fn.fib2UiAssign(fnsub1, n);
        return new Pair<>(fn, fnsub1);
    }

    /**
     * Set this {@code MPZ} to the {@code n}-th Lucas number.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public MPZ lucnumUiAssign(long n) {
        mpz_lucnum_ui(mpzNative, new NativeUnsignedLong(n));
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is the {@code n}-th Lucas number.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public static MPZ lucnumUi(long n) {
        return new MPZ().lucnumUiAssign(n);
    }

    /**
     * Set the value of {@code this} and {@code fnsub1} to the {@code n}-th and
     * {@code (n-1)}-th Lucas numbers respecively.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public MPZ lucnum2UiAssign(MPZ fnsub1, long n) {
        mpz_lucnum2_ui(mpzNative, fnsub1.mpzNative, new NativeUnsignedLong(n));
        return this;
    }

    /**
     * Return two {@code MPZ} whose values are the {@code n}-th and {@code (n-1)}-th
     * Lucas numbers.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public static Pair<MPZ, MPZ> lucnum2Ui(long n) {
        MPZ lnsub1 = new MPZ(), ln = new MPZ();
        ln.lucnum2UiAssign(lnsub1, n);
        return new Pair<>(ln, lnsub1);
    }

    // Integer Comparisons

    /**
     * Compare {@code this} with {@code op}. Return a positive value if
     * {@code (this > op)}, zero if {@code this = op}, or a negative value if
     * {@code this < op}.
     */
    public int cmp(MPZ op) {
        return mpz_cmp(mpzNative, op.mpzNative);
    }

    /**
     * Compare {@code this} with {@code op}. Return a positive value if
     * {@code (this > op)}, zero if {@code this = op}, or a negative value if
     * {@code this < op}. The value of {@code op} may be infinite, but the result is
     * undefined on NaNs.
     *
     * @throws ArithmeticException if {@code op} is a NaN.
     */
    public int cmp(double op) {
        if (Double.isNaN(op))
            throw new ArithmeticException(GMP.MSG_NAN_NOT_ALLOWED);
        return mpz_cmp_d(mpzNative, op);
    }

    /**
     * Compare {@code this} with {@code op}. Return a positive value if
     * {@code (this > op)}, zero if {@code this = op}, or a negative value if
     * {@code this < op}.
     */
    public int cmp(long op) {
        return mpz_cmp_si(mpzNative, new NativeLong(op));
    }

    /**
     * Compare {@code this} with {@code op}. Return a positive value if
     * {@code (this > op)}, zero if {@code this = op}, or a negative value if
     * {@code this < op}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public int cmpUi(long op) {
        return mpz_cmp_ui(mpzNative, new NativeUnsignedLong(op));
    }

    /**
     * Compare the absolute values of {@code this} and {@code op}. Return a positive
     * value if {@code (abs(this) > abs(op))}, zero if {@code abs(this) = abs(op)},
     * or a negative value if {@code abs(this) < abs(op)}.
     */
    public int cmpabs(MPZ op) {
        return mpz_cmpabs(mpzNative, op.mpzNative);
    }

    /**
     * Compare the absolute values of {@code this} and {@code op}. Return a positive
     * value if {@code (abs(this) > abs(op))}, zero if {@code abs(this) = abs(op)},
     * or a negative value if {@code abs(this) < abs(op)}. The value of {@code op}
     * may be infinite, but the result is undefined on NaNs.
     *
     * @throws ArithmeticException if {@code op} is a NaN.
     */
    public int cmpabs(double op) {
        if (Double.isNaN(op))
            throw new ArithmeticException(GMP.MSG_NAN_NOT_ALLOWED);
        return mpz_cmpabs_d(mpzNative, op);
    }

    /**
     * Compare the absolute values of {@code this} and {@code op}. Return a positive
     * value if {@code (abs(this) > abs(op))}, zero if {@code abs(this) = abs(op)},
     * or a negative value if {@code abs(this) < abs(op)}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public int cmpabsUi(long op) {
        return mpz_cmpabs_ui(mpzNative, new NativeUnsignedLong(op));
    }

    /**
     * Return {@code +1} if {@code (this > 0)}, {@code 0} if {@code (this = 0)} and
     * {@code -1} if {@code this < 0}.
     */
    public int sgn() {
        return mpz_sgn(mpzNative);
    }

    // Integer Logic and Bit Fiddling

    /**
     * Set this {@code MPZ} to {@code (op1 & op2)}.
     */
    public MPZ andAssign(MPZ op1, MPZ op2) {
        mpz_and(mpzNative, op1.mpzNative, op2.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to {@code (this & op)}.
     */
    public MPZ andAssign(MPZ op) {
        return andAssign(this, op);
    }

    /**
     * Return an {@code MPZ} whose value is {@code (this & op)}.
     */
    public MPZ and(MPZ op) {
        return new MPZ().andAssign(this, op);
    }

    /**
     * Set this {@code MPZ} to {@code (op1 | op2)}.
     */
    public MPZ iorAssign(MPZ op1, MPZ op2) {
        mpz_ior(mpzNative, op1.mpzNative, op2.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to {@code (this | op)}.
     */
    public MPZ iorAssign(MPZ op) {
        return iorAssign(this, op);
    }

    /**
     * Return an {@code MPZ} whose value is {@code (this | op)}.
     */
    public MPZ ior(MPZ op) {
        return new MPZ().iorAssign(this, op);
    }

    /**
     * Set this {@code MPZ} to {@code (op1 ^ op2)}.
     */
    public MPZ xorAssign(MPZ op1, MPZ op2) {
        mpz_xor(mpzNative, op1.mpzNative, op2.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to {@code (this ^ op)}.
     */
    public MPZ xorAssign(MPZ op) {
        return xorAssign(this, op);
    }

    /**
     * Return an {@code MPZ} whose value is {@code (this ^ op)}.
     */
    public MPZ xor(MPZ op) {
        return new MPZ().xorAssign(this, op);
    }

    /**
     * Set this {@code MPZ} to {@code (~ op)}.
     */
    public MPZ comAssign(MPZ op) {
        mpz_com(mpzNative, op.mpzNative);
        return this;
    }

    /**
     * Set this {@code MPZ} to {@code (~ this)}.
     */
    public MPZ comAssign() {
        return comAssign(this);
    }

    /**
     * Return an {@code MPZ} whose value is {@code (~ op)}.
     */
    public MPZ com() {
        return new MPZ().comAssign(this);
    }

    /**
     * If this {@code MPZ} is non-negative, return its population count, which is
     * the number of {@code 1} bits in its binary representation. If this
     * {@code MPZ} is negative, the number of {@code 1}s is infinite, and the return
     * value is the largest possible value for the native type {@code mp_bitcnt_t}.
     *
     * @apiNote the returned value should be treated as an unigned long.
     */
    public long popcount() {
        return mpz_popcount(mpzNative).longValue();
    }

    /**
     * If {@code this} and {@code op} are both {@code >= 0} or both {@code < 0},
     * return the Hamming distance between them, which is the number of bit
     * positions where {@code this} and {@code op} have different bit values. If one
     * operand is {@code >= 0} and the other {@code < 0} then the number of bits
     * different is infinite, and the return value is the largest possible value for
     * the native type {@code mp_bitcnt_t}.
     *
     * @apiNote the returned value should be treated as an unigned long.
     */
    public long hamdist(MPZ op) {
        return mpz_hamdist(mpzNative, op.mpzNative).longValue();
    }

    /**
     * Scan this {@code MPZ}, starting from bit {@code starting_bit}, towards more
     * significant bits, until the first {@code 0} bit is found. Return the index of
     * the found bit.
     *
     * @apiNote both {@code starting_bit} and the returned value should be treated
     *          as unsigned longs.
     */
    public long scan0(long starting_bit) {
        return mpz_scan0(mpzNative, new MpBitcntT(starting_bit)).longValue();
    }

    /**
     * Scan {@code this}, starting from bit {@code starting_bit}, towards more
     * significant bits, until the first {@code 1} bit is found. Return the index of
     * the found bit.
     *
     * @apiNote both {@code starting_bit} and the returned value should be treated
     *          as unsigned longs.
     */
    public long scan1(long starting_bit) {
        return mpz_scan1(mpzNative, new MpBitcntT(starting_bit)).longValue();
    }

    /**
     * Set the bit {@code index} of this {@code MPZ}.
     *
     * @apiNote both {@code starting_bit} and the returned value should be treated
     *          as unsigned longs.
     */
    public MPZ setbitAssign(long index) {
        mpz_setbit(mpzNative, new MpBitcntT(index));
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is <code>(this | 2<sup>index</sup>)</code>.
     *
     * @apiNote both {@code starting_bit} and the returned value should be treated
     *          as unsigned longs.
     */
    public MPZ setbit(long index) {
        return new MPZ(this).setbitAssign(index);
    }

    /**
     * Clear the bit {@code index} of this {@code MPZ}.
     *
     * @apiNote both {@code starting_bit} and the returned value should be treated
     *          as unsigned longs.
     */
    public MPZ clrbitAssign(long index) {
        mpz_clrbit(mpzNative, new MpBitcntT(index));
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is
     * <code>(this &amp; ~ 2<sup>index</sup>)</code>.
     *
     * @apiNote both {@code starting_bit} and the returned value should be treated
     *          as unsigned longs.
     */
    public MPZ clrbit(long index) {
        return new MPZ(this).clrbitAssign(index);
    }

    /**
     * Complement the bit {@code index} of this {@code MPZ}.
     *
     * @apiNote both {@code starting_bit} and the returned value should be treated
     *          as unsigned longs.
     */
    public MPZ combitAssign(long index) {
        mpz_combit(mpzNative, new MpBitcntT(index));
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is <code>(this ^ 2<sup>index</sup>)</code>.
     *
     * @apiNote both {@code starting_bit} and the returned value should be treated
     *          as unsigned longs.
     */
    public MPZ combit(long index) {
        return new MPZ(this).combitAssign(index);
    }

    /**
     * Return the bit {@code index} of this {@code MPZ}.
     *
     * @apiNote both {@code starting_bit} and the returned value should be treated
     *          as unsigned longs.
     */
    public int tstbit(long index) {
        return mpz_tstbit(mpzNative, new MpBitcntT(index));
    }

    // Integer Random Numbers

    /**
     * Set this {@code MPZ} to a uniformly distributed random integer in the range
     * {@code 0} to <code>(2<sup>n</sup> - 1)</code>, inclusive.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public MPZ urandombAssign(RandState s, long n) {
        mpz_urandomb(mpzNative, s.getNative(), new MpBitcntT(n));
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is an uniformly distributed random integer
     * in the range {@code 0}} to <code>(2<sup>n</sup> - 1)</code>, inclusive.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public static MPZ urandomb(RandState s, long n) {
        var z = new MPZ();
        z.urandombAssign(s, n);
        return z;
    }

    /**
     * Set this {@code MPZ} to a uniformly distributed random integer in the range
     * {@code 0} to {@code (n - 1)}, inclusive.
     */
    public MPZ urandommAssign(RandState s, MPZ n) {
        mpz_urandomm(mpzNative, s.getNative(), n.mpzNative);
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is an uniformly distributed random integer
     * in the range {@code 0} to {@code (n - 1)}, inclusive.
     */
    public static MPZ urandomm(RandState s, MPZ n) {
        var z = new MPZ();
        z.urandommAssign(s, n);
        return z;
    }

    /**
     * Set this {@code MPZ} to a random integer with long strings of zeros and ones
     * in the binary representation. Useful for testing functions and algorithms,
     * since this kind of random numbers have proven to be more likely to trigger
     * corner-case bugs. The random number will be in the range
     * <code>(2<sup>n - 1</sup>)</code> to <code>(2<sup>n</sup> - 1)</code>,
     * inclusive.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public MPZ rrandombAssign(RandState s, long n) {
        mpz_rrandomb(mpzNative, s.getNative(), new MpBitcntT(n));
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is a random integer with long strings of
     * zeros and ones in the binary representation. Useful for testing functions and
     * algorithms, since this kind of random numbers have proven to be more likely
     * to trigger corner-case bugs. The random number will be in the range
     * <code>(2<sup>n - 1</sup>)</code> to <code>(2<sup>n</sup> - 1)</code>,
     * inclusive.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public static MPZ rrandomb(RandState s, long n) {
        var z = new MPZ();
        z.urandombAssign(s, n);
        return z;
    }

    /**
     * Set this {@code MPZ} to a random integer of at most {@code max_size} limbs.
     * The generated random number doesn’t satisfy any particular requirements of
     * randomness. Negative random numbers are generated when {@code max_size} is
     * negative.
     *
     * @deprecated use {@link urandombAssign} or {@link urandommAssign} instead,
     *             since this method uses a global random state and it is not
     *             reentrant.
     * @return this {@code MPZ}.
     */
    @Deprecated
    public MPZ randomAssign(long max_size) {
        mpz_random(mpzNative, new MpSizeT(max_size));
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is a random integer of at most
     * {@code max_size} limbs. The generated random number doesn’t satisfy any
     * particular requirements of randomness. Negative random numbers are generated
     * when {@code max_size} is negative.
     *
     * @deprecated use {@link urandomb} or {@link urandomm} instead, since this
     *             method uses a global random state and it is not reentrant.
     */
    @Deprecated
    public static MPZ random(long max_size) {
        return new MPZ().randomAssign(max_size);
    }

    /**
     * Set this {@code MPZ} to a random integer of at most {@code max_size} limbs,
     * with long strings of zeros and ones in the binary representation. Useful for
     * testing functions and algorithms, since this kind of random numbers have
     * proven to be more likely to trigger corner-case bugs. Negative random numbers
     * are generated when {@code max_size} is negative.
     *
     * @deprecated use {@link rrandombAssign} instead, since this method uses a
     *             global random state and it is not reentrant.
     * @return this {@code MPZ}.
     */
    @Deprecated
    public MPZ random2Assign(long max_size) {
        mpz_random2(mpzNative, new MpSizeT(max_size));
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is a random integer of at most
     * {@code max_size} limbs, with long strings of zeros and ones in the binary
     * representation. Useful for testing functions and algorithms, since this kind
     * of random numbers have proven to be more likely to trigger corner-case bugs.
     * Negative random numbers are generated when {@code max_size} is negative.
     *
     * @deprecated use {@link rrandomb} instead, since this method uses a global
     *             random state and it is not reentrant.
     */
    @Deprecated
    public static MPZ random2(long max_size) {
        return new MPZ().random2Assign(max_size);
    }

    // Integer Import and Export

    /**
     * Set this {@code MPZ} from the buffer of word data at {@code op}. See the
     * detailed description in the documentation of the GMP function
     * <a href="https://gmplib.org/manual/Integer-Import-and-Export" target=
     * "_blank">{@code mpz_import}</a>. The parameter {@code count} in the prototype
     * of {@code mpz_import} is automatically computed by the capacity of the buffer
     * {@code op}.
     *
     * The {@code size} parameter is declared as {@code int} instead of {@code long}
     * since Java does not allow byte buffers to be longer than 4GB.
     *
     * @apiNote {@code nails} should be treated as an unsigned long.
     */
    public MPZ bufferImportAssign(int order, int size, int endian, long nails, ByteBuffer op) {
        var count = op.capacity() / size + (op.capacity() % size == 0 ? 0 : 1);
        mpz_import(mpzNative, new SizeT(count), order, new SizeT(size), endian, new SizeT(nails), op);
        return this;
    }

    /**
     * Return an {@code MPZ} whose value is determined from the buffer of word data
     * at {@code op}.
     *
     * The {@code size} parameter is declared as {@code int} instead of {@code long}
     * since Java does not allow byte buffers to be longer than 4GB.
     *
     * @see bufferImportAssign
     *
     * @apiNote {@code nails} should be treated as an unsigned long.
     */
    public static MPZ bufferImport(int order, int size, int endian, long nails, ByteBuffer op) {
        return new MPZ().bufferImportAssign(order, size, endian, nails, op);
    }

    /**
     * Return a {@link ByteBuffer} filled with word data from this {@code MPZ}. See
     * the detailed description in the documentation of the GMP function
     * <a href="https://gmplib.org/manual/Integer-Import-and-Export" target=
     * "_blank">{@code mpz_export}</a>. We let the function allocate the buffer,
     * since it the easier and safer. The output {@code count} of the original GMP
     * function is not needed, since it corresponds to the capacity of the resulting
     * {@link ByteBuffer}.
     *
     * The {@code size} parameter is declared as {@code int} instead of {@code long}
     * since Java does not allow byte buffers to be longer than 4GB.
     *
     * @apiNote {@code nails} should be treated as an unsigned long.
     */
    public ByteBuffer bufferExport(int order, int size, int endian, long nails) {
        var count = new SizeTByReference();
        var p = mpz_export(null, count, order, new SizeT(size), endian, new SizeT(nails), mpzNative);
        return p.getByteBuffer(0, count.getValue().longValue());
    }

    // Miscellaneous Integer Functions

    /**
     * Return {@code true} if and only if this {@code MPZ} fits into a native
     * unsigned long.
     */
    public boolean fitsUlong() {
        return mpz_fits_ulong_p(mpzNative);
    }

    /**
     * Return {@code true} if and only if this {@code MPZ} fits into a native signed
     * long.
     */
    public boolean fitsSlong() {
        return mpz_fits_slong_p(mpzNative);
    }

    /**
     * Return {@code true} if and only if this {@code MPZ} fits into a native
     * unsigned int.
     */
    public boolean fitsUint() {
        return mpz_fits_uint_p(mpzNative);
    }

    /**
     * Return {@code true} if and only if this {@code MPZ} fits into a native signed
     * int.
     */
    public boolean fitsSint() {
        return mpz_fits_sint_p(mpzNative);
    }

    /**
     * Return {@code true} if and only if this {@code MPZ} fits into a native
     * unsigned short.
     */
    public boolean fitsUshort() {
        return mpz_fits_ushort_p(mpzNative);
    }

    /**
     * Return {@code true} if and only if this {@code MPZ} fits into a native signed
     * short.
     */
    public boolean fitsSshort() {
        return mpz_fits_sshort_p(mpzNative);
    }

    /**
     * Return {@code true} if and only if this {@code MPZ} is odd.
     */
    public boolean isOdd() {
        return fdivUi(2) != 0;
    }

    /**
     * Return {@code true} if and only if this {@code MPZ} is even
     */
    public boolean isEven() {
        return fdivUi(2) == 0l;
    }

    /**
     * Return {@code true} if and only if this {@code MPZ} is zero.
     */
    public boolean isZero() {
        return mpz_sgn(mpzNative) == 0;
    }

    /**
     * Return the size of this {@code MPZ} measured in number of digits in the
     * specified {@code base}. See the the GMP function
     * <a href="https://gmplib.org/manual/Miscellaneous-Integer-Functions" target=
     * "_blank">{@code mpz_sizeinbase}</a>.
     *
     * @throws IllegalArgumentException if base is not between 2 and 62.
     *
     * @apiNote the return value should be treated as an unsigned long.
     */
    public long sizeinbase(int base) {
        if (base < 2 || base > 62)
            throw new IllegalArgumentException(GMP.MSG_INVALID_BASE);
        return mpz_sizeinbase(mpzNative, base).longValue();
    }

    // Constructors

    /**
     * Build an {@code MPZ} whose value is zero.
     */
    public MPZ() {
        mpzNative = new MpzT();
        mpz_init(mpzNative);
        GMP.cleaner.register(mpzNative, new MPZCleaner(mpzNative));
    }

    /**
     * Build an {@code MPZ} whose value is {@code op}.
     */
    public MPZ(MPZ op) {
        mpzNative = new MpzT();
        mpz_init_set(mpzNative, op.mpzNative);
        GMP.cleaner.register(mpzNative, new MPZCleaner(mpzNative));
    }

    /**
     * Build an {@code MPZ} whose value is {@code op}.
     */
    public MPZ(long op) {
        mpzNative = new MpzT();
        mpz_init_set_si(mpzNative, new NativeLong(op));
        GMP.cleaner.register(mpzNative, new MPZCleaner(mpzNative));
    }

    /**
     * Build an {@code MPZ} whose value is the truncation of {@code op}.
     *
     * @throws ArithmeticException if {@code op} is not a finite number.
     */
    public MPZ(double op) {
        mpzNative = new MpzT();
        if (!Double.isFinite(op))
            throw new ArithmeticException(GMP.MSG_FINITE_DOUBLE_REQUIRED);
        mpz_init_set_d(mpzNative, op);
        GMP.cleaner.register(mpzNative, new MPZCleaner(mpzNative));
    }

    /**
     * Build an {@code MPZ} whose value is the truncation of {@code op}.
     */
    public MPZ(MPQ op) {
        mpzNative = new MpzT();
        mpz_init(mpzNative);
        mpz_set_q(mpzNative, op.getNative());
        GMP.cleaner.register(mpzNative, new MPZCleaner(mpzNative));
    }

    /**
     * Build an {@code MPZ} whose value is the truncation of {@code op}.
     */
    public MPZ(MPF op) {
        mpzNative = new MpzT();
        mpz_init(mpzNative);
        mpz_set_f(mpzNative, op.getNative());
        GMP.cleaner.register(mpzNative, new MPZCleaner(mpzNative));
    }

    /**
     * Build an {@code MPZ} whose value is the number represented by the string
     * {@code str} in the specified {@code base}. See the GMP function
     * <a href="https://gmplib.org/manual/Simultaneous-Integer-Init-_0026-Assign"
     * target="_blank">{@code mpz_init_set_str}</a>.
     *
     * @throws NumberFormatException if either {@code base} is not valid or
     *                               {@code str} is not a valid string in the
     *                               specified {@code base}.
     *
     */
    public MPZ(String str, int base) {
        mpzNative = new MpzT();
        int result = mpz_init_set_str(mpzNative, str, base);
        if (result == -1) {
            mpz_clear(mpzNative);
            throw new NumberFormatException(GMP.MSG_INVALID_STRING_CONVERSION);
        }
        GMP.cleaner.register(mpzNative, new MPZCleaner(mpzNative));
    }

    /**
     * Build an {@code MPZ} whose value is the number represented by the string
     * {@code str} in decimal base. See the GMP function
     * <a href="https://gmplib.org/manual/Simultaneous-Integer-Init-_0026-Assign"
     * target="_blank">{@code mpz_init_set_str}</a>.
     *
     * @throws NumberFormatException if {@code str} is not a valid number
     *                               representation in decimal base.
     */
    public MPZ(String str) {
        this(str, 10);
    }

    /**
     * Builds an {@code MPZ} whose value is the same as {@code op}.
     */
    public MPZ(BigInteger op) {
        this();
        set(op);
    }

    // setValue functions

    /**
     * Set this {@code MPZ} to {@code op}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ setValue(MPZ op) {
        return set(op);
    }

    /**
     * Set this {@code MPZ} to signed long {@code op}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ setValue(long op) {
        return set(op);
    }

    /**
     * Set this {@code MPZ} to the truncation op {@code op}.
     *
     * @throws ArithmeticException if {@code op} is not a finite number. In this
     *                             case, {@code this} is not altered.
     *
     * @return this {@code MPZ}.
     */
    public MPZ setValue(double op) {
        return set(op);
    }

    /**
     * Set this {@code MPZ} to the truncation op {@code op}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ setValue(MPQ op) {
        return set(op);
    }

    /**
     * Set this {@code MPZ} to the truncation op {@code op}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ setValue(MPF op) {
        return set(op);
    }

    /**
     * Set this {@code MPZ} to the number represented by the string {@code str} in
     * the specified {@code base}. See the GMP function
     * <a href="https://gmplib.org/manual/Assigning-Integers" target="
     * _blank">{@code mpz_set_str}</a>.
     *
     * @throws NumberFormatException if either {@code base} is not valid or
     *                               {@code str} is not a valid number
     *                               representation in the specified base. In this
     *                               case, {@code this} is not altered.
     */
    public MPZ setValue(String str, int base) {
        var result = set(str, base);
        if (result == -1)
            throw new NumberFormatException(GMP.MSG_INVALID_STRING_CONVERSION);
        return this;
    }

    /**
     * Set this {@code MPZ} to the value represented by the string {@code str} in
     * decimal base.
     *
     * @throws NumberFormatException if {@code str} is not a valid number
     *                               representation in decimal base.
     * @see setValue(String, int)
     */
    public MPZ setValue(String str) {
        return setValue(str, 10);
    }

    /**
     * Sets this {@code MPZ} to {@code op}.
     *
     * @return this {@code MPZ}.
     */
    public MPZ setValue(BigInteger op) {
        return set(op);
    }

    // Interface methods

    /**
     * Compare this {@code MPZ} with {@code op}. Return a positive value if
     * {@code (this > op)}, zero if {@code this = op}, or a negative value if
     * {@code this < op}. This order is compatible with equality.
     */
    @Override
    public int compareTo(MPZ op) {
        return mpz_cmp(mpzNative, op.mpzNative);
    }

    /**
     * Compare this {@code MPZ} with the object {@code op} for equality. It returns
     * {@code true} if and only if {@code op} is an {@code MPZ} with the same value
     * of {@code this}.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof MPZ) {
            var z = (MPZ) obj;
            return mpz_cmp(mpzNative, z.mpzNative) == 0;
        }
        return false;
    }

    /***
     * Return a hash code value for this {@code MPZ}.
     *
     * @implNote Return its {@link intValue}.
     */
    @Override
    public int hashCode() {
        return intValue();
    }

    /**
     * Convert this {@code MPZ} to a signed long. If this number is too big to fit a
     * native signed long, return the least significant part, preserving the sign.
     */
    @Override
    public long longValue() {
        return getSi();
    }

    /**
     * Convert this {@code MPZ} to a signed int, truncating if necessary.
     *
     * @implNote Return the result of {@link longValue} cast to an {@code int}.
     */
    @Override
    public int intValue() {
        return (int) getSi();
    }

    /**
     * Convert this {@code MPZ} to a double, truncating if necessary. If the
     * exponent from the conversion is too big, the result is system dependent. An
     * infinity is returned where available. A hardware overflow trap may or may not
     * occur.
     */
    @Override
    public double doubleValue() {
        return getD();
    }

    /**
     * Convert this {@code MPZ} to a float, truncating if necessary.
     *
     * @implNote Return the result of {@link doubleValue} cast to a {@code float}.
     */
    @Override
    public float floatValue() {
        return (float) getD();
    }

    /**
     * Convert this {@code MPZ} to its string representation in the specified
     * {@code base}, or {@code null} if the base is not valid. See the GMP function
     * <a href="https://gmplib.org/manual/Converting-Integers" target=
     * "_blank">{@code mpz_get_str}</a>.
     */
    public String toString(int base) {
        var s = getStr(base);
        if (s == null)
            throw new IllegalArgumentException(GMP.MSG_INVALID_BASE);
        return s;
    }

    /**
     * Convert this {@code MPZ} to its decimal string representation.
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
        mpzNative = new MpzT();
        mpz_init_set_str(mpzNative, (String) in.readObject(), 62);
        GMP.cleaner.register(mpzNative, new MPZCleaner(mpzNative));
    }

    @SuppressWarnings("unused")
    private void readObjectNoData() throws ObjectStreamException {
        mpzNative = new MpzT();
        mpz_init(mpzNative);
        GMP.cleaner.register(mpzNative, new MPZCleaner(mpzNative));
    }

}
