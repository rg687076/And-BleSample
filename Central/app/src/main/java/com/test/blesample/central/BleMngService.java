package com.test.blesample.central;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import java.util.List;

import static com.test.blesample.central.Constants.BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID;

/**
 * Created by itanbarpeled on 28/01/2018.
 */

public class BleMngService extends Service {
	/* サブクラス : BleMngService.LocalBinder */
	public class LocalBinder extends Binder {
		BleMngService getService() {
			return BleMngService.this;
		}
	}
	/* メッセージID */
	public final static String UWS_GATT_CONNECTED			= "com.tks.uws.GATT_CONNECTED";
	public final static String UWS_GATT_DISCONNECTED		= "com.tks.uws.GATT_DISCONNECTED";
	public final static String UWS_GATT_SERVICES_DISCOVERED	= "com.tks.uws.GATT_SERVICES_DISCOVERED";
	public final static String UWS_DATA_AVAILABLE			= "com.tks.uws.DATA_AVAILABLE";
	public final static String UWS_DATA						= "com.tks.uws.DATA";
	/* Serviceのお約束 */
	private final IBinder mBinder = new LocalBinder();
//	private int mConnectionState = STATE_DISCONNECTED;
	/* Bt通信メンバ */
	private BluetoothManager	mBtManager;
	private BluetoothAdapter	mBtAdapter;
	private BluetoothGatt		mBleGatt;
	private String				mBleDeviceAddr;
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			TLog.d("BluetoothGattCallback::onConnectionStateChange() {0} -> {1}", status, newState);
			TLog.d("BluetoothProfile.STATE_CONNECTING({0}) STATE_CONNECTED({1}) STATE_DISCONNECTING({2}) STATE_DISCONNECTED({3})", BluetoothProfile.STATE_CONNECTING, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.STATE_DISCONNECTING, BluetoothProfile.STATE_DISCONNECTED);
			/* Gattサーバ接続完了 */
			if(newState == BluetoothProfile.STATE_CONNECTED) {
//				mConnectionState = STATE_CONNECTED;
				/* Intent送信(Service->Activity) */
				sendBroadcast(new Intent(UWS_GATT_CONNECTED));
				TLog.d("GATTサーバ接続OK.");
				mBleGatt.discoverServices();
				TLog.d("Discovery開始");
			}
			/* Gattサーバ断 */
			else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//				mConnectionState = STATE_DISCONNECTED;
				TLog.d("GATTサーバ断.");
				/* Intent送信(Service->Activity) */
				sendBroadcast(new Intent(UWS_GATT_DISCONNECTED));
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			TLog.w("Discovery終了 result : {0}", status);
			if (status == BluetoothGatt.GATT_SUCCESS) {
				/* Intent送信(Service->Activity) */
				sendBroadcast(new Intent(UWS_GATT_SERVICES_DISCOVERED));
			}
		}

		/* 読込み要求の応答 */
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			TLog.d("読込み要求の応答 status=", status);
			if (status == BluetoothGatt.GATT_SUCCESS) {
				rcvData(UWS_DATA_AVAILABLE, characteristic);
			}
			else {
				TLog.d("onCharacteristicRead GATT_FAILURE");
			}
			TLog.d("BluetoothGattCallback::onCharacteristicRead() e");
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			TLog.d("ペリフェラルからの受信");
			rcvData(UWS_DATA_AVAILABLE, characteristic);
		}
	};

	/* データ受信(peripheral -> Service -> Activity) */
	private void rcvData(final String action, final BluetoothGattCharacteristic characteristic) {
		Intent intent = new Intent(action);
		if (BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
			/* 受信データ取出し */
			int flag = characteristic.getProperties();
			int format = ((flag & 0x01) != 0) ? BluetoothGattCharacteristic.FORMAT_UINT16 : BluetoothGattCharacteristic.FORMAT_UINT8;
			int msg = characteristic.getIntValue(format, 0);
			TLog.d("message: {0}", msg);
			/* 受信データ取出し */
			intent.putExtra(UWS_DATA, msg);
		}
		/* ↓↓↓この処理はサンプルコード。実際には動かない。 */
		else {
			final byte[] data = characteristic.getValue();
			if (data != null && data.length > 0) {
				final StringBuilder stringBuilder = new StringBuilder(data.length);
				for (byte byteChar : data)
					stringBuilder.append(String.format("%02X ", byteChar));
				intent.putExtra(UWS_DATA, -1);
			}
		}
		/* ↑↑↑この処理はサンプルコード。実際には動かない。 */
		/* 受信データ中継 */
		sendBroadcast(intent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		TLog.d("onBind() s-e");
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		close();
		return super.onUnbind(intent);
	}

	/* Bluetooth初期化 */
	public boolean initBle() {
		mBtManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		if(mBtManager == null) return false;

		mBtAdapter = mBtManager.getAdapter();
		if(mBtAdapter == null) return false;

		return true;
	}

	/* Bluetooth接続 */
	public boolean connect(final String address) {
		if (mBtAdapter == null || address == null) {
			TLog.d("initBle()を呼び出す前に、この関数を呼び出した。");
			throw new IllegalStateException("initBle()を呼び出す前に、この関数を呼び出した。");
		}

		/* 再接続処理 */
		if (mBleDeviceAddr != null && address.equals(mBleDeviceAddr) && mBleGatt != null) {
			TLog.d("接続済のデバイスに再接続します。");
			if (mBleGatt.connect()) {
//				mConnectionState = STATE_CONNECTING;
				TLog.d("接続しました。");
				return true;
			}
			else {
				TLog.d("接続失敗。");
				return false;
			}
		}

		/* 初回接続 */
		BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
		if (device == null) {
			TLog.d("デバイス({0})が見つかりません。接続できませんでした。", address);
			return false;
		}

		/* デバイスに直接接続したい時に、autoConnectをfalseにする。 */
		/* デバイスが使用可能になったら自動的にすぐに接続する様にする時に、autoConnectをtrueにする。 */
		mBleGatt = device.connectGatt(getApplicationContext(), false, mGattCallback);
		mBleDeviceAddr = address;
//		mConnectionState = STATE_CONNECTING;
		TLog.d("GATTサーバ接続開始.address={0}", address);

		return true;
	}

	public void disconnect() {
		if (mBtAdapter == null || mBleGatt == null)
			return;
		mBleGatt.disconnect();
	}

	public void close() {
		if (mBleGatt == null)
			return;
		mBleGatt.close();
		mBleGatt = null;
	}

	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBtAdapter == null || mBleGatt == null) {
			TLog.d("Bluetooth not initialized");
			throw new IllegalStateException("Error!! Bluetooth not initialized!!");
		}

		mBleGatt.readCharacteristic(characteristic);
	}

	/* 指定CharacteristicのCallback登録 */
	public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
		TLog.d("setCharacteristicNotification() s");
		if (mBtAdapter == null || mBleGatt == null) {
			TLog.d("setCharacteristicNotification() e false");
			return;
		}

		mBleGatt.setCharacteristicNotification(characteristic, enabled);
		TLog.d("setCharacteristicNotification() e");
	}

	/* 対象デバイスの保有するサービスを取得 */
	public List<BluetoothGattService> getSupportedGattServices() {
		if (mBleGatt == null)
			return null;
		return mBleGatt.getServices();
	}
}
