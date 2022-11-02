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
 * "_blank">Low-level Function</a>, as defined in the GMP
 * documentation, as well as those functions which depend on types provided by
 * the C standard
 * library (such as the {@code FILE} type), have been omitted entirely.
 * </p>
 * <p>
 * We strived to be type safe, by defining different subclasses of
 * {@code com.sun.jna.PointerType} and {@code com.sun.jna.IntegerType} for
 * different native types. Here is the conversion table between native and
 * Java types. All types not shown below follows standard JNA conventions.
 * </p>
 *
 * <table border="1" style="text-align:center; border-collapse: collapse;" >
 * <caption style="display: none;">Conversion table from native to Java
 * types</caption>
 * <thead>
 * <tr>
 * <th scope="col">native type</th>
 * <th scope="col">Java type</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>{@code mp_bitcnt_t}</td>
 * <td>{@code MPBitCntT}</td>
 * </tr>
 * <tr>
 * <td>{@code mp_expt_t}</td>
 * <td>{@code MPExpT}</td>
 * </tr>
 * <tr>
 * <td>{@code mp_expt_t*}</td>
 * <td>{@code MPExpTByReference}</td>
 * </tr>
 * <tr>
 * <td>{@code mpf_t}</td>
 * <td>{@code MPFPointer}</td>
 * </tr>
 * <tr>
 * <td>{@code mpq_t}</td>
 * <td>{@code MPQPointer}</td>
 * </tr>
 * <tr>
 * <td>{@code mp_size_t}</td>
 * <td>{@code MPSizeT}</td>
 * </tr>
 * <tr>
 * <td>{@code mpz_t}</td>
 * <td>{@code MPZPointer}</td>
 * </tr>
 * <tr>
 * <td>{@code unsigned long}</td>
 * <td>{@code NativeUnsignedLong}</td>
 * </tr>
 * <tr>
 * <td>{@code gmp_randstate_struct}</td>
 * <td>{@code RandStatePointer}</td>
 * </tr>
 * <tr>
 * <td>{@code size_t}</td>
 * <td>{@code SizeT}</td>
 * </tr>
 * <tr>
 * <td>{@code size_t*}</td>
 * <td>{@code SizeTByReference}</td>
 * </tr>
 * </tbody>
 * </table>
 */
public class LibGMP {

