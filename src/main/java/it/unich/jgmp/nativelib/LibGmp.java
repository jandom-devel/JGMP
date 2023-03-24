/**
* Copyright 2022, 2023 Gianluca Amato <gianluca.amato@unich.it>
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
package it.unich.jgmp.nativelib;

import java.nio.ByteBuffer;
import java.util.Map;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * This class contains the static native methods corresponding to GMP functions.
 * This class also contains constants for the size of native GMP structures and
 * some global variables.
 *
 * <p>
 * Direct mapping is used for almost all the functions, with the exception of a
 * few ones with a variable number of arguments which require interface mapping.
 * <p>
 * Some documented GMP functions are actually macros: they have been
 * reimplemented here.
 * <a href="https://gmplib.org/manual/Low_002dlevel-Functions" target=
 * "_blank">Low-level Function</a>, as defined in the GMP documentation, as well
 * as those functions which depend on types provided by the C standard library
 * (such as the {@code FILE} type), have been omitted entirely.
 * </p>
 * <p>
 * We strived to be type safe, by defining different subclasses of
 * {@code com.sun.jna.PointerType} and {@code com.sun.jna.IntegerType} for
 * different native types. Newly defined types have the same name of GMP native
 * types but converted to camel case. For example, {@code mpf_t} becomes
 * {@code MpfT}, while {@code mp_exp_t} is {@code MpExpT}. The only exception is
 * {@code NativeUnsignedLong} which should be used for native unsigned longs.
 * </p>
 */
public class LibGmp {

    /**
     * A private constructor, since this class should never be instantiated.
     */
    private LibGmp() {
    }

    /**
     * The undecorated name of the GMP library.
     */
    private static final String LIBNAME = "gmp";

    /**
     * The number of bits per limb.
     */
    public static final int mp_bits_per_limb;

    /**
     * The major GMP version. It is the "i" component in {@link gmp_version}.
     */
    public static final int __GNU_MP_VERSION;

    /**
     * The minor GMP version. It is the "j" component in {@link gmp_version}.
     */
    public static final int __GNU_MP_VERSION_MINOR;

    /**
     * The patch level GMP version. It is the "k" component in {@link gmp_version}.
     */
    public static final int __GNU_MP_VERSION_PATCHLEVEL;

    /**
     * The native GMP version number, in the form “i.j.k”.
     */
    public static final String gmp_version;

    /**
     * Pointer to the {@code gmp_errno} variable.
     */
    private static final Pointer gmp_errno_pointer;

    /**
     * Return the value of the global error variable, used by obsolete random number
     * functions. Every bit of this variable has a different meaning, encoded by the
     * {@code GMP_ERROR_xx}
     */
    public static int gmp_errno() {
        return gmp_errno_pointer.getInt(0);
    }

    public static final int GMP_ERROR_NONE = 0;
    public static final int GMP_ERROR_UNSUPPORTED_ARGUMENT = 1;
    public static final int GMP_ERROR_DIVISION_BY_ZERO = 2;
    public static final int GMP_ERROR_SQRT_OF_NEGATIVE = 4;
    public static final int GMP_ERROR_INVALID_ARGUMENT = 8;

    public static final int GMP_RAND_ALG_DEFAULT = 0;
    public static final int GMP_RAND_ALG_LC = 0;

    /**
     * The integer 0 (assuming no one changes it)
     */
    private static MpzT mpz_zero;

    /**
     * The rational 0 (assuming no one changes it)
     */
    private static MpqT mpq_zero;

    /**
     * The floating point 0 (assuming no one changes it)
     */
    private static MpfT mpf_zero;

    /**
     * The native deallocator used by the GMP library.
     */
    private static FreeFunc gmp_deallocator;

    /**
     * The default native deallocator used by the GMP library.
     */
    private static FreeFunc gmp_default_deallocator;

    /**
     * Call the native deallocator used by the GMP library. In general, it is not
     * possible to deallocate memory allocated by GMP (such as from the
     * {@code mpz_get_str} function) using {@code Native.free}, since in some
     * environments (e.g., Windows) GMP runs using a Unix compatibility layer which
     * uses a non-standard allocation methods.
     */
    public static void deallocate(Pointer p, SizeT size) {
        gmp_deallocator.invoke(p, size);
    }

