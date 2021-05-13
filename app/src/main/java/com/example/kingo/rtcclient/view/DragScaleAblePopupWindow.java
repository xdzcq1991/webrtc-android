package com.example.kingo.rtcclient.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

public class DragScaleAblePopupWindow extends PopupWindow {
  private ViewGroup mContentView;
  private View view;
  private Context mContext;
  // 悬浮栏位置
  private static final int TOP = 0x15;
  private static final int LEFT = 0x16;
  private static final int BOTTOM = 0x17;
  private static final int RIGHT = 0x18;
  private static final int LEFT_TOP = 0x11;
  private static final int RIGHT_TOP = 0x12;
  private static final int LEFT_BOTTOM = 0x13;
  private static final int RIGHT_BOTTOM = 0x14;
  private static final int TOUCH_TWO = 0x21;
  private static final int CENTER = 0x19;
  private static int touchDistance; //触摸边界的有效距离  80

  private int dragDirection;//触摸的方向
  private int latestX = 0,latestY = 0,latestWidth = -1,latestHeight = -1;

  private int orgX, orgY;
  private int offsetX, offsetY;
  private int orgLeft,orgTop;
  private int orgViewX,orgViewY;
  private int latestRawX,latestRawY;


  public DragScaleAblePopupWindow(ViewGroup contentView,Context context) {
    super(contentView, DensityUtil.dip2px(context,150) , DensityUtil.dip2px(context,150));
    mContext = context;
    touchDistance = DensityUtil.dip2px(context,30);
    this.mContentView = contentView;
    setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    init();
  }

  //初始化
  private void init() {
    view = mContentView.getChildAt(0);
    view.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
          case MotionEvent.ACTION_DOWN:
            orgX = (int) event.getRawX();
            orgY = (int) event.getRawY();
            latestRawX = (int) event.getRawX();
            latestRawY = (int) event.getRawY();
            orgViewX = (int)event.getX();
            orgViewY = (int)event.getY();
            orgLeft = orgX - orgViewX;
            orgTop = orgY - orgViewY;
            dragDirection = getDirection(view,orgViewX,orgViewY);
            break;
          case MotionEvent.ACTION_MOVE:
            offsetX = (int) event.getRawX() - orgX;
            offsetY = (int) event.getRawY() - orgY;
            int dx = (int) event.getRawX() - latestRawX;
            int dy = (int) event.getRawY() - latestRawY;
            switch (dragDirection){
              case CENTER:
                center();
                break;
              case LEFT:
//                left(dx);
                break;
              case TOP:
//                top(dy);
                break;
              case RIGHT:
                right(dx);
                break;
              case BOTTOM:
                bottom(dy);
                break;
              case RIGHT_BOTTOM:
                right(dx);
                bottom(dy);
                break;
            }
            update(latestX, latestY, latestWidth, latestHeight, true);
            latestRawX = (int) event.getRawX();
            latestRawY = (int) event.getRawY();
            break;
          case MotionEvent.ACTION_UP:
            latestWidth = latestHeight = -1;
            break;
        }
        return true;
      }
    });
  }

  private void center(){
    latestX = orgLeft+offsetX;
    latestY = orgTop+offsetY;
  }

  //触摸点为左边缘
  private void left(int dx) {
    latestWidth = view.getWidth() - dx;
    latestX = orgLeft + offsetX;
  }

  //触摸点为右边缘
  private void right(int dx) {
    latestWidth = view.getWidth() + dx;
    if (latestWidth<=DensityUtil.dip2px(mContext,75)){
      latestWidth = DensityUtil.dip2px(mContext,75);
      return;
    }
  }

  //触摸点为上边缘
  private void top(int dy) {
    latestHeight = view.getHeight() - dy;
    latestY = orgTop + offsetY;
  }

  //触摸点为下边缘
  private void bottom(int dy) {
    latestHeight = view.getHeight() + dy;
    if (latestHeight<=DensityUtil.dip2px(mContext,75)){
      latestHeight = DensityUtil.dip2px(mContext,75);
      return;
    }
  }

  //获取触摸点flag
  private int getDirection(View view,float x, float y) {
    int left = view.getLeft();
    int right = view.getRight();
    int bottom = view.getBottom();
    int top = view.getTop();
    if (x < touchDistance && y < touchDistance) {
      return LEFT_TOP;
    }
    if (y < touchDistance && right - left - x < touchDistance) {
      return RIGHT_TOP;
    }
    if (x < touchDistance && bottom - top - y < touchDistance) {
      return LEFT_BOTTOM;
    }
    if (right - left - x < touchDistance && bottom - top - y < touchDistance) {
      return RIGHT_BOTTOM;
    }
    if (x < touchDistance) {
      return LEFT;
    }
    if (y < touchDistance) {
      return TOP;
    }
    if (right - left - x < touchDistance) {
      return RIGHT;
    }
    if (bottom - top - y < touchDistance) {
      return BOTTOM;
    }
    return CENTER;
  }

  static class DensityUtil{
    static int dip2px(Context context, float dpValue){
      float scale = context.getResources().getDisplayMetrics().density;
      return (int)(dpValue*scale+0.5f);
    }
  }
}
