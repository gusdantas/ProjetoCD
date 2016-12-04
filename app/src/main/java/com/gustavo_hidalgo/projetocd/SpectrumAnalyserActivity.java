package com.gustavo_hidalgo.projetocd;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class SpectrumAnalyserActivity extends AppCompatActivity {
    SpectrumAnalyser mSpectrumAnalyser;
    GraphView mGraph;
    LineGraphSeries<DataPoint> mLineGraphSeries;
    ToggleButton mFftButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spec_analyser);
        initialize();
    }

    private void initialize(){
        mGraph = (GraphView) findViewById(R.id.graphView);
        mFftButton = (ToggleButton) findViewById(R.id.fft_button);

        mLineGraphSeries = new LineGraphSeries<>();
        mGraph.getViewport().setYAxisBoundsManual(true);
        mGraph.getViewport().setMinY(0);
        mGraph.getViewport().setMaxY(109);
        mGraph.getViewport().setXAxisBoundsManual(true);
        mGraph.getViewport().setMinX(0);
        mGraph.getViewport().setMaxX(440);
        mGraph.addSeries(mLineGraphSeries);

        mFftButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    mSpectrumAnalyser = new SpectrumAnalyser(TxRxActivity.mRxGain, new SpectrumAnalyserInterface() {
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
}
