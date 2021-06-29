import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.google.gson.Gson;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.pool2.impl.GenericObjectPool;

@WebServlet(name = "WordCountServlet")
public class WordCountServlet extends HttpServlet {

  private Table table;

  public void init() {
    AmazonDynamoDB client = AmazonDynamoDBClientBuilder
        .standard()
        .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("dynamodb.us-east-1.amazonaws.com", "us-east-1"))
        .build();
    DynamoDB dynamoDB = new DynamoDB(client);
    Table table = dynamoDB.getTable("countTable");
    this.table = table;

  }

  protected void doPost(HttpServletRequest request,
      HttpServletResponse response)
      throws ServletException, IOException {

  }

  protected void doGet(HttpServletRequest request,
      HttpServletResponse response)
      throws ServletException, IOException {


    // get uri
    String urlPath = request.getPathInfo();

    // if uri is empty or only has '/', then it is an invalid uri without parameters
    if (urlPath == null || urlPath.isEmpty() || urlPath.split("/").length<=1) {
      response.setContentType("application/json");
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().write("Invalid inputs");
      return;
    }

    // split url
    String[] urlParts = urlPath.split("/");
    String word = urlParts[1];

    GetItemSpec spec = new GetItemSpec().withPrimaryKey("word", word);

    try {
      System.out.println("Attempting to read the item...");
      Item outcome = table.getItem(spec);
      //response.setContentType("text/plain;charset=UTF-8");
      response.setStatus(HttpServletResponse.SC_OK);
      //ResultVal resultVal = new ResultVal();
      BigDecimal res = outcome.getNumber("counterme");
      int intres = res.intValue();
      //resultVal.setMessage(intres);
      //System.out.println(intres);
      //response.getWriter().write(new Gson().toJson(resultVal));
      response.getWriter().print(intres);

    }
    catch (Exception e) {
      System.err.println("Unable to read item: " + word);
      System.out.println(e.getMessage());
      response.setContentType("application/json");
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getWriter().write("word not found");
    }

  }
}
