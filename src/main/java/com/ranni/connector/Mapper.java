package com.ranni.connector;

import com.ranni.container.Context;
import com.ranni.container.Host;
import com.ranni.container.MappingData;
import com.ranni.container.Wrapper;
import com.ranni.core.WebResourceRoot;
import com.ranni.util.buf.Ascii;
import com.ranni.util.buf.CharChunk;
import com.ranni.util.buf.MessageBytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/6/13 21:26
 * @Ref org.apache.catalina.mapper.Mapper
 */
public final class Mapper {

    // ==================================== 属性字段 ====================================
    
    /**
     * 默认主机，如果请求没有指定那个主机处理它，那么就使用这个默认主机处理
     */
    private volatile String defaultHostName = null;
    private volatile MappedHost defaultHost = null;
    
    /**
     * 注册的Host容器
     */
    private volatile MappedHost[] hosts = new MappedHost[0];


    // ==================================== 内部类 ====================================

    /**
     * 容器名字和容器的映射
     *
     * @param <T> 容器类型
     */
    protected abstract class MapElement<T> {
        public final String name;
        public final T obj;

        public MapElement(String name, T obj) {
            this.name = name;
            this.obj = obj;
        }
    }


    /**
     * host容器映射
     */
    protected final class MappedHost extends MapElement<Host> {

        /**
         * Context容器集合
         */
        public volatile ContextList contextList;

        /**
         * 此Host的真名
         */
        private final MappedHost realHost;

        /**
         * 此Host所拥有的别名
         */
        private final List<MappedHost> aliases;


        public MappedHost(String name, Host obj) {
            super(name, obj);
            realHost = this;
            contextList = new ContextList();
            aliases = new ArrayList<>();
        }

        public MappedHost(String alias, MappedHost realHost) {
            super(alias, realHost.obj);
            this.realHost = realHost;
            contextList = realHost.contextList;
            aliases = null; // 不允许别名还有别名
        }
        

        /**
         * @return 如果返回<b>true</b>，则表示此ContextList实例是个别名
         */
        public boolean isAlias() {
            return realHost != this;
        }

        public MappedHost getRealHost() {
            return realHost;
        }

        public String getRealHostName() {
            return realHost.name;
        }

        public Collection<MappedHost> getAliases() {
            return aliases;
        }

        public void addAlias(MappedHost alias) {
            aliases.add(alias);
        }

        public void addAliases(Collection<MappedHost> c) {
            aliases.addAll(c);
        }

        public void removeAlias(MappedHost alias) {
            aliases.remove(alias);
        }
    }


    /**
     * Context容器集合
     */
    protected final class ContextList {
        /**
         * MappedContext集合，以容器名进行升序存储
         */
        public final MappedContext[] contexts;

        /**
         * URL中的第几个分隔符后的。<br>
         * 如：/TestWeb/test，deep = 1则表示是URL中第一个分隔符后的
         */
        public final int deep;


        public ContextList() {
            this(new MappedContext[0], 0);
        }

        public ContextList(MappedContext[] contexts, int deep) {
            this.contexts = contexts;
            this.deep = deep;
        }


        /**
         * 添加容器映射
         *
         * @param context 要添加的容器映射
         * @param deep 此Context属于第几层的
         * @return 如果插入成功，返回添加后的容器映射集合，否则返回null
         */
        public ContextList addContext(MappedContext context, int deep) {
            MappedContext[] newMap = new MappedContext[contexts.length + 1];
            if (insertMap(contexts, newMap, context)) {
                return new ContextList(newMap, Math.max(deep, this.deep));
            }

            return null;
        }

        
        /**
         * 删除容器映射
         *
         * @param path 容器映射名，实际是个路径
         * @return 如果删除成功，返回删除后的容器映射集合，否则返回null
         */
        public ContextList removeContext(String path) {
            MappedContext[] newMap = new MappedContext[contexts.length - 1];
            if (removeMap(contexts, newMap, path)) {
                return new ContextList(newMap, deep);
            }

            return null;
        }
    }


    /**
     * context容器映射
     */
    protected final class MappedContext extends MapElement<Context> {

