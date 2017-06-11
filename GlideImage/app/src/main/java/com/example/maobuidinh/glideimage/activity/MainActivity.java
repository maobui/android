package com.example.maobuidinh.glideimage.activity;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.maobuidinh.glideimage.R;
import com.example.maobuidinh.glideimage.adapter.GalleryAdapter;
import com.example.maobuidinh.glideimage.app.AppController;
import com.example.maobuidinh.glideimage.model.Image;
import com.example.maobuidinh.glideimage.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();
//    private static final String endpoint = "http://api.androidhive.info/json/glide.json";
    private static final String endpoint = "https://raw.githubusercontent.com/maobui/AndroidStudio/master/GlideImage/glide.json";
//    private static final String endpoint_dynamic = "https://api.flickr.com/services/feeds/photos_faves.gne?nsid=38041819@N04&format=json&nojsoncallback=1";
    private static final String endpoint_dynamic = "http://api.flickr.com/services/feeds/photos_public.gne?nsid=hoangchino_photographer&format=json&nojsoncallback=1";
    private ArrayList<Image> images;
    private ProgressDialog pDialog;
    private GalleryAdapter mAdapter;
    private RecyclerView recyclerView;

    private final int SPANCOUNT = 2;

    private boolean mUseFromDynamicServer = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        pDialog = new ProgressDialog(this);
        images = new ArrayList<>();
        mAdapter = new GalleryAdapter(getApplicationContext(), images);

        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getApplicationContext(), SPANCOUNT);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnItemTouchListener(new GalleryAdapter.RecyclerTouchListener(getApplicationContext(), recyclerView, new GalleryAdapter.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("images", images);
                bundle.putInt("position", position);

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                SlideshowDialogFragment newFragment = SlideshowDialogFragment.newInstance();
                newFragment.setArguments(bundle);
                newFragment.show(ft, "slideshow");
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        if (mUseFromDynamicServer)
        {
//            fetchImagesDynamic();
            fetchImagesFromFlickr();
        } else {
            fetchImages();
        }
    }

    private void fetchImages() {

        pDialog.setMessage("Downloading json...");
        pDialog.show();

        JsonArrayRequest req = new JsonArrayRequest(endpoint,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d(TAG, response.toString());
                        pDialog.hide();

                        images.clear();
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject object = response.getJSONObject(i);
                                Image image = new Image();
                                image.setName(object.getString("name"));

                                JSONObject url = object.getJSONObject("url");
                                image.setSmall(url.getString("small"));
                                image.setMedium(url.getString("medium"));
                                image.setLarge(url.getString("large"));
                                image.setTimestamp(object.getString("timestamp"));

                                images.add(image);

                            } catch (JSONException e) {
                                Log.e(TAG, "Json parsing error: " + e.getMessage());
                            }
                        }

                        mAdapter.notifyDataSetChanged();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error: " + error.getMessage());
                pDialog.hide();
            }
        });

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(req);
    }

    private void fetchImagesFromFlickr() {

        pDialog.setMessage("Downloading json...");
        pDialog.show();

        JsonObjectRequest req = new JsonObjectRequest(endpoint_dynamic,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, response.toString());
                        pDialog.hide();

                        JsonParser(response.toString());
                        mAdapter.notifyDataSetChanged();
                    }},
                new Response.ErrorListener(){
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Error: " + error.getMessage());
                            pDialog.hide();
                        }
                });

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(req);
    }

    private void fetchImagesDynamic() {
        new GetJson().execute(endpoint_dynamic);
    }


    private class GetJson extends AsyncTask<String, Void, String> {

        private  static final String  TAG = "GetJSon";
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();
            /**
             * Updating parsed JSON data into ListView
             * */
            JsonParser(result);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        protected String doInBackground(String... params) {
            // Get data from server.
            InputStream in = Utils.openConnection(endpoint_dynamic);
            // Convert to data to String.
            String json = Utils.convertStreamToString(in);
            return json;
        }
    }

    private void JsonParser (String strJson){
        if (strJson != null)
        {
            // try parse the string to a JSON object
            try {
                JSONObject jObj = new JSONObject(strJson);
                Log.d(TAG, "jObj  : " + jObj.toString());
                JSONArray items = jObj.getJSONArray("items");

                images.clear();
                for (int i = 0; i < items.length(); i ++)
                {
                    JSONObject object = items.getJSONObject(i);
                    JSONObject media = object.getJSONObject("media");

                    String name = object.getString("title");
                    String small = media.getString("m");
                    String medium = media.getString("m");
                    String large = media.getString("m");
                    large = large.replace("_m.jpg", "_b.jpg");
                    String timestamp = object.getString("date_taken");
                    Log.d(TAG, "media  : " + media.getString("m"));

                    Image image = new Image(name, small, medium, large, timestamp);
                    images.add(image);
                }

            } catch (JSONException e) {
                Log.e(TAG, "Error parsing data " + e.toString());
            }
        } else {
            Log.e(TAG, "JsonParser null !!! ");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // solve WindownLeaked at android.app.Dialog.show
        if (pDialog != null){
            pDialog.dismiss();
        }
    }
}

/*
*
* Photo Source URLs
* https://www.flickr.com/services/api/misc.urls.html
*
*
*   https://farm{farm-id}.staticflickr.com/{server-id}/{id}_{secret}.jpg
*	    or
*   https://farm{farm-id}.staticflickr.com/{server-id}/{id}_{secret}_[mstzb].jpg
*	    or
*   https://farm{farm-id}.staticflickr.com/{server-id}/{id}_{o-secret}_o.(jpg|gif|png)
* s	small square 75x75
* q	large square 150x150
* t	thumbnail, 100 on longest side
* m	small, 240 on longest side
* n	small, 320 on longest side
* -	medium, 500 on longest side
* z	medium 640, 640 on longest side
* c	medium 800, 800 on longest side†
* b	large, 1024 on longest side*
* h	large 1600, 1600 on longest side†
* k	large 2048, 2048 on longest side†
* o	original image, either a jpg, gif or png, depending on source format
*
*/