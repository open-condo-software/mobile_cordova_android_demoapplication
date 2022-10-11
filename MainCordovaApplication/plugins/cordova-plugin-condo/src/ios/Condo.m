/********* Condo.m Cordova Plugin Implementation *******/

#import <Cordova/CDV.h>
#ifdef DOMA
#import "CordovaDemoApp-Swift.h"
#endif

@interface Condo: CDVPlugin 
{
  // Member variables go here.
}
- (void)requestAuthorization:(CDVInvokedUrlCommand *)command;
- (void)closeApplication:(CDVInvokedUrlCommand *)command;
- (void)getCurrentResident:(CDVInvokedUrlCommand *)command;
- (void)requestServerAuthorizationByUrl:(CDVInvokedUrlCommand *)command;
- (void)openURLWithFallback:(CDVInvokedUrlCommand *)command;
#ifdef DOMA
@property (nonatomic, strong) DemoApplicationMainAppAPI *api;
#endif
@end

@implementation Condo

- (void)requestAuthorization:(CDVInvokedUrlCommand *)command
{
    NSString *clientId = [command.arguments objectAtIndex:0];
    NSString *clientSecret = [command.arguments objectAtIndex:1];
    
    if (clientId != nil && [clientId isKindOfClass:[NSString class]] && [clientId length] > 0) 
    {
#ifdef DOMA
        if (self.api == nil)
        {
            self.api = [[DemoApplicationMainAppAPI alloc] init];
        }
        [self.api getCodeWithClientID:clientId complition:^(NSString *code)
        {
            if (code != nil && [code isKindOfClass:[NSString class]] && [code length] > 0)
            {
                if (self.api == nil)
                {
                    self.api = [[DemoApplicationMainAppAPI alloc] init];
                }
                [self.api sendCodeToServerWithCode:code clientId:clientId clientSecret:clientSecret complition:^(NSString *authorization) {
                    if (authorization != nil && [authorization isKindOfClass:[NSString class]] && [authorization length] > 0)
                    {
                        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:authorization] callbackId:command.callbackId];
                    }
                    else
                    {
                        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR] callbackId:command.callbackId];
                    }
                }];
            }
            else
            {
                [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR] callbackId:command.callbackId];
            }
        }];
#else
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"test_code_for_auth"] callbackId:command.callbackId];
#endif
    } 
    else 
    {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR] callbackId:command.callbackId];
    }
}

- (BOOL)nonEmptyString:(NSString *)string
{
    return (string != nil && [string isKindOfClass:[NSString class]] && [string length] > 0);
}

- (void)requestServerAuthorizationByUrl:(CDVInvokedUrlCommand *)command
{
    NSString *url = [command.arguments objectAtIndex:0];
//    reserved for future use
//    NSString *custom_params = [command.arguments objectAtIndex:1];
    
    if ([self nonEmptyString:url])
    {
#ifdef DOMA
        if (self.api == nil)
        {
            self.api = [[DemoApplicationMainAppAPI alloc] init];
        }
        [self.api getFullAuthByUrlWithUrl:url complition:^(NSDictionary *result) {
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result] callbackId:command.callbackId];
        }];
#else
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"test_code_for_auth"] callbackId:command.callbackId];
#endif
    }
    else
    {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR] callbackId:command.callbackId];
    }
}

- (void)openURLWithFallback:(CDVInvokedUrlCommand *)command
{
    NSString *url = [command.arguments objectAtIndex:0];
    NSString *fallback_url = [command.arguments objectAtIndex:1];
    if ([self nonEmptyString:url])
    {
        NSURL *firstURL = [NSURL URLWithString:url];
        [[UIApplication sharedApplication] openURL:firstURL options:@{} completionHandler:^(BOOL success) {
            if (!success)
            {
                NSURL *fallbackURL = [NSURL URLWithString:fallback_url];
                [[UIApplication sharedApplication] openURL:fallbackURL options:@{} completionHandler:^(BOOL success) {}];
            }
        }];
    }
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
}

- (void)closeApplication:(CDVInvokedUrlCommand *)command
{
#ifdef DOMA
    [[CondoSupport getInstance] closeApp];
#endif
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
}

