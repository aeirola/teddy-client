package fi.iki.aeirola.teddyclient.views.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import fi.iki.aeirola.teddyclient.R;
import fi.iki.aeirola.teddyclient.utils.ColorMaps;

/**
 * Created by Axel on 6.12.2014.
 * <p/>
 * Special thanks to ailin-nemui (https://github.com/ailin-nemui) for providing irssi color handling code!
 */
public class IrssiLineAdapter extends ResourceCursorAdapter {
    private static final String TAG = IrssiLineAdapter.class.getName();

    public IrssiLineAdapter(Context context, Cursor cursor) {
        super(context, R.layout.window_detail_line, cursor, 0);
    }

    private static int ansiBitFlip(int x) {
        return ((x & 8) | (x & 4) >> 2 | (x & 2) | (x & 1) << 2);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        swapCursor(cursor);

        String message = cursor.getString(cursor.getColumnIndex("message"));
        viewHolder.textView.setText(this.buildSpannable(message, ColorMaps.ANSI_FG_COLOR_MAP), TextView.BufferType.EDITABLE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = super.newView(context, cursor, parent);

        ViewHolder viewHolder = new ViewHolder();
        viewHolder.textView = (TextView) view.findViewById(R.id.window_detail_line);
        view.setTag(viewHolder);

        return view;
    }

    private SpannableStringBuilder buildSpannable(String message, int[] colorMap) {
        SpannableStringBuilder mSpanned = new SpannableStringBuilder();

        boolean u = false, r = false, b = false, bl = false, f = false, i = false;
        int curFgColor = -1, curBgColor = -1;
        int len = message.length();
        for (int k = 0; k < len; ) {
            switch (message.charAt(k)) {
                case '\u001f':
                    u = !u;
                    k++;
                    continue;
                case '\u0016':
                    r = !r;
                    k++;
                    continue;
                case '\u0004':
                    k++;
                    if (k >= len) continue;
                    switch (message.charAt(k)) {
                        case 'a':
                            bl = !bl;
                            k++;
                            continue;
                        case 'c':
                            b = !b;
                            k++;
                            continue;
                        case 'i':
                            f = !f;
                            k++;
                            continue;
                        case 'f':
                            i = !i;
                            k++;
                            continue;
                        case 'g':
                            u = false;
                            r = false;
                            b = false;
                            bl = false;
                            f = false;
                            i = false;
                            curFgColor = -1;
                            curBgColor = -1;
                            k++;
                            continue;
                        case 'e':
                            /* prefix separator */
                            k++;
                            continue;
                    }
                    if (k + 2 >= len) {
                        k = len;
                        continue;
                    }
                    int color = -1;
                    boolean bg = false;
                    switch (message.charAt(k)) {
                        /* extended colour */
                        case '.':
                            bg = false;
                            color = 0x10 - 0x3f + message.charAt(k + 1);
                            break;
                        case '-':
                            bg = false;
                            color = 0x60 - 0x3f + message.charAt(k + 1);
                            break;
                        case ',':
                            bg = false;
                            color = 0xb0 - 0x3f + message.charAt(k + 1);
                            break;
                        case '+':
                            bg = true;
                            color = 0x10 - 0x3f + message.charAt(k + 1);
                            break;
                        case '\'':
                            bg = true;
                            color = 0x60 - 0x3f + message.charAt(k + 1);
                            break;
                        case '&':
                            bg = true;
                            color = 0xb0 - 0x3f + message.charAt(k + 1);
                            break;
                    }
                    if (color != -1) {
                        k += 2;
                        if (bg) {
                            curBgColor = colorMap[16 + color];
                        } else {
                            curFgColor = colorMap[16 + color];
                        }
                        continue;
                    }
                    if (message.charAt(k) == '#') {
                        /* html colour */
                        k++;
                        int rgbx[] = {0, 0, 0, 0};
                        if (k + 4 >= len) {
                            len = k;
                            continue;
                        }
                        for (int j = 0; j < 4; ++j, ++k) {
                            rgbx[j] = message.charAt(k);
                        }
                        rgbx[3] -= 0x20;
                        for (int j = 0; j < 3; ++j) {
                            if ((rgbx[3] & (0x10 << j)) != 0) {
                                rgbx[j] -= 0x20;
                            }
                        }
                        if ((rgbx[3] & 1) != 0) {
                            curBgColor = Color.argb(0, rgbx[0], rgbx[1], rgbx[2]);
                        } else {
                            curFgColor = Color.argb(0, rgbx[0], rgbx[1], rgbx[2]);
                        }

                        continue;
                    }
                    if (k + 1 >= len) {
                        k = len;
                        continue;
                    }
                    int colorBase = '0';
                    int colorBaseMax = '?';
                    if (message.charAt(k) >= colorBase && message.charAt(k) <= colorBaseMax) {
                        curFgColor = colorMap[ansiBitFlip(message.charAt(k) - colorBase)];
                    } else if (message.charAt(k) == '\u00ff') {
                        curFgColor = -1;
                    }
                    k++;
                    if (message.charAt(k) >= colorBase && message.charAt(k) <= colorBaseMax) {
                        curBgColor = colorMap[ansiBitFlip(message.charAt(k) - colorBase)];
                    } else if (message.charAt(k) == '\u00ff') {
                        curBgColor = -1;
                    }
                    k++;
                    continue;
            }
            int from = mSpanned.length();
            for (; k < len && message.charAt(k) != '\u0004'
                    && message.charAt(k) != '\u0016'
                    && message.charAt(k) != '\u001f'; ++k) {
                mSpanned.append(message.charAt(k));
            }

            int to = mSpanned.length();
            int fg;
            int bg;
            if (r) {
                fg = curBgColor;
                bg = curFgColor;
            } else {
                fg = curFgColor;
                bg = curBgColor;
            }

            if (fg != -1) {
                mSpanned.setSpan(new ForegroundColorSpan(0xff000000 | fg), from, to, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (bg != -1) {
                mSpanned.setSpan(new BackgroundColorSpan(0xff000000 | bg), from, to, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (b && i) {
                mSpanned.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), from, to, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (b) {
                mSpanned.setSpan(new StyleSpan(Typeface.BOLD), from, to, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (i) {
                mSpanned.setSpan(new StyleSpan(Typeface.ITALIC), from, to, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (u) {
                mSpanned.setSpan(new UnderlineSpan(), from, to, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        return mSpanned;
    }

    private static class ViewHolder {
        TextView textView;
    }
}
