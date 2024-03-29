package org.phomellolitepos;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.basewin.aidl.OnPrinterListener;
import com.basewin.define.FontsType;
import com.basewin.services.PrinterBinder;
import com.basewin.services.ServiceManager;
import com.hoin.btsdk.BluetoothService;
import com.iposprinter.iposprinterservice.IPosPrinterCallback;
import com.iposprinter.iposprinterservice.IPosPrinterService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.phomellolitepos.Util.ExceptionHandler;
import org.phomellolitepos.Util.Globals;
import org.phomellolitepos.database.Acc_Customer;
import org.phomellolitepos.database.Acc_Customer_Credit;
import org.phomellolitepos.database.Contact;
import org.phomellolitepos.database.Database;
import org.phomellolitepos.database.Item;
import org.phomellolitepos.database.Lite_POS_Registration;
import org.phomellolitepos.database.Manufacture;
import org.phomellolitepos.database.Order_Detail;
import org.phomellolitepos.database.Settings;
import org.phomellolitepos.database.User;
import org.phomellolitepos.printer.BytesUtil;
import org.phomellolitepos.printer.MemInfo;
import org.phomellolitepos.printer.PrintLayout;
import org.phomellolitepos.printer.ThreadPoolManager;
import org.phomellolitepos.utils.HandlerUtils;
import org.phomellolitepos.utils.TimerCountTools;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import woyou.aidlservice.jiuiv5.ICallback;
import woyou.aidlservice.jiuiv5.IWoyouService;

public class AccountsActivity extends AppCompatActivity {
    TextView txt_cus_code, txt_cus_name, txt_cr_amt, txt_cr_lc_amt, txt_total_amt, txt_type, txt_type_total;
    EditText edt_pd_amt;
    Button btn_paid;
    ProgressDialog pDialog;
    String operation, code;
    Database db;
    SQLiteDatabase database;
    String date;
    Lite_POS_Registration lite_pos_registration;
    private ICallback callback = null;
    private IWoyouService woyouService;
    Settings settings;
    String decimal_check;
    String data;
    String modified_by;
    Contact contact;
    String ck_project_type;
    String strCRAmount;
    private static final String TAG = "PrinterTestDemo";
    private String PrinterType = "";
    private TimerCountTools timeTools;
    JSONObject printJson = new JSONObject();
    private PrinterListener printer_callback = new PrinterListener();
    public static PrinterBinder printer;
    BluetoothService mService = null;

    /*定义打印机状态*/
    private final int PRINTER_NORMAL = 0;
    /*打印机当前状态*/
    private int printerStatus = 0;

    /*定义状态广播*/
    private final String PRINTER_NORMAL_ACTION = "com.iposprinter.iposprinterservice.NORMAL_ACTION";
    private final String PRINTER_PAPERLESS_ACTION = "com.iposprinter.iposprinterservice.PAPERLESS_ACTION";
    private final String PRINTER_PAPEREXISTS_ACTION = "com.iposprinter.iposprinterservice.PAPEREXISTS_ACTION";
    private final String PRINTER_THP_HIGHTEMP_ACTION = "com.iposprinter.iposprinterservice.THP_HIGHTEMP_ACTION";
    private final String PRINTER_THP_NORMALTEMP_ACTION = "com.iposprinter.iposprinterservice.THP_NORMALTEMP_ACTION";
    private final String PRINTER_MOTOR_HIGHTEMP_ACTION = "com.iposprinter.iposprinterservice.MOTOR_HIGHTEMP_ACTION";
    private final String PRINTER_BUSY_ACTION = "com.iposprinter.iposprinterservice.BUSY_ACTION";
    private final String PRINTER_CURRENT_TASK_PRINT_COMPLETE_ACTION = "com.iposprinter.iposprinterservice.CURRENT_TASK_PRINT_COMPLETE_ACTION";

    /*定义消息*/
    private final int MSG_TEST = 1;
    private final int MSG_IS_NORMAL = 2;
    private final int MSG_IS_BUSY = 3;
    private final int MSG_PAPER_LESS = 4;
    private final int MSG_PAPER_EXISTS = 5;
    private final int MSG_THP_HIGH_TEMP = 6;
    private final int MSG_THP_TEMP_NORMAL = 7;
    private final int MSG_MOTOR_HIGH_TEMP = 8;
    private final int MSG_MOTOR_HIGH_TEMP_INIT_PRINTER = 9;
    private final int MSG_CURRENT_TASK_PRINT_COMPLETE = 10;


    private final int DEFAULT_LOOP_PRINT = 0;

    //循环打印标志位
    private int loopPrintFlag = DEFAULT_LOOP_PRINT;


    private IPosPrinterService mIPosPrinterService;
    private IPosPrinterCallback callbackPPT8555 = null;

    private Random random = new Random();
    private HandlerUtils.MyHandler handlerPPT8555;

    private ServiceConnection connService = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            woyouService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            woyouService = IWoyouService.Stub.asInterface(service);
        }
    };

    private long printCount = 0;

    /**
     * 消息处理
     */
    private HandlerUtils.IHandlerIntent iHandlerIntent = new HandlerUtils.IHandlerIntent() {
        @Override
        public void handlerIntent(Message msg) {
            switch (msg.what) {
                case MSG_TEST:
                    break;
                case MSG_IS_NORMAL:
                    if (getPrinterStatus() == PRINTER_NORMAL) {
//                        loopPrint(loopPrintFlag);
                    }
                    break;
                case MSG_IS_BUSY:
//                    Toast.makeText(IPosPrinterTestDemo.this, R.string.printer_is_working, Toast.LENGTH_SHORT).show();
                    break;
                case MSG_PAPER_LESS:
                    loopPrintFlag = DEFAULT_LOOP_PRINT;
//                    Toast.makeText(IPosPrinterTestDemo.this, R.string.out_of_paper, Toast.LENGTH_SHORT).show();
                    break;
                case MSG_PAPER_EXISTS:
//                    Toast.makeText(IPosPrinterTestDemo.this, R.string.exists_paper, Toast.LENGTH_SHORT).show();
                    break;
                case MSG_THP_HIGH_TEMP:
//                    Toast.makeText(IPosPrinterTestDemo.this, R.string.printer_high_temp_alarm, Toast.LENGTH_SHORT).show();
                    break;
                case MSG_MOTOR_HIGH_TEMP:
                    loopPrintFlag = DEFAULT_LOOP_PRINT;
//                    Toast.makeText(IPosPrinterTestDemo.this, R.string.motor_high_temp_alarm, Toast.LENGTH_SHORT).show();
                    handlerPPT8555.sendEmptyMessageDelayed(MSG_MOTOR_HIGH_TEMP_INIT_PRINTER, 180000);  //马达高温报警，等待3分钟后复位打印机
                    break;
                case MSG_MOTOR_HIGH_TEMP_INIT_PRINTER:
                    printerInit();
                    break;
                case MSG_CURRENT_TASK_PRINT_COMPLETE:
//                    Toast.makeText(IPosPrinterTestDemo.this, R.string.printer_current_task_print_complete, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    private BroadcastReceiver IPosPrinterStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                Log.d(TAG, "IPosPrinterStatusListener onReceive action = null");
                return;
            }
            Log.d(TAG, "IPosPrinterStatusListener action = " + action);
            if (action.equals(PRINTER_NORMAL_ACTION)) {
                handlerPPT8555.sendEmptyMessageDelayed(MSG_IS_NORMAL, 0);
            } else if (action.equals(PRINTER_PAPERLESS_ACTION)) {
                handlerPPT8555.sendEmptyMessageDelayed(MSG_PAPER_LESS, 0);
            } else if (action.equals(PRINTER_BUSY_ACTION)) {
                handlerPPT8555.sendEmptyMessageDelayed(MSG_IS_BUSY, 0);
            } else if (action.equals(PRINTER_PAPEREXISTS_ACTION)) {
                handlerPPT8555.sendEmptyMessageDelayed(MSG_PAPER_EXISTS, 0);
            } else if (action.equals(PRINTER_THP_HIGHTEMP_ACTION)) {
                handlerPPT8555.sendEmptyMessageDelayed(MSG_THP_HIGH_TEMP, 0);
            } else if (action.equals(PRINTER_THP_NORMALTEMP_ACTION)) {
                handlerPPT8555.sendEmptyMessageDelayed(MSG_THP_TEMP_NORMAL, 0);
            } else if (action.equals(PRINTER_MOTOR_HIGHTEMP_ACTION))  //此时当前任务会继续打印，完成当前任务后，请等待2分钟以上时间，继续下一个打印任务
            {
                handlerPPT8555.sendEmptyMessageDelayed(MSG_MOTOR_HIGH_TEMP, 0);
            } else if (action.equals(PRINTER_CURRENT_TASK_PRINT_COMPLETE_ACTION)) {
                handlerPPT8555.sendEmptyMessageDelayed(MSG_CURRENT_TASK_PRINT_COMPLETE, 0);
            } else {
                handlerPPT8555.sendEmptyMessageDelayed(MSG_TEST, 0);
            }
        }
    };

    /**
     * 绑定服务实例
     */
    private ServiceConnection connectService = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mIPosPrinterService = IPosPrinterService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIPosPrinterService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));

        db = new Database(getApplicationContext());
        database = db.getWritableDatabase();
        modified_by = Globals.user;
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", Context.MODE_MULTI_PROCESS); // 0 - for private mode
        int id = pref.getInt("id", 0);
        if (id == 0) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp_mdpi);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_forward_black_24dp);
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pDialog = new ProgressDialog(AccountsActivity.this);
                pDialog.setCancelable(false);
                pDialog.setMessage(getString(R.string.Wait_msg));
                pDialog.show();

                Thread timerThread = new Thread() {
                    public void run() {
                        try {
                            sleep(1000);
                            Intent intent = new Intent(AccountsActivity.this, AccountsListActivity.class);
                            startActivity(intent);
                            pDialog.dismiss();
                            finish();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                        }
                    }
                };
                timerThread.start();
            }
        });

        settings = Settings.getSettings(getApplicationContext(), database, "");
        if (settings == null) {
            PrinterType = "";
        } else {
            try {
                PrinterType = settings.getPrinterId();
            } catch (Exception ex) {
                PrinterType = "";
            }
        }

        if (PrinterType.equals("7")) {
            try {
                ServiceManager.getInstence().init(AccountsActivity.this);
            } catch (Exception ex) {
            }
        }

        callback = new ICallback.Stub() {

            @Override
            public void onRunResult(final boolean success) throws RemoteException {
            }

            @Override
            public void onReturnString(final String value) throws RemoteException {
                Log.i(TAG, "printlength:" + value + "\n");
            }

            @Override
            public void onRaiseException(int code, final String msg) throws RemoteException {
                Log.i(TAG, "onRaiseException: " + msg);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }
        };

        handlerPPT8555 = new HandlerUtils.MyHandler(iHandlerIntent);
        callbackPPT8555 = new IPosPrinterCallback.Stub() {

            @Override
            public void onRunResult(final boolean isSuccess) throws RemoteException {
                Log.i(TAG,"result:" + isSuccess + "\n");
            }

            @Override
            public void onReturnString(final String value) throws RemoteException {
                Log.i(TAG,"result:" + value + "\n");
            }
        };

        //绑定服务
        Intent intent=new Intent();
        intent.setPackage("com.iposprinter.iposprinterservice");
        intent.setAction("com.iposprinter.iposprinterservice.IPosPrintService");
