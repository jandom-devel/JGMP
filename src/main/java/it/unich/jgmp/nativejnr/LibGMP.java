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
package it.unich.jgmp.nativejnr;

import java.nio.ByteBuffer;

import jnr.ffi.NativeLong;
import jnr.ffi.Pointer;
import jnr.ffi.byref.NativeLongByReference;
import jnr.ffi.byref.NumberByReference;
import jnr.ffi.types.size_t;

public interface LibGMP {

    // Integer functions

    public void __gmpz_init(Pointer x);

    public void __gmpz_inits(Pointer xs);

    public void __gmpz_init2(Pointer x, NativeLong n);

    public void __gmpz_clear(Pointer x);

    public void __gmpz_clears(Pointer xs);

    public void __gmpz_realloc2(Pointer x, NativeLong n);

    public void __gmpz_set(Pointer rop, Pointer op);

    public void __gmpz_set_ui(Pointer rop, NativeLong op);

    public void __gmpz_set_si(Pointer rop, NativeLong op);

    public void __gmpz_set_d(Pointer rop, double op);

    public void __gmpz_set_q(Pointer rop, Pointer op);

    public void __gmpz_set_f(Pointer rop, Pointer op);

    public int __gmpz_set_str(Pointer rop, String str, int base);

    public void __gmpz_swap(Pointer rop1, Pointer rop2);

    public void __gmpz_init_set(Pointer rop, Pointer op);

    public void __gmpz_init_set_ui(Pointer rop, NativeLong op);

    public void __gmpz_init_set_si(Pointer rop, NativeLong op);

    public void __gmpz_init_set_d(Pointer rop, double op);

    public int __gmpz_init_set_str(Pointer rop, String str, int base);

    public NativeLong __gmpz_get_ui(Pointer op);

    public NativeLong __gmpz_get_si(Pointer op);

    public double __gmpz_get_d(Pointer op);

    public double __gmpz_get_d_2exp(NativeLongByReference exp, Pointer op);

    public Pointer __gmpz_get_str(ByteBuffer str, int base, Pointer op);

    public void __gmpz_add(Pointer rop, Pointer op1, Pointer op2);

    public void __gmpz_add_ui(Pointer rop, Pointer op1, NativeLong op2);

    public void __gmpz_sub(Pointer rop, Pointer op1, Pointer op2);

    public void __gmpz_sub_ui(Pointer rop, Pointer op1, NativeLong op2);

    public void __gmpz_ui_sub(Pointer rop, NativeLong op1, Pointer op2);

    public void __gmpz_mul(Pointer rop, Pointer op1, Pointer op2);

    public void __gmpz_mul_si(Pointer rop, Pointer op1, NativeLong op2);

    public void __gmpz_mul_ui(Pointer rop, Pointer op1, NativeLong op2);

    public void __gmpz_addmul(Pointer rop, Pointer op1, Pointer op2);

    public void __gmpz_addmul_ui(Pointer rop, Pointer op1, NativeLong op2);

    public void __gmpz_submul(Pointer rop, Pointer op1, Pointer op2);

    public void __gmpz_submul_ui(Pointer rop, Pointer op1, NativeLong op2);

    public void __gmpz_mul_2exp(Pointer rop, Pointer op1, NativeLong op2);

    public void __gmpz_neg(Pointer rop, Pointer op);

    public void __gmpz_abs(Pointer rop, Pointer op);

    public void __gmpz_cdiv_q(Pointer q, Pointer n, Pointer d);

    public void __gmpz_cdiv_r(Pointer r, Pointer n, Pointer d);

    public void __gmpz_cdiv_qr(Pointer q, Pointer r, Pointer n, Pointer d);

    public NativeLong __gmpz_cdiv_q_ui(Pointer q, Pointer n, NativeLong d);

    public NativeLong __gmpz_cdiv_r_ui(Pointer r, Pointer n, NativeLong d);

    public NativeLong __gmpz_cdiv_qr_ui(Pointer q, Pointer r, Pointer n,
            NativeLong d);

    public NativeLong __gmpz_cdiv_ui(Pointer n, NativeLong d);

    public void __gmpz_cdiv_q_2exp(Pointer q, Pointer n, NativeLong b);

    public void __gmpz_cdiv_r_2exp(Pointer r, Pointer n, NativeLong b);

    public void __gmpz_fdiv_q(Pointer q, Pointer n, Pointer d);

    public void __gmpz_fdiv_r(Pointer r, Pointer n, Pointer d);

    public void __gmpz_fdiv_qr(Pointer q, Pointer r, Pointer n, Pointer d);

    public NativeLong __gmpz_fdiv_q_ui(Pointer q, Pointer n, NativeLong d);

    public NativeLong __gmpz_fdiv_r_ui(Pointer r, Pointer n, NativeLong d);