        private volatile boolean paused;
        public volatile WrapperDictTree wrapperDictTree = new WrapperDictTree();
        public final int slashCount;
        public final WebResourceRoot webResourceRoot;
        
        
        public MappedContext(String name, Context obj, int slashCount, 
                             WebResourceRoot webResourceRoot) {
            
            super(name, obj);
            this.slashCount = slashCount;
            this.webResourceRoot = webResourceRoot;
            
        }
        
        public boolean isPaused() {
            return paused;
        }

        public void markPaused() {
            paused = true;
        }
    }


    /**
     * wrapper路径字典树
     */
    protected final class WrapperDictTree {
        private class Node {
            // 其实这个内存占用并不是太高，只是利用率可能会很低
            Node[] map = new Node[26];
            MappedWrapper mappedWrapper;
        }
        
        private volatile Node root = new Node();


        /**
         * 将wrapper的映射实例挂到wrapper路径字典树上<br>
         * 默认传进来的uri是对应servlet的uri部分，已
         * 经不包括context那部分了
         * 
         * @param uri 处理此请求的servlet路径
         * @param wrapper wrapper实例
         */
        public void addMappedWrapper(String uri, Wrapper wrapper) {
            Node node = root;

            for (int i = 0; i < uri.length(); i++) {
                if (uri.charAt(i) == '/') {
                    continue;
                }
                int index = uri.charAt(i) - 'a';
                if (node.map[index] == null) {
                    node.map[index] = new Node();
                }
                node = node.map[index];
            }
            
            node.mappedWrapper = new MappedWrapper(uri, wrapper);            
        }

        public void addMappedWrapper(MappedWrapper wrapper) {
            Node node = root;
            
            for (char ch : wrapper.name.toCharArray()) {
                if (ch == '/') {
                    continue;
                }
                int index = ch - 'a';
                if (node.map[index] == null) {
                    node.map[index] = new Node();
                }
                node = node.map[index];
            }

            node.mappedWrapper = wrapper;
        }


        /**
         * 根据uri取得对应的wrapper映射实例<br>
         * 默认传入的uri是对应servlet的uri部分，
         * 已经不包括context那部分了
         * 
         * @param uri 处理此请求的servlet路径
         */
        public MappedWrapper getMappedWrapper(CharChunk uri) {
            Node node = root;
            Node prev = node;
            MappedWrapper res = null;

            for (int i = uri.charAt(0) == '/' ? 1 : 0; i < uri.length(); i++) {
                if (uri.charAt(i) == '/' && prev.mappedWrapper != null) {
                    res = prev.mappedWrapper;
                }
                
                int index = uri.charAt(i) - 'a';
                if (node.map[index] == null) {
                    // 不用break是为了避免出现下面这种情况：
                    //   url1: /test/a/dc/a
                    //
                    //   ----------- 上面是存入到字典树中的url，下面是请求的url -----------
                    //
                    //   url2: /test/ak
                    // 
                    // 字典树会存为：
                    //           url1                 root             url2
                    //            |                     t                |
                    //            V                     e                |
                    //                                  s                |
                    //   has MappedWrapper 1 ---->      t                V
                    //   has MappedWrapper 2 ---->      a      
                    //                               d     k    <---- 不存在，为null
                    //   has MappedWrapper 3 ---->   c        
                    //   has MappedWrapper 4 ---->   a        
                    //
                    // 当传入的是url2程序会执行到这里。这时prev = testa 中最后一个字符'a'指向的节点
                    // 该位置上，因为有url1这样一个url，这里prev的mappedWrapper是不为null的再如果
                    // 此时这里用的是break，则会执行到循环体外的if。
                    // uri.charAt(uri.length() - 1) != '/' && prev.mappedWrapper != null 
                    // 成立，res错误的指向了url1中编号为2的MappedWrapper。正确是应该指向最精准且正确
                    // 的编号为1的MappedWrapper。
                    
                    return res;
                }
                prev = node;
                node = node.map[index];
            }
            
            if (uri.charAt(uri.length() - 1) != '/' && prev.mappedWrapper != null) {
                res = prev.mappedWrapper;
            }
            
            return res;            
        }

