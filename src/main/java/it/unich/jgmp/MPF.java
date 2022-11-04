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
import com.sun.jna.ptr.NativeLongByReference;

import org.javatuples.Pair;

import it.unich.jgmp.nativelib.MpBitcntT;
import it.unich.jgmp.nativelib.MpExpT;
import it.unich.jgmp.nativelib.MpExpTByReference;
import it.unich.jgmp.nativelib.MpSizeT;
import it.unich.jgmp.nativelib.MpfT;
import it.unich.jgmp.nativelib.NativeUnsignedLong;

/**
 * The class encapsulating the {@code mpf_t} data type, i.e., multi-precision
 * floating point numbers. See the
 * <a href="https://gmplib.org/manual/Floating_002dpoint-Functions" target=
 * "_blank">Floating-point Functions</a> manual page of the GMP manual.
 *
 * <p>
 * An element of {@code MPF} contains a pointer to a native {@code mpf_t}
 * variable and registers itself with {@link GMP#cleaner} for freeing all
 * allocated memory during garbage collection.
 * <p>
 * In determining the names and prototypes of the methods of the {@code MPF}
 * class, we adopted the following rules:
 * <ul>
 * <li>functions {@code mpf_inits}, {@code mpf_clear}, {@code mpf_clears} and
 * {@code mpq_set_prec_raw} are only used internally and are not exposed by the
 * {@code MPF} class;
 * <li>functions in the categories <em>I/O of Floats</em> are not exposed by the
 * {@code MPF} class;
 * <li>if {@code baseName} begins with {@code set} or {@code swap}, we create a
 * method called {@code baseName} which calls the original function, implicitly
 * using {@code this} as the first {@code mpf_t} parameter;
 * <li>if {@code baseName} begins with {@code init}, we create a side-effect
 * free static method (see later);
 * <li>for all the other functions:
 * <ul>
 * <li>if the function has at least a non constant {@code mpf_t} parameter, then
 * we create a method {@code baseNameAssign} which calls the original function,
 * implicitly using {@code this} as the first non-constant {@code mpf_t}
 * parameter;
 * <li>we create a side-effect free method called {@code baseName}, with the
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
 * distinguish between input and output parameters for the GMP function. Some
 * parameters may have both an input and an output nature. The side-effect free
 * method takes all input parameters in its prototype, with the exception of the
 * first input {@code mpf_t} parameter which is mapped to {@code this}. If there
 * are no input {@code mpf_t} parameters, the method will be static. The method
 * creates new objects for the output parameters, eventually cloning the ones
 * also used as an input. After calling the GMP functions, the return value and
 * all the output parameters are returned by the method, eventually packed in a
 * {@link org.javatuples.Tuple}, from left to right according to the function
 * prototype.
 */
public class MPF extends Number implements Comparable<MPF> {

    /**
     * Version for serializability.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The zero multi-precision floating point.
     */
    private static final MPF zero = new MPF();

    /**
     * The pointer to the native {@code mpf_t} object.
     */
    private transient MpfT mpfNative;

    /**
     * Cleaning action for the {@code MPF} class.
     */
    private static class MPFCleaner implements Runnable {
        private MpfT mpfNative;

        MPFCleaner(MpfT mpfNative) {
            this.mpfNative = mpfNative;
        }

        @Override
        public void run() {
            mpf_clear(mpfNative);
        }
    }

    /**
     * A private constructor which build an {@code MPF} starting from a pointer to
     * its native data object. The native object needs to be already initialized.
     */
    private MPF(MpfT pointer) {
        this.mpfNative = pointer;
        GMP.cleaner.register(this, new MPFCleaner(pointer));
    }

    /**
     * Returns the native pointer to the GMP object.
     */
    public MpfT getNative() {
        return mpfNative;
    }

    // Initializing Functions

    /**
     * Set the default precision to be at least {@code prec} bits. Previously
     * initialized variables are unaffected.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    static public void setDefaultPrec(long prec) {
        mpf_set_default_prec(new MpBitcntT(prec));
    }

    /**
     * Returns the default precision actually used.
     *
     * @apiNote return value should be treated as an unsigned long.
     */
    static public long getDefaultPrec() {
        return mpf_get_default_prec().longValue();
    }

    /**
     * Returns an {@code MPF} whose value is zero. The precision of x is undefined
     * unless a default precision has already been established by a call to
     * {@link setDefaultPrec}.
     */
    static public MPF init() {
        return new MPF();
    }

    /**
     * Returns an {@code MPF} whose value is zero, and set its precision to be at
     * least {@code prec} bits.
     *
     * @apiNote {@code prec} should be treated as an unsigned long
     */
    static public MPF init2(long prec) {
        var mpfNative = new MpfT();
        mpf_init2(mpfNative, new MpBitcntT(prec));
        return new MPF(mpfNative);
    }

