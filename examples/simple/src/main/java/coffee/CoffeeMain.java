package coffee;

import dagger.Component;
import java.util.Set;

@Component(modules = { DripCoffeeModule.class, PumpModule.class })
interface CoffeeMain {
  CoffeeMaker getMaker();
  Set<Flavor> flavors();
}
