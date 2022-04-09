package com.ranni.resource;

import javax.naming.*;
import javax.naming.directory.*;

/**
 * Title: HttpServer
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-04-06 19:07
 */
public class WARDirContext extends BaseDirContext {
    @Override
    public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
        return null;
    }

    @Override
    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {

    }

    @Override
    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {

    }

    @Override
    public void bind(String name, Object obj, Attributes attrs) throws NamingException {

    }

    @Override
    public void rebind(String name, Object obj, Attributes attrs) throws NamingException {

    }

    @Override
    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        return null;
    }

    @Override
    public DirContext getSchema(String name) throws NamingException {
        return null;
    }

    @Override
    public DirContext getSchemaClassDefinition(String name) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        return null;
    }

    @Override
    public Object lookup(String name) throws NamingException {
        return null;
    }

    @Override
    public void unbind(String name) throws NamingException {

    }

    @Override
    public void rename(String oldName, String newName) throws NamingException {

    }

    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        return null;
    }

    @Override
    public void destroySubcontext(String name) throws NamingException {

    }

    @Override
    public Context createSubcontext(String name) throws NamingException {
        return null;
    }

    @Override
    public Object lookupLink(String name) throws NamingException {
        return null;
    }

    @Override
    public String getNameInNamespace() throws NamingException {
        return null;
    }
}
