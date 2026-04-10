package com.voice.caePk;

/**
 * Created by admin on 2019/5/6.
 */

public interface OnCaeOperatorlistener {
    void onAudio(byte[] audioData, int dataLen);
    void onWakeup(int angle ,int beam);

}
