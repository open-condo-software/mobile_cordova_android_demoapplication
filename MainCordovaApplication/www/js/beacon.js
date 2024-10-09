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
    if (uuid) {
        const list = document.getElementById('region-list');
        const newItem = document.createElement('li');
        newItem.className = 'collection-item';
        newItem.setAttribute("data-uuid", uuid);
        newItem.id = `monitoring-item-${identifier}`
        newItem.style.paddingBottom = '30px'

        newItem.innerHTML = `
            <span>
                <strong>Identifier:</strong> ${identifier} | <br/> <strong>UUID:</strong> ${uuid} | <br/><strong>Major:</strong> ${major} | <br/><strong>Minor:</strong> ${minor}<br/>
                <span class="new badge green" data-badge-caption="Inside" id="status-${identifier}">Inside</span>
            </span>
            <a href="#!" class="secondary-content" onclick="removeRegionFromMonitoring(this)">
                <i class="material-icons red-text">delete</i>
            </a>
        `;

        list.appendChild(newItem);
        logAction(`Добавлен Region - UUID: ${uuid}, Major: ${major}, Minor: ${minor}`);
        document.getElementById('identifier').value = '';
        document.getElementById('uuid').value = '';
        document.getElementById('major').value = '';
        document.getElementById('minor').value = '';
        updateRegionStatus(identifier, 0);
    } else {
        logAction("Ошибка: Все поля формы должны быть заполнены!");
    }
}

function addRegionForRangingFromUI() {
    const uuid = document.getElementById('ranging-uuid').value;
    const major = document.getElementById('ranging-major').value;
    const minor = document.getElementById('ranging-minor').value;
    const distance = document.getElementById('ranging-distance').value;

    addRegionForRanging(uuid, major, minor, distance);
}

function addRegionForRanging(identifier, uuid, major, minor, distance) {
    if (uuid) {
        const list = document.getElementById('ranging-region-list');
        const newItem = document.createElement('li');
        newItem.className = 'collection-item';
        newItem.id = `ranging-item-${identifier}`

        // Контент нового элемента
        newItem.innerHTML = `
            <span><strong>UUID:</strong> ${uuid} | <strong>Major:</strong> ${major} | <strong>Minor:</strong> ${minor} | <strong>Distance:</strong> ${distance} (m)</span>
            <a href="#!" class="secondary-content" onclick="removeRegionFromRanging(this)">
                <i class="material-icons red-text">delete</i>
            </a>
        `;

        // Добавляем новый элемент в список
        list.appendChild(newItem);

        // Логируем добавление
        logAction(`Добавлен Region - UUID: ${uuid}, Major: ${major}, Minor: ${minor} | <strong>Distance:</strong> ${distance} (m)`);

        // Очищаем поля формы
        document.getElementById('ranging-uuid').value = '';
        document.getElementById('ranging-major').value = '';
        document.getElementById('ranging-minor').value = '';
        document.getElementById('ranging-distance').value = '';
    } else {
        logAction("Ошибка: UUID должен быть заполнен!");
    }
}


function removeRegionFromMonitoring(element) {
    const item = element.parentElement;
    const uuid = item.getAttribute("data-uuid");
    item.remove();
    logAction("Удален Region: UUID - " + uuid);
}

// Функция удаления Region
function removeRegionFromRanging(element) {
    const item = element.parentElement;
    item.remove();
    logAction("Удален Region: " + item.textContent.trim());
}

function logAction(message) {
    const logOutput = document.getElementById('log-output');
    logOutput.innerHTML += `<div>${new Date().toLocaleTimeString()} - ${message}</div>`;
}

function updateRegionStatus(identifier, status) {
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

function updateMonitoringItem(region) {
    let element = document.getElementById(`monitoring-item-${region.identifier}`)
    if (element == null) {
        addRegionForMonitoring(region.identifier, region.uuid, region.major, region.minor)
        return
    }

    element.innerHTML = `
            <span>
                <strong>Identifier:</strong> ${region.identifier} | <br/> <strong>UUID:</strong> ${region.uuid} | <br/><strong>Major:</strong> ${region.major} | <br/><strong>Minor:</strong> ${region.minor}<br/>
                <span class="new badge green" data-badge-caption="Inside" id="status-${region.identifier}">Inside</span>
            </span>
            <a href="#!" class="secondary-content" onclick="removeRegionFromMonitoring(this)">
                <i class="material-icons red-text">delete</i>
            </a>
        `;

    updateRegionStatus(region.identifier, 0)
}



var enteredRegions = {}
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
        logToDom('[DOM] didRangeBeaconsInRegion: ' + JSON.stringify(pluginResult));

    };

    delegate.didEnterRegion = function (pluginResult) {
        console.log('didEnterRegion:', pluginResult);
        enteredRegions[pluginResult.identifier] = pluginResult
        updateRegionStatus(region.identifier, 1)

        logToDom('didEnterRegion:' + JSON.stringify(pluginResult));
    }

    delegate.didExitRegion = function (pluginResult) {
        console.log('didExitRegion:', pluginResult);
        delete enteredRegions[pluginResult.identifier]
//        document.getElementById[`monitoring-item-${pluginResult.identifier}`]
        updateRegionStatus(region.identifier, 0)

        logToDom('didExitRegion:' + JSON.stringify(pluginResult));
    }

    return delegate;
}

function startMonitoringRequest(id, uuid, major, minor) {
    let region = new cordova.plugins.locationManager.BeaconRegion(id, uuid, Number(major), Number(minor))

    cordova.plugins.locationManager.startMonitoringForRegion(region)
        .then((r) => {
            actualizeMonitoring();
        })
        .fail(function(e) { console.log(e); })
}

function startRangingRequest(id, uuid, major, minor, distance) {
    //  TODO: distance support
    let region = cordova.plugins.locationManager.Region(id, uuid, major, minor)
    cordova.plugins.locationManager.startRangingBeaconsInRegion(region)
        .then()
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
}


