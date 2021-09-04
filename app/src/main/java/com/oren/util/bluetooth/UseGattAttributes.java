/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oren.util.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.*;
import android.os.IBinder;
import android.util.Log;
import com.oren.mybikelife.MainActivity;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class UseGattAttributes {
    private static HashMap<String, String> attributes = new HashMap<>();
    public static String CSC_SERVICE = "00001816-0000-1000-8000-00805f9b34fb";
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CSC_MEASUREMENT = "00002a5b-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static Activity mainActi;
    public static Activity mainActi1;
    static {
        // Sample Services.
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put("00001816-0000-1000-8000-00805f9b34fb", "Cadence Service");
        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put(CSC_MEASUREMENT, "Cadence Measurement");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

//    public static String lookup(String uuid, String defaultName) {
//        String name = attributes.get(uuid);
//        return name == null ? defaultName : name;
//    }

    public static BluetoothLeService mBluetoothLeService;
//    public static BluetoothLeService mBluetoothLeService1;
    public static String mDeviceAddress, mDeviceAddress1;
    public static boolean mConnected = false;
//    public static boolean mConnected1 = false;
    private static BluetoothGattCharacteristic mNotifyCharacteristic;
    private static BluetoothGattCharacteristic mNotifyCharacteristic1;
    // Code to manage Service lifecycle.
    public static final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            if(mBluetoothLeService == null) {
                mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
                if (!mBluetoothLeService.initialize()) {
                    Log.d("xy", "Unable to initialize Bluetooth");
                }
                // Automatically connects to the device upon successful start-up initialization.
                Log.d("xy", "initialize Bluetooth 0");
                mBluetoothLeService.setEXTRA_DATA("com.oren.util.bluetooth.le.EXTRA_DATA");
                mBluetoothLeService.connect(mDeviceAddress, mDeviceAddress1);
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
//            mBluetoothLeService1 = null;
        }
    };
    public static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    public static void connect(String address, String address1) {
        mDeviceAddress = address;
        mDeviceAddress1 = address1;
        disconnect();
        Intent gattServiceIntent = new Intent(mainActi, BluetoothLeService.class);
        mainActi.bindService(gattServiceIntent, UseGattAttributes.mServiceConnection, Context.BIND_AUTO_CREATE);
        mainActi.registerReceiver(((MainActivity)mainActi).getMGattUpdateReceiver(), makeGattUpdateIntentFilter());
        if (!mConnected && mBluetoothLeService != null) {
            mConnected = mBluetoothLeService.connect(mDeviceAddress, mDeviceAddress1);
            Log.d("xy", "Connect 0 request result = " + mConnected);
        }
    }

    public static void disconnect() {
        if (mBluetoothLeService != null) {
           mBluetoothLeService.disconnect();
        }
        mConnected = false;
    }
    public static void startService() {
        final BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.fromString(CSC_SERVICE),0,0);
        final int charaProp = characteristic.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            // If there is an active notification on a characteristic, clear
            // it first so it doesn't update the data field on the user interface.
            if (mNotifyCharacteristic != null) {
                mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false, 0);
                mNotifyCharacteristic = null;
            }
            mBluetoothLeService.readCharacteristic(characteristic, 0);
        }
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristic = characteristic;
            mBluetoothLeService.setCharacteristicNotification(characteristic, true, 0);
        }
        if(mDeviceAddress1 != null) {
            final BluetoothGattCharacteristic characteristic1 = new BluetoothGattCharacteristic(UUID.fromString(CSC_SERVICE), 0, 0);
            final int charaProp1 = characteristic1.getProperties();
            if ((charaProp1 | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                if (mNotifyCharacteristic1 != null) {
                    mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic1, false, 1);
                    mNotifyCharacteristic1 = null;
                }
                mBluetoothLeService.readCharacteristic(characteristic1, 1);
            }
            if ((charaProp1 | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                mNotifyCharacteristic1 = characteristic1;
                mBluetoothLeService.setCharacteristicNotification(characteristic1, true, 1);
            }
        }
    }
    public static void receiveData() {
        try {
            List<BluetoothGattService> gattServices = mBluetoothLeService.getSupportedGattServices(0);
            BluetoothGattCharacteristic characteristic = gattServices.get(0).getCharacteristic(UUID.fromString(CSC_MEASUREMENT));
            for (BluetoothGattService gattService : gattServices) {
                if (gattService.getUuid().toString().equals(CSC_SERVICE)) {
                    List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
//                    Log.d("xy", "test ::: "+gattCharacteristic.getUuid().toString());
                        if (gattCharacteristic.getUuid().toString().equals(CSC_MEASUREMENT)) {
                            characteristic = gattCharacteristic;
                            break;
                        }
                    }
                    break;
                }
            }
            if (characteristic == null) return;
            final int charaProp = characteristic.getProperties();
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                if (mNotifyCharacteristic != null) {
                    mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false, 0);
                    mNotifyCharacteristic = null;
                }
                mBluetoothLeService.readCharacteristic(characteristic, 0);
            }
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                mNotifyCharacteristic = characteristic;
                mBluetoothLeService.setCharacteristicNotification(characteristic, true, 0);
            }
        } catch(Exception e) {
            Log.d("xy", e.toString());
        }
        if (mDeviceAddress1 != null) {
            try {
                List<BluetoothGattService> gattServices1 = mBluetoothLeService.getSupportedGattServices(1);
//            if(gattServices1.get(0) == null) return;
                BluetoothGattCharacteristic characteristic1 = gattServices1.get(0).getCharacteristic(UUID.fromString(CSC_MEASUREMENT));
                for (BluetoothGattService gattService : gattServices1) {
                    if (gattService.getUuid().toString().equals(CSC_SERVICE)) {
                        List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
//                        Log.d("xy", "test ::: "+gattCharacteristic.getUuid().toString());
                            if (gattCharacteristic.getUuid().toString().equals(CSC_MEASUREMENT)) {
                                characteristic1 = gattCharacteristic;
                                break;
                            }
                        }
                        break;
                    }
                }

                if (characteristic1 == null) return;
                final int charaProp1 = characteristic1.getProperties();
                if ((charaProp1 | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    if (mNotifyCharacteristic1 != null) {
                        mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic1, false, 1);
                        mNotifyCharacteristic1 = null;
                    }
                    mBluetoothLeService.readCharacteristic(characteristic1, 1);
                }
                if ((charaProp1 | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    mNotifyCharacteristic1 = characteristic1;
                    mBluetoothLeService.setCharacteristicNotification(characteristic1, true, 1);
                }
            } catch(Exception e) {
                Log.d("xy", e.toString());
            }
        }
    }
}
