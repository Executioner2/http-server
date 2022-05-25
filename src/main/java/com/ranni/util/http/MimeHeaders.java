package com.ranni.util.http;

import com.ranni.util.buf.MessageBytes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;

/**
 * Title: HttpServer
 * Description:
 * 消息头部（请求头，响应头）
 * TODO: 存在的缺陷
 * XXX - 字段值的迭代会在非同名字段上浪费时间
 * XXX - 添加字段值时，只会返回第一个匹配上字段名的字段值缓冲区
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/25 19:10
 * @Ref org.apache.tomcat.util.http.MimeHeaders
 */
public class MimeHeaders {

    public static final int DEFAULT_HEADER_SIZE = 8; // 默认头部字段数量为8个
    
    private int count; // 字段数量
    private int limit = -1; // 字段数量限制，-1为不限制
    private MimeHeaderField[] headers = new MimeHeaderField[DEFAULT_HEADER_SIZE];


    /**
     * 字段数量限制，-1为不限制
     * 
     * @param limit 限制的字段数量
     */
    public void setLimit(int limit) {
        this.limit = limit;
        
        if (limit > 0 && headers.length > limit && count < limit) {
            // 扩充
            MimeHeaderField[] tmp = new MimeHeaderField[limit];
            System.arraycopy(headers, 0, tmp, 0, count);
            headers = tmp;
        }
    }

    
    public void recycle() {
        clear();
    }

    
    public void clear() {
        for (int i = 0; i < count; i++) {
            headers[i].recycle();
        }
        count = 0;
    }

    
    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("=== MimeHeaders ===");
        Enumeration<String> e = names();
        while (e.hasMoreElements()) {
            String n = e.nextElement();
            Enumeration<String> ev = values(n);
            while (ev.hasMoreElements()) {
                pw.print(n);
                pw.print(" = ");
                pw.println(ev.nextElement());
            }
        }
        return sw.toString();
    }


    public Enumeration<String> names() {
        return new NamesEnumerator(this);
    }

    
    public Enumeration<String> values(String name) {
        return new ValuesEnumerator(this, name);
    }


    /**
     * 在headers最后一个不为null的MimeHeaderField
     * 后面追加一个MimeHeaderField。如果需要扩充就扩
     * 充。
     * 
     * @return 返回新建的MimeHeaderField
     */
    private MimeHeaderField createHeader() {
        if (limit > -1 && count >= count) {
            throw new IllegalStateException("超出头部最大数量，limit = " + limit);
        }
        
        MimeHeaderField mhf;
        int len = headers.length;
        if (count >= len) {
            // 扩充
            int newLength = count * 2;
            if (limit > 0 && newLength > limit) {
                newLength = limit;
            }

            MimeHeaderField[] tmp = new MimeHeaderField[newLength];
            System.arraycopy(headers, 0, tmp, 0, len);
            headers = tmp;
        }
        
        // 如果不为null说明这个是被回收初始化后移动到有效字段后的待用字段
        if ((mhf = headers[count]) == null) {
            headers[count] = mhf = new MimeHeaderField();
        }
        count++;
        return mhf;
    }


    /**
     * 创建一个字段，设置字段名。然后返回这个字段的
     * MessageBytes类型字段值，便于添加字段值到
     * 缓冲区
     * 
     * @param name 字段名
     * @return 返回字段值缓冲区
     */
    public MessageBytes addValue(String name) {
        MimeHeaderField mhf = createHeader();
        mhf.getName().setString(name);
        return mhf.getValue();
    }


    /**
     * 创建一个字段，设置字段名。然后返回这个字段的
     * MessageBytes类型字段值，便于添加字段值到
     * 缓冲区
     * 
     * @param b 字节数组类型字段名
     * @param startN 字节数组的起始位置
     * @param len 数据长度
     * @return 返回字段值缓冲区
     */
    public MessageBytes addValue(byte[] b, int startN, int len) {
        MimeHeaderField mhf = createHeader();
        mhf.getName().setBytes(b, startN, len);
        return mhf.getValue();
    }


    /**
     * 返回指定字段名对应的字段值缓冲区，如果不存在则创
     * 建一个新的MimeHeaderFiled。
     * 与 {@link #addValue(String)} 的区别在于此
     * 方法会移除重名字段。
     * 
     * @param name 字段名
     * @return 返回字段值缓冲区
     */
    public MessageBytes setValue(String name) {
        for (int i = 0; i < count; i++) {
            if (headers[i].getName().equalsIgnoreCase(name)) {
                
                // 检查是否有重复名字的字段
                for (int j = i + 1; j < count; j++) {
                    if (headers[j].getName().equalsIgnoreCase(name)) {
                        removeHeader(j--);
                    }
                }
                
                return headers[i].getValue();
            }
        }
        
        MimeHeaderField mhf = createHeader();
        mhf.getName().setString(name);
        return mhf.getValue();
    }


    /**
     * 返回指定字段名对应的字段值缓冲区
     * 
     * @param name 字段名
     * @return 返回字段值缓冲区
     */
    public MessageBytes getValue(String name) {
        for (int i = 0; i < count; i++) {
            if (headers[i].getName().equalsIgnoreCase(name)) {
                return headers[i].getValue();
            }
        }
        
        return null;
    }


    /**
     * 返回一个唯一字段名的字段缓冲区。如果字段名不唯一，则抛
     * 出参数异常
     * 
     * @param name 字段名
     * @return 返回字段值缓冲区
     * @exception IllegalArgumentException 字段名不唯一
     */
    public MessageBytes getUniqueValue(String name) {
        MessageBytes result = null;

        for (int i = 0; i < count; i++) {
            if (headers[i].getName().equalsIgnoreCase(name)) {
                if (result == null) {
                    result = headers[i].getValue();
                } else {
                    throw new IllegalArgumentException();
                }
            }
        }
        
        return result;
    }


    /**
     * 返回指定字段名的字段值
     * 
     * @param name 字段名
     * @return 字段值
     */
    public String getHeader(String name) {
        MessageBytes value = getValue(name);
        return value != null ? value.toString() : null;
    }


    /**
     * 移除字段
     * 
     * @param name 要移除的字段名
     */
    public void removeHeader(String name) {
        for (int i = 0; i < count; i++) {
            if (headers[i].getName().equalsIgnoreCase(name)) {
                removeHeader(i--);
            }
        }
    }

    
    /**
     * 移除指定位置的字段。将后面的字段整体往前
     * 移。将此字段初始化后移动到headers末尾
     * 有效字段位置后一个位置
     * 
     * @param i 要移除的字段位置
     */
    private void removeHeader(int i) {
        MimeHeaderField mhf = headers[i];
        mhf.recycle();
        System.arraycopy(headers, i + 1, headers, i, count - i - 1);
        headers[count--] = mhf;
    }


    public int size() {
        return count;
    }

    
    public MessageBytes getName(int n) {
        return n >= 0 && n < count ? headers[n].getName() : null; 
    }

    
    public MessageBytes getValue(int n) {
        return n >= 0 && n < count ? headers[n].getValue() : null;
    }
}


