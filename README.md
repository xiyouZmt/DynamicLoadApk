我们都知道，在Android手机中，应用程序需要通过下载apk文件然后安装才能正常运行，如果说可以不用安装直接运行，那就很方便了。这种想法是可以实现的，我们可以在一个应用中去动态加载一个APK文件中的功能逻辑，这并不是在任何情况下都可以的，这对APK是有一定的要求的，也就是这种APK不仅可以通过手动安装来运行，**也要能够被其它的应用进行动态加载来运行**。当然这里的运行并不是它自身的运行，而是依托于一个现有的，可以正常运行的应用程序。这个应用程序相当于是一个代理，它除了有自身的功能逻辑外，**还要能够加载其它APK中的功能逻辑**。讲了这么多不知道大家有没有看明白呢，总结一下，实现这种情况有两点要求

1. **一个正常运行的应用程序，它除了有自身的功能外还要能够动态加载其它APK文件中的方法。**
2. **一个APK文件，它不仅要能够正常的安装运行，还要能被第一条对应的应用程序加载，执行APK文件中的方法逻辑。**

下面我们就这两点来具体实现下，这也就是Android中的动态加载机制

**一、创建宿主程序**

首先我们需要创建一个应用，在这里称它为宿主程序，这个应用功能很简单，只有两个界面，我们分别用MainActivity和ProxyActivity来表示。这里主要关注的是这个ProxyActivity，这个页面相当于是一个‘壳’，它提供一个Activity完整的声明周期以及交互逻辑，这个代理Activity只负责提供一个平台，其中的具体实现都是由另一个APK文件中的具体类来实现的。主要是通过类加载ClassLoader的方式去加载APK文件中的具体Class，然后再通过反射的方式调用Class中的方法并执行，这样APK中的Activity就被调起来了。这就是大致的实现方案，下面我们看下具体的实现

```java
private void launchTargetActivity(final String className){
    File dexDir = getDir("dex", 0);
    final String dexOutputPath = dexDir.getAbsolutePath();
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    DexClassLoader dexClassLoader = new DexClassLoader(dexPath, dexOutputPath, null, classLoader);

    try {
        Class<?> loadClass = dexClassLoader.loadClass(className);
        Constructor<?> constructor = loadClass.getConstructor();
        instance = constructor.newInstance();

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
```

代码其实不难理解，首先就是初始化ClassLoader，Activity就是通过它调用执行的，接着就是通过这个classLoader来创建一个实例对象，这个对象就是被调用的Activity实例，其中的方法都是要通过这个实例对象来调用的。接下来就是通过反射来调用具体的方法了，在这里有个小细节需要注意下，就是getMethod()方法和getDeclaredMethod()方法，getMethod只能获取到Class中的public修饰符的方法，而getDeclaredMethod()能获取到Class中声明的所有方法，代码中的注释也已经说明了。然后就是调用setProxy()方法设置代理对象和生命周期中的onCreate()方法。

**二、创建待执行APK**

在上面的代码中我们先是通过反射调用了setProxy()方法，这一步主要是让ProxyActivity成为待启动Activity的具体实例，因为APK中的Activity不能自己启动，所以这里通过setProxy()方法让ProxyActivity来接管APK中所有Activity的执行。基于这样的实现，我们需要写一个BaseActivity，在这里对ProxyActivity进行判断，如果有代理就让代理去处理相关逻辑，否则就正常执行该Activity。这样我们的APK既能够独立安装运行，又能够被其它程序动态加载。也就实现了我们之前的想法。下面是具体的实现。

**首先是BaseActivity**，这里的实现是一个基类，主要是做一些判断，看有没有ProxyActivity，然后对视图的添加和显示分别作了设置，最后实现了一个startActivityByProxy()方法，在这里启动Activity就不能直接用startActivity了，因为不能确定实例对象时代理对象还是自身，所以要在这里进行分析判断，对两种情况分别做出处理。下面是主要代码

```java
public void setProxy(Activity mProxyActivity) {
    this.mProxyActivity = mProxyActivity;
}

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

@Override
public void setContentView(View view) {
    if(mProxyActivity == this){
        super.setContentView(view);
    } else {
        mProxyActivity.setContentView(view);
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
    
```

接下来就是主界面的实现了，其实也很简单。

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initViews();
}

public void initViews(){
    View view  = generateContentView(mProxyActivity);
    mProxyActivity.setContentView(view);
}
```

代码确实很简单，就是调用父类onCreate然后初始化布局，generateContentView()方法是动态添加控件的。这样基本实现就完成了，并不复杂。

下面是界面截图

首先是宿主程序的运行图

![](https://github.com/xiyouZmt/DynamicLoadApk/blob/master/Images/DynamicLoad/Screenshot_2017-11-13-18-59-11-553_com.zmt.dynami.png)

![](https://github.com/xiyouZmt/DynamicLoadApk/blob/master/Images/DynamicLoad/Screenshot_2017-11-13-18-59-15-279_com.zmt.dynami.png)

![](https://github.com/xiyouZmt/DynamicLoadApk/blob/master/Images/DynamicLoad/Screenshot_2017-11-13-18-59-18-080_com.zmt.dynami.png)

然后是APK的运行图

![](https://github.com/xiyouZmt/DynamicLoadApk/blob/master/Images/ExecuteApk/Screenshot_2017-11-13-18-59-21-661_com.zmt.execut.png)

![](https://github.com/xiyouZmt/DynamicLoadApk/blob/master/Images/ExecuteApk/Screenshot_2017-11-13-18-59-24-975_com.zmt.execut.png)

可以看到，通过插件的形式启动和直接安装运行APK的效果是完全相同的，除了标题栏的名字，因为插件加载还是要依赖于宿主程序，所以插件启动后的标题栏还是宿主程序的。
以上应用的源码大家感兴趣的可以在github上下载，<a href="https://github.com/xiyouZmt/DynamicLoadApk"> DynamicLoadApk </a>，下载之余别忘了star fork一下下，mua...

**三、总结应用**

动态加载可以很方便的把应用中的各个功能模块化，在需要的时候再去加载，**这也就是插件化的思想**，除了这种方案外还可以通过自己封装H5容器以及React Native插件化，这些大家可以去多了解一下，这里就不再说了。除此之外插件化还能**实现简单的热修复效果**，也就是把插件放到服务器上，用户在需要的时候进行下载，当插件出现一些不可预知的错误时，先将本地的插件删除掉，然后在线上把修复后的插件部署好，接着把插件重新下载到客户端，这样就能大致实现热修复的功能，当然这里只是简单的说了一下，其中还有很多的问题需要处理，有兴趣可以自己实践一下。