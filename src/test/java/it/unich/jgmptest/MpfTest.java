package it.unich.jgmptest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;

import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import it.unich.jgmp.GMP;
import it.unich.jgmp.MPF;
import it.unich.jgmp.MPQ;
import it.unich.jgmp.MPZ;
import it.unich.jgmp.RandState;

public class MpfTest {

    public static final String MAX_ULONG = "18446744073709551615";

    public static final MPF zMaxUlong = new MPF(MAX_ULONG);

    @Test
    void testInit() {
        MPF.setDefaultPrec(10);
        assertTrue(MPF.getDefaultPrec() >= 10);
        assertEquals(new MPF(0), MPF.init());
        var a = MPF.init2(1000);
        assertEquals(new MPF(0), a);
        assertTrue(a.getPrec() >= 1000);
        a.setPrec(10);
        assertTrue(a.getPrec() >= 10);

        var b = new MPF(new MPQ(1, 10));
        MPF.setDefaultPrec(100);
        var c = new MPF(new MPQ(1, 10));
        assertTrue(c.compareTo(b) > 0);
        c.setPrec(10);
        // The following assertion fails. This seems to contradict the fact that
        // calling setPrec will truncate the number to the new precision.
        // assertEquals(f1, f2);
    }

    @Test
    void testAssignment() {
        var a = new MPF();
        assertEquals(new MPF(0), a);
        assertSame(a, a.set(new MPF(1.5)));
        assertEquals(new MPF(1.5), a);
        assertSame(a, a.setUi(1));
        assertEquals(new MPF(1), a);
        assertSame(a, a.setUi(-1));
        assertEquals(zMaxUlong, a);
        assertSame(a, a.set(1));
        assertEquals(new MPF(1), a);
        assertSame(a, a.set(-1));
        assertEquals(new MPF(-1), a);
        assertSame(a, a.set(5.25));
        assertEquals(new MPF(5.25), a);
        assertThrows(ArithmeticException.class, () -> a.set(Double.POSITIVE_INFINITY));
        assertThrows(ArithmeticException.class, () -> a.set(Double.NaN));
        assertEquals(new MPF(5.25), a);
        assertSame(a, a.set(new MPQ(21, 4)));
        assertEquals(new MPF(5.25), a);
        assertEquals(0, a.set("-1A" + GMP.getDecimalSeparator() + "1", 16));
        assertEquals(new MPF(-26.0625), a);
        assertEquals(-1, a.set("2", 63));
        assertEquals(new MPF(-26.0625), a);
        assertEquals(0, a.set("3" + GMP.getDecimalSeparator() + "5e2", 10));
        assertEquals(new MPF(350), a);
        assertEquals(0, a.set("1@100", 2));
        assertEquals(new MPF(16), a);
        assertEquals(0, a.set("1@4", -2));
        assertEquals(new MPF(16), a);
        var b = new MPF(-26);
        var c = new MPF(2).swap(b);
        assertEquals(new MPF(-26), c);
        assertEquals(new MPF(2), b);
    }

    @Test
    void testInitAndAssignment() {
        assertEquals(new MPF(1.5), MPF.initSet(new MPF(1.5)));
        assertEquals(new MPF(1), MPF.initSetUi(1));
        assertEquals(zMaxUlong, MPF.initSetUi(-1));
        assertEquals(new MPF(1), MPF.initSet(1));
        assertEquals(new MPF(-1), MPF.initSet(-1));
        assertEquals(new MPF(5.25), MPF.initSet(5.25));
        assertThrows(ArithmeticException.class, () -> MPF.initSet(Double.POSITIVE_INFINITY));
        assertEquals(new Pair<>(0, new MPF(-26.0625)), MPF.initSet("-1A" + GMP.getDecimalSeparator() + "1", 16));
        assertEquals(new Pair<>(-1, new MPF(0)), MPF.initSet("2", 63));
        assertEquals(new Pair<>(0, new MPF(350)), MPF.initSet("3" + GMP.getDecimalSeparator() + "5e2", 10));
        assertEquals(new Pair<>(0, new MPF(16)), MPF.initSet("1@100", 2));
        assertEquals(new Pair<>(0, new MPF(16)), MPF.initSet("1@4", -2));
    }

