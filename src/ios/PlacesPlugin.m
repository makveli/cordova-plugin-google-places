#import "PlacesPlugin.h"

@import GooglePlaces;


@implementation PlacesPlugin {
    GMSPlacesClient *_placesClient;
}

- (void)pluginInitialize {
    NSString* apiKey = [self.commandDelegate.settings objectForKey:[@"GoogleServicesPlacesKey" lowercaseString]];

    [GMSPlacesClient provideAPIKey:apiKey];

    _placesClient = [GMSPlacesClient sharedClient];
}

- (void)getPredictions:(CDVInvokedUrlCommand *)command {
    NSString* query = [command.arguments objectAtIndex:0];
    NSDictionary* options = [command.arguments objectAtIndex:1];
    GMSAutocompleteFilter* filter = [[GMSAutocompleteFilter alloc] init];
    if (options[@"country"]) {
        filter.country = options[@"country"];
    }
    if (options[@"types"]) {
        filter.type = [options[@"types"] intValue];
    }

    GMSCoordinateBounds* bounds = nil;
//    if (options[@"bounds"]) {
//        CLLocationCoordinate2D sw;
//        CLLocationCoordinate2D ne;
//    }

    [_placesClient autocompleteQuery:query bounds:bounds filter:filter callback:^(NSArray *results, NSError *error) {
        CDVPluginResult *pluginResult;
        if (error != nil) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.localizedDescription];
        } else {
            NSMutableArray *dataArray = [[NSMutableArray alloc] init];
            for (GMSAutocompletePrediction* result in results) {
                [dataArray addObject:[self predictionToDictionary:result]];
            }
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:dataArray];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)getById:(CDVInvokedUrlCommand *)command {
    NSString* placeId = [command.arguments objectAtIndex:0];

    [_placesClient lookUpPlaceID:placeId callback:^(GMSPlace *place, NSError *error) {
        CDVPluginResult *pluginResult;
        if (error != nil) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.localizedDescription];
        } else if (place != nil) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:[self placeToDictionary:place]];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:nil];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (NSDictionary*)predictionToDictionary:(GMSAutocompletePrediction *)prediction {
    return @{
        @"fullText": prediction.attributedFullText.string,
        @"primaryText": prediction.attributedPrimaryText.string,
        @"secondaryText": prediction.attributedSecondaryText.string,
        @"placeId": prediction.placeID,
        @"types": prediction.types
    };
}

- (NSDictionary*)placeToDictionary:(GMSPlace *)place {
    NSMutableArray *addressComponents = [[NSMutableArray alloc] init];
    for (GMSAddressComponent* addressComponent in place.addressComponents) {
        [addressComponents addObject:@{
            @"type": addressComponent.type,
            @"name": addressComponent.name
        }];
    }

    return @{
        @"id": place.placeID,
        @"name": place.name ? place.name : @"",
        @"formattedAddress": place.formattedAddress,
//        @"attributions": place.attributions,
        @"types": place.types,
        @"rating": [NSNumber numberWithDouble:place.rating],
//        @"priceLevel": place.priceLevel,
        @"website": place.website ? place.website.absoluteString : @"",
//        @"openNowStatus": place.openNowStatus,
        @"phoneNumber": place.phoneNumber ? place.phoneNumber : @"",
        @"addressComponents": addressComponents,
        @"coordinate": @{
            @"latitude": [NSNumber numberWithDouble:place.coordinate.latitude],
            @"longitude": [NSNumber numberWithDouble:place.coordinate.longitude]
        }
    };
}

@end
