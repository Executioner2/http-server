package com.ranni.util.buf;

import com.ranni.util.HexUtils;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * Title: HttpServer
 * Description:
 * 消息字节，内含多种类型的缓冲区
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/21 18:24
 */
public final class MessageBytes implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    
    private int type = T_NULL; // 消息类型
    
    public static final int T_NULL = 0; // 空
    public static final int T_STR = 1; // 字符串类型
    public static final int T_BYTES = 2; // 字节类型
    public static final int T_CHARS = 3; // 字符类型
    
    private final ByteChunk byteC = new ByteChunk();
    private final CharChunk charC = new CharChunk();
    private String strValue; // 计算后的str值
    private int hashCode;
    private boolean hasHashCode; // 是否计算了哈希值
    private long longValue;
    private boolean hasLongValue; // 是否表示的是64位类型
    
    
    private MessageBytes() {}
    

    /**
     * tomcat 9中使用了一个不完善工厂模式，
     * 目的是为了将来的升级，这里进行了简化
     * 
     * @return MessageBytes实例
     */
    public static MessageBytes newInstance() {
        return new MessageBytes();
    }
    
    
    public boolean isNull() {
        return strValue != null && byteC.isNull() && charC.isNull();
    }
    
    
    public void recycle() {
        type = T_NULL;
        strValue = null;
        byteC.recycle();
        charC.recycle();
        
        hasHashCode = false;
        hasLongValue = false;
    }


    /**
     * 设置数据
     * 
     * @param b 数据源
     * @param off 偏移量
     * @param len 数据长度
     */
    public void setBytes(byte[] b, int off, int len) {
        byteC.setBytes(b, off, len);
        type = T_BYTES;
        strValue = null;
        hasHashCode = false;
        hasLongValue = false;
    }


    /**
     * 设置数据
     * 
     * @param c 数据源
     * @param off 偏移量
     * @param len 数据长度
     */
    public void setChars(char[] c, int off, int len) {
        charC.setChars(c, off, len);
        type = T_CHARS;
        strValue = null;
        hasHashCode = false;
        hasLongValue = false;
    }

    
    /**
     * 设置数据
     * 
     * @param s 被设置的数据
     */
    public void setString(String s) {
        strValue = s;
        hasHashCode = false;
        hasLongValue = false;
        if (s == null) {
            type = T_NULL;
        } else {
            type = T_STR;
        }
    }
    
    
    @Override
    public String toString() {
        if (strValue != null) {
            return strValue;
        }
        
        if (type == T_BYTES) {
            strValue = byteC.toString();
        } else if (type == T_CHARS) {
            strValue = charC.toString();
        }
        
        return strValue;
    }


    /**
     * @return 消息类型
     */
    public int getType() {
        return type;
    }
    
    
    public ByteChunk getByteChunk() {
        return byteC;
    }
    
    
    public CharChunk getCharChunk() {
        return charC;
    }
    
    
    public String getString() {
        return strValue;
    }


    /**
     * @return 返回字节块的编码格式
     */
    public Charset getCharset() {
        return byteC.getCharset();
    }


    /**
     * 设置字节块的编码格式
     * 
     * @param charset 编码格式
     */
    public void setCharset(Charset charset) {
        byteC.setCharset(charset);
    }


    /**
     * 转字节块
     */
    public void toBytes() {
        if (isNull()) {
            return;
        }
        
        if (!byteC.isNull()) {
            type = T_BYTES;
            return;
        }
        
        toString();
        type = T_BYTES;
        Charset charset = byteC.getCharset();
        ByteBuffer encode = charset.encode(strValue);
        byteC.setBytes(encode.array(), encode.arrayOffset(), encode.limit());
    }


    /**
     * 转字符块
     */
    public void toChars() {
        if (isNull()) {
            return;
        }
        
        if (!charC.isNull()) {
            type = T_CHARS;
            return;
        }
        
        toString();
        type = T_CHARS;
        charC.setChars(strValue.toCharArray(), 0, strValue.length());
    }

    
    /**
     * @return 返回数据长度
     */
    public int getLength() {
        if(type == T_BYTES) {
            return byteC.getLength();
        }
        if(type == T_CHARS) {
            return charC.getLength();
        }
        if(type == T_STR) {
            return strValue.length();
        }
        
        toString();
        return strValue == null ? 0 : strValue.length();
    }


    /**
     * 比较数据内容
     * 
     * @param s 参与比较的数据
     * @return 返回比较结果 true或者false
     */
    public boolean equals(String s) {
        boolean res = false;
        
        if (type == T_STR) {
            res = strValue.equals(s);
        } else if (type == T_BYTES) {
            res = byteC.equals(s);
        } else if (type == T_CHARS) {
            res = charC.equals(s);
        }
        
        return res;
    }

    
    /**
     * 比较数据内容，忽略大小写
     * 
     * @param s 参与比较的数据
     * @return 返回比较结果 true或者false
     */
    public boolean equalsIgnoreCase(String s) {
        boolean res = false;

        if (type == T_STR) {
            res = strValue.equalsIgnoreCase(s);
        } else if (type == T_BYTES) {
            res = byteC.equalsIgnoreCase(s);
        } else if (type == T_CHARS) {
            res = charC.equalsIgnoreCase(s);
        }

        return res;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MessageBytes) {
            return equals((MessageBytes) obj);
        }
        return false;
    }

    
    public boolean equals(MessageBytes mb) {
        if (type == T_STR) {
            return mb.equals(strValue);
        }

        if(mb.type != T_CHARS && mb.type != T_BYTES) {
            return equals(mb.toString());
        }
        
        if( mb.type == T_CHARS && type == T_CHARS ) {
            return charC.equals(mb.charC);
        }
        if( mb.type == T_BYTES && type == T_BYTES ) {
            return byteC.equals(mb.byteC);
        }
        if( mb.type == T_CHARS && type == T_BYTES ) {
            return byteC.equals(mb.charC);
        }
        if( mb.type == T_BYTES && type == T_CHARS ) {
            return mb.byteC.equals(charC);
        }

        return false;
    }


    public boolean startsWithIgnoreCase(String s, int pos) {
        if (type == T_STR) {
            if (strValue == null) {
                return false;
            }
            if (strValue.length() < pos + s.length()) {
                return false;
            }

            for(int i=0; i < s.length(); i++) {
                if(Ascii.toLower(s.charAt(i)) != Ascii.toLower(strValue.charAt(pos + i))) {
                    return false;
                }
            }
            
            return true;
        } else if (type == T_CHARS) {
            return charC.startsWithIgnoreCase(s, pos);
        } else if (type == T_BYTES) {
            return byteC.startsWithIgnoreCase(s, pos);
        }
        
        return false;
    }


    @Override
    public int hashCode() {
        if( hasHashCode ) {
            return hashCode;
        }
        int code = hash();
        hashCode = code;
        hasHashCode = true;
        return code;
    }
    
    
    private int hash() {
        int hash = 0;
        
        if (type == T_STR) {
            for (int i = 0; i < strValue.length(); i++) {
                hash = hash * 37 + strValue.charAt(i);
            }
        } else if (type == T_BYTES) {
            hash = byteC.hash();
        } else if (type == T_CHARS) {
            hash = charC.hash();
        }
        
        return hash;
    }
    
    
    public int indexOf(String s, int starting) {
        toString();
        return strValue.indexOf(s, starting);
    }
    
    
    public int indexOf(String s) {
        return indexOf(s, 0);
    }
    
    
    public int indexOfIgnoreCase(String s, int starting) {
        toString();
        String upper = strValue.toUpperCase(Locale.ENGLISH);
        String _s = s.toUpperCase(Locale.ENGLISH);
        return upper.indexOf(_s, starting);
    }


    /**
     * 克隆数据<br>
     * 
     * 在为新的数据空间分配大小时，以src数据长度的2倍为初始化大小，可能会出
     * 现int溢出或超过 {@link AbstractChunk#ARRAY_MAX_SIZE} 的值。
     * 当int溢出时在创建数组会直接抛出异常，但是如果只是大于
     * {@link AbstractChunk#ARRAY_MAX_SIZE} 是得不到任何信息的。为
     * 了避免这种情况在 {@link ByteChunk#allocate(int, int)} 和
     * {@link CharChunk#allocate(int, int)} 做了边界判断处理，如果
     * 传入的初始化空间大小非法将抛出IllegalArgumentException参数异常。
     * 
     * @param src 源数据
     * @throws IOException 可能抛出I/O异常
     */
    public void duplicate(MessageBytes src) throws IOException {
        if (src.getType() == T_BYTES) {
            type = T_BYTES;
            ByteChunk bc = src.getByteChunk();
            byteC.allocate(2 * bc.getLength(), -1);
            byteC.append(bc);
        } else if (src.getType() == T_CHARS) {
            type = T_CHARS;
            CharChunk cc = src.getCharChunk();
            charC.allocate(2 * cc.getLength(), -1);
            charC.append(cc);
        } else if (src.getType() == T_STR) {
            type = T_STR;
            setString(src.getString());
        }
        
        setCharset(src.getCharset());
    }


    /**
     * 设置长整型数据
     * 
     * @param l 设置的数据
     */
    public void setLong(long l) {
        byteC.allocate(32, 64);
        
        long current = l;
        byte[] buf = byteC.getBuffer();
        int start = 0;
        int end = 0;
        if (l == 0) {
            buf[end++] = (byte) '0';
        }
        if (l < 0) {
            current = -l;
            buf[end++] = (byte) '-';
        }
        while (current > 0) { // 计算出来的数字是倒叙的
            int digit = (int) (current % 10);
            current = current / 10;
            buf[end++] = HexUtils.getHex(digit);
        }
        byteC.setOffset(0);
        byteC.setEnd(end);
        
        // 将倒叙的数字反转为正
        end--;
        if (l < 0) {
            start++;
        }        
        while (end > start) {
            buf[start] ^= buf[end];
            buf[end] ^= buf[start];
            buf[start] ^= buf[end];            
            start++; end--;
        }
        
        longValue = l;
        strValue = null;
        hasHashCode = false;
        hasLongValue = true;
        type = T_BYTES;
    }


    /**
     * @return 返回长整型值
     */
    public long getLong() {
        if(hasLongValue) {
            return longValue;
        }

        if (type == T_BYTES) {
            longValue = byteC.getLong();
        } else {
            longValue = Long.parseLong(toString());
        }

        hasLongValue = true;
        
        return longValue;
    }
    
}
