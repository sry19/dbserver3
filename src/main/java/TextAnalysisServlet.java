import com.google.gson.Gson;
import io.swagger.client.model.ErrMessage;
import io.swagger.client.model.ResultVal;
import io.swagger.client.model.TextLine;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.pool2.impl.GenericObjectPool;
import com.rabbitmq.client.Channel;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;



/**
 * The type Text analysis servlet.
 */
@WebServlet(name = "TextAnalysisServlet")
public class TextAnalysisServlet extends HttpServlet {

  private final static String QUEUE_NAME = "threadExQ";
  private GenericObjectPool<Channel> genericObjectPool;

  public void init() {
    // load properties from disk, do be used by subsequent doGet() calls
    try {
      this.genericObjectPool = new GenericObjectPool<>(new ChannelFactory());
      this.genericObjectPool.setMinIdle(1000);
    } catch (IOException | TimeoutException e) {
      System.out.println("exception");
      e.printStackTrace();
    }

  }

  protected void doPost(HttpServletRequest request,
      HttpServletResponse response)
      throws IOException {

    // get uri
    String urlPath = request.getPathInfo();

    // if uri is empty or only has '/', then it is an invalid uri without parameters
    if (urlPath == null || urlPath.isEmpty() || urlPath.split("/").length<=1) {
      response.setContentType("application/json");
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("missing parameters");
      return;
    }

    // split url
    String[] urlParts = urlPath.split("/");

    StringBuilder jb = new StringBuilder();
    String line = null;

    try {

      Channel channel = this.genericObjectPool.borrowObject();
      channel.queueDeclare(QUEUE_NAME, true, false, false, null);

      // read request body line by line
      BufferedReader reader = request.getReader();
      while ((line = reader.readLine()) != null)
        jb.append(line);

      // convert the request body to TextLine
      TextLine text = new Gson().fromJson(jb.toString(), TextLine.class);

      response.setContentType("application/json");

      // if the function is wordcount, then count the number of words and send back
      if (urlParts[1].equals("wordcount")) {
        response.setStatus(HttpServletResponse.SC_OK);
        ResultVal resultVal = new ResultVal();
        int uniqueWord = handler(text.getMessage(),channel);
        this.genericObjectPool.returnObject(channel);
        resultVal.setMessage(uniqueWord);
        response.getWriter().write(new Gson().toJson(resultVal));
        // other functions
      } else {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        ErrMessage errMessage = new ErrMessage();
        errMessage.setMessage("function need to be added");
        response.getWriter().write(new Gson().toJson(errMessage));
      }
      // if there is any exception, return an error message
    } catch (Exception e) {
      response.setContentType("application/json");
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      ErrMessage errMessage = new ErrMessage();
      errMessage.setMessage(e.toString());
      //System.out.println(e.getMessage());
      response.getWriter().write(new Gson().toJson(errMessage));
    }
  }

  protected void doGet(HttpServletRequest request,
      HttpServletResponse response)
      throws ServletException, IOException {

  }

  private int handler(String text,Channel channel) throws IOException {
    String[] wordList = text.split(" ");
    Map<String, Integer> counter = new HashMap<String, Integer>();
    for (String s: wordList) {
      int count = counter.containsKey(s) ? counter.get(s) + 1 : 1;
      counter.put(s, count);
    }
    int count = 0;
    for (Map.Entry<String, Integer> entry: counter.entrySet()) {
      String message = entry.getKey() + " " + entry.getValue();
      if (entry.getValue() == 1) {
        count += 1;
      }
      channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
    }
    return count;
  }
}


