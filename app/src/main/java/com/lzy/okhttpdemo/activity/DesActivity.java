package com.lzy.okhttpdemo.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.lzy.downloadmanager.DownloadInfo;
import com.lzy.downloadmanager.DownloadListener;
import com.lzy.downloadmanager.DownloadManager;
import com.lzy.okhttpdemo.Bean.ApkInfo;
import com.lzy.okhttpdemo.R;

import java.util.ArrayList;

public class DesActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView downloadSize;
    private TextView tvProgress;
    private TextView netSpeed;
    private ProgressBar pbProgress;
    private Button download;
    private Button remove;
    private Button restart;
    private MyListener listener;
    private DownloadInfo downloadInfo;
    private ApkInfo apk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_des);
        apk = (ApkInfo) getIntent().getSerializableExtra("apk");

        ImageView icon = (ImageView) findViewById(R.id.icon);
        TextView name = (TextView) findViewById(R.id.name);
        downloadSize = (TextView) findViewById(R.id.downloadSize);
        tvProgress = (TextView) findViewById(R.id.tvProgress);
        netSpeed = (TextView) findViewById(R.id.netSpeed);
        pbProgress = (ProgressBar) findViewById(R.id.pbProgress);
        download = (Button) findViewById(R.id.start);
        remove = (Button) findViewById(R.id.remove);
        restart = (Button) findViewById(R.id.restart);

        Glide.with(this).load(apk.getIconUrl()).error(R.mipmap.ic_launcher).into(icon);
        name.setText(apk.getName());
        download.setOnClickListener(this);
        remove.setOnClickListener(this);
        restart.setOnClickListener(this);
        listener = new MyListener();

        downloadInfo = DownloadManager.getInstance(this).getTaskByUrl(apk.getUrl());
        if (downloadInfo != null) {
            downloadInfo.addListener(listener);
            //需要第一次手动刷一次，因为任务可能处于下载完成，暂停，等待状态，此时是不会回调进度方法的
            refreshUi(downloadInfo);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (downloadInfo != null) downloadInfo.removeListener(listener);
    }

    @Override
    public void onClick(View v) {
        //每次点击的时候，需要更新当前对象
        downloadInfo = DownloadManager.getInstance(this).getTaskByUrl(apk.getUrl());
        if (v.getId() == download.getId()) {
            if (downloadInfo == null) {
                DownloadManager.getInstance(this).addTask(apk.getUrl(), listener);
                downloadInfo = DownloadManager.getInstance(this).getTaskByUrl(apk.getUrl());
                refreshButton(download, downloadInfo);
                return;
            }
            switch (downloadInfo.getState()) {
                case DownloadManager.PAUSE:
                case DownloadManager.NONE:
                case DownloadManager.ERROR:
                    DownloadManager.getInstance(this).addTask(downloadInfo.getUrl());
                    break;
                case DownloadManager.DOWNLOADING:
                    DownloadManager.getInstance(this).pause(downloadInfo.getUrl());
                    break;
                case DownloadManager.FINISH:
                    DownloadManager.getInstance(this).restartTask(downloadInfo.getUrl());
                    break;
            }
            refreshButton(download, downloadInfo);
        } else if (v.getId() == remove.getId()) {
            if (downloadInfo == null) {
                Toast.makeText(this, "请先下载任务", Toast.LENGTH_SHORT).show();
                return;
            }
            DownloadManager.getInstance(this).removeTask(downloadInfo.getUrl());
            downloadSize.setText("--M/--M");
            netSpeed.setText("---/s");
            tvProgress.setText("--.--%");
            pbProgress.setProgress(0);
            download.setText("下载");
        } else if (v.getId() == restart.getId()) {
            if (downloadInfo == null) {
                Toast.makeText(this, "请先下载任务", Toast.LENGTH_SHORT).show();
                return;
            }
            DownloadManager.getInstance(this).restartTask(downloadInfo.getUrl());
        }
    }

    private class MyListener extends DownloadListener {

        @Override
        public void onProgress(DownloadInfo downloadInfo) {
            refreshUi(downloadInfo);
        }

        @Override
        public void onFinish(DownloadInfo downloadInfo) {
            System.out.println("onFinish");
            Toast.makeText(DesActivity.this, "下载完成:" + downloadInfo.getTargetPath(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(DownloadInfo downloadInfo, String errorMsg, Exception e) {
            System.out.println("onError");
            if (errorMsg != null)
                Toast.makeText(DesActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshUi(DownloadInfo downloadInfo) {
        String downloadLength = Formatter.formatFileSize(DesActivity.this, downloadInfo.getDownloadLength());
        String totalLength = Formatter.formatFileSize(DesActivity.this, downloadInfo.getTotalLength());
        downloadSize.setText(downloadLength + "/" + totalLength);
        String networkSpeed = Formatter.formatFileSize(DesActivity.this, downloadInfo.getNetworkSpeed());
        netSpeed.setText(networkSpeed + "/s");
        tvProgress.setText((Math.round(downloadInfo.getProgress() * 10000) * 1.0f / 100) + "%");
        pbProgress.setMax((int) downloadInfo.getTotalLength());
        pbProgress.setProgress((int) downloadInfo.getDownloadLength());
        refreshButton(download, downloadInfo);
    }

    public Button refreshButton(Button download, DownloadInfo downloadInfo) {
        switch (downloadInfo.getState()) {
            case DownloadManager.NONE:
                download.setText("下载");
                break;
            case DownloadManager.DOWNLOADING:
                download.setText("暂停");
                break;
            case DownloadManager.PAUSE:
                download.setText("继续");
                break;
            case DownloadManager.WAITING:
                download.setText("等待");
                break;
            case DownloadManager.ERROR:
                download.setText("出错");
                break;
            case DownloadManager.FINISH:
                download.setText("安装");
                break;
        }
        return download;
    }
}