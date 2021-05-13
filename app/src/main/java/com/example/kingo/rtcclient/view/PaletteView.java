package com.example.kingo.rtcclient.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Xfermode;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.example.kingo.rtcclient.util.DimenUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wensefu on 17-3-21.
 */
public class PaletteView extends View {

  private Paint mPaint;
  private Path mPath;
  private float mLastX;
  private float mLastY;
  private Bitmap mBufferBitmap;
  private Bitmap mMemoryBitmap;
  private Canvas mBufferCanvas;

  private static final int MAX_CACHE_STEP = 20;

  private List<DrawingInfo> mDrawingList;
  private List<DrawingInfo> mRemovedList;

  private Xfermode mXferModeClear;
  private Xfermode mXferModeDraw;
  private int mDrawSize;
  private int mEraserSize;
  private int mPenAlpha = 255;

  private int paintColor = Color.BLACK;
  private int transParent = 0xffff00f;
  private int paintSize = 1;

  private boolean mCanEraser = true;

  private boolean isUsing = true;
  private CountDownTimer timer = null;

  private Callback mCallback;

  public enum Mode {
    DRAW, ERASER, LINE, RECT, CIRCLE
  }

  private Mode mMode = Mode.DRAW;


  public PaletteView(Context context) {
    super(context);
    init();
  }

  public PaletteView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public PaletteView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  public interface Callback {
    void onUndoRedoStatusChanged();
  }

  public boolean isUsing() {
    return isUsing;
  }

  public void setUsing(boolean using) {
    isUsing = using;
  }

  public int getPaintColor() {
    return paintColor;
  }

  public void setPaintColor(int paintColor) {
    this.paintColor = paintColor;
    mPaint.setColor(this.paintColor);
  }

  public int getPaintSize() {
    return paintSize;
  }

  public void setPaintSize(int paintSize) {
    this.paintSize = paintSize;
    mDrawSize = DimenUtils.dp2pxInt(this.paintSize);
    mPaint.setStrokeWidth(mDrawSize);
  }

  public void setCallback(Callback callback) {
    mCallback = callback;
  }

  private void init() {
    setDrawingCacheEnabled(true);
    mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    mPaint.setStyle(Paint.Style.STROKE);
    mPaint.setFilterBitmap(true);
    mPaint.setStrokeJoin(Paint.Join.ROUND);
    mPaint.setStrokeCap(Paint.Cap.ROUND);
    mEraserSize = DimenUtils.dp2pxInt(30);
    mDrawSize = DimenUtils.dp2pxInt(this.paintSize);
    mPaint.setStrokeWidth(mDrawSize);
    mPaint.setColor(this.paintColor);
    mXferModeDraw = new PorterDuffXfermode(PorterDuff.Mode.SRC);
    mXferModeClear = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    timer = new CountDownTimer(1500, 1500) {
      @Override
      public void onTick(long millisUntilFinished) {

      }

      @Override
      public void onFinish() {
        setUsing(false);
        Log.v("DataChannel", "canvasState--false");
        timer.cancel();
      }
    };
    mPaint.setXfermode(mXferModeDraw);
  }

  private void initBuffer() {
    mBufferBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
    mMemoryBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
    mBufferCanvas = new Canvas(mBufferBitmap);
  }

  private abstract static class DrawingInfo {
    Paint paint;

    abstract void draw(Canvas canvas);
  }

  private static class PathDrawingInfo extends DrawingInfo {

    Path path;

    @Override
    void draw(Canvas canvas) {
      canvas.drawPath(path, paint);
    }
  }

  public Mode getMode() {
    return mMode;
  }

  public void setMode(Mode mode) {
    if (mode != mMode) {
      mMode = mode;
      if (mMode == Mode.ERASER) {
        mPaint.setXfermode(mXferModeClear);
        mPaint.setStrokeWidth(mEraserSize);
      } else {
        mPaint.setXfermode(mXferModeDraw);
        mPaint.setStrokeWidth(mDrawSize);
      }
    }
  }

