package com.example.kingo.rtcclient;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by roy on 2018/4/16.
 * 用于SDP编解码器的设定
 */

public class SdpParser {
    private static String TAG = "SdpParser";
    public static String setExpectedCodec(String sdp, String codec, boolean isAudio) {
        //根据回车换行符过滤出所有的SDP行
        String[] lines = sdp.split("\r\n");
        //查找是否存在音频流（"m=audio xxxxxxxxxx"）
        int mLineIndex = findMediaDescriptionLine(isAudio, lines);
        if (mLineIndex == -1) {
            Log.w(TAG, "No mediaDescription line, so can't prefer " + codec);
            return sdp;
        }
        //查找编码器类型标识，知名的编码器有固定的类型标识（可百度搜索）
        // 一般类型标识实际上是96-127范围内的整数，以字符串形式表示而已
        final List<String> codecPayloadTypes = new ArrayList<>();
        //编码器类型标识所在SDP行的格式为：
        //a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        //我们采用pattern类型来进行正则表达式匹配，与sdp行进行循环匹配，
        //查找到自己指定的编码器,以及适应编码器的重传编码器，同时删除其他的编码器
        final Pattern codecPattern = Pattern.compile("^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$");
        for (String line : lines) {
            Matcher codecMatcher = codecPattern.matcher(line);
            if (codecMatcher.matches()) {
                codecPayloadTypes.add(codecMatcher.group(1));
            }
        }
        //此处匹配到对应的编码器编号最多只有一个
        if (codecPayloadTypes.isEmpty()) {
            Log.w(TAG, "No payload types with name " + codec);
            return sdp;
        }
        final Pattern rtxPattern = Pattern.compile("^a=fmtp:(\\d+) " + "apt=" + codecPayloadTypes.get(0) + "[\r]?$");
        final List<String> rtxPayloadTypes = new ArrayList<>();
        for (String line : lines) {
            Matcher rtxMatcher = rtxPattern.matcher(line);
            if (rtxMatcher.matches()) {
                rtxPayloadTypes.add(rtxMatcher.group(1));
            }
        }
        //接下来，保留此编码器，移除其余所有编码器
        //解析原始流行，（"m=audio 9 UDP/TLS/RTP/SAVPF 111 103 104 9 102 0 8 106 105 13 11"）
        //或（"m=video 9 UDP/TLS/RTP/SAVPF 96 98 100 127 125 97 99 101 124"）
        List<String> origLineParts = Arrays.asList(lines[mLineIndex].split(" "));
        if (origLineParts.size() <= 3) {
            Log.e(TAG, "Wrong SDP media description format: " + lines[mLineIndex]);
            return sdp;
        }
        List<String> header = origLineParts.subList(0, 3);//数据头不变
        //留下数据头，再配上一个指定的编码器id，去掉其余的所有编码器id，生成一个新的行，替换本行
        List<String> newLineParts = new ArrayList<>();
        newLineParts.addAll(header);
        newLineParts.addAll(codecPayloadTypes);
        newLineParts.addAll(rtxPayloadTypes);
        lines[mLineIndex] = joinString(newLineParts, " ", false);
        //得到不需要的所有编码器id
        List<String> unpreferredPayloadTypes =
                new ArrayList<>(origLineParts.subList(3, origLineParts.size()));
        unpreferredPayloadTypes.removeAll(codecPayloadTypes);
        unpreferredPayloadTypes.removeAll(rtxPayloadTypes);
        //循环删除所有不要的编码器属性行
        List<String> finalList = new ArrayList<>(Arrays.asList(lines));
        for (String unpreferredPayloadType : unpreferredPayloadTypes) {
            String a1 = "a=rtpmap:"+ unpreferredPayloadType;
            String a2 = "a=rtcp-fb:" + unpreferredPayloadType;
            String a3 = "a=fmtp:" + unpreferredPayloadType;
            Iterator<String> it = finalList.iterator();
            while (it.hasNext()) {
                String item = it.next();
                if (item.startsWith(a1) || item.startsWith(a2) || item.startsWith(a3)) {
                    it.remove();
                }
            }
        }
        return joinString(finalList, "\r\n", true);
    }

    /**
     * 查找是否存在指定的流（音频或视频）
     */
    private static int findMediaDescriptionLine(boolean isAudio, String[] sdpLines) {
        String mediaDescription = isAudio ? "m=audio " : "m=video ";
        for (int i = 0; i < sdpLines.length; ++i) {
            if (sdpLines[i].startsWith(mediaDescription)) {
                return i;
            }
        }
        return -1;
    }

    /**
     *将list拼接成string
     */
    private static String joinString(
			Iterable<? extends CharSequence> s, String delimiter, boolean delimiterAtEnd) {
        Iterator<? extends CharSequence> iter = s.iterator();
        if (!iter.hasNext()) {
            return "";
        }
        StringBuilder buffer = new StringBuilder(iter.next());
        while (iter.hasNext()) {
            buffer.append(delimiter).append(iter.next());
        }
        if (delimiterAtEnd) {
            buffer.append(delimiter);
        }
        return buffer.toString();
    }
}
