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
package org.apache.cordova.ble_peripheral

import ai.doma.miniappdemo.ext.logD
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.provider.Settings
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaArgs
import org.apache.cordova.CordovaInterface
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CordovaWebView
import org.apache.cordova.LOG
import org.apache.cordova.PermissionHelper
import org.apache.cordova.PluginResult
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Arrays
import java.util.Hashtable
import java.util.UUID


data class BLECharacteristicReadRequest(
    var contextID: UUID,
    var timeoutJob: Job,
    var device: BluetoothDevice,
    var requestId: Int,
    var offset: Int,
    var characteristic: BluetoothGattCharacteristic
)

@SuppressLint("MissingPermission,LogNotTimber")
class BLEPeripheralPlugin : CordovaPlugin() {
    // callbacks
    private var enableBluetoothCallback: CallbackContext? = null
    private var characteristicValueChangedCallback: CallbackContext? = null
    private var characteristicValueRequestedCallback: CallbackContext? = null
    private var advertisingStartedCallback: CallbackContext? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var gattServer: BluetoothGattServer? = null
    private val services: MutableMap<UUID, BluetoothGattService> = HashMap()
    private val registeredDevices: MutableSet<BluetoothDevice> = HashSet()

    // Bluetooth state notification
    private var stateCallback: CallbackContext? = null
    private var stateReceiver: BroadcastReceiver? = null
    private val bluetoothStates: Map<Int, String> = object : Hashtable<Int, String>() {
        init {
            put(BluetoothAdapter.STATE_OFF, "off")
            put(BluetoothAdapter.STATE_TURNING_OFF, "turningOff")
            put(BluetoothAdapter.STATE_ON, "on")
            put(BluetoothAdapter.STATE_TURNING_ON, "turningOn")
        }
    }
    private var permissionCallback: CallbackContext? = null
    private var action: String? = null
    private var args: CordovaArgs? = null
    private var bluetoothManager: BluetoothManager? = null

    val scope = CoroutineScope(Dispatchers.IO)
    protected fun finalize() {
        scope.cancel()
    }
    private var bLECharacteristicReadRequestMap = mutableMapOf<UUID, BLECharacteristicReadRequest>()
    override fun initialize(cordova: CordovaInterface, webView: CordovaWebView) {
        super.initialize(cordova, webView)
    }

    override fun pluginInitialize() {
        if (COMPILE_SDK_VERSION == -1) {
            val context = cordova.context
            COMPILE_SDK_VERSION = context.applicationContext.applicationInfo.targetSdkVersion
        }
    }

    override fun onDestroy() {
        removeStateListener()
    }

    override fun onReset() {
        removeStateListener()
    }

