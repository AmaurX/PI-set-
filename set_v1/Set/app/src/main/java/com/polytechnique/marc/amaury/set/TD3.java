package com.polytechnique.marc.amaury.set;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.os.Handler;
import java.io.*;
import java.net.*;
import java.util.*; // for Scanner

public class TD3 extends Activity {
    // Handler pour pouvoir appeler le tread principal gerant
    // l'interface graphique.
    final Handler callback = new Handler();
    // Nombre de boites de texte definies dans main.xml.
    final static int nlabels = 6;
    // Tableau stockant les messages a afficher dans les 6 boites.
    private String[] infos = new String[nlabels];
    // Tableau stockant les ids des 6 boites de texte pour l'affichage des messages.
    private int[] ids = new int[nlabels];

    // Objet contenant le code a executer pour mettre a jour les 6 boites de texte.
    final Runnable update_infos = new Runnable() {
            public void run() {
                for (int i = 0; i < nlabels; i++) {
                    // Recupere un pointeur sur un objet EditText.
                    EditText txt_info = (EditText)findViewById(ids[i]);
                    // Affiche le message stocke dans le tableau info.
                    txt_info.setText(infos[i]);
                }
            }
	};

    // Methode effectuant le decalage des messages avant leur affichage
    // Et declenchant le rafraichissement de l'affichage.
    void displayMessage(String s){
        for (int i=0; i < nlabels-1; i++) 
            infos[i] = infos[i+1];
        infos[nlabels-1] = s;
        callback.post(update_infos);
    }
	
    /** Methode appelee lors de la creation de l'Activity */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Appel de la methode de base Activity.onCreate.
        super.onCreate(savedInstanceState);
        /*
        // Sauvegarde des id de chacun des elements graphiques EditText dans le tableau 'id'.
        ids[0] = R.id.txt1;
        ids[1] = R.id.txt2;
        ids[2] = R.id.txt3;
        ids[3] = R.id.txt4;
        ids[4] = R.id.txt5;
        ids[5] = R.id.txt6;
        assert (nlabels == 6);

        // Initialise le tableau contenant les messages a afficher.
        for (int i = 0; i < nlabels; i++) 
            infos[i] = "";

        // Affichage du layout XML dans l'ecran associe a notre Activity.
        setContentView(R.layout.main);

        // Recupere un pointeur sur le bouton, et enregistre la commande a executer 
        // quand on clique dessus.
        final Button button = (Button) findViewById(R.id.send_button);
        button.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    // Recupere un pointeur sur le EditText contenant le message rentre.
                    EditText txt_info = (EditText) findViewById(R.id.editor);
                    // Stocke le contenu de cette boite (le message).
                    String message = txt_info.getText().toString();
                    // Efface l'EditText.
                    txt_info.setText("");
                    // Envoi du message.
                    sendMessage(message);
                }
            });*/
    }

    @Override
    public void onStart() {
        super.onStart();
        // @TODO: code s'executant au debut de l'application.
    }
    
    private void sendMessage(String message) { 
        // @TODO: code s'executant quand l'utilisateur clique le bouton Send
        // pour envoyer un nouveau message.
        displayMessage(message);
    }
	
}
