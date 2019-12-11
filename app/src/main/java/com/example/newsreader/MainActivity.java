package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> content = new ArrayList<>();
    SQLiteDatabase articleDB;
    ArrayAdapter arrayAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView listView = findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);

        // if one news title is clicked a new activity with webview is called passing the downloaded html content with it
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(),ArticleActivity.class);// creating a new intent to ArticleActivity class
                intent.putExtra("content",content.get(position));// passing the downloaded html content to ArticleActivity class
                startActivity(intent);
            }
        });


        //creating a database
        articleDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, content VARCHAR)");// creating a table in database with columns sr no.,article id,title,content

        // calling update list view fn so that if db already exists then load that data to list view
        updateListView();

        //calling the async task to download the web content
        DownloadTask task = new DownloadTask();
        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();// sending the link to get latest article ids
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public class DownloadTask extends AsyncTask<String,Void,String>{


        // downloading the article ids from link passed from oncreate using buffer reader
        @Override
        protected String doInBackground(String... strings) {
            try {
                URL url = new URL(strings[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = connection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder builder = new StringBuilder();
                for (String line;(line = bufferedReader.readLine()) != null;){
                    builder.append(line).append("\n");
                }



                //JSON array of article ids is decoded and content of each id is downloaded


                JSONArray jsonArray = new JSONArray(builder.toString());// creating a json array from the article id data
                int jsonArrayLength = 20;// we need only a maximum of 20 articles

                //if there are only less than 20 article ids got then length is made to that much
                if(jsonArray.length() < 20){
                    jsonArrayLength = jsonArray.length();
                }

                //before downloading and adding data to db the database is cleared
                articleDB.execSQL("DELETE FROM articles");


                for(int i = 0; i < jsonArrayLength; i++){// loop to rotate through 20 article ids
                    String newsId = jsonArray.getString(i);//storing each article id from json to a string

                    // url for a particular news is created using the article id
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + newsId + ".json?print=pretty");

                    //downloading the article details from url created using buffer reader. it gives a json data containing article url and article title and more details
                    connection =(HttpURLConnection)url.openConnection();
                    inputStream = connection.getInputStream();
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    builder = new StringBuilder();
                    for (String line;(line = bufferedReader.readLine()) != null;){
                        builder.append(line).append("\n");
                    }
                    Log.i("Article", "doInBackground: " + builder.toString());


                    //creating a JSON object for the article data we got
                    JSONObject jsonObject = new JSONObject(builder.toString());

                    //checking if there are any article data without a article link or article name
                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        //getting the article name and article link from JSON
                        String articleTitle = jsonObject.getString("title");
                        String articleLink = jsonObject.getString("url");
                        Log.i("info", "articleInfoDecode: " + articleTitle);
                        Log.i("info", "articleInfoDecode: " + articleLink);

                        url = new URL(articleLink);//url with that article link is created

                        // downloading the whole html content of the article
                        connection =(HttpURLConnection)url.openConnection();
                        inputStream = connection.getInputStream();
                        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                        builder = new StringBuilder();
                        for (String line;(line = bufferedReader.readLine()) != null;){
                            builder.append(line).append("\n");
                        }
                        Log.i("Content", "doInBackground: " + builder.toString());

                        // adding the whole data to sql data base
                        String sql = "INSERT INTO articles (articleId, title, content) VALUES (? , ? , ?)";
                        SQLiteStatement statement = articleDB.compileStatement(sql);
                        statement.bindString(1,newsId);
                        statement.bindString(2,articleTitle);
                        statement.bindString(3,builder.toString());//adding the downloaded html content
                        statement.execute();
                    }
                }



            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            updateListView();

            return null;
        }
    }
    // to update the list view
    public void  updateListView(){
        Log.i("note", "updateListView: ");
        Cursor c = articleDB.rawQuery("SELECT * FROM articles",null);// starting a  query
        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");
        if(c.moveToFirst()){
          titles.clear();
          content.clear();
          do {
              titles.add(c.getString(titleIndex));// adding news title to titles array list
              content.add(c.getString(contentIndex));// adding downloaded html content to content array list
          }while (c.moveToNext());
          arrayAdapter.notifyDataSetChanged();// to make the list view update the changes in the array list
        }
    }

}
