//
//  BLEPeripheralPlugin.m
//  BLE Peripheral Cordova Plugin
//
//  (c) 2106 Don Coleman
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#import "BLEPeripheralPlugin.h"
#import <Cordova/CDV.h>

static NSDictionary *dataToArrayBuffer(NSData* data) {
    return @{
             @"CDVType" : @"ArrayBuffer",
             @"data" :[data base64EncodedStringWithOptions:0]
             };
}

@interface BLECharacteristicReadRequest : NSObject
@property (nonatomic, nonnull) NSUUID *contextID;
@property (nonatomic, nullable, weak) BLEPeripheralPlugin *plugin;
@property (nonatomic, nonnull) CBATTRequest *request;
@property (nonatomic, nonnull) NSTimer *timeout;

- (id)initWithPlugin:(BLEPeripheralPlugin *)plugin request:(CBATTRequest *)request;
- (void)dealloc;

@end

@implementation BLECharacteristicReadRequest

@synthesize contextID;
@synthesize timeout;
@synthesize plugin;
@synthesize request;

- (id)initWithPlugin:(BLEPeripheralPlugin *)plugin request:(CBATTRequest *)request {
    if (self = [super init]) {
        
        NSUUID *contextID = [NSUUID UUID];
        self.contextID = contextID;
        self.request = request;
        self.timeout = [NSTimer scheduledTimerWithTimeInterval:3.0 repeats:NO block:^(NSTimer * _Nonnull timer) {
            [plugin performSelector:@selector(requestedCharacteristicValueTimeout:) withObject:contextID];
        }];
        
        return self;
    } else {
        return nil;
    }
}

- (void)dealloc {
    [timeout invalidate];
}

@end

@interface BLEPeripheralPlugin() {
    NSDictionary *bluetoothStates;
}
@end

@implementation BLEPeripheralPlugin

@synthesize manager;

NSMutableDictionary<NSUUID *, BLECharacteristicReadRequest *> *readingCallbacks;

- (void)pluginInitialize {

    NSLog(@"Cordova BLE Peripheral Plugin");
    NSLog(@"(c)2016 Don Coleman");

    [super pluginInitialize];

    manager = [[CBPeripheralManager alloc] initWithDelegate:self queue:nil];
    services = [NSMutableDictionary new];
    readingCallbacks = [NSMutableDictionary new];
    bluetoothStates = [NSDictionary dictionaryWithObjectsAndKeys:
                       @"unknown", @(CBPeripheralManagerStateUnknown),
                       @"resetting", @(CBPeripheralManagerStateResetting),
                       @"unsupported", @(CBPeripheralManagerStateUnsupported),
                       @"unauthorized", @(CBPeripheralManagerStateUnauthorized),
                       @"off", @(CBPeripheralManagerStatePoweredOff),
                       @"on", @(CBPeripheralManagerStatePoweredOn),
                       nil];
}

#pragma mark - Cordova Plugin Methods

- (void)createService:(CDVInvokedUrlCommand *)command {
    NSString *uuidString = [command.arguments objectAtIndex:0];
    CBUUID *serviceUUID = [CBUUID UUIDWithString: uuidString];
    CBMutableService *service = [[CBMutableService alloc] initWithType:serviceUUID primary:YES];
    [services setObject:service forKey:uuidString];

    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)addCharacteristic:(CDVInvokedUrlCommand *)command {
    NSString *serviceUUIDString = [command.arguments objectAtIndex:0];

    CBMutableService *service = [services objectForKey:serviceUUIDString];

    if (service) {
        NSString *characteristicUUIDString = [command.arguments objectAtIndex:1];
        CBUUID *characteristicUUID = [CBUUID UUIDWithString: characteristicUUIDString];

        NSNumber *properties = [command.arguments objectAtIndex:2];
        NSNumber *permissions = [command.arguments objectAtIndex:3];

        CBMutableCharacteristic *characteristic = [[CBMutableCharacteristic alloc]
                                                   initWithType:characteristicUUID
                                                   properties: properties.intValue & 0xff
                                                   value:nil
                                                   permissions: permissions.intValue & 0xff];

        // appending characteristic to existing list
        NSMutableArray *characteristics = [NSMutableArray arrayWithArray:[service characteristics]];
        [characteristics addObject:characteristic];
        service.characteristics = characteristics;

        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

    } else {
        NSString *message = [NSString stringWithFormat:@"Service not found for UUID %@", serviceUUIDString];
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:message];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }

}

// note: need to call this after the characteristics are added
- (void)publishService:(CDVInvokedUrlCommand *)command {
    NSLog(@"%@", @"publishService");
    NSString *serviceUUIDString = [command.arguments objectAtIndex:0];
    publishServiceCallbackId = [command.callbackId copy];
    CBMutableService *service = [services objectForKey:serviceUUIDString];
    [manager addService:service];
}