    @Throws(JSONException::class)
    override fun execute(
        action: String,
        args: CordovaArgs,
        callbackContext: CallbackContext
    ): Boolean {
        LOG.d(TAG, "action = $action")

        //"initial setup" without bt permissions needed
        if (bluetoothAdapter == null) {
            val activity: Activity = cordova.activity
            bluetoothManager =
                activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (bluetoothManager == null) {
                LOG.e(TAG, "bluetoothManager is null")
                callbackContext.error("Unable to get the Bluetooth Manager")
                return false
            }
            bluetoothAdapter = bluetoothManager!!.adapter
        }
        if (action == SET_CHARACTERISTIC_VALUE_CHANGED_LISTENER) {
            characteristicValueChangedCallback = callbackContext
            return true
        } else if (action == SET_CHARACTERISTIC_VALUE_REQUESTED_LISTENER) {
            characteristicValueRequestedCallback = callbackContext
            return true
        } else if (action == RECEIVE_REQUESTED_CHARACTERISTIC_VALUE) {
            val uniqueUUID = args.optString(0)

            val result = args.optString(1)?.let { Base64.decode(it, Base64.DEFAULT) }
            Log.v(TAG, "receiveRequestedCharacteristicValue: $result")
            if (uniqueUUID == null || result == null) {
                callbackContext.error("uniqueUUID or result is null")
                return true
            }
            val contextID = UUIDHelper.uuidFromString(uniqueUUID)
            val requestContext = bLECharacteristicReadRequestMap[contextID]
            if (requestContext == null) {
                //timeout
                return true
            }
            bLECharacteristicReadRequestMap.remove(contextID)
            requestContext.timeoutJob.cancel()
            val cbuuid = requestContext.characteristic.uuid
            val service = requestContext.characteristic.service
            if (service == null) {
                callbackContext.error("service is null")
                return true
            }

            val characteristic = requestContext.characteristic
            characteristic.value = result.drop(requestContext.offset).toByteArray()

            gattServer!!.sendResponse(
                requestContext.device,
                requestContext.requestId,
                BluetoothGatt.GATT_SUCCESS,
                requestContext.offset,
                characteristic.value
            )

            callbackContext.success()


        } else if (action == SET_BLUETOOTH_STATE_CHANGED_LISTENER) {
            if (stateCallback != null) {
                callbackContext.error("State callback already registered.")
            } else {
                stateCallback = callbackContext
                addStateListener()
                sendBluetoothStateChange(bluetoothAdapter!!.state)
            }
            return true
        }

        //everything below here needs bt connect permissions
        val hasConnectPermission = PermissionHelper.hasPermission(this, BLUETOOTH_CONNECT)
        if (!hasConnectPermission && COMPILE_SDK_VERSION >= 31 && Build.VERSION.SDK_INT >= 31) {
            permissionCallback = callbackContext
            this.action = action
            this.args = args
            PermissionHelper.requestPermission(this, REQUEST_BLUETOOTH_CONNECT, BLUETOOTH_CONNECT)
            return true
        }
        if (gattServer == null) {
            val activity: Activity = cordova.activity
            val hardwareSupportsBLE = activity.applicationContext
                .packageManager
                .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
            if (!hardwareSupportsBLE) {
                LOG.e(TAG, "This hardware does not support Bluetooth Low Energy")
                callbackContext.error("This hardware does not support Bluetooth Low Energy")
                return false
            }
            val hardwareSupportsPeripherals = bluetoothAdapter!!.isMultipleAdvertisementSupported
            if (!hardwareSupportsPeripherals) {
                val errorMessage =
                    "This hardware does not support creating Bluetooth Low Energy peripherals"
                LOG.e(TAG, errorMessage)
                callbackContext.error(errorMessage)
                return false
            }

            //bluetooth connect permission
            gattServer = bluetoothManager!!.openGattServer(cordova.context, gattServerCallback)
        }
        return if (action == CREATE_SERVICE) {
            val serviceUUID = uuidFromString(args.getString(0))
            val service = BluetoothGattService(
                serviceUUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            services[serviceUUID] = service
            callbackContext.success()
            true
        } else if (action == REMOVE_SERVICE) {
            val serviceUUID = uuidFromString(args.getString(0))
            val service = services[serviceUUID]
            if (service == null) {
                callbackContext.error("Service $serviceUUID not found")
                return  /* validAction */true // stop processing because of error
            }
            val success = gattServer!!.removeService(service)
            if (success) {
                services.remove(serviceUUID)
                callbackContext.success()
            } else {
                callbackContext.error("Error removing $serviceUUID to GATT Server")
            }
            true
        } else if (action == REMOVE_ALL_SERVICES) {
            gattServer!!.clearServices()
            callbackContext.success()
            true
        } else if (action.contentEquals(ADD_CHARACTERISTIC)) {
            val serviceUUID = uuidFromString(args.getString(0))
            val characteristicUUID = uuidFromString(args.getString(1))
            val properties = args.getInt(2)
            val permissions = args.getInt(3)
            val characteristic = BluetoothGattCharacteristic(
                characteristicUUID,
                properties,
                permissions
            )
            val service = services[serviceUUID]
            if (service == null) {
                callbackContext.error("service not found")
                return true
            }
            service.addCharacteristic(characteristic)

            // If notify or indicate, we need to add the 2902 descriptor
            if (isNotify(characteristic) || isIndicate(characteristic)) {
                characteristic.addDescriptor(createClientCharacteristicConfigurationDescriptor())
            }
            callbackContext.success()
            true
        } else if (action == CREATE_SERVICE_FROM_JSON) {
            val json = args.getJSONObject(0)
            LOG.d(TAG, json.toString())
            try {
                val serviceUUID = uuidFromString(json.getString("uuid"))
                LOG.d(TAG, "Creating service $serviceUUID")
                val service =
                    BluetoothGattService(serviceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
                val characteristicArray = json.getJSONArray("characteristics")
                for (i in 0 until characteristicArray.length()) {
                    val jsonObject = characteristicArray.getJSONObject(i)
                    val uuid = uuidFromString(jsonObject.getString("uuid"))
                    val properties = jsonObject.getInt("properties")
                    val permissions = jsonObject.getInt("permissions")
                    LOG.d(
                        TAG,
                        "Adding characteristic $uuid properties=$properties permissions=$permissions"
                    )
                    val characteristic = BluetoothGattCharacteristic(uuid, properties, permissions)

                    // If notify or indicate, add the 2902 descriptor
                    if (isNotify(characteristic) || isIndicate(characteristic)) {
                        characteristic.addDescriptor(
                            createClientCharacteristicConfigurationDescriptor()
                        )
                    }

                    // TODO handle JSON without descriptors
                    val descriptorsArray = jsonObject.getJSONArray("descriptors")
                    for (j in 0 until descriptorsArray.length()) {
                        val jsonDescriptor = descriptorsArray.getJSONObject(j)
                        val descriptorUUID = uuidFromString(jsonDescriptor.getString("uuid"))

                        // TODO descriptor permissions should be optional in the JSON
                        //int descriptorPermissions = jsonDescriptor.getInt("permissions");
                        val descriptorPermissions =
                            BluetoothGattDescriptor.PERMISSION_READ // | BluetoothGattDescriptor.PERMISSION_WRITE;

                        // future versions need to handle more than Strings
                        val descriptorValue = jsonDescriptor.getString("value")
                        LOG.d(
                            TAG,
                            "Adding descriptor $descriptorUUID permissions=$permissions value=$descriptorValue"
                        )
                        val descriptor =
                            BluetoothGattDescriptor(descriptorUUID, descriptorPermissions)
                        if (!characteristic.addDescriptor(descriptor)) {
                            callbackContext.error("Failed to add descriptor $descriptorValue")
                            return  /*valid action */true // stop processing because of error
                        }
                        if (!descriptor.setValue(descriptorValue.toByteArray())) {
                            callbackContext.error("Failed to set descriptor value to $descriptorValue")
                            return  /*valid action */true // stop processing because of error
                        }
                    }
                    service.addCharacteristic(characteristic)
                }
                services[serviceUUID] = service
                if (gattServer!!.addService(service)) {
                    LOG.d(TAG, "Successfully added service $serviceUUID")
                    callbackContext.success()
                } else {
                    callbackContext.error("Error adding " + service.uuid + " to GATT Server")
                }
            } catch (e: JSONException) {
                LOG.e(TAG, "Invalid JSON for Service", e)
                e.printStackTrace()
                callbackContext.error(e.message)
            }
            true
        } else if (action == PUBLISH_SERVICE) {
            val serviceUUID = uuidFromString(args.getString(0))
            val service = services[serviceUUID]
            if (service == null) {
                callbackContext.error("Service $serviceUUID not found")
                return  /* validAction */true // stop processing because of error
            }
            val success = gattServer!!.addService(service)
            if (success) {
                callbackContext.success()
            } else {
                callbackContext.error("Error adding $serviceUUID to GATT Server")
            }
            true
        } else if (action == START_ADVERTISING) {
            val hasAdvertisingPermission = PermissionHelper.hasPermission(this, BLUETOOTH_ADVERTISE)
            if (!hasAdvertisingPermission && COMPILE_SDK_VERSION >= 31 && Build.VERSION.SDK_INT >= 31) {
                permissionCallback = callbackContext
                this.action = action
                this.args = args
                PermissionHelper.requestPermission(
                    this,
                    REQUEST_BLUETOOTH_ADVERTISE,
                    BLUETOOTH_ADVERTISE
                )
                return true
            }
            val advertisedName = args.getString(1)
            val serviceUUID = uuidFromString(args.getString(0))
            bluetoothAdapter!!.name = advertisedName
            val bluetoothLeAdvertiser = bluetoothAdapter!!.bluetoothLeAdvertiser
            val advertisementData = getAdvertisementData(serviceUUID)
            val advertiseSettings = advertiseSettings
            bluetoothLeAdvertiser.startAdvertising(
                advertiseSettings,
                advertisementData,
                advertiseCallback
            )
            advertisingStartedCallback = callbackContext
            true
        } else if (action == STOP_ADVERTISING) {
            val hasAdvertisingPermission = PermissionHelper.hasPermission(this, BLUETOOTH_ADVERTISE)
            if (!hasAdvertisingPermission && COMPILE_SDK_VERSION >= 31 && Build.VERSION.SDK_INT >= 31) {
                permissionCallback = callbackContext
                this.action = action
                this.args = args
                PermissionHelper.requestPermission(
                    this,
                    REQUEST_BLUETOOTH_ADVERTISE,
                    BLUETOOTH_ADVERTISE
                )
                return true
            }
            val bluetoothLeAdvertiser = bluetoothAdapter!!.bluetoothLeAdvertiser
            bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
            callbackContext.success()
            true
        } else if (action == SET_CHARACTERISTIC_VALUE) {
            val serviceUUID = uuidFromString(args.getString(0))
            val characteristicUUID = uuidFromString(args.getString(1))
            val value = args.getArrayBuffer(2)
            val service = services[serviceUUID]
            if (service == null) {
                callbackContext.error("Service $serviceUUID not found")
                return  /* validAction */true // stop processing because of error
            }
            val characteristic = service.getCharacteristic(characteristicUUID)
            if (characteristic == null) {
                callbackContext.error("Characteristic $characteristicUUID not found on service $serviceUUID")
                return  /* validAction */true // stop processing because of error
            }
            characteristic.value = value
            if (isNotify(characteristic) || isIndicate(characteristic)) {
                notifyRegisteredDevices(characteristic)
            }
            callbackContext.success()
            true
        } else if (action == SETTINGS) {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            cordova.activity.startActivity(intent)
            callbackContext.success()
            true
        } else if (action == ENABLE) {
            enableBluetoothCallback = callbackContext
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            cordova.startActivityForResult(this, intent, REQUEST_ENABLE_BLUETOOTH)
            true
        } else if (action == GET_BLUETOOTH_SYSTEM_STATE) {
            val bleState = getBleState()

            callbackContext.success(bleState)
            true
        } else if (action == START_SENDING_STASHED_READ_WRITE_EVENTS) {
            true
        } else {
            false
        }
    }

    fun getBleState(): JSONObject {
        val jsonResult = JSONObject()
        val jsonServices = JSONArray()

        gattServer?.services
            ?.forEach {
                val jsonService = JSONObject()
                jsonService.put("uuid", it.uuid.toString())
                jsonService.put("isPrimary", it.type == BluetoothGattService.SERVICE_TYPE_PRIMARY)
                val jsonCharacteristics = JSONArray()
                it.characteristics.forEach { it ->
                    val jsonCharacteristic = JSONObject()
                    jsonCharacteristic.put("uuid", it.uuid.toString())
                    jsonCharacteristic.put("properties", it.properties.toString())
                    jsonCharacteristic.put("permissions", it.permissions.toString())
                    jsonCharacteristics.put(jsonCharacteristic)
                }
                jsonService.put("characteristics", jsonCharacteristics)
                jsonServices.put(jsonService)
            }
        jsonResult.put("services", jsonServices)
        return jsonResult
    }

    private fun onBluetoothStateChange(intent: Intent) {
        val action = intent.action
        if (action != null && action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            sendBluetoothStateChange(state)
        }
    }

    private fun sendBluetoothStateChange(state: Int) {
        if (stateCallback != null) {
            val result = PluginResult(PluginResult.Status.OK, bluetoothStates[state])
            result.keepCallback = true
            stateCallback!!.sendPluginResult(result)
        }
    }

    private fun addStateListener() {
        if (stateReceiver == null) {
            stateReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    onBluetoothStateChange(intent)
                }
            }
        }
        try {
            val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            webView.context.registerReceiver(stateReceiver, intentFilter)
        } catch (e: Exception) {
            LOG.e(TAG, "Error registering state receiver: " + e.message, e)
        }
    }

