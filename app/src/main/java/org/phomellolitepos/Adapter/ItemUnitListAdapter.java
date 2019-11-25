package org.phomellolitepos.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.phomellolitepos.R;
import org.phomellolitepos.database.Unit;

import java.util.ArrayList;


/**
 * Created by LENOVO on 8/30/2017.
 */

public class ItemUnitListAdapter extends BaseAdapter {
    Context context;
    LayoutInflater inflater;
    String result1;
    ArrayList<Unit> data;

    public ItemUnitListAdapter(Context context,
                               ArrayList<Unit> list) {
        this.context = context;
        data = list;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int position, View view, ViewGroup viewGroup) {
        TextView txt_unit_name, txt_unit_code;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View itemView = inflater.inflate(R.layout.unit_list_item,
                viewGroup, false);
        Unit resultp = data.get(position);
        txt_unit_name = (TextView) itemView.findViewById(R.id.txt_unit_name);
        txt_unit_code = (TextView) itemView.findViewById(R.id.txt_unit_code);
        txt_unit_code.setVisibility(View.GONE);
        txt_unit_name.setText(resultp.get_name());
        txt_unit_code.setText(resultp.get_code());

        return itemView;
    }
}