    /**
     * Returns the current precision of this {@code MPF}, in bits.
     *
     * @apiNote return value should be treated as an unsigned long.
     */
    public long getPrec() {
        return mpf_get_prec(mpfNative).longValue();
    }

    /**
     * Sets the precision of this {@code MPF} to be at least {@code prec} bits. The
     * value will be truncated to the new precision. This function requires a
     * reallocation, and should not be used in a tight loop.
     *
     * @return this {@code MPF}
     *
     * @apiNote {@code prec} should be treated as an unsigned long.
     */
    public MPF setPrec(long prec) {
        mpf_set_prec(mpfNative, new MpBitcntT(prec));
        return this;
    }

    // Assigning Integers

    /**
     * Sets this {@code MPF} to {@code op}.
     *
     * @return this {@code MPF}.
     */
    public MPF set(MPF op) {
        mpf_set(mpfNative, op.mpfNative);
        return this;
    }

    /**
     * Sets this {@code MPF} to {@code op}.
     *
     * @return this {@code MPF}.
     */
    public MPF set(long op) {
        mpf_set_si(mpfNative, new NativeLong(op));
        return this;
    }

    /**
     * Sets this {@code MPF} to {@code op}.
     *
     * @return this {@code MPF}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF setUi(long op) {
        mpf_set_ui(mpfNative, new NativeUnsignedLong(op));
        return this;
    }

    /**
     * Sets this {@code MPF} to {@code op}, truncating if necessary.
     *
     * @throws ArithmeticException if {@code op} is not a finite number. In this
     *                             case, {@code this} is not altered.
     * @return this {@code MPF}.
     */
    public MPF set(double op) {
        if (!Double.isFinite(op))
            throw new ArithmeticException(GMP.MSG_FINITE_DOUBLE_REQUIRED);
        mpf_set_d(mpfNative, op);
        return this;
    }

    /**
     * Sets this {@code MPF} to the truncation of {@code op}.
     */
    public MPF set(MPZ op) {
        mpf_set_z(mpfNative, op.getNative());
        return this;
    }

    /**
     * Sets this {@code MPF} to the truncation of {@code op}.
     */
    public MPF set(MPQ op) {
        mpf_set_q(mpfNative, op.getNative());
        return this;
    }

    /**
     * Sets this {@code MPF} to the number represented by the string {@code str} in
     * the specified {@code base}. See the GMP function
     * <a href="https://gmplib.org/manual/Assigning-Floats" target="
     * _blank">{@code mpf_set_str}</a>. The decimal point character is taken from
     * the current system locale, which may be different from the Java locale.
     *
     * @return 0 if the operation succeeded, -1 otherwise. In the latter case,
     *         {@code this} is not altered.
     */
    public int set(String str, int base) {
        return mpf_set_str(mpfNative, str, base);
    }

    /**
     * Swap the value of this {@code MPF} with the value of {@code op}.
     *
     * @return this {@code MPF}.
     */
    public MPF swap(MPF op) {
        mpf_swap(mpfNative, op.mpfNative);
        return this;
    }

    // Simultaneous Integer Init & Assign

    /**
     * Returns an {@code MPF} whose value is {@code op}. The precision of the result
     * will be taken from the active default precision, as set by
     * {@link setDefaultPrec}.
     *
     * @throws ArithmeticException if {@code op} is not a finite number. In this
     *                             case, {@code this} is not altered.
     *
     */
    public static MPF initSet(MPF op) {
        return new MPF(op);
    }

    /**
     * Returns an {@code MPF} whose value is {@code op}. The precision of the result
     * will be taken from the active default precision, as set by
     * {@link setDefaultPrec}.
     */
    public static MPF initSet(long op) {
        return new MPF(op);
    }

    /**
     * Returns an {@code MPF} whose value is {@code op}. The precision of the result
     * will be taken from the active default precision, as set by
     * {@link setDefaultPrec}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public static MPF initSetUi(long op) {
        var mpfNative = new MpfT();
        mpf_init_set_ui(mpfNative, new NativeUnsignedLong(op));
        return new MPF(mpfNative);
    }

    /**
     * Returns an {@code MPF} whose value is the truncation of {@code op}. The
     * precision of the result will be taken from the active default precision, as
     * set by {@link setDefaultPrec}.
     *
     * @throws ArithmeticException if {@code op} is not a finite number.
     */
    public static MPF initSet(double op) {
        return new MPF(op);
    }

