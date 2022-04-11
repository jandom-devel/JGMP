package it.unich.jgmp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import it.unich.jgmp.MPZ.Reserve;

public class MPZTest {

    @Test
    void test1() {
        var a = new MPZ();
        var b = new MPZ(10, Reserve.RESERVE);
        assertEquals(a, a);
        assertEquals(a, b);
        a.setValue(3);
        assertNotEquals(a, b);
        a.add(a).add(5).mul(new MPZ("-2"));
        assertEquals(a, new MPZ("-16", 16));
    }
}
