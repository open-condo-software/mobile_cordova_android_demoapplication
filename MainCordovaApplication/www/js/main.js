var services = []
var advServices = []
var characteristicsValue = {}

$("#btn-create-service").on('click', function() {
    // show dialog for service uuid enter
    showCreateServiceDialog();
});



function showCreateServiceDialog() {
    $('#create-service-uuid-field')[0].value = ''
    $('#dialog-service-create').dialog('open');
}

function showAddCharacteristicDialog(service_index) {
    let service = services[service_index]
    $('#dialog-characteristic-title')[0].textContent = `Добавление характеристики для сервиса: ${service.uuid}`
    $('#create-characteristic-uuid-field').attr('service-uuid', service.uuid)
    $('#create-characteristic-uuid-field').attr('service-index', service_index)
    $('#create-characteristic-uuid-field')[0].value = ''

    $('#dialog-characteristic-create').dialog('open');
}

function showCreateServiceJsonDialog() {
    $('#dialog-service-create-json').dialog('open');
}

$('#remove-all-services').on('click', function() {
    removeAllServices()
})

$('#stop-advertising').on('click', function () {
    stopAdvertising()
})

$('#dialog-service-create').dialog({
    autoOpen: false,
    show: {
        effect: "blind",
        duration: 300
    },
    hide: {
        effect: "explode",
        duration: 300
    },
    modal: true,
    resizable: false,
    position: {
        my: "center top",
        at: "center top",
        of: window
    },
    buttons: {
        "RND": function() {
            $(this).find('input')[0].value = crypto.randomUUID()
        },
        "Создать": function() {
            let uuid = $('#create-service-uuid-field')[0].value
            
            blePeripheral.createService(uuid).then(() => {
                showStatus(`Сервис создан: ${uuid}`)
                
                services.push({ 
                    uuid : uuid,
                    characteristics: []
                })
                buildServices()
    
                $(this).dialog('close')
            }).catch((e) => {
                showError(e)
            })
        },
        Cancel: function() {
            $(this).dialog('close');
            $('#create-service-uuid-field')[0].value = '';
        }
    }
})

$('#dialog-characteristic-create').dialog({
    autoOpen: false,
    show: {
        effect: "blind",
        duration: 300
    },
    hide: {
        effect: "explode",
        duration: 300
    },
    modal: true,
    resizable: false,
    position: {
        my: "center top",
        at: "center top",
        of: window
    },
    buttons: {
        "RND": function() {
            $(this).find('input')[0].value = crypto.randomUUID()
        },
        "Добавить": function() {
            let service_uuid = $(this).find('#create-characteristic-uuid-field').attr('service-uuid')
            let service_index = $(this).find('#create-characteristic-uuid-field').attr('service-index')
            let characteristic_uuid = $(this).find('#create-characteristic-uuid-field')[0].value
            
            let permissionsObj = []
            var permissions = 0
            $(this).find("#permissions-list").find('input[type=checkbox]').each((i, e) => {
                let f_type = e.getAttribute('field-type')
                if (e.checked) {
                    permissions = permissions | blePeripheral.permissions[f_type]
                    permissionsObj.push(f_type)
                }
            })

            let propertiesObj = []
            var properties = 0
            $(this).find("#property-list").find('input[type=checkbox]').each((i, e) => {
                let f_type = e.getAttribute('field-type')
                if (e.checked) {
                    properties = properties | blePeripheral.properties[f_type]
                    propertiesObj.push(f_type)
                }
            })

            blePeripheral.addCharacteristic(service_uuid, characteristic_uuid, properties, permissions).then(() => {
                showStatus(`Характеристика добавлена: ${characteristic_uuid}`)
                services[service_index].characteristics.push({
                    uuid: characteristic_uuid,
                    permissions: permissionsObj,
                    properties: propertiesObj
                })
                
                buildServices()
                $(this).dialog('close')
            }).catch((e) => {
                showError(e)
            })

        },
        Cancel: function() {
            $(this).dialog('close');
            $('#create-service-uuid-field')[0].value = '';
        }
    }
})

