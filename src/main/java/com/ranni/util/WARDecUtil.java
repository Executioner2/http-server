package com.ranni.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Title: HttpServer
 * Description:
 * war包解压工具
 * 
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/14 17:02
 */
public final class WARDecUtil {
    private static final int MAX_THREAD = 8; // 最大线程数
    private volatile static WARDecUtil warDecUtil; // WARDecUtil的实例
    private volatile static AtomicInteger count; // 原子整型，最多只有一个此实例，多个WARDecUtil实例访问的是同一个atomicInteger
    private AtomicInteger finishCount = new AtomicInteger(0); // 完成线程数量计数
    private ConcurrentLinkedDeque<File> files; // 需要解压的压缩文件

    private WARDecUtil() {}
    
    
    /**
     * 获取单例的WARDecUtil实例
     * 调用此方法，会重置值
     * 
     * @return
     */
    public static WARDecUtil getInstance() {
        if (warDecUtil == null) {
            synchronized (WARDecUtil.class) {
                if (warDecUtil == null) {
                    warDecUtil = new WARDecUtil();
                    count = new AtomicInteger(0);
                }
            }
        } else {
            warDecUtil.recycleAll();
        }
        
        return warDecUtil;
    }


    /**
     * 初始化所有
     */
    public void recycleAll() {
        count.set(0);
        finishCount.set(0);
        files.clear();
    }


    /**
     * 仅仅初始化一个实例内的变量
     */
    public void recycle() {
        finishCount.set(0);
        files.clear();
    }


    /**
     * 返回计数结果
     * 
     * @return
     */
    public int getCount() {
        synchronized (WARDecUtil.class) {
            return count.get();
        }
    }
    

    /**
     * 解压任务内部类
     */
    private class unzipTask implements Runnable {

        @Override
        public void run() {
            while (!files.isEmpty()) {
                try {
                    unzip(files.pop());
                } catch (NoSuchElementException e) {
                    // 抛出此异常是多线程的竞争，但是并无需加锁，直接跳出循环即可
                    break;
                }
            }

            finishCount.incrementAndGet();
        }
        
        
        /**
         * 正式解压压缩文件
         * 注意，重新解压的压缩文件会覆盖原来的内容
         * 但是，压缩文件中如果有文件被删除了，之前提取出来了的被删除的文件还是会存在于文件夹中
         * 
         * @param file
         */
        private void unzip(File file) {
            ZipFile zipFile = null;
            
            if (file.isDirectory() || !file.canRead())
                return;
            
            try {
                zipFile = new ZipFile(file);
            } catch (Exception e) {
                return;
            }

            String absolutePath = file.getAbsolutePath();
            String base = absolutePath.substring(0, absolutePath.lastIndexOf(".")) + File.separator;

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                File target = new File(base + zipEntry.getName());

                if (!zipEntry.isDirectory()) {
                    if (!target.exists()) {
                        try {
                            target.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    // 拷贝文件内容
                    try (InputStream is = zipFile.getInputStream(zipEntry);
                         FileOutputStream fos = new FileOutputStream(target)) {

                        byte[] buffer = new byte[1024 * 10]; // 10MB的读取
                        int len = -1;
                        while ((len = is.read(buffer, 0, buffer.length)) != -1) {
                            fos.write(buffer, 0, len);
                        }
                        fos.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else {
                    target.mkdirs();
                }

                // 统计
                count.incrementAndGet();                
            }

        }
    }
    

    /**
     * 解压压缩文件
     * 由于解压文件并不是一直都要进行的工作，所以创建的线程需要在用完之后就销毁
     * 
     * @param files 
     * @param background 是否开启后台线程解压，如果开启，会创建新的WARDecUtil实例并新开启一个线程进行解压工作
     */
    public void unzip(ConcurrentLinkedDeque<File> files, boolean background) {
        
        if (background) {
                       
            new Thread(new Runnable() {
                
                @Override
                public void run() {
                    WARDecUtil warDecUtil = new WARDecUtil();
                    warDecUtil.unzip(files, false);
                }
                
            }).start();            
            
        } else {
            
            this.files = files;
            int number = Math.min(files.size() / MAX_THREAD, MAX_THREAD);

            for (int i = 0; i < number; i++)
                new Thread(new unzipTask()).start();
            
            while (finishCount.get() < number) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            finishCount.set(0);                    
        }
        
    }
}
