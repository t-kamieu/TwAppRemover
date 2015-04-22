package jp.mau.twappremover;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;
import com.android.volley.toolbox.Volley;

public class MainActivity extends Activity {
	private final String		KEY_AUTH_TOKEN_TAG	= "authenticity_token";
	private final String		KEY_AUTH_TOKEN		= "auth_token";
	private final String		KEY_SESSION_ID		= "_twitter_sess";
	private final String		KEY_GUEST_ID		= "guest_id";
	private final String		KEY_HEADER_REVOKE	= "btn_oauth_application_";
	private final String		USER_AGENT			= "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:34.0) Gecko/20100101 Firefox/34.0";

	private final String		TOP_PAGE			= "https://twitter.com/";
	private final String		LOGIN_PAGE			= "https://twitter.com/sessions";
	private final String		APP_PAGE			= "https://twitter.com/settings/applications";
	private final String		REVOKE_PAGE			= "https://twitter.com/oauth/revoke";

	Handler						_handler;
	ImageLoader					_imageLoader;

	EditText					_id;
	EditText					_pass;
	AppAdapter					_appadapter;

	DefaultHttpClient			_client;
	String						_auth_token;		// HTMLタグ内のauthenticity_token
	String						_cookie_auth;		// Cookieで与えられたauth_token
	String						_session_id;		// Cookieで与えられた_twitter_sess
	String						_guest_id;

