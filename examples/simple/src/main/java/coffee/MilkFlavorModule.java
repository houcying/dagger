package coffee;

import static dagger.Provides.Type.MAP;

import dagger.Module;
import dagger.Provides;
// 
@Module
class MilkFlavorModule {
  @Provides(type = MAP)
  @StringKey("Chocolate")
  FlavorProcessor provideChocolateProcessor() {
    return new ChocolateFlavorProcessor();
  }
}