$('#dialog-service-create-json').dialog({
    autoOpen: false,
    show: {
        effect: "blind",
        duration: 300
    },
    hide: {
        effect: "explode",
        duration: 300
    },
    modal: true,
    resizable: false,
    position: {
        my: "center top",
        at: "center top",
        of: window
    },
    buttons: {
        // "RND": function() {
        //     $(this).find('input')[0].value = crypto.randomUUID()
        // },
        "Создать": function() {
            let jsonInput = $('#ta-create-service-json')
            let json = jsonInput[0].value
            let jsonObject = JSON.parse(json)

            blePeripheral.createServiceFromJSON(jsonObject).then(() => {
                let characteristics = []
                for (i in jsonObject.characteristics) {
                    let characteristic = jsonObject.characteristics[i]
                    characteristics.push({
                        uuid: characteristic.uuid,
                        properties: characteristic.properties,
                        permissions: characteristic.permissions
                    })
                }
                services.push({
                    uuid: jsonObject.uuid,
                    characteristics: characteristics
                })
                buildServices()
                $(this).dialog('close');
            })

        },
        Cancel: function() {
            $(this).dialog('close');
            $('#create-service-uuid-field')[0].value = '';
        }
    }
})

$('#btn-create-service-json').on('click', function name() {
    showCreateServiceJsonDialog()
})

function buildServices() {
    $('.services-list').children().remove()

    // Services
    services.forEach((service, index) => {
        var serviceAdvertised = serviceIsAdvertised(service.uuid)
        
        let serviceBlock = $('.service-block-tmp').clone()
        serviceBlock.removeClass('service-block-tmp')

        let title = serviceBlock.find('[target="service-title"]')
        title[0].textContent = `Service ${index + 1}: ${service.uuid}`

        let characteristicsList = serviceBlock.find('.characteristics-list')
        if (service.characteristics.length == 0) {
            characteristicsList.find('.empty-list-span').show()
        } else {
            characteristicsList.find('.empty-list-span').hide()
        }
        
        // Characteristics
        service.characteristics.forEach((characteristic, index) => {
            let characteristicElement = $('.characteristic-block-tmp').clone()
            characteristicElement.removeClass('characteristic-block-tmp')

            let characteristicTitle = characteristicElement.find('.characteristic-title-span')
            characteristicTitle[0].textContent = `Characteristic ${characteristic.uuid}`

            characteristicElement.find('.permissions-detail')[0].textContent = `Permissions: ${characteristic.permissions}`
            characteristicElement.find('.properties-detail')[0].textContent = `Properties: ${characteristic.properties}`
            
            characteristicElement.show()
            characteristicsList.append(characteristicElement)
            let characteristicTextValueElement = characteristicElement.find('textarea.characteristic-value')
            characteristicTextValueElement.attr('characteristic-read-value', characteristic.uuid.toLowerCase())
            characteristicTextValueElement[0].addEventListener('input', function() {
                characteristicsValue[characteristic.uuid] = this.value
                // event handling code for sane browsers
            }, false);

            if (characteristicsValue[characteristic.uuid] != undefined) {
                characteristicTextValueElement[0].value = characteristicsValue[characteristic.uuid]
            }
        })

        serviceBlock.find('.btn-add-characteristic').on('click', function() {
            showAddCharacteristicDialog(index)
        })

        let runElement = serviceBlock.find('.run-action')
        runElement.on('click', function () {
            runService(index)
        })

        let removeElement = serviceBlock.find('.remove-action')
        removeElement.on('click', function () {
            removeService(index);
        })

        serviceBlock.show()
        $('.services-list').append(serviceBlock)
    })
}

function removeService(service_index) {
    let service = services[service_index]
    console.log("remove:")
    console.log(service)

    blePeripheral.removeService(service.uuid).then(() => {
        services.pop(service_index)
        buildServices()
        updateAdvServices()
        showStatus(`Сервис удален: ${service.uuid}`)
    }).catch((e) => {
        showError(e)
    })
}

function runService(service_index) {
    let service = services[service_index]
    blePeripheral.publishService(service.uuid).then(() => {
        console.log(1)
        blePeripheral.startAdvertising(service.uuid).then(() => {
            showStatus(`run success [${service.uuid}]`)
            console.log(2)
            updateAdvServices()
        }).catch((e) => {
            showError(e)
        })
    }).catch((e) => {
        showError(e)
    })
    service.uuid
    console.log("run:")
    console.log(service)
}

function removeAllServices() {
    blePeripheral.removeAllServices().then(() => {
        services = []
        buildServices()
        updateAdvServices()
        showStatus("все сервисы удалены")
    }).catch((e) => {
        showError(e)
    })
}

