package com.example.music;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;


public class SongList extends Fragment {

    private MainActivity mainActivity;
    private View view;
    private ListView mSongList;
    private ArrayList<File> audioSongs;

    public SongList() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_song_list, container, false);

        mainActivity = (MainActivity) getActivity();

        fillSongList();

        mSongList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getActivity(), MusicService.class);
                intent.setAction("SONG_LIST");
                intent.putExtra("song", audioSongs);
                intent.putExtra("position", position);
                getActivity().startService(intent);
            }
        });

        return view;
    }

    private void fillSongList() {

        audioSongs = mainActivity.audioSongs;
        String[] data = new String[audioSongs.size()];


        for (int songCounter = 0; songCounter < audioSongs.size(); songCounter++) {
            MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            metaRetriever.setDataSource(audioSongs.get(songCounter).getAbsolutePath());

            String artist = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String title = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);

            data[songCounter] = artist + " - " + title;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, data);
        mSongList = view.findViewById(R.id.SongListItem);
        mSongList.setAdapter(adapter);
    }


}
