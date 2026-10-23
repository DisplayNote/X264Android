package com.github.bakaoh.x264;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.Test;

/**
 * libx264_jni.cpp finds these fields and constructors by name and JNI
 * signature at run time. A rename or type change compiles fine and only
 * fails on a device, so the names and types are pinned here.
 * X264Encoder itself is not touched: its static block loads the native
 * library.
 */
public class JniBindingContractTest {

    @Test
    public void paramsExposeTheFieldsInitEncoderReads() throws Exception {
        assertPublicField(X264Params.class, "width", int.class);
        assertPublicField(X264Params.class, "height", int.class);
        assertPublicField(X264Params.class, "bitrate", int.class);
        assertPublicField(X264Params.class, "fps", int.class);
        assertPublicField(X264Params.class, "gop", int.class);
        assertPublicField(X264Params.class, "preset", String.class);
        assertPublicField(X264Params.class, "profile", String.class);
    }

    // "(I[B[B)V" in initEncoder.
    @Test
    public void initResultHasTheConstructorInitEncoderCalls() throws Exception {
        Constructor<X264InitResult> constructor =
                X264InitResult.class.getConstructor(int.class, byte[].class, byte[].class);

        assertTrue(Modifier.isPublic(constructor.getModifiers()));
    }

    // "(I[BJZ)V" in encodeFrame.
    @Test
    public void encodeResultHasTheConstructorEncodeFrameCalls() throws Exception {
        Constructor<X264EncodeResult> constructor = X264EncodeResult.class.getConstructor(
                int.class, byte[].class, long.class, boolean.class);

        assertTrue(Modifier.isPublic(constructor.getModifiers()));
    }

    private static void assertPublicField(Class<?> owner, String name, Class<?> type)
            throws NoSuchFieldException {
        Field field = owner.getField(name);

        assertEquals(owner.getSimpleName() + "." + name, type, field.getType());
        assertTrue(Modifier.isPublic(field.getModifiers()));
        assertFalse(Modifier.isStatic(field.getModifiers()));
    }
}