/**
 * 头部中的字段
 */
class MimeHeaderField {
    private final MessageBytes nameB = MessageBytes.newInstance(); // 字段名
    private final MessageBytes valueB = MessageBytes.newInstance(); // 字段值

    public void recycle() {
        nameB.recycle();
        valueB.recycle();
    }

    public MessageBytes getName() {
        return nameB;
    }

    public MessageBytes getValue() {
        return valueB;
    }

    @Override
    public String toString() {
        return nameB + ": " + valueB;
    }
}


/**
 * 字段值的迭代器
 * XXX - 字段值的迭代会在非同名字段上浪费时间
 */
class ValuesEnumerator implements Enumeration<String> {
    private int pos;
    private final int size;
    private MessageBytes next;
    private final MimeHeaders headers;
    private final String name;

    
    public ValuesEnumerator(MimeHeaders headers, String name) {
        this.headers = headers;
        this.name = name;
        size = headers.size();
        findNext();
    }
    
    
    private void findNext() {
        next = null;
        for (; pos < size; pos++) {
            MessageBytes nameB = headers.getName(pos);
            if (nameB.equalsIgnoreCase(name)) {
                next = headers.getValue(pos);
                break;
            }
        }
        pos++;
    }

    
    @Override
    public boolean hasMoreElements() {
        return next != null;
    }

    
    @Override
    public String nextElement() {
        MessageBytes current = this.next;
        findNext();
        return current.toString();
    }
}


/**
 * 字段名迭代器
 */
class NamesEnumerator implements Enumeration<String> {
    private int pos;
    private final int size;
    private String next;
    private final MimeHeaders headers;

    public NamesEnumerator(MimeHeaders headers) {
        this.headers = headers;
        this.size = headers.size();
        findNext();
    }
    
    
    private void findNext() {
        next = null;

        for (; pos < size; pos++) {
            next = headers.getName(pos).getString();
            for (int i = 0; i < pos; i++) { // XXX - 属性名重复检测，可以优化
                if (headers.getName(i).equalsIgnoreCase(next)) {
                    // 属性名重复了
                    next = null;
                    break;
                }
                
                if (next != null) {
                    break;
                }
            }
        }
        
        pos++;
    }

    
    @Override
    public boolean hasMoreElements() {
        return next != null;
    }

    
    @Override
    public String nextElement() {
        String current = this.next;
        findNext();
        return current;
    }
}