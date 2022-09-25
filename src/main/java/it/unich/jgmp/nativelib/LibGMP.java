package it.unich.jgmp.nativelib;

import java.nio.ByteBuffer;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.NativeLongByReference;

/**
 * This class contains the static native methods corresponding to GMP functions.
 * This class also contains constants for the size of native GMP structures and
 * some global variables.
 *
 * <p>
 * Direct mapping is used for almost all the functions, with the exception of a
 * few ones with a variable number of arguments which require interface mapping.
 * The use of direct mapping forces us to adopt the real names of the functions
 * in the C library object files, which are different from the ones publicly
 * documented: the C preprocessor is used to convert from the real names to the
 * public ones. The difference between the twos is just in the prefix: for
 * example, all the GMP functions documented as {@code mpz_xxx} should be
 * written here as {@code __gmpz_xxx}. The following is the table of conversion
 * from GMP name prefixes to JGMP name prefixes.
 * </p>
 * <table border="1" style="text-align:center; border-collapse: collapse;">
 * <caption style="display: none;">Conversion table from GMP to JGMP
 * prefixes</caption>
 * <tr>
 * <th style="padding: 0ex 1ex 0ex 1ex;">GMP name prefix</th>
 * <th style="padding: 0ex 1ex 0ex 1ex;">JGMP name prefix</th>
 * </tr>
 * <tr>
 * <td>{@code mpz_}</td>
 * <td>{@code __gmpz_}</td>
 * </tr>
 * <tr>
 * <td>{@code gmp_}</td>
 * <td>{@code __gmp_}</td>
 * </tr>
 * </table>
 * <p>
 * Some documented GMP functions are actually macros: they have been
 * reimplemented here, keeping the JGMP naming convention. <em>Integer Special
 * Functions</em> and <em>Low-level Function</em>, as defined in the GMP
 * documentation, have been omitted entirely.
 * </p>
 * <p>
 * We strived to be type safe, by defining different subclasses of
 * {@code com.sun.jna.PointerType} and {@code com.sun.jna.IntegerType} for
 * different native types.
 * </p>
 */
public class LibGMP {

    /**
     * The undecorated name of the GMP library.
     */
    static final String LIBNAME = "gmp";

    /**
     * The size of the {@code mpz_t} native type.
     */
    static final int MPZ_SIZE = 4 + 4 + Native.POINTER_SIZE;

    /**
     * The size of the {@code __gmp_randstate_struct} structure.
     */
    static final int RANDSTATE_SIZE = MPZ_SIZE + 4 + Native.POINTER_SIZE;

    /**
     * Version of the GMP library.
     */
    public static final String __gmp_version;

    /**
     * The  0 (assumign no one changes it)
     */
    public static MPZPointer __gmpz_zero;

    static {
        var library = NativeLibrary.getInstance(LIBNAME);
        __gmp_version = library.getGlobalVariableAddress("__gmp_version").getPointer(0).getString(0);
        Native.register(library);
        gmpextra = (LibGmpExtra) Native.load(LibGmpExtra.class);
        __gmpz_zero = new MPZPointer();
        __gmpz_init(__gmpz_zero);
    }

    /**
     * Interface for the native functions with a variable number of arguments. These
     * are not supported by direct mapping, so we need to register them separately.
     */
    private static interface LibGmpExtra extends Library {
        int __gmp_printf(String fmt, Object... args);

        void __gmpz_inits(MPZPointer... xs);

        void __gmpz_clears(MPZPointer... xs);
    }

    /**
     * Instance of the {@code LibGmpExtra} interface created at initialization time.
     */
    private static final LibGmpExtra gmpextra;

    // Integer functions

    public static native void __gmpz_init(MPZPointer x);

    public static void __gmpz_inits(MPZPointer xs) {
        gmpextra.__gmpz_inits(xs);
    }

    public static native void __gmpz_init2(MPZPointer x, MPBitCntT n);

    public static native void __gmpz_clear(MPZPointer x);

    public static void __gmpz_clears(MPZPointer xs) {
        gmpextra.__gmpz_clears(xs);
    }

