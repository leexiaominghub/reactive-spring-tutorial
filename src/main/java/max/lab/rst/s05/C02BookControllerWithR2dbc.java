/*
package max.lab.rst.s05;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import max.lab.rst.domain.Book;
import max.lab.rst.domain.BookQuery;
import max.lab.rst.s03.C04ReactiveControllerHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

@RequiredArgsConstructor
@Configuration
public class C02BookControllerWithR2dbc {
    private static final String PATH_PREFIX = "/routed-r2dbc/";

    private final C01BookRepository bookRepository;
    private final Validator validator;
    private final ObjectMapper objectMapper;
    private final TransactionalOperator transactionalOperator;

    @Bean("r2dbcBookRouter")
    public RouterFunction<ServerResponse> routers() {
        return RouterFunctions.route()
                .POST(PATH_PREFIX + "book", this::create)
                .GET(PATH_PREFIX + "books", this::findAll)
                .GET(PATH_PREFIX + "query-books", this::findByPage)
                .GET(PATH_PREFIX + "book/{isbn}", this::find)
                .PUT(PATH_PREFIX + "book/{isbn}", this::update)
                .DELETE(PATH_PREFIX + "book/{isbn}", this::delete)
                .POST(PATH_PREFIX + "books", this::createMany) // 新增事务？
                .build();
    }

    public Mono<ServerResponse> createMany(ServerRequest request) {
        return request.bodyToFlux(Book.class)
                .flatMap(book -> bookRepository.insert(book)) // 为什么不能用doOnNext？ 这是因为doOnNext里面写insert只是声明了这些pipeline，而没有真正的把它们链接到webflux的subscriber，所以不会有数据在里面被subscribe。（这点很重要，也是刚用Reactive时比较容易中的坑）
                .then(ServerResponse.ok().build())
                .as(transactionalOperator::transactional); // 事务管理
    }

    private Mono<ServerResponse> findByPage(ServerRequest request) {
        return C04ReactiveControllerHelper.queryParamsToMono(request, objectMapper,
                    BookQuery.class, validator)
                .flatMap(query -> ServerResponse.ok()
                        .body(bookRepository.findBooksByQuery(query), Book.class)); // 以前是bodyValue, 参数是对象
    }

    private Mono<ServerResponse> delete(ServerRequest request) {
        var isbn = request.pathVariable("isbn");
        return bookRepository.findById(isbn)
                .flatMap(book -> 
                        bookRepository.delete(isbn)
                                .then(ServerResponse.ok().build()) // 与thenReturn的区别，不甚了了
                ).switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> update(ServerRequest request) {
        var isbn = request.pathVariable("isbn");
        return bookRepository.findById(isbn)
                .flatMap(book ->
                        C04ReactiveControllerHelper
                                .requestBodyToMono(request, validator, Book.class)
                                .flatMap(aBook -> 
                                        bookRepository.update(aBook).then(ServerResponse.ok().build())
                                )
                ).switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> find(ServerRequest request) {
        var isbn = request.pathVariable("isbn");
        return bookRepository.findById(isbn)
                .flatMap(book -> ServerResponse.ok().bodyValue(book))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    private Mono<ServerResponse> findAll(ServerRequest request) {
        return ServerResponse.ok().body(bookRepository.findAll(), Book.class);
    }

    private Mono<ServerResponse> create(ServerRequest request) {         
        return C04ReactiveControllerHelper.requestBodyToMono(request, validator,
                (t, errors) -> bookRepository.findById(t.getIsbn())
                            .map((book -> {
                                errors.rejectValue("isbn", "already.exists", "Already exists");
                                return Tuples.of(book, errors);
                            }))
                , Book.class)
                .flatMap(book -> bookRepository.insert(book)
                        .then(ServerResponse.created(UriComponentsBuilder.fromHttpRequest(request.exchange().getRequest()).path("/").path(book.getIsbn()).build().toUri()).build()));
*/
/*
                .flatMap(book -> bookRepository.insert(book).thenReturn(book))
                .flatMap(book -> ServerResponse.created(
                    UriComponentsBuilder.fromHttpRequest(request.exchange().getRequest())
                            .path("/").path(book.getIsbn()).build().toUri())
                        .build());
*//*

    }
}
*/