        public MappedWrapper getMappedWrapper(String uri) {
            Node node = root;
            Node prev = node;
            MappedWrapper res = null;
            
            for (int i = uri.charAt(0) == '/' ? 1 : 0; i < uri.length(); i++) {
                if (uri.charAt(i) == '/' && prev.mappedWrapper != null) {
                    res = prev.mappedWrapper;
                }

                int index = uri.charAt(i) - 'a';
                if (node.map[index] == null) {
                    return res;
                }
                prev = node;
                node = node.map[index];
            }

            if (uri.charAt(uri.length() - 1) != '/' && prev.mappedWrapper != null) {
                res = prev.mappedWrapper;
            }

            return res;
        }
        
        public void recycle() {
            root = new Node();
        }
    }
    

    /**
     * wrapper容器映射
     */
    protected final class MappedWrapper extends MapElement<Wrapper> {
        public MappedWrapper(String name, Wrapper obj) {
            super(name, obj);
        }
    }
    

    // ==================================== 核心方法 ====================================

    /**
     * 添加Host容器
     * 
     * @param name host容器名
     * @param aliases 别名
     * @param host host容器
     */
    public synchronized void addHost(String name, String[] aliases, Host host) {
        MappedHost mappedHost = new MappedHost(name, host);
        MappedHost[] newHosts = new MappedHost[hosts.length + 1];
        if (insertMap(hosts, newHosts, mappedHost)) {
            hosts = newHosts;
            if (mappedHost.name.equals(defaultHostName)) {
                defaultHost = mappedHost;
            }
        } else {
            // 插入失败，已经有这个名字的Host映射了看相同名字的
            // Host映射实例是否相等，如果不等，则直接返回，否则
            // 为此容器添加新的别名
            int i = find(hosts, name);
            if (i >= 0) {
                mappedHost = hosts[i]; 
            } else {
                return;
            }
        } 
        
        // 添加别名
        Collection<MappedHost> list = new ArrayList<>(aliases.length);
        for (String alias : aliases) {
            // 判断此别名是否存在
            MappedHost aliasMappedHost = new MappedHost(alias, mappedHost);
            if (addHostAliasImpl(aliasMappedHost)) {
                list.add(aliasMappedHost);
            }
        }
        
        mappedHost.addAliases(list);
    }


    /**
     * 给容器映射添加别名
     * 
     * @param mappedHost 别名Host容器映射
     * @return 如果返回<b>true</b>，则表示添加成功
     */
    public synchronized boolean addHostAliasImpl(MappedHost mappedHost) {
        MappedHost[] newMappedHosts = new MappedHost[hosts.length + 1];
        
        if (insertMap(hosts, newMappedHosts, mappedHost)) {
            if (mappedHost.name.equals(defaultHostName)) {
                defaultHost = mappedHost;
            }
            return true;
        }
        
        return false;
    }


    /**
     * 删除Host容器映射<br>
     * 
     * @param name 传入的Host名
     * @param delRealHost 是否删除所有的别名容器。如果传入的name是真实容器，
     *                    那么即使这个值为false也会删除所有别名容器
     */
    public synchronized boolean removeHost(String name, boolean delRealHost) {
        MappedHost host = exactFind(hosts, name);
        if (host == null) {
            return false;
        }
        
        MappedHost[] mappedHosts = null;
        if (host.isAlias() && !delRealHost) {
            mappedHosts = new MappedHost[hosts.length - 1];
            if (removeMap(hosts, mappedHosts, host.name)) {
                hosts = mappedHosts;
                return true;
            }
            return false;
        }

        MappedHost realHost = host.isAlias() ? host.realHost : host;
        mappedHosts = new MappedHost[hosts.length - realHost.getAliases().size() - 1];

        for (int i = 0, j = 0; i < hosts.length; i++) {
            if (hosts[i].realHost != realHost) {
                mappedHosts[j++] = hosts[i];
            }
        }

        hosts = mappedHosts;
        return true;
    }