- (void)getCurrentResident:(CDVInvokedUrlCommand *)command
{
    NSDictionary *dic = [NSJSONSerialization JSONObjectWithData:[@"{\"unitName\":\"1\",\"dv\":1,\"property\":{\"id\":\"94e2f48c-24b3-4f0d-bc9d-74ab863805a1\",\"name\":\"Лермонтова, д 7\"},\"organizationName\":\"Тестирую API\",\"organizationFeatures\":{\"hasBillingData\":false,\"hasMeters\":true},\"createdAt\":\"2022-06-29T18:31:56.786+0500\",\"_label_\":\"b7d77f24-9ae6-4f78-9915-6645175e50d1\",\"organization\":{\"sender\":{\"dv\":1},\"_label_\":\"Тестирую API -- <9c2f63c8-9288-4942-b24f-e11da26f6bec>\",\"country\":\"ru\",\"name\":\"Тестирую API\",\"id\":\"9c2f63c8-9288-4942-b24f-e11da26f6bec\",\"v\":1,\"updatedAt\":\"2022-04-18T15:23:45.431+0500\",\"tin\":\"6671095805\",\"__typename\":\"Organization\",\"dv\":1},\"v\":2,\"paymentCategories\":[{\"id\":\"1\",\"acquiringName\":\"default\",\"billingName\":\"default\",\"categoryName\":\"Квартплата\"},{\"id\":\"9\",\"acquiringName\":\"default\",\"billingName\":\"default\",\"categoryName\":\"Капремонт\"}],\"updatedAt\":\"2022-07-27T17:37:52.842+0500\",\"id\":\"b7d77f24-9ae6-4f78-9915-6645175e50d1\",\"createdBy\":{\"sender\":{\"dv\":1},\"id\":\"f919b741-1fd4-40f4-99f0-4f3d9ec031ec\",\"v\":5,\"updatedAt\":\"2022-06-29T16:01:49.279+0500\",\"createdAt\":\"2022-06-20T05:39:00.382+0500\",\"type\":\"resident\",\"name\":\"Kghv\",\"dv\":1},\"propertyId\":\"94e2f48c-24b3-4f0d-bc9d-74ab863805a1\",\"unitType\":\"flat\",\"sender\":{\"dv\":1},\"user\":{\"sender\":{\"dv\":1},\"id\":\"f919b741-1fd4-40f4-99f0-4f3d9ec031ec\",\"v\":5,\"updatedAt\":\"2022-06-29T16:01:49.279+0500\",\"createdAt\":\"2022-06-20T05:39:00.382+0500\",\"type\":\"resident\",\"name\":\"Kghv\",\"dv\":1},\"updatedBy\":{\"sender\":{\"dv\":1},\"id\":\"f919b741-1fd4-40f4-99f0-4f3d9ec031ec\",\"v\":5,\"updatedAt\":\"2022-06-29T16:01:49.279+0500\",\"createdAt\":\"2022-06-20T05:39:00.382+0500\",\"type\":\"resident\",\"name\":\"Kghv\",\"dv\":1},\"organizationId\":\"9c2f63c8-9288-4942-b24f-e11da26f6bec\",\"propertyName\":\"Лермонтова, д 7\",\"addressMeta\":{\"value\":\"г Новосибирск, ул Лермонтова, д 7\",\"data\":{\"cityTypeFull\":\"город\",\"region\":\"Новосибирская\",\"taxOfficeLegal\":\"5406\",\"regionIsoCode\":\"RU-NVS\",\"street\":\"Лермонтова\",\"country\":\"Россия\",\"taxOffice\":\"5406\",\"cityWithType\":\"г Новосибирск\",\"regionWithType\":\"Новосибирская обл\",\"oktmo\":\"50701000001\",\"capitalMarker\":\"2\",\"streetFiasId\":\"25327053-e12e-4e6a-ba9e-a25ee594c281\",\"qcGeo\":\"2\",\"regionTypeFull\":\"область\",\"geoLat\":\"55.044805\",\"countryIsoCode\":\"RU\",\"streetType\":\"ул\",\"postalCode\":\"630091\",\"cityFiasId\":\"8dea00e3-9aab-4d8e-887c-ef2aaa546456\",\"cityKladrId\":\"5400000100000\",\"geonameId\":\"1496747\",\"streetWithType\":\"ул Лермонтова\",\"cityType\":\"г\",\"okato\":\"50401386000\",\"federalDistrict\":\"Сибирский\",\"city\":\"Новосибирск\",\"houseType\":\"д\",\"streetTypeFull\":\"улица\",\"houseTypeFull\":\"дом\",\"fiasId\":\"2cac22cd-721c-4f5a-b25b-97a771da7ef4\",\"regionFiasId\":\"1ac46b49-3209-4814-b7bf-a509ea1aecd9\",\"fiasLevel\":\"8\",\"regionKladrId\":\"5400000000000\",\"house\":\"7\",\"regionType\":\"обл\",\"fiasActualityState\":\"0\",\"geoLon\":\"82.926862\",\"kladrId\":\"5400000100007340013\",\"houseFiasId\":\"2cac22cd-721c-4f5a-b25b-97a771da7ef4\",\"streetKladrId\":\"54000001000073400\",\"houseKladrId\":\"5400000100007340013\"},\"unrestrictedValue\":\"630091, Новосибирская обл, г Новосибирск, Центральный р-н, ул Лермонтова, д 7\"},\"address\":\"г Новосибирск, ул Лермонтова, д 7\"}" dataUsingEncoding:NSUTF8StringEncoding] options:0 error:nil];
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dic] callbackId:command.callbackId];
}

@end
