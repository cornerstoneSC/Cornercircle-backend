package com.cornercircle.backend.servicespage;

import com.cornercircle.backend.media.service.CloudinaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServicesPageControllerTest {
    final ObjectMapper mapper = new ObjectMapper();
    final ServicesPageRepository repository = mock(ServicesPageRepository.class);
    final CloudinaryService media = mock(CloudinaryService.class);
    final String token = "unit-test-token-with-at-least-32-characters";
    ServicesPageController controller(String configured) { return new ServicesPageController(repository,mapper,media,configured); }
    ObjectNode content() {
        var node=mapper.createObjectNode();
        for(String field:new String[]{"heroLabel","heroTitle","heroDescription","heroCta","howLabel","heroAlt","gardenAlt","note","approachTitle","approachDescription","stepsTitle","faqTitle","enquiryTitle","enquiryDescription"}) node.put(field,"Example content");
        node.put("heroImage","/images/services/hero.jpg"); node.put("gardenImage","https://example.com/garden.jpg");
        var offerings=node.putArray("offerings"); for(int i=0;i<4;i++) offerings.addObject().put("title","Title").put("description","Description");
        var steps=node.putArray("steps"); for(int i=0;i<3;i++) steps.addObject().put("title","Step").put("text","Text");
        var questions=node.putArray("questions"); for(int i=0;i<4;i++) questions.addObject().put("question","Question?").put("answer","Answer").putArray("items");
        return node;
    }
    @Test void missingConfigurationDisablesWrites() {
        assertEquals(503,assertThrows(ResponseStatusException.class,()->controller("").save(null,content())).getStatusCode().value());
        verifyNoInteractions(repository,media);
    }
    @Test void wrongTokenCannotSaveOrUpload() {
        var service=controller(token);
        assertEquals(401,assertThrows(ResponseStatusException.class,()->service.save("Bearer wrong",content())).getStatusCode().value());
        assertThrows(ResponseStatusException.class,()->service.upload(null,null));
        verifyNoInteractions(repository,media);
    }
    @Test void validSaveAndPublicReadRoundTrip() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        var original=content(); var service=controller(token);
        assertEquals(original,service.save("Bearer "+token,original));
        var captor=org.mockito.ArgumentCaptor.forClass(ServicesPageEntity.class);
        verify(repository).save(captor.capture());
        when(repository.findById(1L)).thenReturn(Optional.of(captor.getValue()));
        assertEquals(original,service.get().getBody());
    }
    @Test void missingSavedPageReturnsNoContent() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertEquals(204,controller(token).get().getStatusCode().value());
    }
    @Test void malformedAndUnsafeContentCannotSave() {
        var invalid=content(); invalid.put("heroImage","javascript:alert(1)");
        assertThrows(ResponseStatusException.class,()->controller(token).save("Bearer "+token,invalid));
        invalid.put("heroImage","/images/hero.jpg"); invalid.putArray("offerings");
        assertThrows(ResponseStatusException.class,()->controller(token).save("Bearer "+token,invalid));
        verifyNoInteractions(repository);
    }
}
