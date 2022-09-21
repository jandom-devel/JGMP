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
 * The class for the `MPZ` data type, i.e., multi-precision integer numbers.
 */
public class MPZ extends Number implements Comparable<MPZ> {
    /**
     * The pointer to the native MPZ object.
     */
    private MPZPointer mpzPointer;

    /**
     * Dummy type used to mark constructors with additional inputs specifying the
     * amount of memory to reserve in andvance.
     */
    public static enum Reserve {
        RESERVE;
    }

    /**
     * Dummy type used to mark operations which treat a `long` variable as it were
     * unsigned.
     */
    public static enum Unsigned {
        UNSIGNED;
    }

    /**
     * Result type of the `probablePrime` method.
     */
    public static enum PrimalityStatus {
        NON_PRIME, PROBABLY_PRIME, PRIME
    }

    /**
     * Cleaner for the `MPZ` class.
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
     * Returns the native pointer to the MPZ object.
     */
    public MPZPointer getPointer() {
        return mpzPointer;
    }

    // Initializing Integers

    public MPZ() {
        mpzPointer = new MPZPointer();
        __gmpz_init(mpzPointer);
        GMP.cleaner.register(this, new MPZCleaner(mpzPointer));
    }

    public MPZ(int n, Reserve dummy) {
        mpzPointer = new MPZPointer();
        __gmpz_init2(mpzPointer, new MPBitCntT(n));
        GMP.cleaner.register(this, new MPZCleaner(mpzPointer));
    }

    public MPZ realloc2(int n) {
        __gmpz_realloc2(mpzPointer, new MPBitCntT(n));
        return this;
    }

    // Assigning Integers

    public MPZ setValue(MPZ op) {
        __gmpz_set(mpzPointer, op.mpzPointer);
        return this;
    }

    public MPZ setValueSigned(long op) {
        __gmpz_set_si(mpzPointer, new NativeLong(op));
        return this;
    }

    public MPZ setValue(long op) {
        __gmpz_set_ui(mpzPointer, new NativeUnsignedLong(op));
        return this;
    }

    public MPZ setValue(double op) {
        __gmpz_set_d(mpzPointer, op);
        return this;
    }

    /*
     * public MPZ setValue(MPQ op) {
     * __gmpz_set_q(mpzPointer, op.mpqPointer);
     * return this;
     * }
     *
     * public MPZ setValue(MPF op) {
     * __gmpz_set_f(mpzPointer, op.mpfPointer);
     * return this;
     */

    public MPZ setValue(String str, int base) {
        int result = __gmpz_set_str(mpzPointer, str, base);
        if (result == -1)
            throw new IllegalArgumentException("Parameter str is not a valid number in base " + base);
        return this;
    }

    public MPZ setValue(String str) {
        return setValue(str, 10);
    }

    public MPZ swap(MPZ op) {
        __gmpz_swap(mpzPointer, op.mpzPointer);
        return this;
    }

    // Simultaneous Integer Init & Assign

    public MPZ(MPZ op) {
        mpzPointer = new MPZPointer();
        __gmpz_init_set(mpzPointer, op.mpzPointer);
        GMP.cleaner.register(this, new MPZCleaner(mpzPointer));
    }

    public MPZ(long op) {
        mpzPointer = new MPZPointer();
        __gmpz_init_set_si(mpzPointer, new NativeLong(op));
        GMP.cleaner.register(this, new MPZCleaner(mpzPointer));
    }

    public MPZ(long op, Unsigned u) {
        mpzPointer = new MPZPointer();
        __gmpz_init_set_ui(mpzPointer, new NativeUnsignedLong(op));
        GMP.cleaner.register(this, new MPZCleaner(mpzPointer));
    }

    public MPZ(double op) {
        mpzPointer = new MPZPointer();
        __gmpz_init_set_d(mpzPointer, op);
        GMP.cleaner.register(this, new MPZCleaner(mpzPointer));
    }

    public MPZ(String str, int base) {
        mpzPointer = new MPZPointer();
        int result = __gmpz_init_set_str(mpzPointer, str, base);
        if (result == -1) {
            __gmpz_clear(mpzPointer);
            throw new IllegalArgumentException("Parameter str is not a valid number in base " + base);
        }
        GMP.cleaner.register(this, new MPZCleaner(mpzPointer));
    }

    public MPZ(String str) {
        this(str, 10);
    }

    // Converting Integers

    public long getUi() {
        return __gmpz_get_ui(mpzPointer).longValue();
    }

    public long getSi() {
        return __gmpz_get_si(mpzPointer).longValue();
    }