    /**
     * Returns an {@code MPF} whose value is the number represented by the string
     * {@code str} in the specified {@code base}. See the GMP function
     * <a href="https://gmplib.org/manual/Simultaneous-Float-Init-_0026-Assign"
     * target="_blank">{@code mpq_init_set_str}</a>. The decimal point character is
     * taken from the current system locale, which may be different from the Java
     * locale.
     *
     * @return a pair whose first component is {@code 0} if the operation succeeded,
     *         and {@code -1} if either {@code base} is not valid, or {@code str} is
     *         not a valid numeric representation in the specified base. The second
     *         component of the pair is the number represented in {@code str} if the
     *         operation succeeded, {@code 0} otherwise.
     */

    public static Pair<Integer, MPF> initSet(String str, int base) {
        var mpfNative = new MpfT();
        var result = mpf_init_set_str(mpfNative, str, base);
        return new Pair<>(result, new MPF(mpfNative));
    }

    // Converting Integers

    /**
     * Converts this {@code MPF} to a double, truncating if necessary. If the
     * exponent from the conversion is too big, the result is system dependent. An
     * infinity is returned where available. Hardware overflow, underflow and denorm
     * traps may or may not occur.
     */
    public double getD() {
        return mpf_get_d(mpfNative);
    }

    /**
     * Converts this {@code MPF} to a pair made of mantissa and exponent, truncating
     * if necessary. See the GMP function
     * <a href="https://gmplib.org/manual/Converting-Floats" target=
     * "_blank">{@code mpf_get_d_2exp}</a>.
     */
    public Pair<Double, Long> getD2Exp() {
        var pexp = new NativeLongByReference();
        var d = mpf_get_d_2exp(pexp, mpfNative);
        return new Pair<>(d, pexp.getValue().longValue());
    }

    /**
     * Converts this {@code MPF} to an unsigned long, truncating any fraction part.
     *
     * If this number is too big to fit a native unsigned long, the result is
     * undefined.
     *
     * @apiNote the return value should be treated as an unsigned long.
     */
    public long getUi() {
        return mpf_get_ui(mpfNative).longValue();
    }

    /**
     * Converts this {@code MPF} to an signed long, truncating any fraction part.
     *
     * If this number is too big to fit a native unsigned long, the result is
     * undefined.
     *
     */
    public long getSi() {
        return mpf_get_si(mpfNative).longValue();
    }

    /**
     * Returns the String representation of this {@code MPF} in the specified
     * {@code base}, or {@code null} if the base is not valid. See the GMP function
     * <a href="https://gmplib.org/manual/Converting-Floats" target=
     * "_blank">{@code mpf_get_str}</a>.
     */
    public Pair<String, Long> getStr(int base, long nDigits) {
        var expR = new MpExpTByReference();
        Pointer ps = mpf_get_str(null, expR, base, new MpSizeT(nDigits), mpfNative);
        if (ps == null)
            return null;
        var s = ps.getString(0);
        Native.free(Pointer.nativeValue(ps));
        return new Pair<>(s, expR.getValue().longValue());
    }

    // Integer Arithmetic

