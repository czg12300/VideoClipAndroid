package com.jake.ffmpegandroid.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;


public class ContainerActivity extends AppCompatActivity {
    private boolean isShowAnim = true;
    private Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
        isShowAnim = getIntent().getBooleanExtra("is_show_anim", true);
        if (isShowAnim) {
//            overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
        }
        String fragmentName = getIntent().getStringExtra("fragment_name");
        try {
            fragment = (Fragment) Class.forName(fragmentName).newInstance();
            fragment.setArguments(getIntent().getBundleExtra("fragment_bundle"));
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if ((fragment instanceof BaseFragment) && ((BaseFragment) fragment).isFullScreen()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setId(frameLayout.hashCode());
        setContentView(frameLayout);
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction().replace(frameLayout.getId(), fragment).commitAllowingStateLoss();
        }
    }

    @Override
    public void onBackPressed() {
        boolean notNeedSuper = fragment instanceof BaseFragment && ((BaseFragment) fragment).onBackPressed();
        if (!notNeedSuper) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (isShowAnim) {
//            overridePendingTransition(R.anim.slide_left_in, R.anim.slide_right_out);
        }
    }

    public static void startForResult(Context context, String fragmentName, Bundle bundle, int requestCode) {
        startForResult(context, fragmentName, bundle, true, requestCode);
    }

    public static void startForResult(Context context, String fragmentName, Bundle bundle, boolean isShowAnim, int requestCode) {
        if (context != null && context instanceof Activity) {
            Intent it = new Intent();
            it.setClass(context, ContainerActivity.class);
            it.putExtra("fragment_name", fragmentName);
            it.putExtra("is_show_anim", isShowAnim);
            it.putExtra("fragment_bundle", bundle);
            ((Activity) context).startActivityForResult(it, requestCode);
        }
    }

    public static void start(Context context, String fragmentName, Bundle bundle) {
        start(context, fragmentName, bundle, true);
    }

    public static void start(Context context, String fragmentName, Bundle bundle, boolean isShowAnim) {
        LogUtil.d("fragmentName:" + fragmentName);
        if (context != null) {
            Intent it = new Intent();
            it.setClass(context, ContainerActivity.class);
            if (!(context instanceof Activity)) {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            it.putExtra("fragment_name", fragmentName);
            it.putExtra("is_show_anim", isShowAnim);
            it.putExtra("fragment_bundle", bundle);
            context.startActivity(it);
        }


    }

}
