package com.polytechnique.marc.amaury.set;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;


public class ParametreActivity extends AppCompatActivity {


    public ParametreActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parametre);
        final TextView pseudoTextView = (TextView) findViewById(R.id.pseudoText);
        pseudoTextView.setText("Pseudonyme");
        final TextView ipTextView = (TextView) findViewById(R.id.ipText);
        ipTextView.setText("Adresse IP");
        final EditText ipEditText = (EditText) findViewById(R.id.adresseIp);
        final EditText pseudoEditText = (EditText) findViewById(R.id.pseudo);

        Context context = getActivity();
        pseudoEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.preference_file_key),0);
                    sharedPref.edit().putString(getString(R.string.pseudo),  pseudoEditText.getText().toString()).apply();

                }
            }
        });

        ipEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.preference_file_key),0);
                    sharedPref.edit().putString(getString(R.string.ip),  ipEditText.getText().toString()).apply();

                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.preference_file_key),0);
        String lePseudoPrecedent = sharedPref.getString(getString(R.string.pseudo),getResources().getString(R.string.pseudo));
        String leipPrecedent = sharedPref.getString(getString(R.string.ip),getResources().getString(R.string.ip));

        final EditText ipEditText = (EditText) findViewById(R.id.adresseIp);
        final EditText pseudoEditText = (EditText) findViewById(R.id.pseudo);
        pseudoEditText.setText(lePseudoPrecedent);
        ipEditText.setText(leipPrecedent);

        // TODO: code s'executant au debut de l'application


    }

    public void onPause(){
        super.onPause();

        final EditText ipEditText = (EditText) findViewById(R.id.adresseIp);
        final EditText pseudoEditText = (EditText) findViewById(R.id.pseudo);

        SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.preference_file_key),0);

        sharedPref.edit().putString(getString(R.string.ip),  ipEditText.getText().toString()).apply();

        sharedPref.edit().putString(getString(R.string.pseudo),  pseudoEditText.getText().toString()).apply();
    }


    public Activity getActivity() {
        return this;
    }

    public void retour(final View v){
        final EditText ipEditText = (EditText) findViewById(R.id.adresseIp);
        final EditText pseudoEditText = (EditText) findViewById(R.id.pseudo);

        SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.preference_file_key),0);

        sharedPref.edit().putString(getString(R.string.ip),  ipEditText.getText().toString()).apply();

        sharedPref.edit().putString(getString(R.string.pseudo),  pseudoEditText.getText().toString()).apply();
        this.finish();
    }

    public void restartConnection(final View v){
        final EditText ipEditText = (EditText) findViewById(R.id.adresseIp);
        final EditText pseudoEditText = (EditText) findViewById(R.id.pseudo);



        SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.preference_file_key),0);

        sharedPref.edit().putString(getString(R.string.restart), "true").apply();


        sharedPref.edit().putString(getString(R.string.ip),  ipEditText.getText().toString()).apply();

        sharedPref.edit().putString(getString(R.string.pseudo),  pseudoEditText.getText().toString()).apply();
        this.finish();
    }
}
