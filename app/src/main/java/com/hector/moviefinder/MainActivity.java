package com.hector.moviefinder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/*
Similar Movie Finder using REST API
User wil enter a list of movies to find similar ones
In this we use TasteDive and OMDB REST API
one for retrieving similar movies and
another one for getting the Rotten Tomatoes rating for that movie
 */

public class MainActivity extends AppCompatActivity {

    // API KEYS
    String TASTDIVE_API_KEY = "383937-MovieFin-7XRETLZZ";
    String OMDB_API_KEY = "f2c0d5ef";

    // Variable for double tap check
    boolean doubleBackToExitPressedOnce = false;

    // Debug TAG
    String TAG = "Response";

    // UI Elements
    TextInputEditText searchBox;
    Button searchButton;
    ProgressBar progressBar;
    ListView listView;
    Dialog myPopup;
    CardView cardView;

    // Request queue
    RequestQueue requestQueue;
    Cache cache;
    Network network;

    // Result variable
    String [] relatedMovies;
    List<MovieDetails> movieDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Pointing the UI elements to the variables
        searchBox = findViewById(R.id.searchBox);
        searchButton = findViewById(R.id.searchButton);
        progressBar = findViewById(R.id.progressBar);
        listView = findViewById(R.id.resultList);
        cardView = findViewById(R.id.cardViewList);

        // Instance creations
        cache = new DiskBasedCache(getCacheDir(), 1024*1024); //1MB Cache
        network = new BasicNetwork(new HurlStack());
        requestQueue = new RequestQueue(cache, network);
        myPopup = new Dialog(MainActivity.this);
        movieDetails = new ArrayList<>();

        // Search button click action listener
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Search movie reading and storing
                String movieName = searchBox.getText().toString();

