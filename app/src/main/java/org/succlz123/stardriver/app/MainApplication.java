package org.succlz123.stardriver.app;

import android.app.Application;

import org.succlz123.stardriver.lib.StarDriverManager;
import org.succlz123.stardriver.lib.StarDriverStatistics;

import java.util.List;

public class MainApplication extends Application {
    public static MainApplication application;
    public List<StarDriverStatistics> statistics;

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
        methodA();
        StarDriverManager starDriverManager = new StarDriverManager();
        starDriverManager.initTasks(this);
        statistics = starDriverManager.getStatistics();
        methodB();
    }

    void methodA() {

    }

    void methodB() {

    }
}
