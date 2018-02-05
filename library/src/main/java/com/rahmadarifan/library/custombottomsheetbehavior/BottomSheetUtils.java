package com.rahmadarifan.library.custombottomsheetbehavior;

import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

/**
 * Created by Ifan on 2/5/2018.
 */

public class BottomSheetUtils {

    public BottomSheetUtils() {
    }

    public static void setupViewPager(ViewPager viewPager) {
        View bottomSheetParent = findBottomSheetParent(viewPager);
        if (bottomSheetParent != null) {
            viewPager.addOnPageChangeListener(new BottomSheetUtils.BottomSheetViewPagerListener(viewPager, bottomSheetParent));
        }

    }

    private static View findBottomSheetParent(View view) {
        ViewParent parent;
        for (View current = view; current != null; current = parent != null && parent instanceof View ? (View) parent : null) {
            ViewGroup.LayoutParams params = current.getLayoutParams();
            if (params instanceof android.support.design.widget.CoordinatorLayout.LayoutParams && ((android.support.design.widget.CoordinatorLayout.LayoutParams) params).getBehavior() instanceof CustomBottomSheetBehavior) {
                return current;
            }

            parent = current.getParent();
        }

        return null;
    }


    private static class BottomSheetViewPagerListener extends ViewPager.SimpleOnPageChangeListener {
        private final View pager;
        private final CustomBottomSheetBehavior behavior;

        private BottomSheetViewPagerListener(ViewPager pager, View bottomSheetView) {
            this.pager = pager;
            this.behavior = CustomBottomSheetBehavior.from(bottomSheetView);
        }

        @Override
        public void onPageSelected(int position) {
            pager.post(new Runnable() {
                @Override
                public void run() {
                    behavior.invalidateScrollingChild();
                }
            });
        }
    }
}
