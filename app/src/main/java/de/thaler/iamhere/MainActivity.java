/*
 * Copyright 2026 Mathias Uebel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
see also: https://developer.android.com/reference/android/media/AudioManager.html
STREAM_ACCESSIBILITY - Used to identify the volume of audio streams for accessibility prompts
STREAM_ALARM - Used to identify the volume of audio streams for alarms
STREAM_ASSISTANT - Used to identify the volume of audio streams for virtual assistant
STREAM_DTMF - Used to identify the volume of audio streams for DTMF Tones
STREAM_MUSIC - Used to identify the volume of audio streams for music playback
STREAM_NOTIFICATION - Used to identify the volume of audio streams for notifications
STREAM_RING - Used to identify the volume of audio streams for the phone ring
STREAM_SYSTEM - Used to identify the volume of audio streams for system sounds
STREAM_VOICE_CALL - Used to identify the volume of audio streams for phone calls
USE_DEFAULT_STREAM_TYPE - Suggests using the default stream type.
*/
package de.thaler.iamhere;

import static android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE;
import static android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE;
import static android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.osmdroid.config.Configuration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "myLog MainActivity";
    //public static final double[] defLocation = new double[]{54.374301398359776,10.131298899650576};
    public static final double[] defLocation = new double[]{50,10};
    @SuppressLint("StaticFieldLeak")
    public static MainActivity mainActivity;
    public static String strLocation = "unknown location";
    public TextView available;
    @SuppressLint("StaticFieldLeak")
    public static TextView address;
    @SuppressLint("StaticFieldLeak")
    public static ProgressBar progressBarKringel;
    public static SharedPreferences mPreference;
    private int hour, minutes;
    public TextView status;
    public ProgressBar progressBarStreamSystem, progressBarStreamRing, progressBarStreamMusic,
            progressBarStreamAlarm, progressBarStreamNotification;
    AudioManager audioManager;
    public static int[] audioStreamList;
    mBroadcastReceiver mBroadcastReceiver;
    AlarmManager alarmManager;
    Intent intent;
    IntentFilter intentFilter;
    PendingIntent pendingIntent;

    @SuppressLint({"SetTextI18n", "UnspecifiedRegisterReceiverFlag"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mainActivity = this;
        mPreference = this.getSharedPreferences("MyPref", 0);
        // permissions
        String[] locationPerms = {
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.INTERNET,
                Manifest.permission.VIBRATE
        };
        ActivityCompat.requestPermissions(this, locationPerms, 2);

        AudioManager manager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        manager.setStreamVolume(AudioManager.STREAM_ALARM, AudioManager.STREAM_ALARM, AudioManager.FLAG_VIBRATE); //.FLAG_REMOVE_SOUND_AND_VIBRATE);
        //manager.setStreamVolume(AudioManager.STREAM_ALARM, 0, AudioManager.FLAG_PLAY_SOUND);


        //myAlarmReceiver = new myAlarmReceiver();
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        intent = new Intent(this, mBroadcastReceiver.class);
        intentFilter = new IntentFilter();
        intentFilter.addAction(String.valueOf(intent).split("=")[1]);
        registerReceiver(mBroadcastReceiver, intentFilter);
        pendingIntent = PendingIntent.getBroadcast(this,0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // alle audio als Progress anzeigen
        progressBarStreamSystem = findViewById(R.id.progressBarStreamSystem);
        progressBarStreamRing = findViewById(R.id.progressBarStreamRing);
        progressBarStreamMusic = findViewById(R.id.progressBarStreamMusic);
        progressBarStreamAlarm = findViewById(R.id.progressBarStreamAlarm);
        progressBarStreamNotification = findViewById(R.id.progressBarStreamNotification);

        audioStreamList = new int[]{
                AudioManager.STREAM_SYSTEM,
                AudioManager.STREAM_RING,
                AudioManager.STREAM_MUSIC,
                AudioManager.STREAM_ALARM,
                AudioManager.STREAM_NOTIFICATION
        };
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        setActuallyAudioStream();  // sets the current status in the progress bar

        // set time
        Spinner dropDownHour = findViewById(R.id.spinner_hour);
        String[] itemsHour = getResources().getStringArray(R.array.SPINNER_HOUR);
        ArrayAdapter<String> adapterHour = new ArrayAdapter<>(this, R.layout.spinner_list, itemsHour);
        dropDownHour.setAdapter(adapterHour);

        TextView doublePoint = findViewById(R.id.doublePoint);

        Spinner dropDownMinutes = findViewById(R.id.spinner_minutes);
        String[] itemsMinute = getResources().getStringArray(R.array.SPINNER_MINUTE);
        ArrayAdapter<String> adapterMinute = new ArrayAdapter<>(this, R.layout.spinner_list, itemsMinute);
        dropDownMinutes.setAdapter(adapterMinute);


        // store the current value in cache
        for (int i : audioStreamList) {
            mPreference.edit().putInt("currentVolume_" + i, audioManager.getStreamVolume(i)).apply();
        }

        //
        address = findViewById(R.id.textViewAddress);
        progressBarKringel = findViewById(R.id.progressBar);
        EditText textPhoneNumber = findViewById(R.id.editTextPhone);
        textPhoneNumber.setText(mPreference.getString("phoneNumber", "+49 0123 123456789"));

        available = findViewById(R.id.textViewLocationAvailable);
        if (mLocation.isLocationEnabled(mainActivity)) {
            available.setTextColor(getResources().getColor(R.color.black));
            available.setText("Location is available");
        }

        Spinner dropDownInfo = findViewById(R.id.spinnerInfo);
        String[] itemsInfos = new String[]{"beschäftigt", "im Zug/Bus", "beim Arzt*in",
                "in einer Versammlung", "im Kino", "in einer wichtigen Besprechung",
                "im Puff"};
        ArrayAdapter<String> adapterInfos = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, itemsInfos);
        dropDownInfo.setAdapter(adapterInfos);

        Button searchLocation = findViewById(R.id.buttonSearchLocation);
        searchLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLocation.checkLocation();
                address.setText(mLocation.strLocation);
            }
        });

        Button sendSMS = findViewById(R.id.buttonSendSMS);
        sendSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // spinner items
                hour = (int) dropDownHour.getSelectedItemPosition();
                minutes = (int) dropDownMinutes.getSelectedItemPosition() * 15;
                Log.d(TAG, "h+m " + hour + minutes);
                long delay = ((long) hour * 60 * 60000) + (minutes * 60000L);
                mTimer(delay);

                @SuppressLint("SimpleDateFormat")
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                long startTime = System.currentTimeMillis();
                long endTime = startTime + delay;

                //Toast.makeText(mainActivity, "Start: " + simpleDateFormat.format(startTime)
                //        + " Ende: " + simpleDateFormat.format(endTime), Toast.LENGTH_LONG).show();

                String number = mPreference.getString("phoneNumber", "+49 0123 123456789");
                String msg = "Ich bin " + dropDownInfo.getSelectedItem().toString() + ". Ort: "
                        + address.getText().toString() + " Ende (ca.): "
                        + simpleDateFormat.format(endTime);
                //Toast.makeText(mainActivity, msg, Toast.LENGTH_LONG).show();

                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(number, null, msg, null, null);
                    // alles auf null
                    for (int i : audioStreamList) {
                        audioManager.setStreamVolume(i, 0, AudioManager.FLAG_PLAY_SOUND);
                        Log.d(TAG, "volume index " + i + " null: " + audioManager.getStreamVolume(i));
                    }
                    Toast.makeText(getApplicationContext(), "send SMS", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(),"Error. SMS possible?",Toast.LENGTH_LONG).show();
                    Log.i(TAG, e.toString());
                } finally {
                    // note an mich
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        myNotification myNote = new myNotification(MainActivity.mainActivity);
                        myNote.showNotification("I am here", "Start: "
                                + simpleDateFormat.format(startTime)
                                + " Ende: " + simpleDateFormat.format(endTime));
                    }
                    setActuallyAudioStream();
                }
            }
        });

        Button saveNumber = findViewById(R.id.buttonSaveNumber);
        saveNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPreference.edit().putString("phoneNumber",
                        String.valueOf(textPhoneNumber.getEditableText())).apply();
                //mPreference.edit().putString("i_am_here", String.valueOf(textIamHere.getEditableText())).apply();
            }
        });

        Button clear = findViewById(R.id.buttonClear);
        clear.setOnClickListener(new View.OnClickListener() {
            final Set<String> list = mPreference.getStringSet("List", new HashSet<>());
            @Override
            public void onClick(View v) {
                for (int i : audioStreamList) {
                    audioManager.setStreamVolume(i,
                            mPreference.getInt("currentVolume_" + i, 0),
                            AudioManager.FLAG_PLAY_SOUND);
                }
                setActuallyAudioStream();
            }
        });
    }
    /**
     * sets the current status in the progress bar
     */
    public void setActuallyAudioStream() {
        progressBarStreamSystem.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM));
        progressBarStreamRing.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_RING));
        progressBarStreamMusic.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        progressBarStreamAlarm.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_ALARM));
        progressBarStreamNotification.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION));
    }
    /**
     * Starts the timer process with the BroadcastReceiver.
     * @param delay
     */
    @SuppressLint("SetTextI18n")
    private void mTimer (long delay) {
        // test delay = 10000;
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        long startTime = System.currentTimeMillis();
        long endTime = startTime + delay;

        Toast.makeText(this, "Start: "
                + simpleDateFormat.format(new Date(startTime))
                + "\nEnde: "
                + simpleDateFormat.format(new Date(endTime))
                , Toast.LENGTH_SHORT).show();

        status = (TextView) findViewById(R.id.textViewStatus);
        status.setText("Start: " + simpleDateFormat.format(new Date(startTime))
                + "\nEnde: " + simpleDateFormat.format(new Date(endTime)));

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                + (delay), pendingIntent);
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onResume() {
        super.onResume();
        // always on start
        mLocation.checkLocation();
        registerReceiver(mBroadcastReceiver, intentFilter);
    }
    @Override
    public void onPause() {
        super.onPause();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        String error = "";
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
            error += " No Notification.";
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            error += " No Location.";
        }
        if (!error.isEmpty()) {
            Toast.makeText(mainActivity, error, Toast.LENGTH_SHORT).show();
        }

    }
}