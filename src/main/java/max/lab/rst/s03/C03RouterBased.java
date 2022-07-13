package max.lab.rst.s03;

import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import max.lab.rst.domain.Book;
import max.lab.rst.domain.BookQuery;
import max.lab.rst.domain.InMemoryDataSource;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class C03RouterBased {
    private static final String PATH_PREFIX = "/routed/";

    private final Validator validator; // 这个只会做domain类里规定的指定校验，所以讲者做了一个额外校验的接口
    private final ObjectMapper objectMapper;

    @Bean
    public RouterFunction<ServerResponse> routers() {
        return RouterFunctions.route() // 可以做更多动态的设置？
                .POST(PATH_PREFIX + "book", this::create) // 非幂等, 同一本书校验出错
                .GET(PATH_PREFIX + "books", this::findAll)
                .GET(PATH_PREFIX + "query-books", this::findByPage)
                .GET(PATH_PREFIX + "book/{isbn}", this::find)
                .PUT(PATH_PREFIX + "book/{isbn}", this::update) // 幂等操作， 同一本书可以
                .DELETE(PATH_PREFIX + "book/{isbn}", this::delete)
                .build();
    }

    private Mono<ServerResponse> findByPage(ServerRequest request) {
        return C04ReactiveControllerHelper.queryParamsToMono(request, objectMapper,
                    BookQuery.class, validator)
                .flatMap(query -> ServerResponse.ok()
                        .bodyValue(InMemoryDataSource.findBooksByQuery(query)));
    }

    private Mono<ServerResponse> delete(ServerRequest request) {
        var isbn = request.pathVariable("isbn");
        return InMemoryDataSource.findBookMonoById(isbn)
                .flatMap(book -> {
                    InMemoryDataSource.removeBook(book);
                    return ServerResponse.ok().build();
                }).switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> update(ServerRequest request) {
        var isbn = request.pathVariable("isbn");
        return InMemoryDataSource.findBookMonoById(isbn)
                .flatMap(book ->
                        C04ReactiveControllerHelper
                                .requestBodyToMono(request, validator, Book.class)
                                .map(InMemoryDataSource::saveBook)
                                .flatMap(b -> ServerResponse.ok().build())
                ).switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> find(ServerRequest request) {
        var isbn = request.pathVariable("isbn");
        return InMemoryDataSource.findBookMonoById(isbn)
                .flatMap(ServerResponse.ok()::bodyValue) // 可否？
                //.flatMap(book -> ServerResponse.ok().bodyValue(book))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> findAll(ServerRequest request) {
        var books = InMemoryDataSource.findAllBooks();
        return ServerResponse.ok().bodyValue(books);
    }

    private static final AtomicInteger counter = new AtomicInteger(1);

    private Mono<ServerResponse> create(ServerRequest request) {  
//        if (counter.getAndIncrement() < 3) {
//            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//        counter.set(1);

        log.debug("lxm create");
        return C04ReactiveControllerHelper.requestBodyToMono(request, validator,
                (t, errors) -> InMemoryDataSource.findBookMonoById(t.getIsbn()) // 校验唯一性。 新版本的webflux支持注解校验了吗？
                            .map((book -> {
                                errors.rejectValue("isbn", "already.exists", "Already exists");
                                return Tuples.of(book, errors);
                            }))
//                (t, errors) -> {
//                    Optional<Book> theBook = InMemoryDataSource.findBookById(t.getIsbn());
//                    if (theBook.isPresent()) {
//                        errors.rejectValue("isbn", "already.exists", "Already exists");
//                    }
//                    return Tuples.of(t, errors);
//                }
                , Book.class) // 如果校验没通过，这里会被截断？
                .map(InMemoryDataSource::saveBook)
                .flatMap(book -> ServerResponse.created( // 用then不行。then是结束一个mono。thenReturn区别。自动创建了201返回码
                    UriComponentsBuilder.fromHttpRequest(request.exchange().getRequest()) // 返回serverHttpRequest
                            .path("/").path(book.getIsbn()).build().toUri())
                        .build());
    }
}
