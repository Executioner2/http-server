package com.ranni.naming;

import javax.naming.*;
import javax.naming.directory.*;
import java.io.*;
import java.net.URI;
import java.util.*;

/**
 * Title: HttpServer
 * Description:
 * 文件资源容器
 * JNDI中的context
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-06 19:06
 */
public class FileDirContext extends BaseDirContext {
    protected static final int BUFFER_SIZE = 2048; // 缓冲区大小

    protected File base; // 文件夹根目录
    protected String absoluteBase; // 文档根目录的绝对路径
    protected boolean caseSensitive = true; // 是否检查绝对路径的规范性
    protected boolean allowLinking = false; // 是否允许连接


    public FileDirContext() {
    }

    public FileDirContext(Hashtable env) {
        super(env);
    }


    /**
     * 释放资源
     */
    public void release() {
        caseSensitive = true;
        allowLinking = false;
        absoluteBase = null;
        base = null;
    }

    /**
     * 设置文档根目录的绝对路径
     *
     * @param docBase
     */
    @Override
    public void setDocBase(String docBase) {
        if (docBase == null)
            throw new IllegalArgumentException("文件夹路径不能为空！");

        base = new File(docBase);
        try {
            // 取得规范文件，即将".", "..", "./"这种表示方法解析后返回以绝对路径表示形式的文件
            base = base.getCanonicalFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 文件不存在、不是目录、不能读，这些情况将抛出异常
        if (!base.exists() || !base.isDirectory() || !base.canRead())
            throw new IllegalArgumentException("文件目录异常  " + docBase);

        this.absoluteBase = base.getAbsolutePath();
        super.setDocBase(docBase);
    }

    /**
     * 先取得资源，再取得资源属性
     * 此方法返回的也是得到的文件的新的属性，故不存在文件被修改了，但是程序不知道的情况
     *
     * @param name
     * @param attrIds
     * @return
     * @throws NamingException
     */
    @Override
    public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
        File file = file(name);

        if (file == null)
            throw new NamingException("资源未找到！ name：" + name);

        return new FileResourceAttributes(file);
    }

    /**
     * 取得文件
     * 每次返回的都是一个新的文件对象，所以此方法不存在文件被修改了，但是程序还不知道的情况
     *
     * @see {@link File#getCanonicalPath()} 关于此方法的说明如下：
     *      假设有个index.html文件的绝对路径为 E:\JavaProject\project\HttpServer\processor\index.html
     *      在E:\JavaProject\project\HttpServer下有个Test类想访问此index.html文件
     *      Test类使用相对路径 File file = new File("./processor/index.html"); 来取得该文件
     *      file.getAbsolutePath();   的结果为 E:\JavaProject\project\HttpServer\.\processor\index.html
     *      file.getCanonicalPath();  的结果为 E:\JavaProject\project\HttpServer\processor\index.html
     *      可以看到 getCanonicalPath() 方法会把相对路径中的 "." 去掉（查看该方法的说明 ".." 也能去掉）然后得到
     *      规范路径名（规范路径名同时也是绝对路径名，但是绝对路径名不一定是规范路径名）。
     *
     * @param name
     * @return
     */
    protected File file(String name) {
        File file = new File(base, name);

        if (file.exists() && file.canRead()) {
            // 取得规范路径
            String canPath = null;
            try {
                canPath = file.getCanonicalPath();
            } catch (IOException e) {
                ;
            }

            if (canPath == null)
                return null;

            // 如果不允许连接且路径不是以绝对基本路径开头那将返回null
            if (!allowLinking && !canPath.startsWith(absoluteBase))
                return null;

            // 检查绝对路径是否是规范路径
            if (caseSensitive) {
                String absPath = file.getAbsolutePath();
                if (absPath.endsWith("."))
                    absPath += "/";

                absPath = normalize(absPath);
                canPath = normalize(canPath);

                if (canPath == null || absPath == null)
                    return null;

                if (absoluteBase.length() < absPath.length()
                    && absoluteBase.length() < canPath.length()) {

                    absPath = absPath.substring(absoluteBase.length() + 1);
                    canPath = canPath.substring(absoluteBase.length() + 1);

                    if ("".equals(absPath))
                        absPath = "/";
                    if ("".equals(canPath))
                        canPath = "/";
                    if (!canPath.equals(absPath))
                        return null;
                }
            }
        } else {
            return null;
        }

        return file;
    }

