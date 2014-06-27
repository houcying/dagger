package coffee;

import dagger.Component;

@Component(modules = { DripCoffeeModule.class, PumpModule.class, MilkFlavorModule.class })
interface CoffeeMain {
  CoffeeMaker getMaker();
}

