package coffee;

import javax.inject.Inject;
import javax.inject.Provider;

import dagger.Lazy;
import dagger.ObjectGraph;

import java.util.HashMap;
import java.util.Map;

public class CoffeeApp {
  public static void main(String[] args) {
    CoffeeMain coffee = new Dagger_CoffeeMain(new DripCoffeeModule(), new PumpModule(), new MilkFlavorModule());
    coffee.getMaker().brew();
  }
}
