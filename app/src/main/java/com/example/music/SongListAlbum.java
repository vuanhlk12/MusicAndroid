package com.example.music;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.ArrayList;


public class SongListAlbum extends Fragment {

    private MainActivity mainActivity;
    private View view;
    private ListView mSongList;
    private ArrayList<File> audioSongs, ShowAudioSongs;
    private TextInputEditText SearchInput;
    private Button SearchBtn;
    private ArrayList<String> ShowSongList, AllSongList;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_song_list_album, container, false);

        AllSongList = new ArrayList<String>();
        ShowSongList = new ArrayList<String>();

        SearchInput = view.findViewById(R.id.SearchInput);
        SearchBtn = view.findViewById(R.id.SearchBtn);
        mSongList = view.findViewById(R.id.SongListSearchItem);

        mainActivity = (MainActivity) getActivity();

        audioSongs = mainActivity.audioSongs;
        ShowAudioSongs = new ArrayList<File>();
        for (File temp : audioSongs) {
            ShowAudioSongs.add(temp);
        }

        fillAllSongList();

        mSongList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getActivity(), MusicService.class);
                intent.setAction("SONG_LIST_SEARCH");
                intent.putExtra("song", ShowAudioSongs);
                intent.putExtra("position", position);
                getActivity().startService(intent);
            }
        });

        SearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String keyword = SearchInput.getText().toString();
                fillSongBySearch(keyword);
            }
        });

        return view;
    }

    private void fillSongBySearch(String keyword) {
        ShowSongList.clear();
        ShowAudioSongs.clear();
        for (int i = 0; i < audioSongs.size(); i++) {
            String temp = AllSongList.get(i);
            if (temp.contains(keyword)) {
                ShowSongList.add(temp);
                ShowAudioSongs.add(audioSongs.get(i));
            }
        }
        fillSongList(ShowSongList);
    }

    private void fillAllSongList() {
        for (int songCounter = 0; songCounter < audioSongs.size(); songCounter++) {
            MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            metaRetriever.setDataSource(audioSongs.get(songCounter).getAbsolutePath());

            String artist = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String title = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);

            AllSongList.add(artist + " - " + title);
        }

        //fillSongList(AllSongList);
    }

    private void fillSongList(ArrayList<String> data) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, data);
        mSongList.setAdapter(adapter);
    }
}
