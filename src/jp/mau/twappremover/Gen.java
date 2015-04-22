package jp.mau.twappremover;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

/**
 * いろんなところから呼び出すメソッドをstatic化して集約
 * @author Kamiesu
 *
 */
public class Gen {
	private static String		DCIM_DIR			= "DCIM";
	private static String		PARENT_DIR			= "";
	final public static int		WRAP_CONTENT		= ViewGroup.LayoutParams.WRAP_CONTENT;
	final public static int		MATCH_PARENT		= ViewGroup.LayoutParams.MATCH_PARENT;

	/**
	 * 指定したファイル名を元にユニーク名を作成してファイルオブジェクトを生成
	 * @param dstDir ターゲットディレクトリ
	 * @param origName 元ファイル名
	 * @return ファイル
	 */
	public static File createUniqueName (File dstDir, String origName) {
		// かきだし先ファイル（重複ファイル判定付き）
		File					dstFile				= new File(dstDir, origName);
		int						cnt					= 0;
		while (dstFile.exists()) {
			int					index				= origName.lastIndexOf('.');
			String				name				= origName.substring(0, index);
			String				ext					= origName.substring(index);
			dstFile									= new File(dstDir, name + "(" + cnt + ")" + ext);
			cnt++;
		}
		return dstFile;
	}

	/**
	 * 拡張子に対応するMIMEタイプ取得兼対応拡張子チェック
	 * @param filename ファイル名
	 * @return MIMEタイプ（非対応拡張子の場合はnull）
	 */
	public static String getMediaMime(String filename) {
		String mime = null;
		final String lowerName = filename.toLowerCase(Locale.getDefault());
		if (lowerName.endsWith(".jpg")) {
			mime = "image/jpeg";
		} else if (lowerName.endsWith(".jpeg")) {
			mime = "image/jpeg";
		} else if (lowerName.endsWith(".m2ts")) {
			mime = "video/mpeg";
		} else if (lowerName.endsWith(".mts")) {
			mime = "video/mpeg";
		} else if (lowerName.endsWith(".mpeg4")) {
			mime = "video/mp4";
		} else if (lowerName.endsWith(".mp4")) {
			mime = "video/mp4";
		} else if (lowerName.endsWith(".m4v")) {
			mime = "video/mp4";
		} else if (lowerName.endsWith(".avi")) {
			mime = "video/avi";
		} else if (lowerName.endsWith(".mov")) {
			mime = "video/quicktime";
		} else if (lowerName.endsWith(".3gp")) {
			mime = "video/3gpp";
		} else if (lowerName.endsWith(".3g2")) {
			mime = "video/3gpp2";
		} else if (lowerName.endsWith(".png")) {
			mime = "image/png";
		}
		return mime;
	}

	/** 動画かどうか */
	public static boolean isMovie (String filename) {
		final String lowerName = filename.toLowerCase(Locale.getDefault());
		if (lowerName.endsWith(".m2ts")) {
			return true;
		} else if (lowerName.endsWith(".mts")) {
			return true;
		} else if (lowerName.endsWith(".mpeg4")) {
			return true;
		} else if (lowerName.endsWith(".mp4")) {
			return true;
		} else if (lowerName.endsWith(".m4v")) {
			return true;
		} else if (lowerName.endsWith(".avi")) {
			return true;
		} else if (lowerName.endsWith(".mov")) {
			return true;
		} else if (lowerName.endsWith(".3gp")) {
			return true;
		} else if (lowerName.endsWith(".3g2")) {
			return true;
		} else if (lowerName.endsWith(".png")) {
			return true;
		} else {
			return false;
		}
	}


	/** なければ作成してディレクトリを取得する */
	public static File getDirectory (String path) {
		File					file				= new File(path);
		if (!file.exists() && !file.mkdirs()) {
			debug("dstFile mkdirs error!!");
			return null;
		}
		return file;
	}

	/**
	 * デバッグログの出力
	 * @param str ログ
	 */
	public static void debug (Object str) {
		if (BuildConfig.DEBUG) {
			StackTraceElement[] stack = new Throwable().getStackTrace();
			String methodname = stack[1].getMethodName();
			int line = stack[1].getLineNumber();
			StringBuilder builder = new StringBuilder(60);
			builder.append("□■□■ ").append(methodname)
					.append("(").append(line).append(") ").append(str);
			Log.v (stack[1].getClassName(), builder.toString());
		}
	}

	/** これを呼び出したメソッドまでの呼び出し順を出力する */
	public static void stacktrace () {
		if (BuildConfig.DEBUG) {
			StackTraceElement[] stack = new Throwable().getStackTrace();

			StringBuilder builder = new StringBuilder(100);
			for (StackTraceElement element : stack) {
				builder.append(String.format("%s(%s:%d) > ", element.getMethodName(), element.getClassName(), element.getLineNumber()));
			}

			Log.v (stack[1].getClassName(), builder.toString());
		}
	}

