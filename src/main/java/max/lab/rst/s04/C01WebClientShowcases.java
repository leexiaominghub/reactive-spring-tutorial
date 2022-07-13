package max.lab.rst.s04;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import max.lab.rst.domain.Book;
import reactor.core.publisher.Mono;

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
        var webClient = WebClient.create("http://localhost:8080/routed");
        webClient.post().uri("/book")
            .body(Mono.just(book), Book.class)
            .exchange()
            .doOnNext(
                clientResponse -> {
                  System.out.println(">>>>>>>> POST RESPONSE STATUS CODE: " + clientResponse.statusCode());

                  // 如果把mono放在main里，就会启动新线程，否则，就在nio线程, 但仍然会释放channel
                  webClient.get().uri("/book/{isbn}", book.getIsbn()) // 会在新线程启动, 但是这里main线程必须先解锁吗？
                          .retrieve()
                          .bodyToMono(Book.class)
                          .doOnNext(aBook -> System.out.println(">>>>>>> GET BOOK: " + aBook)).subscribe(); // 在这里不可以block, 若无subscribe则不会执行
                  System.out.println("done");

                }
            ).block(); // 日志显示这里client释放了channel， 如何复用channel呢？

      try {
        Thread.currentThread().join(); // 启动客户端以后，如果使客户端线程退出呢？block并不会退出
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

/*
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
/*
        webClient.post().uri("/book")
            .body(Mono.just(book), Book.class)
            .exchange()
            .flatMap(clientResponse -> {
                if (clientResponse.statusCode() != HttpStatus.CREATED) {
                    return clientResponse.createException().flatMap(Mono::error);
                }
                System.out.println(">>>>>>>> POST RESPONSE STATUS CODE: " + clientResponse.statusCode());
                return Mono.just(clientResponse);
            })
            .retryBackoff(3, Duration.ofSeconds(1))
            .block();
*/

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
    }
}
