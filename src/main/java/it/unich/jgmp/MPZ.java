package it.unich.jgmp;

import static it.unich.jgmp.nativelib.LibGMP.*;

import java.nio.ByteBuffer;
import java.util.Optional;

import org.javatuples.Pair;
import org.javatuples.Triplet;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.NativeLongByReference;

import it.unich.jgmp.nativelib.MPBitCntT;
import it.unich.jgmp.nativelib.MPSizeT;
import it.unich.jgmp.nativelib.MPZPointer;
import it.unich.jgmp.nativelib.NativeUnsignedLong;
import it.unich.jgmp.nativelib.SizeT;
import it.unich.jgmp.nativelib.SizeTByReference;

/**
 * The class encapsulating the {@code mpz_t} data type, i.e., multi-precision
 * integer numbers.
 *
 * <p>
 * An element of {@code MPZ} contains a pointer to a native {@code mpz_t}
 * variable and registers itself with {@link GMP#cleaner} for freeing all
 * allocated memory during garbage collection.
 * <p>
 * In developing the interface of the {@code MPZ} class, we tried to adhere to
 * Java naming conventions, while keeping methods discoverable by people who
 * already know the C GMP library. For each GMP function named {@code mpz_xyz},
 * a <em>base name</em> is determined as follows:
 * <ul>
 * <li>the prefix {@code mpz_} and the components {@code _ui_}, {@code _d_} and
 * {@code _str_} are removed, everywhere but inthe {@code get} family of
 * functions, where this would cause a clash of prototypes;
 * <li>the postfix {@code _p}, which marks functions returning booleans, is
 * either removed or replaced by the prefix {@code is} when it makes sense;
 * <li>the rest of the name is transformed to camel case, by converting to
 * uppercase the first letter after each underscore;
 * <li>{@code import} and {@code export} are replace by {@code bufferImport} and
 * {@code bufferExport} to avoid conflict with Java reserved words.
 * </ul>
 * <p>
 * Once the base name has been established, we distinguish several cases:
 * <ul>
 * <li>functions {@code mpz_inits}, {@code mpz_clear} and {@code mpz_clears} are
 * only used internally and are not exposed by the {@code MPZ} class;
 * <li>functions in the categories <em>Integer Import and Export</em> and
 * <em>Special Functions</em> are not exposed by the {@code MPZ} class;
 * <li>if {@code baseName} begins with {@code realloc2}, {@code set} or
 * {@code swap}, we create a method called {@code baseName} which calls the
 * original function, implicitly using {@code this} as the first {@code mpz_t}
 * parameter.
 * <li>for all the other functions:
 * <ul>
 * <li>if the function has at least a non constant {@code mpz_t} parameter, then
 * we create a method {@code baseNameAssign} which calls the original function,
 * implicitly using {@code this} as the first non-constant {@code mpz_t}
 * parameter;
 * <li>we create e side-effect free method called {@code baseName}, with the
 * exception of a few cases where such as function is not particularly useful.
 * </ul>
 * </ul>
 * <p>
 * In general, all the parameters which are not provided implicitly to the
 * original GMP function should be provided explicitly by having them in the
 * method prototype.
 * <p>
 * The side-effect free methods are designed as follows. First of all, we
 * distinguish between input and output parameters for the GMP function. Some
 * parameters may have both an input and an output nature. The side-effect free
 * method takes all input parameters in its prototype, with the exception of the
 * first input {@code mpz_t} parameter which is mapped to {@code this}. If there
 * are no input {@code mpz_t} parameters, the method will be static. The method
 * creates new objects for the output parameters, eventually cloning the ones
 * also used as an input. After calling the GMP functions, the returnì value and
 * all the output parameters are returned by the method, eventually packed in a
 * {@link org.javatuples.Tuple} from left to right according to the function
 * prototype. Sometimes, when the first {@code mpz_t} input parameter comes
 * after other input parameters, this procedure may lead to a prototype clash.
 * In this case, the name of the method is changed into {@code baseNameReverse}.
 * <p>
 * The types of the formal parameters and return value of a GMP function are
 * mapped to the types of the JGMP method as follows:
 * <ol>
 * <li>{@code int} and {@code long}) Generally mapped to the respective Java
 * types. This may cause truncation when the native {@code long} is only 32 bit.
 * If an {@code int} is used to represent a boolean, then the {@code boolean}
 * type is used in JGMP.
 * <li>{@code unsigned long}, {@code size_t}, {@code mp_bitcnt} and
 * {@code mp_size_t}) Mapped to {@code long}. This may cause truncation when the
 * native size of these types is only 32 bit. Morevoer, with the exception of
 * {@code mp_size_t}, they are natively unsigned. Handle with care.
 * <li>{@code mpz_t}) Mapped to {@code MPZ}.
 * <li>{@code gmp_randstate_t}) Mapped to {@code RandState}.
 * <li>{@code const char*}) Mapped to {@code String}.
 * <li>{@code char*}) All functions requiring a non-const {@code char*} may be
 * called with a {@code null} pointer. Since this is much easier to handle, we
 * choose to always follow this pattern. Therefore, non-constant {@code char*}
 * are always removed from the input parameters. When {@code char*} is used a
 * return value, it is mapped to a {@code String}.
 * <li>{@code void}) If a function returns {@code void}, the correspoding
 * {@code JGMP} method returns {@code this}, in order to ease chaining of method
 * calls, if it is not side-effect free.
 * </ol>
 * <p>
 * Every special case which does not fall in the cases above is explained in the
 * specific documentation. The same holds for additional methods of the
 * {@code MPZ}, not directly corresponding to any GMP function.
 */
public class MPZ extends Number implements Comparable<MPZ> {
    /**
     * The pointer to the native {@code mpz_t} object.
     */
    private MPZPointer mpzPointer;

    /**
     * Result enumeration for the {@link isProbabPrime isProbabPrime} method.
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
     * Cleaner for the {@code MPZ} class.
     */
    private static class MPZCleaner implements Runnable {
        private MPZPointer mpzPointer;

        MPZCleaner(MPZPointer mpz) {
            this.mpzPointer = mpz;
        }

        @Override
        public void run() {
            __gmpz_clear(mpzPointer);
        }
    }

    /**
     * A private constructor which build an MPZ starting from a pointer to its
     * native data object. The native object needs to be already initialized.
     */
    private MPZ(MPZPointer pointer) {
        this.mpzPointer = pointer;
        GMP.cleaner.register(this, new MPZCleaner(pointer));
    }

    /**
     * Returns the native pointer to the MPZ object.
     */
    public MPZPointer getPointer() {
        return mpzPointer;
    }

    // Initializing Integers

    /**
     * Returns a new {@code MPZ} initialized to zero.
     */
    static public MPZ init() {
        return new MPZ();
    }

    /**
     * Returns a new {@code MPZ} initialized to zero with space preallocated for
     * {@code n}-it numbers. See the GMP function {@code mpz_init2}.
     *
     * @param n is the number of bit to pre-allocate, should be treated as an
     *          unsigned long.
     */
    static public MPZ init2(long n) {
        var mpzPointer = new MPZPointer();
        __gmpz_init2(mpzPointer, new MPBitCntT(n));
        return new MPZ(mpzPointer);
    }

    /**
     * Change the space allocated for this number to {@code n} bits. See the GMP
     * function {@code mpz_realloc2}.
     *
     * @param n is the number of bits to allocate for this number, should be treated
     *          as an unsigned long.
     */
    public MPZ realloc2(long n) {
        __gmpz_realloc2(mpzPointer, new MPBitCntT(n));
        return this;
    }

    // Assigning Integers

    /** Set the value of this number to the value of {@code op}. */
    public MPZ set(MPZ op) {
        __gmpz_set(mpzPointer, op.mpzPointer);
        return this;
    }

    /** Set the value of this number to the value of the signed long {@code op}. */
    public MPZ setSi(long op) {
        __gmpz_set_si(mpzPointer, new NativeLong(op));
        return this;
    }

    /**
     * Set the value of this number to the value of the unsigned long {@code op}.
     */
    public MPZ set(long op) {
        __gmpz_set_ui(mpzPointer, new NativeUnsignedLong(op));
        return this;
    }

    /**
     * Set the value of this number to the truncated value of {@code op}.
     *
     * @throws IllegalArgumentException if {@code op} is not a finite number. In
     *                                  this case, {@code this} is not altered.
     */
    public MPZ set(double op) {
        if (!Double.isFinite(op))
            throw new IllegalArgumentException("op should be a finite number");
        __gmpz_set_d(mpzPointer, op);
        return this;
    }

    /*
     * public MPZ set(MPQ op) {
     * __gmpz_set_q(mpzPointer, op.mpqPointer);
     * return this;
     * }
     *
     * public MPZ set(MPF op) {
     * __gmpz_set_f(mpzPointer, op.mpfPointer);
     * return this;
     */

    /**
     * Set the value of this number to the value represented by the string
     * {@code str} in the given {@code base}. See the GMP function
     * {@code mpz_set_str}.
     *
     * @return 0 if the operation succeeded, -1 otherwise. In the latter case,
     *         {@code this} is not altered.
     */
    public int set(String str, int base) {
        return __gmpz_set_str(mpzPointer, str, base);
    }

    /** Swap this number with the value in {@code op}. */
    public MPZ swap(MPZ op) {
        __gmpz_swap(mpzPointer, op.mpzPointer);
        return this;
    }

    // Simultaneous Integer Init & Assign

    /**
     * Returns a new {@code MPZ} initialized to the value of {@code op}.
     */
    public static MPZ initSet(MPZ op) {
        return new MPZ(op);
    }