- (void)setCharacteristicValue:(CDVInvokedUrlCommand *)command {
    NSLog(@"%@", @"setCharacteristicValue");
    NSString *serviceUUIDString = [command.arguments objectAtIndex:0];
    CBMutableService *service = [services objectForKey:serviceUUIDString];

    NSString *characteristicUUIDString = [command.arguments objectAtIndex:1];
    CBUUID *characteristicUUID = [CBUUID UUIDWithString:characteristicUUIDString];

    NSData *data = [command.arguments objectAtIndex:2];

    if (service) {
        CBMutableCharacteristic *characteristic  = (CBMutableCharacteristic*)[self findCharacteristicByUUID: characteristicUUID service:service];

        [characteristic setValue:data];

        // if notify && value has changed
        [manager updateValue:data forCharacteristic:characteristic onSubscribedCentrals:nil];

        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

    } else {
        NSString *message = [NSString stringWithFormat:@"Service not found for UUID %@", serviceUUIDString];
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:message];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }

}

- (void)createServiceFromJSON:(CDVInvokedUrlCommand *)command {
    NSLog(@"%@", @"addServiceFromJSON");

    createServiceFromJSONCallbackId = [command.callbackId copy];

    // This might be a problem when the data contains nested ArrayBuffers
    NSDictionary *dictionary = [command.arguments objectAtIndex:0];
    CBMutableService *service = [self serviceFromJSON: dictionary];
    [manager addService:service];
}

- (void)removeService:(CDVInvokedUrlCommand *)command{
    NSString *serviceUUIDString = [command.arguments objectAtIndex:0];
    CBMutableService *service = [services objectForKey:serviceUUIDString];

    if (service) {
        [manager removeService:service];
        [services removeObjectForKey:serviceUUIDString];
        
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
    NSString *message = [NSString stringWithFormat:@"Service not found for UUID %@", serviceUUIDString];
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:message];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)removeAllServices:(CDVInvokedUrlCommand *)command{
    [manager removeAllServices];
    [services removeAllObjects];
    
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)startAdvertising:(CDVInvokedUrlCommand *)command {

    NSString *localName = [command.arguments objectAtIndex:1];
    NSString *serviceUUIDString = [command.arguments objectAtIndex:0];
    CBUUID *serviceUUID = [CBUUID UUIDWithString: serviceUUIDString];


    [manager startAdvertising:@{
                               CBAdvertisementDataServiceUUIDsKey : @[serviceUUID],
                               CBAdvertisementDataLocalNameKey : localName
                               }];

    startAdvertisingCallbackId = [command.callbackId copy];
}

- (void)stopAdvertising:(CDVInvokedUrlCommand *)command {
        [manager stopAdvertising];
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        
}

- (void)setCharacteristicValueChangedListener:(CDVInvokedUrlCommand *)command {
    characteristicValueChangedCallback = [command.callbackId copy];
}

-(void)setCharacteristicValueRequestedListener:(CDVInvokedUrlCommand *)command {
    charactristicReadValueCallback = [command.callbackId copy];
}


