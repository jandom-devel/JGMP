package it.unich.jgmp;

import static it.unich.jgmp.LibGMP.*;

import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

import com.sun.jna.ptr.NativeLongByReference;

public class MPZ extends Number implements Comparable<MPZ> {
    private Pointer mpzPointer;

    /**
     * A coefficient which is equal to zero.
     */
    public static final MPZ ZERO = new MPZ();

    static enum Reserve {
        RESERVE;
    }

    private static class MPZCleaner implements Runnable {
        private Pointer mpzPointer;

        MPZCleaner(Pointer mpz) {
            this.mpzPointer = mpz;
        }

        @Override
        public void run() {
            __gmpz_clear(mpzPointer);
        }
    }

    public MPZ() {
        mpzPointer = new Memory(MPZ_SIZE);
        __gmpz_init(mpzPointer);
        GMP.cleaner.register(this, new MPZCleaner(mpzPointer));
    }

    public MPZ(int n, Reserve dummy) {
        mpzPointer = new Memory(MPZ_SIZE);
        __gmpz_init2(mpzPointer, n);
        GMP.cleaner.register(this, new MPZCleaner(mpzPointer));
    }

    public MPZ(MPZ op) {
        mpzPointer = new Memory(MPZ_SIZE);
        __gmpz_init_set(mpzPointer, op.mpzPointer);
        GMP.cleaner.register(this, new MPZCleaner(mpzPointer));
    }

    public MPZ(long op) {
        mpzPointer = new Memory(MPZ_SIZE);
        __gmpz_init_set_si(mpzPointer, new NativeLong(op));
        GMP.cleaner.register(this, new MPZCleaner(mpzPointer));
    }

    public MPZ(double op) {
        mpzPointer = new Memory(MPZ_SIZE);
        __gmpz_init_set_d(mpzPointer, op);
        GMP.cleaner.register(this, new MPZCleaner(mpzPointer));
    }

    public MPZ(String str) {
        this(str, 10);
    }

    public MPZ(String str, int base) {
        mpzPointer = new Memory(MPZ_SIZE);
        int result = __gmpz_init_set_str(mpzPointer, str, base);
        if (result == -1)
            throw new IllegalArgumentException("Parameter s is not a valid number in base " + base);
        GMP.cleaner.register(this, new MPZCleaner(mpzPointer));
    }

    public Pointer getPointer() {
        return mpzPointer;
    }

    public MPZ realloc(int n) {
        __gmpz_realloc2(mpzPointer, n);
        return this;
    }

    public MPZ setValue(MPZ op) {
        __gmpz_set(mpzPointer, op.mpzPointer);
        return this;
    }

    public MPZ setValue(long op) {
        __gmpz_set_si(mpzPointer, new NativeLong(op));
        return this;
    }

    public MPZ setValue(double op) {
        __gmpz_set_d(mpzPointer, op);
        return this;
    }

    /*
    public MPZ setValue(MPQ op) {
        __gmpz_set_q(mpzPointer, op.mpqPointer);
        return this;
    }

    public MPZ setValue(MPF op) {
    __gmpz_set_f(mpzPointer, op.mpfPointer);
    return this;
    */

    public MPZ setValue(String s, int base) {
        int result = __gmpz_set_str(mpzPointer, s, base);
        if (result == -1)
            throw new IllegalArgumentException("Parameter s is not a valid number in base " + base);
        return this;
    }

    public MPZ setValue(String s) {
        return setValue(s, 10);
    }

    public MPZ swap(MPZ op) {
        __gmpz_swap(mpzPointer, op.mpzPointer);
        return this;
    }

    @Override
    public long longValue() {
        return __gmpz_get_si(mpzPointer).longValue();
    }

    @Override
    public int intValue() {
        return (int) longValue();
    }

    @Override
    public double doubleValue() {
        return __gmpz_get_d(mpzPointer);
    }

    @Override
    public float floatValue() {
        return (float) doubleValue();
    }

    public String toString(int base) {
        Pointer ps = __gmpz_get_str(null, base, mpzPointer);
        var s = ps.getString(0);
        //Native.free(ps);
        return s;
    }

    @Override
    public String toString() {
        return toString(10);
    }

    public double getNormalizedDouble() {
        var pl = new NativeLongByReference();
        return __gmpz_get_d_2exp(pl, mpzPointer);
        // decide how to return pl
    }

    public MPZ add(MPZ op) {
        __gmpz_add(mpzPointer, mpzPointer, op.mpzPointer);
        return this;
    }

    public MPZ add(long op) {
        if (op >= 0)
            __gmpz_add_ui(mpzPointer, mpzPointer, new NativeLong(op));
        else
            __gmpz_sub_ui(mpzPointer, mpzPointer, new NativeLong(-op));
        return this;
    }

    public MPZ sub(MPZ op) {
        __gmpz_sub(mpzPointer, mpzPointer, op.mpzPointer);
        return this;
    }

    public MPZ sub(long op) {
        if (op >= 0)
            __gmpz_sub_ui(mpzPointer, mpzPointer, new NativeLong(op));
        else
            __gmpz_add_ui(mpzPointer, mpzPointer, new NativeLong(-op));
        return this;
    }

    public MPZ mul(MPZ op) {
        __gmpz_mul(mpzPointer, mpzPointer, op.mpzPointer);
        return this;
    }

    public MPZ mul(long op) {
        __gmpz_mul_si(mpzPointer, mpzPointer, new NativeLong(op));
        return this;
    }

    public MPZ addmul(MPZ op1, MPZ op2) {
        __gmpz_addmul(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    public MPZ addmul(MPZ op1, long op2) {
        if (op2 >= 0)
            __gmpz_addmul_ui(mpzPointer, op1.mpzPointer, new NativeLong(op2));
        else
            __gmpz_submul_ui(mpzPointer, op1.mpzPointer, new NativeLong(op2));
        return this;
    }

    public MPZ submul(MPZ op1, MPZ op2) {
        __gmpz_submul(mpzPointer, op1.mpzPointer, op2.mpzPointer);
        return this;
    }

    public MPZ submul(MPZ op1, long op2) {
        if (op2 >= 0)
            __gmpz_submul_ui(mpzPointer, op1.mpzPointer, new NativeLong(op2));
        else
            __gmpz_addmul_ui(mpzPointer, op1.mpzPointer, new NativeLong(op2));
        return this;
    }

    public MPZ mul2exp(long op) {
        if (op >= 0)
            __gmpz_mul_2exp(mpzPointer, mpzPointer, new NativeLong(op));
        return this;
    }

    public MPZ neg() {
        __gmpz_neg(mpzPointer, mpzPointer);
        return this;
    }

    public MPZ abs() {
        __gmpz_abs(mpzPointer, mpzPointer);
        return this;
    }

    public int compareTo(MPZ op) {
        return __gmpz_cmp(mpzPointer, op.mpzPointer);
    }

    public int compareTo(double op) {
        return __gmpz_cmp_d(mpzPointer, op);
    }

    public int compareTo(long op) {
        return __gmpz_cmp_si(mpzPointer, new NativeLong(op));
    }

    public int compareAbsTo(MPZ op) {
        return __gmpz_cmpabs(mpzPointer, op.mpzPointer);
    }

    public int compareAbsTo(double op) {
        return __gmpz_cmp_d(mpzPointer, op);
    }

    public int compareAbsTo(long op) {
        return __gmpz_cmpabs_ui(mpzPointer, new NativeLong(Math.abs(op)));
    }

    public int sgn() {
        return __gmpz_sgn(mpzPointer);
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
}
