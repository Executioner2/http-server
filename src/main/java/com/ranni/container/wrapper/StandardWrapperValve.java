package com.ranni.container.wrapper;

import com.ranni.common.Globals;
import com.ranni.connector.Response;
import com.ranni.connector.Request;
import com.ranni.container.Container;
import com.ranni.container.Context;
import com.ranni.container.Wrapper;
import com.ranni.container.context.StandardContext;
import com.ranni.container.pip.ValveBase;
import com.ranni.container.pip.ValveContext;
import com.ranni.core.ApplicationFilterChain;
import com.ranni.core.ApplicationFilterConfig;
import com.ranni.deploy.FilterMap;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Title: HttpServer
 * Description:
 * 标准的的wrapper基础阀
 * 此基础阀要完成的任务：
 * 1、执行与该servlet实例关联的全部过滤器
 * 2、调用servlet实例的service()方法
 *
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-27 21:47
 */
public class StandardWrapperValve extends ValveBase {
    public StandardWrapperValve(Container container) {
        setContainer(container);
    }

    @Override
    public String getInfo() {
        return null;
    }

    /**
     * 调用对应的servlet执行相应的service()
     * 必要的操作：
     * 1、调用wrapper的allocate()获取servlet实例 {@link Wrapper#allocate(Request, Response)}
     * 2、调用私有方法createFilterChain()，创建过滤链
     * 3、调用过滤链的doFilter()方法，doFilter()中有对servlet实例的service()方法调用
     * 4、释放过滤器的链
     * 5、调用wrapper的deallocate()方法归还servlet资源 {@link StandardWrapper#deallocate(Servlet)}
     * 6、若该servlet再也不会被使用到就会调用wrapper的unload()销毁此servlet实例 {@link StandardWrapper#unload()}
     *
     * XXX 需要添加日志记录
     * 
     * @param request
     * @param response
     * @param valveContext
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void invoke(Request request, Response response, ValveContext valveContext) throws IOException, ServletException {
        boolean unavailable = false; // 是否不可用
        
        StandardWrapper wrapper = (StandardWrapper) getContainer();
        Throwable throwable = null;
        Servlet servlet = null;


        // 检查此Web应用程序是否可用
        if (!((Context) wrapper.getParent()).getAvailable()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "StandardWrapper.getAvailable  此WebApp不可用，" + wrapper.getName());
            unavailable = true;
        }

        // 检查servlet是否未到可用时间或永久不可用
        if (!unavailable && wrapper.isUnavailable()) {
            if (response != null) {
                long available = wrapper.getAvailable();
                if (available > 0L && available < Long.MAX_VALUE) {
                    response.setDateHeader("Retry-After", available);
                }
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "StandardWrapper.isUnavailable  此Servlet未到可用时间或永久不可用，" + wrapper.getName());
            }

            unavailable = true;
        }

        try {
            if (!unavailable) {
                servlet = wrapper.allocate();
            }
        } catch (Throwable e) {
            throwable = e;
            servlet = null;
            exception(request, response, e); // 设置异常
        }

        // 创建请求链
        ApplicationFilterChain filterChain = createFilterChain(request, servlet);
        
        // 执行过滤，如果是jsp文件就要在请求对象中添加jsp文件名属性，过滤完后还要及时删除此属性
        try {
            String jspFile = wrapper.getJspFile();
            if (jspFile != null) {
                request.setAttribute(Globals.JSP_FILE_ATTR, jspFile);
            } else {
                request.removeAttribute(Globals.JSP_FILE_ATTR);
            }
            if (servlet != null && filterChain != null) {
                filterChain.doFilter(request, response);
            }
            request.removeAttribute(Globals.JSP_FILE_ATTR);
        } catch (IOException e) {
            request.removeAttribute(Globals.JSP_FILE_ATTR);
            throwable = e;
            exception(request, response, e);
        } catch (UnavailableException e) {
            request.removeAttribute(Globals.JSP_FILE_ATTR);
            wrapper.unavailable(e);
            long available = wrapper.getAvailable();
            if ((available > 0L) && (available < Long.MAX_VALUE))
                response.setDateHeader("Retry-After", available);
            throwable = e;
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "StandardWrapper.isUnavailable" + wrapper.getName());
        } catch (ServletException e) {
            request.removeAttribute(Globals.JSP_FILE_ATTR);
            throwable = e;
            exception(request, response, e);
        } catch (Throwable e) {
            request.removeAttribute(Globals.JSP_FILE_ATTR);
            throwable = e;
            exception(request, response, e);
        }

        // 到这里就已经完成了一次对请求的处理，开始资源回收
        
        // 回收过滤链
        try {
            if (filterChain != null)
                filterChain.release();
        } catch (Throwable e) {
            if (throwable == null) {
                // 还没发生过错误，那么这次发生的错误将返回给客户端
                throwable = e;
                exception(request, response, e);
            }
        }
        
        // 回收servlet
        try {
            if (servlet != null)
                wrapper.deallocate(servlet);
        } catch (Throwable e) {
            if (throwable == null) {
                // 还没发生过错误，那么这次发生的错误将返回给客户端
                throwable = e;
                exception(request, response, e);
            }
        }
        
        // 如果servlet被置为永久不可用，将卸载此servlet
        try {
            if (servlet != null && wrapper.getAvailable() == Long.MAX_VALUE)
                wrapper.unload();
        } catch (Throwable e) {
            if (throwable == null) {
                exception(request, response, e);
            }
        }
        
    }


    /**
     * 创建过滤链
     * 此处不对过滤器进行实例化，仅仅创建过滤链，
     * 然后将匹配的过滤器关联对象加入到过滤链的集合中
     *
     * @param request
     * @param servlet
     * @return
     */
    private ApplicationFilterChain createFilterChain(Request request, Servlet servlet) {
        if (servlet == null)
            return null;
        
        // 创建并初始化过滤器链对象
        ApplicationFilterChain filterChain = new ApplicationFilterChain();
        filterChain.setServlet(servlet);
        StandardWrapper wrapper = (StandardWrapper) getContainer();
        filterChain.setSupport(wrapper.getInstanceSupport());

        // 取得过滤器的映射集
        StandardContext context = (StandardContext) wrapper.getParent();
        FilterMap[] filterMaps = context.findFilterMaps();
        
        if (filterMaps == null || filterMaps.length == 0)
            return filterChain;
        
        // 实例化所有过滤器
        String requestPath = null;
        HttpServletRequest hreq = request.getRequest();
        String contextPath = hreq.getContextPath();
        if (contextPath == null) {
            contextPath = "";
        }

        String requestURI = request.getRequestURI();
        if (requestURI.length() >= contextPath.length())
            requestPath = requestURI.substring(contextPath.length());

        String servletName = wrapper.getName();
        int count = 0;

        // 将符合条件的过滤器关联实例添加进过滤器链的集合中（按URL匹配）
        for (int i = 0; i < filterMaps.length; i++) {
            // 与此请求路径匹配的过滤器配置
            if (!matchFiltersURL(filterMaps[i], requestPath)) 
                continue;
            
            ApplicationFilterConfig filterConfig = (ApplicationFilterConfig) context.findFilterConfig(filterMaps[i].getFilterName());
            
            if (filterConfig != null) {
                filterChain.addFilter(filterConfig);
                count++;
            }
        }

        // 将符合条件的过滤器关联实例添加进过滤器链的集合中（按Servlet匹配）
        for (int i = 0; i < filterMaps.length; i++) {
            if (!matchFiltersServlet(filterMaps[i], servletName))
                continue;
            
            ApplicationFilterConfig filterConfig = (ApplicationFilterConfig) context.findFilterConfig(filterMaps[i].getFilterName());

            if (filterConfig != null) {
                filterChain.addFilter(filterConfig);
                count++;
            }
        }
        

        return filterChain;
    }


