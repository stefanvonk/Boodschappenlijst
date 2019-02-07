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
    ArrayList<String> shoppingList = null;
    ArrayAdapter<String> adapter = null;
    ListView lv = null;

    private String lijstnaam;
    private String lijstId;

    int todo = 0;
    int afgevinkt = 0;

    DatabaseReference databaseShopping;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping);

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
                // set plus button lokaal
                FloatingActionButton actie_add = (FloatingActionButton) findViewById(R.id.actie_afronden);
                actie_add.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Shopping.this, EndScreen.class);
                        Bundle b = new Bundle();
                        b.putInt("todo", todo);
                        b.putInt("afgevinkt", afgevinkt);
                        intent.putExtras(b); 	// Put your id to your next Intent
                        startActivity(intent);	// start
                    }
                });
            }
        }
    }

    // Voer code voor lokale lijst uit
    public void lokaleLijst(){
        shoppingList = getArrayVal(getApplicationContext());
        Collections.sort(shoppingList);
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, shoppingList);
        lv = (ListView) findViewById(R.id.listView);
        lv.setAdapter(adapter);

        // set aantal todo producten in de lijst
        todo = shoppingList.size();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                String selectedItem = ((TextView) view).getText().toString();
                switchElement(selectedItem, position);
            }
        });
    }

    // Voer code voor online lijst uit
    public void onlineLijst(final String lijstnaam){
        shoppingList = new ArrayList<>();

        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, shoppingList);
        lv = (ListView) findViewById(R.id.listView);

        // Read from the database
        databaseShopping.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                shoppingList.clear();
                for (DataSnapshot productsSnapshot : dataSnapshot.child(lijstId).child("lijstProducts").getChildren()) {
                    String product = productsSnapshot.getValue(String.class);
                    shoppingList.add(product);
                }
                Collections.sort(shoppingList);
                lv.setAdapter(adapter);

                // set aantal todo producten in de lijst
                todo = shoppingList.size();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Failed to read value
                Snackbar.make(findViewById(R.id.actie_add), "Database kan niet gelezen worden", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                String selectedItem = ((TextView) view).getText().toString();
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

    public void switchElement(final String selectedItem, int position){
        String data = shoppingList.get(position);

        if(!data.contains(" → afgevinkt!")){
            shoppingList.set(position, data + " → afgevinkt!");
            lv.setAdapter(adapter);

            // set to do en afgevinkt producten naar de juiste waarde
            afgevinkt++;
            todo--;

            Snackbar.make(findViewById(R.id.actie_afronden), "Het product " + selectedItem + " is afgevinkt", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

        } else{
            String[] parts = data.split(" → afgevinkt!");
            shoppingList.set(position, parts[0]);
            lv.setAdapter(adapter);

            // set to do en afgevinkt producten naar de juiste waarde
            todo++;
            afgevinkt--;

            Snackbar.make(findViewById(R.id.actie_afronden), "Het product " + parts[0] + " is teruggezet", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }


//        if (lv.getChildAt(position).isEnabled()) {
//
//            lv.getChildAt(position).setEnabled(false);
//
//            // set to do en afgevinkt producten naar de juiste waarde
//            afgevinkt++;
//            todo--;
//
//            Snackbar.make(findViewById(R.id.actie_afronden), "Het product " + selectedItem + " is afgevinkt", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show();
//        } else {
//
//            lv.getChildAt(position).setEnabled(true);
//
//            // set to do en afgevinkt producten naar de juiste waarde
//            todo++;
//            afgevinkt--;
//
//            Snackbar.make(findViewById(R.id.actie_afronden), "Het product " + selectedItem + " is teruggezet", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show();
//        }
    }
}
