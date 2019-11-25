package org.phomellolitepos;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.hoin.btsdk.BluetoothService;
import com.itextpdf.text.pdf.codec.Base64;

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
import org.phomellolitepos.Adapter.CusReturnFinalListAdapter;
import org.phomellolitepos.Adapter.InvReturnFinalListAdapter;
import org.phomellolitepos.Adapter.MyAdapter;
import org.phomellolitepos.StockAdjestment.StockAdjectmentDetailList;
import org.phomellolitepos.Util.Globals;
import org.phomellolitepos.database.Acc_Customer;
import org.phomellolitepos.database.Contact;
import org.phomellolitepos.database.Database;
import org.phomellolitepos.database.Item;
import org.phomellolitepos.database.Item_Location;
import org.phomellolitepos.database.Lite_POS_Registration;
import org.phomellolitepos.database.Order_Detail;
import org.phomellolitepos.database.Return_detail;
import org.phomellolitepos.database.Returns;
import org.phomellolitepos.database.Settings;
import org.phomellolitepos.database.User;
import org.phomellolitepos.printer.BytesUtil;
import org.phomellolitepos.printer.PrintLayout;
import org.phomellolitepos.printer.ThreadPoolManager;
import org.phomellolitepos.printer.WifiPrintDriver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import woyou.aidlservice.jiuiv5.ICallback;
import woyou.aidlservice.jiuiv5.IWoyouService;

