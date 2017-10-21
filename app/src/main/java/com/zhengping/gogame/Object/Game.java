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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.zhengping.gogame.Board.ProblemView.BLACK;
import static com.zhengping.gogame.Board.ProblemView.BLANK;
import static com.zhengping.gogame.Board.ProblemView.WHITE;
import static com.zhengping.gogame.GoGameApplication.PERMISSIONS_EXTERNAL_STORAGE;
import static com.zhengping.gogame.GoGameApplication.cacheDir;
import static com.zhengping.gogame.Util.Util._BOARD_LETTERS;
import static com.zhengping.gogame.Util.Util._point2str;
import static com.zhengping.gogame.Util.Util._str2point;


public class Game implements BoardView.BoardViewTouchListener{
    private static final String TAG = "Go";
    public GameInfo gameInfo = new GameInfo();
    public int level = 5;
    public boolean gameFinished = false;
    public boolean connected = true;
    boolean passed = false;
    public boolean istry = false;
    private boolean showGameAlerted = false;

    byte playerColor;
    Context context = null;
    ArrayList<String > ABArray = new ArrayList<>();
    ArrayList<Stone> gameList = new ArrayList<>();
    ArrayList<Stone> tryList = new ArrayList<>();
    byte scoreBoard[][];

    private static final String _BLACK_NAME = "black";
    private static final String _WHITE_NAME = "white";
    private byte[][] territory;
    private byte[][] white_territory;
    private byte[][] final_deads;
    public byte humanColor;
    GtpThread _gtpThread;
    BoardView boardView;
    GameCalculator calculator;
    int currentGameListPosition = 0;
    int gameStepCount = 0;
    EngineProcessAction engineProcessAction = null;
    public interface EngineProcessAction{
        void startInitEngine();
        void endInitEngine();
        void StartThinking();
        void endThinking();
        void finishGame();
        void updateGameInfo(int moveNumber);

    }


     Game(Context context,int size,int hdcp,String komi,byte humanColor, int level,BoardView boardView){

        this.context = context;
        engineProcessAction = (EngineProcessAction)context;
        this.boardView = boardView;
        boardView.boardViewTouchListener = this;
        gameInfo.Size = size;
        gameInfo.Hdcp = hdcp;
        gameInfo.Komi = komi;
        this.level = level;
         SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
         editor.putInt("level",level);
         editor.apply();
        this.humanColor = humanColor;
        if (humanColor == BLACK){
            gameInfo.PlayerB = "Human,0";
            gameInfo.PlayerW = getAIName()+",0";
        }else{
            gameInfo.PlayerW = "Human,0";
            gameInfo.PlayerB = getAIName()+",0";
        }
    }
     Game(Context context,String text,BoardView boardView){
        this.context = context;
        this.boardView = boardView;
        boardView.boardViewTouchListener = this;
         level = PreferenceManager.getDefaultSharedPreferences(context).getInt("level",1);
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

    }

    public void branchGame(Stone stone){
        while (gameList.size()>0 && gameList.size()!=currentGameListPosition){
            gameList.remove(gameList.size()-1);
        }
        gameList.add(stone);
        newGame();
    }

    public String getAIName(){
        return "AI";
    }

    String sendGtpCommand(String command) {
        return "";
    }


    public void setupNewGame(){

        calculator  = new GameCalculator(gameInfo);
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
        updateGameInfo(gameList.size());
    }

    public boolean newGame() {
        if (engineProcessAction != null) engineProcessAction.startInitEngine();
        setupNewGame();
        sendGtpCommand("boardsize " + gameInfo.Size);
        sendGtpCommand("komi " + ((int) ( Double.parseDouble(gameInfo.Komi) * 10.0) / 10.0));
        sendGtpCommand("clear_board");
        setLevel(level);

        String cmdStatus = "";
        int handicap = gameInfo.Hdcp;
        if (ABArray.size()>0){
            String AB ="";
            for (int i=0;i<ABArray.size();i++){
                final Point point = new Point(ABArray.get(i).charAt(0)-'a',ABArray.get(i).charAt(1)-'a');
                AB += " "+_point2str(point.x ,point.y,gameInfo.Size);
            }

            sendGtpCommand("set_free_handicap "+AB);
        }else{
            if(handicap > 0 )
                cmdStatus = sendGtpCommand("fixed_handicap " + handicap);
            if (cmdStatus!=null && cmdStatus.length() > 1) {
                String[] handicapCoords = cmdStatus.replace("\n", "").split(" ");
                for (String handiCoord : handicapCoords) {
                    try {
                        final Point point = _str2point(handiCoord,gameInfo.Size);
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
            _gtpThread.playMove();
        }else{
            boardView.touchScreenAllowed = true;
        }
        updateGameInfo(gameList.size());
        if (engineProcessAction != null) engineProcessAction.endInitEngine();
        return true;
    }

    public void updateBoardView(MainActivity.Replay_Action action){

        switch (action){
            case Pre:
                if (currentGameListPosition ==0)
                    return;
                currentGameListPosition -=1;
                break;
            case Next:
                if(currentGameListPosition>=tryList.size())
                    return;
                currentGameListPosition +=1;
                break;
            case Start:
                currentGameListPosition = 0;
                break;
            case End:
                currentGameListPosition = tryList.size();
                break;

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
                    Stone stone = tryList.get(i);
                    boardView.putStone(stone);
                }
                updateGameInfo(currentGameListPosition);
                boardView.invalidate();
            }
        });

    }

