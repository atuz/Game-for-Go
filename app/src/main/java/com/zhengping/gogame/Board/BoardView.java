package com.zhengping.gogame.Board;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

import com.zhengping.gogame.Object.Stone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * Created by user on 2017-03-07.
 */

public class BoardView extends ProblemView {
    private boolean touching = false;
    byte[][] shadowBoard;
    long scaleTime =0;
    int scaleBase = 0;
    float mScaleFactor = 1.f;
    Point focusPoint = null;
    byte nextColor = 0;
    private Stone moveStone;
    Stone lastDead = null;
    Stone lastStone = null;

    public boolean touchScreenAllowed = true;
    private ScaleGestureDetector mScaleDetector;
    public BoardViewTouchListener boardViewTouchListener;

    public interface BoardViewTouchListener {
        void tapScreen(Stone stone);
    }
    public BoardView(Context context) {
        super(context);

    }

    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());


    }
    @Override
    public void onDraw(Canvas canvas) {
        canvas.save();
        switch (scaleBase){
            case 0:
                canvas.translate(0, 0);
                break;
            case 1:
                canvas.translate(-(mScaleFactor - 1) * (width), 0);
                break;
            case 2:
                canvas.translate(0, -(mScaleFactor - 1) * (width));
                break;
            default:
                canvas.translate(-(mScaleFactor - 1) * (width), -(mScaleFactor - 1) * (width));
                break;
        }

//        if (focusPoint != null)
//        canvas.translate(-(mScaleFactor - 1) * focusPoint.x, -(mScaleFactor - 1) * focusPoint.y);
        canvas.scale(mScaleFactor, mScaleFactor);
        super.onDraw(canvas);
        if (touching && touchScreenAllowed) {
            currentPoint(canvas);
        }
        if (lastStone != null && !showTerritory){
            if (trueBoard[lastStone.point.x][lastStone.point.y] != BLANK) {

                Point c = lastStone.point;
                if (c.x >= 0 && c.y >= 0) {
                    float x = x2Screen(c.x);
                    float y = y2Screen(c.y);
                    int color;
                    if (trueBoard[c.x][c.y] == BLACK) {
                        color = Color.WHITE;
                    } else {
                        color = Color.BLACK;
                    }
                    paint.setColor(color);
                    canvas.drawCircle(x, y, cellWidth / 6, paint);
                }
            }
        }
        canvas.restore();
    }

    @Override

    public boolean onTouchEvent(MotionEvent event) {
        if (!touchScreenAllowed){
            return true;
        }
        mScaleDetector.onTouchEvent(event);

        int pointerCount = event.getPointerCount();
        if (pointerCount != 1) {
            touching = false;
            scaleTime= System.currentTimeMillis();
            this.invalidate();
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE: {
                long time= System.currentTimeMillis();
                if (time - scaleTime < 500) return  true;
                float xf ;
                float yf ;
                switch (scaleBase){
                    case 0:
                        xf = event.getX() / mScaleFactor;
                        yf = event.getY() / mScaleFactor;
                        break;
                    case 1:
                        xf = (event.getX() + (mScaleFactor - 1) * width) / mScaleFactor;
                        yf = event.getY() / mScaleFactor;
                        break;
                    case 2:
                        xf = event.getX()/ mScaleFactor;
                        yf = (event.getY() + (mScaleFactor - 1) * width) / mScaleFactor;
                        break;
                    default:
                        xf = (event.getX() + (mScaleFactor - 1) * width) / mScaleFactor;
                        yf = (event.getY() + (mScaleFactor - 1) * width) / mScaleFactor;;
                        break;
                }

                int x = x2Coordinate(xf);
                int y = y2Coordinate(yf);
                if ((x >= 0 && x < nSize) && (y >= 0 && y < nSize)) {

                    Point c = new Point(x, y);
                    moveStone = new Stone(c, nextColor);
                }
                touching = true;
                this.invalidate();

                break;
            }
            case MotionEvent.ACTION_UP: {
                long time= System.currentTimeMillis();
                if (time - scaleTime < 500) return  true;
                float xf ;
                float yf ;
                switch (scaleBase){
                    case 0:
                        xf = event.getX() / mScaleFactor;
                        yf = event.getY() / mScaleFactor;
                        break;
                    case 1:
                        xf = (event.getX() + (mScaleFactor - 1) * width) / mScaleFactor;
                        yf = event.getY() / mScaleFactor;
                        break;
                    case 2:
                        xf = event.getX()/ mScaleFactor;
                        yf = (event.getY() + (mScaleFactor - 1) * width) / mScaleFactor;
                        break;
                    default:
                        xf = (event.getX() + (mScaleFactor - 1) * width) / mScaleFactor;
                        yf = (event.getY() + (mScaleFactor - 1) * width) / mScaleFactor;;
                        break;
                }
                int x = x2Coordinate(xf);
                int y = y2Coordinate(yf);
                touching = false;
                if ((x >= 0 && x < nSize) && (y >= 0 && y < nSize)) {
                    Point c = new Point(x + boardminx - 1, y + boardminy - 1);
                    final Stone stone = new Stone(c, nextColor);
                    handleTouchPoint(stone);
                }
                this.invalidate();
                break;
            }
            default:
                break;
        }


        return true;
    }
    void currentPoint(Canvas canvas) {
        paint.setStrokeWidth(xOffset);
        paint.setColor(0x22000000);
        int x = moveStone.point.x;
        int y = moveStone.point.y;
        canvas.drawLine(x2Screen(x), y2Screen(0), x2Screen(x),
                y2Screen(nSize - 1), paint);
        canvas.drawLine(x2Screen(0), y2Screen(y), x2Screen(nSize - 1),
                y2Screen(y), paint);
  if (nextColor == BLACK) {
            currImg = blackImg;
        } else {
            currImg = whiteImg;
        }


        x = (int) (x2Screen(x) - cellWidth / 2);
        y = (int) (y2Screen(y) - cellWidth / 2);
        Rect rectangle = new Rect(x, y, (int) (x + cellWidth), (int) (y + cellWidth));
        Paint apphapaint = new Paint();
        if (nextColor == BLACK)
            apphapaint.setAlpha(50);
        else
            apphapaint.setAlpha(80);
        //you can set your transparent value here
        canvas.drawBitmap(currImg, null, rectangle, apphapaint);
    }
    public void handleTouchPoint(Stone stone) {
        if (boardViewTouchListener != null) {
            boardViewTouchListener.tapScreen(stone);
        }
    }
    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (mScaleFactor >= 1f && mScaleFactor<=1.02) {
                Point point = new Point((int)detector.getFocusX(),(int)detector.getFocusY());
                focusPoint = point;
                if (point.x <=width/2 &&point.y <=width/2) {
                    scaleBase =0;
                }else if(point.x >width/2 &&point.y <=width/2){
                    scaleBase =1;
                }else if(point.x <=width/2 &&point.y >width/2){
                    scaleBase =2;
                }else{
                    scaleBase =3;
                }
            }

            detector.getFocusX();
            detector.getFocusY();
            mScaleFactor *= detector.getScaleFactor();
            System.out.println( detector.getScaleFactor()+" : " +mScaleFactor );

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(1f, Math.min(mScaleFactor, 2.0f));
            return true;
        }
    }
    public boolean canPutStone(final Stone stone) // 落子的算法
    {
        boolean canDown = false;
        if (!legalPoint(stone.point)) {
            return false;
        }
        if (trueBoard[stone.point.x][stone.point.y] != 0) {
            return false;
        }
        Set capturedStones = new HashSet();
        makeShadow();
        shadowBoard[stone.point.x][stone.point.y] = stone.color;
        Set group = floodFill(stone.point);
        if (hasLiberties(group)) {
            canDown = true;
        } else {
            boolean cret = canEat(stone, capturedStones);
            if (cret) {
                if (!isJieX(stone.point, capturedStones)) {
                    canDown = true;
                }
            }
        }
        return canDown;
    }
    private boolean isJieX(Point point, Set capturedStones) {
        if (capturedStones.size() == 1) {
            if (lastDead != null) {
                if ((point.x == lastDead.point.x) && (point.y == lastDead.point.y)) {
                    return true;
                }
            }
        }
        return false;
    }
    public boolean placeStone(final Stone stone){
        trueBoard[stone.point.x][stone.point.y] = stone.color;
        this.invalidate();
        return true;

    }
    public boolean putStone(final Stone stone) // 落子的算法
    {
        boolean canDown = false;
        if (!legalPoint(stone.point)) {
            return false;
        }
        if (trueBoard[stone.point.x][stone.point.y] != 0) {
            return false; //TODO 此处已有棋子
        }
        Set capturedStones = new HashSet();
        makeShadow();
        shadowBoard[stone.point.x][stone.point.y] = stone.color;
        Set group = floodFill(stone.point);
        if (hasLiberties(group)) {
            canDown = true;
            canEat(stone, capturedStones);
        } else {
            boolean cret = canEat(stone, capturedStones);
            if (cret) {
                if (!isJieX(stone.point, capturedStones)) {
                    canDown = true;
                }
            }
        }
        if (canDown) {
            if (capturedStones.size() == 1) {
                Point point = (Point) capturedStones.iterator().next();
                lastDead = new Stone(point, stone.color);
            } else {
                lastDead = null;
            }
            if (stone.color ==BLACK)
               black_captures += capturedStones.size();
            else
               white_captures += capturedStones.size();
            cleanDeadBody(capturedStones);
            trueBoard[stone.point.x][stone.point.y] = stone.color;
            nextColor = getOppositeColor(stone.color);
            this.invalidate();
        }
        if (canDown) lastStone = stone;
        return canDown;
    }
    private boolean canEat(Stone stone, Set capturedStones) {
        boolean ret = false;
        char antiColor = 2;
        if (stone.color == 2) {
            antiColor = 1;
        }

        int x = stone.point.x;
        int y = stone.point.y;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if ((dx + dy != 1) && (dx + dy != -1)) continue;
                if (getShadowPoint(x + dx, y + dy) == antiColor) {
                    Set group = floodFill(new Point(x + dx, y + dy));
                    if (!hasLiberties(group)) {
                        for (Object aGroup : group) {
                            ret = true;
                            int[] point = (int[]) aGroup;
                            capturedStones.add(new Point(point[0], point[1]));
                        }
                    }
                }
            }
        }
        return ret;
    }

    private boolean hasLiberties(Set points) {
        Iterator it = points.iterator();
        while (it.hasNext()) {
            int[] point = (int[]) it.next();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if ((dx + dy != 1) && (dx + dy != -1)) continue;
                    if (getShadowPoint(point[0] + dx, point[1] + dy) != BLANK) continue;
                    return true;
                }
            }
        }
        return false;
    }
    private void makeShadow() {
        int i, j;
        for (i = 0; i < x_LINE_NUM; i++) {
            for (j = 0; j < y_LINE_NUM; j++) {
                shadowBoard[i][j] = trueBoard[i][j];
            }
        }
    }
    private Set floodFill(Point stonePoint) {
        int x = stonePoint.x;
        int y = stonePoint.y;
        Set group = new HashSet();
        LinkedList points = new LinkedList();
        points.add(new int[]{x, y});
        while (!points.isEmpty()) {
            int[] point = (int[]) points.removeFirst();
            group.add(point);
            for (int dx = -1; dx <= 1; dx++) {
                inner:
                for (int dy = -1; dy <= 1; dy++) {
                    if ((dx + dy != 1) && (dx + dy != -1)) continue;
                    if (getShadowPoint(point[0] + dx, point[1] + dy) != getShadowPoint(point[0], point[1]))
                        continue;

                    Iterator it = group.iterator();
                    while (it.hasNext()) {
                        int[] p = (int[]) it.next();
                        if ((p[0] == point[0] + dx) && (p[1] == point[1] + dy)) {
                            continue inner;
                        }
                    }

                    points.addLast(new int[]{point[0] + dx, point[1] + dy});
                }
            }
        }
        return group;
    }
    public int getShadowPoint(int x, int y) {
        if (x >= x_LINE_NUM || x < 0 || y >= y_LINE_NUM || y < 0)
            return OFF_BOARD;
        return shadowBoard[x][y];
    }
    private void cleanDeadBody(Set capturedStones) {
        for (Object capturedStone : capturedStones) {
            Point point = (Point) capturedStone;
            trueBoard[point.x][point.y] = BLANK;
        }
    }
    public void clearBoard() {
        super.clearBoard();
        lastDead = null;
        moveStone = null;
        lastStone = null;
        black_captures = 0;
        white_captures = 0;
        invalidate();
    }
    public byte getOppositeColor(byte color){
        return color == WHITE?BLACK:WHITE;
    }
    public void setNextColor(byte color){
       nextColor = color;
    }
    public void setBoardSize(int boardSize, int minLine,int maxLine){
        super.setBoardSize(boardSize,minLine,maxLine);
        shadowBoard = new byte[x_LINE_NUM][x_LINE_NUM];
        clearBoard();

    }
    public void scaleView() {
        new Thread(new Runnable() {
            float dec = (mScaleFactor-1)/20;
            @Override
            public void run() {
                // TODO Auto-generated method stub
                for (int i = 0; i < 20; i++) {
                    mScaleFactor -= dec;
                    postInvalidate();
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            }
        }).start();
    }

}
