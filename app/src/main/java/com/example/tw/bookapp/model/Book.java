package com.example.tw.bookapp.model;

import org.json.JSONObject;

import static android.text.TextUtils.join;

public final class Book {
    private final String title;
    private final String information;
    private final float rating;
    private final String thumbnail;
    private final String largeImg;

    public Book(JSONObject jsonData) {
        this.title = jsonData.optString("title");
        this.information = join("/", new String[]{
                jsonData.optJSONArray("author").optString(0), jsonData.optString("publisher"), jsonData.optString("pubdate")
        });
        this.rating = (float) (jsonData.optJSONObject("rating").optDouble("average"));
        this.thumbnail = jsonData.optString("image");
        this.largeImg = jsonData.optJSONObject("images").optString("large");
    }

    public String getTitle() {
        return title;
    }

    public String getInformation() {
        return information;
    }

    public float getRating() {
        return rating;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public String getLargeImg() {
        return largeImg;
    }
}
