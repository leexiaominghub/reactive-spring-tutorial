package max.lab.rst.s04;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import max.lab.rst.domain.Book;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * https://medium.com/@filia.aleks/microservice-performance-battle-spring-mvc-vs-webflux-80d39fd81bf0
 * https://medium.com/@kalpads/configuring-timeouts-in-spring-reactive-webclient-4bc5faf56411
 */

@Slf4j
public class C01WebClientShowcases {
    public static void main(String[] args) {
        log.info("start");
        var book = Book.builder().isbn(String.valueOf(System.currentTimeMillis()))
                .category("TEST")
                .title("Book from Webclient")
                .price(BigDecimal.valueOf(23.99))
                .build();

        //AtomicReference<Mono<Book>> bookMono = new AtomicReference<>();
        //var webClient = WebClient.create("http://localhost:8080/routed");
/*
        webClient.post().uri("/book")
            .body(Mono.just(book), Book.class)
            .exchange() // exchange不会抛出异常，retrieve会抛出异常
            .doOnNext(
                clientResponse -> {
                  System.out.println(">>>>>>>> POST RESPONSE STATUS CODE: " + clientResponse.statusCode());

                  // 如果把mono放在main里，就会启动新线程，否则，就在nio线程, 但仍然会释放channel
                  // 异步执行, 下面的block不会等
                  webClient.get().uri("/book/{isbn}", book.getIsbn()) // 会在新线程启动, 但是这里main线程必须先解锁吗？
                          .retrieve()
                          .bodyToMono(Book.class)
                          .doOnNext(aBook -> System.out.println(">>>>>>> GET BOOK: " + aBook)).subscribe(); // 在这里不可以block, 若无subscribe则不会执行
                  System.out.println("done");

                }
            ).block(); // 日志显示这里client释放了channel， 如何复用channel呢？看后面的例子，需要配置HttpClient; 这里的释放可能并不是真的释放tcp连接, 因为是池化的连接

      log.info("block done");
      try {
        Thread.currentThread().join(); // 启动客户端以后，如果使客户端线程退出呢？block并不会退出
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

            webClient.get().uri("/book/{isbn}", book.getIsbn())
              .retrieve()
            .bodyToMono(Book.class)
            .doOnNext(aBook -> System.out.println(">>>>>>> GET BOOK: " + aBook))
            .block();

        book.setPrice(BigDecimal.valueOf(39.99));
        webClient.put().uri("/book/{isbn}", book.getIsbn())
            .body(Mono.just(book), Book.class)
            .exchange()
            .doOnNext(
                clientResponse -> System.out.println(">>>>>>>> PUT RESPONSE STATUS CODE: " + clientResponse.statusCode())
            ).block();

        webClient.get().uri("/books")
            .retrieve()
            .bodyToFlux(Book.class)
            .doOnNext(aBook -> System.out.println(">>>>>>> GET BOOKS: " + aBook))
            .blockLast();

        webClient.delete().uri("/book/{isbn}", book.getIsbn())
            .exchange()
            .doOnNext(
                clientResponse -> System.out.println(">>>>>>>> DELETE RESPONSE STATUS CODE: " + clientResponse.statusCode())
            ).block();
*/

      // 异常处理
/*
        webClient.post().uri("/book")
            .body(Mono.just(book), Book.class)
            .exchange()
            .flatMap(clientResponse -> {
                if (clientResponse.statusCode() != HttpStatus.CREATED) {
                    return clientResponse.createException().flatMap(Mono::error); // 会抛出异常
                }
                System.out.println(">>>>>>>> POST RESPONSE STATUS CODE: " + clientResponse.statusCode());
                return Mono.just(clientResponse);
            })
            //.retryBackoff(3, Duration.ofSeconds(1)) // 间隔重试
                //.retry() // 不停地尝试连接
            .block();
*/

      // 自定义webclient
/*
        var httpClient = HttpClient.create()
                            .tcpConfiguration(
                                tcpClient -> {
                                    tcpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
                                        .doOnConnected(
                                            connection -> connection.addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                                        );
                                    return tcpClient;
                                }
                            );
        var connector = new ReactorClientHttpConnector(httpClient);                    
        var webClientWithHttpTimeout = WebClient.builder()
                                        .clientConnector(connector)
                                        .build();
*/


      // 连接池参考文档： https://projectreactor.io/docs/netty/release/reference/index.html#_connection_pool_2
      // 连接提供者
      ConnectionProvider provider =
              ConnectionProvider.builder("custom")
                      .maxConnections(10)
                      .maxIdleTime(Duration.ofSeconds(20))
                      .maxLifeTime(Duration.ofSeconds(60))
                      .pendingAcquireTimeout(Duration.ofSeconds(60))
                      .evictInBackground(Duration.ofSeconds(120))
                      .build();

      // 新版webclient
      HttpClient httpClient1 = HttpClient.create(provider).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000).baseUrl("http://localhost:8080/routed");
      WebClient webClient1 = WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient1)).build();

      // 发送一本书
      webClient1
              .post()
              .uri("/book")
              .body(Mono.just(book), Book.class)
              .retrieve()
              .toBodilessEntity()
              .doOnNext(s -> log.info("{}", s.getStatusCode()))
              //.repeat()
              //.block()
              .subscribe();


      // 获取所有书
      webClient1
              .get()
              .uri("/books")
              .exchangeToFlux(response -> {
                return response.bodyToFlux(Book.class);
              })
              .doOnNext(s -> log.info("{}", s))
              .blockLast();


      //17:46:16.248 [reactor-http-nio-3] DEBUG reactor.netty.resources.PooledConnectionProvider - [id:195fa694, L:/127.0.0.1:53426 - R:localhost/127.0.0.1:8080]
      // Channel connected,
      // now: 2 active connections, 0 inactive connections and 0 pending acquire requests.0
      // 已被分配的连接，空闲的连接，排队等待连接的个数
      // 解释见reactor.netty.resources.PooledConnectionProvider#logPoolState, reactor.pool.InstrumentedPool.PoolMetrics.pendingAcquireSize
    }
}
