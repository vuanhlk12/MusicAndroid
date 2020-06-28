package com.example.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra("action");
        sendControl(context, action);
    }

    private void sendControl(Context context, String action) {
        Intent sendLevel = new Intent(context, MusicService.class);
        sendLevel.setAction("SONG_CONTROL");
        sendLevel.putExtra("control", action);
        context.startService(sendLevel);
    }
}
