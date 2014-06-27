package dagger.internal;

import dagger.Factory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Provider;

/**
 * TODO: Insert description here. (generated by houcy)
 * @param <V>
 * @param <K>
 */
public class MapProviderFactory<K, V> implements Factory<Map<K, Provider<V>>> {
  private final LinkedHashMap<K, Provider<V>> contributingMap;


  public static class Builder<K, V> {
    private final int size;
    private final LinkedHashMap<K, Provider<V>> mapBuilder;

    public Builder(int size) {
      this.size = size;
      this.mapBuilder = new LinkedHashMap<K, Provider<V>>(size);
    }
    public MapProviderFactory<K, V> build() {
      return new MapProviderFactory<K, V>(this.mapBuilder);
    }

    public Builder<K, V> put(K k, Provider<V> pv) {
      this.mapBuilder.put(k, pv);
      return this;
    }
  }

  public static <K, V> Builder<K, V> builder(int size) {
    return new Builder<K, V>(size);
  }

  private MapProviderFactory(LinkedHashMap<K, Provider<V>> contributingMap) {
    this.contributingMap = contributingMap;
  }

  @Override
  public Map<K, Provider<V>> get() {
    return Collections.unmodifiableMap(contributingMap);
  }
}
