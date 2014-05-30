package coffee;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import dagger.Provides.Type;

@Module(
    injects = CoffeeApp.class,
    includes = PumpModule.class
)
class DripCoffeeModule {
  @Provides @Singleton Heater provideHeater() {
    return new ElectricHeater();
  }
  @Provides(type = Type.MAP) Flavor provideFlavor() {
    return new Flavor("Chocoloate");
  }
}
