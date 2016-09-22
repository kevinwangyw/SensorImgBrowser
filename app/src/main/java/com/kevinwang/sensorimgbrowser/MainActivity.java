package com.kevinwang.sensorimgbrowser;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public static final int CODE_FOR_READ_PERMISSION = 1;
    private HashMap<String, List<String>> folderMap = new HashMap<String, List<String>>();
    private List<ImageBean> folderList = new ArrayList<ImageBean>();
    private final static int SCAN_OK = 1;
    private ProgressDialog mProgressDialog;
    private GridView mFolderCoverGrid;
    private ScanImgHandler mScanImgHandler;
    private FolderCoverAdapter mFolderCoverAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mScanImgHandler = new ScanImgHandler(this);

        mFolderCoverGrid = (GridView)findViewById(R.id.cover_gridview);
        mFolderCoverAdapter = new FolderCoverAdapter(this);

        mFolderCoverGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ArrayList<String> fileInFolder = (ArrayList)folderMap.get(folderList.get(i).getFolderName());

                Intent intent = new Intent(MainActivity.this, ImageActivity.class);
                intent.putStringArrayListExtra("images", fileInFolder);
                startActivity(intent);
            }
        });

        int hasReadPermission = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            hasReadPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            Log.i("查看权限","hasReadPerssion = " + hasReadPermission);
            if (hasReadPermission != PackageManager.PERMISSION_GRANTED) {
                Log.i("MainActivity", "申请权限");
                MainActivity activty= this;
                ActivityCompat.requestPermissions(activty,new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        CODE_FOR_READ_PERMISSION);
                return;
            }
        } else {
            mProgressDialog = ProgressDialog.show(MainActivity.this, null, "扫描图片....", true);
            getImages();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[]
            grantResults) {
        if (requestCode == CODE_FOR_READ_PERMISSION){
            if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    &&grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //用户同意使用write
                Log.i("申请权限结果", "getImages()");
                getImages();
            }else{
                //用户不同意，自行处理即可
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * 利用ContentProvider扫描手机中的图片，此方法在运行在子线程中
     */
    private void getImages() {
        //扫描手机中的图片，显示进度条

        new Thread(new Runnable() {
            @Override
            public void run() {

                Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver contentResolver = MainActivity.this.getContentResolver();

                //只查询jpeg和png格式图片
                Cursor cursor = contentResolver.query(mImageUri, null, MediaStore.Images.Media.MIME_TYPE + "=? or "
                        + MediaStore.Images.Media.MIME_TYPE + "=?", new String[]{"image/jpg", "image/png"}, MediaStore.Images.Media.DATE_MODIFIED);

                if (cursor == null) {
                    return;
                }

                Log.i("getImages", "cursor != null");

                cursor.moveToFirst();

                while (cursor.moveToNext()) {
                    //获取图片的路径
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));

                    //获取图片的父路径名称
                    String parentPath = new File(path).getParentFile().getName();

                    if (folderMap.containsKey(parentPath)) {
                        folderMap.get(parentPath).add(path);
                    } else {
                        List<String> list = new ArrayList<String>();
                        list.add(path);
                        folderMap.put(parentPath, list);
                    }
                }

                Iterator<Map.Entry<String, List<String>>> it = folderMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, List<String>> entry = it.next();
                    ImageBean imageBean = new ImageBean();

                    imageBean.setFolderName(entry.getKey());
                    imageBean.setImageCounts(entry.getValue().size());
                    imageBean.setTopImagePath(entry.getValue().get(0));

                    folderList.add(imageBean);
                }

                mScanImgHandler.sendEmptyMessage(SCAN_OK);
                cursor.close();
            }
        }).start();
    }

    /*
    * handler不一定需要static,只是一般全局需要一个hanlder就可以，
    * 所以习惯性的会写成static的，这样在别的activity里面也可以使用这个hanlder
    */
    static class ScanImgHandler extends Handler {
        WeakReference<MainActivity> mWeakReference;

        public ScanImgHandler(MainActivity activity) {
            mWeakReference = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final MainActivity activity = mWeakReference.get();
            if (activity != null) {
                switch (msg.what) {
                    case SCAN_OK:
                        Log.i("ScanImgHandler", "SCAN_OK");
                        activity.mProgressDialog.dismiss();
                        activity.mFolderCoverGrid.setAdapter(activity.mFolderCoverAdapter);
                        break;
                }
            }
        }
    }

    class FolderCoverAdapter extends BaseAdapter {
        private Context mContext;
        private ViewHolder mViewHolder;
        private final LayoutInflater mLayoutInflater;

        public FolderCoverAdapter(Context context) {
            mContext = context;
            mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mViewHolder = new ViewHolder();
        }

        @Override
        public int getCount() {
            return folderList.size();
        }

        @Override
        public Object getItem(int i) {
            return folderList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = mLayoutInflater.inflate(R.layout.item_gridview, null);
                mViewHolder.coverImg = (ImageView)view.findViewById(R.id.folder_cover);
                mViewHolder.folderName = (TextView)view.findViewById(R.id.folder_name);
                view.setTag(mViewHolder);
            } else {
                mViewHolder = (ViewHolder) view.getTag();
            }
            Bitmap bmp= BitmapFactory.decodeFile(folderList.get(i).getTopImagePath());
            mViewHolder.coverImg.setImageBitmap(bmp);
            mViewHolder.folderName.setText(folderList.get(i).getFolderName());
            return view;
        }

        class ViewHolder {
            TextView folderName;
            ImageView coverImg;
        }
    }
}
