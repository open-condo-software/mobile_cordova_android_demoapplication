// Функция добавления Region
function addRegion() {
    const uuid = document.getElementById('uuid').value;
    const major = document.getElementById('major').value;
    const minor = document.getElementById('minor').value;
    const distance = document.getElementById('distance').value;
    addRegionToList(uuid, major, minor, distance);
}

function addRegionToList(uuid, major, minor, distance) {
    if (uuid) {
        const list = document.getElementById('region-list');
        const newItem = document.createElement('li');
        newItem.className = 'collection-item';

        // Контент нового элемента
        newItem.innerHTML = `
            <span><strong>UUID:</strong> ${uuid} | <strong>Major:</strong> ${major} | <strong>Minor:</strong> ${minor} | <strong>Distance:</strong> ${distance} (m)</span>
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
        logAction("Ошибка: UUID должен быть заполнен!");
    }
}

// Функция удаления Region
function removeRegion(element) {
    const item = element.parentElement;
    item.remove();
    logAction("Удален Region: " + item.textContent.trim());
}

// Функция логирования
function logAction(message) {
    const logOutput = document.getElementById('log-output');
    logOutput.innerHTML += `<div>${new Date().toLocaleTimeString()} - ${message}</div>`;
}