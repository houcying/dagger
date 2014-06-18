package coffee;

import dagger.Module;
import dagger.Provides;

import java.util.Collections;

import static dagger.Provides.Type.SET;

import javax.inject.Singleton;

@Module(
    injects = CoffeeApp.class,
    includes = PumpModule.class
)
class DripCoffeeModule {
  @Provides @Singleton Heater provideHeater() {
    return new ElectricHeater();
  }
  
  @Provides(type = SET) Flavor providesVanillaFlavor() {
    return new Flavor("vanilla");
  }
}
