package com.zmt.executeapk;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

public class BaseActivity extends AppCompatActivity {

    public static final String FROM = "extra.from";
    public static final int FROM_EXTERNAL = 0;
    public static final int FROM_INTERNAL = 1;

    public static final String PROXY_VIEW_ACTION = "com.zmt.dynamicload.proxy.activity.VIEW";

    public static final String EXTRA_DEX_PATH = "extra.dex.path";
    public static final String DEX_PATH = "/storage/emulated/0/download/plugin.apk";
    public static final String EXTRA_CLASS = "extra.class";

    protected Activity mProxyActivity;
    private int mFrom = FROM_INTERNAL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(savedInstanceState != null){
            mFrom = savedInstanceState.getInt(FROM, FROM_INTERNAL);
        }

        if(mFrom == FROM_INTERNAL){
            super.onCreate(savedInstanceState);
            mProxyActivity = this;
        }
    }

    protected void startActivityByProxy(String className){
        if(mProxyActivity == this){
            Intent intent = new Intent();
            intent.setClassName(this, className);
            this.startActivity(intent);
        } else {
            Intent intent = new Intent(PROXY_VIEW_ACTION);
            intent.putExtra(EXTRA_DEX_PATH, DEX_PATH);
            intent.putExtra(EXTRA_CLASS, className);
            mProxyActivity.startActivity(intent);
        }
    }

    @Override
    public void setContentView(View view) {
        if(mProxyActivity == this){
            super.setContentView(view);
        } else {
            mProxyActivity.setContentView(view);
        }
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        if(mProxyActivity == this){
            super.setContentView(layoutResID);
        } else {
            mProxyActivity.setContentView(layoutResID);
        }
    }

    @Deprecated
    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        if(mProxyActivity == this){
            super.setContentView(view, params);
        } else {
            mProxyActivity.setContentView(view, params);
        }
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        if(mProxyActivity == this){
            super.addContentView(view, params);
        } else {
            mProxyActivity.addContentView(view, params);
        }
    }

    public void setProxy(Activity mProxyActivity) {
        this.mProxyActivity = mProxyActivity;
    }
}
