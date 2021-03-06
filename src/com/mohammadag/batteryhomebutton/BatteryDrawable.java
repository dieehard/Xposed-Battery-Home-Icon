package com.mohammadag.batteryhomebutton;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.TimeAnimator;
import android.animation.TimeAnimator.TimeListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

public class BatteryDrawable extends Drawable {
	private Paint mPaint;
	private Paint mTextPaint;

	private int mLevel = -1;
	private int mAngle = 0;
	private int mPadding = 20;
	private int mLandscapePadding = 20;
	private int mWidth = 5;
	private int mZeroAgnle = -90; // The starting angle, angle 0 is actually 90 degrees on a circle
	//New preference [S]
	private int mFontSize = 14;
	private int mFontColor = Color.WHITE;
	private int mCircleColor = Color.WHITE;
	//New preference [E]
	private RectF mRectF;

	private boolean mCharging;
	private boolean mChargingAnimationEnabled = true;
	private boolean mScreenOn = true;
	private boolean mEnablePercentage;

	private TimeAnimator mAnimator;

	private ImageView mView;

	public BatteryDrawable(ImageView view) {
		super();
		mView = view;

		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setColor(mCircleColor);
		mPaint.setStrokeWidth(mWidth);

		mTextPaint = new Paint();
		mTextPaint.setTextAlign(Paint.Align.CENTER);
		mTextPaint.setColor(mFontColor);
		mTextPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mFontSize,
				view.getResources().getDisplayMetrics()));
	}

	public void setBatteryLevel(int level) {
		mLevel = level;
		//is this okay?, percentage text only updated when touching home button
		invalidate();
		
		ValueAnimator animator = ValueAnimator.ofInt(mAngle, (mLevel * 360) / 100);
		animator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				mAngle = (Integer) animation.getAnimatedValue();
				invalidateSelf();
			}
		});
		animator.setInterpolator(new AccelerateDecelerateInterpolator());
		animator.setDuration(1000);
		animator.start();
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		updateRectFromBounds(bounds);
	}

	private void updateRectFromBounds(Rect bounds) {
		RectF rect;
		if (bounds.right > bounds.bottom) {
			int width = bounds.bottom;
			int left = bounds.centerX() - (width / 2);
			int right = bounds.centerX() + (width / 2);
			rect = new RectF(left + mPadding,
					bounds.top + mPadding, right - mPadding,
					bounds.bottom - mPadding);
		} else {
			int width = bounds.right;
			int top = bounds.centerY() - (width / 2);
			int bottom = bounds.centerY() + (width / 2);
			rect = new RectF(bounds.left + mLandscapePadding,
					top + mLandscapePadding, bounds.right - mLandscapePadding,
					bottom - mLandscapePadding);
		}

		mRectF = rect;
		invalidateSelf();
	}

	@Override
	public void draw(Canvas canvas) {
		if (mRectF == null)
			return;

		int alpha = mPaint.getAlpha();
		mPaint.setAlpha(70);

		// We could use 0 - 360 here, but why overdraw?
		canvas.drawArc(mRectF, mAngle + mZeroAgnle, 360 - mAngle, false, mPaint);

		mPaint.setAlpha(alpha);
		canvas.drawArc(mRectF, mZeroAgnle, mAngle, false, mPaint);

		//don't draw the text if 100%
		if (mEnablePercentage && mLevel != -1 && mLevel < 100) {
			int xPos = (canvas.getWidth() / 2);
			int yPos = (int) ((canvas.getHeight() / 2) - ((mTextPaint.descent() + mTextPaint.ascent()) / 2)); 
			canvas.drawText(String.valueOf(mLevel), xPos, yPos, mTextPaint);
		}
	}

	public void setBatteryCharging(boolean charging) {
		if (mCharging == charging)
			return;

		mCharging = charging;

		if (!mChargingAnimationEnabled && charging)
			return;

		if (mCharging) {
			mAnimator = new TimeAnimator();
			mAnimator.setTimeListener(new TimeListener() {
				@Override
				public void onTimeUpdate(TimeAnimator arg0, long arg1, long arg2) {
					if (mZeroAgnle == 270)
						mZeroAgnle = -90;
					else
						mZeroAgnle++;

					invalidate();
				}
			});
			mAnimator.start();
		} else {
			ValueAnimator animator = ValueAnimator.ofInt(mZeroAgnle, -90);
			animator.setDuration(1000L);
			animator.addListener(new AnimatorListener() {
				@Override
				public void onAnimationStart(Animator animation) {
					if (mAnimator != null)
						mAnimator.cancel();
				}

				@Override
				public void onAnimationRepeat(Animator animation) { }

				@Override
				public void onAnimationEnd(Animator animation) { }

				@Override
				public void onAnimationCancel(Animator animation) { }
			});
			animator.addUpdateListener(new AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animator) {
					mZeroAgnle = (Integer) animator.getAnimatedValue();
					invalidate();
				}
			});
			animator.setInterpolator(new AccelerateDecelerateInterpolator());
			animator.start();
		}
	}

	@Override
	public int getOpacity() {
		return 0;
	}

	@Override
	public void setAlpha(int alpha) {
		mPaint.setAlpha(alpha);
		mTextPaint.setAlpha(alpha);
		invalidateSelf();
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		mPaint.setColorFilter(cf);
		mTextPaint.setColorFilter(cf);
		invalidateSelf();
	}

	public void setPadding(int padding, int landscapePadding) {
		mPadding = padding;
		mLandscapePadding = landscapePadding;
		updateRectFromBounds(getBounds());
		invalidateSelf();
	}

	public void setStrokeWidth(int width) {
		mWidth = width;
		mPaint.setStrokeWidth(mWidth);
		invalidateSelf();
	}

	public void setChargingAnimationEnabled(boolean enable) {
		boolean oldChargingEnabled = mChargingAnimationEnabled;
		mChargingAnimationEnabled = enable;

		if (mCharging && !oldChargingEnabled && mChargingAnimationEnabled) {
			mCharging = false;
			setBatteryCharging(true);
		}
	}

	public void setPercentageEnabled(boolean enable) {
		if (mEnablePercentage == enable)
			return;

		mEnablePercentage = enable;
		invalidateSelf();
	}

	//New preference [S]
	public void setFontSize(int size) {
		mFontSize = size;
		mTextPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mFontSize,
				mView.getResources().getDisplayMetrics()));		
		invalidateSelf();
	}

	public void setColor(int circleColor, int fontColor) {
		mCircleColor = circleColor;
		mFontColor = fontColor;
		mPaint.setColor(mCircleColor);
		mTextPaint.setColor(mFontColor);
		invalidateSelf();
	}
	//New preference [E]
	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public void setScreenOn(boolean screenOn) {
		if (mScreenOn == screenOn)
			return;

		mScreenOn = screenOn;

		if (mAnimator != null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				if (mAnimator.isRunning() && !screenOn) {
					mAnimator.pause();
				}

				if (mAnimator.isPaused() && screenOn && mCharging) {
					mAnimator.resume();
				}
			} else {
				if (mAnimator.isRunning() && !screenOn) {
					mAnimator.end();
				}

				if (!mAnimator.isRunning() && screenOn && mCharging) {
					mAnimator.start();
				}
			}
		}
	}

	private void invalidate() {
		if (mView != null)
			mView.postInvalidate();
	}
}