    public static native void __gmpz_realloc2(MPZPointer x, MPBitCntT n);

    public static native void __gmpz_set(MPZPointer rop, MPZPointer op);

    public static native void __gmpz_set_ui(MPZPointer rop, NativeUnsignedLong op);

    public static native void __gmpz_set_si(MPZPointer rop, NativeLong op);

    public static native void __gmpz_set_d(MPZPointer rop, double op);

    public static native void __gmpz_set_q(MPZPointer rop, Pointer op);

    public static native void __gmpz_set_f(MPZPointer rop, Pointer op);

    public static native int __gmpz_set_str(MPZPointer rop, String str, int base);

    public static native void __gmpz_swap(MPZPointer rop1, MPZPointer rop2);

    public static native void __gmpz_init_set(MPZPointer rop, MPZPointer op);

    public static native void __gmpz_init_set_ui(MPZPointer rop, NativeUnsignedLong op);

    public static native void __gmpz_init_set_si(MPZPointer rop, NativeLong op);

    public static native void __gmpz_init_set_d(MPZPointer rop, double op);

    public static native int __gmpz_init_set_str(MPZPointer rop, String str, int base);

    public static native NativeUnsignedLong __gmpz_get_ui(MPZPointer op);

    public static native NativeLong __gmpz_get_si(MPZPointer op);

    public static native double __gmpz_get_d(MPZPointer op);

    public static native double __gmpz_get_d_2exp(NativeLongByReference exp, MPZPointer op);

    public static native Pointer __gmpz_get_str(ByteBuffer str, int base, MPZPointer op);