    static {
        var nativeOptions = Map.of(Library.OPTION_FUNCTION_MAPPER, GmpFunctionMapper.getInstance());
        var library = NativeLibrary.getInstance(LIBNAME, nativeOptions);
        Native.register(library);

        var nonNativeOptions = Map.of(Library.OPTION_TYPE_MAPPER, GmpTypeMapper.getInstance(),
                Library.OPTION_FUNCTION_MAPPER, GmpFunctionMapper.getInstance());
        gmpextra = (LibGmpExtra) Native.load(LibGmpExtra.class, nonNativeOptions);

        gmp_version = library.getGlobalVariableAddress("__gmp_version").getPointer(0).getString(0);
        mp_bits_per_limb = library.getGlobalVariableAddress("__gmp_bits_per_limb").getInt(0);
        gmp_errno_pointer = library.getGlobalVariableAddress("__gmp_errno");
        var dotPosition = gmp_version.indexOf(".");
        __GNU_MP_VERSION = Integer.parseInt(gmp_version.substring(0, dotPosition));
        var secondDotPosition = gmp_version.indexOf(".", dotPosition + 1);
        __GNU_MP_VERSION_MINOR = secondDotPosition > 0
                ? Integer.parseInt(gmp_version.substring(dotPosition + 1, secondDotPosition))
                : Integer.parseInt(gmp_version.substring(dotPosition + 1));
        __GNU_MP_VERSION_PATCHLEVEL = secondDotPosition > 0
                ? Integer.parseInt(gmp_version.substring(secondDotPosition + 1))
                : 0;

        var free = new FreeFuncByReference();
        mp_get_memory_functions(null, null, free);
        gmp_deallocator = free.value;
        gmp_default_deallocator = gmp_deallocator;

        mpz_zero = new MpzT();
        mpz_init(mpz_zero);
        mpq_zero = new MpqT();
        mpq_init(mpq_zero);
        mpf_zero = new MpfT();
        mpf_init(mpf_zero);
    }

    /**
     * Return the system decimal separator. Just called one to initialize the
     * {@code decimalSeparator} field.
     */
    private static String getDecimalSeparator() {
        var pp = new PointerByReference();
        var len = gmp_asprintf(pp, "%.1Ff", mpf_zero);
        var p = pp.getValue();
        var s = p.getString(0);
        deallocate(p, new SizeT(len + 1));
        return s.substring(1, s.length() - 1);
    }

    /**
     * The system decimal separator. We compute this value when the {@link LibGmp}
     * class is loaded, and we assume it is not changed later.
     */
    public static final String decimalSeparator = getDecimalSeparator();

    /**
     * Interface for the native functions with a variable number of arguments. These
     * are not supported by direct mapping, so we need to register them separately.
     */
    private static interface LibGmpExtra extends Library {
        int gmp_printf(String fmt, Object... args);

        int gmp_fprintf(Pointer fp, String fmt, Object... args);

        int gmp_sprintf(ByteBuffer buf, String fmt, Object... args);

        int gmp_snprintf(ByteBuffer buf, SizeT size, String fmt, Object... args);

        int gmp_asprintf(PointerByReference pp, String fmt, Object... args);

        int gmp_scanf(String fmt, Object... args);

        int gmp_fscanf(Pointer fp, String fmt, Object... args);

        int gmp_sscanf(String s, String fmt, Object... args);

        void mpz_inits(MpzT... xs);

        void mpz_clears(MpzT... xs);

        void mpq_inits(MpqT... xs);

        void mpq_clears(MpqT... xs);

        void mpf_inits(MpfT... xs);

        void mpf_clears(MpfT... xs);
    }

    /**
     * Instance of the {@code LibGmpExtra} interface created at initialization time.
     */
    private static final LibGmpExtra gmpextra;

    // Integer functions

    public static native void mpz_init(MpzT x);

    public static void mpz_inits(MpzT... xs) {
        gmpextra.mpz_inits(xs);
    }

    public static native void mpz_init2(MpzT x, MpBitcntT n);

    public static native void mpz_clear(MpzT x);

    // Version of mpz_clear which may be called with a Pointer... used by the MPZCleaner class.
    public static native void __gmpz_clear(Pointer x);

    public static void mpz_clears(MpzT... xs) {
        gmpextra.mpz_clears(xs);
    }

    public static native void mpz_realloc2(MpzT x, MpBitcntT n);

    public static native void mpz_set(MpzT rop, MpzT op);

    public static native void mpz_set_ui(MpzT rop, NativeUnsignedLong op);

    public static native void mpz_set_si(MpzT rop, NativeLong op);

    public static native void mpz_set_d(MpzT rop, double op);

