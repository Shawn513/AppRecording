package com.gce.wordrecorder;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by hang on 1/17/2015.
 */
public class DownLoadFileTask extends AsyncTask<URL, Integer, Boolean> {

        private String[] filenames;
        private ProgressDialog progressDialog;
        private MainActivity context;
        private boolean config;

        public DownLoadFileTask(MainActivity context, boolean config, String... filenames) {
            super();
            this.filenames = filenames;
            this.context = context;
            this.config = config;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context);
            progressDialog.setCancelable(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            if (config) {
                progressDialog.setMessage("Downloading configuration files");
            } else {
                progressDialog.setMessage("Downloading data files");
            }
            progressDialog.setTitle("Downloading configuration files");
            progressDialog.show();
        }


        @Override
        protected void onPostExecute(Boolean successDownload) {
            super.onPostExecute(successDownload);
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
                if (config) {
                    // context.updating();
                }
            }
            if (successDownload == false) {
                context.alert("Error downloading files");
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressDialog.setProgress(values[0]);
        }

        @Override
        protected Boolean doInBackground(URL... params) {
             boolean successDownload = true;
            for (int i = 0; i < params.length; ++ i) {
                try {
                    HttpURLConnection connection = (HttpURLConnection) params[i].openConnection();
                    connection.connect();
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        return false;
                    }
                     // maybe select a new way to downloaded the file instead of using the bufferwriter;
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(context.openFileOutput(filenames[i], Context.MODE_PRIVATE)));
                    String content;

                    while ((content = reader.readLine()) != null) {
                        writer.write(content);
                    }
                    writer.close();
                    reader.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

            }
            return successDownload ;
        }
    }

