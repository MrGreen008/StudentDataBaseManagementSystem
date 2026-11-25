package edu.vishvaya.portal.service;

import edu.vishvaya.portal.api.EnquiryReceipt;
import edu.vishvaya.portal.api.EnquiryRequest;
import edu.vishvaya.portal.persistence.EnquiryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnquiryService {

    private final EnquiryRepository repository;

    public EnquiryService(EnquiryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public EnquiryReceipt submit(EnquiryRequest request) {
        return repository.save(request);
    }
}