    public static native void mpz_set_q(MpzT rop, MpqT op);

    public static native void mpz_set_f(MpzT rop, MpfT op);

    public static native int mpz_set_str(MpzT rop, String str, int base);

    public static native void mpz_swap(MpzT rop1, MpzT rop2);

    public static native void mpz_init_set(MpzT rop, MpzT op);

    public static native void mpz_init_set_ui(MpzT rop, NativeUnsignedLong op);

    public static native void mpz_init_set_si(MpzT rop, NativeLong op);

    public static native void mpz_init_set_d(MpzT rop, double op);

    public static native int mpz_init_set_str(MpzT rop, String str, int base);

    public static native NativeUnsignedLong mpz_get_ui(MpzT op);

    public static native NativeLong mpz_get_si(MpzT op);

    public static native double mpz_get_d(MpzT op);

    public static native double mpz_get_d_2exp(NativeLongByReference exp, MpzT op);

    public static native Pointer mpz_get_str(ByteBuffer str, int base, MpzT op);

    public static native void mpz_add(MpzT rop, MpzT op1, MpzT op2);

    public static native void mpz_add_ui(MpzT rop, MpzT op1, NativeUnsignedLong op2);

    public static native void mpz_sub(MpzT rop, MpzT op1, MpzT op2);

    public static native void mpz_sub_ui(MpzT rop, MpzT op1, NativeUnsignedLong op2);

    public static native void mpz_ui_sub(MpzT rop, NativeUnsignedLong op1, MpzT op2);

    public static native void mpz_mul(MpzT rop, MpzT op1, MpzT op2);

    public static native void mpz_mul_si(MpzT rop, MpzT op1, NativeLong op2);

    public static native void mpz_mul_ui(MpzT rop, MpzT op1, NativeUnsignedLong op2);

    public static native void mpz_addmul(MpzT rop, MpzT op1, MpzT op2);

    public static native void mpz_addmul_ui(MpzT rop, MpzT op1, NativeUnsignedLong op2);

    public static native void mpz_submul(MpzT rop, MpzT op1, MpzT op2);

    public static native void mpz_submul_ui(MpzT rop, MpzT op1, NativeUnsignedLong op2);

    public static native void mpz_mul_2exp(MpzT rop, MpzT op1, MpBitcntT op2);

    public static native void mpz_neg(MpzT rop, MpzT op);

    public static native void mpz_abs(MpzT rop, MpzT op);

    public static native void mpz_cdiv_q(MpzT q, MpzT n, MpzT d);

    public static native void mpz_cdiv_r(MpzT r, MpzT n, MpzT d);

    public static native void mpz_cdiv_qr(MpzT q, MpzT r, MpzT n, MpzT d);

    public static native NativeUnsignedLong mpz_cdiv_q_ui(MpzT q, MpzT n, NativeUnsignedLong d);

    public static native NativeUnsignedLong mpz_cdiv_r_ui(MpzT r, MpzT n, NativeUnsignedLong d);

    public static native NativeUnsignedLong mpz_cdiv_qr_ui(MpzT q, MpzT r, MpzT n, NativeUnsignedLong d);

    public static native NativeUnsignedLong mpz_cdiv_ui(MpzT n, NativeUnsignedLong d);

    public static native void mpz_cdiv_q_2exp(MpzT q, MpzT n, MpBitcntT b);

    public static native void mpz_cdiv_r_2exp(MpzT r, MpzT n, MpBitcntT b);

    public static native void mpz_fdiv_q(MpzT q, MpzT n, MpzT d);

    public static native void mpz_fdiv_r(MpzT r, MpzT n, MpzT d);

    public static native void mpz_fdiv_qr(MpzT q, MpzT r, MpzT n, MpzT d);

    public static native NativeUnsignedLong mpz_fdiv_q_ui(MpzT q, MpzT n, NativeUnsignedLong d);

    public static native NativeUnsignedLong mpz_fdiv_r_ui(MpzT r, MpzT n, NativeUnsignedLong d);

    public static native NativeUnsignedLong mpz_fdiv_qr_ui(MpzT q, MpzT r, MpzT n, NativeUnsignedLong d);

    public static native NativeUnsignedLong mpz_fdiv_ui(MpzT n, NativeUnsignedLong d);

    public static native void mpz_fdiv_q_2exp(MpzT q, MpzT n, NativeUnsignedLong b);

    public static native void mpz_fdiv_r_2exp(MpzT r, MpzT n, NativeUnsignedLong b);

