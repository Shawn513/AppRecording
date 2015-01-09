package com.gce.wordrecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;


public class MainActivity extends Activity {

    private EditText ageField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ageField = (EditText) findViewById(R.id.field_age);

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
        EditText nameField = (EditText) findViewById(R.id.field_name);
        if (nameField.getText().toString().isEmpty()) {
            alert();
        } else {

            //  Get user's gender
            RadioGroup genderField = (RadioGroup) findViewById(R.id.field_gender);
            char gender = genderField.getCheckedRadioButtonId() == R.id.radio_female ? 'F' : 'M';

            Intent Record = new Intent(getApplicationContext(), RecordActivity.class);
            Record.putExtra("Name", nameField.getText().toString());
            Record.putExtra("Age", ageField.getText().toString());
            Record.putExtra("Gender", gender);
            startActivity(Record);
        }
    }


    void alert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.warning);
        builder.setTitle(R.string.warning);
        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();

            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
