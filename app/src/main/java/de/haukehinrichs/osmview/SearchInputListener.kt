package de.haukehinrichs.osmview

import android.os.AsyncTask
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.SearchView

import com.google.android.gms.maps.model.LatLng

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.ArrayList

/**
 * Created by hauke on 09/03/17.
 */

class SearchInputListener internal constructor(private val mApiKey: String, suggestionView: ListView, private val mContext: MainActivity) : SearchView.OnSuggestionListener, SearchView.OnQueryTextListener {
    private var mLocation: LatLng? = null
    private val mAdapter: ArrayAdapter<String>
    private val mSuggestions: ArrayList<String>
    private var mResultList: JSONArray? = null

    init {
        mSuggestions = ArrayList<String>()
        mAdapter = ArrayAdapter(suggestionView.context, R.layout.suggestion_list_item, R.id.suggestion_item, mSuggestions)
        suggestionView.adapter = mAdapter
        suggestionView.onItemClickListener = SearchInputClickListener()
    }

    fun updateLocation(location: LatLng) {
        mLocation = location
    }

    override fun onSuggestionSelect(position: Int): Boolean {
        return false
    }

    override fun onSuggestionClick(position: Int): Boolean {
        return false
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        var urlString = "https://maps.googleapis.com/maps/api/place/autocomplete/json?key=" + mApiKey + "&input=" + URLEncoder.encode(newText)
        if (mLocation != null) {
            urlString += "&radius=50&location=" + java.lang.Double.toString(mLocation!!.latitude) + "," + java.lang.Double.toString(mLocation!!.longitude)
        }
        Log.d("places", "searching for " + newText)
        SearchSuggestionsTask().execute(*arrayOf(urlString))
        return true
    }

    @Throws(JSONException::class)
    private fun handleSuggestionsResult(result: JSONObject) {
        mResultList = result.getJSONArray("predictions")
        mSuggestions.clear()
        for (i in 0..mResultList!!.length() - 1) {
            val singleResult = mResultList!!.getJSONObject(i)
            val description = singleResult.getString("description")
            Log.d("places", "result: " + description)
            mSuggestions.add(description)
        }
        mAdapter.notifyDataSetChanged()
    }

    internal open inner class SearchSuggestionsTask : AsyncTask<String, Void, JSONObject>() {

        override fun doInBackground(vararg params: String): JSONObject? {

            try {
                val url = URL(params[0])
                val urlConnection = url.openConnection() as HttpURLConnection
                Log.d("places", "opening url " + params[0])
                try {
                    val reader = urlConnection.inputStream.buffered().reader()
                    val result = JSONObject(reader.readText())
                    reader.close()
                    Log.d("json url result", "status " + result.getString("status"))
                    return result
                } finally {
                    urlConnection.disconnect()
                }
            } catch (e: Exception) {
                Log.e("ERROR", e.message, e)
                return null
            }

        }

        override fun onPostExecute(jsonObject: JSONObject) {
            try {
                handleSuggestionsResult(jsonObject)
            } catch (e: Exception) {
                Log.e("ERROR", e.message, e)
            }

        }
    }

    internal inner class SearchResultTask : SearchSuggestionsTask() {
        override fun onPostExecute(jsonObject: JSONObject) {
            try {
                handlePlaceResult(jsonObject)
            } catch (e: Exception) {
                Log.e("ERROR", e.message, e)
            }

        }
    }

    @Throws(JSONException::class)
    private fun handlePlaceResult(result: JSONObject) {
        val location = result.getJSONObject("result").getJSONObject("geometry").getJSONObject("location")
        val lat = location.getDouble("lat")
        val lng = location.getDouble("lng")
        val latLng = LatLng(lat, lng)
        mContext.addMarker(latLng, result.getJSONObject("result").getString("name"))
        mSuggestions.clear()
        mAdapter.notifyDataSetChanged()
    }


    internal inner class SearchInputClickListener : AdapterView.OnItemClickListener {

        override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            try {
                val place = mResultList!!.getJSONObject(position)
                val placeId = place.getString("place_id")
                val urlString = "https://maps.googleapis.com/maps/api/place/details/json?key=" + mApiKey + "&placeid=" + placeId
                Log.d("places", "looking up " + placeId)
                SearchResultTask().execute(*arrayOf(urlString))
            } catch (e: JSONException) {
                Log.e("ERROR", e.message, e)
            }

        }
    }
}
