package coffee;

import static dagger.Provides.Type.MAP;

import dagger.Module;
import dagger.Provides;

@Module(complete = false, library = true)
class PumpModule {
  @Provides Pump providePump(Thermosiphon pump) {
    return pump;
  }
  
  @Provides(type = MAP)
  @StringKey("Vanilla")
  FlavorProcessor provideVanillaProcessor() {
    return new VanillaFlavorProcessor();
  }
}