    /**
     * 规范化文件路径，开头必须有个/
     * \    =>  /
     * 连续/   =>  /
     * /./  =>  /
     * /../ =>  向上返回一级之后变成/
     *
     * 例子：\root//a/../b/./c\\\e///a/d//v
     * 规范化后：/root/b/c/e/a/d/v
     *
     * 用栈优化原本的字符串拼接
     *
     * @param path
     * @return
     */
    protected String normalize(String path) {
        char[] chars = path.toCharArray();
        Deque<Character> deque = new LinkedList<>();

        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if (ch == '\\') {
                if (!deque.isEmpty() && deque.peekLast() != '/') {
                    deque.addLast('/');
                }
            } else if (ch == '.') {
                // 检索后面两位
                if (i < chars.length - 1) {
                    if (chars[i + 1] == '/') {
                        i++;
                    } else if (chars[i + 1] == '.') {
                        if (i < chars.length - 2 && chars[i + 2] == '/') {
                            // 弹出一个目录
                            deque.pollLast(); // 弹出之前压入的 /
                            while (!deque.isEmpty() && deque.peekLast() != '/')
                                deque.pollLast();
                        }
                        i += 2;
                    }
                }
            } else if (ch == '/') {
                if (deque.peekLast() != null && deque.peekLast() != '/')
                    deque.addLast('/');
            } else {
                deque.addLast(ch);
            }
        }

        if (deque.isEmpty())
            return null;

        StringBuilder sb =  new StringBuilder(deque.size() + 1); // 预留一个开头 / 的空间
        if (deque.getFirst() != '/') sb.append('/');
        for (char ch : deque)
            sb.append(ch);

