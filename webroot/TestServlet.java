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
	public static void main(String[] args) {
		
	}

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        String name = req.getParameter("name");
        String age = req.getParameter("age");
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        res.flushBuffer();
        out.println("<html>");
        out.println("<head><title>SessionServlet</title></head>");
        out.println("<body>");
        out.println("<br><hr>");
        out.println("<form>");
        out.println("New Value: <input name=value>");
        out.println("<input type=submit>");
        out.println("</form>");
        out.println("</body>");
        out.println("</html>");
    }
}