    /**
     * 添加Context容器
     * 
     * @param hostName Context容器所属的Host主机名
     * @param host Context所属的Host主机
     * @param path Context的路径
     * @param context Context容器
     * @param resources Context的资源管理组件（内含了类加载器，JNDI容器等重要信息）
     * @param mappedWrappers Context容器的Wrapper子容器集合
     */
    public void addContext(String hostName, Host host, String path, 
                                        Context context, WebResourceRoot resources, 
                                        Collection<MappedWrapper> mappedWrappers) {

        MappedHost mappedHost = exactFind(hosts, hostName);
        if (mappedHost == null) {
            addHost(hostName, new String[0], host);
            mappedHost = exactFind(hosts, hostName);
            if (mappedHost == null) {
                return;
            }
        }

        int slashCount = slashCount(path);
        MappedContext contextMapped = new MappedContext(path, context, slashCount, resources);
        
        synchronized (mappedHost.contextList) {
            ContextList contextList = mappedHost.contextList.addContext(contextMapped, slashCount);
            if (contextList == null) {
                // 已经有一个相同path的容器映射了
                return;
            }
        }

        // 添加wrapper映射
        synchronized (contextMapped) {
            if (mappedWrappers != null) {
                addWrappers(contextMapped, mappedWrappers);
            }    
        }
    }
    
    
    /**
     * 取得Host容器
     * 
     * @param name 可以是别名也可以是真实名
     * @return 返回Host容器
     */
    public synchronized MappedHost getHost(String name) {
        MappedHost host = exactFind(hosts, name);
        if (host == null) {
            return null;
        }
        
        return host.realHost;
    }


    /**
     * 将批量的wrapper挂在所属的context下
     * 
     * @param context 所属的MappedContext
     * @param mappedWrappers 批量wrapper映射集合
     */
    void addWrappers(MappedContext context, Collection<MappedWrapper> mappedWrappers) {
        for (MappedWrapper wrapper : mappedWrappers) {
            addWrapper(context, wrapper);
        }
    }
    

    /**
     * 添加wrapper到所属的context下。底层是根据path挂在
     * 字典树上。
     * 
     * @param context 所属的MappedContext
     * @param path wrapper的path
     * @param wrapper 需要挂上去的wrapper
     */
    void addWrapper(MappedContext context, String path, Wrapper wrapper) {
        context.wrapperDictTree.addMappedWrapper(path, wrapper);
    }


    /**
     * 添加wrapper到所属的context下。底层是根据path挂在
     * 字典树上。
     *
     * @param context 所属的MappedContext
     * @param wrapper 需要挂上去的MappedWrapper
     */
    void addWrapper(MappedContext context, MappedWrapper wrapper) {
        context.wrapperDictTree.addMappedWrapper(wrapper);
    }
    
    
    /**
     * 容器和请求的映射
     * 
     * @param serverName 处理请求的服务器名
     * @param decodedURI 解码后的URI
     * @param mappingData 映射数据
     */
    public void map(MessageBytes serverName, MessageBytes decodedURI, MappingData mappingData) throws IOException {
        if (serverName.isNull()) {
            if (defaultHostName == null) {
                return;
            }
            serverName.getCharChunk().append(defaultHostName);
        }
        
        serverName.toChars();
        decodedURI.toChars();
        internalMap(serverName.getCharChunk(), decodedURI.getCharChunk(), mappingData);
    }


