package nl.stefanvonk.shoppinglist;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StartScreen extends AppCompatActivity {
    // maak een aantal variabelen aan

    // lijst met alle gebruikte lijsten
    ArrayList<String> userList = null;
    // adapter voor array
    ArrayAdapter<String> adapter = null;
    // listview om array te showen
    ListView lv = null;

    // String lijstId voor verwijderen van ingedrukte lijst
    private String lijstId = "leeg";

    // referentie voor Firebase database
    DatabaseReference databaseShopping;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set layout
        setContentView(R.layout.activity_startscreen);

        // set database met pad
        databaseShopping = FirebaseDatabase.getInstance().getReference("/");

        // nieuwe lijst aanmaken
        userList = new ArrayList<>();

        // set lokale
        userList.add("Lokale boodschappenlijst");
        adapter = new ArrayAdapter(StartScreen.this, android.R.layout.simple_list_item_1, userList);
        lv = (ListView) findViewById(R.id.listView);

        lv.setAdapter(adapter);

        // set clicklistener voor elementen in de lijst
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, final int position, long id) {
                String selectedItem = ((TextView) view).getText().toString();
                Intent intent = new Intent(StartScreen.this, ShoppingList.class);
                Bundle b = new Bundle();
                b.putString("keuze", selectedItem); 	// parameter passing
                intent.putExtras(b); 	// voeg string toe aan intent
                startActivity(intent);	// start nieuw scherm
            }
        });

        FloatingActionButton actie_add = (FloatingActionButton) findViewById(R.id.actie_add);
        actie_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(findViewById(R.id.actie_add), "Je bent offline, er kan geen lijst worden toegevoegd", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    protected void onStart(){
        super.onStart();

        // lees shopping lijsten uit dethe database
        databaseShopping.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userList.clear();

                // set twee standaardlijsten
                userList.add("Lokale boodschappenlijst");
                adapter = new ArrayAdapter(StartScreen.this, android.R.layout.simple_list_item_1, userList);
                lv = (ListView) findViewById(R.id.listView);

                // get lijsten daadwerkelijk uit database
                for(DataSnapshot shoppingSnapshot : dataSnapshot.getChildren()){
                    String name = shoppingSnapshot.child("lijstName").getValue(String.class);
                    userList.add(name);
                }

                lv.setAdapter(adapter);

                // set clicklistener voor elementen in de lijst
                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView parent, View view, final int position, long id) {
                        String selectedItem = ((TextView) view).getText().toString();
                        Intent intent = new Intent(StartScreen.this, ShoppingList.class);
                        Bundle b = new Bundle();
                        b.putString("keuze", selectedItem); 	// parameter passing
                        intent.putExtras(b); 	// voeg string toe aan intent
                        startActivity(intent);	// start nieuw scherm
                    }
                });

                // set longclicklistener om elementen uit de lijst te verwijderen
                lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                        final String selectedItem = ((TextView) view).getText().toString();
                        if(!selectedItem.equals("Lokale boodschappenlijst")) {
                            removeShoppingList(selectedItem, position);
                        } else{
                            Snackbar.make(findViewById(R.id.actie_add), "De lokale boodschappelijst kan niet verwijderd worden", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                        return true;
                    }
                });

                // plusbutton voor nieuwe lijst instellen
                FloatingActionButton actie_add = (FloatingActionButton) findViewById(R.id.actie_add);
                actie_add.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        addShoppingList();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed message
                Snackbar.make(findViewById(R.id.actie_add), "Database kan niet gelezen worden", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    // hoofdletter aan string toevoegen
    public static String hoofdletterToevoegen(String origineel)
    {
        if (origineel.isEmpty())
            return origineel;
        return origineel.substring(0, 1).toUpperCase() + origineel.substring(1);
    }

    // add boodschappenlijst
    public void addShoppingList(){
        // dmv alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Boodschappenlijst toevoegen");
        final EditText input = new EditText(this);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String input_lijst = hoofdletterToevoegen(input.getText().toString());

                if(!userList.contains(input_lijst)) {
                    // push ruimte in database en vraag key aan
                    String id = databaseShopping.push().getKey();

                    List<String> myList = new ArrayList<>();
//                    List<String> myList = new ArrayList<>(Collections.singletonList("Voorbeeldproduct"));
                    Lijst lijst = new Lijst(id, input_lijst, myList);

                    // push de nieuwe lijst daadwerkelijk naar database
                    databaseShopping.child(id).setValue(lijst);

                    // melding dat nieuwe lijst is toegevoegd
                    Snackbar.make(findViewById(R.id.actie_add), "De lijst " + input_lijst + " is toegevoegd", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else{
                    Snackbar.make(findViewById(R.id.actie_add), "De lijst " + input_lijst + " bestaat al", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
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

    public void removeShoppingList(final String selectedItem, final int position){
        AlertDialog.Builder builder = new AlertDialog.Builder(StartScreen.this);
        builder.setTitle(selectedItem + " verwijderen?");
        builder.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Read from the database
                databaseShopping.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot idSnapshot : dataSnapshot.getChildren()) {
                            String name = idSnapshot.child("lijstName").getValue(String.class);
                            if (name.equals(selectedItem)) {
                                lijstId = idSnapshot.child("lijstId").getValue(String.class);
                                if(!lijstId.equals("leeg")){
                                    databaseShopping.child(lijstId).removeValue();
                                    Snackbar.make(findViewById(R.id.actie_add), "De online boodschappelijst " + selectedItem + " is verwijderd", Snackbar.LENGTH_LONG)
                                            .setAction("Action", null).show();
                                } else{
                                    Snackbar.make(findViewById(R.id.actie_add), "De online boodschappelijst " + selectedItem + " kan niet worden verwijderd", Snackbar.LENGTH_LONG)
                                            .setAction("Action", null).show();
                                }
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
}