# CloudinaryService

## What it does

- Upload images — it takes a file, validates it, sends it to Cloudinary, and returns the URL and public ID.
- Delete images — it takes a public ID, sends it to Cloudinary, and deletes the image.
- Think of it like a delivery service: the controller hands it a package (image), it checks the package is okay, ships it to Cloudinary, and gives back the tracking number (public ID) and delivery link (URL).

## Tools and setup

### Fields

```java
private final Cloudinary cloudinary;
private final ImageValidator imageValidator;
```

- `private final Cloudinary cloudinary;` and `private final ImageValidator imageValidator;` are the tools CloudinaryService needs. Spring automatically provides them through the constructor so they are available for use but cannot be changed later.
    - `private` — only this class can access them.
    - `final` — they are set once and never changed.
    - `Cloudinary / ImageValidator` — the types of the tools.
    - `cloudinary / imageValidator` — the names used to call them.

### Constructor

```java
public CloudinaryService( Cloudinary cloudinary,ImageValidator imageValidator) 
{
    this.cloudinary = cloudinary;
    this.imageValidator = imageValidator;
}
```

- This is the constructor. It sets up the service with the tools it needs.
- `This.cloudinary = cloudinary;` and `this.imageValidator = imageValidator;` store the tools in the service so it can use them later.

## Uploading an image

### Method signature

```java
public ImageUploadResponse uploadImage( MultipartFile file,String folder) 
```

- This is the method that uploads the image.
- `MultipartFile file` is the image file that the user uploads. It's like a package that contains the image data. Also it is an interface provided by Spring Framework that represents an uploaded file received in a web request.
- `String folder` is the folder where the image will be stored in Cloudinary. It's like the address where the image will be delivered.

### Validation

```java
imageValidator.validate(file);
```

- This validates the image file. It checks if the file is valid and if it is, it returns the validated file.

### Upload logic

```java
try{
    Map<?, ?> uploadResult = cloudinary.uploader().upload(
        file.getBytes(), ObjectUtils.asMap("folder", folder, "resource_type", "image"));
    }
```

- This is the main logic of the method. It tries to upload the image to Cloudinary.
- `cloudinary.uploader().upload()` is the method that uploads the image.
- `file.getBytes()` gets the image data as bytes.
- `ObjectUtils.asMap("folder", folder, "resource_type", "image")` creates a map of options for the upload.
- `"folder"` is the folder where the image will be stored in Cloudinary.
- `"resource_type"` is the type of resource being uploaded (image, video, etc.).
- `"image"` is the value for `"resource_type"` which means the resource is an image.
- `Map<?, ?> uploadResult` means the method returns a map with unknown key and value types. This map contains the result of the upload operation.

### Getting the URL

```java
String url = uploadResult.get("secure_url").toString();
```

- This gets the secure URL of the uploaded image from the uploadResult map.
- `uploadResult.get("secure_url")` gets the value associated with the key "secure_url" from the uploadResult map.
- `.toString()` converts the value to a string.

### Response

```java
return new ImageUploadResponse(url, publicId);
```

- This creates a new ImageUploadResponse object with the secure URL and public ID of the uploaded image.
- `ImageUploadResponse(url, publicId)` is the constructor of the ImageUploadResponse class.
- The constructor takes the secure URL and public ID as parameters and returns a new ImageUploadResponse object.

### Error handling

```java
catch (IOException exception) 
throw new MediaUploadException("Failed to upload image to cloudinary", exception);
```

- This is the exception handling block. It catches any IOException that might occur during the upload process.
- If an exception occurs, it throws a MediaUploadException with a custom error message and the original exception as the cause.

## Deleting an image

```java
public void deleteImage(String publicId){
    if(publicId == null || publicId.isBlank()){
        return;
    }try{
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());

    } catch (IOException exception){
        throw new MediaUploadException("Failed to delete image from cloudinary", exception);
}
```

- This method deletes an image from Cloudinary by its publicId.
- `public void deleteImage(String publicId)` — a method that takes the image’s publicId.
- `if (publicId == null || publicId.isBlank())` — if the ID is missing or empty, stop and do nothing.
- `cloudinary.uploader().destroy(...)` — sends a delete request to Cloudinary for that image.
- `ObjectUtils.emptyMap()` — passes no extra options.
- `catch (IOException ...)` — if the delete fails, throws MediaUploadException with the error.
- Simple version: “If the ID is valid, ask Cloudinary to delete the image; if that fails, throw an error.”

### ObjectUtils

- `ObjectUtils` is a helper class from the Cloudinary Java library. It provides shortcuts for working with Java objects and collections. t is used for two things:
  - `ObjectUtils.asMap("folder", folder, "resource_type", "image")` — creates a map with those key-value pairs.

