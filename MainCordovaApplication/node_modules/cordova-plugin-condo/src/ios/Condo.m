/********* Condo.m Cordova Plugin Implementation *******/

#import <Cordova/CDV.h>
#ifdef DOMA
#import "CordovaDemoApp-Swift.h"
#endif

@interface Condo: CDVPlugin 
{
  // Member variables go here.
}
- (void)requestAuthorizationCode:(CDVInvokedUrlCommand *)command;
- (void)requestAuthorization:(CDVInvokedUrlCommand *)command;
- (void)closeApplication:(CDVInvokedUrlCommand *)command;
#ifdef DOMA
@property (nonatomic, strong) DemoApplicationMainAppAPI *api;
@property (nonatomic, strong) DemoApplicationServerEmulation *serverApi;
#endif
@end

@implementation Condo

- (void)requestAuthorizationCode:(CDVInvokedUrlCommand *)command
{
    NSString *clientId = [command.arguments objectAtIndex:0];
    
    if (clientId != nil && [clientId isKindOfClass:[NSString class]] && [clientId length] > 0) 
    {
#ifdef DOMA
        if (self.api == nil)
        {
            self.api = [[DemoApplicationMainAppAPI alloc] init];
        }
        [self.api getCodeWithComplition:^(NSString *code)
        {
            if (code != nil && [code isKindOfClass:[NSString class]] && [code length] > 0)
            {
                [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:code] callbackId:command.callbackId];
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
        [self.api getCodeWithComplition:^(NSString *code)
        {
            if (code != nil && [code isKindOfClass:[NSString class]] && [code length] > 0)
            {
                if (self.serverApi == nil)
                {
                    self.serverApi = [[DemoApplicationServerEmulation alloc] init];
                }
                [self.serverApi sendCodeToServerWithCode:code clientId:clientId clientSecret:clientSecret complition:^(NSString *authorization) {
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

- (void)closeApplication:(CDVInvokedUrlCommand *)command
{
#ifdef DOMA
    [[CondoSupport getInstance] closeApp];
#endif
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
}

@end
