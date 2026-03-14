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
  extends MainActivity Class
  method checkLocation
  method isLocationEnabled
 */
package de.thaler.iamhere;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Consumer;


public class mLocation extends MainActivity {
    static String TAG = "myLog Class Location";
    private static double[] here = new double[]{defLocation[0], defLocation[1]};

    /**
     * checkLocation()
     */
    public static void checkLocation() {
        final LocationManager[] locationManager = {(LocationManager) mainActivity
                .getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE)};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            new Thread(new Runnable() {
                public void run() {
                    if (ActivityCompat.checkSelfPermission(mainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(mainActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        String[] locationPerms = {
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                        };
                        ActivityCompat.requestPermissions(mainActivity, locationPerms, 2);
                    }
                    locationManager[0] = (LocationManager) mainActivity.getSystemService(Context.LOCATION_SERVICE);
                    // Handle runtime location permission, before calling this method, otherwise it will throw SecurityException.
                    // Provider, executor and Consumer<Location> cannot be passed null.
                    // CancellationRequest object can be passed as null.
                    locationManager[0].getCurrentLocation(LocationManager.NETWORK_PROVIDER,
                            null,
                            mainActivity.getMainExecutor(),
                            locationCallback);
                }
            }).start();
        } else {
            Toast.makeText(mainActivity, "Android Version < " + Build.VERSION_CODES.R,
                    Toast.LENGTH_SHORT).show();
        }

    }
    private static final Consumer<android.location.Location> locationCallback = location -> {
        if (location != null) {
            //Log.d(TAG, "in LocationCallback " + String.valueOf(location.getLatitude()));
            //Log.d(TAG, "in LocationCallback " + String.valueOf(location.getLongitude()));
            // start progressbar
            progressBarKringel.setVisibility(View.VISIBLE);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        String mUrl = "https://nominatim.openstreetmap.org/reverse?lat=" +
                                String.valueOf(location.getLatitude()) +
                                "&lon=" +
                                String.valueOf(location.getLongitude()) +
                                "&zoom=19&accept-language=de";
                        URL url = new URL(mUrl);
                        //URL url = new URL("https://nominatim.openstreetmap.org/reverse?lat=54.374301398359776&lon=10.131298899650576&zoom=19&accept-language=de");
                        Document doc = Jsoup.parse(url, 3 * 1000);
                        Log.i(TAG, "result " + Objects.requireNonNull(doc.select("result").first()).text());
                        strLocation = Objects.requireNonNull(doc.select("result").first()).text();
                        // in textField
                        address.setText(strLocation);
                    } catch (IOException e) {
                        Log.e(TAG, "error: " + e);
                        //throw new RuntimeException(e);
                    }
                    progressBarKringel.setVisibility(View.INVISIBLE); // stop progress
                }
            }).start();
        }
    };

    /**
     *
     * @param mainActivity
     * @return boolean
     */
    public static boolean isLocationEnabled(MainActivity mainActivity) {
        LocationManager locationManager = (LocationManager) mainActivity
                .getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }
}