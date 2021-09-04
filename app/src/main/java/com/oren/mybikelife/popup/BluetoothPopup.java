package com.oren.mybikelife.popup;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.text.LoginFilter;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.oren.mybikelife.MainActivity;
import com.oren.mybikelife.R;
import com.oren.mybikelife.data.Config;
import com.oren.util.bluetooth.UseGattAttributes;
import com.oren.xml.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.oren.util.bluetooth.UseGattAttributes.*;

public class BluetoothPopup {
	private Activity acti;
	private List<String> list = new ArrayList<>();
	public List<BluetoothDevice> bDeviceList = new ArrayList<>();
	private ArrayAdapter<String> adapter;

//	private LeDeviceListAdapter mLeDeviceListAdapter;
	private Handler mHandler;
	private BluetoothAdapter mBluetoothAdapter;
	private boolean mScanning;
	private static final int REQUEST_ENABLE_BT = 1;
	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 10000;

	public BluetoothPopup(Activity a) {
		acti = a;
		// Use this check to determine whether BLE is supported on the device.  Then you can
		// selectively disable BLE-related features.
		if (!a.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(a, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
			a.finish();
		}

		// Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
		// BluetoothAdapter through BluetoothManager.
		final BluetoothManager bluetoothManager = (BluetoothManager) a.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		// Checks if Bluetooth is supported on the device.
		if (mBluetoothAdapter == null) {
			Toast.makeText(a, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
			a.finish();
			return;
		}

		// Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
		// fire an intent to display a dialog asking the user to grant permission to enable it.
		if (!mBluetoothAdapter.isEnabled()) {
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				a.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}

		mHandler = new Handler();

		ListView listview = a.findViewById(R.id.lvDeviceList);
		//리스트뷰와 리스트를 연결하기 위해 사용되는 어댑터
		adapter =new ArrayAdapter<>(a, R.layout.list_view_item, list);

		//리스트뷰의 어댑터를 지정해준다.
		listview.setAdapter(adapter);

		a.findViewById(R.id.btnRescan).setOnClickListener(v -> {
			start();
		});
		//리스트뷰의 아이템을 클릭시 해당 아이템의 문자열을 가져오기 위한 처리
		listview.setOnItemClickListener((adapterView, view, position, id) -> {
//			bluetooth01.connectDevice(acti, bDeviceList.get(position));
			if (mScanning) {
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				mScanning = false;
			}
			Element elDevice = Config.selectedElBike.aChild("device");
			elDevice.sAttr("deviceName",bDeviceList.get(position).getName());
			elDevice.sAttr("deviceAddress",bDeviceList.get(position).getAddress());
//			Config.selectedElBike.sAttr("connectDevice","true");
			Config.saveConfig();
			Config.setSelectedBike();
//			UseGattAttributes.connect(bDeviceList.get(position).getAddress(), null);
			acti.finish();
		});
	}
	public void start() {
		list.clear();
		bDeviceList.clear();
		adapter.notifyDataSetChanged();
		scanLeDevice(true);
//		bluetoothScan.startLEScan(acti, (bluetoothScan.BLEDiscoveryCallback) acti);

	}
	public void stop() {
		mScanning = false;
		mBluetoothAdapter.stopLeScan(mLeScanCallback);
//		bluetoothScan.stopLEScan();
	}
	private void scanLeDevice(final boolean enable) {
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(() -> {
				mScanning = false;
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				acti.invalidateOptionsMenu();
				acti.findViewById(R.id.btnRescan).setVisibility(View.VISIBLE);
			}, SCAN_PERIOD);
			acti.findViewById(R.id.btnRescan).setVisibility(View.GONE);
			mScanning = true;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				BluetoothLeScanner BLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
				ScanSettings scanSettings = new ScanSettings.Builder().build();
				ScanFilter.Builder scanFilter = new ScanFilter.Builder();
				scanFilter.setServiceUuid(ParcelUuid.fromString(UseGattAttributes.CSC_SERVICE));
				BLEScanner.startScan(Collections.singletonList(scanFilter.build()), scanSettings, mScanCallback);
			} else {
				mBluetoothAdapter.startLeScan(new UUID[]{UUID.fromString(UseGattAttributes.CSC_SERVICE)}, mLeScanCallback);
			}
		} else {
			mScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
		acti.invalidateOptionsMenu();
	}

	@TargetApi(android.os.Build.VERSION_CODES.LOLLIPOP)
	private ScanCallback mScanCallback = new ScanCallback() {
		@Override
		public void onScanResult(int callbackType, final ScanResult result) {
			acti.runOnUiThread(() -> {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					if(bDeviceList.contains(result.getDevice())) return;
					if(Config.isDevice(result.getDevice().getAddress())) return;
					list.add(result.getDevice().getName() + "   "+ result.getDevice().getAddress());
					bDeviceList.add(result.getDevice());
					adapter.notifyDataSetChanged();
				}
			});
		}
	};
	// Device scan callback.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			acti.runOnUiThread(() -> {
				if(bDeviceList.contains(device)) return;
				if(Config.isDevice(device.getAddress())) return;
				list.add(device.getName() + "   "+ device.getAddress());
				bDeviceList.add(device);
				adapter.notifyDataSetChanged();
			});
		}
	};

//	static class ViewHolder {
//		TextView deviceName;
//		TextView deviceAddress;
//	}
}
