/*
 * Copyright (C) 2018 Potato Open Sauce Project
 * Copyright (C) 2021 Jyotiraditya Panda <jyotiraditya@aospa.co>
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

package com.android.internal.util;

import android.os.Handler;
import android.os.HandlerThread;

import java.io.OutputStreamWriter;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Helper functions for uploading to MkrBin (https://bin.mkr.pw/).
 */
public final class MkrBinUtils {

    private static final String binUrl = "https://bin.mkr.pw";
    private static Handler mHandler;

    /**
     * Uploads {@code content} to MkrBin
     *
     * @param content  the content to upload to MkrBin
     * @param callback the callback to call on success / failure
     */
    public static void upload(String content, UploadResultCallback callback) {
        getHandler().post(() -> {
            HttpsURLConnection urlConnection = null;
            try {
                URL url = new URL(binUrl);
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(5000);
                urlConnection.setRequestProperty("Content-Type", "text/plain");
                urlConnection.setInstanceFollowRedirects(false);
                urlConnection.setDoOutput(true);

                try (OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream())) {
                    writer.write(content);
                    writer.flush();
                }

                int responseCode = urlConnection.getResponseCode();
                String urlPath = "";
                if (responseCode == HttpsURLConnection.HTTP_MOVED_TEMP
                        || responseCode == HttpsURLConnection.HTTP_MOVED_PERM
                        || responseCode == HttpsURLConnection.HTTP_SEE_OTHER) {
                    urlPath = urlConnection.getHeaderField("Location");
                }

                if (urlPath != null && !urlPath.isEmpty()) {
                    String fullUrl = urlPath.startsWith("http") ? urlPath : binUrl + urlPath;
                    callback.onSuccess(fullUrl);
                } else {
                    String msg = "Failed to upload to MkrBin: HTTP " + responseCode + " (No id retrieved)";
                    callback.onFail(msg, new Exception(msg));
                }
            } catch (Exception e) {
                String msg = "Failed to upload to MkrBin";
                callback.onFail(msg, e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        });
    }

    private static Handler getHandler() {
        if (mHandler == null) {
            HandlerThread mkrBinThread = new HandlerThread("MkrBinThread");
            if (!mkrBinThread.isAlive()) {
                mkrBinThread.start();
            }
            mHandler = new Handler(mkrBinThread.getLooper());
        }
        return mHandler;
    }

    public interface UploadResultCallback {
        void onSuccess(String url);

        void onFail(String message, Exception e);
    }
}
