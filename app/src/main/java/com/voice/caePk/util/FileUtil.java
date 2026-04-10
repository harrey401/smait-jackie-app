package com.voice.caePk.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Luke on 2016/5/27.
 */
public class FileUtil {

	/**
	 * 读取asset目录下文件。
	 * @return content
	 */
	public static String readFile(Context mContext, String file, String code)
	{
		int len = 0;
		byte []buf = null;
		String result = "";
		try {
			InputStream in = mContext.getAssets().open(file);
			len  = in.available();
			buf = new byte[len];
			in.read(buf, 0, len);

			result = new String(buf,code);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static boolean writeFile2End(String path, String filename,
			String text) {
		String sdPath = getSDPath();
		if (sdPath == null)
			return false;
		text += "\r\n";
		FileOutputStream fos = null;
		try {
			path = sdPath + '/' + path;
			File file = new File(path);
			if (!file.exists()) {
				file.mkdirs();
			}
			fos = new FileOutputStream(path + "/" + filename, true);
			byte[] bytes = text.getBytes();
			fos.write(bytes);
			fos.flush();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			if (fos != null) {
				try {
					fos.close();
					fos = null;
				} catch (IOException e2) {

				}

			}

		}
		return true;
	}

	public static String getSDPath() {
		String sdPath = null;
		// 判断sd卡是否存在
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			sdPath = Environment.getExternalStorageDirectory().getPath();
		}
		return sdPath;
	}

	public static Bitmap getBimmapFromSD(String path) {
		try {
			FileInputStream fileInputStream = new FileInputStream(path);
			return BitmapFactory.decodeStream(fileInputStream);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static byte[] getBytesFromFile(String path) {
		byte[] bytes = null;
		InputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileInputStream(path);
			bytes = new byte[fileOutputStream.available()];
			fileOutputStream.read(bytes);
		} catch (IOException io) {
			io.printStackTrace();
		} finally {
			if (fileOutputStream != null) {
				try {
					fileOutputStream.close();
					fileOutputStream = null;
				} catch (IOException e) {

				}

			}
		}
		return bytes;
	}


	/**
	 * 复制asset文件到指定目录
	 * @param oldPath  asset下的路径
	 * @param newPath  SD卡下保存路径
	 */
	public static void CopyAssets2Sdcard(Context context, String oldPath, String newPath) {
		try {
			String fileNames[] = context.getAssets().list(oldPath);// 获取assets目录下的所有文件及目录名
			if (fileNames.length > 0) {// 如果是目录
				File file = new File(newPath);
				file.mkdirs();// 如果文件夹不存在，则递归
				for (String fileName : fileNames) {
					CopyAssets2Sdcard(context, oldPath + "/" + fileName, newPath + "/" + fileName);

				}
			} else {// 如果是文件
				InputStream is = context.getAssets().open(oldPath);
				FileOutputStream fos = new FileOutputStream(new File(newPath));
				byte[] buffer = new byte[1024];
				int byteCount = 0;
				while ((byteCount = is.read(buffer)) != -1) {// 循环从输入流读取
					// buffer字节
					fos.write(buffer, 0, byteCount);// 将读取的输入流写入到输出流
				}
				fos.flush();// 刷新缓冲区
				is.close();
				fos.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
