package de.thaler.iamhere;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

public class mBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "myLog BroadcastReceiver";

    public void onReceive(Context context, Intent intent) {
        // note an mich
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            myNotification myNote = new myNotification(MainActivity.mainActivity);
            myNote.showNotification("I am here", "Back to normal.");
        }

        // Check the device's sound profile (e.g. silent mode), then vibrate
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
            Vibrator vibrator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = vibratorManager.getDefaultVibrator();
            } else {
                vibrator = context.getSystemService(Vibrator.class);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(500);
            }
        }
        Toast.makeText(context, "ende", Toast.LENGTH_SHORT).show();
        MainActivity.mainActivity.status.setText("ende");

        String number = MainActivity.mPreference.getString("phoneNumber", "+49 0123 123456789");
        String msg = "Ich habe alles angeschaltet und bin wieder erreichbar";
        // send sms
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number, null, msg, null, null);
        } catch (Exception e) {
            Toast.makeText(MainActivity.mainActivity,"Error. SMS possible?",Toast.LENGTH_LONG).show();
            Log.i(TAG, e.toString());
        } finally {
            // wieder alles ein
            for (int i : MainActivity.audioStreamList) {
                audioManager.setStreamVolume(i,
                        MainActivity.mPreference.getInt("currentVolume_" + i, 5),
                        AudioManager.FLAG_PLAY_SOUND);
                Log.d(TAG, "volume: " + MainActivity.mPreference.getInt("currentVolume_" + i, 5));
            }
            MainActivity.mainActivity.setActuallyAudioStream();
            Toast.makeText(MainActivity.mainActivity, "Message Sent", Toast.LENGTH_LONG).show();
        }
        // play music
        MediaPlayer mPlayer = MediaPlayer.create(context, R.raw.dong);
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            public void run() {
                Log.d(TAG, "BroadcastReceiver onReceive");
                mPlayer.start();
            }
        };
        handler.postDelayed(runnable, 0);
    }
}