## Chef analogy

Imagine a restaurant opens for the day. The manager hires a chef and gives them two tools:

```java
Chef mario = new Chef("Mario");
SousChef luigi = new SousChef("Luigi");
 
Kitchen kitchen = new Kitchen(mario, luigi);
```

When `new Kitchen(mario, luigi)` is called, this happens:

```java
public Kitchen(Chef chef, SousChef sousChef) {
    this.chef = chef;       // "the kitchen's chef is Mario"
    this.sousChef = sousChef; // "the kitchen's sous chef is Luigi"
}
```

After that, the Kitchen can do its job because it has Mario and Luigi ready.

Now back to your code. When Spring starts the app, it does something similar behind the scenes:

```java
Cloudinary cloudinary = ...        // from your CloudinaryConfig
ImageValidator imageValidator = ... // from your ImageValidator class
 
CloudinaryService service = new CloudinaryService(cloudinary, imageValidator);
```

So the constructor receives the two objects and stores them in the fields. Later, CloudinaryService can call:

```java
imageValidator.validate(file);
cloudinary.uploader().upload(...);
```

because the constructor already put them in place.

## Photo shop analogy

### 1. Imports and package

```java
package ...;
import ...;
```

This is like the employee getting their ID badge and making sure they know the names of all the other offices and forms they need to use.

### 2. `@Service`

```java
@Service
public class CloudinaryService {
```

This is Spring saying, *“This employee is officially on staff and ready to work.”*

### 3. The two tools on the desk

```java
private final Cloudinary cloudinary;
private final ImageValidator imageValidator;
```

This employee has two tools on their desk, and nobody else touches them directly:

- `cloudinary` — the phone line to the photo warehouse.
- `imageValidator` — a quality checker who makes sure photos are acceptable.

Both are `final` because the employee only needs one of each, and they never get swapped out.

### 4. Constructor

```java
public CloudinaryService(Cloudinary cloudinary, ImageValidator imageValidator) {
    this.cloudinary = cloudinary;
    this.imageValidator = imageValidator;
}
```

On the employee’s first day, the manager hands them the phone and the quality checker. They put them on their desk.

`this.cloudinary = cloudinary` just means, *“The phone on my desk is the phone the manager gave me.”*

### 5. Uploading a photo

```java
public ImageUploadResponse uploadImage(MultipartFile file, String folder) {
```

A customer walks in with a photo (`file`) and says which album (`folder`) to store it in.

```java
imageValidator.validate(file);
```

The employee gives the photo to the quality checker first. If it’s the wrong size or wrong type, the checker sends it back immediately.

```java
Map<?, ?> uploadResult = cloudinary.uploader().upload(
    file.getBytes(),
    ObjectUtils.asMap("folder", folder, "resource_type", "image")
);
```

The employee dials the warehouse and sends the photo over the phone line. They also include a sticky note:

- `folder` — *“Put this in the ‘vacation-2026’ album.”*
- `resource_type` — *“This is a photo, not a video.”*

The warehouse stores the photo and sends back a receipt (`uploadResult`).

```java
String url = uploadResult.get("secure_url").toString();
String publicId = uploadResult.get("public_id").toString();
```

The employee reads the receipt and pulls off two important details:

- `url` — the public link where the photo can be viewed.
- `publicId` — the warehouse’s internal tracking number for that photo.

```java
return new ImageUploadResponse(url, publicId);
```

The employee writes those two details on a card and hands it back to the customer.

```java
catch (IOException exception) {
    throw new MediaUploadException("Failed to upload image to Cloudinary.", exception);
}
```

If the phone line goes down or the warehouse doesn’t answer, the employee does not pretend nothing happened. They raise an alarm and tell the customer, *“Sorry, the upload failed.”*

### 6. Deleting a photo

```java
public void deleteImage(String publicId) {
```

A customer calls and says, *“Please destroy photo #123.”*

```java
if (publicId == null || publicId.isBlank()) {
    return;
}
```

If the customer gives no tracking number, the employee says, *“I can’t do anything with that,”* and hangs up. No need to bother the warehouse.

```java
cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
```

If there is a tracking number, the employee calls the warehouse and says, *“Destroy photo #123, no special instructions.”* The empty map means there are no extra options — just destroy it.

```java
catch (IOException exception) {
    throw new MediaUploadException("Failed to delete image from Cloudinary.", exception);
}
```

If the warehouse can’t destroy it, the employee raises an alarm and reports the failure.

### The whole idea

`CloudinaryService` is the front-desk employee who takes customer photos, gets them checked, ships them to Cloudinary, returns the receipt details, and can also call to have photos destroyed.
