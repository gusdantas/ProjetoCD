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
import java.util.Timer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    EditText mMessageTxEditText, mF0ChannelEditText, mF1ChannelEditText, mBlkSizeEditText, mSamplesEditText;
    Button mTxButton, mMessageButton;
    ToggleButton mFftButton, mRxButton;
    TextView mMessageRxEditText, mF0Tx, mF1Tx, mTxTimeTv, mF0Rx, mF0RxMax, mF1Rx, mF1RxMax;
    AudioGenerator mAudioGenerator;
    ArrayList<double[]> mMessageModulated;
    SpectrumAnalyser mSpectrumAnalyser;
    Reception mReception;
    GraphView mGraph;
    LineGraphSeries<DataPoint> mLineGraphSeries;
    Timer mTimer;
    int mF0Counter, mF1Counter, mF0, mF1;
    double mTxTime;
    int[] mMessageTx = {0};
    StringBuilder mMessageRx;

    final private int PERMISSION = 1;
    double mF0MaxValue, mF0ActualValue, mF1MaxValue, mF1ActualValue = 0;
    public int mF0Channel = 1900;
    public int mF1Channel = 1910;
    public int mBlockSize = 2205;
    public int mSamples = 22050; //500ms
    public static final int mSampleRate = 44100;
    public static final int mBandRate = mSampleRate/2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initialize();
    }

    private void initialize(){
        mF0ChannelEditText = (EditText) findViewById(R.id.f0Channel_editText);
        mF1ChannelEditText = (EditText) findViewById(R.id.f1Channel_editText);
        mBlkSizeEditText = (EditText) findViewById(R.id.blockSize_editText);
        mSamplesEditText = (EditText) findViewById(R.id.bitTxSamples_editText);
        mMessageTxEditText = (EditText) findViewById(R.id.messageTxEditText);
        mMessageRxEditText = (TextView) findViewById(R.id.messageRxTextView);

        mMessageButton = (Button) findViewById(R.id.message_button);
        mF0Tx = (TextView) findViewById(R.id.f0tx_textView);
        mF1Tx = (TextView) findViewById(R.id.f1tx_textView);
        mTxTimeTv = (TextView) findViewById(R.id.bitTxTime_textView);
        mTxButton = (Button) findViewById(R.id.tx_button);
        mF0Rx = (TextView) findViewById(R.id.f0_textView);
        mF0RxMax = (TextView) findViewById(R.id.f0max_textView);
        mF1Rx = (TextView) findViewById(R.id.f1_textView);
        mF1RxMax = (TextView) findViewById(R.id.f1max_textView);
        mRxButton = (ToggleButton) findViewById(R.id.rx_toggleButton);
        mGraph = (GraphView) findViewById(R.id.graph);
        mFftButton = (ToggleButton) findViewById(R.id.fft_button);

        mMessageRx = new StringBuilder();
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
        mF0 = (mBandRate/mBlockSize)*mF0Channel;
        mF1 = (mBandRate/mBlockSize)*mF1Channel;
        mTxTime = (double) mSamples/mSampleRate;
        updateUI();

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
                    mF0Counter = mF1Counter = 0;
                    mReception = new Reception(mBlockSize, new ReceptionInterface() {
                        @Override
                        public void onPreExecute() {
                            //mMessageRx.delete(0,mMessageRx.length()-1);
                            mTimer = new Timer();
                            mTimer.schedule(new SyncReceptor(new SyncReceptorInterface() {
                                @Override
                                public void confirmBit() {
                                    if (mF0Counter > mF1Counter){
                                        mMessageRx.append(0);
                                    } else if (mF0Counter < mF1Counter){
                                        mMessageRx.append(1);
                                    }
                                    mF0Counter = mF1Counter = 0;
                                }
                            }), 0, 500);
                        }

                        @Override
                        public void onPublishProgress(DataPoint[]... values) {
                            mF0ActualValue = values[0][mF0Channel].getY();
                            mF0Rx.setText(String.valueOf(mF0ActualValue));
                            mF1ActualValue = values[0][mF1Channel].getY();
                            mF1Rx.setText(String.valueOf(mF1ActualValue));
                            /*mF0MaxValue = Math.max(mF0MaxValue,mF0ActualValue);
                            mF0RxMax.setText(String.valueOf(mF0MaxValue));
                            mF1MaxValue = Math.max(mF1MaxValue,mF1ActualValue);
                            mF1RxMax.setText(String.valueOf(mF1MaxValue));*/

                            if(mF0ActualValue > 10){
                                mF0RxMax.setText(String.valueOf(mF0Counter++));
                            } else if(mF1ActualValue > 10){
                                mF1RxMax.setText(String.valueOf(mF1Counter++));
                            }
                            mMessageRxEditText.setText(mMessageRx);
                        }

                        @Override
                        public void onCancelled() {
                            mMessageRx.delete(0,mMessageRx.length());
                            mTimer.cancel();
                            mTimer.purge();
                            mTimer = null;
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

    private void updateUI() {
        mF0ChannelEditText.setText(String.valueOf(mF0Channel));
        mF1ChannelEditText.setText(String.valueOf(mF1Channel));
        mBlkSizeEditText.setText(String.valueOf(mBlockSize));
        mSamplesEditText.setText(String.valueOf(mSamples));
        mF0Tx.setText(String.valueOf(mF0));
        mF1Tx.setText(String.valueOf(mF1));
        mTxTimeTv.setText(String.valueOf(mTxTime));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tx_button:
                if(mMessageTx != null) {
                    tx(mMessageTx);
                }
                break;
            case R.id.message_button:
                String[] split = mMessageTxEditText.getText().toString().split("");
                mMessageTx = new int[split.length-1];
                for (int i = 0; i < split.length-1; i++) {
                    mMessageTx[i] = Integer.parseInt(split[i+1]);
                }
                mF0Channel = Integer.parseInt(mF0ChannelEditText.getText().toString());
                mF1Channel = Integer.parseInt(mF1ChannelEditText.getText().toString());
                mBlockSize = Integer.parseInt(mBlkSizeEditText.getText().toString());
                mSamples = Integer.parseInt(mSamplesEditText.getText().toString());
                mF0 = (mBandRate/mBlockSize)*mF0Channel;
                mF1 = (mBandRate/mBlockSize)*mF1Channel;
                mTxTime = (double) mSamples/mSampleRate;
                updateUI();
                break;
        }
    }

    public void tx (int[] message){
        for (int bit : message) {
            int frequency;
            if (bit == 0){
                frequency = mF0;
            } else {
                frequency = mF1;
            }
            mMessageModulated.add(mAudioGenerator.getSineWave(mSamples, mSampleRate, frequency));
        }
        mMessageModulated.add(mAudioGenerator.getSineWave(mSamples, mSampleRate, 0));
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
