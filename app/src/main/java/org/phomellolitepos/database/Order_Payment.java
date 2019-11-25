package org.phomellolitepos.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

/**
 * Created by pegasus-andorid-1 on 1/16/2017.
 */

public class Order_Payment {
    private static String tableName = "order_payment";
    private String order_payment_id;
    private String device_code;
    private String order_code;
    private String sr_no;
    private String pay_amount;
    private String payment_id;
    private String currency_id;
    private String currency_value;
    private String card_number;
    private String card_name;
    private String field1;
    private String field2;

    private Database db;
    private ContentValues value;

    public Order_Payment(Context context, String order_payment_id, String device_code, String order_code,
                         String sr_no, String pay_amount, String payment_id,
                         String currency_id, String currency_value,
                         String card_number, String card_name, String field1,String field2) {

        db = new Database(context);
        value = new ContentValues();

        this.set_order_payment_id(order_payment_id);
        this.set_device_code(device_code);
        this.set_order_code(order_code);
        this.set_sr_no(sr_no);
        this.set_pay_amount(pay_amount);
        this.set_payment_id(payment_id);
        this.set_currency_id(currency_id);
        this.set_currency_value(currency_value);
        this.set_card_number(card_number);
        this.set_card_name(card_name);
        this.set_field1(field1);
        this.set_field2(field2);
    }


    public String get_order_payment_id() {
        return order_payment_id;
    }

    public void set_order_payment_id(String order_payment_id) {
        this.order_payment_id = order_payment_id;
        value.put("order_payment_id", order_payment_id);
    }


    public String get_device_code() {
        return device_code;
    }

    public void set_device_code(String device_code) {
        this.device_code = device_code;
        value.put("device_code", device_code);
    }

    public String get_order_code() {
        return order_code;
    }

    public void set_order_code(String order_code) {
        this.order_code = order_code;
        value.put("order_code", order_code);
    }


    public String get_sr_no() {
        return sr_no;
    }

    public void set_sr_no(String sr_no) {
        this.sr_no = sr_no;
        value.put("sr_no", sr_no);
    }

    public String get_pay_amount() {
        return pay_amount;
    }

    public void set_pay_amount(String pay_amount) {
        this.pay_amount = pay_amount;
        value.put("pay_amount", pay_amount);
    }

    public String get_payment_id() {
        return payment_id;
    }

    public void set_payment_id(String payment_id) {
        this.payment_id = payment_id;
        value.put("payment_id", payment_id);
    }

    public String get_currency_id() {
        return currency_id;
    }

    public void set_currency_id(String currency_id) {
        this.currency_id = currency_id;
        value.put("currency_id", currency_id);
    }

    public String get_currency_value() {
        return currency_value;
    }

    public void set_currency_value(String currency_value) {
        this.currency_value = currency_value;
        value.put("currency_value", currency_value);
    }


    public String get_card_number() {
        return card_number;
    }

    public void set_card_number(String card_number) {
        this.card_number = card_number;
        value.put("card_number", card_number);
    }


    public String get_card_name() {
        return card_name;
    }

    public void set_card_name(String card_name) {
        this.card_name = card_name;
        value.put("card_name", card_name);
    }

    public String get_field1() {
        return field1;
    }

    public void set_field1(String field1) {
        this.field1 = field1;
        value.put("field1", field1);
    }

    public String get_field2() {
        return field2;
    }

    public void set_field2(String field2) {
        this.field2 = field2;
        value.put("field2", field2);
    }


    public long insertOrder_Payment(SQLiteDatabase database) {
        //SQLiteDatabase database = db.getWritableDatabase();
        long insert = database.insert(tableName, "order_payment_id", value);
        //database.close();
        return insert;
    }

    public long updateOrder_Payment(String whereClause, String[] whereArgs, SQLiteDatabase database)
            throws SQLiteConstraintException {
        //SQLiteDatabase sdb = db.getWritableDatabase();
        long insert = database.updateWithOnConflict(tableName, value, whereClause,
                whereArgs, SQLiteDatabase.CONFLICT_FAIL);
//        sdb.close();
        return insert;
    }

    public static long delete_order_payment(Context context, String tablename, String whereClause, String[] whereArgs, SQLiteDatabase database) {
//        Database db = new Database(context);
//        SQLiteDatabase sdb = db.getWritableDatabase();
        long delete = database.delete(tableName, whereClause, whereArgs);
//        sdb.close();
        return delete;
    }

    public static Order_Payment getOrder_Payment(Context context, String WhereClasue, SQLiteDatabase database) {
        String Query = "Select * FROM " + tableName + " " + WhereClasue;
        Order_Payment master = null;
//        Database db = new Database(context);
//        SQLiteDatabase database = db.getReadableDatabase();
        Cursor cursor = database.rawQuery(Query, null);
        if (cursor.moveToFirst()) {
            do {
                master = new Order_Payment(context, cursor.getString(0),
                        cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4),
                        cursor.getString(5), cursor.getString(6),
                        cursor.getString(7), cursor.getString(8),
                        cursor.getString(9), cursor.getString(10), cursor.getString(11));
            } while (cursor.moveToNext());
        }
        cursor.close();
//        database.close();
//        db.close();
        return master;
    }


    public static String getTotalCash(Context context, SQLiteDatabase database, String WhereClasue) {
        String Query = "Select SUM(pay_amount) FROM " + tableName + " " + WhereClasue;
        String master = null;
        Cursor cursor = database.rawQuery(Query, null);
        if (cursor.moveToFirst()) {
            do {

                master = cursor.getString(0);

            } while (cursor.moveToNext());
        }
        cursor.close();
        return master;
    }

    // Here Changed in function  need to update all classes
    public static ArrayList<Order_Payment> getAllOrder_Payment(Context context, String WhereClasue) {
        String Query = "Select * FROM " + tableName + " " + WhereClasue;
        ArrayList<Order_Payment> list = new ArrayList<Order_Payment>();
        Order_Payment master = null;
        Database db = new Database(context);
        SQLiteDatabase database = db.getReadableDatabase();
        Cursor cursor = database.rawQuery(Query, null);
        if (cursor.moveToFirst()) {
            do {
                master = new Order_Payment(context, cursor.getString(0),
                        cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4),
                        cursor.getString(5), cursor.getString(6),
                        cursor.getString(7), cursor.getString(8),
                        cursor.getString(9), cursor.getString(10), cursor.getString(11));
                list.add(master);
            } while (cursor.moveToNext());
        }
        cursor.close();
//        database.close();
//        db.close();
        return list;
    }


}
