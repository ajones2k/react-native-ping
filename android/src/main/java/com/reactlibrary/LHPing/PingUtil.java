package com.reactlibrary.LHPing;

import android.util.ArrayMap;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 类描述:手机ping工具类<br>
 * 权限 <uses-permission android:name="android.permission.INTERNET"/> <br>
 * 由于涉及网络，建议异步操作<br>
 */
public class PingUtil {

    private static final String ipRegex =
            "((?:(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(?:25[0-5]|2[0-4]\\d|(" +
                    "(1\\d{2})|([1-9]?\\d))))";

    /**
     * 获取由ping url得到的IP地址
     *
     * @param url 需要ping的url地址
     * @return url的IP地址 如 192.168.0.1
     */
    public static String getIPFromUrl(String url) {
        String domain = getDomain(url);
        if (null == domain) {
            return null;
        }
        if (isMatch(ipRegex, domain)) {
            return domain;
        }
        String pingString = ping(createSimplePingCommand(1, 100, domain));
        if (null != pingString) {
            try {
                String tempInfo = pingString.substring(pingString.indexOf("from") + 5);
                return tempInfo.substring(0, tempInfo.indexOf("icmp_seq") - 2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 获取ping最小RTT值
     *
     * @param url 需要ping的url地址
     * @return 最小RTT值，单位 ms 注意：-1是默认值，返回-1表示获取失败
     */
    public static int getMinRTT(String url) {
        return getMinRTT(url, 1, 100);
    }

    /**
     * 获取ping的平均RTT值
     *
     * @param url 需要ping的url地址
     * @return 平均RTT值，单位 ms 注意：-1是默认值，返回-1表示获取失败
     */
    public static WritableMap getAvgRTT(String url) {
        return getAvgRTT(url, 1, 100, 25);
    }

    /**
     * 获取ping的最大RTT值
     *
     * @param url 需要ping的url地址
     * @return 最大RTT值，单位 ms 注意：-1是默认值，返回-1表示获取失败
     */
    public static int getMaxRTT(String url) {
        return getMaxRTT(url, 1, 100);
    }

    /**
     * 获取ping的RTT的平均偏差
     *
     * @param url 需要ping的url地址
     * @return RTT平均偏差，单位 ms 注意：-1是默认值，返回-1表示获取失败
     */
    public static int getMdevRTT(String url) {
        return getMdevRTT(url, 1, 100);
    }

    /**
     * 获取ping url的最小RTT
     *
     * @param url     需要ping的url地址
     * @param count   需要ping的次数
     * @param timeout 需要ping的超时，单位ms
     * @return 最小RTT值，单位 ms 注意：-1是默认值，返回-1表示获取失败
     */
    public static int getMinRTT(String url, int count, int timeout) {
        String domain = getDomain(url);
        if (null == domain) {
            return -1;
        }
        String pingString = ping(createSimplePingCommand(count, timeout, domain));
        if (null != pingString) {
            try {
                String tempInfo = pingString.substring(pingString.indexOf("min/avg/max/mdev") + 19);
                String[] temps = tempInfo.split("/");
                return Math.round(Float.valueOf(temps[0]));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    /**
     * 获取ping url的平均RTT
     *
     * @param domain  需要ping的domain
     * @param count   需要ping的次数
     * @param timeout 需要ping的超时时间，单位 ms
     * @return 平均RTT值，单位 ms 注意：-1是默认值，返回-1表示获取失败
     */
    public static WritableMap getAvgRTT(String domain, int count, int timeout, int ttl) {
        String cmd = "/system/bin/ping -c " + count + " -W " + timeout + " -t " + ttl + " " + domain;
        // String pingString = pingNf(cmd);
        // String tempInfo = pingString.substring(pingString.indexOf("min/avg/max/mdev")
        // + 19);
        // String[] temps = tempInfo.split("/");
        return pingNf(cmd); // Math.round(Float.valueOf(temps[1]));
    }

    /**
     * 获取ping url的最大RTT
     *
     * @param url     需要ping的url地址
     * @param count   需要ping的次数
     * @param timeout 需要ping的超时时间，单位ms
     * @return 最大RTT值，单位 ms 注意：-1是默认值，返回-1表示获取失败
     */
    public static int getMaxRTT(String url, int count, int timeout) {
        String domain = getDomain(url);
        if (null == domain) {
            return -1;
        }
        String pingString = ping(createSimplePingCommand(count, timeout, domain));
        if (null != pingString) {
            try {
                String tempInfo = pingString.substring(pingString.indexOf("min/avg/max/mdev") + 19);
                String[] temps = tempInfo.split("/");
                return Math.round(Float.valueOf(temps[2]));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    /**
     * 获取RTT的平均偏差
     *
     * @param url     需要ping的url
     * @param count   需要ping的次数
     * @param timeout 需要ping的超时时间，单位ms
     * @return RTT平均偏差，单位 ms 注意：-1是默认值，返回-1表示获取失败
     */
    public static int getMdevRTT(String url, int count, int timeout) {
        String domain = getDomain(url);
        if (null == domain) {
            return -1;
        }
        String pingString = ping(createSimplePingCommand(count, timeout, domain));
        if (null != pingString) {
            try {
                String tempInfo = pingString.substring(pingString.indexOf("min/avg/max/mdev") + 19);
                String[] temps = tempInfo.split("/");
                return Math.round(Float.valueOf(temps[3].replace(" ms", "")));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    /**
     * 获取ping url的丢包率，浮点型
     *
     * @param url 需要ping的url地址
     * @return 丢包率 如50%可得 50，注意：-1是默认值，返回-1表示获取失败
     */
    public static float getPacketLossFloat(String url) {
        String packetLossInfo = getPacketLoss(url);
        if (null != packetLossInfo) {
            try {
                return Float.valueOf(packetLossInfo.replace("%", ""));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    /**
     * 获取ping url的丢包率，浮点型
     *
     * @param url     需要ping的url地址
     * @param count   需要ping的次数
     * @param timeout 需要ping的超时时间，单位 ms
     * @return 丢包率 如50%可得 50，注意：-1是默认值，返回-1表示获取失败
     */
    public static float getPacketLossFloat(String url, int count, int timeout) {
        String packetLossInfo = getPacketLoss(url, count, timeout);
        if (null != packetLossInfo) {
            try {
                return Float.valueOf(packetLossInfo.replace("%", ""));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    /**
     * 获取ping url的丢包率
     *
     * @param url 需要ping的url地址
     * @return 丢包率 x%
     */
    public static String getPacketLoss(String url) {
        return getPacketLoss(url, 1, 100);
    }

    /**
     * 获取ping url的丢包率
     *
     * @param url     需要ping的url地址
     * @param count   需要ping的次数
     * @param timeout 需要ping的超时时间，单位ms
     * @return 丢包率 x%
     */
    public static String getPacketLoss(String url, int count, int timeout) {
        String domain = getDomain(url);
        if (null == domain) {
            return null;
        }
        String pingString = ping(createSimplePingCommand(count, timeout, domain));
        if (null != pingString) {
            try {
                String tempInfo = pingString.substring(pingString.indexOf("received,"));
                return tempInfo.substring(9, tempInfo.indexOf("packet"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    // ********************以下是一些辅助方法********************//
    private static String getDomain(String url) {
        String domain = null;
        try {
            domain = URI.create(url).getHost();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return domain;
    }

    private static boolean isMatch(String regex, String string) {
        return Pattern.matches(regex, string);
    }

    private static boolean getTtlEx(String respPing) {
        Pattern p = Pattern.compile("Time to live exceeded");
        Matcher m = p.matcher(respPing);
        if (m.find()) {
            return true;
        }
        return false;
    }

    private static boolean getUnkHost(String respErr) {
        Pattern p = Pattern.compile("unknown host");
        Matcher m = p.matcher(respErr);
        if (m.find()) {
            return true;
        }
        return false;
    }

    private static String getIP(String respPing) {
        Pattern p = Pattern.compile("[(][0-9]+[.][0-9]+[.][0-9]+[.][0-9]+[)]");
        Matcher m = p.matcher(respPing);
        if (m.find()) {
            String ret = respPing.substring(m.start() + 1, m.end() - 1);
            return ret;
        }
        return "";
    }


    private static String getFromIp(String respPing) {
        Pattern p = Pattern.compile("From [0-9]+[.][0-9]+[.][0-9]+[.][0-9]+");
        Matcher m = p.matcher(respPing);
        if (m.find()) {
            String ret = respPing.substring(m.start() + 5, m.end());
            return ret;
        }
        return "";
    }

    private static String getRtt(String respPing) {
        Pattern p = Pattern.compile("time=[0-9]+[.][0-9]");
        Matcher m = p.matcher(respPing);
        if (m.find()) {
            String ret = respPing.substring(m.start() + 5, m.end());
            return ret;
        }
        return "";
    }
    
    private static WritableMap pingNf(String command) {
        Process process = null;
        long startTime = System.currentTimeMillis();
        try {
            process = Runtime.getRuntime().exec(command);
            InputStream is = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            InputStream es = process.getErrorStream();
            BufferedReader errreader = new BufferedReader(new InputStreamReader(es));
            StringBuilder sb = new StringBuilder("");
            StringBuilder eb = new StringBuilder("");
            String line;
            String eline;
            // Log.d("DEBUG","Try reading");
            while (null != (line = reader.readLine())) {
                sb.append(line);
                sb.append("\n");
            }
            while (null != (eline = errreader.readLine())) {
                eb.append(eline);
                eb.append("\n");
            }
            long endtime = System.currentTimeMillis();
            reader.close();
            errreader.close();
            is.close();
            es.close();
            Log.w("DEBUG",eb.toString());
            long delta = endtime - startTime;
            WritableMap map = Arguments.createMap();
            String respPing = sb.toString();
            String respErr = eb.toString();
            String rtt = getRtt(respPing);
            String ipAddr = getIP(respPing);
            boolean ttlEx = getTtlEx(respPing);
            boolean unkHost = getUnkHost(respErr);
            String orgRtt = rtt;
            String matchesAddress = "0";
            if (respErr.length() > 2) {
                rtt = "-1";
                if( unkHost ) {
                    matchesAddress = "2";
                } else {
                    matchesAddress = "0";
                }
                
            } else {
                if (rtt.length() == 0) {
                    if( !ttlEx ) {
                        rtt = "-1";
                    }
                    else {
                        rtt = Long.toString(delta);
                    }
                } else {
                    matchesAddress = "1";
                }
            }
            map.putString("respPing", respPing);
            map.putString("respErr", respErr);
            map.putString("rtt", rtt);
            map.putString("fromAddr", getFromIp(respPing));
            map.putString("calTime", Long.toString(delta));
            map.putString("matchesAddress", matchesAddress);
            map.putString("orgTtl", orgRtt);
            map.putString("ipAddr", ipAddr);
            return map;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != process) {
                process.destroy();
            }
        }

        WritableMap map = Arguments.createMap();
        map.putString("respPing", "fail");
        map.putString("respErr", "fail");
        return map;
    }

    private static String ping(String command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            InputStream is = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while (reader.ready() && null != (line = reader.readLine())) {
                sb.append(line);
                sb.append("\n");
            }
            reader.close();
            is.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != process) {
                process.destroy();
            }
        }
        return null;
    }

    private static String ping(String command, int timeout) {
        Process process = null;
        long startTime = System.currentTimeMillis();
        try {
            process = Runtime.getRuntime().exec(command);
            InputStream is = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            boolean isBreak = false;
            while (true) {
                long currentTime = System.currentTimeMillis();
                if (isBreak || (currentTime - startTime > timeout)) {
                    break;
                }
                if (reader.ready()) {
                    while (null != (line = reader.readLine())) {
                        sb.append(line);
                        sb.append("\n");
                    }
                    isBreak = true;
                }
            }
            reader.close();
            is.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != process) {
                process.destroy();
            }
        }
        return null;
    }

    private static String createSimplePingCommand(int count, int timeout, String domain) {
        return "/system/bin/ping -c " + count + " -w " + timeout + " " + domain;
    }

    private static String createPingCommand(ArrayMap<String, String> map, String domain) {
        String command = "/system/bin/ping";
        int len = map.size();
        for (int i = 0; i < len; i++) {
            command = command.concat(" " + map.keyAt(i) + " " + map.get(map.keyAt(i)));
        }
        command = command.concat(" " + domain);
        return command;
    }
}