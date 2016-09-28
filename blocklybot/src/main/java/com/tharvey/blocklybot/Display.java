package com.tharvey.blocklybot;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.PopupWindow;

import java.util.Random;

public class Display {
	private final static String TAG = Display.class.getSimpleName();

	private Activity mActivity;
	private PopupWindow mPopup;
	private View mLayout;
	private ImageView mEyeL;
	private ImageView mEyeR;
	private ImageView mMouth;
	private ObjectAnimator mSpeakAnimation;
	private boolean mSpeaking;

	public Display(Activity activity)
	{
		mActivity = activity;
	}

	public void showFace(String style) {
		Log.i(TAG, "showFace:" + style);
		mLayout = mActivity.getLayoutInflater().inflate(R.layout.display, null);
		mEyeL = (ImageView) mLayout.findViewById(R.id.eyeLeft);
		mEyeR = (ImageView) mLayout.findViewById(R.id.eyeRight);
		mMouth = (ImageView) mLayout.findViewById(R.id.mouth);
		mSpeaking = false;

		mPopup  = new PopupWindow(mLayout, WindowManager.LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.MATCH_PARENT,true);
		mPopup.showAtLocation(mLayout, Gravity.NO_GRAVITY, 100, 100);

		mLayout.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Log.i(TAG, "onTouch");
//				hideFace();
				setSpeaking(!mSpeaking);
				return false;
			}
		});

		/* gentle mouth bobbing */
		mLayout.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					public void onGlobalLayout() {
						Log.i(TAG, "mouth:" + mMouth.getX() + "," + mMouth.getY());
						final ObjectAnimator breathe = ObjectAnimator.ofFloat(mMouth, "y", mMouth.getY(), mMouth.getY() - 30);
						breathe.addListener(new Animator.AnimatorListener() {

							@Override
							public void onAnimationEnd(Animator animation) {
								breathe.setStartDelay(1000);
								breathe.start();
							}

							@Override
							public void onAnimationStart(Animator animation) { }

							@Override
							public void onAnimationCancel(Animator animation) { }

							@Override
							public void onAnimationRepeat(Animator animation) { }
						});
						breathe.setRepeatCount(1);
						breathe.setRepeatMode(ObjectAnimator.REVERSE);
						breathe.setStartDelay(1000);
						breathe.setInterpolator(new AccelerateDecelerateInterpolator());
						breathe.start();
					}
				}
		);

		/* occasional eye blink */
		final ObjectAnimator blinkL = ObjectAnimator.ofFloat(mEyeL, "scaleY", 1f, 0.1f);
		final ObjectAnimator blinkR = ObjectAnimator.ofFloat(mEyeR, "scaleY", 1f, 0.1f);
		final AnimatorSet blink = new AnimatorSet();
		blinkL.setDuration(200);
		blinkL.setRepeatCount(1);
		blinkL.setRepeatMode(ObjectAnimator.REVERSE);
		blinkL.setStartDelay(1000);
		blinkL.setInterpolator(new AccelerateDecelerateInterpolator());
		blinkR.setDuration(200);
		blinkR.setRepeatCount(1);
		blinkR.setRepeatMode(ObjectAnimator.REVERSE);
		blinkR.setStartDelay(1000);
		blinkR.setInterpolator(new AccelerateDecelerateInterpolator());
		blink.play(blinkL).with(blinkR);
		blink.addListener(new Animator.AnimatorListener() {
			@Override
			public void onAnimationEnd(Animator animation) {
				blink.setStartDelay(new Random().nextInt(4000 - 2000) + 2000); // 2 to 4 secs
				blink.start();
			}

			@Override
			public void onAnimationStart(Animator animation) { }

			@Override
			public void onAnimationCancel(Animator animation) { }

			@Override
			public void onAnimationRepeat(Animator animation) { }
		});
		blink.start();

		/* Speaking animation */
		mSpeakAnimation = ObjectAnimator.ofFloat(mMouth, "scaleY", 1f, 1.75f);
		mSpeakAnimation.setRepeatCount(ObjectAnimator.INFINITE);
		mSpeakAnimation.setRepeatMode(ObjectAnimator.REVERSE);

	}

	public void setSpeaking(boolean speaking) {
		mSpeaking = speaking;
		if (speaking) {
			Log.i(TAG, "speaking");
			mLayout.post(new Runnable() {
				public void run() {
					mSpeakAnimation.start();
				}
			});
		} else {
			Log.i(TAG, "done speaking");
			mLayout.post(new Runnable() {
				public void run() {
					mSpeakAnimation.cancel();
					mMouth.clearAnimation();
				}
			});
		}
	}

	public void hideFace() {
		Log.i(TAG, "hideFace");
		mLayout.post(new Runnable() {
			public void run() {
				mPopup.dismiss();
			}
		});
	}
}