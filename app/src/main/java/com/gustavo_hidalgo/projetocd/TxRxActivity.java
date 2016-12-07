package com.gustavo_hidalgo.projetocd;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.jjoe64.graphview.series.DataPoint;

import java.util.ArrayList;
import java.util.Timer;

public class TxRxActivity extends AppCompatActivity implements View.OnClickListener {
    EditText mMessageTxEditText, mChannelEditText, mRxGainEditText;
    RadioButton mModeSelected;
    RadioGroup mModeSelection;
    Button mTxButton, mMessageButton;
    ToggleButton mRxButton;
    TextView mMessageRxTextView, mF0Tx, mF1Tx, mTxTimeTv, mF0Rx, mF0RxMax, mF1Rx, mF1RxMax,
            mBlkSizeTextView;
    AudioGenerator mAudioGenerator;
    ArrayList<double[]> mMessageModulated;
    Receptor mReceptor;
    Timer mTimer;
    int mF0Counter, mF1Counter, mFask, mF0fsk, mF1fsk;
    double mTxTime;
    int[] mMessageTx = {0};
    StringBuilder mMessageRx;
    boolean mIsAsk = false;

    double mF0ActualValue, mF1ActualValue = 0;
    public int mChannel = 400;
    public static int mRxGain = 10;
    public static final int mBlockSize = 441;
    public static final int mSamples = 22050; //500ms
    public static final int mSampleRate = 44100;
    public static final int mBandRate = mSampleRate/2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_txrx);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initialize();
    }

    private void initialize(){
        mModeSelection = (RadioGroup) findViewById(R.id.radioGroup);
        mChannelEditText = (EditText) findViewById(R.id.channel_editText);
        mBlkSizeTextView = (TextView) findViewById(R.id.blockSize_textView);
        mRxGainEditText = (EditText) findViewById(R.id.rxGain_editText);
        mMessageTxEditText = (EditText) findViewById(R.id.messageTxEditText);
        mMessageButton = (Button) findViewById(R.id.message_button);
        mF0Tx = (TextView) findViewById(R.id.f0tx_textView);
        mF1Tx = (TextView) findViewById(R.id.f1tx_textView);
        mTxTimeTv = (TextView) findViewById(R.id.bitTxTime_textView);
        mTxButton = (Button) findViewById(R.id.tx_button);
        mF0Rx = (TextView) findViewById(R.id.f0_textView);
        mF0RxMax = (TextView) findViewById(R.id.f0max_textView);
        mF1Rx = (TextView) findViewById(R.id.f1_textView);
        mF1RxMax = (TextView) findViewById(R.id.f1max_textView);
        mMessageRxTextView = (TextView) findViewById(R.id.messageRxTextView);
        mRxButton = (ToggleButton) findViewById(R.id.rx_toggleButton);

        mMessageRx = new StringBuilder();
        mAudioGenerator = new AudioGenerator(mSampleRate);
        mMessageModulated = new ArrayList<>();
        mFask = (mBandRate/mBlockSize) * mChannel;
        mF0fsk = (mBandRate/mBlockSize) * (mChannel-1);
        mF1fsk = (mBandRate/mBlockSize) * (mChannel+1);
        mTxTime = (double) mSamples/mSampleRate;
        String timeSymbol = "Ts = "+String.valueOf(mTxTime);
        mTxTimeTv.setText(timeSymbol);
        updateUI();

        mModeSelection.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int selectedId = mModeSelection.getCheckedRadioButtonId();
                mModeSelected = (RadioButton)findViewById(selectedId);
                mModeSelected.setOnClickListener(TxRxActivity.this);
            }
        });
        mMessageButton.setOnClickListener(this);
        mTxButton.setOnClickListener(this);
        mRxButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    mF0Counter = mF1Counter = 0;
                    mReceptor = new Receptor(mBlockSize, mRxGain, new ReceptorInterface() {
                        @Override
                        public void onPreExecute() {
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
                            if (mIsAsk) {
                                mF0ActualValue = values[0][mChannel].getY();
                                mF0Rx.setText(String.valueOf(mF0ActualValue));
                                if (mF0ActualValue < 10) {
                                    mF0RxMax.setText(String.valueOf(mF0Counter++));
                                } else {
                                    mF1RxMax.setText(String.valueOf(mF1Counter++));
                                }
                            } else {
                                mF0ActualValue = values[0][mChannel - 1].getY();
                                mF0Rx.setText(String.valueOf(mF0ActualValue));
                                mF1ActualValue = values[0][mChannel + 1].getY();
                                mF1Rx.setText(String.valueOf(mF1ActualValue));

                                if (mF0ActualValue > 10) {
                                    mF0RxMax.setText(String.valueOf(mF0Counter++));
                                } else if (mF1ActualValue > 10) {
                                    mF1RxMax.setText(String.valueOf(mF1Counter++));
                                }
                            }
                            mMessageRxTextView.setText(mMessageRx);
                        }

                        @Override
                        public void onCancelled() {
                            mMessageRx.delete(0,mMessageRx.length());
                            mTimer.cancel();
                            mTimer.purge();
                            mTimer = null;
                        }
                    });
                    mReceptor.execute();
                } else {
                    mReceptor.cancel(true);
                    mReceptor = null;
                }
            }
        });
    }

    private void updateUI() {
        //String channel = "Canal = "+String.valueOf(mChannel);
        mChannelEditText.setText(String.valueOf(mChannel));
        //String rxGain = "Ganho RX = "+String.valueOf(mRxGain);
        mRxGainEditText.setText(String.valueOf(mRxGain));
        if(mIsAsk){
            String freq = "F = "+String.valueOf(mFask);
            mF0Tx.setText(freq);
            mF1Tx.setText("-");
        } else {
            String f0 = "F0 = "+String.valueOf(mF0fsk);
            mF0Tx.setText(f0);
            String f1 = "F1 = "+String.valueOf(mF1fsk);
            mF1Tx.setText(f1);
        }
    }

    public void tx (int[] message){
        for (int bit : message) {
            int frequency;
            if (bit == 0){
                if(mIsAsk){
                    frequency = 0;
                } else {
                    frequency = mF0fsk;
                }
            } else {
                if(mIsAsk){
                    frequency = mFask;
                } else {
                    frequency = mF1fsk;
                }
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
                mChannel = Integer.parseInt(mChannelEditText.getText().toString());
                mRxGain = Integer.parseInt(mRxGainEditText.getText().toString());
                mFask = (mBandRate/mBlockSize) * mChannel;
                mF0fsk = (mBandRate/mBlockSize) * (mChannel-1);
                mF1fsk = (mBandRate/mBlockSize) * (mChannel+1);
                mTxTime = (double) mSamples/mSampleRate;
                updateUI();
                break;
            case R.id.askRadioButton:
                mIsAsk = true;
                break;
            case R.id.fskRadioButton:
                mIsAsk = false;
                break;
        }
    }
}
