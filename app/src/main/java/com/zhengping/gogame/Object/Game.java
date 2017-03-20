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
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.zhengping.gogame.Board.BoardView;
import com.zhengping.gogame.MainActivity;
import com.zhengping.gogame.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static com.zhengping.gogame.Board.ProblemView.BLACK;
import static com.zhengping.gogame.Board.ProblemView.BLANK;
import static com.zhengping.gogame.Board.ProblemView.WHITE;
import static com.zhengping.gogame.GoGameApplication.PERMISSIONS_EXTERNAL_STORAGE;
import static com.zhengping.gogame.GoGameApplication.cacheDir;
import static com.zhengping.gogame.GoGameApplication.pachiFile;




public class Game implements BoardView.BoardViewTouchListener{
    private static final String TAG = "GoGame";
    public GameInfo gameInfo = new GameInfo();
    private int level = 5;
    private int initEngineProcessTimes = 0;
    private boolean showGameAlerted = false;
    private byte playerColor;
    private Context context = null;
    private ArrayList<String > ABArray = new ArrayList<>();
    private ArrayList<Stone> gameList = new ArrayList<>();
    private byte scoreBoard[][];
    private static Process _engineProcess;
    private static Thread _stdErrThread;
    private static Thread _exitThread;
    private static OutputStreamWriter _writer;
    private static BufferedReader _reader;
    private boolean _isRunning;
    private Properties _properties;
    private static final String _BOARD_LETTERS = "ABCDEFGHJKLMNOPQRSTUVWXYZ"; // no 'I'
    private static final String _BLACK_NAME = "black";
    private static final String _WHITE_NAME = "white";
    private byte[][] territory;
    private byte[][] white_territory;
    private byte[][] final_deads;
    public byte humanColor;
    private GtpThread _gtpThread;
    private BoardView boardView;
    private boolean isAnalysisScore = false;
    private int currentGameListPosition = 0;
    private int gameStepCount = 0;
    private EngineProcessAction engineProcessAction = null;
    public interface EngineProcessAction{
        void startInitEngine();
        void endInitEngine();
        void StartThinking();
        void endThinking();


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
                            Toast.makeText(context,R.string.game_exited, Toast.LENGTH_SHORT ).show();
                        }
                    });
                }
                catch (InterruptedException ignored) {
                }
            }
        });
        _exitThread.start();
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
    private boolean restart() {
        return init(_properties);
    }

    public Game(Context context,int size,int hdcp,String komi,byte humanColor, int level,BoardView boardView){

        this.context = context;
        engineProcessAction = (EngineProcessAction)context;
        setSubtitleMoveNumber(0);
        this.boardView = boardView;
        boardView.boardViewTouchListener = this;
        gameInfo.Size = size;
        gameInfo.Hdcp = hdcp;
        gameInfo.Komi = komi;
        this.level = level;
        this.humanColor = humanColor;
        if (humanColor == BLACK){
            gameInfo.PlayerB = "Human,0";
            gameInfo.PlayerW = getAIName()+",0";
        }else{
            gameInfo.PlayerW = "Human,0";
            gameInfo.PlayerB = getAIName()+",0";
        }
        if (_engineProcess==null&&!_isRunning)
            init(null);
        gameList.clear();
        newGame();

    }
    public Game(Context context,String text,BoardView boardView){
        this.context = context;
        this.boardView = boardView;
        boardView.boardViewTouchListener = this;
        setSubtitleMoveNumber(0);
        engineProcessAction = (EngineProcessAction)context;
        try {
            if (text != null) {
                String[] lines = text.split(System.getProperty("line.separator"));
                int step = 0;
                Map<String, Object> heads = new HashMap<>();
                for (String str : lines) {
                    if (str.contains("[Header]") && step == 0) {
                        step = 1;
                        continue;
                    }
                    if (str.contains("[Files]") && step != 0) {
                        step = 2;
                        continue;
                    }
                    if (str.contains("[Data]") && step != 0) {
                        step = 3;
                        gameInfo.parseUGI(heads);
                        continue;
                    }
                    if (step == 1) {
                        String[] prop = str.split("=");
                        if (prop.length == 2) {
                            String h = prop[1].replace("\r\n", "").replace("\r", "").replace("\n", "");
                            heads.put(prop[0], h);
                        }
                    }
                    if (step == 3) {
                        String[] prop = str.split(",");
                        if (prop.length == 4) {
                            String color = prop[1].substring(0, 1);
                            if (!(color.equals("B") || color.equals("W"))) continue;
                            String stonePlace = prop[0].toLowerCase();
                            int nodeId = Integer.parseInt(prop[2]);
                            if (gameInfo.CoordinateType.equals("IGS")) {
                                char t1 = stonePlace.charAt(0);
                                char t2 = stonePlace.charAt(1);
                                t2 = (char) (gameInfo.Size - (t2 - 'a') + 'a' - 1);
                                stonePlace = t1 + "" + t2;
                            }
                            if (nodeId == 0) {
                                if (color.equals("B")) {
                                    ABArray.add(stonePlace);
                                }
                            } else {
                                Stone stone = new Stone(new Point(stonePlace.charAt(0) - 'a', stonePlace.charAt(1) - 'a'), color.equals("B") ? BLACK : WHITE);
                                gameList.add(stone);
                            }

                        }
                    }
                }


            }
        }catch (Exception ignored){
        }
        this.humanColor = (gameInfo.PlayerB.contains("Human")?BLACK:WHITE);
        if (_engineProcess==null&&!_isRunning)
            init(null);
        initErrThread();
        newGame();
    }

    public void branchGame(Stone stone){
        while (gameList.size()>0 && gameList.size()!=currentGameListPosition){
            gameList.remove(gameList.size()-1);
        }
        gameList.add(stone);
        newGame();


    }

    String getAIName(){
        return "AI";
    }
    private File getEngineFile() {
        return pachiFile;
    }
    protected String[] getProcessArgs(int level ) {

        return new String[]{" "};
    }

    private String sendGtpCommand(String command) {
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
    protected Point _str2point(String coords) {
        if (coords.equalsIgnoreCase("pass"))
            return new Point(-1, -1);
        else if (coords.equalsIgnoreCase("resign"))
            return new Point(-3, -3);
        else {
            try {
                return new Point(
                        _BOARD_LETTERS.indexOf(coords.charAt(0)),
                        gameInfo.Size - Integer.parseInt(coords.substring(1).trim()));
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public boolean newGame() {
        if (engineProcessAction != null) engineProcessAction.startInitEngine();
        if (_gtpThread != null && _gtpThread.isAlive()) {
            _gtpThread.quit();
            try {
                _gtpThread.join(); // TODO show a ProgressDialog
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        _gtpThread = new GtpThread();
        _gtpThread.start();
        territory = new byte[gameInfo.Size][gameInfo.Size];
        white_territory = new byte[gameInfo.Size][gameInfo.Size];
        final_deads = new byte[gameInfo.Size][gameInfo.Size];
        scoreBoard = new byte[gameInfo.Size][gameInfo.Size];
        ((Activity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boardView.setBoardSize(gameInfo.Size,1,gameInfo.Size);
                for (int i=0;i<ABArray.size();i++){
                    Point point = new Point(ABArray.get(i).charAt(0)-'a',ABArray.get(i).charAt(1)-'a');
                    boardView.placeStone(new Stone(point,BLACK));
                }
                for (Stone stone :gameList){
                    boardView.putStone(stone);
                }
            }
        });


        sendGtpCommand("boardsize " + gameInfo.Size);
        sendGtpCommand("komi " + ((int) ( Double.parseDouble(gameInfo.Komi) * 10.0) / 10.0));
        sendGtpCommand("clear_board");
//        setLevel(level);
//        String cmd = "protocol_version";
//        String response = sendGtpCommand(cmd);
//        System.out.print(response);
//        cmd = "name";
//        response = sendGtpCommand(cmd);
//        System.out.print(response);
//        cmd = "version";
//        response = sendGtpCommand(cmd);
//        System.out.print(response);
//        cmd = "list_commands";
//        response = sendGtpCommand(cmd);
//        System.out.print(response);


        String cmdStatus = "";
        int handicap = gameInfo.Hdcp;
        if (ABArray.size()>0){
            String AB ="";
            for (int i=0;i<ABArray.size();i++){
                final Point point = new Point(ABArray.get(i).charAt(0)-'a',ABArray.get(i).charAt(1)-'a');
                AB += " "+_point2str(point.x ,point.y);
            }

            sendGtpCommand("set_free_handicap "+AB);
        }else{
            while (handicap > 0 && !cmdSuccess((cmdStatus = sendGtpCommand("fixed_handicap " + handicap))))
                handicap--;
            if (cmdStatus!=null && cmdStatus.length() > 1) {
                String[] handicapCoords = cmdStatus.replace("\n", "").split(" ");
                for (String handiCoord : handicapCoords) {
                    try {
                        final Point point = _str2point(handiCoord);
                        if (point != null){
                            ((MainActivity)context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    boardView.placeStone(new Stone(new Point(point.x,point.y),BLACK));
                                }
                            });


                            String PB =  (char) (point.x + 'a') + "" + (char) (point.y + 'a');
                            ABArray.add(PB);
                        }

                    }
                    catch (Exception ignored) { // Ignore whitespaces and other useless characters
                    }
                }
            }
        }

        int color = BLANK;
        for (final Stone stone :gameList){
            color = stone.color;
            playMove(stone,false);
        }
        currentGameListPosition = gameStepCount = gameList.size();
        if (color != BLANK) {
            playerColor = (color == BLACK)?WHITE:BLACK;
        }else {
            playerColor = (handicap >0)? WHITE : BLACK;
        }
        boardView.setNextColor(playerColor);
        if (playerColor != humanColor){
            boardView.isAIThinking = true;
            _gtpThread.playMove();
        }else{
            boardView.isAIThinking = false;
        }
        setSubtitleMoveNumber(gameList.size());
        if (engineProcessAction != null) engineProcessAction.endInitEngine();
        return true;
    }

    public void updateBoardView(boolean isPre){
        if (isPre){
            if (currentGameListPosition ==0)
                return;
            currentGameListPosition -=1;
        }else{
            if(currentGameListPosition>=gameStepCount)
                return;
            currentGameListPosition +=1;
        }
        ((MainActivity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boardView.clearBoard();
                for (int i=0;i<ABArray.size();i++){
                    Point point = new Point(ABArray.get(i).charAt(0)-'a',ABArray.get(i).charAt(1)-'a');
                    boardView.placeStone(new Stone(point,BLACK));
                }
                for (int i=0;i<currentGameListPosition;i++){
                    Stone stone = gameList.get(i);
                    boardView.putStone(stone);
                }
                setSubtitleMoveNumber(currentGameListPosition);
                boardView.invalidate();
            }
        });

    }
    private void gameMoved(final Stone stone){
        gameList.add(stone);
        currentGameListPosition = gameList.size();
        gameStepCount =  gameList.size();
        setSubtitleMoveNumber(gameList.size());
        saveTmpGame();
        playerColor = getOppositeColor(playerColor);
        boardView.setNextColor(playerColor);

            ((MainActivity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (boardView.isAIThinking) {
                        boardView.putStone(stone);
                        if (stone.point.x == -1) {
                            Toast.makeText(context,context.getString(R.string.board_player_passes,getAIName()), Toast.LENGTH_SHORT).show();
                        }
                        boardView.isAIThinking = false;
                    }
                }
            });



    }
    public boolean playMove(Stone stone, boolean playMove) {
        Point point = stone.point;
        int color = stone.color;
        if ((point.x != -1 || point.y != -1) && (point.x < 0 || point.x >= gameInfo.Size || point.y < 0 || point.y >= gameInfo.Size))
            return false;

        String cmd = String.format("play %1$s %2$s", getColorString(color), _point2str(point.x,point.y));
        boolean success = cmdSuccess(sendGtpCommand(cmd));
        if (success && playMove) {
            gameMoved(stone);
            _gtpThread.playMove();

        }
        return success;
    }

    private void askFinalStatus() {
        try{
            clearTerritory();
            String[] points = sendGtpCommand("final_status_list white_territory").split(" ");
            int len = points.length;
            for (int i = 1; i < len; i++) {
                Point pt = _str2point(points[i]);
                if (pt != null)
                    territory[pt.x][pt.y] = 2 ;
            }

            points = sendGtpCommand("final_status_list black_territory").split(" ");
            len = points.length;
            for (int i = 1; i < len; i++) {
                Point pt = _str2point(points[i]);
                if (pt != null)
                    territory[pt.x][pt.y] = 1;
            }

            points = sendGtpCommand("final_status_list dead").split("[ \n]");
            len = points.length;
            for (int i = 1; i < len; i++) {
                Point pt = _str2point(points[i]);
                if (pt != null) {
                    final_deads[pt.x][pt.y] = 3;
                }
            }
        }catch (Exception e){ e.printStackTrace();}

    }
    public boolean resign() {

        return true;
    }

    private Point genMove() {
        boardView.isAIThinking = true;
        if (engineProcessAction != null) engineProcessAction.StartThinking();
        String move = sendGtpCommand("genmove " + getColorString(playerColor));
        Point point = _str2point(move.substring(move.indexOf(' ') + 1).trim());
        if (point.x == -1){
            Stone stone = new Stone(new Point(-1,-1),getOppositeColor(playerColor));
            gameMoved(stone);
        }
        else if (point.x == -3){
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(R.string.gtp_resign_win);
            builder.setPositiveButton("OK", null);
            final AlertDialog dialog = builder.create();
            ((MainActivity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.show();
                }
            });
        }
        else{
            Stone stone = new Stone(point,playerColor);
            gameMoved(stone);

        }
        if (engineProcessAction != null) engineProcessAction.endThinking();

        return point;
    }
    public String finalScore(){
        String cmd = "final_score";
        String response = sendGtpCommand(cmd);
        boolean success = cmdSuccess(response);
        if (success) {
            System.out.print(response);
            return response;
        }
        return null;

    }
