package com.github.bakaoh.x264;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

public class X264ParamsTest {

    @Test
    public void defaultsDescribeA720p24fpsBaselineUltrafastStream() {
        X264Params params = new X264Params();

        assertEquals(1280, params.width);
        assertEquals(720, params.height);
        assertEquals(24, params.fps);
        assertEquals("baseline", params.profile);
        assertEquals("ultrafast", params.preset);
    }

    @Test
    public void defaultGopIsTwoSecondsOfFrames() {
        X264Params params = new X264Params();

        assertEquals(48, params.gop);
        assertEquals(2 * params.fps, params.gop);
    }

    // The JNI layer divides bitrate by 1000 to get x264's kbps (rc.i_bitrate),
    // so this default reaches x264 as 0 kbps. Pinned as-is; flagged in
    // docs/runbooks/testing.md. Callers set a real bps value.
    @Test
    public void defaultBitrateIsFiveHundredBitsPerSecondWhichTheJniLayerTruncatesToZeroKbps() {
        X264Params params = new X264Params();

        assertEquals(500, params.bitrate);
        assertEquals(0, params.bitrate / 1000);
    }

    // encodeFrame passes the colour format straight to x264 as img.i_csp, so
    // these must equal x264.h's X264_CSP_* values.
    @Test
    public void colourSpaceConstantsMatchX264CspValues() {
        assertEquals(0x0002, X264Params.CSP_I420);
        assertEquals(0x0003, X264Params.CSP_YV12);
        assertEquals(0x0004, X264Params.CSP_NV12);
        assertEquals(0x0005, X264Params.CSP_NV21);
    }

    @Test
    public void eachInstanceHasItsOwnSettings() {
        X264Params first = new X264Params();
        X264Params second = new X264Params();

        first.width = 1920;
        first.height = 1080;
        first.bitrate = 4_000_000;
        first.profile = "high";

        assertNotSame(first, second);
        assertEquals(1280, second.width);
        assertEquals(720, second.height);
        assertEquals(500, second.bitrate);
        assertEquals("baseline", second.profile);
    }
}
