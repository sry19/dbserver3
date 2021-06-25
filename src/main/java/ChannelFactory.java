import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class ChannelFactory extends BasePooledObjectFactory<Channel>  {

  private Connection conn;

  public ChannelFactory() throws IOException, TimeoutException {
    ConnectionFactory connectionFactory = new ConnectionFactory();
    //connectionFactory.setHost("localhost");
    connectionFactory.setHost("ec2-3-86-116-243.compute-1.amazonaws.com");
    connectionFactory.setUsername("user1");
    connectionFactory.setPassword("pass1");
    this.conn=connectionFactory.newConnection();
  }

  @Override
  public Channel create() throws Exception {
    return this.conn.createChannel();
  }

  @Override
  public PooledObject<Channel> wrap(Channel channel) {
    return new DefaultPooledObject<Channel>(channel);
  }
}