    public NativeLong __gmpz_fdiv_qr_ui(Pointer q, Pointer r, Pointer n,
            NativeLong d);

    public NativeLong __gmpz_fdiv_ui(Pointer n, NativeLong d);

    public void __gmpz_fdiv_q_2exp(Pointer q, Pointer n, NativeLong b);

    public void __gmpz_fdiv_r_2exp(Pointer r, Pointer n, NativeLong b);

    public void __gmpz_tdiv_q(Pointer q, Pointer n, Pointer d);

    public void __gmpz_tdiv_r(Pointer r, Pointer n, Pointer d);

    public void __gmpz_tdiv_qr(Pointer q, Pointer r, Pointer n, Pointer d);

    public NativeLong __gmpz_tdiv_q_ui(Pointer q, Pointer n, NativeLong d);

    public NativeLong __gmpz_tdiv_r_ui(Pointer r, Pointer n, NativeLong d);

    public NativeLong __gmpz_tdiv_qr_ui(Pointer q, Pointer r, Pointer n,
            NativeLong d);

    public NativeLong __gmpz_tdiv_ui(Pointer n, NativeLong d);

    public void __gmpz_tdiv_q_2exp(Pointer q, Pointer n, NativeLong b);

    public void __gmpz_tdiv_r_2exp(Pointer r, Pointer n, NativeLong b);

    public void __gmpz_mod(Pointer r, Pointer n, Pointer d);

    public default NativeLong __gmpz_mod_ui(Pointer r, Pointer n, NativeLong d) {
        return __gmpz_fdiv_r_ui(r, n, d);
    }

    public void __gmpz_divexact(Pointer r, Pointer n, Pointer d);

    public void __gmpz_divexact_ui(Pointer r, Pointer n, NativeLong d);

    public boolean __gmpz_divisible_p(Pointer n, Pointer d);

    public boolean __gmpz_divisible_ui_p(Pointer n, NativeLong d);

    public boolean __gmpz_divisible_2exp_p(Pointer n, NativeLong b);

    public boolean __gmpz_congruent_p(Pointer n, Pointer c, Pointer d);

    public boolean __gmpz_congruent_ui_p(Pointer n, NativeLong c, NativeLong d);

    public boolean __gmpz_congruent_2exp_p(Pointer n, Pointer c, NativeLong b);

    public void __gmpz_powm(Pointer rop, Pointer base, Pointer exp, Pointer mod);

    public void __gmpz_powm_ui(Pointer rop, Pointer base, NativeLong exp, Pointer mod);

    public void __gmpz_powm_sec(Pointer rop, Pointer base, Pointer exp, Pointer mod);

    public void __gmpz_pow_ui(Pointer rop, Pointer base, NativeLong exp);

    public void __gmpz_ui_pow_ui(Pointer rop, NativeLong base, NativeLong exp);

    public boolean __gmpz_root(Pointer rop, Pointer op, NativeLong n);

    public void __gmpz_rootrem(Pointer rop, Pointer rem, Pointer op, NativeLong n);

    public void __gmpz_sqrt(Pointer rop, Pointer op);

    public void __gmpz_sqrtrem(Pointer rop, Pointer rem, Pointer op);

    public boolean __gmpz_perfect_power_p(Pointer op);

    public boolean __gmpz_perfect_square_p(Pointer op);

    public int __gmpz_probab_prime_p(Pointer op, int reps);

    public void __gmpz_nextprime(Pointer rop, Pointer op);

    public void __gmpz_gcd(Pointer rop, Pointer op1, Pointer op2);

    public NativeLong __gmpz_gcd_ui(Pointer rop, Pointer op1, NativeLong op2);

    public void __gmpz_gcdext(Pointer g, Pointer s, Pointer t, Pointer a, Pointer b);

    public void __gmpz_lcm(Pointer rop, Pointer op1, Pointer op2);

    public void __gmpz_lcm_ui(Pointer rop, Pointer op1, NativeLong op2);

    public boolean __gmpz_invert(Pointer rop, Pointer op1, Pointer op2);

    public int __gmpz_jacobi(Pointer a, Pointer b);

    public int __gmpz_legendre(Pointer a, Pointer b);

    public int __gmpz_kronecker_si(Pointer a, NativeLong b);

    public int __gmpz_kronecker_ui(Pointer a, NativeLong b);

    public int __gmpz_si_kronecker(NativeLong a, Pointer b);

    public int __gmpz_ui_kronecker(NativeLong a, Pointer b);

    public NativeLong __gmpz_remove(Pointer rop, Pointer op, Pointer f);

    public void __gmpz_fac_ui(Pointer rop, NativeLong n);

    // This has been introduced in GMP 5.1.0
    public void __gmpz_2fac_ui(Pointer rop, NativeLong n);

    // This has been introduced in GMP 5.1.0
    public void __gmpz_mfac_uiui(Pointer rop, NativeLong n, NativeLong m);

