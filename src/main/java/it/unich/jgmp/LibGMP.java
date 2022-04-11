package it.unich.jgmp;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.ptr.NativeLongByReference;

final class LibGMP {

    private static final String LIBNAME = "gmp";

    private static final LibGmpExtra gmpextra;

    static final int MPZ_SIZE = 16;

    static final String __gmp_version;

    static interface LibGmpExtra extends Library {
        int __gmp_printf(String fmt, Object ...args);
        void __gmpz_inits(Pointer... xs);
        void __gmpz_clears(Pointer... xs);
    }

    static {
        NativeLibrary library = NativeLibrary.getInstance(LIBNAME);
        Native.register(LIBNAME);

        //__gmp_printf = library.getFunction("__gmp_printf");
        __gmp_version = library.getGlobalVariableAddress("__gmp_version").getPointer(0).getString(0);

        gmpextra = (LibGmpExtra) Native.load(LibGmpExtra.class);
    }

    static native void __gmpz_init(Pointer x);
    static        void __gmpz_inits(Pointer xs) { gmpextra.__gmpz_inits(xs); }
    static native void __gmpz_init2(Pointer x, int n);
    static native void __gmpz_clear(Pointer integer);
    static        void __gmpz_clears(Pointer xs) { gmpextra.__gmpz_clears(xs); }
    static native void __gmpz_realloc2(Pointer x, int n);

    static native void __gmpz_set(Pointer rop, Pointer op);
    static native void __gmpz_set_ui(Pointer rop, NativeLong op);
    static native void __gmpz_set_si(Pointer rop, NativeLong op);
    static native void __gmpz_set_d(Pointer rop, double op);
    static native void __gmpz_set_q(Pointer rop, Pointer op);
    static native void __gmpz_set_f(Pointer rop, Pointer op);
    static native int __gmpz_set_str(Pointer rop, String str, int base);
    static native int __gmpz_swap(Pointer rop1, Pointer rop2);

    static native void __gmpz_init_set(Pointer rop, Pointer op);
    static native void __gmpz_init_set_ui(Pointer rop, NativeLong op);
    static native void __gmpz_init_set_si(Pointer rop, NativeLong op);
    static native void __gmpz_init_set_d(Pointer rop, double op);
    static native int __gmpz_init_set_str(Pointer rop, String str, int base);

    static native NativeLong __gmpz_get_ui(Pointer op);
    static native NativeLong __gmpz_get_si(Pointer op);
    static native double __gmpz_get_d(Pointer op);
    static native double __gmpz_get_d_2exp(NativeLongByReference exp, Pointer op);
    static native Pointer __gmpz_get_str(Pointer str, int base, Pointer op);

    static native void __gmpz_add(Pointer rop, Pointer op1, Pointer op2);
    static native void __gmpz_add_ui(Pointer rop, Pointer op1, NativeLong op2);
    static native void __gmpz_sub(Pointer rop, Pointer op1, Pointer op2);
    static native void __gmpz_sub_ui(Pointer rop, Pointer op1, NativeLong op2);
    static native void __gmpz_ui_sub(Pointer rop, NativeLong op1, Pointer op2);
    static native void __gmpz_mul(Pointer rop, Pointer op1, Pointer op2);
    static native void __gmpz_mul_si(Pointer rop, Pointer op1, NativeLong op2);
    static native void __gmpz_mul_ui(Pointer rop, Pointer op1, NativeLong op2);
    static native void __gmpz_addmul(Pointer rop, Pointer op1, Pointer op2);
    static native void __gmpz_addmul_ui(Pointer rop, Pointer op1, NativeLong op2);
    static native void __gmpz_submul(Pointer rop, Pointer op1, Pointer op2);
    static native void __gmpz_submul_ui(Pointer rop, Pointer op1, NativeLong op2);
    static native void __gmpz_mul_2exp(Pointer rop, Pointer op1, NativeLong op2);  // mp_bitcnt_r
    static native void __gmpz_neg(Pointer rop, Pointer op);
    static native void __gmpz_abs(Pointer rop, Pointer op);