    public static native void mpz_tdiv_q(MpzT q, MpzT n, MpzT d);

    public static native void mpz_tdiv_r(MpzT r, MpzT n, MpzT d);

    public static native void mpz_tdiv_qr(MpzT q, MpzT r, MpzT n, MpzT d);

    public static native NativeUnsignedLong mpz_tdiv_q_ui(MpzT q, MpzT n, NativeUnsignedLong d);

    public static native NativeUnsignedLong mpz_tdiv_r_ui(MpzT r, MpzT n, NativeUnsignedLong d);

    public static native NativeUnsignedLong mpz_tdiv_qr_ui(MpzT q, MpzT r, MpzT n, NativeUnsignedLong d);

    public static native NativeUnsignedLong mpz_tdiv_ui(MpzT n, NativeUnsignedLong d);

    public static native void mpz_tdiv_q_2exp(MpzT q, MpzT n, NativeUnsignedLong b);

    public static native void mpz_tdiv_r_2exp(MpzT r, MpzT n, NativeUnsignedLong b);

    public static native void mpz_mod(MpzT r, MpzT n, MpzT d);

    public static NativeUnsignedLong mpz_mod_ui(MpzT r, MpzT n, NativeUnsignedLong d) {
        return mpz_fdiv_r_ui(r, n, d);
    }

    public static native void mpz_divexact(MpzT r, MpzT n, MpzT d);

    public static native void mpz_divexact_ui(MpzT r, MpzT n, NativeUnsignedLong d);

    public static native boolean mpz_divisible_p(MpzT n, MpzT d);

    public static native boolean mpz_divisible_ui_p(MpzT n, NativeUnsignedLong d);

    public static native boolean mpz_divisible_2exp_p(MpzT n, MpBitcntT b);

    public static native boolean mpz_congruent_p(MpzT n, MpzT c, MpzT d);

    public static native boolean mpz_congruent_ui_p(MpzT n, NativeUnsignedLong c, NativeUnsignedLong d);

    public static native boolean mpz_congruent_2exp_p(MpzT n, MpzT c, MpBitcntT b);

    public static native void mpz_powm(MpzT rop, MpzT base, MpzT exp, MpzT mod);

    public static native void mpz_powm_ui(MpzT rop, MpzT base, NativeUnsignedLong exp, MpzT mod);

    public static native void mpz_powm_sec(MpzT rop, MpzT base, MpzT exp, MpzT mod);

    public static native void mpz_pow_ui(MpzT rop, MpzT base, NativeUnsignedLong exp);

    public static native void mpz_ui_pow_ui(MpzT rop, NativeUnsignedLong base, NativeUnsignedLong exp);

    public static native boolean mpz_root(MpzT rop, MpzT op, NativeUnsignedLong n);

    public static native void mpz_rootrem(MpzT rop, MpzT rem, MpzT op, NativeUnsignedLong n);

    public static native void mpz_sqrt(MpzT rop, MpzT op);

    public static native void mpz_sqrtrem(MpzT rop, MpzT rem, MpzT op);

    public static native boolean mpz_perfect_power_p(MpzT op);

    public static native boolean mpz_perfect_square_p(MpzT op);

    public static native int mpz_probab_prime_p(MpzT op, int reps);

    public static native void mpz_nextprime(MpzT rop, MpzT op);

    public static native void mpz_gcd(MpzT rop, MpzT op1, MpzT op2);

    public static native NativeUnsignedLong mpz_gcd_ui(MpzT rop, MpzT op1, NativeUnsignedLong op2);

    public static native void mpz_gcdext(MpzT g, MpzT s, MpzT t, MpzT a, MpzT b);

    public static native void mpz_lcm(MpzT rop, MpzT op1, MpzT op2);

    public static native void mpz_lcm_ui(MpzT rop, MpzT op1, NativeUnsignedLong op2);

    public static native boolean mpz_invert(MpzT rop, MpzT op1, MpzT op2);

    public static native int mpz_jacobi(MpzT a, MpzT b);

    public static native int mpz_legendre(MpzT a, MpzT b);

    public static native int mpz_kronecker_si(MpzT a, NativeLong b);

    public static native int mpz_kronecker_ui(MpzT a, NativeUnsignedLong b);

    public static native int mpz_si_kronecker(NativeLong a, MpzT b);

    public static native int mpz_ui_kronecker(NativeUnsignedLong a, MpzT b);

