package com.gce.wordrecorder;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;


public class RecordActivity extends Activity {

    private ArrayList<Word> wordSet;
    private Word currentWord;
    private AudioRecord recorder;
    private Thread recordingThread;
    private boolean isRecording;
    private int index;

    private String name;
    private char gender;
    private String age;

    private Timestamp lastTimeStamp;
    private static final String model = Build.MODEL;
    private static final String filePath = Environment.getExternalStorageDirectory().getPath();

    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "WordRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLE_RATE = 16000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferSize = 0;

    private ArrayList<Integer> selected;

    private ImageButton playButton;
    private Button nextButton;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        Intent intent = getIntent();
        if (intent.hasExtra("Selected")) {
            selected = intent.getIntegerArrayListExtra("Selected");
        } else {
            selected = new ArrayList<>();
        }

        parseSource();

        // Retrieve user's personal information passed from main activity
        name = intent.getStringExtra("Name");
        gender = intent.getCharExtra("Gender", 'M');
        age = intent.getStringExtra("Age");




        // Display the first word on the list
        index = 0;
        currentWord = wordSet.get(index);
        TextView targetText = (TextView) findViewById(R.id.view_targetText);
        targetText.setText(String.valueOf(currentWord.getWord()));

        playButton = (ImageButton) findViewById(R.id.btn_playback);
        playButton.setEnabled(false);
        nextButton = (Button) findViewById(R.id.btn_next);
        nextButton.setEnabled(false);

        //  Set the buffer size for the
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
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

    //  Back button is set to go to home page instead of the personal information form



    public void toggleRecord(View v) {
        if (!isRecording) {
            playButton.setEnabled(false);
            nextButton.setEnabled(false);
            ((ImageButton)v).setImageResource(android.R.drawable.ic_media_pause);
            startRecording();
        } else {
            ((ImageButton)v).setImageResource(android.R.drawable.ic_btn_speak_now);
            stopRecording();
            playButton.setEnabled(true);
            nextButton.setEnabled(true);
        }
    }

    public void play(View v) {
        MediaPlayer player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            player.setDataSource(this, Uri.parse("file://" + getLastFilename()));
            player.prepare();
            player.start();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void nextWord(View v) {

        if (index < wordSet.size() - 1) {
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
            intent.putIntegerArrayListExtra("Selected", selected);
            startActivity(intent);
        }

    }

    private void startRecording() {
        deleteTempFile();
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLE_RATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);

        int i = recorder.getState();
        if(i==1)
            recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                writeAudioDataToFile();
            }
        },"AudioRecord Thread");

        recordingThread.start();

        Toast.makeText(getApplicationContext(), "Start recording...", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        if(recorder != null){
            isRecording = false;

            int i = recorder.getState();
            if(i==1)
                recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }
        copyWaveFile(getTempFilename(),getFilename());
        deleteTempFile();

        Toast.makeText(getApplicationContext(), "File saved", Toast.LENGTH_SHORT).show();
    }


    // Writing the raw audio data to a temporary file
    private void writeAudioDataToFile() {
        byte[] data = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int read;

        if(null != os){
            while(isRecording){
                read = recorder.read(data, 0, bufferSize);

                if(read != AudioRecord.ERROR_INVALID_OPERATION){
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());

        file.delete();

    }

    //  Process the raw audio data and copy its content to the final wave file
    private void copyWaveFile(String inFilename,String outFilename){
        FileInputStream in;
        FileOutputStream out;
        long totalAudioLen;
        long totalDataLen;
        long longSampleRate = RECORDER_SAMPLE_RATE;
        int channels = 1;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLE_RATE * channels/8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            //AppLog.logString("File size: " + totalDataLen);

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while(in.read(data) != -1){
                out.write(data);
            }

            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //  Write the wave file header to the output stream
    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    private String getFilename(){
        File file = new File(filePath,AUDIO_RECORDER_FOLDER);


        if(!file.exists()){
            file.mkdirs();
        }
        Timestamp timestamp = new Timestamp(new Date().getTime());
        lastTimeStamp = timestamp;
        return (file.getAbsolutePath() + "/" + currentWord.getVowel() + "_" + currentWord.getConsonant() + "_" + name + "_" + gender + "_" + age + "_" + model + "_" + timestamp + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private String getLastFilename() {
        File file = new File(filePath,AUDIO_RECORDER_FOLDER);

        file.mkdirs();
        return (file.getAbsolutePath() + "/" + currentWord.getVowel() + "_" + currentWord.getConsonant() + "_" + name + "_" + gender + "_" + age + "_" + model + "_" + lastTimeStamp + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private String getTempFilename(){
        File file = new File(filePath,AUDIO_RECORDER_FOLDER);

        file.mkdirs();

        File tempFile = new File(filePath,AUDIO_RECORDER_TEMP_FILE);

        if(tempFile.exists()) {
            tempFile.delete();
        }

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    //  Parse the source xml containing the words and store it in an ArrayList of words
    private void parseSource() {

        //  Randomly select a word set for the user
        int setNumber;
        do {
            setNumber = new Random().nextInt(14);
        } while(selected.contains(setNumber));

        TypedArray setIDs = getResources().obtainTypedArray(R.array.wordSets);
        XmlPullParser parser = getResources().getXml(setIDs.getResourceId(setNumber, -1));

        // Parse the xml resource (Subject to changes)
        String character = null;
        String vowel = null;
        String consonant = null;
        String text = null;
        Word word;
        wordSet = new ArrayList<>();

        int eventType = -1;
        try {
            eventType = parser.getEventType();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.END_TAG) {
                String tagName = parser.getName().toLowerCase();
                switch (tagName) {
                    case "word":
                        System.out.println("Vowel: " + vowel);
                        word = new Word(character.charAt(0), vowel, consonant);
                        wordSet.add(word);
                        break;
                    case "char":
                        character = text;
                        break;
                    case "vowel":
                        vowel = text;
                        break;
                    case "consonant":
                        consonant = text;
                        break;
                }
            } else if (eventType == XmlPullParser.TEXT) {
                text = parser.getText();
            }

            try {
                eventType = parser.next();
            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            }
        }

        selected.add(setNumber);
    }
}
