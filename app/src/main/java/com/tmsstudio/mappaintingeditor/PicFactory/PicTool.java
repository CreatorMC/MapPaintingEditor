package com.tmsstudio.mappaintingeditor.PicFactory;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;

public class PicTool {

	/**
	 * 重新生成图片宽、高
	 * 
	 * @param srcPath
	 *            图片路径
	 * @param newWith
	 *            新的宽度
	 * @param newHeight
	 *            新的高度
	 * @param forceSize
	 *            是否强制使用指定宽、高,false:会保持原图片宽高比例约束
	 * @return 字节数组
	 * @throws Exception 
	 */
	public static byte[] resizeImage(String srcPath, int newWith, int newHeight, boolean forceSize) throws Exception {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(getImageBytes(srcPath));
		Bitmap image = BitmapFactory.decodeStream(inputStream);
		if(image.getWidth() <= newWith && image.getHeight() <= newHeight){
			//大小比目标小，不进行缩放
			return getImageBytes(srcPath);
		}
		if (forceSize) {
			//强制缩放
			image = Bitmap.createScaledBitmap(image, newWith, newHeight, true);
		} else {
			//按比例缩放
			int width = image.getWidth();
			int height = image.getHeight();
			float scale = Math.max(width, height)/(float)128;	//缩放比例
			image = Bitmap.createScaledBitmap(image, (int)(width/scale), (int)(height/scale), true);
		}
		image.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
		return outputStream.toByteArray();
	}

	/**
	 * 根据指定大小压缩图片
	 *
	 * @param imageBytes
	 *            源图片字节数组
	 * @param desFileSize
	 *            指定图片大小，单位kb
	 * @return 压缩质量后的图片字节数组
	 */
//	public static byte[] compressPicForScale(byte[] imageBytes, long desFileSize) {
//		if (imageBytes == null || imageBytes.length <= 0) {
//			return imageBytes;
//		}
//		try {
//			int quality = 100;
//
//			while (imageBytes.length > desFileSize * 1024) {
//				Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.length);
//				ByteArrayOutputStream outputStream = new ByteArrayOutputStream(imageBytes.length);
//				if(quality > 20){
//					quality -= 20;	//每次质量减少
//				} else {
//					break;
//				}
//				bitmap.compress(Bitmap.CompressFormat.PNG, quality, outputStream);
//				imageBytes = outputStream.toByteArray();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return imageBytes;
//	}

	/**
	 * 自动调节精度(经验数值)
	 *
	 * @param size
	 *            源图片大小
	 * @return 图片压缩质量比
	 */
	private static double getAccuracy(long size) {
		double accuracy;
		if (size < 900) {
			accuracy = 0.85;
		} else if (size < 2047) {
			accuracy = 0.6;
		} else if (size < 3275) {
			accuracy = 0.44;
		} else {
			accuracy = 0.4;
		}
		return accuracy;
	}
	
