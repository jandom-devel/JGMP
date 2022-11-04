
package it.unich.jgmptest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import it.unich.jgmp.nativelib.AllocFuncByReference;
import it.unich.jgmp.nativelib.FreeFuncByReference;
import it.unich.jgmp.nativelib.LibGmp;
import it.unich.jgmp.nativelib.ReallocFuncByReference;
import it.unich.jgmp.nativelib.SizeT;

public class LowLevelTest {

    private final String testString = "ABC123";

    @Test
    void testCustomAllocation() {
        var afp = new AllocFuncByReference();
        var rfp = new ReallocFuncByReference();
        var ffp = new FreeFuncByReference();

        LibGmp.mp_get_memory_functions(afp, rfp, ffp);
        var p = afp.value.invoke(new SizeT(100));
        p.setString(0, testString);
        var p1 = rfp.value.invoke(p, new SizeT(100), new SizeT(200));
        assertEquals(testString, p1.getString(0));
        ffp.value.invoke(p1, new SizeT(100));
    }
}
