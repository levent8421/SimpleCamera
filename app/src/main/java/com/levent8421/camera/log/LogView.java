package com.levent8421.camera.log;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

import org.apache.commons.lang3.time.FastDateFormat;

import java.util.Date;

/**
 * Create by levent8421 2020/10/16 0:39
 * Log View
 *
 * @author levent8421
 */
public class LogView extends AppCompatEditText {
    private final StringBuilder logContext = new StringBuilder();
    private final FastDateFormat dateFormat = FastDateFormat.getInstance("HH:mm:ss.SSS");

    public LogView(Context context) {
        super(context);
        init();
    }

    public LogView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LogView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        this.setEnabled(false);
    }

    public void log(String tag, String msg) {
        final String date = dateFormat.format(new Date());
        final String logLine = String.format("%s-%s:%s\r\n", date, tag, msg);
        this.logContext.append(logLine);
        this.setText(this.logContext);
        this.setSelection(this.logContext.length());
    }

    public void debug(String msg) {
        this.log("DEBUG", msg);
    }

    public void info(String msg) {
        this.log("INFO", msg);
    }

    public void error(String msg) {
        this.log("ERROR", msg);
    }
}