	/**
	 * ファイル名に使えない記号を全角化して退避する
	 * @param filename ファイル名
	 * @return 変換後ファイル名
	 */
	public static String checkFilename (String filename) {
		filename = filename.replaceAll ("<", "＜");
		filename = filename.replaceAll (">", "＞");
		filename = filename.replaceAll (":", "：");
		filename = filename.replaceAll ("\\*", "＊");
		filename = filename.replaceAll ("\\?", "？");
		filename = filename.replaceAll ("\"", "”");
		filename = filename.replaceAll ("\\/", "／");
		filename = filename.replaceAll ("\\\\", "＼");
		filename = filename.replaceAll ("\\|", "｜");
		return filename;
	}

	public static void debugActionCode (MotionEvent event) {
		String					str;
		switch (event.getAction()) {
			case MotionEvent.ACTION_CANCEL:					str = "ACTION_CANCEL";				break;
			case MotionEvent.ACTION_DOWN: 					str = "ACTION_DOWN";				break;
			case MotionEvent.ACTION_HOVER_ENTER:			str = "ACTION_HOVER_ENTER";			break;
			case MotionEvent.ACTION_HOVER_EXIT:				str = "ACTION_HOVER_EXIT";			break;
			case MotionEvent.ACTION_HOVER_MOVE:				str = "ACTION_HOVER_MOVE";			break;
			case MotionEvent.ACTION_MASK:					str = "ACTION_MASK";				break;
			case MotionEvent.ACTION_MOVE:					str = "ACTION_MOVE";				break;
			case MotionEvent.ACTION_OUTSIDE:				str = "ACTION_OUTSIDE";				break;
			case MotionEvent.ACTION_POINTER_DOWN:			str = "ACTION_POINTER_DOWN";		break;
			case MotionEvent.ACTION_POINTER_INDEX_MASK:		str = "ACTION_POINTER_INDEX_MASK";	break;
			case MotionEvent.ACTION_POINTER_INDEX_SHIFT:	str = "ACTION_POINTER_INDEX_SHIFT";	break;
			case MotionEvent.ACTION_POINTER_UP:				str = "ACTION_POINTER_UP";			break;
			case MotionEvent.ACTION_UP:						str = "ACTION_UP";					break;
			default:										str = "OTHERS...?";
		}
		debug (str);
	}

	/** URIからファイルパスを取得する */
	public static String getUriPath (Context context, Uri uri) {
		ContentResolver			contentresolver		= context.getContentResolver();
		String[]				columns				= {MediaStore.Images.Media.DATA};
		Cursor					cursor				= contentresolver.query(uri, columns, null, null, null);
		if (cursor == null) return null;
		cursor.moveToFirst();
		String					path				= cursor.getString(0);
		cursor.close();
		return path;
	}

	/** ファイルオブジェクトを作成，親ディレクトリがなければ作成 */
	public static File makeFilePaths (File dir, String filename) {
		if (dir == null) {
			final File			dcimDir				= new File(Environment.getExternalStorageDirectory(), DCIM_DIR);
			dir										= new File(dcimDir, PARENT_DIR);
		}
		dir.mkdirs();

		// 拡張子の分離
		String					extention;
		String					destName;
		int						index				= filename.lastIndexOf(".");
		if (index != -1) {
			destName								= filename.substring(0, index);
			extention								= filename.substring(index);
		} else {
			destName								= filename;
			extention								= "";
		}

		int						i					= 1;
		File					checkFile			= new File(dir, filename);
		// 保存名が重複しないように
		while (checkFile.exists()) {
			checkFile = new File (dir, String.format("%s-(%d)%s", destName, i, extention));
			i ++;
		}

		return checkFile;
	}

