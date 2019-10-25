package by.chemerisuk.cordova.google;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import by.chemerisuk.cordova.support.CordovaMethod;
import by.chemerisuk.cordova.support.ReflectiveCordovaPlugin;

import com.google.android.libraries.places.compat.AutocompleteFilter;
import com.google.android.libraries.places.compat.AutocompletePrediction;
import com.google.android.libraries.places.compat.AutocompletePredictionBufferResponse;
import com.google.android.libraries.places.compat.GeoDataClient;
import com.google.android.libraries.places.compat.Place;
import com.google.android.libraries.places.compat.PlaceBufferResponse;
import com.google.android.libraries.places.compat.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class PlacesPlugin extends ReflectiveCordovaPlugin {
    private GeoDataClient geoDataClient;
    private Map<Integer, String> types;

    @Override
    protected void pluginInitialize() {
        this.geoDataClient = Places.getGeoDataClient(cordova.getActivity());

        this.types = new HashMap<Integer, String>();
        for (Field f : Place.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) {
                try {
                    this.types.put((Integer)f.get(null), f.getName().substring(5).toLowerCase());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @CordovaMethod
    public void getPredictions(String query, JSONObject settings, final CallbackContext callbackContext) throws JSONException {
        AutocompleteFilter.Builder filterBuilder = new AutocompleteFilter.Builder();
        if (settings.has("country")) {
            filterBuilder.setCountry(settings.getString("country"));
        }
        if (settings.has("types")) {
            filterBuilder.setTypeFilter(settings.getInt("types"));
        }

        LatLngBounds bounds = null;
        JSONObject boundsData = settings.getJSONObject("bounds");
        if (boundsData != null) {
            bounds = new LatLngBounds(
                new LatLng(boundsData.getDouble("south"), boundsData.getDouble("west")),
                new LatLng(boundsData.getDouble("north"), boundsData.getDouble("east"))
            );
        }

        this.geoDataClient.getAutocompletePredictions(query, bounds, filterBuilder.build())
            .addOnCompleteListener(cordova.getActivity(), new OnCompleteListener<AutocompletePredictionBufferResponse>() {
                @Override
                public void onComplete(Task<AutocompletePredictionBufferResponse> task) {
                    if (task.isSuccessful()) {
                        AutocompletePredictionBufferResponse predictions = task.getResult();
                        try {
                            JSONArray result = new JSONArray();
                            for (AutocompletePrediction prediction : predictions) {
                                result.put(predictionToJSON(prediction));
                            }
                            callbackContext.success(result);
                        } finally {
                            predictions.release();
                        }
                    } else {
                        callbackContext.error(task.getException().getMessage());
                    }
                }
            });
    }

    @CordovaMethod
    public void getById(String placeId, final CallbackContext callbackContext) {
        this.geoDataClient.getPlaceById(placeId)
            .addOnCompleteListener(cordova.getActivity(), new OnCompleteListener<PlaceBufferResponse>() {
                @Override
                public void onComplete(Task<PlaceBufferResponse> task) {
                    if (task.isSuccessful()) {
                        PlaceBufferResponse places = task.getResult();
                        try {
                            if (places.getCount() == 0) {
                                callbackContext.success((String)null);
                            } else {
                                callbackContext.success(placeToJSON(places.get(0)));
                            }
                        } finally {
                            places.release();
                        }
                    } else {
                        callbackContext.error(task.getException().getMessage());
                    }
                }
            });
    }

    private JSONObject predictionToJSON(AutocompletePrediction prediction) {
        try {
            JSONObject result = new JSONObject();
            result.put("fullText", prediction.getFullText(null));
            result.put("primaryText", prediction.getPrimaryText(null));
            result.put("secondaryText", prediction.getSecondaryText(null));
            result.put("placeId", prediction.getPlaceId());
            result.put("types", getTypes(prediction.getPlaceTypes()));
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
            result.put("types", getTypes(place.getPlaceTypes()));
            result.put("rating", place.getRating());
            result.put("priceLevel", place.getPriceLevel());
            result.put("phoneNumber", place.getPhoneNumber());
            result.put("website", place.getWebsiteUri());
            result.put("latlng", new JSONArray()
                .put(place.getLatLng().latitude)
                .put(place.getLatLng().longitude)
            );
            return result;
        } catch (JSONException e) {
            return null;
        }
    }

    private JSONArray getTypes(List<Integer> input) {
        List<String> result = new ArrayList<String>(input.size());
        for (Integer type : input) {
            result.add(this.types.get(type));
        }
        return new JSONArray(result);
    }
}