    @Test
    void testConversion() {
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
    void testArithmetic1() {
        assertEquals(new MPF(15), new MPF(8).add(new MPF(7)));
        assertEquals(new MPF(15), new MPF(8).addUi(7));
        assertEquals(new MPF(1), new MPF(8).sub(new MPF(7)));
        assertEquals(new MPF(1), new MPF(8).subUi(7));
        assertEquals(new MPF(-1), new MPF(8).uiSub(7));
        assertEquals(new MPF(56), new MPF(8).mul(new MPF(7)));
        assertEquals(new MPF(56), new MPF(8).mulUi(7));
        assertEquals(new MPF(0.5), new MPF(1).div(new MPF(2)));
        assertThrows(ArithmeticException.class, () -> new MPF(1).div(new MPF(0)));
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
    void testArithmetic2() {
        var a = new MPF(8);
        assertEquals(new MPF(15), a.addAssign(a, new MPF(7)));
        assertEquals(new MPF(16), a.addUiAssign(a, 1));
        assertEquals(new MPF(9), a.subAssign(a, new MPF(7)));
        assertEquals(new MPF(8), a.subUiAssign(a, 1));
        assertEquals(new MPF(-1), a.uiSubAssign(7, a));
        assertEquals(new MPF(-7), a.mulAssign(a, new MPF(7)));
        assertEquals(new MPF(-56), a.mulUiAssign(a, 8));
        assertEquals(new MPF(-7), a.divAssign(a, new MPF(8)));
        assertEquals(new MPF(-0.5), a.divUiAssign(a, 14));
        assertEquals(new MPF(-2), a.uiDivAssign(1, a));
        assertEquals(new Pair<>("806", 1l), a.sqrtAssign(new MPF(65)).getStr(10, 3));
        assertEquals(new Pair<>("806", 1l), a.sqrtUiAssign(65).getStr(10, 3));
        assertEquals(new MPF(0.25), a.powUiAssign(new MPF(0.5), 2));
        assertEquals(new MPF(-0.25), a.negAssign(a));
        assertEquals(new MPF(0.25), a.absAssign(a));
        assertEquals(new MPF(4), a.mul2ExpAssign(a, 4));
        assertEquals(new MPF(0.25), a.div2ExpAssign(a, 4));
    }

    @Test
    void testArithmetic3() {
        var a = new MPF(8);
        assertEquals(new MPF(15), a.addAssign(new MPF(7)));
        assertEquals(new MPF(16), a.addUiAssign(1));
        assertEquals(new MPF(9), a.subAssign(new MPF(7)));
        assertEquals(new MPF(8), a.subUiAssign(1));
        assertEquals(new MPF(-1), a.uiSubAssign(7));
        assertEquals(new MPF(-7), a.mulAssign(new MPF(7)));
        assertEquals(new MPF(-56), a.mulUiAssign(8));
        assertEquals(new MPF(-7), a.divAssign(new MPF(8)));
        assertEquals(new MPF(-0.5), a.divUiAssign(14));
        assertEquals(new MPF(-2), a.uiDivAssign(1));
        a.set(16);
        assertEquals(new MPF(4), a.sqrtAssign());
        assertEquals(new MPF(16), a.powUiAssign(2));
        a.set(0.25);
        assertEquals(new MPF(-0.25), a.negAssign());
        assertEquals(new MPF(0.25), a.absAssign());
        assertEquals(new MPF(4), a.mul2ExpAssign(a, 4));
        assertEquals(new MPF(0.25), a.div2ExpAssign(a, 4));
    }

    @Test
    void testComparisons() {
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
        assertEquals(new MPF(0.75), new MPF(0.5).reldiff(new MPF(0.125)));
        var c = new MPF();
        assertSame(c, c.reldiffAssign(new MPF(0.5), new MPF(0.125)));
        assertEquals(new MPF(0.75), c);
        c.set(0.5);
        assertSame(c, c.reldiffAssign(new MPF(0.125)));
        assertEquals(new MPF(0.75), c);
        assertEquals(1, new MPF(2).sgn());
        assertEquals(0, new MPF().sgn());
        assertEquals(-1, new MPF(-2).sgn());
    }

    @Test
    void testMiscellaneous() {
        var a = new MPF();
        assertSame(a, a.ceilAssign(new MPF(2.1)));
        assertEquals(new MPF(3), a);
        assertSame(a, a.ceilAssign(new MPF(-2.1)));
        assertEquals(new MPF(-2), a);
        assertSame(a, a.floorAssign(new MPF(2.6)));
        assertEquals(new MPF(2), a);
        assertSame(a, a.floorAssign(new MPF(-2.6)));
        assertEquals(new MPF(-3), a);
        assertSame(a, a.truncAssign(new MPF(2.6)));
        assertEquals(new MPF(2), a);
        assertSame(a, a.truncAssign(new MPF(-2.6)));
        assertEquals(new MPF(-2), a);
        a.set(2.1);
        assertSame(a, a.ceilAssign());
        assertEquals(new MPF(3), a);
        a.set(2.6);
        assertSame(a, a.floorAssign());
        assertEquals(new MPF(2), a);
        a.set(2.6);
        assertSame(a, a.truncAssign());
        assertEquals(new MPF(2), a);
        assertEquals(new MPF(3), new MPF(2.1).ceil());
        assertEquals(new MPF(-2), new MPF(-2.1).ceil());
        assertEquals(new MPF(2), new MPF(2.6).floor());
        assertEquals(new MPF(-3), new MPF(-2.6).floor());
        assertEquals(new MPF(2), new MPF(2.6).trunc());
        assertEquals(new MPF(-2), new MPF(-2.6).trunc());
        assertTrue(new MPF(2).isInteger());
        assertFalse(new MPF(2.3).isInteger());
        a.set("-213945" + GMP.getDecimalSeparator() + "5", 10);
        assertTrue(a.fitsSlong());
        assertFalse(a.fitsUlong());
        assertTrue(a.fitsSint());
        assertFalse(a.fitsUint());
        assertFalse(a.fitsSshort());
        assertFalse(a.fitsUshort());
        var rs = new RandState();
        assertSame(a, a.urandombAssign(rs, 2));
        assertTrue(a.cmp(0) >= 0);
        assertTrue(a.cmp(0.75) <= 0);
        assertSame(a, a.random2Assign(10, 1));
        assertTrue(a.cmp(0) >= 0);
        assertTrue(MPF.urandomb(rs, 2).cmp(0) >= 0);
        assertTrue(MPF.urandomb(rs, 2).cmp(0.75) <= 0);
        assertTrue(MPF.random2(10, 1).cmp(0) > 0);
    }

    @Test
    void testSerialization() throws IOException, ClassNotFoundException {
        var a = new MPF(1524132.25);
        var baos = new ByteArrayOutputStream();
        var oos = new ObjectOutputStream(baos);
        oos.writeObject(a);
        var arr = baos.toByteArray();
        oos.close();

        var ois = new ObjectInputStream(new ByteArrayInputStream(arr));
        var b = ois.readObject();
        assertEquals(a, b);
    }

    @Test
    void testConstructors() {
        assertEquals(new MPF(1.5), new MPF(new MPF(1.5)));
        assertEquals(new MPF(1), new MPF(1));
        assertEquals(new MPF(-1), new MPF(-1));
        assertEquals(new MPF(5.25), new MPF(5.25));
        assertThrows(ArithmeticException.class, () -> new MPF(Double.POSITIVE_INFINITY));
        assertEquals(new MPF(5.25), new MPF(new MPQ(21, 4)));
        assertEquals(new MPF(-26.0625), new MPF("-1A.1", 16));
        assertThrows(NumberFormatException.class, () -> new MPF("2", 63));
        if (!GMP.getDecimalSeparator().equals("."))
            assertThrows(NumberFormatException.class, () -> new MPF("0" + GMP.getDecimalSeparator() + "5"));
        assertEquals(new MPF(350), new MPF("3.5e2", 10));
        assertEquals(new MPF(16), new MPF("1@100", 2));
        assertEquals(new MPF(16), new MPF("1@4", -2));
    }

    @Test
    void testToString() {
        assertEquals("14.125", new MPF(14.125).toString());
        assertEquals("-14.125", new MPF(-14.125).toString());
        assertEquals("0.0078125", new MPF(0.0078125).toString());
        assertEquals("-0.0078125", new MPF(-0.0078125).toString());
        assertEquals("-0.0000001", new MPF(-0.0078125).toString(2));
        assertEquals("2450", new MPF(2450).toString());
        assertEquals("-2450", new MPF(-2450).toString());
        assertEquals("-0.0078", new MPF(-0.0078125).toString(10, 2));
        assertEquals("0", new MPF().toString());
    }

    @Test
    void testNumberClass() {
        var a = new MPF(-4.25);
        assertEquals(-4, a.intValue());
        assertEquals(-4l, a.longValue());
        assertEquals(-4.25, a.doubleValue());
        assertEquals(-4.25f, a.floatValue());
    }

    @Test
    void testBigDecimalConversion() {
        var str1 = "-2.45";
        var bd1 = new BigDecimal(str1);
        var f1 = new MPF(str1);
        var str2 = "2450";
        var bd2 = new BigDecimal(str2);
        var f2 = new MPF(str2);
        assertEquals(f1, new MPF(bd1));
        assertEquals(f2, new MPF(bd2));

        var f = new MPF();
        f.set(bd1);
        assertEquals(f1, f);
        f.set(bd2);
        assertEquals(f2, f);

        assertEquals(bd1, f1.getBigDecimal());
        assertTrue(bd2.compareTo(f2.getBigDecimal()) == 0);
    }
}
