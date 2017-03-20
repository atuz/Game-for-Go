/*
 * This file is part of Go game.
 * Copyright (C) 2012   Ping Zheng [emmanuel *at* lr-studios.net]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.zhengping.gogame;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class GoGameApplication extends Application {
    private static final String TAG = "Application";
    static public File cacheDir=new File(android.os.Environment.getExternalStorageDirectory(),"GoGamesPachi");
    public static File pachiFile = null;
    public static File dataFile = null;
    public static File dir = null;
    public static boolean PERMISSIONS_EXTERNAL_STORAGE  = false;
    private static GoGameApplication app;
    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        pachiFile = getEngineFile();
        dataFile = getDataFile();
        dir = getDir("engines", Context.MODE_PRIVATE);
        copyJoseki();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Log.e(TAG, "==========exit with uncaught exception =====");

                ex.printStackTrace();
                System.exit(-1);

            }
        });
    }
    public static Context getAppContext() {
        return app.getApplicationContext();
    }
    protected File getEngineFile() {
        File dir = getDir("engines", Context.MODE_PRIVATE);
        File file = new File(dir, "pachi");
        if (!file.exists()) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(file), 4096);
                inputStream = new BufferedInputStream(getResources().openRawResource(R.raw.pachi), 4096);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                new ProcessBuilder("chmod", "744", file.getAbsolutePath()).start().waitFor();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if (inputStream != null){
                    try {
                        inputStream.close();
                    }
                    catch (Exception ignored) {
                    }
                }
            }
            if (outputStream!= null){ try {
                outputStream.close();
            }
            catch (Exception ignored) {
            }
            }
        }

        return file;
    }
    protected File getDataFile() {
        File dir = getDir("engines", Context.MODE_PRIVATE);
        File file = new File(dir, "book.dat");
        if (!file.exists()) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(file), 4096);
                inputStream = new BufferedInputStream(getResources().openRawResource(R.raw.book), 4096);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if (inputStream != null){
                    try {
                        inputStream.close();
                    }
                    catch (Exception ignored) {
                    }
                }
            }
            if (outputStream!= null){ try {
                outputStream.close();
            }
            catch (Exception ignored) {
            }
            }
        }

        return file;
    }

    protected File copyJoseki() {
        File dir = getDir("engines", Context.MODE_PRIVATE);
        File file = new File(dir, "joseki19.pdict");
        if (!file.exists()) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(file), 4096);
                inputStream = new BufferedInputStream(getResources().openRawResource(R.raw.joseki19), 4096);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if (inputStream != null){
                    try {
                        inputStream.close();
                    }
                    catch (Exception ignored) {
                    }
                }
            }
            if (outputStream!= null){ try {
                outputStream.close();
            }
            catch (Exception ignored) {
            }
            }
        }

        return file;
    }

}