    public static native MpBitcntT mpz_remove(MpzT rop, MpzT op, MpzT f);

    public static native void mpz_fac_ui(MpzT rop, NativeUnsignedLong n);

    // This has been introduced in GMP 5.1.0
    public static native void mpz_2fac_ui(MpzT rop, NativeUnsignedLong n);

    // This has been introduced in GMP 5.1.0
    public static native void mpz_mfac_uiui(MpzT rop, NativeUnsignedLong n, NativeUnsignedLong m);

    // This has been introduced in GMP 5.1.0
    public static native void mpz_primorial_ui(MpzT rop, NativeUnsignedLong n);

    public static native void mpz_bin_ui(MpzT rop, MpzT n, NativeUnsignedLong k);

    public static native void mpz_bin_uiui(MpzT rop, NativeUnsignedLong n, NativeUnsignedLong k);

    public static native void mpz_fib_ui(MpzT fn, NativeUnsignedLong n);

    public static native void mpz_fib2_ui(MpzT fn, MpzT fnsub1, NativeUnsignedLong n);

    public static native void mpz_lucnum_ui(MpzT ln, NativeUnsignedLong n);

    public static native void mpz_lucnum2_ui(MpzT ln, MpzT lnsub1, NativeUnsignedLong n);

    public static native int mpz_cmp(MpzT op1, MpzT op2);

    public static native int mpz_cmp_d(MpzT op1, double op2);

    public static native int mpz_cmp_si(MpzT op1, NativeLong op2);

    public static native int mpz_cmp_ui(MpzT op1, NativeUnsignedLong op2);

    public static native int mpz_cmpabs(MpzT op1, MpzT op2);

    public static native int mpz_cmpabs_d(MpzT op1, double op2);

    public static native int mpz_cmpabs_ui(MpzT op1, NativeUnsignedLong op2);

    public static int mpz_sgn(MpzT op) {
        return mpz_cmp(op, mpz_zero);
    }

    public static native void mpz_and(MpzT rop, MpzT op1, MpzT op2);

    public static native void mpz_ior(MpzT rop, MpzT op1, MpzT op2);

    public static native void mpz_xor(MpzT rop, MpzT op1, MpzT op2);

    public static native void mpz_com(MpzT rop, MpzT op);

    public static native MpBitcntT mpz_popcount(MpzT op);

    public static native MpBitcntT mpz_hamdist(MpzT op1, MpzT op2);

    public static native MpBitcntT mpz_scan0(MpzT op, MpBitcntT starting_bit);

    public static native MpBitcntT mpz_scan1(MpzT op, MpBitcntT starting_bit);

    public static native MpBitcntT mpz_setbit(MpzT rop, MpBitcntT index);

    public static native MpBitcntT mpz_clrbit(MpzT rop, MpBitcntT index);

    public static native MpBitcntT mpz_combit(MpzT rop, MpBitcntT index);

    public static native int mpz_tstbit(MpzT rop, MpBitcntT index);

    public static native SizeT mpz_out_str(Pointer stream, int base, MpzT op);

    public static native SizeT mpz_inp_str(MpzT rop, Pointer stream, int base);

    public static native SizeT mpz_out_raw(Pointer stream, MpzT op);

    public static native SizeT mpz_inp_raw(MpzT rop, Pointer stream);

    public static native void mpz_urandomb(MpzT rop, GmpRandstateT state, MpBitcntT n);

    public static native void mpz_urandomm(MpzT rop, GmpRandstateT state, MpzT n);

    public static native void mpz_rrandomb(MpzT rop, GmpRandstateT state, MpBitcntT n);

    public static native void mpz_random(MpzT rop, MpSizeT max_size);

    public static native void mpz_random2(MpzT rop, MpSizeT max_size);

    public static native void mpz_import(MpzT rop, SizeT count, int order, SizeT size, int endian, SizeT nails,
            ByteBuffer op);

    public static native Pointer mpz_export(ByteBuffer rop, SizeTByReference count, int order, SizeT size, int endian,
            SizeT nails, MpzT op);

    public static native boolean mpz_fits_ulong_p(MpzT op);

    public static native boolean mpz_fits_slong_p(MpzT op);

    public static native boolean mpz_fits_uint_p(MpzT op);

    public static native boolean mpz_fits_sint_p(MpzT op);

    public static native boolean mpz_fits_ushort_p(MpzT op);

    public static native boolean mpz_fits_sshort_p(MpzT op);

