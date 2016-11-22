package com.gustavo_hidalgo.projetocd;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    EditText mMessageEditText;
    Button mTxButton, mMessageButton;
    ToggleButton mFftButton;
    AudioGenerator mAudioGenerator;
    ArrayList<double[]> mMessageModulated;
    SpectrumAnalyser mSpectrumAnalyser;
    GraphView mGraph;
    LineGraphSeries<DataPoint> mLineGraphSeries;
    int[] mMessage = {0};
    final private int PERMISSION = 1;
    public static final int m0Frequency = 15000;
    public static final int m1Frequency = 20000;
    public static final int mSamples = 44100;
    public static final int mBitTxDuration = 22000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initialize();
    }

    private void initialize(){
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mMessageButton = (Button) findViewById(R.id.message_button);
        mTxButton = (Button) findViewById(R.id.tx_button);
        mFftButton = (ToggleButton) findViewById(R.id.fft_button);
        mGraph = (GraphView) findViewById(R.id.graph);
        mAudioGenerator = new AudioGenerator(mSamples);
        mMessageModulated = new ArrayList<>();
        mLineGraphSeries = new LineGraphSeries<>();
        mGraph.getViewport().setYAxisBoundsManual(true);
        mGraph.getViewport().setMinY(0);
        mGraph.getViewport().setMaxY(99);
        mGraph.getViewport().setXAxisBoundsManual(true);
        mGraph.getViewport().setMinX(0);
        mGraph.getViewport().setMaxX(255);
        mGraph.addSeries(mLineGraphSeries);

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.RECORD_AUDIO}, PERMISSION);
                    }
        }

        mMessageButton.setOnClickListener(this);
        mTxButton.setOnClickListener(this);

        mFftButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    mSpectrumAnalyser = new SpectrumAnalyser(new SpectrumAnalyserInterface() {
                        @Override
                        public void onPublishProgress(DataPoint[]... values) {
                            mLineGraphSeries.resetData(values[0]);
                        }

                        @Override
                        public void onCancelled() {
                        }
                    });
                    mSpectrumAnalyser.execute();
                } else {
                    mSpectrumAnalyser.cancel(true);
                    mSpectrumAnalyser = null;
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tx_button:
                tx(mMessage);
                break;
            case R.id.message_button:
                String[] split = mMessageEditText.getText().toString().split("");
                mMessage = new int[split.length-1];
                for (int i = 0; i < split.length-1; i++) {
                    mMessage[i] = Integer.parseInt(split[i+1]);
                }
                break;
        }
    }

    public void tx (int[] message){
        for (int bit : message) {
            int frequency;
            if (bit == 0){
                frequency = m0Frequency;
            } else {
                frequency = m1Frequency;
            }
            mMessageModulated.add(mAudioGenerator.getSineWave(mBitTxDuration, mSamples, frequency));
        }
        mAudioGenerator.createPlayer();
        for (double[] bitModulated:mMessageModulated) {
            mAudioGenerator.writeSound(bitModulated);
        }
        mAudioGenerator.destroyAudioTrack();
        mMessageModulated.clear();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                }
            }
        }
    }
}
