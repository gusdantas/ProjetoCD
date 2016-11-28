package com.gustavo_hidalgo.projetocd;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import com.jjoe64.graphview.series.DataPoint;

import java.util.ArrayList;
import java.util.List;

import ca.uol.aig.fftpack.RealDoubleFFT;

/**
 * Created by hdant on 19/11/2016.
 */

public class Reception extends AsyncTask<Void, DataPoint[], Boolean> {
    RealDoubleFFT mFFTransformer;
    AudioRecord mAudioRecorder;
    DataPoint[] mDataPoints;
    int counter = 0;
    int mBlockSize = 256;
    int mBufferSize, mBufferReadResult;
    //int mFrequency = 44100;
    int mChannelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    boolean mStarted = true;
    short[] mBuffer;
    double[] mTransformed;
    ReceptionInterface mReceptionInterface;
    public static final String TAG = "[ProjCD] Reception";


    public Reception(ReceptionInterface receptionInterface){
        this.mReceptionInterface = receptionInterface;
        this.mDataPoints = new DataPoint[mBlockSize];
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mBufferSize = AudioRecord.getMinBufferSize(MainActivity.mSampleRate, mChannelConfiguration,
                mAudioEncoding);
        mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, MainActivity.mSampleRate,
                mChannelConfiguration, mAudioEncoding, mBufferSize);
        mBuffer = new short[mBlockSize];
        mTransformed = new double[mBlockSize];
        mFFTransformer = new RealDoubleFFT(mBlockSize);
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
                    mDataPoints[i] = new DataPoint((double) i, Math.abs(10 * mTransformed[i]));
                }
                Log.d(TAG, String.valueOf(counter++));
                if(mBufferReadResult == mBlockSize) {
                    publishProgress(mDataPoints);
                }
            }
        }
        return true;
    }

    @Override
    protected void onProgressUpdate(DataPoint[]... values) {
        super.onProgressUpdate(values);
        mReceptionInterface.onPublishProgress(values);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        mAudioRecorder.release();
        mReceptionInterface.onCancelled();
    }
}
