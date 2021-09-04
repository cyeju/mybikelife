package com.oren.util.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
//import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.content.*;
import android.os.Build;
//import android.os.Handler;
//import android.support.v7.app.AlertDialog;
//import android.util.Log;
import com.oren.mybikelife.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class bluetoothScan {
    public static BluetoothAdapter bluetoothAdapter; // 블루투스 어댑터

    // 블루투스 BLE 장치만 검색 bluetoothAdapter
    private static ScanCallback mScanCallback;
    private static BluetoothAdapter.LeScanCallback mLeScanCallback;
    public static void startLEScan(Activity a, final BLEDiscoveryCallback callback) {
        // 블루투스 활성화하기
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // 블루투스 어댑터를 디폴트 어댑터로 설정
        if(bluetoothAdapter == null) { // 디바이스가 블루투스를 지원하지 않을 때
            android.widget.Toast.makeText(a, a.getString(R.string.bluetooth_not_msg), android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BluetoothLeScanner BLEScanner = bluetoothAdapter.getBluetoothLeScanner();

            mScanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    BluetoothDevice device = result.getDevice();
//                    Log.d("xy", device.getAddress()+":"+device.getName());
                    callback.onDeviceFound(device);
                }
            };
//            new ScanFilter().getServiceUuid().getUuid(),
            BLEScanner.startScan( mScanCallback);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    callback.onDeviceFound(device);
                }
            };
            bluetoothAdapter.startLeScan(mLeScanCallback);
        }
    }
    public static void stopLEScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BluetoothLeScanner BLEScanner = bluetoothAdapter.getBluetoothLeScanner();
            BLEScanner.stopScan(mScanCallback);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            bluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }
    private static BluetoothSocket tryCreateSocket(BluetoothDevice device, UUID uuid, boolean secure) throws IOException {
        if (secure || Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
            return device.createRfcommSocketToServiceRecord(uuid);
        } else {
            return device.createInsecureRfcommSocketToServiceRecord(uuid);
        }
    }

    public interface BLEDiscoveryCallback {
        void onDeviceFound(BluetoothDevice bluetoothDevice);
    }

    // 블루투스 기기 스캔 전체
    public static void scanDevices(Context context, final BluetoothDiscoveryCallback callback) {
        BroadcastReceiver scanningBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    callback.onDeviceFound(device);
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                    context.unregisterReceiver(this);
                    callback.onDiscoveryFinished();
                }
            }
        };

        IntentFilter scanningItentFilter = new IntentFilter();
        scanningItentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        scanningItentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(scanningBroadcastReceiver, scanningItentFilter);

        BluetoothAdapter.getDefaultAdapter().startDiscovery();
    }

    private interface BluetoothDiscoveryCallback {
        void onDeviceFound(BluetoothDevice bluetoothDevice);
        void onDiscoveryFinished();
    }
}
