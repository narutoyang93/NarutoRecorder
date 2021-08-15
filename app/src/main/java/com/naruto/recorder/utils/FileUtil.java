package com.naruto.recorder.utils;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import com.naruto.recorder.Config;
import com.naruto.recorder.InterfaceFactory;
import com.naruto.recorder.MyApplication;
import com.naruto.recorder.base.BaseActivity;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * @Purpose 文件操作工具类
 * @Author Naruto Yang
 * @CreateDate 2021/7/11 0011
 * @Note
 */
public class FileUtil {
    public static final String APP_FOLDER = Config.DIR_APP_FOLDER;


    public static boolean delete(String filePath) {
        if (TextUtils.isEmpty(filePath))
            return true;
        File file = new File(filePath);
        return delete(file);
    }

    public static boolean delete(File file) {
        if (file == null)
            return true;
        if (file.isDirectory()) {//目录
            //先把目录下的文件都删除了，再删除目录
            String[] children = file.list();
            int size = 0;
            if (children != null) {
                size = children.length;
                for (int i = 0; i < size; i++) {
                    boolean success = delete(new File(file, children[i]));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }


    /**
     * 写文件
     *
     * @param bytes
     * @param iOutputStream
     * @param callback      回调
     */
    private static void writeDataToFile(byte[] bytes, IOutputStream iOutputStream, InterfaceFactory.Operation<Boolean> callback) {
        OutputStream outputStream = null;
        try {
            outputStream = iOutputStream.getOutputStream();
            outputStream.write(bytes);
            outputStream.flush();
            if (callback != null)
                callback.done(true);
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null)
                callback.done(false);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 写文件
     *
     * @param bytes
     * @param uri      uri地址
     * @param callback 回调
     */
    public static void writeDataToFile(byte[] bytes, Uri uri, InterfaceFactory.Operation<Boolean> callback) {
        if (uri == null) {
            callback.done(false);
            return;
        }
        writeDataToFile(bytes, () -> {
            ContentResolver resolver = MyApplication.getContext().getContentResolver();
            return new BufferedOutputStream(resolver.openOutputStream(uri));
        }, callback);
    }

    public static void writeDataToFile(byte[] bytes, String fileName, String savePath, InterfaceFactory.Operation<Boolean> callback) {
        //创建文件目录
        File dir = new File(savePath);
        if (!dir.exists() && !dir.isDirectory()) {
            dir.mkdirs();
        }
        File file = new File(savePath + File.separator + fileName);

        writeDataToFile(bytes, () -> {
            FileOutputStream fos = new FileOutputStream(file);
            return new BufferedOutputStream(fos);
        }, callback);
    }

    /**
     * 将ByteArrayOutputStream写入文件
     */
    public static void outPutStreamToFile(ByteArrayOutputStream baos, String fileName, String savePath, InterfaceFactory.Operation<Boolean> callback) {
        if (TextUtils.isEmpty(fileName) || TextUtils.isEmpty(savePath)) {
            callback.done(false);
            return;
        }
        File file = new File(savePath);
        if (!file.exists())
            file.mkdirs();
        outPutStreamToFile(baos, () -> new FileOutputStream(new File(savePath, fileName)), callback);
    }

    /**
     * 将ByteArrayOutputStream写入文件
     */
    public static void outPutStreamToFile(ByteArrayOutputStream baos, Uri uri, InterfaceFactory.Operation<Boolean> callback) {
        if (uri == null) {
            if (callback != null)
                callback.done(false);
            return;
        }
        outPutStreamToFile(baos, () -> new BufferedOutputStream(MyApplication.getContext().getContentResolver().openOutputStream(uri)), callback);
    }


    /**
     * 将ByteArrayOutputStream写入文件
     *
     * @param baos
     * @param iOutputStream
     * @param callback
     */
    private static void outPutStreamToFile(ByteArrayOutputStream baos, IOutputStream iOutputStream, InterfaceFactory.Operation<Boolean> callback) {
        if (baos == null || iOutputStream == null) {
            if (callback != null)
                callback.done(false);
            return;
        }
        OutputStream outputStream = null;
        try {
            outputStream = iOutputStream.getOutputStream();
            baos.writeTo(outputStream);
            baos.flush();
            outputStream.flush();
            if (callback != null)
                callback.done(true);
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null)
                callback.done(false);
        } finally {
            try {
                baos.close();
                if (outputStream != null)
                    outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 根据URI创建文件
     *
     * @param contentUri
     * @param relativePath
     * @param fileName
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private static Uri createFile(Uri contentUri, String relativePath, String fileName) {
        ContentResolver resolver = MyApplication.getContext().getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.Downloads.MIME_TYPE, getMimeType(fileName));
        contentValues.put(MediaStore.Downloads.DATE_TAKEN, System.currentTimeMillis());
        contentValues.put(MediaStore.Downloads.RELATIVE_PATH, relativePath);
        return resolver.insert(contentUri, contentValues);
    }

    /**
     * 创建文件
     *
     * @param folderPath 文件夹绝对路径
     * @param fileName
     * @return
     */
    private static Uri createFile(String folderPath, String fileName) {
        File storageDir = new File(folderPath);
        if (!storageDir.exists() && !storageDir.mkdirs()) {
            Log.e("FileUtil", "--->createFile: ", new Exception("mkdirs失败"));
            return null;
        }
        File file = new File(folderPath + fileName);
        try {
            if (!file.createNewFile()) return null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return getUriForFile(file);
    }


    /**
     * 根据文件获取Uri
     *
     * @param file
     * @return
     */
    public static Uri getUriForFile(File file) {
        Context context = MyApplication.getContext();
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider", file);
    }

    /**
     * 外部私有空间，卸载即删除，读写无需申请权限
     *
     * @param relativePath 相对路径，如：”download/picture/“
     * @return
     */
    public static String getPathFromExternalPrivateSpace(String relativePath) {
        return MyApplication.getContext().getExternalFilesDir(null).getAbsolutePath() + File.separator + relativePath;
    }

    /**
     * 外部公共空间，读写需申请权限
     *
     * @param mediaType
     * @param relativePath
     * @return
     */
    public static String getPathFromExternalPublicSpace(MediaType mediaType, String relativePath) {
        MediaData mediaData = getMediaStoreData(mediaType);
        return getPathFromExternalPublicSpace(mediaData.directory, relativePath);
    }

    /**
     * 外部公共空间，读写需申请权限
     *
     * @param systemDirectory
     * @param relativePath
     * @return
     */
    private static String getPathFromExternalPublicSpace(String systemDirectory, String relativePath) {
        return getPathFromExternalPublicSpace(systemDirectory) + "/" + APP_FOLDER + relativePath;
    }

    private static String getPathFromExternalPublicSpace(String systemDirectory) {
        return Environment.getExternalStoragePublicDirectory(systemDirectory).getAbsolutePath();
    }

    /**
     * 在外部私有空间创建文件
     *
     * @param relativePath
     * @param fileName
     * @return
     */
    public static Uri createFileInExternalPrivateSpace(String relativePath, String fileName) {
        String folderPath = FileUtil.getPathFromExternalPrivateSpace(relativePath);
        return createFile(folderPath, fileName);
    }


    /**
     * 在外部公共存储空间创建文件
     *
     * @param mediaType
     * @param relativePath
     * @param fileName     文件名，需带后缀名
     * @return
     */
    private static void createFileInExternalPublicSpace(MediaType mediaType, String relativePath, String fileName, InterfaceFactory.Operation<Uri> callback) {
        if (TextUtils.isEmpty(fileName)) {
            callback.done(null);
            return;
        }
        MediaData mediaData = getMediaStoreData(mediaType);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            MyApplication.doWithPermission(new BaseActivity.RequestPermissionsCallBack(null, Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                @Override
                public void onGranted() {
                    String folderPath = getPathFromExternalPublicSpace(mediaData.directory, relativePath);
                    Uri uri = createFile(folderPath, fileName);
                    callback.done(uri);
                }
            });
        } else {
            try {
                Uri uri = createFile(mediaData.contentUri, getRelativePathInRoot(mediaData.directory, relativePath), fileName);
                callback.done(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 在外部公共存储空间创建音频文件
     *
     * @param relativePath
     * @param fileName
     * @param callback
     */
    public static void createAudioFileInExternalPublicSpace(String relativePath, String fileName, InterfaceFactory.Operation<Uri> callback) {
        createFileInExternalPublicSpace(MediaType.AUDIO, relativePath, fileName, callback);
    }

    /**
     * 在外部公共存储空间创建视频文件
     *
     * @param relativePath
     * @param fileName
     * @param callback
     */
    public static void createVideoFileInExternalPublicSpace(String relativePath, String fileName, InterfaceFactory.Operation<Uri> callback) {
        createFileInExternalPublicSpace(MediaType.VIDEO, relativePath, fileName, callback);
    }

    /**
     * 在外部公共存储空间创建图像文件
     *
     * @param relativePath
     * @param fileName
     * @param callback
     */
    public static void createImageFileInExternalPublicSpace(String relativePath, String fileName, InterfaceFactory.Operation<Uri> callback) {
        createFileInExternalPublicSpace(MediaType.IMAGE, relativePath, fileName, callback);
    }

    /**
     * 在外部公共存储空间创建文本文件
     *
     * @param relativePath
     * @param fileName
     * @param callback
     */
    public static void createDocumentFileInExternalPublicSpace(String relativePath, String fileName, InterfaceFactory.Operation<Uri> callback) {
        createFileInExternalPublicSpace(MediaType.FILE, relativePath, fileName, callback);
    }

    /**
     * 在外部公共存储空间创建下载文件
     *
     * @param relativePath
     * @param fileName
     * @param callback
     */
    public static void createDownloadFileInExternalPublicSpace(String relativePath, String fileName, InterfaceFactory.Operation<Uri> callback) {
        createFileInExternalPublicSpace(MediaType.DOWNLOAD, relativePath, fileName, callback);
    }


    /**
     * 获取外部公共存储空间文件
     *
     * @param mediaType
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @param fileInfoCreator
     * @param <T>
     * @return
     */
    public static <T> List<T> getFileInExternalPublicSpace(MediaType mediaType, String selection, String[] selectionArgs, String sortOrder, FileInfoCreator<T> fileInfoCreator) {
        MediaData mediaData = getMediaStoreData(mediaType);
        return getFileInExternalPublicSpace(mediaData, selection, selectionArgs, sortOrder, fileInfoCreator);
    }

    /**
     * 获取外部公共存储空间文件
     *
     * @param mediaData
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @param fileInfoCreator
     * @param <T>
     * @return
     */
    public static <T> List<T> getFileInExternalPublicSpace(MediaData mediaData, String selection, String[] selectionArgs, String sortOrder, FileInfoCreator<T> fileInfoCreator) {
        String[] projection = new String[]{
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.RELATIVE_PATH,
                MediaStore.MediaColumns.DURATION,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.SIZE
        };

        List<T> list = new ArrayList<>();

        try (Cursor cursor = MyApplication.getContext().getContentResolver().query(
                mediaData.contentUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
        )) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
            int relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION);
            int createTimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                String relativePath = cursor.getString(relativePathColumn);
                int duration = cursor.getInt(durationColumn);
                int size = cursor.getInt(sizeColumn);
                long createTime = cursor.getLong(createTimeColumn);
                Uri fileUri = ContentUris.withAppendedId(mediaData.contentUri, id);
                MediaData data = new MediaData(id, fileUri, name, null, relativePath, duration, size, createTime);
                list.add(fileInfoCreator.create(data));
            }
        }
        return list;
    }


    /**
     * 获取外部公共空间的文件
     *
     * @param mediaType
     * @param relativePath
     * @param myFileFilter
     * @param fileInfoCreator
     * @param <T>
     * @return
     */
    public static <T> List<T> getFileInExternalPublicSpace(MediaType mediaType, String relativePath, MyFileFilter myFileFilter, FileInfoCreator<T> fileInfoCreator) {
        return doWithFilter(myFileFilter, mediaType, relativePath, new FilterHelper<List<T>>() {
            @Override
            public List<T> onResult(File[] files) {
                List<T> list = new ArrayList<>();
                MediaData mediaData;
                for (File f : files) {
                    mediaData = new MediaData(getUriForFile(f), f.getName(), f.getAbsolutePath());
                    list.add(fileInfoCreator.create(mediaData));
                }
                return list;
            }

            @Override
            public List<T> onGotParamForMediaStore(MediaData data, String selection, String[] selectionArgs) {
                return getFileInExternalPublicSpace(data, selection, selectionArgs, null, fileInfoCreator);
            }
        });
    }

    /**
     * 获取外部公共空间的文件
     *
     * @param mediaType
     * @param relativePath
     * @param fileName
     * @return
     */
    public static Uri getFileInExternalPublicSpace(MediaType mediaType, String relativePath, String fileName) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            String folderPath = getPathFromExternalPublicSpace(mediaType, relativePath);
            return getUriForFile(new File(folderPath + fileName));
        } else {
            MediaData mediaData = getMediaStoreData(mediaType);
            String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=? and " + MediaStore.MediaColumns.RELATIVE_PATH + "=?";
            String[] args = new String[]{fileName, getRelativePathInRoot(mediaData.directory, relativePath)};
            List<Uri> list = getFileInExternalPublicSpace(mediaData, selection, args, null, data -> data.fileUri);
            if (list.isEmpty()) return null;
            return list.get(0);
        }
    }


    /**
     * 删除外部公共空间的文件
     *
     * @param mediaType
     * @param selection
     * @param selectionArgs
     * @return
     */
    public static int deleteFileInExternalPublicSpace(MediaType mediaType, String selection, String[] selectionArgs) {
        MediaData mediaData = getMediaStoreData(mediaType);
        ContentResolver resolver = MyApplication.getContext().getContentResolver();
        return resolver.delete(
                mediaData.contentUri,
                selection,
                selectionArgs);
    }

    /**
     * 删除外部公共空间的文件
     *
     * @param mediaType
     * @param relativePath
     * @param fileName
     * @return
     */
    public static boolean deleteFileInExternalPublicSpace(MediaType mediaType, String relativePath, String fileName) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            String filePath = getPathFromExternalPublicSpace(mediaType, relativePath) + fileName;
            return delete(filePath);
        } else {
            String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=? and " + MediaStore.MediaColumns.RELATIVE_PATH + "=?";
            String[] args = new String[]{fileName, getRelativePathInRoot(mediaType, relativePath)};
            return deleteFileInExternalPublicSpace(mediaType, selection, args) > 0;
        }
    }


    /**
     * 删除外部公共空间的文件夹
     *
     * @param mediaType
     * @param relativePath
     * @return
     */
    public static boolean deleteFolderInExternalPublicSpace(MediaType mediaType, String relativePath) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            String folderPath = getPathFromExternalPublicSpace(mediaType, relativePath);
            return delete(folderPath);
        } else {
            String selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?";
            String[] args = new String[]{getRelativePathInRoot(mediaType, relativePath)};
            return deleteFileInExternalPublicSpace(mediaType, selection, args) > 0;
        }
    }


