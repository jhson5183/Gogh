package com.jhson.imageload.imageloader;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;
import java.util.LinkedList;

public class NetworkTask implements Runnable {

    private Context mContext;
    private String mUrl = "";
    private long mViewHashCode = -1;
    private GoghRequest mRequest;
    private GoghDownloader mDownloader;

    public NetworkTask(Context context, String url, long viewHashCode, GoghRequest request) {
        mContext = context;
        mUrl = url;
        mViewHashCode = viewHashCode;
        mRequest = request;
    }

    public void imgDownloadStop() {
        if (null != mDownloader) mDownloader.stop();
    }

    @Override
    public void run() {
        if (!Gogh.getInstance(mContext).isValidRequest(mViewHashCode, this))
            return;

        if (Gogh.getInstance(mContext).mWaitingJobs.containsKey(mUrl)) { // "다운로드 중인 동일한 URL 이 있다면."
            try {
                LinkedList<Runnable> waitingSameUrl = Gogh.getInstance(mContext).mWaitingJobs.get(mUrl);
                if (waitingSameUrl != null) {
                    waitingSameUrl.add(this);
                } else {
                    LinkedList<Runnable> waitingUrl = new LinkedList<Runnable>();
                    waitingUrl.add(this);
                    Gogh.getInstance(mContext).mWaitingJobs.put(mUrl, waitingUrl);
                }
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (!Gogh.getInstance(mContext).isValidRequest(mViewHashCode, this)) {
            Gogh.getInstance(mContext).unLock(mUrl);
            return;
        }

        File cachedFile = GoghDownloader.getCachedImageFile(mUrl, mContext);
        if (!(cachedFile.exists() && cachedFile.length() > 1)) {
            mDownloader = new GoghDownloader();
            mDownloader.get(mUrl, cachedFile); // "error 이면."
        }

        Gogh.getInstance(mContext).unLock(mUrl); // "다운로드 실패가 발생해도 모두 잡혀있는 쓰레드 UNLOCK 해야한다."

        Bitmap raw = NewBitmapManager.getInstrance().getBitmapImage(mUrl, cachedFile, mRequest.getBuilder().width, mRequest.getBuilder().height);

        if (mViewHashCode > 0) {
            if (!Gogh.getInstance(mContext).unRegistRequest(mViewHashCode, this, true)) {
                // 해당 이미지뷰에 해당 스레드가 더이상 유효하지 않다면, 다른 스레드가 이미지뷰를 점유했으므로 자원을 뱉고 스레드 종
                return;
            }
        }
        Gogh.getInstance(mContext).sImageHandler.post(mRequest.new ImageRunnable(raw));
    }
}