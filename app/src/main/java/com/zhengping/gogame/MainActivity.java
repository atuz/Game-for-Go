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

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.zhengping.gogame.Board.ProblemView.BLACK;
import static com.zhengping.gogame.Board.ProblemView.WHITE;
import static com.zhengping.gogame.GoGameApplication.PERMISSIONS_EXTERNAL_STORAGE;
import static com.zhengping.gogame.GoGameApplication.cacheDir;
import static java.lang.Math.abs;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.zhengping.gogame.Board.BoardView;
import com.zhengping.gogame.Object.Game;
import com.zhengping.gogame.Object.Gnugo_game;
import com.zhengping.gogame.Object.SocketGame;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements Game.EngineProcessAction {


    public enum Replay_Action{
        Start, Pre, Next, End
    }

    public static String COM_GNUGO = "gnugo";
    public static String COM_PACHI = "pachi";
    public static String COM_SERVER = "Remote Server";


    final  private int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1002;
    Menu menu;
    Context mainContext;
    private Game game;
    BoardView boardView;
    PopupWindow loadFilesPopWindow;
    private static final String
            _PREF_BOARDSIZE = "newgame_boardsize",
            _PREF_KOMI = "newgame_komi",
            _PREF_COLOR = "newgame_color",
            _PREF_LEVEL = "newgame_level",
            _PREF_HANDICAP = "newgame_handicap";

    private Spinner _spn_boardSize;
    private Spinner _spn_komi;
    private Spinner _spn_color;
    private Spinner _spn_handicap;
    private Spinner _spn_level;
    boolean isSetupNewGame = true;
    View setup_newGame_View = null;
    View loadingView = null;
    ListView moreGamesListView;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainContext = this;
        setContentView(R.layout.activity_main);
        initAds(R.id.adViewGoGame);
        boardView = (BoardView)findViewById(R.id.boardView);
        boardView.touchScreenAllowed = true;
        setup_newGame_View = findViewById(R.id.newGameView);
        loadingView = findViewById(R.id.loading);
        getPermission();
        setupLoadFileView();
        if (resumeGame()){
            isSetupNewGame = false;
            setup_newGame_View.setVisibility(INVISIBLE);
            invalidateOptionsMenu();
        }else{
            setupNewGame();
        }
        setTitle(getString(R.string.board_game_vs, "AI"));
        if (getSupportActionBar()!= null)
            getSupportActionBar().setSubtitle(" ");


      //  new Thread(new Client()).start();


    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        if (isSetupNewGame){
            getMenuInflater().inflate(R.menu.newgame, menu);
        }else{
            if (game != null &&!game.connected)
                getMenuInflater().inflate(R.menu.menu, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event

        int i = item.getItemId();
        if (isSetupNewGame){
            switch (i){
                case R.id.action_cancel:
                    openFile();
                    break;
                default:

          }
        }else{

            switch (i){
                case R.id.action_resume:
                    if(game !=null) game.resume();
                    break;
//                case R.id.action_feedback:
//                    feedback();
//                    break;
                default:

            }
        }
        invalidateOptionsMenu();
        return super.onOptionsItemSelected(item);
    }

    private boolean resumeGame(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String savedGame = prefs.getString("tempGame",null);
        if (savedGame != null){
           int level = PreferenceManager.getDefaultSharedPreferences(this).getInt("level",1);

            if (level>5){
                game = new SocketGame(mainContext,savedGame,boardView);
            }else{
                game = new Gnugo_game(mainContext,savedGame,boardView);
            }

            //  game = new pachiGame(mainContext,savedGame,boardView);


            return true;
        }
        return false;
    }

    private void setupNewGame(){

        setup_newGame_View.setVisibility(VISIBLE);
        _spn_boardSize = (Spinner) findViewById(R.id.spn_play_boardsize);
        _spn_komi = (Spinner) findViewById(R.id.spn_play_komi);
        _spn_color = (Spinner) findViewById(R.id.spn_play_color);
        _spn_level = (Spinner) findViewById(R.id.spn_play_level);
        _spn_handicap = (Spinner) findViewById(R.id.spn_play_handicap);

        // Restore last values
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        _spn_boardSize.setSelection(prefs.getInt(_PREF_BOARDSIZE, 0));
        _spn_komi.setSelection(prefs.getInt(_PREF_KOMI, 1));
        _spn_color.setSelection(prefs.getInt(_PREF_COLOR, 0));
        _spn_level.setSelection(prefs.getInt(_PREF_LEVEL, 4));
        _spn_handicap.setSelection(prefs.getInt(_PREF_HANDICAP, 0));
    }


    public void startNewGame() {
        saveGame();
        final int level = _spn_level.getSelectedItemPosition() + 1;
        byte color;
        int colorPos = _spn_color.getSelectedItemPosition();
        if (colorPos == 0){
            color = BLACK;
        }

        else{
            color = WHITE;
        }

        final byte humanColor = color;
        final int boardsize = Integer.parseInt((String) _spn_boardSize.getSelectedItem());
        final double komi = Double.parseDouble((String) _spn_komi.getSelectedItem());
        int handicap = Integer.parseInt((String) _spn_handicap.getSelectedItem());

        if (boardsize <19 && handicap>4)
            handicap = 4;

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putInt(_PREF_BOARDSIZE,_spn_boardSize.getSelectedItemPosition());
        editor.putInt(_PREF_KOMI,_spn_komi.getSelectedItemPosition());
        editor.putInt(_PREF_LEVEL,_spn_level.getSelectedItemPosition());
        editor.putInt(_PREF_COLOR,_spn_color.getSelectedItemPosition());
        editor.putInt( _PREF_HANDICAP,_spn_handicap.getSelectedItemPosition());
        editor.apply();

               // findViewById(R.id.scoreBtn).setVisibility(View.INVISIBLE);
        if (level>5){
            game = new SocketGame(mainContext,boardsize,handicap,komi+"",humanColor,level,boardView);
        }else{
            game = new Gnugo_game(mainContext,boardsize,handicap,komi+"",humanColor,level,boardView);
        }

         //
        //        game1 = new pachiGame(mainContext,boardsize,handicap,komi+"",AIColor,5,boardView);
    }


    private void setupLoadFileView(){

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels;
        int width = displaymetrics.widthPixels;
      //  int actionBarHeight = 50 ;
//        TypedValue tv = new TypedValue();
//        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
//        {
//            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
//        }
        LayoutInflater layoutInflater = LayoutInflater.from(this);// LayoutInflater.from(context);
        View view =  layoutInflater.inflate(R.layout.game_select, null);
        loadFilesPopWindow = new PopupWindow(view, width, height, true);
        Button cancelBtn = (Button)view.findViewById(R.id.moreGamesCancelBtn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadFilesPopWindow.dismiss();
            }
        });
        moreGamesListView = (ListView)view.findViewById(R.id.moreGamesListView);

    }

    private void saveGame(){
        if (game != null)
            game.saveGame();
    }

    private void openFile(){
        if (PERMISSIONS_EXTERNAL_STORAGE){
            final String[] files = cacheDir.list(
                    new FilenameFilter()
                    {
                        public boolean accept(File dir, String name)
                        {
                            return name.endsWith(".ugi")&&(new File(dir,name)).isFile();

                        }
                    });
            if (files == null || files.length ==0){
                Toast.makeText(this, R.string.cannot_find_files,Toast.LENGTH_SHORT).show();
            }else{
                moreGamesListView.setAdapter(new ArrayAdapter<>(this,
                        android.R.layout.simple_list_item_1, files));
                moreGamesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (game!=null)game.saveGame();
                        loadFilesPopWindow.dismiss();
                        String uri = files[position];// public Game(Context context,Uri uri)
                        int level = 1;
                        try {
                            level = Integer.parseInt(uri.substring(uri.lastIndexOf("_")+1,uri.lastIndexOf(".")));
                        }catch (Exception ignored){}

                        if (level >10) level = 1;
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mainContext).edit();
                        editor.putInt("level",level);
                        editor.commit();
                        File file = new File(cacheDir,uri);
                        final String text;
                        final int gameLevel = level;
                        try{
                            InputStream s = new FileInputStream(file);
                            text = IOUtils.toString(s, Charset.forName("UTF-8"));
                            new Thread(new Runnable() {
                                @Override
                                public void run() {

                                    if(gameLevel>5){
                                        game = new SocketGame(mainContext,text,boardView);
                                    }else{
                                        game = new Gnugo_game(mainContext,text,boardView);
                                    }


                                  //
                                  //  game = new pachiGame(mainContext,text,boardView);
                                }
                            }).start();

                        }catch (Exception e){
                            Toast.makeText(mainContext,R.string.open_file_error,Toast.LENGTH_SHORT).show();
                        }
                        if (isSetupNewGame){
                            isSetupNewGame = false;
                            setup_newGame_View.setVisibility(INVISIBLE);
                            invalidateOptionsMenu();
                        }
                    }
                });
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.cover_border_background);
                loadFilesPopWindow.setBackgroundDrawable(drawable);
                loadFilesPopWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER,0,0);
            }
        }else{
            showAlert(getString(R.string.permission));
        }
    }


    private void setupPattern(){
        if (PERMISSIONS_EXTERNAL_STORAGE){
            if (!cacheDir.exists()){
                cacheDir.mkdir();
            }
            final TextView textView = new TextView(this);
            textView.setText(R.string.dialog_pattern);
            textView.setMovementMethod(LinkMovementMethod.getInstance()); // this is important to make the links clickable
            final AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setPositiveButton("OK",null)
                    .setView(textView)
                    .create();
            alertDialog.show();
        }else {
            showAlert(getString(R.string.permission));
        }
    }
    private void getPermission(){
        if (Build.VERSION.SDK_INT >= 23){
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    ;

                }else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                }
            }else{
                PERMISSIONS_EXTERNAL_STORAGE = true;;
            }
        }else {
            PERMISSIONS_EXTERNAL_STORAGE = true;
            if(!cacheDir.exists())
                cacheDir.mkdirs();
        }
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    PERMISSIONS_EXTERNAL_STORAGE = true;
                    if(!cacheDir.exists())
                        cacheDir.mkdirs();
                }
            }
            break;


        }
    }


    @Override
    public void startInitEngine(){
        final TextView textView = (TextView)findViewById(R.id.loadingText);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (menu!=null)
                    menu.setGroupEnabled(0,false);
                loadingView.setVisibility(VISIBLE);
                if (textView != null){
                    textView.setText(R.string.start_engine);
                    textView.setVisibility(View.VISIBLE);
                }

            }
        });
    }
    @Override
    public void endInitEngine(){
        final TextView textView =  (TextView)findViewById(R.id.loadingText);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (menu!=null)
                 menu.setGroupEnabled(0,true);
                loadingView.setVisibility(INVISIBLE);
                if (textView != null){
                    textView.setVisibility(INVISIBLE);
                }
            }
        });
    }
    @Override
    public void StartThinking(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (menu!=null)
                    menu.setGroupEnabled(0,false);
                loadingView.setVisibility(View.VISIBLE);
                disable((ViewGroup)findViewById(R.id.controlPanel),false);

//                ((MainActivity)mainContext).findViewById(R.id.controlPanel).setFocusable(false);
//                ((MainActivity)mainContext).findViewById(R.id.controlPanel).setEnabled(false);


            }
        });
    }
    @Override
    public void endThinking(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                menu.setGroupEnabled(0,true);
                loadingView.setVisibility(INVISIBLE);
                disable((ViewGroup)findViewById(R.id.controlPanel),true);
               // ((MainActivity)mainContext).findViewById(R.id.controlPanel).setEnabled(true);
            }
        });
    }

    public void toCancelNewGame(View v){
        if (isSetupNewGame){
            isSetupNewGame = false;
            setup_newGame_View.setVisibility(INVISIBLE);
            invalidateOptionsMenu();
        }
    }
    public void toDoneNewGame(View v){
        if (isSetupNewGame){
            isSetupNewGame = false;
            setup_newGame_View.setVisibility(INVISIBLE);
            startNewGame();
            invalidateOptionsMenu();
        }
    }


    public void toNewGame(View v){
        isSetupNewGame = true;
        setup_newGame_View.setVisibility(View.VISIBLE);
        setupNewGame();
        invalidateOptionsMenu();
    }

    public void toUndo(View v){
        if (game!=null)
            game.undo();
    }
    public void toPre(View v){
        if (loadingView.getVisibility() ==View.VISIBLE) return;
        if (game!= null){
            game.updateBoardView(Replay_Action.Pre);
        }

    }
    public void toNext(View v){
        if (loadingView.getVisibility() ==View.VISIBLE) return;
        if (game!= null){
            game.updateBoardView(Replay_Action.Next);
        }
    }
    public void toStart(View v){
        if (loadingView.getVisibility() ==View.VISIBLE) return;
        if (game!= null){
            game.updateBoardView(Replay_Action.Start);
        }
    }

    public void toEnd(View v){
        if (loadingView.getVisibility() ==View.VISIBLE) return;
        if (game!= null){
            game.updateBoardView(Replay_Action.End);
        }

    }
    public void toCloseTerritory(View v){
        boardView.showTerritory = false;
        if (!game.gameFinished)
            boardView.touchScreenAllowed = true;
        findViewById(R.id.territory_view).setVisibility(INVISIBLE);
        findViewById(R.id.player_info_view).setVisibility(VISIBLE);

        boardView.invalidate();

    }

    public void toReplay(View v){
        Button toReplayBtn = (Button)v;
        View play_game_controller = findViewById(R.id.play_game_controller);
        View replay_controller = findViewById(R.id.replay_controller);
        if (play_game_controller.getVisibility()==View.VISIBLE){
            game.copyTryList();
            game.istry = true;
            play_game_controller.setVisibility(View.GONE);
            replay_controller.setVisibility(VISIBLE);
            toReplayBtn.setText(R.string.replay_end);
        }else{
            game.istry = false;
            game.finishTryGame();
            play_game_controller.setVisibility(View.VISIBLE);
            replay_controller.setVisibility(View.GONE);
            toReplayBtn.setText(R.string.replay);
            if (!game.gameFinished)
                boardView.touchScreenAllowed = true;

        }
    }


    @Override
    public void finishGame(){
        game.getFinalScore();
    }

    public void territory(View v){
        if (game== null) return;
        StartThinking();
        boardView.touchScreenAllowed = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
               final int[] scores = game.getInfluence();
               runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       if (scores != null){
                           int b_m = scores[0];
                           int w_m = scores[1];
                           int b_captures = scores[2];
                           int w_captures = scores[3];
                           String s;
                           TextView b_total_m = (TextView)findViewById(R.id.m_black_score);
                           s = b_m+getString(R.string.point);
                           b_total_m.setText(s);
                           TextView w_total_m = (TextView)findViewById(R.id.m_white_score);
                           s = w_m+getString(R.string.point);
                           w_total_m.setText(s);
                           TextView b_e_m = (TextView)findViewById(R.id.m_black_eat);
                           s = b_captures+getString(R.string.point);
                           b_e_m.setText(s);
                           TextView w_e_m = (TextView)findViewById(R.id.m_white_eat);
                           s = w_captures+getString(R.string.point);
                           w_e_m.setText(s);
                           TextView w_back_m = (TextView)findViewById(R.id.m_white_back);
                           s = game.gameInfo.Komi+getString(R.string.point);
                           w_back_m.setText(s);

                           float score  = b_m-w_m+b_captures-w_captures-Float.parseFloat(game.gameInfo.Komi);
                           TextView torritory = (TextView)findViewById(R.id.territory_score);
                           s = (score>0?getString(R.string.black):getString(R.string.white)) +getString(R.string.leads)+ abs(score) + getString(R.string.point);
                           torritory.setText(s);
                           boardView.showTerritory = true;

                           findViewById(R.id.territory_view).setVisibility(VISIBLE);
                           findViewById(R.id.player_info_view).setVisibility(INVISIBLE);

                           boardView.invalidate();
                       }else{
                           if (!game.gameFinished)
                               boardView.touchScreenAllowed = true;
                       }
                       endThinking();
                   }
               });
            }
        }).start();




    }
    public void pass(View v){
        if (loadingView.getVisibility() ==View.VISIBLE) return;
        if (game != null){
            game.pass();
            Toast.makeText(this,getString(R.string.board_player_passes,getString(R.string.player)), Toast.LENGTH_SHORT).show();

        }
    }


    public void updateGameInfo(final int moveNumber){
        final TextView l_player = (TextView) findViewById(R.id.player1);
        final TextView r_player = (TextView) findViewById(R.id.player2);
        final TextView l_eat = (TextView) findViewById(R.id.player1_prisoners);
        final TextView r_eat = (TextView) findViewById(R.id.player2_prisoners);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (game ==null){
                    try {
                        wait(100);
                    }catch (Exception e){
                        e.printStackTrace();
                        return;
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        int playerColor = game.humanColor;
                        String s;
                        s = getString(R.string.board_bot_level) + game.level;
                        if (playerColor == BLACK){
                            l_player.setText(R.string.you);
                            r_player.setText(s);

                        }else{
                            r_player.setText(R.string.you);
                            l_player.setText(s);

                        }
                        s = getString(R.string.eat) + boardView.black_captures;
                        l_eat.setText(s);
                        s = getString(R.string.eat) + boardView.white_captures;
                        r_eat.setText(s);

                        if (getSupportActionBar()!= null)
                            getSupportActionBar().setSubtitle((moveNumber > 0) ?
                                    getString(R.string.board_move_number, moveNumber) : getString(R.string.board_no_moves));
                    }
                });
            }
        }).start();


    }

    private void showAlert(String message){
        AlertDialog alertDialog = new AlertDialog.Builder(mainContext)
                .setPositiveButton("OK",null)
                .setMessage(message)
                .create();
        alertDialog.show();
    }

    private void initAds(int resourceID){
        MobileAds.initialize(this, initializationStatus -> {

        });
        AdView mAdView = (AdView) findViewById(resourceID);
        if (mAdView!=null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }
    }
    void  feedback(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        final EditText edittext = new EditText(this);
        edittext.setLines(5);
        layout.setPadding(10, 0, 10, 0);
        layout.addView(edittext);

        edittext.setHint(R.string.putMessage);
        alert.setTitle(getString(R.string.feedback) );
        alert.setView(layout);
        alert.setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                edittext.setSelected(false);
                String text = edittext.getText().toString();
                String uniqueID;
                if(!text.trim().equals("")){
                    SharedPreferences settings = PreferenceManager
                            .getDefaultSharedPreferences(getApplicationContext());
                    uniqueID = settings.getString("uniqueID","");
                    if (uniqueID.equals("")){
                        uniqueID = UUID.randomUUID().toString();
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("uniqueID",uniqueID);
                        editor.apply();
                    }
                    DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_hhmmss", Locale.US);
                    uniqueID +=   dateFormat.format(new Date());
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference writeRef = database.getReferenceFromUrl("https://go-game-be8dc.firebaseio.com/"); //Getting root reference
                    Map<String,String> report = new HashMap();
                    report.put("uniqueID",uniqueID);
                    report.put("comment",text);
                    writeRef.child("GoGameFeedback").push().setValue(report);
                }
            }
        });

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                edittext.setSelected(false);   // what ever you want to do with No option.
            }
        });

        alert.show();

    }


    private void disable(ViewGroup layout, boolean enable) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            child.setEnabled(enable);
            if (child instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) child;

                for (int j = 0; j < group.getChildCount(); j++) {
                    group.getChildAt(j).setEnabled(enable);
                }
            }

        }
    }









}
