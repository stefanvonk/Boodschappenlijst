package nl.stefanvonk.shoppinglist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Shopping extends AppCompatActivity {
    // maak een aantal variabelen aan

    // arraylist voor boodschappenlijst
    ArrayList<String> shoppingList = null;
    // adapter voor array
    ArrayAdapter<String> adapter = null;
    // listview om array te showen
    ListView lv = null;

    // te gebruiken strings initiëren
    private String lijstnaam;
    private String lijstId;

    // ints voor parameter passing initiëren
    int tedoen = 0;
    int afgevinkt = 0;

    // referentie voor Firebase database
    DatabaseReference databaseShopping;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping);

        // parameter passing variable opvangen en checken of de lokale of online functie moet worden uitgevoerd
        Bundle b = getIntent().getExtras();
        if (b != null){
            this.lijstnaam = b.getString("keuze");
            if (this.lijstnaam != null) {
                if(this.lijstnaam.equals("Lokale boodschappenlijst")) {
                    // voer lokale functie uit
                    lokaleLijst();
                } else {
                    // connectie met database initieren
                    databaseShopping = FirebaseDatabase.getInstance().getReference("/");

                    // vind het goede lijstId in de database bij de aangeklikte lijst uit het vorige scherm
                    databaseShopping.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot idSnapshot : dataSnapshot.getChildren()) {
                                String name = idSnapshot.child("lijstName").getValue(String.class);
                                if (name.equals(lijstnaam)) {
                                    // set lijstId
                                    lijstId = idSnapshot.child("lijstId").getValue(String.class);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // failed to read value
                            Snackbar.make(findViewById(R.id.actie_add), "Database kan niet gelezen worden", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                    });
                    // voer online functie uit
                    onlineLijst();
                }
                // set play button voor lokale lijst
                FloatingActionButton actie_add = (FloatingActionButton) findViewById(R.id.actie_afronden);
                actie_add.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Shopping.this, EndScreen.class);
                        Bundle b = new Bundle();
                        b.putInt("tedoen", tedoen); 	// parameter passing, geef aantal tedoen itmes mee
                        b.putInt("afgevinkt", afgevinkt); 	// geef aantal afgevinkte items mee
                        intent.putExtras(b); 	// put to intent
                        startActivity(intent);	// start nieuw scherm
                    }
                });
            }
        }
    }

    // Voer code voor lokale lijst uit
    public void lokaleLijst(){
        // haal boodschappenlijst op uit Shared Preferences en voeg toe aan shoppingList
        shoppingList = getArrayVal(getApplicationContext());
        // sorteer de lijst en set de adapter en view
        Collections.sort(shoppingList);
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, shoppingList);
        lv = (ListView) findViewById(R.id.listView);
        lv.setAdapter(adapter);

        // set variable tedoen gelijk aan totaal aantal producten in de lijst
        tedoen = shoppingList.size();

        // set klik listener op items in de lijst
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                String selectedItem = ((TextView) view).getText().toString();
                // vink het aangeklikte product af of zet het terug
                switchElement(selectedItem, position);
            }
        });
    }

    // Voer code voor online lijst uit
    public void onlineLijst(){
        // maak de array shoppingList aan
        shoppingList = new ArrayList<>();
        // set de adapter en view van de lijst
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, shoppingList);
        lv = (ListView) findViewById(R.id.listView);

        // lees de data uit de database aan de hand van de lijstId
        databaseShopping.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // clear shoppingList
                shoppingList.clear();
                for (DataSnapshot productsSnapshot : dataSnapshot.child(lijstId).child("lijstProducts").getChildren()) {
                    String product = productsSnapshot.getValue(String.class);
                    // add alle gevonden producten één voor één in de shoppingList
                    shoppingList.add(product);
                }
                // sorteer de lijst
                Collections.sort(shoppingList);
                lv.setAdapter(adapter);

                // set variable tedoen gelijk aan totaal aantal producten in de lijst
                tedoen = shoppingList.size();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // failed to read value
                Snackbar.make(findViewById(R.id.actie_add), "Database kan niet gelezen worden", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // set klik listener op items in de lijst
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                String selectedItem = ((TextView) view).getText().toString();
                // vink het aangeklikte product af of zet het terug
                switchElement(selectedItem, position);
            }
        });
    }

    // Shared Preferences lokale opslag ophalen
    public static ArrayList getArrayVal( Context dan)
    {
        SharedPreferences WordSearchGetPrefs = dan.getSharedPreferences("dbArrayValues",Activity.MODE_PRIVATE);
        Set<String> tempSet = new HashSet<String>();
        tempSet = WordSearchGetPrefs.getStringSet("myArray", tempSet);
        return new ArrayList<String>(tempSet);
    }

    // functie voor het afvinken of terugzetten van items in de lijst
    public void switchElement(final String selectedItem, int position){
        // inhoud van aangeklikt lijstitem naar variable schrijven
        String data = shoppingList.get(position);
        // als het product nog niet is afgevinkt
        if(!data.contains(" → afgevinkt!")){
            // voeg 'afgevinkt' toe aan inhoud lijstitem
            shoppingList.set(position, data + " → afgevinkt!");
            lv.setAdapter(adapter);

            // set tedoen en afgevinkt naar plus en min één voor de grafiek in het volgende scherm
            afgevinkt++;
            tedoen--;

            // melding dat afvinken gelukt is
            Snackbar.make(findViewById(R.id.actie_afronden), "Het product " + selectedItem + " is afgevinkt", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

        } else{
            // als het product wel is afgevinkt
            // verwijder 'afgevinkt' uit inhoud lijstitem
            String[] parts = data.split(" → afgevinkt!");
            shoppingList.set(position, parts[0]);
            lv.setAdapter(adapter);

            // set tedoen en afgevinkt naar min en plus één voor de grafiek in het volgende scherm
            afgevinkt--;
            tedoen++;

            // melding dat terugzetten gelukt is
            Snackbar.make(findViewById(R.id.actie_afronden), "Het product " + parts[0] + " is teruggezet", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }
}
