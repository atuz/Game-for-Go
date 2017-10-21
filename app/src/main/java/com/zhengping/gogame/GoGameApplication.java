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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.StringTokenizer;

public class GoGameApplication extends Application {
    private static final String TAG = "Application";
    private static final String Host = "host";
    private static final String Port = "port";

    static public File cacheDir=new File(android.os.Environment.getExternalStorageDirectory(),"GoGamesPachi");
    public static File pachiFile = null;
    public static File dataFile = null;
    public static File dir = null;
    public static String host = "";
    public static int port;
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
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        host = settings.getString(Host,"192.168.1.17");
        port = settings.getInt(Port,6666);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef1 = database.getReferenceFromUrl("https://go-game-be8dc.firebaseio.com/"); //Getting root reference
        DatabaseReference myRef = myRef1.child(Host);
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                try {
                    Map<String, String> value = (Map<String, String>) dataSnapshot.getValue();
                    if (value.get("author").equals("zhengping")){
                        host = value.get(Host);
                        port = Integer.parseInt(value.get(Port ));
                        SharedPreferences settings = PreferenceManager
                                .getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(Host,host);
                        editor.putInt(Port,port);
                        editor.apply();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
            }
        });


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
