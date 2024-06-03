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

    // send post request
    fetch('https://miniapp.d.doma.ai/admin/api', options)
        .then(res => res.json())
        .then((res) => {
            if (res.data.authenticatedUser) {
                console.log('authentificated => ', JSON.stringify(res.data.authenticatedUser));
                cordova.plugins.condo.getCurrentResident(function(response) {
                    console.log("current resident\address => ", JSON.stringify(response));
                }, function(error) {
                    console.log(error);
                })
            } else {
                console.log('authentification missing => ', JSON.stringify(res));
                // вот она
                cordova.plugins.condo.requestServerAuthorizationByUrl('https://miniapp.d.doma.ai/oidc/auth', {}, function(response) {
                    console.log(response);
                    console.log('receive authorization result => ', JSON.stringify(response));
                    console.log('reloading');
                    window.location.reload();
                }, function(error) {
                    console.log(error);
                });
            }
        })
        .catch(err => console.error(err));


    var cordova = cordova.require('cordova'),
        helpers = cordova.require('./helpers');

    var SUCCESS_EVENT = "pendingcaptureresult";
    var FAILURE_EVENT = "pendingcaptureerror";

    var sChannel = cordova.addStickyDocumentEventHandler(SUCCESS_EVENT);
    var fChannel = cordova.addStickyDocumentEventHandler(FAILURE_EVENT);

    // We fire one of two events in the case where the activity gets killed while
    // the user is capturing audio, image, video, etc. in a separate activity
    document.addEventListener("resume", function(event) {
        console.log('resume');
        if (event.pendingResult && event.pendingResult.pluginServiceName === "Capture") {
            if (event.pendingResult.pluginStatus === "OK") {
                var mediaFiles = helpers.wrapMediaFiles(event.pendingResult.result);
                sChannel.fire(mediaFiles);
            } else {
                fChannel.fire(event.pendingResult.result);
            }
        }
    });
}


//document.getElementById('test_video').onclick = function(e) {
//
//    var captureSuccess = function(mediaFiles) {
//        var i, path, len;
//        for (i = 0, len = mediaFiles.length; i < len; i += 1) {
//            path = mediaFiles[i].fullPath;
//            // do something interesting with the file
//        }
//    };
//
//    // capture error callback
//    var captureError = function(error) {
//        navigator.notification.alert('Error code: ' + error.code, null, 'Capture Error');
//    };
//
//    // start video capture
//    navigator.device.capture.captureVideo(captureSuccess, captureError, { limit : 1 });
//
//    return false
//}

var videoBtnElement = document.getElementById('video')
var videoElement = document.getElementsByTagName('video')[0]
var videoInput = document.getElementById('test_video')

//videoInput.onclick = function(e) {
//    var captureSuccess = function(mediaFiles) {
//        var i, path, len;
//        for (i = 0, len = mediaFiles.length; i < len; i += 1) {
//            console.log(mediaFiles[i])
//            path = mediaFiles[i].fullPath;
////                videoElement.src = path
//            // do something interesting with the file
//            var xhr = new XMLHttpRequest()
//            xhr.open('GET', path)
//            var index = i
//            xhr.onload = function (r) {
//                console.log(r)
//                var content = xhr.response;
//                var blob = new Blob([content]);
//                console.log(blob);
//                file = new File([blob], mediaFiles[index].name, { type: 'video/mp4' })
//                file.end = mediaFiles[index].size
//                console.log(file)
//
//                file.path2 = path
//
//                var dt  = new DataTransfer();
//                dt.items.add(file);
//                var file_list = dt.files;
//
//                videoInput.files = file_list
//            }
//            xhr.responseType = 'blob'
//            xhr.send()
//        }
//    };
//
//    // capture error callback
//    var captureError = function(error) {
//        navigator.notification.alert('Error code: ' + error.code, null, 'Capture Error');
//    };
//
//    // start video capture
//    navigator.device.capture.captureVideo(captureSuccess, captureError, { limit : 1 });
//    return false
//}

videoBtnElement.onclick = function(e) {
    var files = [videoInput.files[0]]
    navigator.share({files})
}