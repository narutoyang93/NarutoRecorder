package com.naruto.recorder.activity;

import android.content.Intent;
import android.os.Handler;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.naruto.recorder.R;
import com.naruto.recorder.SharedPreferencesHelper;
import com.naruto.recorder.adapter.FileListAdapter;
import com.naruto.recorder.base.DataBindingActivity;
import com.naruto.recorder.databinding.ActivityFileListBinding;
import com.naruto.recorder.databinding.DialogRenameBinding;
import com.naruto.recorder.databinding.DialogSortBinding;
import com.naruto.recorder.service.RecordService;
import com.naruto.recorder.utils.DialogFactory;
import com.naruto.recorder.utils.MyTool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Purpose 文件列表
 * @Author Naruto Yang
 * @CreateDate 2020/11/15 0015
 * @Note
 */
public class FileListActivity extends DataBindingActivity<ActivityFileListBinding> {
    private FileListAdapter adapter;
    private FileListAdapter.FileInfo waitingRenameItem;//等待重命名的文件
    private BottomSheetDialog renameDialog;//重命名弹窗
    private BottomSheetDialog sortDialog;//排序弹窗
    DialogRenameBinding renameBinding;
    DialogSortBinding sortBinding;

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_file_list;
    }

    @Override
    protected void init() {
        setTitleBarTitle("录音文件");
        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        adapter = new FileListAdapter(new FileListAdapter.ListListener() {
            boolean enable = true;//防止短时间内重复执行play

            @Override
            public void onItemSelectedChange() {
                int selectedCount = adapter.getSelectedItemSet().size();
                dataBinding.setSelectedCount(selectedCount);
                dataBinding.setTotalCount(adapter.getItemCount());
                if (adapter.getItemCount() == 0 && adapter.isEditMode()) {//删除后没有数据，退出编辑模式
                    dataBinding.ivClose.callOnClick();
                }
            }

            @Override
            public void OnItemLongPress() {
                enterEditMode(null);
            }

            @Override
            public void onSortFinish() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                        dataBinding.setTotalCount(adapter.getItemCount());
                        dismissProgressDialog();
                    }
                });
            }

            @Override
            public void onNoData() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {//获取文件时没有数据
                        dataBinding.setTotalCount(0);
                        dismissProgressDialog();
                    }
                });
            }

            @Override
            public void play(FileListAdapter.FileInfo fileInfo) {
/*                MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
                String mime = mimeTypeMap.getMimeTypeFromExtension(path.substring(path.lastIndexOf(".")));
                Uri uri = MyTool.getFileUri(FileListActivity.this, new File(path));
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(uri, mime);

                startActivity(intent);*/
                if (enable) {
                    enable = false;
                    //500毫秒内只能执行一次
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            enable = true;
                        }
                    }, 500);
                    PlayActivity.launch(FileListActivity.this, fileInfo);
                }
            }
        });
        showProgressDialog();
        recyclerView.setAdapter(adapter);
    }

    public void newRecord(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("tag", "start record");
        startActivity(intent);
    }

    public void enterEditMode(View view) {
        adapter.toggleEditMode();
        dataBinding.vsTitle.showNext();
        dataBinding.vsButton.showNext();
    }

    public void sort(View view) {
        if (sortDialog == null) createSortDialog();//创建弹窗

        sortDialog.show();
    }

    public void share(View view) {
        //获取文件uri
        List<File> files = new ArrayList<>();
        for (FileListAdapter.FileInfo f : adapter.getSelectedItemSet()) {
            files.add(new File(f.path));
        }

        MyTool.shareFile(this, files);
    }

    public void delete(View view) {
        String message = String.format("是否删除所选中的%d个录音？", dataBinding.getSelectedCount());
        DialogFactory.makeSimpleDialog(this, message, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapter.deleteFiles();
            }
        }).first.show();
    }

    public void rename(View view) {
        if (renameDialog == null) createRenameDialog();//创建弹窗
        //获取需要处理的数据
        for (FileListAdapter.FileInfo f : adapter.getSelectedItemSet()) {
            waitingRenameItem = f;
            renameBinding.setValue(waitingRenameItem.name);
            break;
        }
        renameDialog.show();
    }

    public void selectAll(View view) {
        adapter.toggleSelectAll();
    }

    public void quitEditMode(View view) {
        //退出编辑模式
        adapter.toggleEditMode();
        dataBinding.vsTitle.showPrevious();
        dataBinding.vsButton.showPrevious();
    }

    /**
     * 创建重命名弹窗
     */
    private void createRenameDialog() {
        //创建弹窗
        renameDialog = new BottomSheetDialog(this, R.style.dialog_soft_input);
        renameDialog.setCancelable(true);
        renameBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.dialog_rename, (ViewGroup) rootView, false);
        renameDialog.setContentView(renameBinding.getRoot());
        //设置点击事件
        renameBinding.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                renameDialog.dismiss();
            }
        });
        renameBinding.btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String v = renameBinding.getValue().trim();
                if (v.length() == 0) {
                    toast("未输入文件名");
                } else {
                    File file;
                    try {
                        file = new File(waitingRenameItem.path);
                        if (file.exists()) {
                            // TODO: 2020/11/16 0016 处理曾修改保存路径的情况
                            file.renameTo(new File(RecordService.getNewFilePath(v)));
                            waitingRenameItem.name = v;
                            waitingRenameItem.path = file.getAbsolutePath();
                            adapter.notifyItemChanged(adapter.getDataList().indexOf(waitingRenameItem), "name");
                        } else {
                            toast("文件不存在");
                            adapter.deleteFiles();
                        }

                    } catch (Exception e) {
                        toast("操作异常");
                        e.printStackTrace();
                    }
                    renameDialog.dismiss();
                }
            }
        });
    }


    /**
     * 创建排序弹窗
     */
    private void createSortDialog() {
        //创建弹窗
        sortDialog = new BottomSheetDialog(this, R.style.dialog_transparent);
        sortDialog.setCancelable(true);
        sortBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.dialog_sort, (ViewGroup) rootView, false);
        sortDialog.setContentView(sortBinding.getRoot());
        Pair<String, Boolean> sortType = SharedPreferencesHelper.getSortType();
        sortBinding.setHasSelectedTime(sortType.first.equals("time"));
        //设置点击事件
        sortBinding.btnAscending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sortDialog.dismiss();
                Pair<String, Boolean> pair = SharedPreferencesHelper.getSortType();
                if (sortBinding.getHasSelectedTime()) {
                    if (pair.first.equals("time") && !pair.second) return;
                    showProgressDialog();
                    adapter.sortByCreateTime(false);
                } else {
                    if (pair.first.equals("title") && !pair.second) return;
                    showProgressDialog();
                    adapter.sortByName(false);
                }
            }
        });
        sortBinding.btnDescending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sortDialog.dismiss();
                Pair<String, Boolean> pair = SharedPreferencesHelper.getSortType();
                if (sortBinding.getHasSelectedTime()) {
                    if (pair.first.equals("time") && pair.second) return;
                    showProgressDialog();
                    adapter.sortByCreateTime(true);
                } else {
                    if (pair.first.equals("title") && pair.second) return;
                    showProgressDialog();
                    adapter.sortByName(true);
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && adapter.isEditMode()) {
            dataBinding.ivClose.callOnClick();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}