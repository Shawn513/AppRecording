package com.gce.wordrecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;



public class MainActivity extends Activity {

    private EditText ageField;
    private EditText nameField ;
    private RadioGroup genderField;
    private ArrayList<String> fileList;
    private ArrayList<URL> urls;
    private static final String AUDIO_RECORDER_FOLDER = "WordRecorder";
    private static String sdPath = Environment.getExternalStorageDirectory().getPath();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String filePath = sdPath + "/" + AUDIO_RECORDER_FOLDER;
        boolean update;
        fileList = new ArrayList<>();
        urls = new ArrayList<>();
        File app_folder = new File(filePath);
        if (!app_folder.exists()) {
            app_folder.mkdirs();
        }

        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // if no internet
        if (manager.getActiveNetworkInfo() == null) {
            if (!checkIntegrity()) {
                alert("Error loading configuration files");
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        }
        // else have internet connection first download the contents file
        else  {
            try {
                System.out.println("Needs update");
                URL website = new URL("http://ihome.ust.hk/~hpsuenaa/contents.json");
                DownLoadFileTask taskDownLoadContents = new DownLoadFileTask(this, true, "contents_new.json");
                boolean contentDownloaded=taskDownLoadContents.execute(website).get();
                update = checkingUpdate();
                if (contentDownloaded && update) {
                    // do the checking whether the version matches;
                DownLoadFileTask taskDownloadFiles = new DownLoadFileTask(this, false,fileList.toArray(new String[fileList.size()]));
                boolean fileDownloaded = taskDownloadFiles.execute(urls.toArray(new URL[urls.size()])).get();
                    if(fileDownloaded){
                        // change the contents_new to content_current if possible
                        // so that the current and new version of contents would not conflict;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        ageField = (EditText) findViewById(R.id.field_age);
        nameField = (EditText) findViewById(R.id.field_name);
        genderField = (RadioGroup) findViewById(R.id.field_gender);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_record, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean checkingUpdate() {
        FileInputStream newIn;
        boolean requireUpdate = false,fileExist;
        try {
            newIn = openFileInput("contents_new.json");
            byte[] buffer = new byte[newIn.available()];
            newIn.read(buffer);
            String config = new String(buffer, "UTF-8");
            JSONArray configArr = new JSONArray(config);
            newIn.close();
            JSONObject obj;


            for (int i = 0; i < configArr.length(); ++i) {

                obj = configArr.getJSONObject(i);
                String filename = obj.getString("filename");
                String latestVersion = obj.getString("version");

                File existingFile = new File(getFilesDir(), filename);
                fileExist = existingFile.exists();

                if (fileExist) {
                    FileInputStream checkIn = openFileInput(filename);
                    byte[] data = new byte[checkIn.available()];
                    checkIn.read(data);
                    checkIn.close();
                    String dataString = new String(data, "UTF-8");
                    JSONObject currentWordSet = new JSONObject(dataString);
                    System.out.println(filename + " Version " + currentWordSet.getString("version"));
                    requireUpdate = !currentWordSet.getString("version").equals(latestVersion);
                }else {
                    requireUpdate = true ;
                }

                // Download only if a newer version is available
                if (requireUpdate) {
                    System.out.println("Updating " + filename);
                    URL fileURL = new URL("http://ihome.ust.hk/~hpsuenaa/" + filename);
                    urls.add(fileURL);
                    fileList.add(filename);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
         return requireUpdate;
    }

    public void minusAge(View v) {
        int currentAge = Integer.parseInt(ageField.getText().toString());
        if (currentAge > 4) {
            ageField.setText(String.valueOf(currentAge - 1));
        }
    }

    public void plusAge(View v) {
        int currentAge = Integer.parseInt(ageField.getText().toString());
        if (currentAge < 100) {
            ageField.setText(String.valueOf(currentAge + 1));
        }
    }

    public void complete(View v) {
        if (nameField.getText().toString().isEmpty()) {
            alert(getResources().getString(R.string.warning));
        } else {
            //  Get user's gender
            char gender = genderField.getCheckedRadioButtonId() == R.id.radio_female ? 'F' : 'M';

            Intent Record = new Intent(getApplicationContext(), RecordActivity.class);
            Record.putExtra("Name", nameField.getText().toString());
            Record.putExtra("Age", ageField.getText().toString());
            Record.putExtra("Gender", gender);
            Record.putStringArrayListExtra("FileList", fileList);
            startActivity(Record);
            finish();
        }
    }


    public void alert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setTitle(message);
        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();

            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private boolean checkIntegrity() {
        // work when no internet connection, checking the files
        File existingFile = new File(getFilesDir(), "contents_current.json");
        if (!existingFile.exists()) {
            return false;
        } else {
            try {
                FileInputStream input = openFileInput("contents_new.json");
                System.out.println("Available" + input.available());// to be removed
                byte[] buffer = new byte[input.available()];
                input.read(buffer);
                input.close();
                String bufferString = new String(buffer, "UTF-8");
                JSONArray array = new JSONArray(bufferString);
                JSONObject setInfo;
                int i = 0;
                boolean mismatch = false;
                FileInputStream wordSetStream;
                System.out.println("Length " + array.length());
                while (i < array.length() && !mismatch) {
                    setInfo = array.getJSONObject(i);
                    String filename = setInfo.getString("filename");
                    File file = new File(getFilesDir(), filename);
                    if (!file.exists()) {
                        return false;
                    }
                    wordSetStream = openFileInput(filename);
                    byte[] data = new byte[wordSetStream.available()];
                    wordSetStream.read(data);
                    wordSetStream.close();
                    String dataString = new String(data, "UTF-8");
                    String currentVersion  = new JSONObject(dataString).getString("version");
                    if (!setInfo.getString("version").equals(currentVersion)) {
                        return false;
                    }
                    fileList.add(filename);
                    ++i;
                }
            } catch (IOException|JSONException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

}
