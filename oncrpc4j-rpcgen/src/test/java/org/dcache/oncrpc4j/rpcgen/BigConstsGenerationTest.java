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
        //small is within int range
        Assert.assertTrue(Calculator.SMALL_CONST <= Integer.MAX_VALUE);
        Assert.assertTrue(Calculator.SMALL_CONST >= Integer.MIN_VALUE);
        //large is a long above max int
        Assert.assertTrue(Calculator.LARGE_CONST >= Integer.MAX_VALUE);
        Assert.assertTrue(Calculator.LARGE_CONST <= Long.MAX_VALUE);
        //huge us a bigint beyond max long
        Assert.assertTrue(Calculator.HUGE_CONST.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0);
    }
}
