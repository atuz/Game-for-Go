package com.zhengping.gogame.Object;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.zhengping.gogame.Board.BoardView;
import com.zhengping.gogame.MainActivity;
import com.zhengping.gogame.R;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;

import static com.zhengping.gogame.Board.ProblemView.BLACK;
import static com.zhengping.gogame.Board.ProblemView.BLANK;
import static com.zhengping.gogame.Board.ProblemView.WHITE;
import static com.zhengping.gogame.GoGameApplication.host;
import static com.zhengping.gogame.GoGameApplication.port;
import static com.zhengping.gogame.MainActivity.COM_SERVER;
import static com.zhengping.gogame.R.string.abc_shareactionprovider_share_with;
import static com.zhengping.gogame.R.string.handicap;
import static com.zhengping.gogame.R.string.white;
import static com.zhengping.gogame.Util.Util._point2str;
import static com.zhengping.gogame.Util.Util._str2point;

/**
 * Created by user on 2017-10-10.
 */

public class SocketGame extends Game {
    private Socket socket = null;
    private OutputStream outputStream = null;
    private  BufferedReader bufReader = null;
    public SocketGame(Context context, int size, int hdcp, String komi, byte humanColor, int level, BoardView boardView){
        super(context,size,hdcp,komi,humanColor,level,boardView);

        gameList.clear();
        newGame();
    }
    public SocketGame(Context context,String text,BoardView boardView){
        super(context,text,boardView);
        newGame();
    }


    public boolean newGame() {
        if (engineProcessAction != null) engineProcessAction.startInitEngine();
        setupNewGame();
        calculator.sendGtpCommand("boardsize " + gameInfo.Size);
        calculator.sendGtpCommand("komi " + ((int) ( Double.parseDouble(gameInfo.Komi) * 10.0) / 10.0));
        int handicap = gameInfo.Hdcp;
        for (final Stone stone :gameList){
            calculator.playMove(stone);
        }

        String cmdStatus = "";
        if (ABArray.size()>0){
            String AB ="";
            for (int i=0;i<ABArray.size();i++){
                final Point point = new Point(ABArray.get(i).charAt(0)-'a',ABArray.get(i).charAt(1)-'a');
                AB += " "+_point2str(point.x ,point.y,gameInfo.Size);
            }

            calculator.sendGtpCommand("set_free_handicap "+AB);
        }else{
            if(handicap > 0 )
                cmdStatus = calculator.sendGtpCommand("fixed_handicap " + handicap);
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


    Point genMove() {
        boardView.touchScreenAllowed = false;
        if (engineProcessAction != null) engineProcessAction.StartThinking();
        Point point = genSocketMove();
        if (engineProcessAction != null) engineProcessAction.endThinking();
        return point;

    }

    public String getAIName(){
        return COM_SERVER;
    }

    public boolean setLevel(int level) {
        String time = "00 01 01";
        switch (level){
            case 6:
                time = "00 01 01";
                break;
            case 7:
                time = "01 01 01";
                break;
            case 8:
                time = "01 02 01";
                break;
            case 9:
                time = "01 03 01";
            case 10:
                time = "02 04 01";
                break;

        }
        try{
            return cmdSuccess(sendGtpCommand("time_settings " + time));
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;


    }
    String sendGtpCommand(String command){
        try {
            return  sendGtpCommand(outputStream,bufReader,command);
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";

    }
    private String sendGtpCommand(OutputStream outputStream,BufferedReader bufReader,String command) throws Exception{
        if (command.charAt(command.length() - 1) !='\n') command+="\n";


        outputStream.write(command.getBytes());
        outputStream.flush();

        String s;
        String reply="";
        while((s = bufReader.readLine()) != null){
            reply = reply + s +"\n";
            if (s.length() ==0) break;
        }
        if (reply.length () == 0 || reply.charAt (0) == '?')
        {
            return "";
        }
        return reply;
    }
    private Point genSocketMove(){

        Point point = null;
        ConnectException exception = new ConnectException();
        InputStream inputStream = null;
        try{
            socket = new Socket(host,port);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            InputStreamReader reader = new InputStreamReader(inputStream);
            bufReader = new BufferedReader(reader);
            if (!cmdSuccess(sendGtpCommand("boardsize " + gameInfo.Size))){
             throw exception;
            }
            if (!cmdSuccess(sendGtpCommand("komi " + ((int) ( Double.parseDouble(gameInfo.Komi) * 10.0) / 10.0)))){
                throw exception;
            }
            if (!setLevel(level)){
                throw exception;
            }

            if (ABArray.size()>0){
                String AB ="";
                for (int i=0;i<ABArray.size();i++){
                    point = new Point(ABArray.get(i).charAt(0)-'a',ABArray.get(i).charAt(1)-'a');
                    AB += " "+_point2str(point.x ,point.y,gameInfo.Size);
                }
                if (!cmdSuccess(sendGtpCommand("set_free_handicap "+AB))){
                    throw exception;
                }
            }

            for (final Stone stone :gameList){
                point = stone.point;
                int color = stone.color;
                String cmd = String.format("play %1$s %2$s", getColorString(color), _point2str(point.x,point.y,gameInfo.Size));
                if (!cmdSuccess(sendGtpCommand(cmd)))
                    throw exception;
                playMove(stone,false);
            }
            point = AiMove();
            connected = true;

        }catch (Exception e){
           if (e.getClass() == ConnectException.class){
               connected = false;
               Toast.makeText(context, R.string.connect_server_fail,Toast.LENGTH_SHORT).show();
           }
            e.printStackTrace();
        }finally {
            ((MainActivity)context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((MainActivity)context).invalidateOptionsMenu();
                }
            });

            try{
                if (inputStream != null)
                     inputStream.close();
                outputStream.close();
                socket.close();
            }catch (Exception e){
                e.printStackTrace();
            }

        }

        return point;
    }
    private Point AiMove() throws Exception {

        String move = sendGtpCommand("genmove " + getColorString(playerColor));
        if (!cmdSuccess(move)){

           throw( new ConnectException());
        }
        boardView.postInvalidate();
        Point point = _str2point(move.substring(move.indexOf(' ') + 1).trim(),gameInfo.Size);
        if (point.x != -3){
            Stone stone = new Stone(point,playerColor);
            String cmd = String.format("play %1$s %2$s", getColorString(stone.color), _point2str(point.x,point.y,gameInfo.Size));
            boolean success = cmdSuccess(calculator.sendGtpCommand(cmd));
        }
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

    public boolean playMove(Stone stone, boolean playMove) {
        Point point = stone.point;
        if ((point.x != -1 || point.y != -1) && (point.x < 0 || point.x >= gameInfo.Size || point.y < 0 || point.y >= gameInfo.Size))
            return false;

        if (playMove) {
            if (point.x == -1 && point.y == -1 && passed){
                gameMoved(stone);
                finishGame();
            }
            else{
                String cmd = String.format("play %1$s %2$s", getColorString(stone.color), _point2str(point.x,point.y,gameInfo.Size));
                boolean success = cmdSuccess(calculator.sendGtpCommand(cmd));
                gameMoved(stone);
                if (point.x == -1 && point.y == -1) passed = true;
                _gtpThread.playMove();
            }


        }
        return true;
    }

    private String cutString(String str){
        try{
            return str.replaceFirst ("= ", "").replace ("\n\n", "");
        }catch (Exception e){
            return "";
        }

    }

}
