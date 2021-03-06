package com.polytechnique.marc.amaury.set;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity {

    //images view pour afficher le dernier set
    ImageView set1;
    ImageView set2;
    ImageView set3;

    // verrou
    ReentrantLock lock;
    Condition wait;

    //initialisation de textes d'affichages
    String affichage = "Hello";
    String my_login = "default";
    String ip_adresse = "default";

    // N est la copie du N du serveur : c'est le numéro du plateau courant associé a l'ensemble des cartes présentes sur le plateau.
    public static Integer N = 0;

    // outils pour le thread de connection au serveur
    Thread connectionThread;
    static PrintWriter server_out = null;
    static PrintWriter telnet_out = null;
    static Boolean multiJoueur = false;

    //tableaux de stockages des valeurs.
    private boolean[] deck = new boolean[81]; //Deck donne true pour chaque carte posée sur plateau
    public int[] table = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1};   //lie le numéro de la carte( 1à 15) avec sa valeur en tant que card (k)
    //si la carte vaux -1 elle n'existe pas, cf modification de isSet
    private HashMap<Integer, Integer> tas = new HashMap<>();    //lie l'adresse de la carte au numéro de la carte (1 a 15)
    private Integer[] listeDesAdresses = new Integer[15]; // fait la liste des adresses pour chaque position (inverse de tas)
    private int nbCarte = 12;
    private HashMap<Integer, CardDrawable> carteSurTable = new HashMap<>(); // associe à chaque adresse l'objet CardDrawable correspondant
    private volatile Stack<Integer> selected = new Stack<>(); //garde une liste des cartes selectionnées

    //Les trois trous (qui permettent de rajouter 3 cartes si besoin)
    Integer trou1;
    Integer trou2;
    Integer trou3;

    TextView timerTextView;
    TextView scoreTextView;
    TextView affichageTextView;

    //Dans onCreate, on met le code qui ne doit s'executer qu'un seule fois
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //on initialise dans init() la plupart des variables
        init();
        //Test match permet de verifier si il y a bien un set, et sinon il rajoute trois cartes.
        testMatch();
        // On lance maintenant le thread de connection. Si la connection échoue, le boolean multijoueur est false et tout se fait par l'application en solo. Sinon, c'est le serveur qui prend la main.
        connectionThread = new Thread(connection);
        connectionThread.start();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Lorsque la vue reviens, OnStart() est appelé, et donc il faut recharger l'ip et le pseudo qui ont peut-être changés
        final SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.preference_file_key),0);
        String restart =  sharedPref.getString(getString(R.string.restart),getResources().getString(R.string.restart));
        my_login = sharedPref.getString(getString(R.string.pseudo),getResources().getString(R.string.pseudo));
        ip_adresse =  sharedPref.getString(getString(R.string.ip),getResources().getString(R.string.ip));
        //Dans PArametreActivity, si on clique sur le bouton "relancer la connection", le paramtrer restart deviens true. Alors on relance la connection et on le passe a false;
        if(restart.equals("true")){
            restartConnection();
            sharedPref.edit().putString(getString(R.string.restart), "false").apply();
        }
    }

    //Pour relancer la connection, on logout puis on relance le thread connection.
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

    //Permet d'affiche ce qu'on veut en haut de l'application (sert pour le score et le débug)
    void displayMessage(String s) {
        affichage = s;
        runOnUiThread(affichageRunnable);
    }

    //Le thread connection, qui établit s'il peut la connection au serveur.
    Runnable connection = new Runnable() {
        @Override
        public void run() {
            Socket s;
            try {
                s = Net.establishConnection(ip_adresse, 1709);
                displayMessage("CONNECTED");
            } catch (RuntimeException e) {
                displayMessage("Unconnected: " + e.toString());
                return;
            }
            PrintWriter s_out = Net.connectionOut(s);
            final BufferedReader s_in = Net.connectionIn(s);
            // Avant de lancer se connecter, il faut recuperer un vrai pseudo, et pas celui par défaut: on lance l'activité de paramtre.
            if(my_login.equals(getString(R.string.pseudo))) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(MainActivity.this, ParametreActivity.class);
                        startActivity(intent);
                    }
                });
            }
            //On est connecté, il faut s'identifier
            s_out.println("LOGIN/" + my_login);
            String line;
            //on cherche a lire une premiere ligne
            try {
                line = s_in.readLine();
            } catch (IOException e) {
                throw new RuntimeException("in readLine");
            }
            Scanner sc = new Scanner(line);
            sc.useDelimiter(" ");
            if (sc.next().equals("Welcome")) {
                //Dans ce cas, on est connecté et accepté, on passe en mode multijoueur.
                displayMessage("ACCEPTED");
                multiJoueur = true;
                server_out = s_out;

                //On demande au serveur le plateau en cours
                s_out.println("GAMEPLEASE/");
            }

            //Ce thread permet d'attendre en permanance les messages du serveur et de les traiter.
            final Thread from_server = new Thread() {
                //Fonction qui traite les messages et actualise l'affichage
                void traiterMessageServeur(String line) {
                    if (line == null) return;
                    Scanner sc = new Scanner(line);
                    sc.useDelimiter("/");
                    String message = sc.next();
                    switch (message) {
                        case "theGame":
                            int numDeLaCarte;
                            int i = 0;
                            if (sc.hasNext()) {   //Cette étape ne semble aps se faire correctement
                                N=Integer.parseInt(sc.next());
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

                            //On signal à la condition wait pour reveiller le main thread dans la fonction traiterMulti.
                            selected.empty();
                            lock.lock();
                            wait.signalAll();
                            lock.unlock();
                            break;

                        case "result": {
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
                        case "dernierSet":{
                            int a = 0;
                            int b = 0;
                            int c = 0;
                            if(sc.hasNext()) {
                                a = sc.nextInt();
                            }
                            else{
                                displayMessage("error dernier set");
                            }
                            if(sc.hasNext()) {
                                b = sc.nextInt();
                            }
                            else{
                                displayMessage("error dernier set");
                            }
                            if(sc.hasNext()) {
                                c = sc.nextInt();
                            }
                            else{
                                displayMessage("error dernier set");
                            }

                            final int finalA = a;
                            final int finalB = b;
                            final int finalC = c;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    afficherDernierSet(finalA, finalB, finalC);
                                }
                            });

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
            from_server.start();
        }
    };


    //Lorsque le serveur envoie un nouveau plateau, il faut tout mettre à jour:
    public void mettreAJour(){
        //mettre a jour carteSurTable, nbCartes, trou1 2 et 3, selected;
        int count = 0;
        for(int i = 0; i<81;i++){
            deck[i]=false;
        }
        for(int i = 0; i <15; i++){
            if(table[i]!=-1){
                deck[kToNumeroDeCarte(table[i])]=true;
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
                carteSurTable.put(adresse, null);
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

    // pour envoyer un nouveau message
    private void sendMessage(String message) {
        server_out.println(message);
    }

    //Un handler recursif pour afficher le temps
    long startTime = 0;
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


    //Un handler pour afficher le score du joueur.
    long score = -1;
    boolean add = true;
    Handler scoreHandler = new Handler();
    Runnable scoreRunnable = new Runnable() {
        @Override
        public void run() {
            if (add) score += 1;
            else score -= 1;
            scoreTextView.setText(String.format("%s:%02d", "Score", score));
        }
    };

    //Pour afficher le texte en haut de l'appli
    Handler affichageHandler = new Handler();
    Runnable affichageRunnable = new Runnable() {
        @Override
        public void run() {
            affichageTextView.setText(String.format("%s", affichage));
        }
    };

    //initialisation avec des cartes au hasard (quitte a les changer a la connexion du serveur)
    public void init() {
        TextView parametre = (TextView)  findViewById(R.id.parametres);
        parametre.setText(R.string.parametres);
        affichageTextView = (TextView) findViewById(R.id.affichage);
        affichage = "on create";
        affichageHandler.postDelayed(affichageRunnable, 0);
        final SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.preference_file_key),0);
        my_login = sharedPref.getString(getString(R.string.pseudo),getResources().getString(R.string.pseudo));
        ip_adresse = my_login = sharedPref.getString(getString(R.string.ip),getResources().getString(R.string.ip));
        lock = new ReentrantLock();
        wait = lock.newCondition();
        set1 = (ImageView) findViewById(R.id.set1);
        set2 = (ImageView) findViewById(R.id.set2);
        set3 = (ImageView) findViewById(R.id.set3);
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

    //fonction d'ajout d'une carte
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

    //vérifie l'existence d'un set
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

    //verifie l'existence d'un set (avec isThereMatch) et ajoute s'il le faut 3 cartes
    public void testMatch() {
        if (!isThereMatch()) {
            addCard(trou1);
            addCard(trou2);
            addCard(trou3);
            nbCarte = 15;
        }
    }

    //C'est la fonction OnClick des cartes: elle ajoute ou enlève la carte selectionné et appelle les fonctions de test lorsque 3 cartes sont selectionnées.
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
        }
        finally {
            lock.unlock();
        }
    }

    //Pour supprimer une carte lorsqu'elle faisait partie d'un bon set selectionné
    public void clearCarte(int addresse) {                                 //Opérationnelle
        ImageView carte1 = (ImageView) findViewById(addresse);
        //C'est quoi cette ligne qui remet les cartes dans le tas: le jeu n'est pas censé s'arreter quand toute les cartes ont été tirés?
        deck[kToNumeroDeCarte(table[tas.get(addresse) - 1])] = false;
        table[tas.get(addresse) - 1] = -1;
        carteSurTable.put(addresse, null);
        carte1.setImageDrawable(null);
        carte1.invalidate();
    }

    //Fonction qui envoie un set proposé par le joueur au serveur. Il attend à la fin avec la condition wait (qui est reveillé lorsque le thread From_Server recoit un message et signal)
    public void traiterMatchMulti () throws InterruptedException {
        //permet l'afichage un court instant des 3 cartes selectionnées
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

    //idem mais en solo, donc pas besoin d'attente avec la condition wait
    public void traiterMatch() throws InterruptedException {
        Thread.sleep(1000);
        Integer a = selected.pop();
        Integer b = selected.pop();
        Integer c = selected.pop();
        if (Cards.isSet(table[tas.get(a) - 1], table[tas.get(b) - 1], table[tas.get(c) - 1])) {
            //Incrémentation du compteur du joueur et dernier set attrapé
            add = true;
            scoreHandler.postDelayed(scoreRunnable, 0);
            if (multiJoueur) {
                server_out.println("POINT " + 1);
            }
            //setHandler.postDelayed(setRunnable,0);
            afficherDernierSet(table[tas.get(a) - 1], table[tas.get(b) - 1], table[tas.get(c) - 1]);
            selected.removeAllElements();  // Au cas ou non vide
            if (nbCarte == 15) {
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

    //Permet d'afficher en bas de l'ecran le dernier set réussi.
    public void afficherDernierSet(Integer a, Integer b, Integer c) {
        addCard(R.id.set1, a);
        addCard(R.id.set2, b);
        addCard(R.id.set3, c);
    }

    //permet d'ajout une carte d'un valeur donnée
    public void addCard(int adresse, int val) {
        ImageView button = (ImageView) findViewById(adresse);
        CardDrawable nouvelleCard = new CardDrawable(val, Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888));
        nouvelleCard.customDraw();
        button.setImageDrawable(nouvelleCard);
        button.invalidate(); //Etape pour réinitialiser une ImageView
    }

    //Fonction OnClick du bouton parametre, qui envoi vers l'autre activité.
    public void pushParameters(final View v) {
        Intent intent = new Intent(MainActivity.this, ParametreActivity.class);
        startActivity(intent);
    }

    //Permet de recuperer l'activité courante (nécessaire apparement pour avoir les sharedPreferences)
    public Context getActivity() {
        return this;
    }
}

