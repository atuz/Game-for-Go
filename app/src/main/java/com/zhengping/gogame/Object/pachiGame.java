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

import android.content.Context;
import android.util.Log;
import com.zhengping.gogame.Board.BoardView;
import static com.zhengping.gogame.GoGameApplication.PERMISSIONS_EXTERNAL_STORAGE;
import static com.zhengping.gogame.GoGameApplication.cacheDir;
import static com.zhengping.gogame.GoGameApplication.dataFile;
import static com.zhengping.gogame.GoGameApplication.dir;
import static com.zhengping.gogame.Util.Util.getTotalRam;
public class pachiGame extends Game {
    private static final String TAG = "GoGamePachi";

    public pachiGame(Context context, int size, int hdcp, String komi, byte humanColor, int level, BoardView boardView){
        super(context,size,hdcp,komi,humanColor,level,boardView);
    }
    public pachiGame(Context context,String text,BoardView boardView){
        super(context,text,boardView);
    }
    @Override
    String getAIName(){
        return "pachi";
    }
    @Override
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