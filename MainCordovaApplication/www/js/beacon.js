// Показ секции по её ID
function showSection(sectionId) {
    // Скрыть все секции
    document.querySelectorAll('.content-section').forEach(section => {
        section.classList.remove('active-section');
    });

    // Показать выбранную секцию
    document.getElementById(sectionId).classList.add('active-section');
}




// Код для страницы Monitoring
function addRegionForMonitoringFromUI() {
    const identifier = document.getElementById('identifier').value;
    const uuid = document.getElementById('uuid').value;
    const major = document.getElementById('major').value;
    const minor = document.getElementById('minor').value;

    startMonitoringRequest(identifier, uuid, major, minor)
}

function addRegionForMonitoring(identifier, uuid, major, minor) {
    if (identifier) {
        const list = document.getElementById('region-list');
        const newItem = document.createElement('li');
        newItem.className = 'collection-item';
        newItem.setAttribute("data-uuid", uuid);
        newItem.id = `monitoring-item-${identifier}`
        newItem.style.paddingBottom = '30px'

        newItem.innerHTML = `
            <span>
                <strong>Identifier:</strong> ${identifier} | <br/> <strong>UUID:</strong> ${uuid} | <br/><strong>Major:</strong> ${major} | <br/><strong>Minor:</strong> ${minor}<br/>
                <span class="new badge green" id="status-${identifier}">Inside</span>
            </span>
            <a href="#!" class="secondary-content" onclick="removeRegionFromMonitoring('${identifier}', '${uuid}', '${major}', '${minor}')">
                <i class="material-icons red-text">delete</i>
            </a>
        `;

        list.appendChild(newItem);
//        logAction(`Добавлен Region - UUID: ${uuid}, Major: ${major}, Minor: ${minor}`);

        if (enteredRegions[identifier]) {
            updateMonitoringRegionStatus(identifier, 1);
        } else {
            updateMonitoringRegionStatus(identifier, 0);
        }
    } else {
        logAction("Ошибка: Поле identifier формы должно быть заполнено!");
    }
}

function removeRegionFromMonitoring(identifier, uuid, major, minor) {
    if (uuid == 'undefined') uuid = undefined;
    if (major == 'undefined') major = undefined;
    if (minor == 'undefined') minor = undefined;

    let region = new cordova.plugins.locationManager.BeaconRegion(identifier, uuid, major, minor)
    cordova.plugins.locationManager.stopMonitoringForRegion(region).then((r) => {
        actualizeMonitoring()
    })

//    const item = element.parentElement;
//    const uuid = item.getAttribute("data-uuid");
//    item.remove();
//    logAction("Удален Region: UUID - " + uuid);
}

function updateMonitoringItem(region) {
    let element = document.getElementById(`monitoring-item-${region.identifier}`)
    if (element == null) {
        addRegionForMonitoring(region.identifier, region.uuid, region.major, region.minor)
        return
    }

    element.innerHTML = `
            <span>
                <strong>Identifier:</strong> ${region.identifier} | <br/> <strong>UUID:</strong> ${region.uuid} | <br/><strong>Major:</strong> ${region.major} | <br/><strong>Minor:</strong> ${region.minor}<br/>
                <span class="new badge green" id="status-${region.identifier}">Inside</span>
            </span>
            <a href="#!" class="secondary-content" onclick="removeRegionFromMonitoring('${region.identifier}', '${region.uuid}', '${region.major}', '${region.minor}')">
                <i class="material-icons red-text">delete</i>
            </a>
        `;

    if (enteredRegions[region.identifier]) {
        updateMonitoringRegionStatus(region.identifier, 1);
    } else {
        updateMonitoringRegionStatus(region.identifier, 0);
    }
}

