package es.elb4t.opencvbase;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by eloy on 10/4/17.
 */

public class Preferencias extends PreferenceActivity {
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferencias);
    }
}
