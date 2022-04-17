package com.ranni.naming;

import com.ranni.util.Enumerator;

import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.util.List;

/**
 * Title: HttpServer
 * Description:
 * 文件资源目录下的资源集合的迭代器
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-09 17:28
 */
public class NamingContextEnumeration<T extends NameClassPair> implements NamingEnumeration<T> {
    protected Enumerator it;

    public NamingContextEnumeration(List<NamingEntry> list) {
        this.it = new Enumerator(list);
    }

    public NamingContextEnumeration(Enumerator it) {
        this.it = it;
    }

    /**
     * 下一个元素
     *
     * @return
     * @throws NamingException
     */
    @Override
    public T next() throws NamingException {
        return nextElement();
    }

    /**
     * 后面还有不有元素
     *
     * @return
     * @throws NamingException
     */
    @Override
    public boolean hasMore() throws NamingException {
        return it.hasMoreElements();
    }

    /**
     * emm
     *
     * @throws NamingException
     */
    @Override
    public void close() throws NamingException {

    }

    /**
     * 后面还有不有元素
     *
     * @return
     * @throws NamingException
     */
    @Override
    public boolean hasMoreElements() {
        return it.hasMoreElements();
    }

    /**
     * 返回一个NameClassPair的包装类
     *
     * @see NameClassPair 此类表示为绑定的对象名称与类名对
     *      建议联系着{@link NamingEntry}一起解读
     *
     * @return
     */
    @Override
    public T nextElement() {
        NamingEntry entry = (NamingEntry) it.nextElement();
        return (T) new Binding(entry.name, entry.value.getClass().getName(), entry.value, true);
    }
}
