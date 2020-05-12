package org.succlz123.stardriver.app.task;

import android.content.Context;

import org.succlz123.stardriver.annotation.StarDriverInit;
import org.succlz123.stardriver.lib.IStarDriver;
import org.succlz123.stardriver.lib.StarDriverResult;

@StarDriverInit(name = "Application logger")
public class AppInitLogger extends IStarDriver {

    @Override
    public void initialize(Context context, StarDriverResult result) {
        try {
            // simulation initialization code
            Thread.sleep(23);
        } catch (InterruptedException e) {
            result.success = false;
            result.errorMessage = e.toString();
            return;
        }
        result.success = true;
    }
}
