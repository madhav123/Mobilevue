package com.mobilvue.vod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.mobilevue.vod.R;
import com.mobilvue.data.ActivePlansData;
import com.mobilvue.data.ClientData;
import com.mobilvue.data.ClientResponseData;
import com.mobilvue.utils.MySSLSocketFactory;
import com.mobilvue.utils.ResponseObj;
import com.mobilvue.utils.Utilities;

public class AuthenticationAcitivity extends Activity {
	public static String TAG = "AuthenticationAcitivity";
	private final static String NETWORK_ERROR = "Network error.";
	public final static String PREFS_FILE = "PREFS_FILE";
	private SharedPreferences mPrefs;
	private Editor mPrefsEditor;
	private ProgressDialog mProgressDialog;
	Button button;
	int clientid;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_authentication);
		validateDevice();
	}

	private void validateDevice() {
		new ValidateDeviceAsyncTask().execute();
	}

	private class ValidateDeviceAsyncTask extends
			AsyncTask<Void, Void, ResponseObj> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			Log.d(TAG, "onPreExecute");
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			mProgressDialog = new ProgressDialog(AuthenticationAcitivity.this,
					ProgressDialog.THEME_HOLO_DARK);
			mProgressDialog.setMessage("Authenticating Details...");
			mProgressDialog.setCancelable(true);
			mProgressDialog.show();
		}

		@Override
		protected ResponseObj doInBackground(Void... arg0) {
			Log.d(TAG, "doInBackground");
			ResponseObj resObj = new ResponseObj();
			/** authentication deviceid */
			{
				if (Utilities.isNetworkAvailable(getApplicationContext())) {
					HashMap<String, String> map = new HashMap<String, String>();
					// String androidId = "efa4c6299";
					String androidId = Settings.Secure.getString(
							getApplicationContext().getContentResolver(),
							Settings.Secure.ANDROID_ID);
					map.put("TagURL", "mediadevices/" + androidId);
					resObj = Utilities.callExternalApiGetMethod(
							getApplicationContext(), map);

					if (resObj.getStatusCode() == 200) {
						try {
							Log.d(TAG, resObj.getsResponse());
							JSONObject clientJson = new JSONObject(
									resObj.getsResponse());
							clientid = (Integer) (clientJson.get("clientId"));
							mPrefs = getSharedPreferences(
									AuthenticationAcitivity.PREFS_FILE, 0);
							mPrefsEditor = mPrefs.edit();
							mPrefsEditor.putInt("CLIENTID", clientid);
							mPrefsEditor.commit();

							/** Calling client's plans data */
							{
								if (Utilities
										.isNetworkAvailable(getApplicationContext())) {
									map = new HashMap<String, String>();
									// String androidId = "efa4c6299";
									map.put("TagURL", "orders/" + clientid
											+ "/activeplans");
									resObj = Utilities
											.callExternalApiGetMethod(
													getApplicationContext(),
													map);
								} else {
									resObj.setFailResponse(100, NETWORK_ERROR);
									return resObj;
								}
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
					return resObj;
				} else {
					resObj.setFailResponse(100, NETWORK_ERROR);
					return resObj;
				}
			}
		}

		@Override
		protected void onPostExecute(ResponseObj resObj) {
			super.onPostExecute(resObj);
			Log.d(TAG, "onPostExecute");
			if (mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
			}
			if (resObj.getStatusCode() == 200) {
				Log.d("AuthActivity-Planlistdata", resObj.getsResponse());
				List<ActivePlansData> activePlansList = readJsonUser(resObj
						.getsResponse());
				if (!activePlansList.isEmpty()) {
					Intent intent = new Intent(AuthenticationAcitivity.this,
							PlanMenuActivity.class);
					Bundle bundle = new Bundle();
					bundle.putInt("CLIENTID", clientid);
					intent.putExtras(bundle);
					AuthenticationAcitivity.this.finish();
					startActivity(intent);
				} else {
					Intent intent = new Intent(AuthenticationAcitivity.this,
							PlanActivity.class);
					Bundle bundle = new Bundle();
					bundle.putInt("CLIENTID", clientid);
					intent.putExtras(bundle);
					AuthenticationAcitivity.this.finish();
					startActivity(intent);
				}

			} else if (resObj.getStatusCode() == 403) {
				Intent intent = new Intent(AuthenticationAcitivity.this,
						RegisterActivity.class);
				Bundle bundle = new Bundle();
				bundle.putInt("CLIENTID", clientid);
				intent.putExtras(bundle);
				AuthenticationAcitivity.this.finish();
				startActivity(intent);
			} else {
				Toast.makeText(AuthenticationAcitivity.this,
						resObj.getsErrorMessage(), Toast.LENGTH_LONG).show();
			}
		}
	}

	private List<ActivePlansData> readJsonUser(String jsonText) {
		Log.i("readJsonUser", "result is \r\n" + jsonText);
		List<ActivePlansData> data = null;
		try {
			ObjectMapper mapper = new ObjectMapper().setVisibility(
					JsonMethod.FIELD, Visibility.ANY);
			mapper.configure(
					DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,
					false);
			;
			data = mapper.readValue(jsonText,
					new TypeReference<List<ActivePlansData>>() {
					});
			System.out.println(data);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}
}