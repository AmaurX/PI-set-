package com.polytechnique.marc.amaury.set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity {

    ImageView set;

    ReentrantLock lock;
    String debug = "Hello";
    String my_login = "default";
    String ip_adresse = "default";
    public static AtomicInteger N =new AtomicInteger(0);
    Thread connectionThread;
    static PrintWriter server_out = null;
    static PrintWriter telnet_out = null;
    //Pour mettre en marche le multijoueur

    static Boolean multiJoueur = false;
    Condition wait;

    private boolean[] deck = new boolean[81];
    public int[] table = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1};   //lie le numéro de la carte( 1à 15) avec sa valeur en tant que card
    //si la carte vaux -1 elle n'existe pas, cf modification de isSet
    @SuppressLint("UseSparseArrays")
    private HashMap<Integer, Integer> tas = new HashMap<>();    //lie l'adresse au numéro de la carte (1 a 15)
    private Integer[] listeDesAdresses = new Integer[15];
    private int nbCarte = 12;
    @SuppressLint("UseSparseArrays")
    private HashMap<Integer, CardDrawable> carteSurTable = new HashMap<>();
    private volatile Stack<Integer> selected = new Stack<>();

    Integer trou1;
    Integer trou2;
    Integer trou3;

    TextView timerTextView;
    TextView scoreTextView;
    TextView debugTextView;

    long startTime = 0;

    long score = -1;
    boolean add = true;

    Integer addresse;
    int numeroCarteSet = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lock = new ReentrantLock();
        init();
        testMatch();

        TextView parametre = (TextView)  findViewById(R.id.parametres);
        parametre.setText(R.string.parametres);
        debugTextView = (TextView) findViewById(R.id.debug);
        debug = "on create";
        debugHandler.postDelayed(debugRunnable, 0);
        final SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.preference_file_key),0);
        my_login = sharedPref.getString(getString(R.string.pseudo),getResources().getString(R.string.pseudo));
        ip_adresse = my_login = sharedPref.getString(getString(R.string.ip),getResources().getString(R.string.ip));
        connectionThread = new Thread(connection);
        connectionThread.start();
        wait = lock.newCondition();


    }

    @Override
    public void onStart() {
        super.onStart();
        final SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.preference_file_key),0);
        String restart =  sharedPref.getString(getString(R.string.restart),getResources().getString(R.string.restart));
        my_login = sharedPref.getString(getString(R.string.pseudo),getResources().getString(R.string.pseudo));
        ip_adresse =  sharedPref.getString(getString(R.string.ip),getResources().getString(R.string.ip));

        if(restart.equals("true")){
            restartConnection();
            sharedPref.edit().putString(getString(R.string.restart), "false").apply();
        }


    }

    public void restartConnection(){
        try{
            sendMessage("LOGOUT");
            connectionThread.interrupt();
            connectionThread = null;
            connectionThread = new Thread(connection);
            connectionThread.start();
        }
        catch (RuntimeException e){
            System.out.println(e.toString());
        }

    }



    // Variables stockant la valeur courante de l'etat memoire et cpu.

    // Objet contenant le code a executer pour mettre a jour la boite
    // de texte contenant la memoire disponible.


    void displayMessage(String s) {
        debug = s;
        runOnUiThread(debugRunnable);
    }



    Runnable connection = new Runnable() {
        @Override
        public void run() {
            Socket s;
            try {
                s = Net.establishConnection(ip_adresse, 1709);   //Marc: 192.168.0.11    Amaury: 192.168.1.15
                displayMessage("CONNECTED");
            } catch (RuntimeException e) {
                displayMessage("Unconnected: " + e.toString());
                return;
            }
            System.out.println("débug : après la connection");

            PrintWriter s_out = Net.connectionOut(s);
            final BufferedReader s_in = Net.connectionIn(s);
            s_out.println("LOGIN/" + my_login);
            String line;
            System.out.println("débug : avant de lire une ligne");
            try {
                line = s_in.readLine();
                System.out.println("débug : j'ai lu une ligne");
            } catch (IOException e) {
                throw new RuntimeException("in readLine");
            }

            displayMessage(line);
            Scanner sc = new Scanner(line);
            sc.useDelimiter(" ");
            if (sc.next().equals("Welcome")) {
                displayMessage("ACCEPTED");
                multiJoueur = true;
                server_out = s_out;
                s_out.println("GAMEPLEASE/");
            }
            final Thread from_server = new Thread() {

                void traiterMessageServeur(String line) {
                    if (line == null) {
                        return;
                    }
                    Scanner sc = new Scanner(line);
                    sc.useDelimiter("/");

                    String message = sc.next();

                    switch (message) {
                        case "theGame":
                            System.out.println("On est dans theGame");
                            int numDeLaCarte;
                            int i = 0;
                            if (sc.hasNext()) {   //Cette étape ne semble aps se faire correctement
                                N.set(Integer.parseInt(sc.next()));
                            } else {
                                displayMessage("Le game est incomplet");
                            }
                            while (sc.hasNext() && i < 15) {
                                numDeLaCarte = sc.nextInt();
                                if (numDeLaCarte != -1) {
                                    table[i] = numeroDeCarteToK(numDeLaCarte);
                                } else {
                                    table[i] = -1;
                                }
                                i++;
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mettreAJour();
                                }
                            });
                            selected.empty();
                            lock.lock();
                            wait.signalAll();
                            lock.unlock();

                            break;
                        case "result": {
                            System.out.println("On change le score");
                            Integer res = 0;
                            if (sc.hasNext()) {
                                res = sc.nextInt();
                            } else {
                                displayMessage("Le score est invalide");
                            }
                            score = res;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    scoreTextView.setText(String.format("%s:%02d", "Score", score));
                                }
                            });

                            break;
                        }
                        case "scores": {
                            System.out.println("On récupère les scores");
                            String res = "";
                            int count = 0;
                            while (sc.hasNext()) {
                                res += sc.next();
                                if (count == 0) {
                                    res += " : ";
                                    count = 1;
                                } else {
                                    if (sc.hasNext()) {
                                        res += " - ";
                                    }
                                    count = 0;
                                }
                            }

                            displayMessage(res);
                            break;
                        }
                    }

                }

                public void run() {
                    System.out.println("débug : dans le thread from server");
                    String line;
                    while (true) {
                        try {
                            line = s_in.readLine();
                            if(line!=null) {
                                System.out.println("débug : " + line);
                                traiterMessageServeur(line);
                                if (telnet_out != null)
                                    telnet_out.println(line);
                            }
                        } catch (IOException e) {
                            //throw new RuntimeException("in readLine - 2");
                            displayMessage("disconnection from serveur");
                            multiJoueur = false;
                            break;
                        }

                    }
                }
            };

            if(my_login.equals(getString(R.string.pseudo))) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(MainActivity.this, ParametreActivity.class);
                        startActivity(intent);

                    }
                });
            }
            from_server.start();

        }
    };



    public void mettreAJour(){
        //mettre a jour carteSurTable, nbCartes, trou1 2 et 3, selected;
        int count = 0;
        for(int i = 0; i <15; i++){
            if(table[i]!=-1){
                int adresse = listeDesAdresses[i];
                ImageView button = (ImageView) findViewById(adresse);
                CardDrawable nouvelleCard = new CardDrawable(table[i], Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888));
                nouvelleCard.customDraw();
                button.setImageDrawable(nouvelleCard);
                carteSurTable.put(adresse, nouvelleCard);
                button.invalidate();
            }
            else{
                int adresse = listeDesAdresses[i];

                carteSurTable.put(addresse, null);
                ImageView carte1 = (ImageView) findViewById(adresse);
                carte1.setImageDrawable(null);
                carte1.invalidate();
                if(count == 0){
                    trou1 = adresse;
                    count = 1;
                }
                else if(count == 1){
                    trou2 = adresse;
                    count = 2;
                }
                else{
                    trou3 = adresse;
                }
            }

        }
    }

    private void sendMessage(String message) {
        // pour envoyer un nouveau message
        server_out.println("SEND " + message);

    }



    //Time opérationel
    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            timerTextView.setText(String.format("%d:%02d", minutes, seconds));

            timerHandler.postDelayed(this, 500);
        }
    };



    //Score opérationnel
    Handler scoreHandler = new Handler();
    Runnable scoreRunnable = new Runnable() {
        @Override
        public void run() {
            if (add) score += 1;
            else score -= 1;
            scoreTextView.setText(String.format("%s:%02d", "Score", score));
        }
    };

    Handler debugHandler = new Handler();
    Runnable debugRunnable = new Runnable() {
        @Override
        public void run() {
            debugTextView.setText(String.format("%s", debug));
        }
    };
