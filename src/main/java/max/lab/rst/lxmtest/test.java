package max.lab.rst.lxmtest;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class test {

  public static void main(String[] args) {

    String str = null;
    //String str = "hi world";
    Mono.justOrEmpty(str).map(t -> {return  t + "1";}).log().subscribe(System.out::println);


  }
}
