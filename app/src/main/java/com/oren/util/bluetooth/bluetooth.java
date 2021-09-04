package com.oren.util.bluetooth;

import android.app.Activity;
import android.bluetooth.*;
import android.os.Build;
import android.util.Log;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class bluetooth {
    private static final int REQUEST_ENABLE_BT = 10; // 블루투스 활성화 상태
    private static BluetoothDevice bluetoothDevice; // 블루투스 디바이스
    private static BluetoothSocket bluetoothSocket = null; // 블루투스 소켓
    private static OutputStream outputStream = null; // 블루투스에 데이터를 출력하기 위한 출력 스트림
    private static InputStream inputStream = null; // 블루투스에 데이터를 입력하기 위한 입력 스트림
    private static Thread workerThread = null; // 문자열 수신에 사용되는 쓰레드
    private static byte[] readBuffer; // 수신 된 문자열을 저장하기 위한 버퍼
    private static int readBufferPosition; // 버퍼 내 문자 저장 위치
    private static Activity acti;

    private static UUID CE_UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb");
    private static UUID CE_UUID1 = UUID.fromString("00002a5b-0000-1000-8000-00805F9B34FB");
    private static UUID CE_UUID2 = UUID.fromString("00002a5c-0000-1000-8000-00805F9B34FB");
    private static UUID CE_UUID3 = UUID.fromString("00002a5d-0000-1000-8000-00805F9B34FB");
    private static UUID CE_UUID4 = UUID.fromString("00002a55-0000-1000-8000-00805F9B34FB");


    //  케이던스 및 스피드 센서 GATT
//    attr handle: 0x0001, end grp handle: 0x000b uuid: 00001800-0000-1000-8000-00805f9b34fb
//    attr handle: 0x000c, end grp handle: 0x000f uuid: 00001801-0000-1000-8000-00805f9b34fb
//    attr handle: 0x0010, end grp handle: 0x001a uuid: 00001816-0000-1000-8000-00805f9b34fb
//    attr handle: 0x001b, end grp handle: 0x0029 uuid: 0000180a-0000-1000-8000-00805f9b34fb
//    attr handle: 0x002a, end grp handle: 0xffff uuid: 0000180f-0000-1000-8000-00805f9b34fb

//    [CON][00:18:31:E4:D7:89][LE]> characteristics 0x0010 0x001a
//            [CON][00:18:31:E4:D7:89][LE]>
//    handle: 0x0011, char properties: 0x10, char value handle: 0x0012, uuid: 00002a5b-0000-1000-8000-00805F9B34FB
//    handle: 0x0014, char properties: 0x02, char value handle: 0x0015, uuid: 00002a5c-0000-1000-8000-00805F9B34FB
//    handle: 0x0016, char properties: 0x02, char value handle: 0x0017, uuid: 00002a5d-0000-1000-8000-00805F9B34FB
//    handle: 0x0018, char properties: 0x08, char value handle: 0x0019, uuid: 00002a55-0000-1000-8000-00805F9B34FB

    // 아직 구현 못함 연결 안된..
    static BluetoothGatt bgatt;
    public static void connectDevice(Activity a, BluetoothDevice bDevice) {
        acti = a;
        bluetoothDevice = bDevice;
        if(bluetoothDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                bluetoothDevice.createBond();
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            bgatt = bluetoothDevice.connectGatt(a, false, mGattCallback);
        }
    }

    private static final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            String intentAction;/=
            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                intentAction = ACTION_GATT_CONNECTED;
//                mConnectionState = STATE_CONNECTED;
//                broadcastUpdate(intentAction);
                Log.d("xy", "GATT 서버에 연결한다");
                // Attempts to discover services after successful connection.
                bgatt.writeCharacteristic(new BluetoothGattCharacteristic(CE_UUID, 1, 1));
                bgatt.readCharacteristic(new BluetoothGattCharacteristic(CE_UUID, 1, 1));
                Log.d("xy", "서비스 검색을 시작해볼까 한다:" +
                        bgatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                intentAction = ACTION_GATT_DISCONNECTED;
//                mConnectionState = STATE_DISCONNECTED;
//                Log.i(TAG, "GATT 서버에서 연결이 끊어졌다");
//                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                Log.i("xy", gatt.re);
//                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.d("xy", "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("xy", characteristic.toString());
//                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            } else {
                Log.d("xy", "onServicesDiscovered received: " + status);
            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("xy", characteristic.toString());
//                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            } else {
                Log.d("xy", "onServicesDiscovered received: " + status);
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.d("xy", characteristic.toString());
//            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };
}
