//package com.example.music;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import android.content.Intent;
//import android.graphics.BitmapFactory;
//import android.media.MediaMetadataRetriever;
//import android.media.MediaPlayer;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Message;
//import android.view.KeyEvent;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.SeekBar;
//import android.widget.TextView;
//
//import java.io.File;
//import java.util.ArrayList;
//
//public class SongPlayer extends AppCompatActivity {
//
//    private String path, artistName, title;
//    private int position, songCount;
//    private ArrayList<File> mySongs;
//    private MediaPlayer mp;
//    private TextView songName, artist;
//    private Button playBtn, prevBtn, nextBtn, backBtn;
//    private SeekBar positionBar, volumeBar;
//    private TextView elapsedTimeLabel, remainingTimeLabel;
//    private ImageView songImage;
//    private MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
//    byte[] art;
//    int currentPosition, totalTime;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        setContentView(R.layout.activity_song_player);
//
//        //set song name
//        songName = findViewById(R.id.songName);
//        songName.setSelected(true);
//        artist = findViewById(R.id.artist);
//        songImage = findViewById(R.id.songImage);
//
//        backBtn = findViewById(R.id.back);
//        backBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mp.stop();
//                mp.release();
//                mp = null;
//                finish();
//            }
//        });
//
//        //prev and next button
//        prevBtn = findViewById(R.id.prevBtn);
//        nextBtn = findViewById(R.id.nextBtn);
//        prevBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (position > 0) {
//                    position--;
//                    playMusic(position);
//                }
//            }
//        });
//        nextBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (position < songCount - 1) {
//                    position++;
//                    playMusic(position);
//                }
//            }
//        });
//
//        //Pause Button
//        playBtn = findViewById(R.id.playBtn);
//        playBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!mp.isPlaying()) {
//                    // Stopping
//                    mp.start();
//                    playBtn.setBackgroundResource(R.drawable.stop);
//
//                } else {
//                    // Playing
//                    mp.pause();
//                    playBtn.setBackgroundResource(R.drawable.play);
//                }
//            }
//        });
//
//        //receive data then play
//        validateReceiveAndStartPlaying();
//
//        // Position Bar
//        positionBar = (SeekBar) findViewById(R.id.positionBar);
//        positionBar.setMax(totalTime);
//        positionBar.setOnSeekBarChangeListener(
//                new SeekBar.OnSeekBarChangeListener() {
//                    @Override
//                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                        if (fromUser) {
//                            mp.seekTo(progress);
//                            positionBar.setProgress(progress);
//                        }
//                    }
//
//                    @Override
//                    public void onStartTrackingTouch(SeekBar seekBar) {
//
//                    }
//
//                    @Override
//                    public void onStopTrackingTouch(SeekBar seekBar) {
//
//                    }
//                }
//        );
//
//        // Volume Bar
//        volumeBar = (SeekBar) findViewById(R.id.volumeBar);
//        volumeBar.setOnSeekBarChangeListener(
//                new SeekBar.OnSeekBarChangeListener() {
//                    @Override
//                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                        float volumeNum = progress / 100f;
//                        mp.setVolume(volumeNum, volumeNum);
//                    }
//
//                    @Override
//                    public void onStartTrackingTouch(SeekBar seekBar) {
//
//                    }
//
//                    @Override
//                    public void onStopTrackingTouch(SeekBar seekBar) {
//
//                    }
//                }
//        );
//
//
//        //update position
//        elapsedTimeLabel = findViewById(R.id.elapsedTimeLabel);
//        remainingTimeLabel = findViewById(R.id.remainingTimeLabel);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (mp != null) {
//                    try {
//                        currentPosition = mp.getCurrentPosition();
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                //update position bar
//                                positionBar.setProgress(currentPosition);
//
//                                // Update Labels.
//                                elapsedTimeLabel.setText(createTimeLabel(currentPosition));
//
//                                String remainingTime = createTimeLabel(totalTime - currentPosition);
//                                remainingTimeLabel.setText("- " + remainingTime);
//                            }
//                        });
//
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                    }
//                }
//            }
//        }).start();
//    }
//
//    private void playMusic(int position) {
//        path = mySongs.get(position).getAbsolutePath();
//        metaRetriever.setDataSource(path);
//
//        artistName = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
//        title = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
//        art = metaRetriever.getEmbeddedPicture();
//
//
//        if (art != null) {
//            songImage.setImageBitmap(BitmapFactory.decodeByteArray(art, 0, art.length, null));
//        } else {
//            songImage.setImageResource(R.drawable.image);
//        }
//        songName.setText(title);
//        artist.setText(artistName);
//
//
//        if (mp != null && mp.isPlaying()) {
//            mp.stop();
//            mp.release();
//            mp = null;
//        }
//        try {
//            mp = new MediaPlayer();
//            mp.setDataSource(path);
//            mp.prepare();
//            mp.start();
//            mp.setVolume(0.5f, 0.5f);
//            playBtn.setBackgroundResource(R.drawable.stop);
//            totalTime = mp.getDuration();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void validateReceiveAndStartPlaying() {
//
//        Intent intent = getIntent();
//        Bundle bundle = intent.getExtras();
//
//        if (bundle != null) {
//            mySongs = (ArrayList) bundle.getParcelableArrayList("song");
//            songCount = mySongs.size();
//            position = intent.getIntExtra("position", 0);
//
//            playMusic(position);
//        }
//
//
//    }
//
//    public String createTimeLabel(int time) {
//        String timeLabel = "";
//        int min = time / 1000 / 60;
//        int sec = time / 1000 % 60;
//
//        timeLabel = min + ":";
//        if (sec < 10) timeLabel += "0";
//        timeLabel += sec;
//
//        return timeLabel;
//    }
//
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
//            mp.stop();
//            mp.release();
//            mp = null;
//            finish();
//        }
//        return super.onKeyDown(keyCode, event);
//    }
//}
