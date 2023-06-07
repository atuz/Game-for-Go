package com.zhengping.gogame.Object;

import androidx.appcompat.app.AlertDialog;

/**
 * Created by user on 2017-09-28.
 */

public class GnuGoGame{
    private static final String GNUGO_SO_LIBRARY_NAME = "gnuGo-3.8";
    private static final String GNUGO_THREAD_NAME = "gnuGo";
    private static final int GNUGO_MEMORY_SIZE = 128;
    static boolean _isRunning = false;
    native static
    void initGTP (float pMemory);
    native static
    String playGTP (String pInput);
    native static
    void setRules (int chineseRules);

    public static void initGnugoEngine(){
        if (!_isRunning)
            initGTP (GNUGO_MEMORY_SIZE);
        _isRunning = true;
    }

    static String sendGtpCommand(String command) {
        String reply = playGTP (command);

        if (reply != null)
        {
            reply = reply.replaceFirst ("= ", "").replace ("\n\n", "");
        }

        if (reply == null || reply.length () == 0 || reply.charAt (0) == '?')
        {
            return "";
        }
        return reply;
    }
    static
    {
        System.loadLibrary (GNUGO_SO_LIBRARY_NAME);

    }



}
