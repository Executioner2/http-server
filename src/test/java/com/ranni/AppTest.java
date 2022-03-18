package com.ranni;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit test for simple App.
 */
public class AppTest {
    @Test
    public void test() {
        int a = 68;
        System.out.println((a >= 'A' && a <= 'Z'));
    }

    @Test
    public void stringReplaceTest() {
        String uri = "//lani.com//servlet/getName/";

        System.out.println(uri.lastIndexOf("/"));
        System.out.println(uri.lastIndexOf("/", 9));
    }

    @Test
    public void hntTest() {
//        List<String> A = new ArrayList<>(Arrays.asList("1a", "1b"));
//        List<String> A = new ArrayList<>(Arrays.asList("2a", "2b", "1a", "1b"));
//        List<String> A = new ArrayList<>(Arrays.asList("3a", "3b", "2a", "2b", "1a", "1b"));
//        List<String> A = new ArrayList<>(Arrays.asList("4a", "4b", "3a", "3b", "2a", "2b", "1a", "1b"));
        List<String> A = new ArrayList<>(Arrays.asList("5a", "5b", "4a", "4b", "3a", "3b", "2a", "2b", "1a", "1b"));
        List<String> B = new ArrayList<>();
        List<String> C = new ArrayList<>();

        System.out.println("原始数据：");
        System.out.println("A  " + A);
        System.out.println("B  " + B);
        System.out.println("C  " + C);

        hnt(A.size(), A, B, C); // 先将所有盘子转移到柱子C上
        fl(C.size(), A, B, C, true); // 进行分离

        System.out.println("\n结果：");
        System.out.println("A  " + A);
        System.out.println("B  " + B);
        System.out.println("C  " + C);
    }

    /**
     * 分离不同颜色的盘子
     * @param count 需要转移的盘子数量
     * @param A 柱子A
     * @param B 柱子B
     * @param C 柱子C
     * @param isFirst 是否是第一次转移
     */
    public void fl (int count, List<String> A, List<String> B, List<String> C, boolean isFirst) {
        if (count <= 2) {
            B.add(C.remove(C.size() - 1)); // 将最后一个C的头盖骨转移到B上
            return;
        }

        hnt (count - 2, C, B, A); // 将C(n-2)移动到A上
        B.add(C.remove(C.size() - 1)); // 将C的头盖骨转移到B上

        if (isFirst) {
            hnt (count - 2, A, C, B); // 只有第一次才将A移动到B上
            fl(count - 2, A, C, B, false); // 只有第一次才两极反转
        } else {
            hnt (count - 2, A, B, C); // 将A移动到C上
            fl(count - 2, A, B, C, false); // 正常递归两极反转
        }
    }

    /**
     * 将指定柱子上指定的盘子转移到指定柱子上
     * @param count 剩余盘子数
     * @param A 柱子A
     * @param B 柱子B
     * @param C 柱子C
     */
    public void hnt(int count, List<String> A, List<String> B, List<String> C) {
        if (count <= 2) {
            C.add(A.remove(A.size() - 1));
            C.add(A.remove(A.size() - 1));
            return;
        }
        hnt(count - 2, A, C, B);
        hnt(2, A, B, C);
        hnt(count - 2, B, A, C);
    }

    /**
     * 将两个有序数组合并
     */
    @Test
    public void mergeTest() {
        List<Integer> a = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6));
        List<Integer> b = new ArrayList<>(Arrays.asList(4, 7, 8, 10));

        List<Integer> ars = new ArrayList<>(a.size() + b.size());
        int ptr1 = 0, ptr2 = 0, min;

        while (ptr1 < a.size() && ptr2 < b.size()) {
            if (a.get(ptr1) <= b.get(ptr2)) {
                min = a.get(ptr1++);
            } else {
                min = b.get(ptr2++);
            }
            ars.add(min);
        }

        if (ptr1 >= a.size()) {
            for (int i = ptr2; i < b.size(); i++) {
                ars.add(b.get(i));
            }
        } else {
            for (int i = ptr1; i < a.size(); i++) {
                ars.add(a.get(i));
            }
        }

        System.out.println(ars);
    }

    @Test
    public void encodingTest() throws UnsupportedEncodingException {
        String str = "name";
        byte[] bytes = str.getBytes();

        System.out.println(str);
        System.out.println(bytes);

        System.out.println("编码：" + new String(bytes, 0, 4, "ISO-8859-1"));
    }

    @Test
    public void arsTest() {
        int[] ars = {1, 2, 3};
        int i = 0;

        System.out.println(ars[i++] + ars[i++]);
    }

}
