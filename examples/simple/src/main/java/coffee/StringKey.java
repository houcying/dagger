package coffee;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import dagger.MapKey;

import java.lang.annotation.Retention;

@MapKey
@Retention(RUNTIME)
public @interface StringKey {
  String value(); // Or Enum value(); using annotationMirror to obtain the concrete value, compile
                  // reflection
}