/// js did calculate characteristic value;
/// - Parameter command: 0 - contextID, 1 - result;
-(void)receiveRequestedCharacteristicValue:(CDVInvokedUrlCommand *)command {
    NSString *someUniqueIDString = [command.arguments objectAtIndex:0];
    NSData *result = [command.arguments objectAtIndex:1];
    NSLog(@"receiveRequestedCharacteristicValue: %@", result);
    
    if (!someUniqueIDString || !result) {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    
    NSUUID *contextID = [[NSUUID alloc] initWithUUIDString:someUniqueIDString];
    BLECharacteristicReadRequest *requestContext = [readingCallbacks objectForKey:contextID];
    if (!requestContext) {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    
    [readingCallbacks removeObjectForKey:contextID];
    [requestContext.timeout invalidate];
    
    CBUUID *cbuuid = requestContext.request.characteristic.UUID;
    CBService *service = requestContext.request.characteristic.service;
    if (!service) {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    
    CBATTRequest *request = requestContext.request;
    CBCharacteristic *characteristic = [self findCharacteristicByUUID:cbuuid service:service];
    if (!characteristic) {
        characteristic = request.characteristic;
    }
    
    request.value = [result subdataWithRange:NSMakeRange(request.offset, result.length - request.offset)];
    [manager respondToRequest:request
                   withResult:CBATTErrorSuccess];
    
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

-(void)requestedCharacteristicValueTimeout:(NSUUID *)context {
    
    BLECharacteristicReadRequest *requestContext = [readingCallbacks objectForKey:context];
    if (!context) {
        return;
    }
    
    [readingCallbacks removeObjectForKey:context];
    [requestContext.timeout invalidate];
    
    CBCharacteristic * characteristic = requestContext.request.characteristic;
    CBService * service = characteristic.service;
    if (!service) {
        return;
    }
    
    [self sendCharacteristicReadFallbackResultWithCharacteristic:characteristic
                                                         service:service
                                                         request:requestContext.request];
}

- (void)setDescriptorValueChangedListener:(CDVInvokedUrlCommand *)command {
    descriptorValueChangedCallback  = [command.callbackId copy];
}

- (void)setBluetoothStateChangedListener:(CDVInvokedUrlCommand *)command {
    bluetoothStateChangedCallback  = [command.callbackId copy];

    int bluetoothState = [manager state];
    NSString *state = [bluetoothStates objectForKey:[NSNumber numberWithInt:bluetoothState]];
    CDVPluginResult *pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:state];
    [pluginResult setKeepCallbackAsBool:TRUE];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

#pragma mark - CBPeripheralManagerDelegate

- (void)peripheralManagerDidUpdateState:(CBPeripheralManager *)peripheral
{
    NSString *state = [bluetoothStates objectForKey:@(peripheral.state)];

    if (bluetoothStateChangedCallback) {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:state];
        [pluginResult setKeepCallbackAsBool:TRUE];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:bluetoothStateChangedCallback];
    }

}

- (void) peripheralManager:(CBPeripheralManager *)peripheral didAddService:(CBService *)service error:(NSError *)error {

    NSLog(@"Added a service");
    if (error) {
        NSLog(@"There was an error adding service");
        NSLog(@"%@", error);
    }

    if (publishServiceCallbackId) {
        CDVPluginResult *pluginResult = nil;

        if (!error) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        } else {
            // TODO resultsWithStatus:MessageToErrorObject:int
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error localizedDescription]];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:publishServiceCallbackId];

        publishServiceCallbackId = nil;
    }

    // essentially the same as above
    if (createServiceFromJSONCallbackId) {
        CDVPluginResult *pluginResult = nil;

        if (!error) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        } else {
            // TODO resultsWithStatus:MessageToErrorObject:int
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error localizedDescription]];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:createServiceFromJSONCallbackId];

        createServiceFromJSONCallbackId = nil;
    }

}

- (void) peripheralManagerDidStartAdvertising:(CBPeripheralManager *)peripheral error:(NSError *)error {
    NSLog(@"Started advertising");
    if (error) {
        NSLog(@"There was an error advertising");
        NSLog(@"%@", error);
    }

    if (startAdvertisingCallbackId) {
        CDVPluginResult *pluginResult = nil;

        if (!error) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        } else {
            // TODO resultsWithStatus:MessageToErrorObject:int
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error localizedDescription]];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:startAdvertisingCallbackId];

        startAdvertisingCallbackId = nil;
    }
    
}

- (void)peripheralManager:(CBPeripheralManager *)peripheral central:(CBCentral *)central didSubscribeToCharacteristic:(CBCharacteristic *)characteristic
{
    NSLog(@"Central subscribed to characteristic");
}

-(void)peripheralManager:(CBPeripheralManager *)peripheral didReceiveWriteRequests:(NSArray<CBATTRequest *> *)requests
{
    NSLog(@"Received %lu write requests", (unsigned long)[requests count]);

    for (CBATTRequest *request in requests) {
        CBCharacteristic *characteristic = [request characteristic];

        NSMutableDictionary *dictionary = [NSMutableDictionary new];
        [dictionary setObject:[[[characteristic service] UUID] UUIDString] forKey:@"service"];
        [dictionary setObject:[[characteristic UUID] UUIDString] forKey:@"characteristic"];
        if ([request value]) {
            [dictionary setObject:dataToArrayBuffer([request value]) forKey:@"value"];
        }

        if (characteristicValueChangedCallback) {
            CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dictionary];
            [pluginResult setKeepCallbackAsBool:TRUE];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:characteristicValueChangedCallback];
        }

        [peripheral respondToRequest:request withResult:CBATTErrorSuccess];
    }
}

