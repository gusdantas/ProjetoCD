package com.gustavo_hidalgo.projetocd;

import com.jjoe64.graphview.series.DataPoint;

import java.util.ArrayList;

/**
 * Created by hdant on 19/11/2016.
 */

public interface ReceptionInterface {
    void onPublishProgress(DataPoint[]... values);
    void onCancelled();
}