function updateMonitoringRegionStatus(identifier, status) {
    const statusElement = document.getElementById(`status-${identifier}`);

    if (statusElement) {
        if (status === 1) {
            statusElement.classList.remove('red');
            statusElement.classList.add('green');
            statusElement.textContent = 'Inside';
            logAction(`Region ${identifier} - статус обновлен на Inside`);
        } else if (status === 0) {
            statusElement.classList.remove('green');
            statusElement.classList.add('red');
            statusElement.textContent = 'Outside';
            logAction(`Region ${identifier} - статус обновлен на Outside`);
        }
    } else {
        logAction(`Ошибка: Region с UUID ${identifier} не найден`);
    }
}



// Код для страницы Ranging
function addRegionForRangingFromUI() {
    const identifier = document.getElementById('ranging-identifier').value;
    const uuid = document.getElementById('ranging-uuid').value;
    const major = document.getElementById('ranging-major').value;
    const minor = document.getElementById('ranging-minor').value;
    const distance = document.getElementById('ranging-distance').value;

    startRangingRequest(identifier, uuid, major, minor, distance);
}

function addRegionForRanging(identifier, uuid, major, minor) {
    if (identifier) {
        const list = document.getElementById('ranging-region-list');
        const newItem = document.createElement('li');
        newItem.className = 'collection-item';
        newItem.style = 'padding-bottom: 30px;'
        newItem.id = `ranging-item-${identifier}`

        var distance = ''
        if (rangedRegions[identifier]) {
            distance += '<ul>'
            rangedRegions[identifier].beacons.forEach(function(item, i) {
                distance += `<li> ${i + 1})  ${item.accuracy} m</li>`
            })
            distance += '</ul>'
        }
        // Контент нового элемента
        newItem.innerHTML = `
            <span><strong>Identifier:</strong> ${identifier} <br/> <strong>UUID:</strong> ${uuid} <br/> <strong>Major:</strong> ${major} <br/> <strong>Minor:</strong> ${minor} </span> <br>
            <span><strong>Beacons:</strong></span>
             ${distance}
            <a href="#!" class="secondary-content" onclick="removeRegionFromRanging('${identifier}', '${uuid}', '${major}', '${minor}')">
                <i class="material-icons red-text">delete</i>
            </a>
        `;

        // Добавляем новый элемент в список
        list.appendChild(newItem);

        // Логируем добавление
//        logAction(`Добавлен Region - UUID: ${uuid}, Major: ${major}, Minor: ${minor} | <strong>Distance:</strong> ${distance} (m)`);


    } else {
        logAction("Ошибка: Identifier должен быть заполнен!");
    }
}

function removeRegionFromRanging(identifier, uuid, major, minor) {
    if (uuid == 'undefined') uuid = undefined;
    if (major == 'undefined') major = undefined;
    if (minor == 'undefined') minor = undefined;

    let region = new cordova.plugins.locationManager.BeaconRegion(identifier, uuid, major, minor)
    cordova.plugins.locationManager.stopRangingBeaconsInRegion(region).then((r) => {
        actualizeRanging()
    })
//    const item = element.parentElement;
//    item.remove();
//    logAction("Удален Region: " + item.textContent.trim());
}

function updateRangingItem(region) {
    let element = document.getElementById(`ranging-item-${region.identifier}`)
    if (element == null) {
        addRegionForRanging(region.identifier, region.uuid, region.major, region.minor)
        return
    }

    var distance = ''
    if (rangedRegions[region.identifier]) {
        distance += '<ul>'
        rangedRegions[region.identifier].beacons.forEach(function(item, i) {
            distance += `<li> ${i + 1})  ${item.accuracy} m</li>`
        })
        distance += '</ul>'
    }
    // Контент элемента
    element.innerHTML = `
        <span><strong>Identifier:</strong> ${region.identifier} <br/> <strong>UUID:</strong> ${region.uuid} <br/> <strong>Major:</strong> ${region.major} <br/> <strong>Minor:</strong> ${region.minor} </span> <br>
        <span><strong>Beacons:</strong></span>
         ${distance}
        <a href="#!" class="secondary-content" onclick="removeRegionFromRanging('${region.identifier}', '${region.uuid}', '${region.major}', '${region.minor}')">
            <i class="material-icons red-text">delete</i>
        </a>
    `;

}


