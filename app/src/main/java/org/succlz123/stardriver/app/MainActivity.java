package org.succlz123.stardriver.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import org.succlz123.stardriver.lib.StarDriverStatistics;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tv = findViewById(R.id.content);
        for (StarDriverStatistics statistic : MainApplication.application.statistics) {
            tv.append(statistic.name + "\nsuccess: " + statistic.success + " time: " + statistic.useTime + "ms\n");
            if (!statistic.success && statistic.errorMessage != null) {
                tv.append("message: " + statistic.errorMessage + "\n\n");
            } else {
                tv.append("\n");
            }
        }
    }
}
