package nl.stefanvonk.shoppinglist;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;

import java.util.HashSet;
import java.util.Set;

import android.app.AlertDialog;
import android.widget.EditText;
import android.content.DialogInterface;
import android.content.Context;
import android.content.SharedPreferences;
import android.app.Activity;

import android.widget.AdapterView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ShoppingList extends AppCompatActivity {
    // maak een aantal variabelen aan

    // arraylist voor lokale boodschappenlijst
    ArrayList<String> shoppingListLokaal = null;
    // arraylist voor online boodschappenlijst
    ArrayList<String> shoppingListOnline = null;
    // adapter voor array
    ArrayAdapter<String> adapter = null;
    // listview om array te showen
    ListView lv = null;

    // te gebruiken strings initiëren
    private String lijstnaam;
    private String lijstId;

    // referentie voor Firebase database
    DatabaseReference databaseShopping;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shoppinglist);

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
                    onlineLijst(this.lijstnaam);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // add menu item in bar boven in het scherm
        getMenuInflater().inflate(R.menu.menu_shoppinglist, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // voer actie uit als er op winkelen geklikt wordt in het menu
        if (id == R.id.action_shop) {
            // bij lokale boodschappenlijst:
            if(lijstnaam.equals("Lokale boodschappenlijst")) {
                if (!shoppingListLokaal.isEmpty()) {
                    // als er producten aanwezig zijn, ga naar volgende scherm
                    Intent intent = new Intent(ShoppingList.this, Shopping.class);
                    Bundle b = new Bundle();
                    b.putString("keuze", this.lijstnaam);    // lijstnaam naar volgende scherm
                    intent.putExtras(b);
                    startActivity(intent);    // start nieuwe scherm
                } else{
                    // als er geen producten aanwezig zijn geef foutmelding
                    Snackbar.make(findViewById(R.id.actie_add), "Uw boodschappenlijst is nog leeg, voeg eerst producten toe", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            } else{
                // bij online boodschappenlijst:
                if (!shoppingListOnline.isEmpty()) {
                    // als er producten aanwezig zijn, ga naar volgende scherm
                    Intent intent = new Intent(ShoppingList.this, Shopping.class);
                    Bundle b = new Bundle();
                    b.putString("keuze", this.lijstnaam);    // lijstnaam naar volgende scherm
                    intent.putExtras(b);
                    startActivity(intent);    // start nieuwe scherm
                } else{
                    // als er geen producten aanwezig zijn geef foutmelding
                    Snackbar.make(findViewById(R.id.actie_add), "Uw boodschappenlijst is nog leeg, voeg eerst producten toe", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // hoofdletter aan string toevoegen
    public static String hoofdletterToevoegen(String origineel)
    {
        if (origineel.isEmpty())
            return origineel;
        return origineel.substring(0, 1).toUpperCase() + origineel.substring(1);
    }

    // Voer code voor lokale lijst uit
    public void lokaleLijst(){
        // haal producten uit Shared Preferences en voeg toe aan de lokale shopping lijst
        shoppingListLokaal = getArrayVal(getApplicationContext());
        Collections.sort(shoppingListLokaal);
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, shoppingListLokaal);
        lv = (ListView) findViewById(R.id.listView);
        lv.setAdapter(adapter);

        // click listener toevoegen aan elementen in lijst
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, final int position, long id) {
                String selectedItem = ((TextView) view).getText().toString();
                // functie voor element verwijderen aanroepen
                removeElementLokaal(selectedItem, position);
            }
        });

        // set add button voor lokaal
        FloatingActionButton actie_add = (FloatingActionButton) findViewById(R.id.actie_add);
        actie_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addElementLokaal();
            }
        });
    }

    // Voer code voor online lijst uit
    public void onlineLijst(final String lijstnaam){
        // arraylist voor online boodschappenlijst aanmaken
        shoppingListOnline = new ArrayList<>();
        shoppingListOnline.clear();
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, shoppingListOnline);
        lv = (ListView) findViewById(R.id.listView);

        // Read producten uit database en voeg toe aan shoppingListOnline
        databaseShopping.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                shoppingListOnline.clear();
                for (DataSnapshot productsSnapshot : dataSnapshot.child(lijstId).child("lijstProducts").getChildren()) {
                    String product = productsSnapshot.getValue(String.class);
                    shoppingListOnline.add(product);
                    }
                Collections.sort(shoppingListOnline);
                }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // failed to read value
                Snackbar.make(findViewById(R.id.actie_add), "Database kan niet gelezen worden", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // lijst daadwerkelijk toevoegen aan scherm
        lv.setAdapter(adapter);

        // click listener toevoegen aan elementen in lijst
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, final int position, long id) {
                String selectedItem = ((TextView) view).getText().toString();
                // functie voor element verwijderen aanroepen
                removeElementOnline(lijstnaam, selectedItem, position);
            }
        });

        // set add button voor online
        FloatingActionButton actie_add = (FloatingActionButton) findViewById(R.id.actie_add);
        actie_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addElementOnline(lijstnaam, lijstId);
            }
        });
    }

    // Shared Preferences lokale opslag wegschrijven
    public static void storeArrayVal( ArrayList<String> inArrayList, Context context)
    {
        Set<String> WhatToWrite = new HashSet<String>(inArrayList);
        SharedPreferences WordSearchPutPrefs = context.getSharedPreferences("dbArrayValues", Activity.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = WordSearchPutPrefs.edit();
        prefEditor.putStringSet("myArray", WhatToWrite);
        prefEditor.commit();
    }

    // Shared Preferences lokale opslag ophalen
    public static ArrayList getArrayVal( Context dan)
    {
        SharedPreferences WordSearchGetPrefs = dan.getSharedPreferences("dbArrayValues",Activity.MODE_PRIVATE);
        Set<String> tempSet = new HashSet<String>();
        tempSet = WordSearchGetPrefs.getStringSet("myArray", tempSet);
        return new ArrayList<String>(tempSet);
    }

    // lokaal elementen toevoegen aan boodschappenlijst
    public void addElementLokaal(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Product toevoegen");
        final EditText input = new EditText(this);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // input product hoofdletter toevoegen en toewijzen aan variable
                String input_product = hoofdletterToevoegen(input.getText().toString());
                // toevoegen aan lokale lijst
                shoppingListLokaal.add(input_product);
                // lijst sorteren
                Collections.sort(shoppingListLokaal);
                // lijst opnieuw opslaan in Shared Preferences
                storeArrayVal(shoppingListLokaal, getApplicationContext());
                // voeg lijst opnieuw toe aan scherm
                lv.setAdapter(adapter);
                // melding dat toevoegen is gelukt
                Snackbar.make(findViewById(R.id.actie_add), "Het product " + input_product + " is toegevoegd", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    // lokaal elementen verwijderen uit boodschappenlijst
    public void removeElementLokaal(final String selectedItem, final int position){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(selectedItem + " verwijderen?");
        builder.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // verwijderen uit lokale lijst
                shoppingListLokaal.remove(position);
                // lijst sorteren
                Collections.sort(shoppingListLokaal);
                // lijst opnieuw opslaan in Shared Preferences
                storeArrayVal(shoppingListLokaal, getApplicationContext());
                // lijst opnieuw toevoegen aan scherm
                lv.setAdapter(adapter);
                // melding dat verwijderen is gelukt
                Snackbar.make(findViewById(R.id.actie_add), "Het product " + selectedItem + " is verwijderd", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        builder.setNegativeButton("Nee", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    // online elementen toevoegen aan boodschappenlijst
    public void addElementOnline(final String lijstnaam, final String lijstId){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Product toevoegen");
        final EditText input = new EditText(this);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // input product hoofdletter toevoegen en toewijzen aan variable
                String input_product = hoofdletterToevoegen(input.getText().toString());
                // haal de juiste key op uit database
                String id = databaseShopping.child(lijstId).child("lijstProducts").push().getKey();
                // voeg aan de hand van de key het nieuwe product toe aan de database, de lijst wordt automatisch geupdated
                databaseShopping.child(lijstId).child("lijstProducts").child(id).setValue(input_product);
                // melding dat toevoegen is gelukt
                Snackbar.make(findViewById(R.id.actie_add), "Het product " + input_product + " is toegevoegd", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    // online elementen verwijderen uit boodschappenlijst
    public void removeElementOnline(String lijstnaam, final String selectedItem, final int position){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(selectedItem + " verwijderen?");
        builder.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // verwijder element uit de lijst
                shoppingListOnline.remove(position);
                // toon opnieuw op scherm
                lv.setAdapter(adapter);
                // voeg de lijst met producten toe aan een andere tijdelijke andere lijst
                ArrayList<String> shoppingListTemp = shoppingListOnline;
                // verwijder de hele lijst producten uit de juiste online lijst in de database
                databaseShopping.child(lijstId).child("lijstProducts").removeValue();
                // voeg één voor één de producten uit de tijdelijke lijst toe aan de juiste lijst in de database
                for (int i = 0; i < shoppingListTemp.size(); i ++) {
                    String id = databaseShopping.child(lijstId).child("lijstProducts").push().getKey();
                    databaseShopping.child(lijstId).child("lijstProducts").child(id).setValue(shoppingListTemp.get(i));
                }
                // melding dat verwijderen is gelukt
                Snackbar.make(findViewById(R.id.actie_add), "Het product " + selectedItem + " is verwijderd", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }
}
