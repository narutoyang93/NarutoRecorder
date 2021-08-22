package com.naruto.recorder.activity;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.naruto.recorder.Config;
import com.naruto.recorder.R;
import com.naruto.recorder.SharedPreferencesHelper;
import com.naruto.recorder.adapter.FileListAdapter;
import com.naruto.recorder.base.DataBindingActivity;
import com.naruto.recorder.databinding.ActivityFileListBinding;
import com.naruto.recorder.databinding.DialogRenameBinding;
import com.naruto.recorder.databinding.DialogSortBinding;
import com.naruto.recorder.service.RecordService;
import com.naruto.recorder.utils.DialogFactory;
import com.naruto.recorder.utils.FileUtil;
import com.naruto.recorder.utils.MyTool;

import java.util.ArrayList;

/**
 * @Purpose 文件列表
 * @Author Naruto Yang
 * @CreateDate 2020/11/15 0015
 * @Note
 */
public class FileListActivity extends DataBindingActivity<ActivityFileListBinding> {
    private FileListAdapter adapter;
    private FileListAdapter.FileInfo waitingRenameItem;//等待重命名的文件
    private Dialog renameDialog;//重命名弹窗
    private Dialog sortDialog;//排序弹窗
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
                        dismissLoadingDialog();
                    }
                });
            }

            @Override
            public void onNoData() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {//获取文件时没有数据
                        dataBinding.setTotalCount(0);
                        dismissLoadingDialog();
                    }
                });
            }

            @Override
            public void play(FileListAdapter.FileInfo fileInfo) {
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
        showLoadingDialog();
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
        ArrayList<Uri> files = new ArrayList<>();
        for (FileListAdapter.FileInfo f : adapter.getSelectedItemSet()) {
            files.add(f.uri);
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
        showSoftKeyboard(renameBinding.include.editText);
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
        renameBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.dialog_rename, (ViewGroup) rootView, false);
        renameDialog = MyTool.createBottomInputDialog(this, renameBinding.getRoot());
        renameDialog.setCancelable(true);

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
                String newName = renameBinding.getValue().trim();
                if (newName.equals(waitingRenameItem.name)) {//文件名没有改变，不需要执行操作
                    renameDialog.dismiss();
                    return;
                }
                if (!MyTool.checkNewFileName(FileListActivity.this, newName)) return;
                try {
                    String suffix = RecordService.getSuffix();
                    boolean result = FileUtil.renameFileInExternalPublicSpace(FileUtil.MediaType.AUDIO
                            , Config.DIR_RECORD, waitingRenameItem.name + suffix, newName + suffix);
                    if (result) {
                        waitingRenameItem.name = newName;
                        adapter.notifyItemChanged(adapter.getDataList().indexOf(waitingRenameItem), "name");
                        toast("保存成功");
                    } else {
                        throw new Exception("操作异常");
/*                        toast("文件不存在");
                        adapter.deleteFiles();*/
                    }

                } catch (Exception e) {
                    toast("操作异常");
                    e.printStackTrace();
                }
                renameDialog.dismiss();
            }
        });
    }


    /**
     * 创建排序弹窗
     */
    private void createSortDialog() {
        //创建弹窗
        sortBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.dialog_sort, (ViewGroup) rootView, false);
        sortDialog = MyTool.createBottomDialog(this, 0, sortBinding.getRoot());
        sortDialog.setCancelable(true);

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
                    showLoadingDialog();
                    adapter.sortByCreateTime(false);
                } else {
                    if (pair.first.equals("title") && !pair.second) return;
                    showLoadingDialog();
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
                    showLoadingDialog();
                    adapter.sortByCreateTime(true);
                } else {
                    if (pair.first.equals("title") && pair.second) return;
                    showLoadingDialog();
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