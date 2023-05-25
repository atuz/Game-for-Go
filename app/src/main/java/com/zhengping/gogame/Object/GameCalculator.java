package com.zhengping.gogame.Object;

import android.graphics.Point;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.StringTokenizer;

import static com.zhengping.gogame.Util.Util._point2str;
import static com.zhengping.gogame.Util.Util.getColorString;
import static com.zhengping.gogame.Util.Util._str2point;


public class GameCalculator {
    private static final String TAG = "Go";
    public String score = "";
    byte scoreBoard[][];
    private GameInfo gameInfo;
    private int boardsize = 0;


    public GameCalculator(@NonNull GameInfo info){
        GnuGoGame.initGnugoEngine();
        gameInfo = info;
        boardsize = info.Size;
        scoreBoard = new byte[gameInfo.Size][gameInfo.Size];

    }

    String sendGtpCommand(String command) {
         return GnuGoGame.sendGtpCommand(command);
    }
    private boolean playGame(ArrayList<String >  ABArray,ArrayList<Stone> gameList){

        sendGtpCommand("boardsize " + gameInfo.Size);
        sendGtpCommand("komi " + ((int) ( Double.parseDouble(gameInfo.Komi) * 10.0) / 10.0));
        sendGtpCommand("clear_board");
        if (ABArray.size()>0){
            String AB ="";
            for (int i=0;i<ABArray.size();i++){
                final Point point = new Point(ABArray.get(i).charAt(0)-'a',ABArray.get(i).charAt(1)-'a');
                AB += " "+_point2str(point.x ,point.y,boardsize);
            }

            sendGtpCommand("set_free_handicap "+AB);
        }else {
            int handicap = gameInfo.Hdcp;
            if (handicap > 0) {
                sendGtpCommand("fixed_handicap " + handicap);
            }
        }
        for (final Stone stone :gameList){
            playMove(stone);
        }
        return true;
    }

    boolean playMove(Stone stone) {
        Point point = stone.point;
        int color = stone.color;
        if ((point.x != -1 || point.y != -1) && (point.x < 0 || point.x >= boardsize || point.y < 0 || point.y >= boardsize))
            return false;
        String response = "";
        try {
            String cmd = String.format("play %1$s %2$s", getColorString(color), _point2str(point.x,point.y,boardsize));
            cmdSuccess(sendGtpCommand(cmd));
        }catch (Exception e){
            e.printStackTrace();
        }

        return cmdSuccess(response);
    }

    private boolean cmdSuccess(String response) {
        return true;
    }
    private boolean setInfluenceBoard1(String territory1,String territory2){
        final StringTokenizer tokenizer = new StringTokenizer (territory1, " \n");
        final StringTokenizer tokenizer1 = new StringTokenizer (territory2, " \n");
        try {
            while (tokenizer.hasMoreTokens ()&&tokenizer1.hasMoreTokens())
            {
                for (int j=0;j<gameInfo.Size;j++){
                    for (int i=0;i<gameInfo.Size;i++){
                        String value = tokenizer.nextToken ();
                        String value1 = tokenizer1.nextToken ();
                        if (Double.parseDouble(value) +Double.parseDouble(value1) >=0.8){
                            scoreBoard[i][j] = 'O';
                        }else if (Double.parseDouble(value)+Double.parseDouble(value1)<=-0.8){
                            scoreBoard[i][j] = 'X';
                        }

                    }
                }
            }
            return  true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;

    }

    private void clearTerritory(){
        for (int i=0;i<gameInfo.Size;i++){
            for (int j=0;j<gameInfo.Size;j++){
                scoreBoard[i][j] = 0;

            }
        }
    }

    String askInfluence(){
        clearTerritory();
        String cmd;
        String response1;
        String response;
//        cmd = "showboard";
//        response = sendGtpCommand(cmd);
        cmd = "initial_influence white territory_value";
        response1 = sendGtpCommand(cmd);
        cmd = "initial_influence black territory_value";
        response = sendGtpCommand(cmd);
        setInfluenceBoard1(response,response1);
        return response;
//        if (!setInfluenceBoard(response)) return;
//        boardView.showTerritory = true;
//        boardView.setTerritory(scoreBoard);
//        ((MainActivity)context).runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                boardView.invalidate();
//
//            }
//        });

    }
    private void setScoreBoard(String territory,char s){
        final StringTokenizer tokenizer = new StringTokenizer (territory, " \n");
        while (tokenizer.hasMoreTokens ())
        {
            Point point = _str2point(tokenizer.nextToken (),boardsize);
            if (point != null)
                scoreBoard[point.x][point.y] = (byte)s;

        }
    }
    String askFinalScore() {
        String cmd;
        cmd = "final_score";
        return sendGtpCommand(cmd);
    }
    void askFinalStatus(){

        this.score = "";
        try{
            clearTerritory();
            String cmd;
            cmd = "final_status_list black_territory";
            String response = sendGtpCommand(cmd);
            setScoreBoard(response,'X');
            cmd = "final_status_list white_territory";
            response = sendGtpCommand(cmd);
            setScoreBoard(response,'O');

            cmd = "final_status_list dame";
            response = sendGtpCommand(cmd);
            setScoreBoard(response,'?');
            response = sendGtpCommand("final_status_list dead");
            setScoreBoard(response,'D');
        }catch (Exception e) {
            e.printStackTrace();
        }


    }


//    public int[] getTerritory_value(String territory){
//        int[] scores = new int[2];
//        final StringTokenizer tokenizer = new StringTokenizer (territory, " \n");
//        try {
//            while (tokenizer.hasMoreTokens ())
//            {
//
//                for (int i=0;i<gameInfo.Size*gameInfo.Size;i++){
//                    String value = tokenizer.nextToken ();
//                    if (Double.parseDouble(value)>=0.5){
//                        scores[1] +=1;
//                        if (Double.parseDouble(value)>=1.5){
//                            scores[1] +=1;
//                        }
//                    }else if (Double.parseDouble(value)<=-0.5){
//                        if (Double.parseDouble(value)<=-1.5){
//                            scores[1] +=1;
//                        }
//                        scores[0] +=1;
//                    }
//
//                }
//
//            }
//            return scores;
//
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        return new int[2];
//
//    }

//    private boolean setInfluenceBoard(String territory){
//        final StringTokenizer tokenizer = new StringTokenizer (territory, " \n");
//        try {
//            while (tokenizer.hasMoreTokens ())
//            {
//                for (int j=0;j<gameInfo.Size;j++){
//                    for (int i=0;i<gameInfo.Size;i++){
//                        String value = tokenizer.nextToken ();
//                        if (Double.parseDouble(value)>=0.5){
//                            scoreBoard[i][j] = 'O';
//                        }else if (Double.parseDouble(value)<=-0.5){
//                            scoreBoard[i][j] = 'X';
//                        }
//
//                    }
//                }
//            }
//            return  true;
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        return false;
//
//    }




}
