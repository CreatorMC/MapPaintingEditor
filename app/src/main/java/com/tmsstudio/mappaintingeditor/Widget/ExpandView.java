package com.tmsstudio.mappaintingeditor.Widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

public class ExpandView extends FrameLayout implements View.OnClickListener {
    private Animation mExpandAnimation;
    private Animation mCollapseAnimation;
    private boolean mIsExpand;
    private int layoutId;

    public ExpandView(Context context) {
        this(context, null);
    }

    public ExpandView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpandView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    public void setLayout(int layout) {
        this.layoutId = layout;
        initExpandView();
    }

    private void initExpandView() {
        LayoutInflater.from(getContext()).inflate(layoutId, this, true);
        mExpandAnimation = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
        mExpandAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setVisibility(View.VISIBLE);
            }
        });

        mCollapseAnimation = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out);
        mCollapseAnimation.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                setVisibility(View.GONE);
            }
        });

    }

    /**
     * 收缩
     */
    public void collapse() {
        if (mIsExpand) {
            mIsExpand = false;
            clearAnimation();
            startAnimation(mCollapseAnimation);
        }
    }

    /**
     * 展开
     */
    public void expand() {
        if (!mIsExpand) {
            mIsExpand = true;
            clearAnimation();
            startAnimation(mExpandAnimation);
        }
    }

    public boolean isExpand() {
        return mIsExpand;
    }

    public void setContentView(View view) {
        removeAllViews();
        addView(view);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

        }
    }
}