    /**
     * Sets this {@code MPF} to {@code (op1 + op2)}.
     *
     * @return this {@code MPF}.
     */
    public MPF addAssign(MPF op1, MPF op2) {
        mpf_add(mpfNative, op1.mpfNative, op2.mpfNative);
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is {@code (this + op)}.
     */
    public MPF add(MPF op) {
        return new MPF().addAssign(this, op);
    }

    /**
     * Sets this {@code MPF} to {@code (op1 + op2)}.
     *
     * @return this {@code MPF}.
     *
     * @apiNote {@code op2} should be treated as an unsigned long.
     */
    public MPF addUiAssign(MPF op1, long op2) {
        mpf_add_ui(mpfNative, op1.mpfNative, new NativeUnsignedLong(op2));
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is {@code (this + op)}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF addUi(long op) {
        return new MPF().addUiAssign(this, op);
    }

    /**
     * Sets this {@code MPF} to {@code (op1 - op2)}.
     *
     * @return this {@code MPF}.
     */
    public MPF subAssign(MPF op1, MPF op2) {
        mpf_sub(mpfNative, op1.mpfNative, op2.mpfNative);
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is {@code (this - op)}.
     */
    public MPF sub(MPF op) {
        return new MPF().subAssign(this, op);
    }

    /**
     * Sets this {@code MPF} to {@code (op1 - op2)}.
     *
     * @return this {@code MPF}.
     *
     * @apiNote {@code op2} should be treated as an unsigned long.
     */
    public MPF subUiAssign(MPF op1, long op2) {
        mpf_sub_ui(mpfNative, op1.mpfNative, new NativeUnsignedLong(op2));
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is {@code (this - op)}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF subUi(long op) {
        return new MPF().subUiAssign(this, op);
    }

    /**
     * Sets this {@code MPF} to {@code (op1 - op2)}.
     *
     * @return this {@code MPF}.
     *
     * @apiNote {@code op1} should be treated as an unsigned long.
     */
    public MPF uiSubAssign(long op1, MPF op2) {
        mpf_ui_sub(mpfNative, new NativeUnsignedLong(op1), op2.mpfNative);
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is {@code (op - this)}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF uiSub(long op) {
        return new MPF().uiSubAssign(op, this);
    }

    /**
     * Sets this {@code MPF} to {@code (op1 * op2)}.
     *
     * @return this {@code MPF}.
     */
    public MPF mulAssign(MPF op1, MPF op2) {
        mpf_mul(mpfNative, op1.mpfNative, op2.mpfNative);
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is {@code (this * op)}.
     */
    public MPF mul(MPF op) {
        return new MPF().mulAssign(this, op);
    }

    /**
     * Sets this {@code MPF} to {@code (op1 * op2)}.
     *
     * @return this {@code MPF}.
     *
     * @apiNote {@code op2} should be treated as an unsigned long.
     */
    public MPF mulUiAssign(MPF op1, long op2) {
        mpf_mul_ui(mpfNative, op1.mpfNative, new NativeUnsignedLong(op2));
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is {@code (this * op)}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF mulUi(long op) {
        return new MPF().mulUiAssign(this, op);
    }

    /**
     * Sets this {@code MPF} to {@code (op1 / op2)}.
     *
     * @throws ArithmeticException if {@code op2} is zero.
     *
     * @return this {@code MPF}.
     */
    public MPF divAssign(MPF op1, MPF op2) {
        if (op2.isZero())
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        mpf_div(mpfNative, op1.mpfNative, op2.mpfNative);
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is {@code (this / op)}.
     *
     * @throws ArithmeticException if {@code op2} is zero.
     */
    public MPF div(MPF op) {
        return new MPF().divAssign(this, op);
    }

    /**
     * Sets this {@code MPF} to {@code (op1 / op2)}.
     *
     * @throws ArithmeticException if {@code op2} is zero.
     *
     * @return this {@code MPF}.
     *
     * @apiNote {@code op2} should be treated as an unsigned long.
     */
    public MPF divUiAssign(MPF op1, long op2) {
        if (op2 == 0l)
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        mpf_div_ui(mpfNative, op1.mpfNative, new NativeUnsignedLong(op2));
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is {@code (this / op)}.
     *
     * @throws ArithmeticException if {@code op2} is zero.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF divUi(long op) {
        return new MPF().divUiAssign(this, op);
    }

    /**
     * Sets this {@code MPF} to {@code (op1 / op2)}.
     *
     * @throws ArithmeticException if {@code op2} is zero.
     *
     * @return this {@code MPF}.
     *
     * @apiNote {@code op1} should be treated as an unsigned long.
     */
    public MPF uiDivAssign(long op1, MPF op2) {
        if (op2.isZero())
            throw new ArithmeticException(GMP.MSG_DIVIDE_BY_ZERO);
        mpf_ui_div(mpfNative, new NativeUnsignedLong(op1), op2.mpfNative);
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is {@code (op / this)}.
     *
     * @throws ArithmeticException if {@code op2} is zero.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF uiDiv(long op) {
        return new MPF().uiDivAssign(op, this);
    }

    /**
     * Sets this {@code MPF} to the the square root of {@code op}.
     *
     * @return this {@code MPF}.
     */
    public MPF sqrtAssign(MPF op) {
        mpf_sqrt(mpfNative, op.mpfNative);
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is the square root of {@code this}.
     */
    public MPF sqrt() {
        return new MPF().sqrtAssign(this);
    }

    /**
     * Sets this {@code MPF} to the the square root of {@code op}.
     *
     * @return this {@code MPF}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF sqrtUiAssign(long op) {
        mpf_sqrt_ui(mpfNative, new NativeUnsignedLong(op));
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is the square root of {@code this}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public static MPF sqrtUi(long op) {
        return new MPF().sqrtUiAssign(op);
    }

    /**
     * Sets this {@code MPF} to <code>(base<sup>exp</sup>)</code>. The case
     * <code>0<sup>0</sup></code> yields {@code 1}.
     *
     * @return this {@code MPF}.
     *
     * @apiNote {@code exp} should be treated as an unsigned long.
     */
    public MPF powUiAssign(MPF base, long exp) {
        mpf_pow_ui(mpfNative, base.mpfNative, new NativeUnsignedLong(exp));
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is <code>(this<sup>exp</sup>)</code>. The
     * case <code>0<sup>0</sup></code> yields {@code 1}.
     *
     * @apiNote {@code exp} should be treated as an unsigned long.
     */
    public MPF powUi(long exp) {
        return new MPF().powUiAssign(this, exp);
    }

    /**
     * Sets this {@code MPF} to {@code (- op)}.
     *
     * @return this {@code MPF}.
     */
    public MPF negAssign(MPF op) {
        mpf_neg(mpfNative, op.mpfNative);
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is the quotient of {@code (- this)}.
     */
    public MPF neg() {
        return new MPF().negAssign(this);
    }

    /**
     * Sets this {@code MPF} to the absolute value of {@code op}.
     *
     * @return this {@code MPF}.
     */
    public MPF absAssign(MPF op) {
        mpf_abs(mpfNative, op.mpfNative);
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is the absolute value of {@code this}.
     */
    public MPF abs() {
        return new MPF().absAssign(this);
    }

    /**
     * Sets this {@code MPF} to <code>(op * 2<sup>b</sup>)</code>.
     *
     * @return this {@code MPF}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPF mul2ExpAssign(MPF op, long b) {
        mpf_mul_2exp(mpfNative, op.mpfNative, new MpBitcntT(b));
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is <code>(this * 2<sup>b</sup>)</code>.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPF mul2Exp(long b) {
        return new MPF().mul2ExpAssign(this, b);
    }

    /**
     * Sets this {@code MPF} to <code>(op / 2<sup>b</sup>)</code>.
     *
     * @return this {@code MPF}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPF div2ExpAssign(MPF op, long b) {
        mpf_div_2exp(mpfNative, op.mpfNative, new MpBitcntT(b));
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is <code>(this * 2<sup>b</sup>)</code>.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPF div2Exp(long b) {
        return new MPF().div2ExpAssign(this, b);
    }

    // Comparison Functions

    /**
     * Compares {@code this} with {@code op}. Returns a positive value if
     * {@code (this > op)}, zero if {@code this = op}, or a negative value if
     * {@code this < op}.
     */
    public int cmp(MPF op) {
        return mpf_cmp(mpfNative, op.mpfNative);
    }

    /**
     * Compares {@code this} with {@code op}. Returns a positive value if
     * {@code (this > op)}, zero if {@code this = op}, or a negative value if
     * {@code this < op}. The value of {@code op} may be infinite, but the result is
     * undefined on NaNs.
     */
    public int cmp(MPZ op) {
        return mpf_cmp_z(mpfNative, op.getNative());
    }

    /**
     * Compares {@code this} with {@code op}. Returns a positive value if
     * {@code (this > op)}, zero if {@code this = op}, or a negative value if
     * {@code this < op}. The value of {@code op} may be infinite, but the result is
     * undefined on NaNs.
     *
     * @throws ArithmeticException if {@code op} is a NaN.
     */
    public int cmp(double op) {
        if (Double.isNaN(op))
            throw new ArithmeticException(GMP.MSG_NAN_NOT_ALLOWED);
        return mpf_cmp_d(mpfNative, op);
    }

    /**
     * Compares {@code this} with {@code op}. Returns a positive value if
     * {@code (this > op)}, zero if {@code this = op}, or a negative value if
     * {@code this < op}.
     */
    public int cmp(long op) {
        return mpf_cmp_si(mpfNative, new NativeLong(op));
    }

    /**
     * Compares {@code this} with {@code op}. Returns a positive value if
     * {@code (this > op)}, zero if {@code this = op}, or a negative value if
     * {@code this < op}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public int cmpUi(long op) {
        return mpf_cmp_ui(mpfNative, new NativeUnsignedLong(op));
    }

    /**
     * Sets this {@code MPF} to the relative difference between {@code op1} and
     * {@code op2}, i.e., {@code (abs(op1-op2)/op1)}.
     */
    public MPF reldiffAssign(MPF op1, MPF op2) {
        mpf_reldiff(mpfNative, op1.mpfNative, op2.mpfNative);
        return this;
    }

    /**
     * Return the relative difference between {@code this} and {@code this}, i.e.,
     * {@code (abs(this-op)/this)}.
     */
    public MPF reldiff(MPF op) {
        return new MPF().reldiffAssign(this, op);
    }

    /**
     * Returns {@code +1} if {@code (this > 0)}, {@code 0} if {@code (this = 0)} and
     * {@code -1} if {@code this < 0}.
     */
    public int sgn() {
        return mpf_sgn(mpfNative);
    }

    // Miscellaneous Functions Functions

    /**
     * Sets this {@code MPF} to the value of {@code op} rounded to the next higher
     * integer.
     *
     * @return this {@code MPF}.
     */
    public MPF ceilAssign(MPF op) {
        mpf_ceil(mpfNative, op.mpfNative);
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is {@code this} rounded to the next higher
     * integer.
     */
    public MPF ceil() {
        return new MPF().ceilAssign(this);
    }

    /**
     * Sets this {@code MPF} to the value of {@code op} rounded to the next lower
     * integer.
     *
     * @return this {@code MPF}.
     */
    public MPF floorAssign(MPF op) {
        mpf_floor(mpfNative, op.mpfNative);
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is {@code this} rounded to the next lower
     * integer.
     */
    public MPF floor() {
        return new MPF().floorAssign(this);
    }

    /**
     * Sets this {@code MPF} to the value of {@code op} rounded towards zero.
     *
     * @return this {@code MPF}.
     */
    public MPF truncAssign(MPF op) {
        mpf_trunc(mpfNative, op.mpfNative);
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is {@code this} rounded towards zero.
     */
    public MPF trunc() {
        return new MPF().truncAssign(this);
    }

    /**
     * Returns whether this {@code MPF} is an integer.
     */
    public boolean isInteger() {
        return mpf_integer_p(mpfNative);
    }

    /**
     * Returns {@code true} if and only if this {@code MPF} fits into a native
     * unsigned long.
     */
    public boolean fitsUlong() {
        return mpf_fits_ulong_p(mpfNative);
    }

    /**
     * Returns {@code true} if and only if this {@code MPF} fits into a native
     * signed long.
     */
    public boolean fitsSlong() {
        return mpf_fits_slong_p(mpfNative);
    }

    /**
     * Returns {@code true} if and only if this {@code MPF} fits into a native
     * unsigned int.
     */
    public boolean fitsUint() {
        return mpf_fits_uint_p(mpfNative);
    }

    /**
     * Returns {@code true} if and only if this {@code MPF} fits into a native
     * signed int.
     */
    public boolean fitsSint() {
        return mpf_fits_sint_p(mpfNative);
    }

    /**
     * Returns {@code true} if and only if this {@code MPF} fits into a native
     * unsigned short.
     */
    public boolean fitsUshort() {
        return mpf_fits_ushort_p(mpfNative);
    }

    /**
     * Returns {@code true} if and only if this {@code MPF} fits into a native
     * signed short.
     */
    public boolean fitsSshort() {
        return mpf_fits_sshort_p(mpfNative);
    }

    /**
     * Returns true if and only if this {@code this} MPF is zero.
     */
    public boolean isZero() {
        return mpf_cmp(mpfNative, zero.mpfNative) == 0;
    }

    /**
     * Sets this {@code MPF} to a uniformly distributed random float in the range
     * from {@code 0} included to {@code 1} excluded, with {@code nbits} significant
     * bits in the mantissa, or less if the precision of this {@code MPF} is
     * smaller.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public MPF urandombAssign(RandState s, long nbits) {
        mpf_urandomb(mpfNative, s.getNative(), new MpBitcntT(nbits));
        return this;
    }

    /**
     * Returns an {@code MPF} whose value is an uniformly distributed random integer
     * in the range {@code 0}} to <code>(2<sup>n</sup> - 1)</code>, inclusive.
     *
     * @apiNote {@code n} should be treated as an unsigned long.
     */
    public static MPF urandomb(RandState s, long n) {
        return new MPF().urandombAssign(s, n);
    }

    /**
     * Sets this {@code MPF} to a random integer of at most {@code maxSize} limbs,
     * with long strings of zeros and ones in the binary representation. The
     * exponent of the number is in the interval {@code -exp} to {@code exp} (in
     * limbs). This function is useful for testing functions and algorithms, since
     * these kind of random numbers have proven to be more likely to trigger
     * corner-case bugs. Negative random numbers are generated when {@code maxSize}
     * is negative.
     *
     * @return this {@code MPF}.
     */
    public MPF random2Assign(long maxSize, long exp) {
        mpf_random2(mpfNative, new MpSizeT(maxSize), new MpExpT(exp));
        return this;
    }

    /**
     * Returns an {@code MPF} whose valie is a random integer of at most
     * {@code maxSize} limbs, with long strings of zeros and ones in the binary
     * representation. The exponent of the number is in the interval {@code -exp} to
     * {@code exp} (in limbs). This function is useful for testing functions and
     * algorithms, since these kind of random numbers have proven to be more likely
     * to trigger corner-case bugs. Negative random numbers are generated when
     * {@code maxSize} is negative.
     */
    public static MPF random2(long maxSize, long exp) {
        return (new MPF()).random2Assign(maxSize, exp);
    }

    // Java name aliases

    /**
     * Builds an {@code MPF} whose value is zero.
     */
    public MPF() {
        mpfNative = new MpfT();
        mpf_init(mpfNative);
        GMP.cleaner.register(this, new MPFCleaner(mpfNative));
    }

    /**
     * Builds an {@code MPF} whose value is {@code op}, possibly truncated to the
     * default precision.
     */
    public MPF(MPF op) {
        mpfNative = new MpfT();
        mpf_init_set(mpfNative, op.mpfNative);
        GMP.cleaner.register(this, new MPFCleaner(mpfNative));
    }

    /**
     * Builds an {@code MPF} whose value is {@code op}, possibly truncated to the
     * default precision.
     */
    public MPF(long op) {
        mpfNative = new MpfT();
        mpf_init_set_si(mpfNative, new NativeLong(op));
        GMP.cleaner.register(this, new MPFCleaner(mpfNative));
    }

    /**
     * Builds an {@code MPF} whose value is {@code op}, possibly truncated to the
     * default precision.
     *
     * @throws ArithmeticException if {@code op} is not a finite number.
     */
    public MPF(double op) {
        if (!Double.isFinite(op))
            throw new ArithmeticException(GMP.MSG_FINITE_DOUBLE_REQUIRED);
        mpfNative = new MpfT();
        mpf_init_set_d(mpfNative, op);
        GMP.cleaner.register(this, new MPFCleaner(mpfNative));
    }

    /**
     * Builds an {@code MPF} whose value is {@code op}, possibly truncated to the
     * default precision.
     */
    public MPF(MPQ op) {
        mpfNative = new MpfT();
        mpf_init(mpfNative);
        mpf_set_q(mpfNative, op.getNative());
        GMP.cleaner.register(this, new MPFCleaner(mpfNative));
    }

    /**
     * Builds an {@code MPF} whose value is {@code op}, possibly truncated to the
     * default precision.
     */
    public MPF(MPZ op) {
        mpfNative = new MpfT();
        mpf_init(mpfNative);
        mpf_set_z(mpfNative, op.getNative());
        GMP.cleaner.register(this, new MPFCleaner(mpfNative));
    }

    /**
     * Builds an {@code MPF} whose value is the number represented by the string
     * {@code str} in the specified {@code base}, possibly truncated to the default
     * precision. See the GMP function
     * <a href="https://gmplib.org/manual/Simultaneous-Float-Init-_0026-Assign"
     * target="_blank">{@code mpf_init_set_str}</a>.
     *
     * @throws IllegalArgumentException if either {@code base} is not valid or
     *                                  {@code str} is not a valid string in the
     *                                  specified {@code base}.
     *
     */
    public MPF(String str, int base) {
        mpfNative = new MpfT();
        String strCorrect = str;
        if (!GMP.getDecimalSeparator().equals("."))
            if (str.indexOf(GMP.getDecimalSeparator()) == -1)
                strCorrect = str.replace(".", GMP.getDecimalSeparator());
            else
                throw new IllegalArgumentException(GMP.MSG_INVALID_STRING_CONVERSION);
        int result = mpf_init_set_str(mpfNative, strCorrect, base);
        if (result == -1) {
            mpf_clear(mpfNative);
            throw new IllegalArgumentException(GMP.MSG_INVALID_STRING_CONVERSION);
        }
        GMP.cleaner.register(this, new MPFCleaner(mpfNative));
    }

    /**
     * Builds an {@code MPF} whose value is the number represented by the string
     * {@code str} in decimal base, possibly truncated to the default precision. See
     * the GMP function
     * <a href="https://gmplib.org/manual/Simultaneous-Float-Init-_0026-Assign"
     * target="_blank">{@code mpf_init_set_str}</a>.
     *
     * @throws IllegalArgumentException if {@code str} is not a valid number
     *                                  representation in decimal base.
     */
    public MPF(String str) {
        this(str, 10);
    }

    /**
     * Sets this {@code MPF} to {@code op}, possibly truncated according to
     * precision.
     */
    public MPF setValue(MPF op) {
        return set(op);
    }

    /**
     * Sets this {@code MPF} to {@code op}, possibly truncated according to
     * precision.
     */
    public MPF setValue(long op) {
        return set(op);
    }

    /**
     * Sets this {@code MPF} to {@code op}, possibly truncated according to
     * precision.
     *
     * @throws ArithmeticException if {@code op} is not a finite number. In this
     *                             case, {@code this} is not altered.
     */
    public MPF setValue(double op) {
        return set(op);
    }

    /**
     * Sets this {@code MPF} to {@code op}, possibly truncated according to
     * precision.
     */
    public MPF setValue(MPQ op) {
        return set(op);
    }

    /**
     * Sets this {@code MPF} to {@code op}, possibly truncated according to
     * precision.
     */
    public MPF setValue(MPZ op) {
        return set(op);
    }

    /**
     * Set this {@code MPF} to the number represented by the string {@code str} in
     * the specified {@code base}. See the GMP function
     * <a href="https://gmplib.org/manual/Assigning-Floats" target="
     * _blank">{@code mpf_set_str}</a>. The decimal point character is taken from
     * the current system locale, which may be different from the Java locale.
     *
     * @throws IllegalArgumentException if either {@code base} is not valid or
     *                                  {@code str} is not a valid number
     *                                  representation in the specified base. In
     *                                  this case, {@code this} is not altered.
     */
    public MPF setValue(String str, int base) {
        var result = set(str, base);
        if (result == -1)
            throw new IllegalArgumentException(GMP.MSG_INVALID_STRING_CONVERSION);
        return this;
    }

    /**
     * Set this {@code MPF} to the value represented by the string {@code str} in
     * decimal base. The decimal point character is taken from the current system
     * locale, which may be different from the Java locale.
     *
     * @throws IllegalArgumentException if {@code str} is not a valid number
     *                                  representation in decimal base.
     * @see setValue(String, int)
     */
    public MPF setValue(String str) {
        var result = set(str, 10);
        if (result == -1)
            throw new IllegalArgumentException(GMP.MSG_INVALID_DECIMAL_STRING_CONVERSION);
        return this;
    }

    /**
     * Compares this {@code MPF} with {@code op}. Returns a positive value if
     * {@code (this > op)}, zero if {@code this = op}, or a negative value if
     * {@code this < op}. This order is compatible with equality.
     */
    @Override
    public int compareTo(MPF op) {
        return mpf_cmp(mpfNative, op.mpfNative);
    }

    /**
     * Compares this {@code MPF} with the object {@code op} for equality. It returns
     * {@code true} if and only if {@code op} is an {@code MPF} with the same value
     * of {@code this}.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof MPF) {
            var z = (MPF) obj;
            return mpf_cmp(mpfNative, z.mpfNative) == 0;
        }
        return false;
    }

    /***
     * Returns a hash code value for this {@code MPF}.
     */
    @Override
    public int hashCode() {
        return intValue();
    }

    /**
     * Converts this {@code MPF} to a signed long.
     *
     * If this number is too big to fit a signed long, return the least significant
     * part, preserving the sign.
     */
    @Override
    public long longValue() {
        return getSi();
    }

    /**
     * Converts this {@code MPF} to a signed int.
     *
     * If this number is too big to fit a signed long, return the least significant
     * part, preserving the sign.
     */
    @Override
    public int intValue() {
        return (int) getSi();
    }

    /**
     * Converts this {@code MPF} to a double, truncating if necessary.
     *
     * @see getD
     */
    @Override
    public double doubleValue() {
        return getD();
    }

    /**
     * Converts this {@code MPF} to a float, truncating if necessary.
     *
     * @see getD
     */
    @Override
    public float floatValue() {
        return (float) getD();
    }

    /**
     * Converts this {@code MPF} to its string representation in the specified
     * {@code base}, or {@code null} if the base is not valid. See the GMP function
     * <a href="https://gmplib.org/manual/Converting-Floats" target=
     * "_blank">{@code mpf_get_str}</a>.
     */
    public String toString(int base, long nDigits) {
        var t = getStr(base, nDigits);
        var mantissa = t.getValue0();
        var position = t.getValue1().intValue();
        if (mantissa.length() == 0)
            return "0";
        var isNegative = mantissa.charAt(0) == '-';
        if (position >= 0) {
            if (isNegative)
                position += 1;
            return mantissa.substring(0, position) + "." + mantissa.substring(position);
        } else if (isNegative)
            return "-0." + "0".repeat(-position) + mantissa.substring(1);
        else
            return "0." + "0".repeat(-position) + mantissa;
    }

    /**
     * Converts this {@code MPF} to its string representation in the specified
     * {@code base}, or {@code null} if the base is not valid. See the GMP function
     * <a href="https://gmplib.org/manual/Converting-Floats" target=
     * "_blank">{@code mpf_get_str}</a>.
     */
    public String toString(int base) {
        return toString(base, 0);
    }

    /**
     * Converts this {@code MPF} to its decimal string representation.
     */
    @Override
    public String toString() {
        return toString(10, 0);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        // writeUTF seems more efficient, but has a limit of 64Kb
        // use base 62 to have a more compact representation
        out.writeObject(toString(62));
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        mpfNative = new MpfT();
        var s = (String) in.readObject();
        mpf_init_set_str(mpfNative, s.replace(".", decimalSeparator), 62);
        GMP.cleaner.register(this, new MPFCleaner(mpfNative));
    }

    @SuppressWarnings("unused")
    private void readObjectNoData() throws ObjectStreamException {
        mpfNative = new MpfT();
        mpf_init(mpfNative);
        GMP.cleaner.register(this, new MPFCleaner(mpfNative));
    }

}
