
// (c) 2016 Don Coleman
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

/* global cordova, module, console, document, Uint8Array, atob, Promise, ArrayBuffer*/
"use strict";

// Util functions for translating nested array buffers going across the Cordova bridge
function stringToBase64(str) {
    const utf8Bytes = new TextEncoder().encode(str); // UTF-8 -> Uint8Array
    let binary = '';
    for (let i = 0; i < utf8Bytes.length; i++) {
        binary += String.fromCharCode(utf8Bytes[i]);
    }
    return btoa(binary); // Binary -> Base64
}

//    function base64ToString(base64) {
//        const binary = atob(base64); // Base64 -> binary string
//        const bytes = Uint8Array.from(binary, char => char.charCodeAt(0));
//        return new TextDecoder().decode(bytes); // UTF-8 decode
//    }
//
//    var stringToArrayBuffer = function (str) {
//        var ret = new Uint8Array(str.length);
//        for (var i = 0; i < str.length; i++) {
//            ret[i] = str.charCodeAt(i);
//        }
//        // TODO would it be better to return Uint8Array?
//        return ret.buffer;
//    };
//
//    var base64ToArrayBuffer = function (b64) {
//        return stringToArrayBuffer(atob(b64));
//    };

function massageMessageNativeToJs(message) {
      if (message && message.CDVType === "ArrayBuffer") {
        const binary = atob(message.data);
        const len = binary.length;
        const bytes = new Uint8Array(len);
        for (let i = 0; i < len; i++) {
          bytes[i] = binary.charCodeAt(i);
        }
        return bytes;
      }
      return message;
}

// Cordova 3.6 doesn't unwrap ArrayBuffers in nested data structures
// https://github.com/apache/cordova-js/blob/94291706945c42fd47fa632ed30f5eb811080e95/src/ios/exec.js#L107-L122
function convertToNativeJS(object) {
    Object.keys(object).forEach(function (key) {
        var value = object[key];
        object[key] = massageMessageNativeToJs(value);
        if (typeof (value) === 'object') {
            convertToNativeJS(value);
        }
    });
}

// end Util functions

var onWriteRequestCallback;
var onReadRequestCallback;
var onBluetoothStateChangeCallback;

function registerWriteRequestCallback() {

    var didReceiveWriteRequest = async function (json) {
        console.log('didReceiveWriteRequest');
        console.log(json);
        convertToNativeJS(json);
        const contextID = json.contextID;

        if (onWriteRequestCallback && typeof onWriteRequestCallback === 'function') {
            let result = await onWriteRequestCallback(json);

            if (result instanceof ArrayBuffer) {
                var base64String = stringToBase64([].reduce.call(new Uint8Array(result),function(p,c){return p+String.fromCharCode(c)},''));
                cordova.exec(() => { }, () => { }, 'BLEPeripheral', 'receiveChangedCharacteristicValue', [contextID, base64String]);
            } else if (result instanceof Uint8Array) {
                let binaryString = "";
                for (let i = 0; i < result.length; i++) {
                  binaryString += String.fromCharCode(result[i]);
                }
                let base64String = btoa(binaryString);
                cordova.exec(
                  () => {},
                  () => {},
                  "BLEPeripheral",
                  "receiveChangedCharacteristicValue",
                  [contextID, base64String]
                );
            } else if (typeof result === 'string' && result.length > 0) {
                var base64String = stringToBase64(result);
                cordova.exec(() => { }, () => { }, 'BLEPeripheral', 'receiveChangedCharacteristicValue', [contextID, base64String]);
            } else {
                cordova.exec(() => { }, () => { }, 'BLEPeripheral', 'receiveChangedCharacteristicValue', [contextID]);
            }
        }
    };

    var failure = function () {
        // this should never happen
        console.log("Failed to add setCharacteristicValueChangedListener");
    };

    cordova.exec(didReceiveWriteRequest, failure, 'BLEPeripheral', 'setCharacteristicValueChangedListener', []);
}
//registerWriteRequestCallback();

