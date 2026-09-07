CloudinaryConfig.java

- @Value("${cloudinary.cloud-name}")
  @Value is a Spring annotation. It tells Spring “Take a value from my application configuration and put it into this variable.”

- private String cloudName; 
  We make these fields private because they are internal details of CloudinaryConfig. Other classes should not directly access or modify them. That is called encapsulation.

- @Bean
  @Bean is another Spring annotation. It tells Spring, “Whenever someone asks for a Cloudinary object, give them the one I made here.”

  - public Cloudinary cloudinary()
    This is a method that creates and returns a Cloudinary object. It’s like a factory that builds a Cloudinary for us.

- We create a HashMap called config
  A HashMap is like a dictionary. It stores key-value pairs. In this case, the keys are strings like "cloud_name", "api_key", and "api_secret", and the values are the actual values we read from our configuration. So the HashMap is simply a convenient container for passing all Cloudinary settings together.

