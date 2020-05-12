package org.succlz123.stardriver.app.task;

import android.content.Context;

import org.succlz123.stardriver.annotation.StarDriverInit;
import org.succlz123.stardriver.lib.IStarDriver;
import org.succlz123.stardriver.lib.StarDriverResult;

@StarDriverInit(name = "SDK A", after = {AppInitCrashTracking.class, AppInitTaskSDKB.class}, before = {AppInitAccountInfo.class})
public class AppInitTaskSDKA extends IStarDriver {

    @Override
    public void initialize(Context context, StarDriverResult result) {
        try {
            Thread.sleep(166);
        } catch (InterruptedException e) {
            result.success = false;
            result.errorMessage = e.toString();
            return;
        }
        result.success = true;
    }
}
