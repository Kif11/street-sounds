package com.example.snehabelkhale.cpptest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by snehabelkhale on 1/6/18.
 */

public class SoundListActivity extends AppCompatActivity {
    private float x1,x2,downX, downY,upX, upY;
    static final int MIN_DISTANCE = 150;
    public static ArrayList<String> listItems=new ArrayList<String>();
    public static ArrayAdapter<String> adapter;
    private MediaPlayer mediaPlayer = new MediaPlayer();

    public void playSound (String soundName){
        File t = this.getFileStreamPath(soundName);
        Uri u = Uri.fromFile(t);
        try {
            mediaPlayer.setDataSource(getApplicationContext(), u);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaPlayer.start();

        return;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sound_list);

        adapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        ListView soundListView = (ListView) findViewById(R.id.soundList);
        soundListView.setAdapter(adapter);

        soundListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View arg1,
                                    int position, long id) {
                Log.d("*****", "clicked item" + position);
                playSound(listItems.get(position));
            }
        });

        soundListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
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


        if (listItems.size() == 0) {
            //list files in directory
            File path = this.getFilesDir();
            File[] files = path.listFiles();
            Log.d("Files", "Size: " + files.length);
            for (int i = 0; i < files.length; i++) {
                Log.d("***** Files", "FileName: " + files[i].getName());
                listItems.add(files[i].getName());
            }
            adapter.notifyDataSetChanged();
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                Log.i("Completion Listener","Song Complete");
                mp.stop();
                mp.reset();
            }
        });
    }

    public void switchScreens(){
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
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

                if (Math.abs(deltaX) > MIN_DISTANCE)
                {
                    // Right to Left swipe action
                    if (x2 > x1)
                    {
                        switchScreens();
                    }

                    // Right to left swipe action
                }
                else
                {
                    // consider as something else - a screen tap for example
                }
                break;
        }
        return super.onTouchEvent(event);
    }
}
