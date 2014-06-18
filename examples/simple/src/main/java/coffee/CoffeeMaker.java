package coffee;

import dagger.Lazy;

import java.util.Set;

import javax.inject.Inject;

class CoffeeMaker {
  final Lazy<Heater> heater; // Don't want to create a possibly costly heater until we need it.
  final Pump pump;
  final Provider<Flavor> flavors;
  
  @Inject public CoffeeMaker(Lazy<Heater> heater, Pump pump, Set<Flavor> flavors) {
    this.heater = heater;
    this.pump = pump;
    this.flavors = flavors;
  }

  public void brew() {
    heater.get().on();
    pump.pump();
    System.out.println(" [_]P coffee! [_]P ");
    heater.get().off();
    System.out.println("Flavors: " + flavors);
  }
}