    /**
     * 设置映射关系
     * 
     * @param hostCC 处理请求的服务器名（字节块）
     * @param uri 解码后的URI（字节块）
     * @param mappingData 映射数据
     */
    protected void internalMap(CharChunk hostCC, CharChunk uri, MappingData mappingData) {
        
        if (mappingData.host != null) {
            // 已经绑定过了
            throw new AssertionError();
        }
        
        if (uri.isNull()) {
            return;
        }
        
        // 匹配Host容器
        MappedHost mappedHost = exactFindIgnoreCase(hosts, hostCC);
        if (mappedHost == null) {
            // 没有匹配上的Host映射，尝试使用默认容器
            mappedHost = defaultHost;
            if (mappedHost == null) {
                return;
            }
        }
        
        mappingData.host = mappedHost.obj;

        uri.setLimit(-1);

        // 匹配处理的Context容器
        ContextList contextList = mappedHost.contextList;
        MappedContext[] contexts = contextList.contexts;
        int pos = find(contexts, uri);
        if (pos == -1) {
            return;
        }
        
        int lastSlash = -1;
        int uriEnd = uri.getEnd();
        int length = -1;
        boolean found = false;
        MappedContext context = null;
        while (pos >= 0) {
            // 一层一层的往下扒拉
            context = contexts[pos];
            if (uri.startsWith(context.name)) {
                length = context.name.length();
                if (uri.getLength() == length) {
                    found = true;
                    break;
                } else if (uri.startsWithIgnoreCase("/", length)) {
                    found = true;
                    break;
                }
            }
            
            if (lastSlash == -1) {
                lastSlash = nthSlash(uri, contextList.deep + 1);
            } else {
                lastSlash = lastSlash(uri);
            }
            
            uri.setEnd(lastSlash);
            pos = find(contexts, uri);
        }
        uri.setEnd(uriEnd);
        
        if (!found) {
            // 没有找到，如果容器集合的第一个Context容器
            // 的name为空串，那么将这个Context容器作为
            // 处理这个请求的容器
            if (contexts[0].name.equals("")) {
                context = contexts[0];
            } else {
                context = null;
            }
        }

        if (context == null) {
            return;
        }
        
        mappingData.contextPath.setString(context.name);
        mappingData.context = context.obj;
        mappingData.contextSlashCount = context.slashCount;
        
        // 匹配Wrapper容器
        if (!context.isPaused()) {
            internalMapWrapper(context, uri, mappingData);
        }
    }


    /**
     * 匹配Wrapper容器
     * 
     * @param context Context容器
     * @param uri 请求的URI
     * @param mappingData 映射数据
     */
    private final void internalMapWrapper(MappedContext context, CharChunk uri, MappingData mappingData) {
        // XXX - 当前只做严格匹配
        // 做个字典树匹配，以便精准匹配
    }


    // ==================================== 数组操作 ====================================

    /**
     * 斜杠'/'计数
     * 
     * @param str 源字符串
     * @return 返回统计的斜杠'/'数量
     */
    private static final int slashCount(String str) {
        int pos = -1;
        int count = 0;
        while ((pos = str.indexOf('/', pos + 1)) != -1) {
            count++;
        }
        return count;
    }
    

    /**
     * 精确查询
     * 
     * @param map 需要查询的有序集合
     * @param name 匹配的target值
     * @return 返回查询到的元素
     */
    private static final <T, E extends MapElement<T>> E exactFind(E[] map, String name) {
        int pos = find(map, name);
        if (pos >= 0) {
            E result = map[pos];
            if (name.equals(result.name)) {
                return result;
            }
        }
        return null;
    }


    /**
     * 精确查询
     *
     * @param map 需要查询的有序集合
     * @param name 匹配的target值
     * @return 返回查询到的元素
     */
    private static final <T, E extends MapElement<T>> E exactFind(E[] map, CharChunk name) {
        int pos = find(map, name);
        if (pos >= 0) {
            E result = map[pos];
            if (name.equals(result.name)) {
                return result;
            }
        }
        return null;
    }
    
    
    /**
     * @return 返回最后一个斜杠'/'的下标
     */
    private static final int lastSlash(CharChunk name) {
        char[] c = name.getBuffer();
        int end = name.getEnd();
        int start = name.getStart();
        int pos = end;

        while (pos > start) {
            if (c[--pos] == '/') {
                break;
            }
        }

        return pos;
    }
    
    
    /**
     * 在指定的字节块中找到第n个斜杠'/'
     * 
     * @param name 指定的字节块
     * @param n 第n个斜杠
     * @return 返回第n个斜杠的下标（如果有的话）
     */
    private static final int nthSlash(CharChunk name, int n) {
        char[] c = name.getBuffer();
        int end = name.getEnd();
        int start = name.getStart();
        int pos = start;
        int count = 0;

        while (pos < end) {
            if ((c[pos++] == '/') && ((++count) == n)) {
                pos--;
                break;
            }
        }

        return pos;
    }
    
    
    /**
     * 插入一个新的映射元素到旧数组中，保持数组的有序性<br>
     * 如果数组中存在相同名字的映射元素，则不插入
     * 
     * @param oldMap 旧的映射元素数组
     * @param newMap 新的映射元素数组
     * @param mapElement 要添加的元素数组
     * @return 如果返回<b>true</b>，则表示插入成功
     */
    private static final <T> boolean insertMap(MapElement<T>[] oldMap, MapElement<T>[] newMap, MapElement<T> mapElement) {
        int i = find(oldMap, mapElement.name);
        if (i != -1 && oldMap[i].name.equals(mapElement.name)) {
            return false;
        }
        
        System.arraycopy(oldMap, 0, newMap, 0, i + 1);
        newMap[i + 1] = mapElement;
        System.arraycopy(oldMap, i + 1, newMap, i + 2, oldMap.length - i - 1);
        
        return true;
    }


