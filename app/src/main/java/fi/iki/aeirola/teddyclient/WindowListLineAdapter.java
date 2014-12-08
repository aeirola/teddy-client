package fi.iki.aeirola.teddyclient;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import fi.iki.aeirola.teddyclientlib.models.Window;

/**
 * Created by Axel on 7.12.2014.
 */
public class WindowListLineAdapter extends ArrayAdapter<Window> {
    private final LayoutInflater mInflater;

    public WindowListLineAdapter(Context context, List<Window> objects) {
        super(context, R.layout.window_list_line, objects);
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Window window = this.getItem(position);

        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.window_list_line, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.textView = (TextView) convertView.findViewById(R.id.window_list_line_text);
            viewHolder.activityView = (TextView) convertView.findViewById(R.id.window_list_line_activity);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.textView.setText(window.toString(), TextView.BufferType.NORMAL);
        switch (window.activity) {
            case PASSIVE:
                viewHolder.activityView.setVisibility(View.VISIBLE);
                viewHolder.activityView.setTextColor(Color.GRAY);
                break;
            case ACTIVE:
                viewHolder.activityView.setVisibility(View.VISIBLE);
                viewHolder.activityView.setTextColor(Color.BLACK);
                break;
            case HILIGHT:
                viewHolder.activityView.setVisibility(View.VISIBLE);
                viewHolder.activityView.setTextColor(Color.RED);
                break;
            case INACTIVE:
            default:
                viewHolder.activityView.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView textView;
        TextView activityView;
    }
}
