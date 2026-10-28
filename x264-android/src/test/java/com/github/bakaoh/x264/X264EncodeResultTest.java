package com.github.bakaoh.x264;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class X264EncodeResultTest {

    @Test
    public void encodedKeyFrameKeepsItsDataPtsAndKeyFlag() {
        byte[] data = {0x00, 0x00, 0x00, 0x01, 0x65};

        X264EncodeResult result = new X264EncodeResult(0, data, 40_000L, true);

        assertEquals(0, result.err);
        assertSame(data, result.data);
        assertEquals(40_000L, result.pts);
        assertTrue(result.isKey);
    }

    @Test
    public void ptsKeepsTheFullLongRange() {
        X264EncodeResult result = new X264EncodeResult(0, new byte[0], Long.MAX_VALUE, false);

        assertEquals(Long.MAX_VALUE, result.pts);
        assertFalse(result.isKey);
    }

    // The JNI layer returns X264A_ERR_NOT_SUPPORT_CSP (-4) or
    // X264A_ERR_ENCODE_FRAME (-5) with no data, pts 0 and isKey false.
    @Test
    public void failureCarriesTheErrorCodeAndNoData() {
        X264EncodeResult result = new X264EncodeResult(-4, null, 0L, false);

        assertEquals(-4, result.err);
        assertNull(result.data);
        assertEquals(0L, result.pts);
        assertFalse(result.isKey);
    }
}
