package com.droidlogic.mediacenter.flip;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Scroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import java.lang.reflect.Field;

public class FlipViewPager extends ViewPager {

    public FlipViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setChildrenDrawingOrderEnabled(true);
        FlipViewPagerScroller scroller = new FlipViewPagerScroller(context, 2000);
        try {
            Field field = ViewPager.class.getDeclaredField("mScroller");
            field.setAccessible(true);
            field.set(this, scroller);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.setOffscreenPageLimit(1);
        setPageTransformer(false, new Transformer());
    }

    @Override
    public void setOffscreenPageLimit(int limit) {
        //super.setOffscreenPageLimit(limit);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
//        Log.i("getChildDrawingOrder", "childCount:" + childCount + "--i:" + i);
        if (childCount == 3) {
            switch (i) {
                case 0:
                    return 0;
                case 1:
                    return 2;
                case 2:
                    return 1;
            }
        }
        return super.getChildDrawingOrder(childCount, i);
    }

    static class Transformer implements PageTransformer {

        @Override
        public void transformPage(@NonNull View view, float position) {
            final float SCALE = 0.6f;
            final float ALPHA = 0.8f;
            float scale = (position < 0) ? ((1 - SCALE) * position + 1) : ((SCALE - 1) * position + 1);
            float alpha = (position < 0) ? ((1 - ALPHA) * position + 1) : ((ALPHA - 1) * position + 1);
            if (position < 0) {
                view.setPivotX(view.getWidth());
            } else {
                view.setPivotX(0);
            }
            view.setPivotY(view.getHeight() / 2f);
            view.setScaleX(scale);
            view.setScaleY(scale);
            view.setAlpha(Math.abs(alpha));
        }
    }

    static class FlipViewPagerScroller extends Scroller {

        private int duration;

        public FlipViewPagerScroller(Context context, int duration) {
            super(context);
            this.duration = duration;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy) {
            startScroll(startX, startY, dx, dy, this.duration);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            super.startScroll(startX, startY, dx, dy, this.duration);
        }

    }

}