                // Finding Similar movies
                if(movieName.isEmpty() || movieName.length() < 3) // If movie not found
                    searchBox.setError("Movie Name Required");
                else{
                    // Movie Found clearing the current list
                    movieDetails.clear();

                    // Adding the required views and hiding the button
                    searchBox.onEditorAction(EditorInfo.IME_ACTION_DONE);
                    searchButton.setVisibility(View.INVISIBLE);
                    progressBar.setVisibility(View.VISIBLE);
                    cardView.setVisibility(View.INVISIBLE);
                    requestQueue.start();

                    // Calling the getSimilar Movies Function
                    getSimilarMovies(movieName, "7");

                }

            }
        });

        // ListView item click listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Creating UI elements for Dialog box
                ImageView posterImage;
                TextView closeButton, titleText, yearText, genreText, ratingText, plotText;

                // Getting the selected movies from list
                MovieDetails popupMovie = movieDetails.get(position);

                // Setting values to dialog box ui
                myPopup.setContentView(R.layout.movie_details_popup);
                posterImage = myPopup.findViewById(R.id.moviePoster);
                closeButton = myPopup.findViewById(R.id.closeText);
                titleText = myPopup.findViewById(R.id.movieTitle);
                yearText = myPopup.findViewById(R.id.movieYear);
                genreText = myPopup.findViewById(R.id.movieGenre);
                ratingText = myPopup.findViewById(R.id.movieRating);
                plotText = myPopup.findViewById(R.id.moviePlot);

                Glide.with(MainActivity.this)
                        .load(popupMovie.getImgURL())
                        .fitCenter()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)         //ALL or NONE as your requirement
                        .thumbnail(Glide.with(MainActivity.this).load(R.drawable.ic_baseline_image_24))
                        .error(R.drawable.ic_baseline_broken_image_24)
                        .into(posterImage);

                titleText.setText("Title : "+popupMovie.getTitle());
                yearText.setText("Year : "+popupMovie.getYear());
                ratingText.setText("IMDB Rating : "+popupMovie.getImdbRating()+"/10");
                genreText.setText("Genre : "+popupMovie.getGenre());
                plotText.setText("Plot : "+popupMovie.getPlot());

                // Setting up action Listener for dialog box close button
                closeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        myPopup.dismiss();
                    }
                });

                // Calling the dialog box
                myPopup.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                myPopup.show();

            }
        });

    }
    private void getSimilarMovies(String movie_name, String movie_count){
        // URL for tasteDive API
        String tasteDiveURL = "https://tastedive.com/api/similar?q="+movie_name+"&limit="+movie_count+"&k="+TASTDIVE_API_KEY;
        Log.d(TAG, "TasteDive :: "+tasteDiveURL);

        // Creating the request
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, tasteDiveURL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray similar= ((JSONObject)response.get("Similar")).getJSONArray("Results");
                            int lenArr = similar.length();

                            if(lenArr == 0){
                                searchButtonShowAndProgressBarHide();
                                searchBox.setError("Check Spelling");
                                Toast.makeText(MainActivity.this, "Movie Not Found", Toast.LENGTH_SHORT).show();
                            }else{
                                relatedMovies = new String[lenArr];

                                // Assigning the related movie names
                                for(int i=0; i < lenArr; i++) {
                                    relatedMovies[i] = (similar.getJSONObject(i)).getString("Name");

                                    // From here we are calling omdb to get the ratings then sort the data
                                    getTheMovieDetails(relatedMovies[i], i, lenArr);
                                }
                            }

                        } catch (JSONException e) {
                            searchButtonShowAndProgressBarHide();
                            Toast.makeText(MainActivity.this, "Check Your Connection", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        searchButtonShowAndProgressBarHide();
                        Toast.makeText(MainActivity.this, "Check Your Connection", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "TasteDive :: "+error);
                    }
                }
        );

        // Calling the request
        requestQueue.add(jsonObjectRequest);
    }
    private void getTheMovieDetails(final String movieName, final int movieCount, final int totalMovieCount){

        // Base url with parameters for OMDB API
        String omdbURL = "https://www.omdbapi.com/?apikey="+OMDB_API_KEY+"&t="+movieName+"&r=json";
        Log.d(TAG, "OMDB :: "+omdbURL);

        // Creating the request
        JsonObjectRequest omdbRequest = new JsonObjectRequest(Request.Method.GET, omdbURL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        String year, genre, imdbRating, plot, imgURL;

                        try {
                            year = response.getString("Year");
                        }catch (JSONException e) {
                            year = "";
                            e.printStackTrace();
                        }
                        try {
                            genre = response.getString("Genre");
                        } catch (JSONException e) {
                            genre = "";
                            e.printStackTrace();
                        }
                        try {
                            imdbRating = response.getString("imdbRating");
                        } catch (JSONException e) {
                            imdbRating = "0";
                            e.printStackTrace();
                        }
                        try {
                            plot = response.getString("Plot");
                        } catch (JSONException e) {
                            plot = "";
                            e.printStackTrace();
                        }
                        try {
                            imgURL = response.getString("Poster");
                        } catch (JSONException e) {
                            imgURL = "";
                            e.printStackTrace();
                        }

                        movieDetails.add(new MovieDetails(movieName, year, genre, imdbRating, plot, imgURL));

                        // Checking that all movies details are collected
                        if(movieCount == totalMovieCount-1){

                            // Sorting the movie based on IMDB rating
                            Collections.sort(movieDetails, MovieDetails.RatingComparator);


                            // String the sorted movie title to relatedMovies array for listView
                            for (int i=0; i < totalMovieCount; i++){
                                relatedMovies[i] = ((movieDetails.get(i)).getTitle());
                            }

                            // Creating array adapter for listView the related movies

                            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MainActivity.this, R.layout.list_view_coustome_layout, R.id.list_content, relatedMovies);
                            listView.setAdapter(arrayAdapter);

                            searchButtonShowAndProgressBarHide();
                            cardView.setVisibility(View.VISIBLE);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        searchButtonShowAndProgressBarHide();
                        Toast.makeText(MainActivity.this, "Check Your Connection", Toast.LENGTH_SHORT).show();

                        Log.d(TAG, "OMDB :: "+error);
                    }
                });

        // Calling the request
        requestQueue.add(omdbRequest);
    }

    // Showing the search button and hiding progress bar
    public void searchButtonShowAndProgressBarHide(){
        searchButton.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
    }

    // Overriding onBackPressed for double tap check
    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Press Back Again", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }
}