-(void)peripheralManager:(CBPeripheralManager *)peripheral didReceiveReadRequest:(CBATTRequest *)request {
    NSLog(@"Received read request for %@", [request characteristic]);

    CBCharacteristic *requestedCharacteristic = request.characteristic;
    CBService *requestedService = [requestedCharacteristic service];
    if (!requestedService) {
        return;
    }
    
    if (charactristicReadValueCallback) {
        
        BLECharacteristicReadRequest *context = [[BLECharacteristicReadRequest alloc] initWithPlugin:self request:request];
        [readingCallbacks setObject:context forKey:context.contextID];
        
        NSMutableDictionary *dict = [NSMutableDictionary new];
        [dict setObject:context.contextID.UUIDString forKey:@"contextID"];
        [dict setObject:requestedCharacteristic.UUID.UUIDString forKey:@"characteristic"];
        [dict setObject:requestedService.UUID.UUIDString forKey:@"service"];
        
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dict];
        [result setKeepCallbackAsBool:YES];
        [self.commandDelegate sendPluginResult:result callbackId:charactristicReadValueCallback];
        
    } else {
        [self sendCharacteristicReadFallbackResultWithCharacteristic:requestedCharacteristic
                                                             service:requestedService
                                                             request:request];
    }
}

-(void)sendCharacteristicReadFallbackResultWithCharacteristic:(CBCharacteristic *)characteristic service:(CBService *)service request:(CBATTRequest *)request {
    
    CBCharacteristic *requestedCharacteristic = [self findCharacteristicByUUID:characteristic.UUID
                                                                       service:service];
    if (!requestedCharacteristic) {
        requestedCharacteristic = characteristic;
    }
    
    request.value = [characteristic.value subdataWithRange:NSMakeRange(request.offset, characteristic.value.length - request.offset)];
    [manager respondToRequest:request withResult:CBATTErrorSuccess];
}


- (void)peripheralManager:(CBPeripheralManager *)peripheral central:(CBCentral *)central didUnsubscribeFromCharacteristic:(CBCharacteristic *)characteristic
{
    NSLog(@"Central unsubscribed from characteristic");
}


- (void)peripheralManagerIsReadyToUpdateSubscribers:(CBPeripheralManager *)peripheral
{
    NSLog(@"peripheralManagerIsReadyToUpdateSubscribers");
}

#pragma mark - Internal Implementation

// Find a characteristic in service with a specific property
-(CBCharacteristic *) findCharacteristicByUUID:(CBUUID *)UUID service:(CBService*)service
{
    NSLog(@"Looking for %@", UUID);
    for(int i=0; i < service.characteristics.count; i++)
    {
        CBCharacteristic *c = [service.characteristics objectAtIndex:i];
        if ([c.UUID.UUIDString isEqualToString: UUID.UUIDString]) {
            return c;
        }
    }
    return nil; //Characteristic not found on this service
}

// TODO need errors here to call error callback
- (CBMutableService*) serviceFromJSON:(NSDictionary *)serviceDict {

    NSString *serviceUUIDString = [serviceDict objectForKey:@"uuid"];
    CBUUID *serviceUUID = [CBUUID UUIDWithString: serviceUUIDString];

    // TODO primary should be in the JSON
    CBMutableService *service = [[CBMutableService alloc] initWithType:serviceUUID primary:YES];

    // create characteristics
    NSMutableArray *characteristics = [NSMutableArray new];
    NSArray *characteristicList = [serviceDict objectForKey:@"characteristics"];
    for (NSDictionary *characteristicData in characteristicList) {

        NSString *characteristicUUIDString = [characteristicData objectForKey:@"uuid"];
        CBUUID *characteristicUUID = [CBUUID UUIDWithString: characteristicUUIDString];

        NSNumber *properties = [characteristicData objectForKey:@"properties"];
        NSString *permissions = [characteristicData objectForKey:@"permissions"];

        CBMutableCharacteristic *characteristic = [[CBMutableCharacteristic alloc] initWithType:characteristicUUID properties:[properties intValue] value:nil permissions:[permissions intValue]];

        // add descriptors
        NSMutableArray *descriptors = [NSMutableArray new];
        NSArray *descriptorsList = [characteristicData objectForKey:@"descriptors"];
        for (NSDictionary *descriptorData in descriptorsList) {

            // CBUUIDCharacteristicUserDescriptionString
            NSString *descriptorUUIDString = [descriptorData objectForKey:@"uuid"];
            CBUUID *descriptorUUID = [CBUUID UUIDWithString: descriptorUUIDString];

            // TODO this won't always be a String
            NSString *descriptorValue = [descriptorData objectForKey:@"value"];

            CBMutableDescriptor *descriptor = [[CBMutableDescriptor alloc]
                                               initWithType: descriptorUUID
                                               value:descriptorValue];
            [descriptors addObject:descriptor];
        }

        characteristic.descriptors = descriptors;

        [characteristics addObject: characteristic];
    }

    [service setCharacteristics:characteristics];
    [services setObject:service forKey:[[service UUID] UUIDString]];

    return service;

}

@end