    public void finishTryGame(){
        ((MainActivity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boardView.clearBoard();
                for (int i=0;i<ABArray.size();i++){
                    Point point = new Point(ABArray.get(i).charAt(0)-'a',ABArray.get(i).charAt(1)-'a');
                    boardView.placeStone(new Stone(point,BLACK));
                }
                for (int i=0;i<gameList.size();i++){
                    Stone stone = gameList.get(i);
                    boardView.putStone(stone);
                }
                currentGameListPosition = gameList.size();
                updateGameInfo(currentGameListPosition);
                playerColor = getOppositeColor(gameList.get(gameList.size()-1).color);
                boardView.setNextColor(playerColor);
                boardView.invalidate();
            }
        });
    }

    void updateCalculatorMove(Stone stone){
        calculator.playMove(stone);
    }
    void gameMoved(final Stone stone){
        gameList.add(stone);
        currentGameListPosition = gameList.size();
        gameStepCount =  gameList.size();
        updateGameInfo(gameList.size());
        saveTmpGame();
        playerColor = getOppositeColor(playerColor);
        boardView.setNextColor(playerColor);

            ((MainActivity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!boardView.touchScreenAllowed) {
                         boardView.putStone(stone);
                        boardView.touchScreenAllowed = true;
                        if (stone.point.x == -1) {
                            Toast.makeText(context,context.getString(R.string.board_player_passes,getAIName()), Toast.LENGTH_SHORT).show();
                            if (passed){
                                finishGame();
                            }else{
                                passed = true;
                            }
                        }else{
                            passed = false;
                        }
           }
                }
            });



    }
    public boolean playMove(Stone stone, boolean playMove) {
        Point point = stone.point;
        int color = stone.color;
        if ((point.x != -1 || point.y != -1) && (point.x < 0 || point.x >= gameInfo.Size || point.y < 0 || point.y >= gameInfo.Size))
            return false;

        String cmd = String.format("play %1$s %2$s", getColorString(color), _point2str(point.x,point.y,gameInfo.Size));
          boolean success = cmdSuccess(sendGtpCommand(cmd));
        if (success && playMove) {
            if (point.x == -1 && point.y == -1 && passed){
                gameMoved(stone);
                finishGame();
            }
            else{
                gameMoved(stone);
                if (point.x == -1 && point.y == -1) passed = true;
                _gtpThread.playMove();
            }


        }
        return success;
    }

    public void askInfluence(){

    }
    public void askFinalStatus() {
        engineProcessAction.StartThinking();
        calculator.askFinalStatus();
        boardView.setTerritory(calculator.scoreBoard);
        final String gameScore = calculator.askFinalScore();
        ((MainActivity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boardView.invalidate();
                if (!gameScore.equals("")){
                    AlertDialog alertDialog = new AlertDialog.Builder(context)
                            .setPositiveButton("OK",null)
                            .setMessage(gameScore)
                            .create();
                    alertDialog.show();
                }
            }
        });
        engineProcessAction.endThinking();


