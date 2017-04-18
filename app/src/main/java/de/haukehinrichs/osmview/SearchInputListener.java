package de.haukehinrichs.osmview;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by hauke on 09/03/17.
 */

public class SearchInputListener implements SearchView.OnSuggestionListener, SearchView.OnQueryTextListener {

    private String mApiKey;
    private LatLng mLocation;
    private ArrayAdapter<String> mAdapter;
    private ArrayList<String> mSuggestions;
    private JSONArray mResultList;
    private MainActivity mContext;

    SearchInputListener(String apiKey, ListView suggestionView, MainActivity context) {
        mApiKey = apiKey;
        mSuggestions = new ArrayList<>();
        mAdapter = new ArrayAdapter<String>(suggestionView.getContext(), R.layout.suggestion_list_item, R.id.suggestion_item, mSuggestions);
        suggestionView.setAdapter(mAdapter);
        suggestionView.setOnItemClickListener(new SearchInputClickListener());
        mContext = context;
    }

    public void updateLocation(LatLng location) {
        mLocation = location;
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        return false;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        String urlString = "https://maps.googleapis.com/maps/api/place/autocomplete/json?key=" + mApiKey
                + "&input=" + URLEncoder.encode(newText);
        if (mLocation != null) {
            urlString +=  "&radius=50&location=" + Double.toString(mLocation.latitude) + "," + Double.toString(mLocation.longitude);
        }
        Log.d("places", "searching for " + newText);
        new SearchSuggestionsTask().execute(new String[] {urlString});
        return true;
    }

    private void handleSuggestionsResult(JSONObject result) throws JSONException {
        mResultList = result.getJSONArray("predictions");
        mSuggestions.clear();
        for (int i = 0; i < mResultList.length(); i++) {
            JSONObject singleResult = mResultList.getJSONObject(i);
            String description = singleResult.getString("description");
            Log.d("places", "result: " + description);
            mSuggestions.add(description);
        }
        mAdapter.notifyDataSetChanged();
    }

    class SearchSuggestionsTask extends AsyncTask<String, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... params) {

        try {
            URL url = new URL(params[0]);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            Log.d("places", "opening url " + params[0]);
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                JSONObject result = new JSONObject(stringBuilder.toString());
                Log.d("json url result", "status " + result.getString("status"));
                return result;
            } finally {
                urlConnection.disconnect();
            }
        } catch(Exception e) {
            Log.e("ERROR", e.getMessage(), e);
            return null;
        }
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            try {
                handleSuggestionsResult(jsonObject);
            } catch(Exception e) {
                Log.e("ERROR", e.getMessage(), e);
            }
        }
    }

    class SearchResultTask extends SearchSuggestionsTask {
        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            try {
                handlePlaceResult(jsonObject);
            } catch(Exception e) {
                Log.e("ERROR", e.getMessage(), e);
            }
        }
    }

    private void handlePlaceResult(JSONObject result) throws JSONException {
        JSONObject location = result.getJSONObject("result").getJSONObject("geometry").getJSONObject("location");
        double lat = location.getDouble("lat");
        double lng = location.getDouble("lng");
        LatLng latLng = new LatLng(lat, lng);
        mContext.addMarker(latLng, result.getJSONObject("result").getString("name"));
        mSuggestions.clear();
        mAdapter.notifyDataSetChanged();
    }


    class SearchInputClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            try {
                JSONObject place = mResultList.getJSONObject(position);
                String placeId = place.getString("place_id");
                String urlString = "https://maps.googleapis.com/maps/api/place/details/json?key=" + mApiKey
                        + "&placeid=" + placeId;
                Log.d("places", "looking up " + placeId);
                new SearchResultTask().execute(new String[] {urlString});
            } catch (JSONException e) {
                Log.e("ERROR", e.getMessage(), e);
            }

        }
    }
}
