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
    ArrayList<String> shoppingListLokaal = null;
    ArrayList<String> shoppingListOnline = null;
    ArrayAdapter<String> adapter = null;
    ListView lv = null;

    private String lijstnaam;
    private String lijstId;

    DatabaseReference databaseShopping;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shoppinglist);

        Bundle b = getIntent().getExtras();
        if (b != null){
            this.lijstnaam = b.getString("keuze");
            if (this.lijstnaam != null) {
                if(this.lijstnaam.equals("Lokale boodschappenlijst")) {
                    lokaleLijst();
                } else {
                    databaseShopping = FirebaseDatabase.getInstance().getReference("/");

                    // Read from the database
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
                            // Failed to read value
                            Snackbar.make(findViewById(R.id.actie_add), "Database kan niet gelezen worden", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                    });

                    onlineLijst(this.lijstnaam);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_shoppinglist, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_shop) {
            if(lijstnaam.equals("Lokale boodschappenlijst")) {
                if (!shoppingListLokaal.isEmpty()) {
                    Intent intent = new Intent(ShoppingList.this, Shopping.class);
                    Bundle b = new Bundle();
                    b.putString("keuze", this.lijstnaam);    // Your string
                    intent.putExtras(b);    // Put your id to your next Intent
                    startActivity(intent);    // start
                } else{
                    Snackbar.make(findViewById(R.id.actie_add), "Uw boodschappenlijst is nog leeg, voeg eerst producten toe", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            } else{
                if (!shoppingListOnline.isEmpty()) {
                    Intent intent = new Intent(ShoppingList.this, Shopping.class);
                    Bundle b = new Bundle();
                    b.putString("keuze", this.lijstnaam);    // Your string
                    intent.putExtras(b);    // Put your id to your next Intent
                    startActivity(intent);    // start
                } else{
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
        shoppingListLokaal = getArrayVal(getApplicationContext());
        Collections.sort(shoppingListLokaal);
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, shoppingListLokaal);
        lv = (ListView) findViewById(R.id.listView);
        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, final int position, long id) {
                String selectedItem = ((TextView) view).getText().toString();
                removeElementLokaal(selectedItem, position);
            }
        });

        // set plus button lokaal
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
        shoppingListOnline = new ArrayList<>();
        shoppingListOnline.clear();

        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, shoppingListOnline);
        lv = (ListView) findViewById(R.id.listView);

        // Read from the database
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
                // Failed to read value
                Snackbar.make(findViewById(R.id.actie_add), "Database kan niet gelezen worden", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, final int position, long id) {
                String selectedItem = ((TextView) view).getText().toString();
                removeElementOnline(lijstnaam, selectedItem, position);
            }
        });

        // set plus button online
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

    // lokaal elementen toevoegen
    public void addElementLokaal(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Product toevoegen");
        final EditText input = new EditText(this);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String input_product = hoofdletterToevoegen(input.getText().toString());
                shoppingListLokaal.add(input_product);
                Collections.sort(shoppingListLokaal);
                storeArrayVal(shoppingListLokaal, getApplicationContext());
                lv.setAdapter(adapter);
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

    // lokaal elementen verwijderen
    public void removeElementLokaal(final String selectedItem, final int position){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(selectedItem + " verwijderen?");
        builder.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                shoppingListLokaal.remove(position);
                Collections.sort(shoppingListLokaal);
                storeArrayVal(shoppingListLokaal, getApplicationContext());
                lv.setAdapter(adapter);
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

    // online elementen toevoegen
    public void addElementOnline(final String lijstnaam, final String lijstId){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Product toevoegen");
        final EditText input = new EditText(this);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String input_product = hoofdletterToevoegen(input.getText().toString());
                String id = databaseShopping.child(lijstId).child("lijstProducts").push().getKey();
                databaseShopping.child(lijstId).child("lijstProducts").child(id).setValue(input_product);
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

    // online elementen verwijderen
    public void removeElementOnline(String lijstnaam, final String selectedItem, final int position){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(selectedItem + " verwijderen?");
        builder.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                shoppingListOnline.remove(position);
                lv.setAdapter(adapter);

                ArrayList<String> shoppingListTemp = shoppingListOnline;

                databaseShopping.child(lijstId).child("lijstProducts").removeValue();

                for (int i = 0; i < shoppingListTemp.size(); i ++) {
                    String id = databaseShopping.child(lijstId).child("lijstProducts").push().getKey();
                    databaseShopping.child(lijstId).child("lijstProducts").child(id).setValue(shoppingListTemp.get(i));
                }

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
