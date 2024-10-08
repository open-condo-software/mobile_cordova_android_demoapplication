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
    const uuid = document.getElementById('uuid').value;
    const major = document.getElementById('major').value;
    const minor = document.getElementById('minor').value;

    addRegionForMonitoring(uuid, major, minor)
}

function addRegionForMonitoring(uuid, major, minor) {
    if (uuid && major && minor) {
        const list = document.getElementById('region-list');
        const newItem = document.createElement('li');
        newItem.className = 'collection-item';
        newItem.setAttribute("data-uuid", uuid);

        newItem.innerHTML = `
            <span>
                <strong>UUID:</strong> ${uuid} | <strong>Major:</strong> ${major} | <strong>Minor:</strong> ${minor}
                <span class="new badge green" data-badge-caption="Inside" id="status-${uuid}">Inside</span>
            </span>
            <a href="#!" class="secondary-content" onclick="removeRegionFromMonitoring(this)">
                <i class="material-icons red-text">delete</i>
            </a>
        `;

        list.appendChild(newItem);
        logAction(`Добавлен Region - UUID: ${uuid}, Major: ${major}, Minor: ${minor}`);
        document.getElementById('uuid').value = '';
        document.getElementById('major').value = '';
        document.getElementById('minor').value = '';
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

function addRegionForRanging(uuid, major, minor, distance) {
    if (uuid) {
        const list = document.getElementById('ranging-region-list');
        const newItem = document.createElement('li');
        newItem.className = 'collection-item';

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

function updateRegionStatus(uuid, status) {
    const statusElement = document.getElementById(`status-${uuid}`);
    if (statusElement) {
        if (status === 1) {
            statusElement.classList.remove('red');
            statusElement.classList.add('green');
            statusElement.textContent = 'Inside';
            logAction(`Region ${uuid} - статус обновлен на Inside`);
        } else if (status === 0) {
            statusElement.classList.remove('green');
            statusElement.classList.add('red');
            statusElement.textContent = 'Outside';
            logAction(`Region ${uuid} - статус обновлен на Outside`);
        }
    } else {
        logAction(`Ошибка: Region с UUID ${uuid} не найден`);
    }
}
