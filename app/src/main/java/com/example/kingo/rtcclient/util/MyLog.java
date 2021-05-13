package com.example.kingo.rtcclient.util;

import android.util.Log;

/**
 * Created by kinggo on 2017/3/30.
 */

public class MyLog {
    public static void i(String msg, String content) {
        Log.i(msg, content);
    }

    public static void log(String msg) {
        Log.i(msg, "=====");
    }
}
