package com.ranni.handler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/9 22:03
 */
@Deprecated
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
    
    public static final Set<Integer> dbSet = new HashSet<>(){{
        add("byte".hashCode()); add("java.lang.Byte".hashCode());
        add("char".hashCode()); add("java.lang.Character".hashCode());
        add("short".hashCode()); add("java.lang.Short".hashCode());
        add("int".hashCode()); add("java.lang.Integer".hashCode());
        add("long".hashCode()); add("java.lang.Long".hashCode());
        add("float".hashCode()); add("java.lang.Float".hashCode());
        add("double".hashCode()); add("java.lang.Double".hashCode());
        add("java.lang.String".hashCode());
    }};

    
    private static final int JSON_MAX_DEEP = 100; // json最大的嵌套深度
    
    private static final StringBuilder sb = new StringBuilder();

    private JSONUtil() {}


    // ==================================== 对象转json ====================================

    private static Object jsonElementType(Object val) {
        if (val == null) {
            
            return "\"null\"";
        } else if (val instanceof Byte || val instanceof Short
                || val instanceof Integer || val instanceof Long
                || val instanceof Float || val instanceof Double) {
            
            return String.valueOf(val);
        } else if (val instanceof Character || val instanceof String) {
            
            return "\""+val+"\""; 
        } else if (val instanceof Map) {
            
            return toJSONStringInternal((Map) val);
        } else if (val instanceof Collection) {
            
            return toJSONList((Collection) val);
        } else if (val.getClass().isArray()) {
            
            return toJSONList(Arrays.asList(val));
        } else {
            
            return toJSONStringInternal(val);
        }
    }


    /**
     * 将集合转json数组
     * 
     * @param collection 要转换的集合
     * @return 返回转换成的json数组
     */
    private static StringBuilder toJSONList(Collection collection) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (Object val : collection) {
            sb.append(jsonElementType(val).toString());
            sb.append(',');
        }
        if (sb.length() > 1) {
            sb.setCharAt(sb.length() - 1, ']');
        } else {
            sb.append(']');
        }
        return sb;
    }
    
    
    /**
     * 将object的getter属性转换为json字符串格式
     * 
     * @param obj 要转化的对象
     * @return 返回转换后的json字符串
     */
    public static String toJSONString(Object obj) {
        return  toJSONStringInternal(obj).toString();
    }


    /**
     * 将object的getter属性转换为json字符串格式
     *
     * @param obj 要转化的对象
     * @return 返回转换后的json StringBuilder
     */
    public static StringBuilder toJSONStringInternal(Object obj) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Object val = null;
            try {
                val = field.get(obj);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            sb.append("\"");
            sb.append(field.getName());
            sb.append("\"");
            sb.append(":");
            
            sb.append(jsonElementType(val));
            sb.append(',');
        }

        if (sb.length() > 1) {
            sb.setCharAt(sb.length() - 1, '}');
        } else {
            sb.append('}');
        }
        
        return sb;
    }


    /**
     * 将map转为json字符串
     * 
     * @param map 要转为json的map
     * @return 返回转换成后json字符串
     */
    public static String toJSONString(Map map) {
        return toJSONStringInternal(map).toString();
    }


    /**
     * 将map转为json字符串
     *
     * @param map 要转为json的map
     * @return 返回转换后的json StringBuilder
     */
    public static StringBuilder toJSONStringInternal(Map map) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');

        for (Object key : map.keySet()) {
            sb.append("\"");
            sb.append(key);
            sb.append("\"");
            sb.append(":");

            sb.append(jsonElementType(map.get(key)));
            sb.append(',');
        }
        
        
        if (sb.length() > 1) {
            sb.setCharAt(sb.length() - 1, '}');
        } else {
            sb.append('}');
        }
        return sb;
    }



    // ==================================== json转对象 ====================================

    /**
     * json值的实体
     */
    private static class ValueEntity {
        Object val; // 返回的元素
        int index; // 下标

        public ValueEntity(Object val, int index) {
            this.val = val;
            this.index = index;
        }
    }
    

    /**
     * 解析JSON字符串
     *
     * @param clazz1    要解析成的实例类型
     * @param clazz2    集合最底层元素类型，为null表示不是json数组
     * @param json      json字符串
     * @return 
     */
    public static Object parseJSON(Class clazz1, Class clazz2, String json) throws JSONException {
        return parseJSON(clazz1, clazz2, json.toCharArray()); 
    }


    /**
     * 解析JSON字符串
     *
     * @param clazz1    单个类型，如果是集合，那么这个就是集合中一个元素类型
     * @param clazz2    如果是集合，此类不为null
     * @param json      json字符数组
     * @return 
     */
    public static Object parseJSON(Class clazz1, Class clazz2, char[] json) throws JSONException {
        json = normalize(json);
        ValueEntity ve = parseJSON(clazz1, clazz2, json, 0);
        
        if (ve == null)
            return null;
        
        return ve.val;        
    }
    

    /**
     * 解析JSON字符串
     *
     * @param clazz1    单个类型，如果是集合，那么这个就是集合中一个元素类型
     * @param clazz2    如果是集合，此类不为null
     * @param json      json字符数组
     * @param index     下标
     * @return 
     */
    private static ValueEntity parseJSON(Class clazz1, Class clazz2, char[] json, int index) throws JSONException {
        if (clazz1 == null) return new ValueEntity(null, index);

        ValueEntity ve = null;
        
        if (clazz2 != null && Collection.class.isAssignableFrom(clazz2)) {
            // 是JSON数组，clazz不能为空且obj必须是集合
            if (json[index] != '[')
                throw new IllegalArgumentException("JSONUtil.parseJSON  非集合类型");
            
            if (clazz1 == null)
                throw new IllegalArgumentException("JSONUtil.parseJSON  必须指定元素类型");
            
            Class aClass = null;
            if (json[index + 1] == '[')
                aClass = ArrayList.class; 

            Collection collection = null;
            try {
                collection = (Collection) getInstance(ArrayList.class); // XXX - 考虑要不要自定义集合
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (collection == null)
                throw new JSONException("JSONUtil.getInstance  获取实例为空！");
            
            while (json[index] != ']') {
                ve = parseJSON(clazz1, aClass, json, index + 1);

                collection.add(ve.val);
                index = ve.index;                
            }

            ve = new ValueEntity(collection, index + 1);
            
        } else if (json[index] == '{') {
            // 是单个对象
            ve = parseJSON(clazz1, json, index + 1);
            
        } else if (json[index] != '[') {
            // 是普通值
            ve = parseJSONValue(clazz1, json, index);
       
        } else {
            throw new IllegalArgumentException("JSONUtil.parseJSON  非集合类型");
        }
        
        return ve;
    }
    
    
    /**
     * 解析JSON
     * 用递归的方式
     *
     * @param aClass    要解析为的实例
     * @param json      json字符数组
     * @param index     解析的下标位置
     *              
     * @return 返回json字符数组解析到的位置
     */
    private static ValueEntity parseJSON(Class aClass, char[] json, int index) throws JSONException {
        Object obj = getInstance(aClass);

        while (json[index] != '}') {
            Field field = null;
            
            clearStringBuilder();

            if (json[index] == ',')
                index++;
            
            // 取得key
            while (json[index] != ':')
                sb.append(json[index++]);

            index++;

            try {
                field = aClass.getDeclaredField(sb.toString());
                field.setAccessible(true);
            } catch (NoSuchFieldException e) {
                // XXX - 没匹配上的字段就跳过？
                ;
            }

            // 遍历跳过这个value
            if (field == null) {
                index = skip(json, index);
                if (json[index] == ',') index++;
                continue;
            }
            
            // 取得字段的实例
            Class<?> type = field.getType();
            ValueEntity res = null;

            // 是集合，尝试获取集合元素的类型
            if (Collection.class.isAssignableFrom(type)) {
                Class clazz = inferElementClass(field, json[index + 1]);
                res = parseJSON(clazz, type, json, index);
            } else {
                res = parseJSON(type, null, json, index);
            }
            
            try {
                field.set(obj, res.val);
                index = res.index;
            } catch (Exception e) {
                // 可能产生obj为null的情况，这时返回的ValueEntity实例的val为null也行
                e.printStackTrace();
            }

        }
        
        return new ValueEntity(obj, index + 1);
    }


    /**
     * 解析值
     * 
     * @param clazz
     * @param json
     * @param index
     * @return
     */
    private static ValueEntity parseJSONValue(Class clazz, char[] json, int index) {
               
        clearStringBuilder();
        
        // 把value剪出来
        boolean isStr = json[index] == '"'; // 先判断是否是字符串，如果是，那么直到字符串结尾的'}', ']' , ',' 都不作数

        if (isStr)
            index++;
        
        while (true) {
            if (isStr) {
                if (json[index] == '"' && json[index - 1] != '\\') {
                    isStr = false;
                    index++;
                    continue;
                }
            
            } else if (json[index] == '}' || json[index] == ']' || json[index] == ',') {
                break;
            }
            
            sb.append(json[index++]);
        }

        if (json[index] == ',')
            index++;
        
        Object obj = getInstance(clazz, sb.toString());

        return new ValueEntity(obj, index);
    }


    /**
     * 实例化
     * 
     * @param clazz     要实例化的类
     * @param params    构造方法的形参
     * @return
     */
    public static Object getInstance(Class clazz, Object... params) {
        Class<?>[] classes = null;

        if (params != null && params.length > 0) {
            classes = new Class[params.length];
            for (int i = 0; i < params.length; i++) {
                classes[i] = params[i].getClass();
            }
        }

        try {
            return getConstructor(clazz, classes).newInstance(params);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * 实例化
     * @param clazz     要实例化的类
     * @return
     */
    private static Constructor getConstructor(Class clazz, Class... classes) {
        if (clazz.isPrimitive()) {
            // 是基本数据类型
            if (clazz == char.class)
                clazz = Character.class;
            if (clazz == byte.class)
                clazz = Byte.class;
            else if (clazz == short.class) 
                clazz = Short.class;
            else if (clazz == int.class)
                clazz = Integer.class;
            else if (clazz == long.class)
                clazz = Long.class;
            else if (clazz == float.class)
                clazz = Float.class;
            else 
                clazz = Double.class;
        }
        
        try {
            return clazz.getDeclaredConstructor(classes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    

    /**
     * 需要跳过的部分
     * 默认传入的json是正确的，压缩过的json
     * 
     * @param json
     * @param index
     * @return
     */
    private static int skip(char[] json, int index) {
        int bracket = 0, brace = 0; // 中括号和花括号的数量
        boolean isStr = false; // 遇到字符串要跳过匹配
        
        if (json[index] == '[')
            bracket++;
        else if (json[index] == '{')
            brace++;
        
        while (bracket > 0 || brace > 0) {
            index++;
            
            if (json[index] == '"') {
                if (isStr) {
                    if (json[index - 1] == '\\')
                        isStr = false;
                } else {
                    isStr = true;
                }
            } else if (json[index] == ']' && !isStr) {
                bracket--;
            } else if (json[index] == '}' && !isStr) {
                brace--;
            } else if (json[index] == '[' && !isStr) {
                bracket++;
            } else if (json[index] == '{' && !isStr) {
                brace++;
            }
        }
         
        return index;
    }
    

    /**
     * 标准化json字符串格式
     * 消除那些智障空格，空行，回车，tab
     * 消除key的双引号
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
     * 消除那些智障空格，空行，回车，tab
     * 消除key的双引号
     * 让json字符串变为一行紧凑的json字符串
     *
     * @param json
     * @return
     */
    public static char[] normalize(char[] json) throws JSONException {
        int[] len = normalize(json, 0, 0, 0);
        if (len[1] < json.length || (json[0] != '[' && json[0] != '{'))
            throw new JSONException("JSONUtil.normalize  json格式异常！");

        char[] res = new char[len[0]];
        System.arraycopy(json, 0, res, 0, len[0]);
        
        return res; 
    }


    /**
     * 标准化json字符串格式
     * 如果传入的json是个单个实例，那么直接调用{@link JSONUtil#normalize(char[], int, int, boolean, int)}进行标准化
     * 如果传入的json是个json数组，那么需要递归调用此方法进行解析
     * 
     * @param json
     * @param slowPtr
     * @param fastPtr
     * @param deep      深度
     * @return
     * @throws JSONException
     */
    private static int[] normalize(char[] json, int slowPtr, int fastPtr, int deep) throws JSONException {
        if (deep >= JSON_MAX_DEEP) // 限制最大深度，避免栈溢出
            throw new JSONException("JSONUtil JSON嵌套过长！");
        
        int[] len = null;
        
        // 消除前导空白
        while (fastPtr < json.length && isBlank(json[fastPtr])) fastPtr++;

        // 先判断是否是'['开头，如果是，则表示是json数组，如果是'{'则是普通json
        if (fastPtr == json.length)
            throw new JSONException("JSONUtil.normalize  json格式异常！");
        
        if (json[fastPtr] == '{') {
            len = normalize(json, slowPtr, fastPtr, false, deep + 1);
        } else if (json[fastPtr] == '[') {
            json[slowPtr++] = json[fastPtr++];
            while (fastPtr < json.length && isBlank(json[fastPtr])) fastPtr++;
            
            while (true) {
                len = normalize(json, slowPtr, fastPtr, deep + 1);
                slowPtr = len[0]; fastPtr = len[1];

                while (fastPtr < json.length && isBlank(json[fastPtr])) fastPtr++;
                
                if (fastPtr == json.length)
                    throw new JSONException("JSONUtil.normalize  json格式异常！");
                
                if (json[fastPtr] == ']') {
                    json[slowPtr++] = json[fastPtr++];
                    while (fastPtr < json.length && isBlank(json[fastPtr])) fastPtr++;
                    len[0] = slowPtr; len[1] = fastPtr;
                    break;
                } else if (json[fastPtr] == ',') {
                    json[slowPtr++] = json[fastPtr++];
                } else {
                    throw new JSONException("JSONUtil.normalize  json格式异常！");
                }
            }
        } else {
            len = normalize(json, slowPtr, fastPtr, true, deep + 1);
        }
        
        return len;
    }


    /**
     * 标准化json字符串格式
     * 与 {@link JSONUtil#normalize(char[], int, int, int)} 配合使用
     * 
     * XXX - 可靠性有待考证
     * 
     * @param json
     * @param slowPtr
     * @param fastPtr
     * @param isCommon              是否是基本数据类型或字符串              
     * @return {slowPtr, fastPtr}   覆盖到的位置
     * @throws JSONException
     */
    private static int[] normalize(char[] json, int slowPtr, int fastPtr, boolean isCommon, int deep) throws JSONException {
        if (deep >= JSON_MAX_DEEP) // 限制最大深度，避免栈溢出
            throw new JSONException("JSONUtil JSON嵌套过长！");
        
        boolean hasLeftBrace = false;
        
        while (fastPtr < json.length && json[fastPtr] != '}') {
            
            if (isCommon) {
                // 消除前导空白
                while (fastPtr < json.length && isBlank(json[fastPtr])) fastPtr++;

                boolean isStr = fastPtr < json.length && json[fastPtr] == '"';
                
                if (fastPtr < json.length && json[fastPtr] == '"')
                    json[slowPtr++] = json[fastPtr++];
                
                while (fastPtr < json.length && !isEnd(isStr, json[fastPtr - 1], json[fastPtr])) {
                    json[slowPtr++] = json[fastPtr++];
                }
                
                if (fastPtr < json.length && json[fastPtr] == '"') 
                    json[slowPtr++] = json[fastPtr++];
                
                // 消除后导空白
                while (fastPtr < json.length && isBlank(json[fastPtr])) fastPtr++;
                
                return new int[]{slowPtr, fastPtr};                
            } else {
                int state = 0;

                // 消除空白
                while (fastPtr < json.length && state < 2) {
                    if (isBlank(json[fastPtr])) {
                        fastPtr++;
                    } else if (json[fastPtr] == '{' && state == 0) {
                        if (!hasLeftBrace) {
                            hasLeftBrace = true;
                        } else {
                            throw new JSONException("JSONUtil.normalize  json格式异常！");
                        }
                        
                        json[slowPtr++] = json[fastPtr++];
                        state = 1;
                    } else if (json[fastPtr] == '"' && (state == 1 || hasLeftBrace)) {
                        fastPtr++;
                        state = 2;
                    } else {
                        throw new JSONException("JSONUtil.normalize  json格式异常！");
                    }
                }
                
                if (fastPtr == json.length && state != 0)
                    throw new JSONException("JSONUtil.normalize  json格式异常！");
                
                // key
                while (fastPtr < json.length && json[fastPtr] != '"') {
                    if (!isBlank(json[fastPtr])) {
                        json[slowPtr++] = json[fastPtr];
                    }
                    fastPtr++;
                }
                
                if (fastPtr < json.length)
                    fastPtr++;

                // 消除后导空白
                while (fastPtr < json.length && isBlank(json[fastPtr])) fastPtr++;

                if (fastPtr < json.length && json[fastPtr] != ':')
                    throw new JSONException("JSONUtil.normalize  json格式异常！");
                
                if (fastPtr == json.length)
                    return new int[]{slowPtr, fastPtr};
                
                json[slowPtr++] = json[fastPtr++];
            }
            
            // value
            int[] ptr = normalize(json, slowPtr, fastPtr, deep + 1);
            slowPtr = ptr[0]; fastPtr = ptr[1];
            
            // 如果有','，则覆盖过去
            while (fastPtr < json.length && isBlank(json[fastPtr])) fastPtr++;
            if (fastPtr < json.length && json[fastPtr] == ',') {
                json[slowPtr++] = json[fastPtr++];
                while (fastPtr < json.length && isBlank(json[fastPtr])) fastPtr++;
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
        if (isStr) {
            if (ch2 == '"' && ch1 != '\\')
                return true;
        
        } else {
            if (ch2 == ',' || isBlank(ch2) || ch2 == ']' || ch2 == '}')
                return true;
        }
        
        return false;
    }    
    

    /**
     * 推断集合元素的类型
     *
     * @param ch
     * @return
     */
    private static Class inferElementClass(Field field, char ch) throws JSONException {
        Class clazz = null;
        Type genericType = field.getGenericType();
        
        if (genericType instanceof ParameterizedType) {
            // 有指定泛型
            try {
                clazz = (Class) ((ParameterizedType) genericType).getActualTypeArguments()[0];
            } catch (ClassCastException cce) {
                // 泛型设定了上下限定的，尝试通过全限定类名加载
                String className = ((ParameterizedType) genericType).getActualTypeArguments()[0].toString();
                className = className.substring(className.lastIndexOf(' ') + 1);
                ClassLoader ccl = Thread.currentThread().getContextClassLoader();

                try {
                    clazz = ccl.loadClass(className);
                } catch (ClassNotFoundException e) {
                    // 设定了上下限定，但是找不到限定的类就抛出异常
                    e.printStackTrace();
                    throw new JSONException("JSONUtil.classNotFound  找不到集合泛型的上下限定边界类！");
                }
            }
        }

        if (clazz == null) {
            // 没有指定泛型，尝试从json数组元素类型解析成对应的类型
            clazz = inferClass(ch);
        }
        
        return clazz;
    }
    

    /**
     * 从json字符推断出实例类型
     * 
     * @param ch
     * @return
     */
    private static Class inferClass(char ch) {
        if (ch == '{') {
            return JSONArrayEntity.class;
        } else if (ch == '[') {
            return ArrayList.class; // XXX - 考虑要不要自定义一个集合
        } else { // 统统都为字符串
            return String.class;
        }
    }


    /**
     * 清空字符串
     */
    private static void clearStringBuilder() {
        sb.setLength(0);
    }
    
}
