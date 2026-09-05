package com.app.chao.chaoapp.utils;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class StringUtilsTest {
    @Test public void absentThrowableMessageUsesThePagesDefaultError() {
        assertEquals("", StringUtils.getErrorMsg(new ClassCastException().getMessage()));
        assertEquals("", StringUtils.getErrorMsg(""));
        assertEquals("", StringUtils.getErrorMsg("internal transport details"));
    }

    @Test public void explicitBusinessMessageStillReachesThePage() {
        assertEquals("服务器返回错误", StringUtils.getErrorMsg("*服务器返回错误"));
    }
}