	/**
	 * ギャラリーで表示されるように登録(movが対応していないので未使用)
	 * @param file File
	 */
	@SuppressLint("NewApi")
	public static void registerContent(Context context, final File file) {
		String					paths[]				= new String[1];
		paths[0]									= file.getAbsolutePath();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			MediaScannerConnection.scanFile(context, paths, null, new OnScanCompletedListener() {
				public void onScanCompleted(String path, Uri uri) {
					Gen.debug ("path:" + path);
					Gen.debug ("uri :" + uri);
				}
			});
		} else {
			class ScannerClient implements MediaScannerConnectionClient {
				private MediaScannerConnection msc;
				private File file;
				ScannerClient (Context context, File f) {
					file							= f;
					msc								= new MediaScannerConnection(context, this);
					msc.connect();
				}
				public void onMediaScannerConnected() {
					msc.scanFile(file.getAbsolutePath(), null);
				}
				public void onScanCompleted(String path, Uri uri) {
					msc.disconnect();
				}
			}
			new ScannerClient(context, file);
		}
	}

	/** EXIFから画像の回転角度を取得する */
	public static int getRotate (String filepath) {
		int 					ret					= 0;
		try {
			// Exif情報から表示を回転させる
			ExifInterface			exif				= new ExifInterface (filepath);
			if (exif != null) {
				int 				orientation 	= exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
				switch (orientation) {
					case ExifInterface.ORIENTATION_NORMAL:
						ret = 0;		break;
					case ExifInterface.ORIENTATION_ROTATE_90:
						ret = 90;		break;
					case ExifInterface.ORIENTATION_ROTATE_180:
						ret = 180;		break;
					case ExifInterface.ORIENTATION_ROTATE_270:
						ret = 270;		break;
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return ret;
	}

	/** 縮小倍率を計算する */
	public static int culcSampleSize (BitmapFactory.Options opts, int reqWidth, int reqHeight) {
		final int				width				= opts.outWidth;
		final int				height				= opts.outHeight;
		int						inSampleSize		= 1;
		if (width > reqWidth || height > reqHeight) {
			if (width < height) {
				inSampleSize						= (int)Math.floor((float)height / (float)reqHeight);
			} else {
				inSampleSize						= (int)Math.floor((float)width / (float)reqWidth);
			}
		}
		return inSampleSize;
	}

	/** byte配列からリサイズしてBitmapを取得する */
	public static Bitmap resize (byte[] data, int reqWidth, int reqHeight) {
		// まずはサイズだけ取得
		BitmapFactory.Options	opts				= new BitmapFactory.Options ();
		opts.inJustDecodeBounds						= true;
		opts.inPreferredConfig						= Bitmap.Config.RGB_565;
		BitmapFactory.decodeByteArray(data, 0, data.length, opts);
		// 縮小
		opts.inSampleSize							= Gen.culcSampleSize(opts, reqWidth, reqHeight);
		opts.inJustDecodeBounds						= false;
		final Bitmap			bitmap				= BitmapFactory.decodeByteArray(data, 0, data.length, opts);

		return bitmap;
	}

	/** URIからリサイズしてBitmapを取得する */
	public static Bitmap resize (Uri uri, Context context, int reqWidth, int reqHeight) {
		Bitmap					bitmap				= null;
		InputStream				inputstream			= null;
		try {
			inputstream								= context.getContentResolver().openInputStream(uri);
			BitmapFactory.Options opts				= new BitmapFactory.Options ();
			opts.inJustDecodeBounds					= true;
			opts.inPreferredConfig					= Bitmap.Config.RGB_565;
			BitmapFactory.decodeStream(inputstream, null, opts);
			try {if (inputstream != null) inputstream.close();} catch (Exception ex) {ex.printStackTrace();}
			// 縮小
			inputstream								= context.getContentResolver().openInputStream(uri);
			opts.inSampleSize						= Gen.culcSampleSize(opts, reqWidth, reqHeight);
			opts.inJustDecodeBounds					= false;
			bitmap									= BitmapFactory.decodeStream(inputstream, null, opts);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try {if (inputstream != null) inputstream.close();} catch (Exception ex) {ex.printStackTrace();}
		}

		String					path				= getUriPath(context, uri);
		// 回転
		if (bitmap != null && path != null) {
			float				rotation			= Gen.getRotate(path);
			int					width				= bitmap.getWidth();
			int					height				= bitmap.getHeight();
			Matrix				matrix				= new Matrix();
			matrix.postRotate(rotation);
			bitmap									= Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
		}


		return bitmap;
	}

	/** ファイルパスからリサイズしてBitmapを取得する */
	public static Bitmap resize (String path, int reqWidth, int reqHeight) {
		Bitmap					bitmap				= null;

		BitmapFactory.Options opts					= new BitmapFactory.Options ();
		opts.inJustDecodeBounds						= true;
		opts.inPreferredConfig						= Bitmap.Config.RGB_565;
		BitmapFactory.decodeFile(path, opts);
		// 縮小
		opts.inSampleSize							= Gen.culcSampleSize(opts, reqWidth, reqHeight);
		opts.inJustDecodeBounds						= false;
		bitmap										= BitmapFactory.decodeFile(path, opts);

		// 回転
		if (bitmap != null) {
			float				rotation			= Gen.getRotate(path);
			int					width				= bitmap.getWidth();
			int					height				= bitmap.getHeight();
			Matrix				matrix				= new Matrix();
			matrix.postRotate(rotation);
			bitmap									= Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
		}

		return bitmap;
	}

	/** 画像を保存する */
	public static void savePhoto (Bitmap bitmap, File destination, Context context) {
		FileOutputStream		fos					= null;
		try {
			fos										= new FileOutputStream(destination);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			Gen.registerContent (context, destination);
		}
	}

	/** 入力欄を絵文字使用可能にする */
	public static void availableEmoji (EditText et) {
		Bundle					bundle				= et.getInputExtras(true);
		if (bundle != null) bundle.putBoolean("allowEmoji", true);
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public static void setBackgroundDrawable (View v, Drawable drawable) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			v.setBackgroundDrawable(drawable);
		} else {
			v.setBackground(drawable);
		}
	}

	public static float transDP2PX (Context context, float dp) {
		return dp * context.getResources().getDisplayMetrics().density;
	}
	public static float transPX2DP (Context context, float px) {
		return px / context.getResources().getDisplayMetrics().density;
	}
}
