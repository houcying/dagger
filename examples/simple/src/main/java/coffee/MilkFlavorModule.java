package coffee;

import static dagger.Provides.Type.MAP;
import static dagger.Provides.Type.SET;

import dagger.Module;
import dagger.Provides;

@Module
class MilkFlavorModule {
 // @Provides(type = MAP)
  @StringKey("Chocolate")
  FlavorProcessor provideChocolateProcessor() {
    return new ChocolateFlavorProcessor();
  }
  
  @Provides(type = SET)
  Integer provideOne() {
    return 1;
  }
  
}
