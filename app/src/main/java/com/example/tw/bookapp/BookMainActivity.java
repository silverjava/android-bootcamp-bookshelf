package com.example.tw.bookapp;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.example.tw.bookapp.model.Book;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static android.text.TextUtils.join;
import static android.util.Log.e;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class BookMainActivity extends Activity {
    private static final String TAG = BookMainActivity.class.getSimpleName();
    private static final String BOOK_QUERY_URL = "https://api.douban.com/v2/book/search?tag=%s&start=%d&count=%d";
    private static PlaceholderFragment.BookListViewAdapter bookListViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.book_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class PlaceholderFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
        public static final String FIRST_VISIBLE_POSITION = "firstVisiblePosition";
        public static final String LIST_DATA = "data";
        private View loadingMore;
        private AbsListView bookListView;
        private SwipeRefreshLayout swipeRefreshLayout;

        private boolean isLoadingMore;

        public static final int DATA_PER_PAGE = 10;
        public static final int INITIAL_START = 20;
        private BookDataLoader bookDataLoader;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_book_main, container, false);
            bookListView = (AbsListView) view.findViewById(R.id.book_list);

            loadingMore = view.findViewById(R.id.view_loading_more);
            loadingMore.setVisibility(GONE);

            swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);
            swipeRefreshLayout.setColorSchemeResources(
                    android.R.color.holo_blue_light,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light
            );
            swipeRefreshLayout.setOnRefreshListener(this);

            bookListViewAdapter = new BookListViewAdapter(getActivity());
            bookListView.setAdapter(bookListViewAdapter);
            loadBookData(INITIAL_START);
            bookListView.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {
                }

                @Override
                public void onScroll(AbsListView absListView, int i, int i2, int i3) {
                    if (i3 > 0) {
                        int lastVisibleItem = i + i2;
                        if (!isLoadingMore && lastVisibleItem == i3) {
                            loadBookData(bookListViewAdapter.getCount());
                        }
                    }
                }
            });
            return view;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt(FIRST_VISIBLE_POSITION, bookListView.getFirstVisiblePosition());
        }

        @Override
        public void onResume() {
            super.onResume();

        }

        @Override
        public void onPause() {
            super.onPause();
            synchronized (this) {
                if (bookDataLoader != null) {
                    bookDataLoader.cancel(true);
                }
            }
        }

        @Override
        public void onRefresh() {
            bookListViewAdapter.clear();
            loadBookData(INITIAL_START);
        }

        private synchronized void loadBookData(final int start) {
            bookDataLoader = new BookDataLoader(swipeRefreshLayout);
            bookDataLoader.execute(String.format(BOOK_QUERY_URL, "Python", start, DATA_PER_PAGE));
        }

        private class BookListViewAdapter extends ArrayAdapter<Book> {
            private final Context context;
            private LayoutInflater layoutInflater;

            BookListViewAdapter(Context context) {
                super(context, 0);
                this.context = context;
                this.layoutInflater = LayoutInflater.from(context);
            }

            @Override
            public View getView(int i, View convertView, ViewGroup parent) {
                final ViewHolder viewHolder;
                if (convertView == null) {
                    convertView = layoutInflater.inflate(R.layout.list_item_book, parent, false);
                    viewHolder = new ViewHolder(
                            (ImageView) convertView.findViewById(R.id.thumbnail),
                            (TextView) convertView.findViewById(R.id.name),
                            (TextView) convertView.findViewById(R.id.information),
                            (RatingBar) convertView.findViewById(R.id.rating)
                    );
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }

                final Book book = getItem(i);
                viewHolder.bookNameView.setText(book.getTitle());
                viewHolder.ratingBar.setRating(book.getRating());
                viewHolder.bookInfoView.setText(book.getInformation());
                Picasso.with(context).load(book.getLargeImg()).into(viewHolder.thumbnailView);
                return convertView;
            }

            class ViewHolder {
                ImageView thumbnailView;
                TextView bookNameView;
                TextView bookInfoView;
                RatingBar ratingBar;

                public ViewHolder(ImageView thumbnailView, TextView bookNameView, TextView bookInfoView, RatingBar ratingBar) {
                    this.thumbnailView = thumbnailView;
                    this.bookNameView = bookNameView;
                    this.bookInfoView = bookInfoView;
                    this.ratingBar = ratingBar;
                }
            }
        }

        private class BookDataLoader extends AsyncTask<String, Void, List<Book>> {
            private final SwipeRefreshLayout swipeRefreshLayout;

            public BookDataLoader(SwipeRefreshLayout swipeRefreshLayout) {
                this.swipeRefreshLayout = swipeRefreshLayout;
            }

            @Override
            protected List<Book> doInBackground(String... strings) {
                return loadJsonData(strings[0]);
            }

            private List<Book> loadJsonData(final String url) {
                try {
                    URL httpUrl = new URL(url);
                    BufferedReader bufferedReader = null;
                    try {
                        HttpURLConnection urlConnection = (HttpURLConnection) httpUrl.openConnection();
                        bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        ArrayList<String> data = new ArrayList<String>();
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            data.add(line);
                        }
                        return toBookList(new JSONObject(join("", data.toArray(new String[data.size()]))));
                    } catch (IOException e) {
                        e(TAG, e.getMessage());
                    } catch (JSONException e) {
                        e(TAG, e.getMessage());
                    } finally {
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                            } catch (IOException e) {
                                e(TAG, e.getMessage());
                            }
                        }
                    }
                } catch (MalformedURLException e) {
                    e(TAG, e.getMessage());
                }
                return new ArrayList<Book>();
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                swipeRefreshLayout.setRefreshing(true);
                isLoadingMore = true;
                showLoadingMore();
            }

            @Override
            protected void onPostExecute(List<Book> books) {
                bookListViewAdapter.addAll(books);
                swipeRefreshLayout.setRefreshing(false);
                hideLoadingMore();
                isLoadingMore = false;
            }
        }

        private void hideLoadingMore() {
            loadingMore.setVisibility(GONE);
        }

        private void showLoadingMore() {
            loadingMore.setVisibility(VISIBLE);
        }
    }

    private static List<Book> toBookList(JSONObject bookData) {
        ArrayList<Book> result = new ArrayList<Book>();
        JSONArray jsonArray = bookData.optJSONArray("books");
        if (jsonArray == null) {
            return result;
        }

        for (int i = 0; i < jsonArray.length(); i++) {
            result.add(new Book(jsonArray.optJSONObject(i)));
        }

        return result;
    }
}
