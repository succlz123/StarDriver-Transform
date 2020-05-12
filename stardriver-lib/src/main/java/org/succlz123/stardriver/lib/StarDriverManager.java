package org.succlz123.stardriver.lib;

import androidx.annotation.Keep;

import java.util.ArrayList;
import java.util.List;

// DO NOT EDIT THIS JAVA FILE!!!
@Keep
public class StarDriverManager {
    private List<StarDriverStatistics> statistics = new ArrayList<>();

    public List<StarDriverStatistics> getStatistics() {
        return statistics;
    }

    public void initTasks(android.content.Context context) {
    }
}
