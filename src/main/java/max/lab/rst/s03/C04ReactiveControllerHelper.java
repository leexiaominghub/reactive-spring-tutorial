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
    public interface ExtraValidator<T> {
        void validate(T t, Errors errors);
    }

    public static <T> Mono<T> validate(Validator validator,
                                       Mono<T> mono) {
        return validate(validator, null, mono);
    }

    public static <T> Mono<T> validate(Validator validator,
                                    @Nullable ExtraValidator<T> functionalValidator,
                                    Mono<T> mono) {
        Assert.notNull(validator, "validator must NOT be null");
        Assert.notNull(mono, "mono must NOT be null");

        return mono.flatMap(t -> {
            Errors errors = new BeanPropertyBindingResult(t, t.getClass().getName());
            validator.validate(t, errors);
            if (functionalValidator != null) {
                functionalValidator.validate(t, errors);
            }
            if (errors.hasErrors()) {
                return Mono.error(new ValidationException(errors));
            }
            return Mono.just(t);
        });
    }

    public static <T> Mono<T> requestBodyToMono(ServerRequest request,
                                                Validator validator,
                                                Class<T> clz) {
        return validate(validator, request.bodyToMono(clz));
    }

    public static <T> Mono<T> requestBodyToMono(ServerRequest request,
                                                Validator validator,
                                                ExtraValidator extraValidator,
                                                Class<T> clz) {
        return validate(validator, extraValidator, request.bodyToMono(clz));
    }

    public static <T> T convertValue(ObjectMapper objectMapper,
                                     MultiValueMap<String, String> map,
                                     Class<T> clz) {
        Assert.notNull(objectMapper, "objectMapper must NOT be null");
        Assert.notNull(clz, "clz must NOT be null");
        if (map == null) {
            return null;
        }

        var theMap = map.entrySet().stream().map(e -> {
            String key = e.getKey();
            List<String> list = e.getValue();
            if (list == null) {
                return (new SimpleEntry<>(key, list));
            }
            if (list.size() == 1) {
                return (new SimpleEntry<>(key, list.get(0)));
            }
            return e;
        }).collect(toMap(Entry::getKey, Entry::getValue));
        return objectMapper.convertValue(theMap, clz);
    }

    public static <T> Mono<T> queryParamsToMono(ServerRequest request,
                                                ObjectMapper objectMapper,
                                                Class<T> clz,
                                                Validator validator) {
        Assert.notNull(request, "request must NOT be null");
        Assert.notNull(objectMapper, "objectMapper must NOT be null");
        Assert.notNull(clz, "clz must NOT be null");
        var mono = Mono.just(request)
                .flatMap(theRequest -> Mono.just(convertValue(objectMapper,
                        theRequest.queryParams(), clz)));
        return validate(validator, mono);
    }
}
