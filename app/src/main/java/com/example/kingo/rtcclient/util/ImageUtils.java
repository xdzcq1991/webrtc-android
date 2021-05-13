package com.example.kingo.rtcclient.util;

import android.graphics.Bitmap;

/**
 * Created by roy on 2019/7/18.
 */

public class ImageUtils {

    public static byte[] getNV21(Bitmap bitmap, int imgWidth, int imgHeight) {
//        int widht = imgWidth % 2 == 0 ? imgWidth : imgWidth - 1;
//        int height = imgHeight % 2 == 0 ? imgHeight : imgHeight - 1;
        int widht = imgWidth;
        int height = imgHeight;
        int[] argb = new int[widht * height];
        bitmap.getPixels(argb, 0, widht, 0, 0, widht, height);
        byte[] yuv = new byte[widht * height * 3 / 2];
        encodeYUV420SP(yuv, argb, widht, height);
        bitmap.recycle();
        return yuv;
    }

    private static void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;
        int yIndex = 0;
        int uvIndex = frameSize;
        //int vIndex = frameSize ;//* 5 / 4;
        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;
                // well known RGB to YUV algorithm
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
//                if (j % 2 == 0 && index % 2 == 0 && uvIndex < yuv420sp.length - 2){
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                }
                index++;
            }
        }
    }

}
