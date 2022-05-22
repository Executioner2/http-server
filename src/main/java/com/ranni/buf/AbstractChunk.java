package com.ranni.buf;

import java.io.Serializable;

/**
 * Title: HttpServer
 * Description:
 * 抽象的数据块，定义了不同类型数据块的通用方法  
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/21 18:32
 */
public abstract class AbstractChunk implements Cloneable, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 在JVM上，数组的大小会被限制到Integer.MAX_VALUE。
     * 在markt桌面上会被限制到Integer.MAX_VALUE - 2。
     * 而在ArrayList的注释上写有在某些系统上会限制到Integer.MAX_VALUE - 8。
     * 
     * @see {@link java.util.ArrayList#MAX_ARRAY_SIZE} 
     */
    public static final int ARRAY_MAX_SIZE = Integer.MAX_VALUE - 8;
    
    private int limit = -1; // 空间大小上限
    private int hashCode;
    
    protected int start; // 数据块在缓冲区中的起始位置
    protected int end; // 数据块在缓冲区中的结束位置    
    protected boolean isSet; // 是否已经分配了数据块在缓冲区的区间
    protected boolean hasHashCode; // 是否参数哈希计算

    
    public void setLimit(int limit) {
        this.limit = limit;
    }


    public int getLimit() {
        return limit;
    }


    /**
     * 如果limit <= 0，那么将ARRAY_MAX_SIZE作为空间大小上限
     * 否则返回limit
     * 
     * @return 返回真实的空间大小上限。
     */
    protected int getLimitInternal() {
        if (limit > 0) {
            return limit;
        } else {
            return ARRAY_MAX_SIZE;
        }
    }


    /**
     * @return 返回数据在缓冲区中的起始位置
     */
    public int getStart() {
        return start;
    }


    /**
     * @return 返回数据在缓冲区中的结束位置
     */
    public int getEnd() {
        return end;
    }


    /**
     * 设置数据在缓冲区（buffer）中的结束位置
     * 
     * @param i 数据在缓冲区中的结束位置
     */
    public void setEnd(int i) {
        end = i;
    }


    /**
     * 设置数据在缓冲区（buffer）中的起始位置
     * 
     * @param i 数据在缓冲区中的起始位置
     */
    public void setStart(int i) { start = i; }


    /**
     * @return 返回数据在缓冲区中的偏移量（其实就是数据块的start）
     */
    public int getOffset() {
        return start;
    }


    /**
     * 设置数据在缓冲区中的偏移量
     * 如果end小于传入的off，那么将off作为end新的结束位置
     * 
     * @param off 数据在缓冲区中的偏移量
     */
    public void setOffset(int off) {
        if (end < off) {
            end = off;
        }
        start = off;
    }


    /**
     * @return 返回数据块的长度
     */
    public int getLength() {
        return end - start;
    }


    /**
     * 是否还未给此数据块进行分配
     * 
     * @return end <= 0 && isSet == false
     */
    public boolean isNull() {
        if (end > 0) {
            return false;
        }
        return !isSet;
    }


    /**
     * @return 返回数据块的哈希值
     */
    @Override
    public int hashCode() {
        if (!hasHashCode) {
            hashCode = hash();
            hasHashCode = true;
        }
        
        return hashCode;
    }


    /**
     * 对数据块进行哈希计算
     * 
     * @return 一个int类型的哈希值
     */
    public int hash() {
        int code = 0;
        for (int i = start; i < end; i++) {
            code = code * 37 + getBufferElement(i);
        }
        return code;
    }


    /**
     * 初始化，便于重复使用
     * 初始化的值：
     * hasHashCode = false;
     * isSet = false;
     * start = 0;
     * end = 0;
     */
    public void recycle() {
        hasHashCode = false;
        isSet = false;
        start = 0;
        end = 0;
    }
    

    /**
     * 源字符串中的一段在数据块中的位置
     * XXX - 考虑是否改为KMP
     * 
     * @param src 源字符串
     * @param srcOff 源字符串的起始位置
     * @param srcLen 源字符串要参与匹配的长度
     * @param myOff 此数据块在缓冲区中的偏移量
     * @return 返回匹配到的起始位置，如果没有匹配上的返回 -1
     */
    public int indexOf(String src, int srcOff, int srcLen, int myOff) {
        char first = src.charAt(srcOff);
        
        int srcEnd = srcOff + srcLen;

        mainLoop: 
        for (int i = myOff + start; i <= (end - srcLen); i++) {
            if (getBufferElement(i) != first) {
                continue;
            }

            int myPos = i + 1;
            for (int srcPos = srcOff + 1; srcPos < srcEnd;) {
                if (getBufferElement(myPos++) != src.charAt(srcPos++)) {
                    continue mainLoop;
                }
            }
            return i - start;
        }
        
        return -1;
    }
    
    
    /**
     * 返回缓冲区中指定下标的元素
     * 
     * @param index 下标
     * @return 返回缓冲区中指定下标的元素
     */
    protected abstract int getBufferElement(int index);
}
