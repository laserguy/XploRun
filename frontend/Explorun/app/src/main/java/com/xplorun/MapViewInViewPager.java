package com.xplorun;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import org.osmdroid.views.MapView;
//import com.google.android.gms.maps.MapView;

public class MapViewInViewPager extends MapView {
    public MapViewInViewPager(Context context) {
        super(context);
    }

    public MapViewInViewPager(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

//    public MapViewInViewPager(Context context, AttributeSet attributeSet, int i) {
//        super(context, attributeSet, i);
//    }

//    public MapViewInViewPager(Context context, GoogleMapOptions googleMapOptions) {
//        super(context, googleMapOptions);
//    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Fix gesture conflict with ViewPager2
        getParent().requestDisallowInterceptTouchEvent(true);
        return super.dispatchTouchEvent(ev);
    }
}
