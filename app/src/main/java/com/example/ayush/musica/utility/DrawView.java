package com.example.ayush.musica.utility;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.view.View;

public class DrawView extends View {
    Paint paint = new Paint();
    Paint transparentPaint = new Paint();
    int colorBG = Color.parseColor("#ff0000");

    public DrawView(Context context) {
        super(context);
    }


    @Override
    public void onDraw(Canvas canvas) {
        //first fill everything with your covering color
        paint.setColor(colorBG);
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
        //now clear out the area you want to see through
        transparentPaint.setAlpha(0xFF);
        transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
          Rect rect=new Rect(25,25,25,25);//make this your rect!
        canvas.drawRect(rect,transparentPaint);
    }
}