    static native void __gmpz_cdiv_q(Pointer q, Pointer n, Pointer d);
    static native void __gmpz_cdiv_r(Pointer r, Pointer n, Pointer d);
    static native void __gmpz_cdiv_qr(Pointer q, Pointer r, Pointer n, Pointer d);
    static native NativeLong __gmpz_cdiv_q_ui(Pointer q, Pointer n, NativeLong d);
    static native NativeLong __gmpz_cdiv_r_ui(Pointer r, Pointer n, NativeLong d);
    static native NativeLong __gmpz_cdiv_qr_ui(Pointer q, Pointer r, Pointer n, NativeLong d);
    static native NativeLong __gmpz_cdiv_ui(Pointer n, NativeLong d);
    static native void __gmpz_cdiv_q_2exp(Pointer q, Pointer n, NativeLong b); // mp_bitcnt_r
    static native void __gmpz_cdiv_r_2exp(Pointer r, Pointer n, NativeLong b); // mp_bitcnt_r
    static native void __gmpz_fdiv_q(Pointer q, Pointer n, Pointer d);
    static native void __gmpz_fdiv_r(Pointer r, Pointer n, Pointer d);
    static native void __gmpz_fdiv_qr(Pointer q, Pointer r, Pointer n, Pointer d);
    static native NativeLong __gmpz_fdiv_q_ui(Pointer q, Pointer n, NativeLong d);
    static native NativeLong __gmpz_fdiv_r_ui(Pointer r, Pointer n, NativeLong d);
    static native NativeLong __gmpz_fdiv_qr_ui(Pointer q, Pointer r, Pointer n, NativeLong d);
    static native NativeLong __gmpz_fdiv_ui(Pointer n, NativeLong d);
    static native void __gmpz_fdiv_q_2exp(Pointer q, Pointer n, NativeLong b); // mp_bitcnt_r
    static native void __gmpz_fdiv_r_2exp(Pointer r, Pointer n, NativeLong b); // mp_bitcnt_r
    static native void __gmpz_tdiv_q(Pointer q, Pointer n, Pointer d);
    static native void __gmpz_tdiv_r(Pointer r, Pointer n, Pointer d);
    static native void __gmpz_tdiv_qr(Pointer q, Pointer r, Pointer n, Pointer d);
    static native NativeLong __gmpz_tdiv_q_ui(Pointer q, Pointer n, NativeLong d);
    static native NativeLong __gmpz_tdiv_r_ui(Pointer r, Pointer n, NativeLong d);
    static native NativeLong __gmpz_tdiv_qr_ui(Pointer q, Pointer r, Pointer n, NativeLong d);
    static native NativeLong __gmpz_tdiv_ui(Pointer n, NativeLong d);
    static native void __gmpz_tdiv_q_2exp(Pointer q, Pointer n, NativeLong b); // mp_bitcnt_r
    static native void __gmpz_tdiv_r_2exp(Pointer r, Pointer n, NativeLong b); // mp_bitcnt_r

    static native void __gmpz_powm(Pointer rop, Pointer base, Pointer exp, Pointer mod);
    static native void __gmpz_powm_ui(Pointer rop, Pointer base, NativeLong exp, Pointer mod);
    static native void __gmpz_powm_sec(Pointer rop, Pointer base, Pointer exp, Pointer mod);
    static native void __gmpz_pow_ui(Pointer rop, Pointer base, NativeLong exp);
    static native void __gmpz_ui_pow_ui(Pointer rop, NativeLong base, NativeLong exp);

    static native void __gmpz_root(Pointer rop, Pointer op, NativeLong n);
    static native void __gmpz_rootrem(Pointer rop, Pointer op, NativeLong n);
    static native void __gmpz_sqrt(Pointer rop, Pointer op);
    static native void __gmpz_sqrtrem(Pointer rop, Pointer op);
    static native int __gmpz_perfect_power_p(Pointer op);
    static native int __gmpz_perfect_square_p(Pointer op);

    static native int __gmpz_probab_prime_p(Pointer op, int reps);
    static native void __gmpz_nextprime(Pointer rop, Pointer op);