function registerReadRequestCallback() {

    var didReceiveReadRequest = function(dictionaryObject) {
        console.log('didReceiveReadRequest', dictionaryObject);

        const contextID = dictionaryObject.contextID;
        const characteristic = dictionaryObject.characteristic;
        const service = dictionaryObject.service;

        if (!(contextID && characteristic && service)) {
            console.log('didReceiveReadRequest', 'malformed reqeust');
            return
        }

        if (onReadRequestCallback && typeof onReadRequestCallback === 'function') {
            let result = onReadRequestCallback(service, characteristic);

            if (result instanceof ArrayBuffer) {
                var base64String = stringToBase64([].reduce.call(new Uint8Array(result),function(p,c){return p+String.fromCharCode(c)},''));
                cordova.exec(() => { }, () => { }, 'BLEPeripheral', 'receiveRequestedCharacteristicValue', [contextID, base64String]);
            } else if (typeof result === 'string' && result.length > 0) {
                var base64String = stringToBase64(result);
                cordova.exec(() => { }, () => { }, 'BLEPeripheral', 'receiveRequestedCharacteristicValue', [contextID, base64String]);
            } else {
                cordova.exec(() => { }, () => { }, 'BLEPeripheral', 'receiveRequestedCharacteristicValue', [contextID]);
            }

        }
    };

    var failure = function () {
        // this should never happen
        console.log("Failed to add setCharacteristicValueRequestedListener");
    };

    cordova.exec(didReceiveReadRequest, failure, 'BLEPeripheral', 'setCharacteristicValueRequestedListener');
}

function registerBluetoothStateChangeCallback() {

    var bluetoothStateChanged = function (state) {
        console.log('bluetoothStateChanged', state);

        if (onBluetoothStateChangeCallback && typeof onBluetoothStateChangeCallback === 'function') {
            onBluetoothStateChangeCallback(state);
        }
    };

    var failure = function () {
        // this should never happen
        console.log("Failed to add bluetoothStateChangedListener");
    };

    cordova.exec(bluetoothStateChanged, failure, 'BLEPeripheral', 'setBluetoothStateChangedListener', []);
}
registerBluetoothStateChangeCallback();

/*
Characteristic premissions are not consistent across platforms. This will need to be reconciled.
Maybe permissions should be optional and default to read/write based on the properties.

// iOS permissions CBCharacteristic.h
    CBAttributePermissionsReadable                    = 0x01,
    CBAttributePermissionsWriteable                    = 0x02,
    CBAttributePermissionsReadEncryptionRequired    = 0x04,
    CBAttributePermissionsWriteEncryptionRequired    = 0x08

// Android permissions BluetoothGattCharacteristic.java

    public static final int PERMISSION_READ = 0x01;
    public static final int PERMISSION_READ_ENCRYPTED = 0x02;
    public static final int PERMISSION_READ_ENCRYPTED_MITM = 0x04;
    public static final int PERMISSION_WRITE = 0x10;
    public static final int PERMISSION_WRITE_ENCRYPTED = 0x20;
    public static final int PERMISSION_WRITE_ENCRYPTED_MITM = 0x40;
    public static final int PERMISSION_WRITE_SIGNED = 0x80;
    public static final int PERMISSION_WRITE_SIGNED_MITM = 0x100;
*/

