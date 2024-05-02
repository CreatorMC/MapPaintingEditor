package com.tmsstudio.mappaintingeditor;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.util.Log;
import androidx.documentfile.provider.DocumentFile;
import com.litl.leveldb.DB;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Util {


    /**
     * 获取指定文件大小
     * @param file
     * @return
     * @throws Exception
     */
    public static long getFileSize(File file) throws Exception {
        long size = 0;
        if (file.exists()){
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                size = fis.available();
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
        }
        else{
            Log.e("TMS","文件不存在!");
        }
        return size;
    }


    /**
     * 获取文件夹大小
     * @param f
     * @return
     * @throws Exception
     */
    public static long getFileSizes(File f) throws Exception {
        long size = 0;
        File[] flist = f.listFiles();
        if(flist != null) {
            for (File file : flist) {
                if (file.isDirectory()) {
                    size = size + getFileSizes(file);
                } else {
                    size = size + getFileSize(file);
                }
            }
        }
        return size;
    }

    /**
     * 转换文件大小
     * @param fileS
     * @return
     */
    public static String FormetFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        String wrongSize="0B";
        if(fileS==0){
            return wrongSize;
        }
        if (fileS < 1024){
            fileSizeString = df.format((double) fileS) + "B";
        }
        else if (fileS < 1048576){
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        }
        else if (fileS < 1073741824){
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        }
        else{
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        }
        return fileSizeString;
    }


    /**
     * 获取全部应用的相关信息
     * @param context
     * @return
     */
    public static ArrayList<HashMap<String, Object>> getItems(Context context) {
        PackageManager pckMan = context.getPackageManager();
        ArrayList<HashMap<String, Object>> items = new ArrayList<>();
        List<PackageInfo> packageInfo = pckMan.getInstalledPackages(0);
        for (PackageInfo pInfo : packageInfo) {
            HashMap<String, Object> item = new HashMap<>();
            item.put("packageName", pInfo.packageName);
            item.put("appName", pInfo.applicationInfo.loadLabel(pckMan).toString());
            items.add(item);
        }
        return items;
    }

    /**
     * 分辨率转换为像素
     * @param dp
     * @param context
     * @return
     */
    public static float dpToPx(float dp, Context context){
        float scale=context.getResources().getDisplayMetrics().density;
        return dp*scale+0.5f;
    }

    /**
     * 像素转换为分辨率
     * @param px
     * @param context
     * @return
     */
    public static float pxToDp(float px,Context context){
        float scale=context.getResources().getDisplayMetrics().density;
        return (px-0.5f)/scale;
    }

    /**
     * 打开数据库
     * @param testDB 数据库
     * @param folder 地图文件夹路径
     */
    public static DB openDB(DB testDB, String folder) {
        File test = new File(folder + "/db");
        testDB = new DB(test);
        if(testDB.isClosed()){
            testDB.open();
        }
        return testDB;
    }

    /**
     * 关闭数据库
     * @param testDB 数据库
     */
    public static DB closeDB(DB testDB) {
        if(!testDB.isClosed()){
            testDB.close();
        }
        return testDB;
    }





    /*************************以下是适配安卓11及其以上的方法****************************/

    /**
     * 删除软件内文件
     * @param file
     */
    public static void deleteFile(File file){
        if(file.exists()){
            if(file.isDirectory()){
                for(File t: file.listFiles()){
                    deleteFile(t);
                }
                file.delete();
            } else {
                file.delete();
            }
        }
    }

    /**
     * 遍历文件和文件夹，进行复制
     * @param file 原文件的位置
     * @param app_file 当前文件位置
     * @param context 上下文
     */
    public static void listDocumentFile(DocumentFile file, File app_file, Context context){
        for(DocumentFile t: file.listFiles()){
            if(t.isDirectory()){
                //是个文件夹，创建文件夹
                File temp = new File(app_file.getPath()+ "/" + t.getName());
                temp.mkdir();
                listDocumentFile(t, temp, context);
            } else {
                try {
                    copyUriToExternalFilesDir(app_file, t, context);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取指定文件大小（安卓11）
     * @param file
     * @return
     * @throws Exception
     */
    public static long getFileSize(DocumentFile file) throws Exception {
        long size = 0;
        if (file.exists()){
            size = file.length();
        }
        else{
            Log.e("TMS","文件不存在!");
        }
        return size;
    }


    /**
     * 获取文件夹大小（安卓11）
     * @param f
     * @return
     * @throws Exception
     */
    public static long getFileSizes(DocumentFile f) throws Exception {
        long size = 0;
        DocumentFile[] flist = f.listFiles();
        for (DocumentFile file : flist) {
            if (file.isDirectory()) {
                size = size + getFileSizes(file);
            } else {
                size = size + getFileSize(file);
            }
        }
        return size;
    }

    /**
     * 把路径转换成URI
     * @param path 路径
     * @return String
     */
    public static String changeToUri(String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String path2 = path.replace("/storage/emulated/0/", "").replace("/", "%2F");
        return "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3A" + path2;
    }


    //获取指定目录的访问权限
    public static void startFor(String path, Activity context, int REQUEST_CODE_FOR_DIR) {
        String uri = changeToUri(path);//调用方法，把path转换成可解析的uri文本
        Uri parse = Uri.parse(uri);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, parse);
        }
        context.startActivityForResult(intent, REQUEST_CODE_FOR_DIR);//开始授权
    }

    //判断是否已经获取了指定路径权限
    public static boolean isGrant(Context context, String path) {
        DocumentFile documentFile = DocumentFile.fromTreeUri(context, Uri.parse(Util.changeToUri(path)));
        return documentFile != null && documentFile.canRead();
//        for (UriPermission persistedUriPermission : context.getContentResolver().getPersistedUriPermissions()) {
//            if (persistedUriPermission.isReadPermission() && persistedUriPermission.getUri().toString().equals(changeToUri(path))) {
//                return true;
//            }
//        }
//        return false;
    }

    //根据路径获得document文件
    public static DocumentFile getDoucmentFile(Context context, String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String path2 = path.replace("/storage/emulated/0/", "").replace("/", "%2F");
        return DocumentFile.fromSingleUri(context, Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3A" + path2));
    }

    /**
     * 得到DocumentFile输入流
     * @param context
     * @param file
     * @return
     */
    public static InputStream getInputStream(Context context, DocumentFile file) {
        InputStream in = null;
        try {
            if (file != null && file.canWrite()) {
                in = context.getContentResolver().openInputStream(file.getUri());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return in;
    }


    /**
     * 复制文件
     * @param file_folder 目标位置
     * @param documentFile 复制的文件
     * @param context 上下文
     * @throws IOException
     */
    public static void copyUriToExternalFilesDir(File file_folder, DocumentFile documentFile, Context context) throws IOException {
        ContentResolver contentResolver = context.getContentResolver();
        InputStream inputStream = contentResolver.openInputStream(documentFile.getUri());

        if (inputStream != null) {
            File file = new File(file_folder.getPath() + "/" + documentFile.getName());
            FileOutputStream fos = new FileOutputStream(file);
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            byte[] byteArray = new byte[1024];
            int bytes = bis.read(byteArray);
            while (bytes > 0) {
                bos.write(byteArray, 0, bytes);
                bos.flush();
                bytes = bis.read(byteArray);
            }
            bos.close();
            fos.close();
        }
    }

//    /**
//     * 把文件复制回去（失败的方法）
//     * @param context 上下文
//     * @param version 游戏版本
//     * @param folder_name 地图文件夹名称
//     * @param folder 本地存储路径
//     * @throws IOException
//     */
//    public static void copyReturnTOMC(Context context, int version, String folder_name, String folder) throws IOException {
//        DocumentFile documentFile = DocumentFile.fromTreeUri(context, Uri.parse(Util.changeToUri("/storage/emulated/0/Android/data")));
//        DocumentFile doc = null;
//        if(version == MainActivity.INTERNATIONAL){
//            //国际版
//            doc = documentFile.findFile(MainActivity.mojang).findFile("files").findFile("games").findFile("com.mojang").findFile("minecraftWorlds").findFile(folder_name).findFile("db");
//        }
//
//        //清空db文件夹内的文件
//        assert doc != null;
//        for(DocumentFile t : doc.listFiles()){
//            t.delete();
//        }
//
//        File db = new File(folder + "/db");
//        for(File t: Objects.requireNonNull(db.listFiles())){
//
//            ContentResolver contentResolver = context.getContentResolver();
//            OutputStream outputStream = contentResolver.openOutputStream(doc.createFile("", t.getName()).getUri());
//            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
//
//            FileInputStream fileInputStream = new FileInputStream(t);
//            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
//            byte[] byteArray = new byte[1024];
//            int bytes = bufferedInputStream.read(byteArray);
//            while (bytes > 0){
//                bufferedOutputStream.write(byteArray, 0, bytes);
//                bufferedOutputStream.flush();
//                bytes = bufferedInputStream.read(byteArray);
//            }
//            bufferedOutputStream.close();
//            bufferedInputStream.close();
//
//        }
//    }
}
