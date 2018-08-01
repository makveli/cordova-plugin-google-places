#import <Cordova/CDV.h>

@interface PlacesPlugin : CDVPlugin

- (void)getPredictions:(CDVInvokedUrlCommand*)command;
- (void)getById:(CDVInvokedUrlCommand*)command;

@end
