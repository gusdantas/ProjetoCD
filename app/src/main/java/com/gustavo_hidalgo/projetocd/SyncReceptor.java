package com.gustavo_hidalgo.projetocd;

import java.util.TimerTask;

/**
 * Created by hdant on 03/12/2016.
 */

class SyncReceptor extends TimerTask{
    private SyncReceptorInterface mSyncReceptorInterface;

    SyncReceptor(SyncReceptorInterface syncReceptorInterface){
        this.mSyncReceptorInterface = syncReceptorInterface;
    }

    @Override
    public void run() {
        mSyncReceptorInterface.confirmBit();
    }

    @Override
    public boolean cancel() {
        return super.cancel();
    }
}