    // This has been introduced in GMP 5.1.0
    public void __gmpz_primorial_ui(Pointer rop, NativeLong n);

    public void __gmpz_bin_ui(Pointer rop, Pointer n, NativeLong k);

    public void __gmpz_bin_uiui(Pointer rop, NativeLong n, NativeLong k);

    public void __gmpz_fib_ui(Pointer fn, NativeLong n);

    public void __gmpz_fib2_ui(Pointer fn, Pointer fnsub1, NativeLong n);

    public void __gmpz_lucnum_ui(Pointer ln, NativeLong n);

    public void __gmpz_lucnum2_ui(Pointer ln, Pointer lnsub1, NativeLong n);

    public int __gmpz_cmp(Pointer op1, Pointer op2);

    public int __gmpz_cmp_d(Pointer op1, double op2);

    public int __gmpz_cmp_si(Pointer op1, NativeLong op2);

    public int __gmpz_cmp_ui(Pointer op1, NativeLong op2);

    public int __gmpz_cmpabs(Pointer op1, Pointer op2);

    public int __gmpz_cmpabs_d(Pointer op1, double op2);

    public int __gmpz_cmpabs_ui(Pointer op1, NativeLong op2);

    /*
    public default int __gmpz_sgn(Pointer op) {
        return __gmpz_cmp(op, __gmpz_zero);
    }
    */

    public void __gmpz_and(Pointer rop, Pointer op1, Pointer op2);

    public void __gmpz_ior(Pointer rop, Pointer op1, Pointer op2);

    public void __gmpz_xor(Pointer rop, Pointer op1, Pointer op2);

    public void __gmpz_com(Pointer rop, Pointer op);

    public NativeLong __gmpz_popcount(Pointer op);

    public NativeLong __gmpz_hamdist(Pointer op1, Pointer op2);

    public NativeLong __gmpz_scan0(Pointer op, NativeLong starting_bit);

    public NativeLong __gmpz_scan1(Pointer op, NativeLong starting_bit);

    public NativeLong __gmpz_setbit(Pointer rop, NativeLong index);

    public NativeLong __gmpz_clrbit(Pointer rop, NativeLong index);

    public NativeLong __gmpz_combit(Pointer rop, NativeLong index);

    public int __gmpz_tstbit(Pointer rop, NativeLong index);

    public @size_t long __gmpz_out_str(Pointer stream, int base, Pointer op);

    public @size_t long __gmpz_inp_str(Pointer rop, Pointer stream, int base);

    public @size_t long __gmpz_out_raw(Pointer stream, Pointer op);

    public @size_t long __gmpz_inp_raw(Pointer rop, Pointer stream);

    public void __gmpz_urandomb(Pointer rop, Pointer state, NativeLong n);

    public void __gmpz_urandomm(Pointer rop, Pointer state, Pointer n);

    public void __gmpz_rrandomb(Pointer rop, Pointer state, NativeLong n);

    public void __gmpz_random(Pointer rop, NativeLong max_size);

    public void __gmpz_random2(Pointer rop, NativeLong max_size);

    public void __gmpz_import(Pointer rop, @size_t long count, int order, @size_t long size, int endian, @size_t long nails,
            ByteBuffer op);

    public Pointer __gmpz_export(ByteBuffer rop, NumberByReference count, int order, @size_t long size,
            int endian, @size_t long nails, Pointer op);

    public boolean __gmpz_fits_ulong_p(Pointer op);

    public boolean __gmpz_fits_slong_p(Pointer op);

    public boolean __gmpz_fits_uint_p(Pointer op);

    public boolean __gmpz_fits_sint_p(Pointer op);

    public boolean __gmpz_fits_ushort_p(Pointer op);

    public boolean __gmpz_fits_sshort_p(Pointer op);

    public @size_t long __gmpz_sizeinbase(Pointer op, int base);

    // Random Number Functions

    public void __gmp_randinit_default(Pointer state);

    public void __gmp_randinit_mt(Pointer state);

    public void __gmp_randinit_lc_2exp(Pointer state, Pointer a, NativeLong c,
            NativeLong m2exp);

    public int __gmp_randinit_lc_2exp_size(Pointer state, NativeLong m2exp);

    public void __gmp_randinit_set(Pointer rop, Pointer op);

    public void __gmp_randinit(Pointer state, int alg, NativeLong l);

    public void __gmp_randclear(Pointer state);

    public void __gmp_randseed(Pointer state, Pointer seed);

    public void __gmp_randseed_ui(Pointer state, NativeLong seed);

    public NativeLong __gmp_urandomb_ui(Pointer state, NativeLong n);

    public NativeLong __gmp_urandomm_ui(Pointer state, NativeLong n);

    // Formatted Output

    public int __gmp_printf(String fmt, Object... args);
}