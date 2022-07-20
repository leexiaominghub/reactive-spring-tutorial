package max.lab.rst.lxmtest;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class test {

  public static void main(String[] args) {

/*
    //String str = null;
    String str = "hi world";
    Mono.justOrEmpty(str).map(t -> {return  t + "1";}).switchIfEmpty(Mono.justOrEmpty("switch")).subscribe(System.out::println);
*/

/*
    String str1 = "hi world";

    Mono.error(new RuntimeException()).then(Mono.just(str1)).subscribe(System.out::println);
    log.info("————");
    Mono.error(new RuntimeException()).thenReturn(str1).subscribe(System.out::println);
*/

    String str = "hi";

    Mono.justOrEmpty(str)
            .doOnNext(t -> System.out.println(t + "onNext"))
            .subscribe(System.out::println);


  }
}
