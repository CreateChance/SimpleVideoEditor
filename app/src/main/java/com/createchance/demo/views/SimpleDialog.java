package com.createchance.demo.views;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.createchance.demo.R;

/**
 * Custom dialog
 *
 * @author gaochao1-iri
 * @since 21/08/2017
 */
public class SimpleDialog extends Dialog {
    private Builder mBuilder;

    private SimpleDialog(Builder builder) {
        super(builder.context);
        this.mBuilder = builder;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dialog_simple);

        setCanceledOnTouchOutside(mBuilder.cancelTouchout);

        ((TextView) findViewById(R.id.tv_content)).setText(mBuilder.contentText);
        ((TextView) findViewById(R.id.tv_cancel)).setText(mBuilder.cancelText);
        ((TextView) findViewById(R.id.tv_confirm)).setText(mBuilder.confirmText);

        if (mBuilder.cancelBtnOnClickListener != null) {
            findViewById(R.id.tv_cancel).setOnClickListener(mBuilder.cancelBtnOnClickListener);
        } else {
            // 默认关闭
            findViewById(R.id.tv_cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismiss();
                }
            });
        }

        if (mBuilder.confirmBtnOnClickListener != null) {
            findViewById(R.id.tv_confirm).setOnClickListener(mBuilder.confirmBtnOnClickListener);
        } else {
            // 默认关闭
            findViewById(R.id.tv_confirm).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismiss();
                }
            });
        }
    }

    public static final class Builder {
        private Context context;
        private boolean cancelTouchout;
        private View.OnClickListener cancelBtnOnClickListener;
        private View.OnClickListener confirmBtnOnClickListener;
        private String contentText;
        private String cancelText;
        private String confirmText;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setCancelTouchout(boolean cancelTouchout) {
            this.cancelTouchout = cancelTouchout;
            return this;
        }

        public Builder setContentText(String text) {
            this.contentText = text;
            return this;
        }

        public Builder setContentText(int text) {
            this.contentText = context.getString(text);
            return this;
        }

        public Builder setCancelText(String text) {
            this.cancelText = text;
            return this;
        }

        public Builder setCancelText(int text) {
            this.cancelText = context.getString(text);
            return this;
        }

        public Builder setConfirmText(String text) {
            this.confirmText = text;
            return this;
        }

        public Builder setConfirmText(int text) {
            this.confirmText = context.getString(text);
            return this;
        }

        public Builder setCancelOnClickListener(View.OnClickListener listener) {
            cancelBtnOnClickListener = listener;
            return this;
        }

        public Builder setConfirmOnClickListener(View.OnClickListener listener) {
            confirmBtnOnClickListener = listener;
            return this;
        }

        public SimpleDialog build() {
            return new SimpleDialog(this);
        }
    }
}
