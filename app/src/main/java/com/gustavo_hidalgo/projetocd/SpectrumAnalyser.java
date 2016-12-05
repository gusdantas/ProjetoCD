package com.gustavo_hidalgo.projetocd;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import com.jjoe64.graphview.series.DataPoint;

import ca.uol.aig.fftpack.RealDoubleFFT;

/**
 * Created by hdant on 19/11/2016.
 */

class SpectrumAnalyser extends AsyncTask<Void, DataPoint[], Boolean> {
    private RealDoubleFFT mFFTransformer;
    private AudioRecord mAudioRecorder;
    private DataPoint[] mDataPoints;
    private int counter = 0;
    private int mBlockSize = 441;
    int mSampleRate = TxRxActivity.mSampleRate;
    int mBufferSize, mBufferReadResult, mRxGain;
    int mChannelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private boolean mStarted = true;
    private short[] mBuffer;
    private double[] mTransformed;
    private SpectrumAnalyserInterface mSpectrumAnalyserInterface;
    public static final String TAG = "[ProjCD] SpecAnalyser";


    SpectrumAnalyser(int rxGain, SpectrumAnalyserInterface spectrumAnalyserInterface){
        this.mSpectrumAnalyserInterface = spectrumAnalyserInterface;
        this.mRxGain = rxGain;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfiguration,
                mAudioEncoding);
        mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, mSampleRate,
                mChannelConfiguration, mAudioEncoding, mBufferSize);
        mBuffer = new short[mBlockSize];
        mTransformed = new double[mBlockSize];
        mFFTransformer = new RealDoubleFFT(mBlockSize);
        mDataPoints = new DataPoint[mBlockSize];
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            mAudioRecorder.startRecording();
        } catch (IllegalStateException e) {
            Log.e("Recording failed", e.toString());
        }
        while (mStarted) {
            if (isCancelled()) {
                mStarted = false;
                Log.d(TAG, "Cancelling the RecordTask");
                break;
            } else {
                mBufferReadResult = mAudioRecorder.read(mBuffer, 0, mBlockSize);
                for (int i = 0; i < mBlockSize && i < mBufferReadResult; i++) {
                    mTransformed[i] = (double) mBuffer[i] / 32768.0; // signed 16 bit
                }
                mFFTransformer.ft(mTransformed);
                for (int i = 0; i < mBlockSize; i++) {
                    mDataPoints[i] = new DataPoint((double) i, Math.abs(mRxGain * mTransformed[i]));
                }
                Log.d(TAG, String.valueOf(counter++));
                publishProgress(mDataPoints);
            }
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(DataPoint[]... values) {
        super.onProgressUpdate(values);
        mSpectrumAnalyserInterface.onPublishProgress(values);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        mAudioRecorder.release();
        mSpectrumAnalyserInterface.onCancelled();
    }
}