    public static boolean mpz_odd_p(MpzT op) {
        return !mpz_divisible_2exp_p(op, new MpBitcntT(1));
    }

    public static boolean mpz_even_p(MpzT op) {
        return mpz_divisible_2exp_p(op, new MpBitcntT(1));
    }

    public static native SizeT mpz_sizeinbase(MpzT op, int base);

    // Rational Number Functions

    public static native void mpq_canonicalize(MpqT x);

    public static native void mpq_init(MpqT x);

    public static void mpq_inits(MpqT... xs) {
        gmpextra.mpq_inits(xs);
    }

    public static native void mpq_clear(MpqT x);

    // Version of mpq_clear which may be called with a Pointer... used by the MPQCleaner class.
    public static native void __gmpq_clear(Pointer x);

    public static void mpq_clears(MpqT... xs) {
        gmpextra.mpq_clears(xs);
    }

    public static native void mpq_set(MpqT rop, MpqT op);

    public static native void mpq_set_z(MpqT rop, MpzT op);

    public static native void mpq_set_ui(MpqT rop, NativeUnsignedLong op1, NativeUnsignedLong op2);

    public static native void mpq_set_si(MpqT rop, NativeLong op1, NativeLong op2);

    public static native int mpq_set_str(MpqT rop, String str, int base);

    public static native void mpq_swap(MpqT rop1, MpqT rop2);

    public static native double mpq_get_d(MpqT op);

    public static native void mpq_set_d(MpqT rop, double op);

    public static native void mpq_set_f(MpqT rop, MpfT op);

    public static native Pointer mpq_get_str(ByteBuffer str, int base, MpqT op);

    public static native void mpq_add(MpqT rop, MpqT addend1, MpqT addend2);

    public static native void mpq_sub(MpqT rop, MpqT minuend, MpqT subtrahend);

    public static native void mpq_mul(MpqT rop, MpqT multiplier, MpqT multiplicand);

    public static native void mpq_mul_2exp(MpqT rop, MpqT op1, MpBitcntT op2);

    public static native void mpq_div(MpqT rop, MpqT dividend, MpqT divisor);

    public static native void mpq_div_2exp(MpqT rop, MpqT op1, MpBitcntT op2);

    public static native void mpq_neg(MpqT rop, MpqT operand);

    public static native void mpq_abs(MpqT rop, MpqT operand);

    public static native void mpq_inv(MpqT rop, MpqT number);

    public static native int mpq_cmp(MpqT op1, MpqT op2);

    public static native int mpq_cmp_z(MpqT op1, MpzT op2);

    public static native int mpq_cmp_ui(MpqT op1, NativeUnsignedLong num2, NativeUnsignedLong den2);

    public static native int mpq_cmp_si(MpqT op1, NativeLong op2, NativeUnsignedLong den2);

    public static int mpq_sgn(MpqT op) {
        return mpq_cmp(op, mpq_zero);
    }

    public static native boolean mpq_equal(MpqT op1, MpqT op2);

    public static MpzT mpq_numref(MpqT op) {
        return new MpzT(op.getPointer().share(0, MpzT.MPZ_SIZE));
    }

    public static MpzT mpq_denref(MpqT op) {
        return new MpzT(op.getPointer().share(MpzT.MPZ_SIZE, MpzT.MPZ_SIZE));
    }

    public static native void mpq_get_num(MpzT numerator, MpqT rational);

    public static native void mpq_get_den(MpzT denominator, MpqT rational);

    public static native void mpq_set_num(MpqT rational, MpzT numerator);

    public static native void mpq_set_den(MpqT rational, MpzT denominator);

    public static native SizeT mpq_out_str(Pointer stream, int base, MpqT op);

    public static native SizeT mpq_inp_str(MpqT rop, Pointer stream, int base);

    // Floating-point functions

    public static native void mpf_set_default_prec(MpBitcntT prec);

    public static native MpBitcntT mpf_get_default_prec();

    public static native void mpf_init(MpfT x);

    public static native void mpf_init2(MpfT x, MpBitcntT n);

    public static void mpf_inits(MpfT... xs) {
        gmpextra.mpf_inits(xs);
    }

    public static native void mpf_clear(MpfT x);

    // Version of mpf_clear which may be called with a Pointer... used by the MPFCleaner class.
    public static native void __gmpf_clear(Pointer x);

    public static void mpf_clears(MpfT... xs) {
        gmpextra.mpf_clears(xs);
    }

    public static native MpBitcntT mpf_get_prec(MpfT op);

