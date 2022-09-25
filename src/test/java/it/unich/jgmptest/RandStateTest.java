package it.unich.jgmptest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import it.unich.jgmp.MPZ;
import it.unich.jgmp.RandState;

public class RandStateTest {

    @Test
    void test_initialization() {
        assertDoesNotThrow(() -> new RandState());
        assertDoesNotThrow(() -> RandState.create());
        assertDoesNotThrow(() -> RandState.mt());
        assertDoesNotThrow(() -> RandState.lc2ExpSize(10));
        assertThrows(IllegalArgumentException.class, () -> RandState.lc2ExpSize(200));

        var rs1 = new RandState().randseedUi(100);
        var rs2 = new RandState(rs1);
        var rs3 = new RandState().set(rs1);
        var a = rs1.urandommUi(10000);
        var b = rs2.urandommUi(10000);
        var c = rs3.urandommUi(10000);
        assertEquals(a, b);
        assertEquals(a, c);
    }

    @Test
    void test_seeding() {
        var rs = new RandState();
        rs.randseed(new MPZ(10));
        var a = rs.urandommUi(10000);
        rs.randseedUi(10);
        var b = rs.urandommUi(10000);
        assertEquals(a, b);
    }

    @Test
    void test_miscellaneous() {
        var rs = new RandState();
        assertDoesNotThrow(() -> rs.urandombUi(3));
        assertDoesNotThrow(() -> rs.urandommUi(100));
    }
}
