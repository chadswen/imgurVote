package com.chadswen.imgurvote;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.lorentzos.flingswipe.SwipeFlingAdapterView;

import java.util.ArrayList;
import java.util.List;

public class ImgurVoteActivity extends Activity {
    public static final String BASE_API_URL = "https://api.imgur.com/3/";
    public static final String VIRAL_GALLERY_URL = BASE_API_URL + "gallery/hot/viral/";

    public static final String CLIENT_ID = "64485b5edee17b0";
    public static final String CLIENT_SECRET = "deb5c0add5545250b2a743cfde3ffb60e82c4871";
    public static final String REFRESH_TOKEN = "d75438afa8afc6b4631507e5df897c4bc9cde8e4";
    public static final String GRANT_TYPE = "refresh_token";
    public static final String TOKEN_URL = "https://api.imgur.com/oauth2/token";

    private static final String IMAGES_PER_PAGE = "25";

    private static final String PAGE_NUMBER = "PAGE_NUMBER";
    private static final String PAGE_JSON = "PAGE_JSON";

    private SwipeFlingAdapterView mGalleryImageContainer;
    private ArrayAdapter<JsonObject> mGalleryImageAdapter;

    private List<JsonObject> mGalleryImageJsonObjectList = new ArrayList<>();
    private Future<JsonObject> mLoading;
    private int mGalleryPage;
    private String mAccessToken = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imgur_vote);

        // Setup Views, Adapters, and Listeners
        mGalleryImageContainer = (SwipeFlingAdapterView) findViewById(R.id.frame);

        mGalleryImageAdapter = getGalleryImageAdapter();
        mGalleryImageContainer.setAdapter(mGalleryImageAdapter);
        mGalleryImageContainer.setMaxVisible(1);
        mGalleryImageContainer.setMinStackInAdapter(5);
        mGalleryImageContainer.setFlingListener(getOnFlingListener());

        if (savedInstanceState != null) {
            // Deserialize from saved state
            mGalleryPage = savedInstanceState.getInt(PAGE_NUMBER);

            Gson gson = new Gson();
            String jsonString = savedInstanceState.getString(PAGE_JSON);
            JsonArray galleryImageArray = gson.fromJson(jsonString, JsonArray.class);

            for (int i = 0; i < galleryImageArray.size(); i++) {
                mGalleryImageAdapter.addAll(galleryImageArray.get(i).getAsJsonObject());
            }

            mGalleryImageAdapter.notifyDataSetChanged();
        }

        // Refresh Credentials and Load next page of Gallery Images
        getCredentialsAndLoadPage();
    }

    private SwipeFlingAdapterView.onFlingListener getOnFlingListener() {
        return new SwipeFlingAdapterView.onFlingListener() {
            @Override
            public void removeFirstObjectInAdapter() {
                Log.d("ImgurVoteActivity", "Remove first Object");
                mGalleryImageJsonObjectList.remove(0);
                mGalleryImageAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLeftCardExit(Object jsonObject) {
                sendVote(((JsonObject)(jsonObject)).get("id").getAsString(), false);
                Log.d("ImgurVoteActivity", "Swipe left");
            }

            @Override
            public void onRightCardExit(Object jsonObject) {
                sendVote(((JsonObject)(jsonObject)).get("id").getAsString(), true);
                Log.d("ImgurVoteActivity", "Swipe right");
            }

            @Override
            public void onAdapterAboutToEmpty(int itemsInAdapter) {
                Log.d("ImgurVoteActivity", Integer.toString(itemsInAdapter));
            }

            @Override
            public void onScroll(float scrollProgressPercent) {
                View view = mGalleryImageContainer.getSelectedView();
                if (view != null) {
                    view.findViewById(R.id.item_swipe_left_indicator)
                            .setAlpha(scrollProgressPercent < 0 ? -scrollProgressPercent : 0);
                    view.findViewById(R.id.item_swipe_right_indicator)
                            .setAlpha(scrollProgressPercent > 0 ? scrollProgressPercent : 0);
                }
            }
        };
    }

    private ArrayAdapter<JsonObject> getGalleryImageAdapter() {
        return new ArrayAdapter<JsonObject>(this, 0, mGalleryImageJsonObjectList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.item, parent, false);
                }

                // Load next page of images when we're 2 away from the end
                if (position >= getCount() - 2) {
                    getCredentialsAndLoadPage();
                }

                // Get the Gallery image at this position
                JsonObject galleryImage = getItem(position);

                // TODO handle Gallery Albums

                // Get the displayed text fields
                JsonElement titleElement = galleryImage.get("title");
                String title = titleElement.isJsonNull() ? "" : titleElement.getAsString();
                JsonElement descriptionElement = galleryImage.get("description");
                String description = descriptionElement.isJsonNull() ? "" : descriptionElement.getAsString();

                // Set the TextViews
                TextView titleView = (TextView) convertView.findViewById(R.id.title);
                titleView.setText(title);

                TextView descriptionView = (TextView) convertView.findViewById(R.id.description);
                descriptionView.setText(description);
                descriptionView.setMovementMethod(new ScrollingMovementMethod());

                // Get the image URL
                String imageUrl = galleryImage.get("link").getAsString();
                // TODO handle animated images

                // Set the ImageView
                ImageView imageView = (ImageView) convertView.findViewById(R.id.image);
                Ion.with(ImgurVoteActivity.this)
                        .load(imageUrl)
                        .setHeader("Authorization", "Bearer " + mAccessToken)
                        .withBitmap()
                        .placeholder(R.drawable.imgur_vote_logo_dark)
                        .crossfade(true)
                        .intoImageView(imageView);
                return convertView;
            }
        };
    }

    private void sendVote(String imgurId, final boolean liked) {
        Ion.with(this)
                .load("POST","https://api.imgur.com/3/gallery/" + imgurId + "/vote/" + (liked ? "up" : "down"))
                .setHeader("Authorization", "Bearer " + mAccessToken)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject returnObject) {
                            if (e != null || !returnObject.get("success").getAsBoolean()) {
                            Toast.makeText(ImgurVoteActivity.this, "Error sending vote",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        Toast.makeText(ImgurVoteActivity.this, liked ? "Liked!" : "Meh.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getCredentialsAndLoadPage() {
        Ion.with(this)
                .load(TOKEN_URL)
                .setBodyParameter("client_id", CLIENT_ID)
                .setBodyParameter("client_secret", CLIENT_SECRET)
                .setBodyParameter("refresh_token", REFRESH_TOKEN)
                .setBodyParameter("grant_type", GRANT_TYPE)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject returnObject) {
                        if (e != null) {
                            Toast.makeText(ImgurVoteActivity.this, "Error refreshing token",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        mAccessToken = returnObject.get("access_token").getAsString();
                        loadGalleryPage();
                    }
                });

    }

    private void loadGalleryPage() {
        // Don't attempt to load more if a load is already in progress
        if (mLoading != null && !mLoading.isDone() && !mLoading.isCancelled())
            return;

        // Load the next Gallery Page
        if (mGalleryImageAdapter.getCount() > 0) {
            mGalleryPage++;
        }

        mLoading = Ion.with(this)
                .load(VIRAL_GALLERY_URL + mGalleryPage)
                .addQuery("perPage", IMAGES_PER_PAGE)
                .setHeader("Authorization", "Bearer " + mAccessToken)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject galleryObject) {
                        // Ion automatically executes this callback on the UI thread, awesome! :)

                        if (e != null) {
                            Toast.makeText(ImgurVoteActivity.this, "Error loading Images",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Add the Next page of Gallery Images
                        JsonArray galleryImageArray = galleryObject.getAsJsonArray("data");
                        for (int i = 0; i < galleryImageArray.size(); i++) {
                            mGalleryImageJsonObjectList.add(galleryImageArray.get(i).getAsJsonObject());
                        }
                        mGalleryImageAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(PAGE_NUMBER, mGalleryPage);

        // Serialize to JsonArray
        JsonArray galleryImageArray = new JsonArray();
        for (JsonObject jsonObject : mGalleryImageJsonObjectList) {
            galleryImageArray.add(jsonObject);
        }

        outState.putString(PAGE_JSON, galleryImageArray.toString());
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_imgur_vote, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}