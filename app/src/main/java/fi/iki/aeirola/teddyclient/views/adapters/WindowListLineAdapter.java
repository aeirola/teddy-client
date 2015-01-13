package fi.iki.aeirola.teddyclient.views.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import fi.iki.aeirola.teddyclient.R;
import fi.iki.aeirola.teddyclientlib.models.Window;

/**
 * Created by Axel on 7.12.2014.
 */
public class WindowListLineAdapter extends ResourceCursorAdapter {
    public WindowListLineAdapter(Context context, Cursor cursor) {
        super(context, R.layout.window_list_line, cursor, 0);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        swapCursor(cursor);

        String name = cursor.getString(cursor.getColumnIndex("name"));
        Window.Activity activity = Window.Activity.valueOf(cursor.getString(cursor.getColumnIndex("activity")));

        viewHolder.textView.setText(name, TextView.BufferType.NORMAL);
        switch (activity) {
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
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = super.newView(context, cursor, parent);

        ViewHolder viewHolder = new ViewHolder();
        viewHolder.textView = (TextView) view.findViewById(R.id.window_list_line_text);
        viewHolder.activityView = (TextView) view.findViewById(R.id.window_list_line_activity);
        view.setTag(viewHolder);

        return view;
    }

    private static class ViewHolder {
        TextView textView;
        TextView activityView;
    }
}