        return sb.toString();
    }

    /**
     * 暂不支持该操作
     *
     * @param name
     * @param mod_op
     * @param attrs
     * @throws NamingException
     */
    @Override
    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 暂不支持该操作
     *
     * @param name
     * @param mods
     * @throws NamingException
     */
    @Override
    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 绑定资源
     *
     * @param name
     * @param obj
     * @param attrs
     * @throws NamingException
     */
    @Override
    public void bind(String name, Object obj, Attributes attrs) throws NamingException {
        File file = new File(base, name);
        if (file.exists())
            throw new NameAlreadyBoundException("资源已绑定！ " + name);
        rebind(name, obj, attrs);
    }

    /**
     * 正儿八经开始绑定资源
     * 此实现类的该方法不支持自定义属性，所以attrs参数无用
     * 如果需要绑定的对象是个目录，那么就删除原来name对应的资源并创建一个名字为变量name的空文件夹
     * XXX 这个鸡儿卵方法是个狗屎，后面一定要改
     *
     * @param name 资源名
     * @param obj 需要绑定的对象
     * @param attrs 自定义属性，但在该方法中无用
     * @throws NamingException
     */
    @Override
    public void rebind(String name, Object obj, Attributes attrs) throws NamingException {
        File file = new File(base, name);
        InputStream is = null;

        if (obj instanceof Resource) {
            try {
                is = ((Resource) obj).streamContent();
            } catch (IOException e) {
                ;
            }
        } else if (obj instanceof InputStream) {
            is = (InputStream) obj;
        } else if (obj instanceof DirContext) {
            // 如果需要绑定的对象是个目录
            // 那么就删除原来name对应的资源并创建一个名字为变量name的空文件夹
            if (file.exists()) {
                if (!file.delete())
                    throw new NamingException("资源绑定失败（资源已存在且删除失败）！  " + name);
            }

            if (!file.mkdir())
                throw new NamingException("资源绑定失败（文件夹创建失败）！  " + name);
        }

        if (is == null)
            throw new NamingException("资源绑定失败（未取得输入流）  " + name);

        // 将obj中的内容写入到file中
        byte[] buffer = new byte[BUFFER_SIZE];
        int len = -1;
        try (FileOutputStream os = new FileOutputStream(file)) {
            while (true) {
                len = is.read(buffer);
                if (len < 0) break;
                os.write(buffer,0, len);
            }
        } catch (IOException e) {
            throw new NamingException("资源绑定失败！  " + e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 创建子容器
     *
     * @param name
     * @param attrs 不支持自定义属性，此参数无用
     * @return
     * @throws NamingException
     */
    @Override
    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        File file = new File(base, name);
        if (file.exists())
            throw new NameAlreadyBoundException("资源已存在！  " + name);
        if (!file.mkdir())
            throw new NamingException("资源创建失败！  " + name);
        return (DirContext) lookup(name);
    }

    /**
     * 暂不支持该操作
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public DirContext getSchema(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 暂不支持该操作
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public DirContext getSchemaClassDefinition(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 暂不支持该操作
     *
     * @param name
     * @param matchingAttributes
     * @param attributesToReturn
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 暂不支持该操作
     *
     * @param name
     * @param matchingAttributes
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 暂不支持该操作
     *
     * @param name
     * @param filter
     * @param cons
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 暂不支持该操作
     *
     * @param name
     * @param filterExpr
     * @param filterArgs
     * @param cons
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * 检索并创建对象
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public Object lookup(String name) throws NamingException {
        Object res = null;
        File file = file(name); // 取得文件

        if (file == null)            
            throw new NamingException("资源不存在！  " + name);

        if (file.isDirectory()) {
            // 如果是目录，则作为FileDirContext返回
            FileDirContext fileDirContext = new FileDirContext(env);
            fileDirContext.setDocBase(file.getPath());
            res = fileDirContext;
        } else {
            // 不是目录，作为文件资源返回
            res = new FileResource(file);
        }

        return res;
    }

    /**
     * 解绑
     * 就是文件存在就删除文件，不存在就抛出异常
     *
     * @param name
     * @throws NamingException
     */
    @Override
    public void unbind(String name) throws NamingException {
        File file = file(name);
        if (file == null)
            throw new NamingException("资源不存在！  " + name);
        if (!file.delete())
            throw new NamingException("资源删除失败！  " + name);
    }

    /**
     * 重命名
     *
     * @param oldName
     * @param newName
     * @throws NamingException
     */
    @Override
    public void rename(String oldName, String newName) throws NamingException {
        File file = file(oldName);
        if (file == null)
            throw new NamingException("资源不存在！  " + oldName);

        File newFile = new File(base, newName);
        file.renameTo(newFile); // 重命名资源
    }

    /**
     * 得到传入的File文件夹中的资源
     * 如果该资源是个目录，则将该资源封装成FileDirContext对象后存入集合
     * 如果该资源是个文件，则将该资源封装成FileResource对象后存入集合
     *
     * @param file
     * @return 返回一个线程安全的集合 {@link Collections#synchronizedList(List)}
     */
    protected List<NamingEntry> list(File file) {
        // 取得一个线程安全的集合
        List<NamingEntry> list = Collections.synchronizedList(new ArrayList<>());

        if (!file.isDirectory())
            return list;

        String[] names = file.list();
        Arrays.sort(names);
        if (names == null)
            return list;

        NamingEntry entry = null;
        Object o = null;

        for (int i = 0; i < names.length; i++) {
            File currentFile = new File(file, names[i]);

            if (currentFile.isDirectory()) {
                // 当前文件是个目录，那就生成一个目录容器
                FileDirContext fileDirContext = new FileDirContext(env);
                fileDirContext.setDocBase(currentFile.getPath());
                o = fileDirContext;
            } else {
                o = new FileResource(currentFile);
            }
            entry = new NamingEntry(names[i], o, NamingEntry.ENTRY);
            list.add(entry);
        }

        return list;
    }

    /**
     * 返回文件夹中文件集合的迭代器
     * 迭代器中的元素是{@link NameClassPair}包装类
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        File file = file(name);
        if (file == null)
            throw new NamingException("资源不存在！  " + name);

        List<NamingEntry> list = list(file);
        return new NamingContextEnumeration<>(list);
    }


    /**
     * 返回绑定的资源集合的迭代器
     * 迭代器中的元素是{@link Binding}包装类
     * 同 {@link FileDirContext#list(String)} 类似
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        File file = file(name);
        if (file == null)
            throw new NamingException("资源不存在！  " + name);

        List<NamingEntry> list = list(file);
        return new NamingContextEnumeration<>(list);
    }

    /**
     * 销毁子容器，直接就是解绑
     *
     * @param name
     * @throws NamingException
     */
    @Override
    public void destroySubcontext(String name) throws NamingException {
        unbind(name);
    }

    /**
     * 创建子容器
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public Context createSubcontext(String name) throws NamingException {
        File file = new File(base, name);
        if (file.exists())
            throw new NameAlreadyBoundException("资源已存在！  " + name);
        if (!file.mkdir())
            throw new NamingException("资源创建失败！  " + name);

        return (Context) lookup(name);
    }

    /**
     * 不支持连接，这里直接调用{@link FileDirContext#lookup(String)}
     *
     * @param name
     * @return
     * @throws NamingException
     */
    @Override
    public Object lookupLink(String name) throws NamingException {
        return lookup(name);
    }

    /**
     * 返回文件夹根目录
     *
     * @return
     * @throws NamingException
     */
    @Override
    public String getNameInNamespace() throws NamingException {
        return this.docBase;
    }


    /**
     * 返回文件根目录
     * 
     * @return
     */
    public File getBase() {
        return base;
    }
    

    /**
     * 文件资源属性内部类
     */
    protected class FileResourceAttributes extends ResourceAttributes {
        protected File file; // 资源文件
        protected boolean accessed; // 是否访问过子资源

        public FileResourceAttributes(File file) {
            this.file = file;
        }

        /**
         * 是否是目录。
         * 如果是目录，则将accessed标志位置为true
         *
         * @return
         */
        @Override
        public boolean isCollection() {
            if (!accessed) {
                collection = file.isDirectory();
                accessed = true;
            }
            return super.isCollection();
        }

        /**
         * 获取内容长度
         *
         * @return
         */
        @Override
        public long getContentLength() {
            if (contentLength != -1L)
                return contentLength;
            contentLength = file.length();
            return contentLength;
        }

        /**
         * 取得文件的创建时间
         *
         * @return
         */
        @Override
        public long getCreation() {
            if (creation != -1L)
                return creation;
            creation = file.lastModified();
            return creation;
        }

        /**
         * 取得文件的创建日期
         *
         * @return
         */
        @Override
        public Date getCreationDate() {
            if (creation == -1L)
                creation = file.lastModified();
            return super.getCreationDate();
        }

        /**
         * 取得最后一次修改的时间
         *
         * @return
         */
        @Override
        public long getLastModified() {
            if (lastModified != -1L)
                return lastModified;
            lastModified = file.lastModified();
            return lastModified;
        }

        /**
         * 取得最后一次修改的日期
         *
         * @return
         */
        @Override
        public Date getLastModifiedDate() {
            if (lastModified == -1L)
                lastModified = file.lastModified();
            return super.getLastModifiedDate();
        }

        /**
         * 取得名字
         *
         * @return
         */
        @Override
        public String getName() {
            if (name == null)
                name = file.getName();
            return name;
        }

        /**
         * 取得资源类型
         *
         * @return
         */
        public String getResourceType() {
            if (!accessed) {
                collection = file.isDirectory();
                accessed = true;
            }
            return super.getResourceType();
        }
    }

    /**
     * 文件的包装类，以FileDirContext的内部类存在
     */
    public class FileResource extends Resource {
        protected File file;


        public FileResource(File file) {
            this.file = file;
        }


        /**
         * @return 返回URI
         */
        public URI getURI() {
            return file.toURI();
        }
        

        /**
         * 取得输入流
         * 如果binaryContent为null，则说明没有把文件内容存入到数组中
         * 那么即使inputStream不为null，也很可能这个流已经被用过并关闭掉了
         * 所以只要没有把文件内容缓存下来，每次外部请求取得文件的输入流时就要新创建一个输入流对象
         *
         * @return
         * @throws IOException
         */
        @Override
        public InputStream streamContent() throws IOException {
            if (binaryContent == null)
                return new FileInputStream(file);
            return super.streamContent();
        }
    }
}
