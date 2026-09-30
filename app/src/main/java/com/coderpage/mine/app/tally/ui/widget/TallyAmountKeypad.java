package com.coderpage.mine.app.tally.ui.widget;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coderpage.mine.R;

/** The numeric keypad shared by the separate expense editors. */
public final class TallyAmountKeypad {
    private final Activity activity;
    private final TextView amountView;
    private final Typeface font;

    private TallyAmountKeypad(Activity activity, TextView amountView) {
        this.activity = activity;
        this.amountView = amountView;
        this.font = Typeface.createFromAsset(activity.getAssets(), "font/Quicksand-Medium.ttf");
    }

    public static void attach(Activity activity, LinearLayout keypad, TextView amountView,
                              Runnable onConfirm) {
        new TallyAmountKeypad(activity, amountView).build(keypad, onConfirm);
    }

    private void build(LinearLayout keypad, Runnable onConfirm) {
        int keyHeight = (int) activity.getResources().getDimension(R.dimen.tally_input_item_height);
        LinearLayout digits = new LinearLayout(activity);
        digits.setOrientation(LinearLayout.VERTICAL);
        String[][] rows = {{"1", "2", "3"}, {"4", "5", "6"},
                {"7", "8", "9"}, {activity.getString(R.string.num_clean_to_0), "0", "."}};
        for (String[] row : rows) {
            LinearLayout line = new LinearLayout(activity);
            for (String label : row) {
                TextView key = makeKey(label);
                key.setOnClickListener(v -> onKey(label));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, keyHeight, 1);
                params.setMargins(1, 1, 1, 1);
                line.addView(key, params);
            }
            digits.addView(line);
        }
        keypad.addView(digits, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 3));

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.VERTICAL);
        ImageView delete = new ImageView(activity);
        delete.setImageResource(R.drawable.ic_delete);
        delete.setContentDescription(activity.getString(R.string.delete));
        delete.setScaleType(ImageView.ScaleType.CENTER);
        delete.setBackgroundResource(R.drawable.bg_tally_num_input_item);
        delete.setOnClickListener(v -> {
            String current = amountView.getText().toString();
            amountView.setText(current.length() <= 1
                    ? "0" : current.substring(0, current.length() - 1));
        });
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, keyHeight);
        deleteParams.setMargins(1, 1, 1, 1);
        actions.addView(delete, deleteParams);
        TextView confirm = makeKey(activity.getString(R.string.ok));
        confirm.setOnClickListener(v -> onConfirm.run());
        LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, keyHeight * 3);
        confirmParams.setMargins(1, 1, 1, 1);
        actions.addView(confirm, confirmParams);
        keypad.addView(actions, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
    }

    private TextView makeKey(String label) {
        TextView key = new TextView(activity);
        key.setText(label);
        key.setTextAppearance(activity, R.style.TextAppearance_TallyNumInputItem);
        key.setTypeface(font);
        key.setGravity(Gravity.CENTER);
        key.setBackgroundResource(R.drawable.bg_tally_num_input_item);
        key.setClickable(true);
        return key;
    }

    private void onKey(String label) {
        String current = amountView.getText().toString();
        if (label.equals(activity.getString(R.string.num_clean_to_0))) {
            amountView.setText("0");
        } else if (".".equals(label)) {
            if (!current.contains(".")) amountView.setText(current + ".");
        } else if (!current.contains(".") || current.length() - current.indexOf('.') <= 2) {
            amountView.setText("0".equals(current) ? label : current + label);
        }
    }
}
