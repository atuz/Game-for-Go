package com.zhengping.gogame.Object;

import android.content.Context;
import com.zhengping.gogame.Board.BoardView;
import static com.zhengping.gogame.MainActivity.COM_GNUGO;
import static com.zhengping.gogame.Object.GnuGoGame.initGnugoEngine;
public class Gnugo_game  extends Game{
    private static final String TAG = "gnugo";
    public Gnugo_game(Context context, int size, int hdcp, String komi, byte humanColor, int level, BoardView boardView){
        super(context,size,hdcp,komi,humanColor,level,boardView);
        initGnugoEngine();
        gameList.clear();
        newGame();
    }
    public Gnugo_game(Context context,String text,BoardView boardView){
        super(context,text,boardView);
        initGnugoEngine();
        newGame();
    }


    public String getAIName(){
        return COM_GNUGO;
    }

    boolean cmdSuccess(String response) {
        return true;
    }
    public boolean setLevel(int level) {
        level = (int)(level*1.5);
        return cmdSuccess(sendGtpCommand("level " + level));
    }

    public boolean newGame(){
        return super.newGame();
    }

    void updateCalculatorMove(Stone stone){

    }


//    public String getInfluence(){
      //  clearTerritory();
        String cmd;
        String response;
//        cmd = "initial_influence black influence_regions";
//        String response = sendGtpCommand(cmd);
//
//
//
//        cmd = "initial_influence black white_influence";
//        response = sendGtpCommand(cmd);
//
//
//        cmd = "initial_influence black black_influence";
//        response = sendGtpCommand(cmd);
//        cmd = "initial_influence black white_strength";
//        response = sendGtpCommand(cmd);
//        cmd = "initial_influence black black_strength";
//        response = sendGtpCommand(cmd);
//
//
//        cmd = "initial_influence black white_attenuation";
//        response = sendGtpCommand(cmd);
//        cmd = "initial_influence black black_attenuation";
//        response = sendGtpCommand(cmd);
//        cmd = "initial_influence black white_permeability";
//        response = sendGtpCommand(cmd);
//
//        cmd = "initial_influence black black_permeability";
//        response = sendGtpCommand(cmd);

        //captures
//        cmd = "initial_influence black territory_value";
//        response = sendGtpCommand(cmd);
//        return response;
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

//    }



    String sendGtpCommand(String command) {
        return GnuGoGame.sendGtpCommand(command);
    }

}