	List<LinkedApp>				_apps;

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_main);
		_apps										= new ArrayList<LinkedApp>();
		_handler									= new Handler();
		_imageLoader								= new ImageLoader(Volley.newRequestQueue(this), new BitmapCache ());

		_id											= (EditText)findViewById (R.id.activity_main_form_id);
		_pass										= (EditText)findViewById (R.id.activity_main_form_pass);
		ListView list								= (ListView)findViewById (R.id.activity_main_list);
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
				final PopupView dialog		= new PopupView(MainActivity.this);
				dialog
					.setDialog()
					.setLabels(getString(R.string.activity_main_dlgtitle_revoke), getString(R.string.activity_main_dlgmsg_revoke, _apps.get(position).name))
					.setCancelable(true)
					.setNegativeBtn(getString(R.string.activity_main_dlgbtn_revoke_cancel), new OnClickListener() {
						@Override
						public void onClick(View v) {
							dialog.dismiss();
						}
					})
					.setPositiveBtn(getString(R.string.activity_main_dlgbtn_revoke_conf), new OnClickListener() {
						@Override
						public void onClick(View v) {
							dialog.dismiss();
							new Thread(new Runnable() {
								@Override
								public void run() {
									revokeApp(_apps.get(position));
								}
							}).start();
						}
					})
				.show();
			}
		});
		setButton ();

		_appadapter									= new AppAdapter(this);
		list.setAdapter(_appadapter);

		_client										= new DefaultHttpClient();
		HttpParams				params				= _client.getParams();
		HttpConnectionParams.setConnectionTimeout(params, 60 * 1000);
		HttpConnectionParams.setSoTimeout(params, 60 * 1000);
		_client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BEST_MATCH);

	}

	@Override
	public boolean onCreateOptionsMenu (Menu menu) {
		getMenuInflater ().inflate (R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item) {
		int id = item.getItemId ();
		if (id == R.id.action_settings) {
			ScrollView			contents			= new ScrollView(this);
			LinearLayout		layout				= new LinearLayout(this);
			layout.setOrientation(LinearLayout.VERTICAL);
			contents.addView(layout);
			AssetManager		asm					= getResources().getAssets();
			String[]			filelist			= null;
			try {
				filelist							= asm.list("licenses");
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			if (filelist != null) {
				for (String file : filelist) {
					Gen.debug (file);
					InputStream	is					= null;
					BufferedReader br				= null;
					String		txt					= "";
					try {
						is							= getAssets().open("licenses/" + file);
						br							= new BufferedReader(new InputStreamReader(is));
						String	str;
						while ((str = br.readLine()) != null) {
							txt						+= str + "\n";
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					} finally {
						try {if (is != null) is.close ();} catch (Exception ex) {}
						try {if (br != null) br.close ();} catch (Exception ex) {}
					}
					TextView	tv					= new TextView(this);
					tv.setText(txt);
					layout.addView(tv);
				}
			}

			// ライセンス表示
			final PopupView		dialog				= new PopupView(this);
			dialog
				.setDialog()
				.setLabels(getString (R.string.activity_main_dlgtitle_oss), "")
				.setView(contents, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
				.setCancelable(true)
				.setPositiveBtn(getString(R.string.activity_main_dlgbtn_oss), new OnClickListener() {
					@Override
					public void onClick(View v) {
						dialog.dismiss();
					}
				})
			.show();
			return true;
		}
		return super.onOptionsItemSelected (item);
	}

	/** ログインボタンの動作を設定する */
	private void setButton () {
		Button					btn					= (Button)findViewById (R.id.activity_main_btn_submit);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				reset ();
				// ソフトキーボードを非表示にする
				InputMethodManager	imm				= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				if (_id.getText().length() < 1 || _pass.getText().length() < 1) {
					// 入力されていないのでアラート
				} else {
//					 ログインする
					loginTask ();
				}
			}
		});
	}

	private void reset () {
		_apps.clear();
		_appadapter.notifyDataSetChanged();
		_auth_token									= null;
		_cookie_auth								= null;
		_session_id									= null;
		_guest_id									= null;
	}

	private void loginTask () {
		final PopupView			dialog				= new PopupView(MainActivity.this);

		final AsyncTask<Void, Void, Boolean> task	= new AsyncTask<Void, Void, Boolean> () {
			@Override
			protected void onPreExecute() {
				dialog.show();
			}
			@Override
			protected Boolean doInBackground(Void... params) {
				_client.getConnectionManager().shutdown();
				_client								= new DefaultHttpClient();
				HttpParams		hparams				= _client.getParams();
				HttpConnectionParams.setConnectionTimeout(hparams, 60 * 1000);
				HttpConnectionParams.setSoTimeout(hparams, 60 * 1000);
				_client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BEST_MATCH);

				getTopPage();
				// got session id and auth-token
				// login
				if (login ()) {
					getApps ();
				} else {
					return false;
				}
				return true;
			}
			@Override
			protected void onPostExecute(Boolean result) {
				dialog.dismiss();
				if (!result) {
					// ログイン失敗
					final PopupView	dialog			= new PopupView (MainActivity.this);
					dialog
						.setDialog()
						.setLabels(getString(R.string.activity_main_dlgtitle_loginfail), getString(R.string.activity_main_dlgmsg_loginfail))
						.setCancelable(true)
						.setPositiveBtn(getString(R.string.activity_main_dlgbtn_loginfail), new OnClickListener() {
							@Override
							public void onClick(View v) {
								dialog.dismiss();
							}
						})
					.show();
				}
			}

		};

		dialog
			.setDialog ()
			.setLabels (getString(R.string.activity_main_dlgtitle_loading), getString(R.string.activity_main_dlgmsg_loading));

		task.execute();
	}

	private void getTopPage () {

		Document		doc					= null;
		try {
			Connection	conn				= Jsoup.connect(TOP_PAGE);
			conn.header ("User-Agent", USER_AGENT);
			conn.header ("Connection", "keep-alive");
			doc								= conn.get();
			Response	res					= conn.response();
			Map<String, String> cookies		= res.cookies();

			for (Map.Entry<String, String> e : cookies.entrySet()) {
				if (e.getKey().equals(KEY_SESSION_ID)) _session_id = e.getValue();
				if (e.getKey().equals(KEY_GUEST_ID)) _guest_id = e.getValue();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (doc == null) return;
		// parse top page and get authenticity token
		Elements		forms				= doc.getElementsByTag("form");
		for (Element e : forms) {
			Elements	auths				= e.getElementsByAttributeValue("name", "authenticity_token");
			if (auths.size() > 0) {
				_auth_token					= auths.get(0).attr("value");
				break;
			}
		}
		if (_auth_token == null) {
			return;
		}
	}

	private boolean login () {
		HttpPost		request				= new HttpPost(LOGIN_PAGE);
		request.addHeader("User-Agent", USER_AGENT);
		request.addHeader("Referer", TOP_PAGE);
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");
		request.addHeader("Cookie", "_twitter_sess=" + _session_id + "; guest_id=" + _guest_id);

		List<NameValuePair> pairs			= new ArrayList<NameValuePair> ();
		pairs.add(new BasicNameValuePair("session[username_or_email]", _id.getText().toString()));
		pairs.add(new BasicNameValuePair("session[password]", _pass.getText().toString()));
		pairs.add(new BasicNameValuePair("remember_me", "1"));
		pairs.add(new BasicNameValuePair("return_to_ssl", "true"));
		pairs.add(new BasicNameValuePair("redirect_after_login", " "));
		pairs.add(new BasicNameValuePair(KEY_AUTH_TOKEN_TAG, _auth_token));

		try {
			request.setEntity(new UrlEncodedFormEntity(pairs, "UTF-8"));
			String 		result				= _client.execute(request, new ResponseHandler<String>() {
				@Override
				public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
					switch (response.getStatusLine().getStatusCode()) {
					case HttpStatus.SC_OK:
						return EntityUtils.toString(response.getEntity(), "UTF-8");
					case HttpStatus.SC_NOT_FOUND:
						throw new RuntimeException("not found");
					default:
						throw new RuntimeException("error");
					}
				}
			});
			// ログイン成否判定(signin-wrapperというクラスを発見できれば失敗)
			Document			doc					= Jsoup.parse(result);
			Elements			elm					= doc.getElementsByClass("signin-wrapper");
			if (elm.size() > 0) return false;

			// auth_tokenが更新されている
			List<Cookie>		cookies				= _client.getCookieStore().getCookies();
			for (Cookie c : cookies) {
				if (c.getName().equals(KEY_AUTH_TOKEN)) {
					_cookie_auth					= c.getValue();
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return true;
	}

	private void getApps () {
		_apps.clear();

		HttpGet					request				= new HttpGet(APP_PAGE);
		request.addHeader("User-Agent", USER_AGENT);
		request.addHeader("Cookie", "_twitter_sess=" + _session_id + "; auth_token=" + _cookie_auth);

		try {
			String				result				= _client.execute(request, new ResponseHandler<String>() {
				@Override
				public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
					switch (response.getStatusLine().getStatusCode()) {
					case HttpStatus.SC_OK:
						return EntityUtils.toString(response.getEntity(), "UTF-8");
					case HttpStatus.SC_NOT_FOUND:
						throw new RuntimeException("not found");
					default:
						throw new RuntimeException("error");
					}
				}
			});

			Document			doc					= null;
			doc										= Jsoup.parse(result);

			// parse top page and get authenticity token
			Elements		forms				= doc.getElementsByTag("form");
			for (Element e : forms) {
				Elements	auths				= e.getElementsByAttributeValue("name", "authenticity_token");
				if (auths.size() > 0) {
					_auth_token					= auths.get(0).attr("value");
					break;
				}
			}

			Elements			apps				= doc.getElementsByClass("app");
			for (Element e : apps) {
				LinkedApp		app					= new LinkedApp();
				if (e.getElementsByTag("strong").size() > 0)
					app.name							= e.getElementsByTag("strong").get(0).text();
				if (e.getElementsByClass("creator").size() > 0)
					app.creator							= e.getElementsByClass("creator").get(0).text();
				if (e.getElementsByClass("description").size() > 0)
					app.desc							= e.getElementsByClass("description").get(0).text();
				if (e.getElementsByClass("app-img").size() > 0)
					app.imgUrl							= e.getElementsByClass("app-img").get(0).attr("src");
				if (e.getElementsByClass("revoke").size() > 0) {
					String		tmp						= e.getElementsByClass("revoke").get(0).attr("id");
					app.revokeId						= tmp.replaceAll(KEY_HEADER_REVOKE, "");
				} else {
					// revoke id がなければパス(facebook連携しようぜ回避のため)
					continue;
				}
				_apps.add(app);
			}
			_handler.post(new Runnable() {
				@Override
				public void run() {
					_appadapter.notifyDataSetChanged();
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void revokeApp (LinkedApp app) {
		final PopupView			dialog				= new PopupView(this);
		dialog
			.setDialog()
			.setLabels(getString(R.string.activity_main_dlgtitle_loading), getString(R.string.activity_main_dlgmsg_loading));
		_handler.post(new Runnable() {
			@Override
			public void run() {
				dialog.show();
			}
		});

		HttpPost		request				= new HttpPost(REVOKE_PAGE);
		request.addHeader("User-Agent", USER_AGENT);
		request.addHeader("Referer", APP_PAGE);
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");
		request.addHeader("Cookie", "_twitter_sess=" + _session_id + "; auth_token=" + _cookie_auth);

		List<NameValuePair> pairs			= new ArrayList<NameValuePair> ();
		pairs.add(new BasicNameValuePair(KEY_AUTH_TOKEN_TAG, _auth_token));
		pairs.add(new BasicNameValuePair("scribeContext[component]", "oauth_app"));
		pairs.add(new BasicNameValuePair("token", app.revokeId));
		pairs.add(new BasicNameValuePair("twttr", "true"));
		try {
			request.setEntity(new UrlEncodedFormEntity(pairs, "UTF-8"));
			_client.execute(request, new ResponseHandler<String>() {
				@Override
				public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
					switch (response.getStatusLine().getStatusCode()) {
					case HttpStatus.SC_OK:
						return EntityUtils.toString(response.getEntity(), "UTF-8");
					case HttpStatus.SC_NOT_FOUND:
						throw new RuntimeException("not found");
					default:
						throw new RuntimeException("error");
					}
				}
			});

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		getApps();

		_handler.post(new Runnable() {
			@Override
			public void run() {
				dialog.dismiss();
			}
		});
	}

	@SuppressLint("NewApi")
	private void writeOut (String str) {
		FileOutputStream		fos					= null;
		File					file				= new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "test.txt");
		try {
			fos										= new FileOutputStream(file);
			fos.write(str.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fos != null) try { fos.close(); } catch ( Exception e) {; }
		}
	}

	class LinkedApp {
		String					name				= "";
		String					creator				= "";
		String					desc				= "";
		String					imgUrl				= "";
		String					revokeId			= "";
	}

	class AppAdapter extends ArrayAdapter<LinkedApp> {
		LayoutInflater			inf;
		public AppAdapter(Context context) {
			super(context, 0);
			inf										= getLayoutInflater();
		}

		@Override
		public int getCount() {
			return _apps.size();
		}

		@SuppressLint("InflateParams")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder			holder;
			if (convertView != null) {
				holder								= (ViewHolder)convertView.getTag();
			} else {
				convertView							= inf.inflate(R.layout.list_applist, null);

				holder								= new ViewHolder();
				holder.icon							= (ImageView)convertView.findViewById(R.id.list_applist_image_icon);
				holder.name							= (TextView)convertView.findViewById(R.id.list_applist_text_title);
				holder.creator						= (TextView)convertView.findViewById(R.id.list_applist_text_author);
				holder.desc							= (TextView)convertView.findViewById(R.id.list_applist_text_desc);
				convertView.setTag(holder);
			}
			LinkedApp			target				= _apps.get(position);
			holder.name.setText (target.name);
			holder.creator.setText (target.creator);
			holder.desc.setText (target.desc);

			ImageContainer		container			= (ImageContainer)holder.icon.getTag();
			if (container != null) {
				container.cancelRequest();
			}
			ImageListener		listener			= ImageLoader.getImageListener(holder.icon, android.R.drawable.ic_menu_gallery, android.R.drawable.ic_dialog_alert);
			holder.icon.setTag(_imageLoader.get(target.imgUrl, listener));

			return convertView;
		}
	}

	class ViewHolder {
		ImageView				icon;
		TextView				name;
		TextView				creator;
		TextView				desc;
	}
}
