package coffee;

import static dagger.Provides.Type.MAP;
import static dagger.Provides.Type.SET;

import dagger.Module;
import dagger.Provides;

@Module(complete = false, library = true)
class PumpModule {
  @Provides Pump providePump(Thermosiphon pump) {
    return pump;
  }
  
  @Provides(type = MAP)
//  @StringKey("Vanilla")
  FlavorProcessor provideVanillaProcessor() {
    return new VanillaFlavorProcessor();
  }
  
  //@Provides(type = SET)
  //@StringKey("Vanilla")
  Integer provideTwo() {
    return 2;
  }
}

