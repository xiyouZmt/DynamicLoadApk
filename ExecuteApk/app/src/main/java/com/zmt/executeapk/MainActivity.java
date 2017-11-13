package com.zmt.executeapk;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * Created by zmt on 2017/8/19.
 */

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void initViews(){
        View view  = generateContentView(mProxyActivity);
        mProxyActivity.setContentView(view);
    }

    public View generateContentView(final Context context){
        LinearLayout layout = new LinearLayout(context);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        layout.setLayoutParams(layoutParams);
        layout.setBackgroundColor(Color.parseColor("#3f51b5"));

        Button button = new Button(context);
        button.setText("start");
        layout.addView(button, LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(context, "click button", Toast.LENGTH_SHORT).show();
                startActivityByProxy(TestActivity.TEST_ACTIVITY);
            }
        });
        return layout;
    }
}
