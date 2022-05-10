package com.ranni.handler;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/9 22:03
 */
public final class JSONUtil {

    /**
     * 基本数据类型和String的类名hash码
     * 
     * 0: byte      java.lang.Byte
     * 1: char      java.lang.Character
     * 2: short     java.lang.Short
     * 3: int       java.lang.Integer
     * 4: long      java.lang.Long
     * 5: float     java.lang.Float
     * 6: double    java.lang.Double
     * 7: java.lang.String
     */
    public static final int[][] db = {
            {"byte".hashCode(), "java.lang.Byte".hashCode()},
            {"char".hashCode(), "java.lang.Character".hashCode()},
            {"short".hashCode(), "java.lang.Short".hashCode()},
            {"int".hashCode(), "java.lang.Integer".hashCode()},
            {"long".hashCode(), "java.lang.Long".hashCode()},
            {"float".hashCode(), "java.lang.Float".hashCode()},
            {"double".hashCode(), "java.lang.Double".hashCode()},
            {"java.lang.String".hashCode()},
    };

    private JSONUtil() {}


    /**
     * 解析JSON字符串
     *
     * @param obj   要解析为的实例
     * @param json  json字符串
     */
    public static void parseJSON(Object obj, String json) throws JSONException {
        parseJSON(obj, json.toCharArray());
    }
    

    /**
     * 解析JSON字符串
     *
     * @param obj   要解析为的实例
     * @param json  json字符数组
     */
    public static void parseJSON(Object obj, char[] json) throws JSONException {
        parseJSON(obj, json, 0);
    }
    
    
    /**
     * 解析JSON字符串
     * 用递归的方式
     *
     * @param obj   要解析为的实例
     * @param json  json字符数组
     * @param start 解析的起始位置
     *              
     * @return 返回解析到了json字符数组的位置
     */
    private static int parseJSON(Object obj, char[] json, int start) throws JSONException {
        if (obj == null) return start;
        
        json = normalize(json);
        
        if (json[start] != '{') {
            throw new JSONException("StandardServlet.parseJson  json格式异常！");
        }

        int i = start + 1;
        boolean space; // 字段中间是否出现了空格
        StringBuffer sb = new StringBuffer();

        while (i < json.length && json[i] != '}') {
            int j = i;
            sb.setLength(0);
            space = false;

            if (json[j++] != '"') {
                throw new JSONException("StandardServlet.parseJson  json格式异常！");
            }

            while (j < json.length && json[j] != '"') {
                if (json[j] == ' ') {
                    space = sb.length() > 0;
                    continue;
                } else if (space) {
                    throw new JSONException("StandardServlet.parseJson  json字段名格式错误！");
                }
                sb.append(json[j++]);
            }

            // 消除空行
            while (j < json.length && (json[j] == ' ' || json[j] == '\t')) j++;

            if (j == json.length || json[j++] != ':') {
                throw new JSONException("StandardServlet.parseJson  json格式异常！");
            }

            String key = sb.toString();
            Field field = null;
            try {
                field = obj.getClass().getDeclaredField(key);
            } catch (NoSuchFieldException e) {
                continue;
            }

            // 解析value
            Object val = null;
            
            // 消除空行
            while (j < json.length && (json[j] == ' ' || json[j] == '\t')) j++;

            if (j == json.length) {
                throw new JSONException("StandardServlet.parseJson  json格式异常！");
            }
            
            if (json[j] == '"') {
                // 是字符串
                sb.setLength(0);
                j++;
                
                while (j < json.length && !(json[j - 1] != '\\' && json[j] == '"')) {
                    sb.append(json[j++]);
                }

                // 消除空行
                while (j < json.length && (json[j] == ' ' || json[j] == '\t')) j++;
                if (j < json.length && json[j++] != ',') {
                    throw new JSONException("StandardServlet.parseJson  json格式异常！");
                }
                
                val = sb.toString();
                
            } else if (json[j] == '[') {
                // 需求分析：
                //  1、可以自动推断出数组元素的类型
                //  2、可以
                // 是数组
                // 先取得数组元素的类型
                Type type = field.getGenericType();
                Class aClass = null;
                
                if (type instanceof ParameterizedType) {
                    
                    try {
                        aClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];    
                    } catch (Throwable t) {
                        t.printStackTrace(System.err);
                    }         
                    
                } else {
                    System.err.println("集合类型字段应该指明泛型！ filed: " + field);
                }

                // 消除空行
                while (j < json.length && (json[j] == ' ' || json[j] == '\t')) j++;
                if (j == json.length) {
                    throw new JSONException("StandardServlet.parseJson  json格式异常！");
                }
                
                if (aClass != null) {
                    try {
                        val = aClass.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    } 
                }
                val = new JSONArrayList<>(); // XXX - 这里考虑自定义集合
                
                if (val != null) {
                    // 找得到集合元素的类型，开始解析
//                    List<>
                } else {
                    // 找不到，那么就跳过这个字段的值
                    while (j < json.length && json[j++] != ']');
                }

                // 消除空行
                while (j < json.length && (json[j] == ' ' || json[j] == '\t')) j++;
                if (j < json.length && json[j++] != ',') {
                    throw new JSONException("StandardServlet.parseJson  json格式异常！");
                }
                
            } else if (json[j] == '{') {
                // 是嵌套的json
            } else {
                // 是基本数据类型，根据字段类型进行转换

            }

            try {
                field.set(obj, val);
            } catch (IllegalAccessException e) {
                e.printStackTrace(System.err);
            }
            
            i = j;

        }
        
