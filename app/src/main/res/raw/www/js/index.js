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
    // Авторизация происходит в два шага:
    // 1) запрашиваем у основного приложения код авторизации по идентификатору клиента
    // 2) передаём на сервер миниприложения полученный код авторизации
    //      - сервер, используя идентификатор и секрет клиента, обращается на кондо и обменивает его на токены авторизации, получает необходимые данные и создаёт записи
    //      - на основании полученных данных сервер создаёт итоговую авторизацию для клиента и возвращает её в ответ на код
    // В нашем случае сервера не существует, мы получим авторизацию кондо за него

    // cordova.plugins.condo.requestAuthorizationCode(clientId, function(response) {

        // let xhr = new XMLHttpRequest();
        // xhr.open("POST", "https://v1.doma.ai/oidc/token");

        // xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        // xhr.setRequestHeader("Accept", "application/json");
        // xhr.setRequestHeader("Authorization", "Basic bWluaWFwcC1tb2JpbGUtdGVzdC13ZWI6VjNLUUhTakNZUjZQOXpQUXhFWWM4S1d3Zmk5WE5WbW4=");


        // xhr.onload = () => {
        //     // console.log('recive responce result => ', xhr.responseText);
        // }

        // let data = `grant_type=authorization_code&code=${response}&redirect_uri=https%3A%2F%2Fmobile.doma.ai`;

        // xhr.send(data);

    // }, function(error) {
    //     console.log(error);
    // });

    // DOMA!
    // Вариант авторизации номер два
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
    // Вариант авторизации номер три
    // У нас есть сервер и мы просим приложение провести полную клиентскую авторизацию с редиректом на сервер миниаппа и обратно:
    // 1) запрашиваем у основного приложения авторизацию по клиент айди и секрет и получаем токен и рефреш токен
    // 2) при необходимости просим обновить


    // cordova.plugins.condo.requestServerAuthorization(mClientId, miniappServerRedirectUri, authorizationComplitionRedirectUri, function(response) {
    //     console.log(response);
    //     console.log('recive responce result => ', response);
    // }, function(error) {
    //     console.log(error);
    // });

//    let xhr = new XMLHttpRequest();
//    xhr.open("POST", "https://miniapp.d.doma.ai/admin/api");
//
//    xhr.setRequestHeader("Content-Type", "application/json");
//    xhr.setRequestHeader("Accept", "application/json");
//
//    xhr.onload = () => {
//        console.log('recive responce result => ', xhr.responseText);
//    }
//

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

    // send post request
    fetch('https://miniapp.d.doma.ai/admin/api', options)
        .then(res => res.json())
        .then((res) => {
            // {"data":{"authenticatedUser":null}}
            if (res.data.authenticatedUser) {
                console.log('authentificated => ', JSON.stringify(res.data.authenticatedUser));
                cordova.plugins.condo.getCurrentResident(function(response) {
                    console.log("current resident\address => ", JSON.stringify(response));
                }, function(error) {
                    console.log(error);
                })
            } else {
                console.log('authentification missing => ', JSON.stringify(res));
                cordova.plugins.condo.requestServerAuthorizationByUrl('https://miniapp.d.doma.ai/oidc/auth', {}, function(response) {
                    console.log(response);
                    console.log('recive authorization result => ', JSON.stringify(response));
                    console.log('reloading');
                    window.location.reload();
                }, function(error) {
                    console.log(error);
                });
            }
        })
        .catch(err => console.error(err));

    document.getElementById("CloseButton").addEventListener("click", closeApplication);

}
