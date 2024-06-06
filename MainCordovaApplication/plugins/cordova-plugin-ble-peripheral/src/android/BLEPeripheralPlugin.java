// (c) 2018 Don Coleman
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.megster.cordova.ble.peripheral;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.util.Base64;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@SuppressLint("MissingPermission,LogNotTimber")
public class BLEPeripheralPlugin extends CordovaPlugin {
	// actions
	private static final String CREATE_SERVICE = "createService";
	private static final String CREATE_SERVICE_FROM_JSON = "createServiceFromJSON";
	private static final String REMOVE_SERVICE = "removeService";
	private static final String REMOVE_ALL_SERVICES = "removeAllServices";
	private static final String ADD_CHARACTERISTIC = "addCharacteristic";
	private static final String PUBLISH_SERVICE = "publishService";
	private static final String START_ADVERTISING = "startAdvertising";
	private static final String STOP_ADVERTISING = "stopAdvertising";
	private static final String SET_CHARACTERISTIC_VALUE = "setCharacteristicValue";

	private static final String SET_CHARACTERISTIC_VALUE_CHANGED_LISTENER = "setCharacteristicValueChangedListener";

	// 0x2902 https://www.bluetooth.com/specifications/gatt/descriptors
	private static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	// private static final String START_STATE_NOTIFICATIONS = "startStateNotifications";
	// private static final String STOP_STATE_NOTIFICATIONS = "stopStateNotifications";
	private static final String SET_BLUETOOTH_STATE_CHANGED_LISTENER = "setBluetoothStateChangedListener";

	private static final String SETTINGS = "showBluetoothSettings";
	private static final String ENABLE = "enable";

	private static final String BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT";
	private static final String BLUETOOTH_ADVERTISE = "android.permission.BLUETOOTH_ADVERTISE";
	private static final int REQUEST_BLUETOOTH_CONNECT = 42;
	private static final int REQUEST_BLUETOOTH_ADVERTISE = 43;
	private static int COMPILE_SDK_VERSION = -1;

	// callbacks
	private CallbackContext enableBluetoothCallback;
	private CallbackContext characteristicValueChangedCallback;
	private CallbackContext advertisingStartedCallback;

	private static final String TAG = "BLEPeripheral";
	private static final int REQUEST_ENABLE_BLUETOOTH = 17;

	private BluetoothAdapter bluetoothAdapter;
	private BluetoothGattServer gattServer;

	private final Map<UUID, BluetoothGattService> services = new HashMap<>();
	private final Set<BluetoothDevice> registeredDevices = new HashSet<>();

	// Bluetooth state notification
	private CallbackContext stateCallback;
	private BroadcastReceiver stateReceiver;
	private final Map<Integer, String> bluetoothStates = new Hashtable<Integer, String>() {{
		put(BluetoothAdapter.STATE_OFF, "off");
		put(BluetoothAdapter.STATE_TURNING_OFF, "turningOff");
		put(BluetoothAdapter.STATE_ON, "on");
		put(BluetoothAdapter.STATE_TURNING_ON, "turningOn");
	}};

