package com.gustavo_hidalgo.projetocd;

import java.util.TimerTask;

/**
 * Created by hdant on 03/12/2016.
 */

public class SyncReceptor extends TimerTask{
    SyncReceptorInterface mSyncReceptorInterface;
    boolean mIsActive;

    public SyncReceptor(SyncReceptorInterface syncReceptorInterface){
        this.mSyncReceptorInterface = syncReceptorInterface;
        this.mIsActive = false;
    }

    @Override
    public void run() {
        this.mIsActive = true;
        mSyncReceptorInterface.confirmBit();
    }

    public boolean isActive(){
        return this.mIsActive;
    }

    @Override
    public boolean cancel() {
        this.mIsActive = false;
        return super.cancel();
    }
}