    static native int __gmpz_cmp(Pointer op1, Pointer op2);
    static native int __gmpz_cmp_d(Pointer op1, double op2);
    static native int __gmpz_cmp_si(Pointer op1, NativeLong op2);
    static native int __gmpz_cmp_ui(Pointer op1, NativeLong op2);
    static native int __gmpz_cmpabs(Pointer op1, Pointer op2);
    static native int __gmpz_cmpabs_d(Pointer op1, double op2);
    static native int __gmpz_cmpabs_ui(Pointer op1, NativeLong op2);
    static int __gmpz_sgn(Pointer op) {
        // The __gmpz_neg is a macro which requires access to the internal structure of an mpz_t.
        Memory zero = new Memory(MPZ_SIZE);
        __gmpz_init(zero);
        return __gmpz_cmp(op, zero);
    }

	/*
	 — Function: void mpz_cdiv_q (mpz_t q, const mpz_t n, const mpz_t d)
	 — Function: void mpz_cdiv_r (mpz_t r, const mpz_t n, const mpz_t d)
	 — Function: void mpz_cdiv_qr (mpz_t q, mpz_t r, const mpz_t n, const mpz_t d)
	 — Function: unsigned NativeLong int mpz_cdiv_q_ui (mpz_t q, const mpz_t n, unsigned NativeLong int d)
	 — Function: unsigned NativeLong int mpz_cdiv_r_ui (mpz_t r, const mpz_t n, unsigned NativeLong int d)
	 — Function: unsigned NativeLong int mpz_cdiv_qr_ui (mpz_t q, mpz_t r, const mpz_t n, unsigned NativeLong int d)
	 — Function: unsigned NativeLong int mpz_cdiv_ui (const mpz_t n, unsigned NativeLong int d)
	 — Function: void mpz_cdiv_q_2exp (mpz_t q, const mpz_t n, mp_bitcnt_t b)
	 — Function: void mpz_cdiv_r_2exp (mpz_t r, const mpz_t n, mp_bitcnt_t b)
	 — Function: void mpz_fdiv_q (mpz_t q, const mpz_t n, const mpz_t d)
	 — Function: void mpz_fdiv_r (mpz_t r, const mpz_t n, const mpz_t d)
	 — Function: void mpz_fdiv_qr (mpz_t q, mpz_t r, const mpz_t n, const mpz_t d)
	 — Function: unsigned NativeLong int mpz_fdiv_q_ui (mpz_t q, const mpz_t n, unsigned NativeLong int d)
	 — Function: unsigned NativeLong int mpz_fdiv_r_ui (mpz_t r, const mpz_t n, unsigned NativeLong int d)
	 — Function: unsigned NativeLong int mpz_fdiv_qr_ui (mpz_t q, mpz_t r, const mpz_t n, unsigned NativeLong int d)
	 — Function: unsigned NativeLong int mpz_fdiv_ui (const mpz_t n, unsigned NativeLong int d)
	 — Function: void mpz_fdiv_q_2exp (mpz_t q, const mpz_t n, mp_bitcnt_t b)
	 — Function: void mpz_fdiv_r_2exp (mpz_t r, const mpz_t n, mp_bitcnt_t b)
	 — Function: void mpz_tdiv_q (mpz_t q, const mpz_t n, const mpz_t d)
	 — Function: void mpz_tdiv_r (mpz_t r, const mpz_t n, const mpz_t d)
	 — Function: void mpz_tdiv_qr (mpz_t q, mpz_t r, const mpz_t n, const mpz_t d)
	 — Function: unsigned NativeLong int mpz_tdiv_q_ui (mpz_t q, const mpz_t n, unsigned NativeLong int d)
	 — Function: unsigned NativeLong int mpz_tdiv_r_ui (mpz_t r, const mpz_t n, unsigned NativeLong int d)
	 — Function: unsigned NativeLong int mpz_tdiv_qr_ui (mpz_t q, mpz_t r, const mpz_t n, unsigned NativeLong int d)
	 — Function: unsigned NativeLong int mpz_tdiv_ui (const mpz_t n, unsigned NativeLong int d)
	 — Function: void mpz_tdiv_q_2exp (mpz_t q, const mpz_t n, mp_bitcnt_t b)
	 — Function: void mpz_tdiv_r_2exp (mpz_t r, const mpz_t n, mp_bitcnt_t b)
	*/

    static native NativeLong __gmpz_sizeinbase(Pointer op, int base);

    static int __gmp_printf(String fmt, Object... args) { return gmpextra.__gmp_printf(fmt,args); }
}