        return i;
        
    }


    /**
     * 标准化json字符串格式
     * 消除那些智障空格，空行
     * 让json字符串变为一行紧凑的json字符串
     *
     * @param json
     * @return
     */
    public static char[] normalize(String json) throws JSONException {
        return normalize(json.toCharArray());
    }
    

    /**
     * 标准化json字符串格式
     * 消除那些智障空格，空行
     * 让json字符串变为一行紧凑的json字符串
     * 
     * @param json
     * @return
     */
    public static char[] normalize(char[] json) throws JSONException {
        int len = normalize(json, 0, 0, false)[0];
        
        char[] res = new char[len];
        System.arraycopy(json, 0, res, 0, len);
        
        return res;
    }


    /**
     * 标准化json字符串格式
     * 返回覆盖到的位置
     * 
     * @param json
     * @param slowPtr
     * @param fastPtr
     * @param isCommon 是否是基本数据类型或字符串
     * @return
     * @throws JSONException
     */
    public static int[] normalize(char[] json, int slowPtr, int fastPtr, boolean isCommon) throws JSONException {

        while (fastPtr < json.length && json[fastPtr] != '}') {
            if (isCommon) {
                // 消除前导空白
                while (fastPtr < json.length && isBlank(json[fastPtr])) fastPtr++;

                boolean isStr = fastPtr < json.length && json[fastPtr] == '"';
                
                while (fastPtr < json.length && !isEnd(isStr, json[fastPtr - 1], json[fastPtr])) {
                    json[slowPtr++] = json[fastPtr++];
                }
                
                if (fastPtr < json.length && json[fastPtr] == '"') 
                    fastPtr++;
                
                // 消除后导空白
                while (fastPtr < json.length && isBlank(json[fastPtr])) fastPtr++;
                
                if (fastPtr < json.length && json[fastPtr] == ',')
                    json[slowPtr++] = json[fastPtr++];
                
                return new int[]{slowPtr, fastPtr};
                
            } else {
                // key
                while (fastPtr < json.length && json[fastPtr] != ':') {
                    if (!isBlank(json[fastPtr]) && json[fastPtr] != '"') {
                        json[slowPtr++] = json[fastPtr];
                    }
                    fastPtr++;
                }

                if (fastPtr == json.length)
                    return new int[]{slowPtr, fastPtr};
                
                json[slowPtr++] = json[fastPtr++];
            }

            // 消除中间空白
            while (fastPtr < json.length && isBlank(json[fastPtr])) fastPtr++;

            if (json[fastPtr] == '{') {
                int[] ptr = normalize(json, slowPtr, fastPtr, false);
                slowPtr = ptr[0]; fastPtr = ptr[1];
            } else if (json[fastPtr] == '[') {
                json[slowPtr++] = json[fastPtr++];
                
                while (fastPtr < json.length) {
                    // 消除中间空白
                    while (fastPtr < json.length && isBlank(json[fastPtr])) fastPtr++;
                    
                    if (fastPtr == json.length)
                        throw new JSONException("JSONUtil.normalize  json格式异常！");
                    
                    if (json[fastPtr] == ']') {
                        json[slowPtr++] = json[fastPtr++];
                        break;
                    } else if (json[fastPtr] == ',') {
                        json[slowPtr++] = json[fastPtr++];
                        // 消除中间空白
                        while (fastPtr < json.length && isBlank(json[fastPtr])) fastPtr++;
                    } else {
                        throw new JSONException("JSONUtil.normalize  json格式异常！");
                    }

                    int[] ptr = normalize(json, slowPtr, fastPtr, json[fastPtr] != '{');
                    slowPtr = ptr[0]; fastPtr = ptr[1];
                }                
            } else {
                // 是普通值
                int[] ptr = normalize(json, slowPtr, fastPtr, true);
                slowPtr = ptr[0]; fastPtr = ptr[1];
            }
        }
        
        if (fastPtr == json.length)
            throw new JSONException("JSONUtil.normalize  json格式异常！");
        
        json[slowPtr++] = json[fastPtr++]; // 将'}'覆盖过去
        
        return new int[]{slowPtr, fastPtr};
    }


    /**
     * 字符是否是空的
     * 
     * @param ch
     * @return
     */
    public static boolean isBlank(char ch) {
        return ch == ' ' || ch == '\t' || ch == '\n';
    }


    /**
     * 普通JSON值是否到结尾
     * 
     * @param ch1
     * @param ch2
     * @return
     */
    public static boolean isEnd(boolean isStr, char ch1, char ch2) {
        if (isStr && ch2 == '"' && ch1 != '\\')
            return true;
        else if (!isStr && ch2 == ',')
            return true;
        
        return false;
    }
    

    /**
     * 类型匹配
     *
     * @param field
     * @param sb
     * @return
     */
    public static Object typeMatching(Field field, StringBuffer sb) {
        return typeMatching(field, sb,true); 
    }
    

    /**
     * 类型匹配
     * 
     * @param field 类型判断依据的字段。只有当flag为false时，这个值才能为空
     * @param sb
     * @param flag 是否需要进行是否是集合的判断
     * @return
     */
    public static Object typeMatching(Field field, StringBuffer sb, boolean flag) {
        Object obj = null;
        Class clazz = null;

        if (flag && field == null)
            return null;
        
        if (flag && Collection.class.isAssignableFrom(field.getType())) {
            // 是集合，取得元素类型
            Type type = field.getGenericType();
            if (type instanceof ParameterizedType) {
                // 指明了泛型，通过泛型推断出元素类型                
                try {
                    clazz = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];                    
                } catch (ClassCastException cce) {
                    // 设置了上下限定界符，需要手动取得全限定类名
                    ClassLoader ccl = Thread.currentThread().getContextClassLoader();                    
                    String className = ((ParameterizedType) type).getActualTypeArguments()[0].toString();
                    className = className.substring(className.lastIndexOf(' ') + 1);

                    try {
                        clazz = ccl.loadClass(className);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        return null;
                    }
                }

            } else {
                // 不能推断出元素的类型，那么尝试从json值来推断数据类型
                return typeMatching(null, sb, false);
            }

        } else if (sb.charAt(0) == '{') {
            // 是复杂类型
            if (field == null) {
                // 不能根据字段推断属性的类型，那么将其搞成map
                return new HashMap<>();
            }

            clazz = field.getType();
            
        } else {
            // 按基本类型或者字符串转
            if (field == null) {
                // 根据json来判断数据类型
//                if (ch1 >= '')
            }
        }

        // 实例化
        try {
            obj = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return obj;
    }
    
}