public class InvReturnFinalActivity extends AppCompatActivity {
    EditText edt_name, edt_qty, edt_price, edt_return_qty, edt_total_qty;
    EditText edt_toolbar_item_list;
    Button btn_add;
    ListView list, lv;
    String operation, str_voucher_no, str_date, str_remarks, ordCode;
    Database db;
    SQLiteDatabase database;
    ProgressDialog pDialog;
    BottomNavigationView bottomNavigationView;
    String[] invFlag = {};
    ArrayList<Item> arrayListItem;
    ArrayList<StockAdjectmentDetailList> arraylist = new ArrayList<StockAdjectmentDetailList>();
    ArrayList<Return_detail> arrayListReturn_detail;
    String sale_priceStr, decimal_check, qty_decimal_check, str_inv;
    Item resultp;
    Item resultp1;
    String item_code, date;
    InvReturnFinalListAdapter returnFinalListAdapter;
    Returns returns;
    Return_detail return_detail;
    String strupdate = "", strItemCode, cusCode;
    int Position;
    Lite_POS_Registration lite_pos_registration;
    ArrayList<String> arrayList1;
    String relt = "", PayId;
    private boolean iswifi = false;
    private ArrayList<String> mylist = new ArrayList<String>();
    private MyAdapter adp;
    private int order, noofPrint = 0, lang = 0, pos = 0;
    ArrayList<Return_detail> return_details;
    private String PrinterType = "";
    private IWoyouService woyouService;
    Settings settings;
    User user;
    BluetoothService mService = null;
    private ICallback callback = null;
    private ProgressDialog dialog;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inv_return_final);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        invFlag = getResources().getStringArray(R.array.Inv_flag);
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", Context.MODE_MULTI_PROCESS); // 0 - for private mode
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        int id = pref.getInt("id", 0);
        if (id == 0) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp_mdpi);
        } else {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_forward_black_24dp);
        }
        mService = MainActivity.mService;
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pDialog = new ProgressDialog(InvReturnFinalActivity.this);
                pDialog.setCancelable(false);
                pDialog.setMessage(getString(R.string.Wait_msg));
                pDialog.show();

                Thread timerThread = new Thread() {
                    public void run() {
                        try {
                            sleep(1000);
                            pDialog.dismiss();
                            Intent intent = new Intent(InvReturnFinalActivity.this, InvReturnHeaderActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.putExtra("operation", operation);
                            intent.putExtra("voucher_no", str_voucher_no);
                            intent.putExtra("date", str_date);
                            intent.putExtra("remarks", str_remarks);
                            intent.putExtra("contact_code", "");
                            intent.putExtra("order_code", ordCode);
                            startActivity(intent);
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

        Intent intent = getIntent();
        getSupportActionBar().setTitle("");
        operation = intent.getStringExtra("operation");
        str_voucher_no = intent.getStringExtra("voucher_no");
        str_date = intent.getStringExtra("date");
        str_remarks = intent.getStringExtra("remarks");
        PayId = intent.getStringExtra("payment_id");
        ordCode = intent.getStringExtra("order_code");
        cusCode = intent.getStringExtra("contact_code");

        Date d = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        date = format.format(d);
        db = new Database(getApplicationContext());
        database = db.getWritableDatabase();
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.retail_bottom_navigation);
        edt_toolbar_item_list = (EditText) findViewById(R.id.edt_toolbar_item_list);
        edt_name = (EditText) findViewById(R.id.edt_name);
        edt_qty = (EditText) findViewById(R.id.edt_qty);
        edt_return_qty = (EditText) findViewById(R.id.edt_return_qty);
        edt_total_qty = (EditText) findViewById(R.id.edt_total_qty);
        edt_price = (EditText) findViewById(R.id.edt_price);
        list = (ListView) findViewById(R.id.list);
        btn_add = (Button) findViewById(R.id.btn_add);

        callback = new ICallback.Stub() {

            @Override
            public void onRunResult(final boolean success) throws RemoteException {
            }

            @Override
            public void onReturnString(final String value) throws RemoteException {
            }

            @Override
            public void onRaiseException(int code, final String msg) throws RemoteException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //info.append("onRaiseException = " + msg + "\n");
                    }
                });
            }
        };

        try {
            Intent intent1 = new Intent();
            intent1.setPackage("woyou.aidlservice.jiuiv5");
            intent1.setAction("woyou.aidlservice.jiuiv5.IWoyouService");
            startService(intent1);
            bindService(intent1, connService, Context.BIND_AUTO_CREATE);
        } catch (Exception ex) {
        }


        edt_toolbar_item_list.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (edt_toolbar_item_list.getText().toString().trim().equals("")) {
                    return false;
                } else {
                    edt_toolbar_item_list.requestFocus();
                    edt_toolbar_item_list.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    InputMethodManager imm4 = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm4.showSoftInput(edt_toolbar_item_list, InputMethodManager.SHOW_IMPLICIT);
                    edt_toolbar_item_list.selectAll();
                    return true;
                }
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

        try {
            decimal_check = Globals.objLPD.getDecimal_Place();
            qty_decimal_check = settings.get_Qty_Decimal();
        } catch (Exception ex) {
            decimal_check = "1";
        }

        lite_pos_registration = Lite_POS_Registration.getRegistration(getApplicationContext(), database, db, "");
        returns = Returns.getReturns(getApplicationContext(), " where voucher_no ='" + str_voucher_no + "' ", database);
        if (returns == null) {
        } else {
            if (returns.get_is_post().equals("true") || returns.get_is_cancel().equals("true")) {
                Menu menu = bottomNavigationView.getMenu();
                MenuItem cancel = menu.findItem(R.id.action_cancel);
                MenuItem delete = menu.findItem(R.id.action_delete);
                MenuItem post = menu.findItem(R.id.action_post);
                MenuItem save = menu.findItem(R.id.action_save);
                cancel.setEnabled(false);
                delete.setEnabled(false);
                post.setEnabled(false);
                save.setEnabled(false);
                btn_add.setEnabled(false);
            }
        }

        final LongOperation tsk = new LongOperation();
        tsk.execute();
        // setting timeout thread for async task
        Thread thread1 = new Thread() {
            public void run() {
                try {
                    tsk.get(6000, TimeUnit.MILLISECONDS); // set time in
                } catch (Exception e) {
                    tsk.cancel(true);
                    iswifi = false;
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                }
            }
        };
        thread1.start();

        if (operation.equals("Edit")) {
            arrayListReturn_detail = Return_detail.getAllReturn_detail(getApplicationContext(), " where ref_voucher_no ='" + str_voucher_no + "' ", database);
            if (arrayListReturn_detail.size() > 0) {
                String inv;
                for (int i = 0; i < arrayListReturn_detail.size(); i++) {
                    Item item = Item.getItem(getApplicationContext(), " where item_code = '" + arrayListReturn_detail.get(i).get_item_code() + "'", database, db);

                    StockAdjectmentDetailList stockAdjectmentDetailList = new StockAdjectmentDetailList(getApplicationContext(), "", "", "", arrayListReturn_detail.get(i).get_item_code(), arrayListReturn_detail.get(i).get_qty(), "", item.get_item_name(), arrayListReturn_detail.get(i).get_price(), arrayListReturn_detail.get(i).get_line_total());
                    arraylist.add(stockAdjectmentDetailList);
                }
                list_load(arraylist);
            }
        }

        edt_qty.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (edt_qty.getText().toString().trim().equals("")) {
                    return false;
                } else {
                    edt_qty.requestFocus();
                    edt_qty.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    InputMethodManager imm4 = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm4.showSoftInput(edt_qty, InputMethodManager.SHOW_IMPLICIT);
                    edt_qty.selectAll();
                    return true;
                }
            }
        });

        edt_price.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (edt_price.getText().toString().trim().equals("")) {
                    return false;
                } else {
                    edt_price.requestFocus();
                    edt_price.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    InputMethodManager imm4 = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm4.showSoftInput(edt_price, InputMethodManager.SHOW_IMPLICIT);
                    edt_price.selectAll();
                    return true;
                }
            }
        });

        btn_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edt_name.getText().toString().trim().equals("")) {
                    Toast.makeText(getApplicationContext(), "No item selected", Toast.LENGTH_SHORT).show();
//                    return;
                } else {
                    if (edt_qty.getText().toString().trim().equals("")) {
                        edt_qty.setError("Enter Quantity");
                        edt_qty.requestFocus();
                        return;
                    }

                    if (edt_price.getText().toString().trim().equals("")) {
                        edt_price.setError("Enter Price");
                        edt_price.requestFocus();
                        return;
                    }

                    String strQty = Globals.myNumberFormat2Price(Double.parseDouble(edt_qty.getText().toString()), decimal_check);
                    String strReturnQty = Globals.myNumberFormat2Price(Double.parseDouble(edt_return_qty.getText().toString()), decimal_check);
                    Double updReturnQty = Double.parseDouble(strReturnQty) - Double.parseDouble(strQty);
                    if (Double.parseDouble(strQty) <= Double.parseDouble(strReturnQty)) {
                        edt_return_qty.setText(Globals.myNumberFormat2Price(updReturnQty, decimal_check));
                    } else {
                        Toast.makeText(getApplicationContext(), "Can't return more than return quantity!", Toast.LENGTH_SHORT).show();
                        edt_qty.requestFocus();
                        edt_qty.selectAll();
                        return;
                    }


                    closeKeyboard();
                    if (lite_pos_registration.getproject_id().equals("standalone")) {
                        if (strupdate.equals("update")) {
                            StockAdjectmentDetailList stockAdjectmentDetailList = new StockAdjectmentDetailList(getApplicationContext(), "", "", "", strItemCode, edt_qty.getText().toString().trim(), str_inv, edt_name.getText().toString().trim(), edt_price.getText().toString().trim(), (Double.parseDouble(edt_qty.getText().toString().trim()) * Double.parseDouble(edt_price.getText().toString().trim())) + "");
                            arraylist.remove(Position);
                            arraylist.add(Position, stockAdjectmentDetailList);
                            list_load(arraylist);
                        } else {
                            if (settings.get_Is_Stock_Manager().equals("true")) {
                                Item_Location item_location = Item_Location.getItem_Location(getApplication(), "where item_code='" + item_code + "'", database);
                                if (item_location != null) {
//                                if (Double.parseDouble(item_location.get_quantity()) > 0) {
                                    int count = 0;
                                    boolean bFound = false;

                                    while (count < arraylist.size()) {
                                        if (resultp.get_item_code().equals(arraylist.get(count).getItem_code())) {
                                            bFound = true;
                                            arraylist.get(count).setQty(((Integer.parseInt(arraylist.get(count).getQty())) + Integer.parseInt(edt_qty.getText().toString().trim())) + "");
                                            arraylist.get(count).setLine_total(((Double.parseDouble(arraylist.get(count).getQty())) * Double.parseDouble(edt_price.getText().toString().trim())) + "");
                                        }
                                        count = count + 1;
                                    }

                                    if (!bFound) {
                                        StockAdjectmentDetailList stockAdjectmentDetailList = new StockAdjectmentDetailList(getApplicationContext(), "", "", "", item_code, edt_qty.getText().toString().trim(), str_inv, resultp.get_item_name(), edt_price.getText().toString().trim(), (Double.parseDouble(edt_qty.getText().toString().trim()) * Double.parseDouble(edt_price.getText().toString().trim())) + "");
                                        arraylist.add(stockAdjectmentDetailList);
                                    }
                                    list_load(arraylist);
//                                } else {
//                                    Toast.makeText(getApplicationContext(), "No Stock Found", Toast.LENGTH_SHORT).show();
//                                }

                                } else {
                                    Toast.makeText(getApplicationContext(), "This item not in stock", Toast.LENGTH_SHORT).show();
                                }

                            } else {
                                int count = 0;
                                boolean bFound = false;

                                while (count < arraylist.size()) {
                                    if (resultp.get_item_code().equals(arraylist.get(count).getItem_code())) {
                                        bFound = true;
                                        arraylist.get(count).setQty(((Integer.parseInt(arraylist.get(count).getQty())) + Integer.parseInt(edt_qty.getText().toString().trim())) + "");
                                        arraylist.get(count).setLine_total(((Double.parseDouble(arraylist.get(count).getQty())) * Double.parseDouble(edt_price.getText().toString().trim())) + "");
                                    }
                                    count = count + 1;
                                }

                                if (!bFound) {
                                    StockAdjectmentDetailList stockAdjectmentDetailList = new StockAdjectmentDetailList(getApplicationContext(), "", "", "", item_code, edt_qty.getText().toString().trim(), str_inv, resultp.get_item_name(), edt_price.getText().toString().trim(), (Double.parseDouble(edt_qty.getText().toString().trim()) * Double.parseDouble(edt_price.getText().toString().trim())) + "");
                                    arraylist.add(stockAdjectmentDetailList);

                                }
                                list_load(arraylist);
                            }
                        }
                    } else {
                        if (strupdate.equals("update")) {
                            StockAdjectmentDetailList stockAdjectmentDetailList = new StockAdjectmentDetailList(getApplicationContext(), "", "", "", strItemCode, edt_qty.getText().toString().trim(), str_inv, edt_name.getText().toString().trim(), edt_price.getText().toString().trim(), (Double.parseDouble(edt_qty.getText().toString().trim()) * Double.parseDouble(edt_price.getText().toString().trim())) + "");
                            arraylist.remove(Position);
                            arraylist.add(Position, stockAdjectmentDetailList);
                            list_load(arraylist);
                        } else {

                            int count = 0;
                            boolean bFound = false;

                            while (count < arraylist.size()) {
                                if (resultp1.get_item_code().equals(arraylist.get(count).getItem_code())) {
                                    bFound = true;
                                    arraylist.get(count).setQty(((Integer.parseInt(arraylist.get(count).getQty())) + Integer.parseInt(edt_qty.getText().toString().trim())) + "");
                                    arraylist.get(count).setLine_total(((Double.parseDouble(arraylist.get(count).getQty())) * Double.parseDouble(edt_price.getText().toString().trim())) + "");
                                }
                                count = count + 1;
                            }

                            if (!bFound) {
                                StockAdjectmentDetailList stockAdjectmentDetailList = new StockAdjectmentDetailList(getApplicationContext(), "", "", "", resultp1.get_item_code(), edt_qty.getText().toString().trim(), str_inv, resultp1.get_item_name(), edt_price.getText().toString().trim(), (Double.parseDouble(edt_qty.getText().toString().trim()) * Double.parseDouble(edt_price.getText().toString().trim())) + "");
                                arraylist.add(stockAdjectmentDetailList);
                            }
                            list_load(arraylist);
                        }
                    }

                    edt_qty.requestFocus();
                    edt_qty.selectAll();
                }
            }
        });

        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener()

                {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_post:
                                if (arraylist.size() == 0) {
                                    Toast.makeText(getApplicationContext(), "No item added", Toast.LENGTH_SHORT).show();
                                } else {
                                    if (lite_pos_registration.getproject_id().equals("standalone")) {
                                        String rsultPost = stock_post();
                                        if (rsultPost.equals("1")) {
                                            if (settings.get_Is_Stock_Manager().equals("true")) {
                                                String rsultUpdate = stock_update();
                                            }

                                            try {
                                                print_return();
                                            } catch (Exception ex) {}


                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    Toast.makeText(getApplicationContext(), "Post successful", Toast.LENGTH_SHORT).show();
                                                    Intent intent1 = new Intent(InvReturnFinalActivity.this, InvReturnListActivity.class);
                                                    startActivity(intent1);
                                                    finish();
                                                }
                                            });
                                        } else {
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    Toast.makeText(getApplicationContext(), "Record not post", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    } else {
                                        pDialog = new ProgressDialog(InvReturnFinalActivity.this);
                                        pDialog.setCancelable(false);
                                        pDialog.setMessage(getString(R.string.Wait_msg));
                                        pDialog.show();
                                        Thread timerThread1 = new Thread() {
                                            public void run() {
                                                String result = stock_save();
                                                if (result.equals("1")) {
                                                    if (isNetworkStatusAvialable(getApplicationContext())) {
                                                        String rsultUpdtCloud = stock_updt_cloud();
                                                        if (result.equals("1")) {
                                                            result = send_online_return();
                                                            if (result.equals("1")) {
                                                                String rsultPost = stock_post();
                                                                pDialog.dismiss();
                                                                try {
                                                                    print_return();
                                                                } catch (Exception ex) {}
                                                                switch (rsultUpdtCloud) {
                                                                    case "1":
                                                                        runOnUiThread(new Runnable() {
                                                                            public void run() {
                                                                                Toast.makeText(getApplicationContext(), "Post successful", Toast.LENGTH_SHORT).show();
                                                                                Intent intent1 = new Intent(InvReturnFinalActivity.this, InvReturnListActivity.class);
                                                                                startActivity(intent1);
                                                                                finish();
                                                                            }
                                                                        });
                                                                        break;

                                                                    case "2":
                                                                        runOnUiThread(new Runnable() {
                                                                            public void run() {
                                                                                Toast.makeText(getApplicationContext(), R.string.srvr_error, Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        });
                                                                        break;
                                                                    default:
                                                                        runOnUiThread(new Runnable() {
                                                                            public void run() {
                                                                                Toast.makeText(getApplicationContext(), "Record not post", Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        });
                                                                        break;
                                                                }
                                                            } else {
                                                                pDialog.dismiss();
                                                            }
                                                        } else {
                                                            pDialog.dismiss();
                                                            runOnUiThread(new Runnable() {
                                                                public void run() {
                                                                    Toast.makeText(getApplicationContext(), "Record not post on server", Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                        }
                                                    } else {
                                                        pDialog.dismiss();
                                                        runOnUiThread(new Runnable() {
                                                            public void run() {
                                                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.nointernet), Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                                    }
                                                } else {
                                                    pDialog.dismiss();
                                                    runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            Toast.makeText(getApplicationContext(), "Record not saved", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });

                                                }
                                            }
                                        };
                                        timerThread1.start();
                                    }
                                }
                                break;

                            case R.id.action_save:

                                if (arraylist.size() == 0) {
                                    Toast.makeText(getApplicationContext(), "No item added", Toast.LENGTH_SHORT).show();
                                } else {
                                    pDialog = new ProgressDialog(InvReturnFinalActivity.this);
                                    pDialog.setCancelable(false);
                                    pDialog.setMessage(getString(R.string.Wait_msg));
                                    pDialog.show();
                                    Thread timerThread = new Thread() {
                                        public void run() {
                                            String result = stock_save();
                                            pDialog.dismiss();
                                            if (result.equals("1")) {
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        Toast.makeText(getApplicationContext(), "Saved successful", Toast.LENGTH_SHORT).show();
                                                        Intent intent1 = new Intent(InvReturnFinalActivity.this, InvReturnListActivity.class);
                                                        startActivity(intent1);
                                                        finish();
                                                    }
                                                });

                                            } else {
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        Toast.makeText(getApplicationContext(), "Record not saved", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                        }
                                    };
                                    timerThread.start();
                                }
                                break;
                            case R.id.action_cancel:
                                if (arraylist.size() == 0) {
                                    Toast.makeText(getApplicationContext(), "No item added", Toast.LENGTH_SHORT).show();
                                } else {
                                    pDialog = new ProgressDialog(InvReturnFinalActivity.this);
                                    pDialog.setCancelable(false);
                                    pDialog.setMessage(getString(R.string.Wait_msg));
                                    pDialog.show();
                                    Thread timerThread2 = new Thread() {
                                        public void run() {
                                            returns = Returns.getReturns(getApplicationContext(), " where voucher_no ='" + str_voucher_no + "' ", database);
                                            if (returns == null) {
                                                String result = stock_save();
                                                pDialog.dismiss();
                                                if (result.equals("1")) {
                                                    String rsultPost = stock_cancel();
                                                    if (rsultPost.equals("1")) {
                                                        runOnUiThread(new Runnable() {
                                                            public void run() {
                                                                Toast.makeText(getApplicationContext(), "Cancel successful", Toast.LENGTH_SHORT).show();
                                                                Intent intent1 = new Intent(InvReturnFinalActivity.this, InvReturnListActivity.class);
                                                                startActivity(intent1);
                                                                finish();
                                                            }
                                                        });

                                                    } else {
                                                        runOnUiThread(new Runnable() {
                                                            public void run() {
                                                                Toast.makeText(getApplicationContext(), "Record not Cancel", Toast.LENGTH_SHORT).show();
                                                            }
                                                        });

                                                    }

                                                } else {
                                                    runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            Toast.makeText(getApplicationContext(), "Record not saved", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });

                                                }

                                            } else {
                                                String rsultPost = stock_cancel();
                                                pDialog.dismiss();
                                                if (rsultPost.equals("1")) {
                                                    runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            Toast.makeText(getApplicationContext(), "Cancel successful", Toast.LENGTH_SHORT).show();
                                                            Intent intent1 = new Intent(InvReturnFinalActivity.this, InvReturnListActivity.class);
                                                            startActivity(intent1);
                                                            finish();
                                                        }
                                                    });

                                                } else {
                                                    runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            Toast.makeText(getApplicationContext(), "Record not Cancel", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });

                                                }
                                            }
                                        }
                                    };
                                    timerThread2.start();
                                }
                                break;

                            case R.id.action_delete:
                                if (arraylist.size() == 0) {
                                    Toast.makeText(getApplicationContext(), "No item added", Toast.LENGTH_SHORT).show();
                                } else {
                                    pDialog = new ProgressDialog(InvReturnFinalActivity.this);
                                    pDialog.setCancelable(false);
                                    pDialog.setMessage(getString(R.string.Wait_msg));
                                    pDialog.show();
                                    Thread timerThread3 = new Thread() {
                                        public void run() {
                                            returns = Returns.getReturns(getApplicationContext(), " where voucher_no ='" + str_voucher_no + "' ", database);
                                            if (returns == null) {
                                                pDialog.dismiss();
                                                runOnUiThread(new Runnable() {
                                                    public void run() {
                                                        Toast.makeText(getApplicationContext(), "Cannot delete without saving", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            } else {
                                                String result = stock_delete();

                                                pDialog.dismiss();
                                                if (result.equals("1")) {
                                                    runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            Toast.makeText(getApplicationContext(), "Delete successful", Toast.LENGTH_SHORT).show();
                                                            Intent intent1 = new Intent(InvReturnFinalActivity.this, InvReturnListActivity.class);
                                                            startActivity(intent1);
                                                            finish();
                                                        }
                                                    });

                                                } else {
                                                    runOnUiThread(new Runnable() {
                                                        public void run() {
                                                            Toast.makeText(getApplicationContext(), "Record not saved", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }
                                            }
                                        }
                                    };
                                    timerThread3.start();
                                }
                                break;
                        }
                        return true;
                    }
                });

        edt_toolbar_item_list.setOnKeyListener(new View.OnKeyListener()

                                               {
                                                   @Override
                                                   public boolean onKey(View v, int keyCode, KeyEvent event) {
                                                       if (event.getAction() == KeyEvent.ACTION_DOWN
                                                               && keyCode == KeyEvent.KEYCODE_ENTER) {
                                                           String strValue = edt_toolbar_item_list.getText().toString();
                                                           if (edt_toolbar_item_list.getText().toString().equals("\n") || edt_toolbar_item_list.getText().toString().equals("")) {
                                                               Toast.makeText(getApplicationContext(), "field vaccant", Toast.LENGTH_SHORT).show();
                                                               edt_toolbar_item_list.requestFocus();
                                                           } else {

                                                               if (lite_pos_registration.getproject_id().equals("standalone")) {
                                                                   String strWhere = strValue;
                                                                   Order_Detail order_detail = Order_Detail.getOrder_Detail(getApplicationContext(), "where order_code='" + ordCode + "' and item_code='" + strWhere + "'", database);
                                                                   if (order_detail != null) {
                                                                       strupdate = "";
                                                                       item_code = order_detail.get_item_code();

                                                                       String total;
                                                                       Double linetotal = 0d;
                                                                       Double totalQty = 0d;
                                                                       try {
                                                                           String strLineTotal = Globals.myNumberFormat2Price(Double.parseDouble(order_detail.get_line_total()), decimal_check);
                                                                           String strQty = Globals.myNumberFormat2Price(Double.parseDouble(order_detail.get_quantity()), decimal_check);
                                                                           String strRetnQty = Globals.myNumberFormat2Price(Double.parseDouble(order_detail.get_return_quantity()), decimal_check);
                                                                           try {
                                                                               linetotal = Double.parseDouble(strLineTotal) / Double.parseDouble(strQty);
                                                                           } catch (Exception ex) {
                                                                           }

                                                                           total = Globals.myNumberFormat2Price(linetotal, decimal_check);
                                                                           resultp = Item.getItem(getApplicationContext(), "where item_code='" + item_code + "'", database, db);
                                                                           edt_name.setText(resultp.get_item_name());
                                                                           edt_price.setText(total);
                                                                           edt_qty.setText("1");
                                                                           edt_qty.requestFocus();
                                                                           edt_qty.selectAll();
                                                                           try {
                                                                               totalQty = Double.parseDouble(strQty) - Double.parseDouble(strRetnQty);
                                                                           } catch (Exception ex) {
                                                                           }
                                                                           edt_return_qty.setText(Globals.myNumberFormat2Price(totalQty, decimal_check));
                                                                           edt_total_qty.setText(strQty);
                                                                           edt_toolbar_item_list.setText("");
                                                                       } catch (Exception ex) {
                                                                       }

                                                                   } else {
                                                                       edt_toolbar_item_list.selectAll();
                                                                       Toast.makeText(getApplicationContext(), "No Data Found", Toast.LENGTH_SHORT).show();
                                                                   }
                                                               } else {
                                                                   final String strWhere = strValue;

                                                                   pDialog = new ProgressDialog(InvReturnFinalActivity.this);
                                                                   pDialog.setCancelable(false);
                                                                   pDialog.setMessage(getString(R.string.Wait_msg));
                                                                   pDialog.show();

                                                                   Thread timerThread = new Thread() {
                                                                       public void run() {
                                                                           try {
                                                                               sleep(1000);
                                                                               relt = getItem(strWhere);
                                                                               pDialog.dismiss();
                                                                               if (relt.equals("1")) {
                                                                               } else {
                                                                                   runOnUiThread(new Runnable() {
                                                                                       public void run() {
                                                                                           Toast.makeText(getApplicationContext(), "Item Not Valid!", Toast.LENGTH_SHORT).show();
                                                                                       }
                                                                                   });
                                                                               }
                                                                           } catch (InterruptedException e) {
                                                                               e.printStackTrace();
                                                                           } finally {
                                                                           }
                                                                       }
                                                                   };
                                                                   timerThread.start();

                                                               }

                                                           }
                                                           return true;
                                                       }
                                                       return false;
                                                   }
                                               }
        );
    }

    private String stock_updt_cloud() {
        String suc = "0", strItmCode = "";
        JSONObject sender = new JSONObject();
        JSONArray result = new JSONArray();
        JSONObject row = new JSONObject();
        try {
            returns = Returns.getReturns(getApplicationContext(), " where voucher_no ='" + str_voucher_no + "' ", database);
            if (returns == null) {
            } else {
//                if (returns.get_is_post().equals("true")) {
                ArrayList<Return_detail> return_detailArrayList = Return_detail.getAllReturn_detail(getApplicationContext(), " where ref_voucher_no='" + str_voucher_no + "'", database);
                if (return_detailArrayList.size() > 0) {

                    for (int i = 0; i < return_detailArrayList.size(); i++) {
                        strItmCode = return_detailArrayList.get(i).get_item_code();
                        row.put("item_code", strItmCode);
                        row.put("return_qty", return_detailArrayList.get(i).get_qty());
                        result.put(row);
                    }
                    sender.put("Item".toLowerCase(), result);
                }
                if (result.length() > 0) {
                    String serverData = send_returnon_svr(sender.toString());
                    final JSONObject collection_jsonObject1 = new JSONObject(serverData);
                    final String strStatus = collection_jsonObject1.getString("status");
                    if (strStatus.equals("true")) {
                        suc = "1";
                    }
                }
            }
//            }


        } catch (Exception e) {
        }
        return suc;
    }


    private String getItem(String strWhere) {
        String succ = "0";
        String serverData = getItemFromServer(strWhere);
        try {
            final JSONObject jsonObject_bg = new JSONObject(serverData);
            final String strStatus = jsonObject_bg.getString("status");
            if (strStatus.equals("true")) {
                try {
                    JSONArray jsonArray = jsonObject_bg.getJSONArray("result");
                    if (jsonArray.length() > 0) {
                        succ = "1";
                        for (int k = 0; k < jsonArray.length(); k++) {
                            final JSONObject jsonObject = jsonArray.getJSONObject(k);
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    String total;
                                    Double linetotal = 0d;
                                    Double totalQty = 0d;
                                    try {
                                        String strLineTotal = Globals.myNumberFormat2Price(Double.parseDouble(jsonObject.getString("line_total")), decimal_check);
                                        String strQty = Globals.myNumberFormat2Price(Double.parseDouble(jsonObject.getString("quantity")), decimal_check);
                                        String strRetnQty = Globals.myNumberFormat2Price(Double.parseDouble(jsonObject.getString("return_quantity")), decimal_check);
                                        String strItemCode = jsonObject.getString("item_code");
                                        try {
                                            linetotal = Double.parseDouble(strLineTotal) / Double.parseDouble(strQty);
                                        } catch (Exception ex) {
                                        }

                                        total = Globals.myNumberFormat2Price(linetotal, decimal_check);
                                        resultp1 = Item.getItem(getApplicationContext(), "where item_code='" + strItemCode + "'", database, db);
                                        edt_name.setText(resultp1.get_item_name());
                                        try {
                                            if (resultp1.get_item_name().length() > 30) {
                                                edt_name.setText(resultp1.get_item_name().substring(0, 30));
                                            } else {
                                                edt_name.setText(resultp1.get_item_name());
                                            }
                                        } catch (Exception ex) {
                                        }
                                        edt_price.setText(total);
                                        edt_qty.setText("1");
                                        edt_qty.requestFocus();
                                        edt_qty.selectAll();
                                        try {
                                            totalQty = Double.parseDouble(strQty) - Double.parseDouble(strRetnQty);
                                        } catch (Exception ex) {
                                        }
                                        edt_return_qty.setText(Globals.myNumberFormat2Price(totalQty, decimal_check));
                                        edt_total_qty.setText(strQty);
                                        edt_toolbar_item_list.setText("");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                }
                            });


                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                edt_toolbar_item_list.requestFocus();
                                edt_toolbar_item_list.selectAll();
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                runOnUiThread(new Runnable() {
                    public void run() {
                        edt_toolbar_item_list.requestFocus();
                        edt_toolbar_item_list.selectAll();
                    }
                });
            }
        } catch (JSONException e) {
        }
        return succ;
    }

    private String getItemFromServer(String item_code) {
        String serverData = null;//
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(
                "http://" + Globals.App_IP + "/lite-pos/index.php/api/invoice_return/get_item");
        ArrayList nameValuePairs = new ArrayList(5);
        nameValuePairs.add(new BasicNameValuePair("company_id", Globals.Company_Id));
        nameValuePairs.add(new BasicNameValuePair("order_code", ordCode));
        nameValuePairs.add(new BasicNameValuePair("item_code", item_code));
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

    private String send_returnon_svr(String data) {
        String cmpnyId = Globals.Company_Id;
        String serverData = null;//
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(
                "http://" + Globals.App_IP + "/lite-pos/index.php/api/invoice_return/update_return_quantity");
        ArrayList nameValuePairs = new ArrayList(5);
        nameValuePairs.add(new BasicNameValuePair("company_id", cmpnyId));
        nameValuePairs.add(new BasicNameValuePair("order_code", ordCode));
        nameValuePairs.add(new BasicNameValuePair("data", data));
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

    private String send_online_return() {
        String result = Returns.sendOnServer(getApplicationContext(), database, db, "Select * FROM  returns WHERE is_push = 'N' and is_post='false'");
        return result;
    }

    private String get_total() {
        Double total = 0d;
        try {
            if (arraylist.size() > 0) {
                for (int i = 0; i < arraylist.size(); i++) {
                    total = total + Double.parseDouble(arraylist.get(i).getLine_total());
                }
            }
        } catch (Exception e) {
        }
        return total + "";
    }

    private String stock_cancel() {
        String suc = "0";
        returns = Returns.getReturns(getApplicationContext(), " where voucher_no ='" + str_voucher_no + "' ", database);
        returns.set_is_cancel("true");
        long l = returns.updateReturns("voucher_no=?", new String[]{str_voucher_no}, database);
        if (l > 0) {
            suc = "1";
        }
        return suc;
    }

    private String stock_post() {
        String suc = "0";
        returns = Returns.getReturns(getApplicationContext(), " where voucher_no ='" + str_voucher_no + "' ", database);
        returns.set_is_post("true");
        long l = returns.updateReturns("voucher_no=?", new String[]{str_voucher_no}, database);
        if (l > 0) {
            suc = "1";
        }
        return suc;
    }

    private String stock_delete() {
        String suc = "0";
        returns = Returns.getReturns(getApplicationContext(), " where voucher_no ='" + str_voucher_no + "' ", database);
        returns.set_is_active("0");
        long l = returns.updateReturns("voucher_no=?", new String[]{str_voucher_no}, database);
        if (l > 0) {
            suc = "1";
        }
        return suc;
    }

    private String stock_save() {
        String suc = "0";
        try {
            database.beginTransaction();
            String total = get_total();
            returns = Returns.getReturns(getApplicationContext(), " where voucher_no ='" + str_voucher_no + "' ", database);
            if (returns == null) {
                returns = new Returns(getApplicationContext(), null, "", str_voucher_no, str_date, str_remarks, total, "0", "false", "false", "1", "N", Globals.user, date, ordCode, "IR", PayId);
                long l = returns.insertReturns(database);
                if (l > 0) {
                    suc = "1";
                    for (int i = 0; i < arraylist.size(); i++) {
                        return_detail = new Return_detail(getApplicationContext(), null, str_voucher_no, i + 1 + "", arraylist.get(i).getItem_code(), arraylist.get(i).getQty(), arraylist.get(i).getPrice(), arraylist.get(i).getLine_total());
                        long l1 = return_detail.insertReturn_detail(database);
                        if (l1 > 0) {
                            suc = "1";
                        }
                    }
                }
            } else {
                returns = new Returns(getApplicationContext(), returns.get_id(), "", str_voucher_no, str_date, str_remarks, total, returns.get_z_code(), returns.get_is_post(), returns.get_is_cancel(), "1", "N", Globals.user, date, ordCode, "IR", PayId);
                long l = returns.updateReturns("voucher_no=?", new String[]{str_voucher_no}, database);
                if (l > 0) {
                    suc = "1";
                    long e6 = Return_detail.delete_Return_detail(getApplicationContext(), "return_detail", " ref_voucher_no =? ", new String[]{str_voucher_no}, database);
                    for (int i = 0; i < arraylist.size(); i++) {
                        return_detail = new Return_detail(getApplicationContext(), null, str_voucher_no, i + 1 + "", arraylist.get(i).getItem_code(), arraylist.get(i).getQty(), arraylist.get(i).getPrice(), arraylist.get(i).getLine_total());
                        long l1 = return_detail.insertReturn_detail(database);
                        if (l1 > 0) {
                            suc = "1";
                        }
                    }
                }
            }

            if (suc.equals("1")) {
                // Accounts affect here
                if (!cusCode.equals("")) {
                    if (PayId.equals("5")) {
                        Acc_Customer acc_customer = Acc_Customer.getAcc_Customer(getApplicationContext(), " where contact_code='" + cusCode + "'", database);
                        Double strOldBalance = 0d;
                        Double strAmount = 0d;
                        if (acc_customer == null) {
//                    strOldBalance = Double.parseDouble(acc_customer.get_amount());
                            strAmount = strOldBalance + Double.parseDouble(returns.get_total());
                            acc_customer = new Acc_Customer(getApplicationContext(), null, cusCode, strAmount + "");
                            acc_customer.insertAcc_Customer(database);
                        } else {
                            strOldBalance = Double.parseDouble(acc_customer.get_amount());
                            strAmount = strOldBalance + Double.parseDouble(returns.get_total());
                            acc_customer.set_amount(strAmount + "");
                            long a = acc_customer.updateAcc_Customer("contact_code=?", new String[]{cusCode}, database);
                        }
                        database.setTransactionSuccessful();
                        database.endTransaction();
                    }
                }

                database.setTransactionSuccessful();
                database.endTransaction();

            }
        } catch (Exception ex) {
            database.endTransaction();
        }
        return suc;
    }

    private void list_load(ArrayList<StockAdjectmentDetailList> arraylist) {
        ListView list = (ListView) findViewById(R.id.list);
        if (arraylist.size() > 0) {
            returnFinalListAdapter = new InvReturnFinalListAdapter(InvReturnFinalActivity.this, arraylist);
            list.setVisibility(View.VISIBLE);
            list.setAdapter(returnFinalListAdapter);
            returnFinalListAdapter.notifyDataSetChanged();
        } else {
            list.setVisibility(View.GONE);
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_retail, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            String strValue = edt_toolbar_item_list.getText().toString();
//            if (edt_toolbar_item_list.getText().toString().equals("\n") || edt_toolbar_item_list.getText().toString().equals("")) {
//                Toast.makeText(getApplicationContext(), "field vaccant", Toast.LENGTH_SHORT).show();
//                edt_toolbar_item_list.requestFocus();
//            } else {
//                String strWhere = "Where item_code = '" + strValue + "' or item_name ='" + strValue + "' or barcode= '" + strValue + "' or sku = '" + strValue + "'";
//                arrayListItem = Item.getAllItem(getApplicationContext(), strWhere, database);
//                if (arrayListItem.size() >= 1) {
//                    strupdate = "";
//                    resultp = arrayListItem.get(0);
//                    item_code = resultp.get_item_code();
//                    Item_Location item_location = Item_Location.getItem_Location(getApplicationContext(), "Where item_code = '" + item_code + "'", database);
//                    if (item_location == null) {
//                        sale_priceStr = "0";
//                    } else {
//                        sale_priceStr = item_location.get_selling_price();
//                    }
//                    String item_price;
//                    item_price = Globals.myNumberFormat2Price(Double.parseDouble(sale_priceStr), decimal_check);
//                    edt_name.setText(resultp.get_item_name());
//                    edt_price.setText(item_price);
//                    edt_qty.setText("1");
//                    edt_toolbar_item_list.setText("");
//                    closeKeyboard();
//                } else {
//                    edt_toolbar_item_list.selectAll();
//                    Toast.makeText(getApplicationContext(), "No Data Found", Toast.LENGTH_SHORT).show();
//                }
//            }
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    public void setTextView(String position, String item_code, String item_name, String qty, String price, String strUpdate) {
        if (lite_pos_registration.getproject_id().equals("standalone")) {
            try {
                Position = Integer.parseInt(position);
                strupdate = strUpdate;
                strItemCode = item_code;
                String item_price;
                item_price = Globals.myNumberFormat2Price(Double.parseDouble(price), decimal_check);
                edt_price.setText(item_price);
                edt_name.setText(item_name);
                edt_qty.setText(qty);
            } catch (Exception ex) {
            }
        } else {
            try {
                Position = Integer.parseInt(position);
                strupdate = strUpdate;
                strItemCode = item_code;
                String item_price;
                item_price = Globals.myNumberFormat2Price(Double.parseDouble(price), decimal_check);
                edt_price.setText(item_price);
                edt_name.setText(item_name);
                Double updReturnQty = Double.parseDouble(qty) + Double.parseDouble(edt_return_qty.getText().toString());
                edt_return_qty.setText(Globals.myNumberFormat2Price(updReturnQty, decimal_check));
                edt_qty.setText(qty);
                edt_qty.requestFocus();
                edt_qty.selectAll();
            } catch (Exception ex) {
            }

        }
    }

    @Override
    public void onBackPressed() {
        pDialog = new ProgressDialog(InvReturnFinalActivity.this);
        pDialog.setCancelable(false);
        pDialog.setMessage(getString(R.string.Wait_msg));
        pDialog.show();

        Thread timerThread = new Thread() {
            public void run() {
                try {
                    sleep(1000);
                    pDialog.dismiss();
                    Intent intent = new Intent(InvReturnFinalActivity.this, InvReturnHeaderActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("operation", operation);
                    intent.putExtra("voucher_no", str_voucher_no);
                    intent.putExtra("date", str_date);
                    intent.putExtra("remarks", str_remarks);
                    intent.putExtra("contact_code", "");
                    intent.putExtra("order_code", ordCode);
                    startActivity(intent);
                    startActivity(intent);
                    finish();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                }
            }
        };
        timerThread.start();
    }

    private String stock_update() {
        String suc = "0";
        try {
            returns = Returns.getReturns(getApplicationContext(), " where voucher_no ='" + str_voucher_no + "' ", database);
            if (returns == null) {
            } else {
                if (returns.get_is_post().equals("true")) {
                    ArrayList<Return_detail> return_detailArrayList = Return_detail.getAllReturn_detail(getApplicationContext(), " where ref_voucher_no='" + str_voucher_no + "'", database);
                    if (return_detailArrayList.size() > 0) {
                        for (int i = 0; i < return_detailArrayList.size(); i++) {
                            Item_Location item_location = Item_Location.getItem_Location(getApplicationContext(), "where item_code='" + return_detailArrayList.get(i).get_item_code() + "'", database);
                            Double updatedQty = 0d;
                            if (item_location != null) {
                                Double avlQty = Double.parseDouble(item_location.get_quantity());
                                Double effectiveQty = Double.parseDouble(return_detailArrayList.get(i).get_qty());

                                updatedQty = avlQty + effectiveQty;

                                item_location.set_quantity(updatedQty + "");
                                long l = item_location.updateItem_Location("item_code=?", new String[]{return_detailArrayList.get(i).get_item_code()}, database);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return suc;
    }

    private void closeKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
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

    private void print_return() {
        returns = Returns.getReturns(InvReturnFinalActivity.this, "WHERE voucher_no = '" + str_voucher_no + "'", database);
        return_details = Return_detail.getAllReturn_detail(InvReturnFinalActivity.this,
                "WHERE ref_voucher_no = '" + str_voucher_no + "'", database);

        mylist = getlist();
        if (mylist != null) {
            adp = new MyAdapter(getApplicationContext(), mylist);
        }

        if (iswifi) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        performOperationEn();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            thread.start();

        } else if (PrinterType.equals("1")) {
            try {
                if (woyouService == null) {
                } else {
                    mobile_pos(returns, return_details);
                }
            } catch (Exception ex) {
            }
        } else if (PrinterType.equals("3")) {
            String result = bluetooth_56(returns, return_details);
        } else if (PrinterType.equals("4")) {
            String result = bluetooth_80(returns, return_details);
        }
    }

    private void performOperationEn() {
        noofPrint = Integer.parseInt(settings.get_No_Of_Print());
        if (mylist.size() > 0) {
            try {
                String bill = "";
                for (String data : mylist) {
                    bill = bill + data;
                }

                for (int k = 0; k < noofPrint; k++) {
                    WifiPrintDriver.Begin();
                    WifiPrintDriver.ImportData(bill);
                    WifiPrintDriver.ImportData("\r");
                    WifiPrintDriver.excute();
                    WifiPrintDriver.ClearData();
                    String str = "\r\n\r\n\r\n\r\n";
                    byte[] feed = str.getBytes();
                    WifiPrintDriver.SPPWrite(feed);
                    byte[] paramString1 = new byte[]{27, 109, 2};
                    WifiPrintDriver.SPPWrite(paramString1);
                    WifiPrintDriver.excute();
                    WifiPrintDriver.ClearData();
                    InvReturnFinalActivity.this.finish();
                }
            } catch (Exception e) {
            }

        }
    }

    public String LableCentre(String InvoiceLabel) {
        int ln = InvoiceLabel.trim().length();
        int rem = 42 - ln;
        int part = rem / 2;
        String tt1 = "";
        for (int i = 0; i < part; i++) {
            tt1 = tt1 + " ";
        }
        tt1 = tt1 + InvoiceLabel;
        for (int i = 0; i < part; i++) {
            tt1 = tt1 + " ";
        }
        return tt1;
    }

    public Bitmap StringToBitMap(String encodedString) {
        try {
            byte[] encodeByte = Base64.decode(encodedString, Base64.DECODE);
            Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch (Exception e) {
            e.getMessage();
            return null;
        }
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
        // GET CURRENT SIZE
        int width = bm.getWidth();
        int height = bm.getHeight();
        // GET SCALE SIZE
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);
        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }


    private ArrayList<String> getlist() {
        ArrayList<String> mylist = new ArrayList<String>();
        String lbl;
        String tt = "", tt1 = "";
        mylist.add("\n-----------------------------------------------");
        lbl = LableCentre(Globals.objLPR.getCompany_Name());
        mylist.add("\n" + lbl);
        lbl = LableCentre(Globals.objLPR.getAddress());
        mylist.add("\n" + lbl);
        lbl = LableCentre(Globals.objLPR.getMobile_No());
        mylist.add("\n" + lbl);
        try {
            if (Globals.objLPR.getService_code_tariff().equals("null")) {

            } else {
                lbl = LableCentre(Globals.objLPR.getService_code_tariff());
                mylist.add("\n" + lbl);
//                    mylist.add("\n" + "Tariff Code");
            }
        } catch (Exception ex) {
        }
        if (Globals.objLPR.getLicense_No().equals("null")) {
        } else {
            mylist.add("\n" + Globals.GSTNo + ":" + Globals.objLPR.getLicense_No());
        }

//        lbl = LableCentre(Globals.PrintOrder);
//        mylist.add("\n" + lbl);
        mylist.add("\n");
        mylist.add("\n" + "Voucher No" + ":" + str_voucher_no);
        mylist.add("\n" + "Return Date" + ":" + returns.get_date());
        mylist.add("\n" + Globals.PrintDeviceID + ":" + Globals.objLPD.getDevice_Name());

        user = User.getUser(getApplicationContext(), " Where user_code='" + Globals.user + "'", database);
        mylist.add("\n" + Globals.PrintCashier + ":" + user.get_name());

        if (Globals.strContact_Code.equals("")) {
            Globals.strContact_Name = "";
        } else {
            try {
                Contact contact = Contact.getContact(getApplicationContext(), database, db, " WHERE contact_code='" + Globals.strContact_Code + "'");
                mylist.add("\n" + "Customer   : " + contact.get_name());
                if (contact.get_gstin().length() > 0) {
                    mylist.add("\n" + "Customer GST No.: " + contact.get_gstin());
                }
                Globals.strContact_Name = contact.get_name();
            } catch (Exception ex) {
            }
        }

        mylist.add("\n-----------------------------------------------");
        mylist.add("\nItem                    Qty     Price   Total");
        mylist.add("\n-----------------------------------------------\n");
        Double itemFinalTax = 0d;
        int count = 0;
        while (count < return_details.size()) {

            String strItemCode = return_details.get(count).get_item_code();

            String strItemName = Order_Detail.getItemName(getApplicationContext(), " WHERE order_detail.item_code  = '"
                    + strItemCode + "'  GROUP By order_detail.item_Code");

            String line_total;
            line_total = Globals.myNumberFormat2Price(Double.parseDouble(return_details.get(count).get_line_total()), decimal_check);

            //item name
            int l1 = strItemName.length();
            if (l1 > 24) {

                char[] nm = strItemName.toUpperCase().toCharArray();
                for (int k = 0; k < 24; k++) {

                    tt = tt + nm[k];
                }
                tt = tt + " ";
            } else {
                char[] nm = strItemName.toUpperCase()
                        .toCharArray();
                for (int k = 0; k < l1; k++) {

                    tt = tt + nm[k];
                }
                int space = 24 - l1;
                for (int v = 0; v < space; v++) {

                    tt = tt + " ";
                }
            }

            //quantity
            int l2 = Globals.myNumberFormat2QtyDecimal(Double.parseDouble(return_details.get(count).get_qty()), qty_decimal_check).length();
            if (l2 > 8) {
                char[] qt = Globals.myNumberFormat2QtyDecimal(Double.parseDouble(return_details.get(count).get_qty()), qty_decimal_check).toCharArray();
                for (int k = 0; k < 8; k++) {
                    tt = tt + qt[k];
                }
                tt = tt + " ";
            } else {
                char[] qt = Globals.myNumberFormat2QtyDecimal(Double.parseDouble(return_details.get(count).get_qty()), qty_decimal_check).toCharArray();
                for (int k = 0; k < l2; k++) {

                    tt = tt + qt[k];
                }
                int space = 8 - l2;
                for (int v = 0; v < space; v++) {
                    tt = tt + " ";
                }
            }

            String sale_price;

            Double dDisAfterSalePrice = 0d;

            dDisAfterSalePrice = (((Double.parseDouble(return_details.get(count).get_line_total()) / Double.parseDouble(return_details.get(count).get_qty()))));
            sale_price = Globals.myNumberFormat2Price(Double.parseDouble(dDisAfterSalePrice + ""), decimal_check);

            //price
            int l12 = sale_price.length();
            if (l12 > 6) {
                char[] qt = sale_price.toCharArray();
                for (int k = 0; k < 6; k++) {
                    tt = tt + qt[k];
                }
                tt = tt + " ";

            } else {
                char[] qt = sale_price.toCharArray();
                for (int k = 0; k < l12; k++) {

                    tt = tt + qt[k];
                }
                int space = 6 - l12;
                for (int v = 0; v < space; v++) {
                    tt = tt + " ";
                }
            }

            //total
            int l3 = line_total.length();
            if (l3 > 7) {
                char[] r = String.valueOf(line_total).toCharArray();
                for (int k = 0; k < 3; k++) {

                    tt = tt + r[k];
                }

                mylist.add(tt);
                tt = "\n";
            } else {
                int space = 7 - l3;
                for (int v = 0; v < space; v++) {
                    tt = tt + " ";
                }
                char[] r = String.valueOf(line_total).toCharArray();
                for (int k = 0; k < l3; k++) {
                    tt = tt + r[k];
                }
                mylist.add(tt);
                tt = "\n";
            }

            mylist.add(tt);
            count++;
        }

        mylist.add("\n-----------------------------------------------");

        String net_amount;
        net_amount = Globals.myNumberFormat2Price(Double.parseDouble(returns.get_total()), decimal_check);

        tt = "";
        int ln = 0;
        ln = net_amount.length();
        int space = 9 - ln;
        for (int v = 0; v < space; v++) {
            tt = tt + " ";
        }
        tt = tt + net_amount;

        String strCurrency;
        if (Globals.objLPD.getCurreny_Symbol().equals("")) {
            strCurrency = "";
        } else {
            strCurrency = "(" + Globals.objLPD.getCurreny_Symbol() + ")";
        }
        mylist.add("\nNet Amount  :  " + tt + strCurrency);

        if (!settings.get_Footer_Text().equals("")) {
            mylist.add("\n    " + settings.get_Footer_Text());
        }
        mylist.add("\n            " + settings.get_Copy_Right());
        mylist.add("\n");
        mylist.add("\n");
        Globals.strContact_Code = "";
        Globals.strResvContact_Code = "";
        Globals.strOldCrAmt = "0";
        Globals.setEmpty();
        return mylist;
    }

    private String bluetooth_80(Returns returns, ArrayList<Return_detail> return_details) {
        String msg = "", flag = "0";
        String lang = getString(R.string.bluetooth_strLang);
        byte[] cmd = new byte[3];
        cmd[0] = 0x1b;
        cmd[1] = 0x21;
        byte[] ab;
        if ((lang.compareTo("en")) == 0) {

            try {
                if (mService.isAvailable() == false) {
                } else {
                    ab = BytesUtil.setAlignCenter(1);
                    mService.write(ab);
                    mService.sendMessage("" + Globals.objLPR.getCompany_Name().toUpperCase(), "GBK");
                    mService.sendMessage("" + Globals.objLPR.getAddress(), "GBK");
                    mService.sendMessage("" + Globals.objLPR.getMobile_No(), "GBK");
                    try {
                        if (Globals.objLPR.getService_code_tariff() == null) {
                        } else {
                            mService.sendMessage("" + Globals.objLPR.getLicense_No(), "GBK");
                        }
                    } catch (Exception ex) {
                    }

                    ab = BytesUtil.setAlignCenter(0);
                    mService.write(ab);
                    if (Globals.objLPR.getLicense_No().equals("null") || Globals.objLPR.getLicense_No().equals("")) {
                    } else {
                        mService.sendMessage(Globals.GSTNo + " : " + Globals.objLPR.getLicense_No(), "GBK");
                    }
                    ab = BytesUtil.setAlignCenter(1);
                    mService.write(ab);
                    mService.sendMessage(Globals.PrintOrder, "GBK");
                    if (Globals.strIsBarcodePrint.equals("true")) {
                        byte[] sendData;
                        sendData = BytesUtil.getPrintQRCode(str_voucher_no, 1, 0);
                        mService.write(sendData);
                    }
                    ab = BytesUtil.setAlignCenter(0);
                    mService.write(ab);

                    mService.sendMessage(Globals.PrintInvNo + " : " + str_voucher_no, "GBK");
                    mService.sendMessage(Globals.PrintInvDate + "   : " + this.returns.get_date(), "GBK");
                    mService.sendMessage(Globals.PrintDeviceID + "      : " + Globals.objLPD.getDevice_Name(), "GBK");
                    user = User.getUser(getApplicationContext(), " Where user_code='" + Globals.user + "'", database);
                    mService.sendMessage(Globals.PrintCashier + "    : " + user.get_name(), "GBK");

                    if (Globals.ModeResrv.equals("Resv")) {
                        Contact contact = Contact.getContact(getApplicationContext(), database, db, " WHERE contact_code='" + Globals.CustomerResrv + "'");
                        mService.sendMessage("Customer     : " + contact.get_name(), "GBK");
                        if (contact.get_gstin().length() > 0) {
                            mService.sendMessage("Customer GST No. : " + contact.get_gstin(), "GBK");
                        }
                    } else {
                        if (Globals.strContact_Code.equals("")) {
                        } else {
                            Contact contact = Contact.getContact(getApplicationContext(), database, db, " WHERE contact_code='" + Globals.strContact_Code + "'");
                            mService.sendMessage("Customer       : " + contact.get_name(), "GBK");
                            if (contact.get_gstin().length() > 0) {
                                mService.sendMessage("Customer GST No. : " + contact.get_gstin(), "GBK");
                            }
                        }
                    }

                    mService.sendMessage("................................................", "GBK");
                    mService.sendMessage("Item Name           Qty     Price      Total", "GBK");
                    mService.sendMessage("................................................", "GBK");

                    int count = 0;
                    Double itemFinalTax = 0d;
                    while (count < return_details.size()) {
                        String strItemCode = return_details.get(count).get_item_code();
                        String strItemName = Order_Detail.getItemName(getApplicationContext(), "WHERE order_detail.item_Code  = '"
                                + strItemCode + "'  GROUP By order_detail.item_code");
                        String sale_price;
                        Double dDisAfterSalePrice = 0d;
                        dDisAfterSalePrice = (((Double.parseDouble(return_details.get(count).get_line_total()) / Double.parseDouble(return_details.get(count).get_qty()))));
                        sale_price = Globals.myNumberFormat2Price(Double.parseDouble(dDisAfterSalePrice + ""), decimal_check);

                        String line_total;
                        line_total = Globals.myNumberFormat2Price(Double.parseDouble(return_details.get(count).get_line_total()), decimal_check);
                        mService.sendMessage("" + strItemName, "GBK");
                        mService.sendMessage("                    " + return_details.get(count).get_qty() + "      " + sale_price + "      " + line_total, "GBK");
                        count++;
                    }

                    mService.sendMessage("................................................", "GBK");
                    String net_amount;
                    net_amount = Globals.myNumberFormat2Price(Double.parseDouble(returns.get_total()), decimal_check);
                    String strCurrency;
                    if (Globals.objLPD.getCurreny_Symbol().equals("")) {
                        strCurrency = "";
                    } else {
                        strCurrency = "(" + Globals.objLPD.getCurreny_Symbol() + ")";
                    }
                    mService.sendMessage("Net Amount      : " + net_amount + "" + strCurrency, "GBK");

                    if (!settings.get_Footer_Text().equals("")) {
                        mService.sendMessage(settings.get_Footer_Text(), "GBK");
                    }
                    mService.sendMessage("             " + settings.get_Copy_Right() + "\n\n", "GBK");
                    cmd[2] &= 0xEF;
                    mService.write(cmd);
                }
                Globals.strContact_Code = "";
                Globals.strResvContact_Code = "";
                Globals.strOldCrAmt = "0";

                flag = "1";
                Globals.setEmpty();

            } catch (Exception ex) {
            }
        }
        return flag;
    }

    private String bluetooth_56(Returns returns, ArrayList<Return_detail> return_details) {
        String msg = "", flag = "0";
        String lang = getString(R.string.bluetooth_strLang);
        byte[] cmd = new byte[3];
        cmd[0] = 0x1b;
        cmd[1] = 0x21;
        byte[] ab;
        if ((lang.compareTo("en")) == 0) {
            try {
                if (mService.isAvailable() == false) {
                } else {
                    ab = BytesUtil.setAlignCenter(1);
                    mService.write(ab);
                    mService.sendMessage("" + Globals.objLPR.getCompany_Name().toUpperCase(), "GBK");
                    mService.sendMessage("" + Globals.objLPR.getAddress(), "GBK");
                    mService.sendMessage("" + Globals.objLPR.getMobile_No(), "GBK");

                    try {
                        if (Globals.objLPR.getService_code_tariff() == null) {
                        } else {
                            mService.sendMessage("" + Globals.objLPR.getLicense_No(), "GBK");
                        }
                    } catch (Exception ex) {
                    }

                    ab = BytesUtil.setAlignCenter(0);
                    mService.write(ab);
                    if (Globals.objLPR.getLicense_No().equals("null") || Globals.objLPR.getLicense_No().equals("")) {
                    } else {
                        mService.sendMessage(Globals.GSTNo + " : " + Globals.objLPR.getLicense_No(), "GBK");
                    }
                    ab = BytesUtil.setAlignCenter(1);
                    mService.write(ab);
                    mService.sendMessage(Globals.PrintOrder, "GBK");
                    if (Globals.strIsBarcodePrint.equals("true")) {
                        byte[] sendData;
                        sendData = BytesUtil.getPrintQRCode(str_voucher_no, 1, 0);
                        mService.write(sendData);
                    }
                    ab = BytesUtil.setAlignCenter(0);
                    mService.write(ab);

                    mService.sendMessage(Globals.PrintInvNo + ":" + str_voucher_no, "GBK");
                    mService.sendMessage(Globals.PrintInvDate + ":" + returns.get_date(), "GBK");
                    mService.sendMessage(Globals.PrintDeviceID + ":" + Globals.objLPD.getDevice_Name(), "GBK");
                    user = User.getUser(getApplicationContext(), " Where user_code='" + Globals.user + "'", database);
                    mService.sendMessage(Globals.PrintCashier + ":" + user.get_name(), "GBK");

                    if (Globals.ModeResrv.equals("Resv")) {
                        Contact contact = Contact.getContact(getApplicationContext(), database, db, " WHERE contact_code='" + Globals.CustomerResrv + "'");
                        mService.sendMessage("Customer     :" + contact.get_name(), "GBK");
                        if (contact.get_gstin().length() > 0) {
                            mService.sendMessage("Customer GST No. :" + contact.get_gstin(), "GBK");
                        }
                    } else {
                        if (Globals.strContact_Code.equals("")) {
                        } else {
                            Contact contact = Contact.getContact(getApplicationContext(), database, db, " WHERE contact_code='" + Globals.strContact_Code + "'");
                            mService.sendMessage("Customer     :" + contact.get_name(), "GBK");
                            if (contact.get_gstin().length() > 0) {
                                mService.sendMessage("Customer GST No. :" + contact.get_gstin(), "GBK");
                            }
                        }
                    }

                    mService.sendMessage("................................", "GBK");
                    mService.sendMessage("Item Name       Qty       Price", "GBK");
                    mService.sendMessage("                          Total", "GBK");
                    mService.sendMessage("................................\n", "GBK");

                    int count = 0;
                    Double itemFinalTax = 0d;
                    while (count < return_details.size()) {

                        String strItemCode = return_details.get(count).get_item_code();
                        String strItemName = Order_Detail.getItemName(getApplicationContext(), "WHERE order_detail.item_Code  = '"
                                + strItemCode + "'  GROUP By order_detail.item_code");
                        String sale_price;
                        Double dDisAfterSalePrice = 0d;
                        dDisAfterSalePrice = (((Double.parseDouble(return_details.get(count).get_line_total()) / Double.parseDouble(return_details.get(count).get_qty()))));
                        sale_price = Globals.myNumberFormat2Price(Double.parseDouble(dDisAfterSalePrice + ""), decimal_check);
                        String line_total;
                        line_total = Globals.myNumberFormat2Price(Double.parseDouble(return_details.get(count).get_line_total()), decimal_check);
                        mService.sendMessage("" + strItemName, "GBK");
                        mService.sendMessage("        " + return_details.get(count).get_qty() + "  " + sale_price + "  " + line_total, "GBK");
                        count++;
                    }
                    mService.sendMessage("................................", "GBK");
                    String net_amount;
                    net_amount = Globals.myNumberFormat2Price(Double.parseDouble(returns.get_total()), decimal_check);
                    String strCurrency;
                    if (Globals.objLPD.getCurreny_Symbol().equals("")) {
                        strCurrency = "";
                    } else {
                        strCurrency = "(" + Globals.objLPD.getCurreny_Symbol() + ")";
                    }
                    mService.sendMessage("Net Amt   :" + net_amount + "" + strCurrency, "GBK");
                    if (!settings.get_Footer_Text().equals("")) {
                        mService.sendMessage(settings.get_Footer_Text(), "GBK");
                    }
                    mService.sendMessage("      " + settings.get_Copy_Right() + "\n\n", "GBK");
                    cmd[2] &= 0xEF;
                    mService.write(cmd);
                }
                Globals.strContact_Code = "";
                Globals.strResvContact_Code = "";
                Globals.strOldCrAmt = "0";
                flag = "1";
                Globals.setEmpty();
            } catch (Exception ex) {
            }
        }
        return flag;
    }

    private void mobile_pos(final Returns returns, final ArrayList<Return_detail> return_details) {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int k = 0; k < Integer.parseInt(settings.get_No_Of_Print()); k++) {
                        woyouService.setAlignment(1, callback);
                        Bitmap bitmap = StringToBitMap(settings.get_Logo());
                        if (bitmap != null) {
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 50, stream);
                            bitmap = getResizedBitmap(bitmap, 80, 120);
                            woyouService.printBitmap(bitmap, callback);
                        }

                        woyouService.printTextWithFont(" \n", "", 24, callback);
                        woyouService.setAlignment(1, callback);
                        woyouService.printTextWithFont("" + Globals.objLPR.getCompany_Name() + "\n", "", 30, callback);
                        woyouService.printTextWithFont("" + Globals.objLPR.getAddress() + "\n", "", 30, callback);
                        woyouService.printTextWithFont("" + Globals.objLPR.getMobile_No() + "\n", "", 30, callback);
                        try {
                            if (Globals.objLPR.getService_code_tariff().equals("null") || Globals.objLPR.getService_code_tariff().equals("")) {
                            } else {
                                woyouService.printTextWithFont("" + Globals.objLPR.getService_code_tariff() + "\n", "", 30, callback);
                            }
                        } catch (Exception ex) {
                        }
                        woyouService.setFontSize(30, callback);
                        if (Globals.objLPR.getLicense_No().equals("null") || Globals.objLPR.getLicense_No().equals("")) {
                        } else {
                            woyouService.printColumnsText(new String[]{Globals.GSTNo, ":", Globals.objLPR.getLicense_No()}, new int[]{6, 1, 20}, new int[]{0, 0, 0}, callback);
                        }

                        woyouService.setAlignment(1, callback);
                        if (Globals.strIsBarcodePrint.equals("true")) {
                            woyouService.printBarCode(str_voucher_no, 8, 60, 120, 0, callback);
                            woyouService.printTextWithFont(" \n", "", 24, callback);
                        }
                        woyouService.setAlignment(0, callback);
                        woyouService.setFontSize(30, callback);
                        woyouService.printColumnsText(new String[]{"Voucher No", ":", str_voucher_no}, new int[]{14, 1, 16}, new int[]{0, 0, 0}, callback);
                        woyouService.printColumnsText(new String[]{"Return Date", ":", returns.get_date()}, new int[]{12, 1, 20}, new int[]{0, 0, 0}, callback);
                        woyouService.printColumnsText(new String[]{Globals.PrintDeviceID, ":", Globals.objLPD.getDevice_Name()}, new int[]{10, 1, 20}, new int[]{0, 0, 0}, callback);
                        user = User.getUser(getApplicationContext(), " Where user_code='" + Globals.user + "'", database);
                        woyouService.printColumnsText(new String[]{Globals.PrintCashier, ":", user.get_name()}, new int[]{12, 1, 18}, new int[]{0, 0, 0}, callback);

                        Contact contact = Contact.getContact(getApplicationContext(), database, db, " WHERE contact_code='" + returns.get_contact_code() + "'");
                        woyouService.printColumnsText(new String[]{"Customer", ":", contact.get_name()}, new int[]{10, 1, 20}, new int[]{0, 0, 0}, callback);
                        if (contact.get_gstin().length() > 0) {
                            woyouService.printColumnsText(new String[]{"Customer GST No.", ":", contact.get_gstin()}, new int[]{10, 1, 20}, new int[]{0, 0, 0}, callback);
                        }

                        woyouService.printTextWithFont("--------------------------------\n", "", 24, callback);
                        woyouService.printTextWithFont("Item Name\n", "", 30, callback);
                        woyouService.printColumnsText(new String[]{"Qty", "Price", "Total"}, new int[]{7, 9, 15}, new int[]{0, 0, 0}, callback);
                        woyouService.setAlignment(0, callback);
                        woyouService.printTextWithFont("--------------------------------\n", "", 24, callback);
                        int count = 0;
                        while (count < return_details.size()) {
                            String strItemCode = return_details.get(count).get_item_code();
                            String strItemName = Return_detail.getItemNameReturn(getApplicationContext(), " WHERE Return_detail.item_Code  = '"
                                    + strItemCode + "'  GROUP By Return_detail.item_Code");
                            String sale_price;
                            Double dDisAfterSalePrice = 0d;

                            dDisAfterSalePrice = (((Double.parseDouble(return_details.get(count).get_line_total()) / Double.parseDouble(return_details.get(count).get_qty()))));
                            sale_price = Globals.myNumberFormat2Price(Double.parseDouble(dDisAfterSalePrice + ""), decimal_check);

                            String line_total;
                            line_total = Globals.myNumberFormat2Price(Double.parseDouble(return_details.get(count).get_line_total()), decimal_check);
                            woyouService.printTextWithFont(strItemName + "\n", "", 30, callback);
                            woyouService.printColumnsText(new String[]{Globals.myNumberFormat2QtyDecimal(Double.parseDouble(return_details.get(count).get_qty()), qty_decimal_check), sale_price, line_total}, new int[]{8, 8, 8}, new int[]{0, 0, 0}, callback);
                            count++;
                        }


                        woyouService.printTextWithFont("--------------------------------\n", "", 24, callback);
                        String net_amount;
                        net_amount = Globals.myNumberFormat2Price(Double.parseDouble(returns.get_total()), decimal_check);
                        String strCurrency;
                        if (Globals.objLPD.getCurreny_Symbol().equals("")) {
                            strCurrency = "";
                        } else {
                            strCurrency = "(" + Globals.objLPD.getCurreny_Symbol() + ")";
                        }
                        woyouService.printColumnsText(new String[]{"Net Amt", ":", net_amount + "" + strCurrency}, new int[]{10, 1, 20}, new int[]{0, 0, 0}, callback);

                        woyouService.setAlignment(1, callback);
                        woyouService.printTextWithFont(" \n", "", 24, callback);
                        if (!settings.get_Footer_Text().equals("")) {
                            woyouService.printTextWithFont(settings.get_Footer_Text(), "", 24, callback);
                            woyouService.printTextWithFont("\n", "", 24, callback);
                        }

                        woyouService.printTextWithFont("" + settings.get_Copy_Right() + "\n", "", 30, callback);
                        woyouService.printTextWithFont(" \n", "", 24, callback);
                        woyouService.printTextWithFont(" \n", "", 24, callback);
                        woyouService.printTextWithFont(" \n", "", 24, callback);
                    }
                    Globals.strContact_Code = "";
                    Globals.strResvContact_Code = "";
                    Globals.strOldCrAmt = "0";
                    Globals.setEmpty();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private class LongOperation extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            Boolean iswifi = CheckprinterConnection();
            return iswifi;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            if (result != null) {
                iswifi = result;
            }

        }

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(InvReturnFinalActivity.this);
            dialog.setCancelable(false);
            dialog.show();
        }

    }

    public Boolean CheckprinterConnection() {
        if (settings != null) {

            if (settings.getPrinterId().equals("2")) {
                String tmpStr = settings.getPrinterIp().trim();
                String ipAddress = "";
                String tmpPort = "";
                int port = 9100;
                String[] strings = Globals.StringSplit(tmpStr, ":");
                ipAddress = strings[0];
                tmpPort = strings[1];
                port = Integer.parseInt(tmpPort);
                if (!WifiPrintDriver.WIFISocket(ipAddress, port)) {
                    WifiPrintDriver.Close();
                    return false;
                } else {
                    if (WifiPrintDriver.IsNoConnection()) {
                        return false;
                    }
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}