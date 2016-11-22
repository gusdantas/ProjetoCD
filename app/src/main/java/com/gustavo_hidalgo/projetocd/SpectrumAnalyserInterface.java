package com.gustavo_hidalgo.projetocd;

import com.jjoe64.graphview.series.DataPoint;

/**
 * Created by hdant on 19/11/2016.
 */

public interface SpectrumAnalyserInterface {
    void onPublishProgress(DataPoint[]... values);
    void onCancelled();
}
