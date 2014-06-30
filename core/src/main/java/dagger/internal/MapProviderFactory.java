package dagger.internal;

import dagger.Factory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

/**
 * A {@link Factory} implementation used to implement {@link Map} bindings. This factory always
 * returns a new {@link Map<K, Provider<V>>} instance for each call to {@link #get} (as required by {@link Factory})
 * whose elements are populated by subsequent calls to their {@link Provider#get} methods.
 *
 * @author Chenying Hou
 * @since 2.0
 * 
 * @param <V>
 * @param <K>
 */
public class MapProviderFactory<K, V> implements Factory<Map<K, Provider<V>>> {
  private final LinkedHashMap<K, Provider<V>> contributingMap;

  /**
   * A builder to help build the {@link MapProviderFactory}
   */
  public static class Builder<K, V> {
    private final LinkedHashMap<K, Provider<V>> mapBuilder;
    
    public Builder(int size) {
      this.mapBuilder = new LinkedHashMap<K, Provider<V>>(size);
    }
    /**
     * Returns a new {@link MapProviderFactory} 
     */
    public MapProviderFactory<K, V> build() {
      return new MapProviderFactory<K, V>(this.mapBuilder);
    }
    /**
     * Associate k with providerOfValue in {@link Builder}
     */
    public Builder<K, V> put(K key, Provider<V> providerOfValue) {
      this.mapBuilder.put(key, providerOfValue);
      return this;
    }
  }

  /**
   * Returns a new {@link Builder} 
   */
  public static <K, V> Builder<K, V> builder(int size) {
    return new Builder<K, V>(size);
  }

  private MapProviderFactory(LinkedHashMap<K, Provider<V>> contributingMap) {
    this.contributingMap = contributingMap;
  }

  /**
   * Returns a {@link Map<K, Provider<V>>} whose iteration order is that of the elements given by each of the
   * providers, which are invoked in the order given at creation.
   *
   */
  @Override
  public Map<K, Provider<V>> get() {
    return Collections.unmodifiableMap(contributingMap);
  }
}
