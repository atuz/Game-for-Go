package com.zhengping.gogame.Util;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Point;

import java.io.IOException;
import java.io.RandomAccessFile;

import static com.zhengping.gogame.Board.ProblemView.WHITE;
import static com.zhengping.gogame.GoGameApplication.getAppContext;

/**
 * Created by user on 2017-03-05.
 */

public class Util {
    static public final String _BOARD_LETTERS = "ABCDEFGHJKLMNOPQRSTUVWXYZ"; // no 'I'
    private static final String _BLACK_NAME = "black";
    private static final String _WHITE_NAME = "white";

    static public boolean isEmpty(String s){
        return (s==null || s.equals(""));
    }
    static public String pathLastString(String s)
    {
        if (s != null && !s.equals("")) {
            String[] separated = s.split("/");
            return separated[separated.length - 1];
        }
        return  null;


    }
    static public String NtoSpace(String s){
        if (Util.isEmpty(s)){
            return "";
        }
        return s;
    }

    static public String getColorString(int color) {
        return (color == WHITE ? _WHITE_NAME : _BLACK_NAME);
    }
    static public  String _point2str(int x, int y,int boardSize) {
        if (x == -1 && y == -1)
            return "pass";
        else if (x == -3 && y == -3)
            return "resign";
        else
            return String.valueOf(_BOARD_LETTERS.charAt(x)) + (boardSize - y);
    }
    static public Point _str2point(String coords,int boardSize) {
        if (coords.equalsIgnoreCase("pass"))
            return new Point(-1, -1);
        else if (coords.equalsIgnoreCase("resign"))
            return new Point(-3, -3);
        else {
            try {
                return new Point(
                        _BOARD_LETTERS.indexOf(coords.charAt(0)),
                        boardSize - Integer.parseInt(coords.substring(1).trim()));
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    static public  long getTotalRam() {
        Context context = getAppContext();
        if (android.os.Build.VERSION.SDK_INT >= 16) {

            ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            actManager.getMemoryInfo(memInfo);
            return memInfo.totalMem;
        }
        else {
            RandomAccessFile reader = null;
            try {
                reader = new RandomAccessFile("/proc/meminfo", "r");
                String line = reader.readLine();
                String[] arr = line.split("\\s+");
                return Integer.parseInt(arr[1]) * 1024;
            }
            catch (IOException e) {
                return -1;
            }
            finally {
                if (reader !=null) {
                    try{
                        reader.close();
                    }catch (Exception ignored){

                    }

                }

            }
        }
    }

}
