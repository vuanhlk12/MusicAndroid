package com.example.music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Random;

public class MusicService extends Service {

    private MediaPlayer mp;
    private String path, artistName, title;
    private byte[] art;
    private int position, songCount, songDuration, repeat = 0;
    private ArrayList<File> mySongs;
    private ArrayList<File> favorites = new ArrayList<File>();
    private MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
    private Bundle bundle;
    private Binder mBinder = new MyServiceBinder();
    private static final String CHANNEL_1_ID = "channel1";
    private NotificationManagerCompat notificationManagerCompat;
    private Notification notification;
    private NotificationCompat.Builder builder;
    private Random rd = new Random();


    private static final String FILE_NAME = "favor.txt";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public class MyServiceBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();

        loadFavorite();
        if (bundle != null) {
            String action = intent.getAction();
            if (action.equals("SONG_LIST") || action.equals("SONG_LIST_SEARCH") || action.equals("SONG_LIST_FAVORITE")) {
                mySongs = (ArrayList) bundle.getParcelableArrayList("song");
                songCount = mySongs.size();
                position = intent.getIntExtra("position", 0);
                playMusic(position);
            } else {
                String control = intent.getStringExtra("control");
                if (control.equals("next")) {
                    playNext();
                } else if (control.equals("pause")) {
                    if (mp.isPlaying()) {
                        pause();
                    } else {
                        play();
                    }
                } else if (control.equals("prev")) {
                    playPrev();
                } else {
                    clearNotification();
                }
            }
        }

