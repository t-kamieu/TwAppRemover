package jp.mau.twappremover;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.toolbox.ImageLoader.ImageCache;

public class BitmapCache implements ImageCache {

	private LruCache<String, Bitmap> _cache;

	public BitmapCache () {
		int						maxSize				= 10 * 1024 * 1024;
		_cache										= new LruCache<String, Bitmap> (maxSize) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getRowBytes() * value.getHeight();
			}
		};
	}

	@Override
	public Bitmap getBitmap(String url) {
		return _cache.get(url);
	}

	@Override
	public void putBitmap(String url, Bitmap bitmap) {
		_cache.put(url, bitmap);
	}

}
