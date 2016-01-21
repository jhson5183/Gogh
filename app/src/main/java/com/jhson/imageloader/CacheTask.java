package com.jhson.imageload.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

public class CacheTask implements Runnable {

    private final String TAG = "CacheTask";

    private Context mContext;
    private String mUri = "";
    private long mViewHashCode = -1;
    private GoghRequest mRequest;

    public CacheTask(Context context, String uri, long viewHashCode, GoghRequest request) {
        mContext = context;
        mUri = uri;
        mViewHashCode = viewHashCode;
        mRequest = request;
    }

    @Override
    public void run() {
        if (!Gogh.getInstance(mContext).isValidRequest(mViewHashCode, this))
            return;

        int width = mRequest.getBuilder().width;
        int height = mRequest.getBuilder().height;

        Bitmap raw = NewBitmapManager.getInstrance().getBitmapImage(mUri, GoghDownloader.getCachedImageFile(mUri, mContext), width, height);

        if (null != raw) {
            if (mViewHashCode > 0) {
                if (!Gogh.getInstance(mContext).unRegistRequest(mViewHashCode, this, true)) {
                    // 해당 이미지뷰에 해당 스레드가 더이상 유효하지 않다면, 다른 스레드가 이미지뷰를 점유했으므로 자원을 뱉고 스레드 종
                    Log.d(TAG, "request is invalidate");
                    return;
                }
            }
            Log.d(TAG, "request is comp");
            Gogh.getInstance(mContext).sImageHandler.post(mRequest.new ImageRunnable(raw));
        } else {
            Gogh.getInstance(mContext).executeNetworkRequest(mUri, mViewHashCode, mRequest, this);
        }
    }
}