document.addEventListener('deviceready', onReady, false);

function onReady() {
    // Configure properties and permissions
    for(permission in blePeripheral.permissions) {
        let checkBox = $(`
            <div>
                <input type="checkbox" field-type="${permission}" />
                <label for="scales">${permission}</label>
            </div>
        `)
        
        $('#permissions-list').append(checkBox)
    }

    for(property in blePeripheral.properties) {
        let checkBox = $(`
            <div>
                <input type="checkbox" field-type="${property}" />
                <label for="scales">${property}</label>
            </div>
        `)
        
        $('#property-list').append(checkBox)
    }

    blePeripheral.onReadRequest((service, characteristic) => {
        let text = $(`textarea[characteristic-read-value="${characteristic.toLowerCase()}"]`)
        return text[0].value
    });

    blePeripheral.onWriteRequest((obj) => {
        let characteristic = obj.characteristic
        let text = $(`textarea[characteristic-read-value="${characteristic.toLowerCase()}"]`)
        text[0].value = arrayBufferToString(obj.value)
        characteristicsValue[characteristic] = arrayBufferToString(obj.value)
    })

    updateAdvServices()
    buildServices()
}

function serviceIsAdvertised(service_uuid) {
    for (service in advServices) {
        if (service.uuid == service_uuid) {
            return true
        }
    }

    return false
}

function updateAdvServices() {
    blePeripheral.getBluetoothSystemState().then((obj) => {
        advServices = []
        for(serviceIndex in obj.services) {
            let service = obj.services[serviceIndex]
            let characteristics = []
            for(characteristicIndex in service.characteristics) {
                let characteristic = service.characteristics[characteristicIndex]
                characteristics.push({
                    uuid: characteristic.uuid,
                    permissions: characteristic.permissions,
                    properties: characteristic.properties
                })
            }
            advServices.push({
                uuid: service.uuid,
                characteristics: characteristics
            })
        }

        let mappedServices = mapAdvServices(advServices)
        for(mServiceIndex in mappedServices) {
            let mService = mappedServices[mServiceIndex]
            var founded = false
            for(currServiceIndex in services) {
                let currService = services[currServiceIndex]
                if (currService.uuid == mService.uuid) {
                    founded = true
                }
            }

            if (!founded) {
                services.push(mService)
            }

        }

        buildServices()
    }).catch((e) => {
        showError(e)
    })
}

function stopAdvertising() {
    blePeripheral.stopAdvertising().then(() => {
        showStatus("Адвертайзинг остановлен")
        updateAdvServices()
        buildServices()
    }).catch((e) => {
        showError(e)
    })
} 

function showStatus(message) {
    let field = $('#status-field')
    field[0].textContent = `Status: ${message}`
    field.show()
    $('#status-field').delay(3000).hide(0);
}

function showError(error) {
    let field = $('#status-field')
    field[0].textContent = `ERROR: ${error}`
    field.show()
    $('#status-field').delay(5000).hide(0);
}

function arrayBufferToString(buffer){
    var arr = new Uint8Array(buffer);
    var str = String.fromCharCode.apply(String, arr);
    if(/[\u0080-\uffff]/.test(str)){
        throw new Error("this string seems to contain (still encoded) multibytes");
    }
    return str;
}

function getProperties(properties) {
    let resultProps = []
    for(property in blePeripheral.properties) {
        if (properties & blePeripheral.properties[property]) {
            resultProps.push(property)
        }
    }

    return resultProps
}

function getPermissions(permissions) {
    let resultpermissions = []
    for(permission in blePeripheral.permissions) {
        if (permissions & blePeripheral.permissions[permission]) {
            resultpermissions.push(permission)
        }
    }

    return resultpermissions
}

function mapAdvServices(advServices) {
    let mappedServices = []
    for(serviceIndex in advServices) {
        let service = advServices[serviceIndex]
        let charact = []
        for (characteristicIndex in service.characteristics) {
            let characteristic = service.characteristics[characteristicIndex]
            let properties = getProperties(parseInt(characteristic.properties))
            let permissions = getPermissions(parseInt(characteristic.permissions))
            charact.push({
                uuid: characteristic.uuid,
                permissions: permissions,
                properties: properties
            })
        }
        
        mappedServices.push({
            uuid: service.uuid,
            characteristics: charact
        })
    }

    return mappedServices
}