  public void setEraserSize(int size) {
    mEraserSize = size;
  }

  public void setPenRawSize(int size) {
    mDrawSize = size;
    if (mMode == Mode.DRAW) {
      mPaint.setStrokeWidth(mDrawSize);
    }
  }

  public void setPenColor(int color) {
    mPaint.setColor(color);
  }

  private void reDraw() {
    if (mDrawingList != null) {
      mBufferBitmap.eraseColor(Color.TRANSPARENT);
      for (DrawingInfo drawingInfo : mDrawingList) {
        drawingInfo.draw(mBufferCanvas);
      }
      invalidate();
    }
  }

  public int getPenColor() {
    return mPaint.getColor();
  }

  public int getPenSize() {
    return mDrawSize;
  }

  public int getEraserSize() {
    return mEraserSize;
  }

  public void setPenAlpha(int alpha) {
    mPenAlpha = alpha;
    if (mMode == Mode.DRAW) {
      mPaint.setAlpha(alpha);
    }
  }

  public int getPenAlpha() {
    return mPenAlpha;
  }

  public boolean canRedo() {
    return mRemovedList != null && mRemovedList.size() > 0;
  }

  public boolean canUndo() {
    return mDrawingList != null && mDrawingList.size() > 0;
  }

  public void redo() {
    int size = mRemovedList == null ? 0 : mRemovedList.size();
    if (size > 0) {
      DrawingInfo info = mRemovedList.remove(size - 1);
      mDrawingList.add(info);
      mCanEraser = true;
      reDraw();
      if (mCallback != null) {
        mCallback.onUndoRedoStatusChanged();
      }
    }
  }

  public void undo() {
    int size = mDrawingList == null ? 0 : mDrawingList.size();
    if (size > 0) {
      DrawingInfo info = mDrawingList.remove(size - 1);
      if (mRemovedList == null) {
        mRemovedList = new ArrayList<>(MAX_CACHE_STEP);
      }
      if (size == 1) {
        mCanEraser = false;
      }
      mRemovedList.add(info);
      reDraw();
      if (mCallback != null) {
        mCallback.onUndoRedoStatusChanged();
      }
    }
  }

  public void clear() {
    if (mBufferBitmap != null) {
      if (mDrawingList != null) {
        mDrawingList.clear();
      }
      if (mRemovedList != null) {
        mRemovedList.clear();
      }
      mCanEraser = false;
      mBufferBitmap.eraseColor(Color.TRANSPARENT);
      invalidate();
      if (mCallback != null) {
        mCallback.onUndoRedoStatusChanged();
      }
    }
  }

  public Bitmap buildBitmap() {
    Bitmap bm = getDrawingCache();
    Bitmap result = Bitmap.createBitmap(bm);
    destroyDrawingCache();
    return result;
  }

  //绘制远程图片
  public void drawRemoteView(Bitmap bitmap) {
    clear();
    mBufferBitmap = bitmap;
    mBufferCanvas = new Canvas(mBufferBitmap);
    invalidate();
  }