function logAction(message) {
    const logOutput = document.getElementById('log-output');
    logOutput.innerHTML += `<div>${new Date().toLocaleTimeString()} - ${message}</div>`;
}





var enteredRegions = {}
// { ID_example: [{  }] }
var rangedRegions = {}
//  Методы для работы с плагином
function createDelegate() {
    var logToDom = function (message) {
        logAction(message)
    };

    var delegate = new cordova.plugins.locationManager.Delegate();

    delegate.didDetermineStateForRegion = function (pluginResult) {
        logToDom('[DOM] didDetermineStateForRegion: ' + JSON.stringify(pluginResult));

    };

    delegate.didStartMonitoringForRegion = function (pluginResult) {
        console.log('didStartMonitoringForRegion:', pluginResult);

        logToDom('didStartMonitoringForRegion:' + JSON.stringify(pluginResult));
    };

    delegate.didRangeBeaconsInRegion = function (pluginResult) {
//        logToDom('[DOM] didRangeBeaconsInRegion: ' + JSON.stringify(pluginResult));
        rangedRegions[pluginResult.region.identifier] = {
            region: pluginResult.region,
            beacons: pluginResult.beacons
        }
        actualizeRanging()
//        console.log(rangedRegions)
    };

    delegate.didEnterRegion = function (pluginResult) {
        console.log('didEnterRegion:', pluginResult);
        enteredRegions[pluginResult.region.identifier] = pluginResult
        updateMonitoringRegionStatus(pluginResult.region.identifier, 1)

        logToDom('didEnterRegion:' + JSON.stringify(pluginResult));
    }

    delegate.didExitRegion = function (pluginResult) {
        console.log('didExitRegion:', pluginResult);
        delete enteredRegions[pluginResult.region.identifier]
//        document.getElementById[`monitoring-item-${pluginResult.identifier}`]
        updateMonitoringRegionStatus(pluginResult.region.identifier, 0)

        logToDom('didExitRegion:' + JSON.stringify(pluginResult));
    }

    return delegate;
}

function startMonitoringRequest(id, uuid, major, minor) {
    if (!uuid) uuid = undefined
    if (major) { major = Number(major) } else major = undefined
    if (minor) { minor = Number(minor) } else minor = undefined

    let region = new cordova.plugins.locationManager.BeaconRegion(id, uuid, major, minor)

    cordova.plugins.locationManager.startMonitoringForRegion(region)
        .then((r) => {
            document.getElementById('identifier').value = '';
            document.getElementById('uuid').value = '';
            document.getElementById('major').value = '';
            document.getElementById('minor').value = '';

            actualizeMonitoring();
        })
        .fail(function(e) {
            console.log(e);
        })
}

function startRangingRequest(id, uuid, major, minor, distance) {
    if (!uuid) uuid = undefined
    if (major) { major = Number(major) } else major = undefined
    if (minor) { minor = Number(minor) } else minor = undefined

    let region = new cordova.plugins.locationManager.BeaconRegion(id, uuid, major, minor)

    cordova.plugins.locationManager.startRangingBeaconsInRegion(region)
        .then((r) => {
            // Очищаем поля формы
            document.getElementById('ranging-identifier').value = '';
            document.getElementById('ranging-uuid').value = '';
            document.getElementById('ranging-major').value = '';
            document.getElementById('ranging-minor').value = '';
            document.getElementById('ranging-distance').value = '';

            actualizeRanging()
        })
}

function actualizeMonitoring() {
    cordova.plugins.locationManager.getMonitoredRegions()
        .then((beacons) => {
            let l = document.getElementById("region-list")
            l.innerHTML = ''
            beacons.forEach(function(region, index, array) {
                updateMonitoringItem(region)
            })
        })
        .fail(function(e) { console.log(e); })
}

function actualizeRanging() {
    cordova.plugins.locationManager.getRangedRegions()
        .then((regions) => {
            let l = document.getElementById("ranging-region-list")
            l.innerHTML = ''
            regions.forEach(function(region, index, array) {
                updateRangingItem(region)
            })
        })
}

