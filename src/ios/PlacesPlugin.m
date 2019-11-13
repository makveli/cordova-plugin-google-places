#import "PlacesPlugin.h"

@import GooglePlaces;

@implementation PlacesPlugin {
    GMSPlacesClient *placesClient;
    GMSAutocompleteSessionToken *token;
}

- (void)pluginInitialize {
    NSString *apiKey = [self.commandDelegate.settings objectForKey:[@"PLACES_IOS_API_KEY" lowercaseString]];
    [GMSPlacesClient provideAPIKey:apiKey];
    placesClient = [GMSPlacesClient sharedClient];
    token = [[GMSAutocompleteSessionToken alloc] init];
}

- (void)getPredictions:(CDVInvokedUrlCommand *)command {
    NSString *query = [command.arguments objectAtIndex:0];
    NSDictionary *options = [command.arguments objectAtIndex:1];
    
    if (options[@"newSession"]) {
      token = [[GMSAutocompleteSessionToken alloc] init];
    }
    GMSAutocompleteFilter *filter = [[GMSAutocompleteFilter alloc] init];
    if (options[@"types"]) {
      filter.type = [options[@"types"] intValue];
    }
    if (options[@"country"]) {
      filter.country = options[@"country"];
    }
	
    [placesClient findAutocompletePredictionsFromQuery:query bounds:nil boundsMode:kGMSAutocompleteBoundsModeBias
    filter:filter sessionToken:token callback:^(NSArray<GMSAutocompletePrediction *> * _Nullable results, NSError * _Nullable error) {
      CDVPluginResult *pluginResult;
      if (error != nil) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.localizedDescription];
      } else if (results != nil) {
        NSMutableArray *dataArray = [[NSMutableArray alloc] init];
        for (GMSAutocompletePrediction* result in results) {
            [dataArray addObject:[self predictionToDictionary:result]];
        }
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:dataArray];
      } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:nil];
      }
      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)getById:(CDVInvokedUrlCommand *)command {
    NSString *placeId = [command.arguments objectAtIndex:0];

    GMSPlaceField *fields = (GMSPlaceFieldName | GMSPlaceFieldPlaceID | GMSPlaceFieldCoordinate | GMSPlaceFieldFormattedAddress);

    [placesClient fetchPlaceFromPlaceID:placeId placeFields:fields sessionToken:nil callback:^(GMSPlace *place, NSError *error) {
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
    };
}

- (NSDictionary*)placeToDictionary:(GMSPlace *)place {
    return @{
        @"placeId": place.placeID,
        @"name": place.name ? place.name : @"",
        @"formattedAddress": place.formattedAddress ? place.formattedAddress : @"",
        @"latlng": @[
            [NSNumber numberWithDouble:place.coordinate.latitude],
            [NSNumber numberWithDouble:place.coordinate.longitude]
        ]
    };
}

@end
