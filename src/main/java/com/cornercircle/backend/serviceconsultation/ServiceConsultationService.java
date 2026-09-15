package com.cornercircle.backend.serviceconsultation;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ServiceConsultationService {
    private final ServiceConsultationRepository repository;
    public ServiceConsultationService(ServiceConsultationRepository repository){this.repository=repository;}
    @Transactional public ConsultationResponse create(ConsultationCreateRequest request){
        if(!blank(request.website())) throw new ResponseStatusException(BAD_REQUEST,"Unable to submit consultation request.");
        if(request.preferredDate()!=null && request.preferredDate().isBefore(LocalDate.now())) throw new ResponseStatusException(BAD_REQUEST,"Preferred date cannot be in the past.");
        if(request.type()==ConsultationType.EVENT_PLANNING && blank(request.organization())) throw new ResponseStatusException(BAD_REQUEST,"Organization or retirement-home name is required for event planning.");
        var value=new ServiceConsultation(request.type(),clean(request.name()),clean(request.email()).toLowerCase());
        value.setPhone(cleanNullable(request.phone())); value.setOrganization(cleanNullable(request.organization()));
        value.setPreferredDate(request.preferredDate()); value.setPreferredTime(cleanNullable(request.preferredTime())); value.setClientNotes(cleanNullable(request.notes()));
        return ConsultationResponse.from(repository.save(value));
    }
    @Transactional(readOnly=true) public List<ConsultationResponse> list(){return repository.findAllByOrderByCreatedAtDesc().stream().map(ConsultationResponse::from).toList();}
    @Transactional public ConsultationResponse update(UUID id,ConsultationUpdateRequest request){
        var value=repository.findById(id).orElseThrow(()->new ResponseStatusException(NOT_FOUND,"Consultation not found."));
        value.setStatus(request.status()); value.setAdminNotes(cleanNullable(request.adminNotes())); value.setGoogleCalendarEventId(cleanNullable(request.googleCalendarEventId()));
        return ConsultationResponse.from(repository.save(value));
    }
    private static boolean blank(String value){return value==null||value.isBlank();}
    private static String clean(String value){return value.trim().replaceAll("\\s+"," ");}
    private static String cleanNullable(String value){return blank(value)?null:value.trim();}
}