    /**
     * Returns a new {@code MPZ} initialized to the value of the signed long
     * {@code op}.
     */
    public static MPZ initSetSi(long op) {
        return new MPZ(op);
    }

    /**
     * Returns a new {@code MPZ} initialized to the value of the unsigned long
     * {@code op}.
     */
    public static MPZ initSet(long op) {
        var mpzPointer = new MPZPointer();
        __gmpz_init_set_ui(mpzPointer, new NativeUnsignedLong(op));
        return new MPZ(mpzPointer);
    }

    /**
     * Returns a new {@code MPZ} initialized to the truncated value of {@code op}.
     */
    public static MPZ initSet(double op) {
        return new MPZ(op);
    }

    /**
     * Returns a new {@code MPZ} initialized to the value represented by the string
     * {@code str} in the given {@code base}. See the GMP function
     * {@code mpz_init_set_str}.
     *
     * @return a pair whose first component is 0 if the operation succeeded, and -1
     *         if either base is not valid, or str is not a valid number
     *         representation in the given base. The second component of the pair is
     *         the number represented in {@code str}. In case of error, the second
     *         component is 0.
     */

    public static Pair<Integer, MPZ> initSet(String str, int base) {
        var mpzPointer = new MPZPointer();
        var result = __gmpz_init_set_str(mpzPointer, str, base);
        return new Pair<>(result, new MPZ(mpzPointer));
    }

    // Converting Integers

    /**
     * Returns this number as an unsigned long.
     *
     * If this number is too big to fit an unsigned long, then just the least
     * significant bits that do fit are returned. The sign of this number is
     * ignored, only the absolute value is used.
     */
    public long getUi() {
        return __gmpz_get_ui(mpzPointer).longValue();
    }

    /**
     * Returns this number as a signed long.
     *
     * If this number is too big to fit a signed long, return the least significant
     * part, preserving the sign.
     */
    public long getSi() {
        return __gmpz_get_si(mpzPointer).longValue();
    }

    /**
     * Returns this number as a double, truncating if necessary. See the GMP
     * function {@code mpz_get_d}.
     */
    public double getD() {
        return __gmpz_get_d(mpzPointer);
    }

    /**
     * Returns the number as a pair made of mantissa and exponent, truncatin if
     * necessary. See the GMP function {@code mpz_get_d_2exp}.
     */
    public Pair<Double, Long> getD2Exp() {
        var pexp = new NativeLongByReference();
        var d = __gmpz_get_d_2exp(pexp, mpzPointer);
        return new Pair<>(d, pexp.getValue().longValue());
    }

    /**
     * Returns the string representation of this number in the given {@code base},
     * or {@code null} if the base is not valid. See the GMP function
     * {@code mpz_get_str}.
     */
    public String getStr(int base) {
        Pointer ps = __gmpz_get_str(null, base, mpzPointer);
        if (ps == null)
            return null;
        var s = ps.getString(0);
        Native.free(Pointer.nativeValue(ps));
        return s;
    }

    // Integer Arithmetic

