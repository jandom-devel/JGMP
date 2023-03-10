/**
* Copyright 2022, 2023 Gianluca Amato <gianluca.amato@unich.it>
*        and Francesca Scozzari <francesca.scozzari@unich.it>
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
import java.math.BigDecimal;

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
 * Multi-precision floating point numbers. This class encapsulates the
 * {@code mpf_t} data type, see the
 * <a href="https://gmplib.org/manual/Floating_002dpoint-Functions" target=
 * "_blank">Floating-point Functions</a> page of the GMP manual. In determining
 * the names and signatures of the methods of the {@code MPF} class, we adopt
 * the rules described in the documentation of the {@link it.unich.jgmp}
 * package, enriched with the following ones:
 * <ul>
 * <li>the function {@code mpf_set_prec_raw} is not exposed by the {@code MPF}
 * class;
 * <li>the functions in the "<em>I/O of Floats</em>" category are not exposed by
 * the {@code MPF} class.
 * </ul>
 * <p>
 * Every method which returns a new {@code MPF}, use the default precision set
 * by the {@link setDefaultPrec} method, with the excpetion of the {@link init2}
 * method where precision is specificied explicitly.
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
     * Return the native pointer to the GMP object.
     */
    public MpfT getNative() {
        return mpfNative;
    }

    // Initializing Functions

    /**
     * Set the default precision to be at least {@code prec} bits. Previously
     * initialized variables are unaffected.
     *
     * @apiNote {@code prec} should be treated as an unsigned long.
     */
    static public void setDefaultPrec(long prec) {
        mpf_set_default_prec(new MpBitcntT(prec));
    }

    /**
     * Return the default precision actually used.
     *
     * @apiNote the return value should be treated as an unsigned long.
     */
    static public long getDefaultPrec() {
        return mpf_get_default_prec().longValue();
    }

    /**
     * Return an {@code MPF} whose value is zero. The precision of the result will
     * be taken from the active default precision, as set by {@link setDefaultPrec}.
     */
    static public MPF init() {
        var mpfNative = new MpfT();
        mpf_init(mpfNative);
        return new MPF(mpfNative);
    }

    /**
     * Return an {@code MPF} whose value is zero, and set its precision to be at
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
     * Return the current precision of this {@code MPF}, in bits.
     *
     * @apiNote the return value should be treated as an unsigned long.
     */
    public long getPrec() {
        return mpf_get_prec(mpfNative).longValue();
    }

    /**
     * Set the precision of this {@code MPF} to be at least {@code prec} bits. The
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
     * Set this {@code MPF} to {@code op}, possibly truncated according to
     * precision.
     *
     * @return this {@code MPF}.
     */
    public MPF set(MPF op) {
        mpf_set(mpfNative, op.mpfNative);
        return this;
    }

    /**
     * Set this {@code MPF} to {@code op}, possibly truncated according to
     * precision.
     *
     * @return this {@code MPF}.
     */
    public MPF set(long op) {
        mpf_set_si(mpfNative, new NativeLong(op));
        return this;
    }

    /**
     * Set this {@code MPF} to {@code op}, possibly truncated according to
     * precision.
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
     * Set this {@code MPF} to {@code op}, possibly truncated according to
     * precision.
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
     * Set this {@code MPF} to {@code op}, possibly truncated according to
     * precision.
     *
     * @return this {@code MPF}.
     */
    public MPF set(MPZ op) {
        mpf_set_z(mpfNative, op.getNative());
        return this;
    }

    /**
     * Set this {@code MPF} to {@code op}, possibly truncated according to
     * precision.
     *
     * @return this {@code MPF}.
     */
    public MPF set(MPQ op) {
        mpf_set_q(mpfNative, op.getNative());
        return this;
    }

    /**
     * Set this {@code MPF} to the number represented by the string {@code str} in
     * the specified {@code base}, possibly truncated according to precision. See
     * the GMP function
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
     * Set this {@code MPF} to the big decimal {@code op}. Note that, since
     * {@code BigDecimal} represents number in thedecimal base while {@code MPF} use
     * the binary base, rounding is possible.
     *
     * @return this {@code MPF}.
     */
    public MPF set(BigDecimal op) {
        var z = new MPZ(op.unscaledValue());
        set(z);
        var opScale = op.scale();
        var scale = new MPF(10).powUi(Math.abs(opScale));
        if (opScale >= 0)
            divAssign(scale);
        else
            mulAssign(scale);
        return this;
    }

    /**
     * Swap the value of this {@code MPF} with the value of {@code op}. Both the
     * value and the precision of the two objects are swapped.
     *
     * @return this {@code MPF}.
     */
    public MPF swap(MPF op) {
        mpf_swap(mpfNative, op.mpfNative);
        return this;
    }

    // Simultaneous Integer Init & Assign

    /**
     * Return an {@code MPF} whose value is {@code op}, possibly truncated to the
     * default precision.
     *
     * @throws ArithmeticException if {@code op} is not a finite number. In this
     *                             case, {@code this} is not altered.
     *
     */
    public static MPF initSet(MPF op) {
        var mpfNative = new MpfT();
        mpf_init_set(mpfNative, op.mpfNative);
        return new MPF(mpfNative);
    }

    /**
     * Return an {@code MPF} whose value is {@code op}, possibly truncated to the
     * default precision.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public static MPF initSetUi(long op) {
        var mpfNative = new MpfT();
        mpf_init_set_ui(mpfNative, new NativeUnsignedLong(op));
        return new MPF(mpfNative);
    }

    /**
     * Return an {@code MPF} whose value is {@code op}, possibly truncated to the
     * default precision.
     */
    public static MPF initSet(long op) {
        var mpfNative = new MpfT();
        mpf_init_set_si(mpfNative, new NativeLong(op));
        return new MPF(mpfNative);
    }

    /**
     * Return an {@code MPF} whose value is {@code op}, possibly truncated to the
     * default precision.
     *
     * @throws ArithmeticException if {@code op} is not a finite number.
     */
    public static MPF initSet(double op) {
        if (!Double.isFinite(op))
            throw new ArithmeticException(GMP.MSG_FINITE_DOUBLE_REQUIRED);
        var mpfNative = new MpfT();
        mpf_init_set_d(mpfNative, op);
        return new MPF(mpfNative);
    }

    /**
     * Return an {@code MPF} whose value is the number represented by the string
     * {@code str} in the specified {@code base}, possibly truncated to the default
     * precision. See the GMP function
     * <a href="https://gmplib.org/manual/Simultaneous-Float-Init-_0026-Assign"
     * target="_blank">{@code mpf_init_set_str}</a>. The decimal point character is
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

    // Converting floats

    /**
     * Convert this {@code MPF} to a double, truncating if necessary. If the
     * exponent from the conversion is too big or too small, the result is system
     * dependent. For too big an infinity is returned when available. For too small
     * 0.0 is normally returned. Hardware overflow, underflow and denorm traps may
     * or may not occur.
     */
    public double getD() {
        return mpf_get_d(mpfNative);
    }

    /**
     * Convert this {@code MPF} to a pair made of mantissa and exponent, truncating
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
     * Convert this {@code MPF} to an unsigned long, truncating any fraction part.
     * If this number is too big to fit an unsigned long, the result is undefined.
     *
     * @apiNote the return value should be treated as an unsigned long.
     */
    public long getUi() {
        return mpf_get_ui(mpfNative).longValue();
    }

    /**
     * Convert this {@code MPF} to a long, truncating any fraction part. If this
     * number is too big to fit a long, the result is undefined.
     */
    public long getSi() {
        return mpf_get_si(mpfNative).longValue();
    }

    /**
     * Return the String representation of this {@code MPF} in the specified
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

    /**
     * Convert {@code this} to a {@code BigDecimal}.
     */
    public BigDecimal getBigDecimal() {
        var strPair = getStr(10, 0);
        var exp = strPair.getValue1();
        var str = strPair.getValue0();
        exp -= str.charAt(0) == '-' ? str.length() - 1 : str.length();
        str += "E" + exp;
        return new BigDecimal(str);
    }

    // Integer Arithmetic

    /**
     * Set this {@code MPF} to {@code (op1 + op2)}.
     *
     * @return this {@code MPF}.
     */
    public MPF addAssign(MPF op1, MPF op2) {
        mpf_add(mpfNative, op1.mpfNative, op2.mpfNative);
        return this;
    }

    /**
     * Set this {@code MPF} to {@code (this + op)}
     *
     * @return this {@code MPF}
     */
    public MPF addAssign(MPF op) {
        return addAssign(this, op);
    }

    /**
     * Return an {@code MPF} whose value is {@code (this + op)}.
     */
    public MPF add(MPF op) {
        return new MPF().addAssign(this, op);
    }

    /**
     * Set this {@code MPF} to {@code (op1 + op2)}.
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
     * Set this {@code MPF} to {@code (this + op)}
     *
     * @return this {@code MPF}
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF addUiAssign(long op) {
        return addUiAssign(this, op);
    }

    /**
     * Return an {@code MPF} whose value is {@code (this + op)}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF addUi(long op) {
        return new MPF().addUiAssign(this, op);
    }

    /**
     * Set this {@code MPF} to {@code (op1 - op2)}.
     *
     * @return this {@code MPF}.
     */
    public MPF subAssign(MPF op1, MPF op2) {
        mpf_sub(mpfNative, op1.mpfNative, op2.mpfNative);
        return this;
    }

    /**
     * Set this {@code MPF} to {@code (this - op)}
     *
     * @return this {@code MPF}
     */
    public MPF subAssign(MPF op) {
        return subAssign(this, op);
    }

    /**
     * Return an {@code MPF} whose value is {@code (this - op)}.
     */
    public MPF sub(MPF op) {
        return new MPF().subAssign(this, op);
    }

    /**
     * Set this {@code MPF} to {@code (op1 - op2)}.
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
     * Set this {@code MPF} to {@code (this - op)}
     *
     * @return this {@code MPF}
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF subUiAssign(long op) {
        return subUiAssign(this, op);
    }

    /**
     * Return an {@code MPF} whose value is {@code (this - op)}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF subUi(long op) {
        return new MPF().subUiAssign(this, op);
    }

    /**
     * Set this {@code MPF} to {@code (op1 - op2)}.
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
     * Set this {@code MPF} to {@code (op - this)}
     *
     * @return this {@code MPF}
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF uiSubAssign(long op) {
        return uiSubAssign(op, this);
    }

    /**
     * Return an {@code MPF} whose value is {@code (op - this)}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF uiSub(long op) {
        return new MPF().uiSubAssign(op, this);
    }

    /**
     * Set this {@code MPF} to {@code (op1 * op2)}.
     *
     * @return this {@code MPF}.
     */
    public MPF mulAssign(MPF op1, MPF op2) {
        mpf_mul(mpfNative, op1.mpfNative, op2.mpfNative);
        return this;
    }

    /**
     * Set this {@code MPF} to {@code (this * op)}
     *
     * @return this {@code MPF}
     */
    public MPF mulAssign(MPF op) {
        return mulAssign(this, op);
    }

    /**
     * Return an {@code MPF} whose value is {@code (this * op)}.
     */
    public MPF mul(MPF op) {
        return new MPF().mulAssign(this, op);
    }

    /**
     * Set this {@code MPF} to {@code (op1 * op2)}.
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
     * Set this {@code MPF} to {@code (this * op)}
     *
     * @return this {@code MPF}
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF mulUiAssign(long op) {
        return mulUiAssign(this, op);
    }

    /**
     * Return an {@code MPF} whose value is {@code (this * op)}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF mulUi(long op) {
        return new MPF().mulUiAssign(this, op);
    }

    /**
     * Set this {@code MPF} to {@code (op1 / op2)}.
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
     * Set this {@code MPF} to {@code (this / op)}
     *
     * @throws ArithmeticException if {@code op} is zero.
     *
     * @return this {@code MPF}
     */
    public MPF divAssign(MPF op) {
        return divAssign(this, op);
    }

    /**
     * Return an {@code MPF} whose value is {@code (this / op)}.
     *
     * @throws ArithmeticException if {@code op} is zero.
     */
    public MPF div(MPF op) {
        return new MPF().divAssign(this, op);
    }

    /**
     * Set this {@code MPF} to {@code (op1 / op2)}.
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
     * Set this {@code MPF} to {@code (this / op)}
     *
     * @throws ArithmeticException if {@code op} is zero.
     *
     * @return this {@code MPF}
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF divUiAssign(long op) {
        return divUiAssign(this, op);
    }

    /**
     * Return an {@code MPF} whose value is {@code (this / op)}.
     *
     * @throws ArithmeticException if {@code op} is zero.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF divUi(long op) {
        return new MPF().divUiAssign(this, op);
    }

    /**
     * Set this {@code MPF} to {@code (op1 / op2)}.
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
     * Set this {@code MPF} to {@code (op / this)}
     *
     * @throws ArithmeticException if {@code this} is zero.
     *
     * @return this {@code MPF}
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF uiDivAssign(long op) {
        return uiDivAssign(op, this);
    }

    /**
     * Return an {@code MPF} whose value is {@code (op / this)}.
     *
     * @throws ArithmeticException if {@code this} is zero.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public MPF uiDiv(long op) {
        return new MPF().uiDivAssign(op, this);
    }

    /**
     * Set this {@code MPF} to the the square root of {@code op}.
     *
     * @return this {@code MPF}.
     */
    public MPF sqrtAssign(MPF op) {
        mpf_sqrt(mpfNative, op.mpfNative);
        return this;
    }

    /**
     * Set this {@code MPF} to its square root.
     *
     * @return this {@code MPF}.
     */
    public MPF sqrtAssign() {
        return sqrtAssign(this);
    }

    /**
     * Return an {@code MPF} whose value is the square root of {@code this}.
     */
    public MPF sqrt() {
        return new MPF().sqrtAssign(this);
    }

    /**
     * Set this {@code MPF} to the the square root of {@code op}.
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
     * Return an {@code MPF} whose value is the square root of {@code this}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public static MPF sqrtUi(long op) {
        return new MPF().sqrtUiAssign(op);
    }

    /**
     * Set this {@code MPF} to <code>(base<sup>exp</sup>)</code>. The case
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
     * Set this {@code MPF} to <code>(this<sup>exp</sup>)</code>. The case
     * <code>0<sup>0</sup></code> yields {@code 1}.
     *
     * @return this {@code MPF}.
     *
     * @apiNote {@code exp} should be treated as an unsigned long.
     */
    public MPF powUiAssign(long exp) {
        return powUiAssign(this, exp);
    }

    /**
     * Return an {@code MPF} whose value is <code>(this<sup>exp</sup>)</code>. The
     * case <code>0<sup>0</sup></code> yields {@code 1}.
     *
     * @apiNote {@code exp} should be treated as an unsigned long.
     */
    public MPF powUi(long exp) {
        return new MPF().powUiAssign(this, exp);
    }

    /**
     * Set this {@code MPF} to {@code (- op)}.
     *
     * @return this {@code MPF}.
     */
    public MPF negAssign(MPF op) {
        mpf_neg(mpfNative, op.mpfNative);
        return this;
    }

    /**
     * Set this {@code MPF} to its opposite.
     *
     * @return this {@code MPF}.
     */
    public MPF negAssign() {
        return negAssign(this);
    }

    /**
     * Return an {@code MPF} whose value is {@code (- this)}.
     */
    public MPF neg() {
        return new MPF().negAssign(this);
    }

    /**
     * Set this {@code MPF} to the absolute value of {@code op}.
     *
     * @return this {@code MPF}.
     */
    public MPF absAssign(MPF op) {
        mpf_abs(mpfNative, op.mpfNative);
        return this;
    }

    /**
     * Set this {@code MPF} to its absolute value.
     *
     * @return this {@code MPF}.
     */
    public MPF absAssign() {
        return absAssign(this);
    }

    /**
     * Return an {@code MPF} whose value is the absolute value of {@code this}.
     */
    public MPF abs() {
        return new MPF().absAssign(this);
    }

    /**
     * Set this {@code MPF} to <code>(op * 2<sup>b</sup>)</code>.
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
     * Set this {@code MPF} to <code>(this * 2<sup>b</sup>)</code>.
     *
     * @return this {@code MPF}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPF mul2ExpAssign(long b) {
        return mul2ExpAssign(this, b);
    }

    /**
     * Return an {@code MPF} whose value is <code>(this * 2<sup>b</sup>)</code>.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPF mul2Exp(long b) {
        return new MPF().mul2ExpAssign(this, b);
    }

    /**
     * Set this {@code MPF} to <code>(op / 2<sup>b</sup>)</code>.
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
     * Set this {@code MPF} to <code>(this / 2<sup>b</sup>)</code>.
     *
     * @return this {@code MPF}.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPF div2ExpAssign(long b) {
        return div2ExpAssign(this, b);
    }

    /**
     * Return an {@code MPF} whose value is <code>(this * 2<sup>b</sup>)</code>.
     *
     * @apiNote {@code b} should be treated as an unsigned long.
     */
    public MPF div2Exp(long b) {
        return new MPF().div2ExpAssign(this, b);
    }

    // Comparison Functions

    /**
     * Compare {@code this} with {@code op}. Return a positive value if
     * {@code (this > op)}, zero if {@code (this = op)}, or a negative value if
     * {@code (this < op)}.
     */
    public int cmp(MPF op) {
        return mpf_cmp(mpfNative, op.mpfNative);
    }

    /**
     * Compare {@code this} with {@code op}. Return a positive value if
     * {@code (this > op)}, zero if {@code (this = op)}, or a negative value if
     * {@code (this < op)}. The value of {@code op} may be infinite, but the result
     * is undefined on NaNs.
     */
    public int cmp(MPZ op) {
        return mpf_cmp_z(mpfNative, op.getNative());
    }

    /**
     * Compare {@code this} with {@code op}. Return a positive value if
     * {@code (this > op)}, zero if {@code (this = op)}, or a negative value if
     * {@code (this < op)}. The value of {@code op} may be infinite, but the result
     * is undefined on NaNs.
     *
     * @throws ArithmeticException if {@code op} is a NaN.
     */
    public int cmp(double op) {
        if (Double.isNaN(op))
            throw new ArithmeticException(GMP.MSG_NAN_NOT_ALLOWED);
        return mpf_cmp_d(mpfNative, op);
    }

    /**
     * Compare {@code this} with {@code op}. Return a positive value if
     * {@code (this > op)}, zero if {@code (this = op)}, or a negative value if
     * {@code (this < op)}.
     */
    public int cmp(long op) {
        return mpf_cmp_si(mpfNative, new NativeLong(op));
    }

    /**
     * Compare {@code this} with {@code op}. Return a positive value if
     * {@code (this > op)}, zero if {@code (this = op)}, or a negative value if
     * {@code (this < op)}.
     *
     * @apiNote {@code op} should be treated as an unsigned long.
     */
    public int cmpUi(long op) {
        return mpf_cmp_ui(mpfNative, new NativeUnsignedLong(op));
    }

    /**
     * Set this {@code MPF} to the relative difference between {@code op1} and
     * {@code op2}, i.e., {@code (abs(op1-op2)/op1)}.
     *
     * @return this {@code MPF}.
     */
    public MPF reldiffAssign(MPF op1, MPF op2) {
        mpf_reldiff(mpfNative, op1.mpfNative, op2.mpfNative);
        return this;
    }

    /**
     * Set this {@code MPF} to the relative difference between {@code this} and
     * {@code op}, i.e., {@code (abs(this-op)/this)}.
     *
     * @return this {@code MPF}.
     */
    public MPF reldiffAssign(MPF op) {
        return reldiffAssign(this, op);
    }

    /**
     * Return the relative difference between {@code this} and {@code this}, i.e.,
     * {@code (abs(this-op)/this)}.
     */
    public MPF reldiff(MPF op) {
        return new MPF().reldiffAssign(this, op);
    }

    /**
     * Return {@code +1} if {@code (this > 0)}, {@code 0} if {@code (this = 0)} and
     * {@code -1} if {@code (this < 0)}.
     */
    public int sgn() {
        return mpf_sgn(mpfNative);
    }

    // Miscellaneous Functions Functions

    /**
     * Set this {@code MPF} to the value of {@code op} rounded to the next higher
     * integer.
     *
     * @return this {@code MPF}.
     */
    public MPF ceilAssign(MPF op) {
        mpf_ceil(mpfNative, op.mpfNative);
        return this;
    }

    /**
     * Set this {@code MPF} to its value rounded to the next higher integer.
     *
     * @return this {@code MPF}.
     */
    public MPF ceilAssign() {
        return ceilAssign(this);
    }

    /**
     * Return an {@code MPF} whose value is {@code this} rounded to the next higher
     * integer.
     */
    public MPF ceil() {
        return new MPF().ceilAssign(this);
    }

    /**
     * Set this {@code MPF} to the value of {@code op} rounded to the next lower
     * integer.
     *
     * @return this {@code MPF}.
     */
    public MPF floorAssign(MPF op) {
        mpf_floor(mpfNative, op.mpfNative);
        return this;
    }

    /**
     * Set this {@code MPF} to its value rounded to the next lower integer.
     *
     * @return this {@code MPF}.
     */
    public MPF floorAssign() {
        return floorAssign(this);
    }

    /**
     * Return an {@code MPF} whose value is {@code this} rounded to the next lower
     * integer.
     */
    public MPF floor() {
        return new MPF().floorAssign(this);
    }

    /**
     * Set this {@code MPF} to the value of {@code op} rounded towards zero.
     *
     * @return this {@code MPF}.
     */
    public MPF truncAssign(MPF op) {
        mpf_trunc(mpfNative, op.mpfNative);
        return this;
    }

    /**
     * Set this {@code MPF} to its value rounded towards zero.
     *
     * @return this {@code MPF}.
     */
    public MPF truncAssign() {
        return truncAssign(this);
    }

    /**
     * Return an {@code MPF} whose value is {@code this} rounded towards zero.
     */
    public MPF trunc() {
        return new MPF().truncAssign(this);
    }

    /**
     * Return whether this {@code MPF} is an integer.
     */
    public boolean isInteger() {
        return mpf_integer_p(mpfNative);
    }

    /**
     * Return {@code true} if and only if this {@code MPF} fits into a native
     * unsigned long.
     */
    public boolean fitsUlong() {
        return mpf_fits_ulong_p(mpfNative);
    }

    /**
     * Return {@code true} if and only if this {@code MPF} fits into a native signed
     * long.
     */
    public boolean fitsSlong() {
        return mpf_fits_slong_p(mpfNative);
    }

    /**
     * Return {@code true} if and only if this {@code MPF} fits into a native
     * unsigned int.
     */
    public boolean fitsUint() {
        return mpf_fits_uint_p(mpfNative);
    }

    /**
     * Return {@code true} if and only if this {@code MPF} fits into a native signed
     * int.
     */
    public boolean fitsSint() {
        return mpf_fits_sint_p(mpfNative);
    }

    /**
     * Return {@code true} if and only if this {@code MPF} fits into a native
     * unsigned short.
     */
    public boolean fitsUshort() {
        return mpf_fits_ushort_p(mpfNative);
    }

    /**
     * Return {@code true} if and only if this {@code MPF} fits into a native signed
     * short.
     */
    public boolean fitsSshort() {
        return mpf_fits_sshort_p(mpfNative);
    }

    /**
     * Return true if and only if this {@code this} MPF is zero.
     */
    public boolean isZero() {
        return mpf_cmp(mpfNative, zero.mpfNative) == 0;
    }

    /**
     * Set this {@code MPF} to a uniformly distributed random float in the range
     * from {@code 0} included to {@code 1} excluded. The result has {@code nbits}
     * significant bits in the mantissa, or less if the precision of this
     * {@code MPF} is smaller.
     *
     * @apiNote {@code nbits} should be treated as an unsigned long.
     */
    public MPF urandombAssign(RandState s, long nbits) {
        mpf_urandomb(mpfNative, s.getNative(), new MpBitcntT(nbits));
        return this;
    }

    /**
     * Return an {@code MPF} whose value is an uniformly distributed random float in
     * the range from {@code 0} included to {@code 1} excluded. The result has
     * {@code nbits} significant bits in the mantissa, or less if the default
     * precision is smaller.
     *
     * @apiNote {@code nbits} should be treated as an unsigned long.
     */
    public static MPF urandomb(RandState s, long nbits) {
        return new MPF().urandombAssign(s, nbits);
    }

    /**
     * Set this {@code MPF} to a random integer of at most {@code maxSize} limbs,
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
     * Return an {@code MPF} whose value is a random integer of at most
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

    // Constructors

    /**
     * Build an {@code MPF} whose value is zero.
     */
    public MPF() {
        mpfNative = new MpfT();
        mpf_init(mpfNative);
        GMP.cleaner.register(this, new MPFCleaner(mpfNative));
    }

    /**
     * Build an {@code MPF} whose value is {@code op}, possibly truncated to the
     * default precision.
     */
    public MPF(MPF op) {
        mpfNative = new MpfT();
        mpf_init_set(mpfNative, op.mpfNative);
        GMP.cleaner.register(this, new MPFCleaner(mpfNative));
    }

    /**
     * Build an {@code MPF} whose value is {@code op}, possibly truncated to the
     * default precision.
     */
    public MPF(long op) {
        mpfNative = new MpfT();
        mpf_init_set_si(mpfNative, new NativeLong(op));
        GMP.cleaner.register(this, new MPFCleaner(mpfNative));
    }

    /**
     * Build an {@code MPF} whose value is {@code op}, possibly truncated to the
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
     * Build an {@code MPF} whose value is {@code op}, possibly truncated to the
     * default precision.
     */
    public MPF(MPQ op) {
        mpfNative = new MpfT();
        mpf_init(mpfNative);
        mpf_set_q(mpfNative, op.getNative());
        GMP.cleaner.register(this, new MPFCleaner(mpfNative));
    }

    /**
     * Build an {@code MPF} whose value is {@code op}, possibly truncated to the
     * default precision.
     */
    public MPF(MPZ op) {
        mpfNative = new MpfT();
        mpf_init(mpfNative);
        mpf_set_z(mpfNative, op.getNative());
        GMP.cleaner.register(this, new MPFCleaner(mpfNative));
    }

    /**
     * Build an {@code MPF} whose value is the number represented by the string
     * {@code str} in the specified {@code base}, possibly truncated to the default
     * precision. See the GMP function
     * <a href="https://gmplib.org/manual/Simultaneous-Float-Init-_0026-Assign"
     * target="_blank">{@code mpf_init_set_str}</a>.
     *
     * @throws NumberFormatException if either {@code base} is not valid or
     *                               {@code str} is not a valid string in the
     *                               specified {@code base}.
     *
     */
    public MPF(String str, int base) {
        mpfNative = new MpfT();
        String strCorrect = str;
        if (!GMP.getDecimalSeparator().equals("."))
            if (str.indexOf(GMP.getDecimalSeparator()) == -1)
                strCorrect = str.replace(".", GMP.getDecimalSeparator());
            else
                throw new NumberFormatException(GMP.MSG_INVALID_STRING_CONVERSION);
        int result = mpf_init_set_str(mpfNative, strCorrect, base);
        if (result == -1) {
            mpf_clear(mpfNative);
            throw new NumberFormatException(GMP.MSG_INVALID_STRING_CONVERSION);
        }
        GMP.cleaner.register(this, new MPFCleaner(mpfNative));
    }

    /**
     * Build an {@code MPF} whose value is the number represented by the string
     * {@code str} in decimal base, possibly truncated to the default precision. See
     * the GMP function
     * <a href="https://gmplib.org/manual/Simultaneous-Float-Init-_0026-Assign"
     * target="_blank">{@code mpf_init_set_str}</a>.
     *
     * @throws NumberFormatException if {@code str} is not a valid number
     *                               representation in decimal base.
     */
    public MPF(String str) {
        this(str, 10);
    }

    /**
     * Builds an {@code MPF} whose value is the same as {@code op}. Note that, since
     * {@code BigDecimal} represents number in thedecimal base while {@code MPF} use
     * the binary base, rounding is possible.
     */
    public MPF(BigDecimal op) {
        this();
        set(op);
    }

    // setValue functions

    /**
     * Set this {@code MPF} to {@code op}, possibly truncated according to
     * precision.
     *
     * @return this {@code MPF}.
     */
    public MPF setValue(MPF op) {
        return set(op);
    }

    /**
     * Set this {@code MPF} to {@code op}, possibly truncated according to
     * precision.
     *
     * @return this {@code MPF}.
     */
    public MPF setValue(long op) {
        return set(op);
    }

    /**
     * Set this {@code MPF} to {@code op}, possibly truncated according to
     * precision.
     *
     * @throws ArithmeticException if {@code op} is not a finite number. In this
     *                             case, {@code this} is not altered.
     *
     * @return this {@code MPF}.
     */
    public MPF setValue(double op) {
        return set(op);
    }

    /**
     * Set this {@code MPF} to {@code op}, possibly truncated according to
     * precision.
     *
     * @return this {@code MPF}.
     */
    public MPF setValue(MPZ op) {
        return set(op);
    }

    /**
     * Set this {@code MPF} to {@code op}, possibly truncated according to
     * precision.
     *
     * @return this {@code MPF}.
     */
    public MPF setValue(MPQ op) {
        return set(op);
    }

    /**
     * Set this {@code MPF} to the number represented by the string {@code str} in
     * the specified {@code base}, possibly truncated according to precision. See
     * the GMP function
     * <a href="https://gmplib.org/manual/Assigning-Floats" target="
     * _blank">{@code mpf_set_str}</a>. The decimal point character is taken from
     * the current system locale, which may be different from the Java locale.
     *
     * @throws ArithmeticException if either {@code base} is not valid or
     *                             {@code str} is not a valid number representation
     *                             in the specified base. In this case, {@code this}
     *                             is not altered.
     */
    public MPF setValue(String str, int base) {
        var result = set(str, base);
        if (result == -1)
            throw new ArithmeticException(GMP.MSG_INVALID_STRING_CONVERSION);
        return this;
    }

    /**
     * Set this {@code MPF} to the value represented by the string {@code str} in
     * decimal base, possibly truncated according to precision. The decimal point
     * character is taken from the current system locale, which may be different
     * from the Java locale.
     *
     * @throws ArithmeticException if {@code str} is not a valid number
     *                             representation in decimal base.
     * @see setValue(String, int)
     */
    public MPF setValue(String str) {
        var result = set(str, 10);
        if (result == -1)
            throw new ArithmeticException(GMP.MSG_INVALID_STRING_CONVERSION);
        return this;
    }

    /**
     * Set this {@code MPF} to the big decimal {@code op}. Note that, since
     * {@code BigDecimal} represents number in thedecimal base while {@code MPF} use
     * the binary base, rounding is possible.
     *
     * @return this {@code MPF}.
     */
    public MPF setValue(BigDecimal op) {
        return set(op);
    }

    // Interface methods

    /**
     * Compare this {@code MPF} with {@code op}. Return a positive value if
     * {@code (this > op)}, zero if {@code (this = op)}, or a negative value if
     * {@code (this < op)}. This order is compatible with equality.
     */
    @Override
    public int compareTo(MPF op) {
        return mpf_cmp(mpfNative, op.mpfNative);
    }

    /**
     * Compare this {@code MPF} with the object {@code op} for equality. It returns
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
     * Return a hash code value for this {@code MPF}.
     *
     * @implNote Return its {@link intValue}.
     */
    @Override
    public int hashCode() {
        return intValue();
    }

    /**
     * Convert this {@code MPF} to a {@code long}, truncating any fraction part. If
     * this number is too big to fit a {@code long}, the result is undefined.
     */
    @Override
    public long longValue() {
        return getSi();
    }

    /**
     * Convert this {@code MPF} to an {@code int}.
     *
     * @implNote Return the result of {@link longValue} cast to an {@code int}.
     */
    @Override
    public int intValue() {
        return (int) getSi();
    }

    /**
     * Convert this {@code MPF} to a double, truncating if necessary. If the
     * exponent from the conversion is too big or too small to fit a double then the
     * result is system dependent. For too big an infinity is returned when
     * available. For too small 0.0 is normally returned. Hardware overflow,
     * underflow and denorm traps may or may not occur.
     */
    @Override
    public double doubleValue() {
        return getD();
    }

    /**
     * Convert this {@code MPF} to a float, truncating if necessary.
     *
     * @implNote Return the result of {@link doubleValue} cast to a {@code float}.
     */
    @Override
    public float floatValue() {
        return (float) getD();
    }

    /**
     * Convert this {@code MPF} to its string representation in the specified
     * {@code base}, or {@code null} if the base is not valid. See the GMP function
     * <a href="https://gmplib.org/manual/Converting-Floats" target=
     * "_blank">{@code mpf_get_str}</a>.
     *
     * @throws IllegalArgumentException if the base is not valid.
     */
    public String toString(int base, long nDigits) {
        var t = getStr(base, nDigits);
        if (t == null)
            throw new IllegalArgumentException(GMP.MSG_INVALID_BASE);
        var mantissa = t.getValue0();
        var position = t.getValue1().intValue();
        if (mantissa.length() == 0)
            return "0";
        var isNegative = mantissa.charAt(0) == '-';
        if (position >= 0) {
            if (isNegative)
                position += 1;
            if (position >= mantissa.length())
                return mantissa + "0".repeat(position-mantissa.length());
            else
                return mantissa.substring(0, position) + "." + mantissa.substring(position);
        } else if (isNegative)
            return "-0." + "0".repeat(-position) + mantissa.substring(1);
        else
            return "0." + "0".repeat(-position) + mantissa;
    }

    /**
     * Convert this {@code MPF} to its string representation in the specified
     * {@code base}, or {@code null} if the base is not valid. See the GMP function
     * <a href="https://gmplib.org/manual/Converting-Floats" target=
     * "_blank">{@code mpf_get_str}</a>.
     */
    public String toString(int base) {
        return toString(base, 10);
    }

    /**
     * Convert this {@code MPF} to its decimal string representation.
     */
    @Override
    public String toString() {
        return toString(10, 0);
    }

    // Serialization

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