	private CallbackContext permissionCallback;
	private String action;
	private CordovaArgs args;
	private BluetoothManager bluetoothManager;

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
	}

	@Override
	protected void pluginInitialize() {
		if (COMPILE_SDK_VERSION == -1) {
			Context context = cordova.getContext();
			COMPILE_SDK_VERSION = context.getApplicationContext().getApplicationInfo().targetSdkVersion;
		}
	}

	@Override
	public void onDestroy() {
		removeStateListener();
	}

	@Override
	public void onReset() {
		removeStateListener();
	}

	@Override
	public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
		LOG.d(TAG, "action = " + action);

		//"initial setup" without bt permissions needed
		if (bluetoothAdapter == null) {
			Activity activity = cordova.getActivity();
			bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
			if (bluetoothManager == null) {
				LOG.e(TAG, "bluetoothManager is null");
				callbackContext.error("Unable to get the Bluetooth Manager");
				return false;
			}
			bluetoothAdapter = bluetoothManager.getAdapter();
		}

		if (action.equals(SET_CHARACTERISTIC_VALUE_CHANGED_LISTENER)) {

			characteristicValueChangedCallback = callbackContext;
			return true;
		} else if (action.equals(SET_BLUETOOTH_STATE_CHANGED_LISTENER)) {

			if (this.stateCallback != null) {
				callbackContext.error("State callback already registered.");
			} else {
				this.stateCallback = callbackContext;
				addStateListener();
				sendBluetoothStateChange(bluetoothAdapter.getState());
			}
			return true;
		}

		//everything below here needs bt connect permissions

		boolean hasConnectPermission = PermissionHelper.hasPermission(this, BLUETOOTH_CONNECT);
		if (!hasConnectPermission && COMPILE_SDK_VERSION >= 31 && Build.VERSION.SDK_INT >= 31) {
			this.permissionCallback = callbackContext;
			this.action = action;
			this.args = args;

			PermissionHelper.requestPermission(this, REQUEST_BLUETOOTH_CONNECT, BLUETOOTH_CONNECT);
			return true;
		}

		if (gattServer == null) {
			Activity activity = cordova.getActivity();
			boolean hardwareSupportsBLE = activity.getApplicationContext()
					.getPackageManager()
					.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);

			if (!hardwareSupportsBLE) {
				LOG.e(TAG, "This hardware does not support Bluetooth Low Energy");
				callbackContext.error("This hardware does not support Bluetooth Low Energy");
				return false;
			}

			boolean hardwareSupportsPeripherals = bluetoothAdapter.isMultipleAdvertisementSupported();
			if (!hardwareSupportsPeripherals) {
				String errorMessage = "This hardware does not support creating Bluetooth Low Energy peripherals";
				LOG.e(TAG, errorMessage);
				callbackContext.error(errorMessage);
				return false;
			}

			//bluetooth connect permission
			gattServer = bluetoothManager.openGattServer(cordova.getContext(), gattServerCallback);

		}

		if (action.equals(CREATE_SERVICE)) {

			UUID serviceUUID = uuidFromString(args.getString(0));

			BluetoothGattService service = new BluetoothGattService(
					serviceUUID,
					BluetoothGattService.SERVICE_TYPE_PRIMARY);

			services.put(serviceUUID, service);

			callbackContext.success();

			return true;
		} else if (action.equals(REMOVE_SERVICE)) {

			UUID serviceUUID = uuidFromString(args.getString(0));
			BluetoothGattService service = services.get(serviceUUID);

			if (service == null) {
				callbackContext.error("Service " + serviceUUID + " not found");
				return /* validAction */ true; // stop processing because of error
			}

			boolean success = gattServer.removeService(service);

			if (success) {
				services.remove(serviceUUID);
				callbackContext.success();
			} else {
				callbackContext.error("Error removing " + serviceUUID + " to GATT Server");
			}
			return true;

		} else if (action.equals(REMOVE_ALL_SERVICES)) {
			gattServer.clearServices();
			callbackContext.success();
			return true;

		} else if (action.contentEquals(ADD_CHARACTERISTIC)) {

			UUID serviceUUID = uuidFromString(args.getString(0));
			UUID characteristicUUID = uuidFromString(args.getString(1));
			int properties = args.getInt(2);
			int permissions = args.getInt(3);

			BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
					characteristicUUID,
					properties,
					permissions);
			BluetoothGattService service = services.get(serviceUUID);

			if (service == null) {
				callbackContext.error("service not found");
				return true;
			}

			service.addCharacteristic(characteristic);

			// If notify or indicate, we need to add the 2902 descriptor
			if (isNotify(characteristic) || isIndicate(characteristic)) {
				characteristic.addDescriptor(createClientCharacteristicConfigurationDescriptor());
			}

			callbackContext.success();
			return true;

		} else if (action.equals(CREATE_SERVICE_FROM_JSON)) {

			JSONObject json = args.getJSONObject(0);
			LOG.d(TAG, json.toString());

			try {
				UUID serviceUUID = uuidFromString(json.getString("uuid"));
				LOG.d(TAG, "Creating service " + serviceUUID);
				BluetoothGattService service = new BluetoothGattService(serviceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

				JSONArray characteristicArray = json.getJSONArray("characteristics");
				for (int i = 0; i < characteristicArray.length(); i++) {
					JSONObject jsonObject = characteristicArray.getJSONObject(i);
					UUID uuid = uuidFromString(jsonObject.getString("uuid"));
					int properties = jsonObject.getInt("properties");
					int permissions = jsonObject.getInt("permissions");
					LOG.d(TAG, "Adding characteristic " + uuid + " properties=" + properties + " permissions=" + permissions);
					BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(uuid, properties, permissions);

					// If notify or indicate, add the 2902 descriptor
					if (isNotify(characteristic) || isIndicate(characteristic)) {
						characteristic.addDescriptor(createClientCharacteristicConfigurationDescriptor());
					}

					// TODO handle JSON without descriptors
					JSONArray descriptorsArray = jsonObject.getJSONArray("descriptors");
					for (int j = 0; j < descriptorsArray.length(); j++) {
						JSONObject jsonDescriptor = descriptorsArray.getJSONObject(j);

						UUID descriptorUUID = uuidFromString(jsonDescriptor.getString("uuid"));

						// TODO descriptor permissions should be optional in the JSON
						//int descriptorPermissions = jsonDescriptor.getInt("permissions");
						int descriptorPermissions = BluetoothGattDescriptor.PERMISSION_READ; // | BluetoothGattDescriptor.PERMISSION_WRITE;

						// future versions need to handle more than Strings
						String descriptorValue = jsonDescriptor.getString("value");
						LOG.d(TAG, "Adding descriptor " + descriptorUUID + " permissions=" + permissions + " value=" + descriptorValue);

						BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(descriptorUUID, descriptorPermissions);

						if (!characteristic.addDescriptor(descriptor)) {
							callbackContext.error("Failed to add descriptor " + descriptorValue);
							return /*valid action */ true; // stop processing because of error
						}

						if (!descriptor.setValue(descriptorValue.getBytes())) {
							callbackContext.error("Failed to set descriptor value to " + descriptorValue);
							return /*valid action */ true; // stop processing because of error
						}

					}

					service.addCharacteristic(characteristic);

				}

				services.put(serviceUUID, service);

				if (gattServer.addService(service)) {
					LOG.d(TAG, "Successfully added service " + serviceUUID);
					callbackContext.success();
				} else {
					callbackContext.error("Error adding " + service.getUuid() + " to GATT Server");
				}

			} catch (JSONException e) {
				LOG.e(TAG, "Invalid JSON for Service", e);
				e.printStackTrace();
				callbackContext.error(e.getMessage());
			}
			return true;

		} else if (action.equals(PUBLISH_SERVICE)) {

			UUID serviceUUID = uuidFromString(args.getString(0));
			BluetoothGattService service = services.get(serviceUUID);

			if (service == null) {
				callbackContext.error("Service " + serviceUUID + " not found");
				return /* validAction */ true; // stop processing because of error
			}

			boolean success = gattServer.addService(service);

			if (success) {
				callbackContext.success();
			} else {
				callbackContext.error("Error adding " + serviceUUID + " to GATT Server");
			}

			return true;
		} else if (action.equals(START_ADVERTISING)) {

			boolean hasAdvertisingPermission = PermissionHelper.hasPermission(this, BLUETOOTH_ADVERTISE);
			if (!hasAdvertisingPermission && COMPILE_SDK_VERSION >= 31 && Build.VERSION.SDK_INT >= 31) {
				this.permissionCallback = callbackContext;
				this.action = action;
				this.args = args;

				PermissionHelper.requestPermission(this, REQUEST_BLUETOOTH_ADVERTISE, BLUETOOTH_ADVERTISE);
				return true;
			}

			String advertisedName = args.getString(1);
			UUID serviceUUID = uuidFromString(args.getString(0));

			bluetoothAdapter.setName(advertisedName);

			BluetoothLeAdvertiser bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

			AdvertiseData advertisementData = getAdvertisementData(serviceUUID);
			AdvertiseSettings advertiseSettings = getAdvertiseSettings();

			bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertisementData, advertiseCallback);

			advertisingStartedCallback = callbackContext;

			return true;
		} else if (action.equals(STOP_ADVERTISING)) {
			boolean hasAdvertisingPermission = PermissionHelper.hasPermission(this, BLUETOOTH_ADVERTISE);
			if (!hasAdvertisingPermission && COMPILE_SDK_VERSION >= 31 && Build.VERSION.SDK_INT >= 31) {
				this.permissionCallback = callbackContext;
				this.action = action;
				this.args = args;

				PermissionHelper.requestPermission(this, REQUEST_BLUETOOTH_ADVERTISE, BLUETOOTH_ADVERTISE);
				return true;
			}

			BluetoothLeAdvertiser bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

			bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);

			callbackContext.success();

			return true;
		} else if (action.equals(SET_CHARACTERISTIC_VALUE)) {

			UUID serviceUUID = uuidFromString(args.getString(0));
			UUID characteristicUUID = uuidFromString(args.getString(1));
			byte[] value = args.getArrayBuffer(2);

			BluetoothGattService service = services.get(serviceUUID);
			if (service == null) {
				callbackContext.error("Service " + serviceUUID + " not found");
				return /* validAction */ true; // stop processing because of error
			}

			BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);

			if (characteristic == null) {
				callbackContext.error("Characteristic " + characteristicUUID + " not found on service " + serviceUUID);
				return /* validAction */ true; // stop processing because of error
			}

			characteristic.setValue(value);

			if (isNotify(characteristic) || isIndicate(characteristic)) {
				notifyRegisteredDevices(characteristic);
			}

			callbackContext.success();

			return true;
		} else if (action.equals(SETTINGS)) {

			Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
			cordova.getActivity().startActivity(intent);
			callbackContext.success();

			return true;
		} else if (action.equals(ENABLE)) {

			enableBluetoothCallback = callbackContext;
			Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			cordova.startActivityForResult(this, intent, REQUEST_ENABLE_BLUETOOTH);

			return true;
		} else {

			return false;

		}
	}


	private void onBluetoothStateChange(Intent intent) {
		final String action = intent.getAction();

		if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
			final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
			sendBluetoothStateChange(state);
		}
	}

	private void sendBluetoothStateChange(int state) {
		if (this.stateCallback != null) {
			PluginResult result = new PluginResult(PluginResult.Status.OK, this.bluetoothStates.get(state));
			result.setKeepCallback(true);
			this.stateCallback.sendPluginResult(result);
		}
	}

	private void addStateListener() {
		if (this.stateReceiver == null) {
			this.stateReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					onBluetoothStateChange(intent);
				}
			};
		}

		try {
			IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
			webView.getContext().registerReceiver(this.stateReceiver, intentFilter);
		} catch (Exception e) {
			LOG.e(TAG, "Error registering state receiver: " + e.getMessage(), e);
		}
	}

	private void removeStateListener() {
		if (this.stateReceiver != null) {
			try {
				webView.getContext().unregisterReceiver(this.stateReceiver);
			} catch (Exception e) {
				LOG.e(TAG, "Error un-registering state receiver: " + e.getMessage(), e);
			}
		}
		this.stateCallback = null;
		this.stateReceiver = null;
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == REQUEST_ENABLE_BLUETOOTH) {

			if (resultCode == Activity.RESULT_OK) {
				LOG.d(TAG, "User enabled Bluetooth");
				if (enableBluetoothCallback != null) {
					enableBluetoothCallback.success();
				}
			} else {
				LOG.d(TAG, "User did *NOT* enable Bluetooth");
				if (enableBluetoothCallback != null) {
					enableBluetoothCallback.error("User did not enable Bluetooth");
				}
			}

			enableBluetoothCallback = null;
		}
	}

	private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
		@Override
		public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
			super.onConnectionStateChange(device, status, newState);
			LOG.d(TAG, "onConnectionStateChange status=" + status + "->" + newState);

			if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				registeredDevices.remove(device);
			}

		}

		@Override
		public void onServiceAdded(int status, BluetoothGattService service) {
			LOG.d(TAG, "onServiceAdded status=" + service + "->" + service);
			super.onServiceAdded(status, service);
		}

		@Override
		public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
			super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
			LOG.d(TAG, "onCharacteristicReadRequest requestId=" + requestId + " offset=" + offset);

			gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
		}

		@Override
		public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
			super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
			LOG.d(TAG, "onCharacteristicWriteRequest characteristic=" + characteristic.getUuid() + " value=" + Arrays.toString(value));

			if (characteristicValueChangedCallback != null) {
				try {
					JSONObject message = new JSONObject();
					message.put("service", characteristic.getService().getUuid().toString());
					message.put("characteristic", characteristic.getUuid().toString());
					message.put("value", byteArrayToJSON(value));

					PluginResult result = new PluginResult(PluginResult.Status.OK, message);
					result.setKeepCallback(true);
					characteristicValueChangedCallback.sendPluginResult(result);
				} catch (JSONException e) {
					LOG.e(TAG, "JSON encoding failed in onCharacteristicWriteRequest", e);
				}
			}

			if (responseNeeded) {
				gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
			}

		}

		@Override
		public void onNotificationSent(BluetoothDevice device, int status) {
			super.onNotificationSent(device, status);
			LOG.d(TAG, "onNotificationSent device=" + device + " status=" + status);
		}

		@Override
		public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
			super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
			LOG.d(TAG, "onDescriptorWriteRequest");
			LOG.d(TAG, Arrays.toString(value));

			if (CLIENT_CHARACTERISTIC_CONFIGURATION_UUID.equals(descriptor.getUuid())) {
				if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
					LOG.d(TAG, "Subscribe device to notifications: " + device);
					registeredDevices.add(device);
				} else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
					LOG.d(TAG, "Unsubscribe device from notifications: " + device);
					registeredDevices.remove(device);
				}

				if (responseNeeded) {
					gattServer.sendResponse(device,
							requestId,
							BluetoothGatt.GATT_SUCCESS,
							0,
							null);
				}
			} else {
				// TODO allow other descriptors to be written
				LOG.w(TAG, "Unknown descriptor write request");
				if (responseNeeded) {
					gattServer.sendResponse(device,
							requestId,
							BluetoothGatt.GATT_FAILURE,
							0,
							null);
				}
			}

		}

		@Override
		public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
			super.onDescriptorReadRequest(device, requestId, offset, descriptor);
			LOG.d(TAG, "onDescriptorReadRequest device=" + device + " descriptor=" + descriptor.getUuid());

			gattServer.sendResponse(device,
					requestId,
					BluetoothGatt.GATT_SUCCESS,
					0,
					descriptor.getValue());

		}

		@Override
		public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
			super.onExecuteWrite(device, requestId, execute);
			LOG.d(TAG, "onExecuteWrite");
		}

	};

	// https://github.com/don/uribeacon/blob/58c31cf28d06a80880b0ed46b005204821fd623f/beacons/android/app/src/main/java/org/uribeacon/example/beacon/UriBeaconAdvertiserActivity.java
	private AdvertiseData getAdvertisementData(UUID serviceUuid) {
		AdvertiseData.Builder builder = new AdvertiseData.Builder();
		builder.setIncludeTxPowerLevel(false); // reserve advertising space for URI

		builder.addServiceUuid(new ParcelUuid(serviceUuid)); // TODO accept multiple services in the future
		// builder.addServiceUuid(new ParcelUuid(serviceUuid1));
		builder.setIncludeDeviceName(true);
		return builder.build();
	}

	private AdvertiseSettings getAdvertiseSettings() {
		AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
		//builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
		builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
		builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
		builder.setConnectable(true);

		return builder.build();
	}

	private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
		@Override
		public void onStartSuccess(AdvertiseSettings settingsInEffect) {
			super.onStartSuccess(settingsInEffect);
			LOG.d(TAG, "onStartSuccess");
			if (advertisingStartedCallback != null) {
				advertisingStartedCallback.success();
			}
		}

		@Override
		public void onStartFailure(int errorCode) {
			super.onStartFailure(errorCode);
			LOG.d(TAG, "onStartFailure");
			if (advertisingStartedCallback != null) {
				advertisingStartedCallback.error(errorCode);
			}
		}
	};

	private void notifyRegisteredDevices(BluetoothGattCharacteristic characteristic) {
		boolean confirm = isIndicate(characteristic);

		for (BluetoothDevice device : registeredDevices) {
			gattServer.notifyCharacteristicChanged(device, characteristic, confirm);
		}
	}

	// Utils
	private UUID uuidFromString(String uuid) {
		return UUIDHelper.uuidFromString(uuid);
	}

	private boolean isNotify(BluetoothGattCharacteristic characteristic) {
		return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0);
	}

	private boolean isIndicate(BluetoothGattCharacteristic characteristic) {
		return ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0);
	}

	private JSONObject byteArrayToJSON(byte[] bytes) throws JSONException {
		JSONObject object = new JSONObject();
		object.put("CDVType", "ArrayBuffer");
		object.put("data", Base64.encodeToString(bytes, Base64.NO_WRAP));
		return object;
	}

	private BluetoothGattDescriptor createClientCharacteristicConfigurationDescriptor() {
		return new BluetoothGattDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION_UUID,
				BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
	}

	//TODO: Update permission handling when cdv-android platform is updated
	public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
		final CallbackContext callback = this.popPermissionsCallback();
		final String action = this.action;
		final CordovaArgs args = this.args;
		this.action = null;
		this.args = null;

		if (callback == null) {
			if (grantResults.length > 0) {
				// There are some odd happenings if permission requests are made while booting up capacitor
				LOG.w(TAG, "onRequestPermissionResult received with no pending callback");
			}
			return;
		}

		if (grantResults.length == 0) {
			callback.error("No permissions not granted.");
			return;
		}

		for (int i = 0; i < permissions.length; i++) {
			if (permissions[i].equals(BLUETOOTH_CONNECT) && grantResults[i] == PackageManager.PERMISSION_DENIED) {
				LOG.d(TAG, "User *rejected* Bluetooth_Connect Access");
				callback.error("Bluetooth Connect permission not granted.");
				return;
			} else if (permissions[i].equals(BLUETOOTH_ADVERTISE) && grantResults[i] == PackageManager.PERMISSION_DENIED) {
				LOG.d(TAG, "User *rejected* Bluetooth_Advertise Access");
				callback.error("Bluetooth Advertise permission not granted.");
				return;
			}
		}

		try {
			execute(action, args, callback);
		} catch (JSONException e) {
			callback.error(e.getMessage());
		}

	}

	private CallbackContext popPermissionsCallback() {
		final CallbackContext callback = this.permissionCallback;
		this.permissionCallback = null;
		return callback;
	}
}
