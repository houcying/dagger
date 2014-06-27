package coffee;

import javax.inject.Inject;

import dagger.ObjectGraph;

public class CoffeeApp {
  public static void main(String[] args) {
    CoffeeMain coffee = new Dagger_CoffeeMain(new DripCoffeeModule(), new PumpModule(), new MilkFlavorModule());
    coffee.getMaker().brew();
  }
}
