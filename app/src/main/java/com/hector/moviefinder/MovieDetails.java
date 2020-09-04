package com.hector.moviefinder;

import androidx.annotation.NonNull;

import java.util.Comparator;

public class MovieDetails{
    String title, year, genre, imdbRating, plot, imgURL;

    public MovieDetails(String title, String year, String genre, String imdbRating, String plot, String imgURL) {
        this.title = title;
        this.year = year;
        this.genre = genre;
        this.imdbRating = imdbRating;
        this.plot = plot;
        this.imgURL = imgURL;
    }

    public String getTitle() {
        return title;
    }

    public String getYear() {
        return year;
    }

    public String getGenre() {
        return genre;
    }

    public String getImdbRating() {
        return imdbRating;
    }

    public String getPlot() {
        return plot;
    }

    public String getImgURL() {
        return imgURL;
    }

    public static Comparator<MovieDetails> RatingComparator = new Comparator<MovieDetails>() {
        @Override
        public int compare(MovieDetails o1, MovieDetails o2) {
            float r1, r2;
            try {
                r1 = Float.parseFloat(o1.getImdbRating());
            }catch (NumberFormatException e){
                r1 = 0;
            }
            try {
                r2 = Float.parseFloat(o2.getImdbRating());
            }catch (NumberFormatException e){
                r2 = 0;
            }

            return Float.compare(r2, r1 );
        }
    };


    @NonNull
    @Override
    public String toString() {
        return "[ Title : "+title+" Year : "+year+" Genre : "+genre+" IMDB Rating : "+imdbRating+" Image URL : "+imgURL+" Plot : "+plot+" ]";
    }
}