    /**
     * 更新外部存储空间的文件
     *
     * @param mediaType
     * @param selection
     * @param selectionArgs
     * @param updateValues
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static boolean updateFileInExternalPublicSpace(MediaType mediaType, String selection, String[] selectionArgs, ContentValues updateValues) {
        MediaData mediaData = getMediaStoreData(mediaType);
        ContentResolver resolver = MyApplication.getContext().getContentResolver();

        {//设置IS_PENDING
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Files.FileColumns.IS_PENDING, 1);
            resolver.update(mediaData.contentUri, cv, selection, selectionArgs);
        }

        updateValues.put(MediaStore.Files.FileColumns.IS_PENDING, 0);
        return resolver.update(
                mediaData.contentUri,
                updateValues,
                selection,
                selectionArgs) > 0;
    }

    /**
     * 重命名
     *
     * @param mediaType
     * @param relativePath
     * @param oldFileName
     * @param newFileName
     * @return
     */
    public static boolean renameFileInExternalPublicSpace(MediaType mediaType, String relativePath, String oldFileName, String newFileName) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            String folderPath = getPathFromExternalPublicSpace(mediaType, relativePath);
            File old = new File(folderPath + oldFileName);
            return old.renameTo(new File(folderPath + newFileName));
        } else {
            String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=? and " + MediaStore.MediaColumns.RELATIVE_PATH + "=?";
            String[] args = new String[]{oldFileName, getRelativePathInRoot(mediaType, relativePath)};
            ContentValues updateValues = new ContentValues();
            updateValues.put(MediaStore.MediaColumns.DISPLAY_NAME, newFileName);
            return updateFileInExternalPublicSpace(mediaType, selection, args, updateValues);
        }
    }


    /**
     * @param myFileFilter
     * @param mediaType
     * @param relativePath
     * @param filterHelper
     */
    private static <T> T doWithFilter(MyFileFilter myFileFilter, MediaType mediaType, String relativePath, FilterHelper<T> filterHelper) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            String folderPath = getPathFromExternalPublicSpace(mediaType, relativePath);
            File folder = new File(folderPath);
            File[] files;
            if (myFileFilter == null) files = folder.listFiles();
            else {//有过滤条件
                if (myFileFilter.filenameFilter != null)
                    files = folder.listFiles(myFileFilter.filenameFilter);
                else files = folder.listFiles(myFileFilter.fileFilter);
            }
            return filterHelper.onResult(files);
        } else {
            MediaData data = getMediaStoreData(mediaType);
            String selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?";
            String[] args = new String[]{getRelativePathInRoot(data.directory, relativePath)};

            if (myFileFilter != null && !TextUtils.isEmpty(myFileFilter.selection)) {//有过滤条件
                selection += myFileFilter.selection;
                if (myFileFilter.selectionArgs != null && myFileFilter.selectionArgs.length > 0) {//合并参数
                    String[] a = args, b = myFileFilter.selectionArgs, c = new String[a.length + b.length];
                    System.arraycopy(a, 0, c, 0, a.length);
                    System.arraycopy(b, 0, c, a.length, b.length);
                    args = c;
                }
            }
            Log.d("FileUtil", "--->getFileInExternalPublicSpace: selection=" + selection);
            return filterHelper.onGotParamForMediaStore(data, selection, args);
        }
    }

    /**
     * 获取sd根目录下的相对路径
     *
     * @param mediaType
     * @param relativePath
     * @return
     */
    public static String getRelativePathInRoot(MediaType mediaType, String relativePath) {
        MediaData mediaData = getMediaStoreData(mediaType);
        return getRelativePathInRoot(mediaData.directory, relativePath);
    }

    /**
     * 获取sd根目录下的相对路径
     *
     * @param systemDirectory
     * @param relativePath
     * @return
     */
    private static String getRelativePathInRoot(String systemDirectory, String relativePath) {
        return String.format("%s/%s%s", systemDirectory, APP_FOLDER, relativePath);
    }


    /**
     * @param mediaType
     * @return
     */
    private static MediaData getMediaStoreData(MediaType mediaType) {
        MediaData data = new MediaData();
        switch (mediaType) {
            case AUDIO:
                data.directory = Environment.DIRECTORY_MUSIC;
                data.contentUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
                break;
            case IMAGE:
                data.directory = Environment.DIRECTORY_PICTURES;
                data.contentUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                break;
            case VIDEO:
                data.directory = Environment.DIRECTORY_MOVIES;
                data.contentUri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                break;
            case FILE:
                data.directory = Environment.DIRECTORY_DOCUMENTS;
                data.contentUri = MediaStore.Files.getContentUri("external");
                break;
            case DOWNLOAD:
                data.directory = Environment.DIRECTORY_DOWNLOADS;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    data.contentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                }
                break;
        }

        return data;
    }


    /**
     * 删除Uri对应的资源
     */
    public static boolean delete(Uri uri) {
        if (uri == null) return true;
        return MyApplication.getContext().getContentResolver().delete(uri, null, null) > 0;
    }

    /**
     * 判断字符串是否是Uri
     */
    public static boolean isUri(String str) {
        if (TextUtils.isEmpty(str)) return false;
        return str.startsWith("content://");
    }

    /**
     * 是否本地文件
     *
     * @param filePah
     * @return
     */
    public static boolean isLocalFile(String filePah) {
        if (TextUtils.isEmpty(filePah)) return false;
        return filePah.startsWith(getPathFromExternalPublicSpace(""));
    }


    /**
     * 获取文件的 mimeType
     */
    private static String getMimeType(String fileName) {
        String type = "*/*";
        //获取后缀名前的分隔符"."在fName中的位置。
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex < 0) {
            return type;
        }
        /* 获取文件的后缀名*/
        String end = fileName.substring(dotIndex, fileName.length()).toLowerCase();
        if ("".equals(end)) return type;
        //在MIME和文件类型的匹配表中找到对应的MIME类型。
        for (int i = 0; i < getFileMiMeType().length; i++) { //MIME_MapTable??在这里你一定有疑问，这个MIME_MapTable是什么？
            if (end.equals(getFileMiMeType()[i][0]))
                type = getFileMiMeType()[i][1];
        }
        return type;
    }

    /**
     * 文件对应的MimeType
     *
     * @return
     */
    public static String[][] getFileMiMeType() {
        String[][] MIME_MapTable = {
                //{后缀名，MIME类型}
                {".3gp", "video/3gpp"},
                {".apk", "application/vnd.android.package-archive"},
                {".asf", "video/x-ms-asf"},
                {".avi", "video/x-msvideo"},
                {".bin", "application/octet-stream"},
                {".bmp", "image/bmp"},
                {".c", "text/plain"},
                {".class", "application/octet-stream"},
                {".conf", "text/plain"},
                {".cpp", "text/plain"},
                {".doc", "application/msword"},
                {".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
                {".xls", "application/vnd.ms-excel"},
                {".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
                {".exe", "application/octet-stream"},
                {".gif", "image/gif"},
                {".gtar", "application/x-gtar"},
                {".gz", "application/x-gzip"},
                {".h", "text/plain"},
                {".htm", "text/html"},
                {".html", "text/html"},
                {".jar", "application/java-archive"},
                {".java", "text/plain"},
                {".jpeg", "image/jpeg"},
                {".jpg", "image/jpeg"},
                {".js", "application/x-javascript"},
                {".log", "text/plain"},
                {".m3u", "audio/x-mpegurl"},
                {".m4a", "audio/mp4a-latm"},
                {".m4b", "audio/mp4a-latm"},
                {".m4p", "audio/mp4a-latm"},
                {".m4u", "video/vnd.mpegurl"},
                {".m4v", "video/x-m4v"},
                {".mov", "video/quicktime"},
                {".mp2", "audio/x-mpeg"},
                {".mp3", "audio/x-mpeg"},
                {".mp4", "video/mp4"},
                {".mpc", "application/vnd.mpohun.certificate"},
                {".mpe", "video/mpeg"},
                {".mpeg", "video/mpeg"},
                {".mpg", "video/mpeg"},
                {".mpg4", "video/mp4"},
                {".mpga", "audio/mpeg"},
                {".msg", "application/vnd.ms-outlook"},
                {".ogg", "audio/ogg"},
                {".pdf", "application/pdf"},
                {".png", "image/png"},
                {".pps", "application/vnd.ms-powerpoint"},
                {".ppt", "application/vnd.ms-powerpoint"},
                {".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"},
                {".prop", "text/plain"},
                {".rc", "text/plain"},
                {".rmvb", "audio/x-pn-realaudio"},
                {".rtf", "application/rtf"},
                {".rar", "rar application/x-rar-compressed"},
                {".sh", "text/plain"},
                {".tar", "application/x-tar"},
                {".tgz", "application/x-compressed"},
                {".txt", "text/plain"},
                {".wav", "audio/x-wav"},
                {".wma", "audio/x-ms-wma"},
                {".wmv", "audio/x-ms-wmv"},
                {".wps", "application/vnd.ms-works"},
                {".xml", "text/plain"},
                {".z", "application/x-compress"},
                {".zip", "application/x-zip-compressed"},
                {".aac", "audio/x-aac"},
                {"", "*/*"}
        };
        return MIME_MapTable;
    }

    /**
     * @Description
     * @Author Naruto Yang
     * @CreateDate 2021/6/28 0028
     * @Note
     */
    public static class MediaData {
        public MediaData() {
        }

        public MediaData(long id, Uri fileUri, String fileName, String absolutePath, String relativePath, int duration, int size, long createTime) {
            this(fileUri, fileName, absolutePath);
            this.id = id;
            this.relativePath = relativePath;
            this.duration = duration;
            this.size = size;
            this.createTime = createTime;
        }

        public MediaData(Uri fileUri, String name, String absolutePath) {
            this.fileUri = fileUri;
            this.name = name;
        }

        public Uri contentUri;
        public String directory;

        public long id;
        public Uri fileUri;
        public String name;
        public String relativePath;
        public String absolutePath;
        public int duration;
        public int size;
        public long createTime;
    }


    /**
     * @Description
     * @Author Naruto Yang
     * @CreateDate 2021/7/7 0007
     * @Note
     */
    public enum MediaType {
        AUDIO, VIDEO, IMAGE, FILE, DOWNLOAD
    }


    /**
     * @Description 文件过滤
     * @Author Naruto Yang
     * @CreateDate 2021/7/18 0018
     * @Note
     */
    public static class MyFileFilter {
        private FileFilter fileFilter;
        private FilenameFilter filenameFilter;
        private String selection;
        private String[] selectionArgs;

        public MyFileFilter(FileFilter fileFilter, String selection, String[] selectionArgs) {
            this.fileFilter = fileFilter;
            this.selection = selection;
            this.selectionArgs = selectionArgs;
        }

        public MyFileFilter(FilenameFilter filenameFilter, String selection, String[] selectionArgs) {
            this.filenameFilter = filenameFilter;
            this.selection = selection;
            this.selectionArgs = selectionArgs;
        }
    }

    /**
     * @Description 用于搜索回调创建文件对象
     * @Author Naruto Yang
     * @CreateDate 2021/7/18 0018
     * @Note
     */
    public interface FileInfoCreator<T> {
        T create(MediaData mediaData);
    }

    /**
     * @Description
     * @Author Naruto Yang
     * @CreateDate 2021/7/6 0006
     * @Note
     */
    private interface IOutputStream {
        OutputStream getOutputStream() throws Exception;
    }


    /**
     * @Description
     * @Author Naruto Yang
     * @CreateDate 2021/7/19 0019
     * @Note
     */
    private interface FilterHelper<T> {
        T onResult(File[] files);

        T onGotParamForMediaStore(MediaData data, String selection, String[] selectionArgs);
    }
}