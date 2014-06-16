package sk.kandrac.sample;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import sk.kandrac.circularview.CircularView;


public class MainActivity extends ActionBarActivity implements SeekBar.OnSeekBarChangeListener {

    CircularView circularView;
    SeekBar seekBar;
    String negative = "negative";
    String positive = "positive";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        circularView = (CircularView) findViewById(R.id.circle);

        circularView.addItem(positive, 1, getResources().getColor(R.color.blue));
        circularView.addItem(negative, 2, getResources().getColor(R.color.green));
        circularView.addItem("unknown", 3, getResources().getColor(R.color.orange));

        circularView.invalidate();

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);
    }

    public void addpos(View view) {
        circularView.addItemScore(positive, 1f);
    }

    public void circleClick(View view) {
    }

    public void centerClick(View view) {
        Toast.makeText(this, "center clicked",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        circularView.setOuterWidth(i);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
