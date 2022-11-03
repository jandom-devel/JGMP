package it.unich.jgmptest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import it.unich.jgmp.GMP;
import it.unich.jgmp.MPF;
import it.unich.jgmp.MPQ;
import it.unich.jgmp.MPZ;
import it.unich.jgmp.RandState;

public class MPFTest {

    public static final String MAX_ULONG = "18446744073709551615";

    public static final MPF zMaxUlong = new MPF(MAX_ULONG);

    @Test
    void test_init() {
        MPF.setDefaultPrec(10);
        assertTrue(MPF.getDefaultPrec() >= 10);
        assertEquals(new MPF(0), MPF.init());
        var f = MPF.init2(1000);
        assertEquals(new MPF(0), f);
        assertTrue(f.getPrec() >= 1000);
        f.setPrec(10);
        assertTrue(f.getPrec() >= 10);

        var f1 = new MPF(new MPQ(1, 10));
        MPF.setDefaultPrec(100);
        var f2 = new MPF(new MPQ(1, 10));
        assertTrue(f2.compareTo(f1) > 0);
        f2.setPrec(10);
        // The following assertion fails. This seems to contradict the fact that
        // calling setPrec will truncate the number to the new precision.
        // assertEquals(f1, f2);
    }

    @Test
    void test_assignment() {
        var z = new MPF();
        assertEquals(new MPF(0), z);
        assertEquals(new MPF(1.5), z.set(new MPF(1.5)));
        assertEquals(new MPF(1), z.setUi(1));
        assertEquals(zMaxUlong, z.setUi(-1));
        assertEquals(new MPF(1), z.set(1));
        assertEquals(new MPF(-1), z.set(-1));
        assertEquals(new MPF(5.25), z.set(5.25));
        assertThrows(IllegalArgumentException.class, () -> z.set(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> z.set(Double.NaN));
        assertEquals(new MPF(5.25), z.set(new MPQ(21, 4)));
        assertEquals(0, z.set("-1A" + GMP.getDecimalSeparator() + "1", 16));
        assertEquals(new MPF(-26.0625), z);
        assertEquals(-1, z.set("2", 63));
        assertEquals(new MPF(-26.0625), z);
        assertEquals(0, z.set("3" + GMP.getDecimalSeparator() + "5e2", 10));
        assertEquals(new MPF(350), z);
        assertEquals(0, z.set("1@100", 2));
        assertEquals(new MPF(16), z);
        assertEquals(0, z.set("1@4", -2));
        assertEquals(new MPF(16), z);
        var z2 = new MPF(-26);
        var z3 = new MPF(2).swap(z2);
        assertEquals(new MPF(-26), z3);
        assertEquals(new MPF(2), z2);
    }

    @Test
    void test_initandassignment() {
        assertEquals(new MPF(1.5), MPF.initSet(new MPF(1.5)));
        assertEquals(new MPF(1), MPF.initSetUi(1));
        assertEquals(zMaxUlong, MPF.initSetUi(-1));
        assertEquals(new MPF(1), MPF.initSet(1));
        assertEquals(new MPF(-1), MPF.initSet(-1));
        assertEquals(new MPF(5.25), MPF.initSet(5.25));
        assertThrows(IllegalArgumentException.class, () -> MPF.initSet(Double.POSITIVE_INFINITY));
        assertEquals(new Pair<>(0, new MPF(-26.0625)), MPF.initSet("-1A" + GMP.getDecimalSeparator() + "1", 16));
        assertEquals(new Pair<>(-1, new MPF(0)), MPF.initSet("2", 63));
        assertEquals(new Pair<>(0, new MPF(350)), MPF.initSet("3" + GMP.getDecimalSeparator() + "5e2", 10));
        assertEquals(new Pair<>(0, new MPF(16)), MPF.initSet("1@100", 2));
        assertEquals(new Pair<>(0, new MPF(16)), MPF.initSet("1@4", -2));
    }

    @Test
    void test_conversion() {
        assertEquals(-4.25, new MPF(-4.25).getD());
        assertEquals(new Pair<>(-0.53125, 3l), new MPF(-4.25).getD2Exp());
        assertEquals(4l, new MPF(-4.25).getUi());
        assertEquals(-4l, new MPF(-4.25).getSi());
        assertEquals(new Pair<>("12525", 3l), new MPF(125.25).getStr(10, 0));
        assertEquals(new Pair<>("13", 3l), new MPF(125.25).getStr(10, 2));
        assertEquals(new Pair<>("10001", 3l), new MPF(4.25).getStr(2, 0));
        assertEquals(null, new MPF(125.25).getStr(63, 0));
    }

    @Test
    void test_arithmetic1() {
        assertEquals(new MPF(15), new MPF(8).add(new MPF(7)));
        assertEquals(new MPF(15), new MPF(8).addUi(7));
        assertEquals(new MPF(1), new MPF(8).sub(new MPF(7)));
        assertEquals(new MPF(1), new MPF(8).subUi(7));
        assertEquals(new MPF(-1), new MPF(8).uiSub(7));
        assertEquals(new MPF(56), new MPF(8).mul(new MPF(7)));
        assertEquals(new MPF(56), new MPF(8).mulUi(7));
        assertEquals(new MPF(0.5), new MPF(1).div(new MPF(2)));
        assertEquals(new MPF(0.5), new MPF(2).uiDiv(1));
        assertEquals(new MPF(0.5), new MPF(1).divUi(2));
        assertEquals(new Pair<>("806", 1l), new MPF(65).sqrt().getStr(10, 3));
        assertEquals(new Pair<>("806", 1l), MPF.sqrtUi(65).getStr(10, 3));
        assertEquals(new MPF(0.25), new MPF(0.5).powUi(2));
        assertEquals(new MPF(-5), new MPF(5).neg());
        assertEquals(new MPF(5), new MPF(-5).abs());
        assertEquals(new MPF(48), new MPF(3).mul2Exp(4));
        assertEquals(new MPF(0.25), new MPF(1).div2Exp(2));
    }

    @Test
    void test_arithmetic2() {
        var f = new MPF(8);
        assertEquals(new MPF(15), f.addAssign(f, new MPF(7)));
        assertEquals(new MPF(16), f.addUiAssign(f, 1));
        assertEquals(new MPF(9), f.subAssign(f, new MPF(7)));
        assertEquals(new MPF(8), f.subUiAssign(f, 1));
        assertEquals(new MPF(-1), f.uiSubAssign(7, f));
        assertEquals(new MPF(-7), f.mulAssign(f, new MPF(7)));
        assertEquals(new MPF(-56), f.mulUiAssign(f, 8));
        assertEquals(new MPF(-7), f.divAssign(f, new MPF(8)));
        assertEquals(new MPF(-0.5), f.divUiAssign(f, 14));
        assertEquals(new MPF(-2), f.uiDivAssign(1, f));
        assertEquals(new Pair<>("806", 1l), f.sqrtAssign(new MPF(65)).getStr(10, 3));
        assertEquals(new Pair<>("806", 1l), f.sqrtUiAssign(65).getStr(10, 3));
        assertEquals(new MPF(0.25), f.powUiAssign(new MPF(0.5), 2));
        assertEquals(new MPF(-0.25), f.negAssign(f));
        assertEquals(new MPF(0.25), f.absAssign(f));
        assertEquals(new MPF(4), f.mul2ExpAssign(f, 4));
        assertEquals(new MPF(0.25), f.div2ExpAssign(f, 4));
    }

    @Test
    void test_comparison() {
        var a = new MPF(10);
        var b = new MPF(2);
        assertTrue(a.compareTo(b) > 0);
        assertTrue(b.compareTo(a) < 0);
        assertTrue(b.compareTo(b) == 0);
        assertTrue(a.cmp(b) > 0);
        assertTrue(b.cmp(a) < 0);
        assertTrue(b.cmp(b) == 0);
        assertTrue(a.cmp(new MPZ(2)) > 0);
        assertTrue(b.cmp(new MPZ(10)) < 0);
        assertTrue(b.cmp(new MPZ(2)) == 0);
        assertTrue(a.cmp(2.0) > 0);
        assertTrue(b.cmp(10.0) < 0);
        assertTrue(b.cmp(2.0) == 0);
        assertTrue(a.cmp(2) > 0);
        assertTrue(b.cmp(10) < 0);
        assertTrue(b.cmp(2) == 0);
        assertTrue(a.cmpUi(2) > 0);
        assertTrue(b.cmpUi(10) < 0);
        assertTrue(b.cmpUi(2) == 0);
        assertTrue(b.cmpUi(-1) < 0);
        var f = new MPF();
        assertEquals(new MPF(0.75), f.reldiffAssign(new MPF(0.5), new MPF(0.125)));
        assertEquals(new MPF(0.75), new MPF(0.5).reldiff(new MPF(0.125)));
        assertEquals(1, new MPF(2).sgn());
        assertEquals(0, new MPF().sgn());
        assertEquals(-1, new MPF(-2).sgn());
    }

    @Test
    void test_miscellaneous() {
        var f = new MPF();
        assertEquals(new MPF(3), f.ceilAssign(new MPF(2.1)));
        assertEquals(new MPF(-2), f.ceilAssign(new MPF(-2.1)));
        assertEquals(new MPF(2), f.floorAssign(new MPF(2.6)));
        assertEquals(new MPF(-3), f.floorAssign(new MPF(-2.6)));
        assertEquals(new MPF(2), f.truncAssign(new MPF(2.6)));
        assertEquals(new MPF(-2), f.truncAssign(new MPF(-2.6)));
        assertEquals(new MPF(3), new MPF(2.1).ceil());
        assertEquals(new MPF(-2), new MPF(-2.1).ceil());
        assertEquals(new MPF(2), new MPF(2.6).floor());
        assertEquals(new MPF(-3), new MPF(-2.6).floor());
        assertEquals(new MPF(2), new MPF(2.6).trunc());
        assertEquals(new MPF(-2), new MPF(-2.6).trunc());
        assertTrue(new MPF(2).isInteger());
        assertFalse(new MPF(2.3).isInteger());
        f = new MPF("-213945.5");
        assertTrue(f.fitsSlong());
        assertFalse(f.fitsUlong());
        assertTrue(f.fitsSint());
        assertFalse(f.fitsUint());
        assertFalse(f.fitsSshort());
        assertFalse(f.fitsUshort());
        var rs = new RandState();
        f.urandombAssign(rs, 2);
        assertTrue(f.cmp(0) >= 0);
        assertTrue(f.cmp(0.75) <= 0);
        f.random2Assign(10, 1);
        assertTrue(f.cmp(0) >= 0);
        assertTrue(MPF.urandomb(rs, 2).cmp(0) >= 0);
        assertTrue(MPF.urandomb(rs, 2).cmp(0.75) <= 0);
        assertTrue(MPF.random2(10, 1).cmp(0) > 0);
    }

    @Test
    void test_serialize() throws IOException, ClassNotFoundException {
        var n = new MPF(1524132.25);
        var baos = new ByteArrayOutputStream();
        var oos = new ObjectOutputStream(baos);
        oos.writeObject(n);
        var arr = baos.toByteArray();
        oos.close();

        var ois = new ObjectInputStream(new ByteArrayInputStream(arr));
        var n2 = ois.readObject();
        assertEquals(n, n2);
    }

    @Test
    void test_constructors() {
        assertEquals(new MPF(1.5), new MPF(new MPF(1.5)));
        assertEquals(new MPF(1), new MPF(1));
        assertEquals(new MPF(-1), new MPF(-1));
        assertEquals(new MPF(5.25), new MPF(5.25));
        assertThrows(IllegalArgumentException.class, () -> new MPF(Double.POSITIVE_INFINITY));
        assertEquals(new MPF(5.25), new MPF(new MPQ(21, 4)));
        assertEquals(new MPF(-26.0625), new MPF("-1A.1", 16));
        assertThrows(IllegalArgumentException.class, () -> new MPF("2", 63));
        if (!GMP.getDecimalSeparator().equals("."))
            assertThrows(IllegalArgumentException.class, () -> new MPF("0" + GMP.getDecimalSeparator() + "5"));
        assertEquals(new MPF(350), new MPF("3.5e2", 10));
        assertEquals(new MPF(16), new MPF("1@100", 2));
        assertEquals(new MPF(16), new MPF("1@4", -2));
    }

    @Test
    void test_toString() {
        assertEquals("14.125", new MPF(14.125).toString());
        assertEquals("-14.125", new MPF(-14.125).toString());
        assertEquals("0.0078125", new MPF(0.0078125).toString());
        assertEquals("-0.0078125", new MPF(-0.0078125).toString());
        assertEquals("-0.0000001", new MPF(-0.0078125).toString(2));
        assertEquals("-0.0078", new MPF(-0.0078125).toString(10, 2));
        assertEquals("0", new MPF().toString());
    }

    @Test
    void test_number_class() {
        var f = new MPF(-4.25);
        assertEquals(-4, f.intValue());
        assertEquals(-4l, f.longValue());
        assertEquals(-4.25, f.doubleValue());
        assertEquals(-4.25f, f.floatValue());
    }

}
