package com.app.chao.chaoapp.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.app.chao.chaoapp.R;

/** Expandable text which animates line-count changes entirely on the main thread. */
public class TextViewExpandableAnimation extends LinearLayout implements View.OnClickListener {
    private final TextView textView;
    private final TextView stateView;
    private final ImageView stateIcon;
    private final RelativeLayout toggle;
    private Drawable collapseDrawable;
    private Drawable expandDrawable;
    private String expandText;
    private String collapseText;
    private boolean collapsed;
    private boolean expansionNeeded;
    private int collapsedLines;
    private int measuredLines;
    private CharSequence content = "";
    private int animationStepMillis = 22;
    private int textGeneration;
    private ValueAnimator animator;

    public TextViewExpandableAnimation(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.layout_textview_expand_animation, this, true);
        textView = findViewById(R.id.tv_expand_text_view_animation);
        stateView = findViewById(R.id.tv_expand_text_view_animation_hint);
        stateIcon = findViewById(R.id.iv_expand_text_view_animation_toggle);
        toggle = findViewById(R.id.rl_expand_text_view_animation_toggle_layout);

        TypedArray values = context.obtainStyledAttributes(
                attrs, R.styleable.TextViewExpandableAnimation);
        collapsedLines = values.getInteger(
                R.styleable.TextViewExpandableAnimation_tvea_expandLines, 5);
        collapseDrawable = values.getDrawable(
                R.styleable.TextViewExpandableAnimation_tvea_shrinkBitmap);
        expandDrawable = values.getDrawable(
                R.styleable.TextViewExpandableAnimation_tvea_expandBitmap);
        collapseText = values.getString(
                R.styleable.TextViewExpandableAnimation_tvea_textShrink);
        expandText = values.getString(
                R.styleable.TextViewExpandableAnimation_tvea_textExpand);
        int contentColor = values.getColor(
                R.styleable.TextViewExpandableAnimation_tvea_textContentColor,
                ContextCompat.getColor(context, R.color.gray_light));
        int stateColor = values.getColor(
                R.styleable.TextViewExpandableAnimation_tvea_textStateColor,
                ContextCompat.getColor(context, R.color.colorPrimary));
        float contentSize = values.getDimension(
                R.styleable.TextViewExpandableAnimation_tvea_textContentSize,
                textView.getTextSize());
        values.recycle();

        if (collapseDrawable == null) {
            collapseDrawable = ContextCompat.getDrawable(context, R.mipmap.icon_green_arrow_up);
        }
        if (expandDrawable == null) {
            expandDrawable = ContextCompat.getDrawable(context, R.mipmap.icon_green_arrow_down);
        }
        if (TextUtils.isEmpty(collapseText)) {
            collapseText = context.getString(R.string.shrink);
        }
        if (TextUtils.isEmpty(expandText)) {
            expandText = context.getString(R.string.expand);
        }
        textView.setTextColor(contentColor);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, contentSize);
        stateView.setTextColor(stateColor);
        textView.setOnClickListener(this);
        toggle.setOnClickListener(this);
    }

    public void setText(CharSequence value) {
        content = value == null ? "" : value;
        cancelAnimation();
        textView.setMaxLines(Integer.MAX_VALUE);
        textView.setText(content);
        int generation = ++textGeneration;
        textView.post(() -> {
            if (generation != textGeneration) {
                return;
            }
            measuredLines = textView.getLineCount();
            expansionNeeded = measuredLines > collapsedLines;
            collapsed = expansionNeeded;
            textView.setMaxLines(expansionNeeded ? collapsedLines : Integer.MAX_VALUE);
            updateToggle();
        });
    }

    @Override
    public void onClick(View view) {
        if (expansionNeeded) {
            animateLines(collapsed ? collapsedLines : measuredLines,
                    collapsed ? measuredLines : collapsedLines);
            collapsed = !collapsed;
            updateToggle();
        }
    }

    private void animateLines(int start, int end) {
        cancelAnimation();
        animator = ValueAnimator.ofInt(start, end);
        animator.setDuration((long) Math.abs(end - start) * animationStepMillis);
        animator.addUpdateListener(value -> textView.setMaxLines((Integer) value.getAnimatedValue()));
        animator.start();
    }

    private void updateToggle() {
        toggle.setVisibility(expansionNeeded ? View.VISIBLE : View.GONE);
        textView.setClickable(expansionNeeded);
        if (!expansionNeeded) {
            return;
        }
        stateIcon.setVisibility(View.VISIBLE);
        stateIcon.setImageDrawable(collapsed ? expandDrawable : collapseDrawable);
        stateView.setText(collapsed ? expandText : collapseText);
        toggle.setContentDescription(collapsed ? expandText : collapseText);
    }

    private void cancelAnimation() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelAnimation();
        super.onDetachedFromWindow();
    }

    public Drawable getDrawableShrink() {
        return collapseDrawable;
    }

    public void setDrawableShrink(Drawable drawable) {
        collapseDrawable = drawable;
        updateToggle();
    }

    public Drawable getDrawableExpand() {
        return expandDrawable;
    }

    public void setDrawableExpand(Drawable drawable) {
        expandDrawable = drawable;
        updateToggle();
    }

    public int getExpandLines() {
        return collapsedLines;
    }

    public void setExpandLines(int lines) {
        collapsedLines = Math.max(1, lines);
        expansionNeeded = measuredLines > collapsedLines;
        collapsed = expansionNeeded;
        textView.setMaxLines(expansionNeeded ? collapsedLines : Integer.MAX_VALUE);
        updateToggle();
    }

    public CharSequence getTextContent() {
        return content;
    }

    public int getSleepTime() {
        return animationStepMillis;
    }

    public void setSleepTime(int millis) {
        animationStepMillis = Math.max(0, millis);
    }
}
