package com.gustavo_hidalgo.projetocd;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
    ToggleButton mFftButton, mRxButton;
    TextView mF0Rx, mF0RxMax, mF1Rx, mF1RxMax;
    AudioGenerator mAudioGenerator;
    ArrayList<double[]> mMessageModulated;
    SpectrumAnalyser mSpectrumAnalyser;
    Reception mReception;
    GraphView mGraph;
    LineGraphSeries<DataPoint> mLineGraphSeries;
    int[] mMessage = {0};
    final private int PERMISSION = 1;
    double mF0MaxValue, mF0ActualValue, mF1MaxValue, mF1ActualValue = 0;
    public static final int m0Frequency = 18949;
    public static final int m1Frequency = 19811;
    public static final int mSampleRate = 44100;
    public static final int mSamples = 22050; //500ms

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
        mF0Rx = (TextView) findViewById(R.id.f0_textView);
        mF0RxMax = (TextView) findViewById(R.id.f0max_textView);
        mF1Rx = (TextView) findViewById(R.id.f1_textView);
        mF1RxMax = (TextView) findViewById(R.id.f1max_textView);
        mFftButton = (ToggleButton) findViewById(R.id.fft_button);
        mRxButton = (ToggleButton) findViewById(R.id.rx_toggleButton);
        mGraph = (GraphView) findViewById(R.id.graph);
        mAudioGenerator = new AudioGenerator(mSampleRate);
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

        mRxButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    mReception = new Reception(new ReceptionInterface() {
                        @Override
                        public void onPublishProgress(DataPoint[]... values) {
                            mF0ActualValue = values[0][220].getY();
                            mF0Rx.setText(String.valueOf(mF0ActualValue));
                            mF1ActualValue = values[0][230].getY();
                            mF1Rx.setText(String.valueOf(mF1ActualValue));
                            mF0MaxValue = Math.max(mF0MaxValue,mF0ActualValue);
                            mF0RxMax.setText(String.valueOf(mF0MaxValue));
                            mF1MaxValue = Math.max(mF1MaxValue,mF1ActualValue);
                            mF1RxMax.setText(String.valueOf(mF1MaxValue));
                        }

                        @Override
                        public void onCancelled() {
                        }
                    });
                    mReception.execute();
                } else {
                    mReception.cancel(true);
                    mReception = null;
                }
            }
        });

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
            mMessageModulated.add(mAudioGenerator.getSineWave(mSamples, mSampleRate, frequency));
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
