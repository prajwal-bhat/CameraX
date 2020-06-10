package com.apps.camerax.activity;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;

public abstract class SwipeMainActivity extends AppCompatActivity {


    private static final int SWIPE_THRESHOLD = 300;
    private static final int SWIPE_VELOCITY_THRESHOLD = 200;

    private GestureDetector gestureDetector;

    protected abstract void onSwipeRight();
    protected abstract void onSwipeLeft();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        gestureDetector = new GestureDetector( this, new SwipeDetector());
    }

    private class SwipeDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try{
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();
                if(Math.abs(diffX)>Math.abs(diffY)){
                    if(Math.abs(diffX)>SWIPE_THRESHOLD&&Math.abs(velocityX)>SWIPE_VELOCITY_THRESHOLD){
                        if(diffX>0){
                            onSwipeRight();
                        }else{
                            onSwipeLeft();
                        }
                        result = true;
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            return result;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // TouchEvent dispatcher.
        if (gestureDetector != null)
        {
            if (gestureDetector.onTouchEvent(ev))
                // If the gestureDetector handles the event, a swipe has been
                // executed and no more needs to be done.
                return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }
}

