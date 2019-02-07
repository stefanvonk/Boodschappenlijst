package nl.stefanvonk.shoppinglist;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;

public class EndScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end_screen);

        // maak een nieuwe BarChart (balk grafiek) aan op de goede plek in het scherm
        BarChart barChart = (BarChart) findViewById(R.id.barchart);
        // set een aantal instellingen voor de BarChart
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setMaxVisibleValueCount(50);
        barChart.setPinchZoom(false);
        barChart.setDrawGridBackground(true);

        // variable voor data in de grafiek aanmaken
        int enabled = 0;
        int disabled = 0;

        // data vanuit parameterpassing opvangen en toevoegen aan de juiste variabelen
        Bundle b = getIntent().getExtras();
        if(b != null) {
            enabled = b.getInt("tedoen");
            disabled = b.getInt("afgevinkt");
        }

        // nieuwe lijst voor grafiekdata maken
        ArrayList<BarEntry> barEntries = new ArrayList<>();

        // voeg data toe aan lijst voor grafiek
        barEntries.add(new BarEntry(0, disabled));
        barEntries.add(new BarEntry(1, enabled));

        // set een aantal instellingen voor de grafiek
        BarDataSet barDataSet = new BarDataSet(barEntries, "Producten uit de boodschappenlijst");
        barDataSet.setColors(ColorTemplate.COLORFUL_COLORS);

        // de uiteindelijk weergave van de grafiek initiëren, de breedte bepalen en de data toevoegen
        BarData data = new BarData(barDataSet);
        data.setBarWidth(0.8f);
        barChart.setData(data);

        // animatie aan de grafiek toevoegen
        barChart.animateY(600);

        // beschrijving aan de grafiek toevoegen
        Description description = new Description();
        description.setText("Resultaten");
        barChart.setDescription(description);

        // zorgen dat er niet ingezoomd kan worden op de lijst
        barChart.setDragEnabled(false);
        barChart.setScaleEnabled(false);

        // lege beschrijvingen voor de X-as boven de grafiek toevoegen om te voorkomen dat daar cijfers worden getoond
        String[] omschrijving = new String[] {"", ""};
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new MyXAxisValueFormatter(omschrijving));
    }

    // functie om de lege omschrijvingen toe te kunnen voegen
    public class MyXAxisValueFormatter implements IAxisValueFormatter{
        private String[] mValues;
        public MyXAxisValueFormatter(String[] values){
            this.mValues = values;
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis){
            return mValues[(int)value];
        }
    }
}