    /**
     * 判断servlet名是否与传入的过滤映射匹配
     *
     * @param filterMap
     * @param servletName
     * @return
     */
    private boolean matchFiltersServlet(FilterMap filterMap, String servletName) {
        if (servletName == null)
            return false;
        
        return servletName.equals(filterMap.getServletName());
    }


    /**
     * 判断请求路径是否与传入的过滤映射匹配
     * XXX 后面用动态规划进行优化
     * 
     * @param filterMap
     * @param requestPath
     * @return
     */
    private boolean matchFiltersURL(FilterMap filterMap, String requestPath) {
        if (requestPath == null) 
            return false;

        String urlPattern = filterMap.getUrlPattern();
        if (urlPattern == null)
            return false;
        
        // 情况一：完全匹配
        if (urlPattern.equals(requestPath))
            return true;
        
        
        // 情况二：路径匹配（"/*"）
        if ("/*".equals(urlPattern))
            return true;
        
        // 情况二的分支情况
        if (urlPattern.endsWith("/*")) {
            String comparePath = requestPath;
            while (true) {
                if (urlPattern.equals(comparePath + "/*"))
                    return true;
                
                int slash = comparePath.lastIndexOf('/');
                if (slash < 0) break;
                comparePath = comparePath.substring(0, slash);
            }
            return false;
        }
        
        // 情况三：扩展名匹配（"*."）
        if (urlPattern.startsWith("*.")) {
            int slash = requestPath.lastIndexOf('/');
            int period = requestPath.lastIndexOf('.');
            if (slash >= 0 && period > slash)
                return urlPattern.equals("*." + requestPath.substring(period + 1));
        }
        
        return false;
    }


    /**
     * 设置异常状态
     *
     * @param request
     * @param response
     * @param exception
     */
    private void exception(Request request, Response response, Throwable exception) {
        ServletRequest servletRequest = request.getRequest();
        servletRequest.setAttribute(Globals.EXCEPTION_ATTR, exception);

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    
}
