package com.virun.customloading;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.res.ResourcesCompat;

public class CustomLoading extends AppCompatImageView {

    Context context;
    Drawable mLoadingImage;
    int delay=60;
    Handler handler = new Handler();
    Runnable runnable;
    @DrawableRes
    int[] imgs={R.drawable.img_loading01,R.drawable.img_loading02,R.drawable.img_loading03,
            R.drawable.img_loading04,R.drawable.img_loading05,R.drawable.img_loading06,
            R.drawable.img_loading07,R.drawable.img_loading08,R.drawable.img_loading09,
            R.drawable.img_loading10,R.drawable.img_loading11};

    public CustomLoading(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public CustomLoading(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomLoading(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    //initailize components
    private void init() {

        mLoadingImage = ResourcesCompat.getDrawable(getResources(), R.drawable.img_loading01, null);

    }
    //start loading
    public void startLoading(){
        handler.postDelayed(
                runnable = new Runnable() {
                    int i=0;
                    @Override
                    public void run() {
                        //do something
                        if (i>=imgs.length){
                            i=0;
                        }
                        setImageResource(imgs[i]);
                        Log.d("run", "run: "+i);
                        i++;

                        handler.postDelayed(this, delay);
                    }
        }, delay);

    }
    //stop loading
    public void stopLoading(){
        handler.removeCallbacks(runnable);
    }

}
