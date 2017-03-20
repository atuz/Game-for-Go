package com.zhengping.gogame.Util;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Point;

import java.io.IOException;
import java.io.RandomAccessFile;

import static com.zhengping.gogame.GoGameApplication.getAppContext;

/**
 * Created by user on 2017-03-05.
 */

public class Util {
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