    public static native void mpf_set_prec(MpfT rop, MpBitcntT prec);

    public static native void mpf_set_prec_raw(MpfT rop, MpBitcntT prec);

    public static native void mpf_set(MpfT rop, MpfT op);

    public static native void mpf_set_ui(MpfT rop, NativeUnsignedLong op);

    public static native void mpf_set_si(MpfT rop, NativeLong op);

    public static native void mpf_set_d(MpfT rop, double op);

    public static native void mpf_set_z(MpfT rop, MpzT op);

    public static native void mpf_set_q(MpfT rop, MpqT op);

    public static native int mpf_set_str(MpfT rop, String str, int base);

    public static native void mpf_swap(MpfT rop1, MpfT rop2);

    public static native void mpf_init_set(MpfT rop, MpfT op);

    public static native void mpf_init_set_ui(MpfT rop, NativeUnsignedLong op);

    public static native void mpf_init_set_si(MpfT rop, NativeLong op);

    public static native void mpf_init_set_d(MpfT rop, double op);

    public static native int mpf_init_set_str(MpfT rop, String str, int base);

    public static native double mpf_get_d(MpfT op);

    public static native double mpf_get_d_2exp(NativeLongByReference exp, MpfT op);

    public static native NativeLong mpf_get_si(MpfT op);

    public static native NativeUnsignedLong mpf_get_ui(MpfT op);

    public static native Pointer mpf_get_str(ByteBuffer str, MpExpTByReference exp, int base, MpSizeT nDigits, MpfT op);

    public static native void mpf_add(MpfT rop, MpfT op1, MpfT op2);

    public static native void mpf_add_ui(MpfT rop, MpfT op1, NativeUnsignedLong op2);

    public static native void mpf_sub(MpfT rop, MpfT op1, MpfT op2);

    public static native void mpf_sub_ui(MpfT rop, MpfT op1, NativeUnsignedLong op2);

    public static native void mpf_ui_sub(MpfT rop, NativeUnsignedLong op1, MpfT op2);

    public static native void mpf_mul(MpfT rop, MpfT op1, MpfT op2);

    public static native void mpf_mul_ui(MpfT rop, MpfT op1, NativeUnsignedLong op2);

    public static native void mpf_div(MpfT rop, MpfT op1, MpfT op2);

    public static native void mpf_div_ui(MpfT rop, MpfT op1, NativeUnsignedLong op2);

    public static native void mpf_ui_div(MpfT rop, NativeUnsignedLong op1, MpfT op2);

    public static native void mpf_sqrt(MpfT rop, MpfT op);

    public static native void mpf_sqrt_ui(MpfT rop, NativeUnsignedLong op);

    public static native void mpf_pow_ui(MpfT rop, MpfT op1, NativeUnsignedLong op2);

    public static native void mpf_neg(MpfT rop, MpfT op);

    public static native void mpf_abs(MpfT rop, MpfT op);

    public static native void mpf_mul_2exp(MpfT rop, MpfT op1, MpBitcntT op2);

    public static native void mpf_div_2exp(MpfT rop, MpfT op1, MpBitcntT op2);

    public static native int mpf_cmp(MpfT op1, MpfT op2);

    public static native int mpf_cmp_z(MpfT op1, MpzT op2);

    public static native int mpf_cmp_d(MpfT op1, double op2);

    public static native int mpf_cmp_ui(MpfT op1, NativeUnsignedLong op2);

    public static native int mpf_cmp_si(MpfT op1, NativeLong op2);

    public static native boolean mpf_eq(MpfT op1, MpfT op2, MpBitcntT op3);

    public static native int mpf_reldiff(MpfT rop, MpfT op1, MpfT op2);

    public static int mpf_sgn(MpfT op) {
        return mpf_cmp(op, mpf_zero);
    }

    public static native SizeT mpf_out_str(Pointer stream, int base, SizeT nDigits, MpfT op);

    public static native SizeT mpf_inp_str(MpqT rop, Pointer stream, int base);

    public static native void mpf_ceil(MpfT rop, MpfT op);

    public static native void mpf_floor(MpfT rop, MpfT op);

    public static native void mpf_trunc(MpfT rop, MpfT op);

    public static native boolean mpf_integer_p(MpfT op);

    public static native boolean mpf_fits_ulong_p(MpfT op);

    public static native boolean mpf_fits_slong_p(MpfT op);

    public static native boolean mpf_fits_uint_p(MpfT op);

