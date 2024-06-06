/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// Wait for the deviceready event before using any of Cordova's device APIs.
// See https://cordova.apache.org/docs/en/latest/cordova/events/events.html#deviceready


// import UserManager from './oidc-client-ts/index';
// import 'oidc-client-ts.min.js';
// import * as Oidc from './oidc-client-ts.min.js';

document.addEventListener('deviceready', onDeviceReady, false);

function closeApplication() {
    cordova.plugins.condo.closeApplication(function(response) {}, function(error) {})
}

function onDeviceReady() {
    // Cordova is now initialized. Have fun!

    console.log('Running cordova-' + cordova.platformId + '@' + cordova.version);
    document.getElementById('deviceready').classList.add('ready');


    const clientId = 'miniapp-mobile-test-web';

    // DOMA!
    // Вариант авторизации номер один
    // У нас нет сервера и мы просим приложение авторизовать нас на кондо:
    // 1) запрашиваем у основного приложения авторизацию по клиент айди и секрет и получаем токен и рефреш токен
    // 2) при необходимости просим обновить
    // const clientSecret = 'V3KQHSjCYR6P9zPQxEYc8KWwfi9XNVmn';
    // cordova.plugins.condo.requestAuthorization(clientId, clientSecret, function(response) {
    //     console.log(response);
    //     console.log('recive responce result => ', response);
    // }, function(error) {
    //     console.log(error);
    // })

    // DOMA!
    // Вариант авторизации номер два - основной способ, мы обращаемся на сервер миниапа и просим авторизовать, а дльше редиректы по всему сценарию


    let data = {"variables":{},"query":"{\n  authenticatedUser {\n    id\n    name\n   email\n    isAdmin\n    __typename\n  }\n}\n"};

    // request options
    const options = {
        method: 'POST',
        body: JSON.stringify(data),
        credentials: "include",
        headers: {
            'Content-Type': 'application/json'
        }
    }

//    // send post request
//    fetch('https://miniapp.d.doma.ai/admin/api', options)
//        .then(res => res.json())
//        .then((res) => {
//            if (res.data.authenticatedUser) {
//                console.log('authentificated => ', JSON.stringify(res.data.authenticatedUser));
//                cordova.plugins.condo.getCurrentResident(function(response) {
//                    console.log("current resident\address => ", JSON.stringify(response));
//                }, function(error) {
//                    console.log(error);
//                })
//            } else {
//                console.log('authentification missing => ', JSON.stringify(res));
//                // вот она
//                cordova.plugins.condo.requestServerAuthorizationByUrl('https://miniapp.d.doma.ai/oidc/auth', {}, function(response) {
//                    console.log(response);
//                    console.log('receive authorization result => ', JSON.stringify(response));
//                    console.log('reloading');
//                    window.location.reload();
//                }, function(error) {
//                    console.log(error);
//                });
//            }
//        })
//        .catch(err => console.error(err));


    testBleStuff();
}

function testBleStuff() {
    let SERVICE_UUID = 'a53438ce-e4bd-430f-a361-a22fe88b02bc';
    let TX_UUID = '2ff481b3-72be-49e9-a766-5e6cea26932f';
    let RX_UUID = '6d8c6a4d-9c96-4dde-8f13-7260b00f4785';

    let permissions = blePeripheral.permissions;
    let properties = blePeripheral.properties;

    blePeripheral.stopAdvertising();
    blePeripheral.removeAllServices();
    blePeripheral.createService(SERVICE_UUID).then(() => {
        console.log('t1');
        blePeripheral.addCharacteristic(SERVICE_UUID, TX_UUID, properties.WRITE, permissions.WRITEABLE).then(() => {
            console.log('t2');
            blePeripheral.addCharacteristic(SERVICE_UUID, RX_UUID, properties.READ | properties.NOTIFY, permissions.READABLE).then(() => {
                console.log('t3');
                blePeripheral.publishService(SERVICE_UUID).then(() => {
                    console.log('t4');
                    blePeripheral.startAdvertising(SERVICE_UUID, 'UART').then(() => {
                        console.log ('Created UART Service');
                        blePeripheral.onReadRequest((service, characteristic) => {
                            console.log('onReadRequest', 'service:', service, 'characteristic:', characteristic);
//                            var buffer = new ArrayBuffer(333);
//                            var int8 = new Int8Array(buffer);
//                            int8[0] = 1
//                            int8[2] = 2
//                            int8[3] = 63
//                            int8[4] = 64
//                            int8[5] = 127
//                            return buffer;
                              return "place characteristic data you want to send here"
                        });
                    });
                });
            });
        });
    });
}


