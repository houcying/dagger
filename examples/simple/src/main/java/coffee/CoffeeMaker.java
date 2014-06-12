package coffee;

import dagger.Lazy;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

class CoffeeMaker {
  final Lazy<Heater> heater; // Don't want to create a possibly costly heater until needed.
  final Pump pump;
  final Set<Flavor> beanFlavors;
  final Map<String, String> location;
  MilkSteamer steamer;
  @Inject CoffeeMaker(
      Set<Flavor> beanFlavors,
      Map<String, String> location,
      Lazy<Heater> heater,
      Pump pump,
      MilkSteamer steamer) {
    this.location = location;
    this.heater = heater;
    this.pump = pump;
    this.beanFlavors = beanFlavors;
    this.steamer = steamer;
  }

  public void brew() {
    heater.get().on();
    pump.pump();
    System.out.println("Preparing Bean Flavoring: " + beanFlavors);
    steamer.steam();
    System.out.println(" [_]P coffee! [_]P ");
    heater.get().off();
  }
}
