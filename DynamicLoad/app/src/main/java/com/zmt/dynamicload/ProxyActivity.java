package com.zmt.dynamicload;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import dalvik.system.DexClassLoader;

/**
 * Created by zmt on 2017/8/19.
 */

public class ProxyActivity extends AppCompatActivity {

    public static final String EXTRA_FROM = "extra.from";
    public static final int EXTRA_EXTERNAL = 0;

    public static final String EXTRA_DEX_PATH = "extra.dex.path";
    public static final String EXTRA_CLASS = "extra.class";

    private Map<String, Method> lifeCycleMethod = new HashMap<>();

    private AssetManager mAssetManager;
    private Resources mResources;
    private Object instance;

    private String dexPath;
    private String mClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dexPath = getIntent().getStringExtra(EXTRA_DEX_PATH);
        mClass = getIntent().getStringExtra(EXTRA_CLASS);
        if(mClass == null){
            launchTargetActivity();
        } else {
            launchTargetActivity(mClass);
        }
    }

    @Override
    protected void onStart() {
        Method method = lifeCycleMethod.get("onStart");
        if(instance != null){
            try {
                method.invoke(instance);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        super.onStart();
    }

    @Override
    protected void onResume() {
        Method method = lifeCycleMethod.get("onResume");
        if(instance != null){
            try {
                method.invoke(instance);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        super.onResume();
    }

    private void launchTargetActivity(){
        PackageInfo packageInfo = getPackageManager().getPackageArchiveInfo(dexPath, PackageManager.GET_ACTIVITIES);
        if(packageInfo != null){
            if(packageInfo.activities != null && packageInfo.activities.length >= 0){
                mClass = packageInfo.activities[0].name;
                launchTargetActivity(mClass);
            }
        } else {
            Toast.makeText(this, "无法获取插件信息", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchTargetActivity(final String className){
        File dexDir = getDir("dex", 0);
        final String dexOutputPath = dexDir.getAbsolutePath();
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        DexClassLoader dexClassLoader = new DexClassLoader(dexPath, dexOutputPath, null, classLoader);

        try {
            Class<?> loadClass = dexClassLoader.loadClass(className);
            Constructor<?> constructor = loadClass.getConstructor();
            instance = constructor.newInstance();
            initLifeCycle(loadClass);

            /**
             * getMethod() Returns a Method object that reflects the specified public member method
             * of the class or interface represented by this Class object.
             */
            Method setProxy = loadClass.getMethod("setProxy", Activity.class);
            setProxy.setAccessible(true);
            setProxy.invoke(instance, this);

            /**
             * getDeclaredMethod() Returns a Method object that reflects the specified declared method
             * of the class or interface represented by this Class object.
             */
            Method onCreate = loadClass.getDeclaredMethod("onCreate", Bundle.class);
            onCreate.setAccessible(true);
            Bundle bundle = new Bundle();
            bundle.putInt(EXTRA_FROM, EXTRA_EXTERNAL);
            onCreate.invoke(instance, bundle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void laodResources(){
        try {
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
            addAssetPath.invoke(assetManager, dexPath);
            mAssetManager = assetManager;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        Resources superResources = super.getResources();
        mResources = new Resources(mAssetManager, superResources.getDisplayMetrics(), superResources.getConfiguration());

        Resources.Theme theme = mResources.newTheme();
        theme.setTo(super.getTheme());
    }

    protected void initLifeCycle(Class<?> loadClass){

        String [] methods = {
                "onCreate",
                "onStart",
                "onResume",
                "onPause",
                "onStop",
                "onDestroy",
                "onRestart",
                "onActivityResult"
        };
        for (String methodName : methods) {
            try {
                Method method;
                switch (methodName) {
                    case "onCreate":
                        method = loadClass.getDeclaredMethod("onCreate", Bundle.class);
                        break;
                    case "onActivityResult":
                        method = loadClass.getDeclaredMethod("onActivityResult", int.class, int.class, Intent.class);
                        break;
                    default:
                        method = loadClass.getDeclaredMethod(methodName);
                        break;
                }
                method.setAccessible(true);
                lifeCycleMethod.put(methodName, method);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public AssetManager getAssets() {
        return mAssetManager == null ? super.getAssets() : mAssetManager;
    }

    @Override
    public Resources getResources() {
        return mResources == null ? super.getResources() : mResources;
    }
}
