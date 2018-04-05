package com.example.snehabelkhale.cpptest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by snehabelkhale on 1/6/18.
 */

public class SoundListActivity extends AppCompatActivity {

    //public
    public static ArrayList<String> listItems=new ArrayList<String>();
    public static ArrayAdapter<String> adapter;

    //private
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private ListView soundListView;
    private float x1, x2, downX, downY, upX, upY;
    private int lastSelected = -1;
    private int MIN_DISTANCE = 150;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_list);
        setTitle("Decoded Messages");
        adapter=new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent){
                // Get the Item from ListView
                View view = super.getView(position, convertView, parent);
                // Initialize a TextView for ListView each Item
                TextView tv = (TextView) view.findViewById(android.R.id.text1);
                // Set the text color of TextView (ListView Item)
                tv.setTextColor(getResources().getColor(R.color.colorAccent));
                // Generate ListView Item using TextView
                return view;
            }
        };

        //set up the sound list view
        soundListView = findViewById(R.id.soundList);
        soundListView.setAdapter(adapter);
        soundListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View arg1, int position, long id) {
                if (lastSelected != -1) {
                    parent.getChildAt(lastSelected).setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                    TextView tv = parent.getChildAt(lastSelected).findViewById(android.R.id.text1);
                    tv.setTextColor(getResources().getColor(R.color.colorAccent));
                }
                arg1.setBackgroundColor(Color.parseColor("#00cfb7"));
                TextView tx = arg1.findViewById(android.R.id.text1);
                tx.setTextColor(getResources().getColor(R.color.colorPrimary));

                playSound(listItems.get(position));
                lastSelected = position;
            }
        });
        //set on touch listener to handle swipe back to mainActivity
        soundListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction())
                {
                    case MotionEvent.ACTION_DOWN: {
                        downX = event.getX();
                        downY = event.getY();
                        return false; // allow other events like Click to be processed
                    }
                    case MotionEvent.ACTION_MOVE: {
                        upX = event.getX();
                        upY = event.getY();
                        float deltaX = downX - upX;
                        float deltaY = downY - upY;

                        // horizontal swipe detection
                        if (Math.abs(deltaX) > MIN_DISTANCE) {
                            // left or right
                            if (deltaX < 0) {
                                mediaPlayer.stop();
                                mediaPlayer.reset();
                                switchScreens();
                            }
                            if (deltaX > 0) {
                                return true;
                            }
                        } else
                            // vertical swipe detection
                            if (Math.abs(deltaY) > MIN_DISTANCE) {
                                // top or down
                                if (deltaY < 0) {
                                    return false;
                                }
                                if (deltaY > 0) {
                                    return false;
                                }
                            }
                        return true;
                    }
                }
                return false;
            }
        });


        //Add all files in directory to the list view
        File path = this.getFilesDir();
        File[] files = path.listFiles();

        //sort files by date last modified
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                long t1 = f1.lastModified();
                long t2 = f2.lastModified();
                if (t1 < t2) return 1;
                else return -1;
            }
        });
        //add files to listView
        for (int i = 0; i < files.length; i++) {
            if(!listItems.contains(files[i].getName())){
                listItems.add(files[i].getName());
            }
        }
        adapter.notifyDataSetChanged();

        //if there are no recordings, show "no Messages" text view
        if (listItems.size() == 0) {
            TextView textView = (TextView) findViewById(R.id.noMessages);
            textView.setVisibility(View.VISIBLE);
        }

        //Check if there was a sound name sent with the intent, from the main activity
        Intent intent = getIntent();
        if (intent.getExtras() == null) {
        } else {
            //play the corresponding sound
            Bundle bundle = intent.getExtras();
            playSound(bundle.getString("soundName"));
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //and highlight the corresponding sound name in the list view
                    lastSelected = 0;
                    View arg1 = soundListView.getChildAt(0);
                    arg1.setBackgroundColor(Color.parseColor("#49d9ff"));
                }
            }, 1000);
        }

        //set up the media player
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                mp.stop();
                mp.reset();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                float deltaX = x2 - x1;

                if (Math.abs(deltaX) > MIN_DISTANCE) {
                    // Right to Left swipe action
                    if (x2 > x1) {
                        mediaPlayer.stop();
                        mediaPlayer.reset();
                        switchScreens();
                    }
                } else {
                    // consider as something else - a screen tap for example
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    public void switchScreens(){
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }

    public void playSound (String soundName){
        mediaPlayer.stop();
        mediaPlayer.reset();

        File soundFile = this.getFileStreamPath(soundName);
        Uri soundUri = Uri.fromFile(soundFile);
        try {
            mediaPlayer.setDataSource(getApplicationContext(), soundUri);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.start();
        return;
    }
}
