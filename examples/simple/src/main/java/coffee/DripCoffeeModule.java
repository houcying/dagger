package coffee;

import dagger.Module;
import dagger.Provides;

import java.util.Map;

import javax.inject.Singleton;

import static dagger.Provides.Type.SET;
import static dagger.Provides.Type.MAP;

@Module(
    injects = CoffeeApp.class,
    includes = PumpModule.class,
    library = true
)
class DripCoffeeModule {
  @Provides @Singleton Heater provideHeater() {
    return new ElectricHeater();
  }

  @Provides(type = SET) Flavor vanilla() {
    return new Flavor("vanilla");
  }
/*
  @Provides(type = MAP) Map.Entry<String, String> china() {
    return new MyLocation<String, String>("Monka", "US");
  }*/
}
