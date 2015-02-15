package fi.iki.aeirola.teddyclient.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.EditText;

import java.net.URI;
import java.net.URISyntaxException;

import fi.iki.aeirola.teddyclient.R;
import fi.iki.aeirola.teddyclientlib.utils.SSLCertHelper;

/**
 * Created by Axel on 13.2.2015.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String KEY_PREF_URL = "url";
    public static final String KEY_PREF_PASSWORD = "password";
    public static final String KEY_PREF_CERT = "cert";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // Add listener to update summaries
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        ((BaseAdapter) getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
    }
}

class SummaryEditTextPreference extends EditTextPreference {
    public SummaryEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence getSummary() {
        String value = this.getPersistedString("");
        EditText edit = this.getEditText();
        value = edit.getTransformationMethod().getTransformation(value, edit).toString();
        return value;
    }
}

class CertificatePreference extends DialogPreference {
    private static final String TAG = CertificatePreference.class.getName();
    private static final String DEFAULT_VALUE = "";
    private EditText editTextView;
    private String mCurrentValue = DEFAULT_VALUE;

    public CertificatePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.certificate_preference);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        setDialogIcon(null);
    }

    @Override
    public boolean isEnabled() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        String url = pref.getString(SettingsFragment.KEY_PREF_URL, "");

        return url != null && url.toLowerCase().startsWith("wss");
    }

    @Override
    public CharSequence getSummary() {
        if (!this.isEnabled()) {
            return "Unavailable because URL protocol isn't wss";
        } else if (mCurrentValue == null || mCurrentValue.isEmpty()) {
            return "";
        } else {
            return SSLCertHelper.getCertFingerprint(mCurrentValue);
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing state
            mCurrentValue = this.getPersistedString(DEFAULT_VALUE);
        } else {
            // Set default state from the XML attribute
            mCurrentValue = DEFAULT_VALUE;
            persistString(mCurrentValue);
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        this.editTextView = (EditText) view.findViewById(R.id.pref_cert_edit_text);
        this.editTextView.setText(getPersistedString(""));

        view.findViewById(R.id.pref_cert_download_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadCertificate();
            }
        });

        view.findViewById(R.id.pref_cert_clear_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CertificatePreference.this.editTextView.setText("");
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            mCurrentValue = this.editTextView.getText().toString();
            this.persistString(mCurrentValue);
        }
    }

    protected void downloadCertificate() {

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        class CertDownloadTask extends AsyncTask<String, Void, String> {
            @Override
            protected String doInBackground(String... params) {
                URI url = null;
                try {
                    url = new URI(params[0]);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }

                Log.v(TAG, "Loading certificate from " + url);
                String cert = SSLCertHelper.getCert(url);
                Log.v(TAG, "Loaded certificate " + SSLCertHelper.getCertFingerprint(cert));
                return cert;
            }

            protected void onPostExecute(String result) {
                // Set text area value
                CertificatePreference.this.editTextView.setText(result);
            }
        }

        CertDownloadTask task = new CertDownloadTask();
        task.execute(pref.getString(SettingsFragment.KEY_PREF_URL, ""));
    }
}