    public static native boolean mpf_fits_sint_p(MpfT op);

    public static native boolean mpf_fits_ushort_p(MpfT op);

    public static native boolean mpf_fits_sshort_p(MpfT op);

    public static native void mpf_urandomb(MpfT rop, GmpRandstateT state, MpBitcntT n);

    public static native void mpf_random2(MpfT rop, MpSizeT max_size, MpExpT exp);

    // Random Number Functions

    public static native void gmp_randinit_default(GmpRandstateT state);

    public static native void gmp_randinit_mt(GmpRandstateT state);

    public static native void gmp_randinit_lc_2exp(GmpRandstateT state, MpzT a, NativeUnsignedLong c, MpBitcntT m2exp);

    public static native int gmp_randinit_lc_2exp_size(GmpRandstateT state, MpBitcntT m2exp);

    public static native void gmp_randinit_set(GmpRandstateT rop, GmpRandstateT op);

    public static native void gmp_randinit(GmpRandstateT state, int alg, NativeLong l);

    public static native void gmp_randclear(GmpRandstateT state);

    // Version of gmp_randclear which may be called with a Pointer... used by the RandomStateCleaner class.
    public static native void __gmp_randclear(Pointer x);

    public static native void gmp_randseed(GmpRandstateT state, MpzT seed);

    public static native void gmp_randseed_ui(GmpRandstateT state, NativeUnsignedLong seed);

    public static native NativeLong gmp_urandomb_ui(GmpRandstateT state, NativeUnsignedLong n);

    public static native NativeLong gmp_urandomm_ui(GmpRandstateT state, NativeUnsignedLong n);

    // Formatted Output

    public static int gmp_printf(String fmt, Object... args) {
        return gmpextra.gmp_printf(fmt, args);
    }

    public static native int gmp_vprintf(String fmt, Pointer ap);

    public static int gmp_fprintf(Pointer fp, String fmt, Object... args) {
        return gmpextra.gmp_fprintf(fp, fmt, args);
    }

    public static native int gmp_vfprintf(Pointer fp, String fmt, Pointer ap);

    public static int gmp_sprintf(ByteBuffer buf, String fmt, Object... args) {
        return gmpextra.gmp_sprintf(buf, fmt, args);
    }

    public static native int gmp_vsprintf(ByteBuffer buf, String fmt, Pointer ap);

    public static int gmp_snprintf(ByteBuffer buf, SizeT size, String fmt, Object... args) {
        return gmpextra.gmp_snprintf(buf, size, fmt, args);
    }

    public static native int gmp_vsnprintf(ByteBuffer buf, SizeT size, String fmt, Pointer ap);

    public static int gmp_asprintf(PointerByReference pp, String fmt, Object... args) {
        return gmpextra.gmp_asprintf(pp, fmt, args);
    }

    public static native int gmp_vasprintf(PointerByReference pp, String fmt, Pointer ap);

    // Formatted Input

    public static int gmp_scanf(String fmt, Object... args) {
        return gmpextra.gmp_scanf(fmt, args);
    }

    public static native int gmp_vscanf(String fmt, Pointer ap);

    public static int gmp_fscanf(Pointer fp, String fmt, Object... args) {
        return gmpextra.gmp_fscanf(fp, fmt, args);
    }

    public static native int gmp_vfscanf(Pointer fp, String fmt, Pointer ap);

    public static int gmp_sscanf(String s, String fmt, Object... args) {
        return gmpextra.gmp_sscanf(s, fmt, args);
    }

    public static native int gmp_vsscanf(String s, String fmt, Pointer ap);

    // Custom Allocation

    private static native void __gmp_set_memory_functions(AllocFunc alloc_func_ptr, ReallocFunc realloc_func_ptr,
            FreeFunc free_func_ptr);

    public static native void mp_get_memory_functions(AllocFuncByReference alloc_func_ptr,
            ReallocFuncByReference realloc_func_ptr, FreeFuncByReference free_func_ptr);

    /*
     * Wrap the GMP {@code mp_set_memory_functions} in order to keep in {@code gmp_deallocator} up to date.
     */
    public static void mp_set_memory_functions(AllocFunc alloc_func_ptr, ReallocFunc realloc_func_ptr,
            FreeFunc free_func_ptr) {
        gmp_deallocator = free_func_ptr == null ? gmp_default_deallocator : free_func_ptr;
        __gmp_set_memory_functions(alloc_func_ptr, realloc_func_ptr, free_func_ptr);
    }
}