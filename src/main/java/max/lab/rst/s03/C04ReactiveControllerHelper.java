package max.lab.rst.s03;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map.Entry;

import static java.util.stream.Collectors.toMap;

public class C04ReactiveControllerHelper {
    @RequiredArgsConstructor
    @Getter
    public static class ValidationException extends RuntimeException {
        private final Errors errors;
    }

    @FunctionalInterface
    public interface ExtraValidator<T> { // 额外校验的接口
        Mono<Tuple2<T, Errors>> validate(T t, Errors errors);
    }

    public static <T> Mono<T> validate(Validator validator,
                                       Mono<T> mono) {
        return validate(validator, null, mono);
    }

    public static <T> Mono<T> validate(Validator validator,
                                    @Nullable ExtraValidator<T> extraValidator,
                                    Mono<T> mono) {
        Assert.notNull(validator, "validator must NOT be null");
        Assert.notNull(mono, "mono must NOT be null");

        return mono.flatMap(t -> { // 做两次校验，返回元素和错误
                    Errors errors = new BeanPropertyBindingResult(t, t.getClass().getName()); // 这是啥？ 把两次校验的错误都填写到这里
                    validator.validate(t, errors);
                    Mono<Tuple2<T, Errors>> aMono = Mono.empty();
                    if (extraValidator != null) {
                        aMono = extraValidator.validate(t, errors);
                    }
                    return aMono.switchIfEmpty(Mono.just(Tuples.of(t, errors))); // Ensure there will data flowing in the pipeline, 上面可能返回空
                }) // 为什么不用map呢？
                //.map(tuple2 -> {return tuple2.getT2().hasErrors()? Mono.error(new ValidationException(tuple2.getT2())): Mono.just(tuple2.getT1());}); // 为何不可？
               .flatMap(tuple2 -> tuple2.getT2().hasErrors()? Mono.error(new ValidationException(tuple2.getT2())): Mono.just(tuple2.getT1()));
/*
                .flatMap(tuple2 -> {
                    var errors = tuple2.getT2();
                    if (errors.hasErrors()) {
                        return Mono.error(new ValidationException(errors)); // 抛出异常， 确切说是返回校验异常
                    }
                    return Mono.just(tuple2.getT1()); // 校验通过， 返回正常值
                });
*/
    }

    public static <T> Mono<T> requestBodyToMono(ServerRequest request,
                                                Validator validator,
                                                Class<T> clz) {
        return validate(validator, request.bodyToMono(clz));
    }

    public static <T> Mono<T> requestBodyToMono(ServerRequest request,
                                                Validator validator,
                                                @Nullable ExtraValidator<T> extraValidator,
                                                Class<T> clz) {
        return validate(validator, extraValidator, request.bodyToMono(clz));
    }

    public static <T> T convertValue(ObjectMapper objectMapper,
                                     @Nullable MultiValueMap<String, String> map, // api 返回的就是这类型, 一个键，多个值
                                     Class<T> clz) {
        Assert.notNull(objectMapper, "objectMapper must NOT be null");
        Assert.notNull(clz, "clz must NOT be null");
        if (map == null) {
            return null;
        }

        var theMap = map.entrySet().stream().map(e -> {
            String key = e.getKey();
            List<String> list = e.getValue();
            if (list != null && list.size() == 1) {
                return (new SimpleEntry<>(key, list.get(0)));
            }
            return e; // 为何不直接返回？
        }).collect(toMap(Entry::getKey, Entry::getValue));
        return objectMapper.convertValue(theMap, clz);
    }

    public static <T> Mono<T> queryParamsToMono(ServerRequest request,
                                                ObjectMapper objectMapper,
                                                Class<T> clz,
                                                Validator validator) {
        return queryParamsToMono(request, objectMapper, clz, validator, null);
    }

    public static <T> Mono<T> queryParamsToMono(ServerRequest request,
                                                ObjectMapper objectMapper,
                                                Class<T> clz,
                                                Validator validator,
                                                @Nullable ExtraValidator<T> extraValidator) {
        Assert.notNull(request, "request must NOT be null");
        Assert.notNull(objectMapper, "objectMapper must NOT be null");
        Assert.notNull(clz, "clz must NOT be null");
        Assert.notNull(validator, "validator must NOT be null");

        var mono = Mono.just(convertValue(objectMapper, request.queryParams(), clz)); // 只是转换mono的方式不同
        return validate(validator, extraValidator, mono);
    }
}
