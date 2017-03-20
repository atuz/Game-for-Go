package com.zhengping.gogame.Object;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import static com.zhengping.gogame.Board.ProblemView.BLACK;
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

public class GameInfo {
    String ver = "UGF3,200";
    String Lang = "UTF8";
    String Crypt = "0,PLAIN_UGF_FILE,READ_WRITE";
    String Code = "";
    String Title = "";
    String Place = "Go Game Android";
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd,hh:mm:ss", Locale.US);//2017/02/12,16:00:00
    String startDate = dateFormat.format(new Date());
    String Rule = "JPN";
    int Size = 19;
    int Hdcp = 0;
    String Komi = "6.5";
    String Ptime =  ""; //"N;0;10;30,N;0;10;30,0,0";
    String Winner= "";
    String Moves= "0";
    String Writer = "";
    String Copyright="";
    String CoordinateType = "IGS";
    String Comment = "";
    String PlayerB = "";
    String PlayerW = "";
    public void  parseUGI(Map<String,Object> properties){
        for (Map.Entry<String,Object> property : properties.entrySet()) {
            try {
                if (property.getKey().equals("Copyright")) {
                    Copyright = property.getValue().toString();
                    continue;
                }
                if (property.getKey().equals("Date")) {
                    startDate = property.getValue().toString();
                    continue;
                }

                if (property.getKey().equals("Title")) {
                    Title = property.getValue().toString();
                    continue;
                }

                if (property.getKey().equals("PlayerB")) {
                    PlayerB = property.getValue().toString();
                    continue;
                }

                if (property.getKey().equals("PlayerW")) {
                    PlayerW = property.getValue().toString();
                    continue;
                }
                if (property.getKey().equals("Place")) {
                    Place = property.getValue().toString();
                    continue;
                }

                if (property.getKey().equals("Winner")) {
                    Winner = property.getValue().toString();
                    continue;
                }


                if (property.getKey().equals("Rule")) {
                    Rule = property.getValue().toString();
                    continue;
                }

                if (property.getKey().equals("Hdcp")) {
                    String[] str = property.getValue().toString().split(",");
                    if (str.length !=2) continue;;
                    Hdcp = Integer.parseInt(str[0]);
                    Komi = str[1];
                }
                if (property.getKey().equals("Size")) {
                    Size = Integer.parseInt(property.getValue().toString());

                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }

    }
    public String gameString(ArrayList<Stone> gameList,ArrayList<String> ABList){//r.append(System.getProperty("line.separator"));
        String s = "[Header]" +System.getProperty("line.separator");
        s += "Ver="+ver +System.getProperty("line.separator");
        s += "Lang="+Lang +System.getProperty("line.separator");
        s += "Crypt=0,PLAIN_UGF_FILE,READ_WRITE" +System.getProperty("line.separator");
        s += "Code=" +System.getProperty("line.separator");
        s += "Title=" +Title +System.getProperty("line.separator");
        s += "Place=" +Place +System.getProperty("line.separator");
        s += "Date=" +startDate +System.getProperty("line.separator");
        s += "Rule=" +Rule +System.getProperty("line.separator");
        s += "Size=" +Size +System.getProperty("line.separator");
        s += "Hdcp=" +Hdcp +","+Komi +System.getProperty("line.separator");
        s += "Ptime="+System.getProperty("line.separator");
        s += "Winner=" + Winner+System.getProperty("line.separator");
        s += "Moves=0" + Winner+System.getProperty("line.separator");
        s += "Writer=" + Winner+System.getProperty("line.separator");
        s += "Copyright=" +Copyright+ Winner+System.getProperty("line.separator");
        s += "CoordinateType=" +CoordinateType+ Winner+System.getProperty("line.separator");
        s += "Comment=" + Winner+System.getProperty("line.separator");
        s += "PlayerB=" + PlayerB+System.getProperty("line.separator");
        s += "PlayerW=" + PlayerW+System.getProperty("line.separator");
        s += "[Data]" + System.getProperty("line.separator");
        for (int i=0;i< ABList.size();++i){
            String position = ABList.get(i);
            String color = "B1";

            s += position.toUpperCase()+","+color + ","+"0"+",0"+System.getProperty("line.separator");
        }
        for (int i=0;i< gameList.size();++i){
            Stone stone = gameList.get(i);
            String color = stone.color ==BLACK?"B1":"W1";
            if (stone.point.x==-1&&stone.point.y==-1){
                s += "YA"+","+color + ","+",0"+",0"+System.getProperty("line.separator");
            }else{
                char t1 = (char)(stone.point.x +'A');
                char t2;
                if (CoordinateType.equals("IGS")) {
                    t2 = (char)(Size - stone.point.y -1 +'A');
                }else{
                    t2 = (char)(stone.point.y +'A');
                }
                s += String.valueOf(t1)+String.valueOf(t2)+","+color + ","+(i+1+"")+",0"+System.getProperty("line.separator");
            }
        }
        return  s;
    }
}