    /**
     * 从映射数元数组中删除名字与传入的name参数相等的映射元素
     * 
     * @param oldMap 旧的映射元素数组
     * @param newMap 新的映射元素数组
     * @param name 要删除的元素名
     * @return 如果返回<b>true</b>，则表示删除成功
     */
    private static final <T> boolean removeMap(MapElement<T>[] oldMap, MapElement<T>[] newMap, String name) {
        int i = find(oldMap, name);
        if (i == -1 || !oldMap[i].name.equals(name)) {
            return false;
        }
        
        System.arraycopy(oldMap, 0, newMap, 0, i);
        System.arraycopy(oldMap, i + 1, newMap, i, oldMap.length - i - 1);
        return true;
    }


    /**
     * 忽略大小写的匹配
     * 
     * @param name 需要匹配的字节块
     * @param start 字节块参与匹配的起始位置
     * @param end 字节块参与匹配的结束位置
     * @param compareTo 匹配的字符串
     * @return 返回-1，0 或 1。-1小于，0等于，1大于
     */
    private static final int compareIgnoreCase(CharChunk name, int start, int end,
                                               String compareTo) {
        int result = 0;
        char[] c = name.getBuffer();
        int len = compareTo.length();
        if ((end - start) < len) {
            len = end - start;
        }
        for (int i = 0; (i < len) && (result == 0); i++) {
            if (Ascii.toLower(c[i + start]) > Ascii.toLower(compareTo.charAt(i))) {
                result = 1;
            } else if (Ascii.toLower(c[i + start]) < Ascii.toLower(compareTo.charAt(i))) {
                result = -1;
            }
        }
        if (result == 0) {
            if (compareTo.length() > (end - start)) {
                result = -1;
            } else if (compareTo.length() < (end - start)) {
                result = 1;
            }
        }
        return result;
    }

    
    /**
     * 不忽略大小写的匹配
     *
     * @param name 需要匹配的字节块
     * @param start 字节块参与匹配的起始位置
     * @param end 字节块参与匹配的结束位置
     * @param compareTo 匹配的字符串
     * @return 返回-1，0 或 1。-1小于，0等于，1大于
     */
    private static final int compare(CharChunk name, int start, int end,
                                     String compareTo) {
        int result = 0;
        char[] c = name.getBuffer();
        int len = compareTo.length();
        if ((end - start) < len) {
            len = end - start;
        }
        for (int i = 0; (i < len) && (result == 0); i++) {
            if (c[i + start] > compareTo.charAt(i)) {
                result = 1;
            } else if (c[i + start] < compareTo.charAt(i)) {
                result = -1;
            }
        }
        if (result == 0) {
            if (compareTo.length() > (end - start)) {
                result = -1;
            } else if (compareTo.length() < (end - start)) {
                result = 1;
            }
        }
        return result;
    }


