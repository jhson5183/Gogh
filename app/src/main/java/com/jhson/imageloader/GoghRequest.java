package com.jhson.imageload.imageloader;

import android.graphics.Bitmap;
import android.widget.ImageView;


public class GoghRequest {

	private final String TAG = "GoghRequest";

	private String mUrl;
	private Builder mBuilder;
	private ImageView mImageView;
	private Gogh mGoghInstance;

	public class Builder {
		public int width = -1;
		public int height = -1;
		public int errorId = 0;
		public Gogh.GoghListener listener = null;
		public Gogh.GoghListener defaultListener = null;
	}

	public class ImageRunnable implements Runnable {

		private Bitmap bm;

		public ImageRunnable(Bitmap bm) {
			this.bm = bm;
		}

		@Override
		public void run() {
			if (GoghRequest.this.mBuilder.listener != null) {
				if (bm != null) {
					GoghRequest.this.mBuilder.listener.onLoaded(mImageView, bm);
				} else {
					GoghRequest.this.mBuilder.listener.onFailed();
				}

			} else {
				if (bm != null) {
					GoghRequest.this.mBuilder.defaultListener.onLoaded(mImageView, bm);
				} else {
					GoghRequest.this.mBuilder.defaultListener.onFailed();
				}
			}
		}
	}

	public GoghRequest(String url, Gogh gogh) {
		this.mUrl = url;
		this.mGoghInstance = gogh;
		this.mBuilder = new Builder();

		mBuilder.defaultListener = new Gogh.GoghListener() {

			@Override
			public void onLoaded(ImageView iv, Bitmap bm) {
				if (null != mImageView && null != bm)
					mImageView.setImageBitmap(bm);
			}

			@Override
			public void onFailed() {
				if (null != mImageView && mBuilder.errorId != 0) {
					mImageView.setImageResource(mBuilder.errorId);
				}
			}
		};
	}

	public void into(ImageView imageView) {
		mImageView = imageView;
		mImageView.setImageBitmap(null);

		mGoghInstance.executeRequest(mUrl, imageView.hashCode(), this);
	}

	public void into() {
		mGoghInstance.executeRequest(mUrl, -1, this);
	}

	public Builder getBuilder() {
		return mBuilder;
	}

	public GoghRequest listener(Gogh.GoghListener listener) {
		mBuilder.listener = listener;
		return this;
	}

	public GoghRequest error(int resourceId) {
		mBuilder.errorId = resourceId;
		return this;
	}

	public GoghRequest resize(int width, int height) {
		if (width < 1 || height < 1) {
			throw new IllegalArgumentException("width < 1 || height < 1");
		}
		mBuilder.width = width;
		mBuilder.height = height;
		return this;
	}

}