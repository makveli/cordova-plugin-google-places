
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Arrays;

import by.chemerisuk.cordova.support.CordovaMethod;
import by.chemerisuk.cordova.support.ReflectiveCordovaPlugin;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;

import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PlacesPlugin extends ReflectiveCordovaPlugin {

  private PlacesClient placesClient;
  private AutocompleteSessionToken token;

    @Override
    protected void pluginInitialize() {
      String apiKey = preferences.getString("PLACES_ANDROID_API_KEY", "");
      Places.initialize(cordova.getActivity().getApplicationContext(), apiKey);
      placesClient = Places.createClient(cordova.getActivity().getApplicationContext());
      token = AutocompleteSessionToken.newInstance();
    }

    @CordovaMethod
    public void getPredictions(String query, JSONObject settings, final CallbackContext callbackContext) throws JSONException {
      if (settings.getBoolean("newSession")) {
        token = AutocompleteSessionToken.newInstance();
      }
      FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
        .setCountry((settings.has("country")) ? settings.getString("country") : null)
        .setTypeFilter((settings.has("types")) ? TypeFilter.values()[settings.getInt("types") - 1] : null)
        .setSessionToken(token)
        .setQuery(query)
        .build();
      
      placesClient.findAutocompletePredictions(request).addOnSuccessListener((response) -> {
        JSONArray result = new JSONArray();
        for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
          result.put(predictionToJSON(prediction));
        }
        callbackContext.success(result);
      }).addOnFailureListener((exception) -> {
        callbackContext.error(exception.getMessage());
      });
    }

    @CordovaMethod
    public void getById(String placeId, final CallbackContext callbackContext) {
      List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
      FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);

      placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
        Place place = response.getPlace();
        callbackContext.success(placeToJSON(place));
      }).addOnFailureListener((exception) -> {
        callbackContext.error(exception.getMessage());
      });
    }

    private JSONObject predictionToJSON(AutocompletePrediction prediction) {
      try {
        JSONObject result = new JSONObject();
        result.put("fullText", prediction.getFullText(null));
        result.put("primaryText", prediction.getPrimaryText(null));
        result.put("secondaryText", prediction.getSecondaryText(null));
        result.put("placeId", prediction.getPlaceId());
        return result;
      } catch (JSONException e) {
        return null;
      }
    }

    private JSONObject placeToJSON(Place place) {
        try {
            JSONObject result = new JSONObject();
            result.put("placeId", place.getId());
            result.put("name", place.getName());
            result.put("formattedAddress", place.getAddress());
            result.put("latlng", new JSONArray()
                .put(place.getLatLng().latitude)
                .put(place.getLatLng().longitude)
            );
            return result;
        } catch (JSONException e) {
            return null;
        }
    }
}
