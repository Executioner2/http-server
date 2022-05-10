package com.ranni.startup.core;

import com.ranni.container.Context;
import com.ranni.container.Wrapper;
import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.Rule;
import org.apache.commons.digester3.RuleSetBase;
import org.xml.sax.Attributes;

import java.lang.reflect.Method;

/**
 * Title: HttpServer
 * Description:
 * 在Tomcat的web规则上进行的修改
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022/5/7 11:11
 */
public final class WebRuleSet extends RuleSetBase {
    /**
     * 前缀
     */
    protected String prefix = null;

  
    public WebRuleSet() {
        this("");
    }

    public WebRuleSet(String prefix) {
        super();
        this.prefix = prefix;
    }



    /**
     * 添加规则实例
     * 
     * @param digester
     */
    public void addRuleInstances(Digester digester) {

        digester.addRule(prefix + "web-app", new SetPublicIdRule(digester, "setPublicId"));
        digester.addRule(prefix + "web-app", new SetSystemIdRule(digester, "setSystemId"));

        digester.addCallMethod(prefix + "web-app/context-param","addParameter", 2);
        digester.addCallParam(prefix + "web-app/context-param/param-name", 0);
        digester.addCallParam(prefix + "web-app/context-param/param-value", 1);

        digester.addCallMethod(prefix + "web-app/display-name", "setDisplayName", 0);

        digester.addRule(prefix + "web-app/distributable", new SetDistributableRule(digester));

        // ejb
//        digester.addObjectCreate(prefix + "web-app/ejb-local-ref", "org.apache.catalina.deploy.ContextLocalEjb");
//        digester.addSetNext(prefix + "web-app/ejb-local-ref", "addLocalEjb", "org.apache.catalina.deploy.ContextLocalEjb");

//        digester.addCallMethod(prefix + "web-app/ejb-local-ref/description", "setDescription", 0);
//        digester.addCallMethod(prefix + "web-app/ejb-local-ref/ejb-link", "setLink", 0);
//        digester.addCallMethod(prefix + "web-app/ejb-local-ref/ejb-ref-name", "setName", 0);
//        digester.addCallMethod(prefix + "web-app/ejb-local-ref/ejb-ref-type", "setType", 0);
//        digester.addCallMethod(prefix + "web-app/ejb-local-ref/local", "setLocal", 0);
//        digester.addCallMethod(prefix + "web-app/ejb-local-ref/local-home", "setHome", 0);
//
//        digester.addObjectCreate(prefix + "web-app/ejb-ref", "org.apache.catalina.deploy.ContextEjb");
//        digester.addSetNext(prefix + "web-app/ejb-ref", "addEjb", "org.apache.catalina.deploy.ContextEjb");
//
//        digester.addCallMethod(prefix + "web-app/ejb-ref/description", "setDescription", 0);
//        digester.addCallMethod(prefix + "web-app/ejb-ref/ejb-link", "setLink", 0);
//        digester.addCallMethod(prefix + "web-app/ejb-ref/ejb-ref-name", "setName", 0);
//        digester.addCallMethod(prefix + "web-app/ejb-ref/ejb-ref-type", "setType", 0);
//        digester.addCallMethod(prefix + "web-app/ejb-ref/home", "setHome", 0);
//        digester.addCallMethod(prefix + "web-app/ejb-ref/remote", "setRemote", 0);

        digester.addObjectCreate(prefix + "web-app/env-entry", "com.ranni.deploy.ContextEnvironment");
        digester.addSetNext(prefix + "web-app/env-entry", "addEnvironment", "com.ranni.deploy.ContextEnvironment");

        digester.addCallMethod(prefix + "web-app/env-entry/description", "setDescription", 0);
        digester.addCallMethod(prefix + "web-app/env-entry/env-entry-name", "setName", 0);
        digester.addCallMethod(prefix + "web-app/env-entry/env-entry-type", "setType", 0);
        digester.addCallMethod(prefix + "web-app/env-entry/env-entry-value", "setValue", 0);

        // error page
//        digester.addObjectCreate(prefix + "web-app/error-page", "org.apache.catalina.deploy.ErrorPage");
//        digester.addSetNext(prefix + "web-app/error-page", "addErrorPage", "org.apache.catalina.deploy.ErrorPage");
//
//        digester.addCallMethod(prefix + "web-app/error-page/error-code", "setErrorCode", 0);
//        digester.addCallMethod(prefix + "web-app/error-page/exception-type", "setExceptionType", 0);
//        digester.addCallMethod(prefix + "web-app/error-page/location", "setLocation", 0);

        digester.addObjectCreate(prefix + "web-app/filter", "com.ranni.core.FilterDef");
        digester.addSetNext(prefix + "web-app/filter", "addFilterDef", "com.ranni.core.FilterDef");

        digester.addCallMethod(prefix + "web-app/filter/description", "setDescription", 0);
        digester.addCallMethod(prefix + "web-app/filter/display-name", "setDisplayName", 0);
        digester.addCallMethod(prefix + "web-app/filter/filter-class", "setFilterClass", 0);
        digester.addCallMethod(prefix + "web-app/filter/filter-name", "setFilterName", 0);
        digester.addCallMethod(prefix + "web-app/filter/large-icon", "setLargeIcon", 0);
        digester.addCallMethod(prefix + "web-app/filter/small-icon", "setSmallIcon", 0);

        digester.addCallMethod(prefix + "web-app/filter/init-param", "addInitParameter", 2);
        digester.addCallParam(prefix + "web-app/filter/init-param/param-name", 0);
        digester.addCallParam(prefix + "web-app/filter/init-param/param-value", 1);

        digester.addObjectCreate(prefix + "web-app/filter-mapping", "com.ranni.deploy.FilterMap");
        digester.addSetNext(prefix + "web-app/filter-mapping", "addFilterMap", "com.ranni.deploy.FilterMap");

        digester.addCallMethod(prefix + "web-app/filter-mapping/filter-name", "setFilterName", 0);
        digester.addCallMethod(prefix + "web-app/filter-mapping/servlet-name", "setServletName", 0);
        digester.addCallMethod(prefix + "web-app/filter-mapping/url-pattern", "setURLPattern", 0);

        digester.addCallMethod(prefix + "web-app/listener/listener-class", "addApplicationListener", 0);

        // login config
//        digester.addObjectCreate(prefix + "web-app/login-config", "org.apache.catalina.deploy.LoginConfig");
//        digester.addSetNext(prefix + "web-app/login-config", "setLoginConfig", "org.apache.catalina.deploy.LoginConfig");
//
//        digester.addCallMethod(prefix + "web-app/login-config/auth-method", "setAuthMethod", 0);
//        digester.addCallMethod(prefix + "web-app/login-config/realm-name", "setRealmName", 0);
//        digester.addCallMethod(prefix + "web-app/login-config/form-login-config/form-error-page", "setErrorPage", 0);
//        digester.addCallMethod(prefix + "web-app/login-config/form-login-config/form-login-page", "setLoginPage", 0);

//        digester.addCallMethod(prefix + "web-app/mime-mapping", "addMimeMapping", 2);
//        digester.addCallParam(prefix + "web-app/mime-mapping/extension", 0);
//        digester.addCallParam(prefix + "web-app/mime-mapping/mime-type", 1);
//
//        digester.addCallMethod(prefix + "web-app/resource-env-ref", "addResourceEnvRef", 2);
//        digester.addCallParam(prefix + "web-app/resource-env-ref/resource-env-ref-name", 0);
//        digester.addCallParam(prefix + "web-app/resource-env-ref/resource-env-ref-type", 1);

        digester.addObjectCreate(prefix + "web-app/resource-ref", "com.ranni.deploy.ContextResource");
        digester.addSetNext(prefix + "web-app/resource-ref", "addResource", "com.ranni.deploy.ContextResource");

        digester.addCallMethod(prefix + "web-app/resource-ref/description", "setDescription", 0);
        digester.addCallMethod(prefix + "web-app/resource-ref/res-auth", "setAuth", 0);
        digester.addCallMethod(prefix + "web-app/resource-ref/res-ref-name", "setName", 0);
        digester.addCallMethod(prefix + "web-app/resource-ref/res-sharing-scope", "setScope", 0);
        digester.addCallMethod(prefix + "web-app/resource-ref/res-type", "setType", 0);

        // security constraint
//        digester.addObjectCreate(prefix + "web-app/security-constraint", "org.apache.catalina.deploy.SecurityConstraint");
//        digester.addSetNext(prefix + "web-app/security-constraint", "addConstraint", "org.apache.catalina.deploy.SecurityConstraint");
//
//        digester.addRule(prefix + "web-app/security-constraint/auth-constraint", new SetAuthConstraintRule(digester));
//        digester.addCallMethod(prefix + "web-app/security-constraint/auth-constraint/role-name", "addAuthRole", 0);
//        digester.addCallMethod(prefix + "web-app/security-constraint/display-name", "setDisplayName", 0);
//        digester.addCallMethod(prefix + "web-app/security-constraint/user-data-constraint/transport-guarantee", "setUserConstraint", 0);

//        digester.addObjectCreate(prefix + "web-app/security-constraint/web-resource-collection", "org.apache.catalina.deploy.SecurityCollection");
//        digester.addSetNext(prefix + "web-app/security-constraint/web-resource-collection", "addCollection", "org.apache.catalina.deploy.SecurityCollection");
//        digester.addCallMethod(prefix + "web-app/security-constraint/web-resource-collection/http-method", "addMethod", 0);
//        digester.addCallMethod(prefix + "web-app/security-constraint/web-resource-collection/url-pattern", "addPattern", 0);
//        digester.addCallMethod(prefix + "web-app/security-constraint/web-resource-collection/web-resource-name", "setName", 0);
//
//        digester.addCallMethod(prefix + "web-app/security-role/role-name", "addSecurityRole", 0);

        digester.addRule(prefix + "web-app/servlet", new WrapperCreateRule(digester));
        digester.addSetNext(prefix + "web-app/servlet", "addChild", "com.ranni.container.Container");

        digester.addCallMethod(prefix + "web-app/servlet/init-param", "addInitParameter", 2);
        digester.addCallParam(prefix + "web-app/servlet/init-param/param-name", 0);
        digester.addCallParam(prefix + "web-app/servlet/init-param/param-value", 1);

        digester.addCallMethod(prefix + "web-app/servlet/jsp-file", "setJspFile", 0);
        digester.addCallMethod(prefix + "web-app/servlet/load-on-startup", "setLoadOnStartupString", 0);
        digester.addCallMethod(prefix + "web-app/servlet/run-as/role-name", "setRunAs", 0);

        digester.addCallMethod(prefix + "web-app/servlet/security-role-ref", "addSecurityReference", 2);
        digester.addCallParam(prefix + "web-app/servlet/security-role-ref/role-link", 1);
        digester.addCallParam(prefix + "web-app/servlet/security-role-ref/role-name", 0);

        digester.addCallMethod(prefix + "web-app/servlet/servlet-class", "setServletClass", 0);
        digester.addCallMethod(prefix + "web-app/servlet/servlet-name", "setName", 0);

        digester.addCallMethod(prefix + "web-app/servlet-mapping", "addServletMapping", 2);
        digester.addCallParam(prefix + "web-app/servlet-mapping/servlet-name", 1);
        digester.addCallParam(prefix + "web-app/servlet-mapping/url-pattern", 0);

        digester.addCallMethod(prefix + "web-app/session-config/session-timeout", "setSessionTimeout", 1, new Class[] { Integer.TYPE });
        digester.addCallParam(prefix + "web-app/session-config/session-timeout", 0);

        digester.addCallMethod(prefix + "web-app/taglib", "addTaglib", 2);
        digester.addCallParam(prefix + "web-app/taglib/taglib-location", 1);
        digester.addCallParam(prefix + "web-app/taglib/taglib-uri", 0);

        digester.addCallMethod(prefix + "web-app/welcome-file-list/welcome-file", "addWelcomeFile", 0);

    }
    
}


