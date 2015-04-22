package jp.mau.twappremover;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

public class PopupView {
	private static final int	BG_COLOR			= 0x66000000;
	private static final int	WINDOW_COLOR		= 0xFFF0F0F0;
	private static final int	BORDER_COLOR		= 0xFFF00000;
	private static final int	TITLE_COLOR			= 0xFFF06666;
	private static final int	MIN_WINDOW_DIP		= 200;
	private static final int	MATCH_PARENT		= ViewGroup.LayoutParams.MATCH_PARENT;
	private static final int	WRAP_CONTENT		= ViewGroup.LayoutParams.WRAP_CONTENT;
	private static RelativeLayout _root;
	private ArrayList <Pair<View, RelativeLayout.LayoutParams>> _contents = new ArrayList<Pair<View,LayoutParams>>();
	private Context				_context;
	private boolean				_cancelable;

	// 以下標準ダイアログ用
	private LinearLayout		_window;
	private TextView			_message;
	private TextView			_title;
	private FrameLayout			_additional;
	private Button				_okBtn;
	private Button				_cancelBtn;
	static private RelativeLayout.LayoutParams _dialogparams;


	public PopupView (Context context) {
		if (_root == null || _context != context) {
			_context								= context;
			_root									= new RelativeLayout(context);
			_root.setVisibility(View.GONE);
			_root.setBackgroundColor(BG_COLOR);
			_root.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (_cancelable) {
						cancel ();
					}
				}
			});
			ViewGroup				parent			= (ViewGroup)((Activity)context).getWindow().getDecorView().findViewById(android.R.id.content);
			parent.addView(_root, MATCH_PARENT, MATCH_PARENT);

			_root.setOnKeyListener(new OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_BACK && _root.getVisibility() == View.VISIBLE && _cancelable) {
						Gen.debug("back");
						cancel();
						return true;
					}
					return false;
				}
			});

			_dialogparams								= new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
			int						margin				= (int)transDP2PX(context, 30); // 最大幅/高さの設定
			_dialogparams.setMargins(margin, margin, margin, margin);
			_dialogparams.addRule(RelativeLayout.CENTER_IN_PARENT);
		}
		makeDialog(context);
	}

	/** ダイアログを表示する */
	public void show () {
		_root.removeAllViews();
		for (Pair<View, RelativeLayout.LayoutParams> contents : _contents) {
			_root.addView(contents.first, -1, contents.second);
		}
		_root.setVisibility(View.VISIBLE);
		_root.setFocusableInTouchMode(true);
		_root.requestFocus();
	}

	/** ダイアログをキャンセルする(negativeボタンを押す) */
	public void cancel () {
		_cancelBtn.performClick();
		dismiss ();
	}

	/** 背景部分タッチ時にキャンセル可能に設定する */
	public PopupView setCancelable (boolean cancelable) {
		_cancelable									= cancelable;
		return this;
	}

	/** ダイアログを非表示にする */
	public void dismiss () {
		_root.setFocusableInTouchMode(false);
		_root.setVisibility(View.GONE);
	}

	public void addView (View child) {
		_contents.add(new Pair<View, RelativeLayout.LayoutParams>(child, new LayoutParams(WRAP_CONTENT, WRAP_CONTENT)));
	}

	public void addView (View child, LayoutParams params) {
		_contents.add(new Pair<View, RelativeLayout.LayoutParams>(child, params));
	}

	public void addView (View child, int width, int height) {
		_contents.add(new Pair<View, RelativeLayout.LayoutParams>(child, new LayoutParams(width, height)));
	}

	private void makeDialog (Context context) {
		int						padding				= (int)transDP2PX(context, 7);
		TextView				border				= new TextView(context);
		border.setBackgroundColor(BORDER_COLOR);
		_window										= new LinearLayout(context);
		_window.setBackgroundColor(WINDOW_COLOR);
		_window.setMinimumWidth((int)transDP2PX(context, MIN_WINDOW_DIP));
		_window.setOrientation(LinearLayout.VERTICAL);
		_title										= new TextView(context);
		_title.setPadding(padding, padding, padding, padding);
		_title.setTextSize(22);
		_title.setTextColor(TITLE_COLOR);
		_message									= new TextView(context);
		_message.setPadding(padding, padding, padding, (int) transDP2PX(context, 20));
		_message.setTextSize(16);
		_additional									= new FrameLayout(context);
		_window.addView(_title, MATCH_PARENT, WRAP_CONTENT);
		_window.addView(border, MATCH_PARENT, (int)transDP2PX(context, 3));
		_window.addView(_message, MATCH_PARENT, WRAP_CONTENT);
		_window.addView(_additional, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1));

		LinearLayout			buttons				= new LinearLayout(context);
		buttons.setOrientation(LinearLayout.HORIZONTAL);
		_okBtn										= new Button(context);
		_cancelBtn									= new Button(context);
		_okBtn.setVisibility(View.GONE);
		_okBtn.setBackgroundColor(WINDOW_COLOR);
		_cancelBtn.setVisibility(View.GONE);
		_cancelBtn.setBackgroundColor(WINDOW_COLOR);
		buttons.addView(_cancelBtn, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));
		buttons.addView(_okBtn, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));
		buttons.setGravity(Gravity.CENTER_HORIZONTAL);
		buttons.setBackgroundColor(0xFFCCCCCC);
		int						btnsPad				= (int)transDP2PX(context, 2);
		buttons.setPadding(0, btnsPad, 0, 0);
		_window.addView(buttons, MATCH_PARENT, WRAP_CONTENT);

	}

	/** 標準のダイアログを作成する */
	public PopupView setDialog () {
		addView(_window, _dialogparams);
		return this;
	}

	/** タイトルとメッセージを設定する */
	public PopupView setLabels (CharSequence title, CharSequence message) {
		_title.setText(title);
		_message.setText(message);
		return this;
	}

	/** 標準ダイアログにViewを追加する */
	public PopupView setView (View v) {
		_additional.addView(v);
		return this;
	}
	/** 標準ダイアログにViewを追加する */
	public PopupView setView (View v, int w, int h) {
		_additional.addView(v, w, h);
		return this;
	}
	/** 標準ダイアログにViewを追加する */
	public PopupView setView (View v, FrameLayout.LayoutParams params) {
		_additional.addView(v, params);
		return this;
	}

	/** ポジティブボタンの設定をする */
	public PopupView setPositiveBtn (CharSequence label, OnClickListener listener) {
		_okBtn.setText(label);
		_okBtn.setOnClickListener(listener);
		_okBtn.setVisibility(View.VISIBLE);
		return this;
	}

	/** ネガティブボタンの設定をする */
	public PopupView setNegativeBtn (CharSequence label, OnClickListener listener) {
		_cancelBtn.setText(label);
		_cancelBtn.setOnClickListener(listener);
		_cancelBtn.setVisibility(View.VISIBLE);
		return this;
	}

	private static float transDP2PX (Context context, float dp) {
		return dp * context.getResources().getDisplayMetrics().density;
	}

}