//    public void askFinalStatus() {
//        String[] coords = sendGtpCommand("final_status_list white_territory").split(" ");
//        int len = coords.length;
//        for (int i = 1; i < len; i++) {
//            Point pt = _str2point(coords[i]);
//            if (pt != null)
//                boardView.white_territory.add(pt);
//        }
//
//        coords = sendGtpCommand("final_status_list black_territory").split(" ");
//        len = coords.length;
//        for (int i = 1; i < len; i++) {
//            Point pt = _str2point(coords[i]);
//            if (pt != null)
//                boardView.black_territory.add(pt);
//        }
//
//        coords = sendGtpCommand("final_status_list dead").split("[ \n]");
//        len = coords.length;
//        for (int i = 1; i < len; i++) {
//            Point pt = _str2point(coords[i]);
//            if (pt != null) {
//                boardView.dead_stones.add(new Stone(pt,boardView.getPoint(pt.x,pt.y)==WHITE?WHITE:BLACK);
//            }
//        }
//        System.out.println("askFinalStatus down");
//    }
    private String getColorString(int color) {
        return (color == WHITE ? _WHITE_NAME : _BLACK_NAME);
    }
    private boolean cmdSuccess(String response) {
        if (response== null)return false;
        return response.charAt(0) == '=';
    }
    private byte getOppositeColor(byte color){
        return color == WHITE?BLACK:WHITE;
    }
    protected String getOppositeString() {
        return (playerColor == WHITE ? _BLACK_NAME : _WHITE_NAME);
    }
    private String _point2str(int x, int y) {
        if (x == -1 && y == -1)
            return "pass";
        else if (x == -3 && y == -3)
            return "resign";
        else
            return String.valueOf(_BOARD_LETTERS.charAt(x)) + (gameInfo.Size - y);
    }
    private void clearTerritory(){
        for (int i =0;i< gameInfo.Size; ++i){
            for (int j =0;j< gameInfo.Size; ++j){
                territory[i][j] = 0;
                white_territory[i][j] = 0;
                final_deads[i][j] = 0;


            }
        }
    }

    public void pass() {
        Stone stone = new Stone(new Point(-1,-1),playerColor);
        playMove(stone,true);
    }
    public boolean undo() {
        if (gameList.size()<1) return false;
//        String result = sendGtpCommand("undo");
//        if (cmdSuccess(result)) {
//            gameList.remove(gameList.size()-1);
//            if (gameList.size()>1){
//                if (cmdSuccess(sendGtpCommand("undo"))){
//                    gameList.remove(gameList.size()-1);
//                }
//            }
//            return true;
//        } else if (result.contains("cannot undo")) {
//            Log.v(TAG, "Faking undo...");
//            if (gameList.size()>1){
//                gameList.remove(gameList.size()-1);
//            }
//            if (gameList.size()>1){
//                gameList.remove(gameList.size()-1);
//            }
//            newGame();
//            return true;
//        }
        gameList.remove(gameList.size()-1);

        if (gameList.size()>0){
                gameList.remove(gameList.size()-1);
        }
        newGame();
        saveTmpGame();
        return true;
        //return cmdSuccess(sendGtpCommand("gg-undo " + (doubleUndo ? 2 : 1)));
    }


    private void setSubtitleMoveNumber(final int moveNumber) {
       final ActionBar actionBar =((AppCompatActivity)context).getSupportActionBar();
        if (actionBar !=null){
            ((AppCompatActivity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    actionBar.setSubtitle((moveNumber > 0) ?
                            context.getString(R.string.board_move_number, moveNumber) : context.getString(R.string.board_no_moves));
                }
            });

        }


    }

    public boolean setLevel(int level) {
        return cmdSuccess(sendGtpCommand("level " + level));
    }
    private void saveTmpGame(){
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString("tempGame", gameInfo.gameString(gameList,ABArray));
        editor.apply();
    }
    public void saveGame(){
        if (gameList.size()<10) return;
        if(!PERMISSIONS_EXTERNAL_STORAGE) return;
        if(!cacheDir.exists())
            cacheDir.mkdirs();
        Date date = null;
        try {
            date = gameInfo.dateFormat.parse(gameInfo.startDate);
        }catch (Exception ignored){}
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_hhmmss", Locale.US);

        File file=new File(cacheDir, dateFormat.format(date!=null?date:new Date())+".ugi");
        if (file.exists()){
            file.delete();
        }
        try
        {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(gameInfo.gameString(gameList,ABArray));
            myOutWriter.close();
            fOut.flush();
            fOut.close();
        }
        catch (IOException e)
        {
            Log.e("Exception", "File write failed: " + e.toString());
        }
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.remove("tempGame");
        editor.apply();


    }
    private void showGameAlert(){
        if (!showGameAlerted){
            boardView.isAIThinking = false;
            showGameAlerted = true;

            ((MainActivity)context).runOnUiThread(new Runnable() {
                @Override

                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(R.string.game_exited);
                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    });
                    final AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });


        }
        if (engineProcessAction != null) engineProcessAction.endThinking();
    }

    private void parseScoreData(String line){
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
    public void tapScreen(final Stone stone){
        if (boardView.canPutStone(stone)) {
            if (currentGameListPosition == gameStepCount){
                boardView.putStone(stone);
                playMove(stone,true);
            }else if (stone.color == humanColor){

                AlertDialog alertDialog = new AlertDialog.Builder(context)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                branchGame(stone);
                            }
                        })
                        .setMessage(R.string.tap_screen_alert)
                        .setNegativeButton(R.string.cancel,null)
                        .create();
                alertDialog.show();
            }
        }
    }

    public void getFinalScore(){
        _gtpThread.getFinalScore();
    }
    public int getGameSize(){
        return gameInfo.Size;
    }
    public int getGameHdcp(){
        return gameInfo.Hdcp;
    }
    public double getGameKomi(){
        return Double.parseDouble(gameInfo.Komi);
    }
    private class GtpThread extends HandlerThread implements Handler.Callback {
        private static final String TAG = "GtpThread";
        private static final int
                _MSG_PLAY = 1,
                _MSG_FINAL_SCORE = 2;

        private Handler _handler;


        GtpThread() {
            super("GtpThread");
        }

        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();
            synchronized (this) {
                _handler = new Handler(getLooper(), this);
                notifyAll();
            }
        }

        void playMove() {
            synchronized (this) {
                while (_handler == null) {
                    try {
                        wait();
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            _handler.sendMessage(_handler.obtainMessage(_MSG_PLAY));
        }

        void getFinalScore() {
            _handler.sendMessage(_handler.obtainMessage(_MSG_FINAL_SCORE));
        }

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == _MSG_PLAY) {
                genMove();
                return true;
            } else if (msg.what == _MSG_FINAL_SCORE) {
                askFinalStatus();
                return true;
            }
            return false;
        }
    }

}
