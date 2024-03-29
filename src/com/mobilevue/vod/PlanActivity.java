package com.mobilevue.vod;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.RadioButton;
import android.widget.Toast;

import com.mobilevue.adapter.CustomExpandableListAdapter;
import com.mobilevue.data.DeviceDatum;
import com.mobilevue.data.PlanDatum;
import com.mobilevue.data.ResponseObj;
import com.mobilevue.retrofit.OBSClient;
import com.mobilevue.utils.Utilities;

public class PlanActivity extends Activity {

	// public static String TAG = PlanActivity.class.getName();
	private final static String NETWORK_ERROR = "Network error.";
	private ProgressDialog mProgressDialog;

	MyApplication mApplication = null;
	OBSClient mOBSClient;
	boolean mIsReqCanceled = false;

	List<PlanDatum> mPlans;
	CustomExpandableListAdapter listAdapter;
	ExpandableListView expListView;
	public static int selectedGroupItem = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_plan);

		mApplication = ((MyApplication) getApplicationContext());
		mOBSClient = mApplication.getOBSClient();

		fetchAndBuildPlanList();
	}

	public void fetchAndBuildPlanList() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		mProgressDialog = new ProgressDialog(PlanActivity.this,
				ProgressDialog.THEME_HOLO_DARK);
		mProgressDialog.setMessage("Connecting Server...");
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setOnCancelListener(new OnCancelListener() {

			public void onCancel(DialogInterface arg0) {
				if (mProgressDialog.isShowing())
					mProgressDialog.dismiss();
				mIsReqCanceled = true;
			}
		});
		mProgressDialog.show();
		mOBSClient.getPrepaidPlans(getPlansCallBack);
	}

	final Callback<List<PlanDatum>> getPlansCallBack = new Callback<List<PlanDatum>>() {
		@Override
		public void failure(RetrofitError retrofitError) {
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (retrofitError.isNetworkError()) {
					Toast.makeText(
							PlanActivity.this,
							getApplicationContext().getString(
									R.string.error_network), Toast.LENGTH_LONG)
							.show();
				} else {
					Toast.makeText(
							PlanActivity.this,
							"Server Error : "
									+ retrofitError.getResponse().getStatus(),
							Toast.LENGTH_LONG).show();
				}
			} else
				mIsReqCanceled = false;
		}

		@Override
		public void success(List<PlanDatum> planList, Response response) {
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (planList != null) {
					mPlans = planList;
					buildPlansList();
				}
			} else
				mIsReqCanceled = false;
		}
	};

	private void buildPlansList() {
		expListView = (ExpandableListView) findViewById(R.id.a_exlv_plans_services);
		listAdapter = new CustomExpandableListAdapter(this, mPlans);
		expListView.setAdapter(listAdapter);
		expListView.setOnGroupClickListener(new OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v,
					int groupPosition, long id) {

				RadioButton rb1 = (RadioButton) v
						.findViewById(R.id.plan_list_plan_rb);
				if (null != rb1 && (!rb1.isChecked())) {
					PlanActivity.selectedGroupItem = groupPosition;
				} else {
					PlanActivity.selectedGroupItem = -1;
				}
				return false;
			}
		});

	}

	public void btnSubmit_onClick(View v) {
		if (selectedGroupItem >= 0) {
			orderPlans(mPlans.get(selectedGroupItem).toString());
		} else {
			Toast.makeText(getApplicationContext(), "Select a Plan",
					Toast.LENGTH_SHORT).show();
		}
	}

	public void btnCancel_onClick(View v) {
		closeApp();
	}

	private void closeApp() {
		AlertDialog mConfirmDialog = mApplication.getConfirmDialog(this);
		mConfirmDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (mProgressDialog != null) {
							mProgressDialog.dismiss();
							mProgressDialog = null;
						}
						mIsReqCanceled = true;
						PlanActivity.this.finish();
					}
				});
		mConfirmDialog.show();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			closeApp();
		}
		return super.onKeyDown(keyCode, event);
	}

	public void orderPlans(String planid) {
		new OrderPlansAsyncTask().execute();
	}

	private class OrderPlansAsyncTask extends
			AsyncTask<Void, Void, ResponseObj> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			mProgressDialog = new ProgressDialog(PlanActivity.this,
					ProgressDialog.THEME_HOLO_DARK);
			mProgressDialog.setMessage("Processing Order...");
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setOnCancelListener(new OnCancelListener() {

				public void onCancel(DialogInterface arg0) {
					if (mProgressDialog.isShowing())
						mProgressDialog.dismiss();
					cancel(true);
				}
			});
			mProgressDialog.show();
		}

		@Override
		protected ResponseObj doInBackground(Void... params) {
			PlanDatum plan = mPlans.get(selectedGroupItem);
			ResponseObj resObj = new ResponseObj();
			if (Utilities.isNetworkAvailable(getApplicationContext())) {
				HashMap<String, String> map = new HashMap<String, String>();
				Date date = new Date();
				SimpleDateFormat df = new SimpleDateFormat("dd MMMM yyyy",
						new Locale("en"));
				String formattedDate = df.format(date);

				map.put("TagURL", "/orders/" + mApplication.getClientId());
				map.put("planCode", plan.getId().toString());
				map.put("dateFormat", "dd MMMM yyyy");
				map.put("locale", "en");
				map.put("contractPeriod", plan.getContractId().toString());
				map.put("isNewplan", "true");
				map.put("start_date", formattedDate);
				map.put("billAlign", "false");
				map.put("paytermCode", plan.getServices().get(0)
						.getChargeCode());

				resObj = Utilities.callExternalApiPostMethod(
						getApplicationContext(), map);
			} else {
				resObj.setFailResponse(100, NETWORK_ERROR);
			}

			return resObj;
		}

		@Override
		protected void onPostExecute(ResponseObj resObj) {
			super.onPostExecute(resObj);
			if (mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
			}
			if (resObj.getStatusCode() == 200) {
				// update balance config n Values
				CheckBalancenGetData();
			} else {
				Toast.makeText(PlanActivity.this, resObj.getsErrorMessage(),
						Toast.LENGTH_LONG).show();
			}
		}
	}

	private void CheckBalancenGetData() {
		// Log.d("PlanActivity","CheckBalancenGetData");
		validateDevice();
	}

	private void validateDevice() {
		// Log.d("PlanActivity","validateDevice");
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}

		mProgressDialog = new ProgressDialog(this,
				ProgressDialog.THEME_HOLO_DARK);
		mProgressDialog.setMessage("Connecting Server...");
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setOnCancelListener(new OnCancelListener() {

			public void onCancel(DialogInterface arg0) {
				if (mProgressDialog.isShowing())
					mProgressDialog.dismiss();
				mProgressDialog = null;
				mIsReqCanceled = true;
			}
		});
		mProgressDialog.show();

		String androidId = Settings.Secure.getString(this
				.getApplicationContext().getContentResolver(),
				Settings.Secure.ANDROID_ID);
		mOBSClient.getMediaDevice(androidId, deviceCallBack);
	}

	final Callback<DeviceDatum> deviceCallBack = new Callback<DeviceDatum>() {

		@Override
		public void success(DeviceDatum device, Response arg1) {
			// Log.d("PlanActivity","success");
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (device != null) {
					try {
						mApplication.setClientId(Long.toString(device
								.getClientId()));
						mApplication.setBalance(device.getBalanceAmount());
						mApplication.setBalanceCheck(device.isBalanceCheck());
						mApplication.setCurrency(device.getCurrency());
						boolean isPayPalReq = device.getPaypalConfigData()
								.getEnabled();
						mApplication.setPayPalCheck(isPayPalReq);
						if (isPayPalReq) {
							String value = device.getPaypalConfigData()
									.getValue();
							if (value != null && value.length() > 0) {
								JSONObject json = new JSONObject(value);
								if (json != null) {
									mApplication.setPayPalClientID(json.get(
											"clientId").toString());
								}
							} else
								Toast.makeText(PlanActivity.this,
										"Invalid Data for PayPal details",
										Toast.LENGTH_LONG).show();
						}
					} catch (JSONException e) {
						Log.e("PlanActivity",
								(e.getMessage() == null) ? "Json Exception" : e
										.getMessage());
						Toast.makeText(PlanActivity.this,
								"Invalid Data-Json Error", Toast.LENGTH_LONG)
								.show();
					} catch (Exception e) {
						Log.e("PlanActivity",
								(e.getMessage() == null) ? "Exception" : e
										.getMessage());
						Toast.makeText(PlanActivity.this, "Invalid Data-Error",
								Toast.LENGTH_LONG).show();
					}
				}
				Intent intent = new Intent(PlanActivity.this,
						MainActivity.class);
				PlanActivity.this.finish();
				startActivity(intent);
				mIsReqCanceled = false;
			}

		}

		@Override
		public void failure(RetrofitError retrofitError) {
			// Log.d("ChannelsActivity","failure");
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (retrofitError.isNetworkError()) {
					Toast.makeText(PlanActivity.this,
							getString(R.string.error_network),
							Toast.LENGTH_LONG).show();
				} else if (retrofitError.getResponse().getStatus() == 403) {
					String msg = mApplication
							.getDeveloperMessage(retrofitError);
					msg = (msg != null && msg.length() > 0 ? msg
							: "Internal Server Error");
					Toast.makeText(PlanActivity.this, msg, Toast.LENGTH_LONG)
							.show();
				} else {
					Toast.makeText(
							PlanActivity.this,
							"Server Error : "
									+ retrofitError.getResponse().getStatus(),
							Toast.LENGTH_LONG).show();
				}
				Intent intent = new Intent(PlanActivity.this,
						MainActivity.class);
				PlanActivity.this.finish();
				startActivity(intent);
				mIsReqCanceled = false;
			}

		}
	};
}