/**
 * web-app的设置系统id
 */
final class SetSystemIdRule extends Rule {
    private String method;
    
    public SetSystemIdRule(Digester digester, String method) {
        this.method = method;
        super.setDigester(digester);
    }

    @Override
    public void begin(String namespace, String name, Attributes attributes) throws Exception {
        Digester digester = super.getDigester();
        
        Context context = digester.peek(digester.getCount() - 1);
        Object top = digester.peek();
        Class paramClasses[] = new Class[1];
        paramClasses[0] = "String".getClass();
        String paramValues[] = new String[1];
        paramValues[0] = digester.getPublicId();

        Method m = null;
        try {
            m = top.getClass().getMethod(method, paramClasses);
            System.out.println();
            System.out.println();
            System.out.println(top);
            System.out.println();
            System.out.println();
        } catch (NoSuchMethodException e) {
            e.printStackTrace(System.err);
            return;
        }

        m.invoke(top, paramValues);
    }
}


/**
 * 转发规则
 */
final class SetDistributableRule extends Rule {
    
    public SetDistributableRule(Digester digester) {
        super.setDigester(digester);
    }

    @Override
    public void begin(String namespace, String name, Attributes attributes) throws Exception {
        Digester digester = super.getDigester();
        Context context = digester.peek();
        context.setDistributable(true);
    }

}