  private void saveDrawingPath() {
    if (mDrawingList == null) {
      mDrawingList = new ArrayList<>(MAX_CACHE_STEP);
    } else if (mDrawingList.size() == MAX_CACHE_STEP) {
      mDrawingList.remove(0);
    }
    Path cachePath = new Path(mPath);
    Paint cachePaint = new Paint(mPaint);
    PathDrawingInfo info = new PathDrawingInfo();
    info.path = cachePath;
    info.paint = cachePaint;
    mDrawingList.add(info);
    mCanEraser = true;
    if (mCallback != null) {
      mCallback.onUndoRedoStatusChanged();
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (mBufferBitmap != null) {
      canvas.drawBitmap(mBufferBitmap, 0, 0, null);
    }
  }


  //	List<Rect> list;

  @SuppressWarnings("all")
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (!isEnabled()) {
      return false;
    }
    final int action = event.getAction() & MotionEvent.ACTION_MASK;
    final float x = event.getX();
    final float y = event.getY();

    Paint paint = new Paint();
    paint.setStrokeWidth(mDrawSize);
    paint.setColor(Color.TRANSPARENT);
    paint.setStyle(Paint.Style.STROKE);

    switch (action) {
      case MotionEvent.ACTION_DOWN:
        //				list = new ArrayList<>();
        if (mBufferBitmap != null) {
          mMemoryBitmap = mBufferBitmap.copy(Bitmap.Config.ARGB_8888,true);
        }

        mLastX = x;
        mLastY = y;
        if (mPath == null) {
          mPath = new Path();
        }
        mPath.moveTo(x, y);
        setUsing(true);
        if (timer != null) {
          timer.cancel();
        }
        break;
      case MotionEvent.ACTION_MOVE:
        if (mBufferBitmap == null) {
          initBuffer();
        }
        if (mMode == Mode.LINE) {
          if (mMemoryBitmap!=null){
            mBufferBitmap = mMemoryBitmap.copy(Bitmap.Config.ARGB_8888,true);
            mBufferCanvas = new Canvas(mBufferBitmap);
          }
//          mPath.lineTo(x, y);
          mBufferCanvas.drawLine(mLastX, mLastY, x, y, mPaint);

          invalidate();
          //					mLastX = x;
          //					mLastY = y;
        }else if (mMode == Mode.RECT) {
          if (mMemoryBitmap!=null){
            mBufferBitmap = mMemoryBitmap.copy(Bitmap.Config.ARGB_8888,true);
            mBufferCanvas = new Canvas(mBufferBitmap);
          }
          Rect rect = new Rect((int) mLastX, (int) mLastY, (int) x, (int) y);
          mBufferCanvas.drawRect(rect, mPaint);
          invalidate();
        }else if (mMode == Mode.CIRCLE) {
          if (mMemoryBitmap!=null){
            mBufferBitmap = mMemoryBitmap.copy(Bitmap.Config.ARGB_8888,true);
            mBufferCanvas = new Canvas(mBufferBitmap);
          }
          float radius = Math.min(Math.abs(x - mLastX), Math.abs(y - mLastY)) / 2;
          mBufferCanvas.drawCircle(x>mLastX?(mLastX+(int)radius):(mLastX-(int)radius), y>mLastY?(mLastY+(int)radius):(mLastY-(int)radius), radius, mPaint);
          invalidate();
        }else if (mMode == Mode.ERASER || mMode == Mode.DRAW) {
          //				这里终点设为两点的中心点的目的在于使绘制的曲线更平滑，如果终点直接设置为x,y，效果和lineto是一样的,实际是折线效果
          mPath.quadTo(mLastX, mLastY, (x + mLastX) / 2, (y + mLastY) / 2);
          mBufferCanvas.drawPath(mPath, mPaint);
          invalidate();
          mLastX = x;
          mLastY = y;
        }

        break;
      case MotionEvent.ACTION_UP:
        //				list = null;
        if (mMode == Mode.LINE) {
//          mPath.lineTo(x, y);
          mBufferCanvas.drawLine(mLastX,mLastY,x,y,mPaint);
          invalidate();
        }else if (mMode == Mode.RECT) {
          Rect rect = new Rect((int) mLastX, (int) mLastY, (int) x, (int) y);
          mBufferCanvas.drawRect(rect, mPaint);
          //        list.add(rect);
          invalidate();
        }else if (mMode == Mode.CIRCLE) {
          float radius = Math.min(Math.abs(x - mLastX), Math.abs(y - mLastY)) / 2;
          mBufferCanvas.drawCircle(x>mLastX?(mLastX+(int)radius):(mLastX-(int)radius), y>mLastY?(mLastY+(int)radius):(mLastY-(int)radius), radius, mPaint);
          invalidate();
        }else if (mMode == Mode.DRAW || mMode == Mode.ERASER) {
          mPath.reset();//          saveDrawingPath();
          mLastX = x;
          mLastY = y;
        }

        timer.start();

        break;
    }
    return true;
  }
}
