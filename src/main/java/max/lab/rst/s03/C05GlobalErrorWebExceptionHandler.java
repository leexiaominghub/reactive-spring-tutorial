package max.lab.rst.s03;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import max.lab.rst.s03.C04ReactiveControllerHelper.ValidationException;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.validation.ObjectError;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@Component
@Order(-2) // 说明这类似一个切面
public class C05GlobalErrorWebExceptionHandler extends
        AbstractErrorWebExceptionHandler { // 是因为继承这个处理器所以才有自动处理错误的功能吗？
    public C05GlobalErrorWebExceptionHandler(ErrorAttributes errorAttributes,
                                             ResourceProperties resourceProperties,
                                             ApplicationContext applicationContext,
                                             ServerCodecConfigurer configurer) { // 是这个配置的错误信息的中英文吗？
        super(errorAttributes, resourceProperties, applicationContext);
        this.setMessageWriters(configurer.getWriters());
    }

    @RequiredArgsConstructor
    @Data
    private static class Error {
        final List<InvalidField> invalidFields;
        final List<String> errors;
    }

    @RequiredArgsConstructor
    @Data
    private static class InvalidField {
        final String name;
        final String message;
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), (serverRequest -> { // 这里的all路由了所有路径？
            var throwable = errorAttributes.getError(serverRequest);
            if (throwable instanceof ValidationException) { // 这是自定义的异常
                return handleValidationException((ValidationException) throwable);
            }
            if (throwable instanceof ResponseStatusException) {
                return handleResponseStatusException((ResponseStatusException) throwable);
            }
            log.error("Ops, just caught an unknown exception, " +
                    "please have a look at the stack trace of more details", throwable);
            return ServerResponse.status(INTERNAL_SERVER_ERROR).build();
        }));
    }

    private Mono<ServerResponse> handleResponseStatusException(ResponseStatusException exception) {
        var error = new Error(null, Arrays.asList(exception.getReason()));
        return ServerResponse.status(exception.getStatus())
                .bodyValue(error);
    }

    private Mono<ServerResponse> handleValidationException(ValidationException exception) { // 与前面的错误处理实现类似？哪个？
        log.debug("lxm handleValidationException");
        var errors = exception.getErrors();
        var invalidFields = errors.getFieldErrors().stream()
                .map(error -> new InvalidField(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());
        var theErrors = errors.getGlobalErrors().stream() // 全局错误？
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.toList());
        var error = new Error(invalidFields, theErrors); // 返回客户端一个自定义的Error
        return ServerResponse.badRequest().bodyValue(error);
    }
}
