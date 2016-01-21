package com.jhson.imageload.imageloader;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;

public class NewBitmapManager {

	private static final String TAG = "BitmapManager";
	private LruCache<String, Bitmap> mMemoryCache;

	private static NewBitmapManager sNewBitmapManager;

	public synchronized static NewBitmapManager getInstrance() {
		if (sNewBitmapManager == null)
			sNewBitmapManager = new NewBitmapManager();
		return sNewBitmapManager;
	}

	private NewBitmapManager() {
		initMemCache();
	}
	
	private void initMemCache() {
		if (mMemoryCache == null) {
			final int maxMemory = (int) (Runtime.getRuntime().maxMemory());
			int cacheSize = maxMemory / 16;
			if(cacheSize <= 16 * 1024 * 1024){
				cacheSize = 16 * 1024 * 1024;
			}

			mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
				@Override
				protected int sizeOf(String key, Bitmap bitmap) {
					return ((bitmap.getRowBytes() * bitmap.getHeight()));
				}
			};
		}
	}

	public void clearMemoryCache() {
		mMemoryCache.evictAll();
	}

	public synchronized Bitmap getBitmapImage(String url, final File file, int width, int height) {
		if(!file.exists()){
			return null;
		}
		Bitmap bm = null;
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPreferredConfig = Config.RGB_565;
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
		int outWidth = opts.outWidth;
		opts.inJustDecodeBounds = false;

		if (outWidth <= 0)
			return null;

		if (width < 1) {
			width = outWidth;
		}

		int sampleSize = outWidth / width;
		opts.inSampleSize = sampleSize;

		try {
			bm = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
			if (bm == null) {
				file.delete();
			}
		} catch (OutOfMemoryError e) {
			return null;
		} catch (Exception e) {
			if (bm != null) {
//				bm.recycle();
				bm = null;
			}
			Log.e(TAG, "BitmapFactory.decodeFile " + file.length());
		}
		if (bm != null) {
			addBitmapToMemoryCache(TextUtils.isEmpty(url)? file.getName() : url, bm);
		}

		return bm;
	}
	
	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		if (!TextUtils.isEmpty(key) && bitmap != null) {
			if (getBitmapFromMemCache(key) == null) {
				mMemoryCache.put(key, bitmap);
			}
		}
	}

	public Bitmap getBitmapFromMemCache(String key) {
		if (!TextUtils.isEmpty(key)) {
			return mMemoryCache.get(key);
		} else {
			return null;
		}
	}
}
