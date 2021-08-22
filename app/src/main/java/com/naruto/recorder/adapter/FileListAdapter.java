package com.naruto.recorder.adapter;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.naruto.recorder.Config;
import com.naruto.recorder.R;
import com.naruto.recorder.SharedPreferencesHelper;
import com.naruto.recorder.databinding.ItemFileBinding;
import com.naruto.recorder.service.RecordService;
import com.naruto.recorder.utils.FileUtil;
import com.naruto.recorder.utils.MyTool;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Purpose 录音文件列表适配器
 * @Author Naruto Yang
 * @CreateDate 2020/11/15 0015
 * @Note
 */
public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.VH> {
    private static final FileUtil.MediaType MEDIA_TYPE = FileUtil.MediaType.AUDIO;
    private static final String fileFolderPath = Config.DIR_RECORD;
    private static final String SUFFIX = RecordService.getSuffix();

    private List<FileInfo> dataList = new ArrayList<>();
    private boolean isEditMode;//是否处于编辑模式
    private Set<FileInfo> selectedItemSet = new HashSet<>();//记录选中项
    private ListListener listener;

    public FileListAdapter(ListListener listener) {
        this.listener = listener;

        //开启线程获取文件数据
        new Thread(new Runnable() {
            @Override
            public void run() {
                //获取录音路径下所有音频文件
                FilenameFilter filenameFilter = (folder2, name) -> name.toLowerCase().endsWith(SUFFIX);
                FileUtil.MyFileFilter myFileFilter = new FileUtil.MyFileFilter(filenameFilter
                        , " and " + MediaStore.MediaColumns.DISPLAY_NAME + " like ?", new String[]{"%" + SUFFIX});
                final MediaPlayer mediaPlayer = new MediaPlayer();

                List<FileInfo> list = FileUtil.getFileInExternalPublicSpace(MEDIA_TYPE, fileFolderPath, myFileFilter, mediaData -> {
                    FileInfo fileInfo = new FileInfo();
                    fileInfo.name = mediaData.name.substring(0, mediaData.name.lastIndexOf(SUFFIX));
                    fileInfo.uri = mediaData.fileUri;
                    long createTime = 0;
                    int duration = 0;
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        File file = new File(mediaData.absolutePath);
                        createTime = file.lastModified();
                        try {
                            mediaPlayer.setDataSource(file.getAbsolutePath());
                            mediaPlayer.prepare();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        duration = mediaPlayer.getDuration();
                        mediaPlayer.reset();
                    } else {
                        //MediaStore取出来的是秒，这里转毫秒
                        createTime = mediaData.createTime * 1000;
                        duration = mediaData.duration;
                    }
                    Log.d("FileListAdapter", "--->run: createTime=" + createTime);
                    Log.d("FileListAdapter", "--->run: duration=" + duration);
                    fileInfo.createTime = MyTool.formatTime(createTime, "yyyy/MM/dd/HH/mm/ss");
                    fileInfo.duration = MyTool.getTimeString(duration);
                    return fileInfo;
                });

                mediaPlayer.release();
                dataList = list;
                //排序
                Pair<String, Boolean> sortType = SharedPreferencesHelper.getSortType();
                if (sortType.first.equals("time")) {
                    sortByCreateTime(sortType.second);
                } else {
                    sortByName(sortType.second);
                }

            }
        }).start();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        VH vh = new VH(view);
        TypedValue typedValue = new TypedValue();
        parent.getContext().getTheme().resolveAttribute(R.attr.selectableItemBackground, typedValue, true);
        view.setBackgroundResource(typedValue.resourceId);

        if (listener != null) {
            //整个item点击
            vh.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = vh.getAdapterPosition();
                    if (isEditMode) {
                        toggleSelectedPosition(position);
                    } else {//播放
                        listener.play(getData(position));
                    }
                }
            });
            //item长按监听
            vh.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (!isEditMode) listener.OnItemLongPress();
                    toggleSelectedPosition(vh.getAdapterPosition());
                    return true;
                }
            });
        }
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        FileInfo fileInfo = getData(position);
        holder.dataBinding.setInfo(fileInfo);
        setStateUI(holder, getData(position));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {//局部刷新
            FileInfo fileInfo = getData(position);
            if (payloads.get(0).equals("name")) {//刷新文件名
                holder.dataBinding.setInfo(fileInfo);
            } else {//刷新复选框等UI控件状态
                setStateUI(holder, fileInfo);
            }
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }


    private void setStateUI(@NonNull VH holder, FileInfo fileInfo) {
        holder.dataBinding.setSelectable(isEditMode);
        holder.dataBinding.setSelected(isEditMode && !selectedItemSet.isEmpty() && selectedItemSet.contains(fileInfo));
    }

    /**
     * 切换编辑状态
     */
    public void toggleEditMode() {
        isEditMode = !isEditMode;
        selectedItemSet.clear();
        listener.onItemSelectedChange();
        notifyItemRangeChanged(0, getItemCount(), "");
    }


    /**
     * 设置选中位置
     *
     * @param position
     */
    public void toggleSelectedPosition(int position) {
        FileInfo item = getData(position);
        if (selectedItemSet.contains(item)) {
            selectedItemSet.remove(item);
        } else {
            selectedItemSet.add(item);
        }
        //刷新选中效果
        notifyItemChanged(position, "");
        listener.onItemSelectedChange();
    }


    /**
     * 切换全选/全不选
     */
    public void toggleSelectAll() {
        if (selectedItemSet.size() == dataList.size()) {//取消全选
            selectedItemSet.clear();
        } else {//全选
            for (int i = 0; i < dataList.size(); i++) {
                selectedItemSet.add(dataList.get(i));
            }
        }
        //刷新选中效果
        notifyItemRangeChanged(0, getItemCount(), "");
        listener.onItemSelectedChange();
    }

    /**
     * 获取当前选中的item
     *
     * @return
     */
    public Set<FileInfo> getSelectedItemSet() {
        return selectedItemSet;
    }

    /**
     * 删除选中的文件
     */
    public void deleteFiles() {
        for (FileInfo info : selectedItemSet) {
            deleteData(dataList.indexOf(info));
        }
        selectedItemSet.clear();
        listener.onItemSelectedChange();
    }

    /**
     * 根据文件名排序
     *
     * @param isReverse
     */
    public void sortByName(boolean isReverse) {
        Comparator<FileInfo> comparator = new Comparator<FileInfo>() {
            @Override
            public int compare(FileInfo fileInfo, FileInfo t1) {
                return fileInfo.name.compareTo(t1.name);
            }
        };
        sort(comparator, isReverse);
        SharedPreferencesHelper.setSortType("title", isReverse);
    }

    /**
     * 根据创建时间排序
     *
     * @param isReverse
     */
    public void sortByCreateTime(boolean isReverse) {
        Comparator<FileInfo> comparator = new Comparator<FileInfo>() {
            @Override
            public int compare(FileInfo fileInfo, FileInfo t1) {
                return fileInfo.createTime.compareTo(t1.createTime);
            }
        };
        sort(comparator, isReverse);
        SharedPreferencesHelper.setSortType("time", isReverse);
    }

    private void sort(Comparator<FileInfo> comparator, boolean isReverse) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Collections.sort(dataList, comparator);
                if (isReverse) Collections.reverse(dataList);
                listener.onSortFinish();
            }
        }).start();
    }

    public FileInfo getData(int position) {
        if (position >= 0 && position < dataList.size()) return dataList.get(position);
        return null;
    }

    public List<FileInfo> getDataList() {
        return dataList;
    }

    public void setDataList(List<FileInfo> dataList) {
        this.dataList = dataList;
        notifyDataSetChanged();
    }

    /**
     * 删除数据
     *
     * @param position
     */
    public void deleteData(int position) {
        FileInfo fileInfo = getData(position);
        boolean isSuccess = FileUtil.deleteFileInExternalPublicSpace(MEDIA_TYPE, fileFolderPath, fileInfo.name + SUFFIX);
        if (isSuccess) {
            dataList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public boolean isEditMode() {
        return isEditMode;
    }


    /**
     * ViewHolder
     */
    class VH extends RecyclerView.ViewHolder {
        ItemFileBinding dataBinding;

        public VH(@NonNull View itemView) {
            super(itemView);
            dataBinding = DataBindingUtil.bind(itemView);
        }
    }

    /**
     * @Purpose 录音文件信息
     * @Author Naruto Yang
     * @CreateDate 2020/11/15 0015
     * @Note
     */
    public class FileInfo {
        public String name;
        public Uri uri;
        public String createTime;
        public String duration;
    }

    /**
     * @Purpose 列表与activity的交互接口
     * @Author Naruto Yang
     * @CreateDate 2020/11/16 0016
     * @Note
     */
    public interface ListListener {
        void onItemSelectedChange();

        void OnItemLongPress();

        void onSortFinish();

        void onNoData();

        void play(FileInfo fileInfo);
    }
}
