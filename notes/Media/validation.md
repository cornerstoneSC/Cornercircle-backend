# ImageValidator.java

- Validates image files before upload
- Checks file size (max 10MB)
- Checks file type (JPEG, PNG, WebP only)
- Throws InvalidMediaException for invalid files

## Imports

### `@Component`

```java
import org.springframework.stereotype.Component;
```

- `Component` is a Spring annotation that marks a class as a Spring-managed bean.
- `@Component` tells Spring to automatically detect and register this class as a bean.
- It allows the class to be automatically detected and registered as a bean in the Spring application context.

### `MultipartFile`

```java
import org.springframework.web.multipart.MultipartFile;
```

- `MultipartFile` is a Spring Framework class that represents an uploaded file.
- It provides methods to access the file's content, name, size, and other metadata.
- `MultipartFile` — the class name. PascalCase is the Java convention for class names.

## Fields

### `MAX_FILE_SIZE`

```java
private static final long MAX_FILE_SIZE =10 * 1024 * 1024;
```

- Maximum file size in bytes (10MB)
- `10 * 1024 * 1024 = 10 * 1024 * 1024 = 10,485,760 bytes = 10MB`
- `private:` only this class can see or use this value.
- `static:` no need to create an instance of the class to use this value.
- `final:` the value cannot be changed once it is set.
- `MAX_FILE_SIZE` — the variable name. UPPER_SNAKE_CASE is the Java convention for constants.

### `ALLOWED_IMAGE_TYPES`

```java
private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
```

- Set of allowed image MIME types
- `Set<String>` is a Java collection that holds a group of unique String values. It does not allow duplicates and has no guaranteed order.
- `Set.of()` creates an immutable set
- Immutable sets are thread-safe and prevent accidental modification
- `ALLOWED_IMAGE_TYPES` — the variable name. UPPER_SNAKE_CASE is the Java convention for constants.

## Method

### `validateImage`

```java
public static void validateImage(MultipartFile file) 
```

- `validateImage` is the method name. camelCase is the Java convention for method names.
- `public:` other classes can call this method.
- `static:` no need to create an instance of the class to use this method.
- `void:` the method doesn't return a value.

### `getContentType`

```java
String contentType = file.getContentType();
```

- Gets the MIME type of the uploaded file.
- `file.getContentType()` returns a String like "image/jpeg" or "image/png".
- `contentType` — the variable name. camelCase is the Java convention for variable names.
