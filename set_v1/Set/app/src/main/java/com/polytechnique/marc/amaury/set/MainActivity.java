package com.polytechnique.marc.amaury.set;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;

import java.util.HashMap;
import java.util.Random;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {

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



    public void init() {
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
            if (!(addresse.equals(R.id.image15) || addresse.equals(R.id.image14) || addresse.equals(R.id.image13)))
            {
                addCard(addresse);
            }
        }
    }

    public void addCard(int addresse) {
        Random tirage = new Random();
        boolean flag = true;
        int k = 0;
        int numberOfTheCard = 0;
        while (flag) {
            int a = (tirage.nextInt(3)+1);
            int b = (tirage.nextInt(3)+1);
            int c = (tirage.nextInt(3)+1);
            int d = (tirage.nextInt(3)+1);
            k = a + 4 * b + 16* c + 64* d ;
            numberOfTheCard = (a -1) + 3 *(b-1) + 9 * (c-1) + 27 * (d-1);
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
            addCard(R.id.image15);
            addCard(R.id.image14);
            addCard(R.id.image13);
            nbCarte = 15;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)

    public void selection(View view) {
        int id = view.getId();

        selected.push(id);
        //mettre un travail sur CardDrawable avec changement méthode isSelected, puis on dit que Imageview est invalidate et donc changement
        if (selected.size() >= 3) {
            traiterMatch();        //Doit-on enlever la précédente avant d'en mettre une nouvelle?
        }
    }

    public void clearCarte(int addresse) {                                 //Opérationnelle
        ImageView carte1 = (ImageView) findViewById(addresse);
        carte1.setImageDrawable(null);
        carte1.invalidate();
    }

    public void traiterMatch() {
        Integer a = selected.pop();
        Integer b = selected.pop();
        Integer c = selected.pop();
        if (Cards.isSet(table[tas.get(a)], table[tas.get(b)], table[tas.get(c)])) {


            //Incrémentation du compteur du joueur et dernier set attrapé


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
            //Désincrémentation du compteur du joueur et dernier set attrapé

        }


    }
}