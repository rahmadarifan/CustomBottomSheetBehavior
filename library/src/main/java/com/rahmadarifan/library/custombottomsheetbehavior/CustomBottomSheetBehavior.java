package com.rahmadarifan.library.custombottomsheetbehavior;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPagerUtils;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ifan on 2/5/2018.
 */

public class CustomBottomSheetBehavior<V extends View> extends CoordinatorLayout.Behavior<V> {
    public static final int STATE_DRAGGING = 1;
    public static final int STATE_SETTLING = 2;
    public static final int STATE_EXPANDED = 3;
    public static final int STATE_COLLAPSED = 4;
    public static final int STATE_HIDDEN = 5;
    public static final int STATE_ANCHORED = 6;
    public static final int PEEK_HEIGHT_AUTO = -1;
    private static final float HIDE_THRESHOLD = 0.5F;
    private static final float HIDE_FRICTION = 0.1F;
    private static final float EXPAND_FRICTION = 0.3F;
    private static final float COLLAPSE_FRICTION = 0.2F;
    private float mMaximumVelocity;
    private int mPeekHeight;
    private boolean mPeekHeightAuto;
    private int mPeekHeightMin;
    private int mAnchorOffset;
    private boolean mAllowUserDragging = true;
    int mMinOffset;
    int mMaxOffset;
    boolean mHideable;
    private boolean mSkipCollapsed;
    int mState = 4;
    ViewDragHelper mViewDragHelper;
    private boolean mIgnoreEvents;
    private int mLastNestedScrollDy;
    private boolean mNestedScrolled;
    int mParentHeight;
    WeakReference<V> mViewRef;
    WeakReference<View> mNestedScrollingChildRef;
    private List<CustomBottomSheetBehavior.BottomSheetCallback> mCallbacks = new ArrayList(2);
    private VelocityTracker mVelocityTracker;
    int mActivePointerId;
    private int mInitialY;
    boolean mTouchingScrollingChild;
    private final ViewDragHelper.Callback mDragCallback = new ViewDragHelper.Callback() {
        public boolean tryCaptureView(View child, int pointerId) {
            if (CustomBottomSheetBehavior.this.mState == 1) {
                return false;
            } else if (CustomBottomSheetBehavior.this.mTouchingScrollingChild) {
                return false;
            } else {
                if (CustomBottomSheetBehavior.this.mState == 3 && CustomBottomSheetBehavior.this.mActivePointerId == pointerId) {
                    View scroll = (View) CustomBottomSheetBehavior.this.mNestedScrollingChildRef.get();
                    if (scroll != null && ViewCompat.canScrollVertically(scroll, -1)) {
                        return false;
                    }
                }

                return CustomBottomSheetBehavior.this.mViewRef != null && CustomBottomSheetBehavior.this.mViewRef.get() == child;
            }
        }

        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            CustomBottomSheetBehavior.this.dispatchOnSlide(top);
        }

        public void onViewDragStateChanged(int state) {
            if (state == 1) {
                CustomBottomSheetBehavior.this.setStateInternal(1);
            }

        }

        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int top;
            byte targetState;
            if (yvel < 0.0F) {
                if (CustomBottomSheetBehavior.this.shouldExpand(releasedChild, yvel)) {
                    top = CustomBottomSheetBehavior.this.mMinOffset;
                    targetState = 3;
                } else {
                    top = CustomBottomSheetBehavior.this.mAnchorOffset;
                    targetState = 6;
                }
            } else if (CustomBottomSheetBehavior.this.mHideable && CustomBottomSheetBehavior.this.shouldHide(releasedChild, yvel)) {
                top = CustomBottomSheetBehavior.this.mParentHeight;
                targetState = 5;
            } else if (yvel == 0.0F) {
                int currentTop = releasedChild.getTop();
                if (Math.abs(currentTop - CustomBottomSheetBehavior.this.mMinOffset) < Math.abs(currentTop - CustomBottomSheetBehavior.this.mMaxOffset)) {
                    top = CustomBottomSheetBehavior.this.mMinOffset;
                    targetState = 3;
                } else {
                    top = CustomBottomSheetBehavior.this.mMaxOffset;
                    targetState = 4;
                }
            } else if (CustomBottomSheetBehavior.this.shouldCollapse(releasedChild, yvel)) {
                top = CustomBottomSheetBehavior.this.mMaxOffset;
                targetState = 4;
            } else {
                top = CustomBottomSheetBehavior.this.mAnchorOffset;
                targetState = 6;
            }

