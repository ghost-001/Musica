package com.example.ayush.musica.utility;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.view.View;

public class BlurBuilder {
    private static final float BITMAP_SCALE = 0.6f;
    private static final float BLUR_RADIUS = 9.5f;
/*
    public static Bitmap blurImage(View v,Context context) {
        return blurImage(context, getScreenshot(v));
    }*/
    public static Bitmap blur(View v, Context context) {
        Bitmap bb = getScreenshot(v);
        return blurImage(context, bb);
    }

    public static Bitmap blurImage(Context context, Bitmap inputBit) {
         final float BLUR_RADIUS = 25f;
         int width = Math.round(inputBit.getWidth() * BITMAP_SCALE);
        int height = Math.round(inputBit.getHeight() * BITMAP_SCALE);

        // Bitmap inputBitmap = Bitmap.createScaledBitmap(inputBit, width, height, false);
        Bitmap outputBitmap = Bitmap.createBitmap(inputBit);
        RenderScript rs = RenderScript.create(context);

        Bitmap blurBitmap = inputBit.copy(Bitmap.Config.ARGB_8888, true);
        //Bitmap.createBitmap(inputBit);


        Allocation input = Allocation.createFromBitmap(rs, blurBitmap, Allocation.MipmapControl.MIPMAP_FULL, Allocation.USAGE_SHARED);
        Allocation output = Allocation.createTyped(rs, input.getType());

        //Allocation input = Allocation.createFromBitmap(rs, inputBit);
        //Allocation output = Allocation.createFromBitmap(rs, blurBitmap);

        ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setInput(input);

        script.setRadius(BLUR_RADIUS);
        script.forEach(output);

        output.copyTo(blurBitmap);
        inputBit.recycle();
        return blurBitmap;
    }

    private static Bitmap getScreenshot(View v) {
        Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.draw(c);
        return b;
    }
}
