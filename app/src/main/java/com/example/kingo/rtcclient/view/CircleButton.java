package com.example.kingo.rtcclient.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.kingo.rtcclient.R;
import com.example.kingo.rtcclient.util.DimenUtils;

public class CircleButton extends LinearLayout {
  private AttributeSet mAttributeSet;
  private TextView mTextView;
  private ImageView mImageView;

  private String value;
  private Drawable src;
  private Drawable switchSrc;
  private int borderColor;
  private int bgColor;
  private Context mContext;
  private View parent;
  private boolean used;

  public CircleButton(Context context) {
    super(context);
    initView(context);
  }

  public CircleButton(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    mAttributeSet = attrs;
    initView(context);
  }

  public CircleButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    mAttributeSet = attrs;
    initView(context);
  }

  private void initView(Context context){
    mContext = context;
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    parent = inflater.inflate(R.layout.circle_btn,this,true);
    mTextView = parent.findViewById(R.id.circle_btn_txt);
    mImageView = parent.findViewById(R.id.circle_btn_img);
    if (mAttributeSet!=null){
      TypedArray typedArray = context.obtainStyledAttributes(mAttributeSet,R.styleable.CircleButton);
      if (typedArray!=null){
        value = typedArray.getString(R.styleable.CircleButton_value);
        src = typedArray.getDrawable(R.styleable.CircleButton_icon);
        switchSrc = typedArray.getDrawable(R.styleable.CircleButton_icon_switch);
        borderColor = typedArray.getColor(R.styleable.CircleButton_border_color,context.getResources().getColor(R.color.colorPrimary));
        bgColor = typedArray.getColor(R.styleable.CircleButton_bg_color,context.getResources().getColor(R.color.colorAccent));
        used = typedArray.getBoolean(R.styleable.CircleButton_used,false);
        mTextView.setText(value);
        if (used){
          setUsed();
        }else {
          setUnUsed();
        }
      }
    }
  }

  /**
   *
   * @param color       填充色
   * @param strokeColor 线条颜色
   * @param strokeWidth 线条宽度  单位px
   * @param radius      角度  px
   * @return
   */
  private GradientDrawable createRectangleDrawable(@ColorInt int color, @ColorInt int strokeColor, int strokeWidth, float radius){
    GradientDrawable radiusBg = new GradientDrawable();
    radiusBg.setShape(GradientDrawable.RECTANGLE);
    //设置填充颜色
    radiusBg.setColor(color);
    //设置线条粗心和颜色,px
    radiusBg.setStroke(strokeWidth, strokeColor);
    //设置圆角角度,如果每个角度都一样,则使用此方法
    radiusBg.setCornerRadius(radius);
    return radiusBg;
  }

  public void setUsed(){
    mTextView.setTextColor(mContext.getResources().getColor(R.color.white));
    mImageView.setImageDrawable(switchSrc);
    parent.setBackground(createRectangleDrawable(borderColor,borderColor, DimenUtils.dp2pxInt(1),DimenUtils.dp2px(50)));
  }

  public void setUnUsed(){
    mTextView.setTextColor(borderColor);
    mImageView.setImageDrawable(src);
    parent.setBackground(createRectangleDrawable(bgColor,borderColor, DimenUtils.dp2pxInt(1),DimenUtils.dp2px(50)));
  }


  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

  }
}
