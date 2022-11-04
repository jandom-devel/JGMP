
package it.unich.jgmptest;

import static it.unich.jgmp.nativelib.LibGmp.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.jna.NativeLong;

import org.junit.jupiter.api.Test;

import it.unich.jgmp.nativelib.*;


public class LowLevelTest {

    @Test
    void testCustomAllocation() {
        var testString = "ABC123";

        var afp = new AllocFuncByReference();
        var rfp = new ReallocFuncByReference();
        var ffp = new FreeFuncByReference();

        mp_get_memory_functions(afp, rfp, ffp);
        var p = afp.value.invoke(new SizeT(100));
        p.setString(0, testString);
        var p1 = rfp.value.invoke(p, new SizeT(100), new SizeT(200));
        assertEquals(testString, p1.getString(0));
        ffp.value.invoke(p1, new SizeT(100));
    }

    @Test
    void testNumDemRef() {
        var q = new MpqT();
        mpq_init(q);
        mpq_set_si(q, new NativeLong(2), new NativeLong(3));
        var num = mpq_numref(q);
        var den = mpq_denref(q);
        assertEquals(2, mpz_get_si(num).intValue());
        assertEquals(3, mpz_get_si(den).intValue());
        mpq_clear(q);
    }
}
