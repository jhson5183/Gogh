package com.jhson.imageload.imageloader;

import android.content.Context;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

public class GoghDownloader {

	private static final String TAG = "GoghDownloader";

	private DownloadListener mListener;
	private ErrorToastListener mErrorListener;

	private boolean mIsStop = false;

	private static final int BUFFER_SIZE = 8 * 1024;

	public GoghDownloader() {
		HttpManager.init("");
	}

	public synchronized static File getCachedImageFile(String strUrl, Context context) {

		// 캐시 파일
		File cachedFile = null;
		try {
			cachedFile = new File(IOUtils.getFilePrefix(context), URLEncoder.encode(strUrl, "UTF-8").replace(".", "_"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return cachedFile;
	}

	public boolean get(String strUrl, File cachedFile) {
		mIsStop = false;

		if (strUrl == null || strUrl.length() == 0) {
			Log.i(TAG, "URL Validation Error");
			return false;
		}
		
		return download(strUrl, cachedFile, true);
	}

	private synchronized boolean download(String strUrl, File cachedFile, boolean isUseCache) {

		final HttpGet request = new HttpGet(strUrl);
		HttpEntity entity = null;
		boolean result = false;
		
		try {
			
			if (cachedFile.exists())
				cachedFile.delete();

			final HttpResponse response = HttpManager.execute(request);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

				entity = response.getEntity();

				InputStream in = null;
				FileOutputStream out = null;
				ByteArrayOutputStream tempOutStream = null;

				long contentLength = 0;
				long current = 0;
				int read = 0;
				byte[] b = new byte[BUFFER_SIZE];

				try {

					in = entity.getContent();
					contentLength = entity.getContentLength();

					// " 받다가 중간에 끊어지면 이미 받아놓은 cache파일이 못쓰게 되므로 tmp파일에 작성하고 바꿔치기 한다. "
					File tmpFile = new File(cachedFile.getAbsolutePath() + ".temp");
					tmpFile.createNewFile();
					out = new FileOutputStream(tmpFile);
					boolean breakFlag = false;

					tempOutStream = new ByteArrayOutputStream();
					while ((read = in.read(b)) != -1) {
						if (mIsStop) { // 받고 있는 중에 중단 한다.
							in.close();
							tempOutStream.close();
							out.close();
							tmpFile.delete();
							breakFlag = true;
							break;
						}
						tempOutStream.write(b, 0, read);

						current += read;
						if (mListener != null) {
							mListener.onProgress(current, contentLength);
						}
					}
					tempOutStream.flush();

					if(tmpFile.exists()){
						tempOutStream.writeTo(out);
					}

					if (breakFlag)
						return false;


					out.flush();
					out.close();
					
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					}
					
					if (contentLength == current) {
						if(tmpFile.exists()){
							result = tmpFile.renameTo(cachedFile);
						}

						// 성공적으로 이미지를 캐시한 경우 메모리 캐시에 올린다. 
						if (result) {
							//  스트림을 메모리캐시도 시켜둠.
							BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
							decodeOptions.inPreferredConfig = Config.RGB_565;
	//			            decodeOptions.inPurgeable = true;
							if(tmpFile.exists()){
								NewBitmapManager.getInstrance().addBitmapToMemoryCache(strUrl, BitmapFactory.decodeByteArray(tempOutStream.toByteArray(), 0, tempOutStream.toByteArray().length, decodeOptions));
							}
						}
					} else {
						result = false;
					}
					tempOutStream.close();

				} catch (IOException e) {

					Log.e(TAG, "Could not save file from " + request.getURI().toString());
					e.printStackTrace();
					if ( mErrorListener != null)
						mErrorListener.onError("ERROR : " + e.getMessage());

				} catch (OutOfMemoryError error) {
				} finally {
					IOUtils.closeStream(in);
					IOUtils.closeStream(tempOutStream);
					IOUtils.closeStream(out);
				}

			}else{
				Log.e(TAG, " response.getStatusLine().getStatusCode() NO OK "+response.getStatusLine().getStatusCode());
			}

		} catch (IOException e) {

			Log.e(TAG, "Could not load file from " + request.getURI().toString());
			e.printStackTrace();

		} finally {

			if (entity != null) {
				try {
					entity.consumeContent();
				} catch (IOException e) {
					Log.e(TAG, "Could not save file from " + request.getURI().toString());
					e.printStackTrace();
				}
			}

		}

		// cache 사용이라면 다운로드에 실패했지만 이전파일이 있다면 이전파일을 사용.
		if (result == false && isUseCache == true && cachedFile.exists() == true) {
			return true;
		}

		return result;
	}

	public static interface DownloadListener {
		public void onProgress(long current, long total);
	}

	public static interface ErrorToastListener {
		public void onError(String msg);
	}

	public void setDownloadListener(DownloadListener listener) {
		mListener = listener;
	}

	public void setErrorToastListener(ErrorToastListener listener) {
		mErrorListener = listener;
	}

	public void stop() {
		mIsStop = true;
	}

	public static boolean removeCache(Context context) {
		File cacheDir = null;
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			cacheDir = new File(context.getApplicationContext().getExternalCacheDir(), "/cache");
		} else {
			cacheDir = new File(context.getCacheDir(), "/cache");
		}

		if (cacheDir.exists() && cacheDir.isDirectory() && cacheDir.listFiles().length != 0) {
			for (File file : cacheDir.listFiles())
				file.delete();
			return true;
		}
		return false;

	}

}
