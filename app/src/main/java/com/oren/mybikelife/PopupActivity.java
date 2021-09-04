package com.oren.mybikelife;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import com.oren.mybikelife.popup.BluetoothPopup;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 *
 */
public class PopupActivity extends Activity {
	public FrameLayout Framely = null;
	private BluetoothPopup bluetoothPopup = null;
	int nType = 0;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_popup);
		nType = (Integer)this.getIntent().getExtras().get("Layout_Type");
		getWindow().setWindowAnimations(android.R.style.Animation_Dialog);
		if(Framely == null)	Framely = this.findViewById(R.id.ly_popup);
		Framely.addView(LayoutInflater.from(this).inflate(nType, null));
		switch(nType) {
		case R.layout.bluetooth_select: {
			if(bluetoothPopup == null) bluetoothPopup = new BluetoothPopup(this);
			bluetoothPopup.start();
		} break;
//		case R.layout.input_task: {
//			if(inputask == null)
//				inputask = new InputTaskPopup(this);
//			String strGId = (String)this.getIntent().getExtras().get("gid");
//			String strId = (String)this.getIntent().getExtras().get("id");
//			String strDate = (String)this.getIntent().getExtras().get("date");
//			Object out = this.getIntent().getExtras().get("out");
//			if(out != null) bOut = (Boolean)out; else bOut = false;
//			inputask.Show(strGId,strId,strDate);
//		} break;
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	@Override
	public void onStop() {
		super.onStop();
		switch (nType) {
			case R.layout.bluetooth_select: {
				if (bluetoothPopup != null)
					bluetoothPopup.stop();
			}
			break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch(keyCode) {
			case KeyEvent.KEYCODE_BACK:
				switch(nType) {
					case R.layout.bluetooth_select: {
						bluetoothPopup.stop();
						finish();
					} break;
				}
				break;
			default:
		}
		return false;
	}
//	@Override
//	protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first)
//	{
//		super.onApplyThemeResource(theme, resid, first);
//			
//		// no background panel is shown
//		theme.applyStyle(android.R.style.Theme_Panel, true);
//	}
}
