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

    // DOMA!
    // Вариант авторизации номер три
    // У нас есть сервер и мы просим приложение провести полную клиентскую авторизацию с редиректом на сервер миниаппа и обратно:
    // 1) запрашиваем у основного приложения авторизацию по клиент айди и секрет и получаем токен и рефреш токен
    // 2) при необходимости просим обновить

    const miniappServerRedirectUri = 'https://doma-miniapp.hiplabs.dev/oidc/callback';
    const authorizationComplitionRedirectUri = 'https://doma2-miniapp.hiplabs.dev/';
    const mClientId = 'baglansariev-dev-app';

    cordova.plugins.condo.requestServerAuthorization(mClientId, miniappServerRedirectUri, authorizationComplitionRedirectUri, function(response) {
        console.log(response);
        console.log('recive responce result => ', response);
    }, function(error) {
        console.log(error);
    })

    document.getElementById("CloseButton").addEventListener("click", closeApplication);

}