    private fun removeStateListener() {
        if (stateReceiver != null) {
            try {
                webView.context.unregisterReceiver(stateReceiver)
            } catch (e: Exception) {
                LOG.e(TAG, "Error un-registering state receiver: " + e.message, e)
            }
        }
        stateCallback = null
        stateReceiver = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                LOG.d(TAG, "User enabled Bluetooth")
                if (enableBluetoothCallback != null) {
                    enableBluetoothCallback!!.success()
                }
            } else {
                LOG.d(TAG, "User did *NOT* enable Bluetooth")
                if (enableBluetoothCallback != null) {
                    enableBluetoothCallback!!.error("User did not enable Bluetooth")
                }
            }
            enableBluetoothCallback = null
        }
    }

    private val gattServerCallback: BluetoothGattServerCallback =
        object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(
                device: BluetoothDevice,
                status: Int,
                newState: Int
            ) {
                super.onConnectionStateChange(device, status, newState)
                LOG.d(TAG, "onConnectionStateChange status=$status->$newState")
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    registeredDevices.remove(device)
                }
            }

            override fun onServiceAdded(status: Int, service: BluetoothGattService) {
                LOG.d(TAG, "onServiceAdded status=$service->$service")
                super.onServiceAdded(status, service)
            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic)

                if (characteristicValueRequestedCallback != null) {
                    val contextID = UUID.randomUUID()
                    bLECharacteristicReadRequestMap[contextID] = BLECharacteristicReadRequest(
                        contextID = contextID,
                        device = device,
                        requestId = requestId,
                        offset = offset,
                        characteristic = characteristic,
                        timeoutJob = scope.launch {
                            delay(3000)
                            bLECharacteristicReadRequestMap.remove(contextID)
                            logD { "onCharacteristicReadRequest fallback requestId=$requestId offset=$offset" }
                            gattServer!!.sendResponse(
                                device,
                                requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                offset,
                                characteristic.value
                            )
                        }
                    )
                    val map = mapOf(
                        "contextID" to contextID.toString(),
                        "characteristic" to characteristic.uuid.toString(),
                        "service" to characteristic.service.uuid.toString()
                    )
                    val json = JSONObject(map)
                    val pluginResult = PluginResult(PluginResult.Status.OK, json)
                    pluginResult.keepCallback = true
                    characteristicValueRequestedCallback?.sendPluginResult(pluginResult)

                } else {
                    LOG.d(TAG, "onCharacteristicReadRequest requestId=$requestId offset=$offset")
                    gattServer!!.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        offset,
                        characteristic.value
                    )
                }
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                super.onCharacteristicWriteRequest(
                    device,
                    requestId,
                    characteristic,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )
                LOG.d(
                    TAG,
                    "onCharacteristicWriteRequest characteristic=" + characteristic.uuid + " value=" + Arrays.toString(
                        value
                    )
                )
                if (characteristicValueChangedCallback != null) {
                    try {
                        val message = JSONObject()
                        message.put("service", characteristic.service.uuid.toString())
                        message.put("characteristic", characteristic.uuid.toString())
                        message.put("value", byteArrayToJSON(value))
                        val result = PluginResult(PluginResult.Status.OK, message)
                        result.keepCallback = true
                        characteristicValueChangedCallback!!.sendPluginResult(result)
                    } catch (e: JSONException) {
                        LOG.e(TAG, "JSON encoding failed in onCharacteristicWriteRequest", e)
                    }
                }
                if (responseNeeded) {
                    gattServer!!.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        offset,
                        value
                    )
                }
            }

            override fun onNotificationSent(device: BluetoothDevice, status: Int) {
                super.onNotificationSent(device, status)
                LOG.d(TAG, "onNotificationSent device=$device status=$status")
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                descriptor: BluetoothGattDescriptor,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                super.onDescriptorWriteRequest(
                    device,
                    requestId,
                    descriptor,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )
                LOG.d(TAG, "onDescriptorWriteRequest")
                LOG.d(TAG, Arrays.toString(value))
                if (CLIENT_CHARACTERISTIC_CONFIGURATION_UUID == descriptor.uuid) {
                    if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                        LOG.d(TAG, "Subscribe device to notifications: $device")
                        registeredDevices.add(device)
                    } else if (Arrays.equals(
                            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE,
                            value
                        )
                    ) {
                        LOG.d(TAG, "Unsubscribe device from notifications: $device")
                        registeredDevices.remove(device)
                    }
                    if (responseNeeded) {
                        gattServer!!.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null
                        )
                    }
                } else {
                    // TODO allow other descriptors to be written
                    LOG.w(TAG, "Unknown descriptor write request")
                    if (responseNeeded) {
                        gattServer!!.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null
                        )
                    }
                }
            }

            override fun onDescriptorReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                descriptor: BluetoothGattDescriptor
            ) {
                super.onDescriptorReadRequest(device, requestId, offset, descriptor)
                LOG.d(
                    TAG,
                    "onDescriptorReadRequest device=" + device + " descriptor=" + descriptor.uuid
                )
                gattServer!!.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    descriptor.value
                )
            }

            override fun onExecuteWrite(device: BluetoothDevice, requestId: Int, execute: Boolean) {
                super.onExecuteWrite(device, requestId, execute)
                LOG.d(TAG, "onExecuteWrite")
            }
        }

    // https://github.com/don/uribeacon/blob/58c31cf28d06a80880b0ed46b005204821fd623f/beacons/android/app/src/main/java/org/uribeacon/example/beacon/UriBeaconAdvertiserActivity.java
    private fun getAdvertisementData(serviceUuid: UUID): AdvertiseData {
        val builder = AdvertiseData.Builder()
        builder.setIncludeTxPowerLevel(false) // reserve advertising space for URI
        builder.addServiceUuid(ParcelUuid(serviceUuid)) // TODO accept multiple services in the future
        // builder.addServiceUuid(new ParcelUuid(serviceUuid1));
        builder.setIncludeDeviceName(true)
        return builder.build()
    }

    private val advertiseSettings: AdvertiseSettings
        private get() {
            val builder = AdvertiseSettings.Builder()
            //builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
            builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            builder.setConnectable(true)
            return builder.build()
        }
    private val advertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            LOG.d(TAG, "onStartSuccess")
            if (advertisingStartedCallback != null) {
                advertisingStartedCallback!!.success()
            }
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            LOG.d(TAG, "onStartFailure")
            if (advertisingStartedCallback != null) {
                advertisingStartedCallback!!.error(errorCode)
            }
        }
    }

    private fun notifyRegisteredDevices(characteristic: BluetoothGattCharacteristic) {
        val confirm = isIndicate(characteristic)
        for (device in registeredDevices) {
            gattServer!!.notifyCharacteristicChanged(device, characteristic, confirm)
        }
    }

    // Utils
    private fun uuidFromString(uuid: String): UUID {
        return UUIDHelper.uuidFromString(uuid)
    }

    private fun isNotify(characteristic: BluetoothGattCharacteristic): Boolean {
        return characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
    }

    private fun isIndicate(characteristic: BluetoothGattCharacteristic): Boolean {
        return characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
    }

    @Throws(JSONException::class)
    private fun byteArrayToJSON(bytes: ByteArray): JSONObject {
        val `object` = JSONObject()
        `object`.put("CDVType", "ArrayBuffer")
        `object`.put("data", Base64.encodeToString(bytes, Base64.NO_WRAP))
        return `object`
    }

    private fun createClientCharacteristicConfigurationDescriptor(): BluetoothGattDescriptor {
        return BluetoothGattDescriptor(
            CLIENT_CHARACTERISTIC_CONFIGURATION_UUID,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
    }

    //TODO: Update permission handling when cdv-android platform is updated
    override fun onRequestPermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        val callback = popPermissionsCallback()
        val action = action
        val args = args
        this.action = null
        this.args = null
        if (callback == null) {
            if (grantResults.size > 0) {
                // There are some odd happenings if permission requests are made while booting up capacitor
                LOG.w(TAG, "onRequestPermissionResult received with no pending callback")
            }
            return
        }
        if (grantResults.size == 0) {
            callback.error("No permissions not granted.")
            return
        }
        for (i in permissions.indices) {
            if (permissions[i] == BLUETOOTH_CONNECT && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                LOG.d(TAG, "User *rejected* Bluetooth_Connect Access")
                callback.error("Bluetooth Connect permission not granted.")
                return
            } else if (permissions[i] == BLUETOOTH_ADVERTISE && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                LOG.d(TAG, "User *rejected* Bluetooth_Advertise Access")
                callback.error("Bluetooth Advertise permission not granted.")
                return
            }
        }
        try {
            execute(action!!, args!!, callback)
        } catch (e: JSONException) {
            callback.error(e.message)
        }
    }

    private fun popPermissionsCallback(): CallbackContext? {
        val callback = permissionCallback
        permissionCallback = null
        return callback
    }

    companion object {
        // actions
        private const val CREATE_SERVICE = "createService"
        private const val CREATE_SERVICE_FROM_JSON = "createServiceFromJSON"
        private const val REMOVE_SERVICE = "removeService"
        private const val REMOVE_ALL_SERVICES = "removeAllServices"
        private const val ADD_CHARACTERISTIC = "addCharacteristic"
        private const val PUBLISH_SERVICE = "publishService"
        private const val START_ADVERTISING = "startAdvertising"
        private const val STOP_ADVERTISING = "stopAdvertising"
        private const val SET_CHARACTERISTIC_VALUE = "setCharacteristicValue"
        private const val SET_CHARACTERISTIC_VALUE_CHANGED_LISTENER =
            "setCharacteristicValueChangedListener"
        private const val SET_CHARACTERISTIC_VALUE_REQUESTED_LISTENER =
            "setCharacteristicValueRequestedListener"
        private const val RECEIVE_REQUESTED_CHARACTERISTIC_VALUE =
            "receiveRequestedCharacteristicValue"
        private const val GET_BLUETOOTH_SYSTEM_STATE = "getBluetoothSystemState"
        private const val START_SENDING_STASHED_READ_WRITE_EVENTS = "startSendingStashedReadWriteEvents"

        // 0x2902 https://www.bluetooth.com/specifications/gatt/descriptors
        private val CLIENT_CHARACTERISTIC_CONFIGURATION_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // private static final String START_STATE_NOTIFICATIONS = "startStateNotifications";
        // private static final String STOP_STATE_NOTIFICATIONS = "stopStateNotifications";
        private const val SET_BLUETOOTH_STATE_CHANGED_LISTENER = "setBluetoothStateChangedListener"
        private const val SETTINGS = "showBluetoothSettings"
        private const val ENABLE = "enable"
        private const val BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT"
        private const val BLUETOOTH_ADVERTISE = "android.permission.BLUETOOTH_ADVERTISE"
        private const val REQUEST_BLUETOOTH_CONNECT = 42
        private const val REQUEST_BLUETOOTH_ADVERTISE = 43
        private var COMPILE_SDK_VERSION = -1
        private const val TAG = "BLEPeripheral"
        private const val REQUEST_ENABLE_BLUETOOTH = 17
    }
}