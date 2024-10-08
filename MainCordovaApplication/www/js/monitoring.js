// Функция добавления Region
function addRegion() {
    const uuid = document.getElementById('uuid').value;
    const major = document.getElementById('major').value;
    const minor = document.getElementById('minor').value;

    addRegionToList(uuid, major, minor)
}

function addRegionToList(uuid, major, minor) {
    if (uuid && major && minor) {
        const list = document.getElementById('region-list');
        const newItem = document.createElement('li');
        newItem.className = 'collection-item';
        newItem.setAttribute("data-uuid", uuid);

        // Контент нового элемента
        newItem.innerHTML = `
            <span>
                <strong>UUID:</strong> ${uuid} | <strong>Major:</strong> ${major} | <strong>Minor:</strong> ${minor}
                <span class="new badge green" data-badge-caption="Inside" id="status-${uuid}">Inside</span>
            </span>
            <a href="#!" class="secondary-content" onclick="removeRegion(this)">
                <i class="material-icons red-text">delete</i>
            </a>
        `;

        // Добавляем новый элемент в список
        list.appendChild(newItem);

        // Логируем добавление
        logAction(`Добавлен Region - UUID: ${uuid}, Major: ${major}, Minor: ${minor}`);

        // Очищаем поля формы
        document.getElementById('uuid').value = '';
        document.getElementById('major').value = '';
        document.getElementById('minor').value = '';
    } else {
        logAction("Ошибка: Все поля формы должны быть заполнены!");
    }
}

// Функция удаления Region
function removeRegion(element) {
    const item = element.parentElement;
    const uuid = item.getAttribute("data-uuid");
    item.remove();
    logAction("Удален Region: UUID - " + uuid);
}

// Функция логирования
function logAction(message) {
    const logOutput = document.getElementById('log-output');
    logOutput.innerHTML += `<div>${new Date().toLocaleTimeString()} - ${message}</div>`;
}

// Функция обновления статуса Region
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
