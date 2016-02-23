package org.dcache.oncrpc4j.rpcgen;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

/**
 * Created by Radai Rosenblatt
 */
public class BigConstsGenerationTest {
    @Test
    public void testBigCostsGeneration() throws Exception{
        int maxInt = 0xFFFFFFFF; //unsigned
        long maxLong = 0xFFFFFFFFFFFFFFFFL; //unsigned
        Assert.assertEquals(Calculator.PLAIN_ZERO, 0);
        Assert.assertEquals(Calculator.HEX_ZERO, 0);
        //small is within int range
        Assert.assertTrue(Calculator.SMALL_CONST <= Integer.MAX_VALUE);
        Assert.assertTrue(Calculator.SMALL_CONST >= Integer.MIN_VALUE);
        //large is a long above max int
        Assert.assertTrue(Calculator.LARGE_CONST >= Integer.MAX_VALUE);
        Assert.assertTrue(Calculator.LARGE_CONST <= Long.MAX_VALUE);
        //huge us a bigint beyond max long
        Assert.assertTrue(Calculator.HUGE_CONST.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0);
        //the code below is jdk8's compareUnsigned (back ported because project is jdk7)
        //noinspection NumericOverflow
        Assert.assertTrue(Integer.compare(maxInt + Integer.MIN_VALUE, Calculator.UNSIGNED_INT_OCT_CONST + Integer.MIN_VALUE) == 0);
        //noinspection NumericOverflow
        Assert.assertTrue(Integer.compare(maxInt + Integer.MIN_VALUE, Calculator.UNSIGNED_INT_HEX_CONST + Integer.MIN_VALUE) == 0);
        //noinspection NumericOverflow
        Assert.assertTrue(Integer.compare(maxInt + Integer.MIN_VALUE, ((int) Calculator.UNSIGNED_INT_DEC_CONST) + Integer.MIN_VALUE) == 0);
        //noinspection NumericOverflow
        Assert.assertTrue(Long.compare(maxLong + Long.MIN_VALUE, Calculator.UNSIGNED_LONG_OCT_CONST + Long.MIN_VALUE) == 0);
        //noinspection NumericOverflow
        Assert.assertTrue(Long.compare(maxLong + Long.MIN_VALUE, Calculator.UNSIGNED_LONG_HEX_CONST + Long.MIN_VALUE) == 0);
        Assert.assertTrue(Long.compare(maxLong + Long.MIN_VALUE, Calculator.UNSIGNED_LONG_DEC_CONST.longValue() + Long.MIN_VALUE) == 0);
    }
}