    public double getD() {
        return __gmpz_get_d(mpzPointer);
    }

    public Pair<Double, Long> getD2Exp() {
        var pexp = new NativeLongByReference();
        var d = __gmpz_get_d_2exp(pexp, mpzPointer);
        return new Pair<>(d, pexp.getValue().longValue());
    }

    public String getStr(int base) {
        Pointer ps = __gmpz_get_str(null, base, mpzPointer);
        var s = ps.getString(0);
        Native.free(Pointer.nativeValue(ps));
        return s;
    }

    // Integer Arithmetic

    public MPZ addAssign(MPZ op1, MPZ op2) {
        __gmpz_add(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    public MPZ add(MPZ op) {
        return new MPZ().addAssign(this, op);
    }

    public MPZ addAssign(MPZ op1, long op2) {
        __gmpz_add_ui(mpzPointer, op1.mpzPointer, new NativeUnsignedLong(op2));
        return this;
    }

    public MPZ add(long op) {
        return new MPZ().addAssign(this, op);
    }

    public MPZ subAssign(MPZ op1, MPZ op2) {
        __gmpz_sub(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    public MPZ sub(MPZ op) {
        return new MPZ().subAssign(this, op);
    }

    public MPZ subAssign(MPZ op1, long op2) {
        __gmpz_sub_ui(mpzPointer, op1.mpzPointer, new NativeUnsignedLong(op2));
        return this;
    }

    public MPZ sub(long op) {
        return new MPZ().subAssign(this, op);
    }

    public MPZ subReverseAssign(long op1, MPZ op2) {
        __gmpz_ui_sub(mpzPointer, new NativeUnsignedLong(op1), op2.mpzPointer);
        return this;
    }

    public MPZ subReverse(long op) {
        return new MPZ().subReverseAssign(op, this);
    }

    public MPZ mulAssign(MPZ op1, MPZ op2) {
        __gmpz_mul(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    public MPZ mul(MPZ op) {
        return new MPZ().mulAssign(this, op);
    }

    public MPZ mulAssign(MPZ op1, long op2) {
        __gmpz_mul_ui(mpzPointer, op1.mpzPointer, new NativeUnsignedLong(op2));
        return this;
    }

    public MPZ mul(long op) {
        return new MPZ().mulAssign(this, op);
    }

    public MPZ mulAssignSigned(MPZ op1, long op2) {
        __gmpz_mul_si(mpzPointer, op1.mpzPointer, new NativeLong(op2));
        return this;
    }

    public MPZ mulSigned(long op) {
        return new MPZ().mulAssignSigned(this, op);
    }

    public MPZ addmulAssign(MPZ op1, MPZ op2) {
        __gmpz_addmul(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    public MPZ addmul(MPZ op1, MPZ op2) {
        return new MPZ(this).addmulAssign(op1, op2);
    }

    public MPZ addmulAssign(MPZ op1, long op2) {
        __gmpz_addmul_ui(mpzPointer, op1.mpzPointer, new NativeUnsignedLong(op2));
        return this;
    }

    public MPZ addmul(MPZ op1, long op2) {
        return new MPZ(this).addmulAssign(op1, op2);
    }

    public MPZ submulAssign(MPZ op1, MPZ op2) {
        __gmpz_submul(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    public MPZ submul(MPZ op1, MPZ op2) {
        return new MPZ(this).submulAssign(op1, op2);
    }

    public MPZ submulAssign(MPZ op1, long op2) {
        __gmpz_submul_ui(mpzPointer, op1.mpzPointer, new NativeUnsignedLong(op2));
        return this;
    }

    public MPZ submul(MPZ op1, long op2) {
        return new MPZ(this).submulAssign(op1, op2);
    }

    public MPZ mul2ExpAssign(MPZ op1, long op2) {
        __gmpz_mul_2exp(mpzPointer, op1.mpzPointer, new MPBitCntT(op2));
        return this;
    }

    public MPZ mul2Exp(long op) {
        return new MPZ().mul2ExpAssign(this, op);
    }

    public MPZ negAssign(MPZ op) {
        __gmpz_neg(mpzPointer, op.mpzPointer);
        return this;
    }

    public MPZ neg() {
        return new MPZ().negAssign(this);
    }

    public MPZ absAssign(MPZ op) {
        __gmpz_abs(mpzPointer, op.mpzPointer);
        return this;
    }

    public MPZ abs() {
        return new MPZ().absAssign(this);
    }

    // Integer Division

    public MPZ cdivqAssign(MPZ n, MPZ d) {
        __gmpz_cdiv_q(mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    public MPZ cdivrAssign(MPZ n, MPZ d) {
        __gmpz_cdiv_r(mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    public MPZ cdivqrAssign(MPZ r, MPZ n, MPZ d) {
        if (mpzPointer == r.mpzPointer)
            throw new IllegalArgumentException("The target of this method cannot point to the the same object as r");
        __gmpz_cdiv_qr(mpzPointer, r.mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    public long cdivqAssign(MPZ n, long d) {
        return __gmpz_cdiv_q_ui(mpzPointer, n.mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    public long cdivrAssign(MPZ n, long d) {
        return __gmpz_cdiv_r_ui(mpzPointer, n.mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    public long cdivqrAssign(MPZ r, MPZ n, long d) {
        if (mpzPointer == r.mpzPointer)
            throw new IllegalArgumentException("The target of this method cannot point to the the same object as r");
        return __gmpz_cdiv_qr_ui(mpzPointer, r.mpzPointer, n.mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    public long cdiv(long d) {
        return __gmpz_cdiv_ui(mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    public MPZ cdivq2ExpAssign(MPZ n, long b) {
        __gmpz_cdiv_q_2exp(mpzPointer, n.mpzPointer, new MPBitCntT(b));
        return this;
    }

    public MPZ cdivr2ExpAssign(MPZ n, long b) {
        __gmpz_cdiv_r_2exp(mpzPointer, n.mpzPointer, new MPBitCntT(b));
        return this;
    }

    public MPZ cdivq(MPZ d) {
        return new MPZ().cdivqAssign(this, d);
    }

    public MPZ cdivr(MPZ d) {
        return new MPZ().cdivrAssign(this, d);
    }

    public Pair<MPZ, MPZ> cdivqr(MPZ d) {
        MPZ q = new MPZ(), r = new MPZ();
        q.cdivqrAssign(r, this, d);
        return new Pair<>(q, r);
    }

    public MPZ cdivq2Exp(long b) {
        return new MPZ().cdivq2ExpAssign(this, b);
    }

    public MPZ cdivr2Exp(long b) {
        return new MPZ().cdivr2ExpAssign(this, b);
    }

    public MPZ fdivqAssign(MPZ n, MPZ d) {
        __gmpz_fdiv_q(mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    public MPZ fdivrAssign(MPZ n, MPZ d) {
        __gmpz_fdiv_r(mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    public MPZ fdivqrAssign(MPZ r, MPZ n, MPZ d) {
        if (mpzPointer == r.mpzPointer)
            throw new IllegalArgumentException("The target of this method cannot point to the the same object as r");
        __gmpz_fdiv_qr(mpzPointer, r.mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    public long fdivqAssign(MPZ n, long d) {
        return __gmpz_fdiv_q_ui(mpzPointer, n.mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    public long fdivrAssign(MPZ n, long d) {
        return __gmpz_fdiv_r_ui(mpzPointer, n.mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    public long fdivqrAssign(MPZ r, MPZ n, long d) {
        if (mpzPointer == r.mpzPointer)
            throw new IllegalArgumentException("The target of this method cannot point to the the same object as r");
        return __gmpz_fdiv_qr_ui(mpzPointer, r.mpzPointer, n.mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    public long fdiv(long d) {
        return __gmpz_fdiv_ui(mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    public MPZ fdivq2ExpAssign(MPZ n, long b) {
        __gmpz_fdiv_q_2exp(mpzPointer, n.mpzPointer, new NativeUnsignedLong(b));
        return this;
    }

    public MPZ fdivr2ExpAssign(MPZ n, long b) {
        __gmpz_fdiv_r_2exp(mpzPointer, n.mpzPointer, new NativeUnsignedLong(b));
        return this;
    }

    public MPZ fdivq(MPZ d) {
        return new MPZ().fdivqAssign(this, d);
    }

    public MPZ fdivr(MPZ d) {
        return new MPZ().fdivrAssign(this, d);
    }

    public Pair<MPZ, MPZ> fdivqr(MPZ d) {
        MPZ q = new MPZ(), r = new MPZ();
        q.fdivqrAssign(r, this, d);
        return new Pair<>(q, r);
    }

    public MPZ fdivq2Exp(long b) {
        return new MPZ().fdivq2ExpAssign(this, b);
    }

    public MPZ fdivr2Exp(long b) {
        return new MPZ().fdivr2ExpAssign(this, b);
    }

    public MPZ tdivqAssign(MPZ n, MPZ d) {
        __gmpz_fdiv_q(mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    public MPZ tdivrAssign(MPZ n, MPZ d) {
        __gmpz_tdiv_r(mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    public MPZ tdivqrAssign(MPZ r, MPZ n, MPZ d) {
        if (mpzPointer == r.mpzPointer)
            throw new IllegalArgumentException("The target of this method cannot point to the the same object as r");
        __gmpz_tdiv_qr(mpzPointer, r.mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    public MPZ tdivqAssign(MPZ n, long d) {
        __gmpz_tdiv_q_ui(mpzPointer, n.mpzPointer, new NativeUnsignedLong(d));
        return this;
    }

    public long tdivrAssign(MPZ n, long d) {
        return __gmpz_tdiv_r_ui(mpzPointer, n.mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    public long tdivqrAssign(MPZ r, MPZ n, long d) {
        if (mpzPointer == r.mpzPointer)
            throw new IllegalArgumentException("The target of this method cannot point to the the same object as r");
        return __gmpz_tdiv_qr_ui(mpzPointer, r.mpzPointer, n.mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    public long tdiv(long d) {
        return __gmpz_tdiv_ui(mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    public MPZ tdivq2ExpAssign(MPZ n, long b) {
        __gmpz_tdiv_q_2exp(mpzPointer, n.mpzPointer, new NativeUnsignedLong(b));
        return this;
    }

    public MPZ tdivr2ExpAssign(MPZ n, long b) {
        __gmpz_tdiv_r_2exp(mpzPointer, n.mpzPointer, new NativeUnsignedLong(b));
        return this;
    }

    public MPZ tdivq(MPZ d) {
        return new MPZ().tdivqAssign(this, d);
    }

    public MPZ tdivr(MPZ d) {
        return new MPZ().tdivrAssign(this, d);
    }

    public Pair<MPZ, MPZ> tdivqr(MPZ d) {
        MPZ q = new MPZ(), r = new MPZ();
        q.tdivqrAssign(r, this, d);
        return new Pair<>(q, r);
    }

    public MPZ tdivq2Exp(long b) {
        return new MPZ().tdivq2ExpAssign(this, b);
    }

    public MPZ tdivr2Exp(long b) {
        return new MPZ().tdivr2ExpAssign(this, b);
    }

    public MPZ modAssign(MPZ n, MPZ d) {
        __gmpz_mod(mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    public MPZ mod(MPZ d) {
        return new MPZ().modAssign(this, d);
    }

    public long modAssign(MPZ n, long d) {
        return __gmpz_mod_ui(mpzPointer, n.mpzPointer, new NativeUnsignedLong(d)).longValue();
    }

    public long mod(long d) {
        return fdiv(d);
    }

    public MPZ divexactAssign(MPZ n, MPZ d) {
        __gmpz_divexact(mpzPointer, n.mpzPointer, d.mpzPointer);
        return this;
    }

    public MPZ divexact(MPZ d) {
        return new MPZ().divexactAssign(this, d);
    }

    public MPZ divexactAssign(MPZ n, long d) {
        __gmpz_divexact_ui(mpzPointer, n.mpzPointer, new NativeUnsignedLong(d));
        return this;
    }

    public MPZ divexact(long d) {
        return new MPZ().divexactAssign(this, d);
    }

    public boolean isDivisible(MPZ d) {
        return __gmpz_divisible_p(mpzPointer, d.mpzPointer);
    }

    public boolean isDivisible(long d) {
        return __gmpz_divisible_ui_p(mpzPointer, new NativeUnsignedLong(d));
    }

    public boolean isDivisible2Exp(long b) {
        return __gmpz_divisible_2exp_p(mpzPointer, new MPBitCntT(b));
    }

    public boolean isCongruent(MPZ c, MPZ d) {
        return __gmpz_congruent_p(mpzPointer, c.mpzPointer, d.mpzPointer);
    }

    public boolean isCongruent(long c, long d) {
        return __gmpz_congruent_ui_p(mpzPointer, new NativeUnsignedLong(c), new NativeUnsignedLong(d));
    }

    public boolean isCongruent2Exp(MPZ c, long b) {
        return __gmpz_congruent_2exp_p(mpzPointer, c.mpzPointer, new MPBitCntT(b));
    }

    // Integer Exponentiation

    public MPZ powmAssign(MPZ base, MPZ exp, MPZ mod) {
        __gmpz_powm(mpzPointer, base.mpzPointer, exp.mpzPointer, mod.mpzPointer);
        return this;
    }

    public MPZ powm(MPZ exp, MPZ mod) {
        return new MPZ().powmAssign(this, exp, mod);
    }

    public MPZ powmAssign(MPZ base, long exp, MPZ mod) {
        __gmpz_powm_ui(mpzPointer, base.mpzPointer, new NativeUnsignedLong(exp), mod.mpzPointer);
        return this;
    }

    public MPZ powm(long exp, MPZ mod) {
        return new MPZ().powmAssign(this, exp, mod);
    }

    public MPZ powmSecAssign(MPZ base, MPZ exp, MPZ mod) {
        __gmpz_powm_sec(mpzPointer, base.mpzPointer, exp.mpzPointer, mod.mpzPointer);
        return this;
    }

    public MPZ powmSec(MPZ exp, MPZ mod) {
        return new MPZ().powmSecAssign(this, exp, mod);
    }

    public MPZ powAssign(MPZ base, long exp) {
        __gmpz_pow_ui(mpzPointer, base.mpzPointer, new NativeUnsignedLong(exp));
        return this;
    }

    public MPZ pow(long exp) {
        return new MPZ().powAssign(this, exp);
    }

    public MPZ powAssign(long base, long exp) {
        __gmpz_ui_pow_ui(mpzPointer, new NativeUnsignedLong(base), new NativeUnsignedLong(exp));
        return this;
    }

    public static MPZ pow(long base, long exp) {
        return new MPZ().powAssign(base, exp);
    }

    // Integer Roots

    public MPZ rootAssign(MPZ op, long n) {
        __gmpz_root(mpzPointer, op.mpzPointer, new NativeUnsignedLong(n));
        return this;
    }

    public MPZ root(long n) {
        return new MPZ().rootAssign(this, n);
    }

    public MPZ rootremAssign(MPZ rem, MPZ op, long n) {
        __gmpz_rootrem(mpzPointer, rem.mpzPointer, op.mpzPointer, new NativeUnsignedLong(n));
        return this;
    }

    public Pair<MPZ, MPZ> rootrem(long n) {
        MPZ res = new MPZ(), rem = new MPZ();
        res.rootremAssign(rem, this, n);
        return new Pair<>(res, rem);
    }

    public MPZ sqrtAssign(MPZ op) {
        __gmpz_sqrt(mpzPointer, op.mpzPointer);
        return this;
    }

    public MPZ sqrt() {
        return new MPZ().sqrtAssign(this);
    }

    public MPZ sqrtremAssign(MPZ rem, MPZ op) {
        __gmpz_sqrtrem(mpzPointer, rem.mpzPointer, op.mpzPointer);
        return this;
    }

    public Pair<MPZ, MPZ> sqrtrem() {
        MPZ res = new MPZ(), rem = new MPZ();
        res.sqrtremAssign(rem, this);
        return new Pair<>(res, rem);
    }

    public boolean isPerfectPower() {
        return __gmpz_perfect_power_p(mpzPointer);
    }

    public boolean isPerfectSquare() {
        return __gmpz_perfect_square_p(mpzPointer);
    }

    // Number Theoretic Functions

    public PrimalityStatus isProbabPrime(int reps) {
        var res = __gmpz_probab_prime_p(mpzPointer, reps);
        return PrimalityStatus.values()[res];
    }

    public MPZ nextprimeAssign(MPZ op) {
        __gmpz_nextprime(mpzPointer, op.mpzPointer);
        return this;
    }

    public MPZ nextprime() {
        return new MPZ().nextprimeAssign(this);
    }

    public MPZ gcdAssign(MPZ op1, MPZ op2) {
        __gmpz_gcd(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    public MPZ gcd(MPZ op) {
        return new MPZ().gcdAssign(this, op);
    }

    public MPZ gcdAssign(MPZ op1, long op2) {
        __gmpz_gcd_ui(mpzPointer, op1.mpzPointer, new NativeUnsignedLong(op2));
        return this;
    }

    public long gcd(long op2) {
        return __gmpz_gcd_ui(null, mpzPointer, new NativeUnsignedLong(op2)).longValue();
    }

    public MPZ gcdextAssign(MPZ s, MPZ t, MPZ op1, MPZ op2) {
        __gmpz_gcdext(mpzPointer, s == null ? null : s.mpzPointer, t == null ? null : t.mpzPointer, op1.mpzPointer,
                op2.mpzPointer);
        return this;
    }

    public Triplet<MPZ, MPZ, MPZ> gcdext(MPZ op) {
        MPZ r = new MPZ(), s = new MPZ(), t = new MPZ();
        r.gcdextAssign(s, t, this, op);
        return new Triplet<>(r, s, t);
    }

    public MPZ lcmAssign(MPZ op1, MPZ op2) {
        __gmpz_lcm(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    public MPZ lcm(MPZ op) {
        return new MPZ().lcmAssign(this, op);
    }

    public MPZ lcmAssign(MPZ op1, long op2) {
        __gmpz_lcm_ui(mpzPointer, op1.mpzPointer, new NativeUnsignedLong(op2));
        return this;
    }

    public MPZ lcm(long op) {
        return new MPZ().lcmAssign(this, op);
    }

    public boolean invertAssign(MPZ op1, MPZ op2) {
        return __gmpz_invert(mpzPointer, op1.mpzPointer, op2.mpzPointer);
    }

    public Optional<MPZ> invert(MPZ modulus) {
        if (modulus.compareTo(0) == 0)
            return Optional.empty();
        var res = new MPZ();
        var exists = res.invertAssign(this, modulus);
        return exists ? Optional.of(res) : Optional.empty();
    }

    public int jacobi(MPZ op) {
        return __gmpz_jacobi(mpzPointer, op.mpzPointer);
    }

    public int legendre(MPZ op) {
        return __gmpz_legendre(mpzPointer, op.mpzPointer);
    }

    public int kronecker(MPZ op) {
        // the jacobi GMP function already implements the Kronecker extension
        return __gmpz_jacobi(mpzPointer, op.mpzPointer);
    }

    public int kronecker(long op) {
        return __gmpz_kronecker_si(mpzPointer, new NativeLong(op));
    }

    public int kronecker(long op, Unsigned u) {
        return __gmpz_kronecker_ui(mpzPointer, new NativeUnsignedLong(op));
    }

    public int kroneckerReverse(long op) {
        return __gmpz_si_kronecker(new NativeLong(op), mpzPointer);
    }

    public int kroneckerReverse(long op, Unsigned u) {
        return __gmpz_ui_kronecker(new NativeUnsignedLong(op), mpzPointer);
    }

    public long removeAssign(MPZ op, MPZ f) {
        return __gmpz_remove(mpzPointer, op.mpzPointer, f.mpzPointer).longValue();
    }

    public Pair<MPZ, Long> remove(MPZ f) {
        var res = new MPZ();
        var count = __gmpz_remove(res.mpzPointer, mpzPointer, f.mpzPointer);
        return new Pair<MPZ, Long>(res, count.longValue());
    }

    public MPZ facAssign(long n) {
        __gmpz_fac_ui(mpzPointer, new NativeUnsignedLong(n));
        return this;
    }

    public static MPZ fac(long n) {
        return new MPZ().facAssign(n);
    }

    public MPZ dfacAssign(long n) {
        __gmpz_2fac_ui(mpzPointer, new NativeUnsignedLong(n));
        return this;
    }

    public static MPZ dfac(long n) {
        return new MPZ().dfacAssign(n);
    }

    public MPZ mfacAssign(long n, long m) {
        __gmpz_mfac_uiui(mpzPointer, new NativeUnsignedLong(n), new NativeUnsignedLong(m));
        return this;
    }

    public static MPZ mfac(long n, long m) {
        return new MPZ().mfacAssign(n, m);
    }

    public MPZ primorialAssign(long n) {
        __gmpz_primorial_ui(mpzPointer, new NativeUnsignedLong(n));
        return this;
    }

    public static MPZ primorial(long n) {
        return new MPZ().primorialAssign(n);
    }

    public MPZ binAssign(MPZ n, long k) {
        __gmpz_bin_ui(mpzPointer, n.mpzPointer, new NativeUnsignedLong(k));
        return this;
    }

    public MPZ bin(long k) {
        return new MPZ().binAssign(this, k);
    }

    public MPZ binAssign(long n, long k) {
        __gmpz_bin_uiui(mpzPointer, new NativeUnsignedLong(n), new NativeUnsignedLong(k));
        return this;
    }

    public static MPZ bin(long n, long k) {
        return new MPZ().binAssign(n, k);
    }

    public MPZ fibAssign(long n) {
        __gmpz_fib_ui(mpzPointer, new NativeUnsignedLong(n));
        return this;
    }

    public static MPZ fib(long n) {
        return new MPZ().fibAssign(n);
    }

    public MPZ fib2Assign(MPZ fnsub1, long n) {
        __gmpz_fib2_ui(mpzPointer, fnsub1.mpzPointer, new NativeUnsignedLong(n));
        return this;
    }

    public static Pair<MPZ, MPZ> fib2(long n) {
        MPZ fnsub1 = new MPZ(), fn = new MPZ();
        fn.fib2Assign(fnsub1, n);
        return new Pair<>(fn, fnsub1);
    }

    public MPZ lucnumAssign(long n) {
        __gmpz_lucnum_ui(mpzPointer, new NativeUnsignedLong(n));
        return this;
    }

    public static MPZ lucnum(long n) {
        return new MPZ().lucnumAssign(n);
    }

    public MPZ lucnum2Assign(MPZ fnsub1, long n) {
        __gmpz_lucnum2_ui(mpzPointer, fnsub1.mpzPointer, new NativeUnsignedLong(n));
        return this;
    }

    public static Pair<MPZ, MPZ> lucnum2(long n) {
        MPZ fnsub1 = new MPZ(), fn = new MPZ();
        fn.lucnum2Assign(fnsub1, n);
        return new Pair<>(fn, fnsub1);
    }

    // Integer Comparisons

    @Override
    public int compareTo(MPZ op) {
        return __gmpz_cmp(mpzPointer, op.mpzPointer);
    }

    public int compareTo(double op) {
        return __gmpz_cmp_d(mpzPointer, op);
    }

    public int compareTo(long op) {
        return __gmpz_cmp_si(mpzPointer, new NativeLong(op));
    }

    public int compareTo(long op, Unsigned u) {
        return __gmpz_cmp_ui(mpzPointer, new NativeUnsignedLong(op));
    }

    public int compareAbsTo(MPZ op) {
        return __gmpz_cmpabs(mpzPointer, op.mpzPointer);
    }

    public int compareAbsTo(double op) {
        return __gmpz_cmpabs_d(mpzPointer, op);
    }

    public int compareAbsTo(long op) {
        return __gmpz_cmpabs_ui(mpzPointer, new NativeUnsignedLong(Math.abs(op)));
    }

    public int compareAbsTo(long op, Unsigned u) {
        return __gmpz_cmpabs_ui(mpzPointer, new NativeUnsignedLong(op));
    }

    // Integer Logic and Bit Fiddling

    public MPZ andAssign(MPZ op1, MPZ op2) {
        __gmpz_and(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    public MPZ and(MPZ op) {
        return new MPZ().andAssign(this, op);
    }

    public MPZ iorAssign(MPZ op1, MPZ op2) {
        __gmpz_ior(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    public MPZ ior(MPZ op) {
        return new MPZ().iorAssign(this, op);
    }

    public MPZ xorAssign(MPZ op1, MPZ op2) {
        __gmpz_xor(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    public MPZ xor(MPZ op) {
        return new MPZ().xorAssign(this, op);
    }

    public MPZ comAssign(MPZ op) {
        __gmpz_com(mpzPointer, op.mpzPointer);
        return this;
    }

    public MPZ com() {
        return new MPZ().comAssign(this);
    }

    public long popcount() {
        return __gmpz_popcount(mpzPointer).longValue();
    }

    public long hamdist(MPZ op) {
        return __gmpz_hamdist(mpzPointer, op.mpzPointer).longValue();
    }

    public long scan0(long starting_bit) {
        return __gmpz_scan0(mpzPointer, new MPBitCntT(starting_bit)).longValue();
    }

    public long scan1(long starting_bit) {
        return __gmpz_scan1(mpzPointer, new MPBitCntT(starting_bit)).longValue();
    }

    public MPZ setbitAssign(long index) {
        __gmpz_setbit(mpzPointer, new MPBitCntT(index));
        return this;
    }

    public MPZ setbit(long index) {
        return new MPZ(this).setbitAssign(index);
    }

    public MPZ clrbitAssign(long index) {
        __gmpz_clrbit(mpzPointer, new MPBitCntT(index));
        return this;
    }

    public MPZ clrbit(long index) {
        return new MPZ(this).clrbitAssign(index);
    }

    public MPZ combitAssign(long index) {
        __gmpz_combit(mpzPointer, new MPBitCntT(index));
        return this;
    }

    public MPZ combit(long index) {
        return new MPZ(this).combitAssign(index);
    }

    public int tstbit(long index) {
        return __gmpz_tstbit(mpzPointer, new MPBitCntT(index));
    }

    public MPZ urandombAssign(RandomState s, long n) {
        __gmpz_urandomb(mpzPointer, s.getPointer(), new MPBitCntT(n));
        return this;
    }

    // Random Number Functions

    public static MPZ urandomb(RandomState s, long n) {
        var z = new MPZ();
        z.urandombAssign(s, n);
        return z;
    }

    public MPZ urandommAssign(RandomState s, MPZ n) {
        __gmpz_urandomm(mpzPointer, s.getPointer(), n.mpzPointer);
        return this;
    }

    public static MPZ urandomm(RandomState s, MPZ n) {
        var z = new MPZ();
        z.urandommAssign(s, n);
        return z;
    }

    public MPZ rrandombAssign(RandomState s, long n) {
        __gmpz_rrandomb(mpzPointer, s.getPointer(), new MPBitCntT(n));
        return this;
    }

    public static MPZ rrandomb(RandomState s, long n) {
        var z = new MPZ();
        z.urandombAssign(s, n);
        return z;
    }

    public MPZ randomAssign(long max_size) {
        __gmpz_random(mpzPointer, new MPSizeT(max_size));
        return this;
    }

    public static MPZ random(long max_size) {
        return (new MPZ()).randomAssign(max_size);
    }

    public MPZ random2Assign(long max_size) {
        __gmpz_random2(mpzPointer, new MPSizeT(max_size));
        return this;
    }

    public static MPZ random2(long max_size) {
        return (new MPZ()).random2Assign(max_size);
    }

    // Integer Import and Export

    /**
     * Convert the content of a `ByteBuffer` into an multi-precision integer number.
     *
     * See the GMP documentation of the `mpz_import` function for the meaning of the
     * parameters. Since a `ByteBuffer` in the JVM is at most 4GB long, `size` is
     * declared to be of type `int`.
     */
    public MPZ importAssign(int order, int size, int endian, long nails, ByteBuffer op) {
        var count = op.limit() / size + (op.limit() % size == 0 ? 0 : 1);
        __gmpz_import(mpzPointer, new SizeT(count), order, new SizeT(size), endian, new SizeT(nails), op);
        return this;
    }

    public MPZ(int order, int size, int endian, long nails, ByteBuffer op) {
        this();
        importAssign(order, size, endian, nails, op);
    }

    /**
     * Convert `this` into a `ByteBuffer`.
     *
     * See the GMP documentation of the `mpz_export` function for the meaning of the
     * parameters. Note that, since a `ByteBuffer` in the JVM is at most 4GB long,
     * this method might fail for big numbers. For the same reason, `size` is
     * declared to be of type `int`.
     */
    public ByteBuffer export(int order, int size, int endian, long nails) {
        var count = new SizeTByReference();
        var p = __gmpz_export(null, count, order, new SizeT(size), endian, new SizeT(nails), mpzPointer);
        return p.getPointer().getByteBuffer(0, count.getValue().longValue());
    }

    // Miscellaneous Integer Functions

    /**
     * Determines whether `this` fits into a native unsigned long.
     */
    public boolean fitsUlong() {
        return __gmpz_fits_ulong_p(mpzPointer);
    }

    /**
     * Determines whether `this` fits into a native signed long.
     */
    public boolean fitsSlong() {
        return __gmpz_fits_slong_p(mpzPointer);
    }

    /**
     * Determines whether `this` fits into a native unsigned int.
     */
    public boolean fitsUint() {
        return __gmpz_fits_uint_p(mpzPointer);
    }

    /**
     * Determines whether `this` fits into a native signed int.
     */
    public boolean fitsSint() {
        return __gmpz_fits_sint_p(mpzPointer);
    }

    /**
     * Determines whether `this` fits into a native unsigned short.
     */
    public boolean fitsUshort() {
        return __gmpz_fits_ushort_p(mpzPointer);
    }

    /**
     * Determines whether `this` fits into a native signed short.
     */
    public boolean fitsSshort() {
        return __gmpz_fits_sshort_p(mpzPointer);
    }

    /**
     * Determines whether `this` is odd.
     */
    public boolean isOdd() {
        return fdiv(2) != 0;
    }

    /**
     * Determines whether `this` is even.
     */
    public boolean isEven() {
        return fdiv(2) == 0;
    }

    /**
     * Return the size of op measured in number of digits in the given base.
     *
     * @param base can vary from 2 to 62.
     * @throws IllegalArgumentException if base is outside its valid interval.
     */
    public long sizeinbase(int base) {
        if (base < 2 || base > 62)
            throw new IllegalArgumentException("The value of base can vary from 2 to 62");
        return __gmpz_sizeinbase(mpzPointer, base).longValue();
    }

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

    @Override
    public long longValue() {
        return getSi();
    }

    @Override
    public int intValue() {
        return (int) longValue();
    }

    @Override
    public double doubleValue() {
        return getD();
    }

    @Override
    public float floatValue() {
        return (float) doubleValue();
    }

    @Override
    public String toString() {
        return getStr(10);
    }
}
