package com.gce.wordrecorder;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;


public class RecordActivity extends Activity {

    private ArrayList<Word> wordSet;
    private Word currentWord;
    private boolean isRecording;
    private boolean played;
    private int index;

    private String name;
    private char gender;
    private String age;

    private Intent recordIntent;

    private Timestamp lastTimeStamp;
    private static final String model = Build.MODEL;
    private static final String filePath = Environment.getExternalStorageDirectory().getPath();

    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "WordRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";


    private TextView targetText;
    private ImageButton playButton;
    private ImageButton recordButton;
    private Button nextButton;

    private boolean recorded;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        // Retrieve user's personal information passed from main activity
        Intent intent = getIntent();
        name = intent.getStringExtra("Name");
        gender = intent.getCharExtra("Gender", 'M');
        age = intent.getStringExtra("Age");



        targetText = (TextView) findViewById(R.id.view_targetText);
        playButton = (ImageButton) findViewById(R.id.btn_playback);
        nextButton = (Button) findViewById(R.id.btn_next);
        recordButton = (ImageButton) findViewById(R.id.btn_record);

        if (savedInstanceState == null) {

            parseSource();
            // Display the first word on the list
            index = 0;
            currentWord = wordSet.get(index);
            targetText.setText(String.valueOf(currentWord.getWord()));
            playButton.setEnabled(false);
            nextButton.setEnabled(false);

        }

        recorded = false;
        played = false;
        recordIntent = new Intent(this, Recorder.class);

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

    @Override
    public void onRestart() {
        super.onRestart();
        if (isRecording) {
            isRecording = false;
            stopService(recordIntent);
            Toast.makeText(getApplicationContext(), "Previous record is invalid, please record again.", Toast.LENGTH_SHORT).show();
            recordButton.setImageResource(android.R.drawable.ic_btn_speak_now);
            System.out.println(getLastFilename());
            File file = new File(getLastFilename());
            file.delete();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_record);
        targetText = (TextView) findViewById(R.id.view_targetText);
        targetText.setText(String.valueOf(currentWord.getWord()));
        nextButton = (Button) findViewById(R.id.btn_next);
        playButton = (ImageButton) findViewById(R.id.btn_playback);
        recordButton = (ImageButton) findViewById(R.id.btn_record);

        if (isRecording) {
            recordButton.setImageResource(android.R.drawable.ic_media_pause);
        }
        if (!recorded) {
            nextButton.setEnabled(false);
            playButton.setEnabled(false);
        } else if (!played) {
            nextButton.setEnabled(false);
        }
    }

    public void toggleRecord(View v) {
        if (!isRecording) {
            playButton.setEnabled(false);
            nextButton.setEnabled(false);
            ((ImageButton)v).setImageResource(android.R.drawable.ic_media_pause);
            isRecording = true;
            if (recordIntent != null) {
                recordIntent.putExtra("Filename", getFilename());
                recordIntent.putExtra("TempFilename", getTempFilename());
                startService(recordIntent);
            }
            Toast.makeText(getApplicationContext(), "Start recording...", Toast.LENGTH_SHORT).show();
        } else {
            ((ImageButton)v).setImageResource(android.R.drawable.ic_btn_speak_now);
            stopService(recordIntent);
            playButton.setEnabled(true);
            isRecording = false;
            recorded = true;
            Toast.makeText(getApplicationContext(), "File Saved", Toast.LENGTH_SHORT).show();
        }
    }

    public void play(View v) {
        MediaPlayer player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            final ImageButton recordButton = (ImageButton) findViewById(R.id.btn_record);
            player.setDataSource(this, Uri.parse("file://" + getLastFilename()));
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    nextButton.setEnabled(true);
                    recordButton.setEnabled(true);
                    played = true;
                }
            });
            recordButton.setEnabled(false);
            nextButton.setEnabled(false);
            player.prepare();
            player.start();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void nextWord(View v) {

        if (index < wordSet.size() - 1) {
            recorded = false;
            played = false;
            index ++;
            currentWord = wordSet.get(index);
            TextView targetText = (TextView) findViewById(R.id.view_targetText);
            targetText.setText(String.valueOf(currentWord.getWord()));
            playButton.setEnabled(false);
            nextButton.setEnabled(false);
        } else {
            Intent intent = new Intent(getApplicationContext(), CompleteActivity.class);
            intent.putExtra("Name", name);
            intent.putExtra("Age", age);
            intent.putExtra("Gender", gender);
            startActivity(intent);
            finish();
        }

    }

    private String getFilename() {
        File file = new File(filePath, AUDIO_RECORDER_FOLDER);

        Timestamp timestamp = new Timestamp(new Date().getTime());
        lastTimeStamp = timestamp;
        if (!currentWord.getVowel().isEmpty()) {
            return (file.getAbsolutePath() + "/" + currentWord.getVowel() + "_" + currentWord.getConsonant() + "_" + name + "_" + gender + "_" + age + "_" + model + "_" + timestamp + AUDIO_RECORDER_FILE_EXT_WAV);
        } else {
            return (file.getAbsolutePath() + "/" + currentWord.getConsonant() + "_" + name + "_" + gender + "_" + age + "_" + model + "_" + timestamp + AUDIO_RECORDER_FILE_EXT_WAV);
        }
    }

    private String getLastFilename() {
        File file = new File(filePath,AUDIO_RECORDER_FOLDER);

        if (!currentWord.getVowel().isEmpty()) {
            return (file.getAbsolutePath() + "/" + currentWord.getVowel() + "_" + currentWord.getConsonant() + "_" + name + "_" + gender + "_" + age + "_" + model + "_" + lastTimeStamp + AUDIO_RECORDER_FILE_EXT_WAV);
        } else {
            return (file.getAbsolutePath() + "/" + currentWord.getConsonant() + "_" + name + "_" + gender + "_" + age + "_" + model + "_" + lastTimeStamp + AUDIO_RECORDER_FILE_EXT_WAV);
        }
    }

    private String getTempFilename(){

        File tempFile = new File(filePath+"/"+AUDIO_RECORDER_FOLDER, AUDIO_RECORDER_TEMP_FILE);

        System.out.println(tempFile.getAbsolutePath());
        return tempFile.getAbsolutePath();

    }

    private void parseSource() {
        //  Randomly select a word set for the user
        ArrayList<String> fileList = getIntent().getStringArrayListExtra("FileList");
        System.out.println("Size " + fileList.size());
        int setNumber = new Random().nextInt(fileList.size());

        wordSet = new ArrayList<>();

        try {
            FileInputStream input = openFileInput(fileList.get(setNumber));
            byte[] data = new byte[input.available()];
            input.read(data);
            input.close();
            String dataString = new String(data, "UTF-8");
            JSONArray wordArr = new JSONObject(dataString).getJSONArray("words");

            for (int i = 0; i < wordArr.length(); ++ i) {
                String vowel;
                String consonant;
                char character;

                JSONObject wordObj = wordArr.getJSONObject(i);
                if (wordObj.has("vowel")) {
                    vowel = wordObj.getString("vowel");
                } else {
                    vowel = "";
                }
                consonant = wordObj.getString("consonant");
                character = wordObj.getString("char").charAt(0);
                Word word = new Word(character, vowel, consonant);
                wordSet.add(word);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }
}