    public static native void __gmpz_add(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native void __gmpz_add_ui(MPZPointer rop, MPZPointer op1, NativeUnsignedLong op2);

    public static native void __gmpz_sub(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native void __gmpz_sub_ui(MPZPointer rop, MPZPointer op1, NativeUnsignedLong op2);

    public static native void __gmpz_ui_sub(MPZPointer rop, NativeUnsignedLong op1, MPZPointer op2);

    public static native void __gmpz_mul(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native void __gmpz_mul_si(MPZPointer rop, MPZPointer op1, NativeLong op2);

    public static native void __gmpz_mul_ui(MPZPointer rop, MPZPointer op1, NativeUnsignedLong op2);

    public static native void __gmpz_addmul(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native void __gmpz_addmul_ui(MPZPointer rop, MPZPointer op1, NativeUnsignedLong op2);

    public static native void __gmpz_submul(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native void __gmpz_submul_ui(MPZPointer rop, MPZPointer op1, NativeUnsignedLong op2);

    public static native void __gmpz_mul_2exp(MPZPointer rop, MPZPointer op1, MPBitCntT op2);

    public static native void __gmpz_neg(MPZPointer rop, MPZPointer op);

    public static native void __gmpz_abs(MPZPointer rop, MPZPointer op);

    public static native void __gmpz_cdiv_q(MPZPointer q, MPZPointer n, MPZPointer d);

    public static native void __gmpz_cdiv_r(MPZPointer r, MPZPointer n, MPZPointer d);

    public static native void __gmpz_cdiv_qr(MPZPointer q, MPZPointer r, MPZPointer n, MPZPointer d);

    public static native NativeUnsignedLong __gmpz_cdiv_q_ui(MPZPointer q, MPZPointer n, NativeUnsignedLong d);

    public static native NativeUnsignedLong __gmpz_cdiv_r_ui(MPZPointer r, MPZPointer n, NativeUnsignedLong d);

    public static native NativeUnsignedLong __gmpz_cdiv_qr_ui(MPZPointer q, MPZPointer r, MPZPointer n,
            NativeUnsignedLong d);

    public static native NativeUnsignedLong __gmpz_cdiv_ui(MPZPointer n, NativeUnsignedLong d);

    public static native void __gmpz_cdiv_q_2exp(MPZPointer q, MPZPointer n, MPBitCntT b);

    public static native void __gmpz_cdiv_r_2exp(MPZPointer r, MPZPointer n, MPBitCntT b);

    public static native void __gmpz_fdiv_q(MPZPointer q, MPZPointer n, MPZPointer d);

    public static native void __gmpz_fdiv_r(MPZPointer r, MPZPointer n, MPZPointer d);

    public static native void __gmpz_fdiv_qr(MPZPointer q, MPZPointer r, MPZPointer n, MPZPointer d);

    public static native NativeUnsignedLong __gmpz_fdiv_q_ui(MPZPointer q, MPZPointer n, NativeUnsignedLong d);

    public static native NativeUnsignedLong __gmpz_fdiv_r_ui(MPZPointer r, MPZPointer n, NativeUnsignedLong d);

    public static native NativeUnsignedLong __gmpz_fdiv_qr_ui(MPZPointer q, MPZPointer r, MPZPointer n,
            NativeUnsignedLong d);

    public static native NativeUnsignedLong __gmpz_fdiv_ui(MPZPointer n, NativeUnsignedLong d);

    public static native void __gmpz_fdiv_q_2exp(MPZPointer q, MPZPointer n, NativeUnsignedLong b);

    public static native void __gmpz_fdiv_r_2exp(MPZPointer r, MPZPointer n, NativeUnsignedLong b);

    public static native void __gmpz_tdiv_q(MPZPointer q, MPZPointer n, MPZPointer d);

    public static native void __gmpz_tdiv_r(MPZPointer r, MPZPointer n, MPZPointer d);

    public static native void __gmpz_tdiv_qr(MPZPointer q, MPZPointer r, MPZPointer n, MPZPointer d);

    public static native NativeUnsignedLong __gmpz_tdiv_q_ui(MPZPointer q, MPZPointer n, NativeUnsignedLong d);

    public static native NativeUnsignedLong __gmpz_tdiv_r_ui(MPZPointer r, MPZPointer n, NativeUnsignedLong d);

    public static native NativeUnsignedLong __gmpz_tdiv_qr_ui(MPZPointer q, MPZPointer r, MPZPointer n,
            NativeUnsignedLong d);

    public static native NativeUnsignedLong __gmpz_tdiv_ui(MPZPointer n, NativeUnsignedLong d);

    public static native void __gmpz_tdiv_q_2exp(MPZPointer q, MPZPointer n, NativeUnsignedLong b);

    public static native void __gmpz_tdiv_r_2exp(MPZPointer r, MPZPointer n, NativeUnsignedLong b);

    public static native void __gmpz_mod(MPZPointer r, MPZPointer n, MPZPointer d);

    public static NativeUnsignedLong __gmpz_mod_ui(MPZPointer r, MPZPointer n, NativeUnsignedLong d) {
        return __gmpz_fdiv_r_ui(r, n, d);
    }

    public static native void __gmpz_divexact(MPZPointer r, MPZPointer n, MPZPointer d);

    public static native void __gmpz_divexact_ui(MPZPointer r, MPZPointer n, NativeUnsignedLong d);

    public static native boolean __gmpz_divisible_p(MPZPointer n, MPZPointer d);

    public static native boolean __gmpz_divisible_ui_p(MPZPointer n, NativeUnsignedLong d);

    public static native boolean __gmpz_divisible_2exp_p(MPZPointer n, MPBitCntT b);

    public static native boolean __gmpz_congruent_p(MPZPointer n, MPZPointer c, MPZPointer d);

    public static native boolean __gmpz_congruent_ui_p(MPZPointer n, NativeUnsignedLong c, NativeUnsignedLong d);

    public static native boolean __gmpz_congruent_2exp_p(MPZPointer n, MPZPointer c, MPBitCntT b);

    public static native void __gmpz_powm(MPZPointer rop, MPZPointer base, MPZPointer exp, MPZPointer mod);

    public static native void __gmpz_powm_ui(MPZPointer rop, MPZPointer base, NativeUnsignedLong exp, MPZPointer mod);

    public static native void __gmpz_powm_sec(MPZPointer rop, MPZPointer base, MPZPointer exp, MPZPointer mod);

    public static native void __gmpz_pow_ui(MPZPointer rop, MPZPointer base, NativeUnsignedLong exp);

    public static native void __gmpz_ui_pow_ui(MPZPointer rop, NativeUnsignedLong base, NativeUnsignedLong exp);

    public static native boolean __gmpz_root(MPZPointer rop, MPZPointer op, NativeUnsignedLong n);

    public static native void __gmpz_rootrem(MPZPointer rop, MPZPointer rem, MPZPointer op, NativeUnsignedLong n);

    public static native void __gmpz_sqrt(MPZPointer rop, MPZPointer op);

    public static native void __gmpz_sqrtrem(MPZPointer rop, MPZPointer rem, MPZPointer op);

    public static native boolean __gmpz_perfect_power_p(MPZPointer op);

    public static native boolean __gmpz_perfect_square_p(MPZPointer op);

    public static native int __gmpz_probab_prime_p(MPZPointer op, int reps);

    public static native void __gmpz_nextprime(MPZPointer rop, MPZPointer op);

    public static native void __gmpz_gcd(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native NativeUnsignedLong __gmpz_gcd_ui(MPZPointer rop, MPZPointer op1, NativeUnsignedLong op2);

    public static native void __gmpz_gcdext(MPZPointer g, MPZPointer s, MPZPointer t, MPZPointer a, MPZPointer b);

    public static native void __gmpz_lcm(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native void __gmpz_lcm_ui(MPZPointer rop, MPZPointer op1, NativeUnsignedLong op2);

    public static native boolean __gmpz_invert(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native int __gmpz_jacobi(MPZPointer a, MPZPointer b);

    public static native int __gmpz_legendre(MPZPointer a, MPZPointer b);

    public static native int __gmpz_kronecker_si(MPZPointer a, NativeLong b);

    public static native int __gmpz_kronecker_ui(MPZPointer a, NativeUnsignedLong b);

    public static native int __gmpz_si_kronecker(NativeLong a, MPZPointer b);

    public static native int __gmpz_ui_kronecker(NativeUnsignedLong a, MPZPointer b);

    public static native MPBitCntT __gmpz_remove(MPZPointer rop, MPZPointer op, MPZPointer f);

    public static native void __gmpz_fac_ui(MPZPointer rop, NativeUnsignedLong n);

    public static native void __gmpz_2fac_ui(MPZPointer rop, NativeUnsignedLong n);

    public static native void __gmpz_mfac_uiui(MPZPointer rop, NativeUnsignedLong n, NativeUnsignedLong m);

    public static native void __gmpz_primorial_ui(MPZPointer rop, NativeUnsignedLong n);

    public static native void __gmpz_bin_ui(MPZPointer rop, MPZPointer n, NativeUnsignedLong k);

    public static native void __gmpz_bin_uiui(MPZPointer rop, NativeUnsignedLong n, NativeUnsignedLong k);

    public static native void __gmpz_fib_ui(MPZPointer fn, NativeUnsignedLong n);

    public static native void __gmpz_fib2_ui(MPZPointer fn, MPZPointer fnsub1, NativeUnsignedLong n);

    public static native void __gmpz_lucnum_ui(MPZPointer ln, NativeUnsignedLong n);

    public static native void __gmpz_lucnum2_ui(MPZPointer ln, MPZPointer lnsub1, NativeUnsignedLong n);

    public static native int __gmpz_cmp(MPZPointer op1, MPZPointer op2);

    public static native int __gmpz_cmp_d(MPZPointer op1, double op2);

    public static native int __gmpz_cmp_si(MPZPointer op1, NativeLong op2);

    public static native int __gmpz_cmp_ui(MPZPointer op1, NativeUnsignedLong op2);

    public static native int __gmpz_cmpabs(MPZPointer op1, MPZPointer op2);

    public static native int __gmpz_cmpabs_d(MPZPointer op1, double op2);

    public static native int __gmpz_cmpabs_ui(MPZPointer op1, NativeUnsignedLong op2);

    public static int __gmpz_sgn(MPZPointer op) {
        return __gmpz_cmp(op, __gmpz_zero);
    }

    public static native void __gmpz_and(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native void __gmpz_ior(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native void __gmpz_xor(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native void __gmpz_com(MPZPointer rop, MPZPointer op);

    public static native MPBitCntT __gmpz_popcount(MPZPointer op);

    public static native MPBitCntT __gmpz_hamdist(MPZPointer op1, MPZPointer op2);

    public static native MPBitCntT __gmpz_scan0(MPZPointer op, MPBitCntT starting_bit);

    public static native MPBitCntT __gmpz_scan1(MPZPointer op, MPBitCntT starting_bit);

    public static native MPBitCntT __gmpz_setbit(MPZPointer rop, MPBitCntT index);

    public static native MPBitCntT __gmpz_clrbit(MPZPointer rop, MPBitCntT index);

    public static native MPBitCntT __gmpz_combit(MPZPointer rop, MPBitCntT index);

    public static native int __gmpz_tstbit(MPZPointer rop, MPBitCntT index);

    public static native SizeT __gmpz_out_str(Pointer stream, int base, MPZPointer op);

    public static native SizeT __gmpz_inp_str(MPZPointer rop, Pointer stream, int base);

    public static native SizeT __gmpz_out_raw(Pointer stream, MPZPointer op);

    public static native SizeT __gmpz_inp_raw(MPZPointer rop, Pointer stream);

    public static native void __gmpz_urandomb(MPZPointer rop, RandStatePointer state, MPBitCntT n);

    public static native void __gmpz_urandomm(MPZPointer rop, RandStatePointer state, MPZPointer n);

    public static native void __gmpz_rrandomb(MPZPointer rop, RandStatePointer state, MPBitCntT n);

    public static native void __gmpz_random(MPZPointer rop, MPSizeT max_size);

    public static native void __gmpz_random2(MPZPointer rop, MPSizeT max_size);

    public static native void __gmpz_import(MPZPointer rop, SizeT count, int order, SizeT size, int endian, SizeT nails,
            ByteBuffer op);

    public static native MPZPointer __gmpz_export(ByteBuffer rop, SizeTByReference count, int order, SizeT size,
            int endian, SizeT nails, MPZPointer op);

    public static native boolean __gmpz_fits_ulong_p(MPZPointer op);

    public static native boolean __gmpz_fits_slong_p(MPZPointer op);

    public static native boolean __gmpz_fits_uint_p(MPZPointer op);

    public static native boolean __gmpz_fits_sint_p(MPZPointer op);

    public static native boolean __gmpz_fits_ushort_p(MPZPointer op);

    public static native boolean __gmpz_fits_sshort_p(MPZPointer op);

    public static native SizeT __gmpz_sizeinbase(MPZPointer op, int base);

    // Random Number Functions

    public static native void __gmp_randinit_default(RandStatePointer state);

    public static native void __gmp_randinit_mt(RandStatePointer state);

    public static native void __gmp_randinit_lc_2exp(RandStatePointer state, MPZPointer a, NativeLong c,
            NativeLong m2exp);

    public static native int __gmp_randinit_lc_2exp_size(RandStatePointer state, NativeLong m2exp);

    public static native void __gmp_randinit_set(RandStatePointer rop, RandStatePointer op);

    public static native void __gmp_randinit(RandStatePointer state, int alg, NativeLong l);

    public static native void __gmp_randclear(RandStatePointer state);

    public static native void __gmp_randseed (RandStatePointer state, MPZPointer seed);

    public static native void __gmp_randseed_ui (RandStatePointer state, NativeUnsignedLong seed);

    public static native NativeLong __gmp_urandomb_ui (RandStatePointer state, NativeUnsignedLong n);

    public static native NativeLong __gmp_urandomm_ui (RandStatePointer state, NativeUnsignedLong n);

    // Formatted Output

    public static int __gmp_printf(String fmt, Object... args) {
        return gmpextra.__gmp_printf(fmt, args);
    }
}