package com.polytechnique.marc.amaury.set;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Random;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {

    ImageView set1;
    ImageView set2;
    ImageView set3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        testMatch();

    }


    private boolean[] deck = new boolean[81];
    private int[] table = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1};   //lie le numéro de la carte( 1à 15) avec sa valeur en tant que card
    private HashMap<Integer, Integer> tas = new HashMap<Integer, Integer>();    //lie l'adresse au numéro de la carte (1 a 15)
    private int nbCarte = 12;

    private Stack<Integer> selected = new Stack<Integer>();

    TextView timerTextView;
    TextView scoreTextView;

    long startTime = 0;

    long score = -1;
    boolean add = true;

    Integer addresse;
    int numeroCarteSet = 0;

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

    Handler setHandler = new Handler();
    Runnable setRunnable = new Runnable() {
        @Override
        public void run() {
            CardDrawable nouvelleCard = new CardDrawable(171);
            //CardDrawable nouvelleCard = new CardDrawable(table[tas.get(addresse)]);
            Bitmap bit = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888);
            Canvas can = new Canvas(bit);
            nouvelleCard.draw(can);
            switch (numeroCarteSet) {
                case 0:
                    break;
                case 1:
                    set1.setImageDrawable(nouvelleCard);
                    set1.invalidate();
                    break;
                case 2:
                    set2.setImageDrawable(nouvelleCard);
                    set2.invalidate();
                    break;
                case 3:
                    set3.setImageDrawable(nouvelleCard);
                    set3.invalidate();
                    break;
            }
        }
    };


    public void init() {
        set1 = (ImageView) findViewById(R.id.set1);
        numeroCarteSet=1;
        setHandler.postDelayed(setRunnable,0);
        set2 = (ImageView) findViewById(R.id.set2);
        numeroCarteSet=2;
        setHandler.postDelayed(setRunnable,0);
        set3 = (ImageView) findViewById(R.id.set3);
        numeroCarteSet=3;
        setHandler.postDelayed(setRunnable,0);
        numeroCarteSet=0;
        timerTextView = (TextView) findViewById(R.id.time);
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
        scoreTextView = (TextView) findViewById(R.id.score);
        scoreHandler.postDelayed(scoreRunnable, 0);
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
        for (Integer addresse : tas.keySet()) {
            if (!(addresse.equals(R.id.image15) || addresse.equals(R.id.image14) || addresse.equals(R.id.image13))) {
                addCard(addresse);
            }
        }
    }

    //opérationel
    public void addCard(int addresse) {
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
        table[tas.get(addresse)] = k;
        ImageView button = (ImageView) findViewById(addresse);

        CardDrawable nouvelleCard = new CardDrawable(k);
        Bitmap b = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        nouvelleCard.draw(c);
        button.setImageDrawable(nouvelleCard);
        button.invalidate();                     //Etape pour réinitialiser une ImageView
    }

    //opérationel? Jamais vu 15 cartes
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

    //opérationel? Jamais vu 15 cartes
    public void testMatch() {
        if (!isThereMatch()) {
            addCard(R.id.image15);
            addCard(R.id.image14);
            addCard(R.id.image13);
            nbCarte = 15;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)

    //Pb: je n'arrive pas à déselectionner
    //Pb: il faudrait qu'il n'y ai rien quand je selectionne une carte non tiré (13,14,15) la cela bug car nullpointer
    public void selection(View view) {
        int id = view.getId();
        ImageView carte = (ImageView) view;
        CardDrawable card = (CardDrawable) carte.getDrawable();
        if (card.getSelected()) {                        //Si déjà sélectionné on enlève la sélection
            //selected.remove(id);                       //Je pense que la double sélection ne fonctionne pas, donc on va faire rien quand carte sélectionné
            //card.isSelected(false);                //Ne fonctionne pas ...
            /*clearCarte(id);                        //Ne fonctionne pas non plus ...
            ImageView carte1 = (ImageView) findViewById(id);
            carte1.setImageDrawable(new CardDrawable(table[tas.get(id)]));*/
            return;
        } else {
            selected.push(id);
            card.isSelected(true);
            carte.invalidate();
            if (selected.size() >= 3) {
                traiterMatch();        //Doit-on enlever la précédente avant d'en mettre une nouvelle?
            }
        }
    }

    //opérationel
    public void clearCarte(int addresse) {
        ImageView carte1 = (ImageView) findViewById(addresse);
        carte1.setImageDrawable(null);
        carte1.invalidate();
    }

    //le score est bien (dés)incrémenter quand match ou non
    public void traiterMatch() {
        Integer a = selected.pop();
        Integer b = selected.pop();
        Integer c = selected.pop();

        if (Cards.isSet(table[tas.get(a)], table[tas.get(b)], table[tas.get(c)])) {


            //Incrémentation du compteur du joueur et dernier set attrapé
            add = true;
            scoreHandler.postDelayed(scoreRunnable, 0);

            afficherDernierSet(a, b, b);


            selected.removeAllElements();  // Au cas ou non vide
            if (nbCarte == 15) {
                clearCarte(a);
                clearCarte(b);
                clearCarte(c);
                nbCarte = 12;
            } else {
                addCard(a);
                addCard(b);
                addCard(c);
            }

        } else {
            selected.removeAllElements();
            add = false;
            scoreHandler.postDelayed(scoreRunnable, 0);
            //Désincrémentation du compteur du joueur et dernier set attrapé
        }
    }

    //Non opérationnel
    public void afficherDernierSet(Integer a, Integer b, Integer c) {
        addresse = a;
        numeroCarteSet=1;
        setHandler.postDelayed(setRunnable, 0);
        addresse = b;
        numeroCarteSet=2;
        setHandler.postDelayed(setRunnable, 0);
        addresse = c;
        numeroCarteSet=3;
        setHandler.postDelayed(setRunnable, 0);
        numeroCarteSet=0;

    }
}