//        try{
//            clearTerritory();
//            String[] points = sendGtpCommand("final_status_list white_territory").split(" ");
//            int len = points.length;
//            for (int i = 1; i < len; i++) {
//                Point pt = _str2point(points[i]);
//                if (pt != null)
//                    territory[pt.x][pt.y] = 2 ;
//            }
//
//            points = sendGtpCommand("final_status_list black_territory").split(" ");
//            len = points.length;
//            for (int i = 1; i < len; i++) {
//                Point pt = _str2point(points[i]);
//                if (pt != null)
//                    territory[pt.x][pt.y] = 1;
//            }
//
//            points = sendGtpCommand("final_status_list dead").split("[ \n]");
//            len = points.length;
//            for (int i = 1; i < len; i++) {
//                Point pt = _str2point(points[i]);
//                if (pt != null) {
//                    final_deads[pt.x][pt.y] = 3;
//                }
//            }
//        }catch (Exception e){ e.printStackTrace();}


    }
    public boolean resign() {

        return true;
    }

     Point genMove() {
        boardView.touchScreenAllowed = false;
        if (engineProcessAction != null) engineProcessAction.StartThinking();
        String move = sendGtpCommand("genmove " + getColorString(playerColor));
         if (engineProcessAction != null) engineProcessAction.endThinking();
         if (!cmdSuccess(move)){
            ((MainActivity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(R.string.Ai_thinking_fiale);
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, int which) {
                            _gtpThread.playMove();
                        }
                    });
                    builder.setNegativeButton(R.string.cancel, null);
                    final AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });

            return null;
        }
        Point point = _str2point(move.substring(move.indexOf(' ') + 1).trim(),gameInfo.Size);
        if (point.x == -1){
            Stone stone = new Stone(new Point(-1,-1),playerColor);
            gameMoved(stone);
        }
        else if (point.x == -3){

            ((MainActivity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(R.string.gtp_resign_win);
                    builder.setPositiveButton(R.string.ok,null);
                    builder.setNegativeButton(R.string.cancel, null);
                    final AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
        }
        else{
            Stone stone = new Stone(point,playerColor);
            gameMoved(stone);
        }
        return point;
    }

    String getColorString(int color) {
        return (color == WHITE ? _WHITE_NAME : _BLACK_NAME);
    }
     boolean cmdSuccess(String response) {
        if (response== null || response.length() ==0)
            return false;
        return response.charAt(0) == '=';
    }
    private byte getOppositeColor(byte color){
        return color == WHITE?BLACK:WHITE;
    }
    protected String getOppositeString() {
        return (playerColor == WHITE ? _BLACK_NAME : _WHITE_NAME);
    }


    public void resume(){
        if (playerColor != humanColor){
            _gtpThread.playMove();
        }
    }
    public void pass() {

            Stone stone = new Stone(new Point(-1,-1),playerColor);
            playMove(stone,true);
    }

    public void finishGame(){
        gameFinished = true;
        boardView.touchScreenAllowed = false;
        try{
            engineProcessAction.finishGame();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public boolean undo() {
        if (gameList.size()<1) return false;
        gameList.remove(gameList.size()-1);

        if (gameList.size()>0){
                gameList.remove(gameList.size()-1);
        }
        newGame();
        saveTmpGame();
        return true;
        //return cmdSuccess(sendGtpCommand("gg-undo " + (doubleUndo ? 2 : 1)));
    }


    void updateGameInfo(final int moveNumber) {
       if (engineProcessAction != null){
           engineProcessAction.updateGameInfo(moveNumber);
       }
    }

    public boolean setLevel(int level) {
        return true;
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

        File file=new File(cacheDir, dateFormat.format(date!=null?date:new Date())+"_"+level+".ugi");
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
    public void copyTryList() {
        tryList = new ArrayList<>(gameList);
    }
    void showGameAlert(){
        if (!showGameAlerted){
            boardView.touchScreenAllowed = true;
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



    @Override
    public void tapScreen(final Stone stone){
        if (boardView.canPutStone(stone)) {
            boardView.putStone(stone);
            if (!istry){
                playMove(stone,true);

            }else{
                for(int i = tryList.size()-1;i>=currentGameListPosition;i--){
                    tryList.remove(i);
                }
                tryList.add(stone);
                currentGameListPosition = tryList.size();
                playerColor = getOppositeColor(stone.color);
                boardView.setNextColor(playerColor);
            }
            boardView.scaleView();
        }
    }

    public int[] getInfluence(){
        calculator.askInfluence();
        boardView.setTerritory(calculator.scoreBoard);
        return boardView.calculate_m();
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
    class GtpThread extends HandlerThread implements Handler.Callback {
        private static final String TAG = "GtpThread";
        private static final int
                _MSG_PLAY = 1,
                _MSG_FINAL_SCORE = 2,
                _MSG_INFLUENCE = 3 ;




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

        void getInfluence(){
            _handler.sendMessage(_handler.obtainMessage(_MSG_INFLUENCE));
        }
        void getFinalScore() {
            _handler.sendMessage(_handler.obtainMessage(_MSG_FINAL_SCORE));
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case _MSG_PLAY:
                    genMove();
                    return true;
                case _MSG_FINAL_SCORE:
                    askFinalStatus();
                    return true;
                case _MSG_INFLUENCE:
                    askInfluence();
                    return true;
            }


            return false;
        }
    }

}
