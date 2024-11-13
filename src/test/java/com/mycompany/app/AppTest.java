package com.mycompany.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.mycompany.util.RandomStringUtils;

/**
 * Unit test for simple App.
 */
public class AppTest {

    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }

    @Test
    public void testList1() {
        assertEquals(null, List.fromArray(null));
        assertEquals(null, List.fromArray(new int[] {}));
        assertEquals("0,", List.toString(List.fromArray(new int[] { 0 })));
        assertEquals("1,2,", List.toString(List.fromArray(new int[] { 1, 2 })));
        assertEquals("1,2,3,", List.toString(List.fromArray(new int[] { 1, 2, 3 })));
    }

    @Test
    public void testList2() {
        ListNode list = List.fromArray(new int[] { 1, 2, 3, 4, 5 });
        assertEquals("1,2,3,4,5,", List.toString(List.restN(-1, list)));
        assertEquals("1,2,3,4,5,", List.toString(List.restN(0, list)));
        assertEquals("2,3,4,5,", List.toString(List.restN(1, list)));
        assertEquals("3,4,5,", List.toString(List.restN(2, list)));
        assertEquals("4,5,", List.toString(List.restN(3, list)));
        assertEquals("5,", List.toString(List.restN(4, list)));
        assertEquals(null, List.restN(5, list));
        assertEquals(null, List.restN(6, list));
    }

    @Test
    public void testReverseBetween() {
        assertEquals("1,4,3,2,5,", List.toString(
                List.reverseBetween(List.fromArray(new int[] { 1, 2, 3, 4, 5 }), 2, 4)));
        assertEquals("5,", List.toString(
                List.reverseBetween(List.fromArray(new int[] { 5 }), 1, 1)));
    }

    @Test
    public void testReverse() {
        assertEquals("5,4,3,2,1,", List.toString(
                List.reverse(List.fromArray(new int[] { 1, 2, 3, 4, 5 }))));
        assertEquals("3,2,1,", List.toString(
                List.reverse(List.fromArray(new int[] { 1, 2, 3 }))));
        assertEquals("5,", List.toString(
                List.reverse(List.fromArray(new int[] { 5 }))));
    }

    @Test
    public void testRandomString() {
        String str = RandomStringUtils.random(10);
        assertEquals(10, str.length());
    }
}
