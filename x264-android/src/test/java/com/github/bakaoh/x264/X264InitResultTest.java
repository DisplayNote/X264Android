package com.github.bakaoh.x264;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class X264InitResultTest {

    @Test
    public void successKeepsTheSpsAndPpsArraysWithoutCopying() {
        byte[] sps = {0x67, 0x42, 0x00, 0x1f};
        byte[] pps = {0x68, (byte) 0xce, 0x3c, (byte) 0x80};

        X264InitResult result = new X264InitResult(0, sps, pps);

        assertEquals(0, result.err);
        assertSame(sps, result.sps);
        assertSame(pps, result.pps);
    }

    // The JNI layer returns X264A_ERR_APPLY_PROFILE (-2) or
    // X264A_ERR_OPEN_ENCODER (-3) with null headers.
    @Test
    public void failureCarriesTheErrorCodeAndNoHeaders() {
        X264InitResult result = new X264InitResult(-3, null, null);

        assertEquals(-3, result.err);
        assertNull(result.sps);
        assertNull(result.pps);
    }

    @Test
    public void fieldsStayWritable() {
        X264InitResult result = new X264InitResult(-2, null, null);

        result.err = 0;
        result.sps = new byte[] {1};

        assertEquals(0, result.err);
        assertEquals(1, result.sps.length);
    }
}
