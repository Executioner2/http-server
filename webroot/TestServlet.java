import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Title: TestServlet
 * Description:
 *
 * @Author 2Executioner
 * @Email 1205878539@qq.com
 * @Date 2022-03-18 17:07
 */
public class TestServlet extends HttpServlet {

    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println("init method execute");
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        System.out.println("service method start");
        System.out.println("req:  " + req);
        System.out.println("res:  " + res);
        String value = req.getParameter("value");
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        res.flushBuffer();
        out.println("<html>");
        out.println("<head><title>SessionServlet</title></head>");
        out.println("<body>");
        out.println("<br><hr>");
        out.println("<form>");
        out.println("New Value: <input name=value value=" + value + ">");
        out.println("<input type=submit>");
        out.println("</form>");
        out.println("</body>");
        out.println("</html>");
        System.out.println("service method orver");
    }
}
