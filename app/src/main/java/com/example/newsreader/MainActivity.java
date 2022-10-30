package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private RecyclerView recyclerView;
    private Spinner feedSpinner;
    private Button btnAddFeed, btnDeleteFeed;

    private ArrayList<NewsItem> news;
    private ArrayList<String> urls;
    private ArrayList<String> sources;

    private NewsAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        feedSpinner = findViewById(R.id.spinnerSelectFeed);
        btnAddFeed = findViewById(R.id.btnAddNewFeed);
        btnDeleteFeed = findViewById(R.id.btnDeleteFeed);

        news = new ArrayList<>();
        adapter = new NewsAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        initUrlsSources();

        feedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(MainActivity.this,  feedSpinner.getSelectedItem() + " selected", Toast.LENGTH_SHORT).show();
                news = new ArrayList<>();
                new GetNews().execute();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Toast.makeText(MainActivity.this, "Nothing selected", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddFeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: write code to add news feed
            }
        });

        btnDeleteFeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: write code to delete news feed
            }
        });

        Log.d(TAG, "onCreate: executing");
    }

    private void initUrlsSources() {
        urls = new ArrayList<>();
        sources = new ArrayList<>();
        urls.add("https://feeds.bbci.co.uk/news/world/europe/rss.xml#");
        sources.add("BBC Europe");
        urls.add("https://feeds.bbci.co.uk/news/england/rss.xml#");
        sources.add("BBC England");
        urls.add("http://rss.cnn.com/rss/cnn_topstories.rss");
        sources.add("CNN");
        urls.add("https://www.psychologytoday.com/intl/front/feed");
        sources.add("Psychology Today");
        urls.add("https://www.stern.de/feed/standard/kultur/");
        sources.add("Stern Kultur");
    }

    private class GetNews extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "doInBackground: running for link number " + urls.get(feedSpinner.getSelectedItemPosition()));
            InputStream inputStream = getInputStream(urls.get(feedSpinner.getSelectedItemPosition()));

            if(inputStream != null) {
                try {
                    initXMLPullParser(inputStream, sources.get(urls.indexOf(urls.get(feedSpinner.getSelectedItemPosition()))));
                } catch (XmlPullParserException | IOException e) {
                    Log.d(TAG, "doInBackground: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            orderUrlsByTime();
            return null;
        }

        private void orderUrlsByTime() {
            //TODO: write method
        }

        @Override
        protected void onPostExecute(Void unused) {
            Log.d(TAG, "onPostExecute: running");
            super.onPostExecute(unused);
            adapter.setNews(news);
        }

        private InputStream getInputStream(String urlLink) {
            try {
                URL url = new URL(urlLink);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                return connection.getInputStream();
            } catch (IOException e) {
                Log.d(TAG, "getInputStream: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }

        private void initXMLPullParser(InputStream inputStream, String source) throws XmlPullParserException, IOException {
            Log.d(TAG, "initXMLPullParser: Initializing XML Pull Parser");
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, null);
            parser.next();

            parser.require(XmlPullParser.START_TAG, null, "rss");

            while(parser.next() != XmlPullParser.END_TAG) {
                if(parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }

                parser.require(XmlPullParser.START_TAG, null, "channel");
                while(parser.next() != XmlPullParser.END_TAG) {
                    if(parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }
                    if(parser.getName().equals("item")) {
                        parser.require(XmlPullParser.START_TAG, null, "item");

                        String title = "";
                        String description = "";
                        String link = "";
                        String date = "";

                        while(parser.next() != XmlPullParser.END_TAG) {
                            if(parser.getEventType() != XmlPullParser.START_TAG) {
                                continue;
                            }

                            String tagName = parser.getName();
                            switch (tagName) {
                                case "title":
                                    title = getContent(parser, tagName);
                                    break;
                                case "description":
                                    description = getContent(parser, tagName);
                                    break;
                                case "link":
                                    link = getContent(parser, tagName);
                                    break;
                                case "pubDate":
                                    //Log.d(TAG, "initXMLPullParser: attempting to get pubdate");
                                    date = getContent(parser, tagName);
                                    break;
                                default:
                                    //Log.d(TAG, "initXMLPullParser: skipping tag " + tagName);
                                    skipTag(parser);
                                    break;
                            }
                        }

                        NewsItem item = new NewsItem(title, description, link, date, source);
                        //Log.d(TAG, "initXMLPullParser: " + date);
                        news.add(item);
                    } else {
                        skipTag(parser);
                    }
                }
            }
        }

        private String getContent(XmlPullParser parser, String tagName) throws XmlPullParserException, IOException {
            String content = "";
            parser.require(XmlPullParser.START_TAG, null, tagName);

            if(parser.next() == XmlPullParser.TEXT) {
                content = parser.getText();
                parser.next();
            }
            //Log.d(TAG, "getContent: Here's some content: " + content);
            return content;
        }

        private void skipTag(XmlPullParser parser) throws XmlPullParserException, IOException {
            if(parser.getEventType() != XmlPullParser.START_TAG) {
                throw new IllegalStateException();
            }

            int number = 1;

            while(number != 0) {
                switch (parser.next()) {
                    case XmlPullParser.START_TAG:
                        number++;
                        break;
                    case XmlPullParser.END_TAG:
                        number--;
                        break;
                    default:
                        break;
                }
            }
        }
    }
}