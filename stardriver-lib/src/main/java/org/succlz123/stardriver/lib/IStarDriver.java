package org.succlz123.stardriver.lib;

import android.content.Context;

import androidx.annotation.Keep;

@Keep
public abstract class IStarDriver {

    public abstract void initialize(Context context, StarDriverResult result);
}
