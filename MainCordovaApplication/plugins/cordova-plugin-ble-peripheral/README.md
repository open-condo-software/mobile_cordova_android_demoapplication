# Bluetooth Low Energy (BLE) Peripheral Plugin for Apache Cordova

A Cordova plugin for implementing BLE (Bluetooth Low Energy) peripherals.

Need a BLE central module? See [cordova-plugin-ble-central](https://github.com/simplifier-ag/cordova-plugin-ble-peripheral).

## Supported Platforms

* iOS
* Android

## Usage

If at any point a bluetooth or location permission is required, the API will try to ask the user for it before any success or error callbacks are called.

### Callbacks

Register callbacks to receive notifications from the plugin

    blePeripheral.onWriteRequest(app.didReceiveWriteRequest);
    blePeripheral.onBluetoothStateChange(app.onBluetoothStateChange);

### Defining services with JSON

Define your Bluetooth Service using JSON

    var uartService = {
        uuid: SERVICE_UUID,
        characteristics: [
            {
                uuid: TX_UUID,
                properties: property.WRITE,
                permissions: permission.WRITEABLE,
                descriptors: [
                    {
                        uuid: '2901',
                        value: 'Transmit'
                    }
                ]
            },
            {
                uuid: RX_UUID,
                properties: property.READ | property.NOTIFY,
                permissions: permission.READABLE,
                descriptors: [
                    {
                        uuid: '2901',
                        value: 'Receive'
                    }
                ]
            }
        ]
    };

Create the service and start advertising

    Promise.all([
        blePeripheral.createServiceFromJSON(uartService),
        blePeripheral.startAdvertising(uartService.uuid, 'UART')
    ]).then(
        function() { console.log ('Created UART Service'); },
        app.onError
    );

### Defining services programatically

Instead of using JSON, you can create services programtically. Note that for 1.0 descriptors are only supported with the JSON format.

    Promise.all([
        blePeripheral.createService(SERVICE_UUID),
        blePeripheral.addCharacteristic(SERVICE_UUID, TX_UUID, property.WRITE, permission.WRITEABLE),
        blePeripheral.addCharacteristic(SERVICE_UUID, RX_UUID, property.READ | property.NOTIFY, permission.READABLE),
        blePeripheral.publishService(SERVICE_UUID),
        blePeripheral.startAdvertising(SERVICE_UUID, 'UART')
    ]).then(
        function() { console.log ('Created UART Service'); },
        app.onError
    );

## API

### blePeripheral.createService(uuid)
Create a service with given uuid.

### blePeripheral.createServiceFromJSON(json)
Create a service from json.

### blePeripheral.addCharacteristic(service, characteristic, properties, permissions)
Add a characteristic to a service with blePeripheral.properties and required blePeripheral.permissions.

### blePeripheral.publishService(seriveUUID)
Publish a service with given UUID.

### blePeripheral.removeService(seriveUUID)
Remove and unpublish a service with given UUID.

### blePeripheral.removeAllServices()
Remove and unpublish all previously added services.

### blePeripheral.startAdvertising(name, seriveUUID)
Offer your service::UUID under a given name::String.

### blePeripheral.stopAdvertising()
Stop offering the service.

### blePeripheral.setCharacteristicValue(seriveUUID, characteristicUUID, value)
Update a characteristic::UUID value::TypedArray in a given service::UUID.

### blePeripheral.onWriteRequest(callback)
Add callback:Function to get write notifications.

### blePeripheral.onBluetoothStateChange(callback)
Add callback:Function to get bluetooth state changes.

### blePeripheral.properties
Used when defining a GATT characteristic.

    READ, //read a value
    WRITE, //write a value WITH response
    WRITE_NO_RESPONSE, //write a value, but skip reponse
    NOTIFY, //central characteristic value change with acknowledge
    INDICATE //value change without ACK

### blePeripheral.permissions
Used when defining a GATT characteristic.

    READABLE //set readable only
    WRITEABLE //set writable
    READ_ENCRYPTION_REQUIRED //read requires encryption
    WRITE_ENCRYPTION_REQUIRED //write requires encryption

### Examples

See the [examples](https://github.com/simplifier-ag/cordova-plugin-ble-peripheral/tree/master/examples) for more ideas on how this plugin can be used.

# Installing

### Cordova

    $ cordova plugin add https://github.com/simplifier-ag/cordova-plugin-ble-peripheral.git

### PhoneGap

    $ phonegap plugin add https://github.com/simplifier-ag/cordova-plugin-ble-peripheral.git

### PhoneGap Build

Edit config.xml to install the plugin for [PhoneGap Build](http://build.phonegap.com).

    <gap:plugin name="cordova-plugin-ble-peripheral" source="npm" />
    
# License

Apache 2.0

# Feedback

Try the code. If you find an problem or missing feature please create a github issue. When you're submitting an issue please include a sample project that recreates the problem.