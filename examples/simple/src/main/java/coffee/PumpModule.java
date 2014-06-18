package coffee;

import static dagger.Provides.Type.SET;

import dagger.Module;
import dagger.Provides;

@Module(complete = false, library = true)
class PumpModule {
  @Provides Pump providePump(Thermosiphon pump) {
    return pump;
  }
  
  @Provides(type = SET) Flavor providesChocoloateFlavor() {
    return new Flavor("chocolate");
  }
}
