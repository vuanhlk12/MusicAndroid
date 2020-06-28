package com.example.music;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;


public class SongListArtist extends Fragment {

    private static final String FILE_NAME = "favor.txt";

    private ArrayList<File> favorites = new ArrayList<File>();
    private ListView mFavoriteList;
    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_song_list_artist, container, false);
        loadFavorite();
        fillSongList();
        mFavoriteList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getActivity(), MusicService.class);
                intent.setAction("SONG_LIST_FAVORITE");
                intent.putExtra("song", favorites);
                intent.putExtra("position", position);
                getActivity().startService(intent);
            }
        });
        return view;
    }

    public void loadFavorite() {
        Gson gson = new Gson();
        FileInputStream fis = null;
        Type fileListType = new TypeToken<ArrayList<File>>() {
        }.getType();

        try {
            fis = getActivity().openFileInput(FILE_NAME);
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

    private void fillSongList() {
        ArrayList<String> data = new ArrayList<String>();


        for (int songCounter = 0; songCounter < favorites.size(); songCounter++) {
            MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            metaRetriever.setDataSource(favorites.get(songCounter).getAbsolutePath());

            String artist = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String title = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);

            data.add(artist + " - " + title);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, data);
        mFavoriteList = view.findViewById(R.id.SongListFavorite);
        mFavoriteList.setAdapter(adapter);
    }
}
