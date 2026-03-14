package de.thaler.iamhere;
/*
 * Copyright 2024 Mathias Uebel
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
/**
 * <h5>This class contains a lot of helpful methods and functions.</h5>
 * <p>Copyright (c) Mathias Uebel</p>
 */
public class helper {
    private static final String TAG = "myLog helper.java";
    public static Handler mBackgroundHandler;
    public static HandlerThread mBackgroundThread;

    /**
     * <p>it is a toast message</p>
     *
     *     <li> 1 = normal custom</li>
     *     <li> 2 = warnung</li>
     *     <li> 0* ist default Toast</li>
     *
     * @param mContext
     * @param message
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    /*
    public static void showToast(Context mContext, String message, int style) {
        Toast toast = new Toast(mContext.getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);

        LayoutInflater li = mainActivity.getLayoutInflater();
        View layout = li.inflate(R.layout.custom_toast,
                mainActivity.findViewById(R.id.linearLayoutToast));
        //layout.setRotation(0);
        TextView text = layout.findViewById(R.id.textToast);
        text.setText(message);

        // je nachdem als normal oder warnung
        switch(style) {
            case 1:
                layout.setBackground(mContext.getDrawable(R.drawable.button_shapes));
                toast.setView(layout); //setting the view of custom toast layout
                break;
            case 2:
                layout.setBackground(mContext.getDrawable(R.drawable.button_shapes_on));
                //ImageView image = layout.findViewById(R.id.imageToast);
                //image.setImageDrawable(mContext.getDrawable(drawable.ic_stat_name));
                toast.setView(layout); //setting the view of custom toast layout
                break;
            default:
                toast.setText(message);
        }
        toast.show();
    }

     */
    /**
     * returns the current IP in WLAN
     * @return String
     * <p>sample: "192.168.188.50"</p>
     */
    public static String WebIPAddress (Context context) {
        WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String ipString;
        try {
            ipString = InetAddress.getByAddress(
                    ByteBuffer
                            .allocate(Integer.BYTES)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .putInt(mWifiManager.getConnectionInfo().getIpAddress())
                            .array()
            ).getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return ipString;
    }
    public static String lastModified(String filename) {
        String formatDateString = "YYYY-MM-DDThh:mm:ss[.s+]Z";
        try {
            Path file = Paths.get(filename);
            BasicFileAttributes attr =
                    Files.readAttributes(file, BasicFileAttributes.class);
            //Log.i(TAG, "lastModifiedTime: " + attr.lastModifiedTime());
            FileTime fileTime = attr.lastModifiedTime();
            //Log.i(TAG, "lastModifiedTime: " + formatDateTime(fileTime));
            formatDateString = formatDateTime(fileTime);
        } catch (IOException e) {
            throw new RuntimeException();
        }
        return formatDateString;
    }

    /**
     *
     * @param fileTime
     * @return
     */
    public static String formatDateTime(FileTime fileTime) {
        LocalDateTime localDateTime = fileTime
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        return localDateTime.format(DATE_FORMATTER);
    }
    /**
     * <p>formats a time specification</p>
     * <p>"yyyy-MM-dd_HH:mm:ss"</p>
     */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");

    /**
     * <p>creates a temporary file in the cache</p>
     * @param name
     * @param suffix
     * @return file
     */
    public static File makeTempFile (Context context, String name, String suffix) {
        try {
            File outputDir = context.getCacheDir();
            //Log.i(TAG, "Temporary file with secure permissions created at: " + outputFile);
            return File.createTempFile(name, suffix, outputDir);
        } catch (IOException e) {
            Log.e(TAG, "Error creating secure temporary file: " + e.getMessage());
        }
        return null;
    }

    /**
     * <p>returns a MimeType of file</p>
     * @param filename
     * @return type
     */
    public static String getMimeType(String filename) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(filename);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    /**
     * <p>save a bitmap compressed in png</p>
     * @param bmp
     * @param dst
     * @return boolean
     */
    public static Boolean saveBitmap (Bitmap bmp, String dst) {
        try (FileOutputStream out = new FileOutputStream(dst)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 75, out); // bmp is your Bitmap instance
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * <p>copy a file</p>
     * <p>if it doesn't work, then show a toast</p>
     * @param src
     * @param dst
     */
    public static void copyFile (Context context, String src, String dst) {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "copy fail" + e);
            Toast.makeText(context, "copy fail", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * <p>move a file</p>
     * @param src
     * @param dst
     */
    public static void moveFile (Context context, String src, String dst) {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                new File(src).delete();
            }
        } catch (IOException ignore) {
            Log.e(TAG, "move fail");
            Toast.makeText(context, "copy fail", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * <p>delete a file</p>
     * @param context
     * @param file
     * @return boolean
     */
    public static Boolean deleteFile (Context context, File file) {
        //File file = new File(Objects.requireNonNull(uri.getPath()));
        //file.delete();
        boolean res = false;
        try {
            if(file.exists()){
                res = file.getCanonicalFile().delete();
                if(file.exists()){
                    res = context.getApplicationContext().deleteFile(file.getName());
                }
            }
        } catch (RuntimeException | IOException e) {
            throw new RuntimeException(e);
        }
        return res;
    }
    /**
     * <p>cut String in brackets</p>
     * <p>Sample: cutString("[test]", "[", "]") -> test</p>
     * @param str
     * @param start
     * @param end
     */
    public static String cutString(String str, String start, String end) {
        /**
         * cut String in brackets
         */
        String result = str;
        if (str.startsWith(start) && str.endsWith(end)) {
            result = str.substring(0, str.length() - end.length()).substring(start.length());
        }
        return result;
    }

    /**
     * <p>searches a directory and outputs all files</p>
     * @param dir (absolute path string)
     * @return List</String>
     */
    public static List<String> listFilesInDir(String dir) {
        return Stream.of(new File(dir).listFiles())
                .filter(file -> !file.isDirectory())
                .sorted()
                .map(File::getName)
                .collect(Collectors.toList());
    }

    /**
     * <p>Goes through a directory and descends into all subdirectories.</p>
     * <p>https://mkyong.com/java/java-files-walk-examples/</p>
     * @param args
     * @return List</String>
     */
    public static List<String> Walk2Dir (String... args) {
        // https://mkyong.com/java/java-files-walk-examples/
        List<String> fileList = new ArrayList<>();
        Path start = Paths.get(args[0]);
        String suffix;
        if (args[1] != null) {
            suffix = args[1];
        } else {
            suffix = "";
        }
        try {
            Files.walk(start)
                    .sorted()
                    .filter(path -> path.toString().endsWith(suffix))
                    .forEach(path -> {
                        try {
                            fileList.add(path.toString());
                        } catch (Exception e) {
                            throw new RuntimeException();
                        }
                    });
        } catch (IOException e) {
            Log.i(TAG, " " + e);
            return fileList;
        }
        return fileList;
    }

    /**
     * <p>load a picture from assets</p>
     * @param context
     * @param path
     * @return
     */
    public static Bitmap loadImageFromAssets(Context context, String path) {
        try  {
            InputStream is = context.getAssets().open(path);
            return BitmapFactory.decodeStream(is);
        } catch (Exception e) {
            Log.e(TAG, "load Image from Assets fail: " + e);
        }
        return null;
    }

    /**
     * load mp3 from assets, but only short bings <br />
     * if you play songs, take it in raw and load with R.raw...
     * @param context
     * @param path
     * @return
     */
    public static MediaPlayer loadAudioFromAssets(Context context, String path) {
        MediaPlayer player = new MediaPlayer();
        AssetFileDescriptor afd = null;

        try {
            afd = context.getAssets().openFd(path);
            player.setDataSource(afd.getFileDescriptor());
            player.prepare();
            return player;
        } catch (IOException e) {
            Log.e(TAG, "load Audio from Assets fail: " + e);
        }
        return null;
    }

    /**
     * <p>load a textfile and write a String</p>
     * @param context
     * @param path
     * @return Stringbuilder
     */
    public static StringBuilder loadTextFromAssets(Context context, String path) {
        InputStream is;
        StringBuilder sb = new StringBuilder();
        try {
            is = context.getAssets().open(path);
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            for (String line; (line = r.readLine()) != null; ) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb;
    }
    static Rect changeRect(Rect mMaxSensorSize, double mDivider) {
        int left = (int) (mMaxSensorSize.left + mMaxSensorSize.centerX() / mDivider);
        int top = (int) (mMaxSensorSize.top + mMaxSensorSize.centerY() / mDivider);
        int right = (int) (mMaxSensorSize.right - mMaxSensorSize.centerX() / mDivider);
        int bottom = (int) (mMaxSensorSize.bottom - mMaxSensorSize.centerY() / mDivider);
        return new Rect(left, top, right, bottom);
    }
    public static void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        /* this is to allow the camera operations to run on a separate thread and avoid blocking the UI*/
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        /* this is to communicate with the thread*/
    }
    public static void stopBackgroundThread() {
        if (mBackgroundThread != null) { return; }
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            throw new RuntimeException("InterruptedException: " + e);
        }
    }
    public void runMotion() {
        Runnable myRunnable = new Runnable() {
            int testByte = 0;
            public void run() {
                while (testByte == 10) {
                    try {
                        Thread.sleep(1000); // Waits for 1 second (1000 milliseconds)
                        Log.i(TAG, "try sleep: " + testByte);
                    } catch (InterruptedException e) {
                        Log.i(TAG, "InterruptedException " + e);
                        throw new RuntimeException(e);
                    }
                    Log.i(TAG, "testByte: " + testByte);
                    testByte++;
                }
            }
        };
    }
    static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState);
    }
    static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        return !Environment.MEDIA_MOUNTED.equals(extStorageState);
    }
}