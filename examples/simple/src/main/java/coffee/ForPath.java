package coffee;

/**
 * TODO: Insert description here. (generated by houcy)
 */
@MapKey
public @interface ForPath {
  enum PathEnum{
    Admin("/admin"),
    Login("/login");
    private final String path;
    PathEnum(String path) {
      this.path = path;
    }
  }
  PathEnum value();
} 