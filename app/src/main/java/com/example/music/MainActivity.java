package com.example.music;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.app.NotificationCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private TabItem songList, album, artists;

    public PagerAdapter pagerAdapter;
    private Button playMainBtn;
    private ProgressBar mProgressBar;
    private MusicService mMusicService;
    private ImageView songImageTask, songImage;
    private Button prevMainBtn, pauseMainBtn, nextMainBtn, backBtn, prevBtn, nextBtn, playBtn, repeatBtn, favorBtn;
    private TextView songNameBar, artistNameBar, songName, artist, elapsedTimeLabel, remainingTimeLabel;
    private int currentTime, songDuration, repeat = 0;
    private boolean mBound = false;
    private LinearLayout fullPlayerScreen, mainScreen;
    private SeekBar positionBar, volumeBar;

    public ArrayList<File> audioSongs;

    byte[] art;
    private String artistName, title;

    private SongInfoReceiver receiver;
    private CurrentReceiver currentReceiver;

    private ServiceConnection mServiceCon = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MyServiceBinder binder = (MusicService.MyServiceBinder) service;
            mMusicService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    private String[] itemsAll;
    private ListView mSongList;

    private MediaPlayer mp;

    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static final int REQUEST_PERMISSIONS = 12345;

    private static final int PERMISSIONS_COUNT = 1;

    @Override
    protected void onStart() {
        super.onStart();
        audioSongs = readOnlyAudioSongs(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        audioSongs.addAll(readOnlyAudioSongs(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)));
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, mServiceCon, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mServiceCon);
//        unregisterReceiver(receiver);
        //     unregisterReceiver(currentReceiver);
        mBound = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //receiver
        receiver = new SongInfoReceiver();
        registerReceiver(receiver, new IntentFilter("GET_SONG_INFO"));

        currentReceiver = new CurrentReceiver();
        registerReceiver(currentReceiver, new IntentFilter("GET_CURRENT_POSITION"));

        setContentView(R.layout.activity_main);

        initElement();
        setListenerElement();

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                if (tab.getPosition() == 0) {
                    pagerAdapter.notifyDataSetChanged();
                } else if (tab.getPosition() == 1) {
                    pagerAdapter.notifyDataSetChanged();
                } else if (tab.getPosition() == 2) {
                    pagerAdapter.notifyDataSetChanged();
                }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
    }

    private void initElement() {
        fullPlayerScreen = findViewById(R.id.fullPlayerScreen);
        mainScreen = findViewById(R.id.mainScreen);
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        songNameBar = findViewById(R.id.songNameBar);
        songNameBar.setSelected(true);
        artistNameBar = findViewById(R.id.artistNameBar);
        artistNameBar.setSelected(true);

        songName = findViewById(R.id.songName);
        songName.setSelected(true);
        artist = findViewById(R.id.artist);
        songImage = findViewById(R.id.songImage);

        elapsedTimeLabel = findViewById(R.id.elapsedTimeLabel);
        remainingTimeLabel = findViewById(R.id.remainingTimeLabel);

        pagerAdapter = new PageAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(pagerAdapter);

        positionBar = findViewById(R.id.positionBar);
        playMainBtn = findViewById(R.id.pauseMainBtn);
        playBtn = findViewById(R.id.playBtn);
        nextMainBtn = findViewById(R.id.nextMainBtn);
        nextBtn = findViewById(R.id.nextBtn);
        prevMainBtn = findViewById(R.id.prevMainBtn);
        prevBtn = findViewById(R.id.prevBtn);
        songImageTask = findViewById(R.id.songImageTask);
        backBtn = findViewById(R.id.back);
        volumeBar = findViewById(R.id.volumeBar);

        repeatBtn = findViewById(R.id.repeat);
        favorBtn = findViewById(R.id.favor);
    }

    private void setListenerElement() {
        positionBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            mMusicService.setSongPosition(progress);
                            positionBar.setProgress(progress);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                }
        );

        //pause button
        playMainBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mBound == true) {
                    if (mMusicService.isPlaying()) {
                        mMusicService.pause();
                        playMainBtn.setBackgroundResource(R.drawable.play);
                        playBtn.setBackgroundResource(R.drawable.play);
                    } else {
                        mMusicService.play();
                        playMainBtn.setBackgroundResource(R.drawable.stop);
                        playBtn.setBackgroundResource(R.drawable.stop);
                    }
                }
            }
        });

        playBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mBound == true) {
                    if (mMusicService.isPlaying()) {
                        mMusicService.pause();
                        playMainBtn.setBackgroundResource(R.drawable.play);
                        playBtn.setBackgroundResource(R.drawable.play);
                    } else {
                        mMusicService.play();
                        playMainBtn.setBackgroundResource(R.drawable.stop);
                        playBtn.setBackgroundResource(R.drawable.stop);
                    }
                }
            }
        });

        //next button
        nextMainBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBound == true) {
                    mMusicService.playNext();
                }
            }
        });

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBound == true) {
                    mMusicService.playNext();
                }
            }
        });

        //prev button
        prevMainBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBound == true) {
                    mMusicService.playPrev();
                }
            }
        });

        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBound == true) {
                    mMusicService.playPrev();
                }
            }
        });

        //song img click
        songImageTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainScreen.setVisibility(View.GONE);
                fullPlayerScreen.setVisibility(View.VISIBLE);

            }
        });

        //back btn
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainScreen.setVisibility(View.VISIBLE);
                fullPlayerScreen.setVisibility(View.GONE);
            }
        });

        //volumbar
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mMusicService.setVolume(progress);
                    volumeBar.setProgress(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //repeat button
        repeatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                repeat = (repeat + 1 + 3) % 3;
                mMusicService.setRepeat(repeat);
                switch (repeat) {
                    case 0: {
                        repeatBtn.setBackgroundResource(R.drawable.repeat);
                        break;
                    }
                    case 1: {
                        repeatBtn.setBackgroundResource(R.drawable.repeat_one);
                        break;
                    }
                    case 2: {
                        repeatBtn.setBackgroundResource(R.drawable.shuffle);
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        });

        //favor btn
        favorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMusicService.addFavorite();
            }
        });
    }

    private boolean arePermissionDenied() {
        for (int i = 0; i < PERMISSIONS_COUNT; i++) {
            if (checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (arePermissionDenied()) {
            ((ActivityManager) (this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
            recreate();
        } else {
            onResume();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (arePermissionDenied()) {
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
            return;
        }
    }

    public void appExternalStorageStoragePermission() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        //displayAudioSongsName();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();

    }

    public ArrayList<File> readOnlyAudioSongs(File file) {
        ArrayList<File> arrayList = new ArrayList<>();

        File[] allFiles = file.listFiles();

        for (File individualFile : allFiles) {
            if (individualFile.isDirectory()) {
                arrayList.addAll(readOnlyAudioSongs(individualFile));
            } else {
                if (individualFile.getName().endsWith(".mp3")) {
                    arrayList.add(individualFile);
                }
            }
        }

        return arrayList;
    }


//    public void showNotification(View v){
//        RemoteViews collapsedView = new RemoteViews(getPackageName(),R.layout.)
//        Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_1_ID)
//                .setSmallIcon(R.drawable.sound)
//                .setContentTitle("Music")
//                .setContentText("song playing")
//                .build();
//    }

    public String createTimeLabel(int time) {
        String timeLabel = "";
        int min = time / 1000 / 60;
        int sec = time / 1000 % 60;

        timeLabel = min + ":";
        if (sec < 10) timeLabel += "0";
        timeLabel += sec;

        return timeLabel;
    }

    class SongInfoReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean again = false;
            do {
                try {
                    if (mMusicService.isPlaying()) {
                        playMainBtn.setBackgroundResource(R.drawable.stop);
                        playBtn.setBackgroundResource(R.drawable.stop);
                    } else {
                        playMainBtn.setBackgroundResource(R.drawable.play);
                        playBtn.setBackgroundResource(R.drawable.play);
                    }
                    if (intent.getAction().equals("GET_SONG_INFO")) {
                        String oldTitle = title;
                        title = intent.getStringExtra("title");
                        if (!title.equals(oldTitle)) {
                            art = intent.getByteArrayExtra("art");
                            artistName = intent.getStringExtra("artist");

                            songNameBar.setText(title);
                            artistNameBar.setText(artistName);

                            songName.setText(title);
                            artist.setText(artistName);

                            if (art != null) {
                                songImageTask.setImageBitmap(BitmapFactory.decodeByteArray(art, 0, art.length, null));
                                songImage.setImageBitmap(BitmapFactory.decodeByteArray(art, 0, art.length, null));
                            } else {
                                songImageTask.setImageResource(R.drawable.image);
                                songImage.setImageResource(R.drawable.image);
                            }
                        }
                    }
                    again = false;
                } catch (Exception e) {
                    again = true;
                }
            } while (again);


        }

    }

    class CurrentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals("GET_CURRENT_POSITION")) {
                currentTime = intent.getIntExtra("currentTime", 0);
                songDuration = intent.getIntExtra("songDuration", 0);
                boolean isFavorite = intent.getBooleanExtra("isFavorite", false);
                if (isFavorite) {
                    favorBtn.setBackgroundResource(R.drawable.favor);
                } else {
                    favorBtn.setBackgroundResource(R.drawable.not_favor);
                }

                if (currentTime >= songDuration - 1000) {
                    mMusicService.playNext();
                    return;
                }
                positionBar.setMax(songDuration);
                positionBar.setProgress(currentTime);
                elapsedTimeLabel.setText(createTimeLabel(currentTime));
                remainingTimeLabel.setText(createTimeLabel(songDuration - currentTime));
            }
        }

    }


}