    /**
     * Sets this number to {@code op1} + {@code op2}.
     *
     * @return this
     */
    public MPZ addAssign(MPZ op1, MPZ op2) {
        __gmpz_add(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    /**
     * Returns a new number whose value is {@code this} + {@code op}.
     */
    public MPZ add(MPZ op) {
        return new MPZ().addAssign(this, op);
    }

    /**
     * Sets this number to {@code op1} + {@code op2}.
     *
     * @return this
     */
    public MPZ addAssign(MPZ op1, long op2) {
        __gmpz_add_ui(mpzPointer, op1.mpzPointer, new NativeUnsignedLong(op2));
        return this;
    }

    /**
     * Returns a new number whose value is {@code this} + {@code op}.
     */
    public MPZ add(long op) {
        return new MPZ().addAssign(this, op);
    }

    /**
     * Sets this number to {@code op1} - {@code op2}.
     *
     * @return this
     */
    public MPZ subAssign(MPZ op1, MPZ op2) {
        __gmpz_sub(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    /**
     * Returns a new number whose value is {@code this} - {@code op}.
     */
    public MPZ sub(MPZ op) {
        return new MPZ().subAssign(this, op);
    }

    /**
     * Sets this number to {@code op1} - {@code op2}.
     *
     * @return this
     */
    public MPZ subAssign(MPZ op1, long op2) {
        __gmpz_sub_ui(mpzPointer, op1.mpzPointer, new NativeUnsignedLong(op2));
        return this;
    }

    /**
     * Returns a new number whose value is {@code this} - {@code op}.
     */
    public MPZ sub(long op) {
        return new MPZ().subAssign(this, op);
    }

    /**
     * Sets this number to {@code op1} - {@code op2}.
     *
     * @return this
     */
    public MPZ subAssign(long op1, MPZ op2) {
        __gmpz_ui_sub(mpzPointer, new NativeUnsignedLong(op1), op2.mpzPointer);
        return this;
    }

    /**
     * Returns a new number whose value is {@code op} - {@code this}.
     */
    public MPZ subReverse(long op) {
        return new MPZ().subAssign(op, this);
    }

    /**
     * Sets this number to {@code op1} * {@code op2}.
     *
     * @return this
     */
    public MPZ mulAssign(MPZ op1, MPZ op2) {
        __gmpz_mul(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    /**
     * Returns a new number whose value is {@code this} * {@code op}.
     */
    public MPZ mul(MPZ op) {
        return new MPZ().mulAssign(this, op);
    }

    /**
     * Sets this number to {@code op1} * {@code op2}.
     *
     * @return this
     */
    public MPZ mulAssign(MPZ op1, long op2) {
        __gmpz_mul_ui(mpzPointer, op1.mpzPointer, new NativeUnsignedLong(op2));
        return this;
    }

    /**
     * Returns a new number whose value is {@code this} * {@code op}.
     */
    public MPZ mul(long op) {
        return new MPZ().mulAssign(this, op);
    }

    /**
     * Sets this number to {@code op1} * {@code op2}.
     *
     * @return this
     */
    public MPZ mulAssignSi(MPZ op1, long op2) {
        __gmpz_mul_si(mpzPointer, op1.mpzPointer, new NativeLong(op2));
        return this;
    }

    /**
     * Returns a new number whose value is {@code this} * {@code op}.
     */
    public MPZ mulSi(long op) {
        return new MPZ().mulAssignSi(this, op);
    }

    /**
     * Sets this number to {@code op1} * {@code op2}.
     *
     * @return this
     */
    public MPZ addmulAssign(MPZ op1, MPZ op2) {
        __gmpz_addmul(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    /**
     * Returns a new number whose value is {@code this} + {@code op1} * {@code op2}.
     */
    public MPZ addmul(MPZ op1, MPZ op2) {
        return new MPZ(this).addmulAssign(op1, op2);
    }

    /**
     * Adds {@code op1} * {@code op2} to this number.
     *
     * @return this
     */
    public MPZ addmulAssign(MPZ op1, long op2) {
        __gmpz_addmul_ui(mpzPointer, op1.mpzPointer, new NativeUnsignedLong(op2));
        return this;
    }

    /**
     * Returns a new number whose value is {@code this} + {@code op1} * {@code op2}.
     */
    public MPZ addmul(MPZ op1, long op2) {
        return new MPZ(this).addmulAssign(op1, op2);
    }

    /**
     * Subtracts {@code op1} * {@code op2} from this number.
     *
     * @return this
     */
    public MPZ submulAssign(MPZ op1, MPZ op2) {
        __gmpz_submul(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    /**
     * Returns a new number whose value is {@code this} - {@code op1} * {@code op2}.
     */
    public MPZ submul(MPZ op1, MPZ op2) {
        return new MPZ(this).submulAssign(op1, op2);
    }

    /**
     * Subtracts {@code op1} * {@code op2} from this number.
     *
     * @return this
     */
    public MPZ submulAssign(MPZ op1, long op2) {
        __gmpz_submul_ui(mpzPointer, op1.mpzPointer, new NativeUnsignedLong(op2));
        return this;
    }

    /**
     * Returns a new number whose value is {@code this} - {@code op1} * {@code op2}.
     */
    public MPZ submul(MPZ op1, long op2) {
        return new MPZ(this).submulAssign(op1, op2);
    }

    /**
     * Sets this number to {@code op1} * 2 ^ {@code op2}.
     *
     * @return this
     */
    public MPZ mul2ExpAssign(MPZ op1, long op2) {
        __gmpz_mul_2exp(mpzPointer, op1.mpzPointer, new MPBitCntT(op2));
        return this;
    }

    /**
     * Returns a new number whose value is {@code this} * 2 ^ {@code op2}.
     */
    public MPZ mul2Exp(long op) {
        return new MPZ().mul2ExpAssign(this, op);
    }

    /**
     * Sets this number to the opposite of {@code op}.
     *
     * @return this
     */
    public MPZ negAssign(MPZ op) {
        __gmpz_neg(mpzPointer, op.mpzPointer);
        return this;
    }

    /**
     * Returns a new number whose value is the opposite of {@code this}.
     */
    public MPZ neg() {
        return new MPZ().negAssign(this);
    }

    /**
     * Sets this number to absolute value of {@code op}.
     *
     * @return this
     */
    public MPZ absAssign(MPZ op) {
        __gmpz_abs(mpzPointer, op.mpzPointer);
        return this;
    }

    /**
     * Returns a new number whose value is the absolute value of {@code this}.
     */
    public MPZ abs() {
        return new MPZ().absAssign(this);
    }

    // Integer Division

    /**
     * Sets this number to the value of the integer division {@code n} / {@code d},
     * rounded towards zero.
     *
     * @return this
     */
    public MPZ cdivqAssign(MPZ n, MPZ d) {
        __gmpz_cdiv_q(mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    /**
     * Sets this number to the remainder of the integer division {@code n} /
     * {@code d}, rounded towards zero.
     *
     * @return this
     */
    public MPZ cdivrAssign(MPZ n, MPZ d) {
        __gmpz_cdiv_r(mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    /**
     * Sets this number and {@code r} to the value and remainder of the integer
     * division {@code n} / {@code d}, rounded towards zero.
     *
     * @return this
     */
    public MPZ cdivqrAssign(MPZ r, MPZ n, MPZ d) {
        if (mpzPointer == r.mpzPointer)
            throw new IllegalArgumentException("The target of this method cannot point to the same object as r");
        __gmpz_cdiv_qr(mpzPointer, r.mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    /**
     * Sets this number to the value of the integer division {@code n} / {@code d},
     * rounded towards zero, and returns the remainder.
     *
     */
    public long cdivqAssign(MPZ n, long d) {
        return __gmpz_cdiv_q_ui(mpzPointer, n.mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Sets this number to the remainder of the integer division {@code n} /
     * {@code d}, rounded towards zero, and returns the remainder.
     */
    public long cdivrAssign(MPZ n, long d) {
        return __gmpz_cdiv_r_ui(mpzPointer, n.mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Sets this number and {@code r} to the value and remainder of the integer
     * division {@code n} / {@code d}, rounded towards zero, and returns the
     * remainder.
     */
    public long cdivqrAssign(MPZ r, MPZ n, long d) {
        if (mpzPointer == r.mpzPointer)
            throw new IllegalArgumentException("The target of this method cannot point to the same object as r");
        return __gmpz_cdiv_qr_ui(mpzPointer, r.mpzPointer, n.mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Returns the remainder of the integer division {@code this} / {@code d},
     * rounded towards zero.
     */
    public long cdiv(long d) {
        return __gmpz_cdiv_ui(mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Sets this number to the value of the integer division {@code n} / 2 ^
     * {@code b}, rounded toward zero.
     *
     * @return this
     */
    public MPZ cdivq2ExpAssign(MPZ n, long b) {
        __gmpz_cdiv_q_2exp(mpzPointer, n.mpzPointer, new MPBitCntT(b));
        return this;
    }

    /**
     * Sets this number to the remainder of the integer division {@code n} / 2 ^
     * {@code b}, rounded toward zero.
     *
     * @return this
     */
    public MPZ cdivr2ExpAssign(MPZ n, long b) {
        __gmpz_cdiv_r_2exp(mpzPointer, n.mpzPointer, new MPBitCntT(b));
        return this;
    }

    /**
     * Returns a new number with the value of the integer division {@code this} /
     * {@code d}, rounded towards zero.
     */
    public MPZ cdivq(MPZ d) {
        return new MPZ().cdivqAssign(this, d);
    }

    /**
     * Returns a new number with the remainder of the integer division {@code this}
     * / {@code d}, rounded towards zero.
     */
    public MPZ cdivr(MPZ d) {
        return new MPZ().cdivrAssign(this, d);
    }

    /**
     * Returns numbers with the value and remainder of the integer division
     * {@code this} / {@code d}, rounded towards zero.
     */
    public Pair<MPZ, MPZ> cdivqr(MPZ d) {
        MPZ q = new MPZ(), r = new MPZ();
        q.cdivqrAssign(r, this, d);
        return new Pair<>(q, r);
    }

    /**
     * Returns a new number with the value of the integer division {@code this} / 2
     * ^ {@code b}, rounded towards zero.
     */
    public MPZ cdivq2Exp(long b) {
        return new MPZ().cdivq2ExpAssign(this, b);
    }

    /**
     * Returns a new number with the remainder of the integer division {@code this}
     * / 2 ^ {@code b}, rounded towards zero.
     */
    public MPZ cdivr2Exp(long b) {
        return new MPZ().cdivr2ExpAssign(this, b);
    }

    /**
     * Sets this number to the value of the integer division {@code n} / {@code d},
     * rounded towards negative infinity.
     *
     * @return this
     */
    public MPZ fdivqAssign(MPZ n, MPZ d) {
        __gmpz_fdiv_q(mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    /**
     * Sets this number to the remainder of the integer division {@code n} /
     * {@code d}, rounded towards negative infinity.
     *
     * @return this
     */
    public MPZ fdivrAssign(MPZ n, MPZ d) {
        __gmpz_fdiv_r(mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    /**
     * Sets this number and {@code r} to the value and remainder of the integer
     * division {@code n} / {@code d}, rounded towards negative infinity.
     *
     * @return this
     */
    public MPZ fdivqrAssign(MPZ r, MPZ n, MPZ d) {
        if (mpzPointer == r.mpzPointer)
            throw new IllegalArgumentException("The target of this method cannot point to the same object as r");
        __gmpz_fdiv_qr(mpzPointer, r.mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    /**
     * Sets this number to the value of the integer division {@code n} / {@code d},
     * rounded towards negative infinity, and returns the absolute value of the
     * remainder.
     */
    public long fdivqAssign(MPZ n, long d) {
        return __gmpz_fdiv_q_ui(mpzPointer, n.mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Sets this number to the remainder of the integer division {@code n} /
     * {@code d}, rounded towards negative infinity, and returns the absolute value
     * of the remainder.
     */
    public long fdivrAssign(MPZ n, long d) {
        return __gmpz_fdiv_r_ui(mpzPointer, n.mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Sets this number and {@code r} to the value and remainder of the integer
     * division {@code n} / {@code d}, rounded towards negative infinity, and
     * returns the absolute value of the remainder.
     */
    public long fdivqrAssign(MPZ r, MPZ n, long d) {
        if (mpzPointer == r.mpzPointer)
            throw new IllegalArgumentException("The target of this method cannot point to the same object as r");
        return __gmpz_fdiv_qr_ui(mpzPointer, r.mpzPointer, n.mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Returns the remainder of the integer division {@code this} / {@code d},
     * rounded towards negative infinity.
     */
    public long fdiv(long d) {
        return __gmpz_fdiv_ui(mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Sets this number to the value of the integer division {@code n} / 2 ^
     * {@code b}, rounded toward negative infinity.
     *
     * @return this
     */
    public MPZ fdivq2ExpAssign(MPZ n, long b) {
        __gmpz_fdiv_q_2exp(mpzPointer, n.mpzPointer, new NativeUnsignedLong(b));
        return this;
    }

    /**
     * Sets this number to the remainder of the integer division {@code n} / 2 ^
     * {@code b}, rounded toward negative infinity.
     *
     * @return this
     */
    public MPZ fdivr2ExpAssign(MPZ n, long b) {
        __gmpz_fdiv_r_2exp(mpzPointer, n.mpzPointer, new NativeUnsignedLong(b));
        return this;
    }

    /**
     * Returns a new number with the value of the integer division {@code this} /
     * {@code d}, rounded towards negative infinity.
     */
    public MPZ fdivq(MPZ d) {
        return new MPZ().fdivqAssign(this, d);
    }

    /**
     * Returns a new number with the remainder of the integer division {@code this}
     * / {@code d}, rounded towards negative infinity.
     */
    public MPZ fdivr(MPZ d) {
        return new MPZ().fdivrAssign(this, d);
    }

    /**
     * Returns new numbers with the value and remainder of the integer division
     * {@code this} / {@code d}, rounded towards negative infinity.
     */
    public Pair<MPZ, MPZ> fdivqr(MPZ d) {
        MPZ q = new MPZ(), r = new MPZ();
        q.fdivqrAssign(r, this, d);
        return new Pair<>(q, r);
    }

    /**
     * Returns a new number with the value of the integer division {@code this} / 2
     * ^ {@code b}, rounded towards negative infinity.
     */
    public MPZ fdivq2Exp(long b) {
        return new MPZ().fdivq2ExpAssign(this, b);
    }

    /**
     * Returns a new number with the remainder of the integer division {@code this}
     * / 2 ^ {@code b}, rounded towards negative infinity.
     */
    public MPZ fdivr2Exp(long b) {
        return new MPZ().fdivr2ExpAssign(this, b);
    }

    /**
     * Sets this number to the value of the integer division {@code n} / {@code d},
     * rounded towards zero.
     *
     * @return this
     */
    public MPZ tdivqAssign(MPZ n, MPZ d) {
        __gmpz_fdiv_q(mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    /**
     * Sets this number to the remainder of the integer division {@code n} /
     * {@code d}, rounded towards zero.
     *
     * @return this
     */
    public MPZ tdivrAssign(MPZ n, MPZ d) {
        __gmpz_tdiv_r(mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    /**
     * Sets this number and {@code r} to the value and remainder of the integer
     * division {@code n} / {@code d}, rounded towards zero.
     *
     * @return this
     */
    public MPZ tdivqrAssign(MPZ r, MPZ n, MPZ d) {
        if (mpzPointer == r.mpzPointer)
            throw new IllegalArgumentException("The target of this method cannot point to the same object as r");
        __gmpz_tdiv_qr(mpzPointer, r.mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    /**
     * Sets this number to the value of the integer division {@code n} / {@code d},
     * rounded towards zero, and returns the absolute value of the remainder.
     */
    public MPZ tdivqAssign(MPZ n, long d) {
        __gmpz_tdiv_q_ui(mpzPointer, n.mpzPointer, new NativeUnsignedLong(d));
        return this;
    }

    /**
     * Sets this number to the remainder of the integer division {@code n} /
     * {@code d}, rounded towards zero, and returns the absolute value of the
     * remainder.
     */
    public long tdivrAssign(MPZ n, long d) {
        return __gmpz_tdiv_r_ui(mpzPointer, n.mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Sets this number and {@code r} to the value and remainder of the integer
     * division {@code n} / {@code d}, rounded towards zero, and returns the
     * absolute value of the remainder.
     */
    public long tdivqrAssign(MPZ r, MPZ n, long d) {
        if (mpzPointer == r.mpzPointer)
            throw new IllegalArgumentException("The target of this method cannot point to the same object as r");
        return __gmpz_tdiv_qr_ui(mpzPointer, r.mpzPointer, n.mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Returns the remainder of the integer division {@code this} / {@code d},
     * rounded towards zero.
     */
    public long tdiv(long d) {
        return __gmpz_tdiv_ui(mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Sets this number to the value of the integer division {@code n} / 2 ^
     * {@code b}, rounded toward zero.
     *
     * @return this
     */
    public MPZ tdivq2ExpAssign(MPZ n, long b) {
        __gmpz_tdiv_q_2exp(mpzPointer, n.mpzPointer, new NativeUnsignedLong(b));
        return this;
    }

    /**
     * Sets this number to the remainder of the integer division {@code n} / 2 ^
     * {@code b}, rounded toward zero.
     *
     * @return this
     */
    public MPZ tdivr2ExpAssign(MPZ n, long b) {
        __gmpz_tdiv_r_2exp(mpzPointer, n.mpzPointer, new NativeUnsignedLong(b));
        return this;
    }

    /**
     * Returns a new number with the value of the integer division {@code this} /
     * {@code d}, rounded towards zero.
     */
    public MPZ tdivq(MPZ d) {
        return new MPZ().tdivqAssign(this, d);
    }

    /**
     * Returns a new number with the remainder of the integer division {@code this}
     * / {@code d}, rounded towards zero.
     */
    public MPZ tdivr(MPZ d) {
        return new MPZ().tdivrAssign(this, d);
    }

    /**
     * Returns a new number with the value and remainder of the integer division
     * {@code this} / {@code d}, rounded towards zero.
     */
    public Pair<MPZ, MPZ> tdivqr(MPZ d) {
        MPZ q = new MPZ(), r = new MPZ();
        q.tdivqrAssign(r, this, d);
        return new Pair<>(q, r);
    }

    /**
     * Returns a new number with the value of the integer division {@code this} / 2
     * ^ {@code b}, rounded towards zero.
     */
    public MPZ tdivq2Exp(long b) {
        return new MPZ().tdivq2ExpAssign(this, b);
    }

    /**
     * Returns a new number with the remainder of the integer division {@code this}
     * / 2 ^ {@code b}, rounded towards zero.
     */
    public MPZ tdivr2Exp(long b) {
        return new MPZ().tdivr2ExpAssign(this, b);
    }

    /**
     * Sets this to {@code n} mod {@code d}. The sign of the divisor is ignored, the
     * result is always non-negative.
     *
     * @return this
     */
    public MPZ modAssign(MPZ n, MPZ d) {
        __gmpz_mod(mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    /**
     * Returns a new number with the value of {@code this} mod {code d}. The sign of
     * the divisor is ignored, the result is always non-negative.
     */
    public MPZ mod(MPZ d) {
        return new MPZ().modAssign(this, d);
    }

    /**
     * Sets this to {@code n} mod {@code d} and also returns the result. The sign of
     * the divisor is ignored, the result is always non-negative.
     */
    public long modAssign(MPZ n, long d) {
        return __gmpz_mod_ui(mpzPointer, n.mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    /**
     * Returns a new number with the value of {@code this} mod {code d}. The sign of
     * the divisor is ignored, the result is always non-negative.
     */
    public long mod(long d) {
        return fdiv(d);
    }

    /**
     * Sets this to {@code n} / {@code d}. This method produces correct results only
     * when it is known in advance that {@code d} divides {@code n}.
     *
     * @return this
     */
    public MPZ divexactAssign(MPZ n, MPZ d) {
        __gmpz_divexact(mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    /**
     * Returns a new number with the value of {@code this} / {@code d}. This method
     * produces correct results only when it is known in advance that {@code d}
     * divides {@code this}.
     */
    public MPZ divexact(MPZ d) {
        return new MPZ().divexactAssign(this, d);
    }

    /**
     * Sets this to {@code n} / {@code d}. This method produces correct results only
     * when it is known in advance that {@code d} divides {@code n}.
     *
     * @return this
     */
    public MPZ divexactAssign(MPZ n, long d) {
        __gmpz_divexact_ui(mpzPointer, n.mpzPointer, new NativeUnsignedLong(d));
        return this;
    }

    /**
     * Returns a new number with the value of {@code this} / {@code d}. This method
     * produces correct results only when it is known in advance that {@code d}
     * divides {@code this}.
     */
    public MPZ divexact(long d) {
        return new MPZ().divexactAssign(this, d);
    }

    /**
     * Returns whether {@code d} divides {@code this}.
     */
    public boolean isDivisible(MPZ d) {
        return __gmpz_divisible_p(mpzPointer, d.mpzPointer);
    }

    /**
     * Returns whether {@code d} divides {@code this}.
     */
    public boolean isDivisible(long d) {
        return __gmpz_divisible_ui_p(mpzPointer, new NativeUnsignedLong(d));
    }

    /**
     * Returns whether 2 ^ {@code b} divides {@code this}.
     */
    public boolean isDivisible2Exp(long b) {
        return __gmpz_divisible_2exp_p(mpzPointer, new MPBitCntT(b));
    }

    /**
     * Returns whether {@code this} is congruent to {@code c} modulo {@code d}.
     */
    public boolean isCongruent(MPZ c, MPZ d) {
        return __gmpz_congruent_p(mpzPointer, c.mpzPointer, d.mpzPointer);
    }

    /**
     * Returns whether {@code this} is congruent to {@code c} modulo {@code d}.
     */
    public boolean isCongruent(long c, long d) {
        return __gmpz_congruent_ui_p(mpzPointer, new NativeUnsignedLong(c), new NativeUnsignedLong(d));
    }

    /**
     * Returns whether {@code this} is congruent to {@code c} modulo 2 ^ {@code b}.
     */
    public boolean isCongruent2Exp(MPZ c, long b) {
        return __gmpz_congruent_2exp_p(mpzPointer, c.mpzPointer, new MPBitCntT(b));
    }

    // Integer Exponentiation

    /**
     * Sets this number to {@code base} ^ {@code exp} modulo {@code mod}.
     *
     * @return this.
     */
    public MPZ powmAssign(MPZ base, MPZ exp, MPZ mod) {
        __gmpz_powm(mpzPointer, base.mpzPointer, exp.mpzPointer, mod.mpzPointer);
        return this;
    }

    /**
     * Returns a new number whose value is {@code this} ^ {@code exp} modulo
     * {@code mod}.
     */
    public MPZ powm(MPZ exp, MPZ mod) {
        return new MPZ().powmAssign(this, exp, mod);
    }

    /**
     * Sets this number to {@code base} ^ {@code exp} modulo {@code mod}.
     *
     * @return this.
     */
    public MPZ powmAssign(MPZ base, long exp, MPZ mod) {
        __gmpz_powm_ui(mpzPointer, base.mpzPointer, new NativeUnsignedLong(exp), mod.mpzPointer);
        return this;
    }

    /**
     * Returns a new number whose value is {@code this} ^ {@code exp} modulo
     * {@code mod}.
     */
    public MPZ powm(long exp, MPZ mod) {
        return new MPZ().powmAssign(this, exp, mod);
    }

    /**
     * Sets this number to {@code base} ^ {@code exp} modulo {@code mod}. It is
     * required that {@code exp} &gt; 0 and that {@code mod} is odd. This function
     * is intended for cryptographic purposes, where resilience to side-channel
     * attacks is desired.
     *
     * @return this.
     */
    public MPZ powmSecAssign(MPZ base, MPZ exp, MPZ mod) {
        __gmpz_powm_sec(mpzPointer, base.mpzPointer, exp.mpzPointer, mod.mpzPointer);
        return this;
    }

    /**
     * Returns a new number whose value is {@code this} ^ {@code exp} modulo
     * {@code mod}. It is required that {@code exp} &gt; 0 and that {@code mod} is
     * odd. This function is intended for cryptographic purposes, where resilience
     * to side-channel attacks is desired.
     */
    public MPZ powmSec(MPZ exp, MPZ mod) {
        return new MPZ().powmSecAssign(this, exp, mod);
    }

    /**
     * Sets this number to {@code base} ^ {@code exp}. The case 0^0 yields 1.
     *
     * @return this.
     */
    public MPZ powAssign(MPZ base, long exp) {
        __gmpz_pow_ui(mpzPointer, base.mpzPointer, new NativeUnsignedLong(exp));
        return this;
    }

    /**
     * Returns a new number whose value is {@code this} ^ {@code exp}. The case 0^0
     * yields 1.
     */
    public MPZ pow(long exp) {
        return new MPZ().powAssign(this, exp);
    }

    /**
     * Sets this number to {@code base} ^ {@code exp}.
     *
     * @return this.
     */
    public MPZ powAssign(long base, long exp) {
        __gmpz_ui_pow_ui(mpzPointer, new NativeUnsignedLong(base), new NativeUnsignedLong(exp));
        return this;
    }

    public static MPZ pow(long base, long exp) {
        return new MPZ().powAssign(base, exp);
    }

    // Integer Roots

    /**
     * Sets this number to the truncated integer part of the {@code n}th root of
     * {@code op}.
     *
     * @return true if the computation is exact.
     */
    public boolean rootAssign(MPZ op, long n) {
        return __gmpz_root(mpzPointer, op.mpzPointer, new NativeUnsignedLong(n));
    }

    /**
     * Returns a new number whos value is the truncated integer part of the
     * {@code n}th root of {@code this}, and a boolean flag which is true when the
     * result is exact.
     */
    public Pair<Boolean, MPZ> root(long n) {
        var root = new MPZ();
        var exact = root.rootAssign(this, n);
        return new Pair<>(exact, root);
    }

    /**
     * Sets this number to the truncated integer part of the {@code n}th root of
     * {@code u} and {@code rem} to the remainder (i.e., {@code u} - root ^
     * {@code n}).
     *
     * @return this
     */
    public MPZ rootremAssign(MPZ rem, MPZ u, long n) {
        __gmpz_rootrem(mpzPointer, rem.mpzPointer, u.mpzPointer, new NativeUnsignedLong(n));
        return this;
    }

    /**
     * Returns new numbers whose values are the truncated integer part of the
     * {@code n}th root of {@code this} and the remainder (i.e., {@code this} - root
     * ^ {@code n}).
     */
    public Pair<MPZ, MPZ> rootrem(long n) {
        MPZ res = new MPZ(), rem = new MPZ();
        res.rootremAssign(rem, this, n);
        return new Pair<>(res, rem);
    }

    /**
     * Sets this number to the truncated integer part of the square root of
     * {@code op}.
     *
     * @return this
     */
    public MPZ sqrtAssign(MPZ op) {
        __gmpz_sqrt(mpzPointer, op.mpzPointer);
        return this;
    }

    /**
     * Returns a new number whose value is the truncated inegeter part of the square
     * root of {@code this}.
     */
    public MPZ sqrt() {
        return new MPZ().sqrtAssign(this);
    }

    /**
     * Sets this number to the truncated integer part of the square root of
     * {@code op} and {@code rem} to the remainder (i.e., {@code op} - root ^ 2).
     *
     * @return this
     */
    public MPZ sqrtremAssign(MPZ rem, MPZ op) {
        __gmpz_sqrtrem(mpzPointer, rem.mpzPointer, op.mpzPointer);
        return this;
    }

    /**
     * Returns new numbers whose values are the truncated integer part of the square
     * root of {@code this} and the remainder (i.e., {@code this} - root ^ 2).
     */
    public Pair<MPZ, MPZ> sqrtrem() {
        MPZ res = new MPZ(), rem = new MPZ();
        res.sqrtremAssign(rem, this);
        return new Pair<>(res, rem);
    }

    /**
     * Returns whether this number is a perfect power, i.e., if there exist integers
     * a and b, with b &gt; 1, such that {@code this} equals a ^ b. Under this
     * definition both 0 and 1 are considered to be perfect powers. Negative values
     * are accepted, but of course can only be odd perfect powers.
     */
    public boolean isPerfectPower() {
        return __gmpz_perfect_power_p(mpzPointer);
    }

    /**
     * Returns whether this number is a perfect square. Under this definition both 0
     * and 1 are considered to be perfect squares.
     */
    public boolean isPerfectSquare() {
        return __gmpz_perfect_square_p(mpzPointer);
    }

    // Number Theoretic Functions

    /**
     * Returns whether {@code this} is prime. See the GMP function
     * {@code mpz_probab_prime_p}.
     *
     * @param reps can be used to tune the probability of a non-prime being
     *             identified as “probably prime”. Reasonable values of reps are
     *             between 15 and 50.
     * @return an instance of the {@link PrimalityStatus} enum, telling whether
     *         {@code this} is definitely prime, probably prime or definitely
     *         non-prime.
     */
    public PrimalityStatus isProbabPrime(int reps) {
        var res = __gmpz_probab_prime_p(mpzPointer, reps);
        return PrimalityStatus.values()[res];
    }

    /**
     * Sets this number to the next prime greater then {@code op}. See the GMP
     * function {@code mpz_nextprime}.
     */
    public MPZ nextprimeAssign(MPZ op) {
        __gmpz_nextprime(mpzPointer, op.mpzPointer);
        return this;
    }

    /**
     * Returns a new number whose value is the next prime greater then {@code this}.
     * See the GMP function {@code mpz_nextprime}.
     */
    public MPZ nextprime() {
        return new MPZ().nextprimeAssign(this);
    }

    /**
     * Sets this number to the greatest commond divisor of {@code op1} and
     * {@code op2}. The result is always positive even if one or both input operands
     * are negative. Except if both inputs are zero; then this function defines
     * {@code gcd(0,0) = 0}.
     */
    public MPZ gcdAssign(MPZ op1, MPZ op2) {
        __gmpz_gcd(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    /**
     * Returns a new number whose value is the greatest commond divisor of
     * {@code this} and {@code op}.
     *
     * @see gcdAssign(MPZ, MPZ)
     */
    public MPZ gcd(MPZ op) {
        return new MPZ().gcdAssign(this, op);
    }

    /**
     * Sets this number to the greatest commond divisor of {@code op1} and
     * {@code op2}, and returns it. If the result does not fit into an unsigned
     * long, then 0 si returned.
     *
     * @see gcdAssign(MPZ, MPZ)
     */
    public long gcdAssign(MPZ op1, long op2) {
        return __gmpz_gcd_ui(mpzPointer, op1.mpzPointer, new NativeUnsignedLong(op2)).longValue();
    }

    /**
     * Returns the greatest commond divisor of {@code this} and {@code op}. If the
     * result does not fit into an unsigned long, 0 is returned.
     */
    public long gcd(long op) {
        return __gmpz_gcd_ui(null, mpzPointer, new NativeUnsignedLong(op)).longValue();
    }

    /**
     * Sets this number to the greatest common divisor of {@code a} and {@code b},
     * and in addition sets {@code s} and {@code t} to coefficients satisfying
     * <em>a*s + b*t = g</em>. If {@code t} or {@code g} is null, that value is not
     * computed. See GMP function {@code mpz_gcdext}.
     */
    public MPZ gcdextAssign(MPZ s, MPZ t, MPZ a, MPZ b) {
        __gmpz_gcdext(mpzPointer, s == null ? null : s.mpzPointer, t == null ? null : t.mpzPointer, a.mpzPointer,
                b.mpzPointer);
        return this;
    }

    /**
     * Returns the greatest common divisor of {@code this} and {@code op}, together
     * with numbers {@code s} and {@code t} satisfying <em>a*s + b*t = g</em>. See
     * GMP function {@code mpz_gcdext}.
     */
    public Triplet<MPZ, MPZ, MPZ> gcdext(MPZ op) {
        MPZ r = new MPZ(), s = new MPZ(), t = new MPZ();
        r.gcdextAssign(s, t, this, op);
        return new Triplet<>(r, s, t);
    }

    /**
     * Sets this number to the least commond multiple of {@code op1} and
     * {@code op2}. The result is always non-negative even if one or both input
     * operands are negative. The result will be zero if either {@code op1} or
     * {@code op2} is zero.
     */
    public MPZ lcmAssign(MPZ op1, MPZ op2) {
        __gmpz_lcm(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    /**
     * Returns the least commond multiple of {@code this} and {@code op}.
     *
     * @see lcmAssign(MPZ, MPZ)
     */
    public MPZ lcm(MPZ op) {
        return new MPZ().lcmAssign(this, op);
    }

    /**
     * Sets this number to the least commond multiple of {@code op1} and
     * {@code op2}.
     *
     * @see lcmAssign(MPZ, MPZ)
     */
    public MPZ lcmAssign(MPZ op1, long op2) {
        __gmpz_lcm_ui(mpzPointer, op1.mpzPointer, new NativeUnsignedLong(op2));
        return this;
    }

    /**
     * Returns the least commond multiple of {@code this} and {@code op}.
     *
     * @see lcmAssign(MPZ, MPZ)
     */
    public MPZ lcm(long op) {
        return new MPZ().lcmAssign(this, op);
    }

    /**
     * Sets this number to the inverse of {@code op1} modulo {@code op2}. If the
     * inverse does not exists, the new value of this is undefined. The behaviour of
     * this function is undefined when op2 is zero.
     *
     * @return true if the inverse exists, false otherwise.
     */
    public boolean invertAssign(MPZ op1, MPZ op2) {
        return __gmpz_invert(mpzPointer, op1.mpzPointer, op2.mpzPointer);
    }

    /**
     * Optionally returns the inverse of {@code this} modulo {@code op}. The
     * behaviour of this function is undefined when {@code op} is zero.
     */
    public Optional<MPZ> invert(MPZ modulus) {
        var res = new MPZ();
        var exists = res.invertAssign(this, modulus);
        return exists ? Optional.of(res) : Optional.empty();
    }

    /**
     * Returns the Jacobi symbol ({@code thos}/{@code b}). This is defined only for
     * {@code b} odd.
     */
    public int jacobi(MPZ b) {
        return __gmpz_jacobi(mpzPointer, b.mpzPointer);
    }

    /**
     * Returns the Legendre symbol ({@code thos}/{@code p}). This is defined only
     * for {@code p} an odd positive prime, and for such {@code p} it’s identical to
     * the Jacobi symbol.
     */
    public int legendre(MPZ p) {
        return __gmpz_legendre(mpzPointer, p.mpzPointer);
    }

    /**
     * Returns the Jacobi symbol <em>(this/b)</em> with the Kronecker extension
     * <em>(this/2)=(2/this)</em> when <em>this</em> odd, or <em>(this/2)=0</em>
     * when <em>this</em> is even. When b is odd the Jacobi symbol and Kronecker
     * symbol are identical. See the GMP function {@code mpz_kronecker}.
     */
    public int kronecker(MPZ b) {
        // the jacobi GMP function already implements the Kronecker extension
        return __gmpz_jacobi(mpzPointer, b.mpzPointer);
    }

    /**
     * Returns the Jacobi symbol <em>(this/b)</em> with the Kronecker extension.
     *
     * @see kronecker(MPZ)
     */
    public int kroneckerSi(long b) {
        return __gmpz_kronecker_si(mpzPointer, new NativeLong(b));
    }

    /**
     * Returns the Jacobi symbol <em>(this/b)</em> with the Kronecker extension.
     *
     * @see kronecker(MPZ)
     */
    public int kronecker(long b) {
        return __gmpz_kronecker_ui(mpzPointer, new NativeUnsignedLong(b));
    }

    /**
     * Returns the Jacobi symbol <em>(a/this)</em> with the Kronecker extension.
     *
     * @see kronecker(MPZ)
     */
    public int siKronecker(long a) {
        return __gmpz_si_kronecker(new NativeLong(a), mpzPointer);
    }

    /**
     * Returns the Jacobi symbol <em>(a/this)</em> with the Kronecker extension.
     *
     * @see kronecker(MPZ)
     */
    public int kroneckerReverse(long a) {
        return __gmpz_ui_kronecker(new NativeUnsignedLong(a), mpzPointer);
    }

    /**
     * Remove all occurrences of the factor <em>f</em> from <em>op</em> and store
     * the result in this number. The return value is how many such occurrences were
     * removed.
     */
    public long removeAssign(MPZ op, MPZ f) {
        return __gmpz_remove(mpzPointer, op.mpzPointer, f.mpzPointer).longValue();
    }

    /**
     * Return the result of removing the factor <em>f</em> from <em>this</em>,
     * together with the number of occurrences which were removed.
     */
    public Pair<Long, MPZ> remove(MPZ f) {
        var res = new MPZ();
        var count = __gmpz_remove(res.mpzPointer, mpzPointer, f.mpzPointer);
        return new Pair<>(count.longValue(), res);
    }

    /**
     * Sets this number to the value of the factorial of <em>n</em>.
     */
    public MPZ facAssign(long n) {
        __gmpz_fac_ui(mpzPointer, new NativeUnsignedLong(n));
        return this;
    }

    /**
     * Returns the factorial of <em>n</em>.
     */
    public static MPZ fac(long n) {
        return new MPZ().facAssign(n);
    }

    /**
     * Sets this number to the value of the double factorial of <em>n</em>.
     */
    public MPZ dfacAssign(long n) {
        __gmpz_2fac_ui(mpzPointer, new NativeUnsignedLong(n));
        return this;
    }

    /**
     * Returns the double factorial of <em>n</em>.
     */
    public static MPZ dfac(long n) {
        return new MPZ().dfacAssign(n);
    }

    /**
     * Sets this number to the value of the <em>m</em>-multi factorial of
     * <em>n</em>.
     */
    public MPZ mfacAssign(long n, long m) {
        __gmpz_mfac_uiui(mpzPointer, new NativeUnsignedLong(n), new NativeUnsignedLong(m));
        return this;
    }

    /**
     * Returns the <em>m</em>-multi factorial of <em>n</em>.
     */
    public static MPZ mfac(long n, long m) {
        return new MPZ().mfacAssign(n, m);
    }

    /**
     * Sets this number to the value of the primorial of <em>n</em>, i.e. the
     * product of all positive prime numbers &le; <em>n</em>.
     */
    public MPZ primorialAssign(long n) {
        __gmpz_primorial_ui(mpzPointer, new NativeUnsignedLong(n));
        return this;
    }

    /**
     * Returns the primorial of <em>n</em>, i.e. the product of all positive prime
     * numbers &le; <em>n</em>.
     */
    public static MPZ primorial(long n) {
        return new MPZ().primorialAssign(n);
    }

    /**
     * Sets this number to the binomial coefficient <em>n</em> over <em>k</em>.
     * Negative values of <em>n</em> are supported using the identity <em>bin(-n,k)
     * = (-1)^k * bin(n+k-1,k)</em>, see Knuth volume 1 section 1.2.6 part G.
     */
    public MPZ binAssign(MPZ n, long k) {
        __gmpz_bin_ui(mpzPointer, n.mpzPointer, new NativeUnsignedLong(k));
        return this;
    }

    /**
     * Returns the binomial coefficient <em>this</em> over <em>k</em>. Negative
     * values of <em>n</em> are supported as in {@link binAssign(MPZ, long)}.
     */
    public MPZ bin(long k) {
        return new MPZ().binAssign(this, k);
    }

    /**
     * Sets this number to the binomial coefficient <em>n</em> over <em>k</em>.
     */
    public MPZ binAssign(long n, long k) {
        __gmpz_bin_uiui(mpzPointer, new NativeUnsignedLong(n), new NativeUnsignedLong(k));
        return this;
    }

    /**
     * Returns the binomial coefficient <em>n</em> over <em>k</em>.
     */
    public static MPZ bin(long n, long k) {
        return new MPZ().binAssign(n, k);
    }

    /**
     * Sets this number to the <em>n</em>-th Fibonacci number.
     */
    public MPZ fibAssign(long n) {
        __gmpz_fib_ui(mpzPointer, new NativeUnsignedLong(n));
        return this;
    }

    /**
     * Returns the <em>n</em>-th Fibonacci number.
     */
    public static MPZ fib(long n) {
        return new MPZ().fibAssign(n);
    }

    /**
     * Sets {@code this} and {@code fnsub1} to the <em>n</em>-th and <em>n-1</em>-th
     * Fibonacci numbers respecively.
     */
    public MPZ fib2Assign(MPZ fnsub1, long n) {
        __gmpz_fib2_ui(mpzPointer, fnsub1.mpzPointer, new NativeUnsignedLong(n));
        return this;
    }

    /**
     * Returns the <em>n</em>-th and <em>n-1</em>-th Fibonacci numbers.
     */
    public static Pair<MPZ, MPZ> fib2(long n) {
        MPZ fnsub1 = new MPZ(), fn = new MPZ();
        fn.fib2Assign(fnsub1, n);
        return new Pair<>(fn, fnsub1);
    }

    /**
     * Returns the <em>n</em>-th Fibonacci number.
     */
    public MPZ lucnumAssign(long n) {
        __gmpz_lucnum_ui(mpzPointer, new NativeUnsignedLong(n));
        return this;
    }

    /**
     * Sets this number to the <em>n</em>-th Fibonacci number.
     */
    public static MPZ lucnum(long n) {
        return new MPZ().lucnumAssign(n);
    }

    /**
     * Sets {@code this} and {@code lnsub1} to the <em>n</em>-th and <em>n-1</em>-th
     * Lucas numbers numbers respecively.
     */
    public MPZ lucnum2Assign(MPZ fnsub1, long n) {
        __gmpz_lucnum2_ui(mpzPointer, fnsub1.mpzPointer, new NativeUnsignedLong(n));
        return this;
    }

    /**
     * Returns the <em>n</em>-th and <em>n-1</em>-th Lucas numbers.
     */
    public static Pair<MPZ, MPZ> lucnum2(long n) {
        MPZ lnsub1 = new MPZ(), ln = new MPZ();
        ln.lucnum2Assign(lnsub1, n);
        return new Pair<>(ln, lnsub1);
    }

    // Integer Comparisons

    /**
     * Compare {@code this} with {@code op}. Return a positive value if <em>this
     * &gt; op</em>, zero if <em>this = op</em>, or a negative value if <em>this
     * &lt; op</em>.
     */
    public int cmp(MPZ op) {
        return __gmpz_cmp(mpzPointer, op.mpzPointer);
    }

    /**
     * Compare {@code this} with {@code op}. Return a positive value if <em>this
     * &gt; op</em>, zero if <em>this = op</em>, or a negative value if <em>this
     * &lt; op</em>. The value of {@code op} may be infinite, but the result is
     * undefined on NaNs.
     */
    public int cmp(double op) {
        return __gmpz_cmp_d(mpzPointer, op);
    }

    /**
     * Compare {@code this} with {@code op}. Return a positive value if <em>this
     * &gt; op</em>, zero if <em>this = op</em>, or a negative value if <em>this
     * &lt; op</em>.
     */
    public int cmpSi(long op) {
        return __gmpz_cmp_si(mpzPointer, new NativeLong(op));
    }

    /**
     * Compare {@code this} with {@code op}. Return a positive value if <em>this
     * &gt; op</em>, zero if <em>this = op</em>, or a negative value if <em>this
     * &lt; op</em>.
     */
    public int cmp(long op) {
        return __gmpz_cmp_ui(mpzPointer, new NativeUnsignedLong(op));
    }

    /**
     * Compare the absolute values of {@code this} and {@code op}. Return a positive
     * value if <em>abs(this) &gt; abs(op)</em>, zero if <em>abs(this) =
     * abs(op)</em>, or a negative value if <em>abs(this) &lt; abs(op)</em>.
     */
    public int cmpabs(MPZ op) {
        return __gmpz_cmpabs(mpzPointer, op.mpzPointer);
    }

    /**
     * Compare the absolute values of {@code this} and {@code op}. Return a positive
     * value if <em>abs(this) &gt; abs(op)</em>, zero if <em>abs(this) =
     * abs(op)</em>, or a negative value if <em>abs(this) &lt; abs(op)</em>. The
     * value of {@code op} may be infinite, but the result is undefined on NaNs.
     */
    public int cmpabs(double op) {
        return __gmpz_cmpabs_d(mpzPointer, op);
    }

    /**
     * Compare the absolute values of {@code this} and {@code op}. Return a positive
     * value if <em>abs(this) &gt; abs(op)</em>, zero if <em>abs(this) =
     * abs(op)</em>, or a negative value if <em>abs(this) &lt; abs(op)</em>.
     */
    public int cmpabsSi(long op) {
        return __gmpz_cmpabs_ui(mpzPointer, new NativeUnsignedLong(Math.abs(op)));
    }

    /**
     * Compare the absolute values of {@code this} and {@code op}. Return a positive
     * value if <em>abs(this) &gt; abs(op)</em>, zero if <em>abs(this) =
     * abs(op)</em>, or a negative value if <em>abs(this) &lt; abs(op)</em>.
     */
    public int cmpabs(long op) {
        return __gmpz_cmpabs_ui(mpzPointer, new NativeUnsignedLong(op));
    }

    /**
     * Returns <em>+1</em> if <em>op &gt; 0</em>, <em>0</em> if <em>op = 0</em>, and
     * <em>-1</em> <em>if op &lt; 0</em>.
     */
    public int sgn() {
        return __gmpz_sgn(mpzPointer);
    }

    // Integer Logic and Bit Fiddling

    /**
     * Sets {@code this} to {@code (op1 & op2)}.
     */
    public MPZ andAssign(MPZ op1, MPZ op2) {
        __gmpz_and(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    /**
     * Returns an {@code MPZ} whose value is {@code (this & op)}.
     */
    public MPZ and(MPZ op) {
        return new MPZ().andAssign(this, op);
    }

    /**
     * Sets {@code this} to {@code (op1 | op2)}.
     */
    public MPZ iorAssign(MPZ op1, MPZ op2) {
        __gmpz_ior(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    /**
     * Returns an {@code MPZ} whose value is {@code (this | op)}.
     */
    public MPZ ior(MPZ op) {
        return new MPZ().iorAssign(this, op);
    }

    /**
     * Sets {@code this} to {@code (op1 ^ op2)}.
     */
    public MPZ xorAssign(MPZ op1, MPZ op2) {
        __gmpz_xor(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    /**
     * Returns an {@code MPZ} whose value is {@code (this ^ op)}.
     */
    public MPZ xor(MPZ op) {
        return new MPZ().xorAssign(this, op);
    }

    /**
     * Sets {@code this} to {@code ~op)}.
     */
    public MPZ comAssign(MPZ op) {
        __gmpz_com(mpzPointer, op.mpzPointer);
        return this;
    }

    /**
     * Returns an {@code MPZ} whose value is {@code ~op}.
     */
    public MPZ com() {
        return new MPZ().comAssign(this);
    }

    /**
     * If {@code op &ge 0}, returns the population count of {@code op}, which is the
     * number of 1 bits in the binary representation. If {@code op<0}, the number of
     * 1s is infinite, and the return value is the largest possible value for the
     * native type {@code mp_bitcnt_t}.
     */
    public long popcount() {
        return __gmpz_popcount(mpzPointer).longValue();
    }

    /**
     * If {@code this} and {@code op} are both {@code &ge;0} or both {@code &lt;0},
     * returns the Hamming distance between them, which is the number of bit
     * positions where {@code this} and {@code op} different bit values. If one
     * operand is {@code &ge;0} and the other {&lt;0} then the number of bits
     * different is infinite, and the return value is the largest possible value for
     * the native type {@code mp_bitcnt_t}.
     */
    public long hamdist(MPZ op) {
        return __gmpz_hamdist(mpzPointer, op.mpzPointer).longValue();
    }

    /**
     * Scans {@code this}, starting from bit {@code starting_bit}, towards more
     * significant bits, until the first 1 bit is found. Returns the index of the
     * found bit.
     */
    public long scan0(long starting_bit) {
        return __gmpz_scan0(mpzPointer, new MPBitCntT(starting_bit)).longValue();
    }

    /**
     * Scans {@code this}, starting from bit {@code starting_bit}, towards more
     * significant bits, until the first 0 bit is found. Returns the index of the
     * found bit.
     */
    public long scan1(long starting_bit) {
        return __gmpz_scan1(mpzPointer, new MPBitCntT(starting_bit)).longValue();
    }

    /**
     * Sets to 1 the bit {@code index} of this {@code MPZ}.
     */
    public MPZ setbitAssign(long index) {
        __gmpz_setbit(mpzPointer, new MPBitCntT(index));
        return this;
    }

    /**
     * Returns an {@code MPZ} whose value is
     * <code>(this | 2<sup>index</sup>)</code>.
     */
    public MPZ setbit(long index) {
        return new MPZ(this).setbitAssign(index);
    }

    /**
     * Sets to 0 the bit {@code index} of this {@code MPZ}.
     */
    public MPZ clrbitAssign(long index) {
        __gmpz_clrbit(mpzPointer, new MPBitCntT(index));
        return this;
    }

    /**
     * Returns an {@code MPZ} whose value is
     * <code>(this &amp; ~2<sup>index</sup>)</code>.
     */
    public MPZ clrbit(long index) {
        return new MPZ(this).clrbitAssign(index);
    }

    /**
     * Complements the bit {@code index} of this {@code MPZ}.
     */
    public MPZ combitAssign(long index) {
        __gmpz_combit(mpzPointer, new MPBitCntT(index));
        return this;
    }

    /**
     * Returns an {@code MPZ} whose value is
     * <code>(this ^ 2<sup>index</sup>)</code>.
     */
    public MPZ combit(long index) {
        return new MPZ(this).combitAssign(index);
    }

    /**
     * Returns the bit {@code index} of this {@code MPZ}.
     */
    public int tstbit(long index) {
        return __gmpz_tstbit(mpzPointer, new MPBitCntT(index));
    }

    // Random Number Functions

    /**
     * Sets this {@code MPZ} to a uniformly distributed random integer in the range
     * <code>0</code> to <code>2<sup>n</sup>-1</code>, inclusive.
     */
    public MPZ urandombAssign(RandState s, long n) {
        __gmpz_urandomb(mpzPointer, s.getPointer(), new MPBitCntT(n));
        return this;
    }

    /**
     * Returns an {@code MPZ} whose value is an uniformly distributed random integer
     * in the range <code>0</code> to <code>2<sup>n</sup>-1</code>, inclusive.
     */
    public static MPZ urandomb(RandState s, long n) {
        var z = new MPZ();
        z.urandombAssign(s, n);
        return z;
    }

    /**
     * Sets this {@code MPZ} to a uniformly distributed random integer in the range
     * <code>0</code> to <code>n-1</code>, inclusive.
     */
    public MPZ urandommAssign(RandState s, MPZ n) {
        __gmpz_urandomm(mpzPointer, s.getPointer(), n.mpzPointer);
        return this;
    }

    /**
     * Returns an {@code MPZ} whose value is an uniformly distributed random integer
     * in the range <code>0</code> to <code>n-1</code>, inclusive.
     */
    public static MPZ urandomm(RandState s, MPZ n) {
        var z = new MPZ();
        z.urandommAssign(s, n);
        return z;
    }

    /**
     * Sets this {@code MPZ} to a random integer with long strings of zeros and ones
     * in the binary representation. Useful for testing functions and algorithms,
     * since this kind of random numbers have proven to be more likely to trigger
     * corner-case bugs. The random number will be in the range
     * <code>2<sup>n-1</sup></code> to <code>2<sup>n</sup>-1</code>, inclusive.
     */
    public MPZ rrandombAssign(RandState s, long n) {
        __gmpz_rrandomb(mpzPointer, s.getPointer(), new MPBitCntT(n));
        return this;
    }

    /**
     * Returns an {@code MPZ} whose value is a random integer with long strings of
     * zeros and ones in the binary representation. Useful for testing functions and
     * algorithms, since this kind of random numbers have proven to be more likely
     * to trigger corner-case bugs. The random number will be in the range
     * <code>2<sup>n-1</sup></code> to <code>2<sup>n</sup>-1</code>, inclusive.
     */
    public static MPZ rrandomb(RandState s, long n) {
        var z = new MPZ();
        z.urandombAssign(s, n);
        return z;
    }

    /**
     * Sets this {@code MPZ} to a random integer of at most {@code max_size} limbs.
     * The generated random number doesn’t satisfy any particular requirements of
     * randomness. Negative random numbers are generated when {@code max_size} is
     * negative.
     *
     * @deprecated use {@link urandombAssign} or {@link urandommAssign} instead.
     * @return this
     */
    public MPZ randomAssign(long max_size) {
        __gmpz_random(mpzPointer, new MPSizeT(max_size));
        return this;
    }

    /**
     * Returns an {@code MPZ} whose value is a random integer of at most
     * {@code max_size} limbs. The generated random number doesn’t satisfy any
     * particular requirements of randomness. Negative random numbers are generated
     * when {@code max_size} is negative.
     *
     * @deprecated use {@link urandomb} or {@link urandomm} instead.
     */
    public static MPZ random(long max_size) {
        return (new MPZ()).randomAssign(max_size);
    }

    /**
     * Sets this {@code MPZ} to a random integer of at most {@code max_size} limbs,
     * with long strings of zeros and ones in the binary representation. Useful for
     * testing functions and algorithms, since this kind of random numbers have
     * proven to be more likely to trigger corner-case bugs. Negative random numbers
     * are generated when {@code max_size} is negative.
     *
     * @deprecated use {@link rrandombAssign} instead.
     * @return this
     */
    public MPZ random2Assign(long max_size) {
        __gmpz_random2(mpzPointer, new MPSizeT(max_size));
        return this;
    }

    /**
     * Returns an {@code MPZ} whose value is a random integer of at most
     * {@code max_size} limbs, with long strings of zeros and ones in the binary
     * representation. Useful for testing functions and algorithms, since this kind
     * of random numbers have proven to be more likely to trigger corner-case bugs.
     * Negative random numbers are generated when {@code max_size} is negative.
     *
     * @deprecated use {@link rrandomb} instead.
     */
    public static MPZ random2(long max_size) {
        return (new MPZ()).random2Assign(max_size);
    }

    // Integer Import and Export

    /**
     * Sets this {@code MPZ} from the array of word data at {@code op}. See the
     * detailed description in the documentation of the {@code mpz_import} function.
     * The parameter {@code count} in the prototype of {@code mpz_import} is
     * automatically computed by the size of buffer {@code op}, from the beginning
     * to its {@code limit}.
     */
    public MPZ bufferImportAssign(int order, int size, int endian, long nails, ByteBuffer op) {
        var count = op.limit() / size + (op.limit() % size == 0 ? 0 : 1);
        __gmpz_import(mpzPointer, new SizeT(count), order, new SizeT(size), endian, new SizeT(nails), op);
        return this;
    }

    /**
     * Returns an {@code MPZ} whose value is determined from the array of word data
     * at {@code op}.
     *
     * @see bufferImportAssign
     */
    public static MPZ bufferImport(int order, int size, int endian, long nails, ByteBuffer op) {
        return new MPZ().bufferImportAssign(order, size, endian, nails, op);
    }

    /**
     * Returns a {@link ByteBuffer} with word data from {@code op}. See the detailed
     * description in the documentation of the {@code mpz_export} function. We let
     * the function allocate the buffer, since it the easier and safer. The output
     * {@code count} of the original GMP function is not needed, since it
     * corresponds to the capacity of the resulting {@link ByteBuffer}.
     */
    public ByteBuffer bufferExport(int order, int size, int endian, long nails) {
        var count = new SizeTByReference();
        var p = __gmpz_export(null, count, order, new SizeT(size), endian, new SizeT(nails), mpzPointer);
        return p.getPointer().getByteBuffer(0, count.getValue().longValue());
    }

    // Miscellaneous Integer Functions

    /**
     * Returns {@code true} if and only if this {@code MPZ} fits into a native
     * unsigned long.
     */
    public boolean fitsUlong() {
        return __gmpz_fits_ulong_p(mpzPointer);
    }

    /**
     * Returns {@code true} if and only if this {@code MPZ} fits into a native
     * signed long.
     */
    public boolean fitsSlong() {
        return __gmpz_fits_slong_p(mpzPointer);
    }

    /**
     * Returns {@code true} if and only if this {@code MPZ} fits into a native
     * unsigned int.
     */
    public boolean fitsUint() {
        return __gmpz_fits_uint_p(mpzPointer);
    }

    /**
     * Returns {@code true} if and only if this {@code MPZ} fits into a native
     * signed int.
     */
    public boolean fitsSint() {
        return __gmpz_fits_sint_p(mpzPointer);
    }

    /**
     * Returns {@code true} if and only if this {@code MPZ} fits into a native
     * unsigned short.
     */
    public boolean fitsUshort() {
        return __gmpz_fits_ushort_p(mpzPointer);
    }

    /**
     * Returns {@code true} if and only if this {@code MPZ} fits into a native
     * signed short.
     */
    public boolean fitsSshort() {
        return __gmpz_fits_sshort_p(mpzPointer);
    }

    /**
     * Returns {@code true} if and only if this {@code MPZ} is odd.
     */
    public boolean isOdd() {
        return fdiv(2) != 0;
    }

    /**
     * Returns {@code true} if and only if this {@code MPZ} is even
     */
    public boolean isEven() {
        return fdiv(2) == 0;
    }

    /**
     * Returns the size of this {@code MPZ}measured in number of digits in the given
     * base. See the documentation for the GMP function {@code mpz_sizeinbase} for
     * details.
     */
    public long sizeinbase(int base) {
        if (base < 2 || base > 62)
            throw new IllegalArgumentException("The value of base can vary from 2 to 62");
        return __gmpz_sizeinbase(mpzPointer, base).longValue();
    }

    // Java name aliases

    /**
     * Builds a new {@code MPZ} initialized to zero.
     */
    public MPZ() {
        mpzPointer = new MPZPointer();
        __gmpz_init(mpzPointer);
        GMP.cleaner.register(this, new MPZCleaner(mpzPointer));
    }

    /**
     * Builds a new {@code MPZ} initialized to the value of {@code op}.
     */
    public MPZ(MPZ op) {
        mpzPointer = new MPZPointer();
        __gmpz_init_set(mpzPointer, op.mpzPointer);
        GMP.cleaner.register(this, new MPZCleaner(mpzPointer));
    }

    /**
     * Builds a new {@code MPZ} initialized to the value of the signed long
     * {@code op}.
     */
    public MPZ(long op) {
        mpzPointer = new MPZPointer();
        __gmpz_init_set_si(mpzPointer, new NativeLong(op));
        GMP.cleaner.register(this, new MPZCleaner(mpzPointer));
    }

    /**
     * Builds a new {@code MPZ} initialized to the truncated value of {@code d}.
     */
    public MPZ(double op) {
        mpzPointer = new MPZPointer();
        __gmpz_init_set_d(mpzPointer, op);
        GMP.cleaner.register(this, new MPZCleaner(mpzPointer));
    }

    /**
     * Builds a new {@code MPZ} initialized from the string {@code str} the given
     * {@code base}. See the GMP function {@code mpz_init_set_str}.
     *
     * @throws IllegalArgumentException if either {@code base}. is not valid or
     *                                  {@code str} is not a valid string in the
     *                                  given base.
     *
     */
    public MPZ(String str, int base) {
        mpzPointer = new MPZPointer();
        int result = __gmpz_init_set_str(mpzPointer, str, base);
        if (result == -1) {
            __gmpz_clear(mpzPointer);
            throw new IllegalArgumentException(
                    "either base is not valid or str is not a valid number in the given base");
        }
        GMP.cleaner.register(this, new MPZCleaner(mpzPointer));
    }

    /**
     * Builds a new {@code MPZ} initialized from the provided decimal string
     * representation.
     */
    public MPZ(String str) {
        this(str, 10);
    }

    /**
     * Sets the value of this number to the value of {@code op}.
     */
    public MPZ setValue(MPZ op) {
        return set(op);
    }

    /**
     * Sets the value of this number to the value of the signed long {@code op}.
     */
    public MPZ setValue(long op) {
        return setSi(op);
    }

    /**
     * Sets the value of this number to the truncated value op {@code op}.
     *
     * @throws IllegalArgumentException if {@code op} is not a finite number. In
     *                                  this case, {@code this} is not altered.
     */
    public MPZ setValue(double op) {
        return set(op);
    }

    /**
     * Set the value of this number to the value represented by the string
     * {@code str} in the given {@code base}. See the GMP function
     * {@code mpz_set_str}.
     *
     * @throws IllegalArgumentException if either {@code base} is not valid or
     *                                  {@code str} is not a valid number
     *                                  representation in the given base. In this
     *                                  case, {@code this} is not altered.
     */
    public MPZ setValue(String str, int base) {
        var result = set(str, base);
        if (result == -1)
            throw new IllegalArgumentException(
                    "either base is not valid or str is not a valid number in the given base");
        return this;
    }

    /**
     * Set the value of this number to the value represented by the string
     * {@code str} in decimal base.
     *
     * @throws IllegalArgumentException if {@code str} is not a valid number
     *                                  representation in decimal base.
     */
    public MPZ setValue(String str) {
        var result = set(str, 10);
        if (result == -1)
            throw new IllegalArgumentException("str is not a valid number in decimal base");
        return this;
    }

    /**
     * Compares this number with {@code op}. Returns a negative integer, zero, or a
     * positive integer as this is less than, equal to, or greater than {@code op}.
     * This order is compatible with equality.
     */
    @Override
    public int compareTo(MPZ op) {
        return __gmpz_cmp(mpzPointer, op.mpzPointer);
    }

    /**
     * Indicates whether some other object is equal this number. It is true if
     * {@code op} is an {@code MPZ} which represents the same number as
     * {@code this}.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof MPZ) {
            var z = (MPZ) obj;
            return __gmpz_cmp(mpzPointer, z.mpzPointer) == 0;
        }
        return false;
    }

    /***
     * Returns a hash code value for the number. In particular, it uses the output
     * of {@code intValue} as an hash.
     */
    @Override
    public int hashCode() {
        return intValue();
    }

    /**
     * If this numberfits into a signed long int returns the value of {@code op}.
     * Otherwise returns the least significant part of {@code op}, with the same
     * sign as {@code op}. See the GMP {@code mpz_get_si} function.
     */
    @Override
    public long longValue() {
        return getSi();
    }

    /**
     * Returns the value of this {@code MPZ} as an {@code int}, after a narrowing
     * primitive conversion from {@link longValue}.
     */
    @Override
    public int intValue() {
        return (int) longValue();
    }

    /**
     * Returns the value of this {@code MPZ} as {@code double}, truncating if
     * necessary. See the GMP {@code mpz_get_d} function.
     */
    @Override
    public double doubleValue() {
        return getD();
    }

    /**
     * Returns the value of this {@code MPZ} as {@code float}, after a narrowing
     * primitive conversion from {@link doubleValue}.
     */
    @Override
    public float floatValue() {
        return (float) doubleValue();
    }

    /**
     * Convert op to a string of digits in the decimal base. See the GMP
     * {@code mpz_get_str} function.
     */
    @Override
    public String toString() {
        return getStr(10);
    }
}