/*
    Handler setHandler = new Handler();
    Runnable setRunnable = new Runnable() {
        @Override
        public void run() {
            Bitmap bit = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
            CardDrawable nouvelleCard = new CardDrawable(171, bit);
            //CardDrawable nouvelleCard = new CardDrawable(table[tas.get(addresse)]);


            switch (numeroCarteSet) {
                case 0:
                    break;
                case 1:
                    set1.setImageDrawable(nouvelleCard);
                    nouvelleCard.customDraw();
                    set1.invalidate();
                    break;
                case 2:
                    set2.setImageDrawable(nouvelleCard);
                    nouvelleCard.customDraw();
                    set2.invalidate();
                    break;
                case 3:
                    set3.setImageDrawable(nouvelleCard);
                    nouvelleCard.customDraw();
                    set3.invalidate();
                    break;
                default:
                    break;
            }
        }
    };
*/

    public void init() {

        set = (ImageView) findViewById(R.id.Set);
        numeroCarteSet = 1;

        //setHandler.postDelayed(setRunnable, 1);
        numeroCarteSet = 0;
        scoreTextView = (TextView) findViewById(R.id.score);
        scoreHandler.postDelayed(scoreRunnable, 0);
        timerTextView = (TextView) findViewById(R.id.time);
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);

        tas.put(R.id.image1, 1);
        tas.put(R.id.image2, 2);
        tas.put(R.id.image3, 3);
        tas.put(R.id.image4, 4);
        tas.put(R.id.image5, 5);
        tas.put(R.id.image6, 6);
        tas.put(R.id.image7, 7);
        tas.put(R.id.image8, 8);
        tas.put(R.id.image9, 9);
        tas.put(R.id.image10, 10);
        tas.put(R.id.image11, 11);
        tas.put(R.id.image12, 12);
        tas.put(R.id.image13, 13);
        tas.put(R.id.image14, 14);
        tas.put(R.id.image15, 15);
        listeDesAdresses[0]=R.id.image1;
        listeDesAdresses[1]=R.id.image2;
        listeDesAdresses[2]=R.id.image3;
        listeDesAdresses[3]=R.id.image4;
        listeDesAdresses[4]=R.id.image5;
        listeDesAdresses[5]=R.id.image6;
        listeDesAdresses[6]=R.id.image7;
        listeDesAdresses[7]=R.id.image8;
        listeDesAdresses[8]=R.id.image9;
        listeDesAdresses[9]=R.id.image10;
        listeDesAdresses[10]=R.id.image11;
        listeDesAdresses[11]=R.id.image12;
        listeDesAdresses[12]=R.id.image13;
        listeDesAdresses[13]=R.id.image14;
        listeDesAdresses[14]=R.id.image15;
        trou1 = R.id.image13;
        trou2 = R.id.image14;
        trou3 = R.id.image15;
        for (Integer addresse : tas.keySet()) {
            if (!(addresse.equals(R.id.image15) || addresse.equals(R.id.image14) || addresse.equals(R.id.image13))) {
                addCard(addresse);
            }
        }
    }

    public void addCard(int adresse) {
        Random tirage = new Random();
        boolean flag = true;
        int k = 0;
        int numberOfTheCard = 0;
        while (flag) {
            int a = (tirage.nextInt(3) + 1);
            int b = (tirage.nextInt(3) + 1);
            int c = (tirage.nextInt(3) + 1);
            int d = (tirage.nextInt(3) + 1);
            k = a + 4 * b + 16 * c + 64 * d;
            numberOfTheCard = (a - 1) + 3 * (b - 1) + 9 * (c - 1) + 27 * (d - 1);
            flag = deck[numberOfTheCard];
        }
        deck[numberOfTheCard] = true;
        table[tas.get(adresse) - 1] = k;
        ImageView button = (ImageView) findViewById(adresse);

        CardDrawable nouvelleCard = new CardDrawable(k, Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888));
        nouvelleCard.customDraw();
        button.setImageDrawable(nouvelleCard);
        carteSurTable.put(adresse, nouvelleCard);
        button.invalidate(); //Etape pour réinitialiser une ImageView

        if (multiJoueur) {
            server_out.println("NEWCARD " + adresse + " " + numberOfTheCard);
        }

    }

    public int numeroDeCarteToK(int numeroDeCarte) {
        int a = numeroDeCarte % 3;
        int b = (numeroDeCarte - a) / 3 % 3;
        int c = (numeroDeCarte - a - 3 * b) / 9 % 3;
        int d = (numeroDeCarte - a - 3 * b - 9 * c) / 27 % 3;
        return ((a + 1) + 4 * (b + 1) + 16 * (c + 1) + 64 * (d + 1));
    }

    public int kToNumeroDeCarte(int k) {
        int a = k % 4;
        int b = (k - a) / 4 % 4;
        int c = (k - a - 4 * b) / 16 % 4;
        int d = (k - a - 4 * b - 16 * c) / 64 % 4;
        return ((a - 1) + 3 * (b - 1) + 9 * (c - 1) + 27 * (d - 1));
    }

    public boolean isThereMatch() {

        for (int card1 : table) {
            for (int card2 : table) {
                if (card1 == card2) continue;
                for (int card3 : table) {
                    if (card1 == card3 || card2 == card3) continue;
                    if (Cards.isSet(card1, card2, card3)) return true;
                }
            }
        }
        return false;

    }

    public void testMatch() {
        if (!isThereMatch()) {
            add3CartesSinglePlayer();
            nbCarte = 15;
            if (multiJoueur) {
                server_out.println("NUMBEROFCARDS " + 15);
            }
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)

    public void selection(final View view) {

        lock.lock();
        try {
            int id = view.getId();
            //ImageView carte = (ImageView) view;
            try {
                CardDrawable card = carteSurTable.get(id);
                if (card.getSelected()) {
                    //Si déjà sélectionné on enlève la sélection
                    selected.remove(Integer.valueOf(id));
                    card.isSelected(false);
                    view.invalidate();
                    return;
                } else {
                    selected.push(id);
                    card.isSelected(true);
                    view.invalidate();
                    // En gros invalidate il dis qu'il redraw() la prochaine fois qu'il est en idle... sauf que les commandes suivantes l'empeche de redraw avant que la view recoive un nouveau invalidate dans la fonction traiterMatch

                    if (selected.size() >= 3) {
                        if(!multiJoueur) {
                            Handler traiterHandler = new Handler();
                            Runnable traiterRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        traiterMatch();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            };
                            traiterHandler.postDelayed(traiterRunnable, 1);
                        }
                        else{
                            Handler traiterMultiHandler = new Handler();
                            Runnable traiterMultiRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        traiterMatchMulti();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            };
                            traiterMultiHandler.postDelayed(traiterMultiRunnable, 1);


                        }

                    }
                }
            } finally {
                return;
            }

        } finally {
            lock.unlock();
        }
    }

    public void clearCarte(int addresse) {                                 //Opérationnelle
        ImageView carte1 = (ImageView) findViewById(addresse);
        //C'est quoi cette ligne qui remet les cartes dans le tas: le jeu n'est pas censé s'arreter quand toute les cartes ont été tirés?
        deck[kToNumeroDeCarte(table[tas.get(addresse) - 1])] = false;
        table[tas.get(addresse) - 1] = -1;
        carteSurTable.put(addresse, null);
        carte1.setImageDrawable(null);
        carte1.invalidate();
    }

    public void traiterMatchMulti () throws InterruptedException {
        Thread.sleep(800);


        String message = "TRY/" + N + "/";
        int u = selected.pop();
        int v = selected.pop();
        int w = selected.pop();

        int a = tas.get(u)-1;
        int b = tas.get(v)-1;
        int c = tas.get(w)-1;
        message+=a+"/";
        message+=b+"/";
        message+=c+"/";
        selected.empty();
        server_out.println(message);

        CardDrawable card = carteSurTable.get(u);
        card.isSelected(false);
        ImageView carte = (ImageView) findViewById(u);
        carte.invalidate();

        card = carteSurTable.get(v);
        card.isSelected(false);
        carte = (ImageView) findViewById(v);
        carte.invalidate();

        card = carteSurTable.get(w);
        card.isSelected(false);
        carte = (ImageView) findViewById(w);
        carte.invalidate();
        lock.lock();
        wait.awaitUninterruptibly();
        lock.unlock();
    }

    public void traiterMatch() throws InterruptedException {
        Thread.sleep(1000);

        Integer a = selected.pop();
        Integer b = selected.pop();
        Integer c = selected.pop();


        if (Cards.isSet(table[tas.get(a) - 1], table[tas.get(b) - 1], table[tas.get(c) - 1])) {

            /*if (!isServeur) {
                StringBuffer message = new StringBuffer();
                message.append(tas.get(a) - 1 + " " + table[tas.get(a) - 1] + " ");
                message.append(tas.get(b) - 1 + " " + table[tas.get(b) - 1] + " ");
                message.append(tas.get(c) - 1 + " " + table[tas.get(c) - 1] + " ");
                server_out.println("SEND " + message);
                //Comment on attend la réponse
            }*/


            //Incrémentation du compteur du joueur et dernier set attrapé
            add = true;
            scoreHandler.postDelayed(scoreRunnable, 0);


            if (multiJoueur) {
                server_out.println("POINT " + 1);
            }
            afficherDernierSet(a, b, c);

            selected.removeAllElements();  // Au cas ou non vide
            if (nbCarte == 15) {


                if (multiJoueur) {
                    server_out.println("NUMBEROFCARDS " + 12);
                    server_out.println("WIN " + table[tas.get(a) - 1] + " " + table[tas.get(b) - 1] + " " + table[tas.get(c) - 1]);
                }

                clearCarte(a);
                clearCarte(b);
                clearCarte(c);
                trou1 = a;
                trou2 = b;
                trou3 = c;
                nbCarte = 12;


                testMatch();
            } else {
                addCard(a);
                addCard(b);
                addCard(c);
                testMatch();
            }

        } else {
            selected.removeAllElements();
            CardDrawable card = carteSurTable.get(a);
            card.isSelected(false);
            ImageView carte = (ImageView) findViewById(a);
            carte.invalidate();

            card = carteSurTable.get(b);
            card.isSelected(false);
            carte = (ImageView) findViewById(b);
            carte.invalidate();

            card = carteSurTable.get(c);
            card.isSelected(false);
            carte = (ImageView) findViewById(c);
            carte.invalidate();
            add = false;
            scoreHandler.postDelayed(scoreRunnable, 0);
            //Désincrémentation du compteur du joueur et dernier set attrapé
        }
        testMatch();

    }

    public void add3CartesSinglePlayer() {
        addCard(trou1);
        addCard(trou2);
        addCard(trou3);
    }

    //Non opérationnel
    public void afficherDernierSet(Integer a, Integer b, Integer c) {

        int val1 = table[tas.get(a) - 1];
        int val2 = table[tas.get(b) - 1];
        int val3 = table[tas.get(c) - 1];
        CardDrawable lastSet = new CardDrawable(val1, Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888));
        lastSet.drawSet(lastSet.canvas,val1,val2,val3);
        set.setImageDrawable(lastSet);
        set.invalidate();

    }

    public void addCard(int adresse, int val) {
        ImageView button = (ImageView) findViewById(adresse);
        CardDrawable nouvelleCard = new CardDrawable(val, Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888));
        nouvelleCard.customDraw();
        button.setImageDrawable(nouvelleCard);
        button.invalidate(); //Etape pour réinitialiser une ImageView
    }

    public void pushParameters(final View v) {
        Intent intent = new Intent(MainActivity.this, ParametreActivity.class);
        startActivity(intent);
    }

    public Context getActivity() {
        return this;
    }
}