//        startService(intent);
        bindService(intent, connectService, Context.BIND_AUTO_CREATE);

        //注册打印机状态接收器
        IntentFilter printerStatusFilter = new IntentFilter();
        printerStatusFilter.addAction(PRINTER_NORMAL_ACTION);
        printerStatusFilter.addAction(PRINTER_PAPERLESS_ACTION);
        printerStatusFilter.addAction(PRINTER_PAPEREXISTS_ACTION);
        printerStatusFilter.addAction(PRINTER_THP_HIGHTEMP_ACTION);
        printerStatusFilter.addAction(PRINTER_THP_NORMALTEMP_ACTION);
        printerStatusFilter.addAction(PRINTER_MOTOR_HIGHTEMP_ACTION);
        printerStatusFilter.addAction(PRINTER_BUSY_ACTION);
        registerReceiver(IPosPrinterStatusListener,printerStatusFilter);

        Intent intent_1 = new Intent();
        intent_1.setPackage("woyou.aidlservice.jiuiv5");
        intent_1.setAction("woyou.aidlservice.jiuiv5.IWoyouService");
        startService(intent_1);
        bindService(intent_1, connService, Context.BIND_AUTO_CREATE);
        intent = getIntent();
        getSupportActionBar().setTitle(R.string.accounts);
        code = intent.getStringExtra("contact_code");
        contact = Contact.getContact(getApplicationContext(), database, db, "WHERE contact_code='" + code + "'");
        Date d = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        date = format.format(d);
        mService = MainActivity.mService;
        txt_cus_code = (TextView) findViewById(R.id.txt_cus_code);
        txt_cus_name = (TextView) findViewById(R.id.txt_cus_name);
        txt_cr_amt = (TextView) findViewById(R.id.txt_cr_amt);
        txt_type = (TextView) findViewById(R.id.txt_type);
        txt_cr_lc_amt = (TextView) findViewById(R.id.txt_cr_lc_amt);
        txt_type_total = (TextView) findViewById(R.id.txt_type_total);
        txt_total_amt = (TextView) findViewById(R.id.txt_total_amt);
        edt_pd_amt = (EditText) findViewById(R.id.edt_pd_amt);
        btn_paid = (Button) findViewById(R.id.btn_paid);
        edt_pd_amt.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        try {
            decimal_check = Globals.objLPD.getDecimal_Place();
        } catch (Exception ex) {
            decimal_check = "1";
        }
        try {
            txt_cus_code.setText(code);
        } catch (Exception ex) {
            txt_cus_code.setText("");
        }

        try {
            txt_cus_name.setText(contact.get_name());
        } catch (Exception ex) {
            txt_cus_name.setText("");
        }

        lite_pos_registration = Lite_POS_Registration.getRegistration(getApplicationContext(), database, db, "");
        ck_project_type = lite_pos_registration.getproject_id();

        if (ck_project_type.equals("standalone")) {
            try {
                String strCreditAmt = "", strDeditAmount = "";
                Double creditAmount = 0d, debitAmount = 0d;
                Cursor cursor = null;

                String strQry1 = "Select SUM(paid_amount - cr_amount) FROM Acc_Customer_Credit where contact_code ='" + code + "'";
                cursor = database.rawQuery(strQry1, null);
                while (cursor.moveToNext()) {
                    strCreditAmt = cursor.getString(0);
                }
                try {
                    creditAmount = Double.parseDouble(strCreditAmt);
                } catch (Exception ex) {
                }
                String strQry2 = "Select SUM(amount) from acc_customer_dedit Where order_code IN (Select Order_code from orders where contact_code ='" + code + "')";
                cursor = database.rawQuery(strQry2, null);
                while (cursor.moveToNext()) {
                    strDeditAmount = cursor.getString(0);
                }
                try {
                    debitAmount = Double.parseDouble(strDeditAmount);
                } catch (Exception ex) {
                }
                Double showAmount = debitAmount + creditAmount;

                double abs1 = 0d;
                Double strCheckAmmt;
                try {
                    strCheckAmmt = showAmount;
                } catch (Exception ex) {
                    strCheckAmmt = 0d;
                }
                abs1 = Math.abs(strCheckAmmt);
                if (strCheckAmmt > 0) {
                    txt_cr_amt.setText(Globals.myNumberFormat2Price(0, decimal_check));
                    edt_pd_amt.setText(Globals.myNumberFormat2Price(abs1, decimal_check));
                    txt_type_total.setText("DR");
                    txt_type_total.setTextColor(Color.parseColor("#228B22"));
                    txt_total_amt.setText(Globals.myNumberFormat2Price(abs1, decimal_check));
                } else {
                    txt_cr_amt.setText(Globals.myNumberFormat2Price(0, decimal_check));
                    edt_pd_amt.setText(Globals.myNumberFormat2Price(abs1, decimal_check));
                    txt_type_total.setText("CR");
                    txt_type_total.setTextColor(Color.parseColor("#FF0000"));
                    txt_total_amt.setText(Globals.myNumberFormat2Price(abs1, decimal_check));
                }
                txt_cr_lc_amt.setText(Globals.myNumberFormat2Price(abs1, decimal_check));
                edt_pd_amt.requestFocus();
                edt_pd_amt.selectAll();
            } catch (Exception e) {
            }
        } else {
            pDialog = new ProgressDialog(AccountsActivity.this);
            pDialog.setCancelable(false);
            pDialog.setMessage(getString(R.string.waiting));
            pDialog.show();
            new Thread() {
                @Override
                public void run() {
                    GetCustomerCreditDetail();
                    pDialog.dismiss();
//                    runOnUiThread(new Runnable() {
//                        public void run() {
//                            edt_pd_amt.setText("21.000");
//                            txt_cr_amt.setText("21.000");
//                        }
//                    });
                }
            }.start();
        }

        edt_pd_amt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (edt_pd_amt.getText().toString().trim().equals("")) {
                    return false;
                } else {
                    edt_pd_amt.requestFocus();
                    edt_pd_amt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    InputMethodManager imm4 = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm4.showSoftInput(edt_pd_amt, InputMethodManager.SHOW_IMPLICIT);
                    edt_pd_amt.selectAll();
                    return true;
                }
            }
        });


        btn_paid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                        AccountsActivity.this);
                alertDialog.setTitle("Confirmation");
                alertDialog
                        .setMessage("Are you sure?");
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);

                alertDialog.setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                pay_amount();
                            }
                        });

                alertDialog.setNegativeButton("No",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int which) {
                            }
                        });
                AlertDialog alert = alertDialog.create();

                alert.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        lite_pos_registration = Lite_POS_Registration.getRegistration(getApplicationContext(), database, db, "");
                        String ck_project_type = lite_pos_registration.getproject_id();

                        if (ck_project_type.equals("standalone")) {
                            ((AlertDialog) dialog).getButton(
                                    AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                            ((AlertDialog) dialog).getButton(
                                    AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                        } else if (lite_pos_registration.getIndustry_Type().equals("3") || lite_pos_registration.getIndustry_Type().equals("6")) {
                            ((AlertDialog) dialog).getButton(
                                    AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                        }
                    }
                });
                alert.show();
                Button nbutton = alert.getButton(DialogInterface.BUTTON_NEGATIVE);
                nbutton.setTextColor(getResources().getColor(R.color.colorPrimary));
                Button pbutton = alert.getButton(DialogInterface.BUTTON_POSITIVE);
                pbutton.setTextColor(getResources().getColor(R.color.colorPrimary));
            }
        });
    }

    private void pay_amount() {
        if (edt_pd_amt.getText().toString().trim().equals("")) {
            edt_pd_amt.setError("Enter paid amount");
            edt_pd_amt.requestFocus();
            return;
        }

        if (edt_pd_amt.getText().toString().trim().equals(".")) {
            edt_pd_amt.setError("Can't enter decimal only");
            edt_pd_amt.requestFocus();
            edt_pd_amt.selectAll();
            return;
        }

        if (Double.parseDouble(edt_pd_amt.getText().toString()) > 0) {

            if (isNetworkStatusAvialable(getApplicationContext())) {
                if (txt_cr_amt.getText().toString().trim().equals("")) {
                    txt_cr_amt.setError("No Credit Found");
                    txt_cr_amt.requestFocus();
                    return;
                }
                if (edt_pd_amt.getText().toString().trim().equals("")) {
                    edt_pd_amt.setError("Paid Amount Required");
                    edt_pd_amt.requestFocus();
                    return;
                }

                if (ck_project_type.equals("standalone")) {

                    pDialog = new ProgressDialog(AccountsActivity.this);
                    pDialog.setCancelable(false);
                    pDialog.setMessage(getString(R.string.waiting));
                    pDialog.show();
                    new Thread() {
                        @Override
                        public void run() {
                            String result = SaveTransaction();
                            if (result.equals("1")) {
                                pDialog.dismiss();
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Acc_Customer acc_customer = Acc_Customer.getAcc_Customer(getApplicationContext(), " where contact_code='" + code + "'", database);
                                        if (acc_customer == null) {
                                        } else {
                                            Double strOldBalance = 0d;
                                            Double strAmount = 0d;
                                            strOldBalance = Double.parseDouble(acc_customer.get_amount());
                                            strAmount = strOldBalance + Double.parseDouble(edt_pd_amt.getText().toString());
                                            acc_customer.set_amount(strAmount + "");
                                            long a = acc_customer.updateAcc_Customer("contact_code=?", new String[]{Globals.strContact_Code}, database);
                                        }
//                                                if (PrinterType.equals("1")) {
                                        try {

                                            print();

                                        } catch (Exception ex) {
                                        }
//                                                }
                                        Toast.makeText(AccountsActivity.this, "Tranction successful", Toast.LENGTH_SHORT).show();
                                        Intent intent1 = new Intent(AccountsActivity.this, AccountsListActivity.class);
                                        startActivity(intent1);
                                        finish();
                                    }
                                });
                            } else {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        pDialog.dismiss();
                                        Toast.makeText(AccountsActivity.this, "Tranction error", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }.start();
                } else {
                    pDialog = new ProgressDialog(AccountsActivity.this);
                    pDialog.setCancelable(false);
                    pDialog.setMessage(getString(R.string.waiting));
                    pDialog.show();
                    new Thread() {
                        @Override
                        public void run() {
                            String result = SendCustomerCreditDetail();
                            if (result.equals("1")) {
                                pDialog.dismiss();
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Acc_Customer acc_customer = Acc_Customer.getAcc_Customer(getApplicationContext(), " where contact_code='" + code + "'", database);
                                        Double strOldBalance = 0d;
                                        Double strAmount = 0d;
                                        if (acc_customer == null) {
//                                                    strOldBalance = Double.parseDouble(acc_customer.get_amount());
                                            strAmount = strOldBalance + Double.parseDouble(edt_pd_amt.getText().toString());
                                            acc_customer = new Acc_Customer(getApplicationContext(), null, code, strAmount + "");
                                            acc_customer.insertAcc_Customer(database);
                                        } else {
                                            strOldBalance = Double.parseDouble(acc_customer.get_amount());
                                            strAmount = strOldBalance + Double.parseDouble(edt_pd_amt.getText().toString());
                                            acc_customer.set_amount(strAmount + "");
                                            long a = acc_customer.updateAcc_Customer("contact_code=?", new String[]{code}, database);
                                        }

//                                                if (PrinterType.equals("1")) {
                                        try {

                                            print();

                                        } catch (Exception ex) {
                                        }

//                                                }
                                        Toast.makeText(AccountsActivity.this, "Tranction successful", Toast.LENGTH_SHORT).show();
                                        Intent intent1 = new Intent(AccountsActivity.this, AccountsListActivity.class);
                                        startActivity(intent1);
                                        finish();
                                    }
                                });
                            } else {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        pDialog.dismiss();
                                        Toast.makeText(AccountsActivity.this, "Tranction error", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }.start();
                }

            } else {
                pDialog.dismiss();
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.nointernet), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Can't paid", Toast.LENGTH_SHORT).show();
        }
    }

    private String SaveTransaction() {

        String succ = "0";
        database.beginTransaction();

        try {

            Acc_Customer_Credit acc_customer_credit = new Acc_Customer_Credit(getApplicationContext(), null, date, txt_cus_code.getText().toString(), "0", edt_pd_amt.getText().toString().trim(), "0", "0", "1", modified_by, date);
            long l = acc_customer_credit.insertAcc_Customer_Credit(database);
            if (l > 0) {
                succ = "1";
            }

            if (succ.equals("1")) {
                database.setTransactionSuccessful();
                database.endTransaction();
            }
        } catch (Exception e) {
            database.setTransactionSuccessful();
            database.endTransaction();
        }
        return succ;
    }

    private void print() {
        if (ck_project_type.equals("standalone")) {

            if (PrinterType.equals("7")) {
                try {
                    for (int k = 0; k < Integer.parseInt(settings.get_No_Of_Print()); k++) {
                        JSONArray printTest = new JSONArray();
                        timeTools = new TimerCountTools();
                        timeTools.start();
                        ServiceManager.getInstence().getPrinter().setPrintFont(FontsType.simsun);
                        String Print_type = "0";

                        printTest.put(getPrintObject(Globals.objLPR.getCompany_Name(), "3", "center"));
                        printTest.put(getPrintObject(Globals.objLPR.getAddress(), "3", "center"));
                        printTest.put(getPrintObject(Globals.objLPR.getMobile_No(), "3", "center"));
                        try {
                            if (Globals.objLPR.getService_code_tariff().equals("null") || Globals.objLPR.getService_code_tariff().equals("")) {

                            } else {
                                printTest.put(getPrintObject(Globals.objLPR.getService_code_tariff(), "3", "center"));
                            }
                        } catch (Exception ex) {
                        }

                        if (Globals.objLPR.getLicense_No().equals("null") || Globals.objLPR.getLicense_No().equals("")) {
                        } else {

                            printTest.put(getPrintObject(Globals.GSTNo + ":" + Globals.objLPR.getLicense_No(), "3", "center"));
                        }

                        printTest.put(getPrintObject("Payment Receipt", "3", "center"));

                        printTest.put(getPrintObject("Company Name" + ":" + contact.get_company_name(), "2", "left"));

                        printTest.put(getPrintObject("Customer Code" + ":" + txt_cus_code.getText().toString(), "2", "left"));

                        printTest.put(getPrintObject("Customer Name" + ":" + txt_cus_name.getText().toString(), "2", "left"));

                        printTest.put(getPrintObject("Contact No" + ":" + contact.get_contact_1(), "2", "left"));

                        printTest.put(getPrintObject("Date" + ":" + date.substring(0, 10), "2", "left"));

                        printTest.put(getPrintObject("Old Balance", "2", "left"));

                        printTest.put(getPrintObject(Globals.myNumberFormat2Price(Double.parseDouble(txt_total_amt.getText().toString()), decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ")", "2", "right"));

                        printTest.put(getPrintObject("Received", "2", "left"));

                        printTest.put(getPrintObject(Globals.myNumberFormat2Price(Double.parseDouble(edt_pd_amt.getText().toString()), decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ")", "2", "right"));


                        Double ab = Double.parseDouble(txt_total_amt.getText().toString()) + Double.parseDouble(edt_pd_amt.getText().toString());
                        double abs1 = Math.abs(ab);

                        if (abs1 > 0) {

                            printTest.put(getPrintObject("Current Balance", "2", "left"));

                            printTest.put(getPrintObject(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") DR", "2", "right"));


                        } else {

                            printTest.put(getPrintObject("Current Balance", "2", "left"));

                            printTest.put(getPrintObject(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") CR", "2", "right"));
                        }

                        User user = User.getUser(getApplicationContext(), " Where user_code='" + Globals.user + "'", database);
                        ServiceManager.getInstence().getPrinter().printBottomFeedLine(4);
                        printTest.put(getPrintObject("Signature :" + user.get_name(), "2", "left"));
                        printJson.put("spos", printTest);
                        // 设置底部空3行
                        printJson.put("spos", printTest);
                        ServiceManager.getInstence().getPrinter().print(printJson.toString(), null, printer_callback);
                        ServiceManager.getInstence().getPrinter().printBottomFeedLine(5);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (PrinterType.equals("3")) {

            } else if (PrinterType.equals("4")) {

            } else if (PrinterType.equals("8")) {
                ThreadPoolManager.getInstance().executeTask(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            for (int k = 0; k < Integer.parseInt(settings.get_No_Of_Print()); k++) {
                                String Print_type = "0";
                                mIPosPrinterService.setPrinterPrintFontSize(35, callbackPPT8555);
                                mIPosPrinterService.setPrinterPrintAlignment(1, callbackPPT8555);
                                mIPosPrinterService.printSpecifiedTypeText("" + Globals.objLPR.getCompany_Name() + "\n", "", 35, callbackPPT8555);
                                mIPosPrinterService.printSpecifiedTypeText("" + Globals.objLPR.getAddress() + "\n", "", 35, callbackPPT8555);
                                mIPosPrinterService.printSpecifiedTypeText("" + Globals.objLPR.getMobile_No() + "\n", "", 35, callbackPPT8555);
                                try {
                                    if (Globals.objLPR.getService_code_tariff().equals("null") || Globals.objLPR.getService_code_tariff().equals("")) {

                                    } else {
                                        mIPosPrinterService.printSpecifiedTypeText("" + Globals.objLPR.getService_code_tariff() + "\n", "", 35, callbackPPT8555);
                                    }
                                } catch (Exception ex) {
                                }

                                if (Globals.objLPR.getLicense_No().equals("null") || Globals.objLPR.getLicense_No().equals("")) {
                                } else {
                                    mIPosPrinterService.printColumnsText(new String[]{Globals.GSTNo, ":", Globals.objLPR.getLicense_No()}, new int[]{6, 1, 20}, new int[]{0, 0, 0}, 0, callbackPPT8555);
                                }

                                mIPosPrinterService.setPrinterPrintAlignment(1, callbackPPT8555);
                                mIPosPrinterService.printSpecifiedTypeText("Payment Receipt\n", "", 40, callbackPPT8555);
                                mIPosPrinterService.setPrinterPrintAlignment(0, callbackPPT8555);
                                mIPosPrinterService.printColumnsText(new String[]{"Company Name", ":", contact.get_company_name()}, new int[]{13, 1, 20}, new int[]{0, 0, 0}, 0, callbackPPT8555);
                                mIPosPrinterService.printColumnsText(new String[]{"Customer Code", ":", txt_cus_code.getText().toString()}, new int[]{13, 1, 20}, new int[]{0, 0, 0}, 0, callbackPPT8555);
                                mIPosPrinterService.printColumnsText(new String[]{"Customer Name", ":", txt_cus_name.getText().toString()}, new int[]{13, 1, 20}, new int[]{0, 0, 0}, 0, callbackPPT8555);
                                mIPosPrinterService.printColumnsText(new String[]{"Contact No", ":", contact.get_contact_1()}, new int[]{11, 1, 20}, new int[]{0, 0, 0}, 0, callbackPPT8555);
                                mIPosPrinterService.printColumnsText(new String[]{"Date", ":", date.substring(0, 10)}, new int[]{5, 1, 20}, new int[]{0, 0, 0}, 0, callbackPPT8555);
                                mIPosPrinterService.printSpecifiedTypeText("Old Balance\n", "", 35, callbackPPT8555);
                                mIPosPrinterService.setPrinterPrintAlignment(2, callbackPPT8555);
                                mIPosPrinterService.printSpecifiedTypeText(Globals.myNumberFormat2Price(Double.parseDouble(txt_total_amt.getText().toString()), decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ")" + "\n", "", 35, callbackPPT8555);
                                mIPosPrinterService.setPrinterPrintAlignment(0, callbackPPT8555);
                                mIPosPrinterService.printSpecifiedTypeText("Received\n", "", 35, callbackPPT8555);
                                mIPosPrinterService.setPrinterPrintAlignment(2, callbackPPT8555);
                                mIPosPrinterService.printSpecifiedTypeText(Globals.myNumberFormat2Price(Double.parseDouble(edt_pd_amt.getText().toString()), decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ")" + "\n", "", 35, callbackPPT8555);
                                mIPosPrinterService.setPrinterPrintAlignment(0, callbackPPT8555);

                                Double ab = Double.parseDouble(txt_total_amt.getText().toString()) + Double.parseDouble(edt_pd_amt.getText().toString());
                                double abs1 = Math.abs(ab);

                                if (abs1 > 0) {
                                    mIPosPrinterService.printSpecifiedTypeText("Current Balance\n", "", 35, callbackPPT8555);
                                    mIPosPrinterService.setPrinterPrintAlignment(2, callbackPPT8555);
                                    mIPosPrinterService.printSpecifiedTypeText(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") DR" + "\n", "", 35, callbackPPT8555);
                                    mIPosPrinterService.setPrinterPrintAlignment(0, callbackPPT8555);

                                } else {
                                    mIPosPrinterService.printSpecifiedTypeText("Current Balance\n", "", 35, callbackPPT8555);
                                    mIPosPrinterService.setPrinterPrintAlignment(2, callbackPPT8555);
                                    mIPosPrinterService.printSpecifiedTypeText(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") CR" + "\n", "", 35, callbackPPT8555);
                                }
                                mIPosPrinterService.setPrinterPrintAlignment(0, callbackPPT8555);
                                User user = User.getUser(getApplicationContext(), " Where user_code='" + Globals.user + "'", database);
                                mIPosPrinterService.printBlankLines(1, 8, callbackPPT8555);
                                mIPosPrinterService.printBlankLines(1, 8, callbackPPT8555);
                                mIPosPrinterService.printBlankLines(1, 8, callbackPPT8555);
                                mIPosPrinterService.printSpecifiedTypeText("Signature :" + user.get_name() + "\n", "", 35, callbackPPT8555);
                                mIPosPrinterService.printBlankLines(1, 8, callbackPPT8555);
                                mIPosPrinterService.printBlankLines(1, 8, callbackPPT8555);
                                mIPosPrinterService.printBlankLines(1, 8, callbackPPT8555);
                                mIPosPrinterService.printerPerformPrint(160,  callbackPPT8555);
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                if (woyouService == null) {
                } else {

                    ThreadPoolManager.getInstance().executeTask(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                for (int k = 0; k < Integer.parseInt(settings.get_No_Of_Print()); k++) {
                                    String Print_type = "0";
                                    woyouService.setFontSize(35, callback);
                                    woyouService.setAlignment(1, callback);
                                    woyouService.printTextWithFont("" + Globals.objLPR.getCompany_Name() + "\n", "", 35, callback);
                                    woyouService.printTextWithFont("" + Globals.objLPR.getAddress() + "\n", "", 35, callback);
                                    woyouService.printTextWithFont("" + Globals.objLPR.getMobile_No() + "\n", "", 35, callback);
                                    try {
                                        if (Globals.objLPR.getService_code_tariff().equals("null") || Globals.objLPR.getService_code_tariff().equals("")) {

                                        } else {
                                            woyouService.printTextWithFont("" + Globals.objLPR.getService_code_tariff() + "\n", "", 35, callback);
                                        }
                                    } catch (Exception ex) {
                                    }

                                    if (Globals.objLPR.getLicense_No().equals("null") || Globals.objLPR.getLicense_No().equals("")) {
                                    } else {
                                        woyouService.printColumnsText(new String[]{Globals.GSTNo, ":", Globals.objLPR.getLicense_No()}, new int[]{6, 1, 20}, new int[]{0, 0, 0}, callback);
                                    }

                                    woyouService.setAlignment(1, callback);
                                    woyouService.printTextWithFont("Payment Receipt\n", "", 40, callback);
                                    woyouService.setAlignment(0, callback);
                                    woyouService.printColumnsText(new String[]{"Company Name", ":", contact.get_company_name()}, new int[]{13, 1, 20}, new int[]{0, 0, 0}, callback);
                                    woyouService.printColumnsText(new String[]{"Customer Code", ":", txt_cus_code.getText().toString()}, new int[]{13, 1, 20}, new int[]{0, 0, 0}, callback);
                                    woyouService.printColumnsText(new String[]{"Customer Name", ":", txt_cus_name.getText().toString()}, new int[]{13, 1, 20}, new int[]{0, 0, 0}, callback);
                                    woyouService.printColumnsText(new String[]{"Contact No", ":", contact.get_contact_1()}, new int[]{11, 1, 20}, new int[]{0, 0, 0}, callback);
                                    woyouService.printColumnsText(new String[]{"Date", ":", date.substring(0, 10)}, new int[]{5, 1, 20}, new int[]{0, 0, 0}, callback);
                                    woyouService.printTextWithFont("Old Balance\n", "", 35, callback);
                                    woyouService.setAlignment(2, callback);
                                    woyouService.printTextWithFont(Globals.myNumberFormat2Price(Double.parseDouble(txt_total_amt.getText().toString()), decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ")" + "\n", "", 35, callback);
                                    woyouService.setAlignment(0, callback);
                                    woyouService.printTextWithFont("Received\n", "", 35, callback);
                                    woyouService.setAlignment(2, callback);
                                    woyouService.printTextWithFont(Globals.myNumberFormat2Price(Double.parseDouble(edt_pd_amt.getText().toString()), decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ")" + "\n", "", 35, callback);
                                    woyouService.setAlignment(0, callback);

                                    Double ab = Double.parseDouble(txt_total_amt.getText().toString()) + Double.parseDouble(edt_pd_amt.getText().toString());
                                    double abs1 = Math.abs(ab);

                                    if (abs1 > 0) {
                                        woyouService.printTextWithFont("Current Balance\n", "", 35, callback);
                                        woyouService.setAlignment(2, callback);
                                        woyouService.printTextWithFont(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") DR" + "\n", "", 35, callback);
                                        woyouService.setAlignment(0, callback);

                                    } else {
                                        woyouService.printTextWithFont("Current Balance\n", "", 35, callback);
                                        woyouService.setAlignment(2, callback);
                                        woyouService.printTextWithFont(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") CR" + "\n", "", 35, callback);
                                    }
                                    woyouService.setAlignment(0, callback);
                                    User user = User.getUser(getApplicationContext(), " Where user_code='" + Globals.user + "'", database);
                                    woyouService.printTextWithFont("\n", "", 24, callback);
                                    woyouService.printTextWithFont("\n", "", 24, callback);
                                    woyouService.printTextWithFont("\n", "", 24, callback);
                                    woyouService.printTextWithFont("Signature :" + user.get_name() + "\n", "", 35, callback);
                                    woyouService.printTextWithFont("\n", "", 24, callback);
                                    woyouService.printTextWithFont("\n", "", 24, callback);
                                    woyouService.printTextWithFont("\n", "", 24, callback);
                                    woyouService.cutPaper(callback);
                                }
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }

        } else {
            if (PrinterType.equals("7")) {
                try {
                    for (int k = 0; k < Integer.parseInt(settings.get_No_Of_Print()); k++) {
                        JSONArray printTest = new JSONArray();
                        timeTools = new TimerCountTools();
                        timeTools.start();
                        ServiceManager.getInstence().getPrinter().setPrintFont(FontsType.simsun);
                        String Print_type = "0";

                        printTest.put(getPrintObject(Globals.objLPR.getCompany_Name(), "3", "center"));
                        printTest.put(getPrintObject(Globals.objLPR.getAddress(), "3", "center"));
                        printTest.put(getPrintObject(Globals.objLPR.getMobile_No(), "3", "center"));
                        try {
                            if (Globals.objLPR.getService_code_tariff().equals("null") || Globals.objLPR.getService_code_tariff().equals("")) {

                            } else {
                                printTest.put(getPrintObject(Globals.objLPR.getService_code_tariff(), "3", "center"));
                            }
                        } catch (Exception ex) {
                        }

                        if (Globals.objLPR.getLicense_No().equals("null") || Globals.objLPR.getLicense_No().equals("")) {
                        } else {

                            printTest.put(getPrintObject(Globals.GSTNo + ":" + Globals.objLPR.getLicense_No(), "3", "center"));
                        }
                        printTest.put(getPrintObject("Payment Receipt", "3", "center"));
                        printTest.put(getPrintObject("Company Name" + ":" + contact.get_company_name(), "2", "left"));
                        printTest.put(getPrintObject("Customer Code" + ":" + txt_cus_code.getText().toString(), "2", "left"));
                        printTest.put(getPrintObject("Customer Name" + ":" + txt_cus_name.getText().toString(), "2", "left"));
                        printTest.put(getPrintObject("Contact No" + ":" + contact.get_contact_1(), "2", "left"));
                        printTest.put(getPrintObject("Date" + ":" + date.substring(0, 10), "2", "left"));
                        printTest.put(getPrintObject("Old Balance", "2", "left"));
                        printTest.put(getPrintObject(Globals.myNumberFormat2Price(Double.parseDouble(txt_total_amt.getText().toString()), decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ")", "2", "right"));
                        printTest.put(getPrintObject("Received", "2", "left"));
                        printTest.put(getPrintObject(Globals.myNumberFormat2Price(Double.parseDouble(edt_pd_amt.getText().toString()), decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ")", "2", "right"));

                        Double ab;
                        if (txt_type_total.getText().toString().equals("DR")) {
                            ab = Double.parseDouble(txt_total_amt.getText().toString()) + Double.parseDouble(edt_pd_amt.getText().toString());

                            double abs1 = Math.abs(ab);

                            if (ab > 0) {
                                printTest.put(getPrintObject("Current Balance", "2", "left"));
                                printTest.put(getPrintObject(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") DR", "2", "right"));

                            }
                        } else {
                            ab = Double.parseDouble(txt_total_amt.getText().toString()) - Double.parseDouble(edt_pd_amt.getText().toString());
                            double abs1 = Math.abs(ab);

                            if (ab > 0) {
                                printTest.put(getPrintObject("Current Balance", "2", "left"));
                                printTest.put(getPrintObject(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") CR", "2", "right"));

                            } else {
                                printTest.put(getPrintObject("Current Balance", "2", "left"));
                                printTest.put(getPrintObject(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") DR", "2", "right"));
                            }
                        }


                        User user = User.getUser(getApplicationContext(), " Where user_code='" + Globals.user + "'", database);
                        printJson.put("spos", printTest);
                        // 设置底部空3行
                        printJson.put("spos", printTest);
                        ServiceManager.getInstence().getPrinter().printBottomFeedLine(4);
                        printTest.put(getPrintObject("Signature :" + user.get_name(), "2", "left"));
                        printJson.put("spos", printTest);
                        // 设置底部空3行
                        printJson.put("spos", printTest);
                        ServiceManager.getInstence().getPrinter().print(printJson.toString(), null, printer_callback);
                        ServiceManager.getInstence().getPrinter().printBottomFeedLine(5);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (PrinterType.equals("3")) {
                byte[] ab1;
                ab1 = BytesUtil.setAlignCenter(1);
                mService.write(ab1);
                mService.sendMessage("" + Globals.objLPR.getCompany_Name() + "\n", "GBK");
                mService.sendMessage("" + Globals.objLPR.getAddress() + "\n", "GBK");
                mService.sendMessage("" + Globals.objLPR.getMobile_No() + "\n", "GBK");
                try {
                    if (Globals.objLPR.getService_code_tariff().equals("null") || Globals.objLPR.getService_code_tariff().equals("")) {
                    } else {
                        mService.sendMessage("" + Globals.objLPR.getService_code_tariff() + "\n", "GBK");
                    }
                } catch (Exception ex) {
                }
                if (Globals.objLPR.getLicense_No().equals("null") || Globals.objLPR.getLicense_No().equals("")) {
                } else {
                    mService.sendMessage(Globals.GSTNo + ":" + Globals.objLPR.getLicense_No(), "GBK");
                }
                ab1 = BytesUtil.setAlignCenter(1);
                mService.write(ab1);
                mService.sendMessage("Payment Receipt\n", "GBK");
                ab1 = BytesUtil.setAlignCenter(0);
                mService.write(ab1);
                mService.sendMessage("Company Name" + ":" + contact.get_company_name(), "GBK");
                mService.sendMessage("Customer Code" + ":" + txt_cus_code.getText().toString(), "GBK");
                mService.sendMessage("Customer Name" + ":" + txt_cus_name.getText().toString(), "GBK");
                mService.sendMessage("Contact No" + ":" + contact.get_contact_1(), "GBK");
                mService.sendMessage("Date" + ":" + date.substring(0, 10), "GBK");
                mService.sendMessage("Old Balance\n", "GBK");
                ab1 = BytesUtil.setAlignCenter(2);
                mService.write(ab1);
                mService.sendMessage(Globals.myNumberFormat2Price(Double.parseDouble(txt_total_amt.getText().toString()), decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ")" + "\n", "GBK");
                ab1 = BytesUtil.setAlignCenter(0);
                mService.write(ab1);
                mService.sendMessage("Received\n", "GBK");
                ab1 = BytesUtil.setAlignCenter(2);
                mService.write(ab1);
                mService.sendMessage(Globals.myNumberFormat2Price(Double.parseDouble(edt_pd_amt.getText().toString()), decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ")" + "\n", "GBK");
                ab1 = BytesUtil.setAlignCenter(0);
                mService.write(ab1);
                Double ab;
                if (txt_type_total.getText().toString().equals("DR")) {
                    ab = Double.parseDouble(txt_total_amt.getText().toString()) + Double.parseDouble(edt_pd_amt.getText().toString());
                    double abs1 = Math.abs(ab);
                    if (ab > 0) {
                        mService.sendMessage("Current Balance\n", "GBK");
                        ab1 = BytesUtil.setAlignCenter(2);
                        mService.write(ab1);
                        mService.sendMessage(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") DR" + "\n", "GBK");
                        ab1 = BytesUtil.setAlignCenter(0);
                        mService.write(ab1);
                    }
                } else {
                    ab = Double.parseDouble(txt_total_amt.getText().toString()) - Double.parseDouble(edt_pd_amt.getText().toString());
                    double abs1 = Math.abs(ab);
                    if (ab > 0) {
                        mService.sendMessage("Current Balance\n", "GBK");
                        ab1 = BytesUtil.setAlignCenter(2);
                        mService.write(ab1);
                        mService.sendMessage(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") CR" + "\n", "GBK");
                        ab1 = BytesUtil.setAlignCenter(0);
                        mService.write(ab1);
                    } else {
                        mService.sendMessage("Current Balance\n", "GBK");
                        ab1 = BytesUtil.setAlignCenter(2);
                        mService.write(ab1);
                        mService.sendMessage(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") DR" + "\n", "GBK");
                    }
                }
                ab1 = BytesUtil.setAlignCenter(0);
                mService.write(ab1);
                User user = User.getUser(getApplicationContext(), " Where user_code='" + Globals.user + "'", database);
                mService.sendMessage("\n", "GBK");
                mService.sendMessage("\n", "GBK");
                mService.sendMessage("\n", "GBK");
                mService.sendMessage("Signature :" + user.get_name() + "\n", "GBK");
                mService.sendMessage("\n", "GBK");
                mService.sendMessage("\n", "GBK");
                mService.sendMessage("\n", "GBK");
            } else if (PrinterType.equals("4")) {
                byte[] ab1;
                ab1 = BytesUtil.setAlignCenter(1);
                mService.write(ab1);
                mService.sendMessage("" + Globals.objLPR.getCompany_Name() + "\n", "GBK");
                mService.sendMessage("" + Globals.objLPR.getAddress() + "\n", "GBK");
                mService.sendMessage("" + Globals.objLPR.getMobile_No() + "\n", "GBK");
                try {
                    if (Globals.objLPR.getService_code_tariff().equals("null") || Globals.objLPR.getService_code_tariff().equals("")) {
                    } else {
                        mService.sendMessage("" + Globals.objLPR.getService_code_tariff() + "\n", "GBK");
                    }
                } catch (Exception ex) {
                }
                if (Globals.objLPR.getLicense_No().equals("null") || Globals.objLPR.getLicense_No().equals("")) {
                } else {
                    mService.sendMessage(Globals.GSTNo + ":" + Globals.objLPR.getLicense_No(), "GBK");
                }
                ab1 = BytesUtil.setAlignCenter(1);
                mService.write(ab1);
                mService.sendMessage("Payment Receipt\n", "GBK");
                ab1 = BytesUtil.setAlignCenter(0);
                mService.write(ab1);
                mService.sendMessage("Company Name" + ":" + contact.get_company_name(), "GBK");
                mService.sendMessage("Customer Code" + ":" + txt_cus_code.getText().toString(), "GBK");
                mService.sendMessage("Customer Name" + ":" + txt_cus_name.getText().toString(), "GBK");
                mService.sendMessage("Contact No" + ":" + contact.get_contact_1(), "GBK");
                mService.sendMessage("Date" + ":" + date.substring(0, 10), "GBK");
                mService.sendMessage("Old Balance\n", "GBK");
                ab1 = BytesUtil.setAlignCenter(2);
                mService.write(ab1);
                mService.sendMessage(Globals.myNumberFormat2Price(Double.parseDouble(txt_total_amt.getText().toString()), decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ")" + "\n", "GBK");
                ab1 = BytesUtil.setAlignCenter(0);
                mService.write(ab1);
                mService.sendMessage("Received\n", "GBK");
                ab1 = BytesUtil.setAlignCenter(2);
                mService.write(ab1);
                mService.sendMessage(Globals.myNumberFormat2Price(Double.parseDouble(edt_pd_amt.getText().toString()), decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ")" + "\n", "GBK");
                ab1 = BytesUtil.setAlignCenter(0);
                mService.write(ab1);
                Double ab;
                if (txt_type_total.getText().toString().equals("DR")) {
                    ab = Double.parseDouble(txt_total_amt.getText().toString()) + Double.parseDouble(edt_pd_amt.getText().toString());
                    double abs1 = Math.abs(ab);
                    if (ab > 0) {
                        mService.sendMessage("Current Balance\n", "GBK");
                        ab1 = BytesUtil.setAlignCenter(2);
                        mService.write(ab1);
                        mService.sendMessage(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") DR" + "\n", "GBK");
                        ab1 = BytesUtil.setAlignCenter(0);
                        mService.write(ab1);
                    }
                } else {
                    ab = Double.parseDouble(txt_total_amt.getText().toString()) - Double.parseDouble(edt_pd_amt.getText().toString());
                    double abs1 = Math.abs(ab);
                    if (ab > 0) {
                        mService.sendMessage("Current Balance\n", "GBK");
                        ab1 = BytesUtil.setAlignCenter(2);
                        mService.write(ab1);
                        mService.sendMessage(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") CR" + "\n", "GBK");
                        ab1 = BytesUtil.setAlignCenter(0);
                        mService.write(ab1);
                    } else {
                        mService.sendMessage("Current Balance\n", "GBK");
                        ab1 = BytesUtil.setAlignCenter(2);
                        mService.write(ab1);
                        mService.sendMessage(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") DR" + "\n", "GBK");
                    }
                }
                ab1 = BytesUtil.setAlignCenter(0);
                mService.write(ab1);
                User user = User.getUser(getApplicationContext(), " Where user_code='" + Globals.user + "'", database);
                mService.sendMessage("\n", "GBK");
                mService.sendMessage("\n", "GBK");
                mService.sendMessage("\n", "GBK");
                mService.sendMessage("Signature :" + user.get_name() + "\n", "GBK");
                mService.sendMessage("\n", "GBK");
                mService.sendMessage("\n", "GBK");
                mService.sendMessage("\n", "GBK");
            } else if (PrinterType.equals("8")) {
                ThreadPoolManager.getInstance().executeTask(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            for (int k = 0; k < Integer.parseInt(settings.get_No_Of_Print()); k++) {
                                String Print_type = "0";
                                mIPosPrinterService.setPrinterPrintFontSize(32, callbackPPT8555);
                                mIPosPrinterService.setPrinterPrintAlignment(1, callbackPPT8555);
                                mIPosPrinterService.printSpecifiedTypeText("" + Globals.objLPR.getCompany_Name() + "\n", "", 32, callbackPPT8555);
                                mIPosPrinterService.printSpecifiedTypeText("" + Globals.objLPR.getAddress() + "\n", "", 32, callbackPPT8555);
                                mIPosPrinterService.printSpecifiedTypeText("" + Globals.objLPR.getMobile_No() + "\n", "", 32, callbackPPT8555);
                                try {
                                    if (Globals.objLPR.getService_code_tariff().equals("null") || Globals.objLPR.getService_code_tariff().equals("")) {

                                    } else {
                                        mIPosPrinterService.printSpecifiedTypeText("" + Globals.objLPR.getService_code_tariff() + "\n", "", 32, callbackPPT8555);
                                    }
                                } catch (Exception ex) {
                                }

                                if (Globals.objLPR.getLicense_No().equals("null") || Globals.objLPR.getLicense_No().equals("")) {
                                } else {
                                    mIPosPrinterService.printColumnsText(new String[]{Globals.GSTNo, ":", Globals.objLPR.getLicense_No()}, new int[]{6, 1, 14}, new int[]{0, 0, 0}, 0, callbackPPT8555);
                                }

                                mIPosPrinterService.setPrinterPrintAlignment(1, callbackPPT8555);
                                mIPosPrinterService.printSpecifiedTypeText("Payment Receipt\n", "", 48, callbackPPT8555);
                                mIPosPrinterService.setPrinterPrintAlignment(0, callbackPPT8555);
                                mIPosPrinterService.printColumnsText(new String[]{"Company Name", ":", contact.get_company_name()}, new int[]{13, 1, 14}, new int[]{0, 0, 0}, 0, callbackPPT8555);
                                mIPosPrinterService.printColumnsText(new String[]{"Customer Code", ":", txt_cus_code.getText().toString()}, new int[]{13, 1, 14}, new int[]{0, 0, 0}, 0, callbackPPT8555);
                                mIPosPrinterService.printColumnsText(new String[]{"Customer Name", ":", txt_cus_name.getText().toString()}, new int[]{13, 1, 14}, new int[]{0, 0, 0}, 0, callbackPPT8555);
                                mIPosPrinterService.printColumnsText(new String[]{"Contact No", ":", contact.get_contact_1()}, new int[]{11, 1, 14}, new int[]{0, 0, 0}, 0, callbackPPT8555);
                                mIPosPrinterService.printColumnsText(new String[]{"Date", ":", date.substring(0, 10)}, new int[]{5, 1, 14}, new int[]{0, 0, 0}, 0, callbackPPT8555);
                                mIPosPrinterService.printSpecifiedTypeText("Old Balance\n", "", 35, callbackPPT8555);
                                mIPosPrinterService.setPrinterPrintAlignment(2, callbackPPT8555);
                                mIPosPrinterService.printSpecifiedTypeText(Globals.myNumberFormat2Price(Double.parseDouble(txt_total_amt.getText().toString()), decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ")" + "\n", "", 32, callbackPPT8555);
                                mIPosPrinterService.setPrinterPrintAlignment(0, callbackPPT8555);
                                mIPosPrinterService.printSpecifiedTypeText("Received\n", "", 32, callbackPPT8555);
                                mIPosPrinterService.setPrinterPrintAlignment(2, callbackPPT8555);
                                mIPosPrinterService.printSpecifiedTypeText(Globals.myNumberFormat2Price(Double.parseDouble(edt_pd_amt.getText().toString()), decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ")" + "\n", "", 32, callbackPPT8555);
                                mIPosPrinterService.setPrinterPrintAlignment(0, callbackPPT8555);

                                Double ab;
                                if (txt_type_total.getText().toString().equals("DR")) {
                                    ab = Double.parseDouble(txt_total_amt.getText().toString()) + Double.parseDouble(edt_pd_amt.getText().toString());
                                    double abs1 = Math.abs(ab);
                                    if (ab > 0) {
                                        mIPosPrinterService.printSpecifiedTypeText("Current Balance\n", "", 32, callbackPPT8555);
                                        mIPosPrinterService.setPrinterPrintAlignment(2, callbackPPT8555);
                                        mIPosPrinterService.printSpecifiedTypeText(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") DR" + "\n", "", 32, callbackPPT8555);
                                        mIPosPrinterService.setPrinterPrintAlignment(0, callbackPPT8555);
                                    }
                                } else {
                                    ab = Double.parseDouble(txt_total_amt.getText().toString()) - Double.parseDouble(edt_pd_amt.getText().toString());
                                    double abs1 = Math.abs(ab);
                                    if (ab > 0) {
                                        mIPosPrinterService.printSpecifiedTypeText("Current Balance\n", "", 32, callbackPPT8555);
                                        mIPosPrinterService.setPrinterPrintAlignment(2, callbackPPT8555);
                                        mIPosPrinterService.printSpecifiedTypeText(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") CR" + "\n", "", 32, callbackPPT8555);
                                        mIPosPrinterService.setPrinterPrintAlignment(0, callbackPPT8555);
                                    } else {
                                        mIPosPrinterService.printSpecifiedTypeText("Current Balance\n", "", 32, callbackPPT8555);
                                        mIPosPrinterService.setPrinterPrintAlignment(2, callbackPPT8555);
                                        mIPosPrinterService.printSpecifiedTypeText(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") DR" + "\n", "", 32, callbackPPT8555);
                                    }
                                }
                                mIPosPrinterService.setPrinterPrintAlignment(0, callbackPPT8555);
                                User user = User.getUser(getApplicationContext(), " Where user_code='" + Globals.user + "'", database);
                                mIPosPrinterService.printBlankLines(1, 8, callbackPPT8555);
                                mIPosPrinterService.printBlankLines(1, 8, callbackPPT8555);
                                mIPosPrinterService.printBlankLines(1, 8, callbackPPT8555);
                                mIPosPrinterService.printSpecifiedTypeText("Signature :" + user.get_name() + "\n", "", 32, callbackPPT8555);
                                mIPosPrinterService.printBlankLines(1, 8, callbackPPT8555);
                                mIPosPrinterService.printBlankLines(1, 8, callbackPPT8555);
                                mIPosPrinterService.printBlankLines(1, 8, callbackPPT8555);
                                mIPosPrinterService.printerPerformPrint(160,  callbackPPT8555);
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                if (woyouService == null) {
                } else {
                    ThreadPoolManager.getInstance().executeTask(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                for (int k = 0; k < Integer.parseInt(settings.get_No_Of_Print()); k++) {
                                    String Print_type = "0";
                                    woyouService.setFontSize(35, callback);
                                    woyouService.setAlignment(1, callback);
                                    woyouService.printTextWithFont("" + Globals.objLPR.getCompany_Name() + "\n", "", 35, callback);
                                    woyouService.printTextWithFont("" + Globals.objLPR.getAddress() + "\n", "", 35, callback);
                                    woyouService.printTextWithFont("" + Globals.objLPR.getMobile_No() + "\n", "", 35, callback);
                                    try {
                                        if (Globals.objLPR.getService_code_tariff().equals("null") || Globals.objLPR.getService_code_tariff().equals("")) {

                                        } else {
                                            woyouService.printTextWithFont("" + Globals.objLPR.getService_code_tariff() + "\n", "", 35, callback);
                                        }
                                    } catch (Exception ex) {
                                    }

                                    if (Globals.objLPR.getLicense_No().equals("null") || Globals.objLPR.getLicense_No().equals("")) {
                                    } else {
                                        woyouService.printColumnsText(new String[]{Globals.GSTNo, ":", Globals.objLPR.getLicense_No()}, new int[]{6, 1, 20}, new int[]{0, 0, 0}, callback);
                                    }

                                    woyouService.setAlignment(1, callback);
                                    woyouService.printTextWithFont("Payment Receipt\n", "", 40, callback);
                                    woyouService.setAlignment(0, callback);
                                    woyouService.printColumnsText(new String[]{"Company Name", ":", contact.get_company_name()}, new int[]{13, 1, 20}, new int[]{0, 0, 0}, callback);
                                    woyouService.printColumnsText(new String[]{"Customer Code", ":", txt_cus_code.getText().toString()}, new int[]{13, 1, 20}, new int[]{0, 0, 0}, callback);
                                    woyouService.printColumnsText(new String[]{"Customer Name", ":", txt_cus_name.getText().toString()}, new int[]{13, 1, 20}, new int[]{0, 0, 0}, callback);
                                    woyouService.printColumnsText(new String[]{"Contact No", ":", contact.get_contact_1()}, new int[]{11, 1, 20}, new int[]{0, 0, 0}, callback);
                                    woyouService.printColumnsText(new String[]{"Date", ":", date.substring(0, 10)}, new int[]{5, 1, 20}, new int[]{0, 0, 0}, callback);
                                    woyouService.printTextWithFont("Old Balance\n", "", 35, callback);
                                    woyouService.setAlignment(2, callback);
                                    woyouService.printTextWithFont(Globals.myNumberFormat2Price(Double.parseDouble(txt_total_amt.getText().toString()), decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ")" + "\n", "", 35, callback);
                                    woyouService.setAlignment(0, callback);
                                    woyouService.printTextWithFont("Received\n", "", 35, callback);
                                    woyouService.setAlignment(2, callback);
                                    woyouService.printTextWithFont(Globals.myNumberFormat2Price(Double.parseDouble(edt_pd_amt.getText().toString()), decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ")" + "\n", "", 35, callback);
                                    woyouService.setAlignment(0, callback);
                                    Double ab;
                                    if (txt_type_total.getText().toString().equals("DR")) {
                                        ab = Double.parseDouble(txt_total_amt.getText().toString()) + Double.parseDouble(edt_pd_amt.getText().toString());
                                        double abs1 = Math.abs(ab);
                                        if (ab > 0) {
                                            woyouService.printTextWithFont("Current Balance\n", "", 35, callback);
                                            woyouService.setAlignment(2, callback);
                                            woyouService.printTextWithFont(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") DR" + "\n", "", 35, callback);
                                            woyouService.setAlignment(0, callback);
                                        }
                                    } else {
                                        ab = Double.parseDouble(txt_total_amt.getText().toString()) - Double.parseDouble(edt_pd_amt.getText().toString());
                                        double abs1 = Math.abs(ab);
                                        if (ab > 0) {
                                            woyouService.printTextWithFont("Current Balance\n", "", 35, callback);
                                            woyouService.setAlignment(2, callback);
                                            woyouService.printTextWithFont(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") CR" + "\n", "", 35, callback);
                                            woyouService.setAlignment(0, callback);
                                        } else {
                                            woyouService.printTextWithFont("Current Balance\n", "", 35, callback);
                                            woyouService.setAlignment(2, callback);
                                            woyouService.printTextWithFont(Globals.myNumberFormat2Price(abs1, decimal_check) + "(" + Globals.objLPD.getCurreny_Symbol() + ") DR" + "\n", "", 35, callback);
                                        }
                                    }
                                    woyouService.setAlignment(0, callback);
                                    User user = User.getUser(getApplicationContext(), " Where user_code='" + Globals.user + "'", database);
                                    woyouService.printTextWithFont("\n", "", 24, callback);
                                    woyouService.printTextWithFont("\n", "", 24, callback);
                                    woyouService.printTextWithFont("\n", "", 24, callback);
                                    woyouService.printTextWithFont("Signature :" + user.get_name() + "\n", "", 35, callback);
                                    woyouService.printTextWithFont("\n", "", 24, callback);
                                    woyouService.printTextWithFont("\n", "", 24, callback);
                                    woyouService.printTextWithFont("\n", "", 24, callback);
                                    woyouService.cutPaper(callback);
                                }
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        }
    }

    private void GetCustomerCreditDetail() {
        try {
            String serverData = GetCusCrDetailFromServer();
            if (serverData.equals("") || serverData.equals("null")) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "No credit found", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                try {

                    final JSONObject jsonObject_manufacture = new JSONObject(serverData);
                    final String strStatus = jsonObject_manufacture.getString("status");
                    if (strStatus.equals("true")) {
                        JSONArray jsonArray = jsonObject_manufacture.getJSONArray("result");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            strCRAmount = jsonObject.getString("amount");
                            try {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        double abs = 0d;
                                        Double strCheckAmmt;
                                        try {
                                            strCheckAmmt = Double.parseDouble(strCRAmount);
//                                        abs1 = Math.abs(Double.parseDouble(strCRAmount));
                                        } catch (Exception ex) {
                                            strCheckAmmt = 0d;
//                                        abs1 = Math.abs(0);
                                        }
                                        abs = Math.abs(strCheckAmmt);
                                        if (strCheckAmmt > 0) {
                                            txt_type.setText("CR");
                                            txt_type.setTextColor(Color.parseColor("#FF0000"));

                                        } else {
                                            txt_type.setText("DR");
                                            txt_type.setTextColor(Color.parseColor("#228B22"));

                                        }
                                        txt_cr_amt.setText(Globals.myNumberFormat2Price(abs, decimal_check));

                                        String curAmount = "";
                                        try {
                                            String strTableQry = "Select sum(pay_amount) from order_payment left join orders on orders.order_code = order_payment.order_code where orders.order_code In(select orders.order_code from orders where orders.contact_code = '" + code + "' and orders.z_code = '0' and order_payment.payment_id='5')";
                                            Cursor cursor = database.rawQuery(strTableQry, null);
                                            if (cursor.moveToFirst()) {
                                                do {
                                                    curAmount = Globals.myNumberFormat2Price(Double.parseDouble(cursor.getString(0)), decimal_check);

                                                } while (cursor.moveToNext());
                                            }
                                        } catch (Exception ex) {
                                            curAmount = "0";
                                        }
                                        String strCur = Globals.myNumberFormat2Price(Double.parseDouble(curAmount), decimal_check);
                                        txt_cr_lc_amt.setText(strCur);
                                        try {

                                            if (txt_type.getText().toString().equals("DR")) {
                                                try {
                                                    Double strCrAmt = Double.parseDouble(strCRAmount);
                                                    double abs2 = Math.abs(strCrAmt);
                                                    double ab = abs2 - Double.parseDouble(curAmount);
                                                    double abs1 = Math.abs(ab);
                                                    if (ab > 0) {
                                                        txt_total_amt.setText(Globals.myNumberFormat2Price(abs1, decimal_check));
                                                        txt_type_total.setText("DR");
                                                        txt_type_total.setTextColor(Color.parseColor("#228B22"));
                                                        edt_pd_amt.setText(Globals.myNumberFormat2Price(abs1, decimal_check));
                                                    } else {
                                                        txt_total_amt.setText(Globals.myNumberFormat2Price(abs1, decimal_check));
                                                        txt_type_total.setText("CR");
                                                        txt_type_total.setTextColor(Color.parseColor("#FF0000"));
                                                        edt_pd_amt.setText(Globals.myNumberFormat2Price(abs1, decimal_check));
                                                    }
                                                } catch (Exception ex) {
                                                    txt_total_amt.setText(Globals.myNumberFormat2Price(0, decimal_check));
                                                }
                                            } else {
                                                try {
                                                    Double strCrAmt = Double.parseDouble(strCRAmount);
                                                    double abs1 = Math.abs(strCrAmt + Double.parseDouble(curAmount));
                                                    if (abs1 > 0) {
                                                        txt_total_amt.setText(Globals.myNumberFormat2Price(abs1, decimal_check));
                                                        txt_type_total.setText("CR");
                                                        txt_type_total.setTextColor(Color.parseColor("#FF0000"));
                                                        edt_pd_amt.setText(Globals.myNumberFormat2Price(abs1, decimal_check));
                                                    } else {
                                                        txt_total_amt.setText(Globals.myNumberFormat2Price(abs1, decimal_check));
                                                        txt_type_total.setText("CR");
                                                        txt_type_total.setTextColor(Color.parseColor("#FF0000"));
                                                        edt_pd_amt.setText(Globals.myNumberFormat2Price(abs1, decimal_check));
                                                    }
                                                } catch (Exception ex) {
                                                    txt_total_amt.setText(Globals.myNumberFormat2Price(0, decimal_check));
                                                }


                                                edt_pd_amt.requestFocus();
                                                edt_pd_amt.selectAll();
                                            }

                                        } catch (Exception ex) {
                                        }

                                    }


                                });

                            } catch (Exception ex) {
                            }
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), "No credit found", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                } catch (JSONException e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "No credit found", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        } catch (Exception ex) {
        }
    }

    private String SendCustomerCreditDetail() {
        String succ = "0";
        database.beginTransaction();
        String serverData = SendCusCrDetailFromServer();
        try {
            final JSONObject jsonObject_manufacture = new JSONObject(serverData);
            final String strStatus = jsonObject_manufacture.getString("status");

            if (strStatus.equals("true")) {
                Double balance = Double.parseDouble(txt_cr_amt.getText().toString()) - Double.parseDouble(edt_pd_amt.getText().toString().trim());
                String strBalance = Globals.myNumberFormat2Price(balance, decimal_check);
                Acc_Customer_Credit acc_customer_credit = new Acc_Customer_Credit(getApplicationContext(), null, date, txt_cus_code.getText().toString(), txt_cr_amt.getText().toString(), edt_pd_amt.getText().toString().trim(), strBalance, "0", "1", modified_by, date);
                long l = acc_customer_credit.insertAcc_Customer_Credit(database);
                if (l > 0) {
                    succ = "1";
                }
            } else {
                database.endTransaction();
            }

            if (succ.equals("1")) {
                database.setTransactionSuccessful();
                database.endTransaction();
            }
        } catch (JSONException e) {
            database.endTransaction();
        }
        return succ;
    }

    private String GetCusCrDetailFromServer() {
        String serverData = null;//
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(
                "http://" + Globals.App_IP + "/lite-pos/index.php/api/accounts");
        ArrayList nameValuePairs = new ArrayList(5);
        nameValuePairs.add(new BasicNameValuePair("company_id", Globals.Company_Id));
        nameValuePairs.add(new BasicNameValuePair("device_code", Globals.objLPD.getDevice_Code()));
        nameValuePairs.add(new BasicNameValuePair("contact_code", code));
        nameValuePairs.add(new BasicNameValuePair("type", "S"));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            serverData = EntityUtils.toString(httpEntity);
            Log.d("response", serverData);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serverData;
    }

    private String SendCusCrDetailFromServer() {
        String serverData = null;//
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(
                "http://" + Globals.App_IP + "/lite-pos/index.php/api/accounts/data");
        ArrayList nameValuePairs = new ArrayList(5);
        nameValuePairs.add(new BasicNameValuePair("company_id", Globals.Company_Id));
        nameValuePairs.add(new BasicNameValuePair("contact_code", code));
        nameValuePairs.add(new BasicNameValuePair("device_code", Globals.objLPD.getDevice_Code()));
        nameValuePairs.add(new BasicNameValuePair("type", "S"));
        nameValuePairs.add(new BasicNameValuePair("amount", edt_pd_amt.getText().toString().trim()));
//        nameValuePairs.add(new BasicNameValuePair("modified_by", modified_by));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            serverData = EntityUtils.toString(httpEntity);
            Log.d("response", serverData);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serverData;

    }

    private boolean isNetworkStatusAvialable(Context applicationContext) {
        // TODO Auto-generated method stub
        ConnectivityManager connectivityManager = (ConnectivityManager) getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo netInfos = connectivityManager.getActiveNetworkInfo();
            if (netInfos != null)
                if (netInfos.isConnected())

                    return true;
        }
        return false;
    }


    @Override
    public void onBackPressed() {
        pDialog = new ProgressDialog(AccountsActivity.this);
        pDialog.setCancelable(false);
        pDialog.setMessage(getString(R.string.Wait_msg));
        pDialog.show();

        Thread timerThread = new Thread() {
            public void run() {
                try {
                    Intent intent = new Intent(AccountsActivity.this, AccountsListActivity.class);
                    startActivity(intent);
                    pDialog.dismiss();
                    finish();
                } finally {
                }
            }
        };
        timerThread.start();
    }

    private JSONObject getPrintObject(String test) {
        JSONObject json = new JSONObject();
        try {
            json.put("content-type", "txt");
            json.put("content", test);
            json.put("size", "1");
            json.put("position", "left");
            json.put("offset", "0");
            json.put("bold", "0");
            json.put("italic", "0");
            json.put("height", "-1");
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return json;
    }

    private JSONObject getPrintObject(String test, String size, String aline) {
        JSONObject json = new JSONObject();
        try {
            json.put("content-type", "txt");
            json.put("content", test);
            json.put("size", size);
            json.put("position", aline);
            json.put("offset", "0");
            json.put("bold", "0");
            json.put("italic", "0");
            json.put("height", "-1");
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return json;
    }


    class PrinterListener implements OnPrinterListener {
        private final String TAG = "Print";

        @Override
        public void onStart() {
            // TODO 打印开始
            // Print start
//            LOGD("start print");
        }

        @Override
        public void onFinish() {
            // TODO 打印结束
            // End of the print
//            LOGD("pint success");
            timeTools.stop();
//            LOGD("time cost：" + timeTools.getProcessTime());
        }

        @Override
        public void onError(int errorCode, String detail) {
            // TODO 打印出错
            // print error
//            LOGD("print error" + " errorcode = " + errorCode + " detail = " + detail);
            if (errorCode == PrinterBinder.PRINTER_ERROR_NO_PAPER) {
                Toast.makeText(AccountsActivity.this, "paper runs out during printing", Toast.LENGTH_SHORT).show();
            }
            if (errorCode == PrinterBinder.PRINTER_ERROR_OVER_HEAT) {
                Toast.makeText(AccountsActivity.this, "over heat during printing", Toast.LENGTH_SHORT).show();
            }
            if (errorCode == PrinterBinder.PRINTER_ERROR_OTHER) {
                Toast.makeText(AccountsActivity.this, "other error happen during printing", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void printerInit() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printerInit(callbackPPT8555);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public int getPrinterStatus() {

        Log.i(TAG, "***** printerStatus" + printerStatus);
        try {
            printerStatus = mIPosPrinterService.getPrinterStatus();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "#### printerStatus" + printerStatus);
        return printerStatus;
    }
}