    private LibGMP() {
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
     * The integer 0 (assuming no one changes it)
     */
    private static MPZPointer mpz_zero;

    /**
     * The rational 0 (assuming no one changes it)
     */
    private static MPQPointer mpq_zero;

    /**
     * The floating point 0 (assuming no one changes it)
     */
    private static MPFPointer mpf_zero;

    static {
        var nativeOptions = Map.of(
                Library.OPTION_FUNCTION_MAPPER, GMPFunctionMapper.getInstance());
        var library = NativeLibrary.getInstance(LIBNAME, nativeOptions);
        Native.register(library);

        var nonNativeOptions = Map.of(
                Library.OPTION_TYPE_MAPPER, GMPTypeMapper.getInstance(),
                Library.OPTION_FUNCTION_MAPPER, GMPFunctionMapper.getInstance());
        gmpextra = (LibGmpExtra) Native.load(LibGmpExtra.class, nonNativeOptions);

        gmp_version = library.getGlobalVariableAddress("__gmp_version").getPointer(0).getString(0);
        mp_bits_per_limb = library.getGlobalVariableAddress("__gmp_bits_per_limb").getInt(0);
        var dotPosition = gmp_version.indexOf(".");
        __GNU_MP_VERSION = Integer.parseInt(gmp_version.substring(0, dotPosition));
        var secondDotPosition = gmp_version.indexOf(".", dotPosition + 1);
        __GNU_MP_VERSION_MINOR = secondDotPosition > 0
                ? Integer.parseInt(gmp_version.substring(dotPosition + 1, secondDotPosition))
                : Integer.parseInt(gmp_version.substring(dotPosition + 1));
        __GNU_MP_VERSION_PATCHLEVEL = secondDotPosition > 0
                ? Integer.parseInt(gmp_version.substring(secondDotPosition + 1))
                : 0;

        mpz_zero = new MPZPointer();
        mpz_init(mpz_zero);
        mpq_zero = new MPQPointer();
        mpq_init(mpq_zero);
        mpf_zero = new MPFPointer();
        mpf_init(mpf_zero);
    }

    /**
     * Interface for the native functions with a variable number of arguments. These
     * are not supported by direct mapping, so we need to register them separately.
     */
    private static interface LibGmpExtra extends Library {
        int gmp_printf(String fmt, Object... args);

        int gmp_sprintf(ByteBuffer buf, String fmt, Object... args);

        int gmp_snprintf(ByteBuffer buf, SizeT size, String fmt, Object... args);

        int gmp_asprintf(PointerByReference pp, String fmt, Object... args);

        int gmp_scanf(String fmt, Object... args);

        int gmp_sscanf(String s, String fmt, Object... args);

        void mpz_inits(MPZPointer... xs);

        void mpz_clears(MPZPointer... xs);

        void mpq_inits(MPQPointer... xs);

        void mpq_clears(MPQPointer... xs);

        void mpf_inits(MPFPointer... xs);

        void mpf_clears(MPFPointer... xs);
    }

    /**
     * Instance of the {@code LibGmpExtra} interface created at initialization time.
     */
    private static final LibGmpExtra gmpextra;

    // Integer functions

    public static native void mpz_init(MPZPointer x);

    public static void mpz_inits(MPZPointer... xs) {
        gmpextra.mpz_inits(xs);
    }

    public static native void mpz_init2(MPZPointer x, MPBitCntT n);

    public static native void mpz_clear(MPZPointer x);

    public static void mpz_clears(MPZPointer... xs) {
        gmpextra.mpz_clears(xs);
    }

    public static native void mpz_realloc2(MPZPointer x, MPBitCntT n);

    public static native void mpz_set(MPZPointer rop, MPZPointer op);

    public static native void mpz_set_ui(MPZPointer rop, NativeUnsignedLong op);

    public static native void mpz_set_si(MPZPointer rop, NativeLong op);

    public static native void mpz_set_d(MPZPointer rop, double op);

    public static native void mpz_set_q(MPZPointer rop, MPQPointer op);

    public static native void mpz_set_f(MPZPointer rop, MPFPointer op);

    public static native int mpz_set_str(MPZPointer rop, String str, int base);

    public static native void mpz_swap(MPZPointer rop1, MPZPointer rop2);

    public static native void mpz_init_set(MPZPointer rop, MPZPointer op);

    public static native void mpz_init_set_ui(MPZPointer rop, NativeUnsignedLong op);

    public static native void mpz_init_set_si(MPZPointer rop, NativeLong op);

    public static native void mpz_init_set_d(MPZPointer rop, double op);

    public static native int mpz_init_set_str(MPZPointer rop, String str, int base);

    public static native NativeUnsignedLong mpz_get_ui(MPZPointer op);

    public static native NativeLong mpz_get_si(MPZPointer op);

    public static native double mpz_get_d(MPZPointer op);

    public static native double mpz_get_d_2exp(NativeLongByReference exp, MPZPointer op);

    public static native Pointer mpz_get_str(ByteBuffer str, int base, MPZPointer op);

    public static native void mpz_add(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native void mpz_add_ui(MPZPointer rop, MPZPointer op1, NativeUnsignedLong op2);

    public static native void mpz_sub(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native void mpz_sub_ui(MPZPointer rop, MPZPointer op1, NativeUnsignedLong op2);

    public static native void mpz_ui_sub(MPZPointer rop, NativeUnsignedLong op1, MPZPointer op2);

    public static native void mpz_mul(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native void mpz_mul_si(MPZPointer rop, MPZPointer op1, NativeLong op2);

    public static native void mpz_mul_ui(MPZPointer rop, MPZPointer op1, NativeUnsignedLong op2);

    public static native void mpz_addmul(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native void mpz_addmul_ui(MPZPointer rop, MPZPointer op1, NativeUnsignedLong op2);

    public static native void mpz_submul(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native void mpz_submul_ui(MPZPointer rop, MPZPointer op1, NativeUnsignedLong op2);

    public static native void mpz_mul_2exp(MPZPointer rop, MPZPointer op1, MPBitCntT op2);

    public static native void mpz_neg(MPZPointer rop, MPZPointer op);

    public static native void mpz_abs(MPZPointer rop, MPZPointer op);

    public static native void mpz_cdiv_q(MPZPointer q, MPZPointer n, MPZPointer d);

    public static native void mpz_cdiv_r(MPZPointer r, MPZPointer n, MPZPointer d);

    public static native void mpz_cdiv_qr(MPZPointer q, MPZPointer r, MPZPointer n, MPZPointer d);

    public static native NativeUnsignedLong mpz_cdiv_q_ui(MPZPointer q, MPZPointer n, NativeUnsignedLong d);

    public static native NativeUnsignedLong mpz_cdiv_r_ui(MPZPointer r, MPZPointer n, NativeUnsignedLong d);

    public static native NativeUnsignedLong mpz_cdiv_qr_ui(MPZPointer q, MPZPointer r, MPZPointer n,
            NativeUnsignedLong d);

    public static native NativeUnsignedLong mpz_cdiv_ui(MPZPointer n, NativeUnsignedLong d);

    public static native void mpz_cdiv_q_2exp(MPZPointer q, MPZPointer n, MPBitCntT b);

    public static native void mpz_cdiv_r_2exp(MPZPointer r, MPZPointer n, MPBitCntT b);

    public static native void mpz_fdiv_q(MPZPointer q, MPZPointer n, MPZPointer d);

    public static native void mpz_fdiv_r(MPZPointer r, MPZPointer n, MPZPointer d);

    public static native void mpz_fdiv_qr(MPZPointer q, MPZPointer r, MPZPointer n, MPZPointer d);

    public static native NativeUnsignedLong mpz_fdiv_q_ui(MPZPointer q, MPZPointer n, NativeUnsignedLong d);

    public static native NativeUnsignedLong mpz_fdiv_r_ui(MPZPointer r, MPZPointer n, NativeUnsignedLong d);

    public static native NativeUnsignedLong mpz_fdiv_qr_ui(MPZPointer q, MPZPointer r, MPZPointer n,
            NativeUnsignedLong d);

    public static native NativeUnsignedLong mpz_fdiv_ui(MPZPointer n, NativeUnsignedLong d);

    public static native void mpz_fdiv_q_2exp(MPZPointer q, MPZPointer n, NativeUnsignedLong b);

    public static native void mpz_fdiv_r_2exp(MPZPointer r, MPZPointer n, NativeUnsignedLong b);

    public static native void mpz_tdiv_q(MPZPointer q, MPZPointer n, MPZPointer d);

    public static native void mpz_tdiv_r(MPZPointer r, MPZPointer n, MPZPointer d);

    public static native void mpz_tdiv_qr(MPZPointer q, MPZPointer r, MPZPointer n, MPZPointer d);

    public static native NativeUnsignedLong mpz_tdiv_q_ui(MPZPointer q, MPZPointer n, NativeUnsignedLong d);

    public static native NativeUnsignedLong mpz_tdiv_r_ui(MPZPointer r, MPZPointer n, NativeUnsignedLong d);

    public static native NativeUnsignedLong mpz_tdiv_qr_ui(MPZPointer q, MPZPointer r, MPZPointer n,
            NativeUnsignedLong d);

    public static native NativeUnsignedLong mpz_tdiv_ui(MPZPointer n, NativeUnsignedLong d);

    public static native void mpz_tdiv_q_2exp(MPZPointer q, MPZPointer n, NativeUnsignedLong b);

    public static native void mpz_tdiv_r_2exp(MPZPointer r, MPZPointer n, NativeUnsignedLong b);

    public static native void mpz_mod(MPZPointer r, MPZPointer n, MPZPointer d);

    public static NativeUnsignedLong mpz_mod_ui(MPZPointer r, MPZPointer n, NativeUnsignedLong d) {
        return mpz_fdiv_r_ui(r, n, d);
    }

    public static native void mpz_divexact(MPZPointer r, MPZPointer n, MPZPointer d);

    public static native void mpz_divexact_ui(MPZPointer r, MPZPointer n, NativeUnsignedLong d);

    public static native boolean mpz_divisible_p(MPZPointer n, MPZPointer d);

    public static native boolean mpz_divisible_ui_p(MPZPointer n, NativeUnsignedLong d);

    public static native boolean mpz_divisible_2exp_p(MPZPointer n, MPBitCntT b);

    public static native boolean mpz_congruent_p(MPZPointer n, MPZPointer c, MPZPointer d);

    public static native boolean mpz_congruent_ui_p(MPZPointer n, NativeUnsignedLong c, NativeUnsignedLong d);

    public static native boolean mpz_congruent_2exp_p(MPZPointer n, MPZPointer c, MPBitCntT b);

    public static native void mpz_powm(MPZPointer rop, MPZPointer base, MPZPointer exp, MPZPointer mod);

    public static native void mpz_powm_ui(MPZPointer rop, MPZPointer base, NativeUnsignedLong exp, MPZPointer mod);

    public static native void mpz_powm_sec(MPZPointer rop, MPZPointer base, MPZPointer exp, MPZPointer mod);

    public static native void mpz_pow_ui(MPZPointer rop, MPZPointer base, NativeUnsignedLong exp);

    public static native void mpz_ui_pow_ui(MPZPointer rop, NativeUnsignedLong base, NativeUnsignedLong exp);

    public static native boolean mpz_root(MPZPointer rop, MPZPointer op, NativeUnsignedLong n);

    public static native void mpz_rootrem(MPZPointer rop, MPZPointer rem, MPZPointer op, NativeUnsignedLong n);

    public static native void mpz_sqrt(MPZPointer rop, MPZPointer op);

    public static native void mpz_sqrtrem(MPZPointer rop, MPZPointer rem, MPZPointer op);

    public static native boolean mpz_perfect_power_p(MPZPointer op);

    public static native boolean mpz_perfect_square_p(MPZPointer op);

    public static native int mpz_probab_prime_p(MPZPointer op, int reps);

    public static native void mpz_nextprime(MPZPointer rop, MPZPointer op);

    public static native void mpz_gcd(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native NativeUnsignedLong mpz_gcd_ui(MPZPointer rop, MPZPointer op1, NativeUnsignedLong op2);

    public static native void mpz_gcdext(MPZPointer g, MPZPointer s, MPZPointer t, MPZPointer a, MPZPointer b);

    public static native void mpz_lcm(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native void mpz_lcm_ui(MPZPointer rop, MPZPointer op1, NativeUnsignedLong op2);

    public static native boolean mpz_invert(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native int mpz_jacobi(MPZPointer a, MPZPointer b);

    public static native int mpz_legendre(MPZPointer a, MPZPointer b);

    public static native int mpz_kronecker_si(MPZPointer a, NativeLong b);

    public static native int mpz_kronecker_ui(MPZPointer a, NativeUnsignedLong b);

    public static native int mpz_si_kronecker(NativeLong a, MPZPointer b);

    public static native int mpz_ui_kronecker(NativeUnsignedLong a, MPZPointer b);

    public static native MPBitCntT mpz_remove(MPZPointer rop, MPZPointer op, MPZPointer f);

    public static native void mpz_fac_ui(MPZPointer rop, NativeUnsignedLong n);

    // This has been introduced in GMP 5.1.0
    public static native void mpz_2fac_ui(MPZPointer rop, NativeUnsignedLong n);

    // This has been introduced in GMP 5.1.0
    public static native void mpz_mfac_uiui(MPZPointer rop, NativeUnsignedLong n, NativeUnsignedLong m);

    // This has been introduced in GMP 5.1.0
    public static native void mpz_primorial_ui(MPZPointer rop, NativeUnsignedLong n);

    public static native void mpz_bin_ui(MPZPointer rop, MPZPointer n, NativeUnsignedLong k);

    public static native void mpz_bin_uiui(MPZPointer rop, NativeUnsignedLong n, NativeUnsignedLong k);

    public static native void mpz_fib_ui(MPZPointer fn, NativeUnsignedLong n);

    public static native void mpz_fib2_ui(MPZPointer fn, MPZPointer fnsub1, NativeUnsignedLong n);

    public static native void mpz_lucnum_ui(MPZPointer ln, NativeUnsignedLong n);

    public static native void mpz_lucnum2_ui(MPZPointer ln, MPZPointer lnsub1, NativeUnsignedLong n);

    public static native int mpz_cmp(MPZPointer op1, MPZPointer op2);

    public static native int mpz_cmp_d(MPZPointer op1, double op2);

    public static native int mpz_cmp_si(MPZPointer op1, NativeLong op2);

    public static native int mpz_cmp_ui(MPZPointer op1, NativeUnsignedLong op2);

    public static native int mpz_cmpabs(MPZPointer op1, MPZPointer op2);

    public static native int mpz_cmpabs_d(MPZPointer op1, double op2);

    public static native int mpz_cmpabs_ui(MPZPointer op1, NativeUnsignedLong op2);

    public static int mpz_sgn(MPZPointer op) {
        return mpz_cmp(op, mpz_zero);
    }

    public static native void mpz_and(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native void mpz_ior(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native void mpz_xor(MPZPointer rop, MPZPointer op1, MPZPointer op2);

    public static native void mpz_com(MPZPointer rop, MPZPointer op);

    public static native MPBitCntT mpz_popcount(MPZPointer op);

    public static native MPBitCntT mpz_hamdist(MPZPointer op1, MPZPointer op2);

    public static native MPBitCntT mpz_scan0(MPZPointer op, MPBitCntT starting_bit);

    public static native MPBitCntT mpz_scan1(MPZPointer op, MPBitCntT starting_bit);

    public static native MPBitCntT mpz_setbit(MPZPointer rop, MPBitCntT index);

    public static native MPBitCntT mpz_clrbit(MPZPointer rop, MPBitCntT index);

    public static native MPBitCntT mpz_combit(MPZPointer rop, MPBitCntT index);

    public static native int mpz_tstbit(MPZPointer rop, MPBitCntT index);

    public static native SizeT mpz_out_str(Pointer stream, int base, MPZPointer op);

    public static native SizeT mpz_inp_str(MPZPointer rop, Pointer stream, int base);

    public static native SizeT mpz_out_raw(Pointer stream, MPZPointer op);

    public static native SizeT mpz_inp_raw(MPZPointer rop, Pointer stream);

    public static native void mpz_urandomb(MPZPointer rop, RandStatePointer state, MPBitCntT n);

    public static native void mpz_urandomm(MPZPointer rop, RandStatePointer state, MPZPointer n);

    public static native void mpz_rrandomb(MPZPointer rop, RandStatePointer state, MPBitCntT n);

    public static native void mpz_random(MPZPointer rop, MPSizeT max_size);

    public static native void mpz_random2(MPZPointer rop, MPSizeT max_size);

    public static native void mpz_import(MPZPointer rop, SizeT count, int order, SizeT size, int endian, SizeT nails,
            ByteBuffer op);

    public static native MPZPointer mpz_export(ByteBuffer rop, SizeTByReference count, int order, SizeT size,
            int endian, SizeT nails, MPZPointer op);

    public static native boolean mpz_fits_ulong_p(MPZPointer op);

    public static native boolean mpz_fits_slong_p(MPZPointer op);

    public static native boolean mpz_fits_uint_p(MPZPointer op);

    public static native boolean mpz_fits_sint_p(MPZPointer op);

    public static native boolean mpz_fits_ushort_p(MPZPointer op);

    public static native boolean mpz_fits_sshort_p(MPZPointer op);

    public static native SizeT mpz_sizeinbase(MPZPointer op, int base);

    // Rational Number Functions

    public static native void mpq_canonicalize(MPQPointer x);

    public static native void mpq_init(MPQPointer x);

    public static void mpq_inits(MPQPointer... xs) {
        gmpextra.mpq_inits(xs);
    }

    public static native void mpq_clear(MPQPointer x);

    public static void mpq_clears(MPQPointer... xs) {
        gmpextra.mpq_clears(xs);
    }

    public static native void mpq_set(MPQPointer rop, MPQPointer op);

    public static native void mpq_set_z(MPQPointer rop, MPZPointer op);

    public static native void mpq_set_ui(MPQPointer rop, NativeUnsignedLong op1, NativeUnsignedLong op2);

    public static native void mpq_set_si(MPQPointer rop, NativeLong op1, NativeLong op2);

    public static native int mpq_set_str(MPQPointer rop, String str, int base);

    public static native void mpq_swap(MPQPointer rop1, MPQPointer rop2);

    public static native double mpq_get_d(MPQPointer op);

    public static native void mpq_set_d(MPQPointer rop, double op);

    public static native void mpq_set_f(MPQPointer rop, MPFPointer op);

    public static native Pointer mpq_get_str(ByteBuffer str, int base, MPQPointer op);

    public static native void mpq_add(MPQPointer rop, MPQPointer addend1, MPQPointer addend2);

    public static native void mpq_sub(MPQPointer rop, MPQPointer minuend, MPQPointer subtrahend);

    public static native void mpq_mul(MPQPointer rop, MPQPointer multiplier, MPQPointer multiplicand);

    public static native void mpq_mul_2exp(MPQPointer rop, MPQPointer op1, MPBitCntT op2);

    public static native void mpq_div(MPQPointer rop, MPQPointer dividend, MPQPointer divisor);

    public static native void mpq_div_2exp(MPQPointer rop, MPQPointer op1, MPBitCntT op2);

    public static native void mpq_neg(MPQPointer rop, MPQPointer operand);

    public static native void mpq_abs(MPQPointer rop, MPQPointer operand);

    public static native void mpq_inv(MPQPointer rop, MPQPointer number);

    public static native int mpq_cmp(MPQPointer op1, MPQPointer op2);

    public static native int mpq_cmp_z(MPQPointer op1, MPZPointer op2);

    public static native int mpq_cmp_ui(MPQPointer op1, NativeUnsignedLong num2, NativeUnsignedLong den2);

    public static native int mpq_cmp_si(MPQPointer op1, NativeLong op2, NativeUnsignedLong den2);

    public static int mpq_sgn(MPQPointer op) {
        return mpq_cmp(op, mpq_zero);
    }

    public static native boolean mpq_equal(MPQPointer op1, MPQPointer op2);

    public static native void mpq_get_num(MPZPointer numerator, MPQPointer rational);

    public static native void mpq_get_den(MPZPointer denominator, MPQPointer rational);

    public static native void mpq_set_num(MPQPointer rational, MPZPointer numerator);

    public static native void mpq_set_den(MPQPointer rational, MPZPointer denominator);

    // Floating-point functions

    public static native void mpf_set_default_prec(MPBitCntT prec);

    public static native MPBitCntT mpf_get_default_prec();

    public static native void mpf_init(MPFPointer x);

    public static native void mpf_init2(MPFPointer x, MPBitCntT n);

    public static void mpf_inits(MPFPointer... xs) {
        gmpextra.mpf_inits(xs);
    }

    public static native void mpf_clear(MPFPointer x);

    public static void mpf_clears(MPFPointer... xs) {
        gmpextra.mpf_clears(xs);
    }

    public static native MPBitCntT mpf_get_prec(MPFPointer op);

    public static native void mpf_set_prec(MPFPointer rop, MPBitCntT prec);

    public static native void mpf_set_prec_raw(MPFPointer rop, MPBitCntT prec);

    public static native int mpf_cmp(MPFPointer op1, MPFPointer op2);

    public static native int mpf_cmp_z(MPFPointer op1, MPZPointer op2);

    public static native int mpf_cmp_d(MPFPointer op1, double op2);

    public static native int mpf_cmp_ui(MPFPointer op1, NativeUnsignedLong op2);

    public static native int mpf_cmp_si(MPFPointer op1, NativeLong op2);

    public static native boolean mpf_eq(MPFPointer op1, MPFPointer op2, MPBitCntT op3);

    public static native int mpf_reldiff(MPFPointer rop, MPFPointer op1, MPFPointer op2);

    public static int mpf_sgn(MPFPointer op) {
        return mpf_cmp(op, mpf_zero);
    }

    public static native void mpf_set(MPFPointer rop, MPFPointer op);

    public static native void mpf_set_ui(MPFPointer rop, NativeUnsignedLong op);

    public static native void mpf_set_si(MPFPointer rop, NativeLong op);

    public static native void mpf_set_d(MPFPointer rop, double op);

    public static native void mpf_set_z(MPFPointer rop, MPZPointer op);

    public static native void mpf_set_q(MPFPointer rop, MPQPointer op);

    public static native int mpf_set_str(MPFPointer rop, String str, int base);

    public static native void mpf_swap(MPFPointer rop1, MPFPointer rop2);

    public static native void mpf_init_set(MPFPointer rop, MPFPointer op);

    public static native void mpf_init_set_ui(MPFPointer rop, NativeUnsignedLong op);

    public static native void mpf_init_set_si(MPFPointer rop, NativeLong op);

    public static native void mpf_init_set_d(MPFPointer rop, double op);

    public static native int mpf_init_set_str(MPFPointer rop, String str, int base);

    public static native NativeUnsignedLong mpf_get_ui(MPFPointer op);

    public static native NativeLong mpf_get_si(MPFPointer op);

    public static native double mpf_get_d(MPFPointer op);

    public static native double mpf_get_d_2exp(NativeLongByReference exp, MPFPointer op);

    public static native Pointer mpf_get_str(ByteBuffer str, MPExpTByReference exp, int base, MPSizeT nDigits,
            MPFPointer op);

    public static native void mpf_add(MPFPointer rop, MPFPointer op1, MPFPointer op2);

    public static native void mpf_add_ui(MPFPointer rop, MPFPointer op1, NativeUnsignedLong op2);

    public static native void mpf_sub(MPFPointer rop, MPFPointer op1, MPFPointer op2);

    public static native void mpf_sub_ui(MPFPointer rop, MPFPointer op1, NativeUnsignedLong op2);

    public static native void mpf_ui_sub(MPFPointer rop, NativeUnsignedLong op1, MPFPointer op2);

    public static native void mpf_mul(MPFPointer rop, MPFPointer op1, MPFPointer op2);

    public static native void mpf_mul_ui(MPFPointer rop, MPFPointer op1, NativeUnsignedLong op2);

    public static native void mpf_div(MPFPointer rop, MPFPointer op1, MPFPointer op2);

    public static native void mpf_div_ui(MPFPointer rop, MPFPointer op1, NativeUnsignedLong op2);

    public static native void mpf_ui_div(MPFPointer rop, NativeUnsignedLong op1, MPFPointer op2);

    public static native void mpf_sqrt(MPFPointer rop, MPFPointer op);

    public static native void mpf_sqrt_ui(MPFPointer rop, NativeUnsignedLong op);

    public static native void mpf_pow_ui(MPFPointer rop, MPFPointer op1, NativeUnsignedLong op2);

    public static native void mpf_neg(MPFPointer rop, MPFPointer op);

    public static native void mpf_abs(MPFPointer rop, MPFPointer op);

    public static native void mpf_mul_2exp(MPFPointer rop, MPFPointer op1, MPBitCntT op2);

    public static native void mpf_div_2exp(MPFPointer rop, MPFPointer op1, MPBitCntT op2);

    public static native void mpf_ceil(MPFPointer rop, MPFPointer op);

    public static native void mpf_floor(MPFPointer rop, MPFPointer op);

    public static native void mpf_trunc(MPFPointer rop, MPFPointer op);

    public static native boolean mpf_integer_p(MPFPointer op);

    public static native boolean mpf_fits_ulong_p(MPFPointer op);

    public static native boolean mpf_fits_slong_p(MPFPointer op);

    public static native boolean mpf_fits_uint_p(MPFPointer op);

    public static native boolean mpf_fits_sint_p(MPFPointer op);

    public static native boolean mpf_fits_ushort_p(MPFPointer op);

    public static native boolean mpf_fits_sshort_p(MPFPointer op);

    public static native void mpf_urandomb(MPFPointer rop, RandStatePointer state, MPBitCntT n);

    public static native void mpf_random2(MPFPointer rop, MPSizeT max_size, MPExpT exp);

    // Random Number Functions

    public static native void gmp_randinit_default(RandStatePointer state);

    public static native void gmp_randinit_mt(RandStatePointer state);

    public static native void gmp_randinit_lc_2exp(RandStatePointer state, MPZPointer a, NativeLong c,
            NativeLong m2exp);

    public static native int gmp_randinit_lc_2exp_size(RandStatePointer state, NativeLong m2exp);

    public static native void gmp_randinit_set(RandStatePointer rop, RandStatePointer op);

    public static native void gmp_randinit(RandStatePointer state, int alg, NativeLong l);

    public static native void gmp_randclear(RandStatePointer state);

    public static native void gmp_randseed(RandStatePointer state, MPZPointer seed);

    public static native void gmp_randseed_ui(RandStatePointer state, NativeUnsignedLong seed);

    public static native NativeLong gmp_urandomb_ui(RandStatePointer state, NativeUnsignedLong n);

    public static native NativeLong gmp_urandomm_ui(RandStatePointer state, NativeUnsignedLong n);

    // Formatted Output

    public static int gmp_printf(String fmt, Object... args) {
        return gmpextra.gmp_printf(fmt, args);
    }

    public static int gmp_sprintf(ByteBuffer buf, String fmt, Object... args) {
        return gmpextra.gmp_sprintf(buf, fmt, args);
    }

    public static int gmp_snprintf(ByteBuffer buf, SizeT size, String fmt, Object... args) {
        return gmpextra.gmp_snprintf(buf, size, fmt, args);
    }

    public static int gmp_asprintf(PointerByReference pp, String fmt, Object... args) {
        return gmpextra.gmp_asprintf(pp, fmt, args);
    }

    // Formatted Input

    public static int gmp_scanf(String fmt, Object... args) {
        return gmpextra.gmp_scanf(fmt, args);
    }

    public static int gmp_sscanf(String s, String fmt, Object... args) {
        return gmpextra.gmp_sscanf(s, fmt, args);
    }
}