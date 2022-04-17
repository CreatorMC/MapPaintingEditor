package com.tmsstudio.mappaintingeditor.PicFactory;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class PicFactory {

	/**
	 * 处理图片
	 * @param path 图片路径
	 * @param forceSize 是否强制缩放 false:等比缩放
	 * @return
	 */
	public static byte[] convertPicToMinecraft(String path, boolean forceSize) {
		try {
			byte[] img, img_alpha;
			img = PicTool.resizeImage(path, 128, 128, forceSize);
			img_alpha = PicTool.generatePNG( 128, 128);
			img = PicTool.mergeImages(img_alpha, img);
			return img;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 处理图片（网格模式刚选择完图片时）
	 * @param path
	 * @return
	 */
	public static byte[] convertPicTo(String path) {
		try {
			byte[] img;
			img = PicTool.getImageBytes(path);
			return img;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 网格准备图片（写好行和列后触发）
	 * @param img
	 * @param row
	 * @param col
	 * @param unit
	 * @param force
	 * @throws IOException
	 */
	public static ArrayList<Bitmap> preparePicTo(byte[] img, int row, int col, int unit, boolean force) throws IOException {
		Bitmap bitmap = BitmapFactory.decodeByteArray(img, 0, img.length);
		int width = col * unit;
		int height = row * unit;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		if (force) {
			//强制缩放
			bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
		} else {
			//按比例缩放
			int width_yuan = bitmap.getWidth();
			int height_yuan = bitmap.getHeight();
			double scale = width_yuan / (double)width;	//缩放比例
			int ww = (int)(width_yuan/scale);
			int hh = (int)(height_yuan/scale);
			if(ww > width || hh > height){
				scale = height_yuan / (double)height;	//缩放比例
				ww = (int)(width_yuan/scale);
				hh = (int)(height_yuan/scale);
			}
			bitmap = Bitmap.createScaledBitmap(bitmap, ww, hh,true);
		}
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
		byte[] new_img = outputStream.toByteArray();
		byte[] img_alpha = PicTool.generatePNG(width,height);
		byte[] ans_img = PicTool.mergeImages(img_alpha, new_img);	//缩放完成的图片
		bitmap = BitmapFactory.decodeByteArray(ans_img, 0, ans_img.length);
		ArrayList<Bitmap> arrayList = new ArrayList<>();
		width = bitmap.getWidth();
		height = bitmap.getHeight();
		for (int j = 0; j < height; j += unit){
			for (int i = 0; i < width; i += unit){
				arrayList.add(Bitmap.createBitmap(bitmap, i, j, unit, unit));
			}
		}
		return arrayList;	//返回bitmap数组
	}

	/**
	 * 把图片数组转换成MC地图能识别的字节数组
	 * @param img 图片字节数组
	 * @return
	 */
	public static byte[] toMinecraftMap(byte[] img) {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(img);
		Bitmap image = BitmapFactory.decodeStream(inputStream);
		return toMinecraftMap(image);
	}

	/**
	 * 把图片数组转换成MC地图能识别的字节数组
	 * @param image Bitmap
	 * @return
	 */
	public static byte[] toMinecraftMap(Bitmap image) {
		int width = image.getWidth();
		int height = image.getHeight();
		int minX = 0;
		int minY = 0;
		byte[] rgb = new byte[65536];
		int index = 0;
		for (int y = minY; y < height; y++) {
			for (int x = minX; x < width; x++) {
				// 获取包含这个像素的颜色信息的值, int型
				int pixel = image.getPixel(x,y);
				// 从pixel中获取rgb的值
				rgb[index] = (byte)(pixel>>16);	//r
				index++;
				rgb[index] = (byte)(pixel>>8);	//g
				index++;
				rgb[index] = (byte)(pixel);	//b
				index++;
				rgb[index] = (byte)(pixel>>24);	//a
				index++;
			}
		}
		return rgb;
	}
}
