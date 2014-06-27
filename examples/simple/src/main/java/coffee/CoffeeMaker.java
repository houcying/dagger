package coffee;

import dagger.Lazy;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

class CoffeeMaker {
  final Lazy<Heater> heater; // Don't want to create a possibly costly heater until needed.
  final Pump pump;
  final Map<String, Provider<FlavorProcessor>> dispatcher;
  
  @Inject CoffeeMaker(
      Map<String, Provider<FlavorProcessor>> dispatcher,
      Lazy<Heater> heater,
      Pump pump
     ) {
    this.dispatcher = dispatcher;
    this.heater = heater;
    this.pump = pump;
  }

  public void brew() {
    heater.get().on();
    pump.pump();
    System.out.println("Preparing Flavoring:");
    for (Map.Entry<String, Provider<FlavorProcessor>> e: dispatcher.entrySet()) {
      System.out.println("Flavor: " + e.getKey() + " has dispatcher : " + e.getValue().get().toString());
    }
    System.out.println(" [_]P coffee! [_]P ");
    heater.get().off();
  }
}

