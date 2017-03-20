package com.zhengping.gogame.Board;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;


import com.zhengping.gogame.Object.Stone;
import com.zhengping.gogame.R;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * Created by user on 16-09-25.
 */
public class ProblemView extends View {
    public static final int OFF_BOARD = -1;
    public static final byte BLANK = 0;
    public static final byte BLACK = 1;
    public static final byte WHITE = 2;
    boolean rotateX = false;
    boolean rotateY = false;
    boolean rotateZ = false;
    Context context;
    public Paint paint;
    int width = 0;
    int x_LINE_NUM = 19;
    int y_LINE_NUM = 19;
    byte[][] trueBoard = new byte[x_LINE_NUM][y_LINE_NUM];
    public byte scoreBoard[][] = null;
    public boolean showTerritory = false;
    int xOffset, yOffset;
    int boardminx = 1, boardminy = 1, boardmaxx = 19, boardmaxy = 19;
    public int nSize = 19;
    int boardWidth = 2;
    int lineWidth = 2;
    float cellWidth;
    public boolean drawStep = false;
    public ArrayList<Stone> stones = new ArrayList<>();
    Bitmap blackImg, whiteImg, currImg;

    public ProblemView(Context mcontext) {
        super(mcontext);
        this.paint = new Paint();
        context = mcontext;
        blackImg = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.black_new);
        whiteImg = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.white_new);


    }

    public ProblemView(Context mcontext, AttributeSet attrs) {
        super(mcontext, attrs);
        this.paint = new Paint();
        context = mcontext;
        blackImg = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.black_new);
        whiteImg = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.white_new);

    }

    public void setBoardSize(int boardSize, int minLine,int maxLine){
        if (boardSize >19 || boardSize<4) return;
        nSize = boardSize;
        boardmaxx = maxLine;
        boardmaxy = maxLine;
        boardminx = minLine;
        boardminy = minLine;
        x_LINE_NUM = boardSize;
        y_LINE_NUM = boardSize;
        trueBoard = new byte[x_LINE_NUM][x_LINE_NUM];
    }
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBoard(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        int wh = Math.min(w, h);
        super.onSizeChanged(w, wh, oldw, oldh);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = getMeasuredWidth();
        setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth()); //Snap to width
    }

    public boolean legalPoint(Point point) {

        return (point.x >= 0 && point.x < x_LINE_NUM) && (point.y >= 0 && point.y < x_LINE_NUM);

    }

    public void drawBoard(Canvas mCanvas) {
        if (x_LINE_NUM > 1 && y_LINE_NUM > 1) {
            xOffset = width / nSize / 2;
            yOffset = xOffset;
            cellWidth = (((float) width - 2 * xOffset) / (nSize - 1));
            drawBg(mCanvas);
            drawLineGrid(mCanvas);
            drawStar(mCanvas);
            drawBlocks(mCanvas);
            drawStep(mCanvas);
            drawTerritory(mCanvas);


        }

        // drawFlag(g);
    }

    void drawTerritory(Canvas mCanvas){
        if (!showTerritory ) return;
        if (scoreBoard == null) return;
        mCanvas.save();
        for (int i=0;i<nSize;i++){
            for (int j=0;j<nSize;j++){
               if ((char)scoreBoard[i][j] =='X' ||(char)scoreBoard[i][j] =='x'){
                    if (trueBoard[i][j] != BLACK){
                        paint.setStyle(Paint.Style.FILL);
                        paint.setColor(Color.BLACK);
                        float x = x2Screen(i);
                        float y = y2Screen(j);
                        mCanvas.drawRect(x-cellWidth / 6,y-cellWidth / 6,x+cellWidth / 6,y+cellWidth / 6,paint);
                    }
                }
            }
        }
        for (int i=0;i<nSize;i++){
            for (int j=0;j<nSize;j++){
                if ((char)scoreBoard[i][j] =='O' ||(char)scoreBoard[i][j] =='o'){
                    if (trueBoard[i][j] != WHITE){
                        paint.setStyle(Paint.Style.FILL);
                        paint.setColor(Color.WHITE);
                        float x = x2Screen(i);
                        float y = y2Screen(j);
                        mCanvas.drawRect(x-cellWidth / 6,y-cellWidth / 6,x+cellWidth / 6,y+cellWidth / 6,paint);
                    }
                }
            }
        }
        for (int i=0;i<nSize;i++){
            for (int j=0;j<nSize;j++){
                if ((char)scoreBoard[i][j] ==','){
                    if (trueBoard[i][j] == WHITE){
                       // paint.setStyle(Paint.Style.STROKE);
                        paint.setColor(Color.BLACK);
                        float x = x2Screen(i);
                        float y = y2Screen(j);
                        paint.setTextSize(100);
                        paint.setTextAlign(Paint.Align.CENTER);
                        float size = cellWidth/2;
                        size = 100 * size / Math.max(size, paint.measureText("?"));
                        paint.setTextSize(size);
                        mCanvas.drawText("?", x, y - (paint.ascent() + paint.descent()) / 2
                                , paint);
                    }
                    if (trueBoard[i][j] ==BLACK){
                       // paint.setStyle(Paint.Style.STROKE);
                        paint.setColor(Color.WHITE);
                        float x = x2Screen(i);
                        float y = y2Screen(j);
                        paint.setTextSize(100);
                        paint.setTextAlign(Paint.Align.CENTER);
                        float size = cellWidth/2;
                        size = 100 * size / Math.max(size, paint.measureText("?"));
                        paint.setTextSize(size);
                        mCanvas.drawText("?", x, y - (paint.ascent() + paint.descent()) / 2
                                , paint);
                    }
                }
            }
        }
        mCanvas.restore();
   }
    void drawStep(Canvas mCanvas) {
        if(showTerritory) return;
        if (drawStep) {
            int i = 0;

            here:
            for (int j = 0; j < stones.size(); j++) {
                Stone stone = stones.get(j);
                i += 1;
                if (!legalPoint(stone.point)) continue;
                if (trueBoard[stone.point.x][stone.point.y] != BLANK) {


                    for (int k = j + 1; k < stones.size(); k++) {
                        Stone kStone = stones.get(k);
                        if (kStone.point.x == stone.point.x && kStone.point.y == stone.point.y) {
                            continue here;
                        }
                    }
                    Point c = stone.point;
                    if (c.x >= 0 && c.y >= 0) {
                        float x = x2Screen(c.x);
                        float y = y2Screen(c.y);
                        int color;
                        if (trueBoard[c.x][c.y] == BLACK) {
                            color = Color.WHITE;
                        } else {
                            color = Color.BLACK;
                        }

                        paint.setTextSize(100);
                        paint.setTextAlign(Paint.Align.CENTER);
                        paint.setColor(color);
                        float size = (float) (cellWidth * 0.5);
                        String textContent = i + "";
                        size = 100 * size / Math.max(size, paint.measureText(textContent));
                        paint.setTextSize(i<10?(float)(size*0.8):size);
                        mCanvas.drawText(textContent, x, y - (paint.ascent() + paint.descent()) / 2
                                , paint);

                    }
                }

            }
        } else {  ////draw circle
            for (int j = stones.size() - 1; j >= 0; j--) {
                Stone stone = stones.get(j);
                if (!legalPoint(stone.point)) continue;
                if (trueBoard[stone.point.x][stone.point.y] != BLANK) {

                    Point c = stone.point;
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
                        mCanvas.drawCircle(x, y, cellWidth / 6, paint);
                        return;
                    }
                }

            }
        }

    }

    void drawBg(Canvas mCanvas) {

    }

    void drawSuccessImage(Canvas mCanvas) {

    }

    private void drawBlocks(Canvas mCanvas) {
        // System.out.println("draw blocks");
        int pointX = 0, pointY = 0;
        for (int x = 0; x < x_LINE_NUM; x++) {
            for (int y = 0; y < y_LINE_NUM; y++) {
                if (trueBoard[x][y] != 0) {

                    pointX = rotateX ? boardmaxx - boardminx - x : x;
                    pointY = rotateY ? boardmaxy - boardminx - y : y;
                    Stone stone = new Stone(new Point(pointX, pointY), trueBoard[x][y]);
                    drawStone(stone, mCanvas);

                }
            }
        }
    }

    void clearBoard() {
        for (int i = 0; i < x_LINE_NUM; i++) {
            for (int j = 0; j < y_LINE_NUM; j++) {
                trueBoard[i][j] = 0;
            }
        }
    }

    public int getPoint(int x, int y) {
        if (x >= x_LINE_NUM || x < 0 || y >= y_LINE_NUM || y < 0)
            return OFF_BOARD;
        return trueBoard[x][y];
    }

    public void drawStone(Stone stone, Canvas mCanvas) {

        Point c = stone.point;
        // System.out.println("color=" + stone.color);
        if (stone.color == BLACK) {
            currImg = blackImg;
            paint.setColor(Color.BLACK);
        } else if (stone.color == WHITE) {
            currImg = whiteImg;
            paint.setColor(Color.WHITE);
        }

        float x = (c.x - boardminx + 1) * cellWidth - cellWidth / 2 + xOffset;
        float y = (c.y - boardminy + 1) * cellWidth - cellWidth / 2 + yOffset;
        Rect rectangle = new Rect((int) x, (int) y, (int) (x + cellWidth), (int) (y + cellWidth));
        // System.out.println("x = " + x + "  y = " + y + " cellwidth =" + cellWidth + " c.x =" + c.x + " imageH= " + currImg.getHeight() + " imageW= " + currImg.getWidth() + "rect = " + rectangle.left + " " + rectangle.top + "  " + rectangle.width() + " " + rectangle.height());
        mCanvas.drawBitmap(currImg, null, rectangle, paint);
        //canvas.drawBitmap(currImg, (float) x, (float) y, paint);


    }


    public Point[] createStar() {
        Point[] cs = null;
        if (nSize ==9){
            cs = new Point[5];

            int dao3 = x_LINE_NUM - 3;
            cs[0] = new Point(2, 2);
            cs[1] = new Point(dao3, 2);
            cs[2] = new Point(2, dao3);
            cs[3] = new Point(dao3, dao3);
            int zhong = x_LINE_NUM / 2;
            cs[4] = new Point(zhong, zhong);

        }else if(nSize == 13){
            cs = new Point[5];

            int dao3 = x_LINE_NUM - 4;
            cs[0] = new Point(3, 3);
            cs[1] = new Point(dao3, 3);
            cs[2] = new Point(3, dao3);
            cs[3] = new Point(dao3, dao3);
            int zhong = x_LINE_NUM / 2;
            cs[4] = new Point(zhong, zhong);
        }else if(nSize == 19){
            cs = new Point[9];

            int dao3 = x_LINE_NUM - 4;
            cs[0] = new Point(3, 3);
            cs[1] = new Point(dao3, 3);
            cs[2] = new Point(3, dao3);
            cs[3] = new Point(dao3, dao3);

            int zhong = x_LINE_NUM / 2;

            cs[4] = new Point(3, zhong);
            cs[5] = new Point(zhong, 3);
            cs[6] = new Point(zhong, dao3);
            cs[7] = new Point(dao3, zhong);

            cs[8] = new Point(zhong, zhong);
        }


        return cs;
    }

    private void drawStar(Canvas mCanvas) {
//7,17 ,9,19
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(1);
        Point[] cs = createStar();
        if (cs != null){
            for (Point c : cs) {
                if (c != null) {
                    c.x -= boardminx - 1;
                    c.y -= boardminy - 1;
                    if (c.x > 0 && c.y > 0) {
                        mCanvas.drawCircle(x2Screen(c.x), y2Screen(c.y), xOffset / 6, paint);
                    }

                }
            }
        }

    }

    private void drawLineGrid(Canvas mCanvas) {
        paint.setColor(Color.BLACK);
        for (int i = 0; i < nSize; i++) {
            drawVLine(i, mCanvas);
            drawHLine(i, mCanvas);
        }

    }


    void drawVLine(int i, Canvas mCanvas) {
        if ((i == 0 && boardminx != 1)) {

            return;
        }

        if ((i == boardmaxx - boardminx && boardmaxx != x_LINE_NUM)) {
            return;
        }

        if ((i == 0 && boardminx == 1) || (i == boardmaxx - boardminx && boardmaxx == x_LINE_NUM)) {
            paint.setStrokeWidth(boardWidth);
        } else {
            paint.setStrokeWidth(lineWidth);
        }
        mCanvas.drawLine(x2Screen(i), y2Screen(0), x2Screen(i),
                y2Screen(nSize - 1), paint);

    }

    void drawHLine(int i, Canvas canvas) {

        if ((i == 0 && boardminy != 1)) {

            return;
        }

        if ((i == boardmaxy - boardminy && boardmaxy != x_LINE_NUM)) {
            return;
        }

        if ((i == 0 && boardminy == 1) || (i == boardmaxy - boardminy && boardmaxy == x_LINE_NUM)) {
            paint.setStrokeWidth(boardWidth);

        } else {
            paint.setStrokeWidth(lineWidth);
        }
        canvas.drawLine(x2Screen(0), y2Screen(i), x2Screen(nSize - 1),
                y2Screen(i), paint);
    }



    public void setTerritory(byte[][] territory){
        scoreBoard = Arrays.copyOf(territory, nSize*nSize);
    }
    float x2Screen(int x) {
        return (x * cellWidth + xOffset);
    }

    float y2Screen(int y) {
        return (y * cellWidth + yOffset);
    }

    int x2Coordinate(float x) {
        return Math.round((x - xOffset) / cellWidth);
    }

    int y2Coordinate(float y) {
        return Math.round((y - yOffset) / cellWidth);
    }


}