module.exports = {

    properties: {
        READ: 0x02,
        WRITE: 0x08,
        WRITE_NO_RESPONSE: 0x04,
        NOTIFY: 0x10,
        INDICATE: 0x20
    },

    permissions: {
        READABLE: 0x01,
        WRITEABLE: cordova.platformId === 'ios' ? 0x02 : 0x10,
        READ_ENCRYPTION_REQUIRED: cordova.platformId === 'ios' ? 0x04 : 0x02,
        WRITE_ENCRYPTION_REQUIRED: cordova.platformId === 'ios' ? 0x08 : 0x20
    },

    createService: function (uuid) {

        return new Promise(function (resolve, reject) {
            cordova.exec(resolve, reject, 'BLEPeripheral', 'createService', [uuid]);
        });
    },

    createServiceFromJSON: function (json) {

        return new Promise(function (resolve, reject) {
            cordova.exec(resolve, reject, 'BLEPeripheral', 'createServiceFromJSON', [json]);
        });

    },

    addCharacteristic: function (service, characteristic, properties, permissions) {

        return new Promise(function (resolve, reject) {
            cordova.exec(resolve, reject, 'BLEPeripheral', 'addCharacteristic', [service, characteristic, properties, permissions]);
        });

    },

    publishService: function (uuid) {

        return new Promise(function (resolve, reject) {
            cordova.exec(resolve, reject, 'BLEPeripheral', 'publishService', [uuid]);
        });

    },

    removeService: function (service) {

        return new Promise(function (resolve, reject) {
            cordova.exec(resolve, reject, 'BLEPeripheral', 'removeService', [service]);
        });

    },

    removeAllServices: function () {

        return new Promise(function (resolve, reject) {
            cordova.exec(resolve, reject, 'BLEPeripheral', 'removeAllServices', []);
        });

    },

    // Future versions should should allow one or multiple services, name should be optional
    startAdvertising: function (localName, service) {

        return new Promise(function (resolve, reject) {
            // var param = []
            // param.push(localName)
            // for (i=0; i<services.length; i++) {
            //     param.push(services[i])
            // }
            cordova.exec(resolve, reject, 'BLEPeripheral', 'startAdvertising', [localName, service]);
        });

    },

    stopAdvertising: function () {

        return new Promise(function (resolve, reject) {
            cordova.exec(resolve, reject, 'BLEPeripheral', 'stopAdvertising', []);
        });

    },

    // setting the value automatically notifies subscribers
    setCharacteristicValue: function (service, characteristic, value) {

        return new Promise(function (resolve, reject) {
            var buffer = ArrayBuffer.isView(value) ? value.buffer : value;

            if (buffer.constructor.name !== 'ArrayBuffer') {
                reject('value must be an ArrayBuffer');
            }
            cordova.exec(resolve, reject, 'BLEPeripheral', 'setCharacteristicValue', [service, characteristic, buffer]);
        });

    },

    // setDescriptorValue: function(service, characteristic, descriptor, value) {
    //
    //     return new Promise(function(resolve, reject) {
    //         if (value.constructor !== ArrayBuffer) {
    //             reject('value must be an ArrayBuffer');
    //         }
    //         cordova.exec(resolve, reject, 'BLEPeripheral', 'setDescriptorValue', [service, characteristic, descriptor, value]);
    //     });
    //
    // },

    // setDescriptorValueChangedListener: function(success) {
    //     var failure = function() {
    //         // this should never happen
    //         console.log("Failed to add setDescriptorValueChangedListener");
    //     };
    //
    //     cordova.exec(success, failure, 'BLEPeripheral', 'setDescriptorValueChangedListener', []);
    // },

    // callback gets called with JSON object whenever a central
    // updates a characteristic value. For version 1.0 this just
    // informs the app what happened, it can't be rejected
    // {
    //   service: '1234',
    //   characteristic: '5678',
    //   value: someArrayBuffer
    // }
    onWriteRequest: function (callback) {
        onWriteRequestCallback = callback;
        registerWriteRequestCallback();
    },

    onReadRequest: function (callback) {
        onReadRequestCallback = callback;
        registerReadRequestCallback();
    },

    onBluetoothStateChange: function (callback) {
        onBluetoothStateChangeCallback = callback;
    },

    //If miniapp uses background features, services and advertising is performed even when miniapp is closed, or even applicaiton is closed.
    //At some point (on read or write request from some bluetooth central) OS launches app, then app launches miniapp
    //You should call this function when your miniapp is ready to accept such (read & write) events.
    //You should call this function asap, because such bluetooth requests have 3 seconds timeout (OS restrictions),
    //and some of this time is usually already consumed by starting the miniapp.
    startSendingStashedReadWriteEvents: function() {
        cordova.exec(() => {}, () => {}, 'BLEPeripheral', 'startSendingStashedReadWriteEvents', []);
    },

    //If miniapp uses background features, services and advertising is performed even when miniapp is closed, or even applicaiton is closed.
    //So, in context of miniapp it may be dificult to understand, in what state is adverting and services are at the moment (at least aon the start of the miniapp)
    //For that we made this fucntion, which exports data in json
    getBluetoothSystemState: function() {
        return new Promise(function (resolve, reject) {
            cordova.exec(resolve, reject, 'BLEPeripheral', 'getBluetoothSystemState', []);
        });
    }
};