	/**
	 * 把图片转为字节数组
	 * @param path 图片路径
	 * @return
	 * @throws Exception
	 */
	public static byte[] getImageBytes(String path) throws Exception {

//		BitmapFactory.Options opt = new BitmapFactory.Options();
//		opt.inPreferredConfig = Bitmap.Config.RGB_565;
//		opt.inPurgeable = true;
//		opt.inInputShareable = true;
//		//获取资源图片
//		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//		Bitmap bitmap = BitmapFactory.decodeFile(path, opt);
//		bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
//		return outputStream.toByteArray();

		File file = new File(path);     //将图片转换成file类型的文件
		BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
        byte[] byt = new byte[fis.available()]; //字节数据      available() 返回的实际可读字节数，也就是总大小
        fis.read(byt); // 从输入流中读取一定数量的字节，并将其存储在缓冲区数组 b  中。以整数形式返回实际读取的字节数。
        return byt;

//		BitmapFactory.Options opts = new BitmapFactory.Options();
//		opts.inJustDecodeBounds = true;
//		opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
//		BitmapFactory.decodeFile(path, opts);
//		int width = opts.outWidth;
//		int height = opts.outHeight;
//		float scaleWidth = 0.f, scaleHeight = 0.f;
//		int w = 200;
//		int h = 200;
//		if (width > w || height > h) {
//			scaleWidth = ((float) width) / w;
//			scaleHeight = ((float) height) / h;
//		}
//		opts.inJustDecodeBounds = false;
//		float scale = Math.max(scaleWidth, scaleHeight);
//		opts.inSampleSize = (int) scale;
//		WeakReference<Bitmap> weak = new WeakReference<>(BitmapFactory.decodeFile(path, opts));
//		Bitmap bitmap = Bitmap.createScaledBitmap(weak.get(), w, h, true);
//		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//		bitmap.compress(Bitmap.CompressFormat.PNG,100,outputStream);
//		return outputStream.toByteArray();
	}
	
	
	/**
	 * 将字节数组转为图片并生成
	 * @param bytes 图片字节数组
	 * @param path 图片生成路径
	 * @throws IOException
	 */
	public static void bytesToImage(byte[] bytes, String path) throws IOException {
		File apple = new File(path);
        FileOutputStream fos = new FileOutputStream(apple);
        fos.write(bytes);       
        fos.flush();
        fos.close();
	}
	
	
	/**
	 * 生成指定大小的PNG格式透明图片
	 * @return 字节数组
	 * @throws IOException
	 */
	public static byte[] generatePNG(int width, int height) throws IOException {
		int[] ints = new int[width * height];
		Bitmap bitmap = Bitmap.createBitmap(ints,width, height, Bitmap.Config.ARGB_8888);
		Log.i("TMS", "宽: " + bitmap.getWidth() + "高：" + bitmap.getHeight());
		int size = bitmap.getWidth() * bitmap.getHeight() * 4;
		// 创建一个字节数组输出流,流的大小为size
		ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
		// 设置位图的压缩格式，质量为100%，并放入字节数组输出流中
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
		return baos.toByteArray();
	}
	
	
	/**
	 * 合并两个图片
	 * @param img1Path 图片1字节数组
	 * @param img2Path 图片2字节数组
	 * @return 字节数组
	 * @throws IOException 
	 */
	public static byte[] mergeImages(byte[] img1Path, byte[] img2Path) throws IOException {
		byte[] isSuccess = null;
		if (img1Path != null && img2Path != null) {
			ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(img1Path);
			Bitmap img1 = BitmapFactory.decodeStream(arrayInputStream);//读取图片1
			arrayInputStream.close();
			ByteArrayInputStream arrayInputStream_2 = new ByteArrayInputStream(img2Path);
			Bitmap img2 = BitmapFactory.decodeStream(arrayInputStream_2);//读取图片2
			arrayInputStream_2.close();
			//计算起始位置
			int x = (int) Math.round(img1.getWidth()/2.0 - img2.getWidth()/2.0);
			int y = (int) Math.round(img1.getHeight()/2.0 - img2.getHeight()/2.0);
			//以图片1尺寸和坐标为准
			isSuccess = drawNewImageInImage1(img1, img2, x, y);
		}
		return isSuccess;
	}
 
	/**
	 * 在图片1中画图片2
	 * @param x X坐标位置
	 * @param y Y坐标位置
	 * @param img1 图片1
	 * @param img2 图片2
	 * @return 字节数组
	 */
	private static byte[] drawNewImageInImage1(Bitmap img1, Bitmap img2, int x, int y) {
		Bitmap bitmap = Bitmap.createBitmap(img1.getWidth(), img1.getHeight(),img1.getConfig());
		Canvas canvas = new Canvas(bitmap);
		canvas.drawBitmap(img1,new Matrix(), null);
		canvas.drawBitmap(img2, x, y, null);
		int size = bitmap.getWidth() * img1.getHeight() * 4;
		// 创建一个字节数组输出流,流的大小为size
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(size);
		// 设置位图的压缩格式，质量为100%，并放入字节数组输出流中
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
		return outputStream.toByteArray();
	}
}