        return START_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mp.stop();
        mp.release();
        mp = null;
        notificationManagerCompat.cancelAll();
    }

    private void createNotification() {
        Intent intentPrev = new Intent(this, ActionReceiver.class);
        intentPrev.setAction("PREV");
        intentPrev.putExtra("action", "prev");
        PendingIntent pIntentPrev = PendingIntent.getBroadcast(this, 1, intentPrev, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentPause = new Intent(this, ActionReceiver.class);
        intentPause.setAction("PAUSE");
        intentPause.putExtra("action", "pause");
        PendingIntent pIntentPause = PendingIntent.getBroadcast(this, 1, intentPause, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentNext = new Intent(this, ActionReceiver.class);
        intentNext.setAction("NEXT");
        intentNext.putExtra("action", "next");
        PendingIntent pIntentNext = PendingIntent.getBroadcast(this, 1, intentNext, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentClose = new Intent(this, ActionReceiver.class);
        intentClose.setAction("CLOSE");
        intentClose.putExtra("action", "close");
        PendingIntent pIntentClose = PendingIntent.getBroadcast(this, 1, intentClose, PendingIntent.FLAG_UPDATE_CURRENT);

        builder = new NotificationCompat.Builder(this, CHANNEL_1_ID)
                .setSmallIcon(R.drawable.music)
                .addAction(R.drawable.prev, "Prev", pIntentPrev);

        if (mp.isPlaying()) {
            builder.addAction(R.drawable.stop, "Play/Pause", pIntentPause);
        } else {
            builder.addAction(R.drawable.play, "Play/Pause", pIntentPause);
        }

        builder.addAction(R.drawable.next, "Next", pIntentNext)
                .addAction(R.drawable.close, "Close", pIntentClose)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle())
                .setCategory(NotificationCompat.CATEGORY_SERVICE);
        builder.setContentTitle(title)
                .setContentText(artistName)
                .setLargeIcon(BitmapFactory.decodeByteArray(art, 0, art.length, null));
        notificationManagerCompat = NotificationManagerCompat.from(this);
        notification = builder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notificationManagerCompat.notify(1, notification);
    }

    private void clearNotification() {
        this.stopSelf();
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) this.getSystemService(ns);
        nMgr.cancel(1);
    }

    private void playMusic(int position) {
        if (mySongs != null) {
            path = mySongs.get(position).getAbsolutePath();
            metaRetriever.setDataSource(path);

            artistName = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            title = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            art = metaRetriever.getEmbeddedPicture();

            if (mp != null && mp.isPlaying()) {
                mp.stop();
                mp.release();
                mp = null;
            }
            try {
                mp = new MediaPlayer();
                mp.setDataSource(path);
                mp.prepare();
                songDuration = mp.getDuration();
                mp.start();
                mp.setVolume(0.5f, 0.5f);
            } catch (Exception e) {
                e.printStackTrace();
            }

            createNotification();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (mp != null) {
                        try {
                            int currentPosition = 0;
                            if (mp != null) {
                                currentPosition = mp.getCurrentPosition();
                            }
                            sendCurrentPosition(currentPosition, songDuration);
                            sendSongInfo(art, title, artistName);
                            Thread.sleep(1000);
                        } catch (Exception e) {
                        }
                    }
                }
            }).start();
        }


    }

    public void pause() {
        if (mp != null) {
            mp.pause();
            createNotification();
        }
    }

    public void play() {
        if (mp != null) {
            mp.start();
            createNotification();
        }
    }

    public void playNext() {
        switch (repeat) {
            case 0: {
                position = (position + 1 + songCount) % songCount;
                playMusic(position);
                break;
            }
            case 1: {
                playMusic(position);
                break;
            }
            case 2: {
                int i = Math.abs(rd.nextInt()) % songCount;
                while (i == position) {
                    i = Math.abs(rd.nextInt()) % songCount;
                }
                position = i;
                playMusic(position);
                break;
            }
            default: {
                break;
            }
        }


    }

    public void playPrev() {
        switch (repeat) {
            case 0: {
                position = (position - 1 + songCount) % songCount;
                playMusic(position);
                break;
            }
            case 1: {
                playMusic(position);
                break;
            }
            case 2: {
                int i = Math.abs(rd.nextInt()) % songCount;
                while (i == position) {
                    i = Math.abs(rd.nextInt()) % songCount;
                }
                position = i;
                playMusic(position);
            }
            default: {
                break;
            }
        }

    }

    public void setSongPosition(int position) {
        mp.seekTo(position);
    }

    public void setVolume(int volume) {
        float volumeNum = volume / 100f;
        mp.setVolume(volumeNum, volumeNum);
    }

    public void setRepeat(int a) {
        this.repeat = a;
    }

    private boolean isHaveFavoriteSong(File song) {
        for (File temp : favorites) {
            if (temp.getAbsolutePath().equals(song.getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }

    public void addFavorite() {
        File currentSong = mySongs.get(position);
        if (!isHaveFavoriteSong(currentSong)) {
            favorites.add(currentSong);
        } else {
            for (File temp : favorites) {
                if (temp.getAbsolutePath().equals(currentSong.getAbsolutePath())) {
                    favorites.remove(temp);
                    break;
                }
            }
        }
        Gson gson = new Gson();
        String json = gson.toJson(favorites);
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(FILE_NAME, MODE_PRIVATE);
            fos.write(json.getBytes());

            //Toast.makeText(this, "Add to favortite: " + getFilesDir(), Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void loadFavorite() {
        Gson gson = new Gson();
        FileInputStream fis = null;
        Type fileListType = new TypeToken<ArrayList<File>>() {
        }.getType();

        try {
            fis = openFileInput(FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String text;
            while ((text = br.readLine()) != null) {
                sb.append(text).append("");
            }
            favorites = gson.fromJson(sb.toString(), fileListType);
            if (favorites == null) {
                favorites = new ArrayList<File>();
            }
            int i = 1;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendCurrentPosition(int currentTime, int _songDuration) {
        Intent sendLevel = new Intent();
        sendLevel.setAction("GET_CURRENT_POSITION");
        sendLevel.putExtra("currentTime", currentTime);
        sendLevel.putExtra("songDuration", _songDuration);
        if (isHaveFavoriteSong(mySongs.get(position))) {
            sendLevel.putExtra("isFavorite", true);
        } else {
            sendLevel.putExtra("isFavorite", false);
        }
        sendBroadcast(sendLevel);
    }

    private void sendSongInfo(byte[] art, String title, String artist) {
        Intent sendLevel = new Intent();
        sendLevel.setAction("GET_SONG_INFO");
        sendLevel.putExtra("art", art);
        sendLevel.putExtra("title", title);
        sendLevel.putExtra("artist", artist);
        sendBroadcast(sendLevel);
    }

    class ControlReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            int c = 9;
            if (intent.getAction().equals("SONG_CONTROL")) {
                String temp = intent.getStringExtra("control");
                int a = 0;
            }
        }

    }

    public boolean isPlaying() {
        if (mp != null) {
            return mp.isPlaying();
        }
        return false;
    }


}
