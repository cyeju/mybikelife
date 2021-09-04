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

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = "xy";//BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
//    private BluetoothManager mBluetoothManager1;
//    private BluetoothAdapter mBluetoothAdapter1;
    private String mBluetoothDeviceAddress;
    private String mBluetoothDeviceAddress1;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGatt mBluetoothGatt1;
    private int mConnectionState = STATE_DISCONNECTED;
    private int mConnectionState1 = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "com.oren.util.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.oren.util.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.oren.util.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.oren.util.bluetooth.le.ACTION_DATA_AVAILABLE";

    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString(UseGattAttributes.HEART_RATE_MEASUREMENT);
    public final static UUID UUID_CSC_MEASUREMENT = UUID.fromString(UseGattAttributes.CSC_MEASUREMENT);

    public String EXTRA_DATA = "";
    public void setEXTRA_DATA(String extra_data) {
        EXTRA_DATA = extra_data;
    }
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.d(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.d(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.d(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.d(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };
    private final BluetoothGattCallback mGattCallback1 = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState1 = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.d(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.d(TAG, "Attempting to start service discovery:" + mBluetoothGatt1.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState1 = STATE_DISCONNECTED;
                Log.d(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.d(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        if (UUID_CSC_MEASUREMENT.equals(characteristic.getUuid())) {
            int wheelrev = -1, wheeltime=-1, crankrev=-1, cranktime=-1;
            int flag1 = characteristic.getProperties();
            int flag2 = characteristic.getValue().length;
//            android.util.Log.d("xy", flag1+":"+flag2);
            try {
                if (flag2 == 5) {
                    crankrev = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
                    cranktime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 3);
                } else if (flag2 == 7) {
                    wheelrev = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT32, 1);
                    wheeltime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 5);
                } else {
                    wheelrev = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT32, 1);
                    wheeltime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 5);
                    crankrev = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 7);
                    cranktime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 9);
                }
            } catch(Exception e) {
            }
            Log.d(TAG, String.format("Received flag : %d    Wheel r : %d  t : %d       Crank r : %d   t : %d", (flag1 & 0x01), wheelrev, wheeltime, crankrev, cranktime));
            intent.putExtra(EXTRA_DATA, String.valueOf((flag1 & 0x01))+":"+String.valueOf(wheelrev)+":"+String.valueOf(wheeltime)+":"+String.valueOf(crankrev)+":"+String.valueOf(cranktime));
        } else if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        }else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
//        if(mBluetoothDeviceAddress1 != null) {
//            if (mBluetoothManager1 == null) {
//                mBluetoothManager1 = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//                if (mBluetoothManager1 == null) {
//                    Log.e(TAG, "Unable to initialize BluetoothManager.");
//                    return false;
//                }
//            }
//
//            mBluetoothAdapter1 = mBluetoothManager1.getAdapter();
//            if (mBluetoothAdapter1 == null) {
//                Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
//                return false;
//            }
//        }
        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address, final String address1) {

        BluetoothGatt bg = null;

        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        bg = mBluetoothGatt;
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && bg != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (bg.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        bg = device.connectGatt(this, false, mGattCallback);
        mBluetoothGatt = bg;
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        if(address1 != null && !address1.equals("")) {
            bg = mBluetoothGatt1;
            // Previously connected device.  Try to reconnect.
            if (mBluetoothDeviceAddress1 != null && address1.equals(mBluetoothDeviceAddress1) && bg != null) {
                Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
                if (bg.connect()) {
                    mConnectionState1 = STATE_CONNECTING;
                    return true;
                } else {
                    return false;
                }
            }
            Log.w(TAG, "?????????"+address1);
            final BluetoothDevice device1 = mBluetoothAdapter.getRemoteDevice(address1);
            if (device1 == null) {
                Log.w(TAG, "Device1 not found.  Unable to connect.");
                return false;
            }
            // We want to directly connect to the device, so we are setting the autoConnect
            // parameter to false.
            bg = device1.connectGatt(this, false, mGattCallback1);
            mBluetoothGatt1 = bg;
            Log.d(TAG, "Trying to create a other connection.");
            mBluetoothDeviceAddress1 = address1;
            mConnectionState1 = STATE_CONNECTING;
        }
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "disconnect BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        if (mBluetoothAdapter == null || mBluetoothGatt1 == null) {
            Log.w(TAG, "disconnect BluetoothAdapter1 not initialized");
            return;
        }
        mBluetoothGatt1.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        if(mBluetoothGatt1 != null) {
            mBluetoothGatt1.close();
            mBluetoothGatt1 = null;
        }
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic, int num) {
        if(num == 0) {
            if (mBluetoothAdapter == null || mBluetoothGatt == null) {
                Log.w(TAG, "readCharacteristic BluetoothAdapter not initialized");
                return;
            }
            mBluetoothGatt.readCharacteristic(characteristic);
        } else {
            if (mBluetoothAdapter == null || mBluetoothGatt1 == null) {
                Log.w(TAG, "readCharacteristic BluetoothAdapter1 not initialized");
                return;
            }
            mBluetoothGatt1.readCharacteristic(characteristic);
        }
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled, int num) {
        if(num == 0) {
            if (mBluetoothAdapter == null || mBluetoothGatt == null) {
                Log.w(TAG, "setCharacteristicNotification BluetoothAdapter not initialized");
                return;
            }
            mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

            // This is specific to CSC Measurement.
            if (UUID_CSC_MEASUREMENT.equals(characteristic.getUuid())) {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(UseGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
            }
        } else {
            if (mBluetoothAdapter == null || mBluetoothGatt1 == null) {
                Log.w(TAG, "setCharacteristicNotification BluetoothAdapter1 not initialized");
                return;
            }
            mBluetoothGatt1.setCharacteristicNotification(characteristic, enabled);

            // This is specific to CSC Measurement.
            if (UUID_CSC_MEASUREMENT.equals(characteristic.getUuid())) {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(UseGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt1.writeDescriptor(descriptor);
            }
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices(int num) {
        if(num == 0) {
            if (mBluetoothGatt == null) return null;
            return mBluetoothGatt.getServices();
        }else {
            if (mBluetoothGatt1 == null) return null;
            return mBluetoothGatt1.getServices();
        }
    }
}