            if (CustomBottomSheetBehavior.this.mViewDragHelper.settleCapturedViewAt(releasedChild.getLeft(), top)) {
                CustomBottomSheetBehavior.this.setStateInternal(2);
                ViewCompat.postOnAnimation(releasedChild, CustomBottomSheetBehavior.this.new SettleRunnable(releasedChild, targetState));
            } else {
                CustomBottomSheetBehavior.this.setStateInternal(targetState);
            }

        }

        public int clampViewPositionVertical(View child, int top, int dy) {
            return this.constrain(top, CustomBottomSheetBehavior.this.mMinOffset, CustomBottomSheetBehavior.this.mHideable ? CustomBottomSheetBehavior.this.mParentHeight : CustomBottomSheetBehavior.this.mMaxOffset);
        }

        private int constrain(int amount, int low, int high) {
            return amount < low ? low : (amount > high ? high : amount);
        }

        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return child.getLeft();
        }

        public int getViewVerticalDragRange(View child) {
            return CustomBottomSheetBehavior.this.mHideable ? CustomBottomSheetBehavior.this.mParentHeight - CustomBottomSheetBehavior.this.mMinOffset : CustomBottomSheetBehavior.this.mMaxOffset - CustomBottomSheetBehavior.this.mMinOffset;
        }
    };

    public CustomBottomSheetBehavior() {
    }

    public CustomBottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, android.support.design.R.styleable.BottomSheetBehavior_Layout);
        TypedValue value = a.peekValue(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight);
        if (value != null && value.data == -1) {
            this.setPeekHeight(value.data);
        } else {
            this.setPeekHeight(a.getDimensionPixelSize(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight, -1));
        }

        this.setHideable(a.getBoolean(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_hideable, false));
        this.setSkipCollapsed(a.getBoolean(android.support.design.R.styleable.BottomSheetBehavior_Layout_behavior_skipCollapsed, false));
        a.recycle();
        a = context.obtainStyledAttributes(attrs, R.styleable.CustomBottomSheetBehavior_Layout);
        this.mAnchorOffset = (int) a.getDimension(R.styleable.CustomBottomSheetBehavior_Layout_behavior_anchorOffset, 0.0F);
        this.mState = a.getInt(R.styleable.CustomBottomSheetBehavior_Layout_behavior_defaultState, this.mState);
        a.recycle();
        ViewConfiguration configuration = ViewConfiguration.get(context);
        this.mMaximumVelocity = (float) configuration.getScaledMaximumFlingVelocity();
    }

    void invalidateScrollingChild() {
        View scrollingChild = this.findScrollingChild((View) this.mViewRef.get());
        this.mNestedScrollingChildRef = new WeakReference(scrollingChild);
    }

    public Parcelable onSaveInstanceState(CoordinatorLayout parent, V child) {
        return new CustomBottomSheetBehavior.SavedState(super.onSaveInstanceState(parent, child), this.mState);
    }

    public void onRestoreInstanceState(CoordinatorLayout parent, V child, Parcelable state) {
        CustomBottomSheetBehavior.SavedState ss = (CustomBottomSheetBehavior.SavedState) state;
        super.onRestoreInstanceState(parent, child, ss.getSuperState());
        if (ss.state != 1 && ss.state != 2) {
            this.mState = ss.state;
        } else {
            this.mState = 4;
        }

    }

    public boolean onLayoutChild(CoordinatorLayout parent, V child, int layoutDirection) {
        if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
            ViewCompat.setFitsSystemWindows(child, true);
        }

        int savedTop = child.getTop();
        parent.onLayoutChild(child, layoutDirection);
        this.mParentHeight = parent.getHeight();
        int peekHeight;
        if (this.mPeekHeightAuto) {
            if (this.mPeekHeightMin == 0) {
                this.mPeekHeightMin = parent.getResources().getDimensionPixelSize(android.support.design.R.dimen.design_bottom_sheet_peek_height_min);
            }

            peekHeight = Math.max(this.mPeekHeightMin, this.mParentHeight - parent.getWidth() * 9 / 16);
        } else {
            peekHeight = this.mPeekHeight;
        }

        this.mMinOffset = Math.max(0, this.mParentHeight - child.getHeight());
        this.mMaxOffset = Math.max(this.mParentHeight - peekHeight, this.mMinOffset);
        if (this.mState == 3) {
            ViewCompat.offsetTopAndBottom(child, this.mMinOffset);
        } else if (this.mHideable && this.mState == 5) {
            ViewCompat.offsetTopAndBottom(child, this.mParentHeight);
        } else if (this.mState == 4) {
            ViewCompat.offsetTopAndBottom(child, this.mMaxOffset);
        } else if (this.mState != 1 && this.mState != 2) {
            if (this.mState == 6) {
                ViewCompat.offsetTopAndBottom(child, this.mAnchorOffset);
            }
        } else {
            ViewCompat.offsetTopAndBottom(child, savedTop - child.getTop());
        }

        if (this.mViewDragHelper == null) {
            this.mViewDragHelper = ViewDragHelper.create(parent, this.mDragCallback);
        }

        this.mViewRef = new WeakReference(child);
        this.mNestedScrollingChildRef = new WeakReference(this.findScrollingChild(child));
        return true;
    }

    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (child.isShown() && this.mAllowUserDragging) {
            int action = MotionEventCompat.getActionMasked(event);
            if (action == 0) {
                this.reset();
            }

            if (this.mVelocityTracker == null) {
                this.mVelocityTracker = VelocityTracker.obtain();
            }

            this.mVelocityTracker.addMovement(event);
            switch (action) {
                case 0:
                    int initialX = (int) event.getX();
                    this.mInitialY = (int) event.getY();
                    View scroll = (View) this.mNestedScrollingChildRef.get();
                    if (scroll != null && parent.isPointInChildBounds(scroll, initialX, this.mInitialY)) {
                        this.mActivePointerId = event.getPointerId(event.getActionIndex());
                        this.mTouchingScrollingChild = true;
                    }

                    this.mIgnoreEvents = this.mActivePointerId == -1 && !parent.isPointInChildBounds(child, initialX, this.mInitialY);
                    break;
                case 1:
                case 3:
                    this.mTouchingScrollingChild = false;
                    this.mActivePointerId = -1;
                    if (this.mIgnoreEvents) {
                        this.mIgnoreEvents = false;
                        return false;
                    }
                case 2:
            }

            if (!this.mIgnoreEvents && this.mViewDragHelper.shouldInterceptTouchEvent(event)) {
                return true;
            } else {
                View scroll = (View) this.mNestedScrollingChildRef.get();
                return action == 2 && scroll != null && !this.mIgnoreEvents && this.mState != 1 && !parent.isPointInChildBounds(scroll, (int) event.getX(), (int) event.getY()) && Math.abs((float) this.mInitialY - event.getY()) > (float) this.mViewDragHelper.getTouchSlop();
            }
        } else {
            this.mIgnoreEvents = true;
            return false;
        }
    }

    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (child.isShown() && this.mAllowUserDragging) {
            int action = MotionEventCompat.getActionMasked(event);
            if (this.mState == 1 && action == 0) {
                return true;
            } else {
                if (this.mViewDragHelper != null) {
                    this.mViewDragHelper.processTouchEvent(event);
                }

                if (action == 0) {
                    this.reset();
                }

                if (this.mVelocityTracker == null) {
                    this.mVelocityTracker = VelocityTracker.obtain();
                }

                this.mVelocityTracker.addMovement(event);
                if (action == 2 && !this.mIgnoreEvents && Math.abs((float) this.mInitialY - event.getY()) > (float) this.mViewDragHelper.getTouchSlop()) {
                    this.mViewDragHelper.captureChildView(child, event.getPointerId(event.getActionIndex()));
                }

                return !this.mIgnoreEvents;
            }
        } else {
            return false;
        }
    }

    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, V child, View directTargetChild, View target, int nestedScrollAxes) {
        if (!this.mAllowUserDragging) {
            return false;
        } else {
            this.mLastNestedScrollDy = 0;
            this.mNestedScrolled = false;
            return (nestedScrollAxes & 2) != 0;
        }
    }

    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, V child, View target, int dx, int dy, int[] consumed) {
        if (this.mAllowUserDragging) {
            View scrollingChild = (View) this.mNestedScrollingChildRef.get();
            if (target == scrollingChild) {
                int currentTop = child.getTop();
                int newTop = currentTop - dy;
                if (dy > 0) {
                    if (newTop < this.mMinOffset) {
                        consumed[1] = currentTop - this.mMinOffset;
                        ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                        this.setStateInternal(3);
                    } else {
                        consumed[1] = dy;
                        ViewCompat.offsetTopAndBottom(child, -dy);
                        this.setStateInternal(1);
                    }
                } else if (dy < 0 && !ViewCompat.canScrollVertically(target, -1)) {
                    if (newTop > this.mMaxOffset && !this.mHideable) {
                        consumed[1] = currentTop - this.mMaxOffset;
                        ViewCompat.offsetTopAndBottom(child, -consumed[1]);
                        this.setStateInternal(4);
                    } else {
                        consumed[1] = dy;
                        ViewCompat.offsetTopAndBottom(child, -dy);
                        this.setStateInternal(1);
                    }
                }

                this.dispatchOnSlide(child.getTop());
                this.mLastNestedScrollDy = dy;
                this.mNestedScrolled = true;
            }
        }
    }

    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target) {
        if (this.mAllowUserDragging) {
            if (child.getTop() == this.mMinOffset) {
                this.setStateInternal(3);
            } else if (target == this.mNestedScrollingChildRef.get() && this.mNestedScrolled) {
                int top;
                byte targetState;
                if (this.mLastNestedScrollDy > 0) {
                    if (this.shouldExpand(child, this.getYVelocity())) {
                        top = this.mMinOffset;
                        targetState = 3;
                    } else {
                        top = this.mAnchorOffset;
                        targetState = 6;
                    }
                } else if (this.mHideable && this.shouldHide(child, this.getYVelocity())) {
                    top = this.mParentHeight;
                    targetState = 5;
                } else if (this.mLastNestedScrollDy == 0) {
                    int currentTop = child.getTop();
                    if (Math.abs(currentTop - this.mMinOffset) < Math.abs(currentTop - this.mMaxOffset)) {
                        top = this.mMinOffset;
                        targetState = 3;
                    } else {
                        top = this.mMaxOffset;
                        targetState = 4;
                    }
                } else if (this.shouldCollapse(child, this.getYVelocity())) {
                    top = this.mMaxOffset;
                    targetState = 4;
                } else {
                    top = this.mAnchorOffset;
                    targetState = 6;
                }

                if (this.mViewDragHelper.smoothSlideViewTo(child, child.getLeft(), top)) {
                    this.setStateInternal(2);
                    ViewCompat.postOnAnimation(child, new CustomBottomSheetBehavior.SettleRunnable(child, targetState));
                } else {
                    this.setStateInternal(targetState);
                }

                this.mNestedScrolled = false;
            }
        }
    }

    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, V child, View target, float velocityX, float velocityY) {
        return !this.mAllowUserDragging ? false : target == this.mNestedScrollingChildRef.get() && (this.mState != 3 || super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY));
    }

    public final void setPeekHeight(int peekHeight) {
        boolean layout = false;
        if (peekHeight == -1) {
            if (!this.mPeekHeightAuto) {
                this.mPeekHeightAuto = true;
                layout = true;
            }
        } else if (this.mPeekHeightAuto || this.mPeekHeight != peekHeight) {
            this.mPeekHeightAuto = false;
            this.mPeekHeight = Math.max(0, peekHeight);
            this.mMaxOffset = this.mParentHeight - peekHeight;
            layout = true;
        }

        if (layout && this.mState == 4 && this.mViewRef != null) {
            V view = (V) this.mViewRef.get();
            if (view != null) {
                view.requestLayout();
            }
        }

    }

    public final int getPeekHeight() {
        return this.mPeekHeightAuto ? -1 : this.mPeekHeight;
    }

    public final void setAnchorOffset(int anchorOffset) {
        if (this.mAnchorOffset != anchorOffset) {
            this.mAnchorOffset = anchorOffset;
            if (this.mState == 6) {
                this.setStateInternal(2);
                this.setState(6);
            }
        }

    }

    public final int getAnchorOffset() {
        return this.mAnchorOffset;
    }

    public void setHideable(boolean hideable) {
        this.mHideable = hideable;
    }

    public boolean isHideable() {
        return this.mHideable;
    }

    public void setSkipCollapsed(boolean skipCollapsed) {
        this.mSkipCollapsed = skipCollapsed;
    }

    public boolean getSkipCollapsed() {
        return this.mSkipCollapsed;
    }

    public void addBottomSheetCallback(CustomBottomSheetBehavior.BottomSheetCallback callback) {
        this.mCallbacks.add(callback);
    }

    public void removeBottomSheetCallback(CustomBottomSheetBehavior.BottomSheetCallback callback) {
        this.mCallbacks.remove(callback);
    }

    public void setAllowUserDragging(boolean allowUserDragging) {
        this.mAllowUserDragging = allowUserDragging;
    }

    public boolean getAllowUserDragging() {
        return this.mAllowUserDragging;
    }

    public final void setState(final int state) {
        if (state != this.mState) {
            if (this.mViewRef == null) {
                if (state == 4 || state == 3 || state == 6 || this.mHideable && state == 5) {
                    this.mState = state;
                }

            } else {
                final V child = (V) this.mViewRef.get();
                if (child != null) {
                    ViewParent parent = child.getParent();
                    if (parent != null && parent.isLayoutRequested() && ViewCompat.isAttachedToWindow(child)) {
                        child.post(new Runnable() {
                            public void run() {
                                CustomBottomSheetBehavior.this.startSettlingAnimation(child, state);
                            }
                        });
                    } else {
                        this.startSettlingAnimation(child, state);
                    }

                }
            }
        }
    }

    public final int getState() {
        return this.mState;
    }

    void setStateInternal(int state) {
        if (this.mState != state) {
            this.mState = state;
            View bottomSheet = (View) this.mViewRef.get();
            if (bottomSheet != null) {
                for (int i = 0; i < this.mCallbacks.size(); ++i) {
                    ((CustomBottomSheetBehavior.BottomSheetCallback) this.mCallbacks.get(i)).onStateChanged(bottomSheet, state);
                }
            }

        }
    }

    private void reset() {
        this.mActivePointerId = -1;
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }

    }

    boolean shouldHide(View child, float yvel) {
        if (this.mSkipCollapsed) {
            return true;
        } else if (child.getTop() < this.mMaxOffset) {
            return false;
        } else {
            float newTop = (float) child.getTop() + yvel * 0.1F;
            return Math.abs(newTop - (float) this.mMaxOffset) / (float) this.mPeekHeight > 0.5F;
        }
    }

    boolean shouldExpand(View child, float yvel) {
        int currentTop = child.getTop();
        if (currentTop < this.mAnchorOffset) {
            return true;
        } else {
            float newTop = (float) currentTop + yvel * 0.3F;
            return newTop < (float) this.mAnchorOffset;
        }
    }

    boolean shouldCollapse(View child, float yvel) {
        int currentTop = child.getTop();
        if (currentTop > this.mAnchorOffset) {
            return true;
        } else {
            float newTop = (float) currentTop + yvel * 0.2F;
            return newTop > (float) this.mAnchorOffset;
        }
    }

    private View findScrollingChild(View view) {
        if (view instanceof NestedScrollingChild) {
            return view;
        } else {
            if (view instanceof ViewPager) {
                ViewPager viewPager = (ViewPager) view;
                View currentViewPagerChild = ViewPagerUtils.getCurrentView(viewPager);
                View scrollingChild = this.findScrollingChild(currentViewPagerChild);
                if (scrollingChild != null) {
                    return scrollingChild;
                }
            } else if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                int i = 0;

                for (int count = group.getChildCount(); i < count; ++i) {
                    View scrollingChild = this.findScrollingChild(group.getChildAt(i));
                    if (scrollingChild != null) {
                        return scrollingChild;
                    }
                }
            }

            return null;
        }
    }

    private float getYVelocity() {
        this.mVelocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
        return VelocityTrackerCompat.getYVelocity(this.mVelocityTracker, this.mActivePointerId);
    }

    void startSettlingAnimation(View child, int state) {
        int top;
        if (state == 4) {
            top = this.mMaxOffset;
        } else if (state == 3) {
            top = this.mMinOffset;
        } else if (state == 6) {
            top = this.mAnchorOffset;
        } else {
            if (!this.mHideable || state != 5) {
                throw new IllegalArgumentException("Illegal state argument: " + state);
            }

            top = this.mParentHeight;
        }

        this.setStateInternal(2);
        if (this.mViewDragHelper.smoothSlideViewTo(child, child.getLeft(), top)) {
            ViewCompat.postOnAnimation(child, new CustomBottomSheetBehavior.SettleRunnable(child, state));
        }

    }

    void dispatchOnSlide(int top) {
        View bottomSheet = (View) this.mViewRef.get();
        if (bottomSheet != null) {
            float slideOffset;
            if (top > this.mMaxOffset) {
                slideOffset = (float) (this.mMaxOffset - top) / (float) (this.mParentHeight - this.mMaxOffset);
            } else {
                slideOffset = (float) (this.mMaxOffset - top) / (float) (this.mMaxOffset - this.mMinOffset);
            }

            for (int i = 0; i < this.mCallbacks.size(); ++i) {
                ((CustomBottomSheetBehavior.BottomSheetCallback) this.mCallbacks.get(i)).onSlide(bottomSheet, slideOffset);
            }
        }

    }

    @VisibleForTesting
    int getPeekHeightMin() {
        return this.mPeekHeightMin;
    }

    public static <V extends View> CustomBottomSheetBehavior<V> from(V view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof android.support.design.widget.CoordinatorLayout.LayoutParams)) {
            throw new IllegalArgumentException("The view is not a child of CoordinatorLayout");
        } else {
            CoordinatorLayout.Behavior behavior = ((android.support.design.widget.CoordinatorLayout.LayoutParams) params).getBehavior();
            if (!(behavior instanceof CustomBottomSheetBehavior)) {
                throw new IllegalArgumentException("The view is not associated with CustomBottomSheetBehavior");
            } else {
                return (CustomBottomSheetBehavior) behavior;
            }
        }
    }

    protected static class SavedState extends AbsSavedState {
        final int state;
        public static final Creator<CustomBottomSheetBehavior.SavedState> CREATOR = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<CustomBottomSheetBehavior.SavedState>() {
            public CustomBottomSheetBehavior.SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new CustomBottomSheetBehavior.SavedState(in, loader);
            }

            public CustomBottomSheetBehavior.SavedState[] newArray(int size) {
                return new CustomBottomSheetBehavior.SavedState[size];
            }
        });

        public SavedState(Parcel source) {
            this(source, (ClassLoader) null);
        }

        public SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            this.state = source.readInt();
        }

        public SavedState(Parcelable superState, int state) {
            super(superState);
            this.state = state;
        }

        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.state);
        }
    }

    private class SettleRunnable implements Runnable {
        private final View mView;
        private final int mTargetState;

        SettleRunnable(View view, int targetState) {
            this.mView = view;
            this.mTargetState = targetState;
        }

        public void run() {
            if (CustomBottomSheetBehavior.this.mViewDragHelper != null && CustomBottomSheetBehavior.this.mViewDragHelper.continueSettling(true)) {
                ViewCompat.postOnAnimation(this.mView, this);
            } else {
                CustomBottomSheetBehavior.this.setStateInternal(this.mTargetState);
            }

        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo({RestrictTo.Scope.GROUP_ID})
    public @interface State {
    }

    public abstract static class SimpleBottomSheetCallback extends CustomBottomSheetBehavior.BottomSheetCallback {
        public SimpleBottomSheetCallback() {
        }

        public void onStateChanged(@NonNull View bottomSheet, int newState) {
        }

        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    }

    public abstract static class BottomSheetCallback {
        public BottomSheetCallback() {
        }

        public abstract void onStateChanged(@NonNull View var1, int var2);

        public abstract void onSlide(@NonNull View var1, float var2);
    }
}
