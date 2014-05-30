package coffee;

import dagger.Module;
import dagger.Provides;

@Module(complete = false, library = true, injects = CoffeeApp.class)
class PumpModule {
  @Provides Pump providePump(Thermosiphon pump) {
    return pump;
  }
}