    /**
     * 返回集合中匹配的元素
     * 
     * @param map 要进行查询的集合
     * @param name 匹配目标
     * @return 如果找到了就返回匹配的元素
     */
    private static final <T, E extends MapElement<T>> E exactFindIgnoreCase(E[] map, CharChunk name) {
        int index = findIgnoreCase(map, name);
        if (index >= 0) {
            // 还要进行字符匹配，因为返回的可能是个匹配结果较为接近的下标
            if (name.equals(map[index].name)) {
                return map[index];
            }
        }
        
        return null;
    }
    
    
    private static final <T> int findIgnoreCase(MapElement<T>[] map, CharChunk name) {
        return findIgnoreCase(map, name, name.getStart(), name.getEnd());
    }
    

    /**
     * 忽略大小写的二分查找
     * 
     * @param map 从此映射集合中查询目标
     * @param name 需要查询的元素名字节块
     * @param start 字节块的起始位置
     * @param end 字节块的结束位置
     * @return 如果查询到了匹配项返回对应的下标<b>或者集合中至少有两个元素，即使都不匹配，也会根据大小返回其中一个的下标</b>，否则返回-1
     */
    private static final <T> int findIgnoreCase(MapElement<T>[] map, CharChunk name, int start, int end) {

        int a = 0;
        int b = map.length - 1;

        if (b == -1) {
            return -1;
        }
        if (compareIgnoreCase(name, start, end, map[0].name) < 0) {
            return -1;
        }
        if (b == 0) {
            return 0;
        }

        int i = 0;
        while (true) {
            i = (b + a) >>> 1;
            int result = compareIgnoreCase(name, start, end, map[i].name);
            if (result == 1) {
                a = i;
            } else if (result == 0) {
                return i;
            } else {
                b = i;
            }
            if ((b - a) == 1) {
                int result2 = compareIgnoreCase(name, start, end, map[b].name);
                if (result2 < 0) {
                    return a;
                } else {
                    return b;
                }
            }
        }

    }
    
    
    private static <T> int find(MapElement<T>[] map, CharChunk name) {
        return find(map, name, name.getStart(), name.getEnd());
    }


    /**
     * 不忽略大小写的二分查找
     *
     * @param map 从此映射集合中查询目标
     * @param name 需要查询的元素名字节块
     * @param start 字节块的起始位置
     * @param end 字节块的结束位置
     * @return 如果查询到了匹配项返回对应的下标<b>或者集合中至少有两个元素，即使都不匹配，也会根据大小返回其中一个的下标</b>，否则返回-1
     */
    private static final <T> int find(MapElement<T>[] map, CharChunk name, int start, int end) {

        int a = 0;
        int b = map.length - 1;

        if (b == -1) {
            return -1;
        }

        if (compare(name, start, end, map[0].name) < 0) {
            return -1;
        }
        if (b == 0) {
            return 0;
        }

        int i = 0;
        while (true) {
            i = (b + a) >>> 1;
            int result = compare(name, start, end, map[i].name);
            if (result == 1) {
                a = i;
            } else if (result == 0) {
                return i;
            } else {
                b = i;
            }
            if ((b - a) == 1) {
                int result2 = compare(name, start, end, map[b].name);
                if (result2 < 0) {
                    return a;
                } else {
                    return b;
                }
            }
        }

    }
    

    /**
     * 二分查找
     * 
     * @param map 从此映射集合中查询目标
     * @param name 要查询的名字 
     * @return 如果查询到了匹配项返回对应的下标<b>或者集合中至少有两个元素，即使都不匹配，也会根据大小返回其中一个的下标</b>，否则返回-1
     */
    private static final <T> int find(MapElement<T>[] map, String name) {

        int a = 0;
        int b = map.length - 1;
        
        if (b == -1) {
            return -1;
        }

        if (name.compareTo(map[0].name) < 0) {
            return -1;
        }
        if (b == 0) {
            return 0;
        }

        int i = 0;
        while (true) {
            i = (b + a) >>> 1;
            int result = name.compareTo(map[i].name);
            if (result > 0) {
                a = i;
            } else if (result == 0) {
                return i;
            } else {
                b = i;
            }
            if ((b - a) == 1) {
                int result2 = name.compareTo(map[b].name);
                if (result2 < 0) {
                    return a;
                } else {
                    return b;
                }
            }
        }

    }
    
}
