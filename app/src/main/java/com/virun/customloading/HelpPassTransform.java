package com.virun.customloading;

import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;

import android.text.Editable;
import android.text.GetChars;
import android.text.NoCopySpan;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TransformationMethod;
import android.text.style.UpdateLayout;
import android.view.View;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

public class HelpPassTransform implements TransformationMethod, TextWatcher {
    public static final int DOT  = 0;
    public static final int STAR = 1;

    private static final Object ACTIVE = new Concrete();
    private static char mSymbol;
    private static HelpPassTransform sInstance;

    @IntDef(value = {
            DOT,
            STAR
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface Symbol {}

    private HelpPassTransform(@Symbol int symbol) {
        switch (symbol) {
            case DOT:
                mSymbol = '\u25CF';
                break;

            case STAR:
                mSymbol = '\u002A';
                break;
        }
    }

    public static HelpPassTransform getInstance(@Symbol int symbol) {
        if (sInstance != null)
            return sInstance;

        sInstance = new HelpPassTransform(symbol);
        return sInstance;
    }

    private static void removeVisibleSpans(Spannable sp) {
        Visible[] old = sp.getSpans(0, sp.length(), Visible.class);
        for (Visible anOld : old) {
            sp.removeSpan(anOld);
        }
    }

    public CharSequence getTransformation(CharSequence source, View view) {
        if (source instanceof Spannable) {
            Spannable sp = (Spannable) source;

            /*
             * Remove any references to other views that may still be
             * attached.  This will happen when you flip the screen
             * while a password field is showing; there will still
             * be references to the old EditText in the text.
             */
            ViewReference[] vr = sp.getSpans(0, sp.length(),
                    ViewReference.class);
            for (ViewReference aVr : vr) {
                sp.removeSpan(aVr);
            }

            removeVisibleSpans(sp);

            sp.setSpan(new ViewReference(view), 0, 0,
                    Spannable.SPAN_POINT_POINT);
        }

        return new PasswordCharSequence(source);
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // This callback isn't used.
    }

    public void onTextChanged(CharSequence s, int start,
                              int before, int count) {
        if (s instanceof Spannable) {
            Spannable sp = (Spannable) s;
            ViewReference[] vr = sp.getSpans(0, s.length(),
                    ViewReference.class);
            if (vr.length == 0) {
                return;
            }

            /*
             * There should generally only be one ViewReference in the text,
             * but make sure to look through all of them if necessary in case
             * something strange is going on.  (We might still end up with
             * multiple ViewReferences if someone moves text from one password
             * field to another.)
             */
            View v = null;
            for (int i = 0; v == null && i < vr.length; i++) {
                v = vr[i].get();
            }

            if (v == null) {
                return;
            }
            removeVisibleSpans(sp);
            sp.setSpan(new Visible(sp, this), start, start + count,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    public void afterTextChanged(Editable s) {
        // This callback isn't used.
    }

    public void onFocusChanged(View view, CharSequence sourceText,
                               boolean focused, int direction,
                               Rect previouslyFocusedRect) {
        if (!focused) {
            if (sourceText instanceof Spannable) {
                Spannable sp = (Spannable) sourceText;

                removeVisibleSpans(sp);
            }
        }
    }

    private static class PasswordCharSequence implements CharSequence, GetChars {
        private CharSequence mSource;

        private PasswordCharSequence(CharSequence source) {
            mSource = source;
        }

        public int length() {
            return mSource.length();
        }

        public char charAt(int i) {
            if (mSource instanceof Spanned) {
                Spanned sp = (Spanned) mSource;

                int st = sp.getSpanStart(ACTIVE);
                int en = sp.getSpanEnd(ACTIVE);

                if (i >= st && i < en) {
                    return mSource.charAt(i);
                }

                Visible[] visible = sp.getSpans(0, sp.length(), Visible.class);

                for (Visible aVisible : visible) {
                    if (sp.getSpanStart(aVisible.mTransformer) >= 0) {
                        st = sp.getSpanStart(aVisible);
                        en = sp.getSpanEnd(aVisible);

                        if (i >= st && i < en) {
                            return mSource.charAt(i);
                        }
                    }
                }
            }

            return mSymbol;
        }

        public CharSequence subSequence(int start, int end) {
            char[] buf = new char[end - start];

            getChars(start, end, buf, 0);
            return new String(buf);
        }

        @NonNull
        public String toString() {
            return subSequence(0, length()).toString();
        }

        public void getChars(int start, int end, char[] dest, int off) {
            TextUtils.getChars(mSource, start, end, dest, off);

            int st = -1, en = -1;
            int nvisible = 0;
            int[] starts = null, ends = null;

            if (mSource instanceof Spanned) {
                Spanned sp = (Spanned) mSource;

                st = sp.getSpanStart(ACTIVE);
                en = sp.getSpanEnd(ACTIVE);

                Visible[] visible = sp.getSpans(0, sp.length(), Visible.class);
                nvisible = visible.length;
                starts = new int[nvisible];
                ends = new int[nvisible];

                for (int i = 0; i < nvisible; i++) {
                    if (sp.getSpanStart(visible[i].mTransformer) >= 0) {
                        starts[i] = sp.getSpanStart(visible[i]);
                        ends[i] = sp.getSpanEnd(visible[i]);
                    }
                }
            }

            for (int i = start; i < end; i++) {
                if (!(i >= st && i < en)) {
                    boolean visible = false;

                    for (int a = 0; a < nvisible; a++) {
                        if (i >= starts[a] && i < ends[a]) {
                            visible = true;
                            break;
                        }
                    }

                    if (!visible) {
                        dest[i - start + off] = mSymbol;
                    }
                }
            }
        }
    }

    private static class Visible extends Handler implements UpdateLayout, Runnable {
        private Spannable mText;
        private HelpPassTransform mTransformer;

        private Visible(Spannable sp, HelpPassTransform ptm) {
            mText = sp;
            mTransformer = ptm;
            postAtTime(this, SystemClock.uptimeMillis() + 1000);
        }

        public void run() {
            mText.removeSpan(this);
        }
    }

    /**
     * Used to stash a reference back to the View in the Editable so we
     * can use it to check the settings.
     */
    private static class ViewReference extends WeakReference<View>
            implements NoCopySpan {
        private ViewReference(View v) {
            super(v);
        }
    }
}