/**
 * 公共ID创建规则
 */
final class SetPublicIdRule extends Rule {
    private String method = null;
    
    public SetPublicIdRule(Digester digester, String method) {
        this.method = method;
        super.setDigester(digester);
    }

    @Override
    public void begin(String namespace, String name, Attributes attributes) throws Exception {
        Digester digester = super.getDigester();
        
        Context context = digester.peek(digester.getCount() - 1);
        Object top = digester.peek();
        Class paramClasses[] = new Class[1];
        paramClasses[0] = "String".getClass();
        String paramValues[] = new String[1];
        paramValues[0] = digester.getPublicId();

        Method m = null;
        try {
            m = top.getClass().getMethod(method, paramClasses);
            System.out.println();
            System.out.println();
            System.out.println(top);
            System.out.println();
            System.out.println();
        } catch (NoSuchMethodException e) {
            e.printStackTrace(System.err);
            return;
        }

        m.invoke(top, paramValues);

    }

}


/**
 * wrapper创建规则
 */
final class WrapperCreateRule extends Rule {

    public WrapperCreateRule(Digester digester) {
        super.setDigester(digester);
    }

    @Override
    public void begin(String namespace, String name, Attributes attributes) throws Exception {
        Digester digester = super.getDigester();
        Context context = digester.peek(digester.getCount() - 1);
        Wrapper wrapper = context.createWrapper();
        digester.push(wrapper);
        System.out.println("push wrapper: " + wrapper);
    }

    @Override
    public void end(String namespace, String name) throws Exception {
        Digester digester = super.getDigester();
        Wrapper wrapper = digester.pop();
        System.out.println("pop wrapper: " + wrapper);
    }

}
