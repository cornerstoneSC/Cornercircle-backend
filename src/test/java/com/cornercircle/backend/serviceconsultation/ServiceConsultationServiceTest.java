package com.cornercircle.backend.serviceconsultation;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServiceConsultationServiceTest {
    final ServiceConsultationRepository repository=mock(ServiceConsultationRepository.class);
    final ServiceConsultationService service=new ServiceConsultationService(repository);
    @Test void createsCompanionshipRequest(){
        when(repository.save(any())).thenAnswer(invocation->invocation.getArgument(0));
        var result=service.create(new ConsultationCreateRequest(ConsultationType.COMPANIONSHIP," Gloria  Djonret ","GLORIA@example.com","555-0100",null,LocalDate.now().plusDays(2),"9:00 AM","Weekly visits"));
        assertEquals("Gloria Djonret",result.name()); assertEquals("gloria@example.com",result.email()); assertEquals(ConsultationStatus.REQUESTED,result.status());
    }
    @Test void eventPlanningRequiresOrganization(){
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.create(new ConsultationCreateRequest(ConsultationType.EVENT_PLANNING,"Gloria","gloria@example.com",null,null,null,null,null))).getStatusCode().value());
        verifyNoInteractions(repository);
    }
    @Test void updatesStatusAndNotes(){
        var id=UUID.randomUUID(); var value=new ServiceConsultation(ConsultationType.COMPANIONSHIP,"Gloria","gloria@example.com");
        when(repository.findById(id)).thenReturn(Optional.of(value)); when(repository.save(value)).thenReturn(value);
        var result=service.update(id,new ConsultationUpdateRequest(ConsultationStatus.APPROVED,"Approved after consultation",null));
        assertEquals(ConsultationStatus.APPROVED,result.status()); assertEquals("Approved after consultation",result.adminNotes());
    }
}
