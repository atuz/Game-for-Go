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
package com.zhengping.gogame.Object;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.zhengping.gogame.Board.BoardView;
import com.zhengping.gogame.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Properties;

import static com.zhengping.gogame.GoGameApplication.PERMISSIONS_EXTERNAL_STORAGE;
import static com.zhengping.gogame.GoGameApplication.cacheDir;
import static com.zhengping.gogame.GoGameApplication.dataFile;
import static com.zhengping.gogame.GoGameApplication.dir;
import static com.zhengping.gogame.GoGameApplication.pachiFile;
import static com.zhengping.gogame.MainActivity.COM_PACHI;
import static com.zhengping.gogame.Util.Util.getTotalRam;
public class pachiGame extends Game {
    private static final String TAG = "GoGamePachi";

    private static Process _engineProcess;
    private static Thread _stdErrThread;
    private static Thread _exitThread;
    private static OutputStreamWriter _writer;
    private static BufferedReader _reader;
    private boolean _isRunning;
    private Properties _properties;
    private int initEngineProcessTimes = 0;
    private boolean isAnalysisScore = false;


    public pachiGame(Context context, int size, int hdcp, String komi, byte humanColor, int level, BoardView boardView){
        super(context,size,hdcp,komi,humanColor,level,boardView);
        if (_engineProcess==null&&!_isRunning)
            init(null);
        gameList.clear();
        newGame();
    }
    public pachiGame(Context context,String text,BoardView boardView){
        super(context,text,boardView);
        if (_engineProcess==null&&!_isRunning)
            init(null);
        initErrThread();
        newGame();
    }

    private boolean init(Properties properties) {
        _properties = properties;
        try {
            if (!_isRunning) {
                String propArgs = null;
                if (properties != null)
                    propArgs = properties.getProperty("process_args");
                String[] processArgs = (propArgs == null) ? getProcessArgs(level) : propArgs.split(" ");
                int len = processArgs.length;
                String[] args;
                args = new String[len + 1];
                args[0] = getEngineFile().getAbsolutePath();
                System.arraycopy(processArgs, 0, args, 1, len);
                _engineProcess = new ProcessBuilder(args).start();
                _isRunning = true;
            }
            else {
                Log.d(TAG, "Called init() again");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        InputStream is = _engineProcess.getInputStream();
        _reader = new BufferedReader(new InputStreamReader(is), 8192);
        _writer = new OutputStreamWriter(_engineProcess.getOutputStream());


        initEngineProcessTimes +=1;
        return true;
    }
    private File getEngineFile() {
        return pachiFile;
    }

    private boolean restart() {
        return init(_properties);
    }
    private void initErrThread(){
        if (_stdErrThread != null && _stdErrThread.isAlive())
            _stdErrThread.interrupt();
        _stdErrThread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream is = _engineProcess.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8192);
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        parseScoreData(line);
                        Log.e(TAG, "[Err] " + line);
                        if (Thread.currentThread().isInterrupted())
                            return;
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        _stdErrThread.start();

        // Starts a thread to restart the Pachi process if it was killed
        _exitThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Process ep = _engineProcess;
                    if (ep != null)
                        ep.waitFor();
                    _isRunning = false;
                    Log.w(TAG, "##### Engine process has exited with code " + (ep != null ? ep.exitValue() : "[null]"));
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, R.string.game_exited, Toast.LENGTH_SHORT ).show();
                        }
                    });
                }
                catch (InterruptedException ignored) {
                }
            }
        });
        _exitThread.start();
    }

     String sendGtpCommand(String command) {
        try {
            return _intSendGtpCommand(command);
        }
        catch (IOException e) {
            e.printStackTrace();
            // An IOException means that Android killed the process, so we start it
            // again and replay the whole game
            if (initEngineProcessTimes<5){
                if (restart()) {
                    try {
                        newGame();
                        return _intSendGtpCommand(command);
                    }
                    catch (IOException e2) {
                        showGameAlert();
                        Log.e(TAG, "[sendGtpCommand] Unable to restart the engine : cannot replay moves");
                        e2.printStackTrace();
                        return null;
                    }
                }
                else {
                    showGameAlert();
                    Log.e(TAG, "[sendGtpCommand] Unable to restart the engine : init() failed");
                    return null;
                }
            }else{
                showGameAlert();
                return null;
            }

        }
    }
    private String _intSendGtpCommand(String command) throws IOException {
        Log.v(TAG, "Send: " + command);
        _writer.write(command + "\n");
        _writer.flush();
        String res;
        char ch;
        do {
            res = _reader.readLine();
            if (res == null){

                throw new IOException("The process is not running");
            }else{
                initEngineProcessTimes = 0;
            }
            Log.v(TAG, " >> " + res);
            ch = res.length() > 0 ? res.charAt(0) : 0;
        } while (ch != '=' && ch != '?');
        return res;
    }

    void parseScoreData(String line){
        if (line.contains("Move:")){
            isAnalysisScore = true;
        }
        if (line.equals("")) {
            if (isAnalysisScore){
                isAnalysisScore = false;
                for (int i= 0;i<gameInfo.Size;i++){
                    for (int j= 0;j<gameInfo.Size;j++){
                        System.out.print((char)(scoreBoard[j][i]));
                    }
                    System.out.println();
                }
                boardView.setTerritory(scoreBoard);
                ((Activity)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        boardView.invalidate();
                    }
                });

            }

        }
        if (isAnalysisScore){
            try {
                if (line.contains("|")){
                    String[] data = line.split("\\|");
                    if (data.length>=4){
                        int n = gameInfo.Size - Integer.parseInt(data[2].trim());
                        int j=0;
                        for(int i=0;i<data[3].length();i++){
                            if (data[3].charAt(i) !=' '){
                                scoreBoard[j][n] = (byte)data[3].charAt(i);
                                j++;
                            }
                        }
                    }
                }
            }catch (Exception ignored){}

        }

    }
    @Override
    public String getAIName(){
        return COM_PACHI;
    }
    protected String[] getProcessArgs(int level) {
        int _time = (int) Math.round((9 * 1.5) * (0.5 + level / 10.0));

        int _maxTreeSize = 256;
        long totalRam = getTotalRam();

        // The amount of RAM used by pachi (adjustable with max_tree_size) should not
        // be too high compared to the total RAM available, because Android can kill a
        // process at any time if it uses too much memory.
        if (totalRam > 0)
            _maxTreeSize = Math.max(256, (int) Math.round(totalRam / 1024.0 / 1024.0 * 0.5));
        Log.v(TAG, "Set max_tree_size = " + _maxTreeSize);
        Log.v(TAG, "Set time = " + _time);
        if (PERMISSIONS_EXTERNAL_STORAGE){//"-e","patternplay", spat_largest//"patterns=spat_min",
            return new String[]{"-f",dataFile.getAbsolutePath(),"-p",dir.getAbsolutePath(),"-q",cacheDir.getAbsolutePath(),"-t", "" + _time, "max_tree_size=" + _maxTreeSize,"threads=8"};
        }else{
            return new String[]{"-f",dataFile.getAbsolutePath(),"-p",dir.getAbsolutePath(),"-t", "" + _time, "max_tree_size=" + _maxTreeSize,"threads=8"};
        }
//patternplay
    }
}