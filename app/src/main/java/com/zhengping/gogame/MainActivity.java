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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

import static android.view.View.VISIBLE;
import static com.zhengping.gogame.Board.ProblemView.BLACK;
import static com.zhengping.gogame.Board.ProblemView.WHITE;
import static com.zhengping.gogame.GoGameApplication.PERMISSIONS_EXTERNAL_STORAGE;
import static com.zhengping.gogame.GoGameApplication.cacheDir;
import static com.zhengping.gogame.R.string.player;
import static java.lang.Math.abs;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.zhengping.gogame.Board.BoardView;
import com.zhengping.gogame.Object.Game;
import com.zhengping.gogame.Object.Stone;
import com.zhengping.gogame.Object.pachiGame;
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
    final  private int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1002;
    Menu menu;
    Context mainContext;
    public Game game;
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
        boardView.isAIThinking = false;
        setup_newGame_View = findViewById(R.id.newGameView);
        loadingView = findViewById(R.id.loading);
        getPermission();
        setupLoadFileView();
        if (resumeGame()){
            isSetupNewGame = false;
            setup_newGame_View.setVisibility(View.INVISIBLE);
            invalidateOptionsMenu();
        }else{
            setupNewGame();
        }
        setTitle(getString(R.string.board_game_vs, "Pachi"));
        if (getSupportActionBar()!= null)
            getSupportActionBar().setSubtitle(" ");


    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        if (isSetupNewGame){
            getMenuInflater().inflate(R.menu.newgame, menu);
        }else{
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
            isSetupNewGame = false;
            setup_newGame_View.setVisibility(View.INVISIBLE);

            switch (i){
                case R.id.action_cancel:
                    break;
                case R.id.action_done:
                    startNewGame();
                    break;

                default:

          }
        }else{

            switch (i){
                case R.id.action_newGame:
                    saveGame();
                    isSetupNewGame = true;
                    setup_newGame_View.setVisibility(View.VISIBLE);
                    setupNewGame();
                    break;

                case R.id.action_undo:
                    if (game!=null)
                        game.undo();
                    break;

                case R.id.action_open_file:
                        openFile();
                    break;
                case R.id.action_pattern:
                        setupPattern();
                    break;
                case R.id.action_feedback:
                    feedback();
                    break;


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
            new Thread(new Runnable() {
                @Override
                public void run() {
                    game = new pachiGame(mainContext,savedGame,boardView);
                }
            }).start();
            return true;
        }
        return false;
    }

    private void setupNewGame(){

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
            final int level = _spn_level.getSelectedItemPosition() + 1;
            byte color;
            int colorPos = _spn_color.getSelectedItemPosition();
            if (colorPos == 0)
                color = BLACK;
            else
                color = WHITE;
            final byte humanColor = color;
            final int boardsize = Integer.parseInt((String) _spn_boardSize.getSelectedItem());
            final double komi = Double.parseDouble((String) _spn_komi.getSelectedItem());
            final int handicap = Integer.parseInt((String) _spn_handicap.getSelectedItem());

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putInt(_PREF_BOARDSIZE,_spn_boardSize.getSelectedItemPosition());
        editor.putInt(_PREF_KOMI,_spn_komi.getSelectedItemPosition());
        editor.putInt(_PREF_LEVEL,_spn_level.getSelectedItemPosition());
        editor.putInt(_PREF_COLOR,_spn_color.getSelectedItemPosition());
        editor.putInt( _PREF_HANDICAP,_spn_handicap.getSelectedItemPosition());
        editor.apply();

        new Thread(new Runnable() {
            @Override
            public void run() {
                game = new pachiGame(mainContext,boardsize,handicap,komi+"",humanColor,level,boardView);
            }
        }).start();




    }


    private void setupLoadFileView(){

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels;
        int width = displaymetrics.widthPixels;
        int actionBarHeight = 50 ;
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
        {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
        }
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
                        File file = new File(cacheDir,uri);
                        final String text;
                        try{
                            InputStream s = new FileInputStream(file);
                            text = IOUtils.toString(s, Charset.forName("UTF-8"));
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    game = new pachiGame(mainContext,text,boardView);
                                }
                            }).start();

                        }catch (Exception e){
                            Toast.makeText(mainContext,R.string.open_file_error,Toast.LENGTH_SHORT).show();
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

    public void Game(Context context,String uri){
        try {
            File file = new File(uri);
            InputStream s = new FileInputStream(file);
            String text = IOUtils.toString(s, Charset.forName("UTF-8"));
        }catch (Exception ignored){

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
                loadingView.setVisibility(View.INVISIBLE);
                if (textView != null){
                    textView.setVisibility(View.INVISIBLE);
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

            }
        });
    }
    @Override
    public void endThinking(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                menu.setGroupEnabled(0,true);
                loadingView.setVisibility(View.INVISIBLE);
            }
        });
    }
    public void toPre(View v){
        if (loadingView.getVisibility() ==View.VISIBLE) return;
        if (game!= null){
            game.updateBoardView(true);
        }

    }
    public void toNext(View v){
        if (loadingView.getVisibility() ==View.VISIBLE) return;
        if (game!= null){
            game.updateBoardView(false);
        }
    }

    public void territory(View v){
        if (game== null) return;
        if (loadingView.getVisibility() ==View.VISIBLE) return;
        if (boardView.showTerritory){
            boardView.showTerritory = false;
            boardView.invalidate();
            return;
        }
        game.finalScore();
        boardView.showTerritory = true;
        String result;
        if (boardView.scoreBoard!= null){
            int black = 0,white =0;
            for (int i=0;i<boardView.nSize;i++){
                for (int j=0;j<boardView.nSize;j++){
                    if ((char)boardView.scoreBoard[i][j] =='X' ||(char)boardView.scoreBoard[i][j] =='x'){
                        black ++;
                    }else if((char)boardView.scoreBoard[i][j] =='O' ||(char)boardView.scoreBoard[i][j] =='o'){
                        white ++;
                    }
                }
            }
            int hdcp = game.getGameHdcp();
            double komi = game.getGameKomi();
            double score = black - white - komi - hdcp;
            result = String.format(Locale.US, "%.1f", abs(score));
            result = getString(R.string.gtp_game_result,getString(score>0?R.string.black:R.string.white),result);
            showAlert(result);
        }
    }
    public void pass(View v){
        if (loadingView.getVisibility() ==View.VISIBLE) return;
        if (game != null){
            game.pass();
            Toast.makeText(this,getString(R.string.board_player_passes,getString(R.string.player)), Toast.LENGTH_SHORT).show();

        }
    }

    private void showAlert(String message){
        AlertDialog alertDialog = new AlertDialog.Builder(mainContext)
                .setPositiveButton("OK",null)
                .setMessage(message)
                .create();
        alertDialog.show();
    }

    private void initAds(int resourceID){
        MobileAds.initialize(this, "ca-app-pub-8278333774696675~1609399348");
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
                    Map<String,String> report = new HashMap<String,String>();
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

}
