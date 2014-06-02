package coffee;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;


@Module(
    injects = CoffeeApp.class,
    includes = PumpModule.class
)
class DripCoffeeModule {
  @Provides @Singleton Heater provideHeater() {
    return new ElectricHeater();
  }
  @Provides(type = Provides.Type.SET) Flavor provideFlavor() {
    return new Flavor("vanilla");
  }
  @Provides(type = Provides.Type.SET) Flavor provideFlavor2() {
    return new Flavor("chocolate");